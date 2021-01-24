package com.wb.tool;

import com.wb.common.Base;
import com.wb.common.Var;
import com.wb.controls.ExtCombo;
import com.wb.util.StringUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONObject;

public class ExcelForm {
	public static String getHtml(JSONObject params, File excelFile, int sheetIndex, String excelFormAlign)
			throws Exception {
		if (excelFile.getName().toLowerCase().endsWith(".xls")) {
			throw new IllegalArgumentException("Excel file version requires 2007+");
		} else {
			FileInputStream is = new FileInputStream(excelFile);

			String var6;
			try {
				var6 = getHtml(params, (InputStream) is, sheetIndex, excelFormAlign);
			} finally {
				is.close();
			}

			return var6;
		}
	}

	public static String getHtml(JSONObject params, InputStream inputStream, int sheetIndex, String excelFormAlign)
			throws Exception {
		StringBuilder tableHtml = new StringBuilder();
		StringBuilder rowsHtml = new StringBuilder();
		int maxRows = Var.getInt("sys.service.excel.maxTableRows") - 1;
		int maxCols = Var.getInt("sys.service.excel.maxTableColumns") - 1;
		XSSFWorkbook book = new XSSFWorkbook(inputStream);

		try {
			XSSFSheet sheet = book.getSheetAt(sheetIndex);
			ExcelObject.executeInstruction(sheet, params);
			int maxColumnIndex = getMaxColumnIndex(sheet);
			int defaultHeight = getPixcelHeight(sheet.getDefaultRowHeight());
			Object[] mergeRegion = getMergeRegion(sheet);
			Iterator<Row> rows = sheet.rowIterator();
			int lastRow = 0;

			label166 : while (rows.hasNext()) {
				XSSFRow row = (XSSFRow) rows.next();
				int rowIndex = row.getRowNum();
				if (rowIndex > maxRows) {
					throw new IndexOutOfBoundsException(
							"Excel exceeds the maximum allowed rows: Var.sys.service.excel.maxTableRows");
				}

				fillTR(rowsHtml, lastRow, rowIndex, defaultHeight, maxColumnIndex);
				lastRow = rowIndex + 1;
				Iterator<Cell> cells = row.cellIterator();
				rowsHtml.append("<tr height=\"");
				rowsHtml.append(getPixcelHeight(row.getHeight()));
				rowsHtml.append("px\">");
				int lastCol = 0;
				boolean requiredField = false;

				while (true) {
					XSSFCell cell;
					int[] spanXY;
					do {
						if (!cells.hasNext()) {
							fillTD(rowsHtml, lastCol, maxColumnIndex);
							rowsHtml.append("</tr>");
							continue label166;
						}

						cell = (XSSFCell) cells.next();
						int columnIndex = cell.getColumnIndex();
						if (columnIndex > maxCols) {
							throw new IndexOutOfBoundsException(
									"Excel exceeds the maximum allowed columns: Var.sys.service.excel.maxTableColumns");
						}

						spanXY = getSpanXY(mergeRegion, columnIndex, rowIndex);
						fillTD(rowsHtml, lastCol, columnIndex);
						lastCol = columnIndex + 1;
					} while (spanXY[0] == -1);

					XSSFCellStyle style = cell.getCellStyle();
					if (spanXY[0] > 1) {
						lastCol += spanXY[0];
					}

					rowsHtml.append("<td");
					if (spanXY[0] > 1) {
						rowsHtml.append(" colspan=\"");
						rowsHtml.append(spanXY[0]);
						rowsHtml.append("\"");
					}

					if (spanXY[1] > 1) {
						rowsHtml.append(" rowspan=\"");
						rowsHtml.append(spanXY[1]);
						rowsHtml.append("\"");
					}

					rowsHtml.append(" style=\"overflow:hidden;");
					boolean isNumericCell = ExcelObject.isNumericCell(cell);
					if (style != null) {
						rowsHtml.append(ExcelObject.getCellStyle(style, isNumericCell));
					}

					rowsHtml.append("\">");
					String value = ExcelObject.getDisplayText(cell);
					if (!isNumericCell) {
						value = StringUtil.replaceParams(params, value);
						if (value.startsWith("{") && value.endsWith("}")) {
							value = createExpress(value, requiredField);
						} else {
							value = StringUtil.toHTML(value);
						}
					}

					rowsHtml.append(value);
					rowsHtml.append("</td>");
					requiredField = value.startsWith("*");
				}
			}

			int tableWidth = 0;
			tableHtml.append("<tr style=\"height:0;\">");

			for (int i = 0; i < maxColumnIndex; ++i) {
				int columnWidth = getPixcelWidth(sheet.getColumnWidth(i));
				tableHtml.append("<td style=\"border:none;padding:0;width:");
				tableHtml.append(columnWidth);
				tableHtml.append("px;\"></td>");
				tableWidth += columnWidth;
			}

			tableHtml.append("</tr>");
			String alignHtml;
			if ("left".equals(excelFormAlign)) {
				alignHtml = "margin-left:0;margin-right:auto;";
			} else if ("right".equals(excelFormAlign)) {
				alignHtml = "margin-left:auto;margin-right:0;";
			} else {
				alignHtml = "margin-left:auto;margin-right:auto;";
			}

			tableHtml.insert(0, StringUtil.concat(new String[]{
					"<div style=\"padding:8px;\"><table style=\"margin-top:0;margin-bottom:0;", alignHtml,
					"table-layout:fixed;border-collapse:collapse;\" cellspacing=\"0\" cellpadding=\"5\" width=\"",
					Integer.toString(tableWidth), "px\">"}));
			tableHtml.append(rowsHtml);
			tableHtml.append("</table></div>");
		} finally {
			book.close();
		}

		return tableHtml.toString();
	}

