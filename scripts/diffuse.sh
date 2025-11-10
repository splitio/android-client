#!/bin/bash

# API Diff Tool
# Compares public API surface between two Git branches using Diffuse
# Usage: ./scripts/compare-api.sh --target <target-branch> --source <source-branch>

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

# AAR configuration
AAR_OUTPUT_DIR="build/outputs/aar"
AAR_NAME="android-client-release.aar"
GRADLE_TASK="assembleRelease"

# Temporary files
TEMP_DIR=$(mktemp -d)
CLONE_DIR="${TEMP_DIR}/repo"
TARGET_AAR="${TEMP_DIR}/target-branch.aar"
SOURCE_AAR="${TEMP_DIR}/source-branch.aar"

# Script arguments
TARGET_BRANCH=""
SOURCE_BRANCH=""
MODULE_PATH=""
REPO_PATH=""

# Cleanup function - runs on exit, error, or interrupt
cleanup() {
    local exit_code=$?
    
    # Always cleanup, even on success
    if [ -d "$TEMP_DIR" ]; then
        echo -e "\n${BLUE}Cleaning up temporary files...${NC}"
        rm -rf "$TEMP_DIR"
        echo "Removed temporary repository clone and AAR files"
    fi
    
    # Only show error message if there was an error
    if [ $exit_code -ne 0 ]; then
        echo -e "${RED}Script failed with exit code $exit_code${NC}"
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

# Print usage
usage() {
    cat << EOF
Usage: $0 --target <target-ref> --source <source-ref> [--module <path>]

Options:
    --target <ref>       Target branch/tag/commit to compare against (e.g., main, origin/main, v1.0.0)
    --source <ref>       Source branch/tag/commit to compare (e.g., feature/my-feature, v1.1.0)
    --module <path>      Optional: Module path (defaults to root, future-proof for multi-module)

Examples:
    # Compare branches
    $0 --target origin/main --source feature/my-feature
    
    # Compare tags
    $0 --target v1.0.0 --source v1.1.0
    
    # Compare tag to branch
    $0 --target v1.0.0 --source main

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
    echo -e "${BLUE}Detecting repository...${NC}"
    
    # Check if we're in a Git repository
    if ! git rev-parse --git-dir > /dev/null 2>&1; then
        error_exit "Not in a Git repository. Please run this script from within a Git repository."
    fi
    
    # Get the repository root directory
    REPO_PATH=$(git rev-parse --show-toplevel)
    echo "Repository path: $REPO_PATH"
    
    echo -e "${GREEN}✓${NC} Repository detected"
}

# Clone repository to temporary directory
clone_repo() {
    echo -e "${BLUE}Cloning repository to temporary directory...${NC}"
    
    # Clone the repository (using file:// protocol for local repos)
    # This creates a clean copy without affecting the original
    if [ -d "$CLONE_DIR" ]; then
        rm -rf "$CLONE_DIR"
    fi
    
    echo "Cloning to: $CLONE_DIR"
    # Use file:// protocol for local repository cloning
    local repo_url
    if [[ "$REPO_PATH" == /* ]]; then
        # Absolute path - use file:// protocol
        repo_url="file://$REPO_PATH"
    else
        # Relative path
        repo_url="file://$(cd "$REPO_PATH" && pwd)"
    fi
    
    # Clone with tags to ensure all tags are available
    git clone --tags "$repo_url" "$CLONE_DIR" > /dev/null 2>&1 || error_exit "Failed to clone repository"
    
    # Fetch all remotes and tags from the original repo (in case of remote refs)
    (cd "$CLONE_DIR" && git remote set-url origin "$repo_url" > /dev/null 2>&1 || true)
    (cd "$CLONE_DIR" && git fetch --all --tags --prune > /dev/null 2>&1 || true)
    
    # Also fetch tags directly from the original repo to ensure we have all local tags
    (cd "$CLONE_DIR" && git fetch "$repo_url" "+refs/tags/*:refs/tags/*" > /dev/null 2>&1 || true)
    
    echo -e "${GREEN}✓${NC} Repository cloned"
}

# Download and setup Diffuse
setup_diffuse() {
    echo -e "${BLUE}Setting up Diffuse...${NC}"
    
    # Use absolute path for cache directory (since we'll be working in temp dir)
    if [ -z "$DIFFUSE_CACHE_DIR" ]; then
        if [ -n "$REPO_PATH" ]; then
            DIFFUSE_CACHE_DIR="$REPO_PATH/$DIFFUSE_CACHE_DIR_RELATIVE"
        else
            DIFFUSE_CACHE_DIR="$PWD/$DIFFUSE_CACHE_DIR_RELATIVE"
        fi
        DIFFUSE_JAR="$DIFFUSE_CACHE_DIR/diffuse-${DIFFUSE_VERSION}.jar"
        DIFFUSE_BINARY="$DIFFUSE_CACHE_DIR/diffuse-${DIFFUSE_VERSION}"
    fi
    
    # Create cache directory if it doesn't exist
    mkdir -p "$DIFFUSE_CACHE_DIR"
    
    # Set the expected binary path
    DIFFUSE_BINARY="${DIFFUSE_CACHE_DIR}/diffuse-${DIFFUSE_VERSION}/bin/diffuse"
    
    # Check if Diffuse is already cached and valid (binary should exist and be executable)
    if [ -f "$DIFFUSE_BINARY" ] && [ -x "$DIFFUSE_BINARY" ]; then
        echo -e "${GREEN}✓${NC} Diffuse ${DIFFUSE_VERSION} found in cache"
        return 0
    fi
    
    # Check if Java is available (required for Diffuse JAR)
    if ! command -v java > /dev/null 2>&1; then
        error_exit "Java is required to run Diffuse. Please install Java."
    fi
    
    echo "Downloading Diffuse ${DIFFUSE_VERSION} from GitHub..."
    
    # Download Diffuse ZIP (releases are distributed as ZIP files)
    local diffuse_zip="${DIFFUSE_CACHE_DIR}/diffuse-${DIFFUSE_VERSION}.zip"
    echo "Downloading from: $DIFFUSE_URL"
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
    echo "Downloaded ${zip_size} bytes"
    
    # Extract JAR from ZIP
    echo "Extracting JAR from ZIP..."
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
    echo "Extracted Diffuse binary: $DIFFUSE_BINARY"
    
    echo -e "${GREEN}✓${NC} Diffuse ${DIFFUSE_VERSION} downloaded and ready"
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
    
    echo -e "\n${BLUE}Running Diffuse comparison...${NC}"
    echo "OLD: $(basename "$old_aar")"
    echo "NEW: $(basename "$new_aar")"
    echo ""
    
    # Compare AAR files directly using --aar flag
    # Filter output to show only summary tables (size differences)
    # Skip detailed lists of classes, methods, and fields
    local temp_output
    temp_output=$(mktemp)
    
    # Run Diffuse and capture output
    if ! "$DIFFUSE_BINARY" diff --aar "$old_aar" "$new_aar" > "$temp_output" 2>&1; then
        # If Diffuse fails, show the error
        cat "$temp_output"
        rm -f "$temp_output"
        error_exit "Diffuse comparison failed"
    fi
    
    # Filter to show only summary/statistics, skip detailed lists
    awk '
    # Print everything until we hit CLASSES:, METHODS:, or FIELDS: sections
    /^CLASSES:$/ { exit 0 }
    /^METHODS:$/ { exit 0 }
    /^FIELDS:$/ { exit 0 }
    # Print all other lines (headers, size info, summary tables)
    { print }
    ' "$temp_output"
    
    # Clean up
    rm -f "$temp_output"
}


# Build AAR for a specific branch/tag/commit
build_aar() {
    local ref="$1"
    local output_aar="$2"
    
    echo -e "\n${BLUE}Building AAR for ref: ${ref}${NC}"
    
    # Change to cloned repository directory
    cd "$CLONE_DIR" || error_exit "Failed to change to cloned repository directory"
    
    # Verify ref exists (handles branches, tags, and commits)
    # First try to verify directly
    if ! git rev-parse --verify "$ref" > /dev/null 2>&1; then
        # If that fails, try fetching the ref from origin
        echo "Ref not found locally, attempting to fetch..."
        git fetch origin "$ref:$ref" > /dev/null 2>&1 || true
        # Also try fetching as a tag
        if [[ "$ref" =~ ^[0-9] ]]; then
            git fetch origin "refs/tags/$ref:refs/tags/$ref" > /dev/null 2>&1 || true
        fi
        # Try again
        if ! git rev-parse --verify "$ref" > /dev/null 2>&1; then
            # List available tags for debugging
            echo -e "${YELLOW}Available tags:${NC}"
            git tag | grep -E "^5\.[34]" | head -10 || git tag | tail -10
            error_exit "Ref does not exist: $ref (must be a branch, tag, or commit)"
        fi
    fi
    
    # Checkout the ref (works for branches, tags, and commits)
    echo "Checking out ref: $ref"
    if ! git checkout "$ref" 2>/dev/null; then
        # Try with -f flag for tags that might have conflicts
        if ! git checkout -f "$ref" 2>/dev/null; then
            error_exit "Failed to checkout ref: $ref"
        fi
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
        echo "Using Gradle wrapper from: $gradle_root"
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
            echo "Created temporary settings.gradle to isolate build from parent"
        fi
    fi
    
    # Build the AAR
    echo "Running: $gradle_cmd $GRADLE_TASK"
    if ! "$gradle_cmd" "$GRADLE_TASK" --quiet --no-daemon; then
        error_exit "Failed to build AAR for ref: $ref"
    fi
    
    # Find the AAR file - search in multiple possible locations
    local aar_path=""
    local possible_paths=(
        "${AAR_OUTPUT_DIR}/${AAR_NAME}"
        "build/outputs/aar/${AAR_NAME}"
        "build/outputs/aar/*-release.aar"
        "build/outputs/aar/*.aar"
    )
    
    # Also check relative to gradle root if different
    if [ "$gradle_root" != "$PWD" ]; then
        possible_paths+=(
            "$gradle_root/${AAR_OUTPUT_DIR}/${AAR_NAME}"
            "$gradle_root/build/outputs/aar/${AAR_NAME}"
            "$gradle_root/build/outputs/aar/*-release.aar"
            "$gradle_root/build/outputs/aar/*.aar"
        )
    fi
    
    # Try to find the AAR file
    for path_pattern in "${possible_paths[@]}"; do
        # Handle glob patterns
        if [[ "$path_pattern" == *"*"* ]]; then
            # Use find to locate AAR files matching the pattern
            local found_aar
            found_aar=$(find . -path "$path_pattern" -type f 2>/dev/null | head -1)
            if [ -n "$found_aar" ] && [ -f "$found_aar" ]; then
                aar_path="$found_aar"
                break
            fi
        else
            if [ -f "$path_pattern" ]; then
                aar_path="$path_pattern"
                break
            fi
        fi
    done
    
    # If still not found, try a broader search
    if [ -z "$aar_path" ]; then
        echo "Searching for AAR files in build directory..."
        local found_aars
        found_aars=$(find . -path "*/build/outputs/aar/*.aar" -type f 2>/dev/null)
        if [ -n "$found_aars" ]; then
            # Prefer release AAR if available
            aar_path=$(echo "$found_aars" | grep -i release | head -1)
            if [ -z "$aar_path" ]; then
                # Otherwise take the first one found
                aar_path=$(echo "$found_aars" | head -1)
            fi
        fi
    fi
    
    if [ -z "$aar_path" ] || [ ! -f "$aar_path" ]; then
        echo -e "${YELLOW}Debug: Listing build outputs...${NC}"
        find . -path "*/build/outputs/aar/*" -type f 2>/dev/null | head -10 || true
        error_exit "AAR not found after build. Searched in: ${possible_paths[*]}"
    fi
    
    echo "Found AAR at: $aar_path"
    
    # Copy AAR to temp location
    cp "$aar_path" "$output_aar"
    echo -e "${GREEN}✓${NC} AAR built and copied: $(basename "$output_aar")"
    
    # Clean up temporary settings.gradle if we created one
    # (This is cleaned up here, but also safe if script fails - temp dir will be removed)
    if [ "$temp_settings_created" = true ] && [ -f "settings.gradle" ]; then
        rm -f "settings.gradle"
    fi
}

# Main execution
main() {
    echo -e "${GREEN}API Diff Tool${NC}"
    echo "=============="
    echo ""
    
    # Parse arguments
    parse_args "$@"
    
    # Get repository path
    get_repo_path
    
    # Setup Diffuse (use absolute path for cache since we'll be in temp dir)
    local original_dir="$PWD"
    setup_diffuse
    
    # Clone repository to temporary directory
    clone_repo
    
    # Build source branch AAR first (newer version)
    # This ensures we build the newer version first, which may have more dependencies
    build_aar "$SOURCE_BRANCH" "$SOURCE_AAR"
    
    # Build target branch AAR second (older version)
    build_aar "$TARGET_BRANCH" "$TARGET_AAR"
    
    # Run Diffuse comparison
    # Note: Diffuse shows OLD -> NEW, so we pass target (old) first, then source (new)
    # We can run from any directory since we use absolute paths
    run_diffuse "$TARGET_AAR" "$SOURCE_AAR"
    
    echo -e "\n${GREEN}✓${NC} API comparison completed successfully"
}

# Run main function
main "$@"

