package brick.gen.impl

import brick.gen.*

import scala.collection.mutable

val utilsWin =
"""
# Enable ANSI Escape Sequences (Windows Terminal supports it by default)
$ESC = [char]27

# --- Text Colors ---
$FMT_COLOR_BLACK        = "$ESC[30m"
$FMT_COLOR_RED          = "$ESC[31m"
$FMT_COLOR_GREEN        = "$ESC[32m"
$FMT_COLOR_YELLOW       = "$ESC[33m"
$FMT_COLOR_BLUE         = "$ESC[34m"
$FMT_COLOR_MAGENTA      = "$ESC[35m"
$FMT_COLOR_CYAN         = "$ESC[36m"
$FMT_COLOR_WHITE        = "$ESC[37m"
$FMT_COLOR_BRIGHT_BLACK = "$ESC[90m"
$FMT_COLOR_BRIGHT_RED   = "$ESC[91m"
$FMT_COLOR_BRIGHT_GREEN = "$ESC[92m"
$FMT_COLOR_BRIGHT_YELLOW= "$ESC[93m"
$FMT_COLOR_BRIGHT_BLUE  = "$ESC[94m"
$FMT_COLOR_BRIGHT_MAGENTA="$ESC[95m"
$FMT_COLOR_BRIGHT_CYAN  = "$ESC[96m"
$FMT_COLOR_BRIGHT_WHITE = "$ESC[97m"

# --- Background Colors ---
$FMT_BG_BLACK        = "$ESC[40m"
$FMT_BG_RED          = "$ESC[41m"
$FMT_BG_GREEN        = "$ESC[42m"
$FMT_BG_YELLOW       = "$ESC[43m"
$FMT_BG_BLUE         = "$ESC[44m"
$FMT_BG_MAGENTA      = "$ESC[45m"
$FMT_BG_CYAN         = "$ESC[46m"
$FMT_BG_WHITE        = "$ESC[47m"
$FMT_BG_BRIGHT_BLACK = "$ESC[100m"
$FMT_BG_BRIGHT_RED   = "$ESC[101m"
$FMT_BG_BRIGHT_GREEN = "$ESC[102m"
$FMT_BG_BRIGHT_YELLOW= "$ESC[103m"
$FMT_BG_BRIGHT_BLUE  = "$ESC[104m"
$FMT_BG_BRIGHT_MAGENTA="$ESC[105m"
$FMT_BG_BRIGHT_CYAN  = "$ESC[106m"
$FMT_BG_BRIGHT_WHITE = "$ESC[107m"

# --- Text Formatting ---
$FMT_BOLD          = "$ESC[1m"
$FMT_DIM           = "$ESC[2m"
$FMT_ITALIC        = "$ESC[3m"
$FMT_UNDERLINE     = "$ESC[4m"
$FMT_BLINK         = "$ESC[5m"
$FMT_REVERSE       = "$ESC[7m"
$FMT_HIDDEN        = "$ESC[8m"
$FMT_STRIKETHROUGH = "$ESC[9m"
$FMT_RESET         = "$ESC[0m"

function Write-Title {
    param([string]$Text)
    $width = [console]::WindowWidth
    $bar = '=' * [Math]::Floor(($width - $Text.Length - 2) / 2)
    Write-Host "$FMT_COLOR_CYAN$FMT_BOLD$bar $Text $bar$FMT_RESET"
}

function Write-ErrorLog {
    param([string]$Message)
    Write-Host "$FMT_COLOR_RED$FMT_BOLD`ERROR:`$FMT_RESET $Message"
    exit 1
}

function Write-WarningLog {
    param([string]$Message)
    Write-Host "$FMT_COLOR_YELLOW$FMT_BOLD`WARNING:`$FMT_RESET $Message"
}

function Write-InfoLog {
    param([string]$Message)
    Write-Host "$FMT_COLOR_GREEN$FMT_BOLD`INFO:`$FMT_RESET $Message"
}

function Git-Download {
    param(
        [string]$Output,
        [string]$RepoUrl,
        [string]$Commit
    )
    if (-Not $Output -or -Not $RepoUrl -or -Not $Commit) {
        Write-ErrorLog "Git-Download expected three arguments."
    }

    if (Test-Path $Output) {
        Write-InfoLog "Checking out '$RepoUrl':'$Commit' in '$Output'"
        Push-Location $Output
        git checkout $Commit
        $res = $LASTEXITCODE
        Pop-Location
    } else {
        Write-InfoLog "Cloning '$RepoUrl':$Commit to '$Output'"
        git clone --recursive -c feature.manyFiles=true $RepoUrl $Output
        Push-Location $Output
        git checkout $Commit
        $res = $LASTEXITCODE
        Pop-Location
    }

    return $res
}

function Curl-Download {
    param(
        [string]$OutputDir,
        [string]$Url,
        [string]$ArchiveType = ""
    )

    if (-not $OutputDir -or -not $Url) {
        Write-ErrorLog "Curl-Download requires at least two arguments: <OutputDir> <URL>"
    }

    $DownloadDir = $env:TMP_DIR
    if (-not $DownloadDir) { $DownloadDir = "$env:TEMP\downloads" }
    New-Item -ItemType Directory -Force -Path $DownloadDir | Out-Null

    $Filename = [System.IO.Path]::GetFileName($Url)
    $DownloadPath = Join-Path $DownloadDir $Filename

    if (Test-Path $DownloadPath) {
        Write-WarningLog "Source already exists. Skipping re-download."
    } else {
        Write-InfoLog "Downloading '$Url' to '$DownloadPath'"
        Invoke-WebRequest -Uri $Url -OutFile $DownloadPath -UseBasicParsing
    }

    New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
    Push-Location $OutputDir

    $success = $true

    switch -Regex ($ArchiveType, $Filename) {
        {$_ -match '(\.tar\.gz|tar\.gz)'} {
            tar -xvzf $DownloadPath --strip-components=1
        }
        {$_ -match '(\.tar\.xz|tar\.xz)'} {
            tar -xvJf $DownloadPath --strip-components=1
        }
        {$_ -match '(\.tar\.bz2|tar\.bz2)'} {
            tar -xvjf $DownloadPath --strip-components=1
        }
        {$_ -match '(\.zip|zip)'} {
            Expand-Archive -Path $DownloadPath -DestinationPath . -Force
        }
        {$_ -eq "none"} {
            Write-InfoLog "Extraction skipped. Moving file."
            Move-Item -Path $DownloadPath -Destination .
        }
        default {
            Write-InfoLog "Unrecognized archive format. Moving file instead."
            Move-Item -Path $DownloadPath -Destination .
        }
    }

    Pop-Location
    Write-InfoLog "Successfully downloaded and processed '$Url' to '$OutputDir'"
    return $success
}
"""

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

