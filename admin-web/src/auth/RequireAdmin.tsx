import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { supabase } from "../lib/supabase";
import { useAuth } from "./AuthContext";

/**
 * Route guard (spec §6.1, §9). RLS is the real enforcement — a non-admin
 * session gets empty/denied queries regardless — this just keeps them out
 * of the UI.
 */
export default function RequireAdmin({ children }: { children: ReactNode }) {
  const { loading, session, role } = useAuth();

  if (loading) {
    return <div className="min-h-screen flex items-center justify-center text-gray-500">Loading…</div>;
  }
  if (!session) return <Navigate to="/login" replace />;
  if (role !== "admin") {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center gap-4">
        <p className="text-gray-700">This account is not an admin.</p>
        <button onClick={() => supabase.auth.signOut()} className="text-pickle-700 underline">
          Sign out
        </button>
      </div>
    );
  }
  return <>{children}</>;
}
