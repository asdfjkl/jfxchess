#!/usr/bin/env bash
# JFXChess - A Chess Graphical User Interface
# Copyright (C) 2020-2026 Dominik Klein
#
# Runs inside the Ubuntu 22.04 Docker builder to compile JFXChess, generate a
# trimmed Temurin 21 jpackage app-image, bundle safe X11 extension libraries,
# audit ELF dependencies, and pack the final AppImage.

set -euo pipefail

readonly WORK_ROOT="/tmp/jfxchess-work"
readonly SRC_DIR="${WORK_ROOT}/src"
readonly JRE_DIR="${WORK_ROOT}/runtime"
readonly INPUT_DIR="${WORK_ROOT}/input"
readonly JPKG_OUT_DIR="${WORK_ROOT}/jpkg-out"
readonly APPDIR="${WORK_ROOT}/JFXChess.AppDir"
readonly AUDIT_EXTRACT_DIR="${WORK_ROOT}/audit-extract"

readonly JLINK_MODULES="java.base,java.datatransfer,java.desktop,java.logging,java.net.http,java.prefs,java.xml,jdk.charsets,jdk.crypto.ec"
readonly MAX_GLIBC_VERSION="2.35"
readonly X11_EXT_LIBS=(
    "libXext.so.6"
    "libXrender.so.1"
    "libXtst.so.6"
    "libXi.so.6"
)
# Only these universal host libraries (AppImage excludelist) may remain external.
readonly ALLOWED_HOST_LIBS=(
    "ld-linux-x86-64.so.2"
    "libc.so.6"
    "libm.so.6"
    "libdl.so.2"
    "libpthread.so.0"
    "librt.so.1"
    "libgcc_s.so.1"
    "libstdc++.so.6"
    "libX11.so.6"
    "libasound.so.2"
)

# Copy read-only repo inputs into container workspace so host tree stays clean.
prepare_workspace() {
    rm -rf "${WORK_ROOT}"
    mkdir -p "${SRC_DIR}" "${INPUT_DIR}/book" "${INPUT_DIR}/engine" "${JPKG_OUT_DIR}" "${APPDIR}/usr"

    cp /workspace/pom.xml "${SRC_DIR}/pom.xml"
    cp -a /workspace/src "${SRC_DIR}/src"
}

# Read project version from pom.xml without external XML tools.
read_app_version() {
    awk '/<artifactId>jfxchess<\/artifactId>/ { found=1; next }
         found && /<version>/ {
             gsub(/.*<version>|<\/version>.*/, "");
             print;
             exit
         }' "${SRC_DIR}/pom.xml"
}

# Build fat JAR with Maven using Temurin 21.
build_fat_jar() {
    (
        cd "${SRC_DIR}"
        mvn -B clean package
    )
}

# Create a minimal Temurin 21 JRE containing only modules JFXChess needs.
build_trimmed_jre() {
    jlink \
        --add-modules "${JLINK_MODULES}" \
        --strip-debug \
        --no-man-pages \
        --no-header-files \
        --compress=zip-6 \
        --output "${JRE_DIR}"
}

# Assemble jpackage input directory and create the raw app-image.
build_jpackage_image() {
    local app_version="$1"

    cp "${SRC_DIR}/target/jfxchess-jar-with-dependencies.jar" "${INPUT_DIR}/"
    cp /workspace/build/build_app_image/assets/splash.png "${INPUT_DIR}/splash.png"
    cp /workspace/build/build_app_image/assets/splash@2x.png "${INPUT_DIR}/splash@2x.png"
    cp /workspace/build/build_app_image/assets/book/extbook.bin "${INPUT_DIR}/book/extbook.bin"
    cp /workspace/build/build_app_image/assets/engine/stockfish_x64 "${INPUT_DIR}/engine/stockfish_x64"
    cp /workspace/build/build_app_image/assets/engine/stockfish5_x64 "${INPUT_DIR}/engine/stockfish5_x64"
    chmod +x "${INPUT_DIR}/engine/stockfish_x64" "${INPUT_DIR}/engine/stockfish5_x64"

    jpackage \
        --type app-image \
        --name jfxchess \
        --app-version "${app_version}" \
        --input "${INPUT_DIR}" \
        --main-jar jfxchess-jar-with-dependencies.jar \
        --main-class org.asdfjkl.jfxchess.gui.App \
        --runtime-image "${JRE_DIR}" \
        --icon /workspace/jfxchess_icon_256.png \
        --java-options '-splash:$APPDIR/splash.png' \
        --dest "${JPKG_OUT_DIR}"
}

