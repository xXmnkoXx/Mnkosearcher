<template>
  <div class="page">
    <div class="header">
      <div>
        <div class="kicker">ADMIN USUARIOS</div>
        <h1>{{ isEdit ? 'EDITAR USUARIO' : 'CREAR USUARIO' }}</h1>
      </div>

      <div class="header-actions">
        <button class="btn-secondary" @click="volver">Volver</button>
      </div>
    </div>

    <section class="card">
      <form class="form" @submit.prevent="onSubmit">
        <div class="grid">
          <div class="field">
            <label>ID Usuario</label>
            <input :value="isEdit ? id : 'Auto'" disabled />
          </div>

          <div class="field">
            <label>Rol</label>
            <select v-model="form.rol">
              <option value="USER">USER</option>
              <option value="ADMIN">ADMIN</option>
            </select>
          </div>

          <div class="field">
            <label>Username *</label>
            <input v-model="form.username" placeholder="username" required />
          </div>

          <div class="field">
            <label>Email *</label>
            <input v-model="form.email" placeholder="email" required />
          </div>

          <div class="field">
            <label>Activo</label>
            <select v-model="form.activo">
              <option :value="true">Sí</option>
              <option :value="false">No</option>
            </select>
          </div>

          <div class="field">
            <label>Empresa</label>
            <select v-model="form.idEmpresa">
              <option :value="null">-- Sin empresa --</option>
              <option v-for="e in empresas" :key="e.idEmpresa" :value="e.idEmpresa">
                {{ e.nombre }} (ID {{ e.idEmpresa }})
              </option>
            </select>
          </div>

          <div class="field">
            <label>Teléfono</label>
            <input v-model="form.telefono" placeholder="teléfono" />
          </div>

          <div class="field">
            <label>Suscripción</label>
            <input v-model="form.suscripcion" placeholder="ej: BUSQUEDA / BUSQUEDA+PRESENTACION" />
          </div>

          <div class="field">
            <label>Contraseña {{ isEdit ? '(solo si quieres cambiarla)' : '*' }}</label>
            <input
              v-model="password"
              type="password"
              placeholder="password"
              :required="!isEdit"
            />
          </div>
        </div>

        <div class="section-title">CONFIGURACIÓN CLIENTE (TU TABLA USUARIOS)</div>

        <div class="grid">
          <div class="field">
            <label>ID Cliente</label>
            <input v-model.number="form.idCliente" type="number" placeholder="id_cliente" />
          </div>

          <div class="field">
            <label>Nombre Cliente</label>
            <input v-model="form.nombreCliente" placeholder="nombre_cliente" />
          </div>

          <div class="field">
            <label>Email Destino</label>
            <input v-model="form.emailDestino" placeholder="email_destino" />
          </div>

          <div class="field">
            <label>CPVs</label>
            <input v-model="form.cpvs" placeholder="ej: 71300000, 45200000" />
          </div>

          <div class="field">
            <label>Importe Max</label>
            <input v-model.number="form.importeMax" type="number" step="0.01" placeholder="importe_max" />
          </div>

          <div class="field">
            <label>Tipo Contrato</label>
            <input v-model="form.tipoContrato" placeholder="SERVICIOS / SUMINISTROS..." />
          </div>

          <div class="field">
            <label>Estado</label>
            <input v-model="form.estado" placeholder="ACTIVO / PAUSADO..." />
          </div>

          <div class="field">
            <label>Desde offset</label>
            <input v-model.number="form.desdeOffset" type="number" placeholder="desde_offset" />
          </div>

          <div class="field">
            <label>Hasta offset</label>
            <input v-model.number="form.hastaOffset" type="number" placeholder="hasta_offset" />
          </div>

          <div class="field">
            <label>Max páginas</label>
            <input v-model.number="form.maxPaginas" type="number" placeholder="max_paginas" />
          </div>

          <div class="field" style="grid-column: 1 / -1;">
            <label>Descripción</label>
            <input v-model="form.descripcion" placeholder="descripcion" />
          </div>

          <div class="field">
            <label>Precio</label>
            <input v-model.number="form.precio" type="number" step="0.01" placeholder="precio" />
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
import { crearUsuario, actualizarUsuario, obtenerUsuario, type UsuarioAdmin } from '@/services/adminUsuariosApi'
import { listarEmpresas, type Empresa } from '@/services/adminEmpresasApi'

const route = useRoute()
const router = useRouter()

const id = computed(() => Number(route.params.id))
const isEdit = computed(() => !!route.params.id)

const empresas = ref<Empresa[]>([])
const saving = ref(false)
const error = ref<string | null>(null)

const password = ref('')

const form = reactive<UsuarioAdmin>({
  username: '',
  email: '',
  rol: 'USER',
  activo: true,

  idEmpresa: null,
  telefono: '',
  suscripcion: '',

  // defaults cliente
  idCliente: 0,
  nombreCliente: '',
  emailDestino: '',
  cpvs: '',
  importeMax: null,
  tipoContrato: '',
  estado: '',
  desdeOffset: 0,
  hastaOffset: 0,
  maxPaginas: 25,
  descripcion: '',
  precio: null
})

function volver() {
  router.push('/admin/usuarios')
}

async function cargarEmpresas() {
  try {
    empresas.value = await listarEmpresas()
  } catch {
    // no bloquea el formulario
    empresas.value = []
  }
}

async function cargarUsuario() {
  if (!isEdit.value) return
  error.value = null
  try {
    const u = await obtenerUsuario(id.value)
    Object.assign(form, u)
    // password no se precarga nunca
    password.value = ''
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || 'Error cargando usuario'
  }
}

async function onSubmit() {
  error.value = null
  saving.value = true

  try {
    if (!form.username?.trim()) {
      error.value = 'Username obligatorio'
      return
    }
    if (!form.email?.trim()) {
      error.value = 'Email obligatorio'
      return
    }

    const payload: UsuarioAdmin = {
      ...form,
      // aseguramos boolean
      activo: !!form.activo,
      // si viene '' -> null
      idEmpresa: form.idEmpresa === null || form.idEmpresa === ('' as any) ? null : Number(form.idEmpresa)
    }

    // password: obligatorio en crear, opcional en editar
    if (!isEdit.value) {
      if (!password.value || password.value.length < 6) {
        error.value = 'La contraseña es obligatoria (mínimo 6)'
        return
      }
      payload.passwordHash = password.value
    } else {
      if (password.value && password.value.trim().length > 0) {
        if (password.value.length < 6) {
          error.value = 'La contraseña debe tener mínimo 6'
          return
        }
        payload.passwordHash = password.value
      } else {
        payload.passwordHash = null
      }
    }

    if (isEdit.value) {
      await actualizarUsuario(id.value, payload)
    } else {
      await crearUsuario(payload)
    }

    router.push('/admin/usuarios')
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || 'Error guardando usuario'
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await cargarEmpresas()
  await cargarUsuario()
})
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

.field input, .field select {
  width:100%;
  padding:10px 12px;
  border-radius:10px;
  border:1px solid #e5e7eb;
  outline:none;
  background:#fff;
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
