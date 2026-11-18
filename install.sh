#!/bin/sh
# Builds the release APK, installs it on the connected device and launches it.
# Requires adb on PATH and a device visible to `adb devices`.
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
GRADLE="$ROOT/AndroidStudio/gradlew"

APP_ID=$(sed -n 's/^app\.id=//p' "$ROOT/app.properties" 2>/dev/null | tail -n 1)
APP_ID=${APP_ID:-com.example.penguingame}

echo "Uninstalling existing $APP_ID (ignored if not installed)..."
adb uninstall "$APP_ID" || true

echo "Building release APK..."
"$GRADLE" -p "$ROOT/AndroidStudio" assembleRelease

# Signed builds produce app-release.apk, unsigned ones app-release-unsigned.apk.
APK=$(ls "$ROOT"/AndroidStudio/app/build/outputs/apk/release/app-release*.apk 2>/dev/null | head -n 1)
if [ -z "$APK" ]; then
    echo "error: no release APK was produced" >&2
    exit 1
fi
if [ ! -f "$ROOT/signing.properties" ]; then
    echo "warning: no signing.properties, so this APK is unsigned." >&2
    echo "         Android will refuse to install it. See README.md for signing setup." >&2
fi

echo "Installing $APK..."
adb install "$APK"

echo "Launching app..."
adb shell monkey -p "$APP_ID" -c android.intent.category.LAUNCHER 1

echo "Done!"
