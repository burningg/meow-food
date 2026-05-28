import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import '@arco-design/web-vue/dist/arco.css'
import ArcoVue from '@arco-design/web-vue'
import { Message } from '@arco-design/web-vue';
import Vant from 'vant';
import 'vant/lib/index.css';

const app = createApp(App)
Message._context = app._context;
app.use(createPinia())
app.use(Vant);
app.use(router)
app.use(ArcoVue)
app.mount('#app')
