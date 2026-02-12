<template>
  <div class="page">
    <header class="page-title">
      <h1>MIS ALERTAS</h1>
    </header>

    <section class="card">
      <button class="btn-create" type="button" @click="goCreate">
        <span class="plus">＋</span>
        Crear alerta
      </button>

      <div v-if="store.loading" class="muted">Cargando alertas…</div>
      <div v-else-if="store.error" class="error">{{ store.error }}</div>

      <div v-else>
        <div v-if="store.items.length === 0" class="muted">Aún no tienes alertas.</div>

        <div v-else class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th class="col-name">NOMBRE DE LA ALERTA</th>
                <th class="col-freq">TIPO</th>
                <th class="col-last">CPVS</th>
                <th class="col-num">ACTIVA</th>
                <th class="col-actions">ACCIONES</th>
              </tr>
            </thead>

            <tbody>
              <tr v-for="a in store.items" :key="a.idAlerta">
                <td class="name">
                  <a class="name-link" href="javascript:void(0)" @click="goEdit(a.idAlerta)">
                    {{ a.nombre }}
                  </a>
                </td>

                <td class="freq">{{ a.tipo }}</td>

                <td class="last">
                  <span class="mono">{{ a.cpvs }}</span>
                </td>

                <td class="num">
                  <span :class="a.activa ? 'ok' : 'no'">{{ a.activa ? "Sí" : "No" }}</span>
                </td>

                <td class="actions">
                  <button class="btn-mini" type="button" @click="goEdit(a.idAlerta)">Editar</button>
                  <button class="btn-mini danger" type="button" @click="confirmDelete(a.idAlerta, a.descripcion)">
                    Eliminar
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from "vue"
import { useRouter } from "vue-router"
import { useAlertasStore } from "@/stores/alertasStore"

const router = useRouter()
const store = useAlertasStore()

function goCreate() {
  router.push("/alertas/nueva")
}

function goEdit(idAlerta: number) {
  router.push(`/alertas/${idAlerta}/configurar`)
}

async function confirmDelete(idAlerta: number, nombre: string) {
  const ok = confirm(`¿Seguro que quieres eliminar la alerta "${nombre}"?`)
  if (!ok) return
  await store.borrarAlerta(idAlerta)
}

onMounted(() => {
  store.cargarMisAlertas()
})
</script>

<style scoped>
.page {
  padding: 16px 18px 22px;
  background: #f1f3f7;
  min-height: calc(100vh - 56px);
  font-family: Inter, system-ui, Arial, sans-serif;
}

.page-title h1 {
  margin: 0 0 12px;
  letter-spacing: 0.8px;
  font-size: 18px;
  color: #1f2a44;
  font-weight: 800;
}

.card {
  position: relative;
  background: #ffffff;
  border-radius: 14px;
  padding: 24px 26px 18px;
  box-shadow: 0 14px 38px rgba(16, 24, 40, 0.08);
  min-height: 420px;
}

.btn-create {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: #d06a8e;
  color: #fff;
  border: 0;
  border-radius: 8px;
  height: 30px;
  padding: 0 12px;
  font-weight: 800;
  font-size: 12px;
  cursor: pointer;
  box-shadow: 0 8px 18px rgba(208, 106, 142, 0.24);
  margin-bottom: 14px;
}

.plus {
  display: inline-grid;
  place-items: center;
  width: 16px;
  height: 16px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.18);
  line-height: 16px;
}

.table-wrap {
  border-top: 1px solid #e5e9f2;
}

.table {
  width: 100%;
  border-collapse: collapse;
}

.table thead th {
  text-align: left;
  font-size: 11px;
  letter-spacing: 0.7px;
  color: #7b869a;
  padding: 12px 10px;
  font-weight: 800;
  border-bottom: 1px solid #e5e9f2;
}

.table tbody td {
  padding: 14px 10px;
  border-bottom: 1px solid #e5e9f2;
  color: #2b3550;
  font-size: 13px;
  vertical-align: top;
}

.col-name { width: 34%; }
.col-freq { width: 14%; }
.col-last { width: 26%; }
.col-num { width: 10%; }
.col-actions { width: 16%; }

.name-link {
  color: #1b62ff;
  text-decoration: underline;
  cursor: pointer;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
  font-size: 12px;
}

.ok { color: #15803d; font-weight: 900; }
.no { color: #b42318; font-weight: 900; }

.actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.btn-mini {
  height: 28px;
  padding: 0 10px;
  border-radius: 8px;
  border: 1px solid #d8e1ef;
  background: #fff;
  color: #1f2a44;
  font-weight: 900;
  cursor: pointer;
  font-size: 12px;
}

.btn-mini.danger {
  border-color: rgba(180, 35, 24, 0.25);
  color: #b42318;
}

.muted { color: #7b869a; }
.error { color: #b91c1c; font-weight: 800; }
</style>
