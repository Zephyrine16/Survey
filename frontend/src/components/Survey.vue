<template>
  <div class="survey-layout">
    <div v-if="!hasStarted" class="welcome-screen">
      <div class="welcome-card glass-effect">
        <div class="welcome-icon">🍴</div>
        <h1>Welcome to the Food Preference Survey!</h1>
        <p>
          Help us build a smarter AI by rating our menu items. Your feedback directly shapes the
          future of our cafe!
        </p>
        <div class="disclaimer-box">
          <span class="info-icon">⚠️</span>
          <p>
            <strong>DISCLAIMER:</strong> The photos used in this survey are from the internet for
            illustrative purposes only. Actual cafe servings and presentation may vary!
          </p>
        </div>
        <button class="primary-btn pulse" @click="hasStarted = true">
          Start the Survey &rarr;
        </button>
      </div>
    </div>

    <div v-else class="app-container">
      <header class="top-nav">
        <div class="nav-content">
          <div class="logo">
            <span class="logo-icon">🍴</span>
            <h1>Food Preference Survey</h1>
          </div>

          <div class="header-actions">
            <template v-if="currentSection === 1">
              <div class="global-progress">
                <span class="section-indicator-badge blue-badge">Section 1 of 2</span>
                <span class="section-badge-label">Respondent Information</span>
              </div>
            </template>
            <template v-else>
              <div class="global-progress">
                <span class="section-indicator-badge orange-badge">Section 2 of 2</span>
                Item <strong>{{ currentItemIndex + 1 }}</strong> of {{ menuItems.length }}
              </div>
              <button
                class="header-finish-btn pulse-light"
                @click="showConfirmModal = true"
                :disabled="!isCurrentItemComplete"
                :class="{ 'disabled-btn': !isCurrentItemComplete }"
              >
                I'm Done
              </button>
            </template>
          </div>
        </div>
      </header>

      <main class="main-content">
        <!-- ======================================================== -->
        <!-- SECTION 1 — Respondent Information                       -->
        <!-- ======================================================== -->
        <div v-if="currentSection === 1" class="demographic-view fade-in">
          <div class="demographic-card">
            <div class="section-banner">
              <div class="section-scope-pill">
                <span class="scope-dot blue-dot"></span> SECTION 1 OF 2
              </div>
              <h2>SECTION 1 — Respondent Information</h2>
              <p class="section-desc">
                Please provide your demographic background before proceeding to the menu evaluations.
              </p>
            </div>

            <div class="demographic-questions">
              <!-- Question 1: Age Group -->
              <div class="question-card demo-card">
                <div class="q-header">
                  <div
                    class="q-bubble demo-bubble"
                    :class="{ answered: !!demographicAnswers.ageGroup }"
                  >
                    <span v-if="demographicAnswers.ageGroup">✓</span>
                    <span v-else>1</span>
                  </div>
                  <div>
                    <h4>Age Group</h4>
                  </div>
                </div>

                <div class="vertical-options">
                  <button
                    v-for="opt in ageGroupOptions"
                    :key="opt"
                    type="button"
                    class="opt-btn-vertical demo-opt-btn"
                    :class="{ selected: demographicAnswers.ageGroup === opt }"
                    @click="demographicAnswers.ageGroup = opt"
                  >
                    <span
                      class="custom-radio-circle"
                      :class="{ active: demographicAnswers.ageGroup === opt }"
                    ></span>
                    <span class="opt-label">{{ opt }}</span>
                  </button>
                </div>
              </div>

              <!-- Question 2: Dining Frequency -->
              <div class="question-card demo-card">
                <div class="q-header">
                  <div
                    class="q-bubble demo-bubble"
                    :class="{ answered: !!demographicAnswers.diningFrequency }"
                  >
                    <span v-if="demographicAnswers.diningFrequency">✓</span>
                    <span v-else>2</span>
                  </div>
                  <div>
                    <h4>How often do you dine at cafés or restaurants?</h4>
                  </div>
                </div>

                <div class="vertical-options">
                  <button
                    v-for="opt in diningFrequencyOptions"
                    :key="opt"
                    type="button"
                    class="opt-btn-vertical demo-opt-btn"
                    :class="{ selected: demographicAnswers.diningFrequency === opt }"
                    @click="demographicAnswers.diningFrequency = opt"
                  >
                    <span
                      class="custom-radio-circle"
                      :class="{ active: demographicAnswers.diningFrequency === opt }"
                    ></span>
                    <span class="opt-label">{{ opt }}</span>
                  </button>
                </div>
              </div>
            </div>

            <div class="action-footer demographic-footer">
              <p v-if="!isDemographicComplete" class="incomplete-warning">
                * Please answer both questions to unlock Section 2.
              </p>
              <p v-else class="complete-info">
                ✓ All respondent details provided!
              </p>
              <button
                type="button"
                class="nav-btn primary demo-proceed-btn"
                :disabled="!isDemographicComplete"
                :class="{ 'disabled-btn': !isDemographicComplete }"
                @click="proceedToSection2"
              >
                Continue to Section 2: Menu Evaluation &rarr;
              </button>
            </div>
          </div>
        </div>

        <!-- ======================================================== -->
        <!-- SECTION 2 — Menu Item Evaluation                         -->
        <!-- ======================================================== -->
        <div v-else class="rating-view fade-in">
          <div class="left-pane">
            <div class="sticky-card">
              <div class="section-switch-header">
                <button
                  type="button"
                  class="back-to-sec1-btn"
                  @click="goToSection1"
                  title="Return to Section 1 to review or change respondent info"
                >
                  &larr; Back to Section 1 (Respondent Info)
                </button>
                <button
                  type="button"
                  class="view-instructions-btn"
                  @click="showInstructionsModal = true"
                  title="View Section 2 instructions and rating scale"
                >
                  📋 View Instructions
                </button>
              </div>
              <div class="pane-header">
                <span class="item-tag-pill">MENU ITEM {{ currentItemIndex + 1 }}</span>
                <span class="cat-progress"
                  ><strong class="green-text">{{ completedItemsCount }}</strong
                  >/{{ menuItems.length }} done</span
                >
              </div>

              <div class="item-cover">
                <div
                  class="cover-img"
                  :style="
                    currentItem?.imageName
                      ? { backgroundImage: `url('${getImagePath(currentItem)}')` }
                      : {}
                  "
                ></div>
                <div class="cover-info">
                  <span class="menu-item-sub-tag">MENU ITEM {{ currentItemIndex + 1 }}</span>
                  <h3>{{ currentItem?.name }}</h3>
                  <span class="badge" :class="getCategoryPillClass(currentItem?.category)"
                    >🍴 {{ currentItem?.category }}</span
                  >
                </div>
              </div>

              <div class="item-desc-panel">
                <span class="desc-tag">Description:</span>
                <p class="desc-text">{{ getItemDescription(currentItem) }}</p>
              </div>

              <div class="image-disclaimer">* Image is for illustration purposes only.</div>
            </div>
          </div>

          <div class="right-pane">
            <div class="questions-list">
              <!-- Question 1 — Mood Association -->
              <div class="question-card grid-question-card">
                <div class="q-header">
                  <div
                    class="q-bubble grid-bubble"
                    :class="{ answered: isMoodComplete(currentItem?.id) }"
                  >
                    <span v-if="isMoodComplete(currentItem?.id)">✓</span>
                    <span v-else>1</span>
                  </div>
                  <div>
                    <h4>Question 1 — Mood Association</h4>
                  </div>
                </div>

                <p class="grid-prompt-text">
                  How suitable is <strong>{{ currentItem?.name }}</strong> for each of the following moods?
                </p>

                <div class="matrix-wrapper">
                  <table class="matrix-table">
                    <thead>
                      <tr>
                        <th class="col-corner">Mood</th>
                        <th v-for="scale in ratingLevels" :key="scale.value" class="col-scale">
                          <span class="scale-header-num">{{ scale.value }}</span>
                          <span class="scale-header-hint">{{ scale.label }}</span>
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr
                        v-for="mood in moodRows"
                        :key="mood.id"
                        class="matrix-tr"
                        :class="{ 'row-answered': getMoodAnswer(currentItem?.id, mood.id) !== null }"
                      >
                        <td class="matrix-row-title">
                          <span class="row-text">{{ mood.label }}</span>
                        </td>
                        <td
                          v-for="scale in ratingLevels"
                          :key="scale.value"
                          class="matrix-td"
                          @click="setMoodAnswer(currentItem?.id, mood.id, scale.value)"
                        >
                          <span
                            class="grid-radio-circle"
                            :class="{ active: getMoodAnswer(currentItem?.id, mood.id) === scale.value }"
                          ></span>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>

                <div class="grid-req-footer">
                  <span class="req-asterisk">*</span> Require a response in each row.
                </div>
              </div>

              <!-- Question 2 — Weather Association -->
              <div class="question-card grid-question-card">
                <div class="q-header">
                  <div
                    class="q-bubble grid-bubble"
                    :class="{ answered: isWeatherComplete(currentItem?.id) }"
                  >
                    <span v-if="isWeatherComplete(currentItem?.id)">✓</span>
                    <span v-else>2</span>
                  </div>
                  <div>
                    <h4>Question 2 — Weather Association</h4>
                  </div>
                </div>

                <p class="grid-prompt-text">
                  How suitable is <strong>{{ currentItem?.name }}</strong> for each of the following weather conditions?
                </p>

                <div class="matrix-wrapper">
                  <table class="matrix-table">
                    <thead>
                      <tr>
                        <th class="col-corner">Weather</th>
                        <th v-for="scale in ratingLevels" :key="scale.value" class="col-scale">
                          <span class="scale-header-num">{{ scale.value }}</span>
                          <span class="scale-header-hint">{{ scale.label }}</span>
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr
                        v-for="weather in weatherRows"
                        :key="weather.id"
                        class="matrix-tr"
                        :class="{ 'row-answered': getWeatherAnswer(currentItem?.id, weather.id) !== null }"
                      >
                        <td class="matrix-row-title">
                          <span class="row-text">{{ weather.label }}</span>
                        </td>
                        <td
                          v-for="scale in ratingLevels"
                          :key="scale.value"
                          class="matrix-td"
                          @click="setWeatherAnswer(currentItem?.id, weather.id, scale.value)"
                        >
                          <span
                            class="grid-radio-circle"
                            :class="{ active: getWeatherAnswer(currentItem?.id, weather.id) === scale.value }"
                          ></span>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>

                <div class="grid-req-footer">
                  <span class="req-asterisk">*</span> Require a response in each row.
                </div>
              </div>

              <div class="action-footer">
                <button
                  class="nav-btn secondary"
                  @click="prevItem"
                  :disabled="currentItemIndex === 0"
                >
                  &larr; Previous Item
                </button>

                <button
                  v-if="!isLastItem"
                  class="nav-btn primary"
                  @click="nextItem"
                  :disabled="!isCurrentItemComplete"
                >
                  Next Item &rarr;
                </button>

                <button
                  v-else
                  class="nav-btn success"
                  @click="showConfirmModal = true"
                  :disabled="!isCurrentItemComplete"
                >
                  Save Survey Answers
                </button>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>

    <Teleport to="body">
      <div v-if="showInstructionsModal" class="modal-overlay instructions-modal-overlay">
        <div class="modal-card instructions-modal-card">
          <div class="section-banner starter-banner instructions-modal-banner">
            <div class="section-scope-pill orange-scope-pill">
              <span class="scope-dot orange-dot"></span> SECTION 2 OF 2
            </div>
            <h2>SECTION 2 — Menu Item Evaluation</h2>
            <p class="section-desc">
              Evaluate our menu items to help train our AI recommendation engine.
            </p>
          </div>

          <div class="starter-instructions-box instructions-modal-box">
            <div class="instructions-header">
              <span class="instructions-icon">📋</span>
              <h3>Instructions</h3>
            </div>

            <div class="instructions-body">
              <p class="instruction-main">
                You will be asked to evaluate <strong>{{ menuItems.length || SURVEY_ITEM_LIMIT }} menu items</strong>.
              </p>
              <p class="instruction-sub">
                For each menu item, rate how suitable you think the item is for each mood and weather condition.
              </p>
            </div>

            <div class="rating-scale-box">
              <h4 class="scale-heading">Rating scale:</h4>
              <div class="scale-list">
                <div
                  v-for="item in ratingScaleGuide"
                  :key="item.value"
                  class="scale-item"
                  :class="`scale-item-${item.value}`"
                >
                  <span class="scale-badge">{{ item.value }}</span>
                  <span class="scale-separator"> — </span>
                  <span class="scale-label">{{ item.label }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="modal-actions instructions-modal-actions">
            <button
              type="button"
              class="nav-btn secondary"
              @click="showInstructionsModal = false; goToSection1()"
            >
              &larr; Back to Section 1
            </button>
            <button
              type="button"
              class="nav-btn primary starter-proceed-btn"
              @click="startSection2"
            >
              {{ completedItemsCount > 0 ? 'Continue Evaluation' : 'Start Section 2' }} &rarr;
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div v-if="showConfirmModal" class="modal-overlay">
        <div class="modal-card">
          <div class="modal-icon">👋</div>
          <h2>Ready to Finish?</h2>
          <p>
            Are you completely finished rating your items? If you have nothing else to review, click
            Confirm to complete your session!
          </p>
          <div class="modal-actions">
            <button
              class="nav-btn secondary"
              @click="showConfirmModal = false; showReviewModal = true"
            >
              Review Answers
            </button>
            <button class="nav-btn primary" @click="executeFinalSubmit">Confirm & Submit</button>
          </div>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div v-if="showReviewModal" class="modal-overlay">
        <div class="modal-card review-card">
          <div class="review-header">
            <h2>Review Your Answers</h2>
            <p>
              Summary for your {{ answeredItems.length }} item{{
                answeredItems.length === 1 ? '' : 's'
              }}.
            </p>
          </div>

          <div class="review-scroll-area">
            <!-- Review Section 1 -->
            <div class="review-section-box">
              <div class="review-section-title">
                <span class="scope-dot blue-dot"></span>
                <strong>SECTION 1 — Respondent Information</strong>
              </div>
              <div class="review-q-row">
                <div class="r-question">Age Group</div>
                <div class="r-answer">{{ demographicAnswers.ageGroup || 'Not answered' }}</div>
              </div>
              <div class="review-q-row">
                <div class="r-question">How often do you dine at cafés or restaurants?</div>
                <div class="r-answer">{{ demographicAnswers.diningFrequency || 'Not answered' }}</div>
              </div>
            </div>

            <!-- Review Section 2 -->
            <div class="review-section-box mt-3 mb-2">
              <div class="review-section-title">
                <span class="scope-dot orange-dot"></span>
                <strong>SECTION 2 — Menu Item Evaluations ({{ answeredItems.length }} item{{ answeredItems.length === 1 ? '' : 's' }})</strong>
              </div>
            </div>

            <div v-if="answeredItems.length === 0" class="empty-review">
              <p>You haven't rated any items yet!</p>
            </div>

            <div v-for="(item, index) in answeredItems" :key="item.id" class="review-item-block">
              <div class="review-item-header">
                <span class="review-item-number">{{ index + 1 }}</span>
                <div class="review-item-titles">
                  <h3>{{ item.name }}</h3>
                  <span class="badge" :class="getCategoryPillClass(item.category)"
                    >🍴 {{ item.category }}</span
                  >
                </div>
              </div>

              <div class="review-q-list">
                <!-- Question 1 Review -->
                <div class="review-grid-group">
                  <div class="review-grid-title">
                    <span class="review-q-tag">Q1</span>
                    <strong>Question 1 — Mood Association</strong>
                  </div>
                  <div class="review-grid-rows">
                    <div
                      v-for="mood in moodRows"
                      :key="mood.id"
                      class="review-grid-row"
                    >
                      <span class="review-row-name">{{ mood.label }}</span>
                      <span
                        class="review-row-val"
                        :class="{ 'text-missing': getMoodAnswer(item.id, mood.id) === null }"
                      >
                        {{ getMoodAnswer(item.id, mood.id) !== null ? `${getMoodAnswer(item.id, mood.id)} / 5 (${getScaleLabel(getMoodAnswer(item.id, mood.id)!)})` : 'Not answered' }}
                      </span>
                    </div>
                  </div>
                </div>

                <!-- Question 2 Review -->
                <div class="review-grid-group">
                  <div class="review-grid-title">
                    <span class="review-q-tag">Q2</span>
                    <strong>Question 2 — Weather Association</strong>
                  </div>
                  <div class="review-grid-rows">
                    <div
                      v-for="weather in weatherRows"
                      :key="weather.id"
                      class="review-grid-row"
                    >
                      <span class="review-row-name">{{ weather.label }}</span>
                      <span
                        class="review-row-val"
                        :class="{ 'text-missing': getWeatherAnswer(item.id, weather.id) === null }"
                      >
                        {{ getWeatherAnswer(item.id, weather.id) !== null ? `${getWeatherAnswer(item.id, weather.id)} / 5 (${getScaleLabel(getWeatherAnswer(item.id, weather.id)!)})` : 'Not answered' }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="review-footer">
            <button class="nav-btn secondary" @click="showReviewModal = false">
              Back to Survey
            </button>
            <button class="nav-btn primary" @click="executeFinalSubmit">
              Confirm & Submit Data 💾
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div v-if="showLimitModal" class="modal-overlay">
        <div class="modal-card">
          <div class="modal-icon">🎉</div>
          <h2>Maximum Limit Reached</h2>
          <p>We are no longer accepting new responses for this study.</p>
          <button class="primary-btn" @click="showLimitModal = false">Close Window</button>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div v-if="showSuccessModal" class="modal-overlay">
        <div class="modal-card">
          <div class="modal-icon">🎉</div>
          <h2>Amazing Job!</h2>
          <p>Your feedback is going to help us build a much smarter food AI. Thank you!</p>
          <button class="primary-btn" @click="resetSurvey">Start New Survey</button>
        </div>
      </div>
    </Teleport>
  </div>

  <div style="opacity: 0; position: absolute; top: -9999px; left: -9999px">
    <label for="phone_number">Phone Number</label>
    <input type="text" id="phone_number" v-model="honeypotField" tabindex="-1" autocomplete="off" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import axios from 'axios'
import {
  AGE_GROUP_OPTIONS,
  DINING_FREQUENCY_OPTIONS,
  SURVEY_ITEM_LIMIT,
  SURVEY_TEXT_MAX_LENGTH,
  SECTION_2_MOOD_ROWS,
  SECTION_2_WEATHER_ROWS,
  RATING_SCALE_LEVELS,
} from '../config/constants'
import { getCategoryPillClass, getImagePath, getItemDescription } from '../utils/menu'

// --- State ---
const hasStarted = ref(false)

// A fresh UUID generated on every page load. Sent with submissions so the
// backend treats each survey open as a completely independent new session,
// regardless of any existing participant cookie.
const sessionId = ref(crypto.randomUUID())

const ageGroupOptions = AGE_GROUP_OPTIONS
const diningFrequencyOptions = DINING_FREQUENCY_OPTIONS

const moodRows = SECTION_2_MOOD_ROWS
const weatherRows = SECTION_2_WEATHER_ROWS
const ratingLevels = RATING_SCALE_LEVELS

const currentSection = ref(1)
const showInstructionsModal = ref(false)

const ratingScaleGuide = RATING_SCALE_LEVELS

const demographicAnswers = ref({
  ageGroup: '',
  diningFrequency: '',
})

const isDemographicComplete = computed(() => {
  return (
    demographicAnswers.value.ageGroup.trim() !== '' &&
    demographicAnswers.value.diningFrequency.trim() !== ''
  )
})

const proceedToSection2 = () => {
  if (!isDemographicComplete.value) return
  currentSection.value = 2
  showInstructionsModal.value = true
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const startSection2 = () => {
  showInstructionsModal.value = false
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const goToSection1 = () => {
  currentSection.value = 1
  showInstructionsModal.value = false
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// Modal Flags
const showLimitModal = ref(false)
const showConfirmModal = ref(false)
const showSuccessModal = ref(false)
const showReviewModal = ref(false)
const honeypotField = ref('')

const menuItems = ref<any[]>([])
const currentItemIndex = ref(0)

// Answers Dictionary: { itemId: { moods: { moodId: rating }, weather: { weatherId: rating } } }
const answers = ref<
  Record<number, { moods?: Record<string, number>; weather?: Record<string, number> }>
>({})

// 1. The clean, empty reactive array
const questions = ref<any[]>([])

// 2. Fetch function
const fetchQuestions = async () => {
  try {
    const response = await axios.get('/questions/all')
    questions.value = response.data ?? []
  } catch (error: any) {
    console.error('Error fetching dynamic questions:', error)
  }
}

// --- Computed Properties ---
const currentItem = computed(() => {
  if (menuItems.value.length === 0) return null
  return menuItems.value[currentItemIndex.value]
})
const isLastItem = computed(() => {
  return currentItemIndex.value === menuItems.value.length - 1
})

const getMoodAnswer = (itemId: number | undefined, moodId: string): number | null => {
  if (!itemId || !answers.value[itemId] || !answers.value[itemId].moods) return null
  return answers.value[itemId].moods?.[moodId] ?? null
}

const setMoodAnswer = (itemId: number | undefined, moodId: string, rating: number) => {
  if (!itemId) return
  if (!answers.value[itemId]) {
    answers.value[itemId] = { moods: {}, weather: {} }
  }
  if (!answers.value[itemId].moods) {
    answers.value[itemId].moods = {}
  }
  answers.value[itemId].moods[moodId] = rating
}

const getWeatherAnswer = (itemId: number | undefined, weatherId: string): number | null => {
  if (!itemId || !answers.value[itemId] || !answers.value[itemId].weather) return null
  return answers.value[itemId].weather?.[weatherId] ?? null
}

const setWeatherAnswer = (itemId: number | undefined, weatherId: string, rating: number) => {
  if (!itemId) return
  if (!answers.value[itemId]) {
    answers.value[itemId] = { moods: {}, weather: {} }
  }
  if (!answers.value[itemId].weather) {
    answers.value[itemId].weather = {}
  }
  answers.value[itemId].weather[weatherId] = rating
}

const isMoodComplete = (itemId: number | undefined): boolean => {
  if (!itemId || !answers.value[itemId] || !answers.value[itemId].moods) return false
  const itemMoods = answers.value[itemId].moods!
  return moodRows.every(
    (row) => itemMoods[row.id] !== undefined && itemMoods[row.id] !== null,
  )
}

const isWeatherComplete = (itemId: number | undefined): boolean => {
  if (!itemId || !answers.value[itemId] || !answers.value[itemId].weather) return false
  const itemWeather = answers.value[itemId].weather!
  return weatherRows.every(
    (row) => itemWeather[row.id] !== undefined && itemWeather[row.id] !== null,
  )
}

const isCurrentItemComplete = computed(() => {
  if (!currentItem.value) return false
  const itemId = currentItem.value.id
  return isMoodComplete(itemId) && isWeatherComplete(itemId)
})

const completedItemsCount = computed(() => {
  let count = 0
  menuItems.value.forEach((item) => {
    if (isMoodComplete(item.id) && isWeatherComplete(item.id)) {
      count++
    }
  })
  return count
})

const answeredItems = computed(() => {
  return menuItems.value.filter((item) => {
    const itemAns = answers.value[item.id]
    if (!itemAns) return false
    const hasMood = itemAns.moods && Object.keys(itemAns.moods).length > 0
    const hasWeather = itemAns.weather && Object.keys(itemAns.weather).length > 0
    return Boolean(hasMood || hasWeather)
  })
})

const getScaleLabel = (val: number): string => {
  const match = ratingLevels.find((s) => s.value === val)
  return match ? match.label : ''
}

// --- Methods ---

const shuffleArray = <T>(items: T[]) => {
  const array = [...items]
  for (let i = array.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[array[i], array[j]] = [array[j], array[i]]
  }
  return array
}

const fetchMenuItems = async () => {
  try {
    const response = await axios.get('/menu-items')
    const allItems = shuffleArray(response.data ?? [])
    menuItems.value = allItems.slice(0, SURVEY_ITEM_LIMIT)
  } catch (error) {
    console.error('Error fetching menu items:', error)
  }
}

const checkSurveyLimit = async () => {
  try {
    const response = await axios.get('/api/stats/survey-status')
    if (response.data.isFull) {
      showLimitModal.value = true
    }
  } catch (error) {
    console.error('Failed to check survey status:', error)
  }
}

const nextItem = () => {
  if (currentItemIndex.value < menuItems.value.length - 1) {
    currentItemIndex.value++
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

const prevItem = () => {
  if (currentItemIndex.value > 0) {
    currentItemIndex.value--
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

const getMoodQuestionId = (): number => {
  const match = questions.value.find((q: any) => {
    const txt = (q.text || '').toLowerCase()
    return txt.includes('mood') || txt.includes('emotion')
  })
  if (match) return match.id
  const nonDemo = questions.value.find((q: any) => {
    const txt = (q.text || '').toLowerCase()
    return !txt.includes('age group') && !txt.includes('dine')
  })
  return nonDemo?.id ?? questions.value[0]?.id ?? 1
}

const getWeatherQuestionId = (): number => {
  const match = questions.value.find((q: any) => {
    const txt = (q.text || '').toLowerCase()
    return txt.includes('weather')
  })
  if (match) return match.id
  const nonDemo = questions.value.filter((q: any) => {
    const txt = (q.text || '').toLowerCase()
    return !txt.includes('age group') && !txt.includes('dine')
  })
  return nonDemo[1]?.id ?? nonDemo[0]?.id ?? questions.value[1]?.id ?? questions.value[0]?.id ?? 2
}

const resetSurvey = () => {
  showSuccessModal.value = false
  showConfirmModal.value = false
  showReviewModal.value = false
  showLimitModal.value = false
  showInstructionsModal.value = false
  hasStarted.value = false
  currentSection.value = 1
  currentItemIndex.value = 0
  demographicAnswers.value = { ageGroup: '', diningFrequency: '' }
  answers.value = {}
  honeypotField.value = ''
  // Generate a brand-new session ID so this restart is a completely independent session
  sessionId.value = crypto.randomUUID()
  clearStaleDrafts()
}

const executeFinalSubmit = async () => {
  try {
    if (questions.value.length === 0) {
      await fetchQuestions()
    }

    const payload: any[] = []
    const moodQId = getMoodQuestionId()
    const weatherQId = getWeatherQuestionId()

    menuItems.value.forEach((item) => {
      const itemAnswers = answers.value[item.id]
      if (itemAnswers) {
        if (itemAnswers.moods) {
          moodRows.forEach((row) => {
            const val = itemAnswers.moods?.[row.id]
            if (val !== undefined && val !== null) {
              payload.push({
                menuItemId: item.id,
                questionId: moodQId,
                selectedOptionId: null,
                textResponse: `${row.label}: ${val} (${getScaleLabel(val)})`.slice(
                  0,
                  SURVEY_TEXT_MAX_LENGTH,
                ),
              })
            }
          })
        }

        if (itemAnswers.weather) {
          weatherRows.forEach((row) => {
            const val = itemAnswers.weather?.[row.id]
            if (val !== undefined && val !== null) {
              payload.push({
                menuItemId: item.id,
                questionId: weatherQId,
                selectedOptionId: null,
                textResponse: `${row.label}: ${val} (${getScaleLabel(val)})`.slice(
                  0,
                  SURVEY_TEXT_MAX_LENGTH,
                ),
              })
            }
          })
        }
      }
    })

    if (payload.length === 0) return

    const finalSubmission = {
      answers: payload,
      phoneNumber: honeypotField.value,
      ageGroup: demographicAnswers.value.ageGroup || null,
      diningFrequency: demographicAnswers.value.diningFrequency || null,
      sessionId: sessionId.value,
    }

    await axios.post('/submit-category', finalSubmission)

    clearStaleDrafts()

    showConfirmModal.value = false
    showReviewModal.value = false
    showSuccessModal.value = true
  } catch (error: any) {
    const errorData = error.response?.data
    const errorMessage = errorData ? (typeof errorData === 'string' ? errorData : JSON.stringify(errorData)) : ''
    if (errorMessage.includes('LIMIT_REACHED')) {
      showConfirmModal.value = false
      showReviewModal.value = false
      showLimitModal.value = true
    } else {
      console.error('Error saving data:', error)
      const detail = errorData?.error || errorData?.message || error.message || ''
      alert(`Oops! There was a problem saving your answers.${detail ? ' (' + detail + ')' : ''} Please try again.`)
    }
  }
}

// Legacy draft keys from when unfinished answers were persisted locally.
// They are no longer written, but we clear them so reopening the survey
// always starts with a fresh, empty session.
const DRAFT_KEY = 'foodPreferenceSurvey_draft'
const DEMO_DRAFT_KEY = 'foodPreferenceSurvey_demo_draft'
const LEGACY_DRAFT_KEYS = ['cafeRater_survey_draft', 'cafeRater_survey_demo_draft']

const clearStaleDrafts = () => {
  try {
    localStorage.removeItem(DRAFT_KEY)
    localStorage.removeItem(DEMO_DRAFT_KEY)
    LEGACY_DRAFT_KEYS.forEach((key) => localStorage.removeItem(key))
  } catch {
    // Storage may be unavailable (e.g. private mode) — survey still starts fresh
    // because in-memory state is always initialized empty.
  }
}

const resetSessionState = () => {
  hasStarted.value = false
  currentSection.value = 1
  showInstructionsModal.value = false
  currentItemIndex.value = 0
  demographicAnswers.value = { ageGroup: '', diningFrequency: '' }
  answers.value = {}
  honeypotField.value = ''
  showLimitModal.value = false
  showConfirmModal.value = false
  showSuccessModal.value = false
  showReviewModal.value = false
  // Always start with a fresh session ID so each visit is independent
  sessionId.value = crypto.randomUUID()
}

onMounted(() => {
  // Every open starts a new session: discard any unfinished answers left
  // behind by a previous visit instead of restoring them.
  resetSessionState()
  clearStaleDrafts()

  checkSurveyLimit()
  fetchMenuItems()
  fetchQuestions()
})
</script>

<style scoped>
/* GLOBALS */
.survey-layout {
  background-color: #f8fafc;
  min-height: 100vh;
  font-family:
    'Inter',
    -apple-system,
    sans-serif;
  color: #1e293b;
  padding-bottom: 50px;
}

/* SHARED BUTTONS */
.primary-btn {
  background: #f97316;
  color: white;
  border: none;
  padding: 16px 40px;
  border-radius: 12px;
  font-size: 1.1rem;
  font-weight: 700;
  cursor: pointer;
  width: 100%;
  transition:
    transform 0.2s,
    background 0.2s;
}
.primary-btn:hover {
  background: #ea580c;
  transform: translateY(-2px);
}

/* WELCOME SCREEN */
.welcome-screen {
  position: fixed;
  inset: 0;
  /* UPDATE: Removed hardcoded Unsplash URL */
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}
.welcome-screen {
  position: fixed;
  inset: 0;
  background-image: url('https://images.unsplash.com/photo-1554118811-1e0d58224f24?auto=format&fit=crop&q=80&w=2000');
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}
.welcome-card {
  position: relative;
  background: white;
  padding: 50px 40px;
  border-radius: 24px;
  text-align: center;
  max-width: 500px;
  width: 90%;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
  animation: slideUp 0.6s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}
.welcome-icon {
  background: #f97316;
  color: white;
  width: 70px;
  height: 70px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2.5rem;
  margin: 0 auto 20px auto;
  box-shadow: 0 10px 15px -3px rgba(249, 115, 22, 0.3);
}
.welcome-card h1 {
  margin: 0 0 15px 0;
  color: #0f172a;
  font-size: 2.2rem;
  font-weight: 800;
}
.welcome-card p {
  color: #475569;
  line-height: 1.6;
  margin-bottom: 35px;
  font-size: 1.1rem;
}
@keyframes slideUp {
  0% {
    transform: translateY(40px);
    opacity: 0;
  }
  100% {
    transform: translateY(0);
    opacity: 1;
  }
}

/* HEADER */
.top-nav {
  background: white;
  border-bottom: 1px solid #e2e8f0;
  position: sticky;
  top: 0;
  z-index: 50;
}
.nav-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 15px 30px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.logo {
  display: flex;
  align-items: center;
  gap: 10px;
}
.logo-icon {
  background: #f97316;
  color: white;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1rem;
}
.logo h1 {
  margin: 0;
  font-size: 1.2rem;
  color: #0f172a;
  font-weight: 700;
}

/* HEADER ACTIONS */
.header-actions {
  display: flex;
  align-items: center;
  gap: 20px;
}
.global-progress {
  font-size: 0.95rem;
  color: #64748b;
}
.global-progress strong {
  color: #0f172a;
}
.header-finish-btn {
  background: #10b981;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  font-weight: 700;
  font-size: 0.9rem;
  cursor: pointer;
  transition: background 0.2s;
}
.header-finish-btn:hover {
  background: #059669;
}
.pulse-light {
  animation: pulseLight 2s infinite;
}
@keyframes pulseLight {
  0% {
    box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.4);
  }
  70% {
    box-shadow: 0 0 0 8px rgba(16, 185, 129, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(16, 185, 129, 0);
  }
}

.main-content {
  max-width: 1200px;
  margin: 40px auto;
  padding: 0 30px;
}
.rating-view {
  display: grid;
  grid-template-columns: 350px 1fr;
  gap: 40px;
  align-items: start;
}
.left-pane {
  position: sticky;
  top: 140px;
}
.sticky-card {
  background: white;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  overflow: hidden;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.03);
}
.pane-header {
  padding: 12px 20px;
  min-height: 52px;
  box-sizing: border-box;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  font-size: 0.85rem;
  line-height: 1.4;
}
.breadcrumb {
  color: #64748b;
  font-weight: 500;
}
.text-dark {
  color: #0f172a;
  font-weight: 600;
}
.cat-progress {
  color: #64748b;
  font-weight: 500;
}
.green-text {
  color: #16a34a;
  font-weight: 700;
}
.item-cover {
  position: relative;
}
.cover-img {
  height: 250px;
  background-size: cover;
  background-position: center;
  /* UPDATE: Added gray fallback color */
  background-color: #e2e8f0;
}
.cover-img::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.8), transparent 70%);
}
.cover-info {
  position: absolute;
  bottom: 20px;
  left: 20px;
  right: 20px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}
.cover-info h3 {
  margin: 0;
  color: white;
  font-size: 1.4rem;
  font-weight: 700;
  line-height: 1.2;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.5);
}
.badge {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 700;
  display: inline-block;
  border: 1px solid transparent;
}

.questions-list {
  display: flex;
  flex-direction: column;
  gap: 25px;
}
.question-card {
  background: white;
  padding: 30px;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.02);
}
.q-header {
  display: flex;
  align-items: flex-start;
  gap: 15px;
  margin-bottom: 25px;
}
.q-header > div:last-child {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}
.q-bubble {
  width: 28px;
  height: 28px;
  background: #ffedd5;
  color: #ea580c;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 0.9rem;
  flex-shrink: 0;
  transition: all 0.3s;
}
.q-bubble.answered {
  background: #22c55e;
  color: white;
}
.q-header h4 {
  margin: 0;
  font-size: 1.15rem;
  color: #0f172a;
  line-height: 1.4;
  padding-top: 0;
}

.opt-btn-vertical,
.opt-btn-grid {
  position: relative;
  background: #f8fafc;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  text-align: left;
}
.opt-btn-vertical:hover,
.opt-btn-grid:hover {
  border-color: #cbd5e1;
  background: #f1f5f9;
}
.opt-btn-vertical.selected,
.opt-btn-grid.selected {
  border-color: #f97316;
  background: #fff7ed;
}

.vertical-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.opt-btn-vertical {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px 20px;
}
.opt-icon {
  font-size: 1.5rem;
}
.opt-label {
  font-size: 1rem;
  font-weight: 600;
  color: #334155;
}

.grid-options {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 15px;
}
.opt-btn-grid {
  padding: 20px 15px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  gap: 8px;
  min-height: 110px;
}
.opt-icon-large {
  font-size: 2rem;
}
.opt-label-main {
  width: 100%;
  text-align: center;
  font-weight: 700;
  color: #0f172a;
  font-size: 1.05rem;
}
.opt-sub {
  width: 100%;
  text-align: center;
  font-size: 0.8rem;
  color: #64748b;
  font-weight: 500;
}

.text-input-wrapper {
  position: relative;
}
.styled-textarea {
  box-sizing: border-box;
  width: 100%;
  height: 120px;
  padding: 20px;
  border-radius: 12px;
  border: 2px solid #e2e8f0;
  background: #f8fafc;
  font-family: inherit;
  font-size: 1rem;
  color: #334155;
  resize: none;
  transition: all 0.2s;
  outline: none;
}
.styled-textarea:focus {
  border-color: #f97316;
  background: white;
}
.styled-textarea.has-content {
  border-color: #f97316;
  background: #fff7ed;
}
.char-count {
  position: absolute;
  bottom: 35px;
  right: 15px;
  font-size: 0.8rem;
  color: #94a3b8;
  font-weight: 500;
}
.helper-text {
  margin: 10px 0 0 0;
  font-size: 0.85rem;
  color: #64748b;
  text-align: center;
}

.action-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
  padding-top: 20px;
  border-top: 1px solid #e2e8f0;
}
.nav-btn {
  padding: 14px 24px;
  border-radius: 10px;
  font-weight: 700;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}
.nav-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.nav-btn.secondary {
  background: white;
  border: 1px solid #cbd5e1;
  color: #475569;
}
.nav-btn.secondary:hover:not(:disabled) {
  background: #f1f5f9;
}
.nav-btn.primary {
  background: #f97316;
  color: white;
  margin-left: auto;
}
.nav-btn.primary:hover {
  background: #ea580c;
}
.nav-btn.success {
  background: #22c55e;
  color: white;
  margin-left: auto;
}
.nav-btn.success:hover {
  background: #16a34a;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(5px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}
.modal-card {
  background: white;
  padding: 40px;
  border-radius: 24px;
  text-align: center;
  max-width: 450px;
  width: 90%;
  margin: auto;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  animation: popIn 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275) forwards;
}
.modal-icon {
  font-size: 4.5rem;
  margin-bottom: 15px;
}
.modal-card h2 {
  margin: 0 0 15px 0;
  color: #0f172a;
  font-size: 1.8rem;
  font-weight: 800;
}
.modal-card p {
  color: #64748b;
  line-height: 1.6;
  margin-bottom: 30px;
  font-size: 1.05rem;
}
.modal-actions {
  display: flex;
  gap: 15px;
  justify-content: center;
  margin-top: 10px;
}
.modal-actions button {
  flex: 1;
  margin: 0;
}
@keyframes popIn {
  0% {
    transform: scale(0.8);
    opacity: 0;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

.pulse {
  animation: pulse 2s infinite;
}
@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(249, 115, 22, 0.4);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(249, 115, 22, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(249, 115, 22, 0);
  }
}

@media (max-width: 850px) {
  .rating-view {
    grid-template-columns: 1fr;
    gap: 20px;
  }
  .left-pane {
    position: relative;
    top: 0;
  }
  .cover-img {
    height: 180px;
  }
  .grid-options {
    grid-template-columns: 1fr;
  }
  .main-content {
    padding: 0 15px;
  }
  .action-footer {
    flex-direction: column;
    gap: 15px;
  }
  .nav-btn.primary,
  .nav-btn.success {
    margin-left: 0;
    width: 100%;
  }
  .header-actions {
    flex-direction: column;
    gap: 5px;
    align-items: flex-end;
  }
}

/* --- NEW: REVIEW MODAL STYLES --- */
.review-card {
  max-width: 700px !important;
  width: 95%;
  padding: 0 !important;
  display: flex;
  flex-direction: column;
  max-height: 90vh;
  overflow: hidden;
  text-align: left;
}
.review-header {
  padding: 30px 40px 20px 40px;
  border-bottom: 1px solid #e2e8f0;
  background: white;
  z-index: 10;
}
.review-header h2 {
  margin: 0 0 5px 0;
  font-size: 1.6rem;
  color: #0f172a;
  font-weight: 800;
  text-align: left;
}
.review-header p {
  margin: 0;
  color: #64748b;
  font-size: 0.95rem;
}

.review-scroll-area {
  flex-grow: 1;
  overflow-y: auto;
  padding: 25px 40px;
  background: #f8fafc;
}
.review-scroll-area::-webkit-scrollbar {
  width: 8px;
}
.review-scroll-area::-webkit-scrollbar-thumb {
  background-color: #cbd5e1;
  border-radius: 4px;
}

.review-item-block {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  margin-bottom: 20px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.02);
}
.review-item-header {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px 20px;
  background: #f1f5f9;
  border-bottom: 1px solid #e2e8f0;
}
.review-item-number {
  background: #0f172a;
  color: white;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.85rem;
  font-weight: 700;
  flex-shrink: 0;
}
.review-item-titles {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.review-item-titles h3 {
  margin: 0;
  font-size: 1.1rem;
  color: #0f172a;
  font-weight: 700;
}

.review-q-list {
  padding: 15px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.review-q-row {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: flex-start;
  padding-bottom: 12px;
  border-bottom: 1px dashed #f1f5f9;
}
.review-q-row:last-child {
  border-bottom: none;
  padding-bottom: 0;
}
.r-question {
  width: 50%;
  font-size: 0.85rem;
  color: #64748b;
  font-weight: 500;
  line-height: 1.4;
}
.r-answer {
  width: 50%;
  font-size: 0.9rem;
  color: #0f172a;
  font-weight: 600;
  text-align: right;
}
.text-missing {
  color: #ef4444;
  font-style: italic;
  font-weight: 500;
}

.review-footer {
  padding: 20px 40px;
  border-top: 1px solid #e2e8f0;
  background: white;
  display: flex;
  justify-content: space-between;
  gap: 15px;
  z-index: 10;
}
.review-footer button {
  flex: 1;
  margin: 0;
}

@media (max-width: 600px) {
  .review-header,
  .review-scroll-area,
  .review-footer {
    padding: 20px;
  }
  .review-q-row {
    flex-direction: column;
    gap: 4px;
    border-bottom: none;
    background: #f8fafc;
    padding: 10px;
    border-radius: 8px;
  }
  .r-question,
  .r-answer {
    width: 100%;
    text-align: left;
  }
}

/* --- CATEGORY BADGE COLORS --- */
.pill-default {
  background: #f1f5f9;
  border-color: #e2e8f0;
  color: #475569;
}
.pill-appetizer {
  background: #fff7ed;
  border-color: #fed7aa;
  color: #9a3412;
}
.pill-pasta {
  background: #fefce8;
  border-color: #fde68a;
  color: #854d0e;
}
.pill-sandwich {
  background: #fdf5e6;
  border-color: #ebd5b3;
  color: #7c2d12;
}
.pill-wings {
  background: #fef2f2;
  border-color: #fecaca;
  color: #991b1b;
}
.pill-ricemeal {
  background: #fffbeb;
  border-color: #fde68a;
  color: #92400e;
}
.pill-classics {
  background: #f5f3ff;
  border-color: #ddd6fe;
  color: #5b21b6;
}
.pill-iceblended {
  background: #ecfeff;
  border-color: #a5f3fc;
  color: #155e75;
}
.pill-specialty {
  background: #fdf2f8;
  border-color: #fbcfe8;
  color: #9d174d;
}
.pill-noncoffee {
  background: #f0f9ff;
  border-color: #bae6fd;
  color: #0c4a6e;
}
.pill-refresher {
  background: #ecfdf5;
  border-color: #a7f3d0;
  color: #065f46;
}
.pill-matcha {
  background: #f0fdf4;
  border-color: #bbf7d0;
  color: #14532d;
}

.disclaimer-box {
  background: #f1f5f9;
  border-radius: 8px;
  padding: 12px 15px;
  margin-bottom: 30px;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  text-align: left;
  border-left: 4px solid #f97316;
}
.disclaimer-box .info-icon {
  font-size: 1.2rem;
  line-height: 1;
}
.disclaimer-box p {
  margin: 0 !important;
  font-size: 0.85rem !important;
  color: #475569 !important;
  line-height: 1.4 !important;
}
.image-disclaimer {
  padding: 10px 20px;
  background: #f8fafc;
  font-size: 0.75rem;
  color: #64748b;
  text-align: center;
  font-style: italic;
  border-top: 1px solid #e2e8f0;
}

.submit-btn:disabled,
.disabled-btn {
  background-color: #cbd5e1;
  color: #94a3b8;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}
@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.action-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 15px;
  margin-top: 20px;
}
.incomplete-warning {
  flex: 1;
  text-align: center;
  margin: 0;
  color: #ef4444;
  font-size: 0.85rem;
  font-style: italic;
  font-weight: 500;
  animation: fadeIn 0.3s ease-in-out;
}
.header-finish-btn:disabled {
  background-color: #cbd5e1 !important;
  color: #64748b !important;
  cursor: not-allowed;
  animation: none !important;
  box-shadow: none !important;
  opacity: 0.7;
}
.empty-review {
  text-align: center;
  padding: 40px 20px;
  color: #64748b;
  font-size: 1.05rem;
  font-style: italic;
}

/* 🧊 MAXIMUM TRANSLUCENCY GLASS EFFECT */
.welcome-card.glass-effect {
  /* Dropped the white tint down to just 5% (0.05) */
  background: rgba(255, 255, 255, 0.05) !important;

  /* Lowered the blur from 25px to 12px so you see more of the cafe behind it */
  backdrop-filter: blur(12px) saturate(120%);
  -webkit-backdrop-filter: blur(12px) saturate(120%);

  /* Keep the thin border so the glass still has an "edge" */
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.2);
}

/* Ensure the disclaimer box stands out with a solid white background */
.welcome-card.glass-effect .disclaimer-box {
  background: #ffffff; /* Solid white background */
  border: 1px solid #e2e8f0; /* A soft gray border to define its edges */
  color: #334155; /* Ensures the text stays dark and readable */
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05); /* Optional: A tiny shadow to lift it slightly */
}

/* 📝 BOOST TEXT READABILITY ON GLASS */
.welcome-card.glass-effect h1 {
  color: #ffffff;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.3); /* Soft shadow lifts it off the background */
}

.welcome-card.glass-effect > p {
  color: rgba(255, 255, 255, 0.95); /* Bright white with a tiny bit of softness */
  font-weight: 500; /* Making the font slightly thicker helps against busy backgrounds */
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.4);
}
/* SECTION 1 — DEMOGRAPHIC VIEW */
.demographic-view {
  max-width: 780px;
  margin: 0 auto;
}
.demographic-card {
  background: white;
  border-radius: 24px;
  padding: 35px 40px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.04);
}
.section-banner {
  text-align: center;
  margin-bottom: 35px;
  padding-bottom: 25px;
  border-bottom: 1px solid #f1f5f9;
}
.section-scope-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 14px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  color: #1d4ed8;
  border-radius: 20px;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  margin-bottom: 12px;
}
.section-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  display: inline-block;
}
.blue-dot {
  background-color: #3b82f6;
}
.orange-dot {
  background-color: #f97316;
}
.section-banner h2 {
  font-size: 1.6rem;
  color: #0f172a;
  margin: 0 0 8px 0;
  font-weight: 800;
}
.section-desc {
  color: #64748b;
  font-size: 0.95rem;
  margin: 0;
}
.demographic-questions {
  display: flex;
  flex-direction: column;
  gap: 25px;
}
.demo-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 24px;
  border-top: 4px solid #3b82f6 !important;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.02);
}
.demo-bubble {
  background: #eff6ff !important;
  color: #2563eb !important;
  border: 1px solid #bfdbfe !important;
}
.demo-bubble.answered {
  background: #10b981 !important;
  color: white !important;
  border-color: #10b981 !important;
}
.q-sub-badge {
  display: inline-block;
  font-size: 0.75rem;
  font-weight: 600;
  color: #64748b;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 6px;
  margin-top: 4px;
}
.demo-opt-btn {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #fafbfc;
  cursor: pointer;
  transition: all 0.2s;
  width: 100%;
  text-align: left;
  font-size: 0.95rem;
  color: #334155;
}
.demo-opt-btn:hover {
  border-color: #60a5fa;
  background: #f0f7ff;
}
.demo-opt-btn.selected {
  border-color: #3b82f6 !important;
  background: #eff6ff !important;
  color: #1d4ed8 !important;
  font-weight: 600;
}
.custom-radio-circle {
  width: 18px;
  height: 18px;
  box-sizing: border-box;
  border-radius: 50%;
  border: 2px solid #cbd5e1;
  display: inline-block;
  flex-shrink: 0;
  position: relative;
  transition: all 0.2s;
}
.custom-radio-circle.active {
  border-color: #3b82f6 !important;
}
.custom-radio-circle.active::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #3b82f6 !important;
  transform: translate(-50%, -50%);
}
.demographic-footer {
  margin-top: 30px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}
