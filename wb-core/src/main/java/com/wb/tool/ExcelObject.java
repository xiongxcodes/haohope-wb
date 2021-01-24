package com.wb.tool;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Iterator;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FontUnderline;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;

import com.wb.common.Var;
import com.wb.util.DateUtil;
import com.wb.util.JsonUtil;
import com.wb.util.StringUtil;

public class ExcelObject {
    public static String excelToJson(InputStream inputStream, boolean xlsxFormat, boolean jsonFormat) throws Exception {
        int rowIndex = 0;
        int colLength = 0;
        JSONArray fieldList = new JSONArray();
        StringBuilder text = new StringBuilder("");
        Object book;
        if (xlsxFormat) {
            book = new XSSFWorkbook(inputStream);
        } else {
            book = new HSSFWorkbook(inputStream);
        }

        try {
            Sheet sheet = ((Workbook)book).getSheetAt(0);
            Iterator<Row> rows = sheet.rowIterator();
            if (jsonFormat) {
                text.append("{rows:[");
            }

            while (true) {
                if (!rows.hasNext()) {
                    if (jsonFormat) {
                        text.append("]}");
                    }
                    break;
                }

                if (rowIndex > 1) {
                    if (jsonFormat) {
                        text.append(',');
                    } else {
                        text.append("\n");
                    }
                }

                if (rowIndex > 0) {
                    text.append('{');
                }

                Row row = (Row)rows.next();
                Iterator<Cell> cells = row.cellIterator();
                boolean isFirst = true;

                while (true) {
                    while (cells.hasNext()) {
                        Cell cell = (Cell)cells.next();
                        int colIndex = cell.getColumnIndex();
                        Object value = getCellValue(cell);
                        if (rowIndex == 0) {
                            if (value == null) {
                                throw new NullPointerException("Field name has null value.");
                            }

                            String valueStr = value.toString();
                            int pos = valueStr.indexOf(40);
                            int pos1 = valueStr.indexOf("（");
                            if (pos1 != -1 && (pos == -1 || pos1 < pos)) {
                                pos = pos1;
                            }

                            if (pos == -1) {
                                fieldList.add(colIndex, valueStr);
                            } else {
                                fieldList.add(colIndex, valueStr.substring(0, pos));
                            }
                        } else {
                            if (isFirst) {
                                isFirst = false;
                            } else {
                                text.append(',');
                            }

                            if (colIndex >= colLength) {
                                throw new RuntimeException(
                                    "Row " + (rowIndex + 1) + " column " + (colIndex + 1) + " is out of bounds.");
                            }

                            text.append(StringUtil.quote(fieldList.getString(colIndex)));
                            text.append(':');
                            text.append(StringUtil.encode(value));
                        }
                    }

                    if (rowIndex == 0) {
                        colLength = fieldList.length();
                    } else {
                        text.append('}');
                    }

                    ++rowIndex;
                    break;
                }
            }
        } finally {
            ((Workbook)book).close();
        }

        return text.toString();
    }

    public static String excelToJson(File file) throws Exception {
        FileInputStream is = new FileInputStream(file);

        String var3;
        try {
            var3 = excelToJson(is, true, true);
        } finally {
            is.close();
        }

        return var3;
    }

    public static String getDisplayText(Cell cell) {
        Object object = getCellValue(cell);
        if (object == null) {
            return "";
        } else if (object instanceof Boolean) {
            return object.toString();
        } else if (object instanceof Double) {
            double doubleVal = (Double)object;
            String format = cell.getCellStyle().getDataFormatString();
            String value = StringUtil.formatNumber(doubleVal, convertNumFormat(format));
            if (doubleVal < 0.0D && format != null && format.indexOf(59) != -1 && format.indexOf("[Red]") != -1) {
                value = "<span style=\"color:red;\">" + value + "</span>";
            }

            return value;
        } else {
            return object instanceof Date ? DateUtil.format((Date)object, "yyyy/M/d") : object.toString();
        }
    }

