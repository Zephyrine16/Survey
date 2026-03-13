<template>
  <div class="survey-container">

    <div v-if="menuItems.length === 0 || questions.length === 0" class="loading">
      <h2>Loading your survey...</h2>
    </div>

    <div v-else class="carousel-wrapper">
      <div class="header">
        <span class="progress-badge">Item {{ currentIndex + 1 }} of {{ menuItems.length }}</span>

        <div class="food-image-placeholder">
          <span class="placeholder-icon">🍽️</span> </div>

        <h1 class="food-title">{{ currentItem.name }}</h1>
        <p class="food-desc">{{ currentItem.description }}</p>
        <span class="food-price">₱{{ currentItem.price }}</span>
      </div>

      <div class="questions-card">
        <div v-for="(q, index) in questions" :key="q.id" class="question-block">
          <p class="question-text"><strong>{{ index + 1 }}.</strong> {{ q.text }}</p>

          <div v-if="q.questionType === 'RADIO'" class="options-grid">
            <label
              v-for="opt in q.options"
              :key="opt.id"
              class="option-label"
              :class="{ 'selected': answers[currentItem.id]?.[q.id]?.id === opt.id }"
            >
              <input
                type="radio"
                :name="'q_' + q.id + '_item_' + currentItem.id"
                :value="opt"
                v-model="answers[currentItem.id][q.id]"
                class="hidden-radio"
              />
              <span class="opt-main">{{ opt.label }}</span>
              <span v-if="opt.subDescription" class="opt-sub">{{ opt.subDescription }}</span>
            </label>
          </div>

          <div v-else-if="q.questionType === 'TEXT'" class="text-input-wrapper">
            <input
              type="text"
              v-model="answers[currentItem.id][q.id]"
              placeholder="Tell us what you think..."
              class="text-input"
            />
          </div>
        </div>
      </div>

      <div class="navigation-buttons">
        <button
          @click="prevItem"
          :disabled="currentIndex === 0"
          class="nav-btn prev-btn"
        >
          ⬅️ Previous Item
        </button>

        <button
          v-if="currentIndex < menuItems.length - 1"
          @click="nextItem"
          class="nav-btn next-btn"
        >
          Next Item ➡️
        </button>

        <button
          v-else
          @click="submitSurvey"
          class="nav-btn submit-btn"
        >
          ✅ Submit Survey
        </button>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';

const menuItems = ref<any[]>([]);
const questions = ref<any[]>([]);
const currentIndex = ref(0);

const answers = ref<Record<number, Record<number, any>>>({});

const currentItem = computed(() => menuItems.value[currentIndex.value]);

const fetchData = async () => {
  try {
    const qRes = await axios.get('http://localhost:8080/questions/all');
    questions.value = qRes.data;

    try {
      const mRes = await axios.get('http://localhost:8080/menu-items');
      menuItems.value = mRes.data;
    } catch (e) {
      console.warn("Using fallback menu items until the backend controller is built!");
      menuItems.value = [
        { id: 1, name: "Chicken Creamy Mushroom n Aglio Olio Rice", description: "A comforting classic.", price: 150.00 },
        { id: 2, name: "Salisbury Steak n Mushroom Sauce", description: "Savory and filling.", price: 180.00 }
      ];
    }

    menuItems.value.forEach(item => {
      answers.value[item.id] = {};
    });

  } catch (error) {
    console.error("Error fetching survey data:", error);
  }
};

