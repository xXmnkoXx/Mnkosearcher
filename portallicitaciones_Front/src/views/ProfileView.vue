<template>
  <div class="profile-page">

    <div class="tabs">
      <button
        :class="{ active: tab === 'profile' }"
        @click="tab='profile'"
      >Perfil</button>

      <button
        :class="{ active: tab === 'password' }"
        @click="tab='password'"
      >Contraseña</button>

      <button
        :class="{ active: tab === 'plan' }"
        @click="tab='plan'"
      >Plan</button>
    </div>

    <ProfileTab v-if="tab==='profile'" />
    <PasswordTab v-if="tab==='password'" />
    <PlanTab v-if="tab==='plan'" />

  </div>
</template>

<script setup>
import { ref } from 'vue'

import ProfileTab from './ProfileTab.vue'
import PasswordTab from './PasswordTab.vue'
import PlanTab from './PlanTab.vue'

const tab = ref('profile')
</script>

<style scoped>
.tabs {
  display:flex;
  gap:20px;
  border-bottom:1px solid #eee;
  margin-bottom:20px;
}

.tabs button {
  background:none;
  border:none;
  padding:10px;
  cursor:pointer;
}

.tabs .active {
  border-bottom:2px solid #0066ff;
  font-weight:600;
}
</style>
