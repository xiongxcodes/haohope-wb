package com.wb.tool;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;

import com.wb.common.KVBuffer;
import com.wb.common.Var;
import com.wb.util.DateUtil;
import com.wb.util.JsonUtil;
import com.wb.util.StringUtil;
import com.wb.util.SysUtil;

public class DataOutput {
    public static void outputExcel(OutputStream outputStream, JSONArray headers, JSONArray records, String title,
        JSONObject reportInfo, String dateFormat, String timeFormat, boolean neptune) throws Exception {
        int startRow = 0;
        JSONArray topHtml = reportInfo.optJSONArray("topHtml");
        JSONArray bottomHtml = reportInfo.optJSONArray("bottomHtml");
        Workbook book = ExcelObject.getBook();

        try {
            Sheet sheet = book.createSheet();
            if (topHtml != null) {
                createHtml(sheet, topHtml);
                startRow = topHtml.length();
                title = null;
            }

            if (title != null) {
                startRow = 1;
            }

            Object[] values = createHeaders(sheet, headers, startRow, neptune);
            int headerCols = (Integer)values[0];
            int headerRows = (Integer)values[1];
            JSONArray fields = (JSONArray)values[2];
            if (title != null) {
                createTitle(sheet, title, headerCols);
            }

            startRow += headerRows;
            if (Var.getBool("sys.service.excel.freezePane")) {
                sheet.createFreezePane(0, startRow);
            }

            createRecords(sheet, records, fields, startRow, dateFormat, timeFormat);
            ExcelObject.mergeCells(sheet, reportInfo, startRow, Integer.MAX_VALUE);
            if (bottomHtml != null) {
                createHtml(sheet, bottomHtml);
            }

            book.write(outputStream);
        } finally {
            book.close();
        }

    }

