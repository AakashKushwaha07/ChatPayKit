import { useState } from "react";
import { http } from "../../../lib/http";

export default function SettingsPage() {
  const [form, setForm] = useState({
    razorpayKeyId: "",
    razorpayKeySecret: "",
    razorpayWebhookSecret: "",
    whatsappAccessToken: "",
    whatsappPhoneNumberId: "",
  });

  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const saveSettings = async () => {
    try {
      setLoading(true);
      setMessage("");

      const token = localStorage.getItem("token");
      if (!token) {
        setMessage("❌ Not authenticated");
        return;
      }

      await http.post("/api/admin/settings", form, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      setMessage("✅ Settings saved successfully");
    } catch (err: any) {
      const serverMsg =
        err?.response?.data?.message ||
        (typeof err?.response?.data === "string" ? err.response.data : "") ||
        "";
      setMessage(serverMsg ? `❌ ${serverMsg}` : "❌ Failed to save settings");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="rounded-xl border border-zinc-800 bg-zinc-900/30 p-6 space-y-6">
      <h1 className="text-2xl font-semibold">Admin Settings</h1>

      <div className="space-y-4">
        <input
          type="text"
          name="razorpayKeyId"
          placeholder="Razorpay Key ID"
          value={form.razorpayKeyId}
          onChange={handleChange}
          className="w-full rounded-md bg-zinc-800 p-2 text-sm"
        />

        <input
          type="password"
          name="razorpayKeySecret"
          placeholder="Razorpay Key Secret"
          value={form.razorpayKeySecret}
          onChange={handleChange}
          className="w-full rounded-md bg-zinc-800 p-2 text-sm"
        />

        <input
          type="password"
          name="razorpayWebhookSecret"
          placeholder="Razorpay Webhook Secret"
          value={form.razorpayWebhookSecret}
          onChange={handleChange}
          className="w-full rounded-md bg-zinc-800 p-2 text-sm"
        />

        <input
          type="password"
          name="whatsappAccessToken"
          placeholder="WhatsApp Access Token"
          value={form.whatsappAccessToken}
          onChange={handleChange}
          className="w-full rounded-md bg-zinc-800 p-2 text-sm"
        />

        <input
          type="text"
          name="whatsappPhoneNumberId"
          placeholder="WhatsApp Phone Number ID"
          value={form.whatsappPhoneNumberId}
          onChange={handleChange}
          className="w-full rounded-md bg-zinc-800 p-2 text-sm"
        />

        <button
          onClick={saveSettings}
          disabled={loading}
          className="w-full rounded-md bg-green-600 hover:bg-green-700 p-2 text-sm font-semibold disabled:opacity-60"
        >
          {loading ? "Saving..." : "Save Settings"}
        </button>

        {message && (
          <p className="text-sm text-center text-zinc-300">{message}</p>
        )}
      </div>
    </div>
  );
}
