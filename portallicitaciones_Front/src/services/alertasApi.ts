import { apiFetch } from "@/services/api";

export type TipoAlerta = "SUMINISTRO" | "SERVICIOS";
export type FrecuenciaEnvio = "DIARIAMENTE" | "SEMANALMENTE" | "INMEDIATO";

export interface AlertaResponse {
  idAlerta: number;
  idUsuario: number;

  nombre: string;

  cpvs: string;
  descripcion: string;
  tipo: TipoAlerta;
  importeMin: number | null;
  importeMax: number | null;
  activa: boolean;
  fechaCreacion: string;

  // parámetros
  palabrasClave?: string | null;
  descripcionActividad?: string | null;
  tiposContratos?: string | null;
  contratosMenores?: boolean | null;
  lugares?: string | null;
  organosContratacion?: string | null;

  // envío
  frecuenciaEnvio?: FrecuenciaEnvio | null;
  horaEnvio?: string | null;     // "10:00"
  diaSemana?: string | null;     // "Lunes"...
  notificarCambios?: boolean | null;
  email1?: string | null;
  email2?: string | null;
  email3?: string | null;
}

export interface AlertaCreateRequest {
  nombre: string;

  cpvs: string;
  descripcion: string;
  tipo: TipoAlerta;
  importeMin: number | null;
  importeMax: number | null;
  activa: boolean;

  // parámetros
  palabrasClave: string;
  descripcionActividad: string;
  tiposContratos: string;
  contratosMenores: boolean;
  lugares: string;
  organosContratacion: string;

  // envío
  frecuenciaEnvio: FrecuenciaEnvio;
  horaEnvio: string;
  diaSemana: string;
  notificarCambios: boolean;
  email1: string;
  email2: string;
  email3: string;
}

export const alertasApi = {
  listarMias() {
    return apiFetch<AlertaResponse[]>("/api/alertas/mias", { method: "GET" });
  },

  // ✅ importante para editar (precargar)
  obtener(idAlerta: number) {
    return apiFetch<AlertaResponse>(`/api/alertas/${idAlerta}`, { method: "GET" });
  },

  crear(req: AlertaCreateRequest) {
    return apiFetch<AlertaResponse>("/api/alertas", {
      method: "POST",
      body: JSON.stringify(req),
    });
  },

  actualizar(idAlerta: number, req: AlertaCreateRequest) {
    return apiFetch<AlertaResponse>(`/api/alertas/${idAlerta}`, {
      method: "PUT",
      body: JSON.stringify(req),
    });
  },

  eliminar(idAlerta: number) {
    return apiFetch<void>(`/api/alertas/${idAlerta}`, { method: "DELETE" });
  },

  // (opcional) alias por compatibilidad con tu código viejo
  getById(idAlerta: number) {
    return this.obtener(idAlerta);
  },
};
