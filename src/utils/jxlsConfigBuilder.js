/**
 * jxls-reader XML 配置构建器
 * 将可视化配置转为 jxls-reader 兼容的 XML 格式
 */

// 默认配置模板
const DEFAULT_CONFIG = {
  sheetName: 'Sheet1',
  sheetIndex: -1,
  rootKey: 'data',
  sections: [],
  loops: []
}

// 默认空配置 XML
export const EMPTY_XML_TEMPLATE = `<?xml version="1.0" encoding="UTF-8"?>
<workbook>
  <worksheet name="Sheet1">
    <!-- 示例: 固定区域读取标题行 -->
    <!--
    <section startRow="0" endRow="0">
      <mapping cell="B1">header.title</mapping>
      <mapping cell="B2">header.date</mapping>
    </section>
    -->
    <!-- 示例: 循环读取数据行 -->
    <!--
    <loop startRow="3" endRow="3" items="rows" var="row" varType="java.util.HashMap">
      <section startRow="3" endRow="3">
        <mapping cell="A3">row.col1</mapping>
        <mapping cell="B3">row.col2</mapping>
      </section>
      <loopbreakcondition>
        <rowcheck offset="0"/>
      </loopbreakcondition>
    </loop>
    -->
  </worksheet>
</workbook>`

/**
 * 构建 Section 的 XML
 */
function buildSectionXml(section) {
  const startRow = section.startRow || 0
  const endRow = section.endRow !== undefined ? section.endRow : startRow
  let xml = `    <section startRow="${startRow}" endRow="${endRow}">\n`
  
  ;(section.mappings || []).forEach(m => {
    const cellRef = m.cell || colToCellRef(m.col, m.row || startRow)
    const property = m.property || `data.${m.fieldName || ('col' + m.col)}`
    xml += `      <mapping cell="${cellRef}">${property}</mapping>\n`
  })
  
  xml += `    </section>`
  return xml
}

/**
 * 构建 Loop 的 XML
 */
function buildLoopXml(loop) {
  const startRow = loop.startRow || 0
  const endRow = loop.endRow !== undefined ? loop.endRow : startRow
  const items = loop.items || 'rows'
  const varName = loop.var || 'row'
  const varType = loop.varType || 'java.util.HashMap'
  
  let xml = `    <loop startRow="${startRow}" endRow="${endRow}" items="${items}" var="${varName}" varType="${varType}">\n`
  xml += `      <section startRow="${startRow}" endRow="${endRow}">\n`
  
  ;(loop.mappings || []).forEach(m => {
    const cellRef = m.cell || colToCellRef(m.col, startRow)
    const property = m.property || `${varName}.${m.fieldName || ('col' + m.col)}`
    xml += `        <mapping cell="${cellRef}">${property}</mapping>\n`
  })
  
  xml += `      </section>\n`
  
  // 终止条件
  if (loop.breakConditions && loop.breakConditions.length > 0) {
    xml += `      <loopbreakcondition>\n`
    loop.breakConditions.forEach(bc => {
      xml += `        <rowcheck offset="${bc.offset || 0}">\n`
      if (bc.cellChecks && bc.cellChecks.length > 0) {
        bc.cellChecks.forEach(cc => {
          xml += `          <cellcheck offset="${cc.offset || 0}">${cc.expectedValue || ''}</cellcheck>\n`
        })
      }
      xml += `        </rowcheck>\n`
    })
    xml += `      </loopbreakcondition>\n`
  } else {
    // 默认空行终止
    xml += `      <loopbreakcondition>\n`
    xml += `        <rowcheck offset="0"/>\n`
    xml += `      </loopbreakcondition>\n`
  }
  
  xml += `    </loop>`
  return xml
}

/**
 * 将列号转为 Excel 列字母 (0-based)
 */
function colToLetter(col) {
  let letter = ''
  let c = col
  while (c >= 0) {
    letter = String.fromCharCode((c % 26) + 65) + letter
    c = Math.floor(c / 26) - 1
  }
  return letter
}

/**
 * 将行列号转为 Excel 单元格引用 (如 "B4")
 */
function colToCellRef(col, row) {
  return colToLetter(col) + (row + 1)
}

/**
 * 将 Excel 单元格引用转为行列号 (如 "B4" -> {row: 3, col: 1})
 */
