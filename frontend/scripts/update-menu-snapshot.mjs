import { writeFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'

const apiBase = (process.env.VITE_API_BASE_URL ?? '').trim().replace(/\/$/, '')
const outputPath = fileURLToPath(new URL('../src/generated/menu-snapshot.json', import.meta.url))

// Local builds without a production API use the checked-in empty snapshot.
if (!apiBase || apiBase === 'https://api.survey.example.com') {
  console.log('Menu snapshot: no production API configured; using checked-in snapshot.')
  process.exit(0)
}

const deadline = Date.now() + 180_000
let attempt = 0

while (true) {
  try {
    const response = await fetch(`${apiBase}/menu-items?availableOnly=true`, {
      signal: AbortSignal.timeout(30_000),
    })
    if (!response.ok) throw new Error(`Menu API returned HTTP ${response.status}`)
    const items = await response.json()
    if (!Array.isArray(items) || items.some((item) =>
      !Number.isInteger(item?.id) || item.id <= 0 || typeof item.name !== 'string')) {
      throw new Error('Menu API returned invalid menu data')
    }
    await writeFile(outputPath, `${JSON.stringify(items)}\n`, 'utf8')
    console.log(`Menu snapshot: bundled ${items.length} available items.`)
    break
  } catch (error) {
    if (Date.now() >= deadline) {
      throw new Error(`Could not build a current menu snapshot: ${error.message}`)
    }
    const delay = Math.min(2_000 * 2 ** attempt, 10_000)
    attempt++
    await new Promise((resolve) => setTimeout(resolve, delay))
  }
}
