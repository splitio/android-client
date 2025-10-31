#!/usr/bin/env bash
set -euo pipefail

echo "========================================"
echo "  Android Platform Dependencies Setup"
echo "========================================"
echo ""

# ============================================
# 1. Install Java 17
# ============================================
echo "=== Installing Java 17 ==="

# Install Java 17
echo "Installing OpenJDK 17..."
sudo apt-get update -qq
sudo apt-get install -y openjdk-17-jdk

# Set JAVA_HOME to Java 17 explicitly
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# Update alternatives to use Java 17 as default
sudo update-alternatives --set java /usr/lib/jvm/java-17-openjdk-amd64/bin/java
sudo update-alternatives --set javac /usr/lib/jvm/java-17-openjdk-amd64/bin/javac

echo ""
echo "Java 17 installation complete:"
java -version
echo "JAVA_HOME: $JAVA_HOME"

echo "✓ Java 17 installation complete"
echo ""

# ============================================
# 2. Setup Android SDK Tools
# ============================================
echo "=== Setting up Android SDK Tools ==="

# Ensure SDK directory exists
mkdir -p "$ANDROID_HOME"

# Look for command-line tools in common locations
CMDLINE_TOOLS_BIN=""
if [ -d "$ANDROID_HOME/cmdline-tools/latest/bin" ]; then
  CMDLINE_TOOLS_BIN="$ANDROID_HOME/cmdline-tools/latest/bin"
  echo "✓ Found command-line tools at: $CMDLINE_TOOLS_BIN"
elif [ -d "$ANDROID_HOME/tools/bin" ]; then
  CMDLINE_TOOLS_BIN="$ANDROID_HOME/tools/bin"
  echo "✓ Found legacy tools at: $CMDLINE_TOOLS_BIN"
else
  echo "⚠ Command-line tools not found, installing..."
  cd "$ANDROID_HOME"
  
  # Download Linux command-line tools for AMD64
  CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
  
  echo "Downloading from: $CMDLINE_TOOLS_URL"
  curl -fSL -o cmdline-tools.zip "$CMDLINE_TOOLS_URL"
  
  # Extract and organize
  unzip -q cmdline-tools.zip
  mkdir -p cmdline-tools/latest
  mv cmdline-tools/* cmdline-tools/latest/ 2>/dev/null || true
  rm cmdline-tools.zip
  
  CMDLINE_TOOLS_BIN="$ANDROID_HOME/cmdline-tools/latest/bin"
  echo "✓ Installed command-line tools at: $CMDLINE_TOOLS_BIN"
fi

# Update PATH to include all Android tools
export PATH="$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$CMDLINE_TOOLS_BIN"

# Test that sdkmanager is accessible
echo ""
echo "Testing SDK Manager access..."
sdkmanager --version

echo "✓ Android SDK tools setup complete"
echo ""

# ============================================
# 3. Install Android Components
# ============================================
echo "=== Installing required Android components ==="

# Update PATH to include Android tools
if [ -d "$ANDROID_HOME/cmdline-tools/latest/bin" ]; then
  export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin"
elif [ -d "$ANDROID_HOME/tools/bin" ]; then
  export PATH="$PATH:$ANDROID_HOME/tools/bin"
fi
export PATH="$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator"

# Verify SDK Manager is accessible
if ! command -v sdkmanager >/dev/null 2>&1; then
  echo "ERROR: SDK Manager not found in PATH"
  echo "PATH: $PATH"
  exit 1
fi

echo "SDK Manager version: $(sdkmanager --version)"
echo ""

# Accept licenses first
echo "Accepting Android SDK licenses..."
yes | sdkmanager --licenses 2>&1 || echo "Licenses already accepted"
echo ""

# Get list of installed packages
INSTALLED_PACKAGES=$(sdkmanager --list_installed 2>/dev/null)

# Install platform-tools
if ! echo "$INSTALLED_PACKAGES" | grep -q "platform-tools"; then
  echo "Installing platform-tools..."
  sdkmanager "platform-tools"
else
  echo "✓ platform-tools already installed"
fi

# Install build-tools
if ! echo "$INSTALLED_PACKAGES" | grep -q "build-tools;"; then
  echo "Installing build-tools..."
  sdkmanager "build-tools;34.0.0"
else
  echo "✓ build-tools already installed"
fi

# Install Android platform
if ! echo "$INSTALLED_PACKAGES" | grep -q "platforms;android-29"; then
  echo "Installing Android platform 29..."
  sdkmanager "platforms;android-29"
else
  echo "✓ Android platform 29 already installed"
fi

# Install x86_64 system image for AMD64 architecture
SYSTEM_IMAGE="system-images;android-29;default;x86_64"
echo ""
echo "Using system image: $SYSTEM_IMAGE"
if ! echo "$INSTALLED_PACKAGES" | grep -q "$SYSTEM_IMAGE"; then
  echo "Installing system image: $SYSTEM_IMAGE"
  yes | sdkmanager "$SYSTEM_IMAGE" 2>&1 || true
else
  echo "✓ System image already installed: $SYSTEM_IMAGE"
fi

# Install/update emulator (force update to ensure we have a working version)
echo "Installing/updating Android emulator..."
yes | sdkmanager "emulator" 2>&1 || true

# Verify emulator binary is accessible and working
export PATH="$PATH:$ANDROID_HOME/emulator"
if ! command -v emulator >/dev/null 2>&1; then
  echo "ERROR: emulator binary not found after installation"
  echo "Contents of ANDROID_HOME/emulator:"
  ls -la "$ANDROID_HOME/emulator" || echo "Directory does not exist"
  exit 1
fi

# Test emulator can show version (quick check it's not corrupted)
echo "Testing emulator binary..."
if emulator -version 2>&1 | head -1 | grep -q "emulator"; then
  echo "✓ Emulator binary is working:"
  emulator -version | head -3
else
  echo "WARNING: Emulator binary may have issues"
  emulator -version 2>&1 | head -5 || echo "Failed to get emulator version"
fi

echo ""
echo "Refreshing SDK package cache..."
# Force package list refresh so avdmanager recognizes newly installed packages
sdkmanager --list > /dev/null 2>&1 || echo "Warning: Failed to refresh package list"

echo ""
echo "=== Android SDK components installed ==="
echo ""
echo "Installed packages:"
sdkmanager --list_installed

echo ""
echo "========================================"
echo "  ✓ Android Dependencies Complete"
echo "========================================"
