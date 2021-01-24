package com.wb.interact;

import com.wb.common.Base;
import com.wb.tool.ExcelObject;
import com.wb.util.DbUtil;
import com.wb.util.FileUtil;
import com.wb.util.JsonUtil;
import com.wb.util.StringUtil;
import com.wb.util.WebUtil;
import com.wb.util.ZipUtil;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;

public class Service {
	private static void download(HttpServletRequest request, HttpServletResponse response, boolean webFilesOnly)
			throws Exception {
		JSONArray ja = JsonUtil.getArray(WebUtil.fetch(request, "files"));
		int j = ja.length();
		File[] files = new File[j];

		for (int i = 0; i < j; ++i) {
			if (webFilesOnly) {
				files[i] = new File(Base.path, ja.optString(i));
				FileUtil.checkProctected(request, files[i], true);
			} else {
				files[i] = new File(ja.optString(i));
			}
		}

		boolean useZip = StringUtil.getBool(WebUtil.fetch(request, "zip")) || j > 1 || files[0].isDirectory();
		String downloadName = WebUtil.fetch(request, "downloadName");
		String filename;
		if (StringUtil.isEmpty(downloadName)) {
			filename = files[0].getName();
			if (j == 1) {
				if (useZip) {
					filename = FileUtil.removeExtension(filename) + ".zip";
				}
			} else {
				File parentFile = files[0].getParentFile();
				if (parentFile == null) {
					filename = "file.zip";
				} else {
					filename = parentFile.getName() + ".zip";
				}
			}

			if (filename.equals(".zip") || filename.equals("/.zip")) {
				filename = "file.zip";
			}
		} else {
			filename = downloadName;
		}

		response.setHeader("content-type", "application/force-download");
		response.setHeader("content-disposition", "attachment;" + WebUtil.encodeFilename(request, filename));
		if (useZip) {
			ZipUtil.zip(files, response.getOutputStream());
			response.flushBuffer();
		} else {
			response.setContentLength((int) files[0].length());
			WebUtil.send(response, new FileInputStream(files[0]));
		}

	}

	public static void downloadAtAll(HttpServletRequest request, HttpServletResponse response) throws Exception {
		download(request, response, false);
	}

	public static void downloadAtApp(HttpServletRequest request, HttpServletResponse response) throws Exception {
		download(request, response, true);
	}

	public static void upload(HttpServletRequest request, HttpServletResponse response) throws Exception {
		boolean unzip = StringUtil.getBool((String) request.getAttribute("unzip"));
		boolean sync = StringUtil.getBool((String) request.getAttribute("sync"));
		String path = request.getAttribute("path").toString();
		InputStream stream = (InputStream) request.getAttribute("file");
		String filename = (String) request.getAttribute("filename");
		if (filename == null) {
			filename = (String) request.getAttribute("file__name");
		}

		File destPath;
		File syncPath;
		if (unzip) {
			if (!filename.toLowerCase().endsWith(".zip")) {
				throw new Exception("Invalid zip file.");
			}

			destPath = new File(path);
			ZipUtil.unzip(stream, destPath);
			if (sync) {
				syncPath = FileUtil.getSyncPath(destPath);
				if (syncPath != null) {
					FileUtils.copyDirectory(destPath, syncPath);
				}
			}
		} else {
			destPath = new File(path, filename);
			FileUtil.saveStream(stream, destPath);
			if (sync) {
				syncPath = FileUtil.getSyncPath(destPath);
				if (syncPath != null) {
					FileUtils.copyFile(destPath, syncPath);
				}
			}
		}

	}

	public static void getProgress(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String id = request.getParameter("progressId");
		HttpSession session = request.getSession(true);
		Long pos = (Long) session.getAttribute("sys.upread." + id);
		Long len = (Long) session.getAttribute("sys.uplen." + id);
		double result;
		if (pos != null && len != null && len != 0L) {
			result = (double) pos / (double) len;
		} else {
			result = 0.0D;
		}

		WebUtil.send(response, result);
	}

	public static void importData(HttpServletRequest request, HttpServletResponse response) throws Exception {
		boolean fromServer = StringUtil.getBool(WebUtil.fetch(request, "fromServer"));
		String filename;
		Object stream;
		if (fromServer) {
			filename = WebUtil.fetch(request, "filename");
			stream = new FileInputStream(new File(filename));
		} else {
			filename = ((String) request.getAttribute("file__name")).toLowerCase();
			stream = (InputStream) request.getAttribute("file");
		}

		try {
			filename = filename.toLowerCase();
			String tableName = WebUtil.fetch(request, "table");
			boolean trans = StringUtil.getBool(WebUtil.fetch(request, "trans"));
			Connection connection = DbUtil.getConnection(WebUtil.fetch(request, "jndi"));

			try {
				if (trans) {
					connection.setAutoCommit(false);
				}

				BufferedReader reader;
				if (filename.endsWith(".gz")) {
					reader = new BufferedReader(
							new InputStreamReader(new GZIPInputStream((InputStream) stream), "utf-8"));
					DbUtil.importData(connection, tableName, reader);
				} else if (!filename.endsWith(".xls") && !filename.endsWith(".xlsx")) {
					reader = new BufferedReader(new InputStreamReader((InputStream) stream, "utf-8"));
					String fields = reader.readLine();
					char separator;
					if (fields.indexOf(9) == -1) {
						separator = ',';
					} else {
						separator = '\t';
					}

					String[] fieldList = StringUtil.split(fields, separator);
					DbUtil.importData(connection, tableName, reader, fieldList, separator);
				} else {
					reader = new BufferedReader(new StringReader(
							ExcelObject.excelToJson((InputStream) stream, filename.endsWith(".xlsx"), false)));
					DbUtil.importData(connection, tableName, reader);
				}

				if (trans) {
					connection.commit();
				}
			} finally {
				DbUtil.close(connection);
			}
		} finally {
			((InputStream) stream).close();
		}

	}

	public static void exportJson(HttpServletRequest request, HttpServletResponse response) throws Exception {
		ResultSet rs = (ResultSet) DbUtil.run(request, WebUtil.fetch(request, "sql"), WebUtil.fetch(request, "jndi"));
		String filename = WebUtil.fetch(request, "filename");
		if (filename == null) {
			filename = "data.gz";
		}

		response.setHeader("content-type", "application/force-download");
		response.setHeader("content-disposition", "attachment;" + WebUtil.encodeFilename(request, filename));
		BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new GZIPOutputStream(response.getOutputStream()), "utf-8"));

		try {
			DbUtil.exportData(rs, writer);
		} finally {
			writer.close();
		}

		response.flushBuffer();
	}

	public static void exportJsonToServer(HttpServletRequest request, HttpServletResponse response) throws Exception {
		ResultSet rs = (ResultSet) DbUtil.run(request, WebUtil.fetch(request, "sql"), WebUtil.fetch(request, "jndi"));
		String filename = WebUtil.fetch(request, "filename");
		File file = new File(filename);
		if (file.exists()) {
			throw new Exception("文件 “" + filename + "” 已经存在。");
		} else {
			FileOutputStream fos = null;
			BufferedWriter writer = null;

			try {
				fos = new FileOutputStream(file);
				writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(fos), "utf-8"));
				DbUtil.exportData(rs, writer);
			} finally {
				IOUtils.closeQuietly(writer);
				fos.close();
			}

		}
	}
}