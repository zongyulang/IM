<template>
  <div class="register-box">
    <vim-top />
    <div class="logo-container">
      <img src="../assets/icon.png" alt="logo" class="logo" />
      <span class="app-name">{{ vimConfig.name }}</span>
    </div>
    <el-form ref="registerRef" :model="registerForm" :rules="registerRules" class="register-form">
      <div class="title">
        <span>注册</span>
      </div>

      <el-form-item prop="username" class="item">
        <el-input v-model="registerForm.username" placeholder="请输入账号"></el-input>
      </el-form-item>

      <el-form-item prop="password" class="item">
        <el-input
          v-model="registerForm.password"
          type="password"
          placeholder="请输入密码"
        ></el-input>
      </el-form-item>

      <el-form-item prop="confirmPassword" class="item">
        <el-input
          v-model="registerForm.confirmPassword"
          type="password"
          placeholder="请确认密码"
        ></el-input>
      </el-form-item>

      <el-form-item v-if="captchaOnOff" prop="code" class="item captcha-item">
        <el-input
          v-model="registerForm.code"
          placeholder="请输入验证码"
          style="width: 63%"
        ></el-input>
        <div class="login-code">
          <img :src="codeUrl" class="login-code-img" alt="验证码" @click="getCode" />
        </div>
      </el-form-item>

      <div class="button-group">
        <div class="login-btns">
          <el-button type="primary" :loading="loading" @click.prevent="handleRegister">
            <span v-if="!loading">注册</span>
            <span v-else>注册中...</span>
          </el-button>
          <el-button @click="router.push('/')">取消</el-button>
        </div>
      </div>
      <div class="login-link">已有账号？<a @click="router.push('/')">立即登录</a></div>
    </el-form>
  </div>
</template>

<script lang="ts" setup>
import { ElMessageBox } from 'element-plus'
import { getCodeImg, register } from '@renderer/api/Login'
import { useRouter } from 'vue-router'
import { getCurrentInstance, onMounted, ref } from 'vue'
import vimConfig from '@renderer/config/VimConfig'
import VimTop from '@renderer/components/VimTop.vue'

const router = useRouter()

const { proxy } = getCurrentInstance()!

const registerForm = ref({
  username: '',
  password: '',
  confirmPassword: '',
  code: '',
  uuid: ''
})

const equalToPassword = (rule, value, callback) => {
  if (registerForm.value.password !== value) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const registerRules = {
  username: [
    { required: true, trigger: 'blur', message: '请输入您的账号' },
    {
      min: 2,
      max: 20,
      message: '用户账号长度必须介于 2 和 20 之间',
      trigger: 'blur'
    }
  ],
  password: [
    { required: true, trigger: 'blur', message: '请输入您的密码' },
    {
      min: 5,
      max: 20,
      message: '用户密码长度必须介于 5 和 20 之间',
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    { required: true, trigger: 'blur', message: '请再次输入您的密码' },
    { required: true, validator: equalToPassword, trigger: 'blur' }
  ],
  code: [{ required: true, trigger: 'change', message: '请输入验证码' }]
}

const codeUrl = ref('')
const loading = ref(false)
const captchaOnOff = ref(true)

async function handleRegister() {
  try {
    const valid = await proxy.$refs.registerRef.validate()
    if (valid) {
      loading.value = true
      await register(registerForm.value)
      const username = registerForm.value.username
      await ElMessageBox.alert(
        "<font color='red'>恭喜你，您的账号 " + username + ' 注册成功！</font>',
        '系统提示',
        {
          dangerouslyUseHTMLString: true,
          type: 'success'
        }
      )
      await router.push('/')
    }
  } catch (error) {
    console.error('注册失败:', error)
    if (captchaOnOff.value) {
      await getCode()
    }
  } finally {
    loading.value = false
  }
}

const getCode = async () => {
  try {
    const res = await getCodeImg()
    codeUrl.value = 'data:image/gif;base64,' + res.img
    registerForm.value.uuid = res.uuid
  } catch (error) {
    console.error('获取验证码失败:', error)
  }
}

// 页面加载时获取验证码
onMounted(async () => {
  await getCode()
})
</script>

<style lang="less" scoped>
.register-box {
  position: relative;
  background-image: linear-gradient(
    25deg,
    rgb(2, 12, 255),
    rgb(156, 23, 176),
    rgb(195, 53, 101),
    rgb(215, 81, 0)
  );
  background-position: 50% 0px;
  background-size: 100% 100%;
  height: 100%;
  box-shadow: rgba(0, 0, 0, 0.3) 0px 0px 20px;
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

  .register-form {
    width: 42rem;
    margin: 8rem auto;
    background-color: rgba(#fff, 0.3);
    padding: 25px 50px;
    box-shadow: rgba(0, 0, 0, 0.3) 0px 0px 20px;
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

    .button-group {
      display: flex;
      justify-content: center;
      padding: 15px 0;

      .login-btns {
        display: flex;
        gap: 12px;

        .el-button {
          height: 35px;
          min-width: 90px;

          &--primary {
            background: linear-gradient(45deg, #4b6cb7, #182848);
            border: none;

            &:hover {
              background: linear-gradient(45deg, #182848, #4b6cb7);
            }
          }

          &:not(.el-button--primary) {
            background: rgba(255, 255, 255, 0.2);
            border: 1px solid rgba(255, 255, 255, 0.3);
            color: #fff;
            backdrop-filter: blur(5px);

            &:hover {
              background: rgba(255, 255, 255, 0.3);
            }
          }
        }
      }
    }

    .login-link {
      text-align: center;
      font-size: 14px;
      color: rgba(255, 255, 255, 0.8);
      margin-top: 8px;

      a {
        color: #fff;
        text-decoration: none;
        margin-left: 5px;
        cursor: pointer;

        &:hover {
          text-decoration: underline;
        }
      }
    }
  }
}
</style>
