// src/services/usuarioLicitaciones.service.ts
import { apiFetch } from "@/services/api";

export type UsuarioLicitacionTipo = "LICIT" | "MENOR";

export type UsuarioLicitacionItem = {
  // Histórico usuario_licitaciones
  id: number;
  tipo: string; // "LICIT" | "MENOR" (el back lo manda como String)
  id_externo: string | null;
  titulo: string | null;
  organismo: string | null;
  fecha_limite: string | null;   // ISO (string)
  importe: number | null;        // BigDecimal -> number
  enlace: string | null;
  cpv: string | null;
  fuente: string | null;
  fecha_envio: string | null;    // ISO (string)
  fecha_creacion: string | null; // ISO (string)

  // ✅ NUEVOS (enriquecidos desde licitaciones + organismo)
  uuid?: string | null;
  organismo_nombre?: string | null;
  organismo_nif?: string | null;
  titulo_licitacion?: string | null;
  tipo_contrato?: string | null;
  fecha_publicacion?: string | null; // ISO (string)
};

export type UsuarioLicitacionesPage = {
  ok: boolean;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  items: UsuarioLicitacionItem[];
  // En el endpoint /api/usuarios/{id}/licitaciones también viene idUsuario
  idUsuario?: number;
};

export type UsuarioLicitacionesQuery = {
  page?: number; // 0-based
  size?: number; // max 200
  tipo?: UsuarioLicitacionTipo | string; // tolerante
  q?: string;
};

function buildQuery(params?: UsuarioLicitacionesQuery): string {
  const p = new URLSearchParams();

  const page = params?.page ?? 0;
  const size = params?.size ?? 50;

  p.set("page", String(page));
  p.set("size", String(size));

  if (params?.tipo) p.set("tipo", String(params.tipo));
  if (params?.q) p.set("q", String(params.q));

  const qs = p.toString();
  return qs ? `?${qs}` : "";
}

/**
 * ✅ Para el usuario LOGUEADO
 * Back: GET /api/me/licitaciones-enviadas
 */
export async function getMisLicitacionesEnviadas(
  params?: UsuarioLicitacionesQuery
): Promise<UsuarioLicitacionesPage> {
  const qs = buildQuery(params);
  const data = await apiFetch<any>(`/api/me/licitaciones-enviadas${qs}`, { method: "GET" });

  return {
    ok: Boolean(data?.ok ?? true),
    page: Number(data?.page ?? 0),
    size: Number(data?.size ?? (params?.size ?? 50)),
    totalElements: Number(data?.totalElements ?? 0),
    totalPages: Number(data?.totalPages ?? 1),
    items: Array.isArray(data?.items) ? data.items : [],
  };
}

/**
 * ✅ Para admin / o si quieres por idUsuario
 * Back: GET /api/usuarios/{idUsuario}/licitaciones
 */
export async function getLicitacionesDeUsuario(
  idUsuario: number,
  params?: UsuarioLicitacionesQuery
): Promise<UsuarioLicitacionesPage> {
  const qs = buildQuery(params);
  const data = await apiFetch<any>(`/api/usuarios/${idUsuario}/licitaciones${qs}`, {
    method: "GET",
  });

  return {
    ok: Boolean(data?.ok ?? true),
    idUsuario: Number(data?.idUsuario ?? idUsuario),
    page: Number(data?.page ?? 0),
    size: Number(data?.size ?? (params?.size ?? 50)),
    totalElements: Number(data?.totalElements ?? 0),
    totalPages: Number(data?.totalPages ?? 1),
    items: Array.isArray(data?.items) ? data.items : [],
  };
}
