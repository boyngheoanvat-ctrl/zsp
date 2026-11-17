# ERI ZSM — One-Dylib Hook Migration V3

This branch contains an independent `ERIStandalone.dylib` using Dear ImGui + Metal. It does **not** embed or load `zsmvn_no_login_key.dylib`.

## What is implemented
- Native ImGui menu, ERI icon, rainbow Vietnamese title.
- IL2CPP class scanner and existing guarded Unity world timeScale test (this is **not** vehicle speed).
- New read-only native metadata survey in `ERIPortDiagnostics.mm`: it enumerates target classes, method names and argument counts, and field offsets. The ImGui tab offers **SAO CHEP METHOD REPORT** to copy the report to iOS clipboard.

## Why the four hooks are not marked done
The supplied legacy `zsmvn_no_login_key.dylib` contains local ARM64 functions for:
- `ZSM_GetRealVehiclePhysicsCaps` / `ZSM_ApplyForwardVelocityFinishAssist` (vehicle speed)
- `ZSM_ApplyVehicleAndSkinMod` / `ZSM_ApplyUnlockAllGarageVehiclesAndSkins` (vehicle/skin)
- `ZSM_UpdateGroup5ShadowReplayBot` (replay bot)
- `ZSM_UpdateAutoMatchAndRoom` (automatic matchmaking)

These functions depend on a private `ZSMRuntimeChain`, multiple helper functions, IL2CPP method signatures and game-specific object layouts. They are not relocatable standalone object files. Copying function names, addresses or only individual instruction ranges will not recreate their behavior.

**This build does not claim to port those four hooks**. Their runtime metadata must be resolved in the correct game version and the handlers must be reimplemented and verified on device.

## How to gather the remaining information

1. Sign/install the one-file dylib into an authorized copy of the target game.
2. Open `Tinh nang game` in ImGui after Unity/IL2CPP fully starts.
3. Tap `SAO CHEP METHOD REPORT` and paste the text into a file/chat. This copies method/field information; it does not change game memory.
4. Compare the methods with the legacy ARM64 disassembly and write exact typed adapters; then validate each hook on the matching game build.

Do **not** use a method offset, object field, or calling convention inferred from a different game version.

## Build

`.github/workflows/build-eri-standalone.yml` produces `ERIStandalone.dylib` for ARM64 iOS and checks it has no `zsmvn` or `ERIImGuiOverlay` dependency. Mach-O build success does not prove a hook works in the game.
