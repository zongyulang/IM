const getFileName = (url: string): string => {
  if (url) {
    const index = url.lastIndexOf('/')
    return url.substring(index + 1)
  } else {
    return ''
  }
}
export default getFileName
