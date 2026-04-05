<template>
  <div class="survey-layout">

    <div v-if="!hasStarted" class="welcome-screen">
      <div class="welcome-card">
        <div class="welcome-icon">🍴</div>
        <h1>Welcome to CaféRater!</h1>
        <p>Help us build a smarter AI by rating our menu items. Your feedback directly shapes the future of our cafe!</p>
        <div class="disclaimer-box">
          <span class="info-icon">⚠️</span>
          <p><strong>DISCLAIMER:</strong> The photos used in this survey are from the internet for illustrative purposes only. Actual cafe servings and presentation may vary!</p>
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
            <h1>CaféRater</h1>
          </div>

          <div class="header-actions">
            <div class="global-progress">
              Item <strong>{{ currentItemIndex + 1 }}</strong> of {{ menuItems.length }}
            </div>
            <button class="header-finish-btn pulse-light" @click="showConfirmModal = true">
              I'm Done 🏁
            </button>
          </div>
        </div>
      </header>

      <main class="main-content">
        <div class="rating-view">

          <div class="left-pane">
            <div class="sticky-card">
              <div class="pane-header">
                <span class="breadcrumb">{{ currentItem?.category }} — <span class="text-dark">{{ currentItem?.name }}</span></span>
                <span class="cat-progress"><strong class="green-text">{{ completedItemsCount }}</strong>/{{ menuItems.length }} done</span>
              </div>

              <div class="item-cover">
                <div
                  class="cover-img"
                  :style="currentItem?.imageName ? { backgroundImage: `url('${getImagePath(currentItem)}')` } : {}"
                ></div>
                <div class="cover-info">
                  <h3>{{ currentItem?.name }}</h3>
                  <span class="badge" :class="getPillClass(currentItem?.category)">🍴 {{ currentItem?.category }}</span>
                </div>
              </div>

              <div class="image-disclaimer">
                * Image is for illustration purposes only.
              </div>
            </div>
          </div>

          <div class="right-pane">
            <div class="questions-list">

              <div v-for="(q, index) in questions" :key="q.id" class="question-card">
                <div class="q-header">
                  <div class="q-bubble" :class="{ 'answered': isAnswered(currentItem?.id, q.id) }">
                    <span v-if="isAnswered(currentItem?.id, q.id)">✓</span>
                    <span v-else>{{ index + 1 }}</span>
                  </div>
                  <h4>{{ q.text }}</h4>
                </div>

                <div v-if="q.type === 'vertical-radio'" class="vertical-options">
                  <button
                    v-for="opt in q.options"
                    :key="opt.id"
                    class="opt-btn-vertical"
                    :class="{ selected: getAnswer(currentItem?.id, q.id) === opt.id }"
                    @click="setAnswer(currentItem?.id, q.id, opt.id)"
                  >
                    <span class="opt-icon">{{ opt.icon }}</span>
                    <span class="opt-label">{{ opt.label }}</span>
                  </button>
                </div>

                <div v-if="q.type === 'grid-radio'" class="grid-options">
                  <button
                    v-for="opt in q.options"
                    :key="opt.id"
                    class="opt-btn-grid"
                    :class="{ selected: getAnswer(currentItem?.id, q.id) === opt.id }"
                    @click="setAnswer(currentItem?.id, q.id, opt.id)"
                  >
                    <span class="opt-icon-large" v-if="opt.icon">{{ opt.icon }}</span>
                    <span class="opt-label-main">{{ opt.label }}</span>
                    <span class="opt-sub" v-if="opt.sub">{{ opt.sub }}</span>
                  </button>
                </div>

                <div v-if="q.type === 'textarea'" class="text-input-wrapper">
                  <textarea
                    class="styled-textarea"
                    :class="{ 'has-content': getAnswer(currentItem?.id, q.id)?.length > 0 }"
                    placeholder="Describe this dish as if you're telling an AI what it tastes, looks, and feels like..."
                    :value="getAnswer(currentItem?.id, q.id) || ''"
                    @input="setTextAnswer(currentItem?.id, q.id, $event.target.value)"
                    maxlength="300"
                  ></textarea>
                  <div class="char-count">
                    {{ getAnswer(currentItem?.id, q.id)?.length || 0 }} / 300
                  </div>
                  <p class="helper-text">Your description helps train a smarter food recommendation AI.</p>
                </div>
              </div>

              <div class="action-footer">
                <button class="nav-btn secondary" @click="prevItem" :disabled="currentItemIndex === 0">
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
                  Save Survey Answers 💾
                </button>
              </div>

            </div>
          </div>

        </div>
      </main>
    </div>

    <Teleport to="body">
      <div v-if="showConfirmModal" class="modal-overlay">
        <div class="modal-card">
          <div class="modal-icon">👋</div>
          <h2>Ready to Finish?</h2>
          <p>Are you completely finished rating your items? If you have nothing else to review, click Confirm to complete your session!</p>
          <div class="modal-actions">
            <button class="nav-btn secondary" @click="showConfirmModal = false; showReviewModal = true">Review Answers</button>
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
            <p>Summary for your {{ answeredItems.length }} item{{ answeredItems.length === 1 ? '' : 's' }}.</p>
          </div>

          <div class="review-scroll-area">
            <div v-if="answeredItems.length === 0" class="empty-review">
              <p>You haven't rated any items yet!</p>
            </div>

            <div v-for="(item, index) in answeredItems" :key="item.id" class="review-item-block">
              <div class="review-item-header">
                <span class="review-item-number">{{ index + 1 }}</span>
                <div class="review-item-titles">
                  <h3>{{ item.name }}</h3>
                  <span class="badge" :class="getPillClass(item.category)">🍴 {{ item.category }}</span>
                </div>
              </div>

              <div class="review-q-list">
                <div v-for="q in questions" :key="q.id" class="review-q-row">
                  <div class="r-question">{{ q.shortText }}</div>
                  <div class="r-answer" :class="{ 'text-missing': !isAnswered(item.id, q.id) }">
                    {{ getAnswerText(item.id, q) }}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="review-footer">
            <button class="nav-btn secondary" @click="showReviewModal = false">Back to Survey</button>
            <button class="nav-btn primary" @click="executeFinalSubmit">Confirm & Submit Data 💾</button>
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
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';

