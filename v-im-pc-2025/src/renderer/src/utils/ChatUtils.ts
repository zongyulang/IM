import FaceUtils from '@renderer/utils/FaceUtils'
import { getUUID } from '@renderer/utils/UUID'
import DOMPurify from 'dompurify'

const ChatUtils = {
  ErrorType: {
    TIMEOUT_ERROR: 9, //超时
    TOKEN_ERROR: 401, //token 失效错误
    PARAM_ERROR: 400, //参数错误
    FLUSH_TOKEN_ERROR: 7, //刷新token错误
    SERVER_ERROR: 500, //服务器错误
    NET_ERROR: 'TypeError: Failed to fetch' //网络链接不通
  },
  // 生成uuid
  uuid: (): string => {
    const uuid = localStorage.getItem('uuid') ?? getUUID()
    localStorage.setItem('uuid', uuid)
    return uuid
  },
  /**
   * 比较两个字符串，以确定哪个字符串较大。
   * 0:相同
   * 1：>
   * -1:<
   * @param a 要比较的字符串1
   * @param b 要比较的字符串2
   */
  compareLargeNumberStrings(a, b) {
    if (a.length !== b.length) {
      return a.length > b.length ? 1 : -1
    }
    for (let i = 0; i < a.length; i++) {
      const numA = parseInt(a[i], 10)
      const numB = parseInt(b[i], 10)
      if (numA !== numB) {
        return numA > numB ? 1 : -1
      }
    }
    return 0
  },

  /**
   * 图片加载完成，聊天对话框scroll拉到最下
   * @param id 容器id
   */
  imageLoad(id: string): void {
    this.scrollBottom(id)
    const messageBox = document.getElementById(id)
    if (messageBox) {
      const images = messageBox.getElementsByTagName('img')
      if (images) {
        const arr: string[] = []
        for (let i = 0; i < images.length; i++) {
          arr[i] = images[i].src
        }
        this.preloadImages(arr).finally(() => {
          this.scrollBottom(id)
        })
      }
    }
  },
  /**
   * 打开链接
   * @param event
   * @param proxy
   */
  openProxy(event: MouseEvent, proxy: any): void {
    event.preventDefault()
    const target = event.currentTarget as HTMLElement
    if (target.nodeName === 'IMG') {
      proxy.$winControl.openURL(target.getAttribute('src'))
    } else if (target.className === 'file-box' || target.nodeName === 'A') {
      proxy.$winControl.openURL(target.getAttribute('href'))
    }
  },
  /**
   * 加载图片数组
   * @param arr 图片路径数组
   * @returns Promise 所有图片加载完成后resolve，返回每个图片的加载结果
   * @description 使用 Promise.allSettled 并行加载所有图片，可以获取每张图片的加载状态
   * 即使部分图片加载失败，也会等待所有图片处理完成后返回结果
   */
  preloadImages(arr: string[]): Promise<PromiseSettledResult<HTMLImageElement>[]> {
    /**
     * 创建单个图片加载的Promise
     * @param src 图片路径
     * @returns Promise<HTMLImageElement> 返回加载完成的图片元素
     */
    const loadImage = (src: string): Promise<HTMLImageElement> => {
      return new Promise((resolve, reject) => {
        const image = new Image()
        // 图片加载成功时resolve
        image.onload = () => resolve(image)
        // 图片加载失败时reject
        image.onerror = (error) => reject(error)
        // 设置图片src开始加载
        image.src = src
      })
    }

    // 使用 Promise.allSettled 并行加载所有图片
    // map方法将图片路径数组转换为Promise数组
    // allSettled会等待所有Promise完成（无论成功或失败）
    return Promise.allSettled(arr.map((src) => loadImage(src)))
  },
  /**
   * 滚动条到最下方
   * @param id 容器id
   */
  scrollBottom(id: string): void {
    const div = document.getElementById(id)
    if (div) {
      div.scrollTop = div.scrollHeight
    }
  },

  /**
   * 滚动条到指定消息
   * @param boxId 容器id
   * @param messageId 消息id
   */
  scrollTo(boxId: string, messageId: string): void {
    const div = document.getElementById(boxId)
    const message = document.getElementById(messageId)
    if (div) {
      div.scrollTop = message ? message.offsetTop - 50 : div.scrollTop
    }
  },
  /**
   * 消息内容转换
   * @param content 要转换的内容
   */
  transform: (content: string) => {
    // 支持的html标签
    const fa = FaceUtils.faces()
    if (content) {
      content = content.replace(/face\[([^\s\\[\]]+?)]/g, function (face: string) {
        // 转义表情
        const alt = face.replace(/^face/g, '')
        return `<img data-face='true' alt='${alt}' src='${fa.get(alt)}'>`
      })
    }
    return content
  },
  /**
   * 消息内容转换
   * @param content 要转换的内容
   */
  transformXss: (content: string) => {
    // 支持的html标签
    const html = (end?: string) => {
      return new RegExp(
        '\\n*\\[' +
          (end || '') +
          '(code|pre|div|span|p|table|thead|th|tbody|tr|td|ul|li|ol|li|dl|dt|dd|h2|h3|h4|h5)([\\s\\S]*?)]\\n*',
        'g'
      )
    }
    const fa = FaceUtils.faces()
    if (content) {
      content = content
        .replace(/&(?!#?[a-zA-Z0-9]+;)/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/'/g, '&#39;')
        .replace(/"/g, '&quot;') // XSS
        .replace(
          /\b((?:https?:\/\/|www\d{0,3}[.]|[a-z0-9.-]+[.][a-z]{2,4}\/)(?:[^\s()<>]+|\(([^\s()<>]+|(\([^\s()<>]+\)))*\))+(?:\(([^\s()<>]+|(\([^\s()<>]+\)))*\)|[^\s`!()[\]{};:'".,<>?«»""'']))/gi,
          (match) => `<a href="${match}" target="_blank">${match}</a>`
        )

        .replace(/face\[([^\s\\[\]]+?)]/g, function (face: string) {
          // 转义表情
          const alt = face.replace(/^face/g, '')
          return `<img alt="${fa.get(alt)}" title="${fa.get(alt)}" src="${fa.get(alt)}" style='vertical-align: middle'>`
        })
        .replace(html(), '<$1 $2>')
        .replace(html('/'), '</$1>') // 转移HTML代码
        .replace(/\n/g, '<br>') // 转义换行
    }
    return content
  },

  /**
   * 打开文件
   * @param url 文件URL
   * @param _fileName 文件名
   * @param proxy Vue实例代理
   */
  async openFile(url: string, _fileName: string, proxy: any): Promise<void> {
    proxy.$winControl.openURL(url)
  },
  /**
   * 清理HTML内容，包括块级元素转换为换行，最终清理（只保留图片）
   * @param content 要清理的HTML内容
   */
  cleanHtml(content: string) {
    const tempClean = DOMPurify.sanitize(content, {
      ALLOWED_TAGS: ['img', 'p', 'div', 'section', 'h1', 'h2', 'h3', 'h4', 'h5'],
      ALLOWED_ATTR: ['src', 'alt']
    }).trim()
    // 第二步：将段落转换为换行
    // 处理块级元素转换为换行
    const withLineBreaks = tempClean
      // 去除开始标签，但保留img标签
      .replace(/<([A-Za-z]+)[^>]*>/gi, (match) => {
        // 如果是img标签，保留原样
        if (match.toLowerCase().startsWith('<img')) {
          return match;
        }
        // 只处理块级元素
        const blockElements = [
          'div',
          'h1',
          'h2',
          'h3',
          'h4',
          'h5',
          'section',
          'p',
          'ul',
          'ol',
          'li',
          'blockquote'
        ]
        if (blockElements.includes(match.toLowerCase().replace(/</g, '').replace(/\s.*$/g, ''))) {
          return '\n'
        }
        return ''
      })
      // 处理结束标签，但img标签没有结束标签，所以不需要特殊处理
      .replace(/<\/([A-Za-z]+)[^>]*>/gi, (__, tag) => {
        const blockElements = [
          'div',
          'h1',
          'h2',
          'h3',
          'h4',
          'h5',
          'section',
          'p',
          'ul',
          'ol',
          'li',
          'blockquote'
        ]
        if (blockElements.includes(tag.toLowerCase())) {
          return '\n'
        }
        return ''
      })
      // 处理连续换行
      .replace(/\n+/g, '\n\n')

    // 第三步：最终清理（保留图片）
    return DOMPurify.sanitize(withLineBreaks, {
      ALLOWED_TAGS: ['img'], // 最终只保留图片
      ALLOWED_ATTR: ['src', 'alt'] // 允许图片属性
    })
  }
}

export default ChatUtils
