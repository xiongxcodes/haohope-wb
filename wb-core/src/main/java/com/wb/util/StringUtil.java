package com.wb.util;

import com.wb.common.Var;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.RoundingMode;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.mail.internet.MimeUtility;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

public class StringUtil {
	public static String repeat(String text, int count) {
		if (count == 0) {
			return "";
		} else {
			StringBuilder buf = new StringBuilder(text.length() * count);

			for (int i = 0; i < count; ++i) {
				buf.append(text);
			}

			return buf.toString();
		}
	}

	public static String[] split(String string, String separator) {
		return split(string, separator, false);
	}

	public static String[] split(String string, String separator, boolean trim) {
		int pos = 0;
		int oldPos = 0;
		int index = 0;
		int separatorLen = separator.length();
		ArrayList<Integer> posData = new ArrayList();
		if (string == null) {
			string = "";
		}

		while ((pos = string.indexOf(separator, pos)) != -1) {
			posData.add(pos);
			pos += separatorLen;
		}

		posData.add(string.length());
		String[] result = new String[posData.size()];

		for (Iterator var10 = posData.iterator(); var10.hasNext(); ++index) {
			int p = (Integer) var10.next();
			if (trim) {
				result[index] = string.substring(oldPos, p).trim();
			} else {
				result[index] = string.substring(oldPos, p);
			}

			oldPos = p + separatorLen;
		}

		return result;
	}

	public static String[] split(String string, char separator) {
		return split(string, separator, false);
	}

	public static String[] split(String string, char separator, boolean trim) {
		int pos = 0;
		int oldPos = 0;
		int index = 0;
		ArrayList<Integer> posData = new ArrayList();
		if (string == null) {
			string = "";
		}

		while ((pos = string.indexOf(separator, pos)) != -1) {
			posData.add(pos);
			++pos;
		}

		posData.add(string.length());
		String[] result = new String[posData.size()];

		for (Iterator var9 = posData.iterator(); var9.hasNext(); ++index) {
			int p = (Integer) var9.next();
			if (trim) {
				result[index] = string.substring(oldPos, p).trim();
			} else {
				result[index] = string.substring(oldPos, p);
			}

			oldPos = p + 1;
		}

		return result;
	}

	public static boolean isSame(String string1, String string2) {
		if (string1 != null) {
			return string1.equalsIgnoreCase(string2);
		} else {
			return string2 != null ? string2.equalsIgnoreCase(string1) : true;
		}
	}

	public static boolean isEqual(String string1, String string2) {
		if (string1 != null) {
			return string1.equals(string2);
		} else {
			return string2 != null ? string2.equals(string1) : true;
		}
	}

	public static String toString(Object object) {
		if (object == null) {
			return "";
		} else {
			return object instanceof Date ? DateUtil.dateToStr((Date) object) : object.toString();
		}
	}

	public static String textareaQuote(String text) {
		return concat("<textarea>", text, "</textarea>");
	}

	public static String toHTMLKey(String text) {
		if (isEmpty(text)) {
			return "";
		} else {
			int j = text.length();
			StringBuilder out = new StringBuilder(text.length());

			for (int i = 0; i < j; ++i) {
				char c = text.charAt(i);
				switch (c) {
					case '"' :
						out.append("&quot;");
						break;
					case '&' :
						out.append("&amp;");
						break;
					case '\'' :
						out.append("&#39;");
						break;
					case '<' :
						out.append("&lt;");
						break;
					case '>' :
						out.append("&gt;");
						break;
					default :
						out.append(c);
				}
			}

			return out.toString();
		}
	}

	public static String toHTML(String text) {
		return toHTML(text, false, true);
	}

	public static String toHTML(String text, boolean nbspAsEmpty, boolean allowNewLine) {
		if (isEmpty(text)) {
			return nbspAsEmpty ? "&nbsp;" : "";
		} else {
			int j = text.length();
			StringBuilder out = new StringBuilder(text.length());

			for (int i = 0; i < j; ++i) {
				char c = text.charAt(i);
				switch (c) {
					case '\t' :
						out.append("&nbsp;&nbsp;&nbsp;&nbsp;");
						break;
					case '\n' :
						if (allowNewLine) {
							out.append("<br>");
						} else {
							out.append("&nbsp;");
						}
					case '\r' :
						break;
					case ' ' :
						if ((i >= j - 1 || text.charAt(i + 1) != ' ') && (i <= 1 || text.charAt(i - 1) != ' ')) {
							out.append(" ");
						} else {
							out.append("&nbsp;");
						}
						break;
					case '"' :
						out.append("&quot;");
						break;
					case '&' :
						out.append("&amp;");
						break;
					case '\'' :
						out.append("&#39;");
						break;
					case '<' :
						out.append("&lt;");
						break;
					case '>' :
						out.append("&gt;");
						break;
					default :
						out.append(c);
				}
			}

			return out.toString();
		}
	}

