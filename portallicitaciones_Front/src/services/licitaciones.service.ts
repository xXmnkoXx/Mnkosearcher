import { http } from "@/api/http";

export type PageResponse<T> = {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number; // page actual (0-based)
  first: boolean;
  last: boolean;
  numberOfElements: number;
  empty: boolean;
};

type WrappedPageResponse<T> = {
  params?: any;
  page: PageResponse<T>;
};

export type LicitacionUnificada = {
  fuente?: "NACIONAL" | "CATALUNYA" | string;

  idExterno?: string;

  expediente?: string;
  titulo?: string;
  objeto?: string;

  organoNombre?: string;
  organoDir3?: string | null;
  departamento?: string | null;

  tipoContrato?: string | null;
  procedimiento?: string | null;
  estadoFase?: string | null;

  cpvPrincipal?: string | null;
  cpvs?: string | null;
  nuts?: string | null;

  lugarEjecucion?: string | null;

  valorEstimadoSinIva?: number | null;
  presupuestoSinIva?: number | null;
  presupuestoConIva?: number | null;

  importeAdjudicacionSinIva?: number | null;
  importeAdjudicacionConIva?: number | null;

  adjudicatarioNombre?: string | null;
  adjudicatarioId?: string | null;

  numOfertas?: number | null;

  fechaPublicacion?: string | null; // ISO
  fechaLimitePresentacion?: string | null; // ISO
  fechaAdjudicacion?: string | null; // ISO

  duracion?: string | null;
  enlace?: string | null;

  [k: string]: any;
};

function unwrapWrappedPage<T>(data: any): WrappedPageResponse<T> {
  // ✅ soporta: {params,page:{...}} o {page:{...}} o directamente {...}
  if (data?.page?.content) return data as WrappedPageResponse<T>;
  if (data?.content) return { page: data as PageResponse<T>, params: undefined };
  return {
    page: {
      content: [],
      totalPages: 1,
      totalElements: 0,
      size: 0,
      number: 0,
      first: true,
      last: true,
      numberOfElements: 0,
      empty: true,
    },
    params: undefined,
  };
}

function cleanQ(q?: string | null): string | null {
  if (q == null) return null;
  const t = q.trim();
  return t ? t : null;
}

// ✅ Tipos de params unificados (para evitar duplicar en 2 funciones)
export type GetUnificadasParams = {
  source?: string;
  q?: string | null;
  estado?: "all" | "en_plazo" | "vencidas";
  estadoFase?: string | null; // ✅ AÑADIDO (ej: "publicada")
  page?: number;
  size?: number;
};

// ✅ Nuevo: devuelve wrapped {params, page}
export async function getUnificadas(params: GetUnificadasParams) {
  const resp = await http.get<
    WrappedPageResponse<LicitacionUnificada> | PageResponse<LicitacionUnificada>
  >("/api/licitaciones/unificadas", {
    params: {
      ...params,
      q: cleanQ(params.q),
      // ✅ opcional: normaliza estadoFase (trim y null si vacío)
      estadoFase: params.estadoFase != null ? cleanQ(params.estadoFase) : undefined,
    },
  });

  return unwrapWrappedPage<LicitacionUnificada>(resp.data);
}

// ✅ Compat: si en algún sitio quieres solo la page
export async function getUnificadasPage(params: GetUnificadasParams) {
  const wrapped = await getUnificadas(params);
  return wrapped.page;
}

/**
 * Si quieres también consumir SOLO catalunya desde el front:
 */
export type LicitacionCatalunya = {
  licitacionCatalunyaId: number;
  enllacPublicacio?: string;
  codiExpedient?: string;
  denominacio?: string;
  objecteContracte?: string;
  nomOrgan?: string;
  codiDir3?: string;
  nomDepartamentEns?: string;
  tipusContracte?: string;
  procediment?: string;
  fasePublicacio?: string;
  codiCpv?: string;
  codiNuts?: string;
  llocExecucio?: string;
  valorEstimatContracte?: number;
  pressupostLicitacioSenseIva?: number;
  pressupostLicitacioAmbIva?: number;
  importAdjudicacioSenseIva?: number;
  importAdjudicacioAmbIva?: number;
  denominacioAdjudicatari?: string;
  identificacioAdjudicatari?: string;
  ofertesRebudes?: number;
  dataPublicacioAnunci?: string;
  terminiPresentacioOfertes?: string;
  dataAdjudicacioContracte?: string;
  duradaContracte?: string;
};

export async function getCatalunya(params: { q?: string | null; page?: number; size?: number }) {
  const resp = await http.get<
    PageResponse<LicitacionCatalunya> | WrappedPageResponse<LicitacionCatalunya>
  >("/api/licitaciones/catalunya", {
    params: {
      ...params,
      q: cleanQ(params.q),
    },
  });

  const wrapped = unwrapWrappedPage<LicitacionCatalunya>(resp.data);
  return wrapped.page;
}
