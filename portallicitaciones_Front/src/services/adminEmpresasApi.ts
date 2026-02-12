import { apiFetch } from "@/services/api";

export type Empresa = {
  idEmpresa?: number;
  nombre: string;
  cif?: string | null;
  email?: string | null;
  telefono?: string | null;
  web?: string | null;
  tamano?: string | null;
  via?: string | null;
  numero?: string | null;
  codigoPostal?: string | null;
  ciudad?: string | null;
  provincia?: string | null;
  fechaCreacion?: string | null;
};

export async function listarEmpresas(): Promise<Empresa[]> {
  const data = await apiFetch<Empresa[]>("/api/admin/empresas", { method: "GET" });
  return data ?? [];
}

export async function obtenerEmpresa(id: number): Promise<Empresa> {
  return apiFetch<Empresa>(`/api/admin/empresas/${id}`, { method: "GET" });
}

export async function crearEmpresa(payload: Empresa): Promise<Empresa> {
  return apiFetch<Empresa>("/api/admin/empresas", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function actualizarEmpresa(id: number, payload: Empresa): Promise<Empresa> {
  return apiFetch<Empresa>(`/api/admin/empresas/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export async function eliminarEmpresa(id: number): Promise<void> {
  await apiFetch<void>(`/api/admin/empresas/${id}`, { method: "DELETE" });
}
