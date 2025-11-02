import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import AddFriendModal from '@renderer/components/add-friend/AddFriendModal.vue'
import router from '@renderer/router'

/**
 * 函数方式调用添加好友
 * @param friendId friendId
 */
const addFriend = (friendId: string): void => {
  const instance = createApp(AddFriendModal, { friendId, closeDialog })
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

export default addFriend
