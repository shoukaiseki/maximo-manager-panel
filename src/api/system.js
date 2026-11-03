import solonRequest from './solonapi'

/**
 * 查询已启用语言、基准语言及后台配置的默认语言
 * @returns {Promise} { baseLanguage, mmpDefaultLang, languages: [{ code, name, base }] }
 */
export function getLanguages() {
  return solonRequest({
    url: '/system/languages',
    method: 'get'
  })
}