.demo-proceed-btn {
  padding: 14px 28px;
  font-size: 1rem;
  font-weight: 700;
  border-radius: 10px;
  width: 100%;
  max-width: 420px;
  background: #3b82f6 !important;
}
.demo-proceed-btn:hover:not(:disabled) {
  background: #2563eb !important;
}
.complete-info {
  color: #16a34a;
  font-weight: 600;
  font-size: 0.9rem;
  margin: 0;
}
.section-switch-header {
  padding: 12px 20px;
  min-height: 52px;
  box-sizing: border-box;
  background: #f1f5f9;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  line-height: 1.4;
}
.back-to-sec1-btn {
  background: none;
  border: none;
  color: #2563eb;
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
  display: flex;
  align-items: center;
  gap: 4px;
}
.back-to-sec1-btn:hover {
  text-decoration: underline;
}
.view-instructions-btn {
  background: none;
  border: none;
  color: #ea580c;
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
  display: flex;
  align-items: center;
  gap: 4px;
}
.view-instructions-btn:hover {
  text-decoration: underline;
  color: #c2410c;
}
.section-indicator-badge {
  display: inline-block;
  font-size: 0.75rem;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 6px;
  margin-right: 6px;
}
.section-indicator-badge.blue-badge {
  background: #eff6ff;
  color: #1d4ed8;
  border: 1px solid #bfdbfe;
}
.section-indicator-badge.orange-badge {
  background: #fff7ed;
  color: #c2410c;
  border: 1px solid #fed7aa;
}
.section-badge-label {
  color: #475569;
  font-weight: 600;
}
.review-section-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 14px 18px;
}
.review-section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  font-size: 0.95rem;
  color: #0f172a;
}
.mt-3 {
  margin-top: 1rem;
}
.mb-2 {
  margin-bottom: 0.5rem;
}