	public static int indexOf(String[] list, String string) {
		if (list == null) {
			return -1;
		} else {
			int j = list.length;

			for (int i = 0; i < j; ++i) {
				if (list[i].equals(string)) {
					return i;
				}
			}

			return -1;
		}
	}

	public static String convertBool(String value) {
		if ("true".equalsIgnoreCase(value)) {
			return "1";
		} else {
			return "false".equalsIgnoreCase(value) ? "0" : value;
		}
	}

	public static String concat(String... string) {
		int length = 0;
		String[] var5 = string;
		int var4 = string.length;

		for (int var3 = 0; var3 < var4; ++var3) {
			String str = var5[var3];
			length += str.length();
		}

		StringBuilder buf = new StringBuilder(length);
		String[] var6 = string;
		int var9 = string.length;

		for (var4 = 0; var4 < var9; ++var4) {
			String str = var6[var4];
			buf.append(str);
		}

		return buf.toString();
	}

	public static int stringOccur(String source, char dest) {
		return stringOccur(source, dest, 0, source.length())[0];
	}

	public static int[] stringOccur(String source, char dest, int startIndex, int endIndex) {
		int[] result = new int[2];
		int pos = startIndex;

		int newPos;
		int count;
		for (count = 0; (newPos = source.indexOf(dest, pos)) != -1 && newPos <= endIndex; ++count) {
			pos = newPos + 1;
		}

		result[0] = count;
		result[1] = count == 0 ? source.lastIndexOf(dest, endIndex) + 1 : pos;
		return result;
	}

	public static boolean isInteger(String string) {
		if (string == null) {
			return false;
		} else {
			int j = string.length();
			if (j == 0) {
				return false;
			} else {
				for (int i = 0; i < j; ++i) {
					char ch = string.charAt(i);
					if ((ch < '0' || ch > '9') && (i != 0 || ch != '-')) {
						return false;
					}
				}

				return true;
			}
		}
	}

	public static String toLine(String string) {
		int len = string.length();
		if (len == 0) {
			return "";
		} else {
			StringBuilder buffer = new StringBuilder();

			for (int i = 0; i < len; ++i) {
				char c = string.charAt(i);
				switch (c) {
					case '\t' :
					case '\n' :
					case '\r' :
						buffer.append(' ');
						break;
					case '' :
					case '\f' :
					default :
						buffer.append(c);
				}
			}

			return buffer.toString();
		}
	}

	public static String getNamePart(String string) {
		return getNamePart(string, '=');
	}

	public static String getValuePart(String string) {
		return getValuePart(string, '=');
	}

	public static String getNamePart(String string, char separator) {
		if (string == null) {
			return "";
		} else {
			int index = string.indexOf(separator);
			return index == -1 ? string : string.substring(0, index);
		}
	}

	public static String getValuePart(String string, char separator) {
		if (string == null) {
			return "";
		} else {
			int index = string.indexOf(separator);
			return index == -1 ? "" : string.substring(index + 1);
		}
	}

	public static String replaceAll(String string, String oldString, String newString) {
		return innerReplace(string, oldString, newString, true);
	}

	public static String replaceFirst(String string, String oldString, String newString) {
		return innerReplace(string, oldString, newString, false);
	}

	private static String innerReplace(String string, String oldString, String newString, boolean isAll) {
		int index = string.indexOf(oldString);
		if (index == -1) {
			return string;
		} else {
			int start = 0;
			int len = oldString.length();
			if (len == 0) {
				return string;
			} else {
				StringBuilder buffer = new StringBuilder(string.length());

				do {
					buffer.append(string.substring(start, index));
					buffer.append(newString);
					start = index + len;
					if (!isAll) {
						break;
					}

					index = string.indexOf(oldString, start);
				} while (index != -1);

				buffer.append(string.substring(start));
				return buffer.toString();
			}
		}
	}

	public static String getString(InputStream stream) throws IOException {
		return getString(stream, "utf-8");
	}

