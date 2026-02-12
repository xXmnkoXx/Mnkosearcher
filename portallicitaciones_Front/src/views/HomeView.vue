<template>
  <div class="page">
    <header class="header">
      <h1>MIS LICITACIONES</h1>

      <div class="right">
        <button class="btn" @click="reload" :disabled="loading">
          {{ loading ? 'Cargando...' : 'Recargar' }}
        </button>
        <button class="btn danger" @click="logout">
          Cerrar sesión
        </button>
      </div>
    </header>

    <section class="card">
      <div v-if="loading" class="muted">Cargando licitaciones...</div>

      <div v-else-if="error" class="error">
        {{ error }}
      </div>

      <div v-else>
        <div class="muted" v-if="licitaciones.length === 0">
          No hay resultados.
        </div>

        <div class="list" v-else>
          <article v-for="l in licitaciones" :key="l.id" class="item">
            <div class="top">
              <div class="title">{{ l.titulo || l.objeto || 'Sin título' }}</div>
              <div class="pill">{{ l.estado || '—' }}</div>
            </div>

            <div class="meta">
              <span><b>Exp:</b> {{ l.expediente || l.numeroExpediente || '—' }}</span>
              <span><b>Org:</b> {{ l.organismo || l.organo || '—' }}</span>
              <span><b>Pub:</b> {{ formatDate(l.fechaPublicacion || l.publicacion) }}</span>
            </div>

            <div class="desc" v-if="l.descripcion">
              {{ l.descripcion }}
            </div>
          </article>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { apiFetch } from '@/services/api'

type Licitacion = {
  id: string | number
  titulo?: string
  objeto?: string
  descripcion?: string
  expediente?: string
  numeroExpediente?: string
  organismo?: string
  organo?: string
  estado?: string
  fechaPublicacion?: string
  publicacion?: string
}

const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const error = ref<string | null>(null)
const licitaciones = ref<Licitacion[]>([])

function formatDate(v?: string) {
  if (!v) return '—'
  const d = new Date(v)
  if (Number.isNaN(d.getTime())) return v
  return d.toLocaleDateString()
}

function clearAuthAndGoLogin() {
  // soporta distintos nombres de acción sin romper TS
  if (typeof (auth as any).logout === 'function') (auth as any).logout()
  else if (typeof (auth as any).clearSession === 'function') (auth as any).clearSession()
  else if (typeof (auth as any).clear === 'function') (auth as any).clear()

  router.push({ path: '/login', query: { redirect: '/' } })
}

async function fetchLicitaciones() {
  error.value = null
  loading.value = true

  try {
    // 🔁 AJUSTA AQUÍ TU ENDPOINT REAL si cambia:
    // const data = await apiFetch<any>('/api/licitaciones/mis', { method: 'GET' })
    const data = await apiFetch<any>('/api/licitaciones', { method: 'GET' })

    const items = Array.isArray(data) ? data : (data?.items ?? [])
    licitaciones.value = items
  } catch (e: any) {
    const msg = String(e?.message ?? '')

    // apiFetch lanza error tipo: "HTTP 403 Forbidden (...) - ..."
    if (msg.includes('HTTP 401') || msg.includes('HTTP 403')) {
      clearAuthAndGoLogin()
      return
    }

    console.error('[LICITACIONES] error', e)
    error.value = e?.message || 'Error cargando licitaciones'
  } finally {
    loading.value = false
  }
}

async function reload() {
  await fetchLicitaciones()
}

async function logout() {
  clearAuthAndGoLogin()
}

onMounted(() => {
  fetchLicitaciones()
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f7f9fc;
  padding: 18px;
  font-family: Inter, system-ui, Arial, sans-serif;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.header h1 {
  margin: 0;
  letter-spacing: 1px;
}

.right {
  display: flex;
  gap: 10px;
}

.btn {
  border: 0;
  background: #0057ff;
  color: #fff;
  padding: 10px 12px;
  border-radius: 10px;
  cursor: pointer;
}

.btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.btn.danger {
  background: #d32f2f;
}

.card {
  background: #fff;
  border-radius: 14px;
  padding: 16px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.07);
}

.list {
  display: grid;
  gap: 12px;
}

.item {
  border: 1px solid #eef1f6;
  border-radius: 12px;
  padding: 14px;
}

.top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.title {
  font-weight: 700;
  color: #1b2a4a;
}

.pill {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  background: #eef4ff;
  color: #0057ff;
  white-space: nowrap;
}

.meta {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 13px;
  color: #445;
}

.desc {
  margin-top: 10px;
  color: #444;
  font-size: 13px;
  line-height: 1.4;
}

.muted {
  color: #667;
}

.error {
  color: #d32f2f;
  font-weight: 600;
}
</style>
