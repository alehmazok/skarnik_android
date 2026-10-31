#!/usr/bin/env bash
# Installs the release APK, cold-starts it and fails if the process dies or logs a crash.
# Usage: release-smoke.sh <apk>
set -uo pipefail

APP=by.mazokaleh.skarnik
APK=$1

adb install -r "$APK" || exit 1
adb logcat -c
adb shell am start -W -n "$APP/.MainActivity"
sleep 20

PID=$(adb shell pidof "$APP" | tr -d '\r')
adb logcat -d -b main,system,crash -v time > release-logcat.txt
if [ -z "$PID" ] || grep -q "FATAL EXCEPTION" release-logcat.txt; then
  echo "::error::Release build is not running 20 s after launch"
  grep -A 60 "FATAL EXCEPTION" release-logcat.txt || grep -E "AndroidRuntime|$APP" release-logcat.txt | tail -80
  exit 1
fi
echo "Release build running (pid $PID)"
