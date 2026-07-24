# PicklePro PH

Pickleball tournament management platform for the Philippine market.

**Spec:** [`docs/2026-07-24-picklepro-ph-mvp-spec.md`](docs/2026-07-24-picklepro-ph-mvp-spec.md)

| Surface | Platform | Status |
|---|---|---|
| PicklePro PH app (players + organizers) | Android — Kotlin, Jetpack Compose, MVVM | **M3 ✅** (auth, profile, QR, organizer activation + tournament/division setup); M4+ pending |
| PicklePro PH Admin | Web — Vite + React + TS + Tailwind (Vercel) | M7 (pending) |
| Backend | Supabase — Auth, Postgres + RLS, Storage, Edge Functions | **M1 ✅** |

## Repository layout

```
docs/       Product spec and reference documents
prompts/    Per-milestone plan files (plan-first workflow, spec §10)
supabase/
  migrations/   Postgres schema, functions/triggers, RLS policies
  functions/    Edge Functions (Deno):
    redeem-code             activation code → organizer role
    consume-code-on-create  consume 1 code credit + create tournament
    process-match-result    idempotent Elo + rating history + sandbag flags
    claim-shell-profile     claim a manually-added shell player
android/    Kotlin/Jetpack Compose app (Gradle project)
  app/src/main/java/com/gentech/picklepro/
    core/        design system, Taglish strings, QR encode/decode, rating tiers, nav
    auth/        signup + login (screens, ViewModel)
    player/      profile, my QR (screens, ViewModels)
    organizer/   activation, wallet, dashboard, tournament + division setup
    data/        Room cache, Supabase client + DTOs, repositories, DataStore
```

## Backend (M1)

Apply migrations and deploy functions with the Supabase CLI:

```sh
supabase link --project-ref <project-ref>
supabase db push
supabase functions deploy redeem-code consume-code-on-create process-match-result claim-shell-profile
```

Key backend rules:

- **Ratings** — Elo per event type (singles/doubles/mixed); display scale `2.0 + (elo − 800)/400` clamped 2.0–8.0; K = 64 for the first 10 matches per event type, then 32; doubles use pair-average team rating. All Elo writes happen in `process-match-result` (service role), idempotent per match id.
- **Monetization** — activation codes: Generated → Sent → Redeemed (binds to organizer) → consumed by one tournament. Redemption/consumption only via Edge Functions.
- **RLS** — profiles/ratings readable by all authenticated users (transparency); tournament family owned by its organizer; players read once a tournament leaves draft; admin-only tables for codes and sandbag flags.

## Android app (M2–M3)

```sh
cd android
cp local.properties.example local.properties   # fill in SUPABASE_URL / SUPABASE_ANON_KEY
./gradlew assembleDebug
```

> **No Android SDK in this build environment.** The M2/M3 source and Gradle
> config were written and reviewed carefully (package/path consistency,
> string-resource references, and version-catalog wiring were all checked),
> but could not be compiled here — do a real build to catch any API-level
> mismatches (Supabase-kt, Vico) before shipping. See
> `prompts/2026-07-24-m2-android-scaffold-auth-profile-qr.md` and
> `prompts/2026-07-24-m3-organizer-activation-tournament-division-setup.md`
> for details.

**M2** — signup (self-declared starting tier, event-type preferences) and
login against Supabase Auth; player profile (tier badges, Vico rating history
chart, match history); My QR (offline, ZXing-generated, max-brightness
toggle). Session persists via DataStore; profile/ratings cache in Room for
offline viewing.

**M3** — "Become an Organizer" (redeems an activation code), wallet (credit
count + Messenger buy-more CTA), organizer dashboard (summary cards +
tournament list), tournament setup (create via credit consumption, edit,
logo upload, forward-only status control), and division setup (event type,
skill gate, optional age bracket, scoring config). The Organizer bottom-nav
tab appears reactively once the profile's cached role becomes organizer.

## Phase 1 milestones (spec §10)

| # | Milestone | Status |
|---|---|---|
| M1 | Supabase schema + RLS + Edge Functions | ✅ |
| M2 | Android scaffold + auth + player profile + QR | ✅ |
| M3 | Organizer activation + tournament/division setup | ✅ |
| M4 | Registration (QR scan + manual) + brackets (SE + RR) | — |
| M5 | Live scorer (side-out + rally) + scoreboard + offline sync | — |
| M6 | Tabulation + Elo processing + certificates | — |
| M7 | Admin web (codes, organizers, flags, DUPR verify) | — |
| M8 | Polish + Taglish pass + release `PickleProPH-v1.0.0.apk` | — |