    public static String convertNumFormat(String format) {
        if (format == null) {
            return "0";
        } else if ("General".equals(format)) {
            return "0.##";
        } else {
            String[] keys = new String[] {"\"", "_", "(", ")", "*", " ", "\\", "/"};
            int pos = format.indexOf(59);
            if (pos != -1) {
                format = format.substring(0, pos);
            }

            String[] var6 = keys;
            int var5 = keys.length;

            for (int var4 = 0; var4 < var5; ++var4) {
                String key = var6[var4];
                format = StringUtil.replaceAll(format, key, "");
            }

            return format;
        }
    }

    public static short getAlignment(String align, short defaultAlign) {
        if ("right".equals(align)) {
            return 3;
        } else if ("center".equals(align)) {
            return 2;
        } else {
            return "left".equals(align) ? 1 : defaultAlign;
        }
    }

    public static String toHtmlAlign(short align, String defaultAlign) {
        switch (align) {
            case 1:
                return "left";
            case 2:
                return "center";
            case 3:
                return "right";
            case 4:
            default:
                return defaultAlign;
            case 5:
                return "justify";
        }
    }

    public static String toHtmlVerticalAlign(short align, String defaultAlign) {
        switch (align) {
            case 0:
                return "top";
            case 1:
                return "middle";
            case 2:
                return "bottom";
            default:
                return defaultAlign;
        }
    }

    public static Workbook getBook() throws IOException {
        return (Workbook)(Var.getBool("sys.service.excel.xlsx") ? new SXSSFWorkbook() : new HSSFWorkbook());
    }

    public static String getExtName() {
        return Var.getBool("sys.service.excel.xlsx") ? ".xlsx" : ".xls";
    }

    public static String toExcelDateFormat(String format, boolean returnDefault) {
        String[] unSupportFormats = new String[] {"N", "S", "w", "z", "W", "t", "L", "o", "u", "O", "P", "T", "Z", "c",
            "U", "MS", "time", "timestamp"};
        String[][] supportFormats = new String[][] {{"d", "dd"}, {"D", "aaa"}, {"j", "d"}, {"l", "aaaa"}, {"F", "mmmm"},
            {"m", "mm"}, {"M", "mmm"}, {"n", "m"}, {"Y", "yyyy"}, {"y", "yy"}, {"a", "am/pm"}, {"A", "AM/PM"},
            {"g", "h"}, {"G", "hh"}, {"h", "h"}, {"H", "hh"}, {"i", "mm"}, {"s", "ss"}};
        String[] var7 = unSupportFormats;
        int var6 = unSupportFormats.length;

        int var5;
        for (var5 = 0; var5 < var6; ++var5) {
            String s = var7[var5];
            if (format.indexOf(s) != -1) {
                return returnDefault ? "yyyy-mm-dd" : null;
            }
        }

        String[][] var9 = supportFormats;
        var6 = supportFormats.length;

        for (var5 = 0; var5 < var6; ++var5) {
            String[] s = var9[var5];
            format = StringUtil.replaceAll(format, s[0], s[1]);
        }

        return format;
    }

    public static String toJavaDateFormat(String format, boolean returnDefault) {
        String[] unSupportFormats = new String[] {"N", "S", "D", "w", "z", "W", "t", "L", "o", "O", "P", "T", "Z", "c",
            "U", "F", "MS", "l", "M", "time", "timestamp"};
        String[][] supportFormats = new String[][] {{"y", "yy"}, {"Y", "yyyy"}, {"m", "MM"}, {"n", "M"}, {"d", "dd"},
            {"j", "d"}, {"H", "HH"}, {"h", "hh"}, {"G", "H"}, {"g", "h"}, {"i", "mm"}, {"s", "ss"}, {"u", "SSS"},
            {"a", "'_x'"}, {"A", "'_X'"}};
        String[] var7 = unSupportFormats;
        int var6 = unSupportFormats.length;

        int var5;
        for (var5 = 0; var5 < var6; ++var5) {
            String s = var7[var5];
            if (format.indexOf(s) != -1) {
                return returnDefault ? "yyyy-MM-dd" : null;
            }
        }

        String[][] var9 = supportFormats;
        var6 = supportFormats.length;

        for (var5 = 0; var5 < var6; ++var5) {
            String[] s = var9[var5];
            format = StringUtil.replaceAll(format, s[0], s[1]);
        }

        return format;
    }

