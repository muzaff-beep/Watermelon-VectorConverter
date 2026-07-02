; Watermelon Vector Converter — installer hook
; Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
;
; The viewer binary itself is bundled automatically via externalBin in
; tauri.conf.json (Tauri copies it into $INSTDIR as part of the standard
; install step, before this hook runs). This hook only adds the .svg file
; association pointing at the already-installed viewer binary.

!macro NSIS_HOOK_POSTINSTALL
  WriteRegStr HKCR ".svg" "" "WatermelonSvgFile"
  WriteRegStr HKCR "WatermelonSvgFile" "" "SVG Image"
  WriteRegStr HKCR "WatermelonSvgFile\DefaultIcon" "" "$INSTDIR\wvgc-viewer.exe,0"
  WriteRegStr HKCR "WatermelonSvgFile\shell\open\command" "" '"$INSTDIR\wvgc-viewer.exe" "%1"'
  System::Call 'Shell32::SHChangeNotify(i 0x08000000, i 0, i 0, i 0)'
!macroend

!macro NSIS_HOOK_POSTUNINSTALL
  DeleteRegKey HKCR ".svg"
  DeleteRegKey HKCR "WatermelonSvgFile"
!macroend
