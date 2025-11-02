import vimConfig from '../config/VimConfig'
import Auth from '@renderer/api/Auth'

const getFullUrl = (url: string): string => {
  if (url.startsWith('http')) {
    return url
  } else {
    return `${vimConfig.httProtocol}://${Auth.getIp()}:${vimConfig.httPort}/${url}`
  }
}
export default getFullUrl
