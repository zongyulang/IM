<template>
  <div class="login-box">
    <vim-top />
    <div class="logo-container">
      <img src="../assets/icon.png" alt="logo" class="logo" />
      <span class="app-name">{{ vimConfig.name }}</span>
    </div>
    <el-form ref="formRef" v-loading="loading" class="login" :model="form">
      <div class="title">
        <span>登录</span>
      </div>
      <el-form-item class="item">
        <el-input v-model="form.name" placeholder="请输入用户名"></el-input>
      </el-form-item>
      <el-form-item class="item">
        <el-input v-model="form.password" type="password" placeholder="请输入密码"></el-input>
      </el-form-item>
      <el-form-item v-if="captchaOnOff" prop="code" class="item captcha-item">
        <el-input
          v-model="form.code"
          auto-complete="off"
          placeholder="请输入验证码"
          style="width: 63%"
        >
        </el-input>
        <div class="login-code">
          <img alt="验证码" :src="codeUrl" class="login-code-img" @click="getCode" />
        </div>
      </el-form-item>

      <div class="button-group">
        <div class="register-link">
          还没有账号？<a @click="router.push('/register')">立即注册</a>
        </div>
        <div class="login-btns">
          <el-button
            type="primary"
            :loading="isSubmitting"
            :disabled="isSubmitting"
            @click="submit"
          >
            {{ isSubmitting ? '登录中...' : '登录' }}
          </el-button>
        </div>
      </div>
    </el-form>
    <el-button class="settings-btn" type="primary" circle @click="dialogVisible = true">
      <el-icon>
        <Setting />
      </el-icon>
    </el-button>

    <!-- 设置弹窗 -->
    <el-dialog v-model="dialogVisible" title="主机设置" width="30%">
      <el-form :model="settings">
        <el-form-item>
          <el-input v-model="settings.host" placeholder="请输入IP地址"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveSettings"> 确定 </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import Auth from '@renderer/api/Auth'
import { useUserStore } from '@renderer/store/userStore'
import { useChatStore } from '@renderer/store/chatStore'
import UserApi from '@renderer/api/UserApi'
import { getCodeImg, login } from '@renderer/api/Login'
import VimTop from '@renderer/components/VimTop.vue'
import vimConfig from '@renderer/config/VimConfig'
import { Setting } from '@element-plus/icons-vue'
import { useSysConfigStore } from '@renderer/store/sysConfigStore'
import { useWsStore } from '@renderer/store/WsStore'

const router = useRouter()
const userStore = useUserStore()
const chatStore = useChatStore()
const loading = ref(false)
const form = reactive({
  name: '',
  password: '',
  code: '',
  uuid: '',
  host: Auth.getIp()
})
const reg =
  /^(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])$/
const isSubmitting = ref(false)

const submit = async () => {
  if (!reg.test(form.host)) {
    ElMessage.error('主机地址不对')
    return
  }

  isSubmitting.value = true
  try {
    const loginResult = await login(form.name, form.password, form.code, form.uuid)
    Auth.setToken(loginResult.token)
    const user = await UserApi.currentUser()
    userStore.storeUser(user.id, { name: user.name, avatar: user.avatar })
    userStore.setUser(user)
    await chatStore.reloadChats()
    await useSysConfigStore().loadData()
    if (!loginResult.needChangePassword) {
      await router.push('/index/chat')
    } else {
      ElMessage.info('请立即修改密码')
      await router.push('/index/system/pwd')
    }
  } catch (error) {
    console.error('登录失败:', error)
    // 登录失败时刷新验证码
    await getCode()
  } finally {
    isSubmitting.value = false
  }
}

// 验证码开关
const captchaOnOff = ref(true)
const codeUrl = ref('')

const getCode = async () => {
  try {
    const res = await getCodeImg()
    codeUrl.value = 'data:image/gif;base64,' + res.img
    form.uuid = res.uuid
  } catch (error) {
    console.error('获取验证码失败:', error)
  }
}

const dialogVisible = ref(false)
const settings = reactive({
  host: Auth.getIp()
})

const saveSettings = async () => {
  if (!reg.test(settings.host)) {
    ElMessage.error('主机地址不对')
    return
  }
  form.host = settings.host
  Auth.setIp(form.host)
  dialogVisible.value = false
  await getCode()
}

onMounted(async () => {
  useWsStore().close()
  loading.value = true
  try {
    const isLoggedIn = await Auth.isLogin()
    if (isLoggedIn) {
      await router.push('/index/chat')
    }
  } catch (error) {
    loading.value = false
  } finally {
    loading.value = false
  }
  await getCode()
})
</script>

