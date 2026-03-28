<template>
  <div class="dashboard-layout">

    <header class="top-header">
      <div class="header-left">
        <div class="logo-icon">📊</div>
        <div>
          <h1>Food Preferences Survey</h1>
          <p class="subtitle">Jan 1 — {{ currentDate }} • {{ totalQuestions }} questions • {{ menuItems.length }} total items</p>
        </div>
      </div>
      <div class="header-right">
        <span class="live-badge"><span class="dot"></span> Live</span>
        <button class="danger-btn" @click="showClearModal = true">
          🗑️ Clear Data
        </button>
        <button class="export-btn" @click="downloadReport">
          📥 Export Report
        </button>
      </div>
    </header>

    <section class="kpi-grid">
      <!-- KPI CARD 1 -->
      <div class="stat-card">
        <div class="card-header">
          <span class="dot blue"></span> GLOBAL
        </div>
        <div class="card-body">
          <div class="icon-wrapper">📈</div>
          <div class="stat-info">
            <span class="stat-label">BASELINE METRIC</span>
            <h2 class="stat-value">{{ baselineCount }} <span class="limit-text">/ 30 Target</span></h2>
            <span class="stat-subtext" :class="{ 'text-success': baselineCount >= 30 }">
              {{ baselineCount >= 30 ? 'Goal Reached!' : `Need ${30 - baselineCount} more responses` }}
            </span>
          </div>
        </div>
      </div>

      <!-- KPI CARD 2 -->
      <div class="kpi-card">
        <div class="kpi-tag"><span class="t-dot orange"></span> CURRENT ITEM</div>
        <div class="kpi-body">
          <div class="kpi-icon orange">🎯</div>
          <div class="kpi-content">
            <p class="kpi-label">TOTAL RESPONSES</p>
            <h2>{{ itemTotal }}</h2>
            <p class="kpi-subtext">For selected menu item</p>
          </div>
        </div>
      </div>

      <!-- KPI CARD 3 -->
      <div class="kpi-card">
        <div class="kpi-tag"><span class="t-dot teal"></span> CURRENT ITEM</div>
        <div class="kpi-body">
          <div class="kpi-icon teal">👍</div>
          <div class="kpi-content">
            <p class="kpi-label">POSITIVE SENTIMENT</p>
            <h2>{{ sentiment.posPct }}%</h2>
            <p class="kpi-subtext">Happy Reviewers</p>
          </div>
        </div>
      </div>

      <!-- KPI CARD 4 -->
      <div class="kpi-card">
        <div class="kpi-tag"><span class="t-dot orange"></span> CURRENT ITEM</div>
        <div class="kpi-body">
          <div class="kpi-icon orange">🚨</div>
          <div class="kpi-content">
            <p class="kpi-label">NEEDS ATTENTION</p>
            <h2>{{ sentiment.negPct }}%</h2>
            <p class="kpi-subtext">Critical / Negative Feedback</p>
          </div>
        </div>
      </div>

      <!-- KPI CARD 5 -->
      <div class="kpi-card">
        <div class="kpi-tag"><span class="t-dot blue"></span> CURRENT ITEM</div>
        <div class="kpi-body">
          <div class="kpi-icon blue">💬</div>
          <div class="kpi-content">
            <p class="kpi-label">TOP KEYWORD</p>
            <h2 style="text-transform: capitalize;">
              {{ topKeywords.length > 0 ? topKeywords[0] : '-' }}
            </h2>
            <p class="kpi-subtext">Most used in text reviews</p>
          </div>
        </div>
      </div>
    </section>

    <section class="navigation-panel" v-if="menuItems.length > 0">

      <div class="filters-row">
        <div class="category-toggle">
          <button :class="{ active: activeCategory === 'Food' }" @click="setCategory('Food')">🍴 Food</button>
          <button :class="{ active: activeCategory === 'Drink' }" @click="setCategory('Drink')">🥤 Drink</button>
        </div>

        <div class="divider"></div>

        <div class="subcategory-pills">
          <button
            class="f-pill pill-all"
            :class="{ active: activeSubcategory === 'All' }"
            @click="setSubcategory('All')"
          >
            All {{ activeCategory }}
          </button>

          <button
            v-for="sub in currentSubcategories"
            :key="sub"
            class="f-pill"
            :class="[getPillClass(sub), { active: activeSubcategory === sub }]"
            @click="setSubcategory(sub)"
          >
            {{ sub }}
          </button>
        </div>
      </div>

      <div class="item-tabs-container">
        <div v-if="filteredMenuItems.length === 0" class="empty-filter">
          No items found in this category.
        </div>
        <div
          v-for="item in filteredMenuItems"
          :key="item.id"
          class="item-tab"
          :class="{ active: selectedItemId === item.id }"
          @click="selectItem(item.id)"
        >
          <div class="tab-thumb" style="background-image: url('https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&q=80&w=150')"></div>
          <span class="tab-title truncate-text" :title="item.name">{{ item.name }}</span>
          <span class="tab-cat">{{ item.category }}</span>
        </div>
      </div>
    </section>

    <div v-if="isLoading" class="state-message">
      <h2>Loading analytics for {{ menuItem?.name }}...</h2>
    </div>
    <div v-else-if="!menuItem" class="state-message">
      <h2>Select an item to view analytics.</h2>
    </div>
    <div v-else-if="totalResponses === 0" class="state-message empty">
      <h2>No Data Yet 📭</h2>
      <p>Nobody has submitted a survey for <strong>{{ menuItem?.name }}</strong> yet. Check back later!</p>
    </div>

    <section v-else class="cards-grid">

      <div v-for="(answers, question, index) in radioQuestions" :key="question" class="insight-card radio-card-v2">
        <div class="card-image-header-v2">
          <div class="image-overlay-v2">
            <h3 class="truncate-text">{{ menuItem?.name }}</h3>
            <span class="badge-v2 food-badge">🍴 {{ menuItem?.category }}</span>
          </div>
          <span class="q-circle">Q{{ index + 1 }}</span>
        </div>

        <div class="card-body">
          <h4 class="question-title">{{ question }}</h4>
          <p class="response-count">{{ totalResponses }} responses</p>

          <div class="bars-container">
            <div v-for="stat in answers" :key="stat.optionLabel" class="bar-row">
              <span class="bar-label" :title="stat.optionLabel">{{ stat.optionLabel }}</span>
              <div class="bar-track-v2">
                <div class="bar-fill orange-solid" :style="{ width: calculatePercentage(stat.voteCount, answers) + '%' }"></div>
              </div>
              <span class="bar-value">{{ stat.voteCount }}</span>
              <span class="bar-percent">{{ calculatePercentage(stat.voteCount, answers) }}%</span>
            </div>
          </div>

          <div class="key-insight-v2">
            <span class="insight-icon">📈</span>
            <span><strong>Key insight:</strong> {{ topResponse(answers) }} is the leading choice.</span>
          </div>
        </div>
      </div>

      <div v-for="(answers, question, index) in textQuestions" :key="question" class="insight-card text-card-v2">
        <div class="text-top-row">

          <div class="text-col-image">
            <span class="q-circle">Q{{ radioQuestionsCount + index + 1 }}</span>
            <div class="image-overlay-v2">
              <h3 class="truncate-text">{{ menuItem?.name }}</h3>
              <div class="badge-row">
                <span class="badge-v2 food-badge">🍴 {{ menuItem?.category }}</span>
                <span class="badge-v2 type-badge">💬 Open-ended</span>
              </div>
            </div>
          </div>

          <div class="text-col-sentiment">
            <h4 class="question-title-v2">{{ question }}</h4>
            <p class="response-count-v2">{{ answers.length }} text responses</p>
            <p class="section-label">SENTIMENT BREAKDOWN</p>

            <div class="sent-boxes-v2">
              <div class="s-box-v2 pos"><h2>{{ sentiment.pos }}</h2><span>Positive</span></div>
              <div class="s-box-v2 neu"><h2>{{ sentiment.neu }}</h2><span>Neutral</span></div>
              <div class="s-box-v2 neg"><h2>{{ sentiment.neg }}</h2><span>Negative</span></div>
            </div>

            <div class="sent-bar-thick">
              <div class="s-fill-thick pos" :style="{ width: sentiment.posPct + '%' }"></div>
              <div class="s-fill-thick neu" :style="{ width: sentiment.neuPct + '%' }"></div>
              <div class="s-fill-thick neg" :style="{ width: sentiment.negPct + '%' }"></div>
            </div>

            <div class="sent-legend">
              <span class="l-pos">● Positive {{ sentiment.posPct }}%</span>
              <span class="l-neu">● Neutral {{ sentiment.neuPct }}%</span>
              <span class="l-neg">● Negative {{ sentiment.negPct }}%</span>
              <span class="l-total">= 100%</span>
            </div>
          </div>

          <div class="text-col-keywords">
            <p class="section-label"># TOP KEYWORDS</p>
            <p class="section-subtext">Automatically extracted from reviews</p>
            <div class="word-cloud-v2">
              <span v-if="topKeywords.length === 0" class="w-small" style="color: #94a3b8;">Not enough text data yet.</span>

              <span
                v-for="(word, index) in topKeywords"
                :key="word"
                :class="getWordClass(index)"
              >
                {{ word }}
              </span>
            </div>
          </div>
        </div>

        <div class="text-bottom-row">
          <div class="excerpt-header-row">
            <div class="header-left-side">
              <p class="section-label mb-0">⚑ RESPONSE EXCERPTS</p>
              <span class="excerpt-count">{{ Math.min(answers.length, 8) }} of {{ answers.length }}</span>
            </div>
            <div class="excerpt-filters">
              <button class="f-btn active">All</button><button class="f-btn pos-btn">Positive</button><button class="f-btn neu-btn">Neutral</button><button class="f-btn neg-btn">Negative</button>
            </div>
          </div>

          <div class="excerpt-grid-v2">
            <div v-for="(feedback, i) in answers.slice(0, 8)" :key="i" class="exc-card">
              <p class="exc-text">
                "{{ feedback.response || feedback.textResponse || (typeof feedback === 'string' ? feedback : 'No text saved in database') }}"
              </p>
              <div class="exc-footer">
                <span class="exc-tag" :class="getSentimentData(feedback).class">
                  {{ getSentimentData(feedback).icon }} {{ getSentimentData(feedback).label }}
                </span>
              </div>
            </div>
          </div>
        </div>

      </div>

    </section>

    <Teleport to="body">
      <div v-if="showClearModal" class="modal-overlay">
        <div class="modal-card danger-card">
          <div class="modal-icon text-red">⚠️</div>
          <h2>Wipe All Data?</h2>
          <p>This will permanently delete <strong>all survey responses</strong> from the database. This action cannot be undone!</p>
          <div class="modal-actions">
            <button class="nav-btn secondary" @click="showClearModal = false">Cancel</button>
            <button class="nav-btn danger-solid" @click="clearAllData">Yes, Nuke It</button>
          </div>
        </div>
      </div>
    </Teleport>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import axios from 'axios';

