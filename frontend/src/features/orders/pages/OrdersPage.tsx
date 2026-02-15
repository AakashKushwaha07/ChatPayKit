import { useEffect, useMemo, useRef, useState } from "react";
import { OrdersAPI } from "../orders.api";

type Order = {
  id: string;
  customerName?: string;
  customerWhatsapp?: string;
  amountPaise?: number;

  // DB status (may be stale if webhooks not working)
  status?: string;

  // We'll treat this as "liveStatus" coming from /sync response
  liveStatus?: string;

  description?: string | null;
  razorpayOrderId?: string | null;
  razorpayPaymentId?: string | null;

  createdAt?: string;
  paidAt?: string | null;
  failedAt?: string | null;
  refundedAt?: string | null;
};

function StatusBadge({ status }: { status?: string }) {
  const map: Record<string, string> = {
    CREATED: "bg-zinc-800",
    PAYMENT_SENT: "bg-blue-900/60",
    PAID: "bg-emerald-900/60",
    FAILED: "bg-red-900/60",
    EXPIRED: "bg-amber-900/60",
    REFUND_PENDING: "bg-purple-900/60",
    REFUNDED: "bg-zinc-700",
  };

  return (
    <span
      className={`inline-flex px-2 py-1 rounded text-xs ${
        map[status || ""] || "bg-zinc-800"
      }`}
    >
      {status || "UNKNOWN"}
    </span>
  );
}

function rupeesToPaise(rupeesStr: string) {
  const n = Number(rupeesStr);
  if (Number.isNaN(n)) return null;
  return Math.round(n * 100);
}

function formatRupeesFromPaise(amountPaise?: number) {
  if (amountPaise === null || amountPaise === undefined) return "-";
  const n = Number(amountPaise);
  if (Number.isNaN(n)) return "-";
  return `₹${(n / 100).toFixed(2)}`;
}

function shortId(id?: string | null) {
  if (!id) return "-";
  return id.length > 12 ? `${id.slice(0, 8)}…${id.slice(-4)}` : id;
}

const TERMINAL = new Set(["PAID", "REFUNDED", "EXPIRED"]);

