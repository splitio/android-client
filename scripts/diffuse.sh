#!/bin/bash

# API Diff Tool
# Compares public API surface between two Git branches using Diffuse
# Usage: ./scripts/diffuse.sh --target <target-branch> --source <source-branch> [--verbose]

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
DIFFUSE_VERSION="0.3.0"
DIFFUSE_CACHE_DIR_RELATIVE=".diffuse"
DIFFUSE_URL="https://github.com/JakeWharton/diffuse/releases/download/${DIFFUSE_VERSION}/diffuse-${DIFFUSE_VERSION}.zip"

# These will be set after repo path is determined
DIFFUSE_CACHE_DIR=""
DIFFUSE_BINARY=""

# AAR configuration (fused library output at root)
AAR_OUTPUT_DIR="build/outputs/aar"
# Prefer exact fused name; also support variant-suffixed names
AAR_CANDIDATE_NAMES=("android-client.aar" "android-client-release.aar" "android-client-debug.aar")
# Candidate Gradle tasks in order of preference; pick the first that exists
# Prefer 'bundle' for fused plugin roots, then legacy variant-specific bundles
GRADLE_TASK_CANDIDATES=("bundle" "bundleReleaseAar" "assembleRelease")

# Temporary files
TEMP_DIR=$(mktemp -d)
SOURCE_CLONE_DIR="${TEMP_DIR}/source-repo"
TARGET_CLONE_DIR="${TEMP_DIR}/target-repo"
TARGET_AAR="${TEMP_DIR}/target-branch.aar"
SOURCE_AAR="${TEMP_DIR}/source-branch.aar"

# Script arguments
TARGET_BRANCH=""
SOURCE_BRANCH=""
MODULE_PATH=""
REPO_PATH=""
VERBOSE=false
FULL=false

# Cleanup function - runs on exit, error, or interrupt
cleanup() {
    local exit_code=$?
    
    # Always cleanup, even on success
    if [ -d "$TEMP_DIR" ]; then
        [ "$VERBOSE" = true ] && echo -e "\n${BLUE}Cleaning up temporary files...${NC}"
        rm -rf "$TEMP_DIR" 2>/dev/null || true
        [ "$VERBOSE" = true ] && echo "Removed temporary repository clone and AAR files"
    fi
    
    # Only show error message if there was an error
    if [ $exit_code -ne 0 ]; then
        echo -e "${RED}Script failed with exit code $exit_code${NC}" >&2
    fi
    
    exit $exit_code
}

# Set trap for cleanup on exit
trap cleanup EXIT INT TERM

# Print error and exit
error_exit() {
    echo -e "${RED}Error: $1${NC}" >&2
    exit 1
}

# Verbose logging
log_verbose() {
    if [ "$VERBOSE" = true ]; then
        echo -e "${BLUE}$1${NC}"
    fi
}

# Print usage
usage() {
    cat << EOF
Usage: $0 --target <target-ref> --source <source-ref> [--module <path>] [--verbose|--full]

Options:
    --target <ref>       Target branch/tag/commit to compare against (e.g., main, origin/main, v1.0.0)
    --source <ref>       Source branch/tag/commit to compare (e.g., feature/my-feature, v1.1.0)
    --module <path>      Optional: Module path (defaults to root, future-proof for multi-module)
    --verbose            Show verbose output including build logs and progress messages
    --full               Show complete Diffuse output including detailed class/method/field changes

Examples:
    # Compare branches
    $0 --target origin/main --source feature/my-feature
    
    # Compare tags
    $0 --target v1.0.0 --source v1.1.0
    
    # Compare with verbose output (shows build logs)
    $0 --target v1.0.0 --source main --verbose
    
    # Compare with full Diffuse output (shows all details)
    $0 --target v1.0.0 --source main --full

EOF
    exit 1
}

# Parse command line arguments
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --target)
                TARGET_BRANCH="$2"
                shift 2
                ;;
            --source)
                SOURCE_BRANCH="$2"
                shift 2
                ;;
            --module)
                MODULE_PATH="$2"
                shift 2
                ;;
            --verbose)
                VERBOSE=true
                shift
                ;;
            --full)
                FULL=true
                shift
                ;;
            -h|--help)
                usage
                ;;
            *)
                error_exit "Unknown option: $1"
                ;;
        esac
    done
    
    if [ -z "$TARGET_BRANCH" ] || [ -z "$SOURCE_BRANCH" ]; then
        error_exit "Both --target and --source branches must be specified"
    fi
}

