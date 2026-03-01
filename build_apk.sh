#!/bin/bash
# Build script for Cosmic Weather Forecast APK
# Requirements: Android SDK (ANDROID_SDK_ROOT must be set or at /usr/local/lib/android/sdk)
#               kotlinc (Kotlin compiler)

set -e

REPO_DIR="$(cd "$(dirname "$0")" && pwd)"
ANDROID_SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-/usr/local/lib/android/sdk}}"
BUILD_TOOLS_VERSION="${BUILD_TOOLS_VERSION:-34.0.0}"
BUILD_TOOLS="$ANDROID_SDK/build-tools/$BUILD_TOOLS_VERSION"
PLATFORM="$ANDROID_SDK/platforms/android-34/android.jar"
KOTLIN_HOME="${KOTLIN_HOME:-$(dirname $(dirname $(which kotlinc 2>/dev/null || echo "/usr/share/kotlinc/bin/kotlinc")))}"

SRC_DIR="$REPO_DIR/app/src/main/java"
RES_DIR="$REPO_DIR/app/src/main/res"
MANIFEST="$REPO_DIR/app/src/main/AndroidManifest.xml"
OUT_DIR="$REPO_DIR/build_output"

CLASSES_DIR="$OUT_DIR/classes"
DEX_DIR="$OUT_DIR/dex"
KEYSTORE="$OUT_DIR/debug.keystore"
APK_OUT="$REPO_DIR/release/CosmicWeather-debug.apk"

echo "=== Cosmic Weather Forecast Build ==="
echo "SDK: $ANDROID_SDK"
echo "Build Tools: $BUILD_TOOLS_VERSION"
echo ""

# Validate tools
for tool in "$BUILD_TOOLS/aapt2" "$BUILD_TOOLS/d8" "$BUILD_TOOLS/apksigner"; do
    [ -f "$tool" ] || { echo "ERROR: $tool not found"; exit 1; }
done
command -v kotlinc > /dev/null || { echo "ERROR: kotlinc not found in PATH"; exit 1; }

mkdir -p "$CLASSES_DIR" "$DEX_DIR" "$OUT_DIR/compiled_res" "$OUT_DIR/gen" "$(dirname $APK_OUT)"

echo "--- [1/6] Compiling resources ---"
for f in $(find "$RES_DIR" -type f -name "*.xml"); do
    "$BUILD_TOOLS/aapt2" compile "$f" -o "$OUT_DIR/compiled_res/"
done

echo "--- [2/6] Linking resources ---"
"$BUILD_TOOLS/aapt2" link \
    --manifest "$MANIFEST" \
    -I "$PLATFORM" \
    -o "$OUT_DIR/app-resources.apk" \
    --java "$OUT_DIR/gen" \
    --min-sdk-version 26 \
    --target-sdk-version 34 \
    --version-code 1 \
    --version-name "1.0" \
    "$OUT_DIR/compiled_res"/*.flat

echo "--- [3/6] Compiling Kotlin ---"
KOTLIN_STDLIB="$(dirname $(which kotlinc))/../lib/kotlin-stdlib.jar"
KOTLIN_STDLIB_JDK8="$(dirname $(which kotlinc))/../lib/kotlin-stdlib-jdk8.jar"

find "$SRC_DIR" -name "*.kt" > "$OUT_DIR/sources.txt"
find "$OUT_DIR/gen" -name "*.java" >> "$OUT_DIR/sources.txt"

kotlinc \
    @"$OUT_DIR/sources.txt" \
    -classpath "$PLATFORM:$KOTLIN_STDLIB:$KOTLIN_STDLIB_JDK8" \
    -d "$CLASSES_DIR" \
    -jvm-target 17 2>&1

echo "--- [4/6] Converting to DEX ---"
"$BUILD_TOOLS/d8" \
    --classpath "$PLATFORM" \
    --min-api 26 \
    --output "$DEX_DIR" \
    $(find "$CLASSES_DIR" -name "*.class") \
    "$KOTLIN_STDLIB" \
    "$KOTLIN_STDLIB_JDK8" 2>&1

echo "--- [5/6] Packaging APK ---"
cp "$OUT_DIR/app-resources.apk" "$OUT_DIR/app-unsigned.apk"
cd "$DEX_DIR" && zip -j "$OUT_DIR/app-unsigned.apk" classes.dex && cd "$REPO_DIR"

echo "--- [6/6] Signing APK ---"
if [ ! -f "$KEYSTORE" ]; then
    keytool -genkey -v \
        -keystore "$KEYSTORE" \
        -alias androiddebugkey \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -storepass android -keypass android \
        -dname "CN=Android Debug,O=Android,C=US" 2>&1
fi

"$BUILD_TOOLS/apksigner" sign \
    --ks "$KEYSTORE" \
    --ks-pass pass:android \
    --key-pass pass:android \
    --ks-key-alias androiddebugkey \
    --out "$APK_OUT" \
    "$OUT_DIR/app-unsigned.apk"

echo ""
echo "=== BUILD SUCCESSFUL ==="
echo "APK: $APK_OUT"
ls -lh "$APK_OUT"
