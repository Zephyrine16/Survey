import { writeFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'

const apiBase = (process.env.VITE_API_BASE_URL ?? '').trim().replace(/\/$/, '')
const menuOutputPath = fileURLToPath(new URL('../src/generated/menu-snapshot.json', import.meta.url))
const questionsOutputPath = fileURLToPath(new URL('../src/generated/questions-snapshot.json', import.meta.url))

// Local builds without a production API use the checked-in snapshots.
if (!apiBase || apiBase === 'https://api.survey.example.com') {
  console.log('Survey snapshot: no production API configured; using checked-in snapshots.')
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
    const questionsResponse = await fetch(`${apiBase}/questions/all`, {
      signal: AbortSignal.timeout(30_000),
    })
    if (!questionsResponse.ok) throw new Error(`Questions API returned HTTP ${questionsResponse.status}`)
    const questions = await questionsResponse.json()
    if (!Array.isArray(questions) ||
      !questions.some((question) => /mood|emotion/i.test(question?.text ?? '') && question.options?.length) ||
      !questions.some((question) => /weather/i.test(question?.text ?? '') && question.options?.length)) {
      throw new Error('Questions API returned incomplete mood or weather options')
    }
    await writeFile(menuOutputPath, `${JSON.stringify(items)}\n`, 'utf8')
    await writeFile(questionsOutputPath, `${JSON.stringify(questions)}\n`, 'utf8')
    console.log(`Survey snapshot: bundled ${items.length} menu items and ${questions.length} questions.`)
    break
  } catch (error) {
    if (Date.now() >= deadline) {
      throw new Error(`Could not build a current survey snapshot: ${error.message}`)
    }
    const delay = Math.min(2_000 * 2 ** attempt, 10_000)
    attempt++
    await new Promise((resolve) => setTimeout(resolve, delay))
  }
}
