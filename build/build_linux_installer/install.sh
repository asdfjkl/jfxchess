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

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
MIN_JAVA_VERSION=21
APP_NAME="JFXChess"
JAR_NAME="jfxchess-jar-with-dependencies.jar"
LAUNCHER_NAME="jfxchess.sh"
DESKTOP_FILENAME="jfxchess-jfxchess.desktop"
DEFAULT_INSTALL_DIR="${HOME}/jfxchess"
PREFS_DIR="${HOME}/.java/.userPrefs/org/asdfjkl/jfxchess"
OS_RELEASE_FILE="${JFXCHESS_OS_RELEASE:-/etc/os-release}"

SCRIPT_NAME="$(basename "$0")"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PAYLOAD_DIR="${SCRIPT_DIR}/jfxchess"
APPS_DIR="${XDG_DATA_HOME:-${HOME}/.local/share}/applications"
DESKTOP_FILE="${APPS_DIR}/${DESKTOP_FILENAME}"

# Runtime options
ACTION="install"
if [[ "${SCRIPT_NAME}" == "uninstall.sh" ]]; then
    ACTION="uninstall"
fi
UI_PREFERENCE="auto" # auto | gui | cli
UI_MODE="cli"        # zenity | kdialog | cli
ASSUME_YES=0
CUSTOM_PREFIX=""
PREFS_POLICY="ask"   # ask | remove | keep

# Java check results
JAVA_OK=0
JAVA_STATUS_MSG=""
JAVA_INSTALL_HELP=""

# ---------------------------------------------------------------------------
# Help / CLI Parsing
# ---------------------------------------------------------------------------
show_help() {
    local prog="./${SCRIPT_NAME}"
    cat <<EOF
${APP_NAME} Universal Linux Installer / Uninstaller

Usage:
  ${prog} [OPTIONS]

Actions:
  (default)             Install ${APP_NAME} (or uninstall when invoked as uninstall.sh)
  --uninstall           Cleanly uninstall ${APP_NAME} using the installed .desktop file
  -h, --help            Show this help message and exit

Options:
  --prefix DIR          Installation directory (default: ${DEFAULT_INSTALL_DIR})
  --gui                 Prefer graphical dialogs (zenity/kdialog) when a display is active (default)
  --cli                 Force interactive terminal mode
  -y, --yes             Non-interactive mode; accept defaults (default install path,
                        overwrite existing installation, remove preferences on uninstall
                        unless --keep-prefs is specified)
  --remove-prefs        During --uninstall, remove user preferences without prompting
  --keep-prefs          During --uninstall, keep user preferences without prompting

Paths:
  Default install dir : ${DEFAULT_INSTALL_DIR}
  Desktop entry       : ${DESKTOP_FILE}
  User preferences    : ${PREFS_DIR}
  Required Java       : OpenJDK >= ${MIN_JAVA_VERSION} (non-headless JRE)

Examples:
  ./install.sh
  ./install.sh --cli --prefix ~/apps/jfxchess
  ./install.sh --uninstall
  ~/jfxchess/uninstall.sh
EOF
}

parse_args() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            -h|--help)
                show_help
                exit 0
                ;;
            --uninstall)
                ACTION="uninstall"
                shift
                ;;
            --prefix)
                if [[ $# -lt 2 || -z "$2" ]]; then
                    echo "Error: --prefix requires a directory argument." >&2
                    exit 1
                fi
                CUSTOM_PREFIX="$2"
                shift 2
                ;;
            --prefix=*)
                CUSTOM_PREFIX="${1#*=}"
                if [[ -z "${CUSTOM_PREFIX}" ]]; then
                    echo "Error: --prefix requires a non-empty directory argument." >&2
                    exit 1
                fi
                shift
                ;;
            --cli)
                UI_PREFERENCE="cli"
                shift
                ;;
            --gui)
                UI_PREFERENCE="gui"
                shift
                ;;
            -y|--yes)
                ASSUME_YES=1
                shift
                ;;
            --remove-prefs)
                PREFS_POLICY="remove"
                shift
                ;;
            --keep-prefs)
                PREFS_POLICY="keep"
                shift
                ;;
            *)
                echo "Error: Unknown option '$1'. Use --help to see available options." >&2
                exit 1
                ;;
        esac
    done
}