<style scoped lang="less">
.login-box {
  position: relative;
  background-image: linear-gradient(
    25deg,
    rgb(2, 12, 255),
    rgb(156, 23, 176),
    rgb(195, 53, 101),
    rgb(215, 81, 0)
  );
  background-position: 50% 0;
  background-size: 100% 100%;
  height: 100%;
  box-shadow: rgba(0, 0, 0, 0.3) 0 0 20px;
  border-radius: 8px;

  .logo-container {
    position: absolute;
    top: 20px;
    left: 20px;
    z-index: 1;
    background: rgba(255, 255, 255, 0.1);
    padding: 1rem;
    border-radius: 12px;
    backdrop-filter: blur(5px);
    border: 1px solid rgba(255, 255, 255, 0.3);
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
    display: flex;
    align-items: center;
    gap: 12px;
    transition: all 0.3s ease;
    width: calc(6.5rem + 2px);
    overflow: hidden;
    cursor: pointer;

    &:hover {
      width: 18rem;
      background: rgba(255, 255, 255, 0.2);
    }

    .logo {
      min-width: 4.5rem;
      height: 4.5rem;
      object-fit: contain;
      border-radius: 1rem;
      opacity: 0.9;
      filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
    }

    .app-name {
      color: #fff;
      font-size: 1.5rem;
      font-weight: 500;
      text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
      white-space: nowrap;
      letter-spacing: 1px;
      opacity: 0;
      transform: translateX(-20px);
      transition: all 0.3s ease;
    }

    &:hover .app-name {
      opacity: 1;
      transform: translateX(0);
    }
  }

  .login {
    width: 42rem;
    margin: 8rem auto;
    background-color: rgba(#fff, 0.3);
    padding: 25px 50px;
    box-shadow: rgba(0, 0, 0, 0.3) 0 0 20px;
    border-radius: 8px;

    .title {
      padding: 15px;
      text-align: center;
      font-size: 2.5rem;
      font-weight: bold;
      color: #fff;
      display: flex;
      align-items: center;
      justify-content: center;
      text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.5);
    }

    .button-group {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 15px 0;

      .register-link {
        margin: 0;
        color: #fff;

        a {
          color: #fff;
          cursor: pointer;

          &:hover {
            color: #182848;
          }
        }
      }

      .login-btns {
        margin: 0;

        .el-button {
          height: 35px;
          min-width: 90px;

          &--primary {
            background: linear-gradient(45deg, #315995, #56abac) !important;
            border: none;

            &:hover {
              background: linear-gradient(45deg, #56abac, #315995) !important;
            }
          }
        }
      }
    }

    .item {
      margin-bottom: 15px;

      :deep(.el-input) {
        .el-input__wrapper {
          background: rgba(255, 255, 255, 0.2);
          border: 1px solid rgba(255, 255, 255, 0.3);
          box-shadow: none;
          backdrop-filter: blur(5px);
          transition: all 0.3s ease;

          &.is-focus {
            background: rgba(255, 255, 255, 0.3);
            border-color: rgba(255, 255, 255, 0.5);
            box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.1);
          }

          .el-input__inner {
            color: #fff;
            height: 35px;

            &::placeholder {
              color: rgba(255, 255, 255, 0.7);
            }
          }
        }
      }
    }

    .captcha-item {
      display: flex;
      gap: 12px;

      .login-code {
        width: 37%;
        height: 38px;
        overflow: hidden;
        border-radius: 4px;

        img {
          width: 100%;
          height: 100%;
          object-fit: cover;
          cursor: pointer;
        }
      }
    }
  }

  .settings-btn {
    position: fixed;
    right: 20px;
    bottom: 20px;
    width: 40px;
    height: 40px;
    background: rgba(255, 255, 255, 0.2);
    border: 1px solid rgba(255, 255, 255, 0.3);
    backdrop-filter: blur(5px);
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
    transition: all 0.3s ease;
    color: #fff;
    padding: 0;

    &:hover {
      background: rgba(255, 255, 255, 0.3);
      transform: rotate(90deg);
    }

    &:active {
      transform: rotate(90deg) scale(0.95);
    }

    :deep(.el-icon) {
      font-size: 20px;
      transition: all 0.3s ease;
    }
  }

  :deep(.custom-dialog) {
    .el-dialog {
      border-radius: 12px;
      overflow: hidden;
      background: rgba(255, 255, 255, 0.92);
      backdrop-filter: blur(10px);
      box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);

      .el-dialog__header {
        margin: 0;
        padding: 20px;
        background: linear-gradient(45deg, #315995, #56abac); // 使用相同的渐变色
        border-bottom: 1px solid rgba(255, 255, 255, 0.1);

        .el-dialog__title {
          color: #fff;
          font-size: 18px;
          font-weight: 500;
        }

        .el-dialog__headerbtn {
          .el-dialog__close {
            color: #fff;
          }
        }
      }

      .el-dialog__body {
        padding: 30px 20px;

        .el-input {
          .el-input__wrapper {
            background: rgba(255, 255, 255, 0.95);
            border: 1px solid rgba(0, 0, 0, 0.1);
            box-shadow: none;
            transition: all 0.3s ease;

            &.is-focus {
              border-color: #315995; // 使用主色调
              box-shadow: 0 0 0 1px rgba(49, 89, 149, 0.1);
            }

            .el-input__inner {
              color: #333;
              height: 35px;

              &::placeholder {
                color: #999;
              }
            }
          }
        }
      }

      .el-dialog__footer {
        padding: 10px 20px 20px;
        text-align: center;

        .dialog-footer {
          display: flex;
          justify-content: center;
          gap: 12px;

          .el-button {
            min-width: 90px;
            height: 35px;

            &--primary {
              background: linear-gradient(45deg, #315995, #56abac) !important; // 使用相同的渐变色
              border: none;

              &:hover {
                background: linear-gradient(45deg, #56abac, #315995) !important;
              }
            }

            &:not(.el-button--primary) {
              border: 1px solid #dcdfe6;
              color: #606266;

              &:hover {
                color: #315995; // 使用主色调
                border-color: #315995;
                background-color: rgba(49, 89, 149, 0.1);
              }
            }
          }
        }
      }
    }
  }
}
</style>