# Find the Gradle project root (where settings.gradle or build.gradle is located)
find_gradle_root() {
    local current_dir="$PWD"
    
    # Check current directory first
    if [ -f "settings.gradle" ] || [ -f "build.gradle" ]; then
        echo "$PWD"
        return 0
    fi
    
    # Check if we're in a subdirectory and need to go up
    local check_dir="$current_dir"
    while [ "$check_dir" != "/" ]; do
        if [ -f "$check_dir/settings.gradle" ] || [ -f "$check_dir/build.gradle" ]; then
            echo "$check_dir"
            return 0
        fi
        check_dir=$(dirname "$check_dir")
    done
    
    # If no settings.gradle found, assume current directory
    echo "$PWD"
}

# Get repository path/URL
get_repo_path() {
    log_verbose "Detecting repository..."
    
    # Check if we're in a Git repository
    if ! git rev-parse --git-dir > /dev/null 2>&1; then
        error_exit "Not in a Git repository. Please run this script from within a Git repository."
    fi
    
    # Get the repository root directory
    set +eu  # Temporarily disable exit on error and unset variable check
    set +o pipefail
    REPO_PATH=$(git rev-parse --show-toplevel 2>/dev/null)
    local git_exit=$?
    set -eu  # Re-enable exit on error and unset variable check
    set -o pipefail
    
    # Check if git command failed or result is empty
    if [ $git_exit -ne 0 ] || [ -z "${REPO_PATH:-}" ]; then
        error_exit "Failed to determine repository root directory"
    fi
    
    log_verbose "Repository path: $REPO_PATH"
    
    log_verbose "✓ Repository detected"
}

