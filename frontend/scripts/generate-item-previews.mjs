import { readdir, readFile, mkdir, writeFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import { join } from 'node:path'
import sharp from 'sharp'

const itemsDir = fileURLToPath(new URL('../public/items/', import.meta.url))
const previewsDir = join(itemsDir, 'previews')
await mkdir(previewsDir, { recursive: true })

let originalBytes = 0
let previewBytes = 0
let count = 0

for (const name of (await readdir(itemsDir)).filter((name) => name.endsWith('.webp')).sort()) {
  const source = await readFile(join(itemsDir, name))
  const reduced = await sharp(source)
    .rotate()
    .resize({ width: 1000, height: 1000, fit: 'inside', withoutEnlargement: true })
    .webp({ quality: 76, effort: 5 })
    .toBuffer()
  const preview = reduced.length < source.length ? reduced : source

  await writeFile(join(previewsDir, name), preview)
  originalBytes += source.length
  previewBytes += preview.length
  count++
}

console.log(
  `Generated ${count} survey previews: ${(originalBytes / 1024 / 1024).toFixed(2)} MB → ${(previewBytes / 1024 / 1024).toFixed(2)} MB`,
)
