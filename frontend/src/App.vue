<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <header>
    <nav class="navbar">
      <RouterLink to="/" class="nav-link">Take Survey</RouterLink>
      <RouterLink to="/dashboard" class="nav-link">View Analytics</RouterLink>
    </nav>
  </header>

  <main>
    <RouterView />
  </main>
</template>

<style scoped>
body {
  margin: 0;
  background-color: #f8f9fa;
}

.navbar {
  background-color: #ffffff;
  padding: 15px 30px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  display: flex;
  gap: 20px;
  justify-content: center;
  margin-bottom: 20px;
}

.nav-link {
  text-decoration: none;
  color: #4F46E5;
  font-weight: 600;
  padding: 8px 16px;
  border-radius: 6px;
  transition: background-color 0.2s;
}

.nav-link:hover {
  background-color: #f0f0ff;
}

.router-link-active {
  background-color: #4F46E5;
  color: white;
}
</style>
