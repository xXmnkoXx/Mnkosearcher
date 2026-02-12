import { defineStore } from "pinia";
import { getUnificadas, type LicitacionUnificada } from "@/services/licitaciones.service";

export type TabKey = "EN_PLAZO" | "FAVORITAS" | "DESCARTADAS" | "VENCIDAS";

type UiState = { favorita: boolean; descartada: boolean };
type UiMap = Record<string, UiState>;

function loadUiMap(): UiMap {
  try {
    const raw = localStorage.getItem("licis_ui_state");
    if (!raw) return {};
    return JSON.parse(raw);
  } catch {
    return {};
  }
}

function saveUiMap(map: UiMap) {
  try {
    localStorage.setItem("licis_ui_state", JSON.stringify(map));
  } catch {}
}

/**
 * ✅ Normaliza lo que llega del back para que tu tabla no “dependa”
 * de nombres distintos (idExterno vs id, etc.)
 */
export type LicitacionUi = LicitacionUnificada & {
  id: string; // ✅ SIEMPRE existe (para router y keys)
  expediente?: string;
  titulo?: string;

  // alias que tu vista ya usa en algunas funciones
  organismo?: any;
  organo?: string | null;
  entidad?: string | null;

  lugarEjecucion?: string | null;
  fechaPublicacion?: string | null;
  fechaLimitePresentacion?: string | null;

  // campos “de compatibilidad” con tu vista actual
  valorEstimado?: any;
  presupuestoBase?: any;
  moneda?: string | null;
};

function makeId(x: LicitacionUnificada): string {
  if (x.idExterno) return String(x.idExterno);
  const base = [x.fuente ?? "X", x.expediente ?? "", x.titulo ?? ""].join("|");
  return base || crypto.randomUUID();
}

function normalize(x: LicitacionUnificada): LicitacionUi {
  return {
    ...x,
    id: makeId(x),
    expediente: x.expediente,
    titulo: x.titulo,

    organismo: x.organoNombre ?? null,
    organo: x.organoNombre ?? null,
    entidad: x.departamento ?? null,

    lugarEjecucion: x.lugarEjecucion ?? null,
    fechaPublicacion: x.fechaPublicacion ?? null,
    fechaLimitePresentacion: x.fechaLimitePresentacion ?? null,

    valorEstimado: x.valorEstimadoSinIva ?? null,
    presupuestoBase: null,
    moneda: "EUR",
  };
}


function buildBackendParams(estado: "all" | "en_plazo" | "vencidas") {
  return estado === "en_plazo" ? { estadoFase: "Publicada" as const } : {};
}

function isEstadoFasePublicada(row: LicitacionUnificada): boolean {
  const fase = String(row?.estadoFase ?? "").trim().toLowerCase();
  return fase === "publicada";
}
function matchesLocal(row: any, q: string): boolean {
  if (!q) return true;
  const qq = q.trim().toLowerCase();
  if (!qq) return true;

  const organo = String(row?.organoNombre ?? row?.organismo?.nombre ?? row?.organismo ?? "");
  const haystack = [
    row?.titulo ?? "",
    row?.expediente ?? "",
    row?.objeto ?? "",
    organo ?? "",
    row?.departamento ?? "",
    row?.lugarEjecucion ?? "",
    row?.cpvs ?? "",
    row?.cpvPrincipal ?? "",
  ]
    .join(" ")
    .toLowerCase();

  return haystack.includes(qq);
}

