#!/usr/bin/env bash
# Runs Maestro flows on the CI emulator, one Maestro session per flow.
# Usage: maestro-flows.sh <out-dir> <flow.yaml>...
#
# Under load the emulator drops off adb for a few seconds ("device offline") and comes back. One Maestro session
# for all flows loses its driver on such a drop, and every later flow then fails in milliseconds. Here a drop
# costs one flow: before each flow the device must be back and booted, and a flow that failed because the device
# went away (not because an assertion failed) runs once more. Retries are listed in the step summary.
set -uo pipefail

OUT=$(realpath -m "$1")
shift
# Flows are given relative to the repo; Maestro runs from the output dir so screenshots land there.
FLOWS=()
for flow in "$@"; do FLOWS+=("$(realpath "$flow")"); done
mkdir -p "$OUT"
cd "$OUT" || exit 1

# Failures that come from losing the device or Maestro's on-device driver, not from the app.
INFRA_ERRORS="device offline|device '[^']*' not found|no devices/emulators found|DeviceServerDiedException|UNAVAILABLE|Connection refused"
LOGCAT_PATTERNS="AndroidRuntime|FATAL|lowmemorykiller|lmkd|Watchdog|adbd|DownloadDictionary|WM-WorkerWrapper|ViewModel|ObjectBox|MainActivity"
SUMMARY=${GITHUB_STEP_SUMMARY:-/dev/null}

wait_for_device() {
  if ! timeout 120 adb wait-for-device; then
    echo "::error::The emulator did not come back to adb within 2 minutes"
    return 1
  fi
  for _ in $(seq 60); do
    if [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ] && adb shell pm path android >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  echo "::error::The emulator is on adb but not booted after 2 minutes"
  return 1
}

# Streams logcat to a file, so a device that drops off adb still leaves its last log. `adb logcat` exits when the
# device goes offline, so the stream is restarted after each drop.
start_logcat() {
  if [ -z "${LOGCAT_PID:-}" ] || ! kill -0 "$LOGCAT_PID" 2>/dev/null; then
    adb logcat -b main,system,crash -v time >> logcat-live.txt 2>&1 &
    LOGCAT_PID=$!
  fi
}

latest_maestro_log() {
  ls -td ~/.maestro/tests/*/ 2>/dev/null | head -1 | sed 's|$|maestro.log|'
}

# run_flow <flow> <report-name>: 0 passed, 1 failed in the app, 2 failed because the device went away.
run_flow() {
  local flow=$1 name=$2
  maestro test "$flow" --format junit --output "report-$name.xml" > "maestro-$name.txt" 2>&1
  local status=$?
  cat "maestro-$name.txt"
  [ $status -eq 0 ] && return 0
  if grep -qE "$INFRA_ERRORS" "maestro-$name.txt" "$(latest_maestro_log)" 2>/dev/null || [ "$(adb get-state 2>/dev/null)" != device ]; then
    return 2
  fi
  return 1
}

print_diagnostics() {
  local name=$1
  echo "::group::Diagnostics for $name"
  adb devices
  tail -n 120 "$(latest_maestro_log)"
  grep -E "$LOGCAT_PATTERNS" logcat-live.txt | tail -80
  tail -40 logcat-live.txt
  adb logcat -d > "logcat-$name.txt" 2>/dev/null
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1 && adb shell cat /sdcard/ui.xml
  echo "::endgroup::"
}

failed=()
retried=()
for flow in "${FLOWS[@]}"; do
  name=$(basename "$flow" .yaml)
  wait_for_device || exit 1
  start_logcat
  echo "::group::$name"
  run_flow "$flow" "$name"
  result=$?
  echo "::endgroup::"
  if [ $result -eq 2 ]; then
    echo "::warning::$name: the emulator dropped off adb during the flow; running it once more"
    adb devices
    retried+=("$name")
    wait_for_device || exit 1
    start_logcat
    echo "::group::$name (retry)"
    run_flow "$flow" "$name"
    result=$?
    echo "::endgroup::"
  fi
  if [ $result -ne 0 ]; then
    failed+=("$name")
    print_diagnostics "$name"
  fi
done

[ -n "${LOGCAT_PID:-}" ] && kill "$LOGCAT_PID" 2>/dev/null

{
  echo "### Maestro: ${#FLOWS[@]} flows, ${#failed[@]} failed"
  [ ${#retried[@]} -gt 0 ] && echo "Ran again after the emulator dropped off adb: ${retried[*]}"
  [ ${#failed[@]} -gt 0 ] && echo "Failed: ${failed[*]}"
} | tee -a "$SUMMARY"

[ ${#failed[@]} -eq 0 ]
