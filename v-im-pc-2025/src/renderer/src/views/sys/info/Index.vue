<template>
  <div class="system-info-container">
    <div class="info-card">
      <div class="info-content">
        <el-row class="info-item">
          <el-col :span="6" class="info-label">
            <i class="el-icon-monitor"></i>系统名称
          </el-col>
          <el-col :span="18" class="info-value">{{ vimConfig.name }}</el-col>
        </el-row>

        <el-row class="info-item">
          <el-col :span="6" class="info-label">
            <i class="el-icon-location"></i>主机地址
          </el-col>
          <el-col :span="18" class="info-value">{{ host }}</el-col>
        </el-row>

        <el-row class="info-item">
          <el-col :span="6" class="info-label">
            <i class="el-icon-document"></i>系统版本
          </el-col>
          <el-col :span="18" class="info-value">{{ version }}</el-col>
        </el-row>

        <el-row class="action-area">
          <el-col :span="24">
            <el-button
              type="danger"
              size="small"
              :loading="clearing"
              @click="handleClearCache"
              class="clear-button">
              <i class="el-icon-delete"></i> 清理缓存
            </el-button>
          </el-col>
        </el-row>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import Auth from '@renderer/api/Auth'
import packageJson from '../../../../../../package.json'
import vimConfig from '@renderer/config/VimConfig'
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()
const version = packageJson.version
const host = Auth.getIp()
const clearing = ref(false)

// 处理清理缓存
const handleClearCache = async () => {
  try {
    await ElMessageBox.confirm('确定要清理缓存吗？清理后将自动退出登录', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await clearCache()
  } catch {
    // 用户取消操作
  }
}

// 清理缓存函数
const clearCache = async () => {
  try {
    clearing.value = true

    // 清理localStorage
    localStorage.clear()

    // 清理sessionStorage
    sessionStorage.clear()

    // 延迟显示成功消息并跳转到登录页
    setTimeout(() => {
      ElMessage.success('缓存清理成功，正在返回登录页...')
      clearing.value = false

      // 延迟跳转到登录页
      setTimeout(() => {
        router.push('/')
      }, 500)
    }, 1000)
  } catch (error) {
    console.error('清理缓存失败:', error)
    ElMessage.error('清理缓存失败')
    clearing.value = false
  }
}
</script>
<style lang="less" scoped>
.system-info-container {
  display: flex;
  justify-content: center;
  padding: 20px;

  .info-card {
    width: 100%;
    max-width: 800px;
    background: white;
    border-radius: 4px;
    overflow: hidden;
    transition: all 0.3s ease;

    &:hover {
      border-color: #dcdfe6;
    }

    .card-header {
      padding: 20px;
      font-size: 1.5rem;
      font-weight: bold;
      color: #409EFF;
      display: flex;
      align-items: center;
      background-color: #fafafa;
      border-bottom: 1px solid #f0f0f0;

      i {
        margin-right: 10px;
        font-size: 1.6rem;
      }
    }

    .info-content {
      padding: 20px;

      .info-item {
        margin-bottom: 16px;
        padding: 12px;
        border-radius: 0;
        border-bottom: 1px solid #f0f0f0;
        transition: background-color 0.3s;
        display: flex;
        align-items: center;
        &:hover {
          background-color: #fafafa;
        }

        .info-label {
          font-weight: bold;
          color: #606266;
          display: flex;
          align-items: center;

          i {
            margin-right: 8px;
            color: #409EFF;
          }
        }

        .info-value {
          color: #303133;
          word-break: break-all;
        }
      }

      .action-area {
        margin-top: 20px;
        text-align: right;
        .clear-button {
          padding: 10px 30px;
          transition: all 0.3s;
          border-radius: 2px;

          &:hover:not([disabled]) {
            opacity: 0.9;
          }
        }
      }
    }
  }
}
</style>
