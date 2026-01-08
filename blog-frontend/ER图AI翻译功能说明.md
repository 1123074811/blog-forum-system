# ER图工具AI翻译功能说明

## 功能概述

ER图工具现已集成智谱AI翻译功能，当导入SQL文件时，系统会自动将英文的表名和字段名翻译成中文，提升ER图的可读性。

## 翻译机制

### 1. 三级翻译策略

1. **注释优先**: 如果SQL字段有COMMENT注释，直接使用注释内容
2. **映射表查找**: 查找内置的英中映射表（包含常用数据库字段）
3. **AI智能翻译**: 超出映射表范围时，调用智谱AI进行翻译

### 2. 内置映射表

系统预置了200+常用数据库字段的中英文映射，包括：
- 基础字段：id, name, title, content, description等
- 时间字段：created_at, updated_at, deleted_at等
- 用户相关：user_id, username, password, email等
- 状态字段：status, is_active, is_deleted等
- 业务字段：price, amount, count, level等

### 3. AI翻译特性

- **智能识别**: 自动识别下划线分隔的复合字段名
- **上下文理解**: 区分表名和字段名的翻译上下文
- **结果缓存**: 翻译结果自动缓存，避免重复调用
- **降级处理**: AI翻译失败时保留原英文名称

## 使用方法

### 1. 导入SQL文件

1. 点击"导入SQL"按钮
2. 选择SQL文件或直接粘贴SQL内容
3. 点击"导入并生成ER图"
4. 系统会显示"AI翻译解析中..."进度提示
5. 完成后自动生成带中文标签的ER图

### 2. 支持的SQL格式

```sql
-- 标准CREATE TABLE语句
CREATE TABLE employees (
    employee_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '员工ID',
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    department_code VARCHAR(20),
    salary DECIMAL(10,2),
    hire_date DATE,
    is_active BOOLEAN DEFAULT TRUE
);
```

### 3. 翻译示例

| 原英文字段 | 翻译结果 | 翻译来源 |
|-----------|---------|---------|
| employee_id | 员工ID | 映射表 |
| first_name | 名字 | AI翻译 |
| department_code | 部门代码 | AI翻译 |
| salary | 薪资 | AI翻译 |
| hire_date | 入职日期 | AI翻译 |
| is_active | 是否激活 | 映射表 |

## 技术实现

### 前端实现
- 异步SQL解析器支持AI翻译
- 翻译结果本地缓存机制
- 优雅的错误处理和降级策略

### 后端实现
- 智谱AI GLM-4-Flash模型调用
- 专门的翻译提示词优化
- RESTful翻译API接口

### API接口
```
POST /api/translate
Content-Type: application/json

{
  "text": "employee_name",
  "context": "database_field"
}
```

## 注意事项

1. **网络要求**: 需要网络连接以调用智谱AI API
2. **翻译质量**: AI翻译结果可能需要人工校验
3. **性能考虑**: 大量字段首次翻译可能需要较长时间
4. **缓存机制**: 翻译结果会缓存，重复导入相同字段会很快

## 故障排除

### 翻译失败
- 检查网络连接
- 确认智谱AI API密钥配置
- 查看浏览器控制台错误信息

### 翻译结果不理想
- 可以手动双击图形元素编辑文本
- 建议在SQL中添加COMMENT注释
- 常用字段可以提交到映射表中

## 更新日志

- **v1.0**: 基础AI翻译功能
- **v1.1**: 添加缓存机制和错误处理
- **v1.2**: 优化翻译提示词和结果质量