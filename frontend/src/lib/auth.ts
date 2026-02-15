export type AuthUser = {
  userId: string;
  tenantId: string;
  role: "ADMIN" | "STAFF" | string;
};

function base64UrlDecode(input: string) {
  const base64 = input.replace(/-/g, "+").replace(/_/g, "/");
  const padded = base64 + "===".slice((base64.length + 3) % 4);
  return decodeURIComponent(
    atob(padded)
      .split("")
      .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
      .join("")
  );
}

export function decodeJwt(token: string): AuthUser | null {
  try {
    const payload = token.split(".")[1];
    const json = JSON.parse(base64UrlDecode(payload));
    return {
      userId: String(json.userId),
      tenantId: String(json.tenantId),
      role: String(json.role),
    };
  } catch {
    return null;
  }
}

export function setToken(token: string) {
  localStorage.setItem("token", token);
  const user = decodeJwt(token);
  if (user) localStorage.setItem("user", JSON.stringify(user));
}

export function getToken() {
  return localStorage.getItem("token");
}

export function getUser(): AuthUser | null {
  const raw = localStorage.getItem("user");
  if (!raw) return null;
  try { return JSON.parse(raw); } catch { return null; }
}

export function logout() {
  localStorage.removeItem("token");
  localStorage.removeItem("user");
}
