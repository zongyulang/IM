<template>
  <div class="main-view">
    <el-form :inline="true" :model="settingTemp" label-width="120px">
      <el-form-item label="允许加好友" prop="canAddFriend">
        <el-switch v-model="settingTemp.canAddFriend" @change="submit()" />
      </el-form-item>
      <el-form-item label="好友审核" prop="addFriendValidate">
        <el-switch v-model="settingTemp.addFriendValidate" @change="submit()" />
      </el-form-item>
      <el-form-item label="允许私聊" prop="canSendMessage">
        <el-switch v-model="settingTemp.canSendMessage" @change="submit()" />
      </el-form-item>
      <el-form-item label="消息语音提醒" prop="canSoundRemind">
        <el-switch v-model="settingTemp.canSoundRemind" @change="submit()" />
      </el-form-item>
      <!--    <el-form-item label="通话消息提醒" prop="canVoiceRemind">-->
      <!--      <el-switch v-model="settingTemp.canVoiceRemind" @change="submit()" />-->
      <!--    </el-form-item>-->
      <el-form-item label="展示手机" prop="showMobile">
        <el-switch v-model="settingTemp.showMobile" @change="submit()" />
      </el-form-item>
      <el-form-item label="展示邮箱" prop="showEmail">
        <el-switch v-model="settingTemp.showEmail" @change="submit()" />
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import SettingApi from '@renderer/api/SettingApi'
import type { Setting } from '@renderer/mode/Setting'
import { useUserStore } from '@renderer/store/userStore'
import DictUtils from '@renderer/utils/DictUtils'
import { useSettingStore } from '@renderer/store/settingStore'

const settingTemp = reactive({
  canAddFriend: false,
  addFriendValidate: false,
  canSendMessage: false,
  canSoundRemind: false,
  canVoiceRemind: false,
  showMobile: false,
  showEmail: false
})
const userId = useUserStore().user?.id
const loadData = () => {
  if (userId) {
    SettingApi.get(userId).then((res) => {
      const setting_ = res
      settingTemp.canAddFriend = setting_.canAddFriend === DictUtils.YES
      settingTemp.addFriendValidate = setting_.addFriendValidate === DictUtils.YES
      settingTemp.canSendMessage = setting_.canSendMessage === DictUtils.YES
      settingTemp.canSoundRemind = setting_.canSoundRemind === DictUtils.YES
      settingTemp.canVoiceRemind = setting_.canVoiceRemind === DictUtils.YES
      settingTemp.showMobile = setting_.showMobile === DictUtils.YES
      settingTemp.showEmail = setting_.showEmail === DictUtils.YES
    })
  }
}

loadData()

/** 提交按钮 */
function submit() {
  //userId必须传回，清理缓存用
  if (userId) {
    const setting: Setting = {
      canAddFriend: settingTemp.canAddFriend ? DictUtils.YES : DictUtils.NO,
      addFriendValidate: settingTemp.addFriendValidate ? DictUtils.YES : DictUtils.NO,
      canSendMessage: settingTemp.canSendMessage ? DictUtils.YES : DictUtils.NO,
      canSoundRemind: settingTemp.canSoundRemind ? DictUtils.YES : DictUtils.NO,
      canVoiceRemind: settingTemp.canVoiceRemind ? DictUtils.YES : DictUtils.NO,
      showMobile: settingTemp.showMobile ? DictUtils.YES : DictUtils.NO,
      showEmail: settingTemp.showEmail ? DictUtils.YES : DictUtils.NO,
      userId: userId
    }
    SettingApi.update(setting).then(() => {
      useSettingStore().loadData()
      ElMessage.success('修改成功')
    })
  }
}
</script>
