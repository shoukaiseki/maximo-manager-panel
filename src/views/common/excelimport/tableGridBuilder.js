/**
 * JSON → 定位单元格网格构建器 (仿 json4u 风格)
 *
 * 将 JSON 树递归转换为带绝对定位的平面单元格列表。
 * 对象 → 2列 (Key | Value)，同构对象数组 → N列 (属性名为表头)
 * 每个单元格含 type/text/x/y/w/h/id，用于自定义 div 渲染和视图联动。
 */

// --- 常量 ---
const ROW_H = 30            // 行高
const KEY_W = 180           // Key 列固定宽度
const CELL_PAD = 8          // 单元格水平内边距
const FONT_W = 7.2          // 每个字符估算像素宽度 (12px Consolas)
const MAX_CELL_W = 500      // 单格最大宽度
const MIN_CELL_W = 60       // 单格最小宽度
const IDX_W = 50            // 数组索引列宽度

function calcTextWidth(text) {
  if (text == null) return 0
  const s = String(text)
  // 中文字符按 2 个英文字符宽度算
  let w = 0
  for (const ch of s) {
    w += ch.charCodeAt(0) > 127 ? FONT_W * 2 : FONT_W
  }
  return Math.min(Math.max(w + CELL_PAD * 2, MIN_CELL_W), MAX_CELL_W)
}

// --- 类型检测 ---
function isPureObj(v) { return v !== null && typeof v === 'object' && !Array.isArray(v) }
function isPrimitive(v) { return v === null || v === undefined || typeof v !== 'object' }

function detectType(v) {
  if (v === null) return 'null'
  if (typeof v === 'string') return 'string'
  if (typeof v === 'number') return 'number'
  if (typeof v === 'boolean') return 'bool'
  return 'value'
}

function formatVal(v) {
  if (v === null) return 'null'
  if (v === undefined) return ''
  if (typeof v === 'string') return v
  return String(v)
}

/** 检测同构对象数组，返回字段列表或 null */
function detectHomoCols(data) {
  if (!Array.isArray(data) || data.length === 0) return null
  const allObj = data.every(item => isPureObj(item))
  if (!allObj) return null
  const keySets = data.map(item => Object.keys(item))
  const first = keySets[0]
  const same = keySets.every(ks => ks.length === first.length && ks.every(k => first.includes(k)))
  return same ? first : null
}

/** 检测二维数组，返回最大列数或 null */
function detect2D(data) {
  if (!Array.isArray(data) || data.length === 0) return null
  if (!data.every(item => Array.isArray(item))) return null
  const nonEmpty = data.filter(item => item.length > 0)
  if (nonEmpty.length === 0) return null
  return Math.max(...nonEmpty.map(item => item.length))
}

/**
 * 构建表格网格
 * @param {any} data - JSON 数据
 * @param {string} prefix - 路径前缀 (如 'root' / 'root.parsed' / 'root.parsed.staff[0]')
 * @param {number} startX - 起始 X 坐标
 * @param {number} startY - 起始 Y 坐标
 * @param {number} level - 缩进层级
 * @param {Array} cells - 外部 cells 数组，累加输出
 * @returns {{width: number, height: number}} 当前块的总宽高
 */
export function buildGrid(data, prefix, startX, startY, level, cells) {
  const pfx = prefix || 'root'
  const sv = startX || 0
  const sy = startY || 0
  const lv = level || 0
  const out = cells || []

  // 原始值
  if (isPrimitive(data)) {
    const w = calcTextWidth(formatVal(data))
    out.push(makeCell(detectType(data), formatVal(data), sv, sy, w, ROW_H, lv, pfx))
    return { width: w, height: ROW_H }
  }

  // 数组
  if (Array.isArray(data)) {
    if (data.length === 0) {
      const w = calcTextWidth('[]')
      out.push(makeCell('value', '[]', sv, sy, w, ROW_H, lv, pfx))
      return { width: w, height: ROW_H }
    }

    // 同构对象数组 → N 列表格
    const hcols = detectHomoCols(data)
    if (hcols) {
      return buildHomoArray(data, hcols, pfx, sv, sy, lv, out)
    }

    // 二维数组
    const dim2 = detect2D(data)
    if (dim2) {
      return build2DArray(data, dim2, pfx, sv, sy, lv, out)
    }

    // 异构数组 → 逐个展开
    let y = sy
    let maxW = 0
    data.forEach((item, idx) => {
      const itemPfx = `${pfx}[${idx}]`
      const result = buildGrid(item, itemPfx, sv, y, lv + 1, out)
      y += result.height
      maxW = Math.max(maxW, result.width)
    })
    return { width: maxW, height: y - sy }
  }

  // 对象 → 2 列布局
  return buildObject(data, pfx, sv, sy, lv, out)
}

