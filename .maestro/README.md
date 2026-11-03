# Maestro flows

End-to-end flows that drive the real app on an emulator or device ([ADR 0002](../docs/adr/0002-maestro-end-to-end-tests.md)).

```bash
./gradlew installDebug
maestro test .maestro/                # all flows
maestro test .maestro/smoke.yaml      # one flow
```

Rules:
- One user journey per file, named after it (`search-and-open-translation.yaml`). Tag fast flows with `smoke`.
- Select elements by Compose `testTag` (exposed as resource id via `testTagsAsResourceId`) or by visible
  Belarusian text; never by coordinates.
- Flows start from `clearState: true` unless the flow is about kept state (upgrade, history).
- `takeScreenshot` at the end state of each journey; CI uploads them as an artifact.
- Steps shared by several flows live in `subflows/` (not run on their own) and take parameters through `env`.
- `wide/` holds flows for an expanded window (navigation rail, word beside the list). They are not run with the
  rest: resize the device first (`adb shell wm size 1280x800 && adb shell wm density 160`), then
  `maestro test .maestro/wide/`; reset with `wm size reset && wm density reset`.
- `app-links.yaml` needs the link hosts approved for the debug app once after installing (CI does it):
  `adb shell pm set-app-links-user-selection --user 0 --package by.mazokaleh.skarnik.debug true all`.
- CI runs the flows through `.github/scripts/maestro-flows.sh`: one Maestro session per flow, so an emulator
  that drops off adb for a moment costs one flow, not all later ones. A flow that failed because the device went
  away runs once more (listed in the job summary); a failed assertion is never retried.