// Master State
const menuItems = ref<any[]>([]);
const analyticsData = ref<Record<string, any>>({});
const isLoading = ref(true);

// Option 1 Navigation State
const activeCategory = ref('Food');
const activeSubcategory = ref('All');
const selectedItemId = ref<number | null>(null);

const currentDate = new Date().toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
const baselineCount = ref(0);
const showClearModal = ref(false);

// We define what belongs where based on your actual menu!
const foodSubcategories = ['Meal', 'Bread', 'Pasta', 'Waffle'];
const drinkSubcategories = ['Coffee', 'Non-coffee', 'Frappe Series', 'Float', 'Milktea', 'Sparkling Soda', 'Fruit Tea'];

// Computed Helpers for Filtering
const isFood = (cat: string) => foodSubcategories.includes(cat);
const isDrink = (cat: string) => drinkSubcategories.includes(cat);

// Maps categories to their pastel color CSS classes
const getPillClass = (sub: string) => {
  const map: Record<string, string> = {
    // New Individual Food Colors
    'Meal': 'pill-meal',
    'Bread': 'pill-bread',
    'Pasta': 'pill-pasta',
    'Waffle': 'pill-waffle',

    // Existing Drink Colors
    'Coffee': 'pill-coffee',
    'Non-coffee': 'pill-noncoffee',
    'Frappe Series': 'pill-frappe',
    'Float': 'pill-float',
    'Sparkling Soda': 'pill-soda',
    'Milktea': 'pill-milktea',
    'Fruit Tea': 'pill-fruittea'
  };
  return map[sub] || 'pill-all';
};

