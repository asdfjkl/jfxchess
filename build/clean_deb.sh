#!/usr/bin/env bash
# JFXChess - A Chess Graphical User Interface
# Copyright (C) 2020-2026 Dominik Klein
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# Cleans Debian package build outputs. Pass --purge-jdk to also remove
# the cached Eclipse Temurin 21 JDK.

set -euo pipefail

cd "$(dirname "$0")"

readonly CACHE_ROOT="${XDG_CACHE_HOME:-${HOME}/.cache}/jfxchess"
readonly DEFAULT_TEMURIN_DIR="${CACHE_ROOT}/temurin-21"
readonly BUILD_LINUX_DIR="build_linux"

main() {
    local purge_jdk=0

    while [ "$#" -gt 0 ]; do
        case "$1" in
            --purge-jdk)
                purge_jdk=1
                ;;
            *)
                echo "Usage: $(basename "$0") [--purge-jdk]" >&2
                exit 1
                ;;
        esac
        shift
    done

    rm -f "${BUILD_LINUX_DIR}/build/jar"/*.jar
    rm -rf "${BUILD_LINUX_DIR}/runtime"
    rm -f "${BUILD_LINUX_DIR}/output"/*.deb

    if [ "${purge_jdk}" -eq 1 ]; then
        rm -rf "${DEFAULT_TEMURIN_DIR}"
    fi
}

main "$@"
