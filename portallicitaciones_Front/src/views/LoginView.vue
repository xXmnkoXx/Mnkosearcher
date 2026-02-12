<template>
  <div class="login-page">
    <!-- IZQUIERDA (branding) -->
    <aside class="login-left" aria-label="Licis branding">
      <div class="brand">
        <!-- Imagen del logo -->
        <img class="brand-img" :src="tituloPortal" alt="Licis - Licitaciones relevantes para tu negocio" />
      </div>
    </aside>

    <!-- DERECHA (formulario) -->
    <main class="login-right">
      <section class="login-card" aria-label="Acceso al portal">
        <h2 class="card-title">
          <span>ACCESO AL PORTAL</span>
        </h2>

        <form class="form" @submit.prevent="onSubmit">
          <div class="field">
            <label>Email</label>
            <input
              v-model.trim="form.username"
              type="text"
              autocomplete="username"
              required
            />
          </div>

          <div class="field">
            <label>Contraseña</label>
            <input
              v-model="form.password"
              type="password"
              autocomplete="current-password"
              required
            />
          </div>

          <div class="actions">
            <a href="#" class="forgot" @click.prevent>¿Olvidaste la contraseña?</a>

            <button class="btn" type="submit" :disabled="loading">
              {{ loading ? 'Accediendo...' : 'Acceder' }}
            </button>
          </div>

          <p v-if="error" class="error">{{ error }}</p>
        </form>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { login } from '@/services/authApi'
import { useAuthStore } from '@/stores/auth'

// ✅ Pon la imagen aquí: src/assets/TituloPortal.png
import tituloPortal from '@/assets/TituloPortal.png'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const loading = ref(false)
const error = ref<string | null>(null)

const form = reactive({
  username: '',
  password: ''
})

async function onSubmit() {
  error.value = null
  loading.value = true

  try {
    const res = await login({
      username: form.username,
      password: form.password
    })

    auth.setSession({
      token: res.token,
      username: res.username,
      rol: res.rol
    })

    const redirect = (route.query.redirect as string) || '/mis-licitaciones'
    await router.push(redirect)
  } catch (e: any) {
    console.error('[LOGIN] error', e)
    error.value = e?.message || 'Credenciales incorrectas'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ===== LAYOUT 1:1 ===== */
.login-page{
  display:flex;
  height:100vh;
  width:100%;
  font-family: Inter, system-ui, Arial, sans-serif;
}

/* ===== IZQUIERDA ===== */
.login-left{
  flex: 1 1 50%;
  /* degradado similar a la captura */
  background: radial-gradient(1200px 800px at 20% 20%, rgba(242,107,79,.85), rgba(193,60,255,.35) 45%, rgba(0,87,255,.9) 80%),
              linear-gradient(135deg, #f26b4f, #c13cff, #0057ff);
  display:flex;
  align-items:center;
  justify-content:center;
  padding: 24px;
}

.brand{
  display:flex;
  align-items:center;
  justify-content:center;
  width:100%;
}

.brand-img{
  width: min(520px, 78%);
  height: auto;
  display:block;
  filter: drop-shadow(0 18px 34px rgba(0,0,0,.28));
}

/* ===== DERECHA ===== */
.login-right{
  flex: 1 1 50%;
  background:#f7f9fc;
  display:flex;
  align-items:center;
  justify-content:center;
  padding: 24px;
}

.login-card{
  background:#fff;
  width: 420px;
  max-width: 92vw;
  border-radius: 14px;
  padding: 28px 34px 26px;
  box-shadow: 0 16px 42px rgba(16,24,40,.12);
}

/* Título centrado con subrayado azul (como la imagen) */
.card-title{
  margin: 0 0 18px;
  text-align:center;
  font-size: 14px;
  font-weight: 900;
  letter-spacing: .8px;
  color:#0b5ed7;
  position: relative;
}

.card-title span{
  display:inline-block;
  padding-bottom: 10px;
}

.card-title span::after{
  content:"";
  display:block;
  height: 3px;
  width: 190px;
  max-width: 70%;
  margin: 10px auto 0;
  background:#0b5ed7;
  border-radius: 999px;
}

.form{
  margin-top: 6px;
}

.field{
  margin-bottom: 16px;
}

.field label{
  display:block;
  font-size: 13px;
  font-weight: 700;
  color:#4b5563;
  margin-bottom: 8px;
}

.field input{
  width:100%;
  height: 34px;              /* altura como la captura */
  padding: 0 12px;
  border: 1px solid #9aa3b2; /* borde gris */
  border-radius: 7px;
  outline: none;
  background:#fff;
  font-size: 13px;
  color:#111827;
  box-sizing:border-box;
}

.field input:focus{
  border-color:#0b5ed7;
  box-shadow: 0 0 0 3px rgba(11,94,215,.12);
}

/* fila inferior: link a la izquierda + botón a la derecha */
.actions{
  display:flex;
  align-items:center;
  justify-content: space-between;
  gap: 14px;
  margin-top: 14px;
}

.forgot{
  font-size: 13px;
  color:#6b7280;
  text-decoration: underline;
}

.forgot:hover{
  color:#0b5ed7;
}

/* botón pequeño a la derecha */
.btn{
  height: 28px;
  padding: 0 14px;
  border-radius: 8px;
  border: 0;
  background:#0b5ed7;
  color:#fff;
  font-weight: 800;
  font-size: 13px;
  cursor:pointer;
  box-shadow: 0 10px 18px rgba(11,94,215,.18);
}

.btn:disabled{
  opacity:.65;
  cursor:not-allowed;
}

.error{
  margin-top: 14px;
  color:#d32f2f;
  font-size: 13px;
  text-align:center;
  font-weight: 700;
}

/* ===== Responsive: en móvil apila y centra ===== */
@media (max-width: 980px){
  .login-page{
    flex-direction: column;
  }
  .login-left{
    flex: 0 0 auto;
    height: 42vh;
    min-height: 280px;
  }
  .login-right{
    flex: 1 1 auto;
    width: 100%;
  }
  .brand-img{
    width: min(420px, 86%);
  }
}
</style>
