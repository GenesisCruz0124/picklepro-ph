import { useCallback, useEffect, useMemo, useState } from "react";
import type { ColumnDef } from "@tanstack/react-table";
import DataTable from "../components/DataTable";
import { supabase } from "../lib/supabase";

interface OrganizerRow {
  id: string;
  name: string;
  short_code: string | null;
  suspended: boolean;
  created_at: string;
  tournamentCount: number;
  codesRedeemed: number;
}

export default function OrganizersPage() {
  const [rows, setRows] = useState<OrganizerRow[]>([]);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setError(null);
    const [profiles, tournaments, codes] = await Promise.all([
      supabase
        .from("profiles")
        .select("id, name, short_code, suspended, created_at")
        .eq("role", "organizer")
        .order("created_at"),
      supabase.from("tournaments").select("organizer_id"),
      supabase.from("activation_codes").select("redeemed_by").not("redeemed_by", "is", null),
    ]);
    const firstError = profiles.error ?? tournaments.error ?? codes.error;
    if (firstError) {
      setError(firstError.message);
      return;
    }
    const tournamentCounts = new Map<string, number>();
    for (const t of tournaments.data ?? []) {
      tournamentCounts.set(t.organizer_id, (tournamentCounts.get(t.organizer_id) ?? 0) + 1);
    }
    const codeCounts = new Map<string, number>();
    for (const c of codes.data ?? []) {
      if (c.redeemed_by) codeCounts.set(c.redeemed_by, (codeCounts.get(c.redeemed_by) ?? 0) + 1);
    }
    setRows(
      (profiles.data ?? []).map((p) => ({
        ...p,
        tournamentCount: tournamentCounts.get(p.id) ?? 0,
        codesRedeemed: codeCounts.get(p.id) ?? 0,
      })),
    );
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  async function setSuspended(id: string, suspended: boolean) {
    setError(null);
    const { error } = await supabase.from("profiles").update({ suspended }).eq("id", id);
    if (error) setError(error.message);
    else await load();
  }

  const columns = useMemo<ColumnDef<OrganizerRow, any>[]>(
    () => [
      { header: "Name", accessorKey: "name" },
      { header: "Code", accessorKey: "short_code", cell: ({ getValue }) => getValue() ?? "—" },
      { header: "Tournaments", accessorKey: "tournamentCount" },
      { header: "Codes Redeemed", accessorKey: "codesRedeemed" },
      {
        header: "Status",
        accessorKey: "suspended",
        cell: ({ getValue }) =>
          getValue() ? (
            <span className="px-2 py-0.5 rounded text-xs font-medium bg-red-50 text-red-700">suspended</span>
          ) : (
            <span className="px-2 py-0.5 rounded text-xs font-medium bg-pickle-50 text-pickle-700">active</span>
          ),
      },
      { header: "Since", accessorFn: (r) => r.created_at.slice(0, 10) },
      {
        header: "",
        id: "actions",
        cell: ({ row }) => {
          const r = row.original;
          return r.suspended ? (
            <button onClick={() => setSuspended(r.id, false)} className="text-pickle-700 hover:underline">
              Reactivate
            </button>
          ) : (
            <button onClick={() => setSuspended(r.id, true)} className="text-red-600 hover:underline">
              Suspend
            </button>
          );
        },
      },
    ],
    [],
  );

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">Organizers</h1>
      {error && <p className="text-sm text-red-600">{error}</p>}
      <DataTable data={rows} columns={columns} />
    </div>
  );
}
