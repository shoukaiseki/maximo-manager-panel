import { getLanguages } from '@/api/system'

const ENV_SETTINGS_KEY = 'maximo-env-settings'

let languageConfig = null
let languageConfigPromise = null

/**
 * 将语言值规范为语言代码，如 zh-CN / zh_CN / en → ZH / ZH / EN
 */
function normalizeLangcode(value) {
  if (value === null || value === undefined) {
    return ''
  }
  const text = String(value).trim()
  if (!text) {
    return ''
  }
  return text.split(/[-_]/)[0].toUpperCase()
}

/**
 * 加载并缓存后台语言配置（/system/languages）。
 * 同时供默认语言判断和页面下拉选择使用。
 *
 * @returns {Promise<Object|null>} { codes, baseLanguage, mmpDefaultLang }
 */
export function loadLanguageConfig() {
  if (languageConfig) {
    return Promise.resolve(languageConfig)
  }
  if (!languageConfigPromise) {
    languageConfigPromise = getLanguages().then(res => {
      if (res && res.code === 200 && res.data) {
        const data = res.data
        const languages = []
        const seen = {}
        ;(data.languages || []).forEach(item => {
          const code = normalizeLangcode(item.code)
          if (!code || seen[code]) {
            return
          }
          seen[code] = true
          languages.push({
            code,
            name: item.name ? String(item.name) : code,
            base: item.base === true || item.base === 'true'
          })
        })
        languageConfig = {
          codes: languages.map(item => item.code),
          languages,
          baseLanguage: normalizeLangcode(data.baseLanguage),
          mmpDefaultLang: normalizeLangcode(data.mmpDefaultLang)
        }
      }
      if (!languageConfig) {
        // 接口异常时释放缓存, 允许下次调用重试
        languageConfigPromise = null
      }
      return languageConfig
    }).catch(err => {
      console.warn('[mmp-config] 查询支持的语言失败', err)
      languageConfigPromise = null
      return null
    })
  }
  return languageConfigPromise
}

/**
 * 同步获取已缓存的全局语言配置（未加载时为 null）。
 * 需要确保已调用过 loadLanguageConfig()。
 *
 * @returns {Object|null} { codes, languages:[{code,name,base}], baseLanguage, mmpDefaultLang }
 */
export function getLanguageConfig() {
  return languageConfig
}

/**
 * 同步获取已缓存的支持语言代码列表（未加载时为空数组）。
 *
 * @returns {Array<string>}
 */
export function getSupportedLanguageCodes() {
  return languageConfig ? languageConfig.codes.slice() : []
}

/**
 * 同步获取已缓存的支持语言选项（未加载时为空数组）。
 *
 * @returns {Array<Object>} [{ code, name, base }]
 */
export function getLanguageOptions() {
  return languageConfig
    ? languageConfig.languages.map(item => ({ code: item.code, name: item.name, base: item.base }))
    : []
}

/**
 * 获取默认语言代码。
 * 优先取 localStorage 中 maximo-env-settings.defaultLang；
 * 未配置时取浏览器第一语言（navigator.languages[0]），
 * 浏览器语言不在后台支持项中时使用后台配置的 mmpDefaultLang；
 * 仍不可用时回退基准语言或 EN。
 *
 * @returns {string} 语言代码，如 EN、ZH、JA
 */
export function getDefaultLangcode() {
  let defaultLang = null
  try {
    const saved = localStorage.getItem(ENV_SETTINGS_KEY)
    if (saved) {
      const settings = JSON.parse(saved)
      defaultLang = settings && settings.defaultLang
    }
  } catch (e) {
    defaultLang = null
  }

  const configured = normalizeLangcode(defaultLang)
  if (configured) {
    return configured
  }

  const browserLangs = (navigator.languages && navigator.languages.length)
    ? navigator.languages
    : [navigator.language]
  const browser = normalizeLangcode(browserLangs[0])

  if (languageConfig && languageConfig.codes.length) {
    if (browser && languageConfig.codes.indexOf(browser) >= 0) {
      return browser
    }
    return languageConfig.mmpDefaultLang || languageConfig.baseLanguage || 'EN'
  }

  return browser || 'EN'
}
