export interface Page<T> {
  total: number
  size: number
  current: number
  pages: number
  records: T[]
}
