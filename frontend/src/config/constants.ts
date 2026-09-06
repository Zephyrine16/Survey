const requireEnv = (value: string | undefined, name: string, fallback?: string) => {
  const trimmed = (value ?? '').trim()
  if (trimmed) {
    return trimmed
  }
  if (fallback !== undefined) {
    return fallback
  }
  throw new Error(`${name} must be set.`)
}

const parsePositiveInt = (value: string | undefined, name: string, fallback?: number) => {
  const trimmed = (value ?? '').trim()
  if (!trimmed && fallback !== undefined) {
    return fallback
  }
  const parsed = Number.parseInt(requireEnv(value, name), 10)
  if (!Number.isFinite(parsed) || parsed <= 0) {
    throw new Error(`${name} must be a positive integer.`)
  }
  return parsed
}

const DEFAULT_SURVEY_ITEM_LIMIT = 15
const DEFAULT_SURVEY_TEXT_MAX_LENGTH = 250
const DEFAULT_SURVEY_BASELINE_TARGET = 30
const DEFAULT_REPORT_FILENAME = 'CafeRater_Analytics.csv'

export const SURVEY_ITEM_LIMIT = parsePositiveInt(
  import.meta.env.VITE_SURVEY_ITEM_LIMIT,
  'VITE_SURVEY_ITEM_LIMIT',
  DEFAULT_SURVEY_ITEM_LIMIT,
)
export const SURVEY_TEXT_MAX_LENGTH = parsePositiveInt(
  import.meta.env.VITE_SURVEY_TEXT_MAX_LENGTH,
  'VITE_SURVEY_TEXT_MAX_LENGTH',
  DEFAULT_SURVEY_TEXT_MAX_LENGTH,
)
export const SURVEY_BASELINE_TARGET = parsePositiveInt(
  import.meta.env.VITE_SURVEY_BASELINE_TARGET,
  'VITE_SURVEY_BASELINE_TARGET',
  DEFAULT_SURVEY_BASELINE_TARGET,
)
export const REPORT_FILENAME = requireEnv(
  import.meta.env.VITE_REPORT_FILENAME,
  'VITE_REPORT_FILENAME',
  DEFAULT_REPORT_FILENAME,
)

export const FOOD_SUBCATEGORIES = [
  'APPETIZER',
  'PASTA',
  'SANDWICH & WRAPS',
  'CHICKEN WINGS',
  'RICE MEAL',
]
export const DRINK_SUBCATEGORIES = [
  'CLASSICS',
  'ICE-BLENDED',
  'SPECIALTY',
  'NON-COFFEE',
  'REFRESHER',
  'CEREMONIAL MATCHA',
]

// Keep aliases for clarity in call-sites that prefer MEAL/BEVERAGE naming
export const MEAL_SUBCATEGORIES = FOOD_SUBCATEGORIES
export const BEVERAGE_SUBCATEGORIES = DRINK_SUBCATEGORIES

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
  appetizer: 'pill-appetizer',
  pasta: 'pill-pasta',
  'sandwich & wraps': 'pill-sandwich',
  'chicken wings': 'pill-wings',
  'rice meal': 'pill-ricemeal',
  classics: 'pill-classics',
  'ice-blended': 'pill-iceblended',
  specialty: 'pill-specialty',
  'non-coffee': 'pill-noncoffee',
  refresher: 'pill-refresher',
  'ceremonial matcha': 'pill-matcha',
}

export const CATEGORY_THEME_STYLES: Record<string, Record<string, string>> = {
  APPETIZER: { '--c-main': '#f97316', '--c-light': '#fff7ed', '--c-text': '#9a3412' },
  PASTA: { '--c-main': '#eab308', '--c-light': '#fefce8', '--c-text': '#854d0e' },
  'SANDWICH & WRAPS': { '--c-main': '#d97706', '--c-light': '#fdf5e6', '--c-text': '#7c2d12' },
  'CHICKEN WINGS': { '--c-main': '#ef4444', '--c-light': '#fef2f2', '--c-text': '#991b1b' },
  'RICE MEAL': { '--c-main': '#f59e0b', '--c-light': '#fffbeb', '--c-text': '#92400e' },
  CLASSICS: { '--c-main': '#8b5cf6', '--c-light': '#f5f3ff', '--c-text': '#5b21b6' },
  'ICE-BLENDED': { '--c-main': '#06b6d4', '--c-light': '#ecfeff', '--c-text': '#155e75' },
  SPECIALTY: { '--c-main': '#ec4899', '--c-light': '#fdf2f8', '--c-text': '#9d174d' },
  'NON-COFFEE': { '--c-main': '#0ea5e9', '--c-light': '#f0f9ff', '--c-text': '#0c4a6e' },
  REFRESHER: { '--c-main': '#10b981', '--c-light': '#ecfdf5', '--c-text': '#065f46' },
  'CEREMONIAL MATCHA': { '--c-main': '#22c55e', '--c-light': '#f0fdf4', '--c-text': '#14532d' },
}

export const DEFAULT_CATEGORY_STYLE = {
  '--c-main': '#f97316',
  '--c-light': '#fff7ed',
  '--c-text': '#c2410c',
}
