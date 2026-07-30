// process-match-result — spec §3.2, §3.5
// Body: { match_id: string }
// Runs after a finished match syncs from the organizer device:
//   1. Elo update per player (idempotent per match id via rating_history's
//      unique (player_id, event_type, match_id) constraint).
//   2. rating_history rows.
//   3. Sandbag flag checks within the division (spec §3.5).
//
// Elo rules (spec §3.2):
//   E = 1 / (1 + 10^((eloB - eloA)/400))
//   K = 64 for a player's first 10 matches per event type, then 32.
//   Doubles/Mixed: team rating = average of the pair; expected score is
//   computed from team averages. Each player's delta uses their own K, so
//   provisional players converge faster; past provisional both partners
//   gain/lose the same delta.

import { corsHeaders, json, requireUser, serviceClient } from "../_shared/supabase.ts";
import type { SupabaseClient } from "npm:@supabase/supabase-js@2";

type Game = { a: number; b: number };

async function resolvePlayers(
  svc: SupabaseClient,
  eventType: string,
  sideRef: string,
): Promise<string[]> {
  if (eventType === "singles") {
    const { data } = await svc
      .from("registrations")
      .select("player_id")
      .eq("id", sideRef)
      .maybeSingle();
    return data ? [data.player_id] : [];
  }
  const { data } = await svc
    .from("teams")
    .select("player1_id, player2_id")
    .eq("id", sideRef)
    .maybeSingle();
  if (!data) return [];
  return [data.player1_id, data.player2_id].filter(Boolean) as string[];
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json({ ok: false, error: "method_not_allowed" }, 405);

  const auth = await requireUser(req);
  if ("error" in auth) return auth.error;

  let matchId: string;
  try {
    const body = await req.json();
    matchId = String(body.match_id ?? "");
  } catch {
    return json({ ok: false, error: "invalid_body" }, 400);
  }
  if (!matchId) return json({ ok: false, error: "match_id_required" }, 400);

  const svc = serviceClient();

  const { data: match } = await svc
    .from("matches")
    .select(
      "id, division_id, side_a_ref, side_b_ref, status, winner_ref, games, " +
        "divisions ( id, event_type, game_to, tournament_id, tournaments ( organizer_id ) )",
    )
    .eq("id", matchId)
    .maybeSingle();
  if (!match) return json({ ok: false, error: "match_not_found" }, 404);

  const division = match.divisions as unknown as {
    id: string;
    event_type: string;
    game_to: number;
    tournament_id: string;
    tournaments: { organizer_id: string };
  };

  // Only the tournament's organizer (or an admin) may trigger processing.
  const { data: caller } = await svc
    .from("profiles")
    .select("role")
    .eq("id", auth.userId)
    .single();
  const isOwner = division.tournaments.organizer_id === auth.userId;
  if (!isOwner && caller?.role !== "admin") {
    return json({ ok: false, error: "forbidden" }, 403);
  }

  if (match.status !== "done" && match.status !== "walkover") {
    return json({ ok: false, error: "match_not_finished" }, 409);
  }
  if (!match.winner_ref || !match.side_a_ref || !match.side_b_ref) {
    return json({ ok: false, error: "match_incomplete" }, 409);
  }

  // Idempotency: already processed?
  const { count: processed } = await svc
    .from("rating_history")
    .select("id", { count: "exact", head: true })
    .eq("match_id", matchId);
  if ((processed ?? 0) > 0) {
    return json({ ok: true, already_processed: true });
  }

  // Walkovers record no games and don't move ratings.
  const games = (match.games ?? []) as Game[];
  if (match.status === "walkover" || games.length === 0) {
    await svc.from("matches").update({ synced: true }).eq("id", matchId);
    return json({ ok: true, rating_change: false, reason: "walkover_or_no_games" });
  }

  const eventType = division.event_type;
  const sideAPlayers = await resolvePlayers(svc, eventType, match.side_a_ref);
  const sideBPlayers = await resolvePlayers(svc, eventType, match.side_b_ref);
  if (sideAPlayers.length === 0 || sideBPlayers.length === 0) {
    return json({ ok: false, error: "entrants_unresolved" }, 409);
  }

  const allIds = [...sideAPlayers, ...sideBPlayers];
  const { data: ratingRows } = await svc
    .from("ratings")
    .select("player_id, elo, matches_played")
    .eq("event_type", eventType)
    .in("player_id", allIds);
  const ratings = new Map(
    (ratingRows ?? []).map((r) => [r.player_id, r]),
  );
  // Players without a rating row yet (e.g. shells) start at the default.
  for (const id of allIds) {
    if (!ratings.has(id)) {
      await svc
        .from("ratings")
        .insert({ player_id: id, event_type: eventType, elo: 1000 })
        .select()
        .maybeSingle();
      ratings.set(id, { player_id: id, elo: 1000, matches_played: 0 });
    }
  }

  const avg = (ids: string[]) =>
    ids.reduce((s, id) => s + (ratings.get(id)!.elo as number), 0) / ids.length;
  const eloA = avg(sideAPlayers);
  const eloB = avg(sideBPlayers);
  const expectedA = 1 / (1 + Math.pow(10, (eloB - eloA) / 400));
  const aWon = match.winner_ref === match.side_a_ref;

  const updates: {
    player_id: string;
    elo_before: number;
    elo_after: number;
  }[] = [];
  const apply = (ids: string[], score: number, expected: number) => {
    for (const id of ids) {
      const row = ratings.get(id)!;
      const k = (row.matches_played as number) < 10 ? 64 : 32;
      const before = row.elo as number;
      const after = Math.round(before + k * (score - expected));
      updates.push({ player_id: id, elo_before: before, elo_after: after });
    }
  };
  apply(sideAPlayers, aWon ? 1 : 0, expectedA);
  apply(sideBPlayers, aWon ? 0 : 1, 1 - expectedA);

  // rating_history first: its unique constraint is the idempotency lock, so a
  // concurrent duplicate call fails here before touching ratings.
  const { error: histErr } = await svc.from("rating_history").insert(
    updates.map((u) => ({
      player_id: u.player_id,
      event_type: eventType,
      elo_before: u.elo_before,
      elo_after: u.elo_after,
      match_id: matchId,
    })),
  );
  if (histErr) {
    if (histErr.code === "23505") return json({ ok: true, already_processed: true });
    return json({ ok: false, error: "db_error", detail: histErr.message }, 500);
  }

  for (const u of updates) {
    const played = (ratings.get(u.player_id)!.matches_played as number) + 1;
    await svc
      .from("ratings")
      .update({
        elo: u.elo_after,
        matches_played: played,
        provisional: played < 10,
      })
      .eq("player_id", u.player_id)
      .eq("event_type", eventType);
  }

  await svc.from("matches").update({ synced: true }).eq("id", matchId);

  // -------------------------------------------------------------------------
  // Sandbag checks (spec §3.5) for the winning side's players, within this
  // division: wins >= 5 with win rate > 85%, OR avg point differential >= 7
  // in a game-to-11 division.
  // -------------------------------------------------------------------------
  const flagged: string[] = [];
  const winners = aWon ? sideAPlayers : sideBPlayers;
  const winnerSideRef = match.winner_ref as string;

  const { data: divMatches } = await svc
    .from("matches")
    .select("side_a_ref, side_b_ref, winner_ref, status, games")
    .eq("division_id", division.id)
    .in("status", ["done", "walkover"]);

  for (const playerId of winners) {
    // Which side refs belong to this player in this division?
    const refs = new Set<string>([winnerSideRef]);
    const { data: reg } = await svc
      .from("registrations")
      .select("id, team_id")
      .eq("division_id", division.id)
      .eq("player_id", playerId)
      .maybeSingle();
    if (reg) {
      refs.add(reg.id);
      if (reg.team_id) refs.add(reg.team_id);
    }

    let wins = 0;
    let total = 0;
    let diffSum = 0;
    let gamesCount = 0;
    for (const m of divMatches ?? []) {
      const onA = m.side_a_ref && refs.has(m.side_a_ref);
      const onB = m.side_b_ref && refs.has(m.side_b_ref);
      if (!onA && !onB) continue;
      total += 1;
      if (m.winner_ref && refs.has(m.winner_ref)) wins += 1;
      for (const g of (m.games ?? []) as Game[]) {
        diffSum += onA ? g.a - g.b : g.b - g.a;
        gamesCount += 1;
      }
    }

    const winRate = total > 0 ? wins / total : 0;
    const avgDiff = gamesCount > 0 ? diffSum / gamesCount : 0;
    const rule1 = wins >= 5 && winRate > 0.85;
    const rule2 = division.game_to === 11 && gamesCount > 0 && avgDiff >= 7;
    if (!rule1 && !rule2) continue;

    const { count: openFlags } = await svc
      .from("sandbag_flags")
      .select("id", { count: "exact", head: true })
      .eq("player_id", playerId)
      .eq("division_id", division.id)
      .eq("status", "open");
    if ((openFlags ?? 0) > 0) continue;

    await svc.from("sandbag_flags").insert({
      player_id: playerId,
      division_id: division.id,
      reason: rule1 ? "win_rate" : "point_differential",
      evidence: {
        wins,
        matches: total,
        win_rate: Number(winRate.toFixed(3)),
        avg_point_diff: Number(avgDiff.toFixed(2)),
        trigger_match_id: matchId,
      },
    });
    flagged.push(playerId);
  }

  return json({
    ok: true,
    updates: updates.map((u) => ({
      player_id: u.player_id,
      elo_before: u.elo_before,
      elo_after: u.elo_after,
    })),
    sandbag_flagged: flagged,
  });
});
