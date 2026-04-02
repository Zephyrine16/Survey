import './assets/main.css'

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import axios from 'axios' // <-- 1. Import Axios here

// <-- 2. Tell Axios to use your live Render backend for all requests
axios.defaults.baseURL = 'https://survey-backend-dxb5.onrender.com';

const app = createApp(App)

app.use(router)

app.mount('#app')
