<template>
  <div class="survey-layout">

    <header class="top-nav">
      <div class="nav-content">
        <div class="logo">
          <span class="logo-icon">🍴</span>
          <h1>CaféRater</h1>
        </div>
        <div class="global-progress">
          <strong>{{ totalRatedItems }}</strong> of {{ menuItems.length }} rated
        </div>
      </div>
    </header>

    <div class="tabs-container">
      <div class="tabs-scroll">
        <button
          v-for="cat in categories"
          :key="cat"
          class="tab-btn"
          :class="{ active: activeCategory === cat }"
          @click="selectCategory(cat)"
        >
          {{ cat }}
        </button>
      </div>
    </div>

    <main class="main-content">

      <div v-if="isLanding" class="landing-view">
        <div class="landing-card">
          <div class="category-icon">🍽️</div>
          <h2>{{ activeCategory }}</h2>
          <p class="subtitle">{{ filteredItems.length }} items • Scroll to rate each one</p>

          <div class="question-preview-list">
            <div v-for="(q, index) in questions" :key="q.id" class="q-preview">
              <span class="q-num-light">{{ index + 1 }}</span>
              <span>{{ q.shortText }}</span>
            </div>
          </div>

          <button class="primary-btn pulse" @click="startRating">
            Let's go &rarr;
          </button>
        </div>
      </div>

      <div v-else class="rating-view">

        <div class="left-pane">
          <div class="sticky-card">
            <div class="pane-header">
              <span class="breadcrumb">{{ activeCategory }} — <span class="text-dark">{{ currentItem?.name }}</span></span>
              <span class="cat-progress"><strong class="green-text">{{ completedInCategory }}</strong>/{{ filteredItems.length }} done</span>
            </div>

            <div class="item-cover">
              <div class="cover-img" style="background-image: url('https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&q=80&w=800');"></div>
              <div class="cover-info">
                <h3>{{ currentItem?.name }}</h3>
                <span class="badge">🍴 {{ activeCategory }}</span>
              </div>
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
                  <div class="check-indicator" v-if="getAnswer(currentItem?.id, q.id) === opt.id">✓</div>
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
                  <div class="check-indicator" v-if="getAnswer(currentItem?.id, q.id) === opt.id">✓</div>
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

            </div> <div class="action-footer">
            <button class="nav-btn secondary" @click="prevItem" :disabled="currentItemIndex === 0">
              &larr; Previous Item
            </button>

            <button v-if="currentItemIndex < filteredItems.length - 1" class="nav-btn primary" @click="nextItem">
              Next Item &rarr;
            </button>

            <button v-else class="nav-btn success" @click="finishCategory">
              Submit & Return to Categories
            </button>
          </div>

          </div>
        </div>

      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';

// --- State ---
const menuItems = ref<any[]>([]);
const isLanding = ref(true);
const activeCategory = ref('Meal');
const currentItemIndex = ref(0);

// Answers Dictionary: { itemId: { questionId: selectedOptionId / text } }
const answers = ref<Record<number, Record<number, any>>>({});

// The specific categories you requested
const categories = [
  'Meal', 'Bread', 'Pasta', 'Waffle', 'Coffee', 'Non-coffee',
  'Frappe Series', 'Float', 'Milktea', 'Sparkling Soda', 'Fruit Tea'
];

