# ERI ZSM + ImGui — crash-safe startup test (iOS)

This patch targets **game crashes immediately on launch** after signing an IPA with both `ERI.dylib` and `ERIImGuiOverlay.dylib`.

## Findings

- The failing beta file `ERI.dylib` was modified to include a **hard `LC_LOAD_DYLIB` dependency** on `@executable_path/Frameworks/ERIImGuiOverlay.dylib`.
- The user signed an IPA with **both** files injected independently. The forced dependency may cause early load failure; installation success doesn't prove dyld loading succeeds.
- The old overlay creates UIKit/Metal UI and calls `AVGamesMenu.shared` shortly after loading, which can be premature during the game's splash screen.
- **No iOS crash report has been supplied.** These are risk factors, not a proven root cause.

## Changes in crash-guard v3

1. **Do not add a hard overlay dependency to the original hook dylib.** Use the retargeted `ERI.dylib` from the exact `zsmvn_no_login_key(1).dylib` source; change only its `LC_ID_DYLIB`.
2. Build `ERIImGuiOverlay.dylib` with `fix_startup_guard.py`: register delayed UI start after app becomes active. Do not create `UIWindow`, Metal context, or `AVGamesMenu.shared` immediately.
3. Show the floating **ERI** button first; loading the original tab/row list is deferred until the button is tapped.
4. Preserve the 7-color animated native Vietnamese title `Mod By Eri Nguyễn`.

## Installation / controlled test

1. Start from a **clean, unmodified IPA**. Back it up. Never inject over an IPA already modified by a prior beta.
2. Ensure `Payload/zsmvn.app/Frameworks` contains the two libraries:
   - `ERI.dylib`: hooks; **no overlay link dependency**.
   - `ERIImGuiOverlay.dylib`: Metal UI; can load original `ERI.dylib` when needed.
3. Inject each library **once** and re-sign the app, executable and both dylibs with the same valid signing identity. Avoid mixing beta/old filenames (`zsmvn.dylib`, `ERIImGuiOverlay.dylib`, `ERI.dylib`) in the same IPA.
4. Launch and test whether the game reaches the home screen **before tapping ERI**. The menu should initialize after the app is active, initially closed.
5. Tap ERI and inspect `Menu bridge:` status. If the game crashes **before ERI appears**, disable only `ERIImGuiOverlay.dylib` to distinguish a baseline hook incompatibility from UI startup.

## If it still crashes

On the iPhone, open **Settings → Privacy & Security → Analytics & Improvements → Analytics Data**, find the most recent `zsmvn` or app crash `.ips` entry, and share the first crash details containing `Exception Type`, `Termination Reason`, `Triggered by Thread`, and the beginning of the crashed thread backtrace. Redact account/device secrets.

A Mach-O ARM64 build and successful IPA signing do **not** prove runtime compatibility. This is still a two-dylib solution, not a fully merged monolithic dylib. 
