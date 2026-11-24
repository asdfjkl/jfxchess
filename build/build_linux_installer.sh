#!/usr/bin/env bash
# JFXChess - A Chess Graphical User Interface
# Copyright (C) 2020-2026 Dominik Klein
#
# Build script for the universal Linux installer archive (jfxchess-setup-linux.tar.gz).
# Uses relative paths from the build directory for reproducibility across systems.

set -euo pipefail

# Move to the directory containing this script (build/) using a relative path
cd "$(dirname "$0")"

INSTALLER_DIR="build_linux_installer"
APP_DIR="${INSTALLER_DIR}/jfxchess"
ARCHIVE_NAME="jfxchess-setup-linux.tar.gz"

# Verify required binary assets exist before building (fail fast)
MISSING=0
for required_asset in \
    "${APP_DIR}/book/extbook.bin" \
    "${APP_DIR}/engine/stockfish_x64" \
    "${APP_DIR}/engine/stockfish5_x64"
do
    if [ ! -f "${required_asset}" ]; then
        echo "Error: Required file missing: build/${required_asset}" >&2
        MISSING=1
    fi
done

if [ "${MISSING}" -ne 0 ]; then
    echo "Please add the required files listed in build/linux_installer_files.txt and retry." >&2
    exit 1
fi

# Clean previous build outputs
rm -f "${INSTALLER_DIR}/${ARCHIVE_NAME}"
rm -f "${APP_DIR}"/*.jar

# Build fat JAR from repository root (parent of build/)
(
    cd ..
    mvn clean package
)

# Copy fat JAR into installer payload
cp ../target/jfxchess-jar-with-dependencies.jar "${APP_DIR}/"

# Ensure executable permissions on installer, launcher, and engines
chmod +x \
    "${INSTALLER_DIR}/install.sh" \
    "${APP_DIR}/jfxchess.sh" \
    "${APP_DIR}/engine/stockfish_x64" \
    "${APP_DIR}/engine/stockfish5_x64"

# Package into jfxchess-setup-linux.tar.gz under a top-level jfxchess-installer/ directory
(
    cd "${INSTALLER_DIR}"
    tar -zcvf "${ARCHIVE_NAME}" --exclude='.gitkeep' --transform 's,^\./,jfxchess-installer/,' ./install.sh ./jfxchess
)

echo "Created build/${INSTALLER_DIR}/${ARCHIVE_NAME}"
