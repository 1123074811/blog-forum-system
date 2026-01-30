# 忘记密码功能实现文档

## 功能概述
为博客论坛项目添加忘记密码功能，允许用户通过账号触发邮箱验证码来重置密码。

## 业务流程
1. 用户输入账号
2. 系统根据账号查找用户邮箱
3. 生成6位验证码并发送到用户邮箱
4. 用户输入收到的验证码和新密码
5. 系统验证验证码并更新用户密码

## 技术实现

### 后端实现

#### 1. DTO类
- `ForgotPasswordRequest.java`: 包含账号字段
- `VerifyCodeRequest.java`: 包含账号和验证码字段
- `ResetPasswordRequest.java`: 包含账号、验证码和新密码字段

#### 2. 工具类
- `PasswordUtil.java`: 提供密码格式验证方法

#### 3. 邮件服务
- `EmailService.java`: 
  - 添加了验证码存储和验证功能
  - 发送重置密码邮件功能
  - 验证码有效期为30分钟

#### 4. 控制器
- `AuthController.java`:
  - `/api/auth/forgot-password`: 处理忘记密码请求
  - `/api/auth/verify-code`: 验证账号与验证码
  - `/api/auth/reset-password`: 处理密码重置请求

#### 5. 安全措施
- 验证码通过Redis存储，有效期30分钟
- 验证码只能使用一次
- 密码重置前验证密码强度

### 前端实现

#### 1. 页面组件
- `ForgotPassword.vue`: 忘记密码页面，包含两步骤流程

#### 2. API调用
- `forgotPassword()`: 调用后端发送验证码接口
- `resetPassword()`: 调用后端重置密码接口

#### 3. 用户体验
- 响应式设计，适配不同设备
- 表单验证和错误提示
- 倒计时显示验证码有效期
- 加载状态指示器

## 接口详情

### 发送验证码
```
POST /api/auth/forgot-password
Content-Type: application/json

{
  "username": "user123"
}

响应:
{
  "success": true,
  "data": "验证码已发送到邮箱",
  "message": null
}
```

### 重置密码
```
POST /api/auth/reset-password
Content-Type: application/json

{
  "username": "user123",
  "code": "123456",
  "password": "new_password"
}

响应:
{
  "success": true,
  "data": "密码重置成功",
  "message": null
}
```

## 验证规则
- 验证码为6位数字
- 新密码长度至少6位
- 验证码30分钟内有效
- 验证码仅能使用一次

## 错误处理
- 账号不存在：返回"账号不存在"
- 验证码错误：返回"验证码错误或已过期"
- 邮件发送失败：返回"邮件发送失败"
- 参数不完整：返回具体错误信息

## 安全考虑
- 使用HTTPS传输敏感信息
- 验证码一次性使用防止重放攻击
- 密码使用BCrypt加密存储
- Redis中验证码有过期时间限制
