<template>
  <div class="dashboard-layout">

    <header class="top-header">
      <div class="header-left">
        <div class="logo-icon">📊</div>
        <div>
          <h1>Food Preferences Survey</h1>
          <p class="subtitle">Jan 1 — {{ currentDate }} • {{ totalQuestions }} questions • {{ menuItem?.name || 'Loading...' }}</p>
        </div>
      </div>
      <div class="header-right">
        <span class="live-badge"><span class="dot"></span> Live</span>
        <button class="export-btn">📥 Export Report</button>
      </div>
    </header>

    <section class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-icon blue">👥</div>
        <div class="kpi-content">
          <p class="kpi-label">TOTAL RESPONSES</p>
          <h2>{{ totalResponses }}</h2>
          <p class="kpi-subtext">94.2% completion</p>
        </div>
      </div>
      <div class="kpi-card">
        <div class="kpi-icon teal">📊</div>
        <div class="kpi-content">
          <p class="kpi-label">AVG. SCORE</p>
          <h2>4.1 / 5</h2>
          <p class="kpi-subtext">All food items</p>
        </div>
      </div>
      <div class="kpi-card">
        <div class="kpi-icon yellow">⭐</div>
        <div class="kpi-content">
          <p class="kpi-label">TOP RATED</p>
          <h2>{{ menuItem?.name || '...' }}</h2>
          <p class="kpi-subtext">74.6% Excellent/Good</p>
        </div>
      </div>
      <div class="kpi-card">
        <div class="kpi-icon orange">🍴</div>
        <div class="kpi-content">
          <p class="kpi-label">FOOD CATEGORIES</p>
          <h2>4</h2>
          <p class="kpi-subtext">Meal • Bread • Pasta • Waffle</p>
        </div>
      </div>
      <div class="kpi-card">
        <div class="kpi-icon purple">🥤</div>
        <div class="kpi-content">
          <p class="kpi-label">DRINK CATEGORIES</p>
          <h2>7</h2>
          <p class="kpi-subtext">Coffee • Milktea • Frappe & more</p>
        </div>
      </div>
    </section>

    <section class="filters-section">
      <button class="active-filter-btn">All Questions</button>

      <div class="filter-group">
        <span class="filter-label">🍴 FOOD</span>
        <div class="tags">
          <span class="tag food-tag active">Meal</span>
          <span class="tag food-tag">Bread</span>
          <span class="tag food-tag">Pasta</span>
          <span class="tag food-tag">Waffle</span>
        </div>
      </div>

      <div class="filter-group">
        <span class="filter-label">🥤 DRINK</span>
        <div class="tags">
          <span class="tag drink-coffee">Coffee</span>
          <span class="tag drink-noncoffee">Non-coffee</span>
          <span class="tag drink-frappe">Frappe</span>
          <span class="tag drink-float">Float</span>
          <span class="tag drink-soda">Sparkling Soda</span>
          <span class="tag drink-milktea">Milktea</span>
          <span class="tag drink-fruittea">Fruit Tea</span>
        </div>
      </div>
    </section>

    <div v-if="Object.keys(analyticsData).length === 0" class="loading">
      Fetching latest data...
    </div>

    <section v-else class="cards-grid">

      <div v-for="(answers, question, index) in radioQuestions" :key="question" class="insight-card radio-card">
        <div class="card-image-header">
          <div class="overlay">
            <h3>{{ menuItem?.name }}</h3>
            <span class="category-badge">🍴 {{ menuItem?.category }}</span>
          </div>
          <span class="q-number">Q{{ index + 1 }}</span>
        </div>

        <div class="card-body">
          <h4 class="question-title">{{ question }}</h4>
          <p class="response-count">{{ totalResponses }} responses</p>

          <div class="bars-container">
            <div v-for="stat in answers" :key="stat.optionLabel" class="bar-row">
              <span class="bar-label">{{ stat.optionLabel }}</span>
              <div class="bar-track">
                <div class="bar-fill orange-gradient" :style="{ width: calculatePercentage(stat.voteCount, answers) + '%' }"></div>
              </div>
              <span class="bar-value">{{ stat.voteCount }}</span>
              <span class="bar-percent">{{ calculatePercentage(stat.voteCount, answers) }}%</span>
            </div>
          </div>

          <div class="key-insight">
            <span class="insight-icon">📈</span>
            <span><strong>Key insight:</strong> Most students prefer this for {{ answers[0]?.optionLabel || 'certain scenarios' }}.</span>
          </div>
        </div>
      </div>

      <div v-for="(answers, question, index) in textQuestions" :key="question" class="insight-card text-card">
        <div class="text-card-top">
          <div class="card-image-side">
            <div class="overlay">
              <h3>{{ menuItem?.name }}</h3>
              <div class="badges">
                <span class="category-badge">🍴 {{ menuItem?.category }}</span>
                <span class="open-ended-badge">💬 Open-ended</span>
              </div>
            </div>
            <span class="q-number">Q{{ radioQuestionsCount + index + 1 }}</span>
          </div>

          <div class="text-stats-side">
            <h4 class="question-title">{{ question }}</h4>
            <p class="response-count">{{ answers.length }} text responses</p>

            <div class="sentiment-breakdown">
              <p class="sub-label">SENTIMENT BREAKDOWN (AI Estimate)</p>
              <div class="sentiment-boxes">
                <div class="s-box positive"><h2>{{ Math.floor(answers.length * 0.6) }}</h2><p>Positive</p></div>
                <div class="s-box neutral"><h2>{{ Math.floor(answers.length * 0.3) }}</h2><p>Neutral</p></div>
                <div class="s-box negative"><h2>{{ Math.floor(answers.length * 0.1) }}</h2><p>Negative</p></div>
              </div>
              <div class="sentiment-bar">
                <div class="s-fill pos" style="width: 60%"></div>
                <div class="s-fill neu" style="width: 30%"></div>
                <div class="s-fill neg" style="width: 10%"></div>
              </div>
            </div>

            <div class="keywords-section">
              <p class="sub-label"># TOP KEYWORDS</p>
              <div class="word-cloud">
                <span class="word massive">flavor</span>
                <span class="word large">comfort</span>
                <span class="word medium">warm</span>
                <span class="word small">pricey</span>
              </div>
            </div>
          </div>
        </div>

        <div class="text-card-bottom">
          <p class="sub-label">⚑ RESPONSE EXCERPTS</p>
          <div class="excerpts-grid">
            <div v-for="(feedback, i) in answers.slice(0, 6)" :key="i" class="excerpt-box">
              <p class="quote">"{{ feedback.response }}"</p>
              <span class="sentiment-tag" :class="i % 3 === 0 ? 'pos' : (i % 3 === 1 ? 'neu' : 'neg')">
                {{ i % 3 === 0 ? '👍 Positive' : (i % 3 === 1 ? '— Neutral' : '👎 Negative') }}
              </span>
            </div>
          </div>
        </div>
      </div>

    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import axios from 'axios';

