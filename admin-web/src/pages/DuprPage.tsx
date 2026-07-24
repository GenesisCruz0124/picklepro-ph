import { useCallback, useEffect, useState } from "react";
import { supabase } from "../lib/supabase";

interface DuprRow {
  id: string;
  name: string;
  dupr_id: string | null;
  dupr_rating: number | null;
  dupr_proof_url: string | null;
}

/** DUPR verification queue (spec §6.5): pending screenshot proofs → verify / reject. */
export default function DuprPage() {
  const [rows, setRows] = useState<DuprRow[]>([]);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    const { data, error } = await supabase
      .from("profiles")
      .select("id, name, dupr_id, dupr_rating, dupr_proof_url")
      .eq("dupr_verified", false)
      .not("dupr_id", "is", null)
      .not("dupr_proof_url", "is", null);
    if (error) setError(error.message);
    else setRows((data as DuprRow[]) ?? []);
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  async function verify(id: string) {
    setError(null);
    const { error } = await supabase.from("profiles").update({ dupr_verified: true }).eq("id", id);
    if (error) setError(error.message);
    else await load();
  }

  // Reject: clear the proof so it leaves the queue; DUPR id/rating stay, unverified.
  async function reject(id: string) {
    setError(null);
    const { error } = await supabase.from("profiles").update({ dupr_proof_url: null }).eq("id", id);
    if (error) setError(error.message);
    else await load();
  }

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">DUPR Verifications</h1>
      {error && <p className="text-sm text-red-600">{error}</p>}
      {rows.length === 0 && <p className="text-gray-400">No pending verifications.</p>}
      <div className="grid gap-4 md:grid-cols-2">
        {rows.map((row) => (
          <div key={row.id} className="bg-white rounded-lg shadow p-4 space-y-2 text-sm">
            <div className="font-semibold text-base">{row.name}</div>
            <div className="text-gray-600">
              DUPR ID: <span className="font-mono">{row.dupr_id}</span> · Claimed rating: {row.dupr_rating ?? "—"}
            </div>
            {row.dupr_proof_url && (
              <a href={row.dupr_proof_url} target="_blank" rel="noreferrer">
                <img src={row.dupr_proof_url} alt="DUPR proof" className="max-h-64 rounded border" />
              </a>
            )}
            <div className="flex gap-2 pt-1">
              <button onClick={() => verify(row.id)} className="bg-pickle-500 text-white rounded px-3 py-1">
                Verify
              </button>
              <button onClick={() => reject(row.id)} className="text-red-600 hover:underline">
                Reject
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