export const useLicitacionesStore = defineStore("licitaciones", {
  state: () => ({
    items: [] as LicitacionUi[],
    loading: false,
    error: "" as string,

    // ✅ totales globales (para pintar tabs)
    totalEnPlazo: 0,
    totalVencidas: 0,
    totalGeneral: 0,

    tab: "EN_PLAZO" as TabKey,
    q: "" as string,

    // ✅ paginación BACK (0-based)
    page: 0,
    size: 100,
    totalPages: 1,
    totalElements: 0,

    // source
    source: "mix" as "mix" | "nacional" | "catalunya",

    // ui local
    ui: loadUiMap() as UiMap,
  }),

  getters: {
    enriched(state): Array<LicitacionUi & { __favorita: boolean; __descartada: boolean }> {
      return state.items.map((row) => {
        const id = row.id;
        const ui = state.ui[id] ?? { favorita: false, descartada: false };

        return Object.assign({}, row, {
          __favorita: !!ui.favorita,
          __descartada: !!ui.descartada,
        });
      });
    },

    filtered(): LicitacionUi[] {
      const q = (this.q || "").trim();

      if (this.tab === "FAVORITAS") {
        return (this.enriched as any[]).filter((x) => x.__favorita && matchesLocal(x, q));
      }
      if (this.tab === "DESCARTADAS") {
        return (this.enriched as any[]).filter((x) => x.__descartada && matchesLocal(x, q));
      }

      // ✅ EN_PLAZO / VENCIDAS ya vienen filtradas desde backend
      return (this.enriched as any[]).filter((x) => matchesLocal(x, q));
    },

    // ✅ ahora sí: EN_PLAZO/VENCIDAS son globales (por backend totals)
    counts(): Record<TabKey, number> {
      const favoritas = (this.enriched as any[]).filter((x) => x.__favorita).length;
      const descartadas = (this.enriched as any[]).filter((x) => x.__descartada).length;

      return {
        EN_PLAZO: this.totalEnPlazo,
        VENCIDAS: this.totalVencidas,
        FAVORITAS: favoritas,
        DESCARTADAS: descartadas,
      };
    },
  },

  actions: {
    backendEstadoForTab(tab: TabKey): "all" | "en_plazo" | "vencidas" {
      if (tab === "EN_PLAZO") return "en_plazo";
      if (tab === "VENCIDAS") return "vencidas";
      return "all";
    },

    async refreshBackendTotals() {
      const q = this.q?.trim() ? this.q.trim() : null;

      try {
        const [enPlazoRes, vencidasRes] = await Promise.all([
          getUnificadas({
            source: this.source,
            q,
            estado: "en_plazo",
            ...buildBackendParams("en_plazo"),
            page: 0,
            size: 1, // 👈 mínimo: solo queremos totalElements
          }),
          getUnificadas({
            source: this.source,
            q,
            estado: "vencidas",
            page: 0,
            size: 1,
          }),
        ]);

        this.totalEnPlazo = Number(enPlazoRes.page.totalElements ?? 0);
        this.totalVencidas = Number(vencidasRes.page.totalElements ?? 0);
        this.totalGeneral = this.totalEnPlazo + this.totalVencidas;
      } catch {
        // si falla el conteo, no rompemos la pantalla
        this.totalEnPlazo = 0;
        this.totalVencidas = 0;
        this.totalGeneral = 0;
      }
    },

    async load() {
      this.loading = true;
      this.error = "";

      try {
        // ✅ 1) Totales para tabs (globales)
        await this.refreshBackendTotals();

        // ✅ 2) Página del tab actual
        const estado = this.backendEstadoForTab(this.tab);

        const res = await getUnificadas({
          source: this.source,
          q: this.q?.trim() ? this.q.trim() : null,
          estado,
          ...buildBackendParams(estado),
          page: this.page,
          size: this.size,
        });

        const filteredByEstadoFase =
          estado === "en_plazo"
            ? (res.page.content ?? []).filter((row) => isEstadoFasePublicada(row))
            : res.page.content ?? [];

        const normalized = filteredByEstadoFase.map(normalize);

        this.items = normalized;
        this.totalElements = Number(res.page.totalElements ?? 0);
        this.totalPages = Math.max(1, Number(res.page.totalPages ?? 1));
      } catch (e: any) {
        this.error = e?.message ?? "Error cargando licitaciones";
      } finally {
        this.loading = false;
      }
    },

    async setTab(tab: TabKey) {
      this.tab = tab;
      this.page = 0;
      await this.load();
    },

    async setQuery(q: string) {
      this.q = q;
      this.page = 0;
      await this.load();
    },

    async setSource(source: "mix" | "nacional" | "catalunya") {
      this.source = source;
      this.page = 0;
      await this.load();
    },

    async setPage(page0: number) {
      this.page = Math.max(0, page0);
      await this.load();
    },

    async nextPage() {
      if (this.page + 1 >= this.totalPages) return;
      this.page++;
      await this.load();
    },

    async prevPage() {
      if (this.page <= 0) return;
      this.page--;
      await this.load();
    },

    toggleFavorita(row: { id: string }) {
      const id = String(row.id);
      const current = this.ui[id] ?? { favorita: false, descartada: false };

      this.ui = { ...this.ui, [id]: { favorita: !current.favorita, descartada: false } };
      saveUiMap(this.ui);
    },

    toggleDescartada(row: { id: string }) {
      const id = String(row.id);
      const current = this.ui[id] ?? { favorita: false, descartada: false };

      this.ui = { ...this.ui, [id]: { favorita: false, descartada: !current.descartada } };
      saveUiMap(this.ui);
    },
  },
});
