// redeem-code — spec §5.1
// Body: { code: string }
// Validates an activation code, binds it to the calling account
// (status → redeemed) and upgrades the account to organizer.

import { corsHeaders, json, requireUser, serviceClient } from "../_shared/supabase.ts";

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json({ ok: false, error: "method_not_allowed" }, 405);

  const auth = await requireUser(req);
  if ("error" in auth) return auth.error;

  let code: string;
  try {
    const body = await req.json();
    code = String(body.code ?? "").trim().toUpperCase();
  } catch {
    return json({ ok: false, error: "invalid_body" }, 400);
  }
  if (!code) return json({ ok: false, error: "code_required" }, 400);

  const svc = serviceClient();

  const { data: profile } = await svc
    .from("profiles")
    .select("id, role, suspended")
    .eq("id", auth.userId)
    .single();
  if (!profile) return json({ ok: false, error: "profile_not_found" }, 404);
  if (profile.suspended) return json({ ok: false, error: "account_suspended" }, 403);

  // Atomic redeem: only flips codes still in generated/sent state.
  const { data: redeemed, error } = await svc
    .from("activation_codes")
    .update({
      status: "redeemed",
      redeemed_by: auth.userId,
      redeemed_at: new Date().toISOString(),
    })
    .eq("code", code)
    .in("status", ["generated", "sent"])
    .select("id, code")
    .maybeSingle();

  if (error) return json({ ok: false, error: "db_error", detail: error.message }, 500);
  if (!redeemed) {
    const { data: existing } = await svc
      .from("activation_codes")
      .select("status")
      .eq("code", code)
      .maybeSingle();
    if (!existing) return json({ ok: false, error: "code_not_found" }, 404);
    return json({ ok: false, error: `code_${existing.status}` }, 409);
  }

  if (profile.role === "player") {
    await svc.from("profiles").update({ role: "organizer" }).eq("id", auth.userId);
  }

  const { count } = await svc
    .from("activation_codes")
    .select("id", { count: "exact", head: true })
    .eq("redeemed_by", auth.userId)
    .eq("status", "redeemed")
    .is("consumed_by_tournament", null);

  return json({ ok: true, code_id: redeemed.id, credits: count ?? 0 });
});
