# 0013. Dictionary link: where it shows is a Firebase A/B test on a Remote Config parameter

**Status:** Accepted (2026-11-16)

## Context

Aleh wants a Бел-Рус entry to link to the same headword in ТСБМ (and ТСБМ back to Бел-Рус). Two placements were
proposed: a chip under the headword (seen at once, beside «На́ціск») and a row after the translation (seen where the
reader finishes). Aleh asked for both, chosen by a Firebase Remote Config key, so an A/B test decides. skarnik_ios
reads the same key.

## Decision

- **One string parameter, `dictionary_link_placement`:** `header`, `footer` or `none` (control group: no link and
  no lookup). Before the first fetch, without an experiment, or for an unknown value the app uses `header`, so the
  feature works with nothing set up in the console. One parameter instead of a boolean per placement: an experiment
  variant sets one value, and two flags could both be on.
- **Read when a translation loads** (`RemoteConfig.dictionaryLinkPlacement`), not cached in the ViewModel, so a
  value activated by the start-up fetch applies from the next word opened.
- **Goal event `dictionary_link_clicked`** with a `placement` parameter, so the experiment and plain reports can
  compare variants. No impression event: Firebase A/B testing counts exposure by the activated variant, and an event
  for every opened word would be noise.
- The lookup is an exact `lword` match in the bundled index (one indexed query), so it works offline and adds no
  network call.

## Consequences

- Users who declined analytics still get a variant but are not counted in the experiment.
- The fetch activates at once (Flutter's behaviour, parity 8.8), so on the very first start a user may see `header`
  and then the assigned variant once the fetch lands.
- After the experiment, the console default picks the winner; the losing placement can then be removed in code.
