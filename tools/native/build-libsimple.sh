#!/usr/bin/env bash
# 重新构建 armeabi-v7a 的 libsimple.so（SQLite FTS5 中文分词扩展）。
#
# 仓库里 arm64-v8a / x86_64 的 libsimple.so 来自 wangfenjin/simple v0.7.1 的官方 Release；
# 该 Release 不提供 armeabi-v7a，所以 v7a 由本脚本从同一版本源码交叉编译（build without jieba，
# 与仓库里 arm64 产物一致：不依赖运行时词典文件，拼音表通过 cmrc 内嵌）。
#
# 依赖：Android NDK（默认 $ANDROID_NDK_HOME 或 D:/Android_Studio/ndk/<latest>）、
#       CMake（$ANDROID_CMAKE 或 SDK 的 cmake/3.22.1/bin/cmake.exe）、curl、tar、python。
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SIMPLE_VERSION="${SIMPLE_VERSION:-v0.7.1}"
WORK="${WORK:-$ROOT/build/native}"
OUT_DIR="$ROOT/app/src/wear/jniLibs/armeabi-v7a"

find_tool() {
  local name="$1" candidate
  candidate="${!2:-}"
  if [[ -n "$candidate" && -x "$candidate" ]]; then echo "$candidate"; return; fi
  if command -v "$name" >/dev/null 2>&1; then command -v "$name"; return; fi
  echo "missing $name (set $2)" >&2
  exit 1
}

CMAKE="${ANDROID_CMAKE:-}"
if [[ -z "$CMAKE" ]]; then
  for c in "$ROOT/../../Android_Studio/cmake/3.22.1/bin/cmake.exe" \
           "D:/Android_Studio/cmake/3.22.1/bin/cmake.exe"; do
    [[ -x "$c" ]] && CMAKE="$c" && break
  done
fi
[[ -n "$CMAKE" ]] || { echo "cmake not found; set ANDROID_CMAKE" >&2; exit 1; }

NDK="${ANDROID_NDK_HOME:-}"
if [[ -z "$NDK" ]]; then
  NDK="$(ls -d /d/Android_Studio/ndk/* 2>/dev/null | sort -V | tail -1)"
fi
[[ -n "$NDK" && -f "$NDK/build/cmake/android.toolchain.cmake" ]] || {
  echo "Android NDK not found; set ANDROID_NDK_HOME" >&2; exit 1;
}

mkdir -p "$WORK"
SRC="$WORK/simple-$SIMPLE_VERSION"
if [[ ! -d "$SRC/src" ]]; then
  TARBALL="$WORK/simple-$SIMPLE_VERSION.tar.gz"
  if [[ ! -f "$TARBALL" ]]; then
    echo "downloading wangfenjin/simple $SIMPLE_VERSION"
    curl -fL --retry 3 -m 600 -o "$TARBALL" \
      "https://gh-proxy.com/https://github.com/wangfenjin/simple/archive/refs/tags/$SIMPLE_VERSION.tar.gz"
  fi
  mkdir -p "$WORK/simple-extract"
  tar xzf "$TARBALL" -C "$WORK/simple-extract"
  mv "$WORK/simple-extract/simple-${SIMPLE_VERSION#v}" "$SRC"
fi

BUILD="$WORK/simple-build-armeabi-v7a"
rm -rf "$BUILD"
"$CMAKE" -S "$SRC" -B "$BUILD" -G Ninja \
  -DCMAKE_MAKE_PROGRAM="$(dirname "$CMAKE")/ninja.exe" \
  -DCMAKE_TOOLCHAIN_FILE="$NDK/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI=armeabi-v7a \
  -DANDROID_PLATFORM=android-26 \
  -DCMAKE_BUILD_TYPE=Release \
  -DSIMPLE_WITH_JIEBA=OFF \
  -DBUILD_TEST_EXAMPLE=OFF \
  -DBUILD_SHELL=OFF \
  -DBUILD_SQLITE3=OFF

"$CMAKE" --build "$BUILD" --config Release --parallel

mkdir -p "$OUT_DIR"
cp "$BUILD/src/libsimple.so" "$OUT_DIR/libsimple.so"
echo "installed $OUT_DIR/libsimple.so"
