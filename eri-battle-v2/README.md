# ERI Battle V3

New package `com.eri.battlebot`, created independently from the old Xmagic COC APK.

## User opt-in features

- Battle button detection on a *live* AccessibilityService screenshot and automatic centre tap.
- Troop deployment when the active battle HUD is confirmed by on-device OCR (two successive readings), **only when Auto deploy is switched on**.
- An accessible floating in-game **THẢ LÍNH** manual trigger for layouts where OCR cannot read the HUD, and a **DỪNG** button to abort remaining taps.
- Troop card and edge deployment points derived from the screenshot dimensions and image features (approximate; they are not verified deployable regions). Configure 1–7 troop slots, 1–8 taps per slot.
- Optional on-device OCR of Return Home/Về nhà after deployment. Does not simulate battle completion or guarantee a win.
- One attack per cycle. Press *Làm mới chu kỳ* before another. State and logs visible in app.
- No game process modification, memory hooks, account automation or legacy services.

## Limitations
The V3 detector uses pixel-color heuristics and screen OCR, not Clash of Clans game-state APIs. UI skins, terrain, army layout, language, zoom and game updates can change success rate. Test manually with the game open; only enable Auto deploy after confirming the intended troop slots. A green area in a screenshot is **not** proof deployment is allowed.

Requires Android 11+, Accessibility service with screenshot and gesture privileges. OCR model is bundled in the APK and runs locally. No root required.

## Build
JDK 17, Gradle 8.9, Android SDK 35, AGP 8.7.3.

`gradle :app:assembleDebug` from `eri-battle-v2/`.

GitHub Actions workflow: `.github/workflows/eri-battle-v2.yml` on `eri-battle-v3`.
