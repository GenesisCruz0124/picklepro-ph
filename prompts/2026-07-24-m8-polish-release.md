# M8 — Polish + Taglish Pass + Release (v1.0.0)
**Date:** 2026-07-24
**Milestone:** M8 (Phase 1 Milestone Order, spec §10) → `PickleProPH-v1.0.0.apk`
**Spec:** `docs/2026-07-24-picklepro-ph-mvp-spec.md`
**Depends on:** M1-M7
**Status:** Executed (code + release infrastructure; the APK build itself
runs in CI / on a machine with an Android SDK — see "What still needs a
human/CI" below)

## Scope

1. **Player Tournaments tab** (spec §4.4) — the gap flagged in M6's plan
   as the M8-polish candidate: §10's milestone table never assigned the
   player tournament-browsing surface to any earlier milestone. Basic
   form: list of non-draft tournaments grouped Upcoming (registration) /
   Ongoing / Finished with a name-or-location search; detail page with
   venue, dates, entry-fee note, organizer name + logo, and every
   division's event type / skill gate / age bracket / slots taken; a
   **published** division (spec §5.8's toggle) additionally shows its
   podium + standings, reusing M6's `ResultsRepository`. RLS already
   scopes all of this: players read tournaments once they leave draft.
2. **Release signing** — `app/build.gradle.kts` gains a release
   `signingConfig` fed by `android/app/release.keystore` + env vars
   (`KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`); when the keystore
   is absent (local dev, this environment) release builds fall back to
   debug signing rather than failing.
3. **CI/CD** (spec §10.5) — `.github/workflows/release.yml`: pushing a
   `v*` tag builds the signed release APK, renames it
   `PickleProPH-<tag>.apk` (spec §10.4 naming), generates release notes
   from commits since the previous tag, and attaches both to a GitHub
   Release.
4. **Taglish pass** (spec §8) — strings were written Taglish-first from
   M2 on; this pass re-read all of `strings.xml` for consistency
   (labels Taglish, technical/config terms English).
5. **Versioning** — versionCode 6 / versionName **1.0.0** (spec §10.3);
   release notes at `docs/RELEASE-NOTES-v1.0.0.md` (spec §10.6).

## What still needs a human/CI (cannot happen in this environment)
- **The APK.** No Android SDK here — no milestone's Android code has been
  compiled. Before tagging `v1.0.0`: run `./gradlew assembleDebug`, fix
  any API-level drift (Supabase-kt / Vico / CameraX / ML Kit /
  WorkManager versions were written against docs, not a compiler), and
  smoke-test on a device.
- **Repo secrets** for the release workflow: `KEYSTORE_BASE64`,
  `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`, `SUPABASE_URL`,
  `SUPABASE_ANON_KEY`.
- **Backend deploy**: `supabase db push` + `supabase functions deploy`
  (M1/M3 migrations + 4 Edge Functions) against the real project.
- **Admin web deploy**: point Vercel at `admin-web/` (M7 — that one *is*
  build-verified).

## Deferred out of Phase 1 scope (recorded, not dropped)
- Player live view (my schedule / live score of my match — spec §4.5) and
  player directory (§4.6): read paths and RLS exist; screens are P2-adjacent
  follow-ups. FCM notifications (§4.7, "basic in Phase 1") need
  google-services.json + server sender — deferred with them.
- Google sign-in (spec §2.1 "stretch").
