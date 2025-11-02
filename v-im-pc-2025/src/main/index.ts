import * as electron from 'electron'
import {
  app,
  BrowserWindow,
  desktopCapturer,
  ipcMain,
  Menu,
  Notification,
  screen,
  shell,
  Tray
} from 'electron'
import path, { join } from 'path'
import { electronApp, is, optimizer } from '@electron-toolkit/utils'
// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore
import vimConfig from '/src/renderer/src/config/VimConfig'

// 仅在开发环境下禁用证书验证
if (is.dev) {
  process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0'
}

// 优化常量定义 - 集中管理所有路径和配置
const CONFIG = {
  PATHS: {
    ICON: '../../resources/icon.png',
    EMPTY_ICON: '../../resources/empty.png',
    PRELOAD: '../preload/index.js'
  },
  APP: {
    ID: 'cn.v-im',
    NAME: vimConfig.name
  },
  WINDOW: {
    MAIN: {
      WIDTH: 1000,
      HEIGHT: 600
    }
  }
}

// 窗口默认配置
const WINDOW_DEFAULTS = {
  webPreferences: {
    webSecurity: true,
    contextIsolation: false,
    nodeIntegration: true,
    webviewTag: true,
    preload: join(__dirname, CONFIG.PATHS.PRELOAD),
    sandbox: false
  }
}

// 类型定义
interface NotificationPayload {
  content: string
  url?: string
}

// 刷新托盘定时器
let flashIconTimer: NodeJS.Timeout | null = null
let appIcon: Electron.Tray | null = null

/**
 * 创建主应用窗口
 * @returns {BrowserWindow} 创建的主窗口实例
 * @description 创建并配置主应用窗口，设置事件监听器和IPC通信
 */
function createWindow(): BrowserWindow {
  // 创建浏览器窗口
  const mainWindow = new BrowserWindow({
    show: false,
    autoHideMenuBar: true,
    webPreferences: {
      ...WINDOW_DEFAULTS.webPreferences
    },
    useContentSize: true,
    width: CONFIG.WINDOW.MAIN.WIDTH,
    height: CONFIG.WINDOW.MAIN.HEIGHT,
    frame: false
  })

  mainWindow.on('ready-to-show', () => {
    mainWindow.show()
  })

  // 处理窗口失去焦点事件
  mainWindow.on('blur', () => {
    mainWindow.webContents.send('BLUR', true)
  })

  // 处理窗口获得焦点事件
  mainWindow.on('focus', () => {
    mainWindow.webContents.send('FOCUS', false)
  })

  // 系统休眠和唤醒
  electron.powerMonitor.on('suspend', () => {
    mainWindow.webContents.send('SLEEP')
  })

  electron.powerMonitor.on('resume', () => {
    mainWindow.webContents.send('RESUME')
  })

  mainWindow.webContents.setWindowOpenHandler((details) => {
    shell.openExternal(details.url).catch((err) => console.error('打开外部链接失败:', err))
    return { action: 'deny' }
  })

  // 加载应用内容
  loadAppContent(mainWindow)

  // 设置IPC通信处理器
  setupIpcHandlers(mainWindow)

  // 创建系统托盘
  appIcon = createTray(mainWindow, CONFIG.PATHS.ICON)

  return mainWindow
}

/**
 * 加载应用内容
 * @param window - 要加载内容的窗口
 */
function loadAppContent(window: BrowserWindow): void {
  if (is.dev && process.env['ELECTRON_RENDERER_URL']) {
    window.loadURL(process.env['ELECTRON_RENDERER_URL']).catch((err) => {
      console.error('加载开发URL失败:', err)
      showErrorDialog('加载应用失败', `无法加载开发服务器: ${err.message}`)
    })
  } else {
    window.loadFile(join(__dirname, '../renderer/index.html')).catch((err) => {
      console.error('加载生产HTML文件失败:', err)
      showErrorDialog('加载应用失败', `无法加载应用页面: ${err.message}`)
    })
  }
}

/**
 * 显示错误对话框
 * @param title - 对话框标题
 * @param message - 错误信息
 */
function showErrorDialog(title: string, message: string): void {
  electron.dialog.showErrorBox(title, message)
}

/**
 * 设置IPC通信处理器
 * @param mainWindow - 主窗口实例
 * @description 设置所有IPC通信事件的处理逻辑
 */
function setupIpcHandlers(mainWindow: BrowserWindow): void {
  // 注册常用窗口控制功能
  registerWindowControls(mainWindow)

  // 注册通知相关功能
  registerNotificationHandlers(mainWindow)
}

/**
 * 注册窗口控制功能
 */