export function cellRefToRowCol(cell) {
  if (!cell) return { row: 0, col: 0 }
  const match = cell.match(/^([A-Z]+)(\d+)$/)
  if (!match) return { row: 0, col: 0 }
  
  let col = 0
  for (let i = 0; i < match[1].length; i++) {
    col = col * 26 + (match[1].charCodeAt(i) - 64)
  }
  return { row: parseInt(match[2]) - 1, col: col - 1 }
}

/**
 * 将可视化配置对象转为 jxls XML
 * @param {Object} config 配置对象
 * @param {string} config.sheetName 工作表名称
 * @param {number} config.sheetIndex 工作表索引
 * @param {Array} config.sections 固定区域配置
 * @param {Array} config.loops 循环区域配置
 * @returns {string} XML 字符串
 */
export function buildXmlConfig(config) {
  const cfg = { ...DEFAULT_CONFIG, ...config }
  
  // 智能检测 sheet 标识
  let sheetAttr = ''
  if (cfg.sheetName && cfg.sheetName !== '') {
    sheetAttr = `name="${cfg.sheetName}"`
  } else if (cfg.sheetIndex >= 0) {
    sheetAttr = `idx="${cfg.sheetIndex}"`
  } else {
    sheetAttr = 'name="Sheet1"'
  }
  
  const workbookAttr = cfg.rootKey ? ` rootKey="${cfg.rootKey}"` : ''
  
  let xml = `<?xml version="1.0" encoding="UTF-8"?>\n`
  xml += `<workbook${workbookAttr}>\n`
  xml += `  <worksheet ${sheetAttr}>\n`
  
  // Sections
  ;(cfg.sections || []).forEach(s => {
    xml += buildSectionXml(s) + '\n'
  })
  
  // Loops
  ;(cfg.loops || []).forEach(l => {
    xml += buildLoopXml(l) + '\n'
  })
  
  xml += `  </worksheet>\n`
  xml += `</workbook>`
  
  return xml
}

/**
 * 解析单个 mapping XML 字符串，同时支持 cell="A1" 和 row="3" col="4" 两种格式
 */
function parseMappingFromXml(mappingXml) {
  const result = { cell: '', property: '', fieldName: '', type: '' }

  // 提取 cell 属性 (格式: cell="A1")
  const cellMatch = mappingXml.match(/cell="([^"]*)"/)
  
  // 提取 row/col 属性 (格式: row="3" col="4")
  const rowMatch = mappingXml.match(/row="(\d+)"/)
  const colMatch = mappingXml.match(/col="(\d+)"/)
  
  if (cellMatch) {
    result.cell = cellMatch[1]
  } else if (rowMatch && colMatch) {
    const r = parseInt(rowMatch[1])
    const c = parseInt(colMatch[1])
    result.cell = colToCellRef(c, r)
  }

  // 提取 type 属性
  const typeMatch = mappingXml.match(/type="([^"]*)"/)
  result.type = typeMatch ? typeMatch[1] : ''

  // 提取标签内容 (property 文本)
  const textMatch = mappingXml.match(/>([^<]*)<\//)
  result.property = textMatch ? textMatch[1].trim() : ''

  // 从 property 中提取 fieldName
  if (result.property) {
    const parts = result.property.split('.')
    result.fieldName = parts.length > 1 ? parts.slice(1).join('.') : result.property
  }

  return result
}

/**
 * 从 XML 解析为配置对象
 * @param {string} xml XML 字符串
 * @returns {Object} 配置对象
 */