const currentSubcategories = computed(() => {
  return activeCategory.value === 'Food' ? foodSubcategories : drinkSubcategories;
});

// The Magic Filter: This dynamically builds the horizontal scrolling tabs
const filteredMenuItems = computed(() => {
  return menuItems.value.filter(item => {
    // 1. Is it the right master category (Food vs Drink)?
    const matchesTopLevel = activeCategory.value === 'Food' ? isFood(item.category) : isDrink(item.category);
    if (!matchesTopLevel) return false;

    // 2. Is it the right subcategory (Meal vs Pasta)?
    if (activeSubcategory.value !== 'All' && item.category !== activeSubcategory.value) {
      return false;
    }

    return true;
  });
});

// The currently viewed item
const menuItem = computed(() => menuItems.value.find(i => i.id === selectedItemId.value) || null);

// Data Fetching
const fetchMenuItems = async () => {
  try {
    const response = await axios.get('http://localhost:8080/menu-items');
    menuItems.value = response.data;

    // Auto-select the first item in the filtered list
    if (filteredMenuItems.value.length > 0) {
      await selectItem(filteredMenuItems.value[0].id);
    } else {
      isLoading.value = false;
    }
  } catch (error) {
    console.error("Error fetching menu items:", error);
    isLoading.value = false;
  }
};

const fetchAnalyticsForCurrentItem = async () => {
  if (!selectedItemId.value) return;
  isLoading.value = true;
  try {
    const response = await axios.get(`http://localhost:8080/analytics/${selectedItemId.value}`);
    analyticsData.value = response.data;
  } catch (error) {
    console.error(`Error fetching analytics for item ${selectedItemId.value}:`, error);
    analyticsData.value = {};
  } finally {
    isLoading.value = false;
  }
};

// Interaction Functions
const selectItem = async (itemId) => {
  selectedItemId.value = itemId;
  fetchItemStats(itemId);
  fetchAnalyticsForCurrentItem();
};

const setCategory = (category: string) => {
  activeCategory.value = category;
  activeSubcategory.value = 'All';
  autoSelectFirstFilteredItem();
};

const setSubcategory = (subcategory: string) => {
  activeSubcategory.value = subcategory;
  autoSelectFirstFilteredItem();
};

const autoSelectFirstFilteredItem = () => {
  if (filteredMenuItems.value.length > 0) {
    // Check if the currently selected item is still visible in the new filtered list
    const currentStillVisible = filteredMenuItems.value.some(i => i.id === selectedItemId.value);

    // If it disappeared because of the filter, auto-select the first available one!
    if (!currentStillVisible) {
      selectItem(filteredMenuItems.value[0].id);
    }
  } else {
    // Nothing matches the filter
    selectedItemId.value = null;
    analyticsData.value = {};
  }
};

// Chart Computed Properties
const correctQuestionOrder = [
  'Which emotion or physical state most strongly makes you want to order this item?',
  'In what weather condition does this item feel most satisfying?',
  'What is the "vibe" of this specific dish?',
  'Looking at this item, what do you think is a fair "Student-Friendly" price for it?'
];

const radioQuestions = computed(() => {
  const result: Record<string, any> = {};

  // 1. Loop through YOUR specific order first
  correctQuestionOrder.forEach(qTitle => {
    const answers = analyticsData.value[qTitle];
    if (answers && answers.length > 0 && answers[0].voteCount !== undefined) {
      result[qTitle] = answers;
    }
  });

  // 2. Catch any extra questions just in case
  Object.entries(analyticsData.value).forEach(([qTitle, answers]) => {
    if (!correctQuestionOrder.includes(qTitle)) {
      if (answers && answers.length > 0 && answers[0].voteCount !== undefined) {
        result[qTitle] = answers;
      }
    }
  });

  return result;
});

const textQuestions = computed(() => {
  const result: Record<string, any> = {};
  Object.entries(analyticsData.value).forEach(([q, ans]) => {
    if (ans && ans.length > 0 && ans[0].voteCount === undefined) result[q] = ans;
  });
  return result;
});

const totalQuestions = computed(() => Object.keys(analyticsData.value).length);
const radioQuestionsCount = computed(() => Object.keys(radioQuestions.value).length);

const totalResponses = computed(() => {
  const firstQ = Object.values(radioQuestions.value)[0];
  if (!firstQ) return 0;
  return firstQ.reduce((sum: number, stat: any) => sum + Number(stat.voteCount), 0);
});

const calculatePercentage = (votes: number, allStats: any[]) => {
  const total = allStats.reduce((sum, stat) => sum + Number(stat.voteCount), 0);
  if (total === 0) return 0;
  return Math.round((votes / total) * 100);
};

const topResponse = (answers: any[]) => {
  if (!answers || answers.length === 0) return 'N/A';
  const highest = answers.reduce((prev, current) => (Number(prev.voteCount) > Number(current.voteCount)) ? prev : current);
  return highest.optionLabel;
};

const getSentimentData = (feedback) => {
  const text = (feedback.response || feedback.textResponse || (typeof feedback === 'string' ? feedback: '')).toLowerCase();
  if(!text) return { class: 'neu', icon: '-', label: 'Neutral' };

  const positiveWords = ['good', 'great', 'love', 'best', 'delicious', 'yummy', 'perfect', 'nice', 'amazing', 'sweet', 'comfort', 'favorite', 'warm', 'fresh', 'hot', 'filling'];
  const negativeWords = ['bad', 'hate', 'awful', 'terrible', 'gross', 'expensive', 'worse', 'bland', 'nasty', 'disgusting', 'dry', 'salty', 'cold', 'hard', 'stale'];

  const isPositive = positiveWords.some(word => text.includes(word));
  const isNegative = negativeWords.some(word => text.includes(word));

  if(isNegative) return { class: 'neg', icon: '👎', label: 'Negative' };
  if(isPositive) return { class: 'pos', icon: '👍', label: 'Positive' };

  return { class: 'neu', icon: '-', label: 'Neutral'};
}

const downloadReport = async () => {
  try {
    // We tell Axios we are expecting a 'blob' (a file), not just standard JSON
    const response = await axios.get('http://localhost:8080/export', {
      responseType: 'blob'
    });

    // Create a temporary hidden link in the browser to force the download
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', 'CafeRater_Report.csv');
    document.body.appendChild(link);
    link.click();

    // Clean up the link after the download starts
    document.body.removeChild(link);
  } catch (error) {
    console.error("Error downloading report:", error);
    alert("Oops! Could not export the report right now.");
  }
};

const clearAllData = async () => {
  try {
    await axios.delete('http://localhost:8080/api/admin/clear-data');
    showClearModal.value = false;
    window.location.reload();
  } catch(error) {
    console.error("Error clearing data:", error);
    alert("Oops! Could not clear the database.");
  }
};

const globalTotal = ref(0);
const itemTotal = ref(0);
const engagementPct = ref(0);
const topKeywords = ref([]);
const sentiment = ref({pos: 0, neu: 0, neg: 0, posPct: 0, neuPct: 0, negPct: 0});

const getWordClass = (index) => {
  if(index < 2) return 'w-huge';
  if(index < 4) return 'w-large';
  if(index < 6) return 'w-med';
  return 'w-small';
};

const fetchItemStats = async (menuItemId) => {
  try {
    const response = await axios.get(`http://localhost:8080/analytics/stats/${menuItemId}`);
    const data = response.data;

    globalTotal.value = data.globalTotal;
    itemTotal.value = data.itemTotal;
    engagementPct.value = data.engagementPct;

    sentiment.value = {
      pos: data.positiveCount,
      neu: data.neutralCount,
      neg: data.negativeCount,
      posPct: data.positivePct,
      neuPct: data.neutralPct,
      negPct: data.negativePct,
    };

    topKeywords.value = data.topKeywords || [];
  } catch (error) {
    console.error("Failed to load real stats", error);
  }
};