	public static JSONObject getValues(String tplFileRelPath, InputStream dataStream) throws Exception {
		FileInputStream tplStream = null;

		JSONObject var4;
		try {
			tplStream = new FileInputStream(new File(Base.path, tplFileRelPath));
			var4 = getValues(tplStream, 0, dataStream, 0);
		} finally {
			if (tplStream != null) {
				IOUtils.closeQuietly(tplStream);
			}

		}

		return var4;
	}

	public static JSONObject getValues(InputStream tplStream, int tplSheetIndex, InputStream dataStream,
			int dataSheetIndex) throws Exception {
		HashMap<String, String> paramMap = new HashMap();
		JSONObject result = new JSONObject();
		XSSFWorkbook book = new XSSFWorkbook(tplStream);

		XSSFSheet sheet;
		XSSFRow row;
		XSSFCell cell;
		Iterator rows;
		Iterator cells;
		String strValue;
		Object value;
		int rowIndex;
		try {
			sheet = book.getSheetAt(tplSheetIndex);
			rows = sheet.rowIterator();

			while (rows.hasNext()) {
				row = (XSSFRow) rows.next();
				rowIndex = row.getRowNum();
				cells = row.cellIterator();

				while (cells.hasNext()) {
					cell = (XSSFCell) cells.next();
					value = ExcelObject.getCellValue(cell);
					if (value != null) {
						strValue = value.toString();
						if (strValue.startsWith("{") && strValue.endsWith("}")) {
							String itemId = extractItemId(strValue);
							if (!StringUtil.isEmpty(itemId)) {
								paramMap.put(Integer.toString(rowIndex) + "," + Integer.toString(cell.getColumnIndex()),
										itemId);
							}
						}
					}
				}
			}
		} finally {
			book.close();
		}

		book = new XSSFWorkbook(dataStream);

		try {
			sheet = book.getSheetAt(dataSheetIndex);
			rows = sheet.rowIterator();

			while (rows.hasNext()) {
				row = (XSSFRow) rows.next();
				rowIndex = row.getRowNum();
				cells = row.cellIterator();

				while (cells.hasNext()) {
					cell = (XSSFCell) cells.next();
					strValue = (String) paramMap
							.get(Integer.toString(rowIndex) + "," + Integer.toString(cell.getColumnIndex()));
					if (strValue != null) {
						value = ExcelObject.getCellValue(cell);
						result.put(strValue, value == null ? JSONObject.NULL : value);
					}
				}
			}
		} finally {
			book.close();
		}

		return result;
	}

