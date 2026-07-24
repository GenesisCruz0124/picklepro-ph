# PicklePro PH — Phase 1 MVP Specification
**Date:** 2026-07-24
**Version:** 1.0.0 (target first release: `PickleProPH-v1.0.0.apk`)
**Owner/Admin:** Gen (GeNTech)
**Status:** Approved plan → ready for implementation

---

## 1. Product Overview

PicklePro PH is a pickleball tournament management platform for the Philippine market with three surfaces:

| Surface | Platform | Users |
|---|---|---|
| PicklePro PH app | Android — Kotlin, Jetpack Compose, MVVM | Players + Organizers (role-based UI, single APK) |
| PicklePro PH Admin | Web — Vite + React + TypeScript + Tailwind (Vercel) | Admin/Owner |
| Backend | Supabase — Auth, Postgres + RLS, Storage, Edge Functions | All |

**Core value propositions:**
1. **Anti-sandbagging player ratings** — Elo-based ratings from real match results, DUPR linking, transparent profiles, level-gated tournaments.
2. **Full tournament-day toolkit for organizers** — bracket generation (official pickleball formats), QR registration, live scorer with official side-out scoring, fullscreen scoreboard, tabulation, PDF certificates.
3. **Monetization for owner** — one-time activation code per tournament, customizable price, GCash manual payment flow, admin can also generate free codes.

---

## 2. Roles & Access

### 2.1 Player
- Sign up / login (Supabase Auth: email + password; Google sign-in stretch).
- Owns a profile and per-event-type ratings.
- Registers to tournaments via personal QR (scanned by organizer) — player never self-registers into a bracket directly in Phase 1.

### 2.2 Organizer
- Any player account can be upgraded to organizer by redeeming an **activation code** OR admin flags account as organizer.
- Creating a tournament **consumes one unredeemed activation code** owned by that organizer.
- Full tournament management for own tournaments only (RLS enforced).
- Multiple independent organizers supported; data isolated per organizer.

### 2.3 Admin (Gen)
- Web only. Full visibility.
- Generates activation codes (custom price label or free), manages organizers, reviews sandbag flags, overrides ratings.

---

## 3. Rating System

### 3.1 Skill Tiers (pickleball standard)
| Rating | Tier |
|---|---|
| < 2.5 | 2.0 Beginner |
| 2.5–2.99 | 2.5 Novice |
| 3.0–3.49 | 3.0 Intermediate |
| 3.5–3.99 | 3.5 Advanced Intermediate |
| 4.0–4.49 | 4.0 Advanced |
| ≥ 4.5 | 4.5+ Open / Pro |

### 3.2 Internal Elo
- Separate rating per event type: **Singles, Doubles, Mixed Doubles**.
- Displayed on 2.0–8.0 scale. Internally stored as Elo points, mapped: `display = 2.0 + (elo - 800) / 400`, clamped 2.0–8.0. New player starts at elo 1000 → display 2.5 (Novice) unless self-declared higher tier at signup (self-declared start allowed, marked *provisional*).
- **K-factor:** 64 for first 10 matches per event type (provisional), then 32.
- Doubles/Mixed: team rating = average of pair; both partners gain/lose the same delta.
- Expected score: standard Elo formula `E = 1 / (1 + 10^((eloB - eloA)/400))`.
- Rating updates run in a Supabase Edge Function when a match result syncs; idempotent per match id.
- `rating_history` row per change; profile shows Vico line chart.

### 3.3 DUPR Linking (Phase 1 = manual verification)
- Player may enter DUPR ID + current DUPR rating + screenshot upload (Supabase Storage).
- Organizer or Admin can mark it **Verified** → profile shows DUPR badge + verified DUPR rating.
- Verified DUPR rating takes precedence for level-gating when present.
- Official DUPR API integration deferred (partner access required) — schema keeps `dupr_id`, `dupr_rating`, `dupr_verified`, `dupr_proof_url` ready for future API sync.

### 3.4 Level Gating
- Division defines `max_rating` (nullable = Open) and optional `min_rating`.
- Gate check at registration: `effective_rating = dupr_verified ? dupr_rating : internal_display_rating`.
- Buffer: registration allowed if `effective_rating <= max_rating + 0.25`.
- Open division: no gate.

