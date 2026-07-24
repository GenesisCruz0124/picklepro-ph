-- PicklePro PH — M3 Storage: tournament logos (spec §5.3 "logo upload")
--
-- Path convention: {tournament_id}/logo.{ext} — RLS checks the first path
-- segment against owns_tournament() (defined in the M1 functions migration).

insert into storage.buckets (id, name, public)
values ('tournament-logos', 'tournament-logos', true)
on conflict (id) do nothing;

create policy "tournament logos are publicly readable"
  on storage.objects for select
  using (bucket_id = 'tournament-logos');

create policy "organizers upload own tournament logos"
  on storage.objects for insert
  to authenticated
  with check (
    bucket_id = 'tournament-logos'
    and public.owns_tournament(((storage.foldername(name))[1])::uuid)
  );

create policy "organizers replace own tournament logos"
  on storage.objects for update
  to authenticated
  using (
    bucket_id = 'tournament-logos'
    and public.owns_tournament(((storage.foldername(name))[1])::uuid)
  )
  with check (
    bucket_id = 'tournament-logos'
    and public.owns_tournament(((storage.foldername(name))[1])::uuid)
  );

create policy "organizers delete own tournament logos"
  on storage.objects for delete
  to authenticated
  using (
    bucket_id = 'tournament-logos'
    and public.owns_tournament(((storage.foldername(name))[1])::uuid)
  );
