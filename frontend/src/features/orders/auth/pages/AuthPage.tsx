import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { http } from "../../../../lib/http";
import { setToken } from "../../../../lib/auth";

type Mode = "login" | "signup";

export default function AuthPage() {
  const [mode, setMode] = useState<Mode>("login");
  const navigate = useNavigate();

  const [signupForm, setSignupForm] = useState({
    tenantName: "",
    email: "",
    password: "",
  });

  const [loginForm, setLoginForm] = useState({
    email: "",
    password: "",
  });

  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");

  const onSignup = async () => {
    try {
      setLoading(true);
      setMessage("");

      const res = await http.post("/api/auth/signup", {
        tenantName: signupForm.tenantName.trim(),
        email: signupForm.email.trim(),
        password: signupForm.password,
      });

      setToken(res.data.token);
      navigate("/", { replace: true });
    } catch (err: any) {
      const serverMsg =
        err?.response?.data?.message ||
        (typeof err?.response?.data === "string" ? err.response.data : "") ||
        "";
      setMessage(serverMsg ? `❌ ${serverMsg}` : "❌ Signup failed");
    } finally {
      setLoading(false);
    }
  };

  const onLogin = async () => {
    try {
      setLoading(true);
      setMessage("");

      const res = await http.post("/api/auth/login", {
        email: loginForm.email.trim(),
        password: loginForm.password,
      });

      setToken(res.data.token);
      navigate("/", { replace: true });
    } catch (err: any) {
      const serverMsg =
        err?.response?.data?.message ||
        (typeof err?.response?.data === "string" ? err.response.data : "") ||
        "";
      setMessage(serverMsg ? `❌ ${serverMsg}` : "❌ Login failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-[80vh] flex items-center justify-center px-4">
      <div className="w-full max-w-md rounded-2xl border border-zinc-800 bg-zinc-900/40 p-6 space-y-6">
        <div className="space-y-2">
          <h1 className="text-2xl font-semibold text-white">ChatPayKit</h1>
          <p className="text-sm text-zinc-400">
            Login or create a new business (tenant).
          </p>
        </div>

        <div className="grid grid-cols-2 gap-2">
          <button
            onClick={() => {
              setMode("login");
              setMessage("");
            }}
            className={`rounded-lg p-2 text-sm font-semibold ${
              mode === "login"
                ? "bg-white text-black"
                : "bg-zinc-800 text-zinc-200 hover:bg-zinc-700"
            }`}
          >
            Login
          </button>
          <button
            onClick={() => {
              setMode("signup");
              setMessage("");
            }}
            className={`rounded-lg p-2 text-sm font-semibold ${
              mode === "signup"
                ? "bg-white text-black"
                : "bg-zinc-800 text-zinc-200 hover:bg-zinc-700"
            }`}
          >
            Signup
          </button>
        </div>

        {mode === "login" ? (
          <div className="space-y-4">
            <input
              type="email"
              placeholder="Email"
              value={loginForm.email}
              onChange={(e) =>
                setLoginForm({ ...loginForm, email: e.target.value })
              }
              className="w-full rounded-md bg-zinc-800 p-2 text-sm text-white outline-none"
            />
            <input
              type="password"
              placeholder="Password"
              value={loginForm.password}
              onChange={(e) =>
                setLoginForm({ ...loginForm, password: e.target.value })
              }
              className="w-full rounded-md bg-zinc-800 p-2 text-sm text-white outline-none"
            />
            <button
              onClick={onLogin}
              disabled={loading}
              className="w-full rounded-md bg-green-600 hover:bg-green-700 p-2 text-sm font-semibold disabled:opacity-60"
            >
              {loading ? "Logging in..." : "Login"}
            </button>
          </div>
        ) : (
          <div className="space-y-4">
            <input
              type="text"
              placeholder="Business / Tenant Name"
              value={signupForm.tenantName}
              onChange={(e) =>
                setSignupForm({ ...signupForm, tenantName: e.target.value })
              }
              className="w-full rounded-md bg-zinc-800 p-2 text-sm text-white outline-none"
            />
            <input
              type="email"
              placeholder="Admin Email"
              value={signupForm.email}
              onChange={(e) =>
                setSignupForm({ ...signupForm, email: e.target.value })
              }
              className="w-full rounded-md bg-zinc-800 p-2 text-sm text-white outline-none"
            />
            <input
              type="password"
              placeholder="Password"
              value={signupForm.password}
              onChange={(e) =>
                setSignupForm({ ...signupForm, password: e.target.value })
              }
              className="w-full rounded-md bg-zinc-800 p-2 text-sm text-white outline-none"
            />
            <button
              onClick={onSignup}
              disabled={loading}
              className="w-full rounded-md bg-green-600 hover:bg-green-700 p-2 text-sm font-semibold disabled:opacity-60"
            >
              {loading ? "Creating..." : "Create Tenant + Admin"}
            </button>
          </div>
        )}

        {message && (
          <p className="text-sm text-center text-zinc-300">{message}</p>
        )}
      </div>
    </div>
  );
}
