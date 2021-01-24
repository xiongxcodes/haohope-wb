/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.tools.shell;

import java.util.StringTokenizer;

public final class ParsedContentType {
	private final String contentType;
	private final String encoding;

	public ParsedContentType(String mimeType) {
		String contentType = null;
		String encoding = null;
		if (mimeType != null) {
			StringTokenizer tok = new StringTokenizer(mimeType, ";");
			if (tok.hasMoreTokens()) {
				contentType = tok.nextToken().trim();

				while (tok.hasMoreTokens()) {
					String param = tok.nextToken().trim();
					if (param.startsWith("charset=")) {
						encoding = param.substring(8).trim();
						int l = encoding.length();
						if (l > 0) {
							if (encoding.charAt(0) == 34) {
								encoding = encoding.substring(1);
							}

							if (encoding.charAt(l - 1) == 34) {
								encoding = encoding.substring(0, l - 1);
							}
						}
						break;
					}
				}
			}
		}

		this.contentType = contentType;
		this.encoding = encoding;
	}

	public String getContentType() {
		return this.contentType;
	}

	public String getEncoding() {
		return this.encoding;
	}
}