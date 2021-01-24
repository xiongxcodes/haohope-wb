package com.wb.tool;

import com.wb.common.Var;
import com.wb.util.StringUtil;
import com.wb.util.WbUtil;
import com.wb.util.WebUtil;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class Console {
	public static void log(HttpServletRequest request, Object object) {
		print(request, object, "log");
	}

	public static void debug(HttpServletRequest request, Object object) {
		print(request, object, "debug");
	}

	public static void info(HttpServletRequest request, Object object) {
		print(request, object, "info");
	}

	public static void warn(HttpServletRequest request, Object object) {
		print(request, object, "warn");
	}

	public static void error(HttpServletRequest request, Object object) {
		print(request, object, "error");
	}

	public static void print(HttpServletRequest request, Object object, String type, boolean encoded) {
		if (Var.serverConsolePrint) {
			System.out.println(object);
		}

		printToClient(request, object, type, encoded);
	}

	public static void printToClient(HttpServletRequest request, Object object, String type, boolean encoded) {
		if (Var.consolePrint || Var.homeConsolePrint) {
			printToClient(request.getSession(false), object, type, encoded);
		}

	}

	public static void printToClient(HttpSession httpSession, Object object, String type, boolean encoded) {
		if ((Var.consolePrint || Var.homeConsolePrint) && httpSession != null) {
			try {
				String text = encodeObject(object, type, encoded);
				if (Var.consolePrint && WbUtil.canAccess(httpSession, "ide")) {
					WebUtil.send(httpSession, "ide.console", text);
				}

				if (Var.homeConsolePrint) {
					WebUtil.send(httpSession, "sys.home", text);
				}
			} catch (Throwable var5) {
				;
			}

		}
	}

	private static String encodeObject(Object object, String type, boolean encoded) {
		String string;
		if (object == null) {
			string = "null";
		} else {
			string = object.toString();
		}

		StringBuilder buf = new StringBuilder(string.length() + 30);
		buf.append("{\"cate\":\"console\",\"type\":\"");
		buf.append(type);
		buf.append("\",\"msg\":");
		buf.append(StringUtil.quote(string));
		if (encoded) {
			buf.append(",\"encode\":true");
		}

		buf.append('}');
		return buf.toString();
	}

	public static void print(HttpServletRequest request, Object object, String type) {
		print(request, object, type, false);
	}

	public static void broadcast(Object object) throws Exception {
		broadcast(object, "info");
	}

	public static void broadcast(Object object, String type) throws Exception {
		if (Var.consolePrint) {
			WebUtil.sendWithUrl("ide.console", "ide", encodeObject(object, type, false));
		}

	}

	public static void print(String userId, Object object, String type) throws Exception {
		if (Var.consolePrint) {
			WebUtil.send(userId, "ide.console", encodeObject(object, type, false));
		}

	}

	public static void print(String userId, Object object) throws Exception {
		print(userId, object, "info");
	}
}