# ---------------------------------------------------------------------------
# UI Layer (zenity / kdialog / CLI fallback)
# ---------------------------------------------------------------------------
detect_ui_mode() {
    if [[ "${ASSUME_YES}" -eq 1 || "${UI_PREFERENCE}" == "cli" ]]; then
        UI_MODE="cli"
        return
    fi

    if [[ -n "${DISPLAY:-}${WAYLAND_DISPLAY:-}" ]]; then
        if command -v zenity >/dev/null 2>&1; then
            UI_MODE="zenity"
            return
        elif command -v kdialog >/dev/null 2>&1; then
            UI_MODE="kdialog"
            return
        fi
    fi

    UI_MODE="cli"
}

ui_info() {
    local title="$1"
    local msg="$2"
    echo "${msg}"
    case "${UI_MODE}" in
        zenity)
            zenity --info --title="${title}" --text="${msg}" --no-markup 2>/dev/null || true
            ;;
        kdialog)
            kdialog --msgbox "${msg}" --title "${title}" 2>/dev/null || true
            ;;
    esac
}

ui_error() {
    local title="$1"
    local msg="$2"
    echo "Error: ${msg}" >&2
    case "${UI_MODE}" in
        zenity)
            zenity --error --title="${title}" --text="${msg}" --no-markup 2>/dev/null || true
            ;;
        kdialog)
            kdialog --error "${msg}" --title "${title}" 2>/dev/null || true
            ;;
    esac
}

ui_summary() {
    local title="$1"
    local msg="$2"
    printf "\n%s\n\n" "${msg}"
    case "${UI_MODE}" in
        zenity)
            printf "%s\n" "${msg}" | zenity --text-info \
                --title="${title}" \
                --width=680 \
                --height=420 \
                --font="Monospace 10" 2>/dev/null || true
            ;;
        kdialog)
            local tmp_file
            tmp_file="$(mktemp)"
            printf "%s\n" "${msg}" > "${tmp_file}"
            kdialog --textbox "${tmp_file}" 680 420 --title "${title}" 2>/dev/null || true
            rm -f "${tmp_file}"
            ;;
    esac
}

# Returns 0 for Yes, 1 for No. Default is Yes.
ui_confirm_yes_default() {
    local title="$1"
    local msg="$2"

    if [[ "${ASSUME_YES}" -eq 1 ]]; then
        return 0
    fi

    case "${UI_MODE}" in
        zenity)
            local rc=0
            set +e
            zenity --question --title="${title}" --text="${msg}" --no-markup 2>/dev/null
            rc=$?
            set -e
            [[ "${rc}" -eq 0 ]]
            return
            ;;
        kdialog)
            local rc=0
            set +e
            kdialog --yesno "${msg}" --title "${title}" 2>/dev/null
            rc=$?
            set -e
            [[ "${rc}" -eq 0 ]]
            return
            ;;
        cli)
            local reply=""
            read -r -p "${msg} [Y/n]: " reply || true
            case "${reply}" in
                ""|[Yy]|[Yy][Ee][Ss]) return 0 ;;
                *) return 1 ;;
            esac
            ;;
    esac
}

# Returns 0 for Yes, 1 for No. Default is No.
ui_confirm_no_default() {
    local title="$1"
    local msg="$2"

    if [[ "${ASSUME_YES}" -eq 1 ]]; then
        return 0
    fi

    case "${UI_MODE}" in
        zenity)
            local rc=0
            set +e
            zenity --question --default-cancel --title="${title}" --text="${msg}" --no-markup 2>/dev/null
            rc=$?
            # Older zenity versions may not support --default-cancel
            if [[ "${rc}" -eq 255 ]]; then
                zenity --question --title="${title}" --text="${msg}" --no-markup 2>/dev/null
                rc=$?
            fi
            set -e
            [[ "${rc}" -eq 0 ]]
            return
            ;;
        kdialog)
            local rc=0
            set +e
            kdialog --yesno "${msg}" --title "${title}" 2>/dev/null
            rc=$?
            set -e
            [[ "${rc}" -eq 0 ]]
            return
            ;;
        cli)
            local reply=""
            read -r -p "${msg} [y/N]: " reply || true
            case "${reply}" in
                [Yy]|[Yy][Ee][Ss]) return 0 ;;
                *) return 1 ;;
            esac
            ;;
    esac
}