	private static String extractItemId(String exp) {
		String innerText = exp.substring(1, exp.length() - 1).trim();
		if (innerText.startsWith("%")) {
			return innerText.substring(1);
		} else if (innerText.indexOf(58) == -1) {
			String[] items = StringUtil.split(innerText, ' ');
			return !items[0].startsWith("*") && !items[0].startsWith("!") ? items[0] : items[0].substring(1);
		} else {
			try {
				return (new JSONObject("{" + innerText + "}")).optString("itemId");
			} catch (Throwable var3) {
				return null;
			}
		}
	}

	private static String createExpress(String value, boolean required) {
		String innerText = value.substring(1, value.length() - 1).trim();
		if (innerText.startsWith("%")) {
			return "##" + innerText.substring(1);
		} else if (innerText.indexOf(58) == -1) {
			String[] items = StringUtil.split(innerText, ' ');
			StringBuilder script = new StringBuilder();
			if (items[0].startsWith("*")) {
				required = true;
				items[0] = items[0].substring(1);
			} else if (items[0].startsWith("!")) {
				required = false;
				items[0] = items[0].substring(1);
			}

			script.append("##{itemId:\"");
			script.append(items[0]);
			script.append("\"");
			if (required) {
				script.append(",allowBlank:false");
			}

			script.append(",xtype:\"");
			if (items.length > 1) {
				if (items[1].startsWith("%")) {
					script.append("combobox\",");
					script.append(ExtCombo.getkeyNameScript(items[1].substring(1)));
					script.append('}');
				} else {
					script.append(items[1]);
					if (items.length > 2) {
						if ("image".equals(items[1])) {
							script.append("\",src:\"");
						} else {
							script.append("\",vtype:\"");
						}

						script.append(items[2]);
					}

					script.append("\"}");
				}
			} else {
				script.append("textfield");
				script.append("\"}");
			}

			return script.toString();
		} else {
			return "##" + value;
		}
	}

	private static void fillTD(StringBuilder html, int fromIndex, int toIndex) {
		for (int i = fromIndex; i < toIndex; ++i) {
			html.append("<td></td>");
		}

	}

	private static void fillTR(StringBuilder html, int fromIndex, int toIndex, int defaultRowHeight,
			int maxColumnIndex) {
		for (int i = fromIndex; i < toIndex; ++i) {
			html.append("<tr height=\"");
			html.append(defaultRowHeight);
			html.append("px\"><td colspan=\"");
			html.append(maxColumnIndex);
			html.append("\"></tr>");
		}

	}

	private static int[] getSpanXY(Object[] mergeRegion, int columnIndex, int rowIndex) {
		int[] result = new int[]{-2, -2};
		Object[] var8 = mergeRegion;
		int var7 = mergeRegion.length;

		for (int var6 = 0; var6 < var7; ++var6) {
			Object obj = var8[var6];
			int[] info = (int[]) obj;
			if (columnIndex == info[0] && rowIndex == info[1]) {
				result[0] = info[2] - info[0] + 1;
				result[1] = info[3] - info[1] + 1;
				return result;
			}

			if (columnIndex >= info[0] && columnIndex <= info[2] && rowIndex >= info[1] && rowIndex <= info[3]) {
				result[0] = -1;
				result[1] = -1;
			}
		}

		return result;
	}

	private static int getMaxColumnIndex(XSSFSheet sheet) {
		int index = 0;

		XSSFRow row;
		for (Iterator rows = sheet.rowIterator(); rows.hasNext(); index = Math.max(index, row.getLastCellNum())) {
			row = (XSSFRow) rows.next();
		}

		return index;
	}

	private static int getPixcelWidth(int width) {
		return Math.round((float) width * 0.0281F);
	}

	private static int getPixcelHeight(int height) {
		return Math.round((float) height / 15.12F);
	}

	private static Object[] getMergeRegion(XSSFSheet sheet) {
		int mergeCount = sheet.getNumMergedRegions();
		Object[] mergeRegion = new Object[mergeCount];

		for (int i = 0; i < mergeCount; ++i) {
			CellRangeAddress rangeAddress = sheet.getMergedRegion(i);
			int[] info = new int[]{rangeAddress.getFirstColumn(), rangeAddress.getFirstRow(),
					rangeAddress.getLastColumn(), rangeAddress.getLastRow()};
			mergeRegion[i] = info;
		}

		return mergeRegion;
	}
}