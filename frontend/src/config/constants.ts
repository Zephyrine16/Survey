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

const DEFAULT_SURVEY_ITEM_LIMIT = 10
const DEFAULT_SURVEY_TEXT_MAX_LENGTH = 250
const DEFAULT_SURVEY_BASELINE_TARGET = 30
const DEFAULT_REPORT_FILENAME = 'FoodPreferenceSurvey_Analytics.csv'

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

export const AGE_GROUP_OPTIONS = [
  '18–20',
  '21–23',
  '24–26',
  '27–30',
  '31 and above',
] as const

export const DINING_FREQUENCY_OPTIONS = [
  'Several times a week',
  'Once a week',
  'Several times a month',
  'Once a month',
  'Less than once a month',
] as const

export const SECTION_1_DEMOGRAPHIC_QUESTIONS = [
  {
    id: 'demo_age_group',
    text: 'Age Group',
    type: 'RADIO',
    questionTypeLabel: 'Multiple choice',
    options: AGE_GROUP_OPTIONS,
  },
  {
    id: 'demo_dining_frequency',
    text: 'How often do you dine at cafés or restaurants?',
    type: 'RADIO',
    questionTypeLabel: 'Multiple choice',
    options: DINING_FREQUENCY_OPTIONS,
  },
] as const

export const RATING_SCALE_LEVELS = [
  { value: 1, label: 'Not Suitable' },
  { value: 2, label: 'Slightly Suitable' },
  { value: 3, label: 'Moderately Suitable' },
  { value: 4, label: 'Suitable' },
  { value: 5, label: 'Very Suitable' },
] as const

export const SECTION_2_MOOD_ROWS = [
  { id: 'energy', label: 'Energy (Wants something energizing)', short: 'Energy' },
  { id: 'comfort', label: 'Comfort (Wants something warm or familiar)', short: 'Comfort' },
  { id: 'refreshing', label: 'Refreshing (Wants something light or cooling)', short: 'Refreshing' },
  { id: 'healthy', label: 'Healthy (Wants a healthier choice)', short: 'Healthy' },
  { id: 'treat', label: 'Treat (Wants something enjoyable or indulgent)', short: 'Treat' },
  { id: 'focused', label: 'Focused (Wants to concentrate or study)', short: 'Focused' },
  { id: 'familiar', label: 'Familiar (Wants a safe, familiar choice)', short: 'Familiar' },
  { id: 'adventurous', label: 'Adventurous (Wants to try something new)', short: 'Adventurous' },
  { id: 'quick', label: 'Quick (Wants something convenient)', short: 'Quick' },
] as const

export const SECTION_2_WEATHER_ROWS = [
  { id: 'hot_sunny', label: 'Hot/Sunny', short: 'Hot/Sunny' },
  { id: 'hot_humid', label: 'Hot/Humid', short: 'Hot/Humid' },
  { id: 'rainy', label: 'Rainy', short: 'Rainy' },
  {
    id: 'cool_dry',
    label:
      'Cool Dry (Note: Even in tropical climates, "cool dry" exists: breezy December–February days, air-conditioned spaces, or cool hill stations/evening breezes.)',
    short: 'Cool Dry',
  },
] as const

export const SECTION_2_EVALUATION_QUESTIONS = [
  {
    id: 'sec2_mood',
    numberLabel: 'Q2.1',
    title: 'Question 1 — Mood Association',
    prompt: 'How suitable is this item for each of the following moods?',
    type: 'MATRIX',
    typeLabel: 'Rating Matrix (1–5 Likert Scale)',
    scopeLabel: '🍴 Item Evaluation',
    scale: RATING_SCALE_LEVELS,
    rows: SECTION_2_MOOD_ROWS,
    requiredNote: 'Require a response in each row',
  },
  {
    id: 'sec2_weather',
    numberLabel: 'Q2.2',
    title: 'Question 2 — Weather Association',
    prompt: 'How suitable is this item for each of the following weather conditions?',
    type: 'MATRIX',
    typeLabel: 'Rating Matrix (1–5 Likert Scale)',
    scopeLabel: '🍴 Item Evaluation',
    scale: RATING_SCALE_LEVELS,
    rows: SECTION_2_WEATHER_ROWS,
    requiredNote: 'Require a response in each row',
  },
] as const

