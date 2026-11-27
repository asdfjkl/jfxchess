#!/usr/bin/env bash
# JFXChess - A Chess Graphical User Interface
# Copyright (C) 2020-2026 Dominik Klein
#
# Builds the cross-distro Linux AppImage into ./build/app-image/ using an
# Ubuntu 22.04 LTS + Eclipse Temurin 21 Docker container.

set -euo pipefail

cd "$(dirname "$0")"

readonly REPO_ROOT="$(cd .. && pwd)"
readonly BUILDER_DIR="build_app_image"
readonly ASSETS_DIR="${BUILDER_DIR}/assets"
readonly OUTPUT_DIR="app-image"
readonly DOCKER_IMAGE="jfxchess-appimage-builder:ubuntu22.04"
readonly M2_CACHE_VOLUME="jfxchess-appimage-m2-cache"

# Fail fast if Docker is missing or daemon is unreachable.
check_docker() {
    if ! command -v docker >/dev/null 2>&1; then
        echo "Error: docker not found. Install it via: sudo apt install -y docker.io docker-buildx" >&2
        exit 1
    fi

    if ! docker info >/dev/null 2>&1; then
        echo "Error: Docker daemon unreachable. Ensure docker service is running and your user is in the docker group." >&2
        exit 1
    fi
}

# Verify required binary assets exist before invoking Docker.
check_assets() {
    local missing=0
    local required_asset
    for required_asset in \
        "${ASSETS_DIR}/book/extbook.bin" \
        "${ASSETS_DIR}/engine/stockfish_x64" \
        "${ASSETS_DIR}/engine/stockfish5_x64" \
        "${ASSETS_DIR}/splash.png" \
        "${ASSETS_DIR}/splash@2x.png" \
        "../jfxchess_icon_256.png"
    do
        if [ ! -f "${required_asset}" ]; then
            echo "Error: Required file missing: build/${required_asset}" >&2
            missing=1
        fi
    done

    if [ "${missing}" -ne 0 ]; then
        echo "Please add the required files listed in build/app_image_files.txt and retry." >&2
        exit 1
    fi
}

main() {
    check_docker
    check_assets

    mkdir -p "${OUTPUT_DIR}"

    echo "=== Building Docker image (${DOCKER_IMAGE}) ==="
    docker build -t "${DOCKER_IMAGE}" "${BUILDER_DIR}"

    echo "=== Building AppImage inside Docker ==="
    docker run --rm \
        -e HOST_UID="$(id -u)" \
        -e HOST_GID="$(id -g)" \
        -v "${REPO_ROOT}:/workspace:ro" \
        -v "${REPO_ROOT}/build/${OUTPUT_DIR}:/out:rw" \
        -v "${M2_CACHE_VOLUME}:/root/.m2" \
        "${DOCKER_IMAGE}"

    echo "AppImage ready in build/${OUTPUT_DIR}/"
}

main "$@"
