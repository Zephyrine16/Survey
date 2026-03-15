<template>
  <div class="dashboard-layout">

    <header class="top-header">
      <div class="header-left">
        <div class="logo-icon">📊</div>
        <div>
          <h1>Food Preferences Survey</h1>
          <p class="subtitle">Jan 1 — {{ currentDate }} • {{ totalQuestions }} questions • {{ menuItems.length }} food items</p>
        </div>
      </div>
      <div class="header-right">
        <span class="live-badge"><span class="dot"></span> Live</span>
        <button class="export-btn">📥 Export Report</button>
      </div>
    </header>

    <section class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-icon purple">👥</div>
        <div class="kpi-content">
          <p class="kpi-label">TOTAL RESPONSES</p>
          <h2>{{ totalResponses }}</h2>
          <p class="kpi-subtext">94.2% completion</p>
        </div>
      </div>
      <div class="kpi-card">
        <div class="kpi-icon orange">⭐</div>
        <div class="kpi-content">
          <p class="kpi-label">FAVORABLE RATING</p>
          <h2>74.6%</h2>
          <p class="kpi-subtext">Excellent + Good for {{ menuItem?.name?.split(' ')[0] || 'Item' }}</p>
        </div>
      </div>
      <div class="kpi-card">
        <div class="kpi-icon teal">😊</div>
        <div class="kpi-content">
          <p class="kpi-label">SATISFACTION</p>
          <h2>64.9%</h2>
          <p class="kpi-subtext">Satisfied or Very Satisfied</p>
        </div>
      </div>
      <div class="kpi-card">
        <div class="kpi-icon green">👍</div>
        <div class="kpi-content">
          <p class="kpi-label">POSITIVE SENTIMENT</p>
          <h2>67.2%</h2>
          <p class="kpi-subtext">Open-ended responses</p>
        </div>
      </div>
    </section>

    <section class="carousel-bar-v2" v-if="menuItems.length > 0">

      <div class="c-left" :class="{ disabled: currentIndex === 0 }" @click="prevItem">
        <div class="c-arrow">‹</div>
        <div class="c-text">
          <span class="c-label">Previous item</span>
          <span class="c-val truncate-text">{{ menuItems[currentIndex - 1]?.name || '—' }}</span>
        </div>
      </div>

      <div class="c-center">
        <div class="c-thumb" style="background-image: url('https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&q=80&w=150')"></div>
        <div class="c-info">
          <div class="c-title-row">
            <h3 class="truncate-text">{{ menuItem?.name }}</h3>
            <span class="badge-v2 food-badge">🍴 {{ menuItem?.category }}</span>
          </div>
          <div class="c-dots">
            <span v-for="(item, idx) in menuItems" :key="item.id" class="dot" :class="{ active: idx === currentIndex }"></span>
            <span class="c-count">{{ currentIndex + 1 }} of {{ menuItems.length }}</span>
          </div>
        </div>
      </div>

      <div class="c-right" :class="{ disabled: currentIndex === menuItems.length - 1 }" @click="nextItem">
        <div class="c-text">
          <span class="c-label">Next item</span>
          <span class="c-val truncate-text">{{ menuItems[currentIndex + 1]?.name || '—' }}</span>
        </div>
        <div class="c-arrow-btn">›</div>
      </div>

    </section>

    <div v-if="isLoading" class="state-message">
      <h2>Loading analytics for {{ menuItem?.name }}...</h2>
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
              <span class="bar-label">{{ stat.optionLabel }}</span>
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
              <div class="s-box-v2 pos">
                <h2>{{ Math.max(1, Math.floor(answers.length * 0.67)) }}</h2>
                <span>Positive</span>
              </div>
              <div class="s-box-v2 neu">
                <h2>{{ Math.floor(answers.length * 0.22) }}</h2>
                <span>Neutral</span>
              </div>
              <div class="s-box-v2 neg">
                <h2>{{ Math.floor(answers.length * 0.11) }}</h2>
                <span>Negative</span>
              </div>
            </div>
            <div class="sent-bar-v2">
              <div class="s-fill-v2 pos" style="width: 67.2%"></div>
              <div class="s-fill-v2 neu" style="width: 22.4%"></div>
              <div class="s-fill-v2 neg" style="width: 10.4%"></div>
            </div>
            <div class="sent-bar-labels">
              <span>67.2% positive</span>
              <span>10.4% negative</span>
            </div>
          </div>

          <div class="text-col-keywords">
            <p class="section-label"># TOP KEYWORDS</p>
            <div class="word-cloud-v2">
              <span class="w-huge">flavor</span>
              <span class="w-med">warm</span>
              <span class="w-large">comfort</span>
              <span class="w-small">salty</span>
              <span class="w-large">pricey</span>
              <span class="w-med">fresh</span>
              <span class="w-small">serving</span>
              <span class="w-huge">delicious</span>
              <span class="w-med">perfect</span>
            </div>
          </div>
        </div>

        <div class="text-bottom-row">
          <p class="section-label">⚑ RESPONSE EXCERPTS</p>
          <div class="excerpt-grid-v2">
            <div v-for="(feedback, i) in answers.slice(0, 8)" :key="i" class="exc-card">

              <p class="exc-text">
                "{{ feedback.response || feedback.textResponse || (typeof feedback === 'string' ? feedback : 'No text saved in database') }}"
              </p>

              <div class="exc-footer">
                <span class="exc-tag" :class="getSentimentData(i).class">
                  {{ getSentimentData(i).icon }} {{ getSentimentData(i).label }}
                </span>
              </div>
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

