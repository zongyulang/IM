enum SendCode {
  //准备
  READY = 'ready',
  //消息
  MESSAGE = 'message',
  //确认消息
  ACK = 'ack',
  //读取消息
  READ = 'read',
  //其他设备登录
  OTHER_LOGIN = 'other-login',
  //好友申请
  FRIEND_REQUEST = 'friend-request',
  //群申请验证
  GROUP_REQUEST = 'group-request'
}
export default SendCode
