// src/stores/usuarioLicitaciones.store.ts
import { defineStore } from "pinia";
import {
  getMisLicitacionesEnviadas,
  type UsuarioLicitacionItem,
  type UsuarioLicitacionTipo,
} from "@/services/usuarioLicitaciones.service";

type StoreState = {
  items: UsuarioLicitacionItem[];
  loading: boolean;
  error: string | null;

  // filtros
  q: string;
  tipo: UsuarioLicitacionTipo | ""; // "" = todos

  // paginación
  page: number; // 0-based
  size: number;
  totalElements: number;
  totalPages: number;

  // control
  initialized: boolean;
};

export const useUsuarioLicitacionesStore = defineStore("usuarioLicitaciones", {
  state: (): StoreState => ({
    items: [],
    loading: false,
    error: null,

    q: "",
    tipo: "",

    page: 0,
    size: 25,
    totalElements: 0,
    totalPages: 1,

    initialized: false,
  }),

  getters: {
    hasNext: (s) => s.page + 1 < s.totalPages,
    hasPrev: (s) => s.page > 0,
    pageHuman: (s) => s.page + 1,
  },

  actions: {
    reset() {
      this.items = [];
      this.loading = false;
      this.error = null;

      this.q = "";
      this.tipo = "";

      this.page = 0;
      this.size = 25;
      this.totalElements = 0;
      this.totalPages = 1;

      this.initialized = false;
    },

    setSearch(q: string) {
      this.q = (q ?? "").trim();
      this.page = 0; // al cambiar filtro, vuelve a primera página
    },

    setTipo(tipo: UsuarioLicitacionTipo | "") {
      this.tipo = tipo ?? "";
      this.page = 0;
    },

    setSize(size: number) {
      const n = Number(size);
      this.size = Number.isFinite(n) && n > 0 ? n : 25;
      this.page = 0;
    },

    async fetch() {
      this.loading = true;
      this.error = null;

      try {
        const res = await getMisLicitacionesEnviadas({
          page: this.page,
          size: this.size,
          tipo: this.tipo || undefined,
          q: this.q || undefined,
        });

        this.items = res.items ?? [];
        this.page = res.page ?? this.page;
        this.size = res.size ?? this.size;
        this.totalElements = res.totalElements ?? 0;
        this.totalPages = res.totalPages ?? 1;

        this.initialized = true;
      } catch (e: any) {
        const msg =
          e?.message ||
          e?.response?.data?.message ||
          "Error cargando tus licitaciones enviadas.";
        this.error = String(msg);
        this.items = [];
        this.totalElements = 0;
        this.totalPages = 1;
      } finally {
        this.loading = false;
      }
    },

    async goNext() {
      if (!this.hasNext) return;
      this.page += 1;
      await this.fetch();
    },

    async goPrev() {
      if (!this.hasPrev) return;
      this.page -= 1;
      await this.fetch();
    },

    async goPage(page0: number) {
      const p = Number(page0);
      if (!Number.isFinite(p) || p < 0) return;
      if (p >= this.totalPages) return;
      this.page = p;
      await this.fetch();
    },

    // helper típico para cargar al entrar en la vista sin recargar cada vez
    async ensureLoaded() {
      if (this.initialized) return;
      await this.fetch();
    },
  },
});
