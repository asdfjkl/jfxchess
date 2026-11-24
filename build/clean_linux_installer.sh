#!/usr/bin/env bash
# JFXChess - A Chess Graphical User Interface
# Copyright (C) 2020-2026 Dominik Klein
#
# Clean script for the universal Linux installer build artifacts.
# Uses relative paths from the build directory for reproducibility across systems.

set -euo pipefail

cd "$(dirname "$0")"

rm -f build_linux_installer/jfxchess-setup-linux.tar.gz
rm -f build_linux_installer/jfxchess/*.jar
