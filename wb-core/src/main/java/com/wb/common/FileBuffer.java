package com.wb.common;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;

public class FileBuffer {
	private static ConcurrentHashMap<String, Object[]> buffer;

	public static void service(String path, HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		byte[] bt = null;
		boolean isGzip = false;
		String pathKey = path.toLowerCase();
		File file;
		long lastModified;
		if (Var.uncheckModified) {
			file = null;
			lastModified = -1L;
		} else {
			file = new File(Base.path, path);
			lastModified = file.lastModified();
		}

		Object[] obj = (Object[]) buffer.get(pathKey);
		if (obj != null && (Var.uncheckModified || lastModified == (Long) obj[2])) {
			isGzip = (Boolean) obj[0];
			bt = (byte[]) obj[1];
			if (Var.uncheckModified) {
				lastModified = (Long) obj[2];
			}
		}

		if (bt == null) {
			if (Var.uncheckModified) {
				file = new File(Base.path, path);
				lastModified = file.lastModified();
			}

			if (lastModified == 0L) {
				response.sendError(404, path);
				return;
			}

			isGzip = file.length() >= (long) Var.gzipMinSize;
			bt = getBytes(file, isGzip);
			obj = new Object[]{isGzip, bt, lastModified};
			buffer.put(pathKey, obj);
		}

		if (Var.cacheMaxAge != -1) {
			if (Var.cacheMaxAge == 0) {
				response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
			} else {
				response.setDateHeader("Last-Modified", (new Date()).getTime());
				response.setHeader("Cache-Control", "max-age=" + Var.cacheMaxAge);
			}
		}

		String fileEtag = Long.toString(lastModified);
		String reqEtag = request.getHeader("If-None-Match");
		if (fileEtag.equals(reqEtag)) {
			response.setStatus(304);
		} else {
			response.setHeader("Etag", fileEtag);
			if (isGzip) {
				response.setHeader("Content-Encoding", "gzip");
			}

			response.setCharacterEncoding("utf-8");
			String contentType = Base.servletContext.getMimeType(path);
			if (contentType == null) {
				contentType = "application/octet-stream";
			}

			response.setContentType(contentType);
			response.setContentLength(bt.length);
			response.getOutputStream().write(bt);
			response.flushBuffer();
		}
	}

	public static synchronized void load() {
		buffer = new ConcurrentHashMap();
	}

	private static byte[] getBytes(File file, boolean isGzip) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		FileInputStream is = new FileInputStream(file);

		byte[] bt;
		try {
			if (isGzip) {
				GZIPOutputStream gos = new GZIPOutputStream(bos);

				try {
					IOUtils.copy(is, gos);
				} finally {
					gos.close();
				}
			} else {
				IOUtils.copy(is, bos);
			}

			bt = bos.toByteArray();
		} finally {
			is.close();
		}

		return bt;
	}
}