import { http } from "../../lib/http";

export const OrdersAPI = {
  list: async () => (await http.get("/api/orders")).data,
  get: async (id: string) => (await http.get(`/api/orders/${id}`)).data,
  sync: (id: string) => http.get(`/api/orders/${id}/sync`).then(r => r.data),
  create: async (payload: any) => (await http.post("/api/orders", payload)).data,
  sendPayment: async (id: string) =>
    (await http.post(`/api/orders/${id}/send-payment`)).data,
  retry: async (id: string) =>
    (await http.post(`/api/orders/${id}/retry`)).data,
  refund: async (id: string) =>
    (await http.post(`/api/orders/${id}/refund`)).data,
  status: async (id: string) =>
    (await http.get(`/api/orders/${id}/status`)).data,
  checkoutUrl: async (id: string) =>
    (await http.get(`/api/orders/${id}/checkout`)).data,
};
