import express from 'express'
import cors from 'cors'
import fs from 'fs'
import path from 'path'

const app = express()
const port = Number(process.env.API_PORT || process.env.PORT || 3001)
const configFile = path.resolve(process.cwd(), 'server', 'backend.config.json')

app.use(cors())
app.use(express.json())

function loadConfig() {
  try {
    const raw = fs.readFileSync(configFile, 'utf-8')
    return JSON.parse(raw)
  } catch (err) {
    return { backendBaseUrl: 'http://localhost:7001' }
  }
}

function saveConfig(config) {
  fs.writeFileSync(configFile, JSON.stringify(config, null, 2), 'utf-8')
}

let currentConfig = loadConfig()

app.get('/api/config', (req, res) => {
  currentConfig = loadConfig()
  res.json(currentConfig)
})

app.put('/api/config', (req, res) => {
  const payload = req.body
  if (!payload || typeof payload.backendBaseUrl !== 'string') {
    return res.status(400).json({ message: 'backendBaseUrl is required and must be a string.' })
  }

  currentConfig = { backendBaseUrl: payload.backendBaseUrl }
  saveConfig(currentConfig)
  res.json(currentConfig)
})

app.get('/api/messages', async (req, res) => {
  const backendBaseUrl = currentConfig.backendBaseUrl || 'http://localhost:7001'
  const url = new URL('/messages', backendBaseUrl)

  Object.keys(req.query).forEach(key => {
    if (req.query[key] !== undefined) {
      url.searchParams.set(key, String(req.query[key]))
    }
  })

  try {
    const backendResponse = await fetch(url.toString(), {
      method: 'GET',
      headers: {
        accept: 'application/json'
      }
    })

    const data = await backendResponse.text()
    res.status(backendResponse.status)
    res.set('Content-Type', backendResponse.headers.get('content-type') || 'application/json')
    res.send(data)
  } catch (error) {
    console.error('Proxy error:', error)
    res.status(502).json({ message: 'Failed to proxy request to backend server.', detail: String(error) })
  }
})

const server = app.listen(port, () => {
  console.log(`Node proxy server running on http://localhost:${port}`)
  console.log(`Proxying /api/messages to backend server configured in ${configFile}`)
})

server.on('error', (error) => {
  if (error.code === 'EADDRINUSE') {
    console.error(`端口 ${port} 已被占用。请停止占用该端口或通过 API_PORT 环境变量指定其他端口。`)
    process.exit(1)
  }
  throw error
})