// Master State
const menuItems = ref<any[]>([]);
const currentIndex = ref(0);
const analyticsData = ref<Record<string, any>>({});
const isLoading = ref(true);

const currentDate = new Date().toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });

const menuItem = computed(() => menuItems.value[currentIndex.value]);

const fetchMenuItems = async () => {
  try {
    const response = await axios.get('http://localhost:8080/menu-items');
    menuItems.value = response.data;
    if (menuItems.value.length > 0) {
      await fetchAnalyticsForCurrentItem();
    }
  } catch (error) {
    console.error("Error fetching menu items:", error);
    isLoading.value = false;
  }
};

const fetchAnalyticsForCurrentItem = async () => {
  if (!menuItem.value) return;
  isLoading.value = true;
  try {
    const response = await axios.get(`http://localhost:8080/analytics/${menuItem.value.id}`);
    analyticsData.value = response.data;
  } catch (error) {
    console.error(`Error fetching analytics for item ${menuItem.value.id}:`, error);
    analyticsData.value = {};
  } finally {
    isLoading.value = false;
  }
};

const nextItem = async () => {
  if (currentIndex.value < menuItems.value.length - 1) {
    currentIndex.value++;
    await fetchAnalyticsForCurrentItem();
  }
};

const prevItem = async () => {
  if (currentIndex.value > 0) {
    currentIndex.value--;
    await fetchAnalyticsForCurrentItem();
  }
};

