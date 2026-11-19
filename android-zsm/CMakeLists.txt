cmake_minimum_required(VERSION 3.22)
project(eri_zsm_android LANGUAGES C CXX)

if(NOT ANDROID)
  message(FATAL_ERROR "Use the Android NDK toolchain; this project produces Android .so only")
endif()

if(NOT DEFINED IMGUI_DIR)
  message(FATAL_ERROR "Pass -DIMGUI_DIR=/path/to/ocornut/imgui")
endif()
if(NOT EXISTS "${IMGUI_DIR}/imgui.cpp")
  message(FATAL_ERROR "Dear ImGui source not found: ${IMGUI_DIR}")
endif()

add_library(eri_zsm_android SHARED
  src/eri_android.cpp
  ${IMGUI_DIR}/imgui.cpp
  ${IMGUI_DIR}/imgui_draw.cpp
  ${IMGUI_DIR}/imgui_widgets.cpp
  ${IMGUI_DIR}/imgui_tables.cpp
  ${IMGUI_DIR}/backends/imgui_impl_opengl3.cpp
)

target_compile_features(eri_zsm_android PRIVATE cxx_std_17)
target_include_directories(eri_zsm_android PRIVATE
  "${CMAKE_CURRENT_SOURCE_DIR}/include"
  "${IMGUI_DIR}"
  "${IMGUI_DIR}/backends"
)
target_compile_definitions(eri_zsm_android PRIVATE
  IMGUI_IMPL_OPENGL_ES3
)
target_compile_options(eri_zsm_android PRIVATE
  -fvisibility=hidden -fvisibility-inlines-hidden -Wall -Wextra
)
target_link_libraries(eri_zsm_android PRIVATE GLESv3 EGL android log dl)
set_target_properties(eri_zsm_android PROPERTIES OUTPUT_NAME "eri_zsm_android")