    public static String getCellStyle(XSSFCellStyle style, boolean isNumber) {
        StringBuilder buf = new StringBuilder();
        XSSFFont font = style.getFont();
        buf.append("text-align:");
        buf.append(toHtmlAlign(style.getAlignment().getCode(), isNumber ? "right" : "left"));
        buf.append(";vertical-align:");
        buf.append(toHtmlVerticalAlign(style.getVerticalAlignment().getCode(), "middle"));
        buf.append(";font-family:");
        buf.append(font.getFontName());
        buf.append(";font-size:");
        buf.append(font.getFontHeightInPoints());
        buf.append("pt;font-weight:");
        // buf.append(font.getBoldweight());
        buf.append(font.getFontHeight());
        String rgb = getRGBColor(font.getXSSFColor());
        if (rgb != null) {
            buf.append(";color:");
            buf.append(rgb);
        }

        XSSFColor color = style.getFillForegroundXSSFColor();
        if (color != null) {
            rgb = getRGBColor(color);
            if (rgb != null) {
                buf.append(";background-color:");
                buf.append(rgb);
            }
        }

        if (font.getItalic()) {
            buf.append(";font-style:italic;");
        }

        if (font.getStrikeout()) {
            buf.append(";text-decoration:line-through;");
        } else if (FontUnderline.valueOf(font.getUnderline()) != FontUnderline.NONE) {
            buf.append(";text-decoration:underline;");
        }

        buf.append(";border-top:");
        buf.append(getBorderStyle(style.getBorderTop().getCode(), style.getTopBorderXSSFColor()));
        buf.append(";border-right:");
        buf.append(getBorderStyle(style.getBorderRight().getCode(), style.getRightBorderXSSFColor()));
        buf.append(";border-bottom:");
        buf.append(getBorderStyle(style.getBorderBottom().getCode(), style.getBottomBorderXSSFColor()));
        buf.append(";border-left:");
        buf.append(getBorderStyle(style.getBorderLeft().getCode(), style.getLeftBorderXSSFColor()));
        return buf.toString();
    }

    private static String getBorderStyle(short border, XSSFColor color) {
        String width;
        String style;
        switch (border) {
            case 0:
                return "none";
            case 1:
            default:
                width = "thin";
                style = "solid";
                break;
            case 2:
                width = "medium";
                style = "solid";
                break;
            case 3:
            case 9:
            case 11:
            case 13:
                width = "thin";
                style = "dashed";
                break;
            case 4:
            case 7:
                width = "thin";
                style = "dotted";
                break;
            case 5:
                width = "thick";
                style = "solid";
                break;
            case 6:
                width = "thin";
                style = "double";
                break;
            case 8:
            case 10:
            case 12:
                width = "medium";
                style = "dashed";
        }

        String rgb = getRGBColor(color);
        if (rgb == null) {
            rgb = "black";
        }

        return String.format("%s %s %s", width, style, rgb);
    }

    public static String getRGBColor(XSSFColor color) {
        if (color == null) {
            return null;
        } else {
            byte[] xrgb = color.getRGBWithTint();
            if (xrgb == null) {
                return null;
            } else {
                int red = xrgb[0] < 0 ? xrgb[0] + 256 : xrgb[0];
                int green = xrgb[1] < 0 ? xrgb[1] + 256 : xrgb[1];
                int blue = xrgb[2] < 0 ? xrgb[2] + 256 : xrgb[2];
                return String.format("#%02x%02x%02x", red, green, blue);
            }
        }
    }

    public static Object getCellValue(Cell cell) {
        switch (cell.getCellType().getCode()) {
            case 0:
            case 2:
                if (isDateFormatted(cell)) {
                    return cell.getDateCellValue();
                }

                return cell.getNumericCellValue();
            case 1:
                return cell.getStringCellValue();
            case 3:
            default:
                return null;
            case 4:
                return cell.getBooleanCellValue();
        }
    }

