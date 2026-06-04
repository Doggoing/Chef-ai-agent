<template>
  <div class="ai-avatar-fallback" :class="type">
    <span v-if="type === 'chef'">🍳</span>
    <span v-else-if="type === 'manus'">👨‍🍳</span>
    <span v-else>🤖</span>
  </div>
</template>

<script setup>
defineProps({
  type: {
    type: String,
    default: 'default'
  }
})
</script>

<style scoped>
.ai-avatar-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  border-radius: 50%;
}

.chef {
  background: linear-gradient(45deg, #e67e22, #f39c12);
}

.manus, .default, .super {
  background: linear-gradient(45deg, #2c3e50, #3498db);
}
</style> 