export const MENU_ITEM_DESCRIPTIONS: Record<string, string> = {
  'Chicken Alfredo': 'Creamy pasta with chicken and Alfredo sauce.',
  'Aglio e Olio': 'Classic pasta tossed in sautéed garlic, extra virgin olive oil, and chili flakes.',
  'Carbonara': 'Rich and creamy pasta loaded with savory bacon and parmesan.',
  'Spaghetti': 'Traditional pasta served with savory, slow-cooked meat sauce.',
  'Tomato Pesto': 'Pasta tossed in sun-dried tomato and fragrant herbal pesto sauce.',
  'Tuna Pesto': 'Savory pasta infused with flaked tuna and aromatic basil pesto.',
  'Chicken Creamy Mushroom n Aglio Olio Rice':
    'Tender chicken in savory mushroom cream sauce paired with fragrant aglio olio rice.',
  'Salisbury Steak n Mushroom Sauce': 'Savory beef patties smothered in rich brown mushroom gravy.',
  'Fish Fillet n Buttermilk Sauce': 'Golden crispy fish fillet drizzled with tangy, creamy buttermilk sauce.',
  'Crispy Chicken Fingers n Aglio Olio Rice':
    'Crispy breaded chicken strips served with garlic-infused aglio olio rice.',
  'Hungarian Sausage n Aglio Olio Rice': 'Smoky, spiced Hungarian sausage served alongside aglio olio rice.',
  'Corned Beef and Eggs': 'Classic savory corned beef served with fried eggs and rice.',
  'Spam n Eggs': 'Crispy pan-fried Spam slices paired with fluffy eggs.',
  'Italian Meatballs n Basil Tomato Sauce': 'Hearty seasoned meatballs simmered in rich basil tomato sauce.',
  'Bacon Sandwich': 'Crispy bacon layered with fresh greens and dressing in toasted bread.',
  'Ham Sandwich': 'Savory sliced ham with crisp lettuce and spread on fresh artisan bread.',
  'Spam Sandwich': 'Pan-fried Spam with savory spread served on warm toasted bread.',
  'Chicken Sandwich': 'Tender seasoned chicken breast with fresh vegetables and dressing.',
  'Pepperoni Cheese Panini': 'Warm pressed panini packed with zesty pepperoni and melted cheese.',
  'Chicken Pesto Panini': 'Grilled chicken, basil pesto, and melted cheese toasted to perfection.',
  'Classic Waffle': 'Golden crisp Belgian waffle served with butter and maple syrup.',
  'Apple Waffle': 'Warm waffle topped with spiced cinnamon apples and sweet syrup.',
  'Biscoff Waffle': 'Crispy waffle drizzled with creamy Lotus Biscoff spread and biscuit crumbs.',
  'Ice Cream Waffle': 'Warm golden waffle crowned with a scoop of premium ice cream.',
  'Blueberry Waffle': 'Fluffy waffle loaded with sweet, tangy wild blueberry compote.',
  'Cheesy Waffle': 'Savory-sweet waffle infused with melted cheddar cheese.',
  'Choco Waffle': 'Decadent waffle drizzled with rich Belgian chocolate sauce.',
  'Mango Waffle': 'Crisp waffle topped with sweet ripe mango slices and cream.',
  'Bacon Waffle': 'Savory-sweet waffle paired with crisp smoky bacon strips.',
  'Nutella Waffle': 'Golden waffle generously smothered with rich Nutella hazelnut spread.',
  'Strawberry Waffle': 'Fluffy waffle topped with fresh sweet strawberries and glaze.',
  'Hot Americano': 'Rich, bold espresso shots diluted with fresh hot water.',
  'Iced Americano': 'Rich, bold espresso shots served over chilled water and ice.',
  'Hot Caffe Latte': 'Smooth espresso blended with steamed milk and a light layer of foam.',
  'Iced Caffe Latte': 'Smooth espresso poured over chilled milk and fresh ice.',
  'Hot Vanilla Latte': 'Silky espresso and steamed milk infused with sweet aromatic vanilla.',
  'Iced Vanilla Latte': 'Chilled espresso and milk blended with sweet aromatic vanilla syrup.',
  'Hot Spanish Latte': 'Creamy, rich espresso sweetened with luscious condensed milk.',
  'Iced Spanish Latte': 'Chilled espresso and milk sweetened with velvety condensed milk.',
  'Hot Cappuccino': 'Dark espresso topped with deep velvety steamed milk foam.',
  'Iced Cappuccino': 'Bold espresso combined with chilled milk and a layer of creamy froth.',
  'Hot Caramel Macchiato':
    'Fresh steamed milk with vanilla, marked with espresso and drizzled with caramel.',
  'Iced Caramel Macchiato':
    'Chilled milk with vanilla, marked with bold espresso and rich caramel drizzle.',
  'Hot Salted Caramel': 'Espresso and steamed milk infused with buttery sweet salted caramel.',
  'Iced Salted Caramel': 'Iced espresso and milk layered with buttery sweet salted caramel.',
  'Hot Cafe Mocha': 'Espresso combined with bittersweet rich mocha sauce and steamed milk.',
  'Iced Cafe Mocha': 'Chilled espresso blended with bittersweet chocolate mocha and milk.',
  'Hot White Chocolate': 'Smooth espresso blended with sweet velvety white chocolate and milk.',
  'Iced White Chocolate': 'Iced espresso combined with sweet white chocolate and fresh milk.',
  'Hot Caramel Oat Latte': 'Earthy oat milk latte infused with buttery sweet caramel syrup.',
  'Iced Caramel Oat Latte': 'Chilled oat milk latte with rich buttery caramel drizzle.',
  'Iced Biscoff Latte': 'Chilled espresso and milk layered with spiced speculoos Lotus Biscoff.',
  'Iced Shaken Affogato': 'Bold espresso shaken over ice and poured over creamy sweet milk.',
  'Hot Chocolate': 'Rich, velvety cocoa blended with steamed fresh milk.',
  'Iced Chocolate': 'Decadent cold cocoa served over fresh milk and ice.',
  'Hot Matcha': 'Authentic Japanese ceremonial matcha whisked with velvety steamed milk.',
  'Iced Matcha': 'Authentic Japanese ceremonial matcha served chilled over fresh milk.',
  'Caramel Macchiato': 'Ice-blended espresso frappe with sweet vanilla and rich caramel drizzle.',
  'Salted Caramel': 'Ice-blended frappe infused with buttery salted caramel and whipped cream.',
  'Cafe Mocha': 'Ice-blended coffee frappe with decadent dark chocolate mocha.',
  'White Chocolate': 'Blended ice frappe crafted with creamy sweet white chocolate.',
  'Coffee Jelly': 'Ice-blended coffee frappe loaded with chewy, flavorful coffee jelly cubes.',
  'Strawberry Ice cream': 'Creamy blended ice cream shake loaded with sweet strawberry goodness.',
  Vanilla: 'Smooth and classic ice-blended vanilla frappe topped with whipped cream.',
  Biscoff: 'Decadent blended frappe loaded with caramelized Lotus Biscoff speculoos.',
  'Chocolate Chip Cream': 'Creamy blended frappe studded with crunchy chocolate chips.',
  Matcha: 'Rich ice-blended matcha green tea frappe crowned with whipped cream.',
  'Caramel Oreo': 'Decadent blended frappe loaded with crushed Oreo cookies and caramel drizzle.',
  'Mango Cheesecake': 'Rich blended dessert frappe with sweet mango and savory cheesecake notes.',
  'Avocado Creamcheese': 'Silky blended frappe featuring real avocado and velvety cream cheese.',
  'Caramel Float Cereal': 'Chilled caramel beverage topped with vanilla ice cream and crunchy cereal.',
  'Matcha Float': 'Refreshing iced matcha latte topped with a scoop of vanilla ice cream.',
  'Chocolate Float': 'Rich iced chocolate drink crowned with creamy vanilla ice cream.',
  'Mocha Float': 'Chilled mocha drink topped with a generous scoop of vanilla ice cream.',
  'Salted Caramel Float': 'Sweet and salty caramel drink crowned with creamy vanilla ice cream.',
  Okinawa: 'Traditional milk tea infused with roasted brown sugar flavor.',
  'Cookies and Cream': 'Rich milk tea blended with crushed chocolate cream cookies.',
  Wintermelon: 'Sweet, refreshing milk tea flavored with traditional wintermelon syrup.',
  Chocolate: 'Smooth, decadent milk tea with rich chocolate flavor.',
  'Cheese Cake': 'Creamy milk tea layered with savory, velvety cheesecake foam.',
  'Blue Ocean': 'Vibrant sparkling soda with tropical blue curacao citrus notes.',
  'Sunset Dream': 'Layered effervescent sparkling soda inspired by warm tropical sunset flavors.',
  'Strawberry Yakult': 'Fizzy sparkling drink blended with tangy Yakult and sweet strawberry.',
  'Peach Soda': 'Crisp and bubbly sparkling soda infused with sweet juicy peach flavor.',
  'Green Apple': 'Zesty and tart sparkling soda featuring crisp green apple flavor.',
  Blueberry: 'Sweet, fizzy sparkling soda infused with ripe wild blueberry flavor.',
  'Peach Tea': 'Chilled brewed tea infused with natural sweet peach extract.',
  'Lemon Fruit Tea': 'Invigorating iced tea infused with fresh citrusy lemon juice.',
  'Strawberry Fruit Tea': 'Refreshing brewed tea naturally flavored with sweet summer strawberries.',
  'Kiwi Fruit Tea': 'Crisp and tangy iced fruit tea infused with vibrant kiwi essence.',
  'Blueberry Fruit Tea': 'Chilled fragrant iced tea bursting with sweet wild blueberry flavor.',
}
