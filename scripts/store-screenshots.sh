#!/usr/bin/env bash
# Takes the store screenshots (issue #58) on the one connected emulator or device with the debug app installed.
# Usage: scripts/store-screenshots.sh [out-dir]     (default: build/store-screenshots/<device>-<width>x<height>)
#
# Steps (see .maestro/store/README.md):
#   1. Gboard's only keyboard language becomes Belarusian.
#   2. The status bar goes into demo mode (10:00, full battery and Wi-Fi, no notifications).
#   3. fill.yaml: a clean install with the History and Bookmarks of the App Store shots.
#   4. shots.yaml four times: Material You light and dark, then the brand palette light and dark. The system
#      night mode is switched, not the app's theme, so the keyboard and system bars match the app.
# Demo mode and night mode are put back however the run ends.
set -euo pipefail

REPO=$(cd "$(dirname "$0")/.." && pwd)
FLOWS=$REPO/.maestro/store
MAESTRO=${MAESTRO:-$(command -v maestro || echo "$HOME/.maestro/bin/maestro")}
export MAESTRO_CLI_NO_ANALYTICS=1

if [ "$(adb devices | grep -cw device)" -ne 1 ]; then
  echo "Connect exactly one emulator or device (adb devices)." >&2
  exit 1
fi
adb shell pm path by.mazokaleh.skarnik.debug >/dev/null || { echo "Install the debug app first: ./gradlew installDebug" >&2; exit 1; }

size=$(adb shell wm size | tail -1 | grep -oE '[0-9]+x[0-9]+')
model=$(adb shell getprop ro.product.model | tr -d '\r' | tr ' ' '_')
OUT=$(realpath -m "${1:-$REPO/build/store-screenshots/$model-$size}")
RUNS=$OUT/maestro-runs
mkdir -p "$OUT"

maestro_test() {
  "$MAESTRO" test --test-output-dir "$RUNS" "$@"
}

# 1. Belarusian keyboard: Gboard adds a language only from its own settings, so a Maestro flow does it there.
# NEW_TASK | CLEAR_TASK: start on the keyboard list even if Settings or Gboard's settings were left open.
adb shell am start -W -f 0x10008000 -a android.settings.INPUT_METHOD_SETTINGS >/dev/null
maestro_test "$FLOWS/setup/belarusian-keyboard.yaml"

# 2. Demo mode.
night_before=$(adb shell cmd uimode night | tr -d '\r' | awk '{print $NF}')
demo() { adb shell am broadcast -a com.android.systemui.demo -e command "$@" >/dev/null; }
cleanup() {
  demo exit
  adb shell cmd uimode night "$night_before" >/dev/null
}
trap cleanup EXIT
adb shell settings put global sysui_demo_allowed 1
demo enter
demo clock -e hhmm 1000
demo battery -e level 100 -e plugged false
demo network -e wifi show -e level 4 -e fully true
demo network -e mobile hide
demo notifications -e visible false

# 3. History and Bookmarks.
maestro_test "$FLOWS/fill.yaml"

# 4. Four passes.
shoot() {
  adb shell cmd uimode night "$2" >/dev/null
  maestro_test -e PASS="$1" "$FLOWS/shots.yaml"
}
shoot you-light no
shoot you-dark yes
maestro_test "$FLOWS/brand-palette.yaml"
shoot brand-light no
shoot brand-dark yes

find "$RUNS" -path '*/takeScreenshot/*.png' -exec cp {} "$OUT" \;
echo "Screenshots: $OUT"
