# M4 — Registration (QR Scan + Manual) + Brackets (SE + RR)
**Date:** 2026-07-24
**Milestone:** M4 (Phase 1 Milestone Order, spec §10) → target `v0.3.0`
**Spec:** `docs/2026-07-24-picklepro-ph-mvp-spec.md`
**Depends on:** M1 (backend), M2 (scaffold/auth/profile/QR), M3 (organizer activation, tournament/division setup)
**Status:** Executed

## Scope

1. **QR scan registration** (spec §5.4) — CameraX + ML Kit barcode scanning,
   decoding the `PlayerQrPayload` format defined in M2's `core/qr` (shared
   by player-side generation and this organizer-side scan). First scan of a
   player into a division: pick division → validate level gate + slots →
   register. Second scan of an already-registered player: check-in.
2. **Manual add** (spec §5.4) — name + declared tier → creates a shell
   profile (`is_shell = true`, claimable later via its `claim_code`) with a
   rating row for the division's event type, then registers it.
3. **Doubles/mixed pairing** (spec §5.4) — pair two unpaired registrations
   in the same division into a `teams` row.
4. **Registration list** (spec §5.4) — per division: search, check-in
   toggle, unregister.
5. **Bracket generation** (spec §5.5) — Single Elimination (seed by
   effective rating desc, manual reorder before lock, auto-byes to fill
   4/8/16/32, optional bronze match) and Round Robin (circle-method
   schedule; standings tiebreakers: head-to-head → point differential →
   points against → manual coin flip). Generating locks the division;
   regenerating is allowed until the first match result is entered.

## Out of scope (later milestones)
- **Full offline-first sync** (Room-backed registrations/brackets +
  `pending_ops` + WorkManager flush) — the milestone table assigns
  "offline sync" to **M5**, paired with the live scorer, not M4. M4's
  registration and bracket screens are online-first CRUD against Supabase,
  the same pattern M3 used for tournament/division setup. Building the
  sync queue now would mean building it twice (once naively, once for
  real once the live-scorer's conflict/undo requirements are known) —
  M5 is where it's actually needed, since that's where a device can lose
  connectivity mid-match.
- Live scorer, scoreboard (M5).
- Tabulation/results screens beyond the standings table needed to confirm
  bracket generation worked (M6 — full results + publish toggle).
- Certificates (M6). Admin web (M7).
- Manual drag-to-reorder seeding uses up/down move buttons, not a drag
  gesture — same end result (reorder before lock), lower implementation
  risk in an environment with no way to test touch gestures.

## Notes / decisions
- **Bye handling:** Single elimination pads the field up to the next power
  of 2 (4/8/16/32); byes go to the top seeds first, standard tournament
  seeding convention.
- **Round robin schedule:** standard circle method (fix one entrant, rotate
  the rest) — produces `n-1` rounds for even `n`, `n` rounds with one bye
  per round for odd `n`.
- **Regenerate guard:** a division can be regenerated as long as every
  match in it is still `status = 'pending'`; once any match is `live`,
  `done`, or `walkover`, regeneration is blocked (matches the spec's
  "regenerate allowed until first result entered").
- **Level gate re-check:** registration validates
  `effective_rating <= max_rating + 0.25` at scan/add time (spec §3.4);
  `divisions.max_rating = null` (Open) skips the gate entirely.
- **Slot check:** registration is blocked once
  `count(registrations) >= max_slots` for the division (`max_slots = null`
  means unlimited).
- Standings computation (round robin) is done client-side from `matches`
  rows on demand — no new schema needed; tiebreakers applied in spec order
  with the coin-flip step left as a manual admin action (a tie stays a tie
  in the computed table, spec §5.5 note "(4) coin flip (manual)").

## Verification

The Kotlin bracket/round-robin generators can't be compiled in this
environment (no Android SDK), but their core logic is plain algorithms with
no Android dependencies, so both were ported to Python and exercised
directly:

- **Single elimination**: simulated for entrant counts 2–32 (including
  non-power-of-two counts needing byes). This caught a real bug — the
  initial implementation let a round-1 bye recipient auto-advance through
  *every* subsequent round instead of just skipping round 1, which could
  pre-fill later rounds (even a final) with entrants who hadn't actually
  won anything yet. Fixed by making only the round1→round2 transition
  bye-aware; every match from round 2 on always produces an unresolved
  ("TBD") winner until it's actually played. Re-verified after the fix:
  for every tested entrant count, byes total exactly `bracketSize -
  entrantCount`, every (round, position) key is unique, and no round
  beyond 1 is ever pre-filled without a corresponding real match.
- **Round robin**: simulated for 2–10 entrants; confirmed every pair plays
  exactly once (no missing or duplicate pairings) and every entrant plays
  exactly `n-1` games, for both even and odd entrant counts (odd counts
  correctly get one bye per round).
- **Standings tiebreakers**: sanity-checked the win/loss tally and point
  differential arithmetic against hand-computed expected values.
