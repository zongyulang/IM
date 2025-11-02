import FetchRequest from '@renderer/api/FetchRequest'
// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore
import packageJson from '../../../../package.json'
import type { LoginResult } from '@renderer/mode/LoginResult'

const version = packageJson.version
interface loginBody {
  username: string
  password: string
  code: string
  uuid: string
}

// 登录方法
export const login = (
  username: string,
  password: string,
  code: string,
  uuid: string
): Promise<LoginResult> => {
  const data: loginBody = {
    username,
    password,
    code,
    uuid
  }
  return FetchRequest.post('/login', JSON.stringify(data), false)
}

// 注册方法
export const register = (data: any) => FetchRequest.post('/register', JSON.stringify(data), false)

// 退出方法
export const logout = () => FetchRequest.get('/logout', true)

// 获取验证码
export const getCodeImg = () => FetchRequest.get(`/captchaImage?version=${version}`, false)
