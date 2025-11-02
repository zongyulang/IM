import { defineStore } from 'pinia'
import type { User } from '@renderer/mode/User'
import type { UserSimple } from '@renderer/mode/UserSimple'
import UserApi from '../api/UserApi'

// 定义状态接口
interface State {
  user: User | null // 当前用户
  userMap: Record<string, UserSimple> // 用户映射
  pendingRequests: Map<string, Promise<UserSimple>> // 正在进行的请求
  loadingStates: Record<string, boolean> // 加载状态
  errors: Record<string, Error | null> // 错误信息
}

// 定义用户存储
export const useUserStore = defineStore('user_store', {
  state: (): State => ({
    user: null, // 初始化用户为 null
    userMap: {}, // 初始化用户映射为空对象
    pendingRequests: new Map(), // 初始化请求映射为空 Map
    loadingStates: {}, // 初始化加载状态为空对象
    errors: {} // 初始化错误信息为空对象
  }),
  persist: {
    enabled: true, // 启用持久化
    strategies: [
      {
        key: 'user', // 存储的键
        storage: localStorage, // 使用本地存储
        paths: ['user'] // 需要持久化的路径
      }
    ]
  },
  getters: {
    // 获取用户映射
    getMapUser: (state) => {
      return (id: string): UserSimple | undefined => {
        const store = useUserStore()
        // 自动触发获取逻辑（仅在数据不存在时）
        if (!state.userMap[id] && !state.loadingStates[id]) {
          store.fetchUser(id).catch(() => {})
        }
        return state.userMap[id] // 返回用户映射
      }
    },
    // 获取当前用户
    getUser(): User | null {
      return this.user // 返回当前用户
    },
    // 检查是否正在加载
    isLoading: (state) => (id: string) => !!state.loadingStates[id],
    // 获取错误信息
    error: (state) => (id: string) => state.errors[id]
  },
  actions: {
    // 设置用户
    setUser(user: User): void {
      this.user = user // 更新当前用户
    },

    // 获取用户信息
    async fetchUser(id: string): Promise<UserSimple> {
      this.errors[id] = null // 清除错误信息
      this.loadingStates[id] = true // 设置加载状态为 true

      try {
        // 处理并发请求
        const existingPromise = this.pendingRequests.get(id)
        if (existingPromise) return await existingPromise // 如果已有请求，直接返回

        const promise = UserApi.getUser(id) // 发起请求
        this.pendingRequests.set(id, promise) // 存储请求

        const user = await promise // 等待请求结果
        this.storeUser(id, user) // 存储用户信息
        return user // 返回用户信息
      } catch (err) {
        this.errors[id] = err instanceof Error ? err : new Error(String(err)) // 处理错误
        throw err // 抛出错误
      } finally {
        this.loadingStates[id] = false // 设置加载状态为 false
        this.pendingRequests.delete(id) // 删除请求
      }
    },

    // 存储用户信息
    storeUser(userId: string, user: UserSimple): void {
      this.userMap[userId] = user // 更新用户映射
    },

    // 清除用户信息
    clearUser(userId?: string): void {
      if (userId) {
        delete this.userMap[userId] // 删除指定用户
        delete this.errors[userId] // 删除错误信息
      } else {
        this.userMap = {} // 清空用户映射
        this.errors = {} // 清空错误信息
      }
    }
  }
})
