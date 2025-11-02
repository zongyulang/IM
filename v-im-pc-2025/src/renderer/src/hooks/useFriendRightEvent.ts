import { useFriendStore } from '@renderer/store/friendStore'
import { menusEvent } from 'vue3-menus'
import FriendApi from '@renderer/api/FriendApi'
import { ElMessageBox } from 'element-plus'

/**
 * 异步处理好友列表右键菜单事件，提供删除好友功能。
 *
 * @param {string} friendId - 要操作的好友ID。
 * @param {MouseEvent} event - 鼠标事件对象。
 */
const friendRightEvent = async (friendId: string, event: MouseEvent) => {
  const menus = [
    {
      label: '删除',
      click: async () => {
        try {
          // 显示删除确认对话框
          await ElMessageBox.confirm('是否删除好友?', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          })

          // 用户点击了确定，调用API删除好友
          await FriendApi.delete(friendId)

          // 删除成功后，重新加载好友数据
          await useFriendStore().loadData()
        } catch (error) {
          // 用户点击了取消或删除过程中发生错误
          console.log('取消删除', error) // 可以根据具体情况处理错误，例如显示错误消息
        }
      }
    }
  ]
  menusEvent(event, menus, null)
  event.preventDefault()
}

export default friendRightEvent