/* SECTION 2 — INSTRUCTIONS MODAL (atop Section 2 page) */
.instructions-modal-card {
  max-width: 640px !important;
  width: 95%;
  padding: 0 !important;
  display: flex;
  flex-direction: column;
  max-height: 90vh;
  overflow: hidden;
  text-align: left;
  border-top: 4px solid #f97316;
}
.instructions-modal-banner {
  padding: 28px 36px 20px 36px;
  margin-bottom: 0;
}
.instructions-modal-box {
  margin: 20px 36px 0 36px;
  max-height: 50vh;
  overflow-y: auto;
}
.instructions-modal-actions {
  padding: 20px 36px 28px 36px;
}
.instructions-modal-actions .nav-btn {
  flex: 1;
  margin: 0;
  text-align: center;
}
.orange-scope-pill {
  background: #fff7ed !important;
  border-color: #fed7aa !important;
  color: #c2410c !important;
}
.starter-instructions-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 26px 30px;
  margin-bottom: 25px;
}
.instructions-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}
.instructions-icon {
  font-size: 1.4rem;
}
.instructions-header h3 {
  margin: 0;
  font-size: 1.15rem;
  color: #0f172a;
  font-weight: 700;
  line-height: 1.4;
}
.instructions-body {
  margin-bottom: 24px;
}
.instruction-main {
  margin: 0 0 8px 0;
  font-size: 1rem;
  color: #334155;
  font-weight: 500;
  line-height: 1.6;
}
.instruction-sub {
  margin: 0;
  font-size: 1rem;
  font-weight: 400;
  color: #475569;
  line-height: 1.6;
}
.rating-scale-box {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 20px 24px;
}
.scale-heading {
  margin: 0 0 14px 0;
  font-size: 0.8rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #64748b;
}
.scale-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.scale-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid #f1f5f9;
  transition: all 0.2s;
}
.scale-item:hover {
  background: #fff7ed;
  border-color: #fed7aa;
}
.scale-badge {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 0.9rem;
  flex-shrink: 0;
}
.scale-item-1 .scale-badge {
  background: #fee2e2;
  color: #b91c1c;
}
.scale-item-2 .scale-badge {
  background: #ffedd5;
  color: #c2410c;
}
.scale-item-3 .scale-badge {
  background: #fef3c7;
  color: #b45309;
}
.scale-item-4 .scale-badge {
  background: #ecfdf5;
  color: #047857;
}
.scale-item-5 .scale-badge {
  background: #dcfce7;
  color: #15803d;
}
.scale-separator {
  color: #94a3b8;
  font-weight: 600;
}
.scale-label {
  font-size: 1rem;
  font-weight: 600;
  color: #334155;
}
.starter-proceed-btn {
  padding: 14px 28px;
  font-size: 1rem;
  font-weight: 700;
  border-radius: 10px;
  min-width: 200px;
}