# Clone repository to temporary directory
clone_repo() {
    local clone_dir="$1"
    local label="$2"
    
    log_verbose "Cloning repository to temporary directory ($label)..."
    
    # Clone the repository (using file:// protocol for local repos)
    # This creates a clean copy without affecting the original
    if [ -d "$clone_dir" ]; then
        rm -rf "$clone_dir"
    fi
    
    log_verbose "Cloning to: $clone_dir"
    # Use file:// protocol for local repository cloning
    local repo_url
    if [[ "$REPO_PATH" == /* ]]; then
        # Absolute path - use file:// protocol
        repo_url="file://$REPO_PATH"
    else
        # Relative path
        repo_url="file://$(cd "$REPO_PATH" && pwd)"
    fi
    
    git clone --tags "$repo_url" "$clone_dir" > /dev/null 2>&1 || error_exit "Failed to clone repository for $label"
    
    # After cloning from local repo, we need to fetch from the actual remote origin
    # to ensure all remote branches are properly available
    (
      cd "$clone_dir" || error_exit "Failed to enter clone dir for $label"
      
      # Get the actual remote origin URL from the source repo
      ACTUAL_ORIGIN=$(cd "$REPO_PATH" && git remote get-url origin 2>/dev/null || echo "")
      
      if [ -n "$ACTUAL_ORIGIN" ]; then
          log_verbose "Fetching from actual remote origin: $ACTUAL_ORIGIN"
          # Update origin to point to the actual remote
          git remote set-url origin "$ACTUAL_ORIGIN" > /dev/null 2>&1 || true
      else
          log_verbose "Using local repository as origin: $repo_url"
          git remote set-url origin "$repo_url" > /dev/null 2>&1 || true
      fi
      
      # Fetch branches and tags for origin using default refspec
      git fetch origin --prune --tags > /dev/null 2>&1 || true
    )
    
    log_verbose "✓ Repository cloned for $label"
}

# Download and setup Diffuse
setup_diffuse() {
    log_verbose "Setting up Diffuse..."
    
    # Use absolute path for cache directory (since we'll be working in temp dir)
    if [ -z "$DIFFUSE_CACHE_DIR" ]; then
        if [ -n "$REPO_PATH" ]; then
            DIFFUSE_CACHE_DIR="$REPO_PATH/$DIFFUSE_CACHE_DIR_RELATIVE"
        else
            DIFFUSE_CACHE_DIR="$PWD/$DIFFUSE_CACHE_DIR_RELATIVE"
        fi
    fi
    
    # Set the expected binary path
    DIFFUSE_BINARY="${DIFFUSE_CACHE_DIR}/diffuse-${DIFFUSE_VERSION}/bin/diffuse"
    
    # Create cache directory if it doesn't exist
    mkdir -p "$DIFFUSE_CACHE_DIR"
    
    # Check if Diffuse is already cached and valid (binary should exist and be executable)
    if [ -f "$DIFFUSE_BINARY" ] && [ -x "$DIFFUSE_BINARY" ]; then
        log_verbose "✓ Diffuse ${DIFFUSE_VERSION} found in cache"
        return 0
    fi
    
    # Check if Java is available (required for Diffuse JAR)
    if ! command -v java > /dev/null 2>&1; then
        error_exit "Java is required to run Diffuse. Please install Java."
    fi
    
    log_verbose "Downloading Diffuse ${DIFFUSE_VERSION} from GitHub..."
    
    # Download Diffuse ZIP (releases are distributed as ZIP files)
    local diffuse_zip="${DIFFUSE_CACHE_DIR}/diffuse-${DIFFUSE_VERSION}.zip"
    log_verbose "Downloading from: $DIFFUSE_URL"
    if command -v curl > /dev/null 2>&1; then
        if ! curl -L -f -o "$diffuse_zip" "$DIFFUSE_URL"; then
            error_exit "Failed to download Diffuse from $DIFFUSE_URL"
        fi
    elif command -v wget > /dev/null 2>&1; then
        if ! wget -O "$diffuse_zip" "$DIFFUSE_URL"; then
            error_exit "Failed to download Diffuse from $DIFFUSE_URL"
        fi
    else
        error_exit "Neither curl nor wget found. Please install one to download Diffuse."
    fi
    
    # Verify ZIP download was successful (ZIP should be > 100KB)
    local zip_size
    zip_size=$(stat -f%z "$diffuse_zip" 2>/dev/null || stat -c%s "$diffuse_zip" 2>/dev/null || echo "0")
    if [ "$zip_size" -lt 100000 ]; then
        error_exit "Downloaded ZIP appears corrupted (${zip_size} bytes). Expected > 100KB."
    fi
    log_verbose "Downloaded ${zip_size} bytes"
    
    # Extract JAR from ZIP
    log_verbose "Extracting JAR from ZIP..."
    if ! command -v unzip > /dev/null 2>&1; then
        error_exit "unzip is required to extract Diffuse. Please install unzip."
    fi
    
    # Extract to a temporary directory first
    local extract_dir="${DIFFUSE_CACHE_DIR}/extract"
    rm -rf "$extract_dir"
    mkdir -p "$extract_dir"
    
    if ! unzip -q "$diffuse_zip" -d "$extract_dir"; then
        error_exit "Failed to extract Diffuse ZIP"
    fi
    
    # Find the executable script in the extracted contents
    local extracted_bin
    extracted_bin=$(find "$extract_dir" -path "*/bin/diffuse" -type f | head -1)
    if [ -z "$extracted_bin" ] || [ ! -f "$extracted_bin" ]; then
        error_exit "Diffuse executable not found in downloaded ZIP"
    fi
    
    # Copy the entire diffuse directory structure to cache
    local diffuse_extracted_dir
    diffuse_extracted_dir=$(dirname "$(dirname "$extracted_bin")")
    local diffuse_cache_extracted="${DIFFUSE_CACHE_DIR}/diffuse-${DIFFUSE_VERSION}"
    
    # Remove old extracted directory if it exists
    rm -rf "$diffuse_cache_extracted"
    
    # Copy the extracted directory
    cp -r "$diffuse_extracted_dir" "$diffuse_cache_extracted"
    
    # The binary should be at bin/diffuse relative to the extracted directory
    DIFFUSE_BINARY="${diffuse_cache_extracted}/bin/diffuse"
    
    # Make sure it's executable
    chmod +x "$DIFFUSE_BINARY"
    
    # Clean up temporary extraction directory and ZIP
    rm -rf "$extract_dir" "$diffuse_zip"
    
    # Verify the binary exists and is executable
    if [ ! -f "$DIFFUSE_BINARY" ] || [ ! -x "$DIFFUSE_BINARY" ]; then
        error_exit "Diffuse binary not found or not executable at: $DIFFUSE_BINARY"
    fi
    log_verbose "Extracted Diffuse binary: $DIFFUSE_BINARY"
    
    log_verbose "✓ Diffuse ${DIFFUSE_VERSION} downloaded and ready"
}

# Run Diffuse command
run_diffuse() {
    local old_aar="$1"
    local new_aar="$2"
    
    if [ ! -f "$old_aar" ]; then
        error_exit "Target AAR not found: $old_aar"
    fi
    
    if [ ! -f "$new_aar" ]; then
        error_exit "Source AAR not found: $new_aar"
    fi
    
    # Compare AAR files directly using --aar flag
    # Filter output to show only summary tables (size differences including JAR table)
    # Skip detailed lists of classes, methods, and fields
    local temp_output
    temp_output=$(mktemp)
    local saved_log="${DIFFUSE_CACHE_DIR}/last-diffuse.log"
    
    # Run Diffuse and capture output
    # Note: Diffuse may return non-zero exit code when differences are found (which is expected)
    # So we capture the output regardless of exit code
    local diffuse_exit_code=0
    set +e  # Temporarily disable exit on error to capture exit code
    set +o pipefail
    "$DIFFUSE_BINARY" diff --aar "$old_aar" "$new_aar" > "$temp_output" 2>&1
    diffuse_exit_code=$?
    set -e  # Re-enable exit on error
    set -o pipefail
    # Save full raw log for debugging
    cp "$temp_output" "$saved_log" >/dev/null 2>&1 || true
    
    # Check if the output contains actual errors (not just differences)
    if [ $diffuse_exit_code -ne 0 ]; then
        # Always print the full Diffuse output on non-zero exit to aid debugging
        echo -e "${YELLOW}Diffuse exited with code ${diffuse_exit_code}. Full log saved at: ${saved_log}${NC}" >&2
        cat "$temp_output" >&2 || true
    fi
    
    # In verbose or full mode, show full output; otherwise filter to show only key tables
    if [ "$VERBOSE" = true ] || [ "$FULL" = true ]; then
        # Show complete unfiltered Diffuse output
        cat "$temp_output" || true
    else
        # Filter to show only the tables we care about:
        # - AAR summary table
        # - JAR summary table  
        # - AAR file list section
        # Skip: file paths, hashes, MANIFEST section, CLASSES/METHODS/FIELDS details
        set +e  # Temporarily disable exit on error for awk
        set +o pipefail
        awk '
        BEGIN { 
            in_aar_table = 0
            in_jar_table = 0
            in_aar_section = 0
            skip_until_next_section = 0
        }
        
        # Skip OLD/NEW file path lines with hashes
        /^OLD: .*\.aar \(.*bytes.*sha256:/ { next }
        /^NEW: .*\.aar \(.*bytes.*sha256:/ { next }
        
        # Skip simple OLD:/NEW: filename lines
        /^OLD: [^\/]*\.aar$/ { next }
        /^NEW: [^\/]*\.aar$/ { next }
        
        # Stop at detailed sections
        /^CLASSES:$/ { exit 0 }
        /^METHODS:$/ { exit 0 }
        /^FIELDS:$/ { exit 0 }
        
        # Detect AAR summary table start
        / AAR / && /old/ && /new/ && /diff/ { 
            in_aar_table = 1
            print
            next
        }
        
        # Detect JAR summary table start
        / JAR / && /old/ && /new/ && /diff/ { 
            in_jar_table = 1
            print
            next
        }
        
        # Continue printing AAR table until blank line
        in_aar_table == 1 {
            print
            if ($0 == "") {
                in_aar_table = 0
            }
            next
        }
        
        # Continue printing JAR table until blank line
        in_jar_table == 1 {
            print
            if ($0 == "") {
                in_jar_table = 0
            }
            next
        }
        
        # Handle section headers (=====)
        /^=+$/ { 
            # Save the separator line
            sep_line = $0
            # Read the next line to see what section it is
            if (getline > 0) {
                if ($0 ~ /====.*AAR.*====/) {
                    # Start of AAR file list section
                    in_aar_section = 1
                    skip_until_next_section = 0
                    print sep_line
                    print
                    # Read and print the next separator line (there are 2 around the header)
                    if (getline > 0 && $0 ~ /^=+$/) {
                        print
                    }
                } else if ($0 ~ /====.*MANIFEST.*====/) {
                    # Skip MANIFEST section
                    skip_until_next_section = 1
                } else if ($0 ~ /====.*JAR.*====/ && !in_aar_section) {
                    # Skip final JAR section (after MANIFEST)
                    skip_until_next_section = 1
                }
            }
            next
        }
        
        # Continue printing AAR section content
        in_aar_section == 1 {
            # Check if this is the "(total)" line (end of AAR section)
            if ($0 ~ /\(total\)/) {
                print
                in_aar_section = 0
                skip_until_next_section = 1  # Skip everything until we see CLASSES/METHODS/FIELDS or EOF
                next
            }
            print
            next
        }
        
        # Skip lines when we are skipping a section
        skip_until_next_section == 1 {
            next
        }
        ' "$temp_output" || true
        set -e  # Re-enable exit on error
        set -o pipefail
    fi
    
    # Clean up
    rm -f "$temp_output" || true
    
    # Return success (even if Diffuse found differences, that's a successful comparison)
    return 0
}

# Build AAR for a specific branch/tag/commit
build_aar() {
    local clone_dir="$1"
    local ref="$2"
    local output_aar="$3"
    
    log_verbose "Building AAR for ref: ${ref}"
    
    # Change to cloned repository directory
    cd "$clone_dir" || error_exit "Failed to change to cloned repository directory"
    
    # Verify ref exists (handles branches, tags, and commits)
    # At this point clone_repo has already fetched from origin, so the
    # ref should exist if it exists on the remote.
    if ! git rev-parse --verify "$ref" > /dev/null 2>&1; then
        error_exit "Ref does not exist in cloned repo: $ref (ensure this branch/tag/SHA exists on origin)"
    fi
    
    # Checkout the ref (works for branches, tags, and commits)
    log_verbose "Checking out ref: $ref"
    if ! git checkout "$ref" >/dev/null 2>&1; then
        # Try with -f flag for tags that might have conflicts
        if ! git checkout -f "$ref" >/dev/null 2>&1; then
            error_exit "Failed to checkout ref: $ref"
        fi
    fi
    # Log resolved commit for traceability
    if git rev-parse --short HEAD >/dev/null 2>&1; then
        RESOLVED_SHA="$(git rev-parse --short HEAD || true)"
        log_verbose "Resolved ref '$ref' to commit: $RESOLVED_SHA"
    fi
    
    # Find the Gradle project root after checkout (structure might be different)
    local gradle_root
    gradle_root=$(find_gradle_root)
    
    # Check for Gradle wrapper in the project root
    local gradlew_path
    if [ -f "$gradle_root/gradlew" ]; then
        gradlew_path="$gradle_root/gradlew"
    elif [ -f "./gradlew" ]; then
        gradlew_path="./gradlew"
    else
        error_exit "Gradle wrapper (gradlew) not found. Expected in: $gradle_root or current directory"
    fi
    
    # Make gradlew executable
    chmod +x "$gradlew_path" 2>/dev/null || true
    
    # Determine the working directory for gradle
    # If gradle root is different, we might need to run from there or use -p flag
    local gradle_cmd
    if [ "$gradle_root" != "$PWD" ]; then
        # Run from gradle root, but need to specify the project
        # For now, try running from current directory with the gradlew from root
        gradle_cmd="$gradlew_path"
        log_verbose "Using Gradle wrapper from: $gradle_root"
    else
        gradle_cmd="./gradlew"
    fi
    
    # Handle parent settings.gradle interference
    # If current directory has build.gradle but no settings.gradle, and parent has settings.gradle,
    # create a minimal settings.gradle to isolate this build
    local temp_settings_created=false
    if [ -f "build.gradle" ] && [ ! -f "settings.gradle" ]; then
        local parent_settings=$(dirname "$PWD")/settings.gradle
        if [ -f "$parent_settings" ]; then
            # Create a minimal settings.gradle to prevent Gradle from using parent
            echo "rootProject.name = 'android-client'" > "settings.gradle"
            temp_settings_created=true
            log_verbose "Created temporary settings.gradle to isolate build from parent"
        fi
    fi
    
    # Build the fused AAR using the first available Gradle task
    # Clean first to avoid cross-branch artifacts affecting selection
    # Use aggressive cleaning to ensure no cached artifacts remain
    log_verbose "Running: $gradle_cmd clean"
    if [ "$VERBOSE" = true ]; then
        "$gradle_cmd" clean --no-daemon || true
    else
        "$gradle_cmd" clean --quiet --no-daemon >/dev/null 2>&1 || true
    fi
    
    # Additional cleanup: remove build directories to ensure fresh build
    log_verbose "Removing build directories for clean slate..."
    rm -rf build/ */build/ .gradle/ */.gradle/ 2>/dev/null || true
    local build_ok=false
    local used_task=""
    for task in "${GRADLE_TASK_CANDIDATES[@]}"; do
        log_verbose "Running: $gradle_cmd $task"
        if [ "$VERBOSE" = true ]; then
            if "$gradle_cmd" "$task" --no-daemon; then
                build_ok=true
                used_task="$task"
                break
            fi
        else
            if "$gradle_cmd" "$task" --quiet --no-daemon > /dev/null 2>&1; then
                build_ok=true
                used_task="$task"
                break
            fi
        fi
    done
    if [ "$build_ok" != true ]; then
        error_exit "Failed to build AAR for ref: $ref (tried: ${GRADLE_TASK_CANDIDATES[*]})"
    fi
    log_verbose "Selected Gradle build task for ref '${ref}': $used_task"
    
    # Find the AAR file - search in multiple possible locations
    local aar_path=""
    local possible_paths=()
    # Add exact candidate names first (both relative and explicit)
    for name in "${AAR_CANDIDATE_NAMES[@]}"; do
        possible_paths+=("${AAR_OUTPUT_DIR}/${name}")
        possible_paths+=("build/outputs/aar/${name}")
    done
    # Include fused plugin bundle directory
    possible_paths+=("build/bundle/bundle.aar")
    possible_paths+=("build/bundle/*.aar")
    # Then pattern-based fallbacks (root only; try to avoid picking submodule AARs)
    possible_paths+=("build/outputs/aar/android-client-*.aar")
    possible_paths+=("build/outputs/aar/*-release.aar")
    possible_paths+=("build/outputs/aar/*.aar")
    
    # Also check relative to gradle root if different
    if [ "$gradle_root" != "$PWD" ]; then
        for name in "${AAR_CANDIDATE_NAMES[@]}"; do
            possible_paths+=("$gradle_root/${AAR_OUTPUT_DIR}/${name}")
            possible_paths+=("$gradle_root/build/outputs/aar/${name}")
        done
        possible_paths+=("$gradle_root/build/bundle/bundle.aar")
        possible_paths+=("$gradle_root/build/bundle/*.aar")
        possible_paths+=("$gradle_root/build/outputs/aar/android-client-*.aar")
        possible_paths+=("$gradle_root/build/outputs/aar/*-release.aar")
        possible_paths+=("$gradle_root/build/outputs/aar/*.aar")
    fi
    
    # Find the AAR file - priority-based selection across all locations
    log_verbose "Searching for AAR files in build directory..."
    local all_aars
    all_aars=$(find . -type f -name "*.aar" 2>/dev/null | sort || true)
    pick_first() { printf "%s\n" "$all_aars" | grep -E "$1" | head -1 || true; }
    aar_path=$(pick_first "/build/outputs/aar/android-client\\.aar$")
    [ -z "$aar_path" ] && aar_path=$(pick_first "/build/outputs/aar/android-client-release\\.aar$")
    [ -z "$aar_path" ] && aar_path=$(pick_first "/build/outputs/aar/android-client-debug\\.aar$")
    # Fused plugin bundle output
    [ -z "$aar_path" ] && aar_path=$(pick_first "/build/bundle/bundle\\.aar$")
    # Locally published fused artifact
    [ -z "$aar_path" ] && aar_path=$(pick_first "/build/publishing/.*/android-client-[^/]+\\.aar$")
    # Any remaining android-client*.aar anywhere
    [ -z "$aar_path" ] && aar_path=$(pick_first "/android-client[^/]*\\.aar$")
    # Fallback: any root build/outputs release AAR not from logger/main (handles older artifactIds like 'repo-release.aar')
    [ -z "$aar_path" ] && aar_path=$(printf "%s\n" "$all_aars" | grep -E "/build/outputs/aar/[^/]*-release\\.aar$" | grep -Ev "/(logger|main)/" | head -1 || true)
    if [ -z "$aar_path" ] || [ ! -f "$aar_path" ]; then
        echo -e "${YELLOW}Debug: Could not locate AAR. First 50 *.aar files found:${NC}"
        echo "$all_aars" | head -50 || true
        error_exit "AAR not found after build."
    fi
    
    log_verbose "Selected fused AAR artifact for ref '${ref}': $aar_path"
    
    # Copy AAR to temp location
    cp "$aar_path" "$output_aar"
    
    # Verify the AAR was copied and log its size and checksum
    local aar_size
    aar_size=$(stat -f%z "$output_aar" 2>/dev/null || stat -c%s "$output_aar" 2>/dev/null || echo "unknown")
    local aar_sha=""
    if command -v shasum >/dev/null 2>&1; then
        aar_sha=$(shasum -a 256 "$output_aar" 2>/dev/null | awk '{print $1}')
    elif command -v openssl >/dev/null 2>&1; then
        aar_sha=$(openssl dgst -sha256 "$output_aar" 2>/dev/null | awk '{print $2}')
    fi
    log_verbose "✓ AAR built and copied for ref '${ref}': $(basename "$output_aar") (${aar_size} bytes${aar_sha:+, sha256: $aar_sha})"
    
    # Clean up temporary settings.gradle if we created one
    # (This is cleaned up here, but also safe if script fails - temp dir will be removed)
    if [ "$temp_settings_created" = true ] && [ -f "settings.gradle" ]; then
        rm -f "settings.gradle"
    fi
}

# Main execution
main() {
    # Parse arguments
    parse_args "$@"
    
    # Get repository path
    get_repo_path
    
    # Setup Diffuse (use absolute path for cache since we'll be in temp dir)
    local original_dir="$PWD"
    setup_diffuse
    
    # Clone repository twice - once for each branch to ensure complete isolation
    # This prevents any build cache or Gradle state from affecting the comparison
    clone_repo "$SOURCE_CLONE_DIR" "source"
    clone_repo "$TARGET_CLONE_DIR" "target"
    
    # Build source branch AAR first (newer version)
    # This ensures we build the newer version first, which may have more dependencies
    build_aar "$SOURCE_CLONE_DIR" "$SOURCE_BRANCH" "$SOURCE_AAR"
    
    # Build target branch AAR second (older version) in separate clone directory
    build_aar "$TARGET_CLONE_DIR" "$TARGET_BRANCH" "$TARGET_AAR"
    
    # Summarize what will be compared (paths, sizes, hashes) to ensure distinct artifacts
    # Only show in verbose mode
    if [ "$VERBOSE" = true ]; then
        summarize_aar() {
            local label="$1"
            local file="$2"
            local size
            size=$(stat -f%z "$file" 2>/dev/null || stat -c%s "$file" 2>/dev/null || wc -c < "$file" 2>/dev/null || echo "unknown")
            local sha=""
            if command -v shasum >/dev/null 2>&1; then
                sha=$(shasum -a 256 "$file" 2>/dev/null | awk '{print $1}')
            elif command -v openssl >/dev/null 2>&1; then
                sha=$(openssl dgst -sha256 "$file" 2>/dev/null | awk '{print $2}')
            fi
            echo -e "${BLUE}${label}: ${NC}$file (${size} bytes${sha:+, sha256: $sha})"
        }
        summarize_aar "OLD (target)" "$TARGET_AAR"
        summarize_aar "NEW (source)" "$SOURCE_AAR"
    fi
    
    # Run Diffuse comparison
    # Note: Diffuse shows OLD -> NEW, so we pass target (old) first, then source (new)
    # We can run from any directory since we use absolute paths
    set +e  # Temporarily disable exit on error
    set +o pipefail
    run_diffuse "$TARGET_AAR" "$SOURCE_AAR"
    local run_exit=$?
    set -e  # Re-enable exit on error
    set -o pipefail
    
    # Return success (run_diffuse returns 0 on success)
    return 0
}

# Run main function
main "$@"