// --- 对象构建 (2列) ---
function buildObject(data, prefix, startX, startY, level, out) {
  const keys = Object.keys(data)
  if (keys.length === 0) {
    const w = calcTextWidth('{}')
    out.push(makeCell('value', '{}', startX, startY, w, ROW_H, level, prefix))
    return { width: w, height: ROW_H }
  }

  let y = startY
  let maxValW = 0

  for (const k of keys) {
    const v = data[k]
    const path = prefix ? `${prefix}.${k}` : k

    // Key 单元格
    out.push(makeCell('key', k, startX, y, KEY_W, ROW_H, level, path))

    // Value 侧
    const valX = startX + KEY_W
    if (isPrimitive(v)) {
      const w = calcTextWidth(formatVal(v))
      out.push(makeCell(detectType(v), formatVal(v), valX, y, w, ROW_H, level + 1, path))
      y += ROW_H
      maxValW = Math.max(maxValW, w)
    } else {
      // 嵌套结构 (对象或数组)
      const result = buildGrid(v, path, valX, y, level + 1, out)
      y += result.height
      maxValW = Math.max(maxValW, result.width)
    }
  }

  return { width: KEY_W + maxValW, height: y - startY }
}

// --- 同构对象数组构建 (N列) ---
function buildHomoArray(data, cols, prefix, startX, startY, level, out) {
  // 计算每列宽度
  const colW = cols.map(c => {
    let maxW = calcTextWidth(c) // 表头宽度
    data.forEach(item => {
      const w = calcTextWidth(formatVal(item[c]))
      if (w > maxW) maxW = w
    })
    return maxW
  })

  const totalW = IDX_W + colW.reduce((s, w) => s + w, 0)
  let y = startY
  let h = 1 // header row count

  // 表头行
  // 索引列头
  const idxHdrW = Math.max(IDX_W, calcTextWidth('#'))
  out.push(makeCell('index', '#', startX, y, idxHdrW, ROW_H, level, prefix))
  let hx = startX + idxHdrW
  cols.forEach((c, ci) => {
    out.push(makeCell('header', c, hx, y, colW[ci], ROW_H, level, `${prefix}.${c}`))
    hx += colW[ci]
  })
  y += ROW_H

  // 数据行
  data.forEach((item, idx) => {
    const rowPfx = `${prefix}[${idx}]`
    out.push(makeCell('index', String(idx), startX, y, idxHdrW, ROW_H, level + 1, rowPfx))
    let dx = startX + idxHdrW
    cols.forEach((c, ci) => {
      const v = item[c]
      out.push(makeCell(detectType(v), formatVal(v), dx, y, colW[ci], ROW_H, level + 1, `${rowPfx}.${c}`))
      dx += colW[ci]
    })
    y += ROW_H
    h++
  })

  return { width: totalW, height: h * ROW_H }
}

// --- 二维数组构建 (N列, 列标题 [0],[1],...) ---
function build2DArray(data, colCount, prefix, startX, startY, level, out) {
  const colW = []
  for (let c = 0; c < colCount; c++) {
    let maxW = calcTextWidth(`[${c}]`)
    data.forEach(item => {
      if (Array.isArray(item) && c < item.length) {
        const w = calcTextWidth(formatVal(item[c]))
        if (w > maxW) maxW = w
      }
    })
    colW.push(maxW)
  }

  const totalW = IDX_W + colW.reduce((s, w) => s + w, 0)
  let y = startY
  let h = 1

  // 表头行
  const idxHdrW = Math.max(IDX_W, calcTextWidth('#'))
  out.push(makeCell('index', '#', startX, y, idxHdrW, ROW_H, level, prefix))
  let hx = startX + idxHdrW
  for (let c = 0; c < colCount; c++) {
    out.push(makeCell('header', `[${c}]`, hx, y, colW[c], ROW_H, level, `${prefix}[${c}]`))
    hx += colW[c]
  }
  y += ROW_H

  // 数据行
  data.forEach((item, idx) => {
    const rowPfx = `${prefix}[${idx}]`
    out.push(makeCell('index', String(idx), startX, y, idxHdrW, ROW_H, level + 1, rowPfx))
    let dx = startX + idxHdrW
    for (let c = 0; c < colCount; c++) {
      const v = Array.isArray(item) && c < item.length ? item[c] : undefined
      out.push(makeCell(detectType(v), formatVal(v), dx, y, colW[c], ROW_H, level + 1, `${rowPfx}[${c}]`))
      dx += colW[c]
    }
    y += ROW_H
    h++
  })

  return { width: totalW, height: h * ROW_H }
}

// --- 单元格工厂 ---
function makeCell(type, text, x, y, w, h, level, id) {
  return { type, text, x, y, w, h, level, id }
}

/**
 * 便捷入口：传入 JSON 数据，返回完整的网格和查找映射
 * @param {any} jsonData
 * @returns {{ cells: Array, width: number, height: number, cellMap: object }}
 */
export function buildTableGrid(jsonData) {
  const cells = []
  const { width, height } = buildGrid(jsonData, 'root', 0, 0, 0, cells)

  // 构建 id → cell 查找映射
  const cellMap = {}
  for (const c of cells) {
    if (c.id && !cellMap[c.id]) {
      cellMap[c.id] = c
    }
  }

  return { cells, width, height, cellMap }
}
