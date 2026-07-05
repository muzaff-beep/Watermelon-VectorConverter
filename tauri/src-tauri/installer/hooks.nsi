; Watermelon Vector Converter — installer hook
; Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
;
; The viewer binary is bundled via externalBin in tauri.conf.json (copied
; into $INSTDIR automatically during the standard install step).
;
; File associations (.svg / .xml -> viewer) are NOT set here. Attempts to
; inject checkboxes into the Finish page failed: Tauri's installer.nsi
; already defines MUI_PAGE_FINISH, so installerHooks cannot redefine or
; re-insert it (confirmed: tauri-apps/tauri#15267, #10850). Associations
; are instead offered via a first-run dialog and toggleable anytime in the
; app's Settings screen, both calling the same HKCU registry commands.

!macro NSIS_HOOK_POSTUNINSTALL
  DeleteRegKey HKCU "Software\Classes\WatermelonVectorFile"
  DeleteRegValue HKCU "Software\Classes\.svg\OpenWithProgids" "WatermelonVectorFile"
  DeleteRegValue HKCU "Software\Classes\.xml\OpenWithProgids" "WatermelonVectorFile"
!macroend
