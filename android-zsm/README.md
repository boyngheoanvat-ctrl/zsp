# ERI ZSM Android — native single-`.so` port (NDK)

This is an **Android ELF build**, not a renamed iOS `.dylib`. It uses Dear ImGui with OpenGL ES 3.0 and exports C functions from one native library named `liberi_zsm_android.so`.

## What works in the Android source

- Floating ERI toggle, ImGui menu, animated seven-color `Mod By Eri Nguyễn` text.
- Android-specific OpenGL ES 3 backend, native touch events.
- Tabs for speed, vehicle/skin, replay, matchmaking and diagnostics. Features that have not been ported are **explicitly labeled unavailable**, not fake switches.
- Read-only `libil2cpp.so` class discovery for `VehicleMgr`, `GarageMgr`, `MatchMgr`, `MatchVideoMdl`, `QuickMatchMdl` after the game loads.
- Compiles for `arm64-v8a` (primary) and `armeabi-v7a`. Use only the `.so` matching the target APK's ABI.

## Important limitations

- **Vehicle speed, game skins, replay bot and auto-match hooks from iOS are NOT included.** `zsmvn_no_login_key.dylib` is a compiled Mach-O linked to iOS runtime APIs, whereas the Android game uses a distinct ELF/Unity/IL2CPP build. Copying the bytes or renaming the extension is unsafe.
- Loading `liberi_zsm_android.so` does **not** automatically show an overlay. The host application must integrate the exported rendering functions on a thread with a current **OpenGL ES 3 EGL context**, supply frame dimensions, time delta, and forward touch coordinates. Do not call OpenGL code in a static initializer. No EGL function hooking is performed by this library.
- The diagnostic scanner may report no classes if the game uses different Android names, IL2CPP exports are hidden, or scanning happens before initialization. A discovered class does not activate any game hook.
- Some Android games use Vulkan rather than GLES3, in which case this renderer is not compatible without a Vulkan backend.
- Injecting a native library requires properly packaging it in the application and valid signing. Native libraries should be integrated only in apps you're authorized to modify. No signed APK is supplied.

## Exported C API

```cpp
#include "eri_android.h"

// A valid GLES3 EGL context must already be current on this thread.
eri_init(width, height);

// Once per frame on that same render thread:
eri_render_frame(width, height, delta_seconds);

// Forward touch input:
eri_touch(x, y, pressed ? 1 : 0);

// Before destroying the EGL context:
eri_shutdown();

// Optional read-only inspection:
int targetCount = eri_scan_il2cpp(); // -1 if unavailable
const char* status = eri_runtime_status();
```

Use `eri_android.h` for exact declarations. Do not call `eri_init` or `eri_render_frame` in a JNI constructor or from the main UI thread without an EGL context.

## Build locally with Android NDK

```bash
python3 android-zsm/tools/decode_source.py
git clone --depth 1 --branch v1.92.2 https://github.com/ocornut/imgui.git .deps/imgui
cmake -S android-zsm -B .build/arm64-v8a -G Ninja \
  -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-23 \
  -DIMGUI_DIR="$PWD/.deps/imgui" -DCMAKE_BUILD_TYPE=Release
cmake --build .build/arm64-v8a
```

CI: `.github/workflows/build-eri-zsm-android.yml`.

## Next requirements to port the original gameplay functions

An Android APK built for the **same ZingSpeed Mobile version**, its `libil2cpp.so`, Unity native `.so` dependencies and IL2CPP metadata are needed to map real Android methods. There is no reliable mapping from iOS Mach-O offsets to Android ELF function addresses. Do not assume the earlier iOS class layout is valid on Android.
