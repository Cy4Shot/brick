package brick.gen.impl

import brick.gen.*
import brick.util.Logging._

val utilsUnix =
"""
# Reset all formatting
export FMT_RESET="\e[0m"

# --- Text Colors ---
export FMT_COLOR_BLACK="\e[30m"
export FMT_COLOR_RED="\e[31m"
export FMT_COLOR_GREEN="\e[32m"
export FMT_COLOR_YELLOW="\e[33m"
export FMT_COLOR_BLUE="\e[34m"
export FMT_COLOR_MAGENTA="\e[35m"
export FMT_COLOR_CYAN="\e[36m"
export FMT_COLOR_WHITE="\e[37m"
# Bright/Light versions
export FMT_COLOR_BRIGHT_BLACK="\e[90m" # Often appears as gray
export FMT_COLOR_BRIGHT_RED="\e[91m"
export FMT_COLOR_BRIGHT_GREEN="\e[92m"
export FMT_COLOR_BRIGHT_YELLOW="\e[93m"
export FMT_COLOR_BRIGHT_BLUE="\e[94m"
export FMT_COLOR_BRIGHT_MAGENTA="\e[95m"
export FMT_COLOR_BRIGHT_CYAN="\e[96m"
export FMT_COLOR_BRIGHT_WHITE="\e[97m"

# --- Background Colors ---
export FMT_BG_BLACK="\e[40m"
export FMT_BG_RED="\e[41m"
export FMT_BG_GREEN="\e[42m"
export FMT_BG_YELLOW="\e[43m"
export FMT_BG_BLUE="\e[44m"
export FMT_BG_MAGENTA="\e[45m"
export FMT_BG_CYAN="\e[46m"
export FMT_BG_WHITE="\e[47m"
# Bright/Light versions
export FMT_BG_BRIGHT_BLACK="\e[100m"
export FMT_BG_BRIGHT_RED="\e[101m"
export FMT_BG_BRIGHT_GREEN="\e[102m"
export FMT_BG_BRIGHT_YELLOW="\e[103m"
export FMT_BG_BRIGHT_BLUE="\e[104m"
export FMT_BG_BRIGHT_MAGENTA="\e[105m"
export FMT_BG_BRIGHT_CYAN="\e[106m"
export FMT_BG_BRIGHT_WHITE="\e[107m"

# --- Text Formatting ---
export FMT_BOLD="\e[1m"
export FMT_DIM="\e[2m"           # Often not well supported or looks like normal text
export FMT_ITALIC="\e[3m"        # Not universally supported
export FMT_UNDERLINE="\e[4m"
export FMT_BLINK="\e[5m"         # Often annoying and not well supported
export FMT_REVERSE="\e[7m"       # Swaps foreground and background colors
export FMT_HIDDEN="\e[8m"        # Text is not visible (but can be copied)
export FMT_STRIKETHROUGH="\e[9m" # Not universally supported

# --- Reset specific formatting ---
export FMT_RESET_BOLD_DIM="\e[22m"    # Resets both bold and dim
export FMT_RESET_ITALIC="\e[23m"
export FMT_RESET_UNDERLINE="\e[24m"
export FMT_RESET_BLINK="\e[25m"
export FMT_RESET_REVERSE="\e[27m"
export FMT_RESET_HIDDEN="\e[28m"
export FMT_RESET_STRIKETHROUGH="\e[29m"

innerecho() {
    if [ -t 1 ]; then
        echo -e "$1"
    else
        echo -e "$1" > /dev/tty
    fi
}

titleecho() {
	local string="$*"
	local length=${#string}
	local width=$(($(($(tput cols) - $length)) / 2 - 1))

	local bars="${FMT_COLOR_CYAN}${FMT_BOLD}"

	for ((i=0; i<width; i++)); do
		bars+="="
	done
	
	innerecho "${bars} ${string} ${bars}"
}

errecho() {
	innerecho "${FMT_COLOR_RED}${FMT_BOLD}ERROR: $*${FMT_RESET}"
	exit 1
}

taskecho() {
	innerecho "${FMT_COLOR_BLUE}${FMT_BOLD}Task: $*${FMT_RESET}"
}

stepecho() {
	innerecho "${FMT_COLOR_BRIGHT_BLACK}${FMT_BOLD}===> $*${FMT_RESET}"
}

warnecho() {
	innerecho "${FMT_COLOR_YELLOW}${FMT_BOLD}===> $*${FMT_RESET}"
}

successecho() {
	innerecho "${FMT_COLOR_GREEN}${FMT_BOLD}===> $*${FMT_RESET}"
}

gitdownload() {
    if [ $# -lt 3 ]; then
        errecho "gitdownload expected three arguments but received $#"
    fi

    local output="$1"
    local url="$2"
    local commit="$3"

    if [ -d "$output" ]; then
        pushd "$output"
        git checkout "$commit"
        local res=$?
        popd
    else
        # WARNING: feature.manyFiles is not supported by all git clients. If
        # this breaks, remove v-----------------------v
        git clone --recursive -c feature.manyFiles=true "$url" "$output" 2>&1 && pushd "$output" && git checkout "$commit" 2>&1 && popd
        local res=$?
    fi

    return $res
}

curldownload() {
    if [ "$#" -lt 2 ]; then
        errecho "curldownload expected at least two arguments: <output_path> <url> [optional: <archive_type_for_extraction>]"
        return 1
    fi

    local output_dir="$1"
    local url="$2"
    local archive_type="${3:-}"

    local download_dir="${TMP_DIR:-/tmp}/downloads"
    mkdir -p "${download_dir}"

    local filename
    filename=$(basename "$url")
    local download_path="${download_dir}/${filename}"

    if [ -e $download_path ]; then
        warnecho "Source already exists. Skipping re-download. Remove the existing build directory to re-download"
    else
        if ! curl -L -o "$download_path" -Ss "$url"; then
            errecho "Download failed for '$url'"
        fi
    fi
 
    mkdir -p "$output_dir"
    pushd "$output_dir" > /dev/null || return 1

    local success=0
    if [[ "$archive_type" == "tar.gz" || "$filename" == *.tar.gz ]]; then
        if ! tar --strip-components=1 -xvzf "$download_path" -C "."; then 
            errecho "Extraction failed for '$download_path' (tar.gz)"
            success=1
        fi
    elif [[ "$archive_type" == "tar.xz" || "$filename" == *.tar.xz ]]; then
        if ! tar --strip-components=1 -xvJf "$download_path" -C "."; then
            errecho "Extraction failed for '$download_path' (tar.xz)"
            success=1
        fi
    elif [[ "$archive_type" == "zip" || "$filename" == *.zip ]]; then
        if ! unzip -j "$download_path" -d "."; then
            errecho "Extraction failed for '$download_path' (zip)"
            success=1
        fi
    elif [[ "$archive_type" == "tar.bz2" || "$filename" == *.tar.bz2 ]]; then
        if ! tar --strip-components=1 -xvjf "$download_path" -C "."; then
            errecho "Extraction failed for '$download_path' (tar.bz2)"
            success=1
        fi
    elif [[ "$archive_type" == "none" ]]; then
        warnecho "Extraction skipped as per user request (type: none). Moving file instead."
        if ! mv "$download_path" "."; then
                errecho "Failed to move '$download_path' to '$output_dir'"
                success=1
        fi
    else
        warnecho "File is not a recognized archive or type specified is not supported for auto-extraction: '$filename'."
        warnecho "Attempting to move it to output directory."
        if ! mv "$download_path" "."; then
                errecho "Failed to move '$download_path' to '$output_dir'"
                success=1
        else
                success=0
        fi
    fi

    popd > /dev/null || return 1
    return "$success"
}
"""

class UtilsGenerator() extends Generator {

  def validate()(implicit builder: ScriptBuilder): Unit = {}

  def generate()(implicit builder: ScriptBuilder): String =
    if builder.unix then
      utilsUnix
    else
      throwError("Windows utilities are not supported yet.")
}
