# M2 — Android Scaffold + Auth + Player Profile + QR
**Date:** 2026-07-24
**Milestone:** M2 (Phase 1 Milestone Order, spec §10) → target `v0.1.0` internal
**Spec:** `docs/2026-07-24-picklepro-ph-mvp-spec.md`
**Depends on:** M1 (Supabase schema + RLS + Edge Functions)
**Status:** Executed

## Scope

Stand up the Android app project (spec §8) and implement the first player-facing
slice:

1. **Project scaffold** — Gradle (Kotlin DSL, version catalog), single-module
   `app` to start (package split by feature inside it, per spec §8:
   `core`, `auth`, `player`, `data`), min SDK 26.
2. **Core** — Compose Material3 theme, Taglish string resources (spec §8: UI
   labels Taglish, technical/config terms English).
3. **Data layer** — Supabase-kt client (Auth + Postgrest), Room DB for the
   offline profile/ratings cache (foundation for the offline-first pattern
   used more heavily from M4 onward), DataStore for session persistence.
4. **Auth** — signup (email + password, self-declared starting tier,
   event-type preferences per spec §4.1) and login screens, wired to
   Supabase Auth (`handle_new_user` trigger from M1 creates the profile +
   rating rows server-side).
5. **Player profile** — photo, name, location, tier badges per event type,
   Vico rating line chart, match history list, DUPR badge when verified
   (spec §4.2).
6. **My QR** — fullscreen QR (ZXing) encoding `player_id` UUID + short human
   code, max-brightness toggle, generated fully on-device/offline (spec
   §4.3).
7. **Navigation** — single-APK NavHost; role-based organizer tab is stubbed
   for M3, not built yet.

## Out of scope (later milestones)
- Organizer activation/dashboard/tournament setup (M3).
- QR **scanning** (CameraX + ML Kit) — that's the organizer's registration
  flow (M4). M2 only *generates* the player's own QR.
- Bracket, live scorer, scoreboard, certificates, offline sync queue (M4–M6).
- Admin web (M7). FCM notifications wired later once a push flow exists
  beyond scaffolding.

## Environment note
No Android SDK is installed in this execution environment (Java 21 + Gradle
are present, but no `ANDROID_HOME`/`sdkmanager`/emulator). Source and Gradle
config are written to compile cleanly against the declared SDK/dependency
versions, but a real build/lint/test pass could not be run here. Flagged
explicitly so the human build step (per spec §10 CI/CD, tag-triggered signed
release) is not skipped.

## Notes / decisions
- Single Gradle module (`app`) rather than multi-module for Phase 1 — spec
  describes "modules/packages" which is satisfied by a package-per-feature
  layout inside one module; splitting into real Gradle modules is easy to
  do later if build times demand it, and premature modularization isn't
  justified yet.
- Session/auth tokens persisted via DataStore (`Preferences`), not Room —
  matches typical Supabase-kt session persistence pattern.
- Room in M2 only caches `profile` + `ratings` (read side) for offline
  profile viewing; the heavier offline-first tables (`registrations`,
  `matches`, `pending_ops` sync queue) come in M4/M5 when organizer flows
  need them.
- Rating chart uses Vico's `CartesianChart` line component against
  `rating_history` rows fetched from Supabase (read-only per M1 RLS).
