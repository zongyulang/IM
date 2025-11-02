<template>
  <div class="im-top" style="-webkit-app-region: drag">
    <div>
      <i
        v-if="back"
        class="iconfont icon-v-fanhui back"
        style="-webkit-app-region: no-drag"
        @click="backHistory"
      ></i>
    </div>
    <div>
      <a href="javascript:void(0)" style="-webkit-app-region: no-drag" @click="min()">
        <i class="iconfont icon-v-24gl-minimization"></i>
      </a>
      <a href="javascript:void(0)" style="-webkit-app-region: no-drag" @click="max()">
        <i class="iconfont" :class="icon"></i>
      </a>
      <a href="javascript:void(0)" style="-webkit-app-region: no-drag" @click="close()">
        <i class="iconfont icon-v-guanbi1"></i>
      </a>
    </div>
  </div>
</template>
<script setup lang="ts">
import { getCurrentInstance, ref } from 'vue'

const { proxy } = getCurrentInstance()!
defineProps({
  back: {
    type: Boolean,
    default: false
  }
})
function min() {
  proxy.$winControl.min()
}

const iconBig = 'icon-v-yk_fangkuai'
const iconSmall = 'icon-v-xiaoxitixingchuangkoudanchufangshi'
const icon = ref(iconBig)

function max() {
  proxy.$winControl.max()
  icon.value = icon.value === iconBig ? iconSmall : iconBig
}

function close() {
  proxy.$winControl.close()
}

function backHistory() {
  proxy.$router.go(-1)
}
</script>
<style lang="less" scoped>
@import '../assets/styles/theme.less';

.im-top {
  height: 3rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  z-index: 2;
  right: 0;
  width: 100%;

  .back {
    padding: 0.8rem;
    display: block;
    cursor: pointer;
    color: #666666;
  }

  a {
    display: inline-block;
    color: #ffffff;
    text-decoration: none;
    padding: 10px;

    i {
      color: #666666;
      font-size: 1.4rem;
      font-weight: bolder;
    }

    :hover {
      background-color: #dddddd;
    }

    .text-right {
      float: right;
      width: 2.4rem;
      height: 2.4rem;
      display: inline-block;
      padding: 0.5rem;
      text-align: center;
    }
  }
}
</style>