    public static boolean isDateFormatted(Cell cell) {
        String format = cell.getCellStyle().getDataFormatString();
        if (format == null) {
            return true;
        } else if (format.toLowerCase().equals("general")) {
            return false;
        } else if (format.startsWith("reserved")) {
            return true;
        } else {
            int pos = format.lastIndexOf(93);
            if (pos != -1) {
                format = format.substring(pos + 1);
            }

            return format.indexOf(48) == -1 && format.indexOf(35) == -1;
        }
    }

    public static boolean isNumericCell(Cell cell) {
        int cellType = cell.getCellType().getCode();
        return cellType == 2 || cellType == 0;
    }

    public static void fillRows(Sheet sheet, JSONArray fill, JSONObject params) {
        if (fill != null) {
            int j = fill.length();

            for (int i = 0; i < j; ++i) {
                JSONObject obj = fill.optJSONObject(i);
                int x = obj.optInt("x");
                int y = obj.optInt("y");
                JSONArray data = JsonUtil.getArray(params.opt(obj.optString("name")));
                String[] fields = createRows(sheet, x, y, data);
                if (data != null) {
                    JSONObject mergeConfig;
                    try {
                        mergeConfig = new JSONObject();
                        JSONArray mergeRows = obj.optJSONArray("mergeRows");
                        JSONArray mergeCols = obj.optJSONArray("mergeCols");
                        JSONArray mergeInfo = new JSONArray();
                        mergeConfig.put("mergeInfo", mergeInfo);
                        if (mergeRows != null) {
                            mergeConfig.put("mergeRows", true);
                        }

                        String[] var23 = fields;
                        int var22 = fields.length;
                        int var21 = 0;

                        while (true) {
                            if (var21 >= var22) {
                                if (mergeCols != null) {
                                    mergeConfig.put("mergeCols", true);
                                    int l = mergeCols.length();

                                    for (int k = 0; k < l; ++k) {
                                        JSONArray subItem = mergeCols.getJSONArray(k);
                                        int n = subItem.length();

                                        for (int m = 0; m < n; ++m) {
                                            mergeInfo.getJSONArray(StringUtil.indexOf(fields, subItem.getString(m)))
                                                .put(1, "g" + k);
                                        }
                                    }
                                }
                                break;
                            }

                            String field = var23[var21];
                            JSONArray mergeItem = new JSONArray();
                            if (mergeRows != null) {
                                mergeItem.put(mergeRows.indexOf(field) != -1);
                            } else {
                                mergeItem.put(false);
                            }

                            mergeItem.put(JSONObject.NULL);
                            mergeInfo.put(mergeItem);
                            ++var21;
                        }
                    } catch (Throwable var24) {
                        throw new IllegalArgumentException("Invalid merge config " + obj.toString());
                    }

                    mergeCells(sheet, mergeConfig, y, y + data.length());
                }
            }

        }
    }

    public static String[] createRows(Sheet sheet, int x, int y, JSONArray recs) {
        Row row = sheet.getRow(y);
        CellStyle rowStyle = row.getRowStyle();
        short height = row.getHeight();
        int l = row.getLastCellNum() - x;
        String[] fields = new String[l];
        CellStyle[] style = new CellStyle[l];

        int k;
        Cell cell;
        for (k = x; k < l; ++k) {
            cell = row.getCell(k);
            Object objVal = getCellValue(cell);
            int m = k - x;
            fields[m] = objVal == null ? null : StringUtil.force(objVal.toString());
            style[m] = cell.getCellStyle();
        }

        if (recs != null && recs.length() != 0) {
            int j = recs.length();
            if (j != 1) {
                sheet.shiftRows(y, sheet.getLastRowNum(), j - 1);
            }

            for (int i = 0; i < j; ++i) {
                row = sheet.createRow(i + y);
                row.setRowStyle(rowStyle);
                row.setHeight(height);
                JSONObject rec = recs.optJSONObject(i);
                if (rec != null) {
                    for (k = 0; k < l; ++k) {
                        cell = row.createCell(k + x);
                        cell.setCellStyle(style[k]);
                        setCellValue(cell, JsonUtil.opt(rec, fields[k]));
                    }
                }
            }

            return fields;
        } else {
            removeRow(sheet, y);
            return fields;
        }
    }

