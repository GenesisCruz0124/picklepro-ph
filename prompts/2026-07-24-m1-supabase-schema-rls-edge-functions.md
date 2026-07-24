# M1 — Supabase Schema + RLS + Edge Functions
**Date:** 2026-07-24
**Milestone:** M1 (Phase 1 Milestone Order, spec §10)
**Spec:** `docs/2026-07-24-picklepro-ph-mvp-spec.md`
**Status:** Executed

## Scope

Implement the full Supabase backend foundation for PicklePro PH Phase 1:

1. **Migrations** (`supabase/migrations/`)
   - `..._initial_schema.sql` — all tables from spec §7: `profiles`, `ratings`, `rating_history`, `activation_codes`, `tournaments`, `divisions`, `registrations`, `teams`, `bracket_slots`, `matches`, `sandbag_flags`, `certificates`. Includes generated `display` rating column (elo → 2.0–8.0 clamp), check constraints, FKs, indexes.
   - `..._functions_triggers.sql` — helper functions + triggers:
     - `generate_short_code()` / profile trigger → unique `PP-XXXXX` short codes; `claim_code` for shell profiles.
     - `handle_new_user()` on `auth.users` insert → creates profile + 3 rating rows (singles/doubles/mixed), honoring self-declared starting tier from signup metadata (provisional).
     - `protect_profile_columns()` → blocks non-admin clients from editing `role`, `dupr_verified`, `is_shell`, `short_code`, `claim_code`, rating override paths.
     - `claim_shell_profile(claim_code)` — merges a shell profile's registrations/teams/ratings into the claiming account (called by Edge Function with service role).
   - `..._rls_policies.sql` — RLS enabled on every table, per spec §7 RLS highlights.

2. **Edge Functions** (`supabase/functions/`)
   - `redeem-code` — validates + redeems an activation code; upgrades account to organizer.
   - `consume-code-on-create` — atomically consumes one unconsumed code credit and creates the tournament.
   - `process-match-result` — idempotent Elo update (K=64 provisional first 10 matches, else 32; doubles = pair average), `rating_history` rows, sandbag flag checks (win-rate + point-differential rules, spec §3.5).
   - `claim-shell-profile` — lets a real account claim a shell player via claim code.

## Out of scope (later milestones)
- Android app (M2–M6), Admin web (M7), FCM, Storage buckets policies beyond DUPR proofs note, DUPR API sync (P3).

## Notes / decisions
- Shell profiles have no `auth.users` row, so `profiles.id` does not FK to `auth.users`; `handle_new_user` links auth accounts by using `auth.uid()` as the profile id.
- `matches.side_a_ref` / `side_b_ref` / `winner_ref` are UUIDs pointing at `registrations.id` (singles) or `teams.id` (doubles/mixed), per division event type.
- Doubles Elo: expected score computed from team averages; each player's delta uses their own K-factor (provisional players converge faster). When both partners are past provisional, deltas are identical per spec.
- Code redemption/consumption and all Elo writes go through Edge Functions with the service role; RLS blocks client writes to those tables.