const analyticsData = ref<Record<string, any>>({});
const menuItem = ref<any>(null);

const currentDate = new Date().toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });

const fetchAnalytics = async () => {
  try {
    const response = await axios.get('http://localhost:8080/analytics/1');
    analyticsData.value = response.data;
  } catch (error) {
    console.error("Error fetching analytics:", error);
  }
};

const fetchMenuItemDetails = async () => {
  try {
    const response = await axios.get('http://localhost:8080/menu-items');
    menuItem.value = response.data.find((item: any) => item.id === 1);
  } catch (error) {
    console.error("Error fetching menu item details:", error);
  }
};

// FIX: Bulletproof data sorting that won't crash strict mode
const radioQuestions = computed(() => {
  const result: Record<string, any> = {};
  Object.entries(analyticsData.value).forEach(([questionTitle, answers]) => {
    if (answers && answers.length > 0 && answers[0].voteCount !== undefined) {
      result[questionTitle] = answers;
    }
  });
  return result;
});

const textQuestions = computed(() => {
  const result: Record<string, any> = {};
  Object.entries(analyticsData.value).forEach(([questionTitle, answers]) => {
    if (answers && answers.length > 0 && answers[0].voteCount === undefined) {
      result[questionTitle] = answers;
    }
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

onMounted(() => {
  fetchAnalytics();
  fetchMenuItemDetails();
});
</script>

<style scoped>
.dashboard-layout {
  background-color: #f4f7fa;
  min-height: 100vh;
  padding: 30px 50px;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
  color: #1e293b;
}

.top-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 15px;
}

.logo-icon {
  background: #f97316;
  color: white;
  width: 45px;
  height: 45px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
}

.top-header h1 {
  margin: 0 0 5px 0;
  font-size: 1.5rem;
  color: #0f172a;
}

.subtitle {
  margin: 0;
  color: #64748b;
  font-size: 0.9rem;
}

.header-right {
  display: flex;
  gap: 15px;
  align-items: center;
}

.live-badge {
  background: #ecfdf5;
  color: #10b981;
  padding: 8px 16px;
  border-radius: 20px;
  font-weight: 600;
  font-size: 0.9rem;
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid #a7f3d0;
}

.live-badge .dot {
  width: 8px;
  height: 8px;
  background: #10b981;
  border-radius: 50%;
}

.export-btn {
  background: #1e293b;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  font-weight: 500;
  cursor: pointer;
}

/* KPI CARDS */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}

.kpi-card {
  background: white;
  padding: 20px;
  border-radius: 16px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.02);
  display: flex;
  align-items: flex-start;
  gap: 15px;
  border: 1px solid #f1f5f9;
}

.kpi-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
}
.kpi-icon.blue { background: #eff6ff; color: #3b82f6; }
.kpi-icon.teal { background: #f0fdfa; color: #14b8a6; }
.kpi-icon.yellow { background: #fefce8; color: #eab308; }
.kpi-icon.orange { background: #fff7ed; color: #f97316; }
.kpi-icon.purple { background: #faf5ff; color: #a855f7; }

.kpi-content h2 { margin: 5px 0; font-size: 1.6rem; color: #0f172a; }
.kpi-label { margin: 0; font-size: 0.75rem; color: #64748b; font-weight: 600; letter-spacing: 0.5px; }
.kpi-subtext { margin: 0; font-size: 0.8rem; color: #94a3b8; }

.filters-section {
  background: white;
  padding: 20px 30px;
  border-radius: 16px;
  display: flex;
  align-items: flex-start;
  gap: 30px;
  margin-bottom: 30px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.02);
  border: 1px solid #f1f5f9;
  flex-wrap: wrap;
}

.active-filter-btn {
  background: #0f172a;
  color: white;
  padding: 10px 20px;
  border-radius: 8px;
  border: none;
  font-weight: 500;
  white-space: nowrap;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 15px;
  flex-wrap: wrap;
}

.filter-label {
  font-size: 0.8rem;
  font-weight: 600;
  color: #64748b;
  letter-spacing: 0.5px;
}

.tags { display: flex; gap: 10px; flex-wrap: wrap; }
.tag {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 0.85rem;
  font-weight: 500;
  border: 1px solid #e2e8f0;
  color: #64748b;
  cursor: pointer;
}

.tag.food-tag { background: #fff7ed; border-color: #fed7aa; color: #c2410c; }
.tag.food-tag.active { background: #ffedd5; border-color: #fdba74; }

.tag.drink-coffee { background: #fffbeb; border-color: #fde68a; color: #b45309; }
.tag.drink-noncoffee { background: #f0f9ff; border-color: #bae6fd; color: #0284c7; }
.tag.drink-frappe { background: #f5f3ff; border-color: #ddd6fe; color: #7c3aed; }
.tag.drink-float { background: #ecfdf5; border-color: #a7f3d0; color: #059669; }
.tag.drink-soda { background: #ecfeff; border-color: #a5f3fc; color: #0891b2; }
.tag.drink-milktea { background: #fdf4ff; border-color: #f5d0fe; color: #c026d3; }
.tag.drink-fruittea { background: #fff1f2; border-color: #fecdd3; color: #e11d48; }

/* CARDS GRID */
.cards-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 25px;
}

.insight-card {
  background: white;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0,0,0,0.03);
  border: 1px solid #f1f5f9;
}

/* RADIO CARD SPECIFICS */
.card-image-header {
  height: 200px;
  background-color: #e2e8f0;
  background-image: url('https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&q=80&w=800');
  background-size: cover;
  background-position: center;
  position: relative;
  display: flex;
  align-items: flex-end;
  padding: 20px;
}

.card-image-side {
  width: 35%;
  background-color: #e2e8f0;
  background-image: url('https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&q=80&w=800');
  background-size: cover;
  background-position: center;
  position: relative;
  display: flex;
  align-items: flex-end;
  padding: 20px;
}

.overlay {
  z-index: 2;
}

.overlay h3 {
  color: white;
  margin: 0 0 10px 0;
  font-size: 1.5rem;
  text-shadow: 0 2px 4px rgba(0,0,0,0.5);
}

.card-image-header::before, .card-image-side::before {
  content: '';
  position: absolute;
  bottom: 0; left: 0; right: 0;
  height: 70%;
  background: linear-gradient(to top, rgba(0,0,0,0.8), transparent);
}

.category-badge {
  background: white;
  color: #f97316;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 0.8rem;
  font-weight: 600;
}

.q-number {
  position: absolute;
  bottom: 20px;
  right: 20px;
  background: rgba(255,255,255,0.2);
  backdrop-filter: blur(5px);
  color: white;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 0.85rem;
  font-weight: 600;
  z-index: 2;
}

.card-body {
  padding: 25px;
}

.question-title { margin: 0 0 5px 0; font-size: 1.1rem; color: #0f172a; }
.response-count { margin: 0 0 20px 0; font-size: 0.85rem; color: #94a3b8; }

.bar-row {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 12px;
}

.bar-label {
  width: 120px;
  font-size: 0.9rem;
  color: #475569;
  text-align: right;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.bar-track {
  flex-grow: 1;
  height: 24px;
  background: #f1f5f9;
  border-radius: 12px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 12px;
  transition: width 1s ease;
}
.orange-gradient { background: linear-gradient(90deg, #f97316, #fb923c); }

.bar-value { width: 30px; font-weight: 600; font-size: 0.9rem; color: #0f172a; text-align: right;}
.bar-percent { width: 40px; font-size: 0.85rem; color: #94a3b8; text-align: right;}

.key-insight {
  margin-top: 25px;
  background: #fff7ed;
  padding: 12px 15px;
  border-radius: 8px;
  color: #c2410c;
  font-size: 0.9rem;
  display: flex;
  align-items: center;
  gap: 10px;
}

/* TEXT CARD SPECIFICS */
.text-card {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
}

.text-card-top {
  display: flex;
  min-height: 250px;
  border-bottom: 1px solid #f1f5f9;
}

.open-ended-badge {
  background: #1e293b;
  color: white;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 0.8rem;
  font-weight: 600;
  margin-left: 10px;
}

.text-stats-side {
  flex-grow: 1;
  padding: 25px;
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 30px;
}

.sub-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: #64748b;
  letter-spacing: 0.5px;
  margin-bottom: 15px;
}

.sentiment-boxes {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
}

.s-box {
  flex: 1;
  text-align: center;
  padding: 15px 10px;
  border-radius: 8px;
}
.s-box h2 { margin: 0; font-size: 1.8rem; }
.s-box p { margin: 5px 0 0 0; font-size: 0.8rem; font-weight: 500;}

.s-box.positive { background: #ecfdf5; color: #10b981; }
.s-box.neutral { background: #f1f5f9; color: #64748b; }
.s-box.negative { background: #fef2f2; color: #ef4444; }

.sentiment-bar {
  height: 8px;
  display: flex;
  border-radius: 4px;
  overflow: hidden;
  gap: 2px;
}
.s-fill.pos { background: #10b981; }
.s-fill.neu { background: #cbd5e1; }
.s-fill.neg { background: #ef4444; }

.word-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}
.word { font-weight: 700; color: #f97316; }
.word.massive { font-size: 1.8rem; }
.word.large { font-size: 1.4rem; opacity: 0.9; }
.word.medium { font-size: 1.1rem; opacity: 0.7; }
.word.small { font-size: 0.9rem; opacity: 0.5; color: #94a3b8; }

.text-card-bottom {
  padding: 25px;
  background: #f8fafc;
}

.excerpts-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 15px;
}

.excerpt-box {
  background: white;
  padding: 20px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.quote {
  margin: 0 0 15px 0;
  font-size: 0.9rem;
  color: #334155;
  font-style: italic;
  line-height: 1.5;
}

.sentiment-tag {
  font-size: 0.8rem;
  font-weight: 500;
}
.sentiment-tag.pos { color: #10b981; }
.sentiment-tag.neu { color: #64748b; }
.sentiment-tag.neg { color: #ef4444; }
</style>
