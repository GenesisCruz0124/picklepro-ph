-- PicklePro PH — M1 initial schema (spec §7)
-- Tables: profiles, ratings, rating_history, activation_codes, tournaments,
--         divisions, registrations, teams, bracket_slots, matches,
--         sandbag_flags, certificates

create extension if not exists pgcrypto;

-- ---------------------------------------------------------------------------
-- Core identity
-- ---------------------------------------------------------------------------

-- No FK to auth.users: shell players (manual adds) have a profile row but no
-- auth account. Auth-backed profiles use auth.uid() as their id
-- (see handle_new_user trigger in the functions migration).
create table public.profiles (
  id            uuid primary key default gen_random_uuid(),
  name          text not null default '',
  photo_url     text,
  location      text,
  role          text not null default 'player'
                check (role in ('player', 'organizer', 'admin')),
  short_code    text unique,
  dupr_id       text,
  dupr_rating   numeric,
  dupr_verified boolean not null default false,
  dupr_proof_url text,
  is_shell      boolean not null default false,
  claim_code    text unique,
  suspended     boolean not null default false,
  created_at    timestamptz not null default now()
);

create table public.ratings (
  player_id      uuid not null references public.profiles (id) on delete cascade,
  event_type     text not null check (event_type in ('singles', 'doubles', 'mixed')),
  elo            integer not null default 1000,
  -- display = 2.0 + (elo - 800) / 400, clamped to 2.0–8.0 (spec §3.2)
  display        numeric generated always as
                 (round(least(8.0, greatest(2.0, 2.0 + (elo - 800) / 400.0)), 2)) stored,
  matches_played integer not null default 0,
  provisional    boolean not null default true,
  override       numeric,
  primary key (player_id, event_type)
);

-- ---------------------------------------------------------------------------
-- Monetization
-- ---------------------------------------------------------------------------

create table public.activation_codes (
  id          uuid primary key default gen_random_uuid(),
  code        text not null unique,
  price_label numeric,
  is_free     boolean not null default false,
  note        text,
  status      text not null default 'generated'
              check (status in ('generated', 'sent', 'redeemed', 'revoked')),
  redeemed_by uuid references public.profiles (id),
  redeemed_at timestamptz,
  consumed_by_tournament uuid,  -- FK added below (circular with tournaments)
  created_at  timestamptz not null default now()
);

-- ---------------------------------------------------------------------------
-- Tournaments
-- ---------------------------------------------------------------------------

create table public.tournaments (
  id                 uuid primary key default gen_random_uuid(),
  organizer_id       uuid not null references public.profiles (id),
  name               text not null,
  venue              text,
  description        text,
  logo_url           text,
  entry_fee_note     text,
  court_count        integer not null default 1 check (court_count > 0),
  start_date         date,
  end_date           date,
  status             text not null default 'draft'
                     check (status in ('draft', 'registration', 'ongoing', 'finished')),
  activation_code_id uuid references public.activation_codes (id),
  created_at         timestamptz not null default now()
);

alter table public.activation_codes
  add constraint activation_codes_consumed_by_tournament_fkey
  foreign key (consumed_by_tournament) references public.tournaments (id);

create table public.divisions (
  id            uuid primary key default gen_random_uuid(),
  tournament_id uuid not null references public.tournaments (id) on delete cascade,
  event_type    text not null check (event_type in ('singles', 'doubles', 'mixed')),
  name          text not null,
  min_rating    numeric,
  max_rating    numeric,          -- null = Open (spec §3.4)
  age_bracket   text,             -- null = open age; '19+','35+','50+','60+'
  max_slots     integer,
  format        text not null default 'single_elim'
                check (format in ('single_elim', 'round_robin', 'double_elim', 'pool_bracket')),
  game_to       integer not null default 11 check (game_to in (11, 15, 21)),
  win_by_2      boolean not null default true,
  best_of       integer not null default 1 check (best_of in (1, 3)),
  scoring_mode  text not null default 'sideout' check (scoring_mode in ('sideout', 'rally')),
  bronze_match  boolean not null default false,
  locked        boolean not null default false,
  published     boolean not null default false
);

create table public.teams (
  id          uuid primary key default gen_random_uuid(),
  division_id uuid not null references public.divisions (id) on delete cascade,
  player1_id  uuid not null references public.profiles (id),
  player2_id  uuid references public.profiles (id),
  seed        integer,
  created_at  timestamptz not null default now()
);

