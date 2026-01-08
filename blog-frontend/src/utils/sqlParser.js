// SQL解析器 - 解析MySQL CREATE TABLE语句
export class SQLParser {
  constructor() {
    // 英文转中文映射表
    this.fieldNameMap = {
      // 学生选课系统
      'id': 'ID',
      'student_id': '学生ID',
      'course_id': '课程ID',
      'name': '姓名',
      'course_name': '课程名',
      'department': '所在院系',
      'dormitory': '宿舍',
      'grade': '课程成绩',
      'enrollment_date': '选课日期',
      
      // 用户相关
      'user_id': '用户ID',
      'username': '用户名',
      'password': '密码',
      'email': '邮箱',
      'nickname': '昵称',
      'avatar': '头像',
      'bio': '个人简介',
      'role': '角色',
      'activated': '激活状态',
      
      // 时间相关
      'created_at': '创建时间',
      'updated_at': '更新时间',
      'last_message_time': '最后消息时间',
      'visit_date': '访问日期',
      
      // 文章相关
      'title': '标题',
      'content': '内容',
      'category_id': '分类ID',
      'status': '状态',
      'view_count': '浏览次数',
      'article_id': '文章ID',
      'tag_id': '标签ID',
      
      // 评论相关
      'comment_id': '评论ID',
      'parent_id': '父级ID',
      'like_count': '点赞数',
      
      // 关注相关
      'follower_id': '关注者ID',
      'following_id': '被关注者ID',
      
      // 文件相关
      'file_name': '文件名',
      'file_path': '文件路径',
      'file_size': '文件大小',
      'file_type': '文件类型',
      'file_url': '文件链接',
      
      // 分类标签
      'description': '描述',
      
      // 题库相关
      'quiz_bank_id': '题库ID',
      'question': '题目',
      'options': '选项',
      'answer': '答案',
      'explanation': '解析',
      'sort_order': '排序',
      'question_count': '题目数量',
      'type': '类型',
      
      // 相册媒体
      'album_id': '相册ID',
      'cover_url': '封面链接',
      'is_public': '是否公开',
      'is_anonymous': '是否匿名',
      'media_count': '媒体数量',
      'url': '链接',
      'thumbnail_url': '缩略图链接',
      'source': '来源',
      
      // 统计相关
      'pv': '页面浏览量',
      'uv': '独立访客',
      'new_users': '新用户数',
      'new_articles': '新文章数',
      'new_comments': '新评论数',
      
      // 会话消息
      'conversation_id': '会话ID',
      'user1_id': '用户1ID',
      'user2_id': '用户2ID',
      'last_message_id': '最后消息ID',
      'user1_unread': '用户1未读',
      'user2_unread': '用户2未读',
      'sender_id': '发送者ID',
      'receiver_id': '接收者ID',
      'is_read': '是否已读',
      
      // 网站信息
      'site_name': '网站名称',
      'site_description': '网站描述',
      'site_keywords': '网站关键词',
      'site_logo': '网站Logo',
      'site_favicon': '网站图标',
      'icp_number': 'ICP备案号',
      'police_number': '公安备案号',
      'contact_email': '联系邮箱',
      'contact_phone': '联系电话',
      'contact_address': '联系地址',
      'contact_qq': '联系QQ',
      'contact_wechat': '联系微信',
      'github_url': 'GitHub链接',
      'gitee_url': 'Gitee链接',
      
      // 公告相关
      'is_pinned': '是否置顶',
      'is_active': '是否启用',
      'created_by': '创建者'
    }

    // AI翻译缓存，避免重复调用
    this.aiTranslationCache = new Map()
  }

  /**
   * 解析SQL文件内容
   * @param {string} sqlContent SQL文件内容
   * @returns {Array} 解析后的表结构数组
   */
  async parseSQLContent(sqlContent) {
    const tables = []
    
    // 移除注释和多余空白
    const cleanSQL = this.cleanSQL(sqlContent)
    
    // 匹配所有CREATE TABLE语句
    const createTableRegex = /CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?`?(\w+)`?\s*\(([\s\S]*?)\);/gi
    let match
    
    while ((match = createTableRegex.exec(cleanSQL)) !== null) {
      const tableName = match[1]
      const tableContent = match[2]
      
      const table = await this.parseTable(tableName, tableContent)
      if (table) {
        tables.push(table)
      }
    }
    
    return tables
  }