	public static String getStringA(InputStream stream) throws IOException {
		return getString(stream, "utf-8", false);
	}

	public static String getString(InputStream stream, String charset) throws IOException {
		return getString(stream, charset, true);
	}

	public static String getString(InputStream stream, String charset, boolean closeStream) throws IOException {
		if (stream == null) {
			return null;
		} else {
			String var5;
			try {
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				IOUtils.copy(stream, os);
				if (isEmpty(charset)) {
					var5 = new String(os.toByteArray());
					return var5;
				}

				var5 = new String(os.toByteArray(), charset);
			} finally {
				if (closeStream) {
					stream.close();
				}

			}

			return var5;
		}
	}

	public static String ellipsis(String string, int length) {
		return string.length() > length ? string.substring(0, length - 3) + "..." : string;
	}

	public static String quote(String string) {
		return quote(string, true);
	}

	public static String text(Object value) {
		return quote(value == null ? null : value.toString(), false);
	}

	public static String quote(String string, boolean addQuotes) {
		int len;
		if (string != null && (len = string.length()) != 0) {
			char curChar = 0;
			StringBuilder sb = new StringBuilder(len + 10);
			if (addQuotes) {
				sb.append('"');
			}

			for (int i = 0; i < len; ++i) {
				char lastChar = curChar;
				curChar = string.charAt(i);
				switch (curChar) {
					case '\b' :
						sb.append("\\b");
						continue;
					case '\t' :
						sb.append("\\t");
						continue;
					case '\n' :
						sb.append("\\n");
						continue;
					case '\f' :
						sb.append("\\f");
						continue;
					case '\r' :
						sb.append("\\r");
						continue;
					case '"' :
					case '\\' :
						sb.append('\\');
						sb.append(curChar);
						continue;
					case '/' :
						if (lastChar == '<') {
							sb.append('\\');
						}

						sb.append(curChar);
						continue;
				}

				if (curChar >= ' ' && (curChar < 128 || curChar >= 160) && (curChar < 8192 || curChar >= 8448)) {
					sb.append(curChar);
				} else {
					sb.append("\\u");
					String str = Integer.toHexString(curChar);
					sb.append("0000", 0, 4 - str.length());
					sb.append(str);
				}
			}

			if (addQuotes) {
				sb.append('"');
			}

			return sb.toString();
		} else {
			return addQuotes ? "\"\"" : "";
		}
	}

	public static String opt(String string) {
		return string == null ? "" : string;
	}

	public static String force(String string) {
		return isEmpty(string) ? null : string;
	}

	public static String select(String... string) {
		String[] var4 = string;
		int var3 = string.length;

		for (int var2 = 0; var2 < var3; ++var2) {
			String s = var4[var2];
			if (!isEmpty(s)) {
				return s;
			}
		}

		return "";
	}

	public static boolean getBool(String value) {
		return value != null && !value.equalsIgnoreCase("false") && !value.equals("0") && !value.isEmpty();
	}

	public static boolean isEmpty(String string) {
		return string == null || string.length() == 0;
	}

	public static String encodeBase64(byte[] bytes) throws Exception {
		OutputStream os = null;

		ByteArrayOutputStream data;
		try {
			data = new ByteArrayOutputStream();
			os = MimeUtility.encode(data, "base64");
			os.write(bytes);
		} finally {
			os.close();
		}

		return new String(data.toByteArray());
	}

	public static String encodeBase64(InputStream is) throws Exception {
		OutputStream os = null;
		ByteArrayOutputStream inData = null;
		ByteArrayOutputStream outData = null;

		try {
			inData = new ByteArrayOutputStream();
			outData = new ByteArrayOutputStream();
			IOUtils.copy(is, inData);
			os = MimeUtility.encode(outData, "base64");
			os.write(inData.toByteArray());
		} finally {
			IOUtils.closeQuietly(is);
			IOUtils.closeQuietly(os);
		}

		return new String(outData.toByteArray());
	}

	public static byte[] decodeBase64(String data) throws Exception {
		ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		InputStream base64is = MimeUtility.decode(is, "base64");

		try {
			IOUtils.copy(base64is, os);
		} finally {
			base64is.close();
		}

		return os.toByteArray();
	}

