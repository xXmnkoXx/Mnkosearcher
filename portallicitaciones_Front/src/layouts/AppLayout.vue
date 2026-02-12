<template>
  <div class="app-shell">
    <!-- SIDEBAR -->
    <aside class="sidebar" :class="{ 'admin-mode': isAdminRoute }">
      <!-- BRAND -->
      <div class="brand">
        <img class="brandImg" :src="logoUrl" alt="Licis" />
      </div>

      <!-- NAV -->
      <nav class="nav">
        <RouterLink class="navItem" to="/mis-licitaciones" active-class="active">
          <span class="icon" aria-hidden="true">
            <!-- bell -->
            <svg viewBox="0 0 24 24" fill="none">
              <path
                d="M18 8a6 6 0 10-12 0c0 7-3 7-3 7h18s-3 0-3-7Z"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
              <path
                d="M13.73 21a2 2 0 01-3.46 0"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </span>
          <span class="label">MIS LICITACIONES</span>
        </RouterLink>

        <RouterLink class="navItem" to="/mis-alertas" active-class="active">
          <span class="icon" aria-hidden="true">
            <!-- gear -->
            <svg viewBox="0 0 24 24" fill="none">
              <path
                d="M12 15.5a3.5 3.5 0 110-7 3.5 3.5 0 010 7Z"
                stroke="currentColor"
                stroke-width="2"
              />
              <path
                d="M19.4 15a8.1 8.1 0 00.1-1l2-1.2-2-3.4-2.3.6a8 8 0 00-1.7-1l-.3-2.4H10.8l-.3 2.4a8 8 0 00-1.7 1l-2.3-.6-2 3.4 2 1.2a8.1 8.1 0 000 2l-2 1.2 2 3.4 2.3-.6a8 8 0 001.7 1l.3 2.4h4.4l.3-2.4a8 8 0 001.7-1l2.3.6 2-3.4-2-1.2a8.1 8.1 0 00-.1-1Z"
                stroke="currentColor"
                stroke-width="2"
                stroke-linejoin="round"
              />
            </svg>
          </span>
          <span class="label">MIS ALERTAS</span>
        </RouterLink>

        <RouterLink class="navItem" to="/buscador" active-class="active">
          <span class="icon" aria-hidden="true">
            <!-- search -->
            <svg viewBox="0 0 24 24" fill="none">
              <path
                d="M11 19a8 8 0 100-16 8 8 0 000 16Z"
                stroke="currentColor"
                stroke-width="2"
              />
              <path
                d="M21 21l-4.3-4.3"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
              />
              <path
                d="M6.5 10.5h9"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
              />
            </svg>
          </span>
          <span class="label">BUSCADOR</span>
        </RouterLink>

        <!-- ADMIN -->
        <div v-if="isAdmin" class="adminBlock">
          <div class="adminTitle">ADMINISTRACIÓN</div>

          <RouterLink class="navItem small" to="/admin/usuarios" active-class="active">
            <span class="icon" aria-hidden="true">
              <!-- user -->
              <svg viewBox="0 0 24 24" fill="none">
                <path
                  d="M12 12a4 4 0 100-8 4 4 0 000 8Z"
                  stroke="currentColor"
                  stroke-width="2"
                />
                <path
                  d="M20 21a8 8 0 10-16 0"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                />
              </svg>
            </span>
            <span class="label">GESTIONAR USUARIOS</span>
          </RouterLink>

          <RouterLink class="navItem small" to="/admin/empresas" active-class="active">
            <span class="icon" aria-hidden="true">
              <!-- briefcase simple -->
              <svg viewBox="0 0 24 24" fill="none">
                <path
                  d="M9 7V6a2 2 0 012-2h2a2 2 0 012 2v1"
                  stroke="currentColor"
                  stroke-width="2"
                />
                <path
                  d="M4 7h16v13H4V7Z"
                  stroke="currentColor"
                  stroke-width="2"
                />
                <path
                  d="M4 12h16"
                  stroke="currentColor"
                  stroke-width="2"
                />
              </svg>
            </span>
            <span class="label">GESTIONAR EMPRESAS</span>
          </RouterLink>
        </div>
      </nav>

      <div class="spacer" />

      <!-- CARD BLANCA GRANDE (como el diseño) -->
      <div class="whiteCard" />

      <div class="spacerSmall" />

      <!-- FOOTER -->
      <nav class="nav bottom">
        <RouterLink class="navItem" to="/mi-perfil" active-class="active">
          <span class="icon" aria-hidden="true">
            <!-- user -->
            <svg viewBox="0 0 24 24" fill="none">
              <path
                d="M12 12a4 4 0 100-8 4 4 0 000 8Z"
                stroke="currentColor"
                stroke-width="2"
              />
              <path
                d="M20 21a8 8 0 10-16 0"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
              />
            </svg>
          </span>
          <span class="label">MI PERFIL</span>
        </RouterLink>

        <a class="navItem" href="#" @click.prevent>
          <span class="icon" aria-hidden="true">
            <!-- support -->
            <svg viewBox="0 0 24 24" fill="none">
              <path
                d="M12 22a10 10 0 110-20 10 10 0 010 20Z"
                stroke="currentColor"
                stroke-width="2"
              />
              <path
                d="M8 14.5c1.2 1.2 2.6 1.8 4 1.8s2.8-.6 4-1.8"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
              />
              <path
                d="M8.2 10.2h.01M15.8 10.2h.01"
                stroke="currentColor"
                stroke-width="3"
                stroke-linecap="round"
              />
            </svg>
          </span>
          <span class="label">SOPORTE</span>
        </a>

        <a class="navItem" href="#" @click.prevent="logout">
          <span class="icon" aria-hidden="true">
            <!-- logout -->
            <svg viewBox="0 0 24 24" fill="none">
              <path
                d="M10 17l5-5-5-5"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
              <path
                d="M15 12H3"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
              />
              <path
                d="M21 3v18"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
              />
            </svg>
          </span>
          <span class="label">SALIR</span>
        </a>
      </nav>
    </aside>

    <!-- CONTENIDO -->
    <main class="content">
      <!-- TOP BAR -->
      <header class="topbar">
        <div class="topbar-spacer"></div>

        <div class="user-info">
          {{ auth.username }}
        </div>
      </header>

      <!-- ROUTER VIEW -->
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { RouterLink, RouterView, useRouter, useRoute } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import logoUrl from "@/assets/TituloPortal.png";

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();

