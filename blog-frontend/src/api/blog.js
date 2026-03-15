import api from './index'

export const login = (data) => api.post('/auth/login', data)
export const register = (data) => api.post('/auth/register', data)
export const sendRegisterCode = (email) => api.post('/auth/send-register-code', { email })
export const refreshToken = () => api.post('/auth/refresh', null, {
  headers: { Authorization: `Bearer ${localStorage.getItem('refreshToken') || ''}` }
})
export const getCaptcha = () => api.get('/auth/captcha')
export const forgotPassword = (username) => api.post('/auth/forgot-password', { username })
export const verifyCode = (data) => api.post('/auth/verify-code', data)
export const resetPassword = (data) => api.post('/auth/reset-password', data)

export const getArticles = (params) => api.get('/articles', { params })
export const getArticle = (id) => api.get(`/articles/${id}`)
export const createArticle = (data) => api.post('/articles', data)
export const updateArticle = (id, data) => api.put(`/articles/${id}`, data)
export const deleteArticle = (id) => api.delete(`/articles/${id}`)
export const crawlArticle = (url) => api.post('/articles/crawl', { url })

export const getComments = (articleId) => api.get(`/articles/${articleId}/comments`)
export const createComment = (data) => api.post('/comments', data)
export const deleteComment = (id) => api.delete(`/comments/${id}`)
export const likeComment = (id) => api.post(`/comments/${id}/like`)
export const unlikeComment = (id) => api.delete(`/comments/${id}/like`)

export const getUser = (id) => api.get(`/users/${id}`)
export const updateUser = (id, data) => api.put(`/users/${id}`, data)
export const followUser = (id) => api.post(`/users/${id}/follow`)
export const unfollowUser = (id) => api.delete(`/users/${id}/follow`)

export const getCategories = () => api.get('/categories')
export const getTags = () => api.get('/tags')
export const createUserTag = (name) => api.post('/tags', { name })

export const uploadFile = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return api.post('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// Admin APIs
export const getStatistics = (params) => api.get('/admin/statistics', { params })
export const getAdminUsers = () => api.get('/admin/users')
export const deleteUser = (id) => api.delete(`/admin/users/${id}`)
export const getAdminArticles = () => api.get('/admin/articles')
export const adminDeleteArticle = (id) => api.delete(`/admin/articles/${id}`)
export const getAdminComments = () => api.get('/admin/comments')
export const adminDeleteComment = (id) => api.delete(`/admin/comments/${id}`)
export const getAdminCategories = () => api.get('/admin/categories')
export const createCategory = (data) => api.post('/admin/categories', data)
export const updateCategory = (id, data) => api.put(`/admin/categories/${id}`, data)
export const deleteCategory = (id) => api.delete(`/admin/categories/${id}`)

// Site Info APIs
export const getSiteInfo = () => api.get('/site-info')
export const updateSiteInfo = (data) => api.put('/admin/site-info', data)

// Announcement APIs
export const getAnnouncements = () => api.get('/announcements')
export const getAllAnnouncements = () => api.get('/admin/announcements')
export const createAnnouncement = (data) => api.post('/admin/announcements', data)
export const updateAnnouncement = (id, data) => api.put(`/admin/announcements/${id}`, data)
export const deleteAnnouncement = (id) => api.delete(`/admin/announcements/${id}`)
export const getAdminTags = () => api.get('/admin/tags')
export const createTag = (data) => api.post('/admin/tags', data)
export const updateTag = (id, data) => api.put(`/admin/tags/${id}`, data)
export const deleteTag = (id) => api.delete(`/admin/tags/${id}`)
export const getAdminFavorites = () => api.get('/admin/favorites')
export const deleteAdminFavorite = (id) => api.delete(`/admin/favorites/${id}`)

// Quiz APIs
export const importQuiz = (content) => api.post('/quiz/import', { content })
export const getQuizList = () => api.get('/quiz')
export const getPublicQuizList = () => api.get('/quiz/public')
export const getQuiz = (id) => api.get(`/quiz/${id}`)
export const getQuizQuestions = (id, page = 1, size = 50) => api.get(`/quiz/${id}/questions`, { params: { page, size } })
export const deleteQuiz = (id) => api.delete(`/quiz/${id}`)
export const toggleQuizPublic = (id, isPublic) => api.put(`/quiz/${id}/public`, null, { params: { isPublic } })
export const uploadQuizFile = (file, title) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('title', title)
  return api.post('/quiz/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
}

// Album APIs
export const createAlbum = (data) => api.post('/albums', data)
export const updateAlbum = (id, data) => api.put(`/albums/${id}`, data)
export const deleteAlbum = (id) => api.delete(`/albums/${id}`)
export const getAlbum = (id) => api.get(`/albums/${id}`)
export const getMyAlbums = () => api.get('/albums/my')
export const getPublicAlbums = (page = 1, size = 20) => api.get('/albums/public', { params: { page, size } })
export const toggleAlbumPublic = (id, isAnonymous) => api.post(`/albums/${id}/toggle-public`, null, { params: { isAnonymous } })

// Media APIs
export const uploadMedia = (file, albumId, title, description) => {
  const formData = new FormData()
  formData.append('file', file)
  if (albumId) formData.append('albumId', albumId)
  if (title) formData.append('title', title)
  if (description) formData.append('description', description)
  return api.post('/media/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
export const updateMedia = (id, title, description) => api.put(`/media/${id}`, null, { params: { title, description } })
export const deleteMedia = (id) => api.delete(`/media/${id}`)
export const getMedia = (id) => api.get(`/media/${id}`)
export const getAlbumMedia = (albumId) => api.get(`/media/album/${albumId}`)
export const getMyMedia = (type, page = 1, size = 20) => api.get('/media/my', { params: { type, page, size } })
export const getPublicMedia = (type, page = 1, size = 20) => api.get('/media/public', { params: { type, page, size } })
export const toggleMediaPublic = (id, isAnonymous) => api.post(`/media/${id}/toggle-public`, null, { params: { isAnonymous } })

// Notification APIs
export const getNotifications = () => api.get('/notifications')
export const getUnreadCount = () => api.get('/notifications/unread-count')
export const markAsRead = (id) => api.post(`/notifications/${id}/read`)
export const markAllAsRead = () => api.post('/notifications/read-all')

// TreeHole APIs
export const getTreeHoles = () => api.get('/tree-hole')
export const createTreeHole = (data) => api.post('/tree-hole', data)

// Message APIs
export const getConversations = () => api.get('/messages/conversations')
export const getMessages = (conversationId) => api.get(`/messages/conversations/${conversationId}`)
export const sendMessage = (data) => api.post('/messages/send', data)
export const uploadMessageFile = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return api.post('/messages/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
}
export const getMessageUnreadCount = () => api.get('/messages/unread')
export const getFriends = () => api.get('/messages/friends')
export const checkMutualFollow = (userId) => api.get(`/messages/check-mutual/${userId}`)
