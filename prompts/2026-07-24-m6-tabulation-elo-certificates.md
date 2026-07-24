# M6 — Tabulation + Elo Processing + Certificates
**Date:** 2026-07-24
**Milestone:** M6 (Phase 1 Milestone Order, spec §10) → target `v0.5.0`
**Spec:** `docs/2026-07-24-picklepro-ph-mvp-spec.md`
**Depends on:** M1-M5
**Status:** Executed

## Scope

1. **Tabulation & Results** (spec §5.8) — per-division results screen:
   auto-derived champion + runner-up (single elim: final's winner/loser,
   plus bronze-match winner as third place when present; round robin:
   standings top 2), the full standings table (RR) or round-by-round
   progression (SE), and the **publish toggle** that flips
   `divisions.published` so results become player-visible.
2. **Elo processing** — the server side already exists end-to-end (M1's
   idempotent `process-match-result`, invoked by M5's sync worker after a
   finished match syncs). What M6 adds is the client-side surfacing spec
   §5.10 asks for: a pending-sync indicator (unsynced `pending_ops` count)
   on the organizer dashboard, so it's visible that results/Elo haven't
   reached the server yet ("rating pending sync" state).
3. **Certificates** (spec §5.9) — on-device PDF generation with Android's
   `PdfDocument`: Champion / Runner-Up / Participation templates carrying
   tournament name, division name, recipient (player or team), date, and
   organizer name + logo. Shared via the system share sheet (FileProvider);
   each generated certificate also records a row in the `certificates`
   table (`pdf_url` stays null — the PDF lives on-device, no Storage
   upload in Phase 1).

## Out of scope (later)
- Fancier certificate templates + bulk generation (P2, spec §5.9 note).
- Storage upload of generated PDFs (`certificates.pdf_url` reserved).
- Player-side tournament list / live view screens (spec §4.4-4.5) — those
  player surfaces were never assigned a Phase-1 milestone row of their own
  in §10's table; the publish flag gates what they *will* see. Flagged as
  an M8-polish candidate rather than silently dropped.
- Admin web (M7).

## Notes / decisions
- **Publish toggle uses a dedicated single-field DTO** (`published` only),
  same as the M5 fixes — never a multi-field nullable DTO that could PATCH
  unrelated columns to null.
- **Third place**: only derived when the division actually has a bronze
  match with a result; otherwise results show champion + runner-up only.
- **Certificate PDFs are landscape A4** (842×595 pt at 72dpi), drawn with
  plain `Canvas`/`Paint` text — no external PDF library; `PdfDocument` is
  in the Android SDK (spec §5.9 names it directly).
- **FileProvider** added to the manifest with a `file_paths.xml` scoped to
  the app's cache subdirectory `certificates/` — PDFs are transient share
  artifacts, not user documents; regenerating is cheap and deterministic.
- **Logo in the PDF**: fetched via Coil's `ImageLoader.execute` when the
  tournament has one; a missing/unfetchable logo degrades to text-only,
  never blocks generation (tournament day may be offline — spec §5.10).
