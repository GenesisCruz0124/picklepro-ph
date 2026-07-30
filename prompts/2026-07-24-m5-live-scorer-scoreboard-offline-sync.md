# M5 — Live Scorer + Scoreboard + Offline Sync
**Date:** 2026-07-24
**Milestone:** M5 (Phase 1 Milestone Order, spec §10) → target `v0.4.0`
**Spec:** `docs/2026-07-24-picklepro-ph-mvp-spec.md`
**Depends on:** M1-M4
**Status:** Executed

## Scope

1. **Score engine** (spec §5.6) — event-sourced pure Kotlin domain logic:
   traditional side-out scoring (doubles 3-number call, 0-0-2 game start,
   server 1→2→side-out rotation; singles 2-number call, immediate side-out),
   rally scoring, win-by-2 enforcement, best-of-N match completion,
   game/match point detection, end-swap reminder at 6 pts for game-to-11.
2. **Offline-first infrastructure** (spec §5.10) — Room becomes the source
   of truth for match scoring: `MatchEntity` mirrors `matches`, a generic
   `pending_ops` queue records every mutation, and a WorkManager worker
   flushes it to Supabase when online (organizer-device-wins conflict
   policy), then triggers `process-match-result` once a completed match's
   row has synced.
3. **Live Scorer** (spec §5.6) — two giant team panels, tap-to-score,
   SIDE OUT button (side-out mode), UNDO (unbounded, via event replay),
   timeouts (2/team/game, 60s countdown), end-swap reminder, game/match
   point indicator, match-complete confirm dialog.
4. **Scoreboard Display** (spec §5.7) — fullscreen landscape, keep-screen-on,
   read-only view of a live match for a second device/spectator screen.
5. **Bracket advancement** — closes the gap M4 deliberately left open: when
   a match completes, the winner is written into the next round's bracket
   match (side A if this match was an even position, side B if odd).

## Out of scope (later milestones)
- **Full offline-first for registration/check-in/bracket generation** (M3/M4
  flows). Spec §5.10 says "registrations, check-ins, brackets, scores all
  work with zero connectivity," but the milestone table pairs "offline
  sync" specifically with M5's live scorer — that's the flow with the real
  zero-connectivity requirement (mid-match, spotty gym wifi). Retrofitting
  M3/M4's already-shipped online CRUD to the same Room+pending_ops pattern
  is a natural following change using the infrastructure built here, but
  doing it for every M3/M4 screen in this milestone would multiply scope
  well beyond what's tractable in one pass. Flagged explicitly, not silently
  dropped.
- Court-manager grid, dedicated multi-device per-court scorer role (P2).
- Tabulation/results screens, certificates (M6). Admin web (M7).
- A visual court diagram for serve position — implemented as a text hint
  (Kanan/Kaliwa) instead; conveys the same information without building
  custom court graphics this milestone.

## Notes / decisions
- **Room schema migration**: `PickleProDatabase` moves from version 1 to 2
  with `fallbackToDestructiveMigration()` rather than a hand-written
  `Migration`. No version of this app has shipped externally yet (all
  builds so far are internal `0.x`), so there's no real user data at risk —
  a proper migration path is needed before `v1.0.0`, noted here so it isn't
  forgotten.
- **Rally scoring simplification**: spec doesn't detail rally-mode serve
  rules beyond naming the mode. Implemented as the common simple
  convention — whoever wins a rally scores a point and serves next: no
  server 1/2 tracking, since side-out doesn't apply.
- **Elo update is queued, not computed client-side**: per spec §5.6 "Elo
  update queued for sync," the client never runs Elo math itself (that
  logic — K-factor, sandbag checks — lives server-side in M1's
  `process-match-result`, which is idempotent). The sync worker's job is
  to (a) push the finished match row, then (b) invoke that Edge Function
  once the row exists server-side.
- **UNDO is unbounded via event replay**, not an inverse-operation stack:
  the point log is the single source of truth; game state is *derived* by
  replaying it from empty. Undo = drop the last event, re-derive. This
  can't drift from the log (which is also what spec wants persisted for
  disputes/analytics) and needed no separate "undo stack" data structure.
- **End-swap reminder** is implemented exactly as spec'd — triggered only
  when `game_to == 11` and a score first reaches 6 — not extrapolated to
  15/21, since spec only names the 11 case.

## Verification
Same approach as M4's bracket algorithms: the score engine has no Android
dependency, so its core (`replay`/`applyRally`/win checks) was ported to
Python and exercised against hand-traced real pickleball rally sequences
before being written as the Kotlin source of truth, to catch logic bugs
no compiler would.
