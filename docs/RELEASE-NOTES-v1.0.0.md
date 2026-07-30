# PicklePro PH v1.0.0

First public release of the Phase 1 MVP (spec `docs/2026-07-24-picklepro-ph-mvp-spec.md`).
One APK, two roles: Player and Organizer.

## Highlights

**For every player**
- Sign up / sign in, set a starting (provisional) rating, and get a
  DUPR-style Elo rating that updates after every tabulated match.
- A personal QR code (works offline) organizers scan to register you for a
  tournament.
- Browse all open, ongoing, and finished tournaments; see venue, dates,
  divisions, and — once an organizer publishes them — full results and
  podiums.
- Profile shows rating history, match history, and win/loss record.

**For organizers**
- Redeem a Gen-issued activation code to unlock the Organizer role and get
  tournament credits.
- Create a tournament, add divisions (singles/doubles/mixed, skill gate,
  age bracket, format, scoring rules).
- Register players by QR scan or manual entry, pair up doubles teams.
- Auto-generate single-elimination (with byes) or round-robin brackets
  from seeding.
- Score live matches (traditional side-out or rally scoring) with a
  fullscreen scoreboard display and one-tap undo.
- Works offline mid-tournament — scores queue locally and sync
  automatically, organizer's device wins on conflict.
- Bracket auto-advances winners; standings compute automatically for
  round robin.
- Publish results, then generate and share PDF certificates (champion,
  runner-up, participation) for every entrant.

**For Gen/GeNTech (admin)**
- Web dashboard (`admin-web/`, deployed separately) to issue and revoke
  organizer activation codes, review organizers, review sandbag flags and
  DUPR verification requests, and see platform-wide stats.

## What's not in this release

Deferred past Phase 1 (tracked, not forgotten): a player's live/upcoming
match view, a player directory, push notifications, and Google sign-in.

## Installing

Download `PickleProPH-v1.0.0.apk` from this release, enable "install from
unknown sources," and install. Requires Android 8.0 (API 26) or newer.
