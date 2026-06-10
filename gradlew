#!/usr/bin/env sh
set -eu

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROPS_FILE="$APP_HOME/gradle/wrapper/gradle-wrapper.properties"

if [ ! -f "$PROPS_FILE" ]; then
  echo "Missing $PROPS_FILE" >&2
  exit 1
fi

DIST_URL=$(grep '^distributionUrl=' "$PROPS_FILE" | cut -d= -f2-)
DIST_VERSION=$(printf '%s' "$DIST_URL" | sed -n 's#.*/gradle-\([0-9.]*\)-bin\.zip#\1#p')

if [ -z "$DIST_VERSION" ]; then
  echo "Unable to read Gradle version from $PROPS_FILE" >&2
  exit 1
fi

CACHE_DIR="$APP_HOME/.gradle-wrapper/gradle-$DIST_VERSION"
ZIP_FILE="$CACHE_DIR/gradle-$DIST_VERSION-bin.zip"
UNPACK_DIR="$CACHE_DIR/unpacked"
GRADLE_EXE="$UNPACK_DIR/gradle-$DIST_VERSION/bin/gradle"

if [ ! -x "$GRADLE_EXE" ]; then
  mkdir -p "$CACHE_DIR"

  if [ ! -f "$ZIP_FILE" ]; then
    if command -v curl >/dev/null 2>&1; then
      curl -fsSL "$DIST_URL" -o "$ZIP_FILE"
    elif command -v wget >/dev/null 2>&1; then
      wget -O "$ZIP_FILE" "$DIST_URL"
    else
      echo "curl or wget is required to download Gradle" >&2
      exit 1
    fi
  fi

  rm -rf "$UNPACK_DIR"
  mkdir -p "$UNPACK_DIR"
  unzip -q "$ZIP_FILE" -d "$UNPACK_DIR"
fi

exec "$GRADLE_EXE" "$@"
