import { useCallback, useEffect, useState } from "react";
import { supabase } from "../lib/supabase";

interface FlagRow {
  id: string;
  player_id: string;
  reason: string;
  evidence: Record<string, unknown> | null;
  created_at: string;
  player: { name: string } | null;
  division: { name: string; event_type: string } | null;
}

/** Sandbag review queue (spec §6.4): dismiss, or set a rating override + note → actioned. */
export default function SandbagPage() {
  const [rows, setRows] = useState<FlagRow[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [noteById, setNoteById] = useState<Record<string, string>>({});
  const [overrideById, setOverrideById] = useState<Record<string, string>>({});

  const load = useCallback(async () => {
    const { data, error } = await supabase
      .from("sandbag_flags")
      .select(
        "id, player_id, reason, evidence, created_at, player:profiles!player_id(name), division:divisions!division_id(name, event_type)",
      )
      .eq("status", "open")
      .order("created_at");
    if (error) setError(error.message);
    else setRows((data as unknown as FlagRow[]) ?? []);
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  async function dismiss(flag: FlagRow) {
    setError(null);
    const { error } = await supabase
      .from("sandbag_flags")
      .update({ status: "dismissed", admin_note: noteById[flag.id]?.trim() || null })
      .eq("id", flag.id);
    if (error) setError(error.message);
    else await load();
  }

  async function applyOverride(flag: FlagRow) {
    setError(null);
    const override = Number(overrideById[flag.id]);
    if (!flag.division || Number.isNaN(override) || override < 2 || override > 8) {
      setError("Override must be a rating between 2.0 and 8.0.");
      return;
    }
    const ratingUpdate = await supabase
      .from("ratings")
      .update({ override })
      .eq("player_id", flag.player_id)
      .eq("event_type", flag.division.event_type);
    if (ratingUpdate.error) {
      setError(ratingUpdate.error.message);
      return;
    }
    const flagUpdate = await supabase
      .from("sandbag_flags")
      .update({ status: "actioned", admin_note: noteById[flag.id]?.trim() || `override set to ${override}` })
      .eq("id", flag.id);
    if (flagUpdate.error) setError(flagUpdate.error.message);
    else await load();
  }

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">Sandbag Review Queue</h1>
      {error && <p className="text-sm text-red-600">{error}</p>}
      {rows.length === 0 && <p className="text-gray-400">No open flags.</p>}
      {rows.map((flag) => (
        <div key={flag.id} className="bg-white rounded-lg shadow p-4 space-y-3 text-sm">
          <div className="flex items-center justify-between">
            <div>
              <span className="font-semibold text-base">{flag.player?.name ?? flag.player_id}</span>
              <span className="text-gray-500">
                {" "}
                — {flag.division?.name ?? "?"} ({flag.division?.event_type ?? "?"})
              </span>
            </div>
            <span className="px-2 py-0.5 rounded text-xs font-medium bg-amber-50 text-amber-700">{flag.reason}</span>
          </div>
          <pre className="bg-gray-50 rounded p-2 text-xs overflow-x-auto">
            {JSON.stringify(flag.evidence, null, 2)}
          </pre>
          <div className="flex flex-wrap items-center gap-2">
            <input
              placeholder="Admin note"
              value={noteById[flag.id] ?? ""}
              onChange={(e) => setNoteById((m) => ({ ...m, [flag.id]: e.target.value }))}
              className="border rounded px-2 py-1 flex-1 min-w-40"
            />
            <input
              placeholder="Override (2.0–8.0)"
              type="number"
              step="0.1"
              min={2}
              max={8}
              value={overrideById[flag.id] ?? ""}
              onChange={(e) => setOverrideById((m) => ({ ...m, [flag.id]: e.target.value }))}
              className="border rounded px-2 py-1 w-36"
            />
            <button onClick={() => applyOverride(flag)} className="bg-pickle-500 text-white rounded px-3 py-1">
              Set Override
            </button>
            <button onClick={() => dismiss(flag)} className="text-gray-600 hover:underline">
              Dismiss
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}
