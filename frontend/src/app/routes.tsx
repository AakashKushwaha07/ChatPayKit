import { Routes, Route } from "react-router-dom";
import OrdersPage from "../features/orders/pages/OrdersPage";
import SettingsPage from "../features/settings/pages/SettingsPage";
import AuthPage from "../features/orders/auth/pages/AuthPage";
import RequireAuth from "../components/RequireAuth";
import AppShell from "../components/layout/AppShell";

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<AuthPage />} />

      <Route
        path="/"
        element={
          <RequireAuth>
            <AppShell>
              <OrdersPage />
            </AppShell>
          </RequireAuth>
        }
      />

      <Route
        path="/settings"
        element={
          <RequireAuth role="ADMIN">
            <AppShell>
              <SettingsPage />
            </AppShell>
          </RequireAuth>
        }
      />
    </Routes>
  );
}
