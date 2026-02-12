import { http } from "./http";

export async function getLicitaciones() {
  const { data } = await http.get("/api/licitaciones");
  return data;
}