# Prints selected path to stdout; returns non-zero if user cancels.
ui_prompt_dir() {
    local title="$1"
    local prompt_text="$2"
    local default_val="$3"

    if [[ "${ASSUME_YES}" -eq 1 ]]; then
        printf "%s\n" "${default_val}"
        return 0
    fi

    case "${UI_MODE}" in
        zenity)
            local out=""
            local rc=0
            set +e
            out="$(zenity --entry \
                --title="${title}" \
                --text="${prompt_text}" \
                --entry-text="${default_val}" \
                --width=520 2>/dev/null)"
            rc=$?
            set -e
            if [[ "${rc}" -ne 0 ]]; then
                return 1
            fi
            printf "%s\n" "${out:-${default_val}}"
            ;;
        kdialog)
            local out=""
            local rc=0
            set +e
            out="$(kdialog --inputbox "${prompt_text}" "${default_val}" --title "${title}" 2>/dev/null)"
            rc=$?
            set -e
            if [[ "${rc}" -ne 0 ]]; then
                return 1
            fi
            printf "%s\n" "${out:-${default_val}}"
            ;;
        cli)
            local reply=""
            read -r -p "${prompt_text} [${default_val}]: " reply || true
            printf "%s\n" "${reply:-${default_val}}"
            ;;
    esac
}

