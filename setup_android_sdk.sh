#!/usr/bin/env bash
# =============================================================================
# setup_android_sdk.sh
# Downloads and installs Android SDK command-line tools + required packages
# Run this ONCE after JDK 17 is installed.
# =============================================================================
set -e

SDK_DIR="$HOME/Android/Sdk"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
CMDLINE_ZIP="/tmp/cmdline-tools.zip"

echo "=== Setting up Android SDK at $SDK_DIR ==="

# 1. Create SDK directories
mkdir -p "$SDK_DIR/cmdline-tools"

# 2. Download command-line tools
echo "Downloading Android SDK command-line tools..."
wget -q --show-progress "$CMDLINE_TOOLS_URL" -O "$CMDLINE_ZIP"

# 3. Extract and rename to 'latest' (required directory name)
echo "Extracting..."
unzip -q "$CMDLINE_ZIP" -d "$SDK_DIR/cmdline-tools"
mv "$SDK_DIR/cmdline-tools/cmdline-tools" "$SDK_DIR/cmdline-tools/latest" 2>/dev/null || true
rm "$CMDLINE_ZIP"

# 4. Set environment variables
export ANDROID_HOME="$SDK_DIR"
export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools"

echo "=== Accepting licenses and installing SDK packages ==="

# Accept all licenses
yes | "$SDK_DIR/cmdline-tools/latest/bin/sdkmanager" --licenses

# Install required packages
"$SDK_DIR/cmdline-tools/latest/bin/sdkmanager" \
  "platform-tools" \
  "platforms;android-34" \
  "build-tools;34.0.0"

echo ""
echo "=== Android SDK setup complete! ==="
echo ""
echo "Add these to your ~/.bashrc or ~/.profile:"
echo "  export JAVA_HOME=\$(dirname \$(dirname \$(readlink -f \$(which java))))"
echo "  export ANDROID_HOME=\$HOME/Android/Sdk"
echo "  export PATH=\$PATH:\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools"
echo ""
echo "Then run: source ~/.bashrc && cd /home/saurabh/workspace/github/Auto_Accept && ./gradlew assembleDebug"
