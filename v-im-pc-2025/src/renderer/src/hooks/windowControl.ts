// 定义事件回调类型
type EventCallback = () => void | unknown
type CutInfoCallback = () => void

class WindowControl {
  private readonly ipcRenderer = window.electron.ipcRenderer
  public readonly isWeb = false

  // URL 操作
  public openURL(url: string): void {
    if (!url) {
      console.warn('openURL: URL cannot be empty')
      return
    }
    this.ipcRenderer.send('OPEN-URL', url)
  }

  // 窗口闪烁
  public flashFrame(): void {
    this.ipcRenderer.send('FLASH-FRAME')
  }

  // 系统通知
  public notification(content: string): void {
    if (!content) {
      console.warn('notification: Content cannot be empty')
      return
    }
    this.ipcRenderer.send('NOTIFICATION', content)
  }

  // 窗口控制
  public min(): void {
    this.ipcRenderer.send('MIN')
  }

  public max(): void {
    this.ipcRenderer.send('MAX')
  }

  public close(): void {
    this.ipcRenderer.send('CLOSE')
  }

  // 图标闪烁控制
  public flashIcon(): void {
    this.ipcRenderer.send('FLASH-ICON')
  }

  public clearFlashIcon(): void {
    this.ipcRenderer.send('CLEAR-FLASH-ICON')
  }

  // 截屏相关
  public openCut(getCutInfo: CutInfoCallback, showChatWindow: boolean): void {
    this.ipcRenderer.send('OPEN_CUT_SCREEN', showChatWindow)
    this.ipcRenderer.removeAllListeners('GET-CUT-INFO')
    this.ipcRenderer.once('GET-CUT-INFO', getCutInfo)
  }

  public screenCut(pic: CutInfoCallback): void {
    this.ipcRenderer.send('CUT_SCREEN', pic)
  }

  public closeCut(): void {
    this.ipcRenderer.send('CLOSE_CUT_SCREEN')
  }

  // 系统事件监听
  public onSleep(sleep: EventCallback): void {
    this.ipcRenderer.once('SLEEP', sleep)
  }

  public onResume(resume: EventCallback): void {
    this.ipcRenderer.once('RESUME', resume)
  }

  public onBlur(blur: EventCallback): void {
    this.ipcRenderer.on('BLUR', blur)
  }

  public onFocus(focus: EventCallback): void {
    this.ipcRenderer.on('FOCUS', focus)
  }

  // 窗口打开相关
  public openOfficeWindow(url: string): void {
    if (!url) {
      console.warn('openOfficeWindow: URL cannot be empty')
      return
    }
    this.ipcRenderer.send('OPEN-OFFICE-WINDOW', { url })
  }

}

export default new WindowControl()
