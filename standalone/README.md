# ERI Standalone — one dylib, no zsmvn dependency

Independent iOS ARM64 Dear ImGui / Metal overlay.

## Implemented and wired
- `ERI` floating button; open/close ImGui overlay.
- `Mod By Eri Nguyễn` title using native iOS font, changing hue continuously.
- Menu opacity slider, color speed slider, FPS display and preference persistence.
- Touch passthrough outside menu and button.

## NOT transferred
Original `zsmvn.dylib` contains Unity/IL2CPP game-specific hooks, private offsets, game state machines and replay/vehicle routines. Those are NOT included. This repo does not call, embed, load, link to, or otherwise require `zsmvn.dylib`. The `Tinh nang game` tab explicitly describes what is missing and offers **no fake switches**.

Migrating actual old game functionality requires a verified reimplementation for each feature from the old binary plus runtime testing against the target version. A UI-only build does not establish those game features work. This build is intended as a safe standalone foundation.

## Build / install
The macOS GitHub Actions workflow downloads the pinned Dear ImGui source and builds `ERIStandalone.dylib` for iPhoneOS arm64. Add the single library to an app you own and can modify, ensure your app loads it, then sign the app and dylib with the right iOS certificate. Do not include `zsmvn.dylib`.

No on-device validation was possible; ARM64 Mach-O build does not guarantee runtime compatibility.