create table public.registrations (
  id          uuid primary key default gen_random_uuid(),
  division_id uuid not null references public.divisions (id) on delete cascade,
  player_id   uuid not null references public.profiles (id) on delete cascade,
  team_id     uuid references public.teams (id) on delete set null,
  checked_in  boolean not null default false,
  seed        integer,
  effective_rating_at_reg numeric,
  created_at  timestamptz not null default now(),
  unique (division_id, player_id)
);

-- ---------------------------------------------------------------------------
-- Bracket & matches
-- entrant_ref / side_*_ref / winner_ref / recipient_ref are UUIDs pointing at
-- registrations.id (singles) or teams.id (doubles/mixed) per division type.
-- ---------------------------------------------------------------------------

create table public.matches (
  id          uuid primary key default gen_random_uuid(),
  division_id uuid not null references public.divisions (id) on delete cascade,
  round       integer not null,
  position    integer not null,
  court       integer,
  side_a_ref  uuid,
  side_b_ref  uuid,
  status      text not null default 'pending'
              check (status in ('pending', 'live', 'done', 'walkover')),
  winner_ref  uuid,
  games       jsonb,   -- [{"a":11,"b":7}, ...]
  point_log   jsonb,   -- point-by-point event list (point/side_out/timeout/undo)
  started_at  timestamptz,
  finished_at timestamptz,
  synced      boolean not null default false,
  unique (division_id, round, position)
);

create table public.bracket_slots (
  id              uuid primary key default gen_random_uuid(),
  division_id     uuid not null references public.divisions (id) on delete cascade,
  round           integer not null,
  position        integer not null,
  entrant_ref     uuid,
  source_match_id uuid references public.matches (id) on delete set null,
  unique (division_id, round, position)
);

create table public.rating_history (
  id         uuid primary key default gen_random_uuid(),
  player_id  uuid not null references public.profiles (id) on delete cascade,
  event_type text not null check (event_type in ('singles', 'doubles', 'mixed')),
  elo_before integer not null,
  elo_after  integer not null,
  match_id   uuid references public.matches (id) on delete set null,
  created_at timestamptz not null default now(),
  -- idempotency guard for process-match-result (spec §3.2)
  unique (player_id, event_type, match_id)
);

-- ---------------------------------------------------------------------------
-- Anti-sandbag & certificates
-- ---------------------------------------------------------------------------

create table public.sandbag_flags (
  id          uuid primary key default gen_random_uuid(),
  player_id   uuid not null references public.profiles (id) on delete cascade,
  division_id uuid not null references public.divisions (id) on delete cascade,
  reason      text not null,
  evidence    jsonb,
  status      text not null default 'open'
              check (status in ('open', 'dismissed', 'actioned')),
  admin_note  text,
  created_at  timestamptz not null default now()
);

create table public.certificates (
  id            uuid primary key default gen_random_uuid(),
  tournament_id uuid not null references public.tournaments (id) on delete cascade,
  division_id   uuid not null references public.divisions (id) on delete cascade,
  recipient_ref uuid not null,
  kind          text not null check (kind in ('champion', 'runner_up', 'participation')),
  pdf_url       text,
  created_at    timestamptz not null default now()
);

-- ---------------------------------------------------------------------------
-- Indexes
-- ---------------------------------------------------------------------------

create index profiles_short_code_idx      on public.profiles (short_code);
create index profiles_role_idx            on public.profiles (role);
create index rating_history_player_idx    on public.rating_history (player_id, event_type, created_at);
create index activation_codes_redeemed_idx on public.activation_codes (redeemed_by) where redeemed_by is not null;
create index activation_codes_status_idx  on public.activation_codes (status);
create index tournaments_organizer_idx    on public.tournaments (organizer_id);
create index tournaments_status_idx       on public.tournaments (status);
create index divisions_tournament_idx     on public.divisions (tournament_id);
create index teams_division_idx           on public.teams (division_id);
create index registrations_division_idx   on public.registrations (division_id);
create index registrations_player_idx     on public.registrations (player_id);
create index matches_division_idx         on public.matches (division_id);
create index matches_status_idx           on public.matches (division_id, status);
create index bracket_slots_division_idx   on public.bracket_slots (division_id);
create index sandbag_flags_status_idx     on public.sandbag_flags (status);
create index certificates_tournament_idx  on public.certificates (tournament_id);
