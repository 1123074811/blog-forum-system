// 测试SQL解析器
import SQLParser from './sqlParser.js'

const testSQL = `
-- 用户表
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱地址',
    nickname VARCHAR(50) COMMENT '昵称',
    avatar VARCHAR(255) COMMENT '头像链接',
    created_at VARCHAR(30) COMMENT '创建时间'
);

-- 文章表
CREATE TABLE articles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '文章ID',
    user_id BIGINT NOT NULL COMMENT '作者ID',
    title VARCHAR(255) NOT NULL COMMENT '文章标题',
    content LONGTEXT COMMENT '文章内容',
    status VARCHAR(20) DEFAULT 'draft' COMMENT '文章状态',
    view_count INT DEFAULT 0 COMMENT '浏览次数',
    created_at VARCHAR(30) COMMENT '创建时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
`

// 测试解析
function testParser() {
  const parser = new SQLParser()
  const tables = parser.parseSQLContent(testSQL)
  
  console.log('解析结果:', tables)
  
  tables.forEach(table => {
    console.log(`\n表名: ${table.name} (${table.chineseName})`)
    console.log('字段:')
    table.fields.forEach(field => {
      const primaryMark = field.isPrimary ? ' [主键]' : ''
      console.log(`  - ${field.name} (${field.chineseName}) ${field.type}${primaryMark}`)
    })
    
    if (table.foreignKeys.length > 0) {
      console.log('外键:')
      table.foreignKeys.forEach(fk => {
        console.log(`  - ${fk.field} -> ${fk.referencedTable}.${fk.referencedField}`)
      })
    }
  })
}

// 如果在浏览器环境中运行
if (typeof window !== 'undefined') {
  window.testSQLParser = testParser
}

export { testParser }