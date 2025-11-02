<template>
  <div class="main-view">
    <el-form ref="userRef" :model="userForm" :rules="rules" label-width="80px">
      <el-form-item label="头像" prop="avatar" class="avatar-box">
        <avatar-upload
          size="default"
          :avatar="userForm.avatar"
          @upload-success="uploadSuccess"
        ></avatar-upload>
      </el-form-item>
      <el-form-item label="名称" prop="name">
        <el-input v-model="userForm.name" maxlength="30" />
      </el-form-item>
      <el-form-item label="手机" prop="mobile">
        <el-input v-model="userForm.mobile" maxlength="11" />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="userForm.email" maxlength="50" />
      </el-form-item>
      <el-form-item label="性别">
        <el-radio-group v-model="userForm.sex">
          <el-radio value="0">男</el-radio>
          <el-radio value="1">女</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submit">保存</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useUserStore } from '@renderer/store/userStore'
import type { User } from '@renderer/mode/User'
import UserApi from '@renderer/api/UserApi'
import AvatarUpload from '@renderer/components/AvatarUpload.vue'
import { ElMessage } from 'element-plus'

const userRef = ref()
const userStore = useUserStore()
const userForm = reactive<User>({
  id: '',
  name: '',
  mobile: '',
  email: '',
  avatar: '',
  deptId: '',
  sex: '',
  canAddFriend: ''
})

/** 初始化用户 */
onMounted(() => {
  UserApi.currentUser().then((res) => {
    userForm.id = res.id
    userForm.name = res.name
    userForm.deptId = res.deptId
    userForm.mobile = res.mobile
    userForm.email = res.email
    userForm.avatar = res.avatar === '' ? '' : res.avatar
    userForm.sex = res.sex
  })
})
/** 验证规则 */
const rules = ref({
  avatar: [{ required: true, message: '头像不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '用户名不能为空', trigger: 'blur' }],
  email: [
    { required: true, message: '邮箱地址不能为空', trigger: 'blur' },
    {
      type: 'email',
      message: "'请输入正确的邮箱地址",
      trigger: ['blur', 'change']
    }
  ],
  mobile: [
    { required: true, message: '手机号码不能为空', trigger: 'blur' },
    {
      pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/,
      message: '请输入正确的手机号码',
      trigger: 'blur'
    }
  ]
})

/** 上传成功 */
const uploadSuccess = (url: string) => {
  userForm.avatar = url
}

/** 提交按钮 */
const submit = async () => {
  userRef.value.validate((f) => {
    if (f) {
      UserApi.update(userForm.id, userForm)
        .then(() => {
          return UserApi.getUser(userForm.id)
        })
        .then((user) => {
          userStore.setUser(user)
          userStore.storeUser(userForm.id, user)
          ElMessage.success('修改成功')
        })
    }
  })
}
</script>
<style scoped lang="less">
.avatar-box {
  line-height: normal !important;
}
.avatar {
  border: 1px saddlebrown solid;
  width: 64px;
  height: 64px;
  display: flex;
  justify-content: center;
  align-items: center;
}
.avatar-box > label {
  align-items: center;
  display: flex;
  justify-content: flex-end;
}
</style>
