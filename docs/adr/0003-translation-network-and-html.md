# 0003. Translation: plain OkHttp, PostgREST without an SDK, HTML rendered natively with jsoup

**Status:** Accepted (2026-10-30)

## Context

The translation screen fetches one entry by id from the skarnik_admin API and, when that fails, from Supabase
`main_word`, caches responses for 24 hours (Flutter: Dio + `dio_http_cache_lts`, lifetime from Remote Config),
and renders the entry's legacy HTML: `<font color>`, `<font size="+2">`, `<b>`, `<i>`, `<br>`, `&nbsp;`, and
`<a href>` links to other words. Colours must follow the M3 colour scheme, not fixed hex.

Options considered:

- **HTTP.** Retrofit (+ converter) or Ktor on OkHttp, or OkHttp alone. There are exactly two GET requests,
  each a URL and a JSON decode.
- **Supabase.** The `supabase-kt` SDK (brings Ktor, its engines and plugins) or its REST endpoint (PostgREST)
  called directly, as the iOS app does.
- **HTML.** A WebView (heavy per screen, no Compose text selection or theming), `AnnotatedString.fromHtml`
  (Android's `Html.fromHtml` under the hood: not testable on the JVM, no hook to remap colours before spans
  are built), or parsing with jsoup and building an `AnnotatedString` ourselves.

## Decision

- **OkHttp only**, one app-wide client with a 20 MB disk cache and a network interceptor that makes successful
  GETs fresh for 24 h (Remote Config takes over the lifetime in parity 8.8). Calls are suspend functions over
  `enqueue`, so cancelling the screen cancels the request. kotlinx.serialization decodes the JSON. No Retrofit:
  for two fixed GETs it would only forward calls.
- **Supabase over PostgREST** with the publishable key in the `apikey`/`Authorization` headers, filtered by
  `external_id` and `direction`. No SDK until a feature needs auth or realtime (none is planned).
- **jsoup → `AnnotatedString`.** Legacy colours map to roles (headwords → `primary`, grammar labels →
  `tertiary`, black → `onSurface`, greys → `onSurfaceVariant`, white spacers stay invisible); unknown colours
  render as plain text. Links become `LinkAnnotation.Clickable`: word links open in the app, other http(s) links
  in the browser. The text sits in a `SelectionContainer`, as Flutter's `SelectionArea`.

## Consequences

- No WebView, Ktor or Supabase SDK in the APK; jsoup (~450 KB before R8) is the only new parser.
- The renderer is a pure function, unit-tested on the JVM with real API samples.
- Offline dictionaries (parity 5.2) page through the same PostgREST endpoint with the same client.
- The admin API host, Supabase URL and publishable key are public build config in `gradle.properties` (they ship
  in every app build and in the iOS repo); override them with `-P` for a staging backend.
