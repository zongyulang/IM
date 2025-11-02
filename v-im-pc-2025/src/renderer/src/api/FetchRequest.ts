import Auth from '@renderer/api/Auth'
import { ElMessage } from 'element-plus'
import vimConfig from '@renderer/config/VimConfig'

/**
 * 请求类，支持无感刷新token
 * @author 乐天
 */
class FetchRequest {
  isRefreshing: boolean
  private static instance: FetchRequest

  private constructor() {
    this.isRefreshing = false
  }

  /**
   * 单例构造方法，构造一个广为人知的接口，供用户对该类进行实例化
   * @returns {FetchRequest}
   */
  static getInstance() {
    if (!this.instance) {
      this.instance = new FetchRequest()
    }
    return this.instance
  }

  /**
   * 请求方法
   * @param url 请求路径
   * @param params 参数
   * @param method 方法
   * @param isNeedToken 是否需要token
   */
  request = async (url: string, params: string, method: string, isNeedToken = false) => {
    const header: HeadersInit = {
      Accept: 'application/json',
      'Content-Type': 'application/json'
    }

    const token = Auth.getToken()
    if (isNeedToken && token) {
      header['sa-token'] = token
    }

    const config: RequestInit = {
      method: method,
      mode: 'cors',
      headers: header
    }

    if (method !== 'GET') {
      config.body = params
    }
    let response: Response | undefined = undefined
    try {
      response = await fetch(this.getHost() + url, config)
    } catch (e) {
      console.error(e)
      ElMessage.error('无法连接服务器')
      return Promise.reject()
    }
    if (response) {
      return await this.check(response)
    }
  }

  /**
   * upload请求方法
   */
  upload = async (file: File) => {
    const token = Auth.getToken()
    const header: HeadersInit = {
      'Access-Control-Allow-Origin': '*',
      'sa-token': token
    }
    const formData = new FormData()
    formData.append('file', file)
    const config: RequestInit = {
      method: 'POST',
      mode: 'cors',
      headers: header,
      body: formData
    }

    const response = await fetch(`${this.getHost()}/vim/upload`, config)
    return await this.check(response)
  }

  /**
   * 检查请求返回值，如果token失效,执行刷新方法
   * @param response 请求响应数据
   */
  check = async (response: Response): Promise<any> => {
    if (response.status === 200) {
      const res = await response.json()
      if (res.code === 401) {
        Auth.logout()
        return Promise.reject('token失效，请重新登录')
      } else if (res.code !== 200) {
        ElMessage.error(res.msg)
        return Promise.reject(res.msg)
      } else {
        return Promise.resolve(res.data)
      }
    } else {
      ElMessage.error('请求出错，状态码：' + response.status)
      return Promise.reject('请求出错')
    }
  }

  getHost = (): string => {
    return `${vimConfig.httProtocol}://${Auth.getIp()}:${vimConfig.httPort}`
  }

  // 有些 api 并不需要用户授权使用，则无需携带 sa-token；默认不携带，需要传则设置第三个参数为 true
  get = async (url: string, isNeedToken = false) => {
    return await this.request(url, '', 'GET', isNeedToken)
  }

  post = async (url: string, params: string, isNeedToken = false) => {
    return await this.request(url, params, 'POST', isNeedToken)
  }

  put = async (url: string, params: string, isNeedToken = false) => {
    return await this.request(url, params, 'PUT', isNeedToken)
  }

  del = async (url: string, params: string, isNeedToken = false) => {
    return await this.request(url, params, 'DELETE', isNeedToken)
  }

  patch = async (url: string, params: string, isNeedToken = false) => {
    return await this.request(url, params, 'PATCH', isNeedToken)
  }
}

export default FetchRequest.getInstance()