@media (max-width: 600px) {
  .instructions-modal-banner {
    padding: 22px 20px 16px 20px;
  }
  .instructions-modal-box {
    margin: 16px 20px 0 20px;
  }
  .instructions-modal-actions {
    padding: 16px 20px 22px 20px;
    flex-direction: column-reverse;
  }
  .starter-instructions-box {
    padding: 18px 16px;
  }
  .rating-scale-box {
    padding: 14px 16px;
  }
}

/* ========================================================= */
/* SECTION 2 — MENU ITEM EVALUATION & MATRIX TABLE STYLES   */
/* ========================================================= */
.item-tag-pill {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  background: #fff7ed;
  border: 1px solid #fed7aa;
  color: #c2410c;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 800;
  letter-spacing: 0.05em;
}

.menu-item-sub-tag {
  display: inline-block;
  font-size: 0.75rem;
  font-weight: 800;
  color: #fdba74;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  margin-bottom: 0;
  line-height: 1.4;
}

.item-desc-panel {
  padding: 16px 20px;
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}

.desc-tag {
  display: block;
  font-size: 0.78rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #64748b;
  margin-bottom: 0;
  line-height: 1.4;
}

.desc-text {
  margin: 0;
  font-size: 0.95rem;
  line-height: 1.5;
  color: #334155;
}

