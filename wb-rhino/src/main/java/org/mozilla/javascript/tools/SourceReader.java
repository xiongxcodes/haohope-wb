/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.tools.shell.ParsedContentType;

public class SourceReader {
	public static Object readFileOrUrl(String path, boolean convertToString,
			String defaultEncoding) throws IOException {
		URL url = null;
		if (path.indexOf(58) >= 2) {
			try {
				url = new URL(path);
			} catch (MalformedURLException arg13) {
				;
			}
		}

		Object is = null;
		boolean capacityHint = false;

		String encoding;
		String contentType;
		byte[] data;
		try {
			int capacityHint1;
			if (url == null) {
				File result = new File(path);
				encoding = null;
				contentType = null;
				capacityHint1 = (int) result.length();
				is = new FileInputStream(result);
			} else {
				URLConnection result1 = url.openConnection();
				is = result1.getInputStream();
				if (convertToString) {
					ParsedContentType strResult = new ParsedContentType(
							result1.getContentType());
					contentType = strResult.getContentType();
					encoding = strResult.getEncoding();
				} else {
					encoding = null;
					contentType = null;
				}

				capacityHint1 = result1.getContentLength();
				if (capacityHint1 > 1048576) {
					capacityHint1 = -1;
				}
			}

			if (capacityHint1 <= 0) {
				capacityHint1 = 4096;
			}

			data = Kit.readStream((InputStream) is, capacityHint1);
		} finally {
			if (is != null) {
				((InputStream) is).close();
			}

		}

		Object result2;
		if (!convertToString) {
			result2 = data;
		} else {
			if (encoding == null) {
				if (data.length > 3 && data[0] == -1 && data[1] == -2
						&& data[2] == 0 && data[3] == 0) {
					encoding = "UTF-32LE";
				} else if (data.length > 3 && data[0] == 0 && data[1] == 0
						&& data[2] == -2 && data[3] == -1) {
					encoding = "UTF-32BE";
				} else if (data.length > 2 && data[0] == -17 && data[1] == -69
						&& data[2] == -65) {
					encoding = "UTF-8";
				} else if (data.length > 1 && data[0] == -1 && data[1] == -2) {
					encoding = "UTF-16LE";
				} else if (data.length > 1 && data[0] == -2 && data[1] == -1) {
					encoding = "UTF-16BE";
				} else {
					encoding = defaultEncoding;
					if (defaultEncoding == null) {
						if (url == null) {
							encoding = System.getProperty("file.encoding");
						} else if (contentType != null
								&& contentType.startsWith("application/")) {
							encoding = "UTF-8";
						} else {
							encoding = "US-ASCII";
						}
					}
				}
			}

			String strResult1 = new String(data, encoding);
			if (strResult1.length() > 0 && strResult1.charAt(0) == '﻿') {
				strResult1 = strResult1.substring(1);
			}

			result2 = strResult1;
		}

		return result2;
	}
}