    private static void createTitle(Sheet sheet, String title, int headerCols) {
        Row row = sheet.createRow(0);
        Object[] styles = createCellStyle(sheet.getWorkbook(), "title");
        row.setHeight((Short)styles[1]);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, headerCols - 1));
        Cell cell = row.createCell(0);
        cell.setCellStyle((CellStyle)styles[0]);
        cell.setCellValue(title);
    }

    private static void createHtml(Sheet sheet, JSONArray data) {
        Workbook book = sheet.getWorkbook();
        int y = data.length();
        int startIndex = sheet.getLastRowNum();
        if (startIndex > 0) {
            ++startIndex;
        }

        for (int x = 0; x < y; ++x) {
            JSONObject dataRow = data.optJSONObject(x);
            JSONArray dataCells = dataRow.optJSONArray("items");
            Row row = sheet.createRow(startIndex + x);
            row.setHeight((short)(dataRow.optInt("height") * 20));
            int j = dataCells.length();
            int cellIndex = 0;

            for (int i = 0; i < j; ++i) {
                JSONObject dataCell = dataCells.optJSONObject(i);
                int colSpan = dataCell.optInt("colSpan");
                Cell cell = row.createCell(cellIndex);
                cell.setCellValue(dataCell.optString("value"));
                CellStyle style = book.createCellStyle();
                // style.setAlignment(ExcelObject.getAlignment(dataCell.optString("align"), (short)1));
                style.setAlignment(
                    HorizontalAlignment.forInt(ExcelObject.getAlignment(dataCell.optString("align"), (short)1)));
                // style.setVerticalAlignment((short)1);
                style.setVerticalAlignment(VerticalAlignment.forInt((short)1));
                Font font = book.createFont();
                if ("bold".equals(dataCell.optString("weight"))) {
                    // font.setBoldweight((short)700);
                    font.setFontHeight((short)700);
                }

                int fontSize = dataCell.optInt("size");
                if (fontSize > 0) {
                    font.setFontHeightInPoints((short)fontSize);
                }

                style.setFont(font);
                cell.setCellStyle(style);

                for (int k = 1; k < colSpan; ++k) {
                    row.createCell(cellIndex + k);
                }

                if (colSpan > 1) {
                    sheet.addMergedRegion(
                        new CellRangeAddress(startIndex + x, startIndex + x, cellIndex, cellIndex + colSpan - 1));
                }

                cellIndex += colSpan;
            }
        }

    }

    private static Object[] createHeaders(Sheet sheet, JSONArray headers, int startRow, boolean neptune) {
        Workbook book = sheet.getWorkbook();
        JSONArray processedHeaders = new JSONArray();
        Object[] values = prepareHeaders(sheet, headers, processedHeaders, startRow, neptune);
        Cell[][] cells = (Cell[][])values[0];
        Object[] styles = createCellStyle(book, "header");
        CellStyle baseStyle = (CellStyle)styles[0];
        int j = processedHeaders.length();

        for (int i = 0; i < j; ++i) {
            JSONObject header = processedHeaders.getJSONObject(i);
            int x = header.getInt("x");
            int y = header.getInt("y");
            int colspan = Math.max(header.getInt("colspan"), 0);
            int rowspan = Math.max(header.getInt("rowspan"), 0);
            if (colspan > 0 || rowspan > 0) {
                sheet.addMergedRegion(new CellRangeAddress(y + startRow, y + startRow + rowspan, x, x + colspan));
            }

            Cell cell = cells[x][y];
            CellStyle style = book.createCellStyle();
            style.cloneStyleFrom(baseStyle);
            // style.setAlignment(ExcelObject.getAlignment(header.optString("titleAlign"), (short)(header.has("child") ?
            // 2 : 1)));
            style.setAlignment(HorizontalAlignment.forInt(
                ExcelObject.getAlignment(header.optString("titleAlign"), (short)(header.has("child") ? 2 : 1))));
            cell.setCellStyle(style);
            cell.setCellValue(header.optString("text"));
        }

        Object[] result = new Object[] {cells.length, cells[0].length, values[1]};
        return result;
    }

    private static Object[] prepareHeaders(Sheet sheet, JSONArray rawHeaders, JSONArray processedHeaders, int startRow,
        boolean neptune) {
        JSONArray leafs = new JSONArray();
        Object[] result = new Object[2];
        int flexWidth = Var.getInt("sys.service.excel.flexColumnMaxWidth");
        Object[] styles = createCellStyle(sheet.getWorkbook(), "header");
        CellStyle style = (CellStyle)styles[0];
        short rowHeight = (Short)styles[1];
        double rate;
        if (neptune) {
            rate = 32.06D;
        } else {
            rate = 36.55D;
        }

        leafs.put(0);
        markParents(leafs, rawHeaders, (JSONObject)null, 0);
        int maxDepth = leafs.getInt(0);
        leafs.remove(0);
        int j = leafs.length();

        for (int i = 0; i < j; ++i) {
            JSONObject node = leafs.getJSONObject(i);
            int width;
            if (node.has("width")) {
                width = node.getInt("width");
            } else if (node.has("flex")) {
                width = flexWidth;
            } else {
                width = 100;
            }

            sheet.setColumnWidth(i, Math.min(65280, (int)((double)width * rate)));
            node.put("rowspan", maxDepth - node.getInt("y"));

            do {
                node.put("colspan", node.getInt("colspan") + 1);
                if (!node.has("x")) {
                    node.put("x", i);
                    processedHeaders.put(node);
                }
            } while ((node = (JSONObject)node.opt("parent")) != null);
        }

        ++maxDepth;
        Cell[][] cells = new Cell[j][maxDepth];

        for (int k = 0; k < maxDepth; ++k) {
            Row row = sheet.createRow(k + startRow);
            row.setHeight(rowHeight);

            for (int l = 0; l < j; ++l) {
                Cell cell = row.createCell(l);
                cell.setCellStyle(style);
                cells[l][k] = cell;
            }
        }

        result[0] = cells;
        result[1] = leafs;
        return result;
    }

    private static void markParents(JSONArray leafs, JSONArray headers, JSONObject parent, int depth) {
        int j = headers.length();
        leafs.put(0, Math.max(leafs.getInt(0), depth));

        for (int i = 0; i < j; ++i) {
            JSONObject header = headers.getJSONObject(i);
            header.put("y", depth);
            header.put("colspan", -1);
            header.put("rowspan", -1);
            if (parent != null) {
                header.put("parent", parent);
                parent.put("child", header);
            }

            JSONArray items = (JSONArray)header.opt("items");
            if (items != null) {
                markParents(leafs, items, header, depth + 1);
            } else {
                leafs.put(header);
            }
        }

    }

    private static void createRecords(Sheet sheet, JSONArray records, JSONArray fields, int startRow,
        String defaultDateFormat, String defaultTimeFormat) {
        int j = records.length();
        int l = fields.length();
        String[] fieldNames = new String[l];
        Workbook book = sheet.getWorkbook();
        Object[] cellStyles = createCellStyle(book, "text");
        CellStyle baseStyle = (CellStyle)cellStyles[0];
        CellStyle[] colStyles = new CellStyle[l];
        CellStyle[][] dateTimeStyles = new CellStyle[l][2];
        short rowHeight = (Short)cellStyles[1];
        String boolString = Var.getString("sys.service.excel.boolText");
        String trueText = null;
        String falseText = null;
        int[] dataTypes = new int[l];
        Object[] keyMaps = new Object[l];
        boolean useBoolString = !boolString.isEmpty();
        if (useBoolString) {
            String[] boolStrings = boolString.split(",");
            trueText = boolStrings[0];
            falseText = boolStrings[1];
        }

        int k;
        for (k = 0; k < l; ++k) {
            JSONObject field = fields.getJSONObject(k);
            fieldNames[k] = field.optString("field");
            CellStyle style = book.createCellStyle();
            style.cloneStyleFrom(baseStyle);
            // style.setAlignment(ExcelObject.getAlignment(field.optString("align"), (short)1));
            style
                .setAlignment(HorizontalAlignment.forInt(ExcelObject.getAlignment(field.optString("align"), (short)1)));
            if (Boolean.TRUE.equals(field.opt("autoWrap"))) {
                style.setWrapText(true);
            }

            String keyName = field.optString("keyName");
            String dataTypeStr;
            if (keyName.isEmpty()) {
                keyMaps[k] = null;
                dataTypeStr = field.optString("type").toLowerCase();
            } else {
                keyMaps[k] = KVBuffer.buffer.get(keyName);
                dataTypeStr = "string";
            }

            String format = field.optString("format");
            byte dataType;
            if (dataTypeStr.equals("string")) {
                dataType = 1;
            } else if (!dataTypeStr.startsWith("int") && !dataTypeStr.equals("float")
                && !dataTypeStr.equals("number")) {
                if (dataTypeStr.equals("date")) {
                    dataType = 3;
                    if (StringUtil.isEmpty(format)) {
                        CellStyle dateStyle = book.createCellStyle();
                        dateStyle.cloneStyleFrom(style);
                        CellStyle dateTimeStyle = book.createCellStyle();
                        dateTimeStyle.cloneStyleFrom(style);
                        format = ExcelObject.toExcelDateFormat(defaultDateFormat, true);
                        dateStyle.setDataFormat(book.createDataFormat().getFormat(format));
                        dateTimeStyles[k][0] = dateStyle;
                        format = ExcelObject.toExcelDateFormat(defaultDateFormat + " " + defaultTimeFormat, true);
                        dateTimeStyle.setDataFormat(book.createDataFormat().getFormat(format));
                        dateTimeStyles[k][1] = dateTimeStyle;
                        style = dateStyle;
                    } else {
                        dateTimeStyles[0][0] = null;
                        format = ExcelObject.toExcelDateFormat(format, false);
                        if (format == null) {
                            format = ExcelObject.toExcelDateFormat(defaultDateFormat, true);
                        }

                        style.setDataFormat(book.createDataFormat().getFormat(format));
                    }
                } else if (dataTypeStr.startsWith("bool")) {
                    dataType = 4;
                } else {
                    dataType = 5;
                }
            } else {
                dataType = 2;
                if (!StringUtil.isEmpty(format)) {
                    style.setDataFormat(book.createDataFormat().getFormat(format));
                }
            }

            dataTypes[k] = dataType;
            colStyles[k] = style;
        }

        for (int i = 0; i < j; ++i) {
            Row row = sheet.createRow(startRow + i);
            row.setHeight(rowHeight);
            JSONObject record = (JSONObject)records.opt(i);

            for (k = 0; k < l; ++k) {
                Cell cell = row.createCell(k);
                cell.setCellStyle(colStyles[k]);
                Object value = JsonUtil.opt(record, fieldNames[k]);
                if (value != null) {
                    if (keyMaps[k] != null) {
                        value = KVBuffer.getValue((ConcurrentHashMap)keyMaps[k], value);
                    }

                    if (dataTypes[k] == 5) {
                        if (value instanceof Number) {
                            dataTypes[k] = 2;
                        } else if (value instanceof Date) {
                            dataTypes[k] = 3;
                        } else if (value instanceof Boolean) {
                            dataTypes[k] = 4;
                        } else {
                            dataTypes[k] = 1;
                        }
                    }

                    switch (dataTypes[k]) {
                        case 2:
                            double number;
                            if (value instanceof Number) {
                                number = ((Number)value).doubleValue();
                            } else {
                                number = Double.parseDouble(value.toString());
                            }

                            cell.setCellValue(number);
                            break;
                        case 3:
                            Object date;
                            if (dateTimeStyles[k][0] == null) {
                                if (value instanceof Date) {
                                    date = (Date)value;
                                } else {
                                    date = Timestamp.valueOf(value.toString());
                                }
                            } else {
                                boolean hasTime;
                                if (value instanceof Date) {
                                    date = (Date)value;
                                    hasTime = !DateUtil.dateToStr((Date)date).endsWith("00:00:00.0");
                                } else {
                                    String dateTimeStr = value.toString();
                                    date = Timestamp.valueOf(dateTimeStr);
                                    hasTime = !dateTimeStr.endsWith("00:00:00.0")
                                        && !(dateTimeStr.endsWith("00:00:00") | dateTimeStr.endsWith("00:00:00.000"));
                                }

                                if (hasTime) {
                                    cell.setCellStyle(dateTimeStyles[k][1]);
                                } else {
                                    cell.setCellStyle(dateTimeStyles[k][0]);
                                }
                            }

                            cell.setCellValue((Date)date);
                            break;
                        case 4:
                            if (useBoolString) {
                                cell.setCellValue(StringUtil.getBool(value.toString()) ? trueText : falseText);
                            } else {
                                cell.setCellValue(StringUtil.getBool(value.toString()));
                            }
                            break;
                        default:
                            cell.setCellValue(value.toString());
                    }
                }
            }
        }

    }

    private static Object[] createCellStyle(Workbook book, String type) {
        CellStyle style = book.createCellStyle();
        Font font = book.createFont();
        String fontName = Var.getString("sys.service.excel." + type + ".fontName");
        int fontHeight = Var.getInt("sys.service.excel." + type + ".fontHeight");
        double rowHeight = Var.getDouble("sys.service.excel." + type + ".rowHeight");
        Object[] result = new Object[2];
        if (!fontName.isEmpty()) {
            font.setFontName(fontName);
        }

        // font.setBoldweight((short)Var.getInt("sys.service.excel." + type + ".fontWeight"));
        font.setFontHeight((short)Var.getInt("sys.service.excel." + type + ".fontWeight"));
        font.setFontHeight((short)fontHeight);
        if (rowHeight < 10.0D) {
            rowHeight *= (double)fontHeight;
        }

        if (!"text".equals(type) && Var.getBool("sys.service.excel." + type + ".wrapText")) {
            style.setWrapText(true);
        }

        String backColor;
        Object[][] colors;
        if ("title".equals(type)) {
            backColor = Var.getString("sys.service.excel." + type + ".align");
            if (!backColor.isEmpty()) {
                colors = new Object[][] {{"居中", Short.valueOf((short)2)}, {"左", Short.valueOf((short)1)},
                    {"右", Short.valueOf((short)3)}, {"居中选择", Short.valueOf((short)6)}, {"填充", Short.valueOf((short)4)},
                    {"常规", Short.valueOf((short)0)}, {"两端对齐", Short.valueOf((short)5)}};
                style.setAlignment(HorizontalAlignment.forInt((Short)SysUtil.getValue(colors, backColor)));
            }
        } else if (Var.getBool("sys.service.excel.border")) {
            style.setBorderTop(BorderStyle.valueOf((short)1));
            style.setBorderBottom(BorderStyle.valueOf((short)1));
            style.setBorderLeft(BorderStyle.valueOf((short)1));
            style.setBorderRight(BorderStyle.valueOf((short)1));
        }

        if ("header".equals(type)) {
            backColor = Var.getString("sys.service.excel.header.backColor");
            if (!"默认".equals(backColor)) {
                colors = new Object[][] {{"默认", -1}, {"金色", Short.valueOf((short)51)}, {"灰色", Short.valueOf((short)22)},
                    {"浅黄", Short.valueOf((short)43)}};
                style.setFillForegroundColor((Short)SysUtil.getValue(colors, backColor));
                style.setFillPattern(FillPatternType.forInt((short)1));
            }
        }
        style.setVerticalAlignment(VerticalAlignment.forInt((short)1));
        style.setFont(font);
        result[0] = style;
        result[1] = Double.valueOf(rowHeight).shortValue();
        return result;
    }

    public static void outputHtml(OutputStream outputStream, JSONArray headers, JSONArray records, String title,
        String dateFormat, String timeFormat, boolean neptune, int rowNumberWidth, String rowNumberTitle,
        String decimalSeparator, String thousandSeparator) throws Exception {
        StringBuilder html = new StringBuilder();
        html.append(
            "<!DOCTYPE html><html><head><meta http-equiv=\"content-type\" content=\"text/html;charset=utf-8\">");
        if (!StringUtil.isEmpty(title)) {
            html.append("<title>");
            html.append(title);
            html.append("</title>");
        }

        html.append("<style type=\"text/css\">table{table-layout:fixed;border-collapse:collapse;word-wrap:break-word;");
        String value = Var.getString("sys.service.preview.textFont");
        if (!value.isEmpty()) {
            html.append("font-family:");
            html.append(value);
            html.append(';');
        }

        html.append("line-height:");
        html.append(Var.getString("sys.service.preview.textLineHeight"));
        html.append(";font-size:");
        html.append(Var.getString("sys.service.preview.textFontSize"));
        html.append(";}.header{");
        value = Var.getString("sys.service.preview.headerBackColor");
        if (!value.isEmpty()) {
            html.append("background-color:");
            html.append(value);
            html.append(';');
        }

        html.append("font-weight:");
        html.append(Var.getString("sys.service.preview.headerFontWeight"));
        html.append(
            ";}td{border:1px solid #000000;padding:0 2px 0 2px;}th{border:0;}.wrap{word-wrap:break-word;}</style></head><body>");
        JSONArray fields = createHtmlHeaders(html, headers, title, neptune, rowNumberWidth, rowNumberTitle);
        getTextContent(html, records, fields, dateFormat, timeFormat, rowNumberWidth > -1, true, decimalSeparator,
            thousandSeparator, (String)null);
        html.append("</table></body></html>");
        outputStream.write(html.toString().getBytes("utf-8"));
    }

    public static void outputText(OutputStream outputStream, JSONArray headers, JSONArray records,
        String defaultDateFormat, String defaultTimeFormat, String decimalSeparator, String thousandSeparator)
        throws Exception {
        StringBuilder text = new StringBuilder();
        JSONArray leafs = new JSONArray();
        String lineSeparator = SysUtil.getLineSeparator();
        leafs.put(0);
        markParents(leafs, headers, (JSONObject)null, 0);
        leafs.remove(0);
        int j = leafs.length();

        for (int i = 0; i < j; ++i) {
            if (i > 0) {
                text.append('\t');
            }

            text.append(leafs.getJSONObject(i).optString("text"));
        }

        text.append(lineSeparator);
        getTextContent(text, records, leafs, defaultDateFormat, defaultTimeFormat, false, false, decimalSeparator,
            thousandSeparator, lineSeparator);
        outputStream.write(text.toString().getBytes("utf-8"));
    }

    private static JSONArray createHtmlHeaders(StringBuilder html, JSONArray rawHeaders, String title, boolean neptune,
        int rowNumberWidth, String rowNumberTitle) {
        JSONArray leafs = new JSONArray();
        JSONArray grid = new JSONArray();
        int flexWidth = Var.getInt("sys.service.excel.flexColumnMaxWidth");
        double rate;
        if (neptune) {
            rate = 0.87719298D;
        } else {
            rate = 1.0D;
        }

        int tableWidth;
        if (rowNumberWidth > -1) {
            rowNumberWidth = (int)Math.round((double)rowNumberWidth * rate);
            tableWidth = rowNumberWidth;
        } else {
            tableWidth = 0;
        }

        leafs.put(0);
        markParents(leafs, rawHeaders, (JSONObject)null, 0);
        int maxDepth = leafs.getInt(0);
        leafs.remove(0);
        int j = leafs.length();

        JSONArray row;
        JSONObject node;
        int i;
        for (i = 0; i < j; ++i) {
            node = leafs.getJSONObject(i);
            tableWidth += getHtmlCellWidth(node, flexWidth, rate);
            int y = node.getInt("y");
            node.put("rowspan", maxDepth - y);

            do {
                node.put("colspan", node.getInt("colspan") + 1);
                if (!node.has("x")) {
                    node.put("x", i);
                    y = node.getInt("y");
                    row = grid.optJSONArray(y);
                    if (row == null) {
                        row = new JSONArray();
                        grid.put(y, row);
                    }

                    row.put(i, node);
                }
            } while ((node = (JSONObject)node.opt("parent")) != null);
        }

        if (title != null) {
            html.append("<p style=\"text-align:center;width:");
            html.append(tableWidth);
            html.append("px;");
            String value = Var.getString("sys.service.preview.titleFont");
            if (!value.isEmpty()) {
                html.append("font-family:");
                html.append(value);
                html.append(';');
            }

            html.append("font-weight:");
            html.append(Var.getString("sys.service.preview.titleFontWeight"));
            html.append(";line-height:");
            html.append(Var.getString("sys.service.preview.titleLineHeight"));
            html.append(";font-size:");
            html.append(Var.getString("sys.service.preview.titleFontSize"));
            html.append(";\">");
            html.append(StringUtil.toHTML(title, true, true));
            html.append("</p>");
        }

        html.append("<table style=\"width:");
        html.append(tableWidth);
        html.append("px;\">");
        html.append("<tr style=\"height:0\">");
        if (rowNumberWidth > -1) {
            html.append("<th width=\"");
            html.append(rowNumberWidth);
            html.append("px\"></th>");
        }

        j = leafs.length();

        for (i = 0; i < j; ++i) {
            node = leafs.getJSONObject(i);
            html.append("<th width=\"");
            html.append(getHtmlCellWidth(node, flexWidth, rate));
            html.append("px\"></th>");
        }

        html.append("</tr>");
        j = grid.length();

        for (i = 0; i < j; ++i) {
            html.append("<tr class=\"header\">");
            if (rowNumberWidth > -1 && i == 0) {
                html.append("<td rowspan=\"");
                html.append(maxDepth + 1);
                html.append("\">");
                html.append(rowNumberTitle);
                html.append("</td>");
            }

            row = grid.getJSONArray(i);
            int l = row.length();

            for (int k = 0; k < l; ++k) {
                node = row.optJSONObject(k);
                if (node != null) {
                    html.append("<td align=\"");
                    int colspan = node.getInt("colspan");
                    String align = node.optString("titleAlign");
                    if (StringUtil.isEmpty(align) && colspan > 0) {
                        align = "center";
                    }

                    html.append(align);
                    html.append('"');
                    if (colspan > 0) {
                        html.append(" colspan=\"");
                        html.append(colspan + 1);
                        html.append("\"");
                    }

                    int rowspan = node.getInt("rowspan");
                    if (rowspan > 0) {
                        html.append(" rowspan=\"");
                        html.append(rowspan + 1);
                        html.append("\"");
                    }

                    html.append('>');
                    html.append(node.optString("text"));
                    html.append("</td>");
                }
            }

            html.append("</tr>");
        }

        return leafs;
    }

    private static int getHtmlCellWidth(JSONObject node, int flexWidth, double rate) {
        int width;
        if (node.has("width")) {
            width = node.getInt("width");
        } else if (node.has("flex")) {
            width = flexWidth;
        } else {
            width = 100;
        }

        return (int)Math.round((double)width * rate);
    }

    private static void getTextContent(StringBuilder buf, JSONArray records, JSONArray fields, String defaultDateFormat,
        String defaultTimeFormat, boolean hasRowNumber, boolean isHtml, String decimalSeparator,
        String thousandSeparator, String lineSeparator) {
        int j = records.length();
        int l = fields.length();
        String[] fieldNames = new String[l];
        String boolString = Var.getString("sys.service.excel.boolText");
        String trueText = null;
        String falseText = null;
        String[] aligns = new String[l];
        boolean[] wraps = new boolean[l];
        int[] dataTypes = new int[l];
        Format[] formats = new Format[l];
        Format dateFormat = new SimpleDateFormat(ExcelObject.toJavaDateFormat(defaultDateFormat, true));
        Format dateTimeFormat =
            new SimpleDateFormat(ExcelObject.toJavaDateFormat(defaultDateFormat + " " + defaultTimeFormat, true));
        Object[] keyMaps = new Object[l];
        boolean useBoolString = !boolString.isEmpty();
        if (useBoolString) {
            String[] boolStrings = boolString.split(",");
            trueText = boolStrings[0];
            falseText = boolStrings[1];
        }

        int k;
        for (k = 0; k < l; ++k) {
            JSONObject field = fields.getJSONObject(k);
            String keyName = field.optString("keyName");
            String dataTypeStr;
            if (keyName.isEmpty()) {
                keyMaps[k] = null;
                dataTypeStr = field.optString("type").toLowerCase();
            } else {
                keyMaps[k] = KVBuffer.buffer.get(keyName);
                dataTypeStr = "string";
            }

            String format = field.optString("format");
            formats[k] = null;
            byte dataType;
            if (dataTypeStr.equals("string")) {
                dataType = 1;
            } else if (!dataTypeStr.startsWith("int") && !dataTypeStr.equals("float")
                && !dataTypeStr.equals("number")) {
                if (dataTypeStr.equals("date")) {
                    dataType = 3;
                    if (!StringUtil.isEmpty(format)) {
                        format = ExcelObject.toJavaDateFormat(format, false);
                        if (format == null) {
                            formats[k] = dateFormat;
                        } else {
                            formats[k] = new SimpleDateFormat(format);
                        }
                    }
                } else if (dataTypeStr.startsWith("bool")) {
                    dataType = 4;
                } else {
                    dataType = 5;
                }
            } else {
                dataType = 2;
                if (!format.isEmpty()) {
                    DecimalFormat decimalFormat = new DecimalFormat(format);
                    decimalFormat.setRoundingMode(RoundingMode.HALF_UP);
                    DecimalFormatSymbols dfs = new DecimalFormatSymbols();
                    dfs.setDecimalSeparator(decimalSeparator.charAt(0));
                    dfs.setGroupingSeparator(thousandSeparator.charAt(0));
                    decimalFormat.setDecimalFormatSymbols(dfs);
                    formats[k] = decimalFormat;
                }
            }

            dataTypes[k] = dataType;
            fieldNames[k] = field.optString("field");
            aligns[k] = field.optString("align");
            wraps[k] = Boolean.TRUE.equals(field.opt("autoWrap"));
        }

        for (int i = 0; i < j; ++i) {
            JSONObject record = (JSONObject)records.opt(i);
            if (isHtml) {
                buf.append("<tr>");
                if (hasRowNumber) {
                    buf.append("<td align=\"right\">");
                    buf.append(i + 1);
                    buf.append("</td>");
                }
            } else if (i > 0) {
                buf.append(lineSeparator);
            }

            for (k = 0; k < l; ++k) {
                Object value = JsonUtil.opt(record, fieldNames[k]);
                if (value == null) {
                    if (isHtml) {
                        buf.append("<td></td>");
                    } else {
                        buf.append("\t");
                    }
                } else {
                    if (isHtml) {
                        buf.append("<td");
                        if (!aligns[k].isEmpty()) {
                            buf.append(" align=\"");
                            buf.append(aligns[k]);
                            buf.append("\"");
                        }

                        if (wraps[k]) {
                            buf.append(" class=\"wrap\"");
                        }

                        buf.append('>');
                    } else if (k > 0) {
                        buf.append("\t");
                    }

                    if (keyMaps[k] != null) {
                        value = KVBuffer.getValue((ConcurrentHashMap)keyMaps[k], value);
                    }

                    if (dataTypes[k] == 5) {
                        if (value instanceof Number) {
                            dataTypes[k] = 2;
                        } else if (value instanceof Date) {
                            dataTypes[k] = 3;
                        } else if (value instanceof Boolean) {
                            dataTypes[k] = 4;
                        } else {
                            dataTypes[k] = 1;
                        }
                    }

                    String valueText;
                    switch (dataTypes[k]) {
                        case 2:
                            double number;
                            if (value instanceof Number) {
                                if (formats[k] == null) {
                                    valueText = StringUtil.replaceFirst(value.toString(), ".", decimalSeparator);
                                } else {
                                    number = ((Number)value).doubleValue();
                                    valueText = formats[k].format(number);
                                }
                            } else if (formats[k] == null) {
                                valueText = StringUtil.replaceFirst(value.toString(), ".", decimalSeparator);
                            } else {
                                number = Double.parseDouble(value.toString());
                                valueText = formats[k].format(number);
                            }
                            break;
                        case 3:
                            Object date;
                            if (formats[k] == null) {
                                boolean hasTime;
                                if (value instanceof Date) {
                                    date = (Date)value;
                                    hasTime = !DateUtil.dateToStr((Date)date).endsWith("00:00:00.0");
                                } else {
                                    String dateTimeStr = value.toString();
                                    date = Timestamp.valueOf(dateTimeStr);
                                    hasTime = !dateTimeStr.endsWith("00:00:00.0")
                                        && !(dateTimeStr.endsWith("00:00:00") | dateTimeStr.endsWith("00:00:00.000"));
                                }

                                if (hasTime) {
                                    valueText = dateTimeFormat.format(date);
                                } else {
                                    valueText = dateFormat.format(date);
                                }
                            } else {
                                if (value instanceof Date) {
                                    date = (Date)value;
                                } else {
                                    date = Timestamp.valueOf(value.toString());
                                }

                                valueText = formats[k].format(date);
                            }
                            break;
                        case 4:
                            if (useBoolString) {
                                valueText = StringUtil.getBool(value.toString()) ? trueText : falseText;
                            } else {
                                valueText = value.toString();
                            }
                            break;
                        default:
                            valueText = value.toString();
                    }

                    if (isHtml) {
                        buf.append(StringUtil.toHTML(valueText, true, true));
                        buf.append("</td>");
                    } else {
                        buf.append(valueText);
                    }
                }
            }

            if (isHtml) {
                buf.append("</tr>");
            }
        }

    }

    public static void importToExcel(JSONObject params, File excelFile, OutputStream outputStream, int sheetIndex)
        throws Exception {
        if (excelFile.getName().toLowerCase().endsWith(".xls")) {
            throw new IllegalArgumentException("Excel file version requires 2007+");
        } else {
            FileInputStream is = new FileInputStream(excelFile);

            try {
                importToExcel(params, (InputStream)is, outputStream, sheetIndex);
            } finally {
                is.close();
            }

        }
    }

    public static void importToExcel(JSONObject params, InputStream inputStream, OutputStream outputStream,
        int sheetIndex) throws Exception {
        XSSFWorkbook book = null;

        try {
            book = new XSSFWorkbook(inputStream);
            int j = book.getNumberOfSheets();

            for (int i = j - 1; i >= 0; --i) {
                if (sheetIndex != -1 && i != sheetIndex) {
                    book.removeSheetAt(i);
                } else {
                    Sheet sheet = book.getSheetAt(i);
                    importToSheet(sheet, params);
                    ExcelObject.executeInstruction(sheet, params);
                }
            }

            book.write(outputStream);
        } finally {
            IOUtils.closeQuietly(book);
        }
    }

    public static void importToSheet(Sheet sheet, JSONObject data) {
        Iterator rows = sheet.rowIterator();

        label73:
        while (rows.hasNext()) {
            Row row = (Row)rows.next();
            Iterator cells = row.cellIterator();

            while (true) {
                Cell cell;
                Object value;
                boolean replaced;
                do {
                    do {
                        if (!cells.hasNext()) {
                            continue label73;
                        }

                        cell = (Cell)cells.next();
                        value = ExcelObject.getCellValue(cell);
                    } while (value == null);

                    String name = value.toString();
                    if (name.indexOf("{#") != -1) {
                        replaced = true;
                        name = StringUtil.replaceParams(data, name);
                        value = name;
                    } else {
                        replaced = false;
                    }

                    if (name.startsWith("{") && name.endsWith("}")) {
                        if (name.indexOf(58) == -1) {
                            String[] express = StringUtil.split(name.substring(1, name.length() - 1), ' ');
                            name = express[0];
                            if (name.length() > 0) {
                                char firstChar = name.charAt(0);
                                if (firstChar == '!' || firstChar == '%' || firstChar == '*') {
                                    name = name.substring(1);
                                }
                            } else {
                                name = null;
                            }
                        } else {
                            try {
                                name = (new JSONObject(name)).optString("itemId");
                            } catch (Throwable var13) {
                                name = null;
                            }
                        }

                        if (name == null) {
                            value = null;
                        } else {
                            if (name.startsWith("_") && !data.has(name)) {
                                name = name.substring(1);
                            }

                            String showName = "%" + name;
                            if (data.has(showName)) {
                                value = JsonUtil.opt(data, showName);
                            } else {
                                value = JsonUtil.opt(data, name);
                            }
                        }
                        break;
                    }
                } while (!replaced);

                ExcelObject.setCellValue(cell, value);
            }
        }

    }
}