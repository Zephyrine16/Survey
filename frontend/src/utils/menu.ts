import {
  CATEGORY_PILL_CLASSES,
  CATEGORY_THEME_STYLES,
  DEFAULT_CATEGORY_STYLE,
  DRINK_SUBCATEGORIES,
  FOOD_SUBCATEGORIES,
} from '../config/constants'

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

export const getImagePath = (item?: { imageName?: string | null }) => {
  const imageName = item?.imageName?.trim()
  if (!imageName) return ''
  if (/^(https?:\/\/|data:|blob:)/i.test(imageName)) {
    return imageName
  }
  if (imageName.startsWith('/')) {
    const base = (import.meta.env.VITE_API_BASE_URL ?? '').toString().replace(/\/$/, '')
    if (/^\/uploads\//i.test(imageName) && base) {
      return `${base}${imageName}`
    }
    return imageName
  }
  const base = (import.meta.env.VITE_API_BASE_URL ?? '').toString().replace(/\/$/, '')
  const encoded = encodeURIComponent(imageName)
  if (base) {
    return `${base}/uploads/${encoded}`
  }
  return `/uploads/${encoded}`
}
