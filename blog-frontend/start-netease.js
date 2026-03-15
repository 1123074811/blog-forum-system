// 以 PORT=3001 启动 NeteaseCloudMusicApi（降级备用服务）
process.env.PORT = '3001'
import('./node_modules/NeteaseCloudMusicApi/app.js')
