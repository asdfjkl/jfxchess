#!/usr/bin/env bash
# JFXChess - A Chess Graphical User Interface
# Copyright (C) 2020-2026 Dominik Klein
#
# Clean script for AppImage build outputs.

set -euo pipefail

cd "$(dirname "$0")"

rm -f app-image/*.AppImage
