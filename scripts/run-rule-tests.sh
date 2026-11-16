#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
KOTLIN_BIN="${KOTLIN_BIN:-$(command -v kotlinc || true)}"
if [[ -z "$KOTLIN_BIN" ]]; then
  echo "kotlinc not found. Install Kotlin compiler or run the Android unit tests in Android Studio." >&2
  exit 1
fi
OUT="$ROOT/build/rule-smoke-tests.jar"
mkdir -p "$ROOT/build"
"$KOTLIN_BIN" "$ROOT/app/src/main/java/id/ac/um/mivvisecure/RuleBasedAnalyzer.kt" "$ROOT/tests/RuleSmokeTest.kt" -include-runtime -d "$OUT"
java -jar "$OUT"
