<template>
  <el-dialog v-model="visible" title="举报内容" width="420px" :close-on-click-modal="false" destroy-on-close>
    <el-form label-position="top">
      <el-form-item label="举报类型">
        <el-tag>{{ targetType === 'article' ? '文章' : '评论' }}</el-tag>
      </el-form-item>
      <el-form-item label="举报原因">
        <el-select v-model="reason" placeholder="请选择举报原因" class="w-full">
          <el-option label="垃圾广告" value="spam" />
          <el-option label="色情低俗" value="porn" />
          <el-option label="暴力血腥" value="violence" />
          <el-option label="违法违规" value="illegal" />
          <el-option label="人身攻击" value="harassment" />
          <el-option label="虚假信息" value="misinformation" />
          <el-option label="侵权内容" value="copyright" />
          <el-option label="其他" value="other" />
        </el-select>
      </el-form-item>
      <el-form-item label="补充说明（选填）">
        <el-input v-model="description" type="textarea" :rows="3" placeholder="请简要描述举报原因..." />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">提交举报</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { createReport } from '@/api/blog'

const visible = ref(false)
const targetType = ref('article')
const targetId = ref(null)
const reason = ref('')
const description = ref('')
const submitting = ref(false)

const emit = defineEmits(['reported'])

const open = (type, id) => {
  targetType.value = type
  targetId.value = id
  reason.value = ''
  description.value = ''
  visible.value = true
}

const submit = async () => {
  if (!reason.value) {
    return
  }
  submitting.value = true
  try {
    const res = await createReport({
      targetType: targetType.value,
      targetId: targetId.value,
      reason: reason.value,
      description: description.value
    })
    if (res.success) {
      visible.value = false
      emit('reported')
    }
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>