// UPDATED: Now fetches from your newly built baseline endpoint
const fetchStats = async () => {
  try {
    const response = await axios.get('http://localhost:8080/api/stats/baseline');
    baselineCount.value = response.data;
  } catch (error) {
    console.error("Error fetching baseline count:", error);
  }
};

onMounted(() => {
  fetchMenuItems();
  fetchStats();
});
</script>

<style scoped>
/* GLOBALS & HEADER */
.dashboard-layout { background-color: #f4f7fa; min-height: 100vh; padding: 30px 50px; font-family: 'Inter', -apple-system, sans-serif; color: #1e293b; }
.top-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 30px; }
.header-left { display: flex; align-items: center; gap: 15px; }
.logo-icon { background: #f97316; color: white; width: 45px; height: 45px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 1.5rem; }
.top-header h1 { margin: 0 0 5px 0; font-size: 1.5rem; color: #0f172a; }
.subtitle { margin: 0; color: #64748b; font-size: 0.9rem; }
.header-right { display: flex; gap: 15px; align-items: center; }
.live-badge { background: #ecfdf5; color: #10b981; padding: 8px 16px; border-radius: 20px; font-weight: 600; font-size: 0.9rem; display: flex; align-items: center; gap: 8px; border: 1px solid #a7f3d0; }
.live-badge .dot { width: 8px; height: 8px; background: #10b981; border-radius: 50%; }
.export-btn { background: #1e293b; color: white; border: none; padding: 10px 20px; border-radius: 8px; font-weight: 500; cursor: pointer; }

.truncate-text { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 100%; }

/* KPI CARDS */
.kpi-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 20px; margin-bottom: 25px; }
.kpi-card { background: white; border-radius: 16px; box-shadow: 0 2px 10px rgba(0,0,0,0.02); display: flex; flex-direction: column; border: 1px solid #e2e8f0; overflow: hidden; }
.kpi-tag { padding: 8px 15px; border-bottom: 1px solid #f1f5f9; font-size: 0.7rem; font-weight: 700; color: #94a3b8; letter-spacing: 0.5px; display: flex; align-items: center; gap: 6px; }
.t-dot { width: 6px; height: 6px; border-radius: 50%; }
.t-dot.blue { background: #3b82f6; } .t-dot.teal { background: #0d9488; } .t-dot.orange { background: #ea580c; }
.kpi-body { padding: 15px; display: flex; align-items: flex-start; gap: 15px; }
.kpi-icon { width: 42px; height: 42px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 1.2rem; flex-shrink: 0; }
.kpi-icon.blue { background: #eff6ff; color: #3b82f6; } .kpi-icon.teal { background: #f0fdfa; color: #0d9488; } .kpi-icon.orange { background: #fff7ed; color: #ea580c; } .kpi-icon.green { background: #f0fdf4; color: #16a34a; }
.kpi-content h2 { margin: 4px 0; font-size: 1.6rem; color: #0f172a; font-weight: 700;}
.kpi-label { margin: 0; font-size: 0.75rem; color: #64748b; font-weight: 600; letter-spacing: 0.5px; }
.kpi-subtext { margin: 0; font-size: 0.75rem; color: #94a3b8; }

/* OPTION 1 NAVIGATION PANEL */
.navigation-panel { background: white; border-radius: 16px; margin-bottom: 30px; border: 1px solid #e2e8f0; box-shadow: 0 2px 10px rgba(0,0,0,0.02); overflow: hidden; }
.filters-row { display: flex; align-items: center; padding: 15px 20px; border-bottom: 1px solid #f1f5f9; background: #fafbfc; gap: 20px;}

.category-toggle { display: flex; background: #e2e8f0; border-radius: 8px; padding: 4px; }
.category-toggle button { background: transparent; border: none; padding: 6px 16px; border-radius: 6px; font-weight: 600; color: #64748b; cursor: pointer; transition: all 0.2s; }
.category-toggle button.active { background: white; color: #0f172a; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }

.divider { width: 1px; height: 24px; background: #cbd5e1; }

.subcategory-pills { display: flex; gap: 10px; overflow-x: auto; padding-bottom: 2px;}
.subcategory-pills::-webkit-scrollbar { display: none; }

/* Base pill styling */
.f-pill { padding: 6px 14px; border-radius: 20px; font-size: 0.85rem; font-weight: 600; cursor: pointer; transition: all 0.2s; white-space: nowrap; border: 1px solid; }

/* Meal: Savory Red */
.pill-meal { background: #fef2f2; border-color: #fecaca; color: #b91c1c; }
.pill-meal:hover { background: #fee2e2; }
.pill-meal.active { background: #fca5a5; color: #7f1d1d; border-color: #f87171; }

/* Bread: Toasted Wheat/Tan */
.pill-bread { background: #fdf5e6; border-color: #ebd5b3; color: #8b5a2b; }
.pill-bread:hover { background: #faebd7; }
.pill-bread.active { background: #deb887; color: #5c3317; border-color: #cdaa7d; }

/* Pasta: Bright Butter Yellow */
.pill-pasta { background: #fefce8; border-color: #fde047; color: #854d0e; }
.pill-pasta:hover { background: #fef9c3; }
.pill-pasta.active { background: #facc15; color: #422006; border-color: #eab308; }

/* Waffle: Sweet Orange */
.pill-waffle { background: #fff7ed; border-color: #fed7aa; color: #c2410c; }
.pill-waffle:hover { background: #ffedd5; }
.pill-waffle.active { background: #fdba74; color: #9a3412; border-color: #f97316; }

/* Food Colors (Orange/Yellow) */
.pill-food { background: #fff7ed; border-color: #fed7aa; color: #c2410c; }
.pill-food:hover { background: #ffedd5; }
.pill-food.active { background: #fdba74; color: #9a3412; border-color: #f97316; }

/* Drink Colors (Matched to Figma!) */
.pill-coffee { background: #fffbeb; border-color: #fde68a; color: #b45309; }
.pill-coffee:hover { background: #fef3c7; }
.pill-coffee.active { background: #fcd34d; color: #78350f; border-color: #f59e0b; }

.pill-noncoffee { background: #f0f9ff; border-color: #bae6fd; color: #0284c7; }
.pill-noncoffee:hover { background: #e0f2fe; }
.pill-noncoffee.active { background: #7dd3fc; color: #0369a1; border-color: #0ea5e9; }

.pill-frappe { background: #f5f3ff; border-color: #ddd6fe; color: #7c3aed; }
.pill-frappe:hover { background: #ede9fe; }
.pill-frappe.active { background: #c4b5fd; color: #5b21b6; border-color: #8b5cf6; }

.pill-float { background: #ecfdf5; border-color: #a7f3d0; color: #059669; }
.pill-float:hover { background: #d1fae5; }
.pill-float.active { background: #6ee7b7; color: #064e3b; border-color: #10b981; }

.pill-soda { background: #ecfeff; border-color: #a5f3fc; color: #0891b2; }
.pill-soda:hover { background: #cffafe; }
.pill-soda.active { background: #67e8f9; color: #164e63; border-color: #06b6d4; }

.pill-milktea { background: #fdf4ff; border-color: #f5d0fe; color: #c026d3; }
.pill-milktea:hover { background: #fae8ff; }
.pill-milktea.active { background: #f0abfc; color: #86198f; border-color: #d946ef; }

.pill-fruittea { background: #fff1f2; border-color: #fecdd3; color: #e11d48; }
.pill-fruittea:hover { background: #ffe4e6; }
.pill-fruittea.active { background: #fda4af; color: #9f1239; border-color: #f43f5e; }

.item-tabs-container { display: flex; gap: 5px; padding: 15px 20px; overflow-x: auto; }
.empty-filter { padding: 20px; color: #94a3b8; font-style: italic; font-size: 0.9rem; }
.item-tab { display: flex; flex-direction: column; align-items: center; min-width: 100px; padding: 10px; cursor: pointer; border-bottom: 3px solid transparent; transition: all 0.2s; opacity: 0.6; }
.item-tab:hover { opacity: 0.9; }
.item-tab.active { opacity: 1; border-bottom-color: #f97316; }
.tab-thumb { width: 45px; height: 45px; border-radius: 10px; background-size: cover; background-position: center; border: 1px solid #e2e8f0; margin-bottom: 8px;}
.tab-title { font-size: 0.85rem; font-weight: 600; color: #0f172a; max-width: 110px; text-align: center; }
.item-tab.active .tab-title { color: #ea580c; }
.tab-cat { font-size: 0.7rem; color: #94a3b8; }
.item-tab.active .tab-cat { color: #fb923c; }

/* STATES */
.state-message { text-align: center; padding: 80px 20px; color: #64748b; }
.state-message.empty h2 { color: #0f172a; font-size: 2rem; margin-bottom: 10px; }
.state-message.empty p { font-size: 1.1rem; }
.badge-v2 { padding: 4px 10px; border-radius: 6px; font-size: 0.75rem; font-weight: 600; }
.badge-v2.food-badge { background: #ffedd5; color: #c2410c; }
.badge-v2.type-badge { background: rgba(0,0,0,0.6); color: white; border: 1px solid rgba(255,255,255,0.2); backdrop-filter: blur(4px); }
.q-circle { position: absolute; bottom: 15px; right: 15px; background: rgba(255,255,255,0.2); color: white; width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 0.85rem; font-weight: 700; z-index: 3; backdrop-filter: blur(4px); border: 1px solid rgba(255,255,255,0.3); }

/* RADIO CARDS */
.cards-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 25px; }
.insight-card { background: white; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.03); border: 1px solid #e2e8f0; }
.radio-card-v2 { border-top: 4px solid #f97316; }
.card-image-header-v2 { height: 180px; background-color: #e2e8f0; background-image: url('https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&q=80&w=800'); background-size: cover; background-position: center; position: relative; display: flex; align-items: flex-end; padding: 20px; }
.card-image-header-v2::before { content: ''; position: absolute; bottom: 0; left: 0; right: 0; height: 70%; background: linear-gradient(to top, rgba(0,0,0,0.8), transparent); z-index: 1;}
.image-overlay-v2 { z-index: 2; width: 100%; }
.image-overlay-v2 h3 { color: white; margin: 0 0 8px 0; font-size: 1.3rem; font-weight: 700; text-shadow: 0 2px 4px rgba(0,0,0,0.5); }
.card-body { padding: 25px; }
.insight-card .card-body { display: block; }
.question-title { margin: 0 0 5px 0; font-size: 1.1rem; color: #0f172a; }
.response-count { margin: 0 0 20px 0; font-size: 0.85rem; color: #94a3b8; }
.bar-row { display: flex; align-items: center; gap: 15px; margin-bottom: 12px; }
.bar-label { width: 100px; font-size: 0.9rem; color: #475569; text-align: right; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.bar-track-v2 { flex-grow: 1; height: 28px; background: #fff7ed; border-radius: 8px; overflow: hidden; }
.bar-fill.orange-solid { height: 100%; border-radius: 8px; transition: width 1s ease; background: #f97316; }
.bar-value { width: 30px; font-weight: 600; font-size: 0.9rem; color: #0f172a; text-align: right;}
.bar-percent { width: 40px; font-size: 0.85rem; color: #94a3b8; text-align: right;}
.key-insight-v2 { margin-top: 25px; background: #fff7ed; padding: 12px 15px; border-radius: 8px; color: #c2410c; font-size: 0.9rem; display: flex; align-items: center; gap: 10px; }

/* TEXT CARDS */
.text-card-v2 { grid-column: 1 / -1; display: flex; flex-direction: column; border-top: 4px solid #f97316; }
.text-top-row { display: grid; grid-template-columns: 280px 1.5fr 1fr; border-bottom: 1px solid #f1f5f9; }
.text-col-image { position: relative; background-image: url('https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&q=80&w=800'); background-size: cover; background-position: center; min-height: 250px; display: flex; align-items: flex-end; padding: 20px; }
.text-col-image::before { content: ''; position: absolute; bottom: 0; left: 0; right: 0; height: 60%; background: linear-gradient(to top, rgba(0,0,0,0.85), transparent); }
.text-col-image::after { content: ''; position: absolute; top: 0; left: 0; right: 0; height: 30%; background: linear-gradient(to bottom, rgba(0,0,0,0.3), transparent); }
.text-col-image .q-circle { top: 15px; right: 15px; bottom: auto; background: rgba(255,255,255,0.8); color: #94a3b8; border: none;}
.badge-row { display: flex; gap: 8px; }
.text-col-sentiment { padding: 25px; border-right: 1px solid #f1f5f9; }
.question-title-v2 { margin: 0 0 5px 0; font-size: 1.1rem; color: #0f172a; font-weight: 600; }
.response-count-v2 { margin: 0 0 20px 0; font-size: 0.85rem; color: #94a3b8; }
.section-label { font-size: 0.75rem; font-weight: 600; color: #64748b; letter-spacing: 0.5px; margin-bottom: 12px; text-transform: uppercase; }
.section-subtext { font-size: 0.7rem; color: #94a3b8; margin-top: -8px; margin-bottom: 12px; }
.sent-boxes-v2 { display: flex; gap: 10px; margin-bottom: 20px; }
.s-box-v2 { flex: 1; text-align: center; padding: 12px 10px; border-radius: 8px; border: 1px solid #f1f5f9; }
.s-box-v2 h2 { margin: 0; font-size: 1.6rem; font-weight: 700;}
.s-box-v2 span { font-size: 0.8rem; font-weight: 500;}
.s-box-v2.pos { background: #f0fdf4; color: #16a34a; border-color: #dcfce7; }
.s-box-v2.neu { background: #f8fafc; color: #64748b; }
.s-box-v2.neg { background: #fef2f2; color: #ef4444; border-color: #fee2e2; }
.sent-bar-thick { height: 16px; display: flex; border-radius: 8px; overflow: hidden; gap: 0px; margin-bottom: 12px; }
.s-fill-thick { height: 100%; }
.s-fill-thick.pos { background: #22c55e; }
.s-fill-thick.neu { background: #94a3b8; }
.s-fill-thick.neg { background: #ef4444; }
.sent-legend { display: flex; gap: 15px; font-size: 0.8rem; font-weight: 500; }
.l-pos { color: #16a34a; } .l-neu { color: #64748b; } .l-neg { color: #ef4444; } .l-total { color: #94a3b8; margin-left: auto; }
.text-col-keywords { padding: 25px; display: flex; flex-direction: column; }
.word-cloud-v2 { flex-grow: 1; display: flex; flex-wrap: wrap; align-content: flex-start; gap: 12px; margin-top: 10px; }
.word-cloud-v2 span { font-weight: 700; color: #f97316; cursor: pointer; transition: opacity 0.2s; }
.word-cloud-v2 span:hover { opacity: 0.7; }
.w-huge { font-size: 1.8rem; } .w-large { font-size: 1.3rem; opacity: 0.8; } .w-med { font-size: 1.05rem; opacity: 0.6; } .w-small { font-size: 0.85rem; opacity: 0.4; color: #94a3b8 !important;}

/* Excerpts */
.text-bottom-row { padding: 25px; background: white; }
.excerpt-header-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.header-left-side { display: flex; align-items: center; gap: 10px; }
.mb-0 { margin-bottom: 0 !important; }
.excerpt-count { font-size: 0.8rem; color: #64748b; background: #f1f5f9; padding: 2px 8px; border-radius: 10px; font-weight: 500;}
.excerpt-filters { display: flex; gap: 8px; }
.f-btn { padding: 6px 14px; border-radius: 20px; font-size: 0.8rem; font-weight: 600; cursor: pointer; border: 1px solid #e2e8f0; background: white; color: #64748b; transition: all 0.2s;}
.f-btn:hover { background: #f8fafc; }
.f-btn.active { background: #0f172a; color: white; border-color: #0f172a; }
.pos-btn { color: #16a34a; border-color: #dcfce7; } .pos-btn:hover { background: #f0fdf4; }
.neu-btn { color: #64748b; border-color: #e2e8f0; } .neu-btn:hover { background: #f8fafc; }
.neg-btn { color: #ef4444; border-color: #fee2e2; } .neg-btn:hover { background: #fef2f2; }
.excerpt-grid-v2 { display: grid; grid-template-columns: repeat(4, 1fr); gap: 15px; }
.exc-card { background: #f8fafc; padding: 20px; border-radius: 12px; display: flex; flex-direction: column; justify-content: space-between; border: 1px solid #e2e8f0; }
.exc-text { margin: 0 0 15px 0; font-size: 0.9rem; color: #334155; line-height: 1.5; }
.exc-footer { display: flex; }
.exc-tag { font-size: 0.8rem; font-weight: 600; display: flex; align-items: center; gap: 5px; }
.exc-tag.pos { color: #16a34a; } .exc-tag.neu { color: #64748b; } .exc-tag.neg { color: #ef4444; }

/* NOTE: Added a missing class that your HTML was using for KPI Card 1 */
.stat-card { background: white; border-radius: 16px; box-shadow: 0 2px 10px rgba(0,0,0,0.02); display: flex; flex-direction: column; border: 1px solid #e2e8f0; overflow: hidden; }
.card-header { padding: 8px 15px; border-bottom: 1px solid #f1f5f9; font-size: 0.7rem; font-weight: 700; color: #94a3b8; letter-spacing: 0.5px; display: flex; align-items: center; gap: 6px; }
.dot { width: 6px; height: 6px; border-radius: 50%; }
.dot.blue { background: #3b82f6; }
.card-body { padding: 15px; display: flex; align-items: flex-start; gap: 15px; }
.icon-wrapper { width: 42px; height: 42px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 1.2rem; flex-shrink: 0; background: #eff6ff; color: #3b82f6;}
.stat-info { display: flex; flex-direction: column; }
.stat-label { font-size: 0.75rem; color: #64748b; font-weight: 600; letter-spacing: 0.5px; margin-bottom: 4px; }
.stat-value { margin: 0; font-size: 1.6rem; font-weight: 700; color: #0f172a; line-height: 1; display: flex; align-items: baseline; gap: 6px;}
.limit-text { font-size: 1rem; color: #94a3b8; font-weight: 600; }
.stat-subtext { font-size: 0.75rem; color: #94a3b8; margin-top: 6px;}
.text-success { color: #10b981; font-weight: 600; }

/* CLEAR DATABASE */
.danger-btn { background: white; color: #ef4444; border: 1px solid #fca5a5; padding: 10px 20px; border-radius: 8px; font-weight: 600; cursor: pointer; transition: all 0.2s; margin-right: 10px; }
.danger-btn:hover { background: #fef2f2; border-color: #ef4444; }

.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(15, 23, 42, 0.6); backdrop-filter: blur(5px); display: flex; align-items: center; justify-content: center; z-index: 9999; }
.modal-card { background: white; padding: 40px; border-radius: 24px; text-align: center; max-width: 450px; width: 90%; margin: auto; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25); animation: popIn 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275) forwards; }
.modal-icon { width: 70px; height: 70px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 2.5rem; margin: 0 auto 15px auto; }
.modal-card h2 { margin: 0 0 15px 0; color: #0f172a; font-size: 1.8rem; font-weight: 800; }
.modal-card p { color: #64748b; line-height: 1.6; margin-bottom: 30px; font-size: 1.05rem; }
.modal-actions { display: flex; gap: 15px; justify-content: center; margin-top: 10px; }
.nav-btn { padding: 14px 24px; border-radius: 10px; font-weight: 700; font-size: 1rem; cursor: pointer; transition: all 0.2s; border: none; flex: 1; }
.nav-btn.secondary { background: white; border: 1px solid #cbd5e1; color: #475569; }
.nav-btn.secondary:hover { background: #f1f5f9; }
.danger-solid { background: #ef4444; color: white; }
.danger-solid:hover { background: #dc2626; }
@keyframes popIn { 0% { transform: scale(0.8); opacity: 0; } 100% { transform: scale(1); opacity: 1; } }
</style>
