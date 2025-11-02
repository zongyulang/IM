/**
 * Base64 和文件转换工具类
 * 用于处理 Base64 编码的数据转换为文件对象
 * 主要用于处理图片、文件等二进制数据的转换
 */

/**
 * 将 Base64 字符串转换为 ArrayBuffer
 * @param base64 需要转换的 Base64 字符串
 * @returns ArrayBuffer 转换后的二进制缓冲区
 * @description
 * 1. 处理 Base64 字符串的填充
 * 2. 替换特殊字符（-替换为+，_替换为/）
 * 3. 将 Base64 字符串转换为字节数组
 * 4. 最后转换为 ArrayBuffer
 */
function base64ToArrayBuffer(base64: string) {
  // 添加必要的填充字符 =
  const padding = '='.repeat((4 - (base64.length % 4)) % 4)
  // 替换 URL 安全的 Base64 字符为标准 Base64 字符
  const base64Url = base64.replace(/-/g, '+').replace(/_/g, '/')
  // 拼接完整的 Base64 字符串
  const base64String = base64Url + padding
  // 解码 Base64 为二进制字符串
  const byteCharacters = atob(base64String)
  // 创建字节数组
  const byteNumbers = new Array(byteCharacters.length)

  // 将字符转换为字节码
  for (let i = 0; i < byteCharacters.length; i++) {
    byteNumbers[i] = byteCharacters.charCodeAt(i)
  }

  // 创建 TypedArray
  const byteArray = new Uint8Array(byteNumbers)
  return byteArray.buffer
}

/**
 * 将 Base64 字符串转换为文件对象
 * @param base64String Base64 编码的字符串，需要包含 MIME 类型（如：data:image/png;base64,）
 * @param fileName 生成文件的文件名
 * @returns File 转换后的文件对象
 * @example
 * ```typescript
 * // 使用示例
 * const base64Data = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA..."
 * const file = base64ToFile(base64Data, "image.png")
 * ```
 */
export function base64ToFile(base64String: string, fileName: string) {
  // 从 base64 字符串中提取 MIME 类型
  const mimeType = base64String.match(/data:([^;]+);/)?.[1] || 'image/png'

  // 去除 Base64 字符串的前缀部分（如：data:image/png;base64,）
  const base64Data = base64String.replace(/^data:([^;]+);base64,/, '')

  // 转换为 ArrayBuffer
  const arrayBuffer = base64ToArrayBuffer(base64Data)

  // 创建 Blob 对象，使用正确的 MIME 类型
  const blob = new Blob([arrayBuffer], { type: mimeType })

  // 返回 File 对象
  return new File([blob], fileName, { type: mimeType })
}
