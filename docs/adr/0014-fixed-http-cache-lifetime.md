# 0014. HTTP cache lifetime: a 24-hour constant, not Remote Config

**Status:** Accepted (2026-11-18)

## Context

Translations and stress tables fetched over the network (Supabase, the admin API, starnik.by) go through one
OkHttp disk cache. A network interceptor marks successful GETs fresh for a fixed time, because none of these
servers sends useful cache headers. Flutter took that time from the Remote Config parameter
`http_cache_duration_in_hours` (default 24), and the first native version did the same (parity 8.8).

Downloaded dictionaries are read before any network source, so the cache only serves words a user has already
opened online. Dictionary entries change only when they are edited in skarnik_admin, which is rare, and a fix
reaching readers within a day is fine. skarnik_ios has no such parameter: it uses `URLSession.shared` with the
default cache policy.

The remote value also cannot do what an emergency switch would be for: the interceptor writes `max-age` into each
response when it is stored, so a lower value only applies to responses fetched afterwards, and entries already
cached keep their old lifetime.

## Decision

The lifetime is a constant, `24.hours` in `NetworkModule`. The app no longer reads `http_cache_duration_in_hours`.
The parameter stays in the Firebase console while the Flutter app is still installed; the native app ignores it.

## Consequences

- Same behaviour as the default before: 24 h freshness.
- Less code: no parsing of console values (0, negative, non-numeric) and the OkHttp client no longer depends on
  Remote Config. Remote Config itself stays for `dictionary_link_placement` (ADR 0013).
- Changing the lifetime needs an app release. If cached data ever has to be dropped at once, that needs its own
  mechanism (for example a cache key version), which the Remote Config value never provided.
