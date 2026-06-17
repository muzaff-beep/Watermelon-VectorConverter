#!/usr/bin/env python3
# Watermelon Vector Converter
# Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
# Proprietary and source-available. See LICENSE.
"""
JNI signature parity checker (Contract C-3, Phase 6).

Parses the Kotlin `external fun` declarations and the Rust `Java_...` exports
and verifies, for each native method:
  - it exists on both sides
  - the argument counts match (after the implicit env/class on the Rust side)
  - the Kotlin types map to the Rust JNI types used

Exit code 0 = parity OK; nonzero = drift (breaks CI).
This runs WITHOUT a JVM, so it works in any CI container.
"""
import re
import sys
from pathlib import Path

# Kotlin type -> the JNI Rust type we expect to see in the signature.
KT_TO_RUST = {
    "ByteArray": "JByteArray",
    "String":    "JString",      # param; return String is jstring
    "Int":       "jint",
    "ProgressCallback": "JObject",
}
# Kotlin return type -> Rust return type
KT_RET_TO_RUST = {
    "String":    "jstring",
    "ByteArray": "jbyteArray",
    "":          "()",           # void
}

def parse_kotlin(path: Path):
    text = path.read_text()
    methods = {}
    # external fun name(arg: Type, ...): Ret    (Ret optional)
    pat = re.compile(r"external\s+fun\s+(\w+)\s*\(([^)]*)\)\s*(?::\s*(\w+))?")
    for m in pat.finditer(text):
        name, args_s, ret = m.group(1), m.group(2).strip(), (m.group(3) or "")
        args = []
        if args_s:
            for a in args_s.split(","):
                # "svg: ByteArray"
                t = a.split(":")[1].strip()
                args.append(t)
        methods[name] = {"args": args, "ret": ret}
    return methods

def parse_rust(path: Path):
    text = path.read_text()
    methods = {}
    # pub extern "system" fn Java_pkg_Class_methodName<'a>( env, _cls, args... ) -> Ret
    pat = re.compile(
        r'extern\s+"system"\s+fn\s+(Java_[\w]+?)\s*(?:<[^>]*>)?\s*\(([^)]*)\)\s*(?:->\s*([\w:()]+))?',
        re.DOTALL,
    )
    for m in pat.finditer(text):
        full, args_s, ret = m.group(1), m.group(2), (m.group(3) or "()")
        # JNI symbol: Java_<pkg>_<Class>_<method>. The method is the segment
        # after the class name; by convention our methods start with "native".
        idx = full.rfind("_native")
        name = full[idx + 1:] if idx != -1 else full.split("_")[-1]
        # strip the implicit env + class (first two params)
        raw = [a.strip() for a in args_s.split(",") if a.strip()]
        # each like "svg: JByteArray<'a>" ; drop env/_cls
        typed = []
        for a in raw:
            if ":" not in a:
                continue
            t = a.split(":", 1)[1].strip()
            t = re.sub(r"<[^>]*>", "", t).strip()  # drop lifetimes
            typed.append(t)
        # first two are JNIEnv and JClass
        arg_types = typed[2:] if len(typed) >= 2 else []
        methods[name] = {"args": arg_types, "ret": ret.strip()}
    return methods

def main():
    root = Path(__file__).resolve().parent.parent
    kt = parse_kotlin(root / "android/app/src/main/java/com/watermelon/converter/jni/SvgConverterNative.kt")
    rs = parse_rust(root / "svg-converter-core/src/jni.rs")

    errors = []
    for name, k in kt.items():
        if name not in rs:
            errors.append(f"MISSING on Rust side: {name}")
            continue
        r = rs[name]
        if len(k["args"]) != len(r["args"]):
            errors.append(f"ARG COUNT mismatch in {name}: kotlin={len(k['args'])} rust={len(r['args'])}")
            continue
        for i, (kt_t, rs_t) in enumerate(zip(k["args"], r["args"])):
            want = KT_TO_RUST.get(kt_t)
            if want is None:
                errors.append(f"{name} arg{i}: unknown Kotlin type {kt_t}")
            elif want != rs_t:
                errors.append(f"{name} arg{i}: kotlin {kt_t}->expect {want}, rust has {rs_t}")
        want_ret = KT_RET_TO_RUST.get(k["ret"], None)
        if want_ret is not None and r["ret"] not in (want_ret, "()" if want_ret == "()" else want_ret):
            errors.append(f"{name} return: kotlin {k['ret']!r}->expect {want_ret}, rust has {r['ret']}")

    for name in rs:
        if name not in kt:
            errors.append(f"EXTRA on Rust side (no Kotlin decl): {name}")

    print(f"Kotlin methods: {sorted(kt)}")
    print(f"Rust   methods: {sorted(rs)}")
    if errors:
        print("\nPARITY FAILURES:")
        for e in errors:
            print("  -", e)
        return 1
    print(f"\nOK — {len(kt)} native methods match across the FFI boundary.")
    return 0

if __name__ == "__main__":
    sys.exit(main())