// Master list of questions matching your screenshots
const questions = [
  {
    id: 1,
    shortText: "Which emotion makes you crave it?",
    text: "Which emotion or physical state most strongly makes you want to order this item?",
    type: 'vertical-radio',
    options: [
      { id: 11, label: "Stressed/Overwhelmed", icon: "😩" },
      { id: 12, label: "Happy/Celebratory", icon: "😊" },
      { id: 13, label: "Tired/Low Energy", icon: "😴" },
      { id: 14, label: "Relaxed/Chilling", icon: "😌" },
      { id: 15, label: "Focused/Working", icon: "🎯" }
    ]
  },
  {
    id: 2,
    shortText: "What weather suits it best?",
    text: "In what weather condition does this item feel most satisfying?",
    type: 'grid-radio',
    options: [
      { id: 21, label: "Hot/Sunny", icon: "☀️" },
      { id: 22, label: "Cold/Rainy", icon: "🌧️" },
      { id: 23, label: "Any Weather", icon: "⛅" }
    ]
  },
  {
    id: 3,
    shortText: "What's the vibe of the dish?",
    text: "What is the vibe of this specific dish?",
    type: 'grid-radio',
    options: [
      { id: 31, label: "Heavy Meal", icon: "🍽️" },
      { id: 32, label: "Light Snack", icon: "🥗" },
      { id: 33, label: "Drink/Refreshment", icon: "🥤" }
    ]
  },
  {
    id: 4,
    shortText: "What's a fair student price?",
    text: "What do you think is a fair Student-Friendly price for this item?",
    type: 'grid-radio',
    options: [
      { id: 41, label: "Under ₱150", sub: "Budget" },
      { id: 42, label: "₱150 - ₱249", sub: "Mid-range" },
      { id: 43, label: "₱250 and above", sub: "Premium" }
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
const filteredItems = computed(() => {
  return menuItems.value.filter(item => item.category === activeCategory.value);
});

const currentItem = computed(() => {
  if (filteredItems.value.length === 0) return null;
  return filteredItems.value[currentItemIndex.value];
});

// Calculate how many items in the CURRENT category have at least 1 answer
const completedInCategory = computed(() => {
  let count = 0;
  filteredItems.value.forEach(item => {
    if (answers.value[item.id] && Object.keys(answers.value[item.id]).length > 0) {
      count++;
    }
  });
  return count;
});

// Calculate total completely rated items globally
const totalRatedItems = computed(() => {
  let count = 0;
  menuItems.value.forEach(item => {
    if (answers.value[item.id] && Object.keys(answers.value[item.id]).length > 0) {
      count++;
    }
  });
  return count;
});

// --- Methods ---
const fetchMenuItems = async () => {
  try {
    const response = await axios.get('http://localhost:8080/menu-items');
    menuItems.value = response.data;
  } catch (error) {
    console.error("Error fetching menu items:", error);
  }
};

const selectCategory = (cat: string) => {
  activeCategory.value = cat;
  isLanding.value = true;
  currentItemIndex.value = 0;
  window.scrollTo({ top: 0, behavior: 'smooth' });
};

const startRating = () => {
  isLanding.value = false;
  window.scrollTo({ top: 0, behavior: 'smooth' });
};

const nextItem = () => {
  if (currentItemIndex.value < filteredItems.value.length - 1) {
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

const finishCategory = () => {
  // Logic to actually save to backend would go here!
  console.log("Saving answers:", answers.value);
  isLanding.value = true;
  window.scrollTo({ top: 0, behavior: 'smooth' });
  alert("Awesome! You finished the " + activeCategory.value + " category.");
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

onMounted(() => {
  fetchMenuItems();
});
</script>

<style scoped>
/* GLOBALS */
.survey-layout { background-color: #f8fafc; min-height: 100vh; font-family: 'Inter', -apple-system, sans-serif; color: #1e293b; padding-bottom: 50px;}

/* HEADER */
.top-nav { background: white; border-bottom: 1px solid #e2e8f0; position: sticky; top: 0; z-index: 50; }
.nav-content { max-width: 1200px; margin: 0 auto; padding: 15px 30px; display: flex; justify-content: space-between; align-items: center; }
.logo { display: flex; align-items: center; gap: 10px; }
.logo-icon { background: #f97316; color: white; width: 32px; height: 32px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 1.1rem; }
.logo h1 { margin: 0; font-size: 1.2rem; color: #0f172a; font-weight: 700; }
.global-progress { font-size: 0.9rem; color: #64748b; }
.global-progress strong { color: #0f172a; }

/* TABS */
.tabs-container { background: white; border-bottom: 1px solid #e2e8f0; position: sticky; top: 63px; z-index: 40; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.02); }
.tabs-scroll { max-width: 1200px; margin: 0 auto; padding: 10px 30px; display: flex; gap: 8px; overflow-x: auto; scrollbar-width: none; }
.tabs-scroll::-webkit-scrollbar { display: none; }
.tab-btn { padding: 8px 18px; border-radius: 20px; border: 1px solid #cbd5e1; background: white; color: #64748b; font-weight: 600; font-size: 0.85rem; cursor: pointer; transition: all 0.2s; white-space: nowrap; }
.tab-btn:hover { background: #f1f5f9; }
.tab-btn.active { background: #f97316; color: white; border-color: #f97316; }

.main-content { max-width: 1200px; margin: 40px auto; padding: 0 30px; }

/* LANDING VIEW */
.landing-view { display: flex; justify-content: center; align-items: center; margin-top: 60px; }
.landing-card { background: white; padding: 50px; border-radius: 24px; text-align: center; max-width: 500px; width: 100%; box-shadow: 0 10px 30px rgba(0,0,0,0.05); border: 1px solid #f1f5f9;}
.category-icon { font-size: 5rem; margin-bottom: 20px; }
.landing-card h2 { margin: 0 0 5px 0; font-size: 2rem; color: #0f172a; }
.subtitle { color: #64748b; margin-bottom: 30px; font-size: 1.1rem; }
.question-preview-list { display: flex; flex-direction: column; gap: 12px; margin-bottom: 40px; text-align: left; }
.q-preview { display: flex; align-items: center; gap: 15px; background: #f8fafc; padding: 12px 20px; border-radius: 12px; border: 1px solid #e2e8f0; font-size: 0.95rem; font-weight: 500; color: #475569;}
.q-num-light { background: #ffedd5; color: #ea580c; width: 24px; height: 24px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 0.8rem; font-weight: 700; flex-shrink: 0; }
.primary-btn { background: #f97316; color: white; border: none; padding: 16px 40px; border-radius: 12px; font-size: 1.1rem; font-weight: 700; cursor: pointer; width: 100%; transition: transform 0.2s, background 0.2s; }
.primary-btn:hover { background: #ea580c; transform: translateY(-2px); }

/* RATING VIEW (Split Pane) */
.rating-view { display: grid; grid-template-columns: 350px 1fr; gap: 40px; align-items: start; }

/* LEFT PANE (Sticky Item Card) */
.left-pane { position: sticky; top: 140px; }
.sticky-card { background: white; border-radius: 16px; border: 1px solid #e2e8f0; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.03); }
.pane-header { padding: 15px 20px; display: flex; justify-content: space-between; align-items: center; background: #f8fafc; border-bottom: 1px solid #e2e8f0; font-size: 0.85rem; }
.breadcrumb { color: #64748b; font-weight: 500;}
.text-dark { color: #0f172a; font-weight: 600; }
.cat-progress { color: #64748b; font-weight: 500;}
.green-text { color: #16a34a; font-weight: 700; }
.item-cover { position: relative; }
.cover-img { height: 250px; background-size: cover; background-position: center; }
.cover-img::before { content: ''; position: absolute; inset: 0; background: linear-gradient(to top, rgba(0,0,0,0.8), transparent 70%); }
.cover-info { position: absolute; bottom: 20px; left: 20px; right: 20px; }
.cover-info h3 { margin: 0 0 10px 0; color: white; font-size: 1.4rem; font-weight: 700; line-height: 1.2; text-shadow: 0 2px 4px rgba(0,0,0,0.5);}
.badge { background: #ffedd5; color: #c2410c; padding: 4px 10px; border-radius: 6px; font-size: 0.75rem; font-weight: 700; }

/* RIGHT PANE (Questions List) */
.questions-list { display: flex; flex-direction: column; gap: 20px; }
.question-card { background: white; padding: 30px; border-radius: 16px; border: 1px solid #e2e8f0; box-shadow: 0 4px 15px rgba(0,0,0,0.02); }

.q-header { display: flex; align-items: flex-start; gap: 15px; margin-bottom: 25px; }
.q-bubble { width: 28px; height: 28px; background: #ffedd5; color: #ea580c; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 0.9rem; flex-shrink: 0; transition: all 0.3s;}
.q-bubble.answered { background: #22c55e; color: white; }
.q-header h4 { margin: 0; font-size: 1.15rem; color: #0f172a; line-height: 1.4; padding-top: 2px;}

/* Option Buttons Shared */
.opt-btn-vertical, .opt-btn-grid { position: relative; background: #f8fafc; border: 2px solid #e2e8f0; border-radius: 12px; cursor: pointer; transition: all 0.2s; text-align: left; }
.opt-btn-vertical:hover, .opt-btn-grid:hover { border-color: #cbd5e1; background: #f1f5f9; }
.opt-btn-vertical.selected, .opt-btn-grid.selected { border-color: #f97316; background: #fff7ed; }
.check-indicator { position: absolute; top: 10px; right: 10px; background: #f97316; color: white; width: 20px; height: 20px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 0.7rem; font-weight: bold; }

/* Vertical Layout (Q1) */
.vertical-options { display: flex; flex-direction: column; gap: 10px; }
.opt-btn-vertical { display: flex; align-items: center; gap: 15px; padding: 15px 20px; }
.opt-icon { font-size: 1.5rem; }
.opt-label { font-size: 1rem; font-weight: 600; color: #334155; }

/* Grid Layout (Q2, Q3, Q4) */
.grid-options { display: grid; grid-template-columns: repeat(3, 1fr); gap: 15px; }
.opt-btn-grid { padding: 20px 15px; display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; gap: 8px; min-height: 110px;}
.opt-icon-large { font-size: 2rem; }
.opt-label-main { font-weight: 700; color: #0f172a; font-size: 1.05rem; }
.opt-sub { font-size: 0.8rem; color: #64748b; font-weight: 500; }

/* Text Area (Q5) */
.text-input-wrapper { position: relative; }
.styled-textarea { width: 100%; height: 120px; padding: 20px; border-radius: 12px; border: 2px solid #e2e8f0; background: #f8fafc; font-family: inherit; font-size: 1rem; color: #334155; resize: none; transition: all 0.2s; outline: none; }
.styled-textarea:focus { border-color: #f97316; background: white; }
.styled-textarea.has-content { border-color: #f97316; background: #fff7ed; }
.char-count { position: absolute; bottom: 35px; right: 15px; font-size: 0.8rem; color: #94a3b8; font-weight: 500;}
.helper-text { margin: 10px 0 0 0; font-size: 0.85rem; color: #64748b; text-align: center; }

/* Footer Actions */
.action-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; padding-top: 20px; border-top: 1px solid #e2e8f0;}
.nav-btn { padding: 14px 24px; border-radius: 10px; font-weight: 700; font-size: 1rem; cursor: pointer; transition: all 0.2s; border: none; }
.nav-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.nav-btn.secondary { background: white; border: 1px solid #cbd5e1; color: #475569; }
.nav-btn.secondary:hover:not(:disabled) { background: #f1f5f9; }
.nav-btn.primary { background: #f97316; color: white; margin-left: auto;}
.nav-btn.primary:hover { background: #ea580c; }
.nav-btn.success { background: #22c55e; color: white; margin-left: auto;}
.nav-btn.success:hover { background: #16a34a; }

/* Simple entrance animation */
.pulse { animation: pulse 2s infinite; }
@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(249, 115, 22, 0.4); }
  70% { box-shadow: 0 0 0 10px rgba(249, 115, 22, 0); }
  100% { box-shadow: 0 0 0 0 rgba(249, 115, 22, 0); }
}
</style>
