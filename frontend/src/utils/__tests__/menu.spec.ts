/// <reference types="node" />
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { existsSync, readFileSync } from 'node:fs'
import { join, resolve } from 'node:path'
import axios from 'axios'
import { getImagePath } from '../menu'

describe('getImagePath', () => {
  const TEST_BASE = 'http://localhost:8080'
  let savedBaseURL: string | undefined

  beforeEach(() => {
    savedBaseURL = axios.defaults.baseURL
    axios.defaults.baseURL = TEST_BASE
  })

  afterEach(() => {
    axios.defaults.baseURL = savedBaseURL
  })

  it('serves bundled menu photos from the frontend', () => {
    expect(getImagePath({ imageName: 'chicken-alfredo.webp' })).toBe('/items/chicken-alfredo.webp')
    expect(getImagePath({ imageName: 'white-chocolate.webp' })).toBe('/items/white-choco.webp')
  })

  it('serves uploaded photos from the backend after a refresh', () => {
    const filename = '0123456789abcdef0123456789abcdef.png'
    expect(getImagePath({ imageName: filename })).toBe(`${TEST_BASE}/uploads/${filename}`)
  })

  it('supports absolute upload paths and remote image URLs', () => {
    expect(getImagePath({ imageName: '/uploads/photo.png' })).toBe(`${TEST_BASE}/uploads/photo.png`)
    expect(getImagePath({ imageName: 'https://example.com/photo.png' })).toBe(
      'https://example.com/photo.png',
    )
  })

  it('resolves every seeded image to an existing bundled photo or honest fallback', () => {
    const seedPath = resolve(process.cwd(), '../backend/src/main/resources/seed/menu-items.json')
    const publicPath = resolve(process.cwd(), 'public')
    const items = JSON.parse(readFileSync(seedPath, 'utf8')) as { name: string }[]

    for (const item of items) {
      const imageName = `${item.name
        .replace(/^(Hot |Iced )/i, '')
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/-$/, '')}.webp`
      const imagePath = getImagePath({ imageName })
      expect(imagePath, item.name).toMatch(/^\/items\//)
      expect(existsSync(join(publicPath, imagePath.slice(1))), item.name).toBe(true)
    }
  })
})
