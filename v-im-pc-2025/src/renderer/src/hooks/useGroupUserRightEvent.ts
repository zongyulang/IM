import { menusEvent } from 'vue3-menus'
import { ElMessage, ElMessageBox } from 'element-plus'
import GroupApi from '@renderer/api/GroupApi'
import type { User } from '@renderer/mode/User'
import { useGroupStore } from '@renderer/store/groupStore'

const groupUserRightEvent = async (
  user: User,
  groupId: string,
  isMaster: boolean,
  event: MouseEvent
) => {
  const menus = isMaster
    ? [
        {
          label: '转让',
          click: async () => {
            try {
              await ElMessageBox.confirm('是否转让群给此用户?', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
              })
              await GroupApi.transference(groupId, user.id)
              await useGroupStore().loadGroupData(groupId)
              ElMessage.success('转让成功')
            } catch (error) {
              console.log('取消转让', error) // 可以根据实际情况处理错误
            }
          }
        },
        {
          label: '删除',
          click: async () => {
            try {
              await ElMessageBox.confirm('是否从群中删除此人?', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
              })
              await GroupApi.deleteUser(groupId, user.id)
              await useGroupStore().loadGroupData(groupId)
              ElMessage.success('删除成功!')
            } catch (error) {
              console.log('取消删除', error) // 可以根据实际情况处理错误
            }
          }
        }
      ]
    : []

  menusEvent(event, menus, null)
  event.preventDefault()
}

export default groupUserRightEvent
