<template>
  <div class="card">

    <h3>Plan actual</h3>

    <div class="row">
      <label>Plan:</label>
      <span>{{ plan.nombre }}</span>
    </div>

    <div class="row">
      <label>Importe mensual:</label>
      <span>{{ plan.precio }} €</span>
    </div>

    <div class="row">
      <label>Próxima factura:</label>
      <span>{{ plan.proximaFactura }}</span>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/services/api'

const plan = ref({})

onMounted(async () => {
  const res = await api.get('/user/plan')
  plan.value = res.data
})
</script>
