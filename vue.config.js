'use strict'
const path = require('path')
const defaultSettings = require('./src/settings.js')
const MonacoWebpackPlugin = require('monaco-editor-webpack-plugin')

function resolve(dir) {
  return path.join(__dirname, dir)
}

const name = defaultSettings.title || '' // 标题
const port = process.env.port || process.env.npm_config_port || 28765 // 端口
var serurl="http://localhost:9080"
var baseTarget = process.env.VUE_APP_DEFAULT_TARGET || serurl

var target = process.env.npm_config_target || baseTarget;
console.log("target="+target)

// 是否开启 SSE 原始数据日志调试
const enableSseDebugLog = false

// vue.config.js 配置说明
//官方vue.config.js 参考文档 https://cli.vuejs.org/zh/config/#css-loaderoptions
// 这里只列一部分，具体配置参考文档
module.exports = {
  // 部署生产环境和开发环境下的URL。
  // 默认情况下，Vue CLI 会假设你的应用是被部署在一个域名的根路径上
  // 例如 https://www.ruoyi.vip/。如果应用被部署在一个子路径上，你就需要用这个选项指定这个子路径。例如，如果你的应用被部署在 https://www.ruoyi.vip/admin/，则设置 baseUrl 为 /admin/。
  publicPath: process.env.NODE_ENV === "production" ? "/" : "/",
  // 在npm run build 或 yarn build 时 ，生成文件的目录名称（要和baseUrl的生产环境路径一致）（默认dist）
  outputDir: 'dist',
  // 用于放置生成的静态资源 (js、css、img、fonts) 的；（项目打包之后，静态资源会放在这个文件夹下）
  assetsDir: 'static',
  // 是否开启eslint保存检测，有效值：ture | false | 'error'
  lintOnSave: process.env.NODE_ENV === 'development',
  // 如果你不需要生产环境的 source map，可以将其设置为 false 以加速生产环境构建。
  productionSourceMap: false,
  // webpack-dev-server 相关配置
  devServer: {
    host: '0.0.0.0',
    port: port,
    open: true,
    proxy: {
      // detail: https://cli.vuejs.org/config/#devserver-proxy
      '/maximo': {
        target: target,
        // target: 'http://localhost:9080',

        changeOrigin: true,
        ws: true,  // 支持 WebSocket 和 SSE 长连接
        logLevel: 'debug',  // 启用调试日志
        // SSE 需要禁用缓冲以实现实时传输
        onProxyReq: (proxyReq, req, res) => {
          console.log('代理请求:', req.url, '→', proxyReq.path)
          // 对于 SSE 请求，设置不缓冲
          if (req.headers.accept && req.headers.accept.includes('text/event-stream')) {
            proxyReq.setHeader('Cache-Control', 'no-cache')
            proxyReq.setHeader('Connection', 'keep-alive')
          }
        },
        onProxyRes: (proxyRes, req, res) => {
          console.log('代理响应:', proxyRes.statusCode, req.url)
          console.log('响应头:', JSON.stringify(proxyRes.headers))
          // 对于 SSE 响应，确保不缓冲
          if (proxyRes.headers['content-type'] && proxyRes.headers['content-type'].includes('text/event-stream')) {
            res.setHeader('Cache-Control', 'no-cache')
            res.setHeader('Connection', 'keep-alive')
            res.setHeader('X-Accel-Buffering', 'no')  // 禁用 Nginx 缓冲

            // 监听原始响应数据，检查是否已乱码
            if (enableSseDebugLog) {
              let chunkIndex = 0
              proxyRes.on('data', (chunk) => {
                if (chunkIndex < 5) { // 只打印前5个数据块，避免刷屏
                  const isBuffer = Buffer.isBuffer(chunk)
                  const rawHex = isBuffer ? chunk.slice(0, 200).toString('hex') : '(非Buffer)'
                  const rawText = isBuffer ? chunk.slice(0, 200).toString('utf8') : '(非Buffer)'
                  console.log(`[SSE 原始数据 #${chunkIndex}] hex:`, rawHex)
                  console.log(`[SSE 原始数据 #${chunkIndex}] utf8:`, rawText)
                  console.log(`[SSE 原始数据 #${chunkIndex}] 长度:`, chunk.length, 'bytes')
                  chunkIndex++
                }
              })
            }
          }
        },
        onError: (err, req, res) => {
          console.error('代理错误:', err.message, req.url)
        }
      },
      '/solonapi': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        pathRewrite: {
          '^/solonapi': ''
        }
      }
    },
    disableHostCheck: true
  },
  configureWebpack: {
    name: name,
    resolve: {
      alias: {
        '@': resolve('src')
      }
    },
    plugins: [
      new MonacoWebpackPlugin({
        languages: ['json', 'javascript', 'typescript','python']
      })
    ],
      module: {
          rules: [
              //加上这段 start
              {
                  test: /\.js$/,    //打包规则应用到以css结尾的文件上
                  loader: 'babel-loader',
                  include: [resolve('src'),resolve('node_modules/sks-plugin-el-erp/lib')]
              },
              //加上这段 end
          ]
      }
  },
    chainWebpack(config) {
    config.plugins.delete('preload') // TODO: need test
    config.plugins.delete('prefetch') // TODO: need test

    // set svg-sprite-loader
    config.module
      .rule('svg')
      .exclude.add(resolve('src/assets/icons'))
      .end()
    config.module
      .rule('icons')
      .test(/\.svg$/)
      .include.add(resolve('src/assets/icons'))
      .end()
      .use('svg-sprite-loader')
      .loader('svg-sprite-loader')
      .options({
        symbolId: 'icon-[name]'
      })
      .end()

    config
      .when(process.env.NODE_ENV !== 'development',
        config => {
          config
            .plugin('ScriptExtHtmlWebpackPlugin')
            .after('html')
            .use('script-ext-html-webpack-plugin', [{
            // `runtime` must same as runtimeChunk name. default is `runtime`
              inline: /runtime\..*\.js$/
            }])
            .end()
          config
            .optimization.splitChunks({
              chunks: 'all',
              cacheGroups: {
                libs: {
                  name: 'chunk-libs',
                  test: /[\\/]node_modules[\\/]/,
                  priority: 10,
                  chunks: 'initial' // only package third parties that are initially dependent
                },
                elementUI: {
                  name: 'chunk-elementUI', // split elementUI into a single package
                  priority: 20, // the weight needs to be larger than libs and app or it will be packaged into libs or app
                  test: /[\\/]node_modules[\\/]_?element-ui(.*)/ // in order to adapt to cnpm
                },
                commons: {
                  name: 'chunk-commons',
                  test: resolve('src/components'), // can customize your rules
                  minChunks: 3, //  minimum common number
                  priority: 5,
                  reuseExistingChunk: true
                }
              }
            })
          config.optimization.runtimeChunk('single'),
          {
             from: path.resolve(__dirname, './public/robots.txt'),//防爬虫文件
             to:'./',//到根目录下
          }
        }
      )
  }
}