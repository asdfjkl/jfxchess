#!/usr/bin/env bash
# JFXChess - A Chess Graphical User Interface
# Copyright (C) 2020-2026 Dominik Klein
#
# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License
# as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

set -euo pipefail

MIN_JAVA_VERSION=21
APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
    JAVA_BIN="${JAVA_HOME}/bin/java"
elif command -v java >/dev/null 2>&1; then
    JAVA_BIN="$(command -v java)"
elif [[ -x "/usr/bin/java" ]]; then
    JAVA_BIN="/usr/bin/java"
else
    MSG="Java ${MIN_JAVA_VERSION} (OpenJDK ${MIN_JAVA_VERSION} JRE) was not found. Please install OpenJDK ${MIN_JAVA_VERSION} and try again."
    if [[ -n "${DISPLAY:-}${WAYLAND_DISPLAY:-}" ]]; then
        if command -v zenity >/dev/null 2>&1; then
            zenity --error --title="JFXChess" --text="${MSG}" --no-markup 2>/dev/null || true
        elif command -v kdialog >/dev/null 2>&1; then
            kdialog --error "${MSG}" --title "JFXChess" 2>/dev/null || true
        fi
    fi
    echo "Error: ${MSG}" >&2
    exit 1
fi

exec "${JAVA_BIN}" \
    -splash:"${APP_DIR}/splash.png" \
    -jar "${APP_DIR}/jfxchess-jar-with-dependencies.jar" \
    "$@"
