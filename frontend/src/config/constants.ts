const requireEnvNumber = (name: string, value: string | undefined) => {
  const trimmed = (value ?? '').trim()
  const parsed = Number.parseInt(trimmed, 10)
  if (!trimmed || !Number.isFinite(parsed) || parsed <= 0) {
    throw new Error(`Missing or invalid ${name} environment variable.`)
  }
  return parsed
}

const requireEnvString = (name: string, value: string | undefined) => {
  const trimmed = (value ?? '').trim()
  if (!trimmed) {
    throw new Error(`Missing ${name} environment variable.`)
  }
  return trimmed
}

export const CLOUDINARY_FOLDER = requireEnvString(
  'VITE_CLOUDINARY_FOLDER',
  import.meta.env.VITE_CLOUDINARY_FOLDER,
)
export const SURVEY_ITEM_LIMIT = requireEnvNumber(
  'VITE_SURVEY_ITEM_LIMIT',
  import.meta.env.VITE_SURVEY_ITEM_LIMIT,
)
export const SURVEY_TEXT_MAX_LENGTH = requireEnvNumber(
  'VITE_SURVEY_TEXT_MAX_LENGTH',
  import.meta.env.VITE_SURVEY_TEXT_MAX_LENGTH,
)
export const SURVEY_BASELINE_TARGET = requireEnvNumber(
  'VITE_SURVEY_BASELINE_TARGET',
  import.meta.env.VITE_SURVEY_BASELINE_TARGET,
)
export const REPORT_FILENAME = requireEnvString(
  'VITE_REPORT_FILENAME',
  import.meta.env.VITE_REPORT_FILENAME,
)

export const FOOD_SUBCATEGORIES = ['Meal', 'Bread', 'Pasta', 'Waffle']
export const DRINK_SUBCATEGORIES = [
  'Coffee',
  'Non-coffee',
  'Frappe Series',
  'Float',
  'Milktea',
  'Sparkling Soda',
  'Fruit Tea',
]

export const QUICK_EMOJIS = [
  '🔘',
  '⭐',
  '❤️',
  '👍',
  '👎',
  '🧂',
  '🌶️',
  '🧀',
  '🍋',
  '🍫',
  '☕',
  '🍰',
  '🍔',
  '🍕',
  '🥗',
  '😊',
  '😐',
  '😞',
]

export const CATEGORY_PILL_CLASSES: Record<string, string> = {
  meal: 'pill-meal',
  bread: 'pill-bread',
  pasta: 'pill-pasta',
  waffle: 'pill-waffle',
  coffee: 'pill-coffee',
  'non-coffee': 'pill-noncoffee',
  'frappe series': 'pill-frappe',
  float: 'pill-float',
  'sparkling soda': 'pill-soda',
  milktea: 'pill-milktea',
  'fruit tea': 'pill-fruittea',
}

export const CATEGORY_THEME_STYLES: Record<string, Record<string, string>> = {
  Meal: { '--c-main': '#ef4444', '--c-light': '#fef2f2', '--c-text': '#b91c1c' },
  Bread: { '--c-main': '#d97706', '--c-light': '#fdf5e6', '--c-text': '#8b5a2b' },
  Pasta: { '--c-main': '#eab308', '--c-light': '#fefce8', '--c-text': '#854d0e' },
  Waffle: { '--c-main': '#f97316', '--c-light': '#fff7ed', '--c-text': '#c2410c' },
  Coffee: { '--c-main': '#f59e0b', '--c-light': '#fffbeb', '--c-text': '#b45309' },
  'Non-coffee': { '--c-main': '#0ea5e9', '--c-light': '#f0f9ff', '--c-text': '#0284c7' },
  'Frappe Series': { '--c-main': '#8b5cf6', '--c-light': '#f5f3ff', '--c-text': '#7c3aed' },
  Float: { '--c-main': '#10b981', '--c-light': '#ecfdf5', '--c-text': '#059669' },
  'Sparkling Soda': { '--c-main': '#06b6d4', '--c-light': '#ecfeff', '--c-text': '#0891b2' },
  Milktea: { '--c-main': '#d946ef', '--c-light': '#fdf4ff', '--c-text': '#c026d3' },
  'Fruit Tea': { '--c-main': '#ec4899', '--c-light': '#fdf2f8', '--c-text': '#be185d' },
}

export const DEFAULT_CATEGORY_STYLE = {
  '--c-main': '#f97316',
  '--c-light': '#fff7ed',
  '--c-text': '#c2410c',
}
