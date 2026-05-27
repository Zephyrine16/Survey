import './assets/main.css'

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import axios from 'axios'

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? '').trim()
if(!apiBaseUrl) {
  throw new Error("VITE_API_BASE_URL must be set.")
}

axios.defaults.baseURL = apiBaseUrl
axios.defaults.withCredentials = true
axios.defaults.headers.common['X-Requested-With'] = 'XMLHttpRequest'

const app = createApp(App)

app.use(router)

app.mount('#app')
