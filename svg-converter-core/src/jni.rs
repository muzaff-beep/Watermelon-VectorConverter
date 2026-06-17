// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. See LICENSE.

//! Rust JNI side of the FFI bridge (Contract C-3). THIN marshalling only.
//!
//! Signatures here MUST match android/.../jni/SvgConverterNative.kt byte-for-byte.
//! The CI parity checker (ci/verify_interfaces.py) enforces this on every push.
//!
//! STATUS: write + type-check only in CI containers. The real round-trip
//! (JVM loads the .so, ByteArray<->String, exception throwing, progress
//! callbacks across 500 events) must be validated by an on-device
//! instrumented test (Module B test plan, Doc 5 §6).
//!
//! This module is compiled only for Android targets.
#![cfg(target_os = "android")]

use crate::batch_processor::{convert_zip, CancelFlag, ProgressEvent};
use crate::error::ConversionError;
use crate::image_export::{render_svg_preview, render_vd_preview};
use crate::convert_svg;

use jni::objects::{JByteArray, JClass, JObject, JString, JValue};
use jni::sys::{jbyteArray, jint, jstring};
use jni::JNIEnv;
use std::sync::atomic::{AtomicBool, Ordering};

const EXCEPTION_CLASS: &str = "com/watermelon/converter/jni/ConversionException";

/// Process-wide cancel flag, shared between a running batch and nativeCancel().
static CANCEL: AtomicBool = AtomicBool::new(false);

/// Throw com.watermelon.converter.jni.ConversionException(code, message).
/// Never returns a value to Java on error; the pending exception is what Java sees.
fn throw_conversion(env: &mut JNIEnv, err: &ConversionError) {
    // Construct the exception object with (int, String) so the Kotlin side
    // can read .code. Fall back to a plain throw if construction fails.
    let msg = env.new_string(err.to_string()).ok();
    let built = match (env.find_class(EXCEPTION_CLASS), msg.as_ref()) {
        (Ok(cls), Some(m)) => env
            .new_object(
                cls,
                "(ILjava/lang/String;)V",
                &[JValue::Int(err.code() as jint), JValue::Object(&JObject::from(m))],
            )
            .ok(),
        _ => None,
    };
    if let Some(obj) = built {
        let _ = env.throw(jni::objects::JThrowable::from(obj));
    } else {
        let _ = env.throw_new(EXCEPTION_CLASS, err.to_string());
    }
}

fn bytes_from(env: &mut JNIEnv, arr: &JByteArray) -> Result<Vec<u8>, ConversionError> {
    env.convert_byte_array(arr)
        .map_err(|e| ConversionError::Internal(format!("jni byte read: {e}")))
}

/// nativeConvertSvg(svg: ByteArray): String   [Contract C-3]
#[no_mangle]
pub extern "system" fn Java_com_watermelon_converter_jni_SvgConverterNative_nativeConvertSvg<'a>(
    mut env: JNIEnv<'a>,
    _cls: JClass<'a>,
    svg: JByteArray<'a>,
) -> jstring {
    let null = JObject::null().into_raw();
    let bytes = match bytes_from(&mut env, &svg) {
        Ok(b) => b,
        Err(e) => { throw_conversion(&mut env, &e); return null; }
    };
    match convert_svg(&bytes) {
        Ok(xml) => match env.new_string(xml) {
            Ok(s) => s.into_raw(),
            Err(e) => { throw_conversion(&mut env, &ConversionError::Internal(e.to_string())); null }
        },
        Err(e) => { throw_conversion(&mut env, &e); null }
    }
}

/// nativeRenderSvgPreview(svg: ByteArray, px: Int): ByteArray   [Contract C-3]
#[no_mangle]
pub extern "system" fn Java_com_watermelon_converter_jni_SvgConverterNative_nativeRenderSvgPreview<'a>(
    mut env: JNIEnv<'a>,
    _cls: JClass<'a>,
    svg: JByteArray<'a>,
    px: jint,
) -> jbyteArray {
    let null = JObject::null().into_raw();
    let bytes = match bytes_from(&mut env, &svg) { Ok(b) => b, Err(e) => { throw_conversion(&mut env, &e); return null; } };
    match render_svg_preview(&bytes, px.max(0) as u32) {
        Ok(png) => env.byte_array_from_slice(&png).map(|a| a.into_raw()).unwrap_or(null),
        Err(e) => { throw_conversion(&mut env, &e); null }
    }
}

