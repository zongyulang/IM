<template>
  <router-view></router-view>
</template>

<script setup lang="ts">
</script>
<style lang="less">
html {
  font-size: 16px;
  height: 100%;
}
body {
  height: 100%;
  margin: 0;
  padding: 0;
}
#v-im-app {
  width: 100%;
  height: 100%;
  margin: 0 auto;
}
//引用
#quote {
  position: absolute;
}
/* 定义旋转动画 */
@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 为自定义类名添加样式 */
.loading-icon .el-message__icon {
  animation: spin 2s linear infinite; /* 应用旋转动画 */
}

/* 全局样式，不使用scoped */
.el-drawer__header {
  margin-bottom: 10px !important;
}
</style>
