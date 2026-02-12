import { apiFetch } from "@/services/api";

/**
 * Devuelve el detalle de una licitación.
 */
export async function getLicitacionDetalle(id: string | number) {

  const data = await apiFetch<any>(`/api/licitaciones/${id}`, {
    method: "GET"
  });

  return data;
}