titleecho() {
	local string="$1"
	local length=${#string}
	local width=$(($(($(tput cols) - $length)) / 2 - 1))

	local bars="${FMT_COLOR_CYAN}${FMT_BOLD}"

	for ((i=0; i<width; i++)); do
		bars+="="
	done
	
	echo -e "${bars} $1 ${bars}"
}

# Error message
errecho() {
	echo -e "${FMT_COLOR_RED}${FMT_BOLD}ERROR:${FMT_RESET} $1"
	exit 1
}

# Warning message
warnecho() {
	echo -e "${FMT_COLOR_YELLOW}${FMT_BOLD}WARNING:${FMT_RESET} $1"
}

# Log message
logecho() {
	echo -e "${FMT_COLOR_GREEN}${FMT_BOLD}INFO:${FMT_RESET} $1"
}

gitdownload() {
    if [ $# -lt 3 ]; then
        errecho "gitdownload expected three arguments but received $#"
    fi

    local output="$1"
    local url="$2"
    local commit="$3"

    if [ -d $output ]; then
        logecho "Checking out '$url':'$commit' in '$output'"

        pushd $output

        git checkout $commit
        local res=$?

        popd
    else
        logecho "Cloning '$url':$commit to '$output'"

        # WARNING: feature.manyFiles is not supported by all git clients. If
        # this breaks, remove v-----------------------v
        git clone --recursive -c feature.manyFiles=true $url $output && pushd $output && git checkout $commit && popd
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
    local archive_type="${3:-}" # Default to tar.gz

    local download_dir="${TMP_DIR:-/tmp}/downloads" # Use /tmp if TMP_DIR is not set
    mkdir -p "${download_dir}"

    # Extract filename from URL
    local filename
    filename=$(basename "$url")
    local download_path="${download_dir}/${filename}"

    if [ -e $download_path ]; then
        warnecho "Source already exists. Skipping re-download. Remove the existing build directory to re-download"
    else
        logecho "Downloading '$url' to '$download_path'"

        # -L: follow redirects
        # -o: output to file
        # -S: show errors
        # -s: silent mode (don't show progress meter)
        if ! curl -L -o "$download_path" -Ss "$url"; then
            errecho "Download failed for '$url'"
        fi
        logecho "Download complete: '$download_path'"
    fi
 
    # Create output directory if it doesn't exist
    mkdir -p "$output_dir"

    logecho "Extracting '$download_path' to '$output_dir'"
    pushd "$output_dir" > /dev/null || return 1 # Enter output directory

    local success=0
    if [[ "$archive_type" == "tar.gz" || "$filename" == *.tar.gz ]]; then
        if ! tar --strip-components=1 -xvzf "$download_path" -C "."; then # Extract to current dir (output_dir)
            errecho "Extraction failed for '$download_path' (tar.gz)"
            success=1
        fi
    elif [[ "$archive_type" == "tar.xz" || "$filename" == *.tar.xz ]]; then
        if ! tar --strip-components=1 -xvJf "$download_path" -C "."; then # Extract to current dir (output_dir)
            errecho "Extraction failed for '$download_path' (tar.xz)"
            success=1
        fi
    elif [[ "$archive_type" == "zip" || "$filename" == *.zip ]]; then
        if ! unzip -j "$download_path" -d "."; then # Extract to current dir (output_dir)
            errecho "Extraction failed for '$download_path' (zip)"
            success=1
        fi
    elif [[ "$archive_type" == "tar.bz2" || "$filename" == *.tar.bz2 ]]; then
        if ! tar --strip-components=1 -xvjf "$download_path" -C "."; then
            errecho "Extraction failed for '$download_path' (tar.bz2)"
            success=1
        fi
    elif [[ "$archive_type" == "none" ]]; then
        logecho "Extraction skipped as per user request (type: none). Moving file instead."
        if ! mv "$download_path" "."; then
                errecho "Failed to move '$download_path' to '$output_dir'"
                success=1
        fi
    else
        logecho "File is not a recognized archive or type specified is not supported for auto-extraction: '$filename'."
        logecho "Attempting to move it to output directory."
        if ! mv "$download_path" "."; then
                errecho "Failed to move '$download_path' to '$output_dir'"
                success=1
        else
                logecho "Moved '$download_path' to '$output_dir' instead of extracting."
                # Set success to 0 because moving is not an error in this context
                success=0
        fi
    fi

    popd > /dev/null || return 1 # Return to original directory

    if [ "$success" -eq 0 ]; then
        logecho "Successfully downloaded and processed '$url' to '$output_dir'"
        # Optionally, clean up the downloaded archive if extraction was successful and it's not needed
        # rm "$download_path"
    fi

    return "$success"
}
"""

class UtilsGenerator() extends Generator {

  def validate()(implicit builder: ScriptBuilder): Unit = {}

  def generate()(implicit builder: ScriptBuilder): String =
    if builder.unix then
      utilsUnix
    else
      utilsWin
}
