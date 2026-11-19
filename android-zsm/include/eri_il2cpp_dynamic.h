#pragma once
#include <stddef.h>
#include <stdint.h>
#ifdef __cplusplus
extern "C" {
#endif
#if defined(__GNUC__)
#define ERI_IL2CPP_EXPORT __attribute__((visibility("default")))
#else
#define ERI_IL2CPP_EXPORT
#endif

// Resolve IL2CPP MethodInfo from the currently loaded runtime, using complete
// assembly/namespace/class/name/parameter signature. Never a fixed RVA.
// Returned pointers are process-specific and cannot be used across app runs.
ERI_IL2CPP_EXPORT const void* eri_il2cpp_resolve(
  const char* assembly, const char* name_space, const char* class_name,
  const char* method_name, int param_count, const char* parameter_type_names);

// Caller must be IN-PROCESS and provide a verified live managed object for
// instance methods; no hook installation or object discovery takes place.
ERI_IL2CPP_EXPORT int eri_il2cpp_invoke(
  const void* method, void* managed_instance, void** args, void** boxed_result);

// Read-only probe for 4 candidate method groups from the iOS IL2CPP report.
// Bit 1 vehicle, bit 2 garage, bit 4 replay, bit 8 matchmaking.
// A FOUND signature does NOT mean the corresponding game hook is installed.
ERI_IL2CPP_EXPORT uint32_t eri_zsm_binding_mask(void);
ERI_IL2CPP_EXPORT const char* eri_zsm_binding_report(void);
ERI_IL2CPP_EXPORT const char* eri_il2cpp_last_error(void);
#ifdef __cplusplus
}
#endif
