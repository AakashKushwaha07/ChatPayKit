import axios from "axios";

const baseURL =
  import.meta.env.VITE_API_BASE?.trim() ||
  "http://localhost:8080";

export const http = axios.create({
  baseURL,
  withCredentials: true,
});

// ✅ Attach JWT automatically
http.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers = config.headers ?? {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
