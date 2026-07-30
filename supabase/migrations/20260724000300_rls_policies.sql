-- PicklePro PH — M1 Row Level Security (spec §7 RLS highlights)
--
-- Summary:
--   profiles / ratings / rating_history : readable by all authenticated
--     (transparency vs smurfing); writable by owner (profiles) or the
--     service role only (ratings, rating_history — Elo writes happen in
--     the process-match-result Edge Function).
--   tournament family : organizer full CRUD on own tournaments; players
--     read once the tournament leaves draft. Tournament INSERT is service
--     role only — creation must consume an activation code via the
--     consume-code-on-create Edge Function.
--   activation_codes : admin full; organizer reads own redeemed rows;
--     redemption via Edge Function (service role) only.
--   sandbag_flags : admin only (insert via service role).

alter table public.profiles         enable row level security;
alter table public.ratings          enable row level security;
alter table public.rating_history   enable row level security;
alter table public.activation_codes enable row level security;
alter table public.tournaments      enable row level security;
alter table public.divisions        enable row level security;
alter table public.teams            enable row level security;
alter table public.registrations    enable row level security;
alter table public.matches          enable row level security;
alter table public.bracket_slots    enable row level security;
alter table public.sandbag_flags    enable row level security;
alter table public.certificates     enable row level security;

-- ---------------------------------------------------------------------------
-- profiles
-- ---------------------------------------------------------------------------

create policy "profiles are readable by authenticated users"
  on public.profiles for select
  to authenticated
  using (true);

create policy "users update own profile"
  on public.profiles for update
  to authenticated
  using (id = auth.uid())
  with check (id = auth.uid());
-- (privileged columns additionally guarded by the profiles_protect_columns trigger)

create policy "admins update any profile"
  on public.profiles for update
  to authenticated
  using (public.is_admin())
  with check (public.is_admin());

-- Organizers create shell profiles for manual adds (spec §5.4).
create policy "organizers create shell profiles"
  on public.profiles for insert
  to authenticated
  with check (public.is_organizer() and is_shell);

-- ---------------------------------------------------------------------------
-- ratings & rating_history — read-only for clients
-- ---------------------------------------------------------------------------

create policy "ratings are readable by authenticated users"
  on public.ratings for select
  to authenticated
  using (true);

create policy "admins set rating overrides"
  on public.ratings for update
  to authenticated
  using (public.is_admin())
  with check (public.is_admin());

-- Shell players get rating rows created by the organizer device at manual add.
create policy "organizers create shell ratings"
  on public.ratings for insert
  to authenticated
  with check (
    public.is_organizer()
    and exists (select 1 from public.profiles p where p.id = player_id and p.is_shell)
  );

create policy "rating history is readable by authenticated users"
  on public.rating_history for select
  to authenticated
  using (true);
-- no insert/update/delete policies: service role only

-- ---------------------------------------------------------------------------
-- activation_codes
-- ---------------------------------------------------------------------------

create policy "admins manage activation codes"
  on public.activation_codes for all
  to authenticated
  using (public.is_admin())
  with check (public.is_admin());

create policy "organizers read own redeemed codes"
  on public.activation_codes for select
  to authenticated
  using (redeemed_by = auth.uid());
-- redemption + consumption only via Edge Functions (service role)

-- ---------------------------------------------------------------------------
-- tournaments
-- ---------------------------------------------------------------------------

create policy "tournaments readable when not draft"
  on public.tournaments for select
  to authenticated
  using (status <> 'draft' or organizer_id = auth.uid() or public.is_admin());

-- no insert policy: creation goes through consume-code-on-create (service role)

create policy "organizers update own tournaments"
  on public.tournaments for update
  to authenticated
  using (organizer_id = auth.uid() or public.is_admin())
  with check (organizer_id = auth.uid() or public.is_admin());

create policy "organizers delete own draft tournaments"
  on public.tournaments for delete
  to authenticated
  using ((organizer_id = auth.uid() and status = 'draft') or public.is_admin());

-- ---------------------------------------------------------------------------
-- divisions / teams / registrations / matches / bracket_slots
-- organizer of the parent tournament: full CRUD; players: read when visible
-- ---------------------------------------------------------------------------

create policy "divisions readable when tournament visible"
  on public.divisions for select
  to authenticated
  using (public.tournament_visible(tournament_id) or public.owns_tournament(tournament_id));

create policy "organizers manage own divisions"
  on public.divisions for all
  to authenticated
  using (public.owns_tournament(tournament_id))
  with check (public.owns_tournament(tournament_id));

create policy "teams readable when division visible"
  on public.teams for select
  to authenticated
  using (public.division_visible(division_id) or public.owns_division(division_id));

create policy "organizers manage own teams"
  on public.teams for all
  to authenticated
  using (public.owns_division(division_id))
  with check (public.owns_division(division_id));

create policy "registrations readable when division visible"
  on public.registrations for select
  to authenticated
  using (
    public.division_visible(division_id)
    or public.owns_division(division_id)
    or player_id = auth.uid()
  );

create policy "organizers manage own registrations"
  on public.registrations for all
  to authenticated
  using (public.owns_division(division_id))
  with check (public.owns_division(division_id));

create policy "matches readable when division visible"
  on public.matches for select
  to authenticated
  using (public.division_visible(division_id) or public.owns_division(division_id));

create policy "organizers manage own matches"
  on public.matches for all
  to authenticated
  using (public.owns_division(division_id))
  with check (public.owns_division(division_id));

create policy "bracket slots readable when division visible"
  on public.bracket_slots for select
  to authenticated
  using (public.division_visible(division_id) or public.owns_division(division_id));

create policy "organizers manage own bracket slots"
  on public.bracket_slots for all
  to authenticated
  using (public.owns_division(division_id))
  with check (public.owns_division(division_id));

-- ---------------------------------------------------------------------------
-- sandbag_flags — admin review queue (insert via service role)
-- ---------------------------------------------------------------------------

create policy "admins read sandbag flags"
  on public.sandbag_flags for select
  to authenticated
  using (public.is_admin());

create policy "admins action sandbag flags"
  on public.sandbag_flags for update
  to authenticated
  using (public.is_admin())
  with check (public.is_admin());

-- ---------------------------------------------------------------------------
-- certificates
-- ---------------------------------------------------------------------------

create policy "certificates readable when tournament visible"
  on public.certificates for select
  to authenticated
  using (public.tournament_visible(tournament_id) or public.owns_tournament(tournament_id));

create policy "organizers manage own certificates"
  on public.certificates for all
  to authenticated
  using (public.owns_tournament(tournament_id))
  with check (public.owns_tournament(tournament_id));
