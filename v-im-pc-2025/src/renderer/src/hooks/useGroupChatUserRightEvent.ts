import { menusEvent } from 'vue3-menus'

const groupChatUserRightEvent = (
  name: string,
  isGroup: boolean,
  atCallBack: undefined | ((name: string) => void),
  event: MouseEvent
) => {
  const menus = isGroup
    ? [
        {
          label: `@${name}`,
          click: () => {
            if (atCallBack !== undefined) {
              atCallBack(`${name}`)
            }
          }
        }
      ]
    : []

  menusEvent(event, menus, null)
  event.preventDefault()
}

export default groupChatUserRightEvent
