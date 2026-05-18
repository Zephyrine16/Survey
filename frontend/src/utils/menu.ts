import {
  CATEGORY_PILL_CLASSES,
  CATEGORY_THEME_STYLES,
  CLOUDINARY_FOLDER,
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

  const cloudName = (import.meta.env.VITE_CLOUDINARY_CLOUD_NAME ?? '').trim()
  const safeImageName = encodeURIComponent(imageName)

  if (!cloudName) {
    return `/${CLOUDINARY_FOLDER}/${safeImageName}`
  }

  return `https://res.cloudinary.com/${cloudName}/image/upload/${CLOUDINARY_FOLDER}/${safeImageName}`
}