export default function OrdersPage() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");
  const [info, setInfo] = useState("");
  const [busyId, setBusyId] = useState<string | null>(null);

  // CreateOrderRequest fields
  const [amountRupees, setAmountRupees] = useState("500");
  const [customerName, setCustomerName] = useState("");
  const [customerWhatsapp, setCustomerWhatsapp] = useState("");
  const [description, setDescription] = useState("");

  // UI helpers
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<"ALL" | string>("ALL");

  // Auto refresh live statuses
  const [autoRefresh, setAutoRefresh] = useState(true);
  const [lastUpdated, setLastUpdated] = useState<string>("-");
  const timerRef = useRef<number | null>(null);

  const total = useMemo(() => orders.length, [orders]);

  const statuses = useMemo(() => {
    const set = new Set<string>();
    orders.forEach((o) => {
      const s = o.liveStatus || o.status;
      if (s) set.add(s);
    });
    return ["ALL", ...Array.from(set)];
  }, [orders]);

  const filteredOrders = useMemo(() => {
    const q = search.trim().toLowerCase();
    return orders.filter((o) => {
      const matchQ =
        !q ||
        (o.customerName || "").toLowerCase().includes(q) ||
        (o.customerWhatsapp || "").toLowerCase().includes(q) ||
        (o.id || "").toLowerCase().includes(q);

      const effectiveStatus = o.liveStatus || o.status || "";
      const matchStatus = statusFilter === "ALL" ? true : effectiveStatus === statusFilter;

      return matchQ && matchStatus;
    });
  }, [orders, search, statusFilter]);

  const loadList = async (silent = false) => {
    if (!silent) {
      setLoading(true);
      setErr("");
      setInfo("");
    }
    try {
      const data = await OrdersAPI.list();
      if (Array.isArray(data)) {
        // keep existing liveStatus if present
        setOrders((prev) => {
          const prevMap = new Map(prev.map((p) => [p.id, p]));
          return data.map((d: any) => {
            const old = prevMap.get(d.id);
            return { ...d, liveStatus: old?.liveStatus || d.status };
          });
        });
      } else {
        setOrders([]);
        setErr("Backend response is not a list.");
      }
      setLastUpdated(new Date().toLocaleTimeString());
    } catch (e: any) {
      setOrders([]);
      setErr(
        e?.response?.data?.message || e?.response?.data || e?.message || "Failed to load orders"
      );
    } finally {
      if (!silent) setLoading(false);
    }
  };

  const syncOne = async (id: string, silent = false) => {
    if (!silent) {
      setBusyId(id);
      setErr("");
      setInfo("");
    }
    try {
      const data = await OrdersAPI.sync(id);
      // /sync returns updated Order (ideal)
      setOrders((prev) =>
        prev.map((o) => (o.id === id ? { ...o, ...(data as any), liveStatus: (data as any).status || o.liveStatus } : o))
      );
      setLastUpdated(new Date().toLocaleTimeString());
      if (!silent) setInfo("Live status updated ✅");
    } catch (e: any) {
      if (!silent) {
        setErr(
          e?.response?.data?.message || e?.response?.data || e?.message || "Sync failed"
        );
      }
    } finally {
      if (!silent) setBusyId(null);
    }
  };

  // Sync all non-terminal orders (lightweight)
  const syncActive = async () => {
    const active = orders
      .filter((o) => !TERMINAL.has(o.liveStatus || o.status || ""))
      .slice(0, 10); // safety: don’t spam if list is huge

    await Promise.allSettled(active.map((o) => syncOne(o.id, true)));
    setLastUpdated(new Date().toLocaleTimeString());
  };

  useEffect(() => {
    loadList();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!autoRefresh) {
      if (timerRef.current) window.clearInterval(timerRef.current);
      timerRef.current = null;
      return;
    }
    timerRef.current = window.setInterval(async () => {
      await loadList(true);
      await syncActive();
    }, 4000);

    return () => {
      if (timerRef.current) window.clearInterval(timerRef.current);
      timerRef.current = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [autoRefresh, orders.length]);

  const onCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setErr("");
    setInfo("");

    const amountPaise = rupeesToPaise(amountRupees);
    if (amountPaise === null) return setErr("Amount must be a number");
    if (amountPaise < 100) return setErr("Amount must be at least ₹1");

    const waDigits = (customerWhatsapp || "").replace(/\s+/g, "");
    if (!/^[0-9]{10,20}$/.test(waDigits)) {
      return setErr("WhatsApp must be digits only (10-20)");
    }

    try {
      await OrdersAPI.create({
        customerName: customerName.trim(),
        customerWhatsapp: waDigits,
        amountPaise,
        description: description.trim() ? description.trim() : null,
      });

      setInfo("Order created ✅");
      setCustomerName("");
      setCustomerWhatsapp("");
      setDescription("");
      setAmountRupees("500");
      await loadList();
    } catch (e: any) {
      setErr(e?.response?.data?.message || e?.response?.data || e?.message || "Create failed");
    }
  };

  const doAction = async (id: string, fn: () => Promise<any>, okMsg: string) => {
    setErr("");
    setInfo("");
    setBusyId(id);
    try {
      await fn();
      setInfo(okMsg);
      await loadList(true);
      await syncOne(id, true); // after action, immediately sync from Razorpay
    } catch (e: any) {
      setErr(e?.response?.data?.message || e?.response?.data || e?.message || "Action failed");
    } finally {
      setBusyId(null);
    }
  };

