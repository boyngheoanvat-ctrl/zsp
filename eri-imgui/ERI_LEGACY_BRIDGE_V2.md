# ERI ImGui + legacy ZSM hook integration

**Note:** The requested single merged dylib is **not yet achieved**. This branch builds the actual Dear ImGui/Metal UI as a second dylib and bridges it to the unmodified legacy ARM64 hook code. Do not mislabel the resulting two-library IPA as a single dylib.

## Original hook binary

Source supplied in the conversation: `zsmvn_no_login_key(1).dylib` (ARM64 Mach-O, 7,923,200 bytes).

The metadata was inspected and shows `AVGamesMenu` deriving from `UIViewController`, a `+[AVGamesMenu shared]` class method, and category methods `-[AVGamesMenu(Content) segmentTitles]` and `-[AVGamesMenu(Content) rowsForSegment:]`. These are the concrete bridge selectors used by the overlay.

The legacy dylib contains compiled hook symbols including:

- `ZSM_ApplyVehicleAndSkinMod`
- `ZSM_GetRealVehiclePhysicsCaps`
- `ZSM_UpdateGroup5ShadowReplayBot`
- `ZSM_UpdateAutoMatchAndRoom`

The original hook code is retained: only Mach-O load commands are modified when generating the loader copy. This does **not** imply that those hooks were verified on the target iPhone/game version.

## Integration

1. Build `ERIImGuiOverlay.dylib` from `.github/workflows/build-ios-imgui-rebuild.yml` (Dear ImGui v1.92.2, Metal, arm64).
2. Retarget the user-provided legacy dylib's install name to `@executable_path/Frameworks/ERI.dylib` with `standalone/tools/retarget_legacy_dylib.py`.
3. Add the load dependency on `@executable_path/Frameworks/ERIImGuiOverlay.dylib` with the standalone `eri-imgui/patch_loader.py` source in the original source bundle.
4. Put **both** dylibs in `Payload/App.app/Frameworks`, ensure the original injection entry actually loads `ERI.dylib`, and re-sign every changed component.
5. Test the UI. If tabs don't load, observe the new **Menu bridge:** diagnostic (missing class / missing selector / empty list / no row models) rather than guessing.

## UI changes

- Independent floating ERI toggle, Dark ImGui window, horizontal tab navigation, Metal renderer.
- Header `Mod By Eri Nguyễn` uses UIKit's native Unicode font and **animated seven-color characters**.
- A retrying bridge requests original row factories only once per second until they are available; previous row objects stay retained for callbacks.
- Existing original menu chrome is suppressed; original game hook code is not altered.

## Limitation

A dynamic Mach-O dylib is not a relocatable object archive; linking it into a newly compiled ImGui dylib does not merge its compiled code. A real one-file solution requires a trustworthy original object/source build or a verified native binary reconstruction and signing. The two-dylib bridge is the only buildable integration option currently validated by static checks. Actual on-device behavior is **not verified**.
