#pragma once
#include <stddef.h>
#include <stdint.h>
#ifdef __cplusplus
extern "C" {
#endif
#if defined(__GNUC__)
# define ERI_IL2CPP_EXPORT __attribute__((visibility("default")))
#else
# define ERI_IL2CPP_EXPORT
#endif
// Resolve MethodInfo by managed image/class/method and full parameter types.
// NULL assembly searches all images; use exact class namespace ("" for global).
// Ambiguous overloads are rejected. Pointers are valid only in current process.
ERI_IL2CPP_EXPORT const void* eri_il2cpp_resolve(
    const char* assembly, const char* name_space, const char* class_name,
    const char* method_name, int param_count, const char* parameter_type_names);
// Calls verified MethodInfo via il2cpp_runtime_invoke, NOT an inline code hook.
// Requires live managed_instance for instance methods and same game process.
ERI_IL2CPP_EXPORT int eri_il2cpp_invoke(
    const void* method, void* managed_instance, void** args, void** boxed_result);
// Read-only mapping of four ZSM candidate groups: bit 1 vehicle, bit 2 garage,
// bit 4 replay, bit 8 matchmaking; FOUND does not mean hook is active.
ERI_IL2CPP_EXPORT uint32_t eri_zsm_binding_mask(void);
ERI_IL2CPP_EXPORT const char* eri_zsm_binding_report(void);
ERI_IL2CPP_EXPORT const char* eri_il2cpp_last_error(void);
#ifdef __cplusplus
}
#endif
