import vimConfig from '@renderer/config/VimConfig'
import FetchRequest from '@renderer/api/FetchRequest'
import { useWsStore } from '@renderer/store/WsStore'
import { useChatStore } from '@renderer/store/chatStore'
import { logout } from './Login'
import router from '@renderer/router'

class Auth {
  static getToken = (): string => {
    return localStorage.getItem('sa-token') ?? ''
  }

  static setToken = (token: string): void => {
    localStorage.setItem('sa-token', token)
  }

  static setRefreshToken = (token: string): void => {
    localStorage.setItem('refresh_token', token)
  }

  static getRefreshToken = (): string => {
    return localStorage.getItem('refresh_token') ?? ''
  }

  static clearToken = (): void => {
    localStorage.removeItem('sa-token')
    localStorage.removeItem('refresh_token')
  }

  static setIp = (ip: string): void => {
    localStorage.setItem('ip', ip)
  }

  static getIp = (): string => {
    return localStorage.getItem('ip') ?? vimConfig.host
  }

  static isLogin = () => {
    return new Promise((resolve, reject) => {
      const header: HeadersInit = {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        'sa-token': Auth.getToken()
      }
      const config: RequestInit = {
        method: 'GET',
        mode: 'cors',
        headers: header
      }
      fetch(`${FetchRequest.getHost()}/checkLogin`, config)
        .then((res) => {
          return res.json()
        })
        .then((res) => {
          if (res.code === 200) {
            resolve(true)
          } else {
            reject(false)
          }
        })
        .catch(() => {
          reject(false)
        })
    })
  }

  static logout = () => {
    logout().finally(() => {
      try {
        useChatStore().clearMessage()
        useWsStore().close()
      } finally {
        router.push('/').then()
      }
    })
  }
}
export default Auth
