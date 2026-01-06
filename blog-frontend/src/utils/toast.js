import { createApp, h, ref } from 'vue'

const toasts = ref([])
let container = null

const Toast = {
  setup() {
    return () => h('div', { class: 'fixed top-4 right-4 z-50 space-y-2' },
      toasts.value.map(toast =>
        h('div', {
          key: toast.id,
          class: 'bg-white dark:bg-gray-800 rounded-lg shadow-lg px-4 py-3 flex items-center gap-3 min-w-[200px] animate-slide-in'
        }, [
          h('span', { class: 'flex-1 text-gray-700 dark:text-gray-200' }, toast.message),
          h('button', {
            class: 'text-gray-400 hover:text-gray-600 dark:hover:text-gray-300',
            onClick: () => removeToast(toast.id)
          }, '×')
        ])
      )
    )
  }
}

function ensureContainer() {
  if (!container) {
    container = document.createElement('div')
    document.body.appendChild(container)
    createApp(Toast).mount(container)
  }
}

function removeToast(id) {
  toasts.value = toasts.value.filter(t => t.id !== id)
}

let id = 0
export function toast(message, duration = 3000) {
  ensureContainer()
  const toastId = ++id
  toasts.value.push({ id: toastId, message })
  setTimeout(() => removeToast(toastId), duration)
}

export default toast
