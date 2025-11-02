import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import EditNameModal from './EditNameModal.vue'
import type { Group } from '@renderer/mode/Group'
import router from '@renderer/router'

/**
 * 展示修改群名称对话框
 * @param group group
 */
const handleEditGroupName = (group: Group): void => {
  const instance = createApp(EditNameModal, { group, closeDialog })
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

export default handleEditGroupName 