    public static void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
        } else {
            String strVal;
            if (value instanceof String) {
                strVal = (String)value;
                if (DateUtil.isDate(strVal)) {
                    cell.setCellValue(DateUtil.strToDate(strVal));
                } else {
                    if (strVal.indexOf(10) != -1) {
                        cell.getCellStyle().setWrapText(true);
                    }

                    cell.setCellValue(strVal);
                }
            } else if (value instanceof Number) {
                cell.setCellValue(((Number)value).doubleValue());
            } else if (value instanceof Date) {
                cell.setCellValue((Date)value);
            } else if (value instanceof Boolean) {
                cell.setCellValue((Boolean)value);
            } else {
                strVal = value.toString();
                if (strVal.indexOf(10) != -1) {
                    cell.getCellStyle().setWrapText(true);
                }

                cell.setCellValue(value.toString());
            }
        }

    }

    public static void removeRow(Sheet sheet, int rowIndex) {
        int lastRowNum = sheet.getLastRowNum();
        int mergeRegions = sheet.getNumMergedRegions();

        for (int i = mergeRegions - 1; i >= 0; --i) {
            CellRangeAddress range = sheet.getMergedRegion(i);
            if (range.getFirstRow() <= rowIndex && range.getLastRow() >= rowIndex) {
                sheet.removeMergedRegion(i);
            }
        }

        if (rowIndex >= 0 && rowIndex < lastRowNum) {
            sheet.shiftRows(rowIndex + 1, lastRowNum, -1);
        }

        if (rowIndex == lastRowNum) {
            Row removingRow = sheet.getRow(rowIndex);
            if (removingRow != null) {
                sheet.removeRow(removingRow);
            }
        }

    }

    public static void executeInstruction(Sheet sheet, JSONObject params) {
        int lastRowIndex = sheet.getLastRowNum();
        Row lastRow = sheet.getRow(lastRowIndex);
        if (lastRow != null) {
            Cell cell = lastRow.getCell(0);
            if (cell != null) {
                Object value = getCellValue(cell);
                if (value != null) {
                    String instruction = value.toString();
                    instruction = StringUtil.replaceParams(params, instruction);
                    if (instruction.startsWith("!!")) {
                        instruction = instruction.substring(2);
                        removeRow(sheet, lastRowIndex);

                        JSONArray ja;
                        try {
                            if (instruction.startsWith("{")) {
                                ja = new JSONArray("[" + instruction + "]");
                            } else {
                                ja = new JSONArray(instruction);
                            }

                            int j = ja.length();

                            for (int i = 0; i < j; ++i) {
                                JSONObject jo = ja.getJSONObject(i);
                                jo.put("mergeRows", JsonUtil.toArray(jo.opt("mergeRows")));
                                JSONArray mergeCols = jo.optJSONArray("mergeCols");
                                if (mergeCols != null && mergeCols.length() > 0
                                    && !(mergeCols.opt(0) instanceof JSONArray)) {
                                    jo.put("mergeCols", (new JSONArray()).put(mergeCols));
                                }
                            }
                        } catch (Throwable var13) {
                            throw new IllegalArgumentException("Invalid fill instruction " + instruction);
                        }

                        fillRows(sheet, ja, params);
                    }
                }
            }
        }
    }

    public static void mergeCells(Sheet sheet, JSONObject config, int startRow, int endRow) {
        JSONArray mergeInfo = config.optJSONArray("mergeInfo");
        int j = mergeInfo.length();
        int lastRowNum = sheet.getLastRowNum();
        int lastColNum = j - 1;
        int i;
        int rowIndex;
        int span;
        Iterator rows;
        Row row;
        Cell cell;
        Object object;
        String value;
        String prevValue;
        boolean isLast;
        if (config.optBoolean("mergeRows")) {
            label156:
            for (i = 0; i < j; ++i) {
                if (mergeInfo.getJSONArray(i).getBoolean(0)) {
                    rows = sheet.rowIterator();
                    span = 0;
                    rowIndex = 0;
                    prevValue = null;

                    while (true) {
                        while (true) {
                            if (!rows.hasNext()) {
                                continue label156;
                            }

                            row = (Row)rows.next();
                            if (rowIndex < startRow) {
                                ++rowIndex;
                            } else {
                                if (rowIndex > endRow) {
                                    continue label156;
                                }

                                cell = row.getCell(i);
                                object = getCellValue(cell);
                                if (object == null) {
                                    value = "";
                                } else {
                                    value = object.toString();
                                }

                                isLast = rowIndex == lastRowNum;
                                if (prevValue != null && (!value.equals(prevValue) || isLast)) {
                                    if (isLast) {
                                        if (value.equals(prevValue)) {
                                            ++span;
                                        } else {
                                            isLast = false;
                                        }
                                    }

                                    if (span > 1) {
                                        if (isLast) {
                                            sheet.addMergedRegion(
                                                new CellRangeAddress(rowIndex - span + 1, rowIndex - 1, i, i));
                                        } else {
                                            sheet.addMergedRegion(
                                                new CellRangeAddress(rowIndex - span, rowIndex - 1, i, i));
                                        }
                                    }

                                    span = 0;
                                }

                                prevValue = value;
                                ++span;
                                ++rowIndex;
                            }
                        }
                    }
                }
            }
        }

        rowIndex = 0;
        if (config.optBoolean("mergeCols")) {
            rows = sheet.rowIterator();
            String[] colGroup = new String[j];

            for (i = 0; i < j; ++i) {
                colGroup[i] = mergeInfo.getJSONArray(i).optString(1);
            }

            while (true) {
                while (rows.hasNext()) {
                    row = (Row)rows.next();
                    if (rowIndex < startRow) {
                        ++rowIndex;
                    } else {
                        if (rowIndex > endRow) {
                            return;
                        }

                        Iterator<Cell> cells = row.cellIterator();
                        span = 0;
                        int colIndex = 0;
                        prevValue = null;

                        for (String prevGroup = null; cells.hasNext(); ++colIndex) {
                            cell = (Cell)cells.next();
                            object = getCellValue(cell);
                            String group = colGroup[colIndex];
                            if (object == null) {
                                value = "";
                            } else {
                                value = object.toString();
                            }

                            isLast = colIndex == lastColNum;
                            if (prevValue != null && (!value.equals(prevValue) || !group.equals(prevGroup) || isLast)) {
                                if (isLast) {
                                    if (value.equals(prevValue)) {
                                        ++span;
                                    } else {
                                        isLast = false;
                                    }
                                }

                                if (span > 1) {
                                    if (isLast && !group.isEmpty()
                                        && notInMergeRegion(sheet, rowIndex, colIndex - span + 1, colIndex)) {
                                        sheet.addMergedRegion(
                                            new CellRangeAddress(rowIndex, rowIndex, colIndex - span + 1, colIndex));
                                    } else if (!prevGroup.isEmpty()
                                        && notInMergeRegion(sheet, rowIndex, colIndex - span, colIndex - 1)) {
                                        sheet.addMergedRegion(
                                            new CellRangeAddress(rowIndex, rowIndex, colIndex - span, colIndex - 1));
                                    }
                                }

                                span = 0;
                            }

                            prevValue = value;
                            prevGroup = group;
                            ++span;
                        }

                        ++rowIndex;
                    }
                }

                return;
            }
        }
    }

    private static boolean notInMergeRegion(Sheet sheet, int rowIndex, int beginCol, int endCol) {
        int j = sheet.getNumMergedRegions();

        for (int i = 0; i < j; ++i) {
            CellRangeAddress range = sheet.getMergedRegion(i);
            int col = range.getFirstColumn();
            if (range.getFirstRow() <= rowIndex && range.getLastRow() >= rowIndex && col >= beginCol && col <= endCol) {
                return false;
            }
        }

        return true;
    }
}