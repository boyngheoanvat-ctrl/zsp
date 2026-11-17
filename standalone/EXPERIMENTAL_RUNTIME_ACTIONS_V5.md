# ERIStandalone V5 — Typed runtime actions (experimental)

Build result: [Actions #44089306664](https://github.com/boyngheoanvat-ctrl/zsp/actions/runs/44089306664) — succeeded; the single ARM64 `ERIStandalone.dylib` was compiled without `zsmvn.dylib` or `ERIImGuiOverlay.dylib` dependencies.

## Grounding
The user supplied the actual ZingSpeed IL2CPP typed report: 84 assemblies, 64,148 classes. Relevant signatures are:
- `VehicleMgr.GetMyVehicle() : IVehicle`
- `GarageMgr.SetGarageListShowVehicleID(UInt32) : void`
- `QuickMatchMdl.get_IsMatching() : Boolean`
- `QuickMatchMdl.QuickMatch(ENMClientGameModeType, Boolean, Byte, Int32, MatchUseMapType) : void`
- `MatchVideoMdl.ChallengeShadow(Int64, UInt64) : MatchVideoErrCode`
- `MatchVideoMdl.<WaitingVideoMark>k__BackingField : Int64`, `<WaitingShadowUID>k__BackingField : UInt64`

## Experimental controls in ImGui
`standalone/Sources/ERIRuntimeActions.mm` resolves IL2CPP public runtime APIs dynamically, searches for **strongly typed static references** to a live manager instance, validates method parameter types and calls through `il2cpp_runtime_invoke` with managed-exception checks. It never calls fixed ARM64 addresses or copies `zsmvn` executable code.

| UI | Actual behavior | Limitations |
| --- | --- | --- |
| KIEM TRA XE DANG SU DUNG | Invoke `VehicleMgr.GetMyVehicle()` | **Read-only**, does not increase vehicle speed |
| HIEN THI XE TRONG GARAGE | Invoke `GarageMgr.SetGarageListShowVehicleID(id)` | Garage *preview only*, does not unlock car/skin |
| YEU CAU QUICK MATCH | Read `get_IsMatching()`, if false invoke `QuickMatch` once using entered mode, player count, map ID, map selector | **One-shot request**, not old auto-match loop; mode/enum validity and server acceptance unverified |
| THU THACH SHADOW DANG CHO | Read managed waiting video mark and shadow UID; if valid invoke `ChallengeShadow(Int64, UInt64)` | **Challenge attempt**, not the legacy autonomous replay bot |

### Important boundaries
- The live-instance lookup intentionally accepts only strongly typed IL2CPP static fields. If the game stores manager instances through a generic object graph, this will **fail safely and report no manager found** instead of guessing pointers.
- No automatic unverified menu actions on start. All four actions require manual button presses. The IL2CPP runtime calls are not tested on-device, may fail due to game lifecycle or network state, and must not be treated as fully functional.
- No native speed physics hook and no unlock skin hook is implemented. `Unity Time.timeScale` from V2 remains a distinct world-time experiment.
- Original ZSM hooks rely on proprietary `ZSMRuntimeChain`, the exact versioned vehicle physics and replay/match state systems. These remain unported.

## Validation plan
Check that the game reaches a stable lobby before using a control. Try **KIEM TRA XE DANG SU DUNG** first, report the visible `V5:` status. If it says manager not found, do not force calls to guesses; a reliable manager locator is required. Test preview and matching only on an authorized test account, not during ranked play. Retain a backup before injecting, re-sign the IPA legitimately, and collect crash logs for failures.
