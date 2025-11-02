import { createApp } from 'vue'
import App from './App.vue'
import router from './router/index'
import store from './store/index'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

import './assets/styles/theme.less'
import './assets/styles/v-im.less'
import './assets/styles/g.css'
import 'element-plus/dist/index.css'
import './assets/font/iconfont.css'
import Menus from 'vue3-menus'
import preview from 'vue3-image-preview'

const app = createApp(App)

if (import.meta.env.VITE_TYPE === 'WEB') {
  app.config.globalProperties.$winControl = (await import('./hooks/webControl')).default
} else {
  app.config.globalProperties.$winControl = (await import('./hooks/windowControl')).default
}
// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore
app.use(Menus)
  .use(preview)
  .use(store)
  .use(router)
  .use(ElementPlus, { locale: zhCn })
  .mount('#v-im-app')
