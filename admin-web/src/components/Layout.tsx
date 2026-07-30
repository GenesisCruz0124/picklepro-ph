import type { ReactNode } from "react";
import { NavLink } from "react-router-dom";
import { supabase } from "../lib/supabase";

const links = [
  { to: "/", label: "Stats", end: true },
  { to: "/codes", label: "Activation Codes" },
  { to: "/organizers", label: "Organizers" },
  { to: "/sandbag", label: "Sandbag Queue" },
  { to: "/dupr", label: "DUPR Verify" },
];

export default function Layout({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-screen flex bg-gray-50">
      <aside className="w-56 bg-pickle-700 text-white flex flex-col">
        <div className="p-4 font-bold text-lg">PicklePro PH</div>
        <nav className="flex-1">
          {links.map((l) => (
            <NavLink
              key={l.to}
              to={l.to}
              end={l.end}
              className={({ isActive }) =>
                `block px-4 py-2 text-sm ${isActive ? "bg-pickle-500 font-medium" : "hover:bg-pickle-500/50"}`
              }
            >
              {l.label}
            </NavLink>
          ))}
        </nav>
        <button
          onClick={() => supabase.auth.signOut()}
          className="p-4 text-left text-sm text-pickle-50 hover:bg-pickle-500/50"
        >
          Sign out
        </button>
      </aside>
      <main className="flex-1 p-6 overflow-x-auto">{children}</main>
    </div>
  );
}
