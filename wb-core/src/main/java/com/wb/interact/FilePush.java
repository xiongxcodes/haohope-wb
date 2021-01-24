package com.wb.interact;

import com.wb.fit.CustomResponse;
import com.wb.tool.DataOutput;
import com.wb.tool.ExcelObject;
import com.wb.util.FileUtil;
import com.wb.util.StringUtil;
import com.wb.util.WbUtil;
import com.wb.util.WebUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.GZIPOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class FilePush {
	public static void getFile(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String filename = WebUtil.fetch(request, "filename");
		String data = WebUtil.fetch(request, "data");
		boolean gzip = Boolean.parseBoolean(WebUtil.fetch(request, "gzip"));
		if (filename == null) {
			if (gzip) {
				filename = "data.gz";
			} else {
				filename = "data";
			}
		}

		response.setHeader("content-type", "application/force-download");
		response.setHeader("content-disposition", "attachment;" + WebUtil.encodeFilename(request, filename));
		if (gzip) {
			GZIPOutputStream gos = new GZIPOutputStream(response.getOutputStream());

			try {
				gos.write(data.getBytes("utf-8"));
			} finally {
				gos.close();
			}

			response.flushBuffer();
		} else {
			WebUtil.send(response, data);
		}

	}

	public static void getText(HttpServletRequest request, HttpServletResponse response) throws Exception {
		WebUtil.send(response, request.getParameter("text"));
	}

	public static void getExcel(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String ext = ExcelObject.getExtName();
		String filename = WebUtil.fetch(request, "filename");
		String title = WebUtil.fetch(request, "title");
		if (StringUtil.isEmpty(filename)) {
			filename = "data" + ext;
		} else {
			filename = filename + ext;
		}

		response.setHeader("content-type", "application/force-download");
		response.setHeader("content-disposition", "attachment;" + WebUtil.encodeFilename(request, filename));
		DataOutput.outputExcel(response.getOutputStream(), new JSONArray(WebUtil.fetch(request, "headers")),
				new JSONArray(WebUtil.fetch(request, "rows")), StringUtil.isEmpty(title) ? null : title,
				new JSONObject("{mergeInfo:[]}"),
				StringUtil.select(new String[]{WebUtil.fetch(request, "dateFormat"), "Y-m-d"}),
				StringUtil.select(new String[]{WebUtil.fetch(request, "timeFormat"), "H:i:s"}), false);
		response.flushBuffer();
	}

	public static void writeFile(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String filename = WebUtil.fetch(request, "filename");
		String data = WebUtil.fetch(request, "data");
		boolean gzip = Boolean.parseBoolean(WebUtil.fetch(request, "gzip"));
		File file = new File(filename);
		if (file.exists()) {
			throw new Exception("文件 “" + filename + "” 已经存在。");
		} else {
			if (gzip) {
				FileOutputStream fos = null;
				GZIPOutputStream gos = null;

				try {
					fos = new FileOutputStream(file);
					gos = new GZIPOutputStream(fos);
					gos.write(data.getBytes("utf-8"));
				} finally {
					IOUtils.closeQuietly(gos);
					if (fos != null) {
						fos.close();
					}

				}
			} else {
				FileUtil.writeString(file, data);
			}

		}
	}

	public static void transfer(HttpServletRequest request, HttpServletResponse response) throws Exception {
		JSONObject metaParams = new JSONObject(request.getParameter("__metaParams"));
		boolean isHtml = false;
		String type = metaParams.optString("type");
		String fileExtName;
		if ("excel".equals(type)) {
			fileExtName = ExcelObject.getExtName();
		} else if ("text".equals(type)) {
			fileExtName = ".txt";
		} else {
			if (!"html".equals(type)) {
				throw new IllegalArgumentException("Invalid request file type.");
			}

			fileExtName = ".html";
			isHtml = true;
		}

		String data = metaParams.optString("data", (String) null);
		JSONArray records;
		if (data != null) {
			records = new JSONArray(data);
		} else {
			CustomResponse resp = new CustomResponse(response);
			request.setAttribute("sys.rowOnly", 1);
			request.setAttribute("sys.fromExport", 1);
			WebUtil.include(request, resp, metaParams.optString("url"));
			String respText = WbUtil.getResponseString(resp);
			if (respText.startsWith("<textarea>")) {
				data = respText.substring(10, respText.length() - 11);
				JSONObject responseObject = new JSONObject(data);
				data = (String) responseObject.opt("value");
				if (!responseObject.optBoolean("success")) {
					WebUtil.send(response, data, false);
					return;
				}
			} else {
				respText = respText.trim();
				if (!respText.startsWith("{") || !respText.endsWith("}")) {
					WebUtil.send(response, respText, false);
					return;
				}

				data = respText;
			}

			records = (new JSONObject(data)).getJSONArray("rows");
		}

		JSONArray headers = metaParams.optJSONArray("headers");
		String title = metaParams.optString("title", (String) null);
		JSONObject reportInfo = metaParams.optJSONObject("reportInfo");
		String dateFormat = metaParams.optString("dateFormat");
		String timeFormat = metaParams.optString("timeFormat");
		boolean neptune = metaParams.optBoolean("neptune");
		String decimalSeparator = metaParams.optString("decimalSeparator");
		String thousandSeparator = metaParams.optString("thousandSeparator");
		String filename = metaParams.optString("filename");
		if (StringUtil.isEmpty(filename)) {
			filename = "data";
		}

		filename = filename + fileExtName;
		if (!isHtml) {
			response.setHeader("content-type", "application/force-download");
			response.setHeader("content-disposition", "attachment;" + WebUtil.encodeFilename(request, filename));
		}

		if ("excel".equals(type)) {
			DataOutput.outputExcel(response.getOutputStream(), headers, records, title, reportInfo, dateFormat,
					timeFormat, neptune);
		} else if ("text".equals(type)) {
			DataOutput.outputText(response.getOutputStream(), headers, records, dateFormat, timeFormat,
					decimalSeparator, thousandSeparator);
		} else {
			DataOutput.outputHtml(response.getOutputStream(), headers, records, title, dateFormat, timeFormat, neptune,
					metaParams.getInt("rowNumberWidth"), metaParams.optString("rowNumberTitle"), decimalSeparator,
					thousandSeparator);
		}

		response.flushBuffer();
	}
}