/* ===== ADMIN ROLE ===== */
const isAdmin = computed(() => {
  const role = (auth as any).rol ?? (auth as any).role ?? "";
  return String(role).toUpperCase() === "ADMIN";
});

/* ===== ADMIN ROUTE DETECTOR ===== */
const isAdminRoute = computed(() => {
  return route.path.startsWith("/admin");
});

/* ===== LOGOUT ===== */
function logout() {
  if (typeof (auth as any).logout === "function") {
    (auth as any).logout();
  } else {
    localStorage.removeItem("token");
    localStorage.removeItem("username");
    localStorage.removeItem("rol");
  }
  router.push("/login");
}
</script>
<style scoped>
/* =========================
   LAYOUT (FIJO)
   ========================= */
.app-shell {
  display: flex;
  height: 100vh;
  width: 100%;
  overflow: hidden; /* evita que el layout baile por scroll */
}

/* =========================
   SIDEBAR (DISEÑO + ANCHO FIJO)
   ========================= */
.sidebar {
  --blue: #0b63ce;
  --accent: #36d1c4;

  /* ✅ ANCHO FIJO: NO se encoge nunca */
  width: 240px;
  min-width: 240px;
  max-width: 240px;
  flex: 0 0 240px; /* clave */
  flex-shrink: 0;  /* clave */

  height: calc(100vh - 36px);
  margin: 18px 0 18px 18px;

  background: var(--blue);
  border-radius: 28px;

  padding: 26px 20px 22px;
  color: #fff;

  display: flex;
  flex-direction: column;
  transition: background 0.25s ease;
}

/* BRAND (logo PNG) */
.brand {
  display: flex;
  align-items: center;
  padding: 4px 4px 18px 4px;
}

.brandImg {
  width: 210px; /* ajusta si el PNG tiene margen transparente */
  max-width: 100%;
  height: auto;
  object-fit: contain;
  display: block;
}

/* NAV */
.nav {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.navItem {
  display: flex;
  align-items: center;
  gap: 12px;

  text-decoration: none;
  color: rgba(255, 255, 255, 0.95);

  font-weight: 800;
  font-size: 13px;
  letter-spacing: 0.08em;
  text-transform: uppercase;

  padding: 4px 6px;
  user-select: none;
}

.navItem.small {
  font-size: 12px;
  letter-spacing: 0.07em;
}

.navItem:hover {
  opacity: 0.92;
}

.navItem.active {
  color: var(--accent);
}

.icon {
  width: 20px;
  height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 20px;
  color: currentColor;
}

.icon svg {
  width: 20px;
  height: 20px;
  display: block;
}

/* ADMIN */
.adminBlock {
  margin-top: 6px;
  padding-top: 10px;
  border-top: 1px solid rgba(255, 255, 255, 0.18);
}

.adminTitle {
  font-size: 11px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  opacity: 0.85;
  font-weight: 800;
  padding: 0 6px 4px;
}

/* BLOQUE BLANCO GRANDE */
.spacer {
  flex: 1;
}

.whiteCard {
  height: 230px;
  border-radius: 22px;
  background: #ffffff;
  margin: 0 6px;
}

.spacerSmall {
  height: 18px;
}

/* NAV inferior */
.bottom {
  gap: 14px;
}

/* =========================
   ADMIN MODE (OSCURO)
   ========================= */
.sidebar.admin-mode {
  background: #1f2937;
}

.sidebar.admin-mode .navItem {
  color: rgba(255, 255, 255, 0.92);
}

.sidebar.admin-mode .navItem.active {
  color: #a7f3d0;
}

.sidebar.admin-mode .adminBlock {
  border-top: 1px solid rgba(255, 255, 255, 0.12);
}

/* =========================
   CONTENT (NO EMPUJA SIDEBAR)
   ========================= */
.content {
  flex: 1;
  min-width: 0;   /* ✅ clave: evita que tablas/contenido empujen al sidebar */
  overflow: auto; /* ✅ el scroll ocurre aquí */

  background: #f5f7fb;
  display: flex;
  flex-direction: column;
}

/* TOP BAR */
.topbar {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 24px;
  flex: 0 0 auto;
}

.topbar-spacer {
  flex: 1;
}

.user-info {
  background: white;
  padding: 6px 14px;
  border-radius: 10px;
  font-weight: 600;
  color: #1f2937;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  font-size: 14px;
}
</style>
