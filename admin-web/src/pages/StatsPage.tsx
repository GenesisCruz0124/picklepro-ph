import { useEffect, useState } from "react";
import { supabase } from "../lib/supabase";

interface Stats {
  players: number;
  organizers: number;
  tournaments: number;
  matches: number;
  codesSold: number;
  codesRedeemed: number;
}

async function countOf(query: PromiseLike<{ count: number | null }>): Promise<number> {
  const { count } = await query;
  return count ?? 0;
}

/** Totals (spec §6.6). "Sold" = non-free codes that went out (sent or redeemed). */
export default function StatsPage() {
  const [stats, setStats] = useState<Stats | null>(null);

  useEffect(() => {
    async function load() {
      const [players, organizers, tournaments, matches, codesSold, codesRedeemed] = await Promise.all([
        countOf(supabase.from("profiles").select("id", { count: "exact", head: true }).eq("role", "player")),
        countOf(supabase.from("profiles").select("id", { count: "exact", head: true }).eq("role", "organizer")),
        countOf(supabase.from("tournaments").select("id", { count: "exact", head: true })),
        countOf(supabase.from("matches").select("id", { count: "exact", head: true })),
        countOf(
          supabase
            .from("activation_codes")
            .select("id", { count: "exact", head: true })
            .eq("is_free", false)
            .in("status", ["sent", "redeemed"]),
        ),
        countOf(supabase.from("activation_codes").select("id", { count: "exact", head: true }).eq("status", "redeemed")),
      ]);
      setStats({ players, organizers, tournaments, matches, codesSold, codesRedeemed });
    }
    void load();
  }, []);

  const tiles: [string, number | null][] = [
    ["Players", stats?.players ?? null],
    ["Organizers", stats?.organizers ?? null],
    ["Tournaments", stats?.tournaments ?? null],
    ["Matches", stats?.matches ?? null],
    ["Codes Sold", stats?.codesSold ?? null],
    ["Codes Redeemed", stats?.codesRedeemed ?? null],
  ];

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">Stats</h1>
      <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
        {tiles.map(([label, value]) => (
          <div key={label} className="bg-white rounded-lg shadow p-6">
            <div className="text-3xl font-bold text-pickle-700">{value ?? "…"}</div>
            <div className="text-sm text-gray-500 mt-1">{label}</div>
          </div>
        ))}
      </div>
    </div>
  );
}
