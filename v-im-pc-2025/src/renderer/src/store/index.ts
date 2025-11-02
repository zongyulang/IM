import { createPinia } from 'pinia'
import piniaPluginPersist from 'pinia-plugin-persist'

export default createPinia().use(piniaPluginPersist)