### 3.5 Sandbag Flags (Phase 1 = data collection + simple flags; full engine Phase 3)
Auto-create a `sandbag_flags` row when, within one tournament division:
- Player wins ≥ 5 matches with win rate > 85%, **or**
- Average point differential ≥ 7 in a game-to-11 division.
Flags appear in Admin review queue. Admin action: dismiss / adjust rating / restrict tiers.

---

## 4. Player App — Phase 1 Features

1. **Auth & Onboarding** — signup, self-declared starting tier, event-type preferences.
2. **Profile** — photo, name, location, tier badges per event type, rating chart (Vico), match history list (opponent, score, W/L, tournament), DUPR badge if verified.
3. **My QR** — fullscreen QR encoding `player_id` (UUID) + short human code (e.g. `PP-8F3K2`), max-brightness toggle. Works offline (generated locally).
4. **Tournaments** — list (Upcoming / Ongoing / Finished), filters: level, date, location; detail page: divisions, gates, slots, venue, entry-fee note, organizer info.
5. **Live view** — for tournaments where registered: my schedule ("Next: Court 2 vs Team X"), live score of my match, bracket view, division standings.
6. **Player directory** — search players, view public profiles (transparency vs smurfing).
7. **Notifications** — FCM: match ready, result posted, tournament updates. (Basic in Phase 1.)

---

## 5. Organizer App — Phase 1 Features

### 5.1 Activation
- "Become an Organizer" screen → enter activation code → validates via Edge Function → account gains organizer role; code bound to account.
- Creating a tournament consumes 1 code credit. Wallet screen shows unredeemed credits + "Buy more" → opens Messenger CTA `m.me/genesiscruz0124` with prefilled message (GCash manual flow).

### 5.2 Dashboard
- Cards: active tournaments, total registered players, matches pending / completed today.
- Tournament list with status chips (Draft / Registration / Ongoing / Finished).

### 5.3 Tournament Setup
- Fields: name, venue, dates, description, entry-fee note, logo upload, court count.
- **Divisions:** event type (S/D/MX) × skill gate (tier ceiling or Open) × **optional age bracket** (Open age default; optional 19+, 35+, 50+, 60+ — only shown if organizer enables age divisions), max slots, scoring config.
- Scoring config per division: game to 11/15/21, win by 2 (toggle), best of 1 or 3, **traditional side-out** (default) or rally scoring.

### 5.4 Registration & Check-in
- **QR scan** (CameraX + ML Kit barcode): scan player QR → pick division → validates level gate + slots → registered. Second scan on event day = check-in.
- **Manual add:** name + declared tier → creates *shell player* (claimable later by real account via short code).
- Doubles pairing: pair two registered players into a team per division.
- Registration list per division with search, unregister, check-in toggle.

### 5.5 Bracket Generator (Phase 1: Single Elimination + Round Robin)
- **Seeding:** by effective rating (desc); manual drag-to-reorder before lock; auto-byes to top seeds (bracket sizes 4/8/16/32).
- **Single elimination:** optional bronze match toggle.
- **Round robin:** all-play-all schedule (circle method); standings tiebreakers per USA Pickleball order: (1) head-to-head, (2) point differential, (3) points against, (4) coin flip (manual).
- Bracket locked on generate; regenerate allowed until first result entered.
- (Double elim + pool play → bracket = Phase 2, schema already supports.)

### 5.6 Live Scorer (official pickleball scoring)
The organizer device (or per-court device, Phase 2) is the official scorer.

- **Traditional side-out scoring:**
  - Doubles: 3-number score call display (`server – receiver – server#`), game starts **0-0-2**; only serving team scores; server 1 → server 2 → side out rotation; serving position logic (right/even, left/odd) shown as court diagram hint.
  - Singles: 2-number score; serve side derived from server's score parity.
- **Rally scoring** mode when division configured for it.
- UI: two giant team panels; tap serving team panel = +1; **SIDE OUT** button; **UNDO** (unbounded point-by-point history stack); timeout buttons (2/team/game, 60s countdown via existing timer pattern); end-swap reminder at 6 pts (game to 11); game/match point indicator; win-by-2 enforcement.
- Match complete → confirm dialog → result saved locally (Room), winner auto-advances in bracket, Elo update queued for sync.
- **Point-by-point log** persisted per match (sequence of events: point, side-out, timeout, undo) — feeds disputes + future sandbag analytics.

### 5.7 Scoreboard Display
- Fullscreen landscape scoreboard mode (keep-screen-on): team names, big scores, serving indicator dots (server 1/2), game #, division + tournament name, organizer logo strip.
- Launchable from any live match; intended for a second device or screen-cast facing spectators.
- (Court-manager grid + dedicated multi-device scorer role = Phase 2.)

### 5.8 Tabulation & Results
- Division standings table (RR) / bracket progression (SE), champions + runners-up auto-derived.
- Results screen per division; publish toggle → visible to players.

### 5.9 Certificates (basic in Phase 1)
- PDF generation on-device (Android `PdfDocument`): Champion / Runner-Up / Participation templates; fields: tournament name, division, player/team name, date, organizer name + logo. Share sheet / print.
- (Fancier templates + bulk generation = Phase 2.)

### 5.10 Offline-first
- Room DB = source of truth during tournament day: registrations, check-ins, brackets, scores all work with zero connectivity.
- Sync queue table (`pending_ops`) → WorkManager flush to Supabase when online. Conflict policy: **organizer device wins** for match/tournament data.
- Elo recomputation happens server-side only after sync (players see "rating pending sync" state).

---

## 6. Admin Web — Phase 1 Features

1. **Auth** — Supabase auth, admin role only (email allowlist).
2. **Activation Codes** — generate single/batch; fields: code (auto 12-char), **price label (customizable per code/batch, ₱)**, note, free/paid flag; states: Generated → Sent → Redeemed (with organizer + timestamp); revoke unredeemed codes; copy/export CSV.
3. **Organizers** — list, tournaments count, codes redeemed, suspend/reactivate.
4. **Sandbag Review Queue** — flag list with match evidence, actions: dismiss / set rating override / restrict from tiers.
5. **DUPR Verifications** — pending screenshot proofs → verify/reject.
6. **Stats** — totals: players, organizers, tournaments, matches, codes sold vs redeemed (simple; full revenue dashboard Phase 3).

---

## 7. Database Schema (Supabase Postgres)

```sql
-- Core identity
profiles(id uuid pk = auth.uid, name, photo_url, location, role text check in ('player','organizer','admin'),
         short_code text unique, dupr_id, dupr_rating numeric, dupr_verified bool default false,
         dupr_proof_url, is_shell bool default false, claim_code, created_at)

ratings(player_id fk, event_type text check in ('singles','doubles','mixed'),
        elo int default 1000, display numeric generated, matches_played int default 0,
        provisional bool default true, override numeric null, pk(player_id, event_type))

rating_history(id, player_id, event_type, elo_before, elo_after, match_id, created_at)

-- Monetization
activation_codes(id, code text unique, price_label numeric null, is_free bool default false,
                 note, status text check in ('generated','sent','redeemed','revoked'),
                 redeemed_by fk profiles null, redeemed_at, consumed_by_tournament fk null, created_at)

-- Tournaments
tournaments(id, organizer_id fk, name, venue, description, logo_url, entry_fee_note,
            court_count int, start_date, end_date,
            status text check in ('draft','registration','ongoing','finished'),
            activation_code_id fk, created_at)

divisions(id, tournament_id fk, event_type, name,
          min_rating numeric null, max_rating numeric null,  -- null max = Open
          age_bracket text null,                              -- null = open age (optional feature)
          max_slots int, format text check in ('single_elim','round_robin','double_elim','pool_bracket'),
          game_to int default 11, win_by_2 bool default true, best_of int default 1,
          scoring_mode text check in ('sideout','rally') default 'sideout',
          bronze_match bool default false, locked bool default false, published bool default false)

registrations(id, division_id fk, player_id fk, team_id null, checked_in bool default false,
              effective_rating_at_reg numeric, created_at, unique(division_id, player_id))

teams(id, division_id fk, player1_id fk, player2_id fk, seed int null)

-- Bracket & matches
bracket_slots(id, division_id fk, round int, position int, entrant_ref, source_match_id null)

matches(id, division_id fk, round int, position int, court int null,
        side_a_ref, side_b_ref, status text check in ('pending','live','done','walkover'),
        winner_ref null, games jsonb,          -- [{a:11,b:7},...]
        point_log jsonb,                        -- point-by-point events
        started_at, finished_at, synced bool default false)

-- Anti-sandbag & certs
sandbag_flags(id, player_id fk, division_id fk, reason, evidence jsonb,
              status text check in ('open','dismissed','actioned'), admin_note, created_at)

certificates(id, tournament_id fk, division_id fk, recipient_ref, kind text
             check in ('champion','runner_up','participation'), pdf_url null, created_at)
```

**RLS highlights:**
- `profiles`, `ratings`, `rating_history`: readable by all authenticated (transparency); writable by owner/service role only.
- Tournament-family tables: organizer full CRUD where `organizer_id = auth.uid()`; players read where `published` or registered.
- `activation_codes`: admin full; organizer can read own redeemed rows; redemption via Edge Function (service role) only.
- Elo writes: Edge Function service role only.

**Edge Functions:** `redeem-code`, `consume-code-on-create`, `process-match-result` (Elo + history + sandbag check, idempotent), `claim-shell-profile`.

---

## 8. Android App Architecture

- **Stack:** Kotlin, Jetpack Compose, MVVM, Room, DataStore, Vico charts, CameraX + ML Kit (QR scan), ZXing (QR generate), WorkManager (sync), FCM, Supabase-kt.
- **Modules/packages:** `core` (design system, Taglish strings), `auth`, `player` (profile, tournaments, live), `organizer` (dashboard, setup, registration, bracket, scorer, scoreboard, certificates), `sync`, `data` (Room + repositories + Supabase DTOs).
- Role-based navigation: single APK; organizer tab appears after role upgrade.
- UI language: **Taglish labels**, technical/config terms in English (consistent with GeNTech apps).
- Min SDK 26, target latest stable.

## 9. Admin Web Architecture
- Vite + React + TypeScript + Tailwind, deployed on Vercel; Supabase JS client; route guard on admin role; tables with TanStack Table; CSV export client-side.

---

## 10. Delivery Workflow (per Gen's standard)

1. **Plan-first:** each build milestone gets a prompt file `YYYY-MM-DD-descriptive-name.md`; approval before execution.
2. **Subagents:** `verify-implementation` gates every build — re-checks last issue fix + implementation before APK build; `allow-once-approver` handles allow-once prompts.
3. **Versioning:** semantic versioning, `versionCode` + `versionName` incremented every build.
4. **APK naming:** `PickleProPH-vX.X.X.apk`.
5. **CI/CD:** GitHub Actions workflow, tag-triggered (`v*`): build signed release APK → create GitHub Release → attach APK → generate release notes from commits since last tag.
6. **Release notes** provided with every release.

### Phase 1 Milestone Order
| # | Milestone | Key output |
|---|---|---|
| M1 | Supabase schema + RLS + Edge Functions | migrations, functions |
| M2 | Android scaffold + auth + player profile + QR | `v0.1.0` internal |
| M3 | Organizer activation + tournament/division setup | `v0.2.0` |
| M4 | Registration (QR scan + manual) + brackets (SE + RR) | `v0.3.0` |
| M5 | Live scorer (side-out + rally) + scoreboard + offline sync | `v0.4.0` |
| M6 | Tabulation + Elo processing + certificates | `v0.5.0` |
| M7 | Admin web (codes, organizers, flags, DUPR verify) | web deploy |
| M8 | Polish + Taglish pass + release | **`PickleProPH-v1.0.0.apk`** |

---

## 11. Phase 2 / 3 Backlog (already schema-ready)
- **P2:** double elimination, pool play → medal bracket, court-manager grid, multi-device per-court scorer role, bulk certificate generation + template designer, richer notifications, spectator live scores at scale.
- **P3:** full sandbag detection engine (statistical model on point logs), DUPR API integration, revenue dashboard, staff sub-accounts, GCash/Maya online checkout for codes.

---

## 12. Open Assumptions (confirm anytime, non-blocking)
- Entry fees are cash/GCash handled outside the app (app only shows a note) — no in-app fee collection in Phase 1.
- One activation code = one tournament regardless of division count.
- Shell players (manual adds) get provisional ratings only after claiming their account.
