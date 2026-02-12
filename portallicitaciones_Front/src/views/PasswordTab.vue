<template>
  <div class="card">

    <h3>Cambiar contraseña</h3>

    <input
      type="password"
      v-model="actual"
      placeholder="Contraseña actual"
    />

    <input
      type="password"
      v-model="nueva"
      placeholder="Nueva contraseña"
    />

    <input
      type="password"
      v-model="repetir"
      placeholder="Repetir contraseña"
    />

    <button @click="cambiar">Guardar cambios</button>

  </div>
</template>

<script setup>
import { ref } from 'vue'
import api from '@/services/api'

const actual = ref('')
const nueva = ref('')
const repetir = ref('')

const cambiar = async () => {

  if (nueva.value !== repetir.value) {
    alert('No coinciden')
    return
  }

  await api.put('/user/password', {
    actual: actual.value,
    nueva: nueva.value
  })

  alert('Contraseña actualizada')
}
</script>
