let i = 1
const winControl = {
  isWeb: true,
  openURL: (url: string): void => {
    window.open(url)
  },
  flashFrame: (): void => {
    console.log('do nothing')
  },
  min: (): void => {
    alert('方法需要自定义')
  },
  max: (): void => {
    if (i % 2 === 1) {
      // eslint-disable-next-line @typescript-eslint/ban-ts-comment
      // @ts-ignore
      document.getElementById('v-im-app').style.cssText =
        'width:1000px;height:600px;margin:30px auto'
    } else {
      // eslint-disable-next-line @typescript-eslint/ban-ts-comment
      // @ts-ignore
      document.getElementById('v-im-app').style.cssText = 'width:100%;height:100%;margin:0'
    }
    i++
  },
  flashIcon: (): void => {
    console.log('FLASH-ICON')
  },
  clearFlashIcon: (): void => {
    console.log('CLEAR-FLASH-ICON')
  },
  close: (): void => {
    alert('方法需要自定义')
  },
  openCut: (): void => {
    console.log('openCut')
  },
  initCut: (): void => {
    console.log('initCut')
  },
  screenCut: (): void => {
    console.log('screenCut')
  },
  closeCut: (): void => {
    console.log('closeCut')
  },
  onSleep: (): void => {
    console.log('onSleep')
  },
  onResume: (): void => {
    console.log('onResume')
  },
  //窗口失去焦点
  onBlur: (): void => {
    console.log('onBlur')
  },
  //窗口失去焦点
  onFocus: (): void => {
    console.log('onFocus')
  },
  openWindow: (): void => {
    alert('网页端不支持此功能')
  },
  closeRouterWindow: (): void => {}
}
export default winControl
