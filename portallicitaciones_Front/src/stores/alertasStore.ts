// src/stores/alertas.ts
import { defineStore } from "pinia";
import {
  alertasApi,
  type AlertaCreateRequest,
  type AlertaResponse,
} from "@/services/alertasApi";

export const useAlertasStore = defineStore("alertas", {
  state: () => ({
    items: [] as AlertaResponse[],
    loading: false,
    error: "" as string,
  }),

  actions: {
    async cargarMisAlertas() {
      this.loading = true;
      this.error = "";
      try {
        this.items = await alertasApi.listarMias();
        return this.items;
      } catch (e: any) {
        this.error = e?.message ?? "Error cargando alertas";
        throw e;
      } finally {
        this.loading = false;
      }
    },

    async crearAlerta(payload: AlertaCreateRequest) {
      this.loading = true;
      this.error = "";
      try {
        const creada = await alertasApi.crear(payload);
        // añadimos arriba para que salga la nueva primero
        this.items = [creada, ...this.items];
        return creada;
      } catch (e: any) {
        this.error = e?.message ?? "Error creando alerta";
        throw e;
      } finally {
        this.loading = false;
      }
    },

    async actualizarAlerta(idAlerta: number, payload: AlertaCreateRequest) {
      this.loading = true;
      this.error = "";
      try {
        const actualizada = await alertasApi.actualizar(idAlerta, payload);

        // actualiza en memoria si ya estaba en el listado
        this.items = this.items.map((a) =>
          a.idAlerta === idAlerta ? actualizada : a
        );

        return actualizada;
      } catch (e: any) {
        this.error = e?.message ?? "Error actualizando alerta";
        throw e;
      } finally {
        this.loading = false;
      }
    },

    async eliminarAlerta(idAlerta: number) {
      this.loading = true;
      this.error = "";
      try {
        await alertasApi.eliminar(idAlerta);

        // quítala del listado
        this.items = this.items.filter((a) => a.idAlerta !== idAlerta);
      } catch (e: any) {
        this.error = e?.message ?? "Error eliminando alerta";
        throw e;
      } finally {
        this.loading = false;
      }
    },

    // ✅ alias por compatibilidad (si en la vista llamabas "borrar")
    async borrarAlerta(idAlerta: number) {
      return this.eliminarAlerta(idAlerta);
    },

    // (opcional) limpiar estado de error
    limpiarError() {
      this.error = "";
    },
  },
});
