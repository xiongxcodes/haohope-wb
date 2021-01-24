/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package com.wb.fit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class CustomResponse implements HttpServletResponse {
	private HashSet<String> headerSet;
	private HttpServletResponse response;
	private PrintWriter writer;
	private ServletOutputStream sos;
	private ByteArrayOutputStream bos = new ByteArrayOutputStream(8192);
	private boolean submited = false;
	private Locale locale;
	private String contentType;
	private String charEncoding;
	private int statusCode = -1;
	private boolean usingWriter;
	private boolean usingOutputStream;

	public CustomResponse(HttpServletResponse response) {
		this.response = response;
		this.headerSet = new HashSet();
	}

	public byte[] getBytes() {
		if (this.usingWriter) {
			this.writer.flush();
		}

		return this.bos.toByteArray();
	}

	public int getStatusCode() {
		return this.statusCode;
	}

	public void addCookie(Cookie cookie) {
	}

	public void addDateHeader(String name, long date) {
		this.headerSet.add(name);
	}

	public void addHeader(String name, String value) {
		this.headerSet.add(name);
	}

	public void addIntHeader(String name, int value) {
		this.headerSet.add(name);
	}

	public boolean containsHeader(String name) {
		return this.headerSet.contains(name);
	}

	public String encodeRedirectURL(String url) {
		return this.response == null ? url : this.response
				.encodeRedirectURL(url);
	}

	public String encodeRedirectUrl(String url) {
		return this.response == null ? url : this.response
				.encodeRedirectURL(url);
	}

	public String encodeURL(String url) {
		return this.response == null ? url : this.response.encodeURL(url);
	}

	public String encodeUrl(String url) {
		return this.response == null ? url : this.response.encodeURL(url);
	}

	public void sendError(int sc) throws IOException {
		this.statusCode = sc;
		if (this.response != null) {
			this.response.sendError(sc);
		}

	}

	public void sendError(int sc, String msg) throws IOException {
		this.statusCode = sc;
		if (this.response != null) {
			this.response.sendError(sc, msg);
		}

	}

	public void sendRedirect(String location) throws IOException {
	}

	public void setDateHeader(String name, long date) {
		this.headerSet.add(name);
	}

	public void setHeader(String name, String value) {
		this.headerSet.add(name);
	}

	public void setIntHeader(String name, int value) {
		this.headerSet.add(name);
	}

	public void setStatus(int sc) {
		this.statusCode = sc;
		if (this.response != null) {
			this.response.setStatus(sc);
		}

	}

	public void setStatus(int sc, String sm) {
		this.statusCode = sc;
	}

	public void flushBuffer() throws IOException {
		this.submited = true;
	}

	public int getBufferSize() {
		return 8192;
	}

	public String getCharacterEncoding() {
		return this.charEncoding == null && this.response != null
				? this.response.getCharacterEncoding()
				: this.charEncoding;
	}

	public String getContentType() {
		return this.contentType;
	}

	public Locale getLocale() {
		return this.locale;
	}

	public ServletOutputStream getOutputStream() throws IOException {
		if (this.usingWriter) {
			throw new IllegalStateException(
					"getWriter() has already been called for this response");
		} else if (this.sos != null) {
			return this.sos;
		} else {
			this.sos = new ServletOutputStream() {
				public void write(byte[] data, int offset, int length) {
					if (!CustomResponse.this.submited) {
						CustomResponse.this.bos.write(data, offset, length);
					}

				}

				public void write(int b) throws IOException {
					if (!CustomResponse.this.submited) {
						CustomResponse.this.bos.write(b);
					}

				}
			};
			this.usingOutputStream = true;
			return this.sos;
		}
	}

	public PrintWriter getWriter() throws IOException {
		if (this.usingOutputStream) {
			throw new IllegalStateException(
					"getOutputStream() has already been called for this response");
		} else if (this.writer != null) {
			return this.writer;
		} else {
			this.writer = new PrintWriter(new OutputStreamWriter(
					this.getOutputStream(), "utf-8"));
			this.usingWriter = true;
			return this.writer;
		}
	}

	public boolean isCommitted() {
		return this.submited;
	}

	public void reset() {
		this.resetBuffer();
	}

	public void resetBuffer() {
		if (!this.submited) {
			this.bos.reset();
		}

	}

	public void setBufferSize(int size) {
	}

	public void setCharacterEncoding(String charset) {
		this.charEncoding = charset;
	}

	public void setContentLength(int len) {
	}

	public void setContentType(String type) {
		this.contentType = type;
	}

	public void setLocale(Locale loc) {
		this.locale = loc;
	}

	public void setContentLengthLong(long arg0) {
	}

	public String getHeader(String arg0) {
		return null;
	}

	public Collection<String> getHeaderNames() {
		return null;
	}

	public Collection<String> getHeaders(String arg0) {
		return null;
	}

	public int getStatus() {
		return 0;
	}
}