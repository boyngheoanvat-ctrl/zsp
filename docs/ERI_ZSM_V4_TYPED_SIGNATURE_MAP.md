# ERI ZSM — typed-hook migration V4

This report is based on the **actual method survey pasted from the target game**. No gameplay hook is claimed to work in V4.

## Confirmed class and candidate methods

| Feature | IL2CPP class | Evidence from report | What's still unknown |
|---|---|---|---|
| Vehicle speed | `VehicleMgr` | `GetMyVehicle() argc=0`, `GetRaceCarParams() argc=3`, `RefreshVehicleParam() argc=6` | Parameter/return types; vehicle physics owner and correct update event |
| Garage/skin | `GarageMgr` | `SetGarageShowBySkinData() argc=3`, `OnChangeVehicle() argc=2`, `SetGarageListShowVehicleID() argc=1` | Which methods merely preview versus authoritatively change equipped items |
| Replay bot | `MatchVideoMdl` | `ReplayVideo() argc=2,4`, `ChallengeShadow() argc=1,2,4`, `StartEnterGame() argc=3` | Managed object instance; overload parameter types, network-side authorization |
| Auto-match | `QuickMatchMdl` | `get_IsMatching() argc=0`, `QuickMatch() argc=5`, `CancelMatch() argc=0` | Type of five parameters; valid game mode, state and event timing |
| Match state | `MatchMgr` | `get_CurMatch() argc=0`, `GetInGameState() argc=0` | Instance and permitted state transitions |

The number shown in parentheses is **argc**, not the full C++/managed calling signature. For fields, `off=0` is often static metadata and should not be treated as a safe instance pointer offset.

## Original `zsmvn_no_login_key.dylib` symbol prototypes

The following were inspected directly in Mach-O's symbol table and demangled (the source was a compiled `dylib`, not an object library):

- `ZSM_ApplyVehicleAndSkinMod(bool, char const*)`
- `ZSM_GetRealVehiclePhysicsCaps(ZSMRuntimeChain const&, float&, float&, float&, float&)`
- `ZSM_UpdateGroup5ShadowReplayBot(ZSMRuntimeChain&)`
- `ZSM_UpdateAutoMatchAndRoom(ZSMRuntimeChain const&, bool)`

Those functions are local to the legacy library and depend on `ZSMRuntimeChain`, other helper functions and private state. Do not reinterpret their *library-local addresses* as game method offsets.

## V4 changes

`ERITypedTargetReport.mm` dynamically resolves read-only IL2CPP metadata exports:
`il2cpp_method_get_param`, `il2cpp_method_get_return_type`, `il2cpp_type_get_name`, `il2cpp_method_get_flags`, and the field counterparts. The new ImGui button **SAO CHEP HOOK TYPE REPORT** copies exact method signatures of candidate methods.

Only once typed signatures are known can a safe native wrapper be designed. Further validation needs method ownership and object lifecycle in the current ZSM build. V4 does **not** call `QuickMatch`, `ReplayVideo` or modify vehicle/skin fields. It is still one standalone dylib with a diagnostic component.
