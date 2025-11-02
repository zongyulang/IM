<template>
  <ul class="table-right-menu">
    <!-- 循环菜单项，事件带参数抛出 -->
    <li
      v-for="item in keywordFilter(rightClickInfo?.menuList)"
      :key="item.btnName"
      class="table-right-menu-item"
      @click.stop="fnHandler(item)"
    >
      <div class="table-right-menu-item-btn">
        <span>{{ item.btnName }}</span>
      </div>
    </li>
  </ul>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import type { RightClickMenu } from '@renderer/mode/RightClickMenu'
import { storeToRefs } from 'pinia'
import { useAtStore } from '@renderer/store/atStore'
import { match } from 'pinyin-pro'

const atStore = useAtStore()
const { keyword } = storeToRefs(atStore)
const props = defineProps({
  // 接收右键点击的信息
  rightClickInfo: {
    type: Object,
    default: () => {
      return {
        position: {
          // 右键点击的位置
          x: null,
          y: null
        },
        menuList: new Array<RightClickMenu>()
      }
    }
  },
  // 重要参数，用于标识是哪个右键菜单dom元素
  classIndex: {
    type: Number,
    default: 0
  },
  closeRightMenu: {
    type: Function,
    default: () => {
      console.log()
    }
  }
})
const hide = (e: MouseEvent) => {
  //鼠标左键点击隐藏右键菜单
  if (e.button === 0) {
    //防止先执行
    setTimeout(() => {
      props.closeRightMenu()
      atStore.setKeyword('')
    }, 0)
  }
}
const fnHandler = (item: RightClickMenu) => {
  item.fn()
  props.closeRightMenu()
}

const keywordFilter = (items: RightClickMenu[]): RightClickMenu[] => {
  return items.filter((item) => {
    return !!(keyword.value.trim() === '' || match(item.btnName, keyword.value))
  })
}

onMounted(() => {
  const x = props.rightClickInfo?.position.x // 获取x轴坐标
  const y = props.rightClickInfo?.position.y // 获取y轴坐标
  const innerWidth = window.innerWidth // 获取页面可是区域宽度，即页面的宽度
  const innerHeight = window.innerHeight // 获取可视区域高度，即页面的高度
  /**
   * 注意，这里要使用getElementsByClassName去选中对应dom，因为右键菜单组件可能被多处使用
   * classIndex标识就是去找到对应的那个右键菜单组件的，需要加的
   * */
  const menu: HTMLElement = document.getElementsByClassName('table-right-menu')[
    props.classIndex
  ] as HTMLElement
  menu.style.display = 'block'
  let menuHeight = props.rightClickInfo.menuList.length * 30 // 菜单容器高
  menuHeight = menuHeight > 180 ? 180 : menuHeight
  const menuWidth = 180 // 菜单容器宽
  // 菜单的位置计算
  menu.style.height = menuWidth + 'px'
  menu.style.top = (y + menuHeight > innerHeight ? y - menuHeight : y) + 'px'
  menu.style.left = (x + menuWidth > innerWidth ? innerWidth - menuWidth : x) + 'px'
  // 因为菜单还要关闭，就绑定一个鼠标点击事件，通过e.button判断点击的是否是左键，左键关闭菜单
  document.addEventListener('mouseup', hide, false)
})
</script>

<style lang="less" scoped>
.table-right-menu {
  color: #333;
  background: #fff;
  border-radius: 4px;
  list-style-type: none;
  box-shadow: 2px 2px 3px 0 rgba(0, 0, 0, 0.3);
  font-size: 12px;
  font-weight: 500;
  box-sizing: border-box;
  padding: 4px 0;
  // 固定定位，抬高层级，初始隐藏，右击时置为display:block显示
  position: fixed;
  z-index: 3000;
  max-height: 180px;
  width: 120px;
  overflow-y: scroll;
  //display: none;
  .table-right-menu-item {
    box-sizing: border-box;
    padding: 6px 12px;
    border-radius: 4px;
    transition: all 0.36s;
    cursor: pointer;
    .table-right-menu-item-btn {
      .iii {
        margin-right: 4px;
      }
    }
  }
  .table-right-menu-item:hover {
    background-color: #ebf5ff;
    color: #6bacf2;
  }
}
</style>
