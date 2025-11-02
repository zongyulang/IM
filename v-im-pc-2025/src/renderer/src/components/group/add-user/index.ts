import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import AddGroupUser from './AddGroupUser.vue'
import type { Group } from '@renderer/mode/Group'

/**
 * 函数方式拉用户进群
 * @param group group
 */
const showAddGroupUser = (group: Group): void => {
  const instance = createApp(AddGroupUser, { group, closeDialog })
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

export default showAddGroupUser