/// nativeRenderVdPreview(vdXml: String, px: Int): ByteArray   [Contract C-3]
#[no_mangle]
pub extern "system" fn Java_com_watermelon_converter_jni_SvgConverterNative_nativeRenderVdPreview<'a>(
    mut env: JNIEnv<'a>,
    _cls: JClass<'a>,
    vd_xml: JString<'a>,
    px: jint,
) -> jbyteArray {
    let null = JObject::null().into_raw();
    let xml: String = match env.get_string(&vd_xml) {
        Ok(s) => s.into(),
        Err(e) => { throw_conversion(&mut env, &ConversionError::Internal(e.to_string())); return null; }
    };
    match render_vd_preview(&xml, px.max(0) as u32) {
        Ok(png) => env.byte_array_from_slice(&png).map(|a| a.into_raw()).unwrap_or(null),
        Err(e) => { throw_conversion(&mut env, &e); null }
    }
}

/// nativeConvertZip(zip: ByteArray, cb: ProgressCallback): ByteArray   [Contract C-3]
/// Progress is delivered by calling cb.onProgress(done, total, currentName)
/// from the coordinator. The JNIEnv passed in is valid for the duration of
/// this call (single-threaded w.r.t. the callback, because convert_zip's
/// coordinator runs on THIS thread).
#[no_mangle]
pub extern "system" fn Java_com_watermelon_converter_jni_SvgConverterNative_nativeConvertZip<'a>(
    mut env: JNIEnv<'a>,
    _cls: JClass<'a>,
    zip: JByteArray<'a>,
    cb: JObject<'a>,
) -> jbyteArray {
    let null = JObject::null().into_raw();
    CANCEL.store(false, Ordering::SeqCst);

    let bytes = match bytes_from(&mut env, &zip) { Ok(b) => b, Err(e) => { throw_conversion(&mut env, &e); return null; } };

    // The progress sink calls back into Java on the coordinator thread (this one).
    // We use a Cell of the env via raw pointer is unsafe; instead route through a
    // RefCell-guarded closure capturing &mut env is not possible across the Fn
    // bound. Practical approach: collect progress markers is the safe default,
    // but the contract wants live callbacks. We therefore call back using a
    // short-lived JNIEnv obtained per event is overkill on one thread; instead
    // we capture a raw JNIEnv pointer known-valid for this synchronous call.
    let env_ptr: *mut jni::sys::JNIEnv = env.get_raw();
    let cb_ref = &cb;

    let sink = move |ev: ProgressEvent| {
        // SAFETY: convert_zip's coordinator invokes the sink synchronously on
        // the same thread that owns `env` for the duration of this JNI call.
        let mut env = unsafe { JNIEnv::from_raw(env_ptr).expect("valid env") };
        if let Ok(name) = env.new_string(&ev.current_name) {
            let _ = env.call_method(
                cb_ref,
                "onProgress",
                "(IILjava/lang/String;)V",
                &[
                    JValue::Int(ev.done as jint),
                    JValue::Int(ev.total as jint),
                    JValue::Object(&JObject::from(name)),
                ],
            );
        }
    };

    match convert_zip(&bytes, &sink, &CANCEL) {
        Ok(out) => env.byte_array_from_slice(&out).map(|a| a.into_raw()).unwrap_or(null),
        Err(e) => { throw_conversion(&mut env, &e); null }
    }
}

/// nativeCancel()   [Contract C-3]
#[no_mangle]
pub extern "system" fn Java_com_watermelon_converter_jni_SvgConverterNative_nativeCancel<'a>(
    _env: JNIEnv<'a>,
    _cls: JClass<'a>,
) {
    CANCEL.store(true, Ordering::SeqCst);
}