const openCheckout = async (id: string) => {
  setErr("");
  setInfo("");
  setBusyId(id);

  try {
    // Ensure Razorpay order exists
    const current = orders.find((o) => o.id === id);
    if (!current?.razorpayOrderId) {
      await OrdersAPI.sendPayment(id);
      await loadList(true);
    }

    const r = await OrdersAPI.checkoutUrl(id);

    if (!r?.url) {
      setErr("No checkout url returned");
      return;
    }

    // ✅ IMPORTANT: pass token to checkout.html
    const token = localStorage.getItem("token");
    if (!token) {
      setErr("Missing auth token. Please login again.");
      return;
    }

    const finalUrl =
      r.url + (r.url.includes("?") ? "&" : "?") + `token=${encodeURIComponent(token)}`;

    window.open(finalUrl, "_blank");
    setInfo("Checkout opened ✅");
    await syncOne(id, true);
  } catch (e: any) {
    setErr(
      e?.response?.data?.message ||
        e?.response?.data ||
        e?.message ||
        "Checkout failed"
    );
  } finally {
    setBusyId(null);
  }
};

  const copy = async (text: string, ok = "Copied ✅") => {
    try {
      await navigator.clipboard.writeText(text);
      setInfo(ok);
      setErr("");
    } catch {
      setErr("Clipboard permission blocked");
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
        <div>
          <h1 className="text-2xl font-semibold">Orders ({total})</h1>
          <p className="text-xs text-zinc-500 mt-1">Last updated: {lastUpdated}</p>
        </div>

        <div className="flex flex-col gap-2 md:flex-row md:items-center">
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search name / whatsapp / id"
            className="w-full md:w-64 p-2 bg-zinc-900 border border-zinc-700 rounded"
          />
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="w-full md:w-44 p-2 bg-zinc-900 border border-zinc-700 rounded"
          >
            {statuses.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>

          <label className="flex items-center gap-2 text-sm text-zinc-300 select-none">
            <input
              type="checkbox"
              checked={autoRefresh}
              onChange={(e) => setAutoRefresh(e.target.checked)}
            />
            Auto live refresh
          </label>

          <button
            onClick={() => loadList()}
            className="bg-zinc-100 text-black px-4 py-2 rounded"
          >
            Refresh
          </button>
        </div>
      </div>

      {/* Create Order */}
      <form
        onSubmit={onCreate}
        className="space-y-3 border border-zinc-700 p-4 rounded-lg bg-zinc-950/40"
      >
        <div className="grid gap-3 md:grid-cols-4">
          <input
            value={amountRupees}
            onChange={(e) => setAmountRupees(e.target.value)}
            placeholder="Amount ₹ (e.g., 500 or 500.50)"
            className="w-full p-2 bg-zinc-900 border border-zinc-700 rounded"
          />
          <input
            value={customerName}
            onChange={(e) => setCustomerName(e.target.value)}
            placeholder="Customer Name"
            className="w-full p-2 bg-zinc-900 border border-zinc-700 rounded"
          />
          <input
            value={customerWhatsapp}
            onChange={(e) => setCustomerWhatsapp(e.target.value)}
            placeholder="WhatsApp digits only (10-20)"
            className="w-full p-2 bg-zinc-900 border border-zinc-700 rounded"
          />
          <input
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Description (optional)"
            className="w-full p-2 bg-zinc-900 border border-zinc-700 rounded"
          />
        </div>
        <div className="flex items-center gap-3">
          <button className="bg-white text-black px-4 py-2 rounded">Create Order</button>
          <span className="text-xs text-zinc-400">
            Status in UI is fetched from Razorpay via /sync (not only DB).
          </span>
        </div>
      </form>

      {/* Messages */}
      {err ? (
        <div className="rounded border border-red-900/60 bg-red-950/30 p-3 text-red-200">
          {err}
        </div>
      ) : null}
      {info ? (
        <div className="rounded border border-emerald-900/60 bg-emerald-950/20 p-3 text-emerald-200">
          {info}
        </div>
      ) : null}

      {/* List */}
      {loading ? (
        <div>Loading...</div>
      ) : filteredOrders.length === 0 ? (
        <div className="text-zinc-400">No orders yet</div>
      ) : (
        <div className="space-y-3">
          {filteredOrders.map((o) => {
            const isBusy = busyId === o.id;

            const live = o.liveStatus || o.status || "CREATED";

            const canSendPayment = live === "CREATED" || live === "FAILED";
            const canCheckout = live === "PAYMENT_SENT" || live === "FAILED";
            const canRetry = live === "FAILED";
            const canRefund = live === "PAID";

            return (
              <div
                key={o.id}
                className="border border-zinc-700 p-4 rounded-lg bg-zinc-950/40"
              >
                <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                  <div className="space-y-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <div className="font-semibold">{o.customerName || "-"}</div>
                      <StatusBadge status={live} />
                      {/* optional: show DB status too */}
                      {o.status && o.status !== live ? (
                        <span className="text-xs text-zinc-500">
                          (DB: {o.status})
                        </span>
                      ) : null}
                    </div>

                    <div className="text-sm text-zinc-300">
                      WhatsApp:{" "}
                      <span className="font-mono">{o.customerWhatsapp || "-"}</span>
                    </div>

                    <div className="text-sm text-zinc-300">
                      Amount:{" "}
                      <span className="font-medium">
                        {formatRupeesFromPaise(o.amountPaise)}
                      </span>
                    </div>

                    <div className="text-xs text-zinc-500">
                      Order ID:{" "}
                      <button
                        type="button"
                        className="underline hover:text-zinc-300"
                        onClick={() => copy(o.id, "Order ID copied ✅")}
                      >
                        {o.id}
                      </button>
                    </div>

                    {o.razorpayOrderId ? (
                      <div className="text-xs text-zinc-500">
                        Razorpay Order:{" "}
                        <button
                          type="button"
                          className="underline hover:text-zinc-300"
                          onClick={() =>
                            copy(o.razorpayOrderId!, "Razorpay Order ID copied ✅")
                          }
                        >
                          {shortId(o.razorpayOrderId)}
                        </button>
                      </div>
                    ) : null}

                    {o.razorpayPaymentId ? (
                      <div className="text-xs text-zinc-500">
                        Payment ID:{" "}
                        <button
                          type="button"
                          className="underline hover:text-zinc-300"
                          onClick={() =>
                            copy(o.razorpayPaymentId!, "Payment ID copied ✅")
                          }
                        >
                          {shortId(o.razorpayPaymentId)}
                        </button>
                      </div>
                    ) : null}
                  </div>

                  <div className="flex flex-wrap gap-2">
                    <button
                      type="button"
                      disabled={!canSendPayment || isBusy}
                      onClick={() =>
                        doAction(o.id, () => OrdersAPI.sendPayment(o.id), "Payment request sent ✅")
                      }
                      className={`px-3 py-1.5 rounded text-sm ${
                        canSendPayment && !isBusy
                          ? "bg-blue-600 hover:bg-blue-500"
                          : "bg-zinc-800 text-zinc-400 cursor-not-allowed"
                      }`}
                    >
                      {isBusy ? "Working..." : "Send Payment"}
                    </button>

                    <button
                      type="button"
                      disabled={!canCheckout || isBusy}
                      onClick={() => openCheckout(o.id)}
                      className={`px-3 py-1.5 rounded text-sm ${
                        canCheckout && !isBusy
                          ? "bg-white text-black hover:opacity-90"
                          : "bg-zinc-800 text-zinc-400 cursor-not-allowed"
                      }`}
                    >
                      Checkout
                    </button>

                    <button
                      type="button"
                      disabled={isBusy}
                      onClick={() => syncOne(o.id)}
                      className={`px-3 py-1.5 rounded text-sm ${
                        !isBusy
                          ? "bg-zinc-200 text-black hover:opacity-90"
                          : "bg-zinc-800 text-zinc-400 cursor-not-allowed"
                      }`}
                    >
                      {isBusy ? "Working..." : "Check Live Status"}
                    </button>

                    <button
                      type="button"
                      disabled={!canRetry || isBusy}
                      onClick={() =>
                        doAction(o.id, () => OrdersAPI.retry(o.id), "Retry initiated ✅")
                      }
                      className={`px-3 py-1.5 rounded text-sm ${
                        canRetry && !isBusy
                          ? "bg-zinc-200 text-black hover:opacity-90"
                          : "bg-zinc-800 text-zinc-400 cursor-not-allowed"
                      }`}
                    >
                      Retry
                    </button>

                    <button
                      type="button"
                      disabled={!canRefund || isBusy}
                      onClick={() =>
                        doAction(o.id, () => OrdersAPI.refund(o.id), "Refund initiated ✅")
                      }
                      className={`px-3 py-1.5 rounded text-sm ${
                        canRefund && !isBusy
                          ? "bg-amber-500 text-black hover:opacity-90"
                          : "bg-zinc-800 text-zinc-400 cursor-not-allowed"
                      }`}
                    >
                      Refund
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