// Data Computed Properties
const radioQuestions = computed(() => {
  const result: Record<string, any> = {};
  Object.entries(analyticsData.value).forEach(([q, ans]) => {
    if (ans && ans.length > 0 && ans[0].voteCount !== undefined) result[q] = ans;
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

// Helper for Fake Sentiment Tags
const getSentimentData = (index: number) => {
  if (index % 3 === 0) return { class: 'pos', icon: '👍', label: 'Positive' };
  if (index % 3 === 1) return { class: 'pos', icon: '👍', label: 'Positive' };
  if (index % 3 === 2 && index % 2 === 0) return { class: 'neu', icon: '—', label: 'Neutral' };
  return { class: 'neg', icon: '👎', label: 'Negative' };
};

onMounted(() => {
  fetchMenuItems();
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

/* KPI CARDS (4 Column Grid now) */
.kpi-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 30px; }
.kpi-card { background: white; padding: 20px; border-radius: 16px; box-shadow: 0 2px 10px rgba(0,0,0,0.02); display: flex; align-items: flex-start; gap: 15px; border: 1px solid #e2e8f0; overflow: hidden; }
.kpi-icon { width: 45px; height: 45px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 1.3rem; flex-shrink: 0; }
.kpi-icon.purple { background: #f3e8ff; color: #9333ea; }
.kpi-icon.orange { background: #fff7ed; color: #ea580c; }
.kpi-icon.teal { background: #f0fdfa; color: #0d9488; }
.kpi-icon.green { background: #f0fdf4; color: #16a34a; }
.kpi-content { overflow: hidden; }
.kpi-content h2 { margin: 5px 0; font-size: 1.8rem; color: #0f172a; font-weight: 700;}
.kpi-label { margin: 0; font-size: 0.75rem; color: #64748b; font-weight: 600; letter-spacing: 0.5px; }
.kpi-subtext { margin: 0; font-size: 0.8rem; color: #94a3b8; }

/* NEW SLEEK CAROUSEL BAR */
.carousel-bar-v2 {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  padding: 15px 20px;
  border-radius: 16px;
  margin-bottom: 30px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 10px rgba(0,0,0,0.02);
}

.c-left, .c-right {
  display: flex;
  align-items: center;
  gap: 15px;
  cursor: pointer;
  transition: opacity 0.2s;
  flex: 1;
}
.c-right { justify-content: flex-end; }
.c-left.disabled, .c-right.disabled { opacity: 0.3; cursor: not-allowed; }

.c-arrow { font-size: 1.5rem; color: #94a3b8; font-weight: 300; }
.c-arrow-btn { background: #fff7ed; color: #ea580c; width: 36px; height: 36px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 1.2rem; font-weight: 600; }

.c-text { display: flex; flex-direction: column; }
.c-left .c-text { align-items: flex-start; }
.c-right .c-text { align-items: flex-end; }
.c-label { font-size: 0.75rem; color: #94a3b8; font-weight: 600; text-transform: uppercase; margin-bottom: 2px;}
.c-val { font-size: 0.95rem; font-weight: 600; color: #0f172a; max-width: 150px; }
.c-right .c-val { color: #ea580c; } /* Highlight next item in orange */

.c-center {
  display: flex;
  align-items: center;
  gap: 20px;
  flex: 2;
  justify-content: center;
  border-left: 1px solid #f1f5f9;
  border-right: 1px solid #f1f5f9;
  padding: 0 20px;
}
.c-thumb { width: 50px; height: 50px; border-radius: 12px; background-size: cover; background-position: center; border: 1px solid #e2e8f0;}
.c-info { display: flex; flex-direction: column; gap: 4px; }
.c-title-row { display: flex; align-items: center; gap: 10px; }
.c-title-row h3 { margin: 0; font-size: 1.1rem; color: #0f172a; max-width: 200px;}
.c-dots { display: flex; align-items: center; gap: 6px; }
.dot { width: 6px; height: 6px; background: #cbd5e1; border-radius: 50%; }
.dot.active { background: #ea580c; width: 16px; border-radius: 4px; }
.c-count { font-size: 0.8rem; color: #94a3b8; margin-left: 5px; font-weight: 500;}

/* STATES */
.state-message { text-align: center; padding: 80px 20px; color: #64748b; }
.state-message.empty h2 { color: #0f172a; font-size: 2rem; margin-bottom: 10px; }
.state-message.empty p { font-size: 1.1rem; }

/* SHARED BADGE STYLES */
.badge-v2 { padding: 4px 10px; border-radius: 6px; font-size: 0.75rem; font-weight: 600; }
.badge-v2.food-badge { background: #ffedd5; color: #c2410c; }
.badge-v2.type-badge { background: rgba(0,0,0,0.6); color: white; border: 1px solid rgba(255,255,255,0.2); backdrop-filter: blur(4px); }

/* SHARED Q-CIRCLE */
.q-circle { position: absolute; bottom: 15px; right: 15px; background: rgba(255,255,255,0.2); color: white; width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 0.85rem; font-weight: 700; z-index: 3; backdrop-filter: blur(4px); border: 1px solid rgba(255,255,255,0.3); }

/* RADIO CARDS V2 */
.cards-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 25px; }
.insight-card { background: white; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.03); border: 1px solid #e2e8f0; }

.radio-card-v2 { border-top: 4px solid #f97316; }
.card-image-header-v2 { height: 180px; background-color: #e2e8f0; background-image: url('https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&q=80&w=800'); background-size: cover; background-position: center; position: relative; display: flex; align-items: flex-end; padding: 20px; }
.card-image-header-v2::before { content: ''; position: absolute; bottom: 0; left: 0; right: 0; height: 70%; background: linear-gradient(to top, rgba(0,0,0,0.8), transparent); z-index: 1;}
.image-overlay-v2 { z-index: 2; width: 100%; }
.image-overlay-v2 h3 { color: white; margin: 0 0 8px 0; font-size: 1.3rem; font-weight: 700; text-shadow: 0 2px 4px rgba(0,0,0,0.5); }

.card-body { padding: 25px; }
.question-title { margin: 0 0 5px 0; font-size: 1.1rem; color: #0f172a; }
.response-count { margin: 0 0 20px 0; font-size: 0.85rem; color: #94a3b8; }
.bar-row { display: flex; align-items: center; gap: 15px; margin-bottom: 12px; }
.bar-label { width: 100px; font-size: 0.9rem; color: #475569; text-align: right; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

/* LIGHT ORANGE TRACKS */
.bar-track-v2 { flex-grow: 1; height: 28px; background: #fff7ed; border-radius: 8px; overflow: hidden; }
.bar-fill.orange-solid { height: 100%; border-radius: 8px; transition: width 1s ease; background: #f97316; }

.bar-value { width: 30px; font-weight: 600; font-size: 0.9rem; color: #0f172a; text-align: right;}
.bar-percent { width: 40px; font-size: 0.85rem; color: #94a3b8; text-align: right;}

.key-insight-v2 { margin-top: 25px; background: #fff7ed; padding: 12px 15px; border-radius: 8px; color: #c2410c; font-size: 0.9rem; display: flex; align-items: center; gap: 10px; }

/* TEXT CARDS V2 */
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

.sent-boxes-v2 { display: flex; gap: 10px; margin-bottom: 15px; }
.s-box-v2 { flex: 1; text-align: center; padding: 12px 10px; border-radius: 8px; border: 1px solid #f1f5f9; }
.s-box-v2 h2 { margin: 0; font-size: 1.6rem; font-weight: 700;}
.s-box-v2 span { font-size: 0.8rem; font-weight: 500;}
.s-box-v2.pos { background: #f0fdf4; color: #16a34a; border-color: #dcfce7; }
.s-box-v2.neu { background: #f8fafc; color: #64748b; }
.s-box-v2.neg { background: #fef2f2; color: #ef4444; border-color: #fee2e2; }

.sent-bar-v2 { height: 8px; display: flex; border-radius: 4px; overflow: hidden; gap: 2px; margin-bottom: 8px; }
.s-fill-v2.pos { background: #22c55e; }
.s-fill-v2.neu { background: #cbd5e1; }
.s-fill-v2.neg { background: #ef4444; }
.sent-bar-labels { display: flex; justify-content: space-between; font-size: 0.75rem; color: #64748b; }

.text-col-keywords { padding: 25px; display: flex; flex-direction: column; }
.word-cloud-v2 { flex-grow: 1; display: flex; flex-wrap: wrap; align-content: flex-start; gap: 12px; margin-top: 10px; }
.word-cloud-v2 span { font-weight: 700; color: #f97316; }
.w-huge { font-size: 1.8rem; }
.w-large { font-size: 1.3rem; opacity: 0.8; }
.w-med { font-size: 1.05rem; opacity: 0.6; }
.w-small { font-size: 0.85rem; opacity: 0.4; color: #94a3b8 !important;}

.text-bottom-row { padding: 25px; background: white; }
.excerpt-grid-v2 { display: grid; grid-template-columns: repeat(4, 1fr); gap: 15px; }
.exc-card { background: #f8fafc; padding: 20px; border-radius: 12px; display: flex; flex-direction: column; justify-content: space-between; border: 1px solid #e2e8f0; }
.exc-text { margin: 0 0 15px 0; font-size: 0.9rem; color: #334155; line-height: 1.5; }
.exc-footer { display: flex; }
.exc-tag { font-size: 0.8rem; font-weight: 600; display: flex; align-items: center; gap: 5px; }
.exc-tag.pos { color: #16a34a; }
.exc-tag.neu { color: #64748b; }
.exc-tag.neg { color: #ef4444; }
</style>
