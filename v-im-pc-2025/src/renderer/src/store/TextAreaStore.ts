import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useTextAreaStore = defineStore('messageTextArea', () => {
  const messageTextArea = ref<HTMLElement>()

  const setMessageTextArea = (el: HTMLElement) => {
    messageTextArea.value = el
  }

  const getMessageTextArea = () => {
    return messageTextArea.value
  }

  return {
    messageTextArea,
    setMessageTextArea,
    getMessageTextArea
  }
})
