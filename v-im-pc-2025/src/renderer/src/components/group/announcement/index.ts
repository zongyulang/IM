import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import AnnouncementModal from './AnnouncementModal.vue'
import type { Group } from '@renderer/mode/Group'
import router from '@renderer/router'

/**
 * 展示修改公告
 * @param group group
 */
const handleEditAnnouncement = (group: Group): void => {
  const instance = createApp(AnnouncementModal, { group, closeDialog })
  // 使用element-plus 并且设置全局的大小
  instance.use(ElementPlus)
  instance.use(router)
  const node = document.createElement('div')
  document.body.appendChild(node)
  instance.mount(node)

  function closeDialog() {
    instance.unmount()
    document.body.removeChild(node)
  }
}

export default handleEditAnnouncement
