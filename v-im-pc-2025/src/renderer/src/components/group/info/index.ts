import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import GroupModal from './GroupModal.vue'
import router from '@renderer/router'

/**
 * 函数方式调用日志
 * @param groupId groupId
 * @param showSend showSend
 */
const showGroup = (groupId: string, showSend: boolean): void => {
  const instance = createApp(GroupModal, { groupId, showSend, closeDialog })
  instance.use(router)
  // 使用element-plus 并且设置全局的大小
  instance.use(ElementPlus)
  const node = document.createElement('div')
  document.body.appendChild(node)
  instance.mount(node)

  function closeDialog() {
    instance.unmount()
    document.body.removeChild(node)
  }
}

export default showGroup
