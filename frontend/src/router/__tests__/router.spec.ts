import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { getAppRole, createRoutesForRole } from '../index'

describe('Domain & Role Router Separation', () => {
  const originalLocation = window.location

  beforeEach(() => {
    vi.unstubAllEnvs()
  })

  afterEach(() => {
    Object.defineProperty(window, 'location', {
      value: originalLocation,
      writable: true,
    })
  })

  it('detects role from VITE_APP_ROLE environment variable when configured', () => {
    vi.stubEnv('VITE_APP_ROLE', 'survey')
    expect(getAppRole()).toBe('survey')

    vi.stubEnv('VITE_APP_ROLE', 'admin')
    expect(getAppRole()).toBe('admin')

    vi.stubEnv('VITE_APP_ROLE', 'all')
    expect(getAppRole()).toBe('all')
  })

  it('detects admin role from hostname when starts with admin', () => {
    Object.defineProperty(window, 'location', {
      value: { hostname: 'admin.myfoodsurvey.com' },
      writable: true,
    })
    expect(getAppRole()).toBe('admin')

    Object.defineProperty(window, 'location', {
      value: { hostname: 'admin.localhost' },
      writable: true,
    })
    expect(getAppRole()).toBe('admin')

    Object.defineProperty(window, 'location', {
      value: { hostname: 'admin-survey.vercel.app' },
      writable: true,
    })
    expect(getAppRole()).toBe('admin')
  })

  it('detects survey role from hostname when starts with survey', () => {
    Object.defineProperty(window, 'location', {
      value: { hostname: 'survey.myfoodsurvey.com' },
      writable: true,
    })
    expect(getAppRole()).toBe('survey')

    Object.defineProperty(window, 'location', {
      value: { hostname: 'survey.localhost' },
      writable: true,
    })
    expect(getAppRole()).toBe('survey')

    Object.defineProperty(window, 'location', {
      value: { hostname: 'survey-xi-ten.vercel.app' },
      writable: true,
    })
    expect(getAppRole()).toBe('survey')
  })

  it('falls back to all in development when hostname is plain localhost and no env set', () => {
    Object.defineProperty(window, 'location', {
      value: { hostname: 'localhost' },
      writable: true,
    })
    expect(getAppRole()).toBe('all')
  })

  it('constructs routes for survey role that block /admin with NotFound component', () => {
    const routes = createRoutesForRole('survey')
    const rootRoute = routes.find((r) => r.path === '/')
    expect(rootRoute?.name).toBe('survey')

    const adminRoute = routes.find((r) => r.path === '/admin')
    expect(adminRoute?.name).toBe('admin-404')

    const dashboardRoute = routes.find((r) => r.path === '/dashboard')
    expect(dashboardRoute?.name).toBe('dashboard-404')
  })

  it('constructs routes for admin role where root is Dashboard and redirects /admin to /', () => {
    const routes = createRoutesForRole('admin')
    const rootRoute = routes.find((r) => r.path === '/')
    expect(rootRoute?.name).toBe('admin')

    const adminRoute = routes.find((r) => r.path === '/admin')
    expect(adminRoute?.redirect).toBe('/')

    const dashboardRoute = routes.find((r) => r.path === '/dashboard')
    expect(dashboardRoute?.redirect).toBe('/')
  })

  it('constructs routes for all role where both survey and /admin are accessible', () => {
    const routes = createRoutesForRole('all')
    const rootRoute = routes.find((r) => r.path === '/')
    expect(rootRoute?.name).toBe('survey')

    const adminRoute = routes.find((r) => r.path === '/admin')
    expect(adminRoute?.name).toBe('admin')

    const dashboardRoute = routes.find((r) => r.path === '/dashboard')
    expect(dashboardRoute?.name).toBe('dashboard')
  })
})
