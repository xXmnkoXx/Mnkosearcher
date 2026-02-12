<template>
  <div class="page">
    <div class="header">
      <div>
        <div class="kicker">ADMIN EMPRESAS</div>
        <h1>EMPRESAS</h1>
      </div>

      <div class="header-actions">
        <!-- BOTÓN ROSA -->
        <button class="btn-create" @click="goNueva">
          <span class="btn-icon">+</span>
          Crear empresa
        </button>

        <!-- BOTÓN AZUL -->
        <button class="btn-secondary" @click="cargar" :disabled="loading">
          {{ loading ? 'Cargando...' : 'Actualizar' }}
        </button>
      </div>
    </div>

    <section class="card">
      <p v-if="error" class="error">{{ error }}</p>

      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th style="width: 90px;">ID</th>
              <th>Nombre</th>
              <th style="width: 140px;">CIF</th>
              <th style="width: 260px;">Email</th>
              <th style="width: 160px;">Teléfono</th>
              <th style="width: 160px;">Ciudad</th>
              <th style="width: 160px;">Provincia</th>
              <th style="width: 170px;">Acciones</th>
            </tr>
          </thead>

          <tbody>
            <tr v-for="e in empresas" :key="e.idEmpresa">
              <td class="link" @click="goEditar(e.idEmpresa!)">
                {{ e.idEmpresa }}
              </td>

              <td>{{ e.nombre }}</td>
              <td>{{ e.cif || '-' }}</td>
              <td>{{ e.email || '-' }}</td>
              <td>{{ e.telefono || '-' }}</td>
              <td>{{ e.ciudad || '-' }}</td>
              <td>{{ e.provincia || '-' }}</td>

              <td>
                <button class="btn-mini" @click="goEditar(e.idEmpresa!)">
                  Editar
                </button>

                <button class="btn-mini danger" @click="onDelete(e.idEmpresa!)">
                  Borrar
                </button>
              </td>
            </tr>

            <tr v-if="!loading && empresas.length === 0">
              <td colspan="8" class="empty">
                No hay empresas
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { eliminarEmpresa, listarEmpresas, type Empresa } from '@/services/adminEmpresasApi'

const router = useRouter()

const empresas = ref<Empresa[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

async function cargar() {
  error.value = null
  loading.value = true

  try {
    empresas.value = await listarEmpresas()
  } catch (e: any) {
    error.value =
      e?.response?.data?.message ||
      e?.message ||
      'Error cargando empresas'
  } finally {
    loading.value = false
  }
}

function goNueva() {
  router.push('/admin/empresas/nueva')
}

function goEditar(id: number) {
  router.push(`/admin/empresas/${id}`)
}

async function onDelete(id: number) {
  const ok = confirm('¿Seguro que quieres borrar esta empresa?')
  if (!ok) return

  try {
    await eliminarEmpresa(id)
    await cargar()
  } catch (e: any) {
    alert(
      e?.response?.data?.message ||
      e?.message ||
      'No se pudo borrar'
    )
  }
}

onMounted(cargar)
</script>

<style scoped>
.page { padding: 24px; }

.header {
  display:flex;
  align-items:flex-end;
  justify-content:space-between;
  margin-bottom:16px;
}

.kicker { font-size:12px; opacity:.6; margin-bottom:4px; }

h1 {
  margin:0;
  font-size:18px;
  font-weight:800;
  color:#1f2937;
}

.header-actions { display:flex; gap:10px; }

/* ================= BOTÓN ROSA ================= */

.btn-create {
  display: inline-flex;
  align-items: center;
  gap: 8px;

  height: 32px;
  padding: 0 14px;

  background: #ec5b8c;
  color: #ffffff;

  border: none;
  border-radius: 8px;

  font-size: 12px;
  font-weight: 800;

  cursor: pointer;

  box-shadow: 0 4px 12px rgba(236, 91, 140, 0.35);
  transition: all .15s ease;
}

.btn-create:hover {
  background: #e24c80;
}

.btn-icon {
  width: 18px;
  height: 18px;
  border-radius: 6px;
  background: rgba(255,255,255,.22);

  display: grid;
  place-items: center;

  font-size: 14px;
  font-weight: 900;
}

/* ================= BOTÓN AZUL ================= */

.btn-secondary {
  background:white;
  color:#0b5ed7;
  border:1px solid #cfe0ff;
  border-radius:10px;
  padding:8px 12px;
  cursor:pointer;
  font-weight:700;
}

.btn-secondary:disabled {
  opacity:.6;
  cursor:not-allowed;
}

/* ================= CARD ================= */

.card {
  background: white;
  border-radius: 14px;
  padding: 16px;
  box-shadow: 0 10px 30px rgba(0,0,0,.06);
}

/* ================= TABLE ================= */

.table-wrap {
  overflow:auto;
  border-radius: 12px;
  border: 1px solid #eef2f7;
}

.table {
  width:100%;
  border-collapse: collapse;
  font-size: 13px;
}

th, td {
  padding: 10px 12px;
  border-bottom: 1px solid #eef2f7;
  text-align:left;
  white-space: nowrap;
}

th {
  background:#f8fafc;
  font-weight: 700;
  color:#334155;
}

.link { color:#0b5ed7; font-weight:700; cursor:pointer; }

.empty { text-align:center; padding:20px; opacity:.7; }

/* ================= BOTONES TABLA ================= */

.btn-mini {
  background:white;
  border:1px solid #e5e7eb;
  border-radius:10px;
  padding:6px 10px;
  cursor:pointer;
  margin-right:6px;
}

.btn-mini.danger {
  border-color:#fecaca;
  color:#b91c1c;
}

/* ================= ERROR ================= */

.error {
  color:#b91c1c;
  font-weight:700;
  margin-bottom: 10px;
}
</style>
