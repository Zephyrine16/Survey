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