	public static String encode(Object object) throws Exception {
		if (object == null) {
			return "null";
		} else if (object instanceof InputStream) {
			return quote(encodeBase64((InputStream) object));
		} else if (!(object instanceof Number) && !(object instanceof Boolean)) {
			if (!(object instanceof Timestamp) && !(object instanceof java.sql.Date) && !(object instanceof Time)) {
				return object instanceof Date ? quote(DateUtil.dateToStr((Date) object)) : quote(object.toString());
			} else {
				return quote(object.toString());
			}
		} else {
			return object.toString();
		}
	}

	public static String replaceParams(JSONObject jo, String text) {
		if (jo != null && !isEmpty(text)) {
			int start = 0;
			int startPos = text.indexOf("{#", start);
			int endPos = text.indexOf("#}", startPos + 2);
			if (startPos != -1 && endPos != -1) {
				StringBuilder buf;
				for (buf = new StringBuilder(text.length()); startPos != -1
						&& endPos != -1; endPos = text.indexOf("#}", startPos + 2)) {
					String paramName = text.substring(startPos + 2, endPos);
					String paramValue;
					if (paramName.startsWith("Var.")) {
						paramValue = Var.getString(paramName.substring(4));
					} else {
						paramValue = jo.optString(paramName);
					}

					buf.append(text.substring(start, startPos));
					if (paramValue != null) {
						buf.append(paramValue);
					}

					start = endPos + 2;
					startPos = text.indexOf("{#", start);
				}

				buf.append(text.substring(start));
				return buf.toString();
			} else {
				return text;
			}
		} else {
			return text;
		}
	}

	public static void checkArray(String[] strings) {
		String[] var4 = strings;
		int var3 = strings.length;

		for (int var2 = 0; var2 < var3; ++var2) {
			String string = var4[var2];
			if (!checkName(string)) {
				throw new RuntimeException("Invalid param \"" + string + "\"");
			}
		}

	}

	public static boolean checkName(String name) {
		int j = name.length();

		for (int i = 0; i < j; ++i) {
			char c = name.charAt(i);
			if ((c < 'a' || c > 'z') && (c < 'A' || c > 'Z') && c != '_' && (c < '0' || c > '9')) {
				return false;
			}
		}

		return true;
	}

	public static String quoteIf(String name) {
		return checkName(name) ? name : quote(name);
	}

	public static String join(String[] strings, String splitter) {
		StringBuilder buf = new StringBuilder();
		boolean added = false;
		String[] var7 = strings;
		int var6 = strings.length;

		for (int var5 = 0; var5 < var6; ++var5) {
			String s = var7[var5];
			if (!isEmpty(s)) {
				if (added) {
					buf.append(splitter);
				} else {
					added = true;
				}

				buf.append(s);
			}
		}

		return buf.toString();
	}

	public static String joinQuote(String[] strings) {
		return concat("'", join(strings, "','"), "'");
	}

	public static boolean across(String[] source, String[] dest) {
		if (source != null && dest != null) {
			String[] var5 = source;
			int var4 = source.length;

			for (int var3 = 0; var3 < var4; ++var3) {
				String s = var5[var3];
				String[] var9 = dest;
				int var8 = dest.length;

				for (int var7 = 0; var7 < var8; ++var7) {
					String d = var9[var7];
					if (isEqual(s, d)) {
						return true;
					}
				}
			}

			return false;
		} else {
			return false;
		}
	}

	public static String join(List<String> list, String splitter) {
		StringBuilder buf = new StringBuilder();
		boolean added = false;
		Iterator var5 = list.iterator();

		while (var5.hasNext()) {
			String item = (String) var5.next();
			if (!isEmpty(item)) {
				if (added) {
					buf.append(splitter);
				} else {
					added = true;
				}

				buf.append(item);
			}
		}

		return buf.toString();
	}

	public static String substring(String string, int beginIndex, int endIndex) {
		if (string == null) {
			return null;
		} else {
			int len = string.length();
			int i;
			int j;
			if (beginIndex > endIndex) {
				i = endIndex;
				j = beginIndex;
			} else {
				i = beginIndex;
				j = endIndex;
			}

			if (i < 0) {
				i = 0;
			} else if (i > len) {
				i = len;
			}

			if (j < 0) {
				j = 0;
			} else if (j > len) {
				j = len;
			}

			return string.substring(i, j);
		}
	}

	public static String formatNumber(double value, String format) {
		DecimalFormat df = new DecimalFormat(format);
		df.setRoundingMode(RoundingMode.HALF_UP);
		return df.format(value);
	}
}