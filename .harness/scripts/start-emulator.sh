#!/usr/bin/env bash
set -euo pipefail

echo "========================================"
echo "  Starting Android Emulator"
echo "========================================"
echo ""

# Update PATH to include Android tools
export PATH="$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin"

# Verify required tools are available
if ! command -v avdmanager >/dev/null 2>&1; then
  echo "ERROR: avdmanager not found in PATH"
  exit 1
fi

if ! command -v emulator >/dev/null 2>&1; then
  echo "ERROR: emulator not found in PATH"
  exit 1
fi

# Use x86_64 system image for AMD64 architecture
AVD_NAME="test_device_api29"
SYSTEM_IMAGE="system-images;android-29;default;x86_64"

echo "Creating AVD: $AVD_NAME"
echo "System image: $SYSTEM_IMAGE"
echo ""

# Create AVD (force recreate if exists)
echo no | avdmanager create avd \
  -n "$AVD_NAME" \
  -k "$SYSTEM_IMAGE" \
  --force \
  --device "pixel_5"

echo "✓ AVD created successfully"
echo ""

# Verify AVD was created
echo "Verifying AVD creation..."
echo "HOME: $HOME"
echo "Contents of $HOME/.android/avd/:"
ls -la "$HOME/.android/avd/" || echo "AVD directory does not exist!"

AVD_INI_FILE="$HOME/.android/avd/${AVD_NAME}.ini"
AVD_CONFIG_PATH="$HOME/.android/avd/${AVD_NAME}.avd/config.ini"

if [ ! -f "$AVD_INI_FILE" ]; then
  echo "ERROR: AVD ini file not found at: $AVD_INI_FILE"
  echo "Listing available AVDs:"
  avdmanager list avd
  exit 1
fi

echo "✓ AVD ini file found: $AVD_INI_FILE"
echo "Contents:"
cat "$AVD_INI_FILE"
echo ""

# Configure AVD for CI environment (disable animations, reduce resources)
if [ -f "$AVD_CONFIG_PATH" ]; then
  echo "Optimizing AVD configuration for CI..."
  # Increase RAM and heap for better performance
  echo "hw.ramSize=2048" >> "$AVD_CONFIG_PATH"
  echo "vm.heapSize=256" >> "$AVD_CONFIG_PATH"
  echo "✓ AVD configuration optimized"
else
  echo "WARNING: AVD config file not found at: $AVD_CONFIG_PATH"
fi

echo ""
echo "Starting emulator in background..."
echo ""

# Start emulator with CI-friendly flags
nohup emulator -avd "$AVD_NAME" \
  -no-window \
  -no-audio \
  -no-boot-anim \
  -gpu swiftshader_indirect \
  -no-snapshot \
  -wipe-data \
  -camera-back none \
  -camera-front none \
  > /tmp/emulator.log 2>&1 &

EMULATOR_PID=$!
echo "Emulator started with PID: $EMULATOR_PID"
echo ""

# Wait for emulator to be detected by adb
echo "Waiting for emulator to be detected by adb..."
TIMEOUT=300  # 5 minutes timeout
ELAPSED=0
until adb devices | grep -q "emulator"; do
  if [ $ELAPSED -ge $TIMEOUT ]; then
    echo "ERROR: Emulator not detected after ${TIMEOUT}s"
    echo "Last 50 lines of emulator log:"
    tail -50 /tmp/emulator.log
    exit 1
  fi
  sleep 2
  ELAPSED=$((ELAPSED + 2))
  echo -n "."
done

echo ""
echo "✓ Emulator detected by adb"
echo ""

# Wait for device to be ready
echo "Waiting for device to boot (this may take a few minutes)..."
adb wait-for-device

# Wait for boot to complete
BOOT_TIMEOUT=600  # 10 minutes timeout
BOOT_ELAPSED=0
until adb shell getprop sys.boot_completed 2>/dev/null | grep -q "1"; do
  if [ $BOOT_ELAPSED -ge $BOOT_TIMEOUT ]; then
    echo "ERROR: Device boot not completed after ${BOOT_TIMEOUT}s"
    echo "Last 50 lines of emulator log:"
    tail -50 /tmp/emulator.log
    adb shell getprop
    exit 1
  fi
  sleep 5
  BOOT_ELAPSED=$((BOOT_ELAPSED + 5))
  echo -n "."
done

echo ""
echo "✓ Device boot completed"
echo ""

# Disable animations for faster test execution
echo "Disabling animations..."
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
echo "✓ Animations disabled"
echo ""

# Show device info
echo "Device information:"
echo "  API Level: $(adb shell getprop ro.build.version.sdk)"
echo "  Model: $(adb shell getprop ro.product.model)"
echo "  Android Version: $(adb shell getprop ro.build.version.release)"
echo ""

echo "========================================"
echo "  ✓ Emulator Ready for Testing"
echo "========================================"
