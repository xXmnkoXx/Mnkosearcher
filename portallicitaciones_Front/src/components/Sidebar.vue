<script setup lang="ts">
import { ref, computed } from "vue";
import logoUrl from "@/assets/TituloPortal.png";

type Item = { key: string; label: string; icon: string; href?: string };

const activeKey = ref("mis-licitaciones");

const topItems: Item[] = [
  { key: "mis-licitaciones", label: "MIS LICITACIONES", icon: "bell", href: "#" },
  { key: "mis-alertas", label: "MIS ALERTAS", icon: "gear", href: "#" },
  { key: "buscador", label: "BUSCADOR", icon: "search", href: "#" },
];

const bottomItems: Item[] = [
  { key: "perfil", label: "MI PERFIL", icon: "user", href: "#" },
  { key: "soporte", label: "SOPORTE", icon: "support", href: "#" },
  { key: "salir", label: "SALIR", icon: "logout", href: "#" },
];

const allItems = computed(() => ({ top: topItems, bottom: bottomItems }));

function onNavClick(item: Item) {
  // Marca activo solo en el menú superior (como en el diseño)
  if (topItems.some((x) => x.key === item.key)) activeKey.value = item.key;
}
</script>

<template>
  <aside class="sidebar">
    <!-- BRAND -->
    <div class="brand">
      <img class="brandImg" :src="logoUrl" alt="Licis" />
    </div>

    <!-- TOP NAV -->
    <nav class="nav">
      <a
        v-for="i in allItems.top"
        :key="i.key"
        class="navItem"
        :class="{ active: i.key === activeKey }"
        :href="i.href || '#'"
        @click.prevent="onNavClick(i)"
      >
        <span class="icon" aria-hidden="true">
          <!-- bell -->
          <svg v-if="i.icon === 'bell'" viewBox="0 0 24 24" fill="none">
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

          <!-- gear -->
          <svg v-else-if="i.icon === 'gear'" viewBox="0 0 24 24" fill="none">
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

          <!-- search -->
          <svg v-else-if="i.icon === 'search'" viewBox="0 0 24 24" fill="none">
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

          <!-- user -->
          <svg v-else-if="i.icon === 'user'" viewBox="0 0 24 24" fill="none">
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

          <!-- support -->
          <svg v-else-if="i.icon === 'support'" viewBox="0 0 24 24" fill="none">
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

          <!-- logout -->
          <svg v-else-if="i.icon === 'logout'" viewBox="0 0 24 24" fill="none">
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

        <span class="label">{{ i.label }}</span>
      </a>
    </nav>

    <!-- LARGE WHITE CARD (como en el diseño) -->
    <div class="midSpacer" />
    <div class="whiteCard" />
    <div class="midSpacerSmall" />

    <!-- BOTTOM NAV -->
    <nav class="nav bottom">
      <a
        v-for="i in allItems.bottom"
        :key="i.key"
        class="navItem bottomItem"
        :href="i.href || '#'"
        @click.prevent="onNavClick(i)"
      >
        <span class="icon" aria-hidden="true">
          <!-- reutiliza los mismos SVGs -->
          <svg v-if="i.icon === 'user'" viewBox="0 0 24 24" fill="none">
            <path d="M12 12a4 4 0 100-8 4 4 0 000 8Z" stroke="currentColor" stroke-width="2" />
            <path d="M20 21a8 8 0 10-16 0" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
          </svg>
          <svg v-else-if="i.icon === 'support'" viewBox="0 0 24 24" fill="none">
            <path d="M12 22a10 10 0 110-20 10 10 0 010 20Z" stroke="currentColor" stroke-width="2" />
            <path
              d="M8 14.5c1.2 1.2 2.6 1.8 4 1.8s2.8-.6 4-1.8"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
            />
            <path d="M8.2 10.2h.01M15.8 10.2h.01" stroke="currentColor" stroke-width="3" stroke-linecap="round" />
          </svg>
          <svg v-else-if="i.icon === 'logout'" viewBox="0 0 24 24" fill="none">
            <path d="M10 17l5-5-5-5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
            <path d="M15 12H3" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
            <path d="M21 3v18" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
          </svg>
        </span>
        <span class="label">{{ i.label }}</span>
      </a>
    </nav>
  </aside>
</template>

<style scoped>
/* Ajustes para que quede como la maqueta */
.sidebar {
  --blue: #0b63ce;
  --accent: #36d1c4; /* turquesa del item activo (aprox) */

  width: 270px;
  height: calc(100vh - 36px);
  margin: 18px 0 18px 18px;

  background: var(--blue);
  border-radius: 28px;

  padding: 26px 20px 22px;
  color: #fff;
  display: flex;
  flex-direction: column;
}

/* Logo arriba: usa tu PNG y lo escalamos */
.brand {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 4px 4px 18px 4px;
}

.brandImg {
  width: 210px;      /* ajusta si lo quieres más grande/pequeño */
  max-width: 100%;
  height: auto;
  object-fit: contain;
  display: block;
}

/* NAV */
.nav {
  display: flex;
  flex-direction: column;
  gap: 18px; /* separación entre items como en el diseño */
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

/* Bloque blanco grande */
.midSpacer {
  flex: 1;
}

.whiteCard {
  height: 230px;
  border-radius: 22px;
  background: #ffffff;
  margin: 0 6px;
}

.midSpacerSmall {
  height: 18px;
}

/* NAV inferior */
.bottom {
  gap: 14px;
}

.bottomItem {
  font-size: 13px;
  opacity: 0.95;
}
</style>
