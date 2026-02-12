import { apiFetch } from "@/services/api";

export type UsuarioAdmin = {
  idUsuario?: number;

  username: string;
  email: string;
  rol: string;
  activo: boolean;

  // Empresa
  idEmpresa?: number | null;
  empresa?: any | null; // si el backend devuelve la entidad empresa embebida

  // Extras admin
  telefono?: string | null;
  suscripcion?: string | null;
  fechaBaja?: string | null;
  ultimoAcceso?: string | null;

  alertasConfiguradas?: number;
  recibidas?: number;
  favoritas?: number;
  ultimoFavorito?: string | null;

  // Datos cliente existentes (de tu tabla usuarios)
  idCliente?: number;
  nombreCliente?: string | null;
  emailDestino?: string | null;
  cpvs?: string | null;
  importeMax?: number | null;
  tipoContrato?: string | null;
  estado?: string | null;
  desdeOffset?: number | null;
  hastaOffset?: number | null;
  maxPaginas?: number | null;
  descripcion?: string | null;
  precio?: number | null;

  // Fechas existentes
  fechaCreacion?: string | null;
  fechaUpdate?: string | null;

  // Para crear/actualizar password (en admin)
  passwordHash?: string | null;
};

export async function listarUsuarios(): Promise<UsuarioAdmin[]> {
  const data = await apiFetch<UsuarioAdmin[]>("/api/admin/usuarios", { method: "GET" });
  return data ?? [];
}

export async function obtenerUsuario(id: number): Promise<UsuarioAdmin> {
  return apiFetch<UsuarioAdmin>(`/api/admin/usuarios/${id}`, { method: "GET" });
}

export async function crearUsuario(payload: UsuarioAdmin): Promise<UsuarioAdmin> {
  return apiFetch<UsuarioAdmin>("/api/admin/usuarios", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function actualizarUsuario(id: number, payload: UsuarioAdmin): Promise<UsuarioAdmin> {
  return apiFetch<UsuarioAdmin>(`/api/admin/usuarios/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export async function eliminarUsuario(id: number): Promise<void> {
  await apiFetch<void>(`/api/admin/usuarios/${id}`, { method: "DELETE" });
}
