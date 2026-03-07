<template>
  <div class="survey-container">
    <div class="survey-card">

      <div class="food-header">
        <div class="food-image-placeholder">🍽️</div>
        <div class="food-details">
          <h2>Sample Food Item</h2>
          <span class="price">₱185.00</span>
        </div>
      </div>

      <hr class="divider"/>

      <div v-if="loading" class="loading-text">Loading survey questions...</div>

      <div v-else class="questions-list">
        <div v-for="question in questions" :key="question.id" class="question-block">

          <h3 class="questions-list">
            <span class="icon">✨</span> {{ question.text }}
          </h3>

          <div v-if="question.questionType === 'RADIO' " class="options-container">
            <label
              v-for="option in question.options"
              :key="option.id"
              class="option-card"
              :class ="{ 'selected': answers[question.id] === option.id }"
            >
              <input
                type="radio"
                :name="'question_' + question.id"
                :value="option.id"
                v-model="answers[question.id]"
                class="hidden-ratio"
              />
              <span class="option-label">{{ optioni.label }}</span>
              <span class="option-sub" v-if="option.sub_description">{{ option.sub_description }}</span>
            </label>
          </div>

          <div v-else-if="question.questionType === 'TEXT'" class="text-container">
            <input
              type="text"
              v-model="answers[question.id]"
              placeholder="e.g. Creamy, Warm, Heavy"
              class="text-input"
            />
            <small class="helper-text">Keywords help NLP identify this dish in natural conversation.</small>
          </div>

        </div>
      </div>
      <button class="submit-btn" type="submit" @click="submitAnswers">Submit Survey Tag</button>

    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios'

const questions = ref([]);
const loading = ref(true);

// Will store user's selected option IDs or text
const answers = ref({});

const fetchQuestions = async () => {
  try{
    const response = await axios.get('http://localhost:8080/questions/all');
    questions.value = reponse.data;
  } catch (error) {
    console.error("Error fetching questions: ", error);
  } finally {
    loading.value = false;
  }
};

const submitAnswers = () => {
  console.log("Current Answers Ready to Send:", answers.value);
  alert("Check you browser console to see the collected answers!");
};

onMounted(() => {
  fetchQuestions();
});
</script>

<style scoped>
.survey-container {
  display: flex;
  justify-content: center;
  padding: 20px;
  background-color: #f4f7f6;
  min-height: 100vh;
  font-family: sans-serif;
}

.survey-card {
  background: white;
  width: 100%;
  max-width: 450px;
  border-radius: 16px;
  box-shaodw: 0 8px 24px rgba(0, 0, 0, 0.08);
  padding: 24px;
}

.food-header {
  display: flex;
  gap: 16px;
  aligh-items: center;
  background-color: #f9fbfd;
  padding: 16px;
  border-radius: 12px;
  border: 1px solid #edf2f7;
}

.food-image-placeholder {
  font-size: 50px;
  background: white;
  padding: 10px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.food-details h2 {
  margin: 0 0 4px 0;
  font-size: 18px;
  color: #2c3e50;
}

.food-details p {
  margin: 0 0 8px 0;
  font-size: 12px;
  color: #7f8c8d;
}

.price {
  font-weight: bold;
  color: #34495e;
}

.divider {
  border: 0;
  height: 1px;
  background: #edf2f7;
  margin: 24px 0;
}

.question-block {
  margin-bottom: 28px;
}

.question-text {
  font-size: 15px;
  color: #2c3e50;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon {
  color: #ff6b6b; //Placeholder icon colour
}

.ptionis-container {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.options-card {
  flex: 1 1 calc(50% - 10px);
  min-width: 120px;
  border: 1.5px solid #e2e8f0;
  border-radius: 12px;
  padding: 14px 10px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.hidden-ratio {
  display: none;
}

.option-label {
  font-weight: bold;
  font-size: 13px;
  color: #4a5568;
}

.option-sub {
  font-size: 10px;
  color: #a0aec0;
  margin-top: 4px;
}

// Vue variable matches this options's ID
.option-card.selected {
  border-color: #6a8dff;
  background-color: #f0f4ff;
}

.option-card.selected .option-label, .option-card.selected .option-sub {
  color: #4a6ee0;
}

.text-input {
  width: 100%;
  padding: 14px;
  border: 1.5px solid #e2e8f0;
  border-radius: 12px;
  font-size: 14px;
  box-sizing: border-box;
  outline: none;
}

.text-input:focus {
  border-color: #6a8dff;
}

.helper-text {
  display: block;
  margin-top: 6ppx;
  font-size: 11px;
  color: #a0aec0;
  font-style: italic;
}

//Main button
.submit-btn {
  width: 100%;
  padding: 16px;
  background-color: #8bb0ff;
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  margin-top: 10px;
  transition: background-color 0.2s;
}

.submit-btn.hover {
  background-color: #6a8dff;
}
</style>
