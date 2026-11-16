#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
if ! command -v gradle >/dev/null 2>&1; then
  echo "Gradle 8.9 tidak ditemukan. Install Android Studio/JDK 17/SDK 35 dan Gradle 8.9, atau buka folder project melalui Android Studio." >&2
  exit 1
fi
cd "$ROOT"
gradle --version
gradle assembleDebug
mkdir -p "$ROOT/output"
cp "$ROOT/app/build/outputs/apk/debug/app-debug.apk" "$ROOT/output/MIVVI-SECURE-debug.apk"
echo "APK built: $ROOT/output/MIVVI-SECURE-debug.apk"
