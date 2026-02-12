<template>
  <div class="page">
    <div class="header">
      <div>
        <div class="kicker">ADMIN USUARIOS</div>
        <h1>USUARIOS</h1>
      </div>

      <div class="header-actions">
        <!-- BOTÓN ROSA -->
        <button class="btn-create" @click="goNuevo">
          <span class="btn-icon">+</span>
          Crear usuario
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
              <th style="width: 170px;">Username</th>
              <th style="width: 240px;">Email</th>
              <th style="width: 110px;">Rol</th>
              <th style="width: 110px;">Activo</th>
              <th>Empresa</th>
              <th style="width: 170px;">Suscripción</th>
              <th style="width: 170px;">Acciones</th>
            </tr>
          </thead>

          <tbody>
            <tr v-for="u in usuarios" :key="u.idUsuario">
              <td class="link" @click="goEditar(u.idUsuario!)">
                {{ u.idUsuario }}
              </td>

              <td>{{ u.username }}</td>
              <td>{{ u.email }}</td>

              <td>
                <span class="pill">{{ u.rol }}</span>
              </td>

              <td>
                <span :class="['pill', u.activo ? 'ok' : 'bad']">
                  {{ u.activo ? 'Sí' : 'No' }}
                </span>
              </td>

              <td>
                {{ empresaNombre(u) }}
              </td>

              <td>{{ u.suscripcion || '-' }}</td>

              <td>
                <button class="btn-mini" @click="goEditar(u.idUsuario!)">
                  Editar
                </button>

                <button class="btn-mini danger" @click="onDelete(u.idUsuario!)">
                  Borrar
                </button>
              </td>
            </tr>

            <tr v-if="!loading && usuarios.length === 0">
              <td colspan="8" class="empty">
                No hay usuarios
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
import { eliminarUsuario, listarUsuarios, type UsuarioAdmin } from '@/services/adminUsuariosApi'

const router = useRouter()

const usuarios = ref<UsuarioAdmin[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

function empresaNombre(u: UsuarioAdmin) {
  const embedded = (u as any)?.empresa?.nombre
  if (embedded) return embedded
  if (u.idEmpresa) return `ID ${u.idEmpresa}`
  return '-'
}

async function cargar() {
  error.value = null
  loading.value = true

  try {
    usuarios.value = await listarUsuarios()
  } catch (e: any) {
    error.value =
      e?.response?.data?.message ||
      e?.message ||
      'Error cargando usuarios'
  } finally {
    loading.value = false
  }
}

function goNuevo() {
  router.push('/admin/usuarios/nuevo')
}

function goEditar(id: number) {
  router.push(`/admin/usuarios/${id}`)
}

async function onDelete(id: number) {
  const ok = confirm('¿Seguro que quieres borrar este usuario?')
  if (!ok) return

  try {
    await eliminarUsuario(id)
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

/* ================= PILLS ================= */

.pill {
  display:inline-block;
  padding:4px 10px;
  border-radius:999px;
  border:1px solid #e5e7eb;
  font-weight:700;
  font-size:12px;
}

.pill.ok { border-color:#bbf7d0; color:#166534; }
.pill.bad { border-color:#fecaca; color:#b91c1c; }

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