const nextItem = () => {
  if (currentIndex.value < menuItems.value.length - 1) {
    currentIndex.value++;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
};

const prevItem = () => {
  if (currentIndex.value > 0) {
    currentIndex.value--;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
};

const submitSurvey = async () => {
  const payload = [];

  const userEmail = "student_" + Math.floor(Math.random() * 1000) + "@university.edu";

  for (const itemId in answers.value) {
    for (const qId in answers.value[itemId]) {
      const answerData = answers.value[itemId][qId];

      if (answerData) {
        const isText = typeof answerData === 'string';

        payload.push({
          menuItemId: Number(itemId),
          questionId: Number(qId),
          selectedOptionId: isText ? null : answerData.id,
          response: isText ? answerData : null,
          userEmail: userEmail
        });
      }
    }
  }

  console.log("🚀 THE 1 HTTP REQUEST PAYLOAD:", payload);
  alert("Survey Complete! Open your browser console (F12) to see the massive JSON payload ready to be sent to Spring Boot!");

  /* Back-end endpoint connection is ready for activation! */
  /* await axios.post('http://localhost:8080/answers/batch', payload); */
};

onMounted(() => {
  fetchData();
});
</script>

<style scoped>
.survey-container {
  max-width: 800px;
  margin: 0 auto;
  font-family: 'Inter', sans-serif;
  color: #333;
  padding-bottom: 50px;
}

.loading {
  text-align: center;
  margin-top: 100px;
  color: #666;
}

.header {
  text-align: center;
  margin-bottom: 30px;
  padding: 30px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 15px rgba(0,0,0,0.05);
}

.progress-badge {
  background: #e0e7ff;
  color: #4F46E5;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 0.85rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* NEW STYLES FOR IMAGE PLACEHOLDER */
.food-image-placeholder {
  background-color: #f1f5f9; /* Light grey background */
  height: 150px; /* Adjust height as needed */
  border-radius: 12px;
  margin: 20px auto; /* Margin above/below, auto horizontally */
  display: flex; /* Flexbox for centering content */
  align-items: center; /* Vertical centering */
  justify-content: center; /* Horizontal centering */
  border: 2px dashed #e2e8f0; /* Dashed border for placeholder look */
  max-width: 100%; /* Responsive width */
}

.placeholder-icon {
  font-size: 3rem; /* Large icon size */
  color: #94a3b8; /* Medium grey icon color */
}

/* Optional text-based placeholder styling:
.placeholder-text {
  font-size: 1rem;
  color: #94a3b8;
  font-style: italic;
}
*/

.food-title {
  margin: 15px 0 5px 0;
  font-size: 1.8rem;
  color: #111;
}

.food-desc {
  color: #666;
  margin-bottom: 15px;
}

.food-price {
  font-weight: bold;
  font-size: 1.2rem;
  color: #10B981;
}

.question-block {
  background: white;
  padding: 25px;
  border-radius: 12px;
  margin-bottom: 20px;
  box-shadow: 0 4px 15px rgba(0,0,0,0.03);
  border: 1px solid #f0f0f0;
}

.question-text {
  font-size: 1.1rem;
  font-weight: 500;
  margin-bottom: 15px;
}

.options-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.option-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15px;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.option-label:hover {
  border-color: #a5b4fc;
  background-color: #f8fafc;
}

.option-label.selected {
  border-color: #4F46E5;
  background-color: #eef2ff;
}

.hidden-radio {
  display: none;
}

.opt-main {
  font-weight: 500;
}

.opt-sub {
  font-size: 0.85rem;
  color: #666;
}

.text-input {
  width: 100%;
  padding: 15px;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  font-size: 1rem;
  box-sizing: border-box;
}

.text-input:focus {
  outline: none;
  border-color: #4F46E5;
}

.navigation-buttons {
  display: flex;
  justify-content: space-between;
  margin-top: 30px;
}

.nav-btn {
  padding: 12px 24px;
  font-size: 1rem;
  font-weight: 600;
  border-radius: 8px;
  cursor: pointer;
  border: none;
  transition: transform 0.1s, opacity 0.2s;
}

.nav-btn:active {
  transform: scale(0.97);
}

.nav-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.prev-btn {
  background: #f1f5f9;
  color: #475569;
}

.next-btn {
  background: #4F46E5;
  color: white;
  margin-left: auto;
}

.submit-btn {
  background: #10B981;
  color: white;
  margin-left: auto;
}
</style>
