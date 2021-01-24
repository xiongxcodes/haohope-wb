package com.wb.util;

import com.wb.common.Var;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import javax.servlet.http.HttpServletRequest;

public class LogUtil {
	public static final int INFO = 1;
	public static final int WARN = 2;
	public static final int ERROR = 3;

	private static void record(String userName, String ip, int type, Object object) {
		long milliSec = System.currentTimeMillis();
		Connection conn = null;
		PreparedStatement st = null;

		try {
			conn = DbUtil.getConnection();
			st = conn.prepareStatement("insert into WB_LOG values(?,?,?,?,?)");
			if (StringUtil.isEmpty(ip)) {
				ip = "-";
			}

			if (StringUtil.isEmpty(userName)) {
				userName = "-";
			}

			String text;
			if (object == null) {
				text = "-";
			} else {
				text = object.toString();
				if (StringUtil.isEmpty(text)) {
					text = "-";
				}
			}

			st.setTimestamp(1, new Timestamp(milliSec));
			st.setString(2, userName);
			st.setString(3, ip);
			st.setInt(4, type);

			for (int len = Math.min(text.length(), 256); text.getBytes().length > 255; text = text.substring(0, len)) {
				--len;
			}

			st.setString(5, text);
			st.executeUpdate();
		} catch (Throwable var14) {
			;
		} finally {
			DbUtil.close(st);
			DbUtil.close(conn);
		}

	}

	private static void recordMsg(int type, Object msg) {
		if (Var.log) {
			record((String) null, (String) null, type, msg);
		}

	}

	private static void recordUserMsg(HttpServletRequest request, int type, Object msg) {
		if (Var.log) {
			record(WebUtil.fetch(request, "sys.username"), request.getRemoteAddr(), type, msg);
		}

	}

	public static void log(String userName, String ip, int type, Object msg) {
		if (Var.log) {
			record(userName, ip, type, msg);
		}

	}

	public static void info(HttpServletRequest request, Object msg) {
		recordUserMsg(request, 1, msg);
	}

	public static void info(Object msg) {
		recordMsg(1, msg);
	}

	public static void warn(HttpServletRequest request, Object s) {
		recordUserMsg(request, 2, s);
	}

	public static void warn(Object s) {
		recordMsg(2, s);
	}

	public static void error(HttpServletRequest request, Object s) {
		recordUserMsg(request, 3, s);
	}

	public static void error(Object s) {
		recordMsg(3, s);
	}
}