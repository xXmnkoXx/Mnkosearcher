// src/router/index.ts
import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '@/layouts/AppLayout.vue'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // =====================
    // LOGIN (PÚBLICO)
    // =====================
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { requiresAuth: false }
    },

    // =====================
    // APP (PROTEGIDA)
    // =====================
    {
      path: '/',
      component: AppLayout,
      meta: { requiresAuth: true },
      children: [
        { path: '', redirect: '/mis-licitaciones' },

        {
          path: 'mis-licitaciones',
          name: 'mis-licitaciones',
          component: () => import('@/views/MisLicitacionesView.vue'),
          meta: { requiresAuth: true }
        },

        {
          path: 'licitaciones/:id',
          name: 'licitacion-detalle',
          component: () => import('@/views/DetalleLicitacionView.vue'),
          meta: { requiresAuth: true }
        },

        {
          path: 'mis-alertas',
          name: 'mis-alertas',
          component: () => import('@/views/MisAlertas.vue'),
          meta: { requiresAuth: true }
        },

        {
          path: 'buscador',
          name: 'buscador',
          component: () => import('@/views/PlaceholderView.vue'),
          meta: { requiresAuth: true }
        },

        // =====================
        // ✅ ADMIN EMPRESAS
        // =====================
        {
          path: 'admin/empresas',
          name: 'admin-empresas',
          component: () => import('@/views/AdminEmpresasView.vue'),
          meta: { requiresAuth: true, requiresAdmin: true }
        },
        {
          path: 'admin/empresas/nueva',
          name: 'admin-empresa-nueva',
          component: () => import('@/views/AdminEmpresaFormView.vue'),
          meta: { requiresAuth: true, requiresAdmin: true }
        },
        {
          path: 'admin/empresas/:id',
          name: 'admin-empresa-editar',
          component: () => import('@/views/AdminEmpresaFormView.vue'),
          meta: { requiresAuth: true, requiresAdmin: true }
        },

        // =====================
        // ✅ ADMIN USUARIOS
        // =====================
        {
          path: 'admin/usuarios',
          name: 'admin-usuarios',
          component: () => import('@/views/AdminUsuariosView.vue'),
          meta: { requiresAuth: true, requiresAdmin: true }
        },
        {
          path: 'admin/usuarios/nuevo',
          name: 'admin-usuario-nuevo',
          component: () => import('@/views/AdminUsuarioFormView.vue'),
          meta: { requiresAuth: true, requiresAdmin: true }
        },
        {
          path: 'admin/usuarios/:id',
          name: 'admin-usuario-editar',
          component: () => import('@/views/AdminUsuarioFormView.vue'),
          meta: { requiresAuth: true, requiresAdmin: true }
        },
        {
          path: 'mi-perfil',
          name: 'mi-perfil',
          component: () => import('@/views/MiPerfilView.vue'),
          meta: { requiresAuth: true }
        },

        // OJO: rutas absolutas (las dejo como las tienes)
        {
          path: '/alertas/nueva',
          name: 'alertas-nueva',
          component: () => import('@/views/ConfigurarAlertaView.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: '/alertas/:id/configurar',
          name: 'alertas-configurar',
          component: () => import('@/views/ConfigurarAlertaView.vue'),
          meta: { requiresAuth: true }
        }
      ]
    },

    // =====================
    // FALLBACK
    // =====================
    { path: '/:pathMatch(.*)*', redirect: '/mis-licitaciones' }
  ]
})

// =====================
// GUARD GLOBAL
// =====================
router.beforeEach((to) => {
  const auth = useAuthStore()

  const requiresAuth = to.matched.some(r => r.meta?.requiresAuth)
  const requiresAdmin = to.matched.some(r => r.meta?.requiresAdmin)
  const isLogin = to.name === 'login'
  const isAuthenticated = auth.isAuthenticated

  // rol tolerante (auth.rol o auth.role)
  const role = (auth as any).rol ?? (auth as any).role ?? null
  const isAdmin = String(role || '').toUpperCase() === 'ADMIN'

  // No logueado → login
  if (requiresAuth && !isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  // Ya logueado → no volver al login
  if (isLogin && isAuthenticated) {
    const redirect = (to.query.redirect as string) || '/mis-licitaciones'
    return redirect
  }

  // No admin → fuera de /admin
  if (requiresAdmin && !isAdmin) {
    return '/mis-licitaciones'
  }

  return true
})

export default router
