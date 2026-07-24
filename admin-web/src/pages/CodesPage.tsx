import { useCallback, useEffect, useMemo, useState, type FormEvent } from "react";
import type { ColumnDef } from "@tanstack/react-table";
import DataTable from "../components/DataTable";
import { generateActivationCode } from "../lib/codes";
import { downloadCsv } from "../lib/csv";
import { supabase } from "../lib/supabase";

interface CodeRow {
  id: string;
  code: string;
  price_label: number | null;
  is_free: boolean;
  note: string | null;
  status: "generated" | "sent" | "redeemed" | "revoked";
  redeemed_at: string | null;
  created_at: string;
  redeemer: { name: string } | null;
}

export default function CodesPage() {
  const [rows, setRows] = useState<CodeRow[]>([]);
  const [count, setCount] = useState(1);
  const [priceLabel, setPriceLabel] = useState("");
  const [note, setNote] = useState("");
  const [isFree, setIsFree] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [copiedId, setCopiedId] = useState<string | null>(null);

  const load = useCallback(async () => {
    const { data, error } = await supabase
      .from("activation_codes")
      .select("id, code, price_label, is_free, note, status, redeemed_at, created_at, redeemer:profiles!redeemed_by(name)")
      .order("created_at", { ascending: false });
    if (error) setError(error.message);
    else setRows((data as unknown as CodeRow[]) ?? []);
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  async function onGenerate(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    const batch = Array.from({ length: Math.max(1, Math.min(100, count)) }, () => ({
      code: generateActivationCode(),
      price_label: isFree || priceLabel === "" ? null : Number(priceLabel),
      is_free: isFree,
      note: note.trim() || null,
    }));
    const { error } = await supabase.from("activation_codes").insert(batch);
    if (error) setError(error.message);
    else await load();
    setBusy(false);
  }

  async function setStatus(id: string, status: "sent" | "revoked") {
    setError(null);
    const { error } = await supabase.from("activation_codes").update({ status }).eq("id", id);
    if (error) setError(error.message);
    else await load();
  }

  async function copyCode(row: CodeRow) {
    await navigator.clipboard.writeText(row.code);
    setCopiedId(row.id);
    setTimeout(() => setCopiedId(null), 1500);
  }

  function exportCsv() {
    downloadCsv(
      "activation-codes.csv",
      ["Code", "Price (PHP)", "Free", "Note", "Status", "Redeemed By", "Redeemed At", "Created At"],
      rows.map((r) => [
        r.code,
        r.price_label,
        r.is_free ? "yes" : "no",
        r.note,
        r.status,
        r.redeemer?.name ?? null,
        r.redeemed_at,
        r.created_at,
      ]),
    );
  }

  const columns = useMemo<ColumnDef<CodeRow, any>[]>(
    () => [
      {
        header: "Code",
        accessorKey: "code",
        cell: ({ row }) => (
          <button onClick={() => copyCode(row.original)} className="font-mono hover:text-pickle-700" title="Copy">
            {row.original.code}
            {copiedId === row.original.id && <span className="ml-2 text-xs text-pickle-500">copied!</span>}
          </button>
        ),
      },
      {
        header: "Price",
        accessorKey: "price_label",
        cell: ({ row }) =>
          row.original.is_free ? "FREE" : row.original.price_label != null ? `₱${row.original.price_label}` : "—",
      },
      { header: "Note", accessorKey: "note", cell: ({ getValue }) => getValue() ?? "—" },
      {
        header: "Status",
        accessorKey: "status",
        cell: ({ getValue }) => {
          const s = getValue() as CodeRow["status"];
          const tone =
            s === "redeemed"
              ? "bg-pickle-50 text-pickle-700"
              : s === "revoked"
                ? "bg-red-50 text-red-700"
                : s === "sent"
                  ? "bg-blue-50 text-blue-700"
                  : "bg-gray-100 text-gray-700";
          return <span className={`px-2 py-0.5 rounded text-xs font-medium ${tone}`}>{s}</span>;
        },
      },
      {
        header: "Redeemed By",
        accessorFn: (r) => r.redeemer?.name ?? "",
        cell: ({ row }) =>
          row.original.redeemer
            ? `${row.original.redeemer.name} (${row.original.redeemed_at?.slice(0, 10) ?? ""})`
            : "—",
      },
      { header: "Created", accessorFn: (r) => r.created_at.slice(0, 10) },
      {
        header: "",
        id: "actions",
        cell: ({ row }) => {
          const r = row.original;
          return (
            <span className="space-x-2 whitespace-nowrap">
              {r.status === "generated" && (
                <button onClick={() => setStatus(r.id, "sent")} className="text-blue-600 hover:underline">
                  Mark Sent
                </button>
              )}
              {(r.status === "generated" || r.status === "sent") && (
                <button onClick={() => setStatus(r.id, "revoked")} className="text-red-600 hover:underline">
                  Revoke
                </button>
              )}
            </span>
          );
        },
      },
    ],
    [copiedId],
  );

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Activation Codes</h1>
        <button onClick={exportCsv} className="text-sm text-pickle-700 hover:underline">
          Export CSV
        </button>
      </div>

      <form onSubmit={onGenerate} className="bg-white rounded-lg shadow p-4 flex flex-wrap items-end gap-3 text-sm">
        <label className="flex flex-col gap-1">
          Count
          <input
            type="number"
            min={1}
            max={100}
            value={count}
            onChange={(e) => setCount(Number(e.target.value))}
            className="border rounded px-2 py-1 w-20"
          />
        </label>
        <label className="flex flex-col gap-1">
          Price label (₱)
          <input
            type="number"
            min={0}
            step="0.01"
            value={priceLabel}
            onChange={(e) => setPriceLabel(e.target.value)}
            disabled={isFree}
            className="border rounded px-2 py-1 w-28 disabled:bg-gray-100"
          />
        </label>
        <label className="flex flex-col gap-1 flex-1 min-w-40">
          Note
          <input value={note} onChange={(e) => setNote(e.target.value)} className="border rounded px-2 py-1" />
        </label>
        <label className="flex items-center gap-2 pb-1.5">
          <input type="checkbox" checked={isFree} onChange={(e) => setIsFree(e.target.checked)} />
          Free
        </label>
        <button type="submit" disabled={busy} className="bg-pickle-500 text-white rounded px-4 py-1.5 disabled:opacity-50">
          Generate
        </button>
      </form>

      {error && <p className="text-sm text-red-600">{error}</p>}
      <DataTable data={rows} columns={columns} />
    </div>
  );
}
