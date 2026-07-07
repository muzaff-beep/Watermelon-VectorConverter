// Watermelon Vector Converter — VD -> SVG reverse conversion (Contract C-4).
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.

use svg_converter_core::convert_vd;

#[test]
fn basic_path_round_trips() {
    let vd = br##"<vector xmlns:android="http://schemas.android.com/apk/res/android"
        android:width="24dp" android:height="24dp"
        android:viewportWidth="24" android:viewportHeight="24">
        <path android:pathData="M2,2 L22,2 L22,22 Z" android:fillColor="#FFFF0000" android:fillType="nonZero"/>
    </vector>"##;
    let out = convert_vd(vd).expect("should convert");
    assert!(out.contains("<svg"), "{out}");
    assert!(out.contains("viewBox=\"0 0 24 24\""), "{out}");
    assert!(out.contains("d=\"M2,2 L22,2 L22,22 Z\""), "{out}");
    assert!(out.contains("fill=\"#FF0000\""), "{out}");
}

#[test]
fn group_transform_round_trips() {
    let vd = br##"<vector xmlns:android="http://schemas.android.com/apk/res/android"
        android:viewportWidth="24" android:viewportHeight="24">
        <group android:translateX="5" android:translateY="3" android:rotation="45">
            <path android:pathData="M0,0 L1,1" android:fillColor="#FF000000"/>
        </group>
    </vector>"##;
    let out = convert_vd(vd).expect("should convert");
    assert!(out.contains("<g"), "{out}");
    assert!(out.contains("translate(5,3)"), "{out}");
    assert!(out.contains("rotate(45)"), "{out}");
}

#[test]
fn clip_path_round_trips_to_clip_path_def() {
    let vd = br##"<vector xmlns:android="http://schemas.android.com/apk/res/android"
        android:viewportWidth="24" android:viewportHeight="24">
        <group>
            <clip-path android:pathData="M6,6 h12 v12 h-12 z"/>
            <path android:pathData="M0,24 L12,0 L24,24 Z" android:fillColor="#FF000000"/>
        </group>
    </vector>"##;
    let out = convert_vd(vd).expect("should convert");
    assert!(out.contains("<clipPath"), "{out}");
    assert!(out.contains("clip-path=\"url(#"), "{out}");
    assert!(out.contains("d=\"M6,6 h12 v12 h-12 z\""), "{out}");
}

#[test]
fn linear_gradient_round_trips() {
    let vd = br##"<vector xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:aapt="http://schemas.android.com/aapt"
        android:viewportWidth="24" android:viewportHeight="24">
        <path android:pathData="M0,0 L24,24" android:fillType="nonZero">
            <aapt:attr name="android:fillColor">
                <gradient android:type="linear" android:startX="0" android:startY="0" android:endX="24" android:endY="0">
                    <item android:offset="0" android:color="#FFFF0000"/>
                    <item android:offset="1" android:color="#FF0000FF"/>
                </gradient>
            </aapt:attr>
        </path>
    </vector>"##;
    let out = convert_vd(vd).expect("should convert");
    assert!(out.contains("<linearGradient"), "{out}");
    assert!(out.contains("stop-color=\"#FF0000\""), "{out}");
    assert!(out.contains("stop-color=\"#0000FF\""), "{out}");
    assert!(out.contains("fill=\"url(#grad"), "{out}");
}

#[test]
fn stroke_round_trips() {
    let vd = br##"<vector xmlns:android="http://schemas.android.com/apk/res/android"
        android:viewportWidth="24" android:viewportHeight="24">
        <path android:pathData="M0,0 L24,24" android:strokeColor="#FF00FF00" android:strokeWidth="2"/>
    </vector>"##;
    let out = convert_vd(vd).expect("should convert");
    assert!(out.contains("stroke=\"#00FF00\""), "{out}");
    assert!(out.contains("stroke-width=\"2\""), "{out}");
}

#[test]
fn invalid_root_element_is_rejected() {
    let vd = br##"<not-a-vector/>"##;
    assert_eq!(convert_vd(vd).unwrap_err().code(), 1001);
}
