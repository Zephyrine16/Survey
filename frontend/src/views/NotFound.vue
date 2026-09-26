<template>
  <div class="not-found-container">
    <div class="not-found-card">
      <div class="not-found-icon">🍽️</div>
      <h1 class="not-found-code">404</h1>
      <h2 class="not-found-title">Page Not Found</h2>
      <p class="not-found-desc">
        The page you are looking for does not exist or is not accessible on this domain.
      </p>
      <router-link to="/" class="not-found-btn">
        ← Return to Survey
      </router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
// 404 Not Found Page
</script>

<style scoped>
.not-found-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
  padding: 20px;
  font-family: inherit;
}
.not-found-card {
  background: white;
  padding: 48px 36px;
  border-radius: 20px;
  box-shadow: 0 20px 40px -15px rgba(0, 0, 0, 0.08);
  text-align: center;
  max-width: 440px;
  width: 100%;
}
.not-found-icon {
  font-size: 3.5rem;
  margin-bottom: 8px;
}
.not-found-code {
  font-size: 3.5rem;
  font-weight: 900;
  color: #ea580c;
  margin: 0;
  line-height: 1;
}
.not-found-title {
  font-size: 1.4rem;
  font-weight: 700;
  color: #0f172a;
  margin: 12px 0 8px 0;
}
.not-found-desc {
  font-size: 0.95rem;
  color: #64748b;
  margin: 0 0 28px 0;
  line-height: 1.5;
}
.not-found-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #ea580c;
  color: white;
  text-decoration: none;
  font-weight: 600;
  font-size: 0.95rem;
  padding: 12px 24px;
  border-radius: 10px;
  transition: all 0.2s ease;
  box-shadow: 0 4px 12px rgba(234, 88, 12, 0.25);
}
.not-found-btn:hover {
  background: #c2410c;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(234, 88, 12, 0.35);
}
</style>
