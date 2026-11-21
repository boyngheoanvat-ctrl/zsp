#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
NDK="${ANDROID_NDK_HOME:-${ANDROID_NDK_ROOT:-}}"
ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "$NDK" || -z "$ANDROID_HOME" ]]; then echo 'ANDROID_NDK_HOME / ANDROID_HOME required' >&2; exit 2; fi
BT="$(find "$ANDROID_HOME/build-tools" -maxdepth 1 -mindepth 1 -type d | sort -V | tail -1)"
JAR="$(find "$ANDROID_HOME/platforms" -name android.jar | sort -V | tail -1)"
HOST="$(find "$NDK/toolchains/llvm/prebuilt" -mindepth 1 -maxdepth 1 -type d | head -1)"
CLANG="$HOST/bin/aarch64-linux-android26-clang++"
mkdir -p "$ROOT/build/classes" "$ROOT/build/dex" "$ROOT/dist"
javac -source 8 -target 8 -encoding UTF-8 -cp "$JAR" -d "$ROOT/build/classes" "$ROOT/java/com/eri/zsm/EriBridge.java"
mapfile -d '' CLASSFILES < <(find "$ROOT/build/classes" -name '*.class' -print0)
"$BT/d8" --min-api 26 --lib "$JAR" --output "$ROOT/build/dex" "${CLASSFILES[@]}"
python3 - "$ROOT/build/dex/classes.dex" "$ROOT/build/eri_ui_dex.h" <<'PY'
from pathlib import Path
import sys
b=Path(sys.argv[1]).read_bytes()
with open(sys.argv[2],"w") as out:
 out.write("static const unsigned char g_eri_dex[] = {\n")
 for i in range(0,len(b),20):
  out.write("  "+",".join("0x%02x"%x for x in b[i:i+20])+",\n")
 out.write("};\nstatic const unsigned int g_eri_dex_size = sizeof(g_eri_dex);\n")
print("Embedded UI DEX:",len(b))
PY
"$CLANG" -std=c++17 -O2 -fPIC -fvisibility=hidden -DANDROID -D__ANDROID_API__=26 \
 -I"$ROOT/build" -shared "$ROOT/src/eri.cpp" -o "$ROOT/dist/libERI.so" \
 -Wl,-soname,libERI.so -Wl,--no-undefined -Wl,--gc-sections \
 -static-libstdc++ -llog -ldl -latomic
file "$ROOT/dist/libERI.so"
readelf -h "$ROOT/dist/libERI.so" | grep -E 'Class:|Machine:|Type:'
sha256sum "$ROOT/dist/libERI.so"
