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
        const codes = (data.languages || [])
          .map(item => normalizeLangcode(item.code))
          .filter(Boolean)
        languageConfig = {
          codes,
          baseLanguage: normalizeLangcode(data.baseLanguage),
          mmpDefaultLang: normalizeLangcode(data.mmpDefaultLang)
        }
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
