import { Navigate } from "react-router-dom";
import { getUser } from "../lib/auth";

export default function RequireAuth({
  children,
  role,
}: {
  children: React.ReactNode;
  role?: "ADMIN" | "STAFF";
}) {
  const user = getUser();

  if (!user) return <Navigate to="/login" replace />;
  if (role && user.role !== role) return <Navigate to="/" replace />;

  return <>{children}</>;
}
