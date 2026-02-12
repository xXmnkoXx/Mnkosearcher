<template>
  <div class="page">
    <div class="header">
      <div>
        <div class="kicker">ADMIN EMPRESAS</div>
        <h1>{{ isEdit ? 'EDITAR EMPRESA' : 'CREAR EMPRESA' }}</h1>
      </div>

      <div class="header-actions">
        <button class="btn-secondary" @click="volver">Volver</button>
      </div>
    </div>

    <section class="card">
      <form class="form" @submit.prevent="onSubmit">

        <div class="grid">
          <div class="field">
            <label>ID Empresa</label>
            <input :value="isEdit ? id : 'Auto'" disabled />
          </div>

          <div class="field">
            <label>CIF</label>
            <input v-model="form.cif" placeholder="CIF" />
          </div>

          <div class="field">
            <label>Nombre *</label>
            <input v-model="form.nombre" placeholder="Nombre empresa" required />
          </div>

          <div class="field">
            <label>Email</label>
            <input v-model="form.email" placeholder="Email" />
          </div>

          <div class="field">
            <label>Teléfono</label>
            <input v-model="form.telefono" placeholder="Teléfono" />
          </div>

          <div class="field">
            <label>Web</label>
            <input v-model="form.web" placeholder="Web" />
          </div>

          <div class="field">
            <label>Tamaño</label>
            <input v-model="form.tamano" placeholder="Tamaño empresa" />
          </div>
        </div>

        <div class="section-title">DIRECCIÓN</div>

        <div class="grid">
          <div class="field">
            <label>Vía</label>
            <input v-model="form.via" placeholder="Calle / Avenida" />
          </div>

          <div class="field">
            <label>Número</label>
            <input v-model="form.numero" placeholder="Número" />
          </div>

          <div class="field">
            <label>Código Postal</label>
            <input v-model="form.codigoPostal" placeholder="Código postal" />
          </div>

          <div class="field">
            <label>Ciudad</label>
            <input v-model="form.ciudad" placeholder="Ciudad" />
          </div>

          <div class="field">
            <label>Provincia</label>
            <input v-model="form.provincia" placeholder="Provincia" />
          </div>
        </div>

        <div class="actions">
          <button class="btn-primary" type="submit" :disabled="saving">
            {{ saving ? 'Guardando...' : 'Guardar' }}
          </button>
        </div>

        <p v-if="error" class="error">{{ error }}</p>

      </form>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  crearEmpresa,
  actualizarEmpresa,
  obtenerEmpresa,
  type Empresa
} from '@/services/adminEmpresasApi'

const route = useRoute()
const router = useRouter()

const id = computed(() => Number(route.params.id))
const isEdit = computed(() => !!route.params.id)

const saving = ref(false)
const error = ref<string | null>(null)

const form = reactive<Empresa>({
  nombre: '',
  cif: '',
  email: '',
  telefono: '',
  web: '',
  tamano: '',
  via: '',
  numero: '',
  codigoPostal: '',
  ciudad: '',
  provincia: ''
})

function volver() {
  router.push('/admin/empresas')
}

async function cargarEmpresa() {
  if (!isEdit.value) return

  try {
    const data = await obtenerEmpresa(id.value)
    Object.assign(form, data)
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || 'Error cargando empresa'
  }
}

async function onSubmit() {
  error.value = null
  saving.value = true

  try {
    if (!form.nombre?.trim()) {
      error.value = 'El nombre es obligatorio'
      return
    }

    if (isEdit.value) {
      await actualizarEmpresa(id.value, form)
    } else {
      await crearEmpresa(form)
    }

    router.push('/admin/empresas')

  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || 'Error guardando empresa'
  } finally {
    saving.value = false
  }
}

onMounted(cargarEmpresa)
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

.card {
  background:white;
  border-radius:14px;
  padding:16px;
  box-shadow:0 10px 30px rgba(0,0,0,.06);
}

.form { display:flex; flex-direction:column; gap:14px; }

.grid {
  display:grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap:12px 14px;
}

.field label {
  font-size:12px;
  opacity:.7;
  display:block;
  margin-bottom:6px;
}

.field input {
  width:100%;
  padding:10px 12px;
  border-radius:10px;
  border:1px solid #e5e7eb;
  outline:none;
}

.section-title {
  font-size:12px;
  font-weight:800;
  opacity:.7;
  margin-top:6px;
}

.actions { margin-top:8px; }

.btn-primary {
  background:#0b5ed7;
  color:white;
  border:none;
  border-radius:10px;
  padding:10px 14px;
  cursor:pointer;
  font-weight:800;
}

.btn-secondary {
  background:white;
  color:#0b5ed7;
  border:1px solid #cfe0ff;
  border-radius:10px;
  padding:8px 12px;
  cursor:pointer;
  font-weight:700;
}

.error {
  color:#b91c1c;
  font-weight:700;
  margin-top:10px;
}
</style>