# ---------------------------------------------------------------------------
# Path & Desktop Helpers
# ---------------------------------------------------------------------------
normalize_path() {
    local p="$1"

    # Trim leading/trailing whitespace
    p="${p#"${p%%[![:space:]]*}"}"
    p="${p%"${p##*[![:space:]]}"}"

    # Expand leading tilde
    if [[ "${p}" == "~" ]]; then
        p="${HOME}"
    elif [[ "${p}" == "~/"* ]]; then
        p="${HOME}/${p#~/}"
    fi

    # Make relative paths absolute
    if [[ "${p}" != /* ]]; then
        p="${PWD}/${p}"
    fi

    # Normalize .. and . safely even when the target does not exist yet
    if command -v realpath >/dev/null 2>&1; then
        p="$(realpath -m -- "${p}")"
    else
        # Strip duplicate and trailing slashes (except root)
        while [[ "${p}" == *"//"* ]]; do
            p="${p//\/\//\/}"
        done
        if [[ "${p}" != "/" ]]; then
            p="${p%/}"
        fi
    fi

    printf "%s\n" "${p}"
}

is_unsafe_dir() {
    local dir="$1"
    local home_norm
    home_norm="$(normalize_path "${HOME}")"

    case "${dir}" in
        ""|"/"|"/home"|"/root"|"/usr"|"/usr/local"|"/opt"|"/etc"|"/var"|"/tmp"|"${home_norm}")
            return 0
            ;;
        *)
            return 1
            ;;
    esac
}

# Escape a path for use inside double quotes in a Desktop Entry Exec= key
desktop_escape_exec_arg() {
    local s="$1"
    s="${s//\\/\\\\}"
    s="${s//\"/\\\"}"
    s="${s//\$/\\\$}"
    s="${s//\`/\\\`}"
    s="${s//%/%%}"
    printf "%s" "${s}"
}

refresh_desktop_database() {
    if command -v update-desktop-database >/dev/null 2>&1; then
        update-desktop-database "${APPS_DIR}" >/dev/null 2>&1 || true
    fi
}

# ---------------------------------------------------------------------------
# Java & Distro Detection
# ---------------------------------------------------------------------------
build_java_install_help() {
    local distro_id=""
    local distro_like=""
    local distro_name="Linux"

    if [[ -r "${OS_RELEASE_FILE}" ]]; then
        distro_id="$(sed -n 's/^ID=//p' "${OS_RELEASE_FILE}" | head -n1 | tr -d '"' | tr '[:upper:]' '[:lower:]')"
        distro_like="$(sed -n 's/^ID_LIKE=//p' "${OS_RELEASE_FILE}" | head -n1 | tr -d '"' | tr '[:upper:]' '[:lower:]')"
        distro_name="$(sed -n 's/^PRETTY_NAME=//p' "${OS_RELEASE_FILE}" | head -n1 | tr -d '"')"
        [[ -z "${distro_name}" ]] && distro_name="${distro_id:-Linux}"
    fi

    local combined=" ${distro_id} ${distro_like} "
    local ver="${MIN_JAVA_VERSION}"

    if [[ "${combined}" == *" ubuntu "* || "${combined}" == *" debian "* || "${combined}" == *" linuxmint "* || "${combined}" == *" pop "* ]]; then
        JAVA_INSTALL_HELP="Detected distribution: ${distro_name}
To install OpenJDK ${ver} (full desktop JRE) and select it as default:

  sudo apt update && sudo apt install openjdk-${ver}-jre
  sudo update-alternatives --config java

If openjdk-${ver}-jre is not available in your distribution's default repository,
download Eclipse Temurin ${ver} (JRE) from: https://adoptium.net/"
    elif [[ "${combined}" == *" fedora "* || "${combined}" == *" rhel "* || "${combined}" == *" centos "* || "${combined}" == *" rocky "* || "${combined}" == *" almalinux "* || "${combined}" == *" ol "* ]]; then
        local pkg_mgr="dnf"
        if ! command -v dnf >/dev/null 2>&1 && command -v yum >/dev/null 2>&1; then
            pkg_mgr="yum"
        fi
        JAVA_INSTALL_HELP="Detected distribution: ${distro_name}
To install OpenJDK ${ver} (full desktop JRE) and select it as default:

  sudo ${pkg_mgr} install java-${ver}-openjdk
  sudo alternatives --config java

If java-${ver}-openjdk is not available in your distribution's default repository,
download Eclipse Temurin ${ver} (JRE) from: https://adoptium.net/"
    elif [[ "${combined}" == *" arch "* || "${combined}" == *" manjaro "* || "${combined}" == *" endeavouros "* || "${combined}" == *" cachyos "* || "${combined}" == *" garuda "* ]]; then
        JAVA_INSTALL_HELP="Detected distribution: ${distro_name}
To install OpenJDK ${ver} (full desktop JRE) and select it as default:

  sudo pacman -S jre${ver}-openjdk
  sudo archlinux-java set java-${ver}-openjdk"
    elif [[ "${combined}" == *" opensuse"* || "${combined}" == *" sles "* || "${combined}" == *" suse "* ]]; then
        JAVA_INSTALL_HELP="Detected distribution: ${distro_name}
To install OpenJDK ${ver} (full desktop JRE) and select it as default:

  sudo zypper install java-${ver}-openjdk
  sudo update-alternatives --config java"
    else
        JAVA_INSTALL_HELP="Detected distribution: ${distro_name}
Please install OpenJDK ${ver} (full non-headless desktop JRE, not the -headless package)
using your distribution's package manager, or download Eclipse Temurin ${ver} from:

  https://adoptium.net/"
    fi
}

check_java() {
    local java_bin=""
    if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
        java_bin="${JAVA_HOME}/bin/java"
    elif command -v java >/dev/null 2>&1; then
        java_bin="$(command -v java)"
    elif [[ -x "/usr/bin/java" ]]; then
        java_bin="/usr/bin/java"
    fi

    build_java_install_help

    if [[ -z "${java_bin}" ]]; then
        JAVA_OK=0
        JAVA_STATUS_MSG="Java was not found on your system (OpenJDK >= ${MIN_JAVA_VERSION} JRE is required)."
        return
    fi

    local version_output=""
    version_output="$("${java_bin}" -version 2>&1 || true)"

    local version_str=""
    version_str="$(printf "%s\n" "${version_output}" | sed -n 's/.*version "\([^"]*\)".*/\1/p' | head -n1)"

    local major_ver=""
    if [[ "${version_str}" == 1.* ]]; then
        major_ver="$(printf "%s" "${version_str}" | cut -d. -f2 | sed 's/[^0-9].*//')"
    else
        major_ver="$(printf "%s" "${version_str}" | sed 's/[^0-9].*//')"
    fi

    if [[ -z "${major_ver}" || ! "${major_ver}" =~ ^[0-9]+$ ]]; then
        JAVA_OK=0
        JAVA_STATUS_MSG="Could not determine Java version from '${java_bin}' (OpenJDK >= ${MIN_JAVA_VERSION} JRE is required)."
        return
    fi

    if (( major_ver < MIN_JAVA_VERSION )); then
        JAVA_OK=0
        JAVA_STATUS_MSG="Found Java ${version_str} (${java_bin}), but ${APP_NAME} requires Java >= ${MIN_JAVA_VERSION}."
        return
    fi

    # Detect headless-only JRE (Swing GUI requires libawt_xawt.so)
    if command -v readlink >/dev/null 2>&1; then
        local java_real jvm_home
        java_real="$(readlink -f "${java_bin}" 2>/dev/null || true)"
        if [[ -n "${java_real}" ]]; then
            jvm_home="$(dirname "$(dirname "${java_real}")")"
            if [[ -f "${jvm_home}/lib/libawt_headless.so" && ! -f "${jvm_home}/lib/libawt_xawt.so" ]]; then
                JAVA_OK=0
                JAVA_STATUS_MSG="Found Java ${version_str} (${java_bin}), but it appears to be a headless-only JRE. ${APP_NAME} requires the full desktop JRE."
                return
            fi
        fi
    fi

    JAVA_OK=1
    JAVA_STATUS_MSG="Found compatible Java ${version_str} (${java_bin})."
}

# ---------------------------------------------------------------------------
# Install Action
# ---------------------------------------------------------------------------
do_install() {
    if [[ ! -d "${PAYLOAD_DIR}" || ! -f "${PAYLOAD_DIR}/${JAR_NAME}" ]]; then
        ui_error "${APP_NAME} Installer" \
            "Application payload not found at '${PAYLOAD_DIR}'. Please run install.sh from the extracted installer archive."
        exit 1
    fi

    check_java

    local chosen_dir=""
    if [[ -n "${CUSTOM_PREFIX}" ]]; then
        chosen_dir="${CUSTOM_PREFIX}"
    else
        if ! chosen_dir="$(ui_prompt_dir \
            "${APP_NAME} Installer" \
            "Enter the installation directory for ${APP_NAME}:" \
            "${DEFAULT_INSTALL_DIR}")"; then
            echo "Installation cancelled."
            exit 1
        fi
    fi

    local install_dir
    install_dir="$(normalize_path "${chosen_dir}")"

    if is_unsafe_dir "${install_dir}"; then
        ui_error "${APP_NAME} Installer" \
            "Refusing to install directly into '${install_dir}'. Please choose a dedicated folder (e.g., ${DEFAULT_INSTALL_DIR})."
        exit 1
    fi

    if [[ -e "${install_dir}" ]]; then
        if ! ui_confirm_yes_default \
            "${APP_NAME} Installer" \
            "The directory '${install_dir}' already exists. Overwrite/update files in this directory?"; then
            echo "Installation cancelled."
            exit 1
        fi
    fi

    echo "Installing ${APP_NAME} to: ${install_dir}"
    mkdir -p "${install_dir}"

    # Copy payload excluding .gitkeep files
    (
        cd "${PAYLOAD_DIR}"
        tar --exclude='.gitkeep' -cf - .
    ) | (
        cd "${install_dir}"
        tar -xf -
    )

    # Ensure executable permissions on launcher and bundled engines
    chmod +x "${install_dir}/${LAUNCHER_NAME}"
    if [[ -f "${install_dir}/engine/stockfish_x64" ]]; then
        chmod +x "${install_dir}/engine/stockfish_x64"
    fi
    if [[ -f "${install_dir}/engine/stockfish5_x64" ]]; then
        chmod +x "${install_dir}/engine/stockfish5_x64"
    fi

    # Install standalone uninstaller (copy of this script invoked as uninstall.sh)
    local uninstall_script="${install_dir}/uninstall.sh"
    cp -f "${BASH_SOURCE[0]}" "${uninstall_script}"
    chmod +x "${uninstall_script}"

    # Create FreeDesktop .desktop entry
    mkdir -p "${APPS_DIR}"
    local exec_escaped
    exec_escaped="$(desktop_escape_exec_arg "${install_dir}/${LAUNCHER_NAME}")"

    cat > "${DESKTOP_FILE}" <<EOF
[Desktop Entry]
Version=1.0
Type=Application
Name=${APP_NAME}
Comment=Chess GUI with Stockfish analysis and database support
Exec="${exec_escaped}" %f
Path=${install_dir}
Icon=${install_dir}/jfxchess.png
StartupWMClass=org-asdfjkl-jfxchess-gui-App
Terminal=false
Categories=Game;BoardGame;
X-JFXChess-InstallDir=${install_dir}
EOF
    chmod 644 "${DESKTOP_FILE}"
    refresh_desktop_database

    # Build final summary message
    local summary=""
    summary="${APP_NAME} has been installed successfully!

Installed directory : ${install_dir}
Launcher script     : ${install_dir}/${LAUNCHER_NAME}
Desktop entry       : ${DESKTOP_FILE}

Uninstallation:
  The uninstaller has been saved to:
    ${uninstall_script}

  To cleanly uninstall ${APP_NAME} later, run either:
    \"${uninstall_script}\"
  or (from the installer archive):
    ./install.sh --uninstall"

    if [[ "${JAVA_OK}" -eq 1 ]]; then
        summary="${summary}

Java check:
  ${JAVA_STATUS_MSG}"
        ui_summary "${APP_NAME} Installation Complete" "${summary}"
    else
        summary="${summary}

=======================================================================
ACTION REQUIRED: Java >= ${MIN_JAVA_VERSION} (OpenJDK ${MIN_JAVA_VERSION} JRE)
=======================================================================
${JAVA_STATUS_MSG}

${APP_NAME} has been installed, but requires OpenJDK ${MIN_JAVA_VERSION} (non-headless JRE)
before it can be launched.

${JAVA_INSTALL_HELP}"
        ui_summary "${APP_NAME} Installed (Java ${MIN_JAVA_VERSION} Required)" "${summary}"
    fi
}

# ---------------------------------------------------------------------------
# Uninstall Action
# ---------------------------------------------------------------------------
resolve_install_dir_from_desktop() {
    local desktop_path="$1"
    local resolved=""

    if [[ -r "${desktop_path}" ]]; then
        # 1. Preferred: explicit metadata key
        resolved="$(sed -n 's/^X-JFXChess-InstallDir=//p' "${desktop_path}" | head -n1)"

        # 2. Fallback: Icon=<dir>/jfxchess.png
        if [[ -z "${resolved}" ]]; then
            local icon_line
            icon_line="$(sed -n 's/^Icon=//p' "${desktop_path}" | head -n1)"
            if [[ "${icon_line}" == */jfxchess.png ]]; then
                resolved="${icon_line%/jfxchess.png}"
            fi
        fi

        # 3. Secondary fallback: Exec= containing jfxchess.sh or jar
        if [[ -z "${resolved}" ]]; then
            local exec_line
            exec_line="$(sed -n 's/^Exec=//p' "${desktop_path}" | head -n1)"
            if [[ "${exec_line}" == *"/${LAUNCHER_NAME}"* ]]; then
                resolved="${exec_line%%/${LAUNCHER_NAME}*}"
                resolved="${resolved#\"}"
                resolved="${resolved//\\\"/\"}"
                resolved="${resolved//\\\\/\\}"
            elif [[ "${exec_line}" == *"/${JAR_NAME}"* ]]; then
                resolved="${exec_line%%/${JAR_NAME}*}"
                resolved="${resolved##*-jar }"
                resolved="${resolved#\"}"
            fi
        fi
    fi

    # 4. Fallback when invoked directly as <INSTALL_DIR>/uninstall.sh
    if [[ -z "${resolved}" && "${SCRIPT_NAME}" == "uninstall.sh" && -f "${SCRIPT_DIR}/${JAR_NAME}" ]]; then
        resolved="${SCRIPT_DIR}"
    fi

    if [[ -n "${resolved}" ]]; then
        normalize_path "${resolved}"
    fi
}

do_uninstall() {
    local install_dir=""
    install_dir="$(resolve_install_dir_from_desktop "${DESKTOP_FILE}")"

    if [[ -z "${install_dir}" ]]; then
        ui_error "${APP_NAME} Uninstaller" \
            "Could not determine the ${APP_NAME} installation directory (desktop entry '${DESKTOP_FILE}' not found or unrecognized)."
        exit 1
    fi

    if is_unsafe_dir "${install_dir}"; then
        ui_error "${APP_NAME} Uninstaller" \
            "Refusing to remove unsafe path '${install_dir}'."
        exit 1
    fi

    if [[ ! -d "${install_dir}" || ! -f "${install_dir}/${JAR_NAME}" ]]; then
        ui_error "${APP_NAME} Uninstaller" \
            "Expected ${APP_NAME} installation at '${install_dir}', but '${JAR_NAME}' was not found there. Aborting for safety."
        exit 1
    fi

    if ! ui_confirm_yes_default \
        "${APP_NAME} Uninstaller" \
        "Uninstall ${APP_NAME} from '${install_dir}' and remove '${DESKTOP_FILE}'?"; then
        echo "Uninstallation cancelled."
        exit 0
    fi

    local remove_prefs=0
    if [[ -d "${PREFS_DIR}" ]]; then
        case "${PREFS_POLICY}" in
            remove)
                remove_prefs=1
                ;;
            keep)
                remove_prefs=0
                ;;
            ask)
                if ui_confirm_yes_default \
                    "${APP_NAME} Uninstaller" \
                    "Also remove ${APP_NAME} user preferences (${PREFS_DIR})?"; then
                    remove_prefs=1
                else
                    remove_prefs=0
                fi
                ;;
        esac
    fi

    # Move out of install_dir before deleting it (important when running uninstall.sh inside install_dir)
    cd "${HOME}"

    rm -rf -- "${install_dir}"
    if [[ -f "${DESKTOP_FILE}" ]]; then
        rm -f -- "${DESKTOP_FILE}"
    fi
    refresh_desktop_database

    local prefs_msg="User preferences kept."
    if [[ "${remove_prefs}" -eq 1 && -d "${PREFS_DIR}" ]]; then
        rm -rf -- "${PREFS_DIR}"
        # Clean up empty parent nodes up to ~/.java/.userPrefs without failing if non-empty
        rmdir "${HOME}/.java/.userPrefs/org/asdfjkl" 2>/dev/null || true
        rmdir "${HOME}/.java/.userPrefs/org" 2>/dev/null || true
        prefs_msg="User preferences removed (${PREFS_DIR})."
    elif [[ ! -d "${PREFS_DIR}" ]]; then
        prefs_msg="No user preferences directory found."
    fi

    ui_info "${APP_NAME} Uninstalled" \
        "${APP_NAME} has been uninstalled.

Removed folder : ${install_dir}
Removed entry  : ${DESKTOP_FILE}
Preferences    : ${prefs_msg}"
}

# ---------------------------------------------------------------------------
# Entrypoint (wrapped in main so uninstall.sh can safely delete itself)
# ---------------------------------------------------------------------------
main() {
    parse_args "$@"
    detect_ui_mode

    case "${ACTION}" in
        install)
            do_install
            ;;
        uninstall)
            do_uninstall
            ;;
        *)
            echo "Error: Unsupported action '${ACTION}'." >&2
            exit 1
            ;;
    esac
}

main "$@"
exit $?
