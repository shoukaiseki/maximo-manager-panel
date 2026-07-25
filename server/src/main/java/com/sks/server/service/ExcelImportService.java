package com.sks.server.service;

import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.*;

/**
 * Excel 导入服务 - 解析 jxls XML 配置 + 读取 Excel 数据
 * 支持预览模式(返回JSON)和执行模式
 */
@org.noear.solon.annotation.Component
public class ExcelImportService {

    private static final Logger log = LoggerFactory.getLogger(ExcelImportService.class);

    /**
     * 预览 Excel - 读取原始数据，返回 JSON
     * @param fileStream Excel 文件流
     * @return 预览结果 (包含 sheets、rows、columns 等信息)
     */
    public Map<String, Object> previewExcel(InputStream fileStream) throws IOException {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> sheetsList = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(fileStream)) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                Map<String, Object> sheetInfo = new HashMap<>();
                sheetInfo.put("name", sheet.getSheetName());
                sheetInfo.put("index", i);
                sheetInfo.put("rowCount", sheet.getLastRowNum() + 1);

                List<List<Object>> rows = new ArrayList<>();
                int maxCols = 0;

                for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    List<Object> rowData = new ArrayList<>();
                    if (row != null) {
                        for (int c = 0; c < row.getLastCellNum(); c++) {
                            Cell cell = row.getCell(c);
                            rowData.add(readCellValue(cell));
                        }
                        maxCols = Math.max(maxCols, row.getLastCellNum());
                    }
                    rows.add(rowData);
                }

                sheetInfo.put("rows", rows);
                sheetInfo.put("columnCount", maxCols);
                sheetsList.add(sheetInfo);
            }
        }

        result.put("sheets", sheetsList);
        result.put("totalSheets", sheetsList.size());
        return result;
    }

    /**
     * 使用 jxls XML 配置解析 Excel，返回结构化 JSON 数据
     * @param fileStream Excel 文件流
     * @param xmlConfig  jxls-reader XML 配置字符串
     * @return 解析后的结构化数据
     */
    public Map<String, Object> parseWithConfig(InputStream fileStream, String xmlConfig) throws Exception {
        // 1. 解析 XML 配置
        JxlsConfig config = parseXmlConfig(xmlConfig);

        // 2. 读取 Excel
        try (Workbook workbook = WorkbookFactory.create(fileStream)) {
            Sheet sheet = getSheet(workbook, config);
            if (sheet == null) {
                throw new IllegalArgumentException("找不到匹配的工作表: " + config.sheetName);
            }

            // 3. 按配置解析数据
            Map<String, Object> result = new HashMap<>();
            String resultKey = config.rootKey != null ? config.rootKey : "data";

            // 处理 sections (固定区域)
            for (SectionConfig section : config.sections) {
                Map<String, Object> sectionData = readSection(sheet, section);
                result.putAll(sectionData);
            }

            // 处理 loops (循环区域)
            for (LoopConfig loop : config.loops) {
                List<Map<String, Object>> loopData = readLoop(sheet, loop);
                String listKey = loop.items != null && loop.items.contains(".") ?
                    loop.items.substring(loop.items.lastIndexOf('.') + 1) : "rows";
                result.put(listKey, loopData);
            }

            // 添加原始数据作为参考
            List<List<Object>> rawRows = new ArrayList<>();
            for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                List<Object> rowData = new ArrayList<>();
                if (row != null) {
                    for (int c = 0; c < row.getLastCellNum(); c++) {
                        rowData.add(readCellValue(row.getCell(c)));
                    }
                }
                rawRows.add(rowData);
            }

            Map<String, Object> finalResult = new HashMap<>();
            finalResult.put("parsed", result);
            finalResult.put("raw", rawRows);
            finalResult.put("sheetName", sheet.getSheetName());
            finalResult.put("totalRows", sheet.getLastRowNum() + 1);

            return finalResult;
        }
    }



    // ======== 内部方法 ========

    private Object readCellValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue();
                }
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    yield (long) val;
                }
                yield val;
            }
            case BOOLEAN -> cell.getBooleanCellValue();
            case FORMULA -> {
                try {
                    yield switch (cell.getCachedFormulaResultType()) {
                        case STRING -> cell.getStringCellValue();
                        case NUMERIC -> cell.getNumericCellValue();
                        case BOOLEAN -> cell.getBooleanCellValue();
                        default -> cell.getCellFormula();
                    };
                } catch (Exception e) {
                    yield cell.getCellFormula();
                }
            }
            case BLANK -> null;
            default -> null;
        };
    }

    private Sheet getSheet(Workbook workbook, JxlsConfig config) {
        if (config.sheetName != null && !config.sheetName.isEmpty()) {
            return workbook.getSheet(config.sheetName);
        }
        if (config.sheetIndex >= 0) {
            return workbook.getSheetAt(config.sheetIndex);
        }
        return workbook.getSheetAt(0);
    }

    private Map<String, Object> readSection(Sheet sheet, SectionConfig section) {
        Map<String, Object> data = new HashMap<>();
        for (MappingConfig mapping : section.mappings) {
            int rowNum = section.startRow + (mapping.row >= 0 ? mapping.row : 0);
            Row row = sheet.getRow(rowNum);
            if (row == null) continue;

            Cell cell = row.getCell(mapping.col);
            Object value = readCellValue(cell);

            // 解析 beanKey.propertyName
            if (mapping.property != null && mapping.property.contains(".")) {
                String[] parts = mapping.property.split("\\.", 2);
                String beanKey = parts[0];
                String propName = parts[1];
                data.computeIfAbsent(beanKey, k -> new HashMap<String, Object>());
                ((Map<String, Object>) data.get(beanKey)).put(propName, value);
            } else if (mapping.property != null) {
                data.put(mapping.property, value);
            }
        }
        return data;
    }

    private List<Map<String, Object>> readLoop(Sheet sheet, LoopConfig loop) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        int currentRow = loop.startRow;

        while (currentRow <= sheet.getLastRowNum()) {
            // 检查终止条件
            if (checkBreakCondition(sheet, currentRow, loop.breakConditions)) {
                break;
            }

            Row row = sheet.getRow(currentRow);
            if (row == null) {
                // 检查是否为空行（空行也视为终止）
                boolean emptyRow = true;
                for (int c = 0; c < Math.max(10, row != null ? row.getLastCellNum() : 0); c++) {
                    if (row != null && readCellValue(row.getCell(c)) != null) {
                        emptyRow = false;
                        break;
                    }
                }
                if (emptyRow) break;
                currentRow++;
                continue;
            }

            Map<String, Object> rowData = new HashMap<>();
            for (MappingConfig mapping : loop.mappings) {
                int mappingRow = currentRow + (mapping.row - loop.startRow);
                Row targetRow = sheet.getRow(mappingRow);
                if (targetRow == null) continue;

                Cell cell = targetRow.getCell(mapping.col);
                Object value = readCellValue(cell);

                // 用列号作为默认字段名，或者用用户指定的属性名
                String fieldName = mapping.fieldName != null ? mapping.fieldName : ("col" + mapping.col);
                rowData.put(fieldName, value);
            }

            resultList.add(rowData);
            currentRow++;
        }

        return resultList;
    }

    private boolean checkBreakCondition(Sheet sheet, int rowNum, List<BreakCondition> conditions) {
        if (conditions == null || conditions.isEmpty()) return false;

        for (BreakCondition cond : conditions) {
            int checkRow = rowNum + cond.offset;
            if (checkRow < 0 || checkRow > sheet.getLastRowNum()) return true;

            Row row = sheet.getRow(checkRow);
            if (row == null) return true;

            if (cond.cellChecks == null || cond.cellChecks.isEmpty()) {
                // 空行检查
                boolean empty = true;
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    if (readCellValue(row.getCell(c)) != null) {
                        empty = false;
                        break;
                    }
                }
                if (empty) return true;
            } else {
                // 单元格值检查
                for (CellCheck cellCheck : cond.cellChecks) {
                    Cell cell = row.getCell(cellCheck.offset);
                    Object value = readCellValue(cell);
                    if (cellCheck.expectedValue != null) {
                        if (value != null && value.toString().equals(cellCheck.expectedValue)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    // ======== XML 配置解析 ========

    private JxlsConfig parseXmlConfig(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));

        JxlsConfig config = new JxlsConfig();

        Element workbookEl = (Element) doc.getDocumentElement();
        NodeList worksheetList = workbookEl.getElementsByTagName("worksheet");
        if (worksheetList.getLength() == 0) {
            throw new IllegalArgumentException("XML 配置中缺少 worksheet 元素");
        }

        Element worksheetEl = (Element) worksheetList.item(0);

        // 解析 sheet 名称或索引
        if (worksheetEl.hasAttribute("name")) {
            config.sheetName = worksheetEl.getAttribute("name");
        }
        if (worksheetEl.hasAttribute("idx")) {
            config.sheetIndex = Integer.parseInt(worksheetEl.getAttribute("idx"));
        }

        // 解析根 key
        if (workbookEl.hasAttribute("rootKey")) {
            config.rootKey = workbookEl.getAttribute("rootKey");
        }

        // 解析 section 元素
        NodeList sectionList = worksheetEl.getElementsByTagName("section");
        for (int i = 0; i < sectionList.getLength(); i++) {
            Element sectionEl = (Element) sectionList.item(i);
            // 排除 loop 内部的 section
            if (isInsideLoop(sectionEl)) continue;

            SectionConfig section = new SectionConfig();
            section.startRow = parseIntAttr(sectionEl, "startRow", 0);
            section.endRow = parseIntAttr(sectionEl, "endRow", 0);

            NodeList mappingList = sectionEl.getElementsByTagName("mapping");
            for (int j = 0; j < mappingList.getLength(); j++) {
                Element mappingEl = (Element) mappingList.item(j);
                section.mappings.add(parseMapping(mappingEl));
            }

            config.sections.add(section);
        }

        // 解析 loop 元素
        NodeList loopList = worksheetEl.getElementsByTagName("loop");
        for (int i = 0; i < loopList.getLength(); i++) {
            Element loopEl = (Element) loopList.item(i);
            LoopConfig loop = new LoopConfig();
            loop.startRow = parseIntAttr(loopEl, "startRow", 0);
            loop.endRow = parseIntAttr(loopEl, "endRow", 0);
            loop.items = loopEl.getAttribute("items");
            loop.var = loopEl.getAttribute("var");
            loop.varType = loopEl.getAttribute("varType");

            // 解析 loop 内部的 section 的 mappings
            NodeList loopSectionList = loopEl.getElementsByTagName("section");
            for (int j = 0; j < loopSectionList.getLength(); j++) {
                Element sectionEl = (Element) loopSectionList.item(j);
                NodeList mappingList = sectionEl.getElementsByTagName("mapping");
                for (int k = 0; k < mappingList.getLength(); k++) {
                    Element mappingEl = (Element) mappingList.item(k);
                    loop.mappings.add(parseMapping(mappingEl));
                }
            }

            // 解析 loopbreakcondition
            NodeList breakList = loopEl.getElementsByTagName("loopbreakcondition");
            if (breakList.getLength() > 0) {
                Element breakEl = (Element) breakList.item(0);
                NodeList rowcheckList = breakEl.getElementsByTagName("rowcheck");
                for (int j = 0; j < rowcheckList.getLength(); j++) {
                    Element rowcheckEl = (Element) rowcheckList.item(j);
                    BreakCondition bc = new BreakCondition();
                    bc.offset = parseIntAttr(rowcheckEl, "offset", 0);

                    NodeList cellcheckList = rowcheckEl.getElementsByTagName("cellcheck");
                    for (int k = 0; k < cellcheckList.getLength(); k++) {
                        Element cellcheckEl = (Element) cellcheckList.item(k);
                        CellCheck cc = new CellCheck();
                        cc.offset = parseIntAttr(cellcheckEl, "offset", 0);
                        cc.expectedValue = cellcheckEl.getTextContent();
                        bc.cellChecks.add(cc);
                    }

                    loop.breakConditions.add(bc);
                }
            }

            config.loops.add(loop);
        }

        return config;
    }

    private boolean isInsideLoop(Element sectionEl) {
        return sectionEl.getParentNode() != null && "loop".equals(sectionEl.getParentNode().getNodeName());
    }

    private MappingConfig parseMapping(Element mappingEl) {
        MappingConfig mapping = new MappingConfig();
        String cellRef = mappingEl.getAttribute("cell");
        if (cellRef != null && !cellRef.isEmpty()) {
            org.apache.poi.ss.util.CellReference cr = new org.apache.poi.ss.util.CellReference(cellRef);
            mapping.row = cr.getRow();
            mapping.col = cr.getCol();
        } else {
            mapping.row = parseIntAttr(mappingEl, "row", 0);
            mapping.col = parseIntAttr(mappingEl, "col", 0);
        }

        mapping.property = mappingEl.getTextContent();

        // 从 property 中提取字段名 (如 "row.name" -> "name")
        if (mapping.property != null && mapping.property.contains(".")) {
            mapping.fieldName = mapping.property.substring(mapping.property.lastIndexOf('.') + 1);
            mapping.beanKey = mapping.property.substring(0, mapping.property.indexOf('.'));
        } else {
            mapping.fieldName = mapping.property;
        }

        // 支持 type 属性
        if (mappingEl.hasAttribute("type")) {
            mapping.type = mappingEl.getAttribute("type");
        }

        return mapping;
    }

    private int parseIntAttr(Element el, String attr, int defaultValue) {
        String val = el.getAttribute(attr);
        if (val == null || val.isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // ======== 内部配置类 ========

    static class JxlsConfig {
        String sheetName;
        int sheetIndex = -1;
        String rootKey = "data";
        List<SectionConfig> sections = new ArrayList<>();
        List<LoopConfig> loops = new ArrayList<>();
    }

    static class SectionConfig {
        int startRow;
        int endRow;
        List<MappingConfig> mappings = new ArrayList<>();
    }

    static class LoopConfig {
        int startRow;
        int endRow;
        String items;
        String var;
        String varType;
        List<MappingConfig> mappings = new ArrayList<>();
        List<BreakCondition> breakConditions = new ArrayList<>();
    }

    static class MappingConfig {
        int row;
        int col;
        String property;
        String fieldName;
        String beanKey;
        String type;
    }

    static class BreakCondition {
        int offset;
        List<CellCheck> cellChecks = new ArrayList<>();
    }

    static class CellCheck {
        int offset;
        String expectedValue;
    }
}
