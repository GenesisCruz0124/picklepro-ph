# M3 — Organizer Activation + Tournament/Division Setup
**Date:** 2026-07-24
**Milestone:** M3 (Phase 1 Milestone Order, spec §10) → target `v0.2.0`
**Spec:** `docs/2026-07-24-picklepro-ph-mvp-spec.md`
**Depends on:** M1 (backend), M2 (Android scaffold, auth, player profile, QR)
**Status:** Executed

## Scope

1. **Storage** — `tournament-logos` bucket + RLS (public read, organizer-owned
   write), added as a follow-up migration since M1 only defined schema/table
   RLS, not Storage buckets (spec §5.3 "logo upload").
2. **Data layer** — Supabase Functions plugin (to call `redeem-code` and
   `consume-code-on-create`); `TournamentDto`/`DivisionDto`;
   `OrganizerRepository` (redeem code, credit count), `TournamentRepository`
   (create via credit consumption, list/update, logo upload),
   `DivisionRepository` (CRUD, direct Postgrest — RLS already scopes to the
   owning organizer).
3. **Become an Organizer** — activation code entry (spec §5.1).
4. **Wallet** — unredeemed credit count, redeem another code, "Buy more"
   Messenger CTA (`m.me/genesiscruz0124`, prefilled message) — spec §5.1.
5. **Dashboard** — summary cards + tournament list with status chips (spec
   §5.2).
6. **Tournament setup** — create (consumes 1 activation code credit via
   `consume-code-on-create`) and edit tournament fields, logo upload, status
   control (spec §5.3).
7. **Division setup** — add/edit/delete divisions: event type, skill gate,
   optional age bracket, max slots, scoring config (spec §5.3).
8. **Navigation** — Organizer tab appears once the cached profile's role is
   `organizer`/`admin` (single APK, role-based per spec §8); nested graph
   Dashboard → Tournament detail → Divisions, plus Become-Organizer/Wallet.

## Out of scope (later milestones)
- Registration (QR scan + manual), doubles pairing, check-in (M4).
- Bracket generation (M4) — division `locked` flag exists in schema but
  nothing sets it yet; division setup disables editing when `locked` as
  cheap future-proofing, though it's never true in M3.
- Live scorer, scoreboard, offline sync queue (M5).
- Tabulation/results and the division `published` toggle — that flag is for
  *results* visibility (spec §5.8), not division setup, so M3 does not
  expose it.
- Certificates (M6). Admin web (M7).

## Notes / decisions
- **Tournament status** (Draft → Registration → Ongoing → Finished) is a
  manual, forward-only control on the tournament detail screen. Nothing in
  the M1 RLS ties registration/match writes to this status — it's a
  workflow/display field (spec §5.2 status chips) the organizer advances
  themselves; it does not block or unblock any feature.
- **Division edits go straight through Postgrest**, not an Edge Function —
  the M1 RLS policy `"organizers manage own divisions"` already scopes
  CRUD to `owns_tournament()`, so there's no privileged logic to centralize
  server-side (unlike tournament creation, which must atomically consume a
  code credit).
- **Credit count** is read directly from `activation_codes` (RLS: organizer
  reads own redeemed rows) rather than cached — it changes rarely and is
  cheap to query fresh on the wallet/dashboard/create-tournament screens.
- Logo upload path convention: `{tournament_id}/logo.{ext}` in the
  `tournament-logos` bucket, matching the storage RLS policy that checks
  `owns_tournament()` against the first path segment.