function registerWindowControls(mainWindow: BrowserWindow): void {
  // 最小化窗口
  ipcMain.on('MIN', () => {
    mainWindow.minimize()
  })

  // 在外部浏览器中打开URL
  ipcMain.on('OPEN-URL', (_: Electron.IpcMainEvent, url: string) => {
    shell.openExternal(url).catch((err) => console.error('打开外部URL失败:', err))
  })

  // 最大化或取消最大化窗口
  ipcMain.on('MAX', () => {
    if (mainWindow.isMaximized()) {
      mainWindow.unmaximize()
    } else {
      mainWindow.maximize()
    }
  })

  // 隐藏主窗口
  ipcMain.on('CLOSE', () => {
    hideMain(mainWindow)
  })

  // 闪烁任务栏
  ipcMain.on('FLASH-FRAME', () => {
    if (!mainWindow.isFocused()) {
      mainWindow.flashFrame(true)
    }
  })

  // 获取桌面窗口信息（整个显示屏幕）
  ipcMain.handle('getScreen', async () => {
    const sources = await desktopCapturer.getSources({
      types: ['screen'],
      thumbnailSize: getSize()
    })
    const { id } = screen.getDisplayNearestPoint(screen.getCursorScreenPoint())
    return sources.find((source) => source.display_id === id + '') ?? sources[0]
  })

  // 获取设备窗口信息, 包括屏幕和窗口
  ipcMain.handle('getSources', async () => {
    return await desktopCapturer.getSources({
      types: ['screen', 'window'],
      thumbnailSize: getSize()
    })
  })
}

/**
 * 获取截屏的size
 */
function getSize() {
  const { size, scaleFactor } = screen.getDisplayNearestPoint(screen.getCursorScreenPoint())
  return {
    width: Math.floor(size.width * scaleFactor),
    height: Math.floor(size.height * scaleFactor)
  }
}

/**
 * 注册通知相关功能
 */
function registerNotificationHandlers(mainWindow: BrowserWindow): void {
  // 发送系统通知
  ipcMain.on('NOTIFICATION', (_: Electron.IpcMainEvent, { content, url }: NotificationPayload) => {
    if (!mainWindow.isFocused()) {
      const notification = new Notification({
        title: CONFIG.APP.NAME,
        body: content,
        timeoutType: 'never'
      })

      notification.on('click', (event) => {
        event.preventDefault()
        showMain(mainWindow)
        if (url) {
          shell.openExternal(url).catch((err) => console.error('打开通知URL失败:', err))
        }
      })

      notification.show()
    }
  })

  // 闪烁托盘图标
  ipcMain.on('FLASH-ICON', () => {
    startFlashingTrayIcon(mainWindow)
  })

  // 清除闪烁托盘图标
  ipcMain.on('CLEAR-FLASH-ICON', () => {
    clearFlashIconTimer()
  })
}

/**
 * 开始闪烁托盘图标
 */
function startFlashingTrayIcon(mainWindow: BrowserWindow): void {
  if (!mainWindow.isVisible() && appIcon) {
    clearFlashIconTimer()
    let count = 0
    flashIconTimer = setInterval(() => {
      count++
      if (appIcon) {
        const iconPath = count % 2 === 0 ? CONFIG.PATHS.EMPTY_ICON : CONFIG.PATHS.ICON
        appIcon.setImage(path.join(__dirname, iconPath))
      }
    }, 500)
  }
}

/**
 * 隐藏主窗口并从任务栏移除
 * @param win - 要隐藏的窗口实例
 */
function hideMain(win: BrowserWindow): void {
  win.setSkipTaskbar(true)
  win.hide()
}

/**
 * 显示主窗口并在任务栏显示
 * @param win - 要显示的窗口实例
 * @description 显示窗口，恢复任务栏图标，并停止托盘图标闪烁
 */
function showMain(win: BrowserWindow): void {
  win.setSkipTaskbar(false)
  win.show()
  clearFlashIconTimer()
}

/**
 * 清除托盘图标闪烁定时器
 * @description 停止托盘图标闪烁并恢复原始图标
 */
function clearFlashIconTimer(): void {
  if (flashIconTimer) {
    clearInterval(flashIconTimer)
    if (appIcon) {
      appIcon.setImage(path.join(__dirname, CONFIG.PATHS.ICON))
    }
  }
}

/**
 * 创建系统托盘图标
 * @param win - 主窗口实例，用于托盘交互
 * @param iconPath - 托盘图标路径
 * @returns {Electron.Tray} 创建的托盘实例
 * @description 创建系统托盘图标并设置上下文菜单和点击事件
 */
function createTray(win: BrowserWindow, iconPath: string): Electron.Tray {
  const appIcon = new Tray(path.join(__dirname, iconPath))
  const contextMenu = Menu.buildFromTemplate([
    {
      label: '显示',
      click: () => {
        showMain(win)
      }
    },
    {
      label: '退出',
      click: () => {
        app.quit()
      }
    }
  ])
  appIcon.setToolTip(CONFIG.APP.NAME)
  appIcon.setContextMenu(contextMenu)
  appIcon.on('click', () => {
    showMain(win)
  })

  return appIcon
}

// 应用初始化
app.whenReady().then(() => {
  electronApp.setAppUserModelId(CONFIG.APP.ID)
  app.on('browser-window-created', (_, window) => {
    optimizer.watchWindowShortcuts(window)
  })

  // 处理自签名证书的错误
  app.on('certificate-error', (event, _webContents, _url, _error, _certificate, callback) => {
    // 阻止默认行为（停止加载页面）
    event.preventDefault()
    // 接受证书
    callback(true)
  })

  createWindow()

  // 设置全局错误处理
  process.on('uncaughtException', (error) => {
    console.error('未捕获的异常:', error)
    showErrorDialog('应用错误', `发生了未预期的错误: ${error.message}`)
  })

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow()
  })
})

// 应用退出管理
app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})

app.on('browser-window-focus', () => {
  clearFlashIconTimer()
})

// 清理资源
app.on('will-quit', () => {
  clearFlashIconTimer()
  // 释放资源
  appIcon = null
})
