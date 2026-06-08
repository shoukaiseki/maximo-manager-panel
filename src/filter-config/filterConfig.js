// 日志自定义过滤配置与过滤函数
// type: 'endsWith'   - 行尾匹配
// type: 'startsWith' - 行首匹配
// type: 'contains'   - 包含匹配（行内容包含指定关键词）
//
// 规则中的 pattern 直接在此定义（硬编码），logs/ 目录下的 .txt 文件作为参考记录
// 修改过滤规则直接改下面的 filterRules 即可

const filterRules = [
  // --- endsWith 类型（行尾匹配）---
  // 匹配以这些字符串结尾的日志行（注意日志行末尾可能有空格）
  { type: 'endsWith', pattern: 'Getting MAXPROP from maximo cache' },
  { type: 'endsWith', pattern: 'MAXPROP successfully fetched from maximo cache' },
  { type: 'endsWith', pattern: '[WARNING ] SRVE8115W: WARNING: Cannot set status. Response already committed' },
  // --- startsWith 类型（行首匹配）---
  // appdoctype.log 内容均为 [err] 开头的异常栈
  { type: 'startsWith', pattern: '[err]' },
  // --- contains 类型（包含匹配）---
  // 适用于关键词过滤，可在此处追加
  { type: 'contains', patterns: ['APPDOCTYPE'] },
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
      if (rule.type === 'endsWith') {
        // endsWith 前先去除末尾可能的空格 /r，提高匹配率
        const trimmed = log.replace(/[\s\r]+$/, '')
        if (rule.pattern && trimmed.endsWith(rule.pattern)) return false
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