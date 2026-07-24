// consume-code-on-create — spec §2.2, §5.1, §5.3
// Body: { name, venue?, description?, entry_fee_note?, logo_url?,
//         court_count?, start_date?, end_date? }
// Consumes one unconsumed redeemed activation code owned by the caller and
// creates the tournament (status: draft) bound to that code. One code = one
// tournament regardless of division count (spec §12).

import { corsHeaders, json, requireUser, serviceClient } from "../_shared/supabase.ts";

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json({ ok: false, error: "method_not_allowed" }, 405);

  const auth = await requireUser(req);
  if ("error" in auth) return auth.error;

  let body: Record<string, unknown>;
  try {
    body = await req.json();
  } catch {
    return json({ ok: false, error: "invalid_body" }, 400);
  }
  const name = String(body.name ?? "").trim();
  if (!name) return json({ ok: false, error: "name_required" }, 400);

  const svc = serviceClient();

  const { data: profile } = await svc
    .from("profiles")
    .select("id, role, suspended")
    .eq("id", auth.userId)
    .single();
  if (!profile) return json({ ok: false, error: "profile_not_found" }, 404);
  if (profile.suspended) return json({ ok: false, error: "account_suspended" }, 403);
  if (profile.role !== "organizer" && profile.role !== "admin") {
    return json({ ok: false, error: "not_an_organizer" }, 403);
  }

  // Pick the oldest unconsumed credit.
  const { data: credit } = await svc
    .from("activation_codes")
    .select("id")
    .eq("redeemed_by", auth.userId)
    .eq("status", "redeemed")
    .is("consumed_by_tournament", null)
    .order("redeemed_at", { ascending: true })
    .limit(1)
    .maybeSingle();
  if (!credit) return json({ ok: false, error: "no_credits" }, 402);

  const { data: tournament, error: insertErr } = await svc
    .from("tournaments")
    .insert({
      organizer_id: auth.userId,
      name,
      venue: body.venue ?? null,
      description: body.description ?? null,
      entry_fee_note: body.entry_fee_note ?? null,
      logo_url: body.logo_url ?? null,
      court_count: Number(body.court_count ?? 1) || 1,
      start_date: body.start_date ?? null,
      end_date: body.end_date ?? null,
      status: "draft",
      activation_code_id: credit.id,
    })
    .select("id")
    .single();
  if (insertErr || !tournament) {
    return json({ ok: false, error: "db_error", detail: insertErr?.message }, 500);
  }

  // Bind the credit; guard against a concurrent consume of the same code.
  const { data: consumed, error: consumeErr } = await svc
    .from("activation_codes")
    .update({ consumed_by_tournament: tournament.id })
    .eq("id", credit.id)
    .is("consumed_by_tournament", null)
    .select("id")
    .maybeSingle();
  if (consumeErr || !consumed) {
    await svc.from("tournaments").delete().eq("id", tournament.id);
    return json({ ok: false, error: "credit_conflict_retry" }, 409);
  }

  const { count } = await svc
    .from("activation_codes")
    .select("id", { count: "exact", head: true })
    .eq("redeemed_by", auth.userId)
    .eq("status", "redeemed")
    .is("consumed_by_tournament", null);

  return json({ ok: true, tournament_id: tournament.id, credits_left: count ?? 0 });
});