# Populate AppDir and bundle X11 extension libs inside runtime/lib ($ORIGIN RPATH).
assemble_appdir() {
    cp -a "${JPKG_OUT_DIR}/jfxchess/bin" "${APPDIR}/usr/bin"
    cp -a "${JPKG_OUT_DIR}/jfxchess/lib" "${APPDIR}/usr/lib"

    cp /workspace/build/build_app_image/AppRun "${APPDIR}/AppRun"
    chmod +x "${APPDIR}/AppRun"

    cp /workspace/build/build_app_image/jfxchess.desktop "${APPDIR}/jfxchess.desktop"
    cp /workspace/jfxchess_icon_256.png "${APPDIR}/jfxchess.png"
    ln -sf jfxchess.png "${APPDIR}/.DirIcon"

    desktop-file-validate "${APPDIR}/jfxchess.desktop"

    # Bundle X11 extension libs into runtime/lib so libawt_xawt.so finds them via
    # $ORIGIN without exporting LD_LIBRARY_PATH to external UCI chess engines.
    local runtime_lib_dir="${APPDIR}/usr/lib/runtime/lib"
    local lib_name
    for lib_name in "${X11_EXT_LIBS[@]}"; do
        local src_lib="/usr/lib/x86_64-linux-gnu/${lib_name}"
        if [ ! -f "${src_lib}" ]; then
            echo "Error: Missing X11 extension library: ${src_lib}" >&2
            exit 1
        fi
        cp -L "${src_lib}" "${runtime_lib_dir}/${lib_name}"
    done
}

# Pack AppDir into an AppImage using static FUSE type2-runtime.
pack_appimage() {
    local output_path="$1"

    mkdir -p "$(dirname "${output_path}")"
    rm -f "${output_path}"

    ARCH=x86_64 /opt/appimagetool/AppRun \
        --no-appstream \
        --runtime-file /opt/appimage-runtime/runtime-x86_64 \
        "${APPDIR}" \
        "${output_path}"

    chmod +x "${output_path}"
}

# Extract the packed AppImage and verify strict host-lib allowlist and glibc <= 2.35.
audit_appimage() {
    local output_path="$1"
    local failed=0

    echo "=== Auditing packed AppImage: $(basename "${output_path}") ==="

    rm -rf "${AUDIT_EXTRACT_DIR}"
    mkdir -p "${AUDIT_EXTRACT_DIR}"
    (
        cd "${AUDIT_EXTRACT_DIR}"
        "${output_path}" --appimage-extract >/dev/null
    )

    local extract_root="${AUDIT_EXTRACT_DIR}/squashfs-root"
    local max_glibc="0.0"

    # Collect all shared library basenames bundled inside the AppImage.
    local bundled_libs
    bundled_libs="$(find "${extract_root}/usr/lib" -type f -name '*.so*' -printf '%f\n' | sort -u)"

    local allowed_host_libs
    allowed_host_libs="$(printf '%s\n' "${ALLOWED_HOST_LIBS[@]}")"

    while IFS= read -r -d '' elf_file; do
        if ! file -b "${elf_file}" | grep -q 'ELF '; then
            continue
        fi

        local needed_lib
        while IFS= read -r needed_lib; do
            [ -z "${needed_lib}" ] && continue

            if echo "${bundled_libs}" | grep -qxF "${needed_lib}"; then
                continue
            fi

            if echo "${allowed_host_libs}" | grep -qxF "${needed_lib}"; then
                continue
            fi

            echo "FAIL: Disallowed external dependency '${needed_lib}' in ${elf_file}" >&2
            failed=1
        done < <(readelf -d "${elf_file}" 2>/dev/null | awk '/\(NEEDED\)/ { gsub(/\[|\]/, "", $5); print $5 }')

        local bin_max_glibc
        bin_max_glibc="$(readelf -V "${elf_file}" 2>/dev/null \
            | grep -oE 'GLIBC_[0-9]+\.[0-9]+(\.[0-9]+)?' \
            | sed 's/^GLIBC_//' \
            | sort -V \
            | tail -n 1 || true)"

        if [ -n "${bin_max_glibc}" ]; then
            if [ "$(printf '%s\n%s\n' "${MAX_GLIBC_VERSION}" "${bin_max_glibc}" | sort -V | tail -n 1)" != "${MAX_GLIBC_VERSION}" ]; then
                echo "FAIL: ${elf_file} requires GLIBC_${bin_max_glibc} (> ${MAX_GLIBC_VERSION})" >&2
                failed=1
            fi
            if [ "$(printf '%s\n%s\n' "${max_glibc}" "${bin_max_glibc}" | sort -V | tail -n 1)" = "${bin_max_glibc}" ]; then
                max_glibc="${bin_max_glibc}"
            fi
        fi
    done < <(find "${extract_root}" -type f \( -name '*.so*' -o -perm -111 \) -print0)

    if [ "${failed}" -ne 0 ]; then
        echo "ELF audit failed." >&2
        exit 1
    fi

    echo "ELF audit passed: max GLIBC=${max_glibc} (limit ${MAX_GLIBC_VERSION}), all external deps in host allowlist."
}

main() {
    prepare_workspace

    local app_version
    app_version="$(read_app_version)"
    if [ -z "${app_version}" ]; then
        echo "Error: Could not read version from pom.xml" >&2
        exit 1
    fi

    local output_path="/out/JFXChess-${app_version}-x86_64.AppImage"

    build_fat_jar
    build_trimmed_jre
    build_jpackage_image "${app_version}"
    assemble_appdir
    pack_appimage "${output_path}"
    audit_appimage "${output_path}"

    # Match output file ownership to the host user invoking build_app_image.sh.
    if [ -n "${HOST_UID:-}" ] && [ -n "${HOST_GID:-}" ]; then
        chown "${HOST_UID}:${HOST_GID}" "${output_path}"
    fi

    ls -lh "${output_path}"
}

main "$@"
