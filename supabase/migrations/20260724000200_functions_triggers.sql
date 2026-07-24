-- PicklePro PH — M1 helper functions & triggers

-- ---------------------------------------------------------------------------
-- Role helpers (security definer so RLS policies can use them without
-- recursing into profiles' own policies)
-- ---------------------------------------------------------------------------

create or replace function public.is_admin()
returns boolean
language sql stable security definer
set search_path = public
as $$
  select exists (
    select 1 from profiles
    where id = auth.uid() and role = 'admin'
  );
$$;

create or replace function public.is_organizer()
returns boolean
language sql stable security definer
set search_path = public
as $$
  select exists (
    select 1 from profiles
    where id = auth.uid() and role in ('organizer', 'admin') and not suspended
  );
$$;

create or replace function public.owns_tournament(t_id uuid)
returns boolean
language sql stable security definer
set search_path = public
as $$
  select exists (
    select 1 from tournaments
    where id = t_id and organizer_id = auth.uid()
  ) or public.is_admin();
$$;

create or replace function public.owns_division(d_id uuid)
returns boolean
language sql stable security definer
set search_path = public
as $$
  select exists (
    select 1
    from divisions d
    join tournaments t on t.id = d.tournament_id
    where d.id = d_id and t.organizer_id = auth.uid()
  ) or public.is_admin();
$$;

-- Players can see tournament data once it leaves draft (spec §7 RLS notes).
create or replace function public.tournament_visible(t_id uuid)
returns boolean
language sql stable security definer
set search_path = public
as $$
  select exists (
    select 1 from tournaments
    where id = t_id and status <> 'draft'
  );
$$;

create or replace function public.division_visible(d_id uuid)
returns boolean
language sql stable security definer
set search_path = public
as $$
  select exists (
    select 1
    from divisions d
    join tournaments t on t.id = d.tournament_id
    where d.id = d_id and t.status <> 'draft'
  );
$$;

-- ---------------------------------------------------------------------------
-- Short code / claim code generation (e.g. PP-8F3K2, spec §4.3)
-- Alphabet omits 0/O/1/I to stay unambiguous on printed QR sheets.
-- ---------------------------------------------------------------------------

create or replace function public.generate_pp_code(prefix text default 'PP-')
returns text
language plpgsql volatile
set search_path = public
as $$
declare
  alphabet constant text := '23456789ABCDEFGHJKMNPQRSTUVWXYZ';
  result   text;
begin
  loop
    result := prefix;
    for i in 1..5 loop
      result := result || substr(alphabet, 1 + floor(random() * length(alphabet))::int, 1);
    end loop;
    exit when not exists (
      select 1 from profiles where short_code = result or claim_code = result
    );
  end loop;
  return result;
end;
$$;

create or replace function public.set_profile_codes()
returns trigger
language plpgsql security definer
set search_path = public
as $$
begin
  if new.short_code is null then
    new.short_code := public.generate_pp_code('PP-');
  end if;
  if new.is_shell and new.claim_code is null then
    new.claim_code := public.generate_pp_code('CL-');
  end if;
  return new;
end;
$$;

create trigger profiles_set_codes
  before insert on public.profiles
  for each row execute function public.set_profile_codes();

-- ---------------------------------------------------------------------------
-- Protect privileged profile columns from client-side edits.
-- Service role bypasses RLS but triggers still run, so allow when there is no
-- authenticated user (service role / migrations) or the caller is an admin.
-- ---------------------------------------------------------------------------

create or replace function public.protect_profile_columns()
returns trigger
language plpgsql security definer
set search_path = public
as $$
begin
  if auth.uid() is null or public.is_admin() then
    return new;
  end if;
  if new.role       is distinct from old.role
     or new.dupr_verified is distinct from old.dupr_verified
     or new.is_shell   is distinct from old.is_shell
     or new.short_code is distinct from old.short_code
     or new.claim_code is distinct from old.claim_code
     or new.suspended  is distinct from old.suspended then
    raise exception 'not allowed to modify protected profile fields';
  end if;
  return new;
end;
$$;

create trigger profiles_protect_columns
  before update on public.profiles
  for each row execute function public.protect_profile_columns();

-- ---------------------------------------------------------------------------
-- Auth signup → profile + per-event-type ratings (spec §3.2, §4.1)
-- Signup metadata (raw_user_meta_data):
--   name            text
--   declared_rating numeric, self-declared starting display rating 2.0–8.0
-- Starting elo = 800 + (display - 2.0) * 400; default 1000 (display 2.5).
-- ---------------------------------------------------------------------------

create or replace function public.handle_new_user()
returns trigger
language plpgsql security definer
set search_path = public
as $$
declare
  declared numeric;
  start_elo integer := 1000;
begin
  declared := nullif(new.raw_user_meta_data ->> 'declared_rating', '')::numeric;
  if declared is not null then
    declared := least(8.0, greatest(2.0, declared));
    start_elo := round(800 + (declared - 2.0) * 400)::integer;
  end if;

  insert into public.profiles (id, name)
  values (new.id, coalesce(new.raw_user_meta_data ->> 'name', ''))
  on conflict (id) do nothing;

  insert into public.ratings (player_id, event_type, elo)
  values (new.id, 'singles', start_elo),
         (new.id, 'doubles', start_elo),
         (new.id, 'mixed',   start_elo)
  on conflict (player_id, event_type) do nothing;

  return new;
end;
$$;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- ---------------------------------------------------------------------------
-- Shell profile claiming (spec §5.4, §12). Called by the claim-shell-profile
-- Edge Function with the service role. Moves the shell's registrations, team
-- memberships and rating history onto the claiming account, adopts the
-- shell's ratings when the claimer has no match history for that event type,
-- then removes the shell profile.
-- ---------------------------------------------------------------------------

create or replace function public.claim_shell_profile(p_claim_code text, p_claimer uuid)
returns jsonb
language plpgsql security definer
set search_path = public
as $$
declare
  shell profiles%rowtype;
  r ratings%rowtype;
begin
  select * into shell
  from profiles
  where claim_code = upper(trim(p_claim_code)) and is_shell
  for update;

  if not found then
    return jsonb_build_object('ok', false, 'error', 'invalid_claim_code');
  end if;
  if shell.id = p_claimer then
    return jsonb_build_object('ok', false, 'error', 'cannot_claim_self');
  end if;
  if not exists (select 1 from profiles where id = p_claimer and not is_shell) then
    return jsonb_build_object('ok', false, 'error', 'claimer_not_found');
  end if;

  -- Registrations: move unless the claimer is already registered in the
  -- same division (then drop the shell's duplicate).
  delete from registrations sr
  where sr.player_id = shell.id
    and exists (
      select 1 from registrations cr
      where cr.player_id = p_claimer and cr.division_id = sr.division_id
    );
  update registrations set player_id = p_claimer where player_id = shell.id;

  update teams set player1_id = p_claimer where player1_id = shell.id;
  update teams set player2_id = p_claimer where player2_id = shell.id;

  update rating_history set player_id = p_claimer where player_id = shell.id;

  -- Adopt shell ratings for event types where the claimer has no history.
  for r in select * from ratings where player_id = shell.id loop
    update ratings
    set elo = r.elo,
        matches_played = r.matches_played,
        provisional = true
    where player_id = p_claimer
      and event_type = r.event_type
      and matches_played = 0;
  end loop;

  update sandbag_flags set player_id = p_claimer where player_id = shell.id;

  delete from profiles where id = shell.id;

  return jsonb_build_object('ok', true, 'claimed_profile', shell.id);
end;
$$;

-- Client code never calls these directly with elevated effect; execution is
-- limited to the service role for the claim function.
revoke execute on function public.claim_shell_profile(text, uuid) from public, anon, authenticated;
