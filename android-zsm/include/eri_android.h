#pragma once

// ERI ZSM Android (ELF, NDK) - one native library.
// Call from the app's own GLES3 render thread after a valid EGL context exists.
// Drawing is NOT injected automatically into another app.
#ifdef __cplusplus
extern "C" {
#endif

#if defined(__GNUC__)
#define ERI_EXPORT __attribute__((visibility("default")))
#else
#define ERI_EXPORT
#endif

ERI_EXPORT int eri_init(int width, int height);
ERI_EXPORT int eri_render_frame(int width, int height, float delta_seconds);
ERI_EXPORT void eri_touch(float x, float y, int pressed);
ERI_EXPORT void eri_shutdown(void);
ERI_EXPORT const char* eri_version(void);
ERI_EXPORT const char* eri_runtime_status(void);
// Returns number of target manager classes found, or -1 if IL2CPP isn't ready.
// Read-only class name discovery; no game hooks or memory patches.
ERI_EXPORT int eri_scan_il2cpp(void);

#ifdef __cplusplus
}
#endif
