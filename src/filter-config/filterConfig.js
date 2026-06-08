// 日志自定义过滤配置与过滤函数
// type: 'endsWith'   - 行尾字符串匹配
// type: 'startsWith' - 行首字符串匹配
// type: 'contains'   - 包含匹配（行内容包含指定关键词）
// type: 'regex'      - 正则匹配（pattern 为正则字符串，对去ANSI+去尾部空白后的行测试）
//
// 规则中的 pattern 直接在此定义（硬编码），logs/ 目录下的 .txt 文件作为参考记录
// 修改过滤规则直接改下面的 filterRules 即可

const filterRules = [
  // --- regex 类型（正则匹配）---
  // 匹配所有 "Getting XXX from maximo cache" 变体
  { type: 'regex', pattern: 'Getting \\w+ from maximo cache$' },
  // 匹配所有 "XXX successfully fetched from maximo cache" 变体
  { type: 'regex', pattern: '\\w+ successfully fetched from maximo cache$' },
  // --- endsWith 类型（字符串行尾匹配）---
  { type: 'endsWith', pattern: '[WARNING ] SRVE8115W: WARNING: Cannot set status. Response already committed' },
  // --- startsWith 类型（行首匹配）---
  // appdoctype.log 内容均为 [err] 开头的异常栈
  { type: 'startsWith', pattern: '[err]' },
  // --- contains 类型（包含匹配）---
  // 适用于关键词过滤，可在此处追加
  { type: 'contains', patterns: ['APPDOCTYPE', 'Mbo isAppLockEnabled'] },
]

/**
 * 过滤日志行
 * @param {string[]} logs - 原始日志行数组
 * @returns {string[]} 过滤后的日志行数组
 */
export function filterLogs(logs) {
  if (!logs || logs.length === 0) return logs
  return logs.filter(log => {
    for (const rule of filterRules) {
      // 先统一去除ANSI转义码和尾部空白
      var clean = log.replace(/\u001b\[[0-9;]*m/g, '').replace(/[\s\r]+$/, '')
      if (rule.type === 'regex') {
        if (rule.pattern && new RegExp(rule.pattern).test(clean)) return false
      } else if (rule.type === 'endsWith') {
        if (rule.pattern && clean.endsWith(rule.pattern)) return false
      } else if (rule.type === 'startsWith') {
        if (rule.pattern && log.startsWith(rule.pattern)) return false
      } else if (rule.type === 'contains') {
        for (const pattern of (rule.patterns || [])) {
          if (log.includes(pattern)) return false
        }
      }
    }
    return true
  })
}

export default filterRules