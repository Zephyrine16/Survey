import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

// Dynamic lazy imports for efficient code-splitting and isolation
const Survey = () => import('../components/Survey.vue')
const Dashboard = () => import('../views/Dashboard.vue')
const NotFound = () => import('../views/NotFound.vue')

/**
 * Determine the active application role:
 * 1. Environment variable: VITE_APP_ROLE ('survey', 'admin', 'all')
 * 2. Hostname detection:
 *    - 'admin.*' (e.g. admin.localhost, admin.survey.com, admin-survey.vercel.app) -> 'admin'
 *    - 'survey.*' (e.g. survey.localhost, survey.survey.com, survey-xi-ten.vercel.app) -> 'survey'
 * 3. Default fallback: 'all' (development mode / single host allows both survey and /admin)
 */
export function getAppRole(): 'survey' | 'admin' | 'all' {
  const envRole = (import.meta.env.VITE_APP_ROLE || '').trim().toLowerCase()
  if (envRole === 'survey' || envRole === 'admin' || envRole === 'all') {
    return envRole as 'survey' | 'admin' | 'all'
  }

  if (typeof window !== 'undefined') {
    const hostname = window.location.hostname.toLowerCase()
    if (hostname.startsWith('admin.') || hostname.startsWith('admin-') || hostname.includes('admin')) {
      return 'admin'
    }
    if (hostname.startsWith('survey.') || hostname.startsWith('survey-')) {
      return 'survey'
    }
  }

  return 'all'
}

export function createRoutesForRole(role: 'survey' | 'admin' | 'all'): RouteRecordRaw[] {
  if (role === 'admin') {
    return [
      {
        path: '/',
        name: 'admin',
        component: Dashboard,
      },
      {
        path: '/admin',
        redirect: '/',
      },
      {
        path: '/dashboard',
        redirect: '/',
      },
      {
        path: '/:pathMatch(.*)*',
        redirect: '/',
      },
    ]
  }

  if (role === 'survey') {
    return [
      {
        path: '/',
        name: 'survey',
        component: Survey,
      },
      // Admin routes are explicitly blocked / hidden with a 404 on the public survey domain
      {
        path: '/admin',
        name: 'admin-404',
        component: NotFound,
      },
      {
        path: '/dashboard',
        name: 'dashboard-404',
        component: NotFound,
      },
      {
        path: '/:pathMatch(.*)*',
        name: 'not-found',
        component: NotFound,
      },
    ]
  }

  // Role === 'all' (Development / single-domain mode)
  return [
    {
      path: '/',
      name: 'survey',
      component: Survey,
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: Dashboard,
    },
    {
      path: '/admin',
      name: 'admin',
      component: Dashboard,
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: NotFound,
    },
  ]
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: createRoutesForRole(getAppRole()),
})

export default router
