import api from '@/api'
import { ElMessageBox } from 'element-plus'

export async function getStepUpToken(operation) {
  const { value } = await ElMessageBox.prompt('请输入当前密码以完成高危操作', '二次验证', {
    inputType: 'password',
    inputPlaceholder: '当前密码',
    confirmButtonText: '验证',
    cancelButtonText: '取消'
  })

  const res = await api.post('/auth/step-up/verify', {
    password: value,
    operation
  })

  if (!res.success || !res.data) {
    throw new Error(res.message || '二次验证失败')
  }

  return res.data
}
