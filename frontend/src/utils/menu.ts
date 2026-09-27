import {
  CATEGORY_PILL_CLASSES,
  CATEGORY_THEME_STYLES,
  DEFAULT_CATEGORY_STYLE,
  DRINK_SUBCATEGORIES,
  FOOD_SUBCATEGORIES,
  MENU_ITEM_DESCRIPTIONS,
} from '../config/constants'
import axios from 'axios'

const normalizeCategory = (category?: string) => (category ?? '').trim().toLowerCase()

const FOOD_CATEGORY_SET = new Set(FOOD_SUBCATEGORIES.map((cat) => cat.toLowerCase()))
const DRINK_CATEGORY_SET = new Set(DRINK_SUBCATEGORIES.map((cat) => cat.toLowerCase()))

export const isFoodCategory = (category?: string) =>
  FOOD_CATEGORY_SET.has(normalizeCategory(category))

export const isDrinkCategory = (category?: string) =>
  DRINK_CATEGORY_SET.has(normalizeCategory(category))

// Aliases that match the business language (Meals / Beverages)
export const isMealCategory = isFoodCategory
export const isBeverageCategory = isDrinkCategory

export const getCategoryPillClass = (category?: string) =>
  CATEGORY_PILL_CLASSES[normalizeCategory(category)] || 'pill-default'

export const getCategoryStyles = (category?: string) => {
  if (!category) return DEFAULT_CATEGORY_STYLE
  const cleanCategory = category.trim()
  return CATEGORY_THEME_STYLES[cleanCategory] || DEFAULT_CATEGORY_STYLE
}

const BUNDLED_IMAGE_ALIASES: Record<string, string> = {
  'chicken-creamy-mushroom-n-aglio-olio-rice.webp': 'chicken-creamy-mushroom-n-aglio-olio.webp',
  'hungarian-sausage-n-aglio-olio-rice.webp': 'hungarian-sausage-n-aglio-olio.webp',
  'white-chocolate.webp': 'white-choco.webp',
  'biscoff.webp': 'biscoff-latte.webp',
  'chocolate-chip-cream.webp': 'choco-chip-cream.webp',
  'caramel-oreo.webp': 'caramel-oreo-frappe.webp',
  'avocado-creamcheese.webp': 'avocado-cream-chesse.webp',
  'chocolate-float.webp': 'choco-float.webp',
  'salted-caramel-float.webp': 'salted-caramel.webp',
}

const PHOTOS_NOT_BUNDLED = new Set([
  'crispy-chicken-fingers-n-aglio-olio-rice.webp',
  'mango-cheesecake.webp',
  'caramel-float-cereal.webp',
  'mocha-float.webp',
  'cheese-cake.webp',
  'peach-soda.webp',
])

/**
 * Returns the runtime API base URL. Uses axios.defaults.baseURL (set once in
 * main.ts at boot) so the value is always the *runtime* config, not the
 * build-time VITE_API_BASE_URL that Vite bakes into the bundle.
 */
const getApiBase = () => (axios.defaults.baseURL ?? '').toString().replace(/\/$/, '')

export const getImagePath = (item?: { imageName?: string | null }) => {
  const imageName = item?.imageName?.trim()
  if (!imageName) return ''
  if (/^(https?:\/\/|data:|blob:)/i.test(imageName)) {
    return imageName
  }
  if (imageName.startsWith('/')) {
    const base = getApiBase()
    if (/^\/uploads\//i.test(imageName) && base) {
      return `${base}${imageName}`
    }
    return imageName
  }
  const base = getApiBase()
  // New and legacy uploads use a generated 32-character filename. Seeded images ship with the frontend.
  if (/^[a-f0-9]{32}\.(?:jpg|jpeg|png|webp)$/i.test(imageName)) {
    return base ? `${base}/uploads/${imageName}` : `/uploads/${imageName}`
  }
  if (PHOTOS_NOT_BUNDLED.has(imageName)) return '/items/photo-unavailable.svg'
  return `/items/${encodeURIComponent(BUNDLED_IMAGE_ALIASES[imageName] ?? imageName)}`
}

export const getItemDescription = (item?: { name?: string; description?: string } | null): string => {
  if (!item) return ''
  if (item.description && item.description.trim()) {
    return item.description.trim()
  }
  const cleanName = (item.name ?? '').trim()
  if (MENU_ITEM_DESCRIPTIONS[cleanName]) {
    return MENU_ITEM_DESCRIPTIONS[cleanName]
  }
  const baseName = cleanName.replace(/^(Hot|Iced)\s+/i, '')
  if (MENU_ITEM_DESCRIPTIONS[baseName]) {
    return MENU_ITEM_DESCRIPTIONS[baseName]
  }
  return 'Delicious cafe specialty prepared fresh with premium ingredients.'
}
