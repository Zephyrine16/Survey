<template>
  <div class="dashboard-container">
    <div class="header">
      <h1>📊 Analytics Dashboard</h1>
      <p>Showing survey results for: <strong>Item #1</strong></p>
    </div>

    <div v-if="Object.keys(analyticsData).length === 0" class="loading">
      Fetching latest data...
    </div>

    <div v-else class="results-wrapper">

      <div v-for="(answers, question) in radioQuestions" :key="question" class="question-card">
        <h3>{{ question }}</h3>
        <div class="radio-stats">
          <div v-for="stat in answers" :key="stat.optionLabel" class="stat-row">
            <div class="stat-label">
              <span>{{ stat.optionLabel }}</span>
              <span class="vote-count">{{ stat.voteCount }} votes</span>
            </div>
            <div class="progress-bar-bg">
              <div
                class="progress-bar-fill"
                :style="{ width: calculatePercentage(stat.voteCount, answers) + '%' }"
              ></div>
            </div>
          </div>
        </div>
      </div>

      <div v-for="(answers, question) in textQuestions" :key="question" class="question-card">
        <h3>{{ question }}</h3>
        <div class="text-feedback">
          <table>
            <thead>
            <tr>
              <th>User Email</th>
              <th>3-Word Vibe</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="feedback in answers" :key="feedback.userEmail">
              <td>{{ feedback.userEmail }}</td>
              <td class="feedback-text">"{{ feedback.response }}"</td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import axios from 'axios';

const analyticsData = ref<Record<string, any>>({});

const fetchAnalytics = async () => {
  try {
    const response = await axios.get('http://localhost:8080/analytics/1');
    analyticsData.value = response.data;
  } catch (error) {
    console.error("Error fetching analytics:", error);
  }
};

const radioQuestions = computed(() => {
  const result: Record<string, any> = {};
  for (const question in analyticsData.value) {
    const answers = analyticsData.value[question];

    if (answers.length > 0 && answers[0].voteCount !== undefined) {
      result[question] = answers;
    }
  }
  return result;
});

const textQuestions = computed(() => {
  const result: Record<string, any> = {};
  for (const question in analyticsData.value) {
    const answers = analyticsData.value[question];

    if (answers.length > 0 && answers[0].voteCount === undefined) {
      result[question] = answers;
    }
  }
  return result;
});

const calculatePercentage = (votes: number, allStats: any[]) => {
  let total = 0;

  allStats.forEach(stat => {
    total += Number(stat.voteCount);
  });

  if (total === 0) return 0;
  return Math.round((votes / total) * 100);
};

onMounted(() => {
  fetchAnalytics();
});
</script>

<style scoped>

.dashboard-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 40px 20px;
  font-family: 'Inter', sans-serif;
  color: #333;
}

.header {
  text-align: center;
  margin-bottom: 40px;
}

.loading {
  text-align: center;
  font-size: 1.2rem;
  color: #666;
}

.question-card {
  background: white;
  border-radius: 12px;
  padding: 25px;
  margin-bottom: 25px;
  box-shadow: 0 4px 15px rgba(0,0,0,0.05);
  border: 1px solid #eee;
}

.question-card h3 {
  margin-top: 0;
  margin-bottom: 20px;
  color: #1a1a1a;
  font-size: 1.1rem;
}

.stat-row {
  margin-bottom: 15px;
}

.stat-label {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
  font-weight: 500;
  font-size: 0.95rem;
}

.vote-count {
  color: #666;
  font-size: 0.85rem;
}

.progress-bar-bg {
  background: #f0f0f0;
  height: 10px;
  border-radius: 5px;
  overflow: hidden;
}

.progress-bar-fill {
  background: #4F46E5;
  height: 100%;
  border-radius: 5px;
  transition: width 0.8s ease-out;
}

table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 10px;
}

th, td {
  border-bottom: 1px solid #eee;
  padding: 12px 15px;
  text-align: left;
}

th {
  background-color: #f8f9fa;
  font-weight: 600;
  color: #555;
}

.feedback-text {
  font-style: italic;
  color: #444;
}
</style>