// --- State ---
const hasStarted = ref(false);
const currentSessionId = Math.random().toString(36).substring(2, 10);

// Modal Flags
const showLimitModal = ref(false);
const showConfirmModal = ref(false);
const showSuccessModal = ref(false);
const showReviewModal = ref(false);

const menuItems = ref<any[]>([]);
const currentItemIndex = ref(0);

// Answers Dictionary: { itemId: { questionId: selectedOptionId / text } }
const answers = ref<Record<number, Record<number, any>>>({});

const questions = [
  {
    id: 1,
    shortText: "Which emotion makes you crave it?",
    text: "Which emotion or physical state most strongly makes you want to order this item?",
    type: 'vertical-radio',
    options: [
      { id: 1, label: "Stressed/Overwhelmed", icon: "😩" },
      { id: 2, label: "Happy/Celebratory", icon: "😊" },
      { id: 3, label: "Tired/Low Energy", icon: "😴" },
      { id: 4, label: "Relaxed/Chilling", icon: "😌" },
      { id: 5, label: "Focused/Working", icon: "🎯" }
    ]
  },
  {
    id: 2,
    shortText: "What weather suits it best?",
    text: "In what weather condition does this item feel most satisfying?",
    type: 'grid-radio',
    options: [
      { id: 6, label: "Hot/Sunny", icon: "☀️" },
      { id: 7, label: "Cold/Rainy", icon: "🌧️" },
      { id: 8, label: "Any Weather", icon: "⛅" }
    ]
  },
  {
    id: 3,
    shortText: "What's the vibe of the dish?",
    text: "What is the vibe of this specific dish?",
    type: 'grid-radio',
    options: [
      { id: 9, label: "Heavy Meal", icon: "🍽️" },
      { id: 10, label: "Light Snack", icon: "🥗" },
      { id: 11, label: "Drink/Refreshment", icon: "🥤" }
    ]
  },
  {
    id: 4,
    shortText: "What's a fair student price?",
    text: "What do you think is a fair Student-Friendly price for this item?",
    type: 'grid-radio',
    options: [
      { id: 12, label: "Under ₱150", sub: "Budget" },
      { id: 13, label: "₱150 - ₱249", sub: "Mid-range" },
      { id: 14, label: "₱250 and above", sub: "Premium" }
    ]
  },
  {
    id: 5,
    shortText: "Describe it to an AI.",
    text: "How would you describe this dish to the AI chatbot?",
    type: 'textarea'
  }
];

