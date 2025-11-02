interface VimConfig {
  // 名称
  name: string
  // 主机地址
  host: string
  // HTTP 协议
  httProtocol: string
  // WebSocket 协议
  wsProtocol: string
  // HTTP 端口
  httPort: number
  // WebSocket 端口
  wsPort: number
  // 客户端类型
  client: string
  // 声音文件路径
  soundPath: string
  // 表情文件路径
  facesPath: string
}

const vimConfig: VimConfig = {
  name: 'v-im',
  host: '107.174.250.107',
  //host: '127.0.0.1',
  httProtocol: 'http',
  wsProtocol: 'ws',
  httPort: 8080,
  wsPort: 9326,
  client: 'pc',
  soundPath: '/static/Message.mp3',
  facesPath: '/static/faces/'
}
export default vimConfig
