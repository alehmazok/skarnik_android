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
