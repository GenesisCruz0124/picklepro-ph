// claim-shell-profile — spec §5.4, §12
// Body: { claim_code: string }
// Lets an authenticated real account claim a shell player created by an
// organizer's manual add. Delegates the merge to the claim_shell_profile
// Postgres function (service role only).

import { corsHeaders, json, requireUser, serviceClient } from "../_shared/supabase.ts";

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json({ ok: false, error: "method_not_allowed" }, 405);

  const auth = await requireUser(req);
  if ("error" in auth) return auth.error;

  let claimCode: string;
  try {
    const body = await req.json();
    claimCode = String(body.claim_code ?? "").trim().toUpperCase();
  } catch {
    return json({ ok: false, error: "invalid_body" }, 400);
  }
  if (!claimCode) return json({ ok: false, error: "claim_code_required" }, 400);

  const svc = serviceClient();
  const { data, error } = await svc.rpc("claim_shell_profile", {
    p_claim_code: claimCode,
    p_claimer: auth.userId,
  });
  if (error) return json({ ok: false, error: "db_error", detail: error.message }, 500);

  const result = data as { ok: boolean; error?: string };
  if (!result.ok) return json(result, 409);
  return json(result);
});