.grid-question-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 24px;
  border-top: 4px solid #f97316 !important;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.02);
}

.grid-bubble {
  background: #fff7ed !important;
  color: #ea580c !important;
  border: 1px solid #fed7aa !important;
}

.grid-bubble.answered {
  background: #10b981 !important;
  color: white !important;
  border-color: #10b981 !important;
}

.grid-prompt-text {
  font-size: 1rem;
  font-weight: 400;
  color: #334155;
  margin: 0 0 18px 0;
  line-height: 1.6;
}
.grid-prompt-text strong,
.instruction-main strong {
  color: #0f172a;
  font-weight: 600;
}

.matrix-wrapper {
  width: 100%;
  overflow-x: auto;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  background: white;
  margin-bottom: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
  -webkit-overflow-scrolling: touch;
}

.matrix-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  text-align: center;
  font-size: 0.95rem;
}

.matrix-table thead {
  background: #fafbfc;
  border-bottom: 1px solid #e2e8f0;
}

.matrix-table th {
  padding: 12px 10px;
  font-weight: 600;
  color: #475569;
  vertical-align: middle;
}

.col-corner {
  text-align: left;
  padding-left: 18px !important;
  font-size: 0.78rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #64748b;
  min-width: 220px;
}

.col-scale {
  width: 72px;
  min-width: 64px;
  text-align: center;
  vertical-align: middle;
  padding: 10px 6px;
}

.scale-header-num {
  display: block;
  text-align: center;
  font-size: 1rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0 auto 2px auto;
  line-height: 1.4;
}

.scale-header-hint {
  display: block;
  text-align: center;
  font-size: 0.75rem;
  font-weight: 600;
  color: #64748b;
  line-height: 1.3;
  margin: 0 auto;
}

.matrix-tr {
  border-bottom: 1px solid #f1f5f9;
  transition: all 0.2s ease;
}

.matrix-tr:last-child {
  border-bottom: none;
}

.matrix-tr:hover {
  background-color: #fffaf5;
}

.matrix-tr.row-answered {
  background-color: #fff7ed;
}

.matrix-row-title {
  text-align: left;
  vertical-align: middle;
  padding: 14px 18px;
  font-size: 0.95rem;
  font-weight: 500;
  color: #334155;
  line-height: 1.45;
  transition: color 0.2s ease;
}

.matrix-tr.row-answered .matrix-row-title {
  color: #c2410c;
  font-weight: 600;
}

.row-text {
  display: block;
}

.matrix-td {
  padding: 10px 4px;
  text-align: center;
  vertical-align: middle;
  cursor: pointer;
}

.matrix-td:hover .grid-radio-circle {
  border-color: #f97316;
  transform: scale(1.1);
}

.grid-radio-circle {
  width: 18px;
  height: 18px;
  box-sizing: border-box;
  border-radius: 50%;
  border: 2px solid #cbd5e1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  vertical-align: middle;
  margin: 0 auto;
  position: relative;
  transition: all 0.2s ease-in-out;
  background: white;
}

.grid-radio-circle.active {
  border-color: #f97316;
  background: white;
}

.grid-radio-circle.active::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f97316;
  transform: translate(-50%, -50%);
}

.grid-req-footer {
  margin-top: 10px;
  font-size: 0.85rem;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 4px;
}

.req-asterisk {
  color: #ef4444;
  font-weight: 800;
}

/* SECTION 2 — REVIEW MODAL LIST */
.review-grid-group {
  background: #ffffff;
  border: 1px solid #f1f5f9;
  border-radius: 10px;
  padding: 12px 16px;
  margin-bottom: 12px;
}

.review-grid-group:last-child {
  margin-bottom: 0;
}

.review-grid-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.9rem;
  color: #0f172a;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f1f5f9;
}

.review-q-tag {
  background: #fff7ed;
  color: #c2410c;
  border: 1px solid #fed7aa;
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 0.72rem;
  font-weight: 700;
}

.review-grid-rows {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.review-grid-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.85rem;
  padding: 4px 0;
  border-bottom: 1px dashed #f8fafc;
}

.review-grid-row:last-child {
  border-bottom: none;
}

.review-row-name {
  color: #475569;
  max-width: 65%;
  line-height: 1.35;
}

.review-row-val {
  font-weight: 700;
  color: #0f172a;
  text-align: right;
}

.text-missing {
  color: #ef4444 !important;
  font-weight: 500 !important;
  font-style: italic;
}
</style>
