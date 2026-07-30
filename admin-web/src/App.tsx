import { Navigate, Route, Routes } from "react-router-dom";
import LoginPage from "./auth/LoginPage";
import RequireAdmin from "./auth/RequireAdmin";
import { useAuth } from "./auth/AuthContext";
import Layout from "./components/Layout";
import CodesPage from "./pages/CodesPage";
import DuprPage from "./pages/DuprPage";
import OrganizersPage from "./pages/OrganizersPage";
import SandbagPage from "./pages/SandbagPage";
import StatsPage from "./pages/StatsPage";

export default function App() {
  const { session } = useAuth();

  return (
    <Routes>
      <Route path="/login" element={session ? <Navigate to="/" replace /> : <LoginPage />} />
      <Route
        path="/*"
        element={
          <RequireAdmin>
            <Layout>
              <Routes>
                <Route path="/" element={<StatsPage />} />
                <Route path="/codes" element={<CodesPage />} />
                <Route path="/organizers" element={<OrganizersPage />} />
                <Route path="/sandbag" element={<SandbagPage />} />
                <Route path="/dupr" element={<DuprPage />} />
                <Route path="*" element={<Navigate to="/" replace />} />
              </Routes>
            </Layout>
          </RequireAdmin>
        }
      />
    </Routes>
  );
}
