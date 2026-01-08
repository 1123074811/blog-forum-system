# ER图工具使用说明

## 功能概述

这是一个强大的ER图（实体关系图）绘制工具，支持从SQL文件导入数据库表结构，自动生成实体和属性，并提供丰富的手动编辑功能。

## 主要特性

### 1. SQL导入功能
- 支持MySQL CREATE TABLE语句解析
- 自动识别表名、字段名、字段类型
- 自动识别主键（PRIMARY KEY、AUTO_INCREMENT）
- 支持COMMENT注释作为中文名称
- 内置常用字段名英文转中文映射

### 2. 自动布局
- 实体以矩形显示，居中放置
- 属性以椭圆显示，围绕实体放射状分布
- 主键属性显示下划线标识
- 多个实体自动横向排列，避免重叠

### 3. 交互功能
- 拖拽移动实体和属性
- 鼠标滚轮缩放画布
- 撤销/重做操作
- 多种绘图工具（实体、属性、关系、文本）

### 4. 导出功能
- 导出为PNG图片
- 支持高分辨率导出
- 自动保存到localStorage

## 使用步骤

### 1. 访问工具
1. 进入博客系统
2. 点击导航栏的"发现"
3. 选择"ER图工具"卡片

### 2. 导入SQL
1. 点击工具栏的"导入SQL"按钮
2. 选择SQL文件或直接粘贴SQL内容
3. 点击"导入"按钮
4. 系统自动解析并生成ER图

### 3. 编辑图形
1. 使用"选择"工具拖拽移动元素
2. 使用其他工具添加新的实体、属性等
3. 双击元素可以编辑名称
4. 使用缩放功能调整视图

### 4. 导出结果
1. 点击"导出图片"按钮
2. 自动下载PNG格式的ER图

## 支持的SQL语法

```sql
CREATE TABLE table_name (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    field_name VARCHAR(50) NOT NULL COMMENT '字段描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (field_name) REFERENCES other_table(id)
);
```

## 字段名映射示例

系统内置了常用字段名的中文映射：

- `id` → `ID`
- `user_id` → `用户ID`
- `username` → `用户名`
- `email` → `邮箱`
- `created_at` → `创建时间`
- `updated_at` → `更新时间`
- 等等...

## 快捷键

- `Ctrl+Z` / `Cmd+Z`: 撤销
- `Ctrl+Shift+Z` / `Cmd+Shift+Z`: 重做
- `Ctrl+S` / `Cmd+S`: 保存项目
- `Ctrl+A` / `Cmd+A`: 全选
- `Delete` / `Backspace`: 删除选中元素

## 注意事项

1. 目前主要支持MySQL语法
2. 复杂的SQL语句可能需要简化处理
3. 建议使用标准的CREATE TABLE语句
4. COMMENT注释会优先作为中文名称使用

## 技术实现

- 前端框架：Vue 3 + Element Plus
- 画布渲染：Konva.js + vue-konva
- 数据存储：localStorage（自动保存）
- SQL解析：自定义JavaScript解析器

## 未来计划

- [ ] 支持更多数据库类型（PostgreSQL、SQLite等）
- [ ] 添加关系连线的自动生成
- [ ] 支持导出为SVG、PDF格式
- [ ] 添加协作编辑功能
- [ ] 支持从数据库直连导入

---

如有问题或建议，请联系开发团队。