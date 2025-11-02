import { menusEvent } from 'vue3-menus'
import ImmunityApi from '@renderer/api/ImmunityApi'
import { useChatStore } from '@renderer/store/chatStore'
import { useImmunityStore } from '@renderer/store/immunityStore'
import { useUserStore } from '@renderer/store/userStore'

const chatRightEvent = async (chatId: string, event: MouseEvent) => {
  const chatStore = useChatStore()
  const immunityStore = useImmunityStore()
  const userStore = useUserStore()
  const immunityList = immunityStore.immunityList
  const chat = chatStore.getChat(chatId)
  const userId = userStore.user?.id

  const menus = [
    {
      label: '删除聊天',
      click: async () => {
        await chatStore.delChat(chatId)
      }
    }
  ]

  // 置顶操作
  menus.push({
    label: chat?.top ? '取消置顶' : '聊天置顶',
    click: async () => {
      if (chat?.top) {
        await chatStore.cancelTop(chatId)
      } else {
        await chatStore.topChat(chatId)
      }
    }
  })

  // 提醒操作
  if (userId) {
    const isImmune = immunityList.includes(chatId)
    menus.push({
      label: isImmune ? '开启提醒' : '关闭提醒',
      click: async () => {
        if (!isImmune) {
          await ImmunityApi.save(userId, chatId)
        } else {
          await ImmunityApi.delete(userId, chatId)
        }
        immunityStore.loadData()
      }
    })
  }

  menusEvent(event, menus, null)
  event.preventDefault()
}

export default chatRightEvent
