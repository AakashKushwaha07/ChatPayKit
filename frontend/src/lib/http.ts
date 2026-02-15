import axios from "axios";

const baseURL =
  import.meta.env.VITE_API_BASE?.trim() ||
  "http://localhost:8080";

export const http = axios.create({
  baseURL,
  withCredentials: true,
});