export function parseXmlConfig(xml) {
  if (!xml || xml.trim() === '') return { sheetName: 'Sheet1', sheetIndex: -1, rootKey: 'data', sections: [], loops: [] }
  
  const config = { sheetName: 'Sheet1', sheetIndex: -1, rootKey: 'data', sections: [], loops: [] }
  
  // 提取 sheet 名称
  const sheetNameMatch = xml.match(/worksheet\s+name="([^"]+)"/)
  if (sheetNameMatch) config.sheetName = sheetNameMatch[1]
  
  const sheetIdxMatch = xml.match(/worksheet\s+idx="(\d+)"/)
  if (sheetIdxMatch) config.sheetIndex = parseInt(sheetIdxMatch[1])
  
  const rootKeyMatch = xml.match(/workbook[^>]*\s+rootKey="([^"]+)"/)
  if (rootKeyMatch) config.rootKey = rootKeyMatch[1]
  
  // 提取 section
  const sectionRegex = /<section\s+startRow="(\d+)"\s+endRow="(\d+)">([\s\S]*?)<\/section>/g
  let sectionMatch
  while ((sectionMatch = sectionRegex.exec(xml)) !== null) {
    // 排除 loop 内部的 section
    const beforeText = xml.substring(0, sectionMatch.index)
    const lastLoop = beforeText.lastIndexOf('<loop')
    const lastLoopEnd = beforeText.lastIndexOf('</loop>')
    if (lastLoop > lastLoopEnd) continue // 在 loop 内部
    
    const section = {
      startRow: parseInt(sectionMatch[1]),
      endRow: parseInt(sectionMatch[2]),
      mappings: []
    }
    
    // 提取 mapping (同时支持 cell="A1" 和 row="3" col="4" 格式)
    const mappingRegex = /<mapping[^>]*>([^<]*)<\/mapping>/g
    let mappingMatch
    while ((mappingMatch = mappingRegex.exec(sectionMatch[3])) !== null) {
      section.mappings.push(parseMappingFromXml(mappingMatch[0]))
    }
    
    config.sections.push(section)
  }
  
  // 提取 loop
  const loopRegex = /<loop\s+startRow="(\d+)"\s+endRow="(\d+)"\s+items="([^"]*)"\s+var="([^"]*)"(?:\s+varType="([^"]*)")?>([\s\S]*?)<\/loop>/g
  let loopMatch
  while ((loopMatch = loopRegex.exec(xml)) !== null) {
    const loop = {
      startRow: parseInt(loopMatch[1]),
      endRow: parseInt(loopMatch[2]),
      items: loopMatch[3],
      var: loopMatch[4],
      varType: loopMatch[5] || 'java.util.HashMap',
      mappings: [],
      breakConditions: []
    }
    
    // 提取 loop 内部的 section mappings
    const loopSectionMatch = loopMatch[6].match(/<section[\s\S]*?<\/section>/)
    if (loopSectionMatch) {
      const mappingRegex = /<mapping[^>]*>([^<]*)<\/mapping>/g
      let mappingMatch
      while ((mappingMatch = mappingRegex.exec(loopSectionMatch[0])) !== null) {
        loop.mappings.push(parseMappingFromXml(mappingMatch[0]))
      }
    }
    
    // 提取 break condition
    const rowcheckRegex = /<rowcheck\s+offset="(-?\d+)">([\s\S]*?)<\/rowcheck>/g
    let rowcheckMatch
    while ((rowcheckMatch = rowcheckRegex.exec(loopMatch[6])) !== null) {
      const bc = {
        offset: parseInt(rowcheckMatch[1]),
        cellChecks: []
      }
      const cellcheckRegex = /<cellcheck\s+offset="(\d+)"[^>]*>([^<]*)<\/cellcheck>/g
      let cellcheckMatch
      while ((cellcheckMatch = cellcheckRegex.exec(rowcheckMatch[2])) !== null) {
        bc.cellChecks.push({
          offset: parseInt(cellcheckMatch[1]),
          expectedValue: cellcheckMatch[2]
        })
      }
      loop.breakConditions.push(bc)
    }
    
    // 空行终止条件
    const emptyRowcheckMatch = loopMatch[6].match(/<rowcheck\s+offset="(-?\d+)"\s*\/>/)
    if (emptyRowcheckMatch) {
      loop.breakConditions.push({
        offset: parseInt(emptyRowcheckMatch[1]),
        cellChecks: []
      })
    }
    
    config.loops.push(loop)
  }
  
  return config
}

/**
 * 创建默认循环配置
 */
export function createDefaultLoop(startRow, endRow, fieldNames) {
  const loop = {
    startRow: startRow || 3,
    endRow: endRow !== undefined ? endRow : 3,
    items: 'rows',
    var: 'row',
    varType: 'java.util.HashMap',
    mappings: (fieldNames || ['col1', 'col2', 'col3']).map((name, idx) => ({
      cell: colToCellRef(idx, startRow || 3),
      property: 'row.' + name,
      fieldName: name
    })),
    breakConditions: [{ offset: 0, cellChecks: [] }]
  }
  return loop
}

/**
 * 创建默认 section 配置
 */
export function createDefaultSection(startRow, endRow, fieldMappings) {
  return {
    startRow: startRow || 0,
    endRow: endRow !== undefined ? endRow : 0,
    mappings: (fieldMappings || [{ cell: 'A1', property: 'header.title', fieldName: 'title' }])
  }
}

export { colToCellRef, colToLetter }
