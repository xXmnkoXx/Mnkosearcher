import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

import { useAuthStore } from '@/stores/auth'

const app = createApp(App)

const pinia = createPinia()
app.use(pinia)

// ✅ Cargar sesión guardada (si existe) ANTES de montar
const auth = useAuthStore(pinia)

// Si tu store tiene este método, lo usamos.
// Si no existe, no pasa nada: el token ya vive en localStorage y apiFetch lo lee igual.
if (typeof (auth as any).loadFromStorage === 'function') {
  (auth as any).loadFromStorage()
}

app.use(router)
app.mount('#app')
