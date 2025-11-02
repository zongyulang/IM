import { createApp } from 'vue'
import type { User } from '@renderer/mode/User'
import type { RightClickMenu } from '@renderer/mode/RightClickMenu'
import AtMenu from '@renderer/components/at-menu/AtMenu.vue'
import ElementPlus from 'element-plus'
/**
 * 函数方式调用
 * @param isMaster 是否是管理员
 * @param users 群用户
 * @param x x坐标
 * @param y y坐标
 * @param atCallback 回调函数
 */
const atMenu = (
  isMaster: boolean,
  users: User[],
  x: number,
  y: number,
  atCallback: (item: string) => void
): void => {
  const rightClickInfo = {
    position: {
      x: x,
      y: y
    },
    menuList: new Array<RightClickMenu>()
  }
  if (isMaster) {
    rightClickInfo.menuList.push({
      btnName: '所有人',
      fn: () => {
        atCallback('所有人')
      }
    })
  }
  users.forEach((user) => {
    rightClickInfo.menuList.push({
      btnName: user.name,
      fn: () => {
        atCallback(user.name)
      }
    })
  })

  const instance = createApp(AtMenu, {
    rightClickInfo,
    classIndex: 0,
    closeRightMenu
  })
  // 使用element-plus 并且设置全局的大小
  instance.use(ElementPlus)
  const node = document.createElement('div')
  document.body.appendChild(node)
  instance.mount(node)

  function closeRightMenu() {
    instance.unmount()
    if (document.body.contains(node)) {
      document.body.removeChild(node)
    }
  }
}

export default atMenu