  /**
   * 清理SQL内容
   */
  cleanSQL(sql) {
    // 移除单行注释
    sql = sql.replace(/--.*$/gm, '')
    // 移除多行注释
    sql = sql.replace(/\/\*[\s\S]*?\*\//g, '')
    return sql
  }

  /**
   * 解析单个表
   */
  async parseTable(tableName, tableContent) {
    const fields = []
    const primaryKeys = []
    const foreignKeys = []
    
    // 分割字段定义
    const lines = tableContent.split(',')
    
    for (let line of lines) {
      line = line.trim()
      if (!line) continue
      
      // 解析字段定义
      if (this.isFieldDefinition(line)) {
        const field = await this.parseField(line)
        if (field) {
          fields.push(field)
          if (field.isPrimary) {
            primaryKeys.push(field.name)
          }
        }
      }
      // 解析主键约束
      else if (line.match(/PRIMARY\s+KEY/i)) {
        const pkFields = this.parsePrimaryKey(line)
        primaryKeys.push(...pkFields)
      }
      // 解析外键约束
      else if (line.match(/FOREIGN\s+KEY/i)) {
        const fk = this.parseForeignKey(line)
        if (fk) {
          foreignKeys.push(fk)
        }
      }
    }

    // 标记主键字段
    fields.forEach(field => {
      if (primaryKeys.includes(field.name)) {
        field.isPrimary = true
      }
    })

    return {
      name: tableName,
      chineseName: await this.translateTableName(tableName),
      fields,
      primaryKeys,
      foreignKeys
    }
  }

  /**
   * 判断是否为字段定义
   */
  isFieldDefinition(line) {
    return !line.match(/^\s*(PRIMARY\s+KEY|FOREIGN\s+KEY|INDEX|UNIQUE|KEY|CONSTRAINT)/i)
  }

  /**
   * 解析字段定义
   */
  async parseField(line) {
    // 匹配字段定义: field_name TYPE [constraints] [COMMENT 'comment']
    const fieldRegex = /^\s*`?(\w+)`?\s+([^\s,]+)(?:\s*\([^)]*\))?\s*(.*?)(?:\s*,\s*)?$/i
    const match = line.match(fieldRegex)
    
    if (!match) return null
    
    const fieldName = match[1]
    const fieldType = match[2]
    const constraints = match[3] || ''
    
    // 检查是否为主键
    const isPrimary = constraints.match(/PRIMARY\s+KEY/i) || constraints.match(/AUTO_INCREMENT/i)
    
    // 提取注释
    const commentMatch = constraints.match(/COMMENT\s+['"](.*?)['"]/i)
    const comment = commentMatch ? commentMatch[1] : ''
    
    return {
      name: fieldName,
      chineseName: await this.translateFieldName(fieldName, comment),
      type: fieldType,
      isPrimary: !!isPrimary,
      comment
    }
  }

  /**
   * 解析主键约束
   */
  parsePrimaryKey(line) {
    const match = line.match(/PRIMARY\s+KEY\s*\(\s*([^)]+)\s*\)/i)
    if (!match) return []
    
    return match[1].split(',').map(field => field.trim().replace(/[`'"]/g, ''))
  }

  /**
   * 解析外键约束
   */
  parseForeignKey(line) {
    const match = line.match(/FOREIGN\s+KEY\s*\(\s*`?(\w+)`?\s*\)\s*REFERENCES\s+`?(\w+)`?\s*\(\s*`?(\w+)`?\s*\)/i)
    if (!match) return null
    
    return {
      field: match[1],
      referencedTable: match[2],
      referencedField: match[3]
    }
  }

  /**
   * 翻译表名
   */
  async translateTableName(tableName) {
    const tableNameMap = {
      'students': '学生',
      'courses': '课程', 
      'enrollments': '选课',
      'users': '用户',
      'articles': '文章',
      'categories': '分类',
      'tags': '标签',
      'article_tags': '文章标签',
      'comments': '评论',
      'comment_likes': '评论点赞',
      'follows': '关注',
      'files': '文件',
      'quiz_banks': '题库',
      'questions': '题目',
      'albums': '相册',
      'media': '媒体',
      'site_visits': '网站访问统计',
      'conversations': '会话',
      'messages': '消息',
      'site_info': '网站信息',
      'announcements': '公告'
    }
    
    // 使用内置映射表
    if (tableNameMap[tableName]) {
      return tableNameMap[tableName]
    }
    
    // 如果没有映射，调用AI翻译
    try {
      const translatedName = await this.translateWithAI(tableName, 'table')
      if (translatedName && translatedName !== tableName) {
        return translatedName
      }
    } catch (error) {
      console.warn('表名AI翻译失败，使用原表名:', error)
    }
    
    return tableName
  }

  /**
   * 翻译字段名
   */
  async translateFieldName(fieldName, comment = '') {
    console.log(`开始翻译字段: ${fieldName}, 注释: ${comment}`)
    
    // 优先使用注释中的中文名
    if (comment && comment.trim()) {
      console.log(`使用注释翻译: ${fieldName} -> ${comment.trim()}`)
      return comment.trim()
    }
    
    // 使用内置映射表
    if (this.fieldNameMap[fieldName]) {
      console.log(`使用映射表翻译: ${fieldName} -> ${this.fieldNameMap[fieldName]}`)
      return this.fieldNameMap[fieldName]
    }
    
    // 如果没有映射，调用智谱AI进行翻译
    try {
      console.log(`调用AI翻译字段: ${fieldName}`)
      const translatedName = await this.translateWithAI(fieldName, 'database_field')
      if (translatedName && translatedName !== fieldName) {
        // 将翻译结果缓存到映射表中，避免重复调用
        this.fieldNameMap[fieldName] = translatedName
        console.log(`AI翻译成功: ${fieldName} -> ${translatedName}`)
        return translatedName
      }
    } catch (error) {
      console.warn('AI翻译失败，使用原字段名:', fieldName, error)
    }
    
    // 如果AI翻译失败，返回原字段名
    console.log(`翻译失败，保留原名: ${fieldName}`)
    return fieldName
  }

  /**
   * 使用智谱AI翻译字段名或表名
   */
  async translateWithAI(text, context = 'database_field') {
    // 检查缓存
    const cacheKey = `${context}:${text}`
    if (this.aiTranslationCache.has(cacheKey)) {
      return this.aiTranslationCache.get(cacheKey)
    }

    // 检查是否为常见的英文单词，如果是则直接翻译
    const commonTranslations = {
      'id': 'ID',
      'name': '名称',
      'title': '标题',
      'content': '内容',
      'description': '描述',
      'status': '状态',
      'type': '类型',
      'category': '分类',
      'tag': '标签',
      'user': '用户',
      'admin': '管理员',
      'phone': '电话',
      'address': '地址',
      'city': '城市',
      'country': '国家',
      'province': '省份',
      'age': '年龄',
      'gender': '性别',
      'birthday': '生日',
      'create_time': '创建时间',
      'update_time': '更新时间',
      'delete_time': '删除时间',
      'is_deleted': '是否删除',
      'sort': '排序',
      'order': '排序',
      'level': '等级',
      'parent': '父级',
      'children': '子级',
      'count': '数量',
      'total': '总计',
      'price': '价格',
      'amount': '金额',
      'discount': '折扣',
      'image': '图片',
      'avatar': '头像',
      'logo': '标志',
      'icon': '图标',
      'url': '链接',
      'link': '链接',
      'path': '路径',
      'file': '文件',
      'size': '大小',
      'weight': '重量',
      'height': '高度',
      'width': '宽度',
      'length': '长度',
      'color': '颜色',
      'style': '样式',
      'theme': '主题',
      'template': '模板',
      'config': '配置',
      'setting': '设置',
      'option': '选项',
      'value': '值',
      'key': '键',
      'code': '代码',
      'number': '编号',
      'serial': '序列号',
      'version': '版本',
      'remark': '备注',
      'note': '注释',
      'comment': '评论',
      'message': '消息',
      'notification': '通知',
      'alert': '警告',
      'error': '错误',
      'success': '成功',
      'info': '信息',
      'warning': '警告',
      'debug': '调试',
      'log': '日志',
      'record': '记录',
      'history': '历史',
      'backup': '备份',
      'restore': '恢复',
      'import': '导入',
      'export': '导出',
      'download': '下载',
      'upload': '上传',
      'publish': '发布',
      'draft': '草稿',
      'review': '审核',
      'approve': '批准',
      'reject': '拒绝',
      'enable': '启用',
      'disable': '禁用',
      'active': '激活',
      'inactive': '未激活',
      'online': '在线',
      'offline': '离线',
      'public': '公开',
      'private': '私有',
      'secret': '秘密',
      'visible': '可见',
      'hidden': '隐藏',
      'locked': '锁定',
      'unlocked': '解锁',
      'expired': '过期',
      'valid': '有效',
      'invalid': '无效'
    }

    // 先检查常见翻译
    const lowerText = text.toLowerCase()
    if (commonTranslations[lowerText]) {
      const result = commonTranslations[lowerText]
      this.aiTranslationCache.set(cacheKey, result)
      return result
    }

    // 处理下划线分隔的字段名
    if (text.includes('_')) {
      const parts = text.split('_')
      const translatedParts = parts.map(part => {
        const lowerPart = part.toLowerCase()
        return commonTranslations[lowerPart] || part
      })
      
      // 如果所有部分都能翻译，则组合返回
      if (translatedParts.every((part, index) => part !== parts[index])) {
        const result = translatedParts.join('')
        this.aiTranslationCache.set(cacheKey, result)
        return result
      }
    }

    // 调用智谱AI API
    try {
      console.log(`发送AI翻译请求: ${text}, 上下文: ${context}`)
      const response = await fetch('/api/translate', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          text: text,
          context: context
        })
      })

      console.log(`AI翻译响应状态: ${response.status}`)
      
      if (!response.ok) {
        throw new Error(`翻译API调用失败: ${response.status}`)
      }

      const result = await response.json()
      console.log(`AI翻译响应结果:`, result)
      
      if (result.success && result.data) {
        const translatedText = result.data.trim()
        // 缓存翻译结果
        this.aiTranslationCache.set(cacheKey, translatedText)
        console.log(`AI翻译成功: ${text} -> ${translatedText}`)
        return translatedText
      }

      throw new Error('翻译结果为空')
    } catch (error) {
      console.warn(`AI翻译失败 (${text}):`, error)
      // 翻译失败时也缓存原文，避免重复调用
      this.aiTranslationCache.set(cacheKey, text)
      throw error
    }
  }
}

export default SQLParser