// --- Computed Properties ---
const currentItem = computed(() => {
  if (menuItems.value.length === 0) return null;
  return menuItems.value[currentItemIndex.value];
});

const isLastItem = computed(() => {
  return currentItemIndex.value === menuItems.value.length - 1;
});

const isCurrentItemComplete = computed(() => {
  if (!currentItem.value) return false;
  const itemId = currentItem.value.id;
  const itemAnswers = answers.value[itemId];
  if (!itemAnswers) return false;

  for (const q of questions) {
    const ans = itemAnswers[q.id];
    if (q.type === 'textarea') {
      if (!ans || typeof ans !== 'string' || ans.trim() === '') return false;
    } else {
      if (ans === undefined || ans === null) return false;
    }
  }
  return true;
});

const completedItemsCount = computed(() => {
  let count = 0;
  menuItems.value.forEach(item => {
    if (answers.value[item.id] && Object.keys(answers.value[item.id]).length > 0) count++;
  });
  return count;
});

const answeredItems = computed(() => {
  return menuItems.value.filter(item => {
    return questions.some(q => isAnswered(item.id, q.id));
  });
});

const getImagePath = (item) => {
  if (!item || !item.imageName) return '';

  // 1. Ask Vite where the live server's root folder actually is
  const baseUrl = import.meta.env.BASE_URL;

  // 2. Clean up the URL just in case Vite adds an extra slash
  const cleanBase = baseUrl.endsWith('/') ? baseUrl.slice(0, -1) : baseUrl;

  // 3. Build the absolute bulletproof path
  return `${cleanBase}/items/${item.imageName}`;
};

/**
 * Helper to match badge colors based on category.
 */
const getPillClass = (cat: string | undefined) => {
  if (!cat) return 'pill-default';

  const map: Record<string, string> = {
    'Meal': 'pill-meal',
    'Bread': 'pill-bread',
    'Pasta': 'pill-pasta',
    'Waffle': 'pill-waffle',
    'Coffee': 'pill-coffee',
    'Non-coffee': 'pill-noncoffee',
    'Frappe Series': 'pill-frappe',
    'Float': 'pill-float',
    'Sparkling Soda': 'pill-soda',
    'Milktea': 'pill-milktea',
    'Fruit Tea': 'pill-fruittea'
  };
  return map[cat.trim()] || 'pill-default';
};

// --- Methods ---

const shuffleArray = (array: any[]) => {
  for (let i = array.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [array[i], array[j]] = [array[j], array[i]];
  }
};

const fetchMenuItems = async () => {
  try {
    const response = await axios.get('/menu-items');
    let allItems = response.data;
    shuffleArray(allItems);
    menuItems.value = allItems.slice(0, 15);
  } catch (error) {
    console.error("Error fetching menu items:", error);
  }
};

const checkSurveyLimit = async () => {
  try {
    const response = await axios.get('/api/stats/survey-status');
    if(response.data.isFull) {
      showLimitModal.value = true;
    }
  } catch(error) {
    console.error("Failed to check survey status:", error);
  }
};

const nextItem = () => {
  if (currentItemIndex.value < menuItems.value.length - 1) {
    currentItemIndex.value++;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
};

const prevItem = () => {
  if (currentItemIndex.value > 0) {
    currentItemIndex.value--;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
};

const resetSurvey = () => {
  showSuccessModal.value = false;
  window.location.reload();
};

const executeFinalSubmit = async () => {
  try {
    const payload: any[] = [];

    menuItems.value.forEach(item => {
      const itemAnswers = answers.value[item.id];
      if (itemAnswers) {
        Object.entries(itemAnswers).forEach(([qId, ans]) => {
          const isText = typeof ans === 'string';
          payload.push({
            userId: currentSessionId,
            menuItemId: item.id,
            questionId: Number(qId),
            selectedOptionId: isText ? null : ans,
            textResponse: isText ? ans : null
          });
        });
      }
    });

    if (payload.length === 0) return;

    await axios.post('/submit-category', payload);

    showConfirmModal.value = false;
    showReviewModal.value = false;
    showSuccessModal.value = true;

  } catch (error: any) {
    const errorMessage = error.response?.data ? JSON.stringify(error.response.data) : "";
    if (errorMessage.includes("LIMIT_REACHED")) {
      showConfirmModal.value = false;
      showReviewModal.value = false;
      showLimitModal.value = true;
    } else {
      console.error("Error saving data:", error);
      alert("Oops! There was a problem saving your answers. Please try again.");
    }
  }
};

// Answer Management
const setAnswer = (itemId: number | undefined, questionId: number, optionId: number) => {
  if (!itemId) return;
  if (!answers.value[itemId]) answers.value[itemId] = {};
  answers.value[itemId][questionId] = optionId;
};

const setTextAnswer = (itemId: number | undefined, questionId: number, text: string) => {
  if (!itemId) return;
  if (!answers.value[itemId]) answers.value[itemId] = {};
  answers.value[itemId][questionId] = text;
};

const getAnswer = (itemId: number | undefined, questionId: number) => {
  if (!itemId || !answers.value[itemId]) return null;
  return answers.value[itemId][questionId];
};

const isAnswered = (itemId: number | undefined, questionId: number) => {
  if (!itemId || !answers.value[itemId]) return false;
  const ans = answers.value[itemId][questionId];
  if (typeof ans === 'string') return ans.trim().length > 0;
  return ans !== undefined && ans !== null;
};

const getAnswerText = (itemId: number, q: any) => {
  const ans = getAnswer(itemId, q.id);

  if (ans === null || ans === undefined || ans === '') {
    return 'Not answered';
  }

  if (q.type === 'textarea') {
    return `"${ans}"`;
  }

  const selectedOption = q.options?.find((opt: any) => opt.id === ans);
  if (selectedOption) {
    return `${selectedOption.icon || ''} ${selectedOption.label}`;
  }

  return 'Unknown';
};

onMounted(() => {
  checkSurveyLimit();
  fetchMenuItems();
});
</script>

<style scoped>
/* GLOBALS */
.survey-layout { background-color: #f8fafc; min-height: 100vh; font-family: 'Inter', -apple-system, sans-serif; color: #1e293b; padding-bottom: 50px;}

/* SHARED BUTTONS */
.primary-btn {
  background: #f97316; color: white; border: none; padding: 16px 40px; border-radius: 12px; font-size: 1.1rem; font-weight: 700; cursor: pointer; width: 100%; transition: transform 0.2s, background 0.2s;
}
.primary-btn:hover { background: #ea580c; transform: translateY(-2px); }

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
.welcome-screen { position: fixed; inset: 0; background-image: url('https://images.unsplash.com/photo-1554118811-1e0d58224f24?auto=format&fit=crop&q=80&w=2000'); background-size: cover; background-position: center; display: flex; align-items: center; justify-content: center; z-index: 100; }
.welcome-card { position: relative; background: white; padding: 50px 40px; border-radius: 24px; text-align: center; max-width: 500px; width: 90%; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5); animation: slideUp 0.6s cubic-bezier(0.16, 1, 0.3, 1) forwards; }
.welcome-icon { background: #f97316; color: white; width: 70px; height: 70px; border-radius: 20px; display: flex; align-items: center; justify-content: center; font-size: 2.5rem; margin: 0 auto 20px auto; box-shadow: 0 10px 15px -3px rgba(249, 115, 22, 0.3); }
.welcome-card h1 { margin: 0 0 15px 0; color: #0f172a; font-size: 2.2rem; font-weight: 800; }
.welcome-card p { color: #475569; line-height: 1.6; margin-bottom: 35px; font-size: 1.1rem; }
@keyframes slideUp { 0% { transform: translateY(40px); opacity: 0; } 100% { transform: translateY(0); opacity: 1; } }

/* HEADER */
.top-nav { background: white; border-bottom: 1px solid #e2e8f0; position: sticky; top: 0; z-index: 50; }
.nav-content { max-width: 1200px; margin: 0 auto; padding: 15px 30px; display: flex; justify-content: space-between; align-items: center; }
.logo { display: flex; align-items: center; gap: 10px; }
.logo-icon { background: #f97316; color: white; width: 32px; height: 32px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 1.1rem; }
.logo h1 { margin: 0; font-size: 1.2rem; color: #0f172a; font-weight: 700; }

/* HEADER ACTIONS */
.header-actions { display: flex; align-items: center; gap: 20px; }
.global-progress { font-size: 0.95rem; color: #64748b; }
.global-progress strong { color: #0f172a; }
.header-finish-btn { background: #10b981; color: white; border: none; padding: 10px 20px; border-radius: 8px; font-weight: 700; font-size: 0.9rem; cursor: pointer; transition: background 0.2s;}
.header-finish-btn:hover { background: #059669; }
.pulse-light { animation: pulseLight 2s infinite; }
@keyframes pulseLight {
  0% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.4); }
  70% { box-shadow: 0 0 0 8px rgba(16, 185, 129, 0); }
  100% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
}

.main-content { max-width: 1200px; margin: 40px auto; padding: 0 30px; }
.rating-view { display: grid; grid-template-columns: 350px 1fr; gap: 40px; align-items: start; }
.left-pane { position: sticky; top: 140px; }
.sticky-card { background: white; border-radius: 16px; border: 1px solid #e2e8f0; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.03); }
.pane-header { padding: 15px 20px; display: flex; justify-content: space-between; align-items: center; background: #f8fafc; border-bottom: 1px solid #e2e8f0; font-size: 0.85rem; }
.breadcrumb { color: #64748b; font-weight: 500;}
.text-dark { color: #0f172a; font-weight: 600; }
.cat-progress { color: #64748b; font-weight: 500;}
.green-text { color: #16a34a; font-weight: 700; }
.item-cover { position: relative; }
.cover-img {
  height: 250px;
  background-size: cover;
  background-position: center;
  /* UPDATE: Added gray fallback color */
  background-color: #e2e8f0;
}
.cover-img::before { content: ''; position: absolute; inset: 0; background: linear-gradient(to top, rgba(0,0,0,0.8), transparent 70%); }
.cover-info { position: absolute; bottom: 20px; left: 20px; right: 20px; }
.cover-info h3 { margin: 0 0 10px 0; color: white; font-size: 1.4rem; font-weight: 700; line-height: 1.2; text-shadow: 0 2px 4px rgba(0,0,0,0.5);}
.badge { padding: 4px 10px; border-radius: 6px; font-size: 0.75rem; font-weight: 700; display: inline-block; border: 1px solid transparent; }

.questions-list { display: flex; flex-direction: column; gap: 20px; }
.question-card { background: white; padding: 30px; border-radius: 16px; border: 1px solid #e2e8f0; box-shadow: 0 4px 15px rgba(0,0,0,0.02); }
.q-header { display: flex; align-items: flex-start; gap: 15px; margin-bottom: 25px; }
.q-bubble { width: 28px; height: 28px; background: #ffedd5; color: #ea580c; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 0.9rem; flex-shrink: 0; transition: all 0.3s;}
.q-bubble.answered { background: #22c55e; color: white; }
.q-header h4 { margin: 0; font-size: 1.15rem; color: #0f172a; line-height: 1.4; padding-top: 2px;}

.opt-btn-vertical, .opt-btn-grid { position: relative; background: #f8fafc; border: 2px solid #e2e8f0; border-radius: 12px; cursor: pointer; transition: all 0.2s; text-align: left; }
.opt-btn-vertical:hover, .opt-btn-grid:hover { border-color: #cbd5e1; background: #f1f5f9; }
.opt-btn-vertical.selected, .opt-btn-grid.selected { border-color: #f97316; background: #fff7ed; }

.vertical-options { display: flex; flex-direction: column; gap: 10px; }
.opt-btn-vertical { display: flex; align-items: center; gap: 15px; padding: 15px 20px; }
.opt-icon { font-size: 1.5rem; }
.opt-label { font-size: 1rem; font-weight: 600; color: #334155; }

.grid-options { display: grid; grid-template-columns: repeat(3, 1fr); gap: 15px; }
.opt-btn-grid { padding: 20px 15px; display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; gap: 8px; min-height: 110px;}
.opt-icon-large { font-size: 2rem; }
.opt-label-main { width: 100%; text-align: center; font-weight: 700; color: #0f172a; font-size: 1.05rem; }
.opt-sub { width: 100%; text-align: center; font-size: 0.8rem; color: #64748b; font-weight: 500; }

.text-input-wrapper { position: relative; }
.styled-textarea { box-sizing: border-box; width: 100%; height: 120px; padding: 20px; border-radius: 12px; border: 2px solid #e2e8f0; background: #f8fafc; font-family: inherit; font-size: 1rem; color: #334155; resize: none; transition: all 0.2s; outline: none; }
.styled-textarea:focus { border-color: #f97316; background: white; }
.styled-textarea.has-content { border-color: #f97316; background: #fff7ed; }
.char-count { position: absolute; bottom: 35px; right: 15px; font-size: 0.8rem; color: #94a3b8; font-weight: 500;}
.helper-text { margin: 10px 0 0 0; font-size: 0.85rem; color: #64748b; text-align: center; }

.action-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; padding-top: 20px; border-top: 1px solid #e2e8f0;}
.nav-btn { padding: 14px 24px; border-radius: 10px; font-weight: 700; font-size: 1rem; cursor: pointer; transition: all 0.2s; border: none; }
.nav-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.nav-btn.secondary { background: white; border: 1px solid #cbd5e1; color: #475569; }
.nav-btn.secondary:hover:not(:disabled) { background: #f1f5f9; }
.nav-btn.primary { background: #f97316; color: white; margin-left: auto;}
.nav-btn.primary:hover { background: #ea580c; }
.nav-btn.success { background: #22c55e; color: white; margin-left: auto;}
.nav-btn.success:hover { background: #16a34a; }

.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(15, 23, 42, 0.6); backdrop-filter: blur(5px); display: flex; align-items: center; justify-content: center; z-index: 9999; }
.modal-card { background: white; padding: 40px; border-radius: 24px; text-align: center; max-width: 450px; width: 90%; margin: auto; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25); animation: popIn 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275) forwards; }
.modal-icon { font-size: 4.5rem; margin-bottom: 15px; }
.modal-card h2 { margin: 0 0 15px 0; color: #0f172a; font-size: 1.8rem; font-weight: 800; }
.modal-card p { color: #64748b; line-height: 1.6; margin-bottom: 30px; font-size: 1.05rem; }
.modal-actions { display: flex; gap: 15px; justify-content: center; margin-top: 10px; }
.modal-actions button { flex: 1; margin: 0; }
@keyframes popIn { 0% { transform: scale(0.8); opacity: 0; } 100% { transform: scale(1); opacity: 1; } }

.pulse { animation: pulse 2s infinite; }
@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(249, 115, 22, 0.4); }
  70% { box-shadow: 0 0 0 10px rgba(249, 115, 22, 0); }
  100% { box-shadow: 0 0 0 0 rgba(249, 115, 22, 0); }
}

@media (max-width: 850px) {
  .rating-view { grid-template-columns: 1fr; gap: 20px; }
  .left-pane { position: relative; top: 0; }
  .cover-img { height: 180px; }
  .grid-options { grid-template-columns: 1fr; }
  .main-content { padding: 0 15px; }
  .action-footer { flex-direction: column; gap: 15px; }
  .nav-btn.primary, .nav-btn.success { margin-left: 0; width: 100%; }
  .header-actions { flex-direction: column; gap: 5px; align-items: flex-end;}
}

/* --- NEW: REVIEW MODAL STYLES --- */
.review-card { max-width: 700px !important; width: 95%; padding: 0 !important; display: flex; flex-direction: column; max-height: 90vh; overflow: hidden; text-align: left; }
.review-header { padding: 30px 40px 20px 40px; border-bottom: 1px solid #e2e8f0; background: white; z-index: 10; }
.review-header h2 { margin: 0 0 5px 0; font-size: 1.6rem; color: #0f172a; font-weight: 800; text-align: left; }
.review-header p { margin: 0; color: #64748b; font-size: 0.95rem; }

.review-scroll-area { flex-grow: 1; overflow-y: auto; padding: 25px 40px; background: #f8fafc; }
.review-scroll-area::-webkit-scrollbar { width: 8px; }
.review-scroll-area::-webkit-scrollbar-thumb { background-color: #cbd5e1; border-radius: 4px; }

.review-item-block { background: white; border: 1px solid #e2e8f0; border-radius: 12px; margin-bottom: 20px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.02); }
.review-item-header { display: flex; align-items: center; gap: 15px; padding: 15px 20px; background: #f1f5f9; border-bottom: 1px solid #e2e8f0; }
.review-item-number { background: #0f172a; color: white; width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 0.85rem; font-weight: 700; flex-shrink: 0; }
.review-item-titles { display: flex; align-items: center; gap: 10px; flex-wrap: wrap;}
.review-item-titles h3 { margin: 0; font-size: 1.1rem; color: #0f172a; font-weight: 700; }

.review-q-list { padding: 15px 20px; display: flex; flex-direction: column; gap: 12px; }
.review-q-row { display: flex; justify-content: space-between; gap: 20px; align-items: flex-start; padding-bottom: 12px; border-bottom: 1px dashed #f1f5f9; }
.review-q-row:last-child { border-bottom: none; padding-bottom: 0; }
.r-question { width: 50%; font-size: 0.85rem; color: #64748b; font-weight: 500; line-height: 1.4; }
.r-answer { width: 50%; font-size: 0.9rem; color: #0f172a; font-weight: 600; text-align: right; }
.text-missing { color: #ef4444; font-style: italic; font-weight: 500; }

.review-footer { padding: 20px 40px; border-top: 1px solid #e2e8f0; background: white; display: flex; justify-content: space-between; gap: 15px; z-index: 10; }
.review-footer button { flex: 1; margin: 0; }

@media (max-width: 600px) {
  .review-header, .review-scroll-area, .review-footer { padding: 20px; }
  .review-q-row { flex-direction: column; gap: 4px; border-bottom: none; background: #f8fafc; padding: 10px; border-radius: 8px;}
  .r-question, .r-answer { width: 100%; text-align: left; }
}

/* --- CATEGORY BADGE COLORS --- */
.pill-default { background: #f1f5f9; border-color: #e2e8f0; color: #475569; }
.pill-meal { background: #fef2f2; border-color: #fecaca; color: #b91c1c; }
.pill-bread { background: #fdf5e6; border-color: #ebd5b3; color: #8b5a2b; }
.pill-pasta { background: #fefce8; border-color: #fde047; color: #854d0e; }
.pill-waffle { background: #fff7ed; border-color: #fed7aa; color: #c2410c; }
.pill-coffee { background: #fffbeb; border-color: #fde68a; color: #b45309; }
.pill-noncoffee { background: #f0f9ff; border-color: #bae6fd; color: #0284c7; }
.pill-frappe { background: #f5f3ff; border-color: #ddd6fe; color: #7c3aed; }
.pill-float { background: #ecfdf5; border-color: #a7f3d0; color: #059669; }
.pill-soda { background: #ecfeff; border-color: #a5f3fc; color: #0891b2; }
.pill-milktea { background: #fdf4ff; border-color: #f5d0fe; color: #c026d3; }
.pill-fruittea { background: #fff1f2; border-color: #fecdd3; color: #e11d48; }

.disclaimer-box { background: #f1f5f9; border-radius: 8px; padding: 12px 15px; margin-bottom: 30px; display: flex; align-items: flex-start; gap: 10px; text-align: left; border-left: 4px solid #f97316; }
.disclaimer-box .info-icon { font-size: 1.2rem; line-height: 1; }
.disclaimer-box p { margin: 0 !important; font-size: 0.85rem !important; color: #475569 !important; line-height: 1.4 !important; }
.image-disclaimer { padding: 10px 20px; background: #f8fafc; font-size: 0.75rem; color: #64748b; text-align: center; font-style: italic; border-top: 1px solid #e2e8f0; }

.empty-review { text-align: center; padding: 40px 20px; color: #64748b; font-size: 1.05rem; font-style: italic; }
</style>
