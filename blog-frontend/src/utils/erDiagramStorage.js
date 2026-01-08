// ER图项目存储管理
export class ERDiagramStorage {
  constructor() {
    this.storageKey = 'er_diagram_projects'
    this.currentProjectKey = 'er_diagram_current_project'
  }

  /**
   * 获取所有项目列表
   */
  getAllProjects() {
    try {
      const projects = localStorage.getItem(this.storageKey)
      return projects ? JSON.parse(projects) : {}
    } catch (error) {
      console.error('获取项目列表失败:', error)
      return {}
    }
  }

  /**
   * 获取当前项目ID
   */
  getCurrentProjectId() {
    return localStorage.getItem(this.currentProjectKey) || null
  }

  /**
   * 设置当前项目ID
   */
  setCurrentProjectId(projectId) {
    localStorage.setItem(this.currentProjectKey, projectId)
  }

  /**
   * 创建新项目
   */
  createProject(name, description = '') {
    const projectId = this.generateProjectId()
    const project = {
      id: projectId,
      name,
      description,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      data: {
        entities: [],
        relationships: [],
        shapes: [],
        canvasState: {
          scale: 1,
          position: { x: 0, y: 0 }
        }
      }
    }

    const projects = this.getAllProjects()
    projects[projectId] = project
    
    this.saveProjects(projects)
    this.setCurrentProjectId(projectId)
    
    return project
  }

  /**
   * 获取项目
   */
  getProject(projectId) {
    const projects = this.getAllProjects()
    return projects[projectId] || null
  }

  /**
   * 保存项目数据
   */
  saveProject(projectId, data) {
    const projects = this.getAllProjects()
    if (projects[projectId]) {
      projects[projectId].data = data
      projects[projectId].updatedAt = new Date().toISOString()
      this.saveProjects(projects)
      return true
    }
    return false
  }

  /**
   * 删除项目
   */
  deleteProject(projectId) {
    const projects = this.getAllProjects()
    if (projects[projectId]) {
      delete projects[projectId]
      this.saveProjects(projects)
      
      // 如果删除的是当前项目，清除当前项目ID
      if (this.getCurrentProjectId() === projectId) {
        localStorage.removeItem(this.currentProjectKey)
      }
      
      return true
    }
    return false
  }

  /**
   * 重命名项目
   */
  renameProject(projectId, newName) {
    const projects = this.getAllProjects()
    if (projects[projectId]) {
      projects[projectId].name = newName
      projects[projectId].updatedAt = new Date().toISOString()
      this.saveProjects(projects)
      return true
    }
    return false
  }

  /**
   * 复制项目
   */
  duplicateProject(projectId, newName) {
    const originalProject = this.getProject(projectId)
    if (!originalProject) return null

    const newProjectId = this.generateProjectId()
    const newProject = {
      ...originalProject,
      id: newProjectId,
      name: newName,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      data: JSON.parse(JSON.stringify(originalProject.data)) // 深拷贝
    }

    const projects = this.getAllProjects()
    projects[newProjectId] = newProject
    this.saveProjects(projects)

    return newProject
  }

  /**
   * 导出项目数据
   */
  exportProject(projectId) {
    const project = this.getProject(projectId)
    if (!project) return null

    return {
      version: '1.0',
      exportTime: new Date().toISOString(),
      project
    }
  }

  /**
   * 导入项目数据
   */
  importProject(exportData) {
    try {
      if (!exportData.project) {
        throw new Error('无效的项目数据')
      }

      const project = exportData.project
      const newProjectId = this.generateProjectId()
      
      // 创建新项目
      const newProject = {
        ...project,
        id: newProjectId,
        name: `${project.name} (导入)`,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      }

      const projects = this.getAllProjects()
      projects[newProjectId] = newProject
      this.saveProjects(projects)

      return newProject
    } catch (error) {
      console.error('导入项目失败:', error)
      return null
    }
  }

  /**
   * 保存项目列表到localStorage
   */
  saveProjects(projects) {
    try {
      localStorage.setItem(this.storageKey, JSON.stringify(projects))
    } catch (error) {
      console.error('保存项目失败:', error)
    }
  }

  /**
   * 生成项目ID
   */
  generateProjectId() {
    return 'project_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
  }

  /**
   * 清空所有项目（慎用）
   */
  clearAllProjects() {
    localStorage.removeItem(this.storageKey)
    localStorage.removeItem(this.currentProjectKey)
  }

  /**
   * 获取存储使用情况
   */
  getStorageInfo() {
    const projects = this.getAllProjects()
    const projectCount = Object.keys(projects).length
    const dataSize = JSON.stringify(projects).length
    
    return {
      projectCount,
      dataSize,
      dataSizeFormatted: this.formatBytes(dataSize)
    }
  }

  /**
   * 格式化字节数
   */
  formatBytes(bytes) {
    if (bytes === 0) return '0 Bytes'
    const k = 1024
    const sizes = ['Bytes', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }
}

export default ERDiagramStorage