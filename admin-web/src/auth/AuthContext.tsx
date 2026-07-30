import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import type { Session } from "@supabase/supabase-js";
import { supabase } from "../lib/supabase";

interface AuthState {
  loading: boolean;
  session: Session | null;
  /** From the user's own profiles row — the guard requires 'admin' (mirrors RLS is_admin()). */
  role: string | null;
}

const AuthContext = createContext<AuthState>({ loading: true, session: null, role: null });

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>({ loading: true, session: null, role: null });

  useEffect(() => {
    let cancelled = false;

    async function resolveRole(session: Session | null) {
      if (!session) {
        if (!cancelled) setState({ loading: false, session: null, role: null });
        return;
      }
      const { data } = await supabase
        .from("profiles")
        .select("role")
        .eq("id", session.user.id)
        .maybeSingle();
      if (!cancelled) setState({ loading: false, session, role: data?.role ?? null });
    }

    supabase.auth.getSession().then(({ data }) => resolveRole(data.session));
    const { data: sub } = supabase.auth.onAuthStateChange((_event, session) => {
      resolveRole(session);
    });
    return () => {
      cancelled = true;
      sub.subscription.unsubscribe();
    };
  }, []);

  return <AuthContext.Provider value={state}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  return useContext(AuthContext);
}
