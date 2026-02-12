<template>
  <div class="card">

    <h3>Usuario de la plataforma</h3>

    <div class="grid">
      <input v-model="form.nombre" placeholder="Nombre" />
      <input v-model="form.apellidos" placeholder="Apellidos" />
      <input v-model="form.email" disabled />
      <input v-model="form.telefono" placeholder="Teléfono" />
    </div>

    <h3>Empresa (facturación)</h3>

    <div class="grid">
      <input v-model="form.empresa" placeholder="Empresa" />
      <input v-model="form.cif" placeholder="CIF" />
    </div>

    <button @click="guardar">Guardar cambios</button>

  </div>
</template>

<script setup>
import { reactive, onMounted } from 'vue'
import api from '@/services/api'

const form = reactive({
  nombre:'',
  apellidos:'',
  email:'',
  telefono:'',
  empresa:'',
  cif:''
})

onMounted(async () => {
  const res = await api.get('/user/profile')
  Object.assign(form, res.data)
})

const guardar = async () => {
  await api.put('/user/profile', form)
  alert('Perfil actualizado')
}
</script>
