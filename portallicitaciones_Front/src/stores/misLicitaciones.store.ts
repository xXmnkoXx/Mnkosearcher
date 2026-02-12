// src/stores/misLicitaciones.store.ts
import { defineStore } from "pinia";
import {
  getMisLicitacionesEnviadas,
  type UsuarioLicitacionItem,
} from "@/services/usuarioLicitaciones.service";

export type MisLicitacionesTab =
  | "EN_PLAZO"
  | "FAVORITAS"
  | "DESCARTADAS"
  | "VENCIDAS";

type Counts = Record<MisLicitacionesTab, number>;

const LS_KEY = "misLicitaciones:v1";

/** ✅ Extendemos el item para soportar uuid sin romper el service */
export type UsuarioLicitacionItemWithUuid = UsuarioLicitacionItem & {
  uuid?: string | null;
};

export type DecoratedRow = UsuarioLicitacionItemWithUuid & {
  __favorita: boolean;
  __descartada: boolean;
  __vencida: boolean;
};

function todayStart(): Date {
  const t = new Date();
  t.setHours(0, 0, 0, 0);
  return t;
}

function parseISO(iso?: string | null): Date | null {
  if (!iso) return null;
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? null : d;
}

function isVencida(row: UsuarioLicitacionItemWithUuid): boolean {
  const d = parseISO(row.fecha_limite);
  if (!d) return false;
  const t0 = todayStart();
  const dd = new Date(d);
  dd.setHours(0, 0, 0, 0);
  return dd.getTime() < t0.getTime();
}

function norm(s: any): string {
  return String(s ?? "").toLowerCase().trim();
}

function matchesQuery(row: UsuarioLicitacionItemWithUuid, q: string): boolean {
  const qq = norm(q);
  if (!qq) return true;

  const hay = [
    row.titulo,
    row.organismo,
    row.id_externo,
    row.fuente,
    row.cpv,
    row.uuid, // ✅ por si quieres buscar por uuid
  ]
    .map(norm)
    .join(" | ");

  return hay.includes(qq);
}

/** Favoritas/descartadas se guardan por ID numérico (mantengo tu enfoque) */
function safeLoadSets(): { favoritas: Set<number>; descartadas: Set<number> } {
  try {
    const raw = localStorage.getItem(LS_KEY);
    if (!raw) return { favoritas: new Set(), descartadas: new Set() };

    const obj = JSON.parse(raw);
    const fav = Array.isArray(obj?.favoritas) ? obj.favoritas : [];
    const des = Array.isArray(obj?.descartadas) ? obj.descartadas : [];

    return {
      favoritas: new Set(
        fav
          .map((x: any) => Number(x))
          .filter((n: number) => Number.isFinite(n))
      ),
      descartadas: new Set(
        des
          .map((x: any) => Number(x))
          .filter((n: number) => Number.isFinite(n))
      ),
    };
  } catch {
    return { favoritas: new Set(), descartadas: new Set() };
  }
}

function safeSaveSets(favoritas: Set<number>, descartadas: Set<number>) {
  try {
    const payload = {
      favoritas: Array.from(favoritas.values()),
      descartadas: Array.from(descartadas.values()),
    };
    localStorage.setItem(LS_KEY, JSON.stringify(payload));
  } catch {
    // noop
  }
}

export const useMisLicitacionesStore = defineStore("misLicitaciones", {
  state: () => {
    const { favoritas, descartadas } = safeLoadSets();

    return {
      items: [] as UsuarioLicitacionItemWithUuid[],
      loading: false,
      error: null as string | null,

      tab: "EN_PLAZO" as MisLicitacionesTab,
      q: "",

      favoritas, // Set<number>
      descartadas, // Set<number>
      initialized: false,
    };
  },

  getters: {
    decorated(state): DecoratedRow[] {
      return (state.items || []).map((r) => {
        const fav = state.favoritas.has(r.id);
        const des = state.descartadas.has(r.id);
        const ven = isVencida(r);

        return {
          ...r,
          __favorita: fav,
          __descartada: des,
          __vencida: ven,
        };
      });
    },

    filtered(state): DecoratedRow[] {
      const q = state.q;

      return (this.decorated as DecoratedRow[])
        .filter((r) => matchesQuery(r, q))
        .filter((r) => {
          if (state.tab === "EN_PLAZO") return !r.__vencida && !r.__descartada;
          if (state.tab === "FAVORITAS")
            return r.__favorita && !r.__vencida && !r.__descartada;
          if (state.tab === "DESCARTADAS") return r.__descartada;
          if (state.tab === "VENCIDAS") return r.__vencida;
          return true;
        });
    },

    counts(state): Counts {
      const rows = (this.decorated as DecoratedRow[]).filter((r) =>
        matchesQuery(r, state.q)
      );

      const c: Counts = {
        EN_PLAZO: 0,
        FAVORITAS: 0,
        DESCARTADAS: 0,
        VENCIDAS: 0,
      };

      for (const r of rows) {
        if (!r.__vencida && !r.__descartada) c.EN_PLAZO += 1;
        if (r.__favorita && !r.__vencida && !r.__descartada) c.FAVORITAS += 1;
        if (r.__descartada) c.DESCARTADAS += 1;
        if (r.__vencida) c.VENCIDAS += 1;
      }

      return c;
    },
  },

  actions: {
    setTab(tab: MisLicitacionesTab) {
      this.tab = tab;
    },

    setQuery(q: string) {
      this.q = (q ?? "").trim();
    },

    toggleFavorita(row: UsuarioLicitacionItemWithUuid) {
      if (!row?.id) return;
      if (this.favoritas.has(row.id)) this.favoritas.delete(row.id);
      else this.favoritas.add(row.id);
      safeSaveSets(this.favoritas, this.descartadas);
    },

    toggleDescartada(row: UsuarioLicitacionItemWithUuid) {
      if (!row?.id) return;
      if (this.descartadas.has(row.id)) this.descartadas.delete(row.id);
      else this.descartadas.add(row.id);

      // si descartas, quitamos de favoritas para evitar doble estado
      if (this.descartadas.has(row.id) && this.favoritas.has(row.id)) {
        this.favoritas.delete(row.id);
      }

      safeSaveSets(this.favoritas, this.descartadas);
    },

    async load() {
      this.loading = true;
      this.error = null;

      try {
        const pageSize = 200;
        const maxPages = 20;
        let page = 0;

        const acc: UsuarioLicitacionItemWithUuid[] = [];

        while (page < maxPages) {
          const res = await getMisLicitacionesEnviadas({ page, size: pageSize });

          // ⚠️ el service quizá tipa items sin uuid, por eso casteamos al tipo extendido
          const chunk = (res.items ?? []) as UsuarioLicitacionItemWithUuid[];

          acc.push(...chunk);

          const totalPages = Number(res.totalPages ?? 1);
          page += 1;

          if (page >= totalPages) break;
          if (chunk.length === 0) break;
        }

        this.items = acc;
        this.initialized = true;
      } catch (e: any) {
        this.error =
          e?.message ||
          e?.response?.data?.message ||
          "Error cargando tus licitaciones enviadas.";
        this.items = [];
      } finally {
        this.loading = false;
      }
    },
  },
});
