#!/usr/bin/env bash
# JFXChess - A Chess Graphical User Interface
# Copyright (C) 2020-2026 Dominik Klein
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# Builds the Debian package into build/build_linux/output/ using a cached
# Eclipse Temurin 21 JDK (leaving the system apt-managed Java untouched).

set -euo pipefail

cd "$(dirname "$0")"

readonly TEMURIN_URL="https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.13.1%2B2/OpenJDK21U-jdk_x64_linux_hotspot_21.0.13.1_2.tar.gz"
readonly TEMURIN_SHA256="bb8a93f9ae6e7a693b2b721e85dbeff26ae1a77447985a8d7afcdd9c7cf6222e"
readonly CACHE_ROOT="${XDG_CACHE_HOME:-${HOME}/.cache}/jfxchess"
readonly DEFAULT_TEMURIN_DIR="${CACHE_ROOT}/temurin-21"
readonly TEMURIN_HOME="${TEMURIN_HOME:-${DEFAULT_TEMURIN_DIR}}"

readonly BUILD_LINUX_DIR="build_linux"
readonly JAR_INPUT_DIR="${BUILD_LINUX_DIR}/build/jar"
readonly RUNTIME_DIR="${BUILD_LINUX_DIR}/runtime"
readonly OUTPUT_DIR="${BUILD_LINUX_DIR}/output"

readonly JLINK_MODULES="java.base,java.datatransfer,java.desktop,java.logging,java.prefs,java.xml,jdk.charsets"
readonly DISALLOWED_DEPS_REGEX='(t64|libjpeg-turbo8|libfreetype6|libharfbuzz|libgif7|liblcms2|libpcsclite1)'

# Download and verify Eclipse Temurin 21 into user cache if not present.
ensure_temurin() {
    if [ -x "${TEMURIN_HOME}/bin/jlink" ] && [ -x "${TEMURIN_HOME}/bin/jpackage" ]; then
        return 0
    fi

    if [ "${TEMURIN_HOME}" != "${DEFAULT_TEMURIN_DIR}" ]; then
        echo "Error: TEMURIN_HOME (${TEMURIN_HOME}) lacks bin/jlink or bin/jpackage." >&2
        exit 1
    fi

    mkdir -p "${CACHE_ROOT}"

    local tmp_tar="${CACHE_ROOT}/temurin-21.tar.gz.tmp"
    local tmp_dir="${CACHE_ROOT}/temurin-21.extract.tmp"
    rm -rf "${tmp_tar}" "${tmp_dir}"

    echo "=== Downloading Eclipse Temurin 21 ==="
    curl -fsSL "${TEMURIN_URL}" -o "${tmp_tar}"
    echo "${TEMURIN_SHA256}  ${tmp_tar}" | sha256sum -c -

    mkdir -p "${tmp_dir}"
    tar -xzf "${tmp_tar}" -C "${tmp_dir}" --strip-components=1
    rm -f "${tmp_tar}"

    rm -rf "${DEFAULT_TEMURIN_DIR}"
    mv "${tmp_dir}" "${DEFAULT_TEMURIN_DIR}"
}

# Remove previous build artifacts while preserving static assets and .gitkeep.
clean_outputs() {
    rm -f "${JAR_INPUT_DIR}"/*.jar
    rm -rf "${RUNTIME_DIR}"
    rm -f "${OUTPUT_DIR}"/*.deb
}

# Build fat JAR using the system Maven/Java toolchain.
build_fat_jar() {
    (
        cd ..
        mvn clean package
    )
    cp ../target/jfxchess-jar-with-dependencies.jar "${JAR_INPUT_DIR}/"
}

# Build a trimmed Temurin 21 JRE and drop unused libjsound.so (avoids libasound2t64).
build_runtime() {
    "${TEMURIN_HOME}/bin/jlink" \
        --add-modules "${JLINK_MODULES}" \
        --strip-debug \
        --no-man-pages \
        --no-header-files \
        --compress=zip-6 \
        --output "${RUNTIME_DIR}"

    rm -f "${RUNTIME_DIR}/lib/libjsound.so"
}

# Assemble the .deb package with Temurin's jpackage and the trimmed runtime.
build_deb_pkg() {
    (
        cd "${BUILD_LINUX_DIR}"
        "${TEMURIN_HOME}/bin/jpackage" \
            --java-options '-splash:$APPDIR/splash.png' \
            --type deb \
            -n jfxchess \
            -i build/jar \
            --main-jar jfxchess-jar-with-dependencies.jar \
            --runtime-image runtime \
            -d output \
            --icon build/linux/app_icon.png \
            --resource-dir build/linux \
            --linux-menu-group games \
            --linux-shortcut \
            --license-file build/LICENSE.TXT \
            --about-url https://github.com/asdfjkl/jfxchess \
            --linux-app-release 26.12 \
            --main-class org.asdfjkl.jfxchess.gui.App
    )
}

# Fail the build if dpkg recorded distro-specific or t64 package dependencies.
audit_deb_deps() {
    local deb_file
    deb_file="$(find "${OUTPUT_DIR}" -maxdepth 1 -name '*.deb' | head -n 1)"

    if [ -z "${deb_file}" ]; then
        echo "Error: No .deb artifact found in ${OUTPUT_DIR}" >&2
        exit 1
    fi

    local deps
    deps="$(dpkg-deb -f "${deb_file}" Depends)"
    echo "=== Package: ${deb_file} ==="
    echo "Depends: ${deps}"

    if echo "${deps}" | grep -qE "${DISALLOWED_DEPS_REGEX}"; then
        echo "Error: Disallowed distro-specific dependency detected in ${deb_file}" >&2
        exit 1
    fi
}

main() {
    ensure_temurin
    clean_outputs
    build_fat_jar
    build_runtime
    build_deb_pkg
    audit_deb_deps
}

main "$@"
