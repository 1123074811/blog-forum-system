<template>
  <el-popover placement="top" :width="300" trigger="click">
    <template #reference>
      <span class="emoji-trigger cursor-pointer">😊</span>
    </template>
    <div class="emoji-grid">
      <span v-for="e in emojis" :key="e" class="emoji-item" @click="$emit('select', e)">{{ e }}</span>
    </div>
  </el-popover>
</template>

<script setup>
defineEmits(['select'])
const emojis = [
  '😀','😁','😂','🤣','😃','😄',
  '😅','😆','😉','😊','😋','😎',
  '😍','🥰','😘','😗','🙂','🤗',
  '🤩','🤔','🤨','😐','😑','😶',
  '🙄','😏','😣','😥','😮','🤐',
  '😯','😪','😫','😴','🤤','😛',
  '😜','😝','🤑','🤠','😈','👿',
  '💀','👻','👽','🤖','💩','😺',
  '😸','😹','😻','😼','😽','🙀',
  '🙈','🙉','🙊','💋','💌','💘',
  '💝','💖','💗','💓','💞','💕',
  '❤️','🧡','💛','💚','💙','💜',
  '💯','💢','💥','💫','💦','💨',
  '💣','💬','👋','✋','👌','✌️',
  '👈','👉','👆','👇','👍','👎',
  '✊','👊','👏','🙌','👐','🤝',
  '🙏','💪','👀','👄','🔥','⭐'
]
</script>

<style scoped>
.emoji-grid {
  display: grid;
  grid-template-columns: repeat(6, 42px);
  gap: 4px;
  max-height: 220px;
  overflow-y: auto;
  overflow-x: hidden;
}
.emoji-item {
  width: 42px;
  height: 36px;
  font-size: 22px;
  cursor: pointer;
  text-align: center;
  line-height: 36px;
  border-radius: 6px;
  transition: background .2s;
}
.emoji-item:hover {
  background: #f0f0f0;
}
.emoji-trigger {
  font-size: 20px;
}
</style>
