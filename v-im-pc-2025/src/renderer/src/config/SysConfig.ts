/**
 * 系统配置类
 * 该类映射配置属性
 * 用于WebRTC和通信服务器设置
 */
export interface SysConfig {

  /**
   * 是否显示部门信息
   */
  showDept: boolean

  /**
   * 是否显示应用信息
   */
  showOauth2: boolean

  /**
   * 是否显示视频通话
   */
  showVideo: boolean

  /**
   * 是否显示视频会议
   */
  showMeeting: boolean

  /**
   * 密码的正则
   */
  passwordRegex: string

  /**
   * 密码的说明
   */
  passwordRegexDesc: string

  /**
   * 上传文件大小
   */
  uploadSize: number

  /**
   * 上传文件类型
   */
  uploadType: string
}
