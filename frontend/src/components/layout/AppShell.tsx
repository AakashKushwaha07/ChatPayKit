import { Link, useLocation, useNavigate } from "react-router-dom";
import { getUser, logout } from "../../lib/auth";

export default function AppShell({ children }: { children: React.ReactNode }) {
  const user = getUser();
  const loc = useLocation();
  const nav = useNavigate();

  return (
    <div className="min-h-screen bg-black text-white">
      <div className="sticky top-0 z-50 border-b border-zinc-800 bg-zinc-950/80 backdrop-blur">
        <div className="mx-auto max-w-6xl px-4 py-3 flex items-center justify-between">
          <div className="font-semibold">ChatPayKit</div>

          <div className="flex items-center gap-2 text-sm">
            <Link
              to="/"
              className={`px-3 py-2 rounded-md ${
                loc.pathname === "/" ? "bg-zinc-800" : "hover:bg-zinc-900"
              }`}
            >
              Orders
            </Link>

            {user?.role === "ADMIN" && (
              <Link
                to="/settings"
                className={`px-3 py-2 rounded-md ${
                  loc.pathname === "/settings"
                    ? "bg-zinc-800"
                    : "hover:bg-zinc-900"
                }`}
              >
                Settings
              </Link>
            )}

            <button
              onClick={() => {
                logout();
                nav("/login");
              }}
              className="ml-2 px-3 py-2 rounded-md bg-zinc-800 hover:bg-zinc-700"
            >
              Logout
            </button>
          </div>
        </div>
      </div>

      <div className="mx-auto max-w-6xl px-4 py-6">{children}</div>
    </div>
  );
}
