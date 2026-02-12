<template>
  <div class="page">
    <h1 class="title">MI PERFIL</h1>

    <div class="tabs">
      <button class="tab" :class="{ active: tab==='perfil' }" @click="tab='perfil'">PERFIL</button>
      <button class="tab" :class="{ active: tab==='password' }" @click="tab='password'">CONTRASEÑA</button>
      <button class="tab" :class="{ active: tab==='plan' }" @click="tab='plan'">PLAN</button>
    </div>

    <div class="card" v-if="loading">Cargando…</div>
    <div class="card error" v-else-if="error">{{ error }}</div>

    <!-- PERFIL -->
    <div class="card" v-else-if="tab==='perfil'">
      <h3 class="section">USUARIO DE LA PLATAFORMA</h3>

      <div class="grid">
        <div>
          <label>Usuario</label>
          <input v-model="perfil.username" disabled />
          <small class="hint">Este dato se bloquea / define el usuario</small>
        </div>

        <div>
          <label>Email</label>
          <input v-model="perfil.email" disabled />
        </div>

        <div>
          <label>Teléfono</label>
          <input v-model="perfil.telefono" placeholder="Número de teléfono" />
        </div>
      </div>

      <h3 class="section">EMPRESA (A EFECTOS DE FACTURACIÓN)</h3>

      <div class="grid">
        <div>
          <label>Nombre de la empresa</label>
          <input v-model="perfil.empresaNombre" disabled />
        </div>

        <div>
          <label>CIF</label>
          <input v-model="perfil.empresaCif" disabled />
          <small class="hint">Este dato se bloquea / define la empresa</small>
        </div>
      </div>

      <button class="btn" @click="guardarPerfil">Guardar cambios</button>
    </div>

    <!-- CONTRASEÑA -->
    <div class="card" v-else-if="tab==='password'">
      <h3 class="section">CAMBIAR CONTRASEÑA</h3>

      <div class="grid one">
        <div>
          <label>Contraseña actual</label>
          <input type="password" v-model="pwd.actual" />
        </div>

        <div>
          <label>Nueva contraseña</label>
          <input type="password" v-model="pwd.nueva" />
        </div>

        <div>
          <label>Repite la nueva contraseña</label>
          <input type="password" v-model="pwd.repite" />
        </div>
      </div>

      <button class="btn" @click="cambiarPassword">Guardar cambios</button>
    </div>

    <!-- PLAN -->
    <div class="card" v-else>
      <h3 class="section">PLAN</h3>

      <div class="grid one">
        <div>
          <label>Actualmente disfrutas del plan</label>
          <input :value="plan.suscripcion || '-'" disabled />
        </div>

        <div>
          <label>Importe (anual)</label>
          <input :value="plan.importeAnual != null ? (plan.importeAnual + '€') : '-'" disabled />
        </div>

        <div>
          <label>Nueva factura</label>
          <input :value="plan.proximaFactura || '-'" disabled />
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { apiFetch } from '@/services/api'

type PerfilDTO = {
  username: string
  email: string
  telefono: string | null
  empresaNombre: string | null
  empresaCif: string | null
}

type PlanDTO = {
  suscripcion: string | null
  importeAnual: number | null
  proximaFactura: string | null
}

const tab = ref<'perfil' | 'password' | 'plan'>('perfil')
const loading = ref(true)
const error = ref('')

const perfil = reactive<PerfilDTO>({
  username: '',
  email: '',
  telefono: '',
  empresaNombre: '',
  empresaCif: ''
})

const plan = reactive<PlanDTO>({
  suscripcion: null,
  importeAnual: null,
  proximaFactura: null
})

const pwd = reactive({ actual: '', nueva: '', repite: '' })

async function cargar() {
  loading.value = true
  error.value = ''
  try {
    const p = await apiFetch<PerfilDTO>('/api/me')
    Object.assign(perfil, p)

    const pl = await apiFetch<PlanDTO>('/api/me/plan')
    Object.assign(plan, pl)
  } catch (e: any) {
    error.value = e?.message ?? 'Error cargando perfil'
  } finally {
    loading.value = false
  }
}

async function guardarPerfil() {
  try {
    await apiFetch<void>('/api/me', {
      method: 'PUT',
      body: JSON.stringify({ telefono: perfil.telefono ?? '' })
    })
    alert('Perfil actualizado')
  } catch (e: any) {
    alert(e?.message ?? 'Error al guardar')
  }
}

async function cambiarPassword() {
  if (!pwd.actual || !pwd.nueva) return alert('Rellena todos los campos')
  if (pwd.nueva !== pwd.repite) return alert('Las contraseñas no coinciden')

  try {
    await apiFetch<void>('/api/me/password', {
      method: 'PUT',
      body: JSON.stringify({ actual: pwd.actual, nueva: pwd.nueva })
    })
    pwd.actual = ''; pwd.nueva = ''; pwd.repite = ''
    alert('Contraseña actualizada')
  } catch (e: any) {
    alert(e?.message ?? 'Error cambiando contraseña')
  }
}

onMounted(cargar)
</script>

<style scoped>
.page { padding: 18px 22px; }
.title { margin: 0 0 14px; font-size: 14px; letter-spacing: .6px; color: #6b7280; }

.tabs { display:flex; gap:18px; border-bottom: 1px solid #eef2f7; margin-bottom: 16px; }
.tab { background:none; border:none; padding: 10px 6px; font-weight:700; font-size:12px; cursor:pointer; color:#6b7280; }
.tab.active { color:#0b5ed7; border-bottom:2px solid #0b5ed7; }

.card { background:#fff; border-radius:12px; padding:18px; box-shadow: 0 10px 24px rgba(16,24,40,.06); }
.section { margin: 0 0 12px; font-size:11px; color:#6b7280; letter-spacing:.5px; }

.grid { display:grid; grid-template-columns: 1fr 1fr; gap:14px 18px; margin-bottom: 16px; }
.grid.one { grid-template-columns: 1fr; max-width: 420px; }

label { display:block; font-size:11px; color:#6b7280; margin-bottom:6px; }
input {
  width:100%;
  height:30px;
  border:1px solid #dbe3ef;
  border-radius: 6px;
  padding: 0 10px;
  font-size:12px;
}
input:disabled { background:#f8fafc; color:#64748b; }
.hint { display:block; margin-top:4px; font-size:10px; color:#9aa4b2; }

.btn {
  background:#0b5ed7;
  color:#fff;
  border:none;
  border-radius:6px;
  height:28px;
  padding: 0 12px;
  font-size:12px;
  cursor:pointer;
}
.error { color:#b42318; }
</style>
