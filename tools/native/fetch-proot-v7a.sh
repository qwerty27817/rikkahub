#!/usr/bin/env bash
# 准备 armeabi-v7a 的 PRoot 运行环境。
#
# 仓库只带 arm64-v8a / x86_64 的 libproot_exec.so、libproot_loader.so（termux/proot 5.1.107.96，
# 自包含）。armeabi-v7a 使用 Termux 官方发行版：
#   - proot_5.1.107.102_arm.deb      → libproot_exec.so / libproot_loader.so
#   - libtalloc_2.5.0_arm.deb        → libtalloc.so（SONAME libtalloc.so.2）
#   - libandroid-shmem（termux/libandroid-shmem 源码）→ libandroid_shmem.so
# 后两个是 Termux 官方 arm proot 的动态依赖；运行时由 ProotShellRunner 通过 LD_PRELOAD /
# LD_LIBRARY_PATH 解析（Android 只把 lib*.so 放进 nativeLibraryDir，而 DT_NEEDED 是
# libtalloc.so.2 / libandroid-shmem.so）。
#
# 另建议同时准备 x86_64/i686 版本用于模拟器验证：本脚本默认只处理 arm。
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
WORK="${WORK:-$ROOT/build/native}"
OUT_DIR="$ROOT/workspace/src/main/jniLibs/armeabi-v7a"
TERMUX_POOL="${TERMUX_POOL:-https://packages-cf.termux.dev/apt/termux-main/pool/main}"
NDK="${ANDROID_NDK_HOME:-}"
if [[ -z "$NDK" ]]; then
  NDK="$(ls -d /d/Android_Studio/ndk/* 2>/dev/null | sort -V | tail -1)"
fi
[[ -f "$NDK/build/cmake/android.toolchain.cmake" ]] || { echo "Android NDK not found" >&2; exit 1; }
CLANG="$NDK/toolchains/llvm/prebuilt/windows-x86_64/bin/armv7a-linux-androideabi26-clang"
[[ -x "$CLANG" ]] || CLANG="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/armv7a-linux-androideabi26-clang"

mkdir -p "$WORK" "$OUT_DIR"

fetch() {
  local url="$1" file="$2"
  [[ -f "$file" ]] || curl -fL --retry 3 -m 600 -o "$file" "$url"
}

# 从 .deb（ar 归档）里抽取文件；Windows 上不一定有 ar，用 python 解析
extract_deb() {
  local deb="$1" outdir="$2"
  python - "$deb" "$outdir" <<'PY'
import io, lzma, os, sys, tarfile

deb, outdir = sys.argv[1], sys.argv[2]
data = open(deb, "rb").read()
off, members = 8, {}
while off + 60 <= len(data):
    name = data[off:off + 16].decode(errors="ignore").strip().rstrip("/")
    try:
        size = int(data[off + 48:off + 58].decode().strip())
    except ValueError:
        break
    members[name] = data[off + 60:off + 60 + size]
    off += 60 + size + (size % 2)
for name, raw in members.items():
    if not name.startswith("data.tar"):
        continue
    if name.endswith(".xz"):
        raw = lzma.decompress(raw)
    with tarfile.open(fileobj=io.BytesIO(raw)) as tf:
        for member in tf.getmembers():
            if member.isfile():
                target = os.path.join(outdir, os.path.basename(member.name))
                os.makedirs(outdir, exist_ok=True)
                with open(target, "wb") as fh:
                    fh.write(tf.extractfile(member).read())
                print("extracted", target)
PY
}

PROOT_DEB="$WORK/proot_5.1.107.102_arm.deb"
TALLOC_DEB="$WORK/libtalloc_2.5.0_arm.deb"
fetch "$TERMUX_POOL/p/proot/proot_5.1.107.102_arm.deb" "$PROOT_DEB"
fetch "$TERMUX_POOL/libt/libtalloc/libtalloc_2.5.0_arm.deb" "$TALLOC_DEB"

PROOT_DIR="$WORK/proot-arm"
TALLOC_DIR="$WORK/talloc-arm"
extract_deb "$PROOT_DEB" "$PROOT_DIR"
extract_deb "$TALLOC_DEB" "$TALLOC_DIR"

cp "$PROOT_DIR/proot" "$OUT_DIR/libproot_exec.so"
cp "$PROOT_DIR/loader" "$OUT_DIR/libproot_loader.so"
cp "$TALLOC_DIR/libtalloc.so.2.5.0" "$OUT_DIR/libtalloc.so"

# libandroid-shmem：termux 官方源码，编出来带 SONAME libandroid-shmem.so
SHMEM_SRC="$WORK/libandroid-shmem"
if [[ ! -f "$SHMEM_SRC/shmem.c" ]]; then
  mkdir -p "$SHMEM_SRC"
  curl -fL --retry 3 -m 600 -o "$WORK/libandroid-shmem.tar.gz" \
    "https://gh-proxy.com/https://github.com/termux/libandroid-shmem/archive/refs/heads/master.tar.gz"
  tar xzf "$WORK/libandroid-shmem.tar.gz" -C "$WORK"
  mv "$WORK/libandroid-shmem-master" "$SHMEM_SRC"
fi
"$CLANG" -fpic -shared -std=c11 -Wall -Wextra '-D_PATH_TMP="/tmp/"' \
  -Wl,--version-script="$SHMEM_SRC/exports.txt" \
  -Wl,-soname,libandroid-shmem.so \
  "$SHMEM_SRC/shmem.c" -o "$OUT_DIR/libandroid_shmem.so" -llog -landroid

chmod 0755 "$OUT_DIR"/libproot_exec.so "$OUT_DIR"/libproot_loader.so
echo "installed PRoot runtime into $OUT_DIR"
ls -l "$OUT_DIR"
