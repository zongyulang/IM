<template>
  <span v-if="user" class="u-name" @click="showUser(props.id, false)">{{ user.name }}</span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useUserStore } from '@renderer/store/userStore'
import showUser from '@renderer/components/user-modal'

interface IProps {
  id: string
}

const props = defineProps<IProps>()

const user = computed(() => {
  return useUserStore().getMapUser(props.id)
})
</script>

<style scoped>
.u-name {
  cursor: pointer;
}
</style>
