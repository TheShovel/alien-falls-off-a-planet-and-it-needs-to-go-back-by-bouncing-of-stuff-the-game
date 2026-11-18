#!/bin/sh
# Copies a PenguinMod project into the Android app's asset folder.
#
#   ./pack.sh game.pmp                 # project only; keeps the existing
#                                      # index.html + script.js runtime
#   ./pack.sh penguinmod-export.zip    # full Packager export; refreshes runtime
#   ./pack.sh penguinmod-export.zip extra-assets
#                                      # also merges extra-assets/* into the
#                                      # project's assets (loose files your
#                                      # project loads by name, e.g. images
#                                      # added with a custom block)
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEST="$ROOT/AndroidStudio/app/src/main/assets"

usage() {
    echo "usage: $0 <project.pmp|packager-export.zip> [extra-assets-dir]" >&2
}

if [ "$#" -lt 1 ] || [ "$#" -gt 2 ]; then
    usage
    exit 1
fi

SRC=$1
EXTRA=${2:-}

if [ ! -f "$SRC" ]; then
    echo "error: no such file: $SRC" >&2
    exit 1
fi
if [ -n "$EXTRA" ] && [ ! -d "$EXTRA" ]; then
    echo "error: extra assets directory not found: $EXTRA" >&2
    exit 1
fi

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

unzip -q -o "$SRC" -d "$WORK"

if [ -f "$WORK/index.html" ]; then
    echo "Packager export detected: replacing runtime and project assets"
    find "$DEST" -mindepth 1 ! -name .gitkeep -exec rm -rf {} +
    mkdir -p "$DEST"
    cp -R "$WORK"/. "$DEST"/
    mkdir -p "$DEST/assets"
else
    if [ ! -f "$DEST/index.html" ]; then
        echo "error: this archive has no index.html, so it is a project file." >&2
        echo "       index.html and script.js must come from a Packager export" >&2
        echo "       at least once. Export one with $ROOT/penguinmod-packager-settings.json," >&2
        echo "       then run: $0 <that-export.zip>" >&2
        exit 1
    fi
    echo "Project archive detected: replacing project files in assets/"
    rm -rf "$DEST/assets"
    mkdir -p "$DEST/assets"
    cp -R "$WORK"/. "$DEST/assets"/
fi

if [ -n "$EXTRA" ]; then
    echo "Merging extra assets from $EXTRA"
    cp -R "$EXTRA"/. "$DEST/assets"/
fi

COUNT=$(find "$DEST" -type f | wc -l | tr -d ' ')
SIZE=$(du -sh "$DEST" | cut -f1 | tr -d ' ')
echo "Done: $COUNT files ($SIZE) in $DEST"
