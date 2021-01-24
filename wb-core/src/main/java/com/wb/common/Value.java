package com.wb.common;

import com.wb.util.DateUtil;
import com.wb.util.DbUtil;
import com.wb.util.StringUtil;
import com.wb.util.WebUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONObject;

public class Value {
	public static int getInt(String id, int defaultValue) {
		String val = getString(id, (String) null);
		return val == null ? defaultValue : Integer.parseInt(val);
	}

	public static int getInt(HttpServletRequest request, String id, int defaultValue) {
		return getInt(WebUtil.getIdWithUser(request, id), defaultValue);
	}

	public static long getLong(String id, long defaultValue) {
		String val = getString(id, (String) null);
		return val == null ? defaultValue : Long.parseLong(val);
	}

	public static long getLong(HttpServletRequest request, String id, long defaultValue) {
		return getLong(WebUtil.getIdWithUser(request, id), defaultValue);
	}

	public static double getDouble(String id, double defaultValue) {
		String val = getString(id, (String) null);
		return val == null ? defaultValue : Double.parseDouble(val);
	}

	public static double getDouble(HttpServletRequest request, String id, double defaultValue) {
		return getDouble(WebUtil.getIdWithUser(request, id), defaultValue);
	}

	public static boolean getBool(String id, boolean defaultValue) {
		String val = getString(id, (String) null);
		return val == null ? defaultValue : Boolean.parseBoolean(val);
	}

	public static boolean getBool(HttpServletRequest request, String id, boolean defaultValue) {
		return getBool(WebUtil.getIdWithUser(request, id), defaultValue);
	}

	public static Timestamp getDate(String id, Timestamp defaultValue) {
		String val = getString(id, (String) null);
		return val == null ? defaultValue : Timestamp.valueOf(val);
	}

	public static Timestamp getDate(HttpServletRequest request, String id, Timestamp defaultValue) {
		return getDate(WebUtil.getIdWithUser(request, id), defaultValue);
	}

	public static String getString(HttpServletRequest request, String id, String defaultValue) {
		return getString(WebUtil.getIdWithUser(request, id), defaultValue);
	}

	public static String getString(String id, String defaultValue) {
		Connection conn = null;
		PreparedStatement st = null;
		ResultSet rs = null;

		String var7;
		try {
			conn = DbUtil.getConnection();
			st = conn.prepareStatement("select VAL_CONTENT from WB_VALUE where VAL_ID=?");
			st.setString(1, id);
			rs = st.executeQuery();
			if (!rs.next()) {
				return defaultValue;
			}

			var7 = rs.getString(1);
		} catch (Throwable var10) {
			throw new RuntimeException(var10);
		} finally {
			DbUtil.close(rs);
			DbUtil.close(st);
			DbUtil.close(conn);
		}

		return var7;
	}

	public static JSONObject getValues(HttpServletRequest request, String idList) {
		Connection conn = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		String[] list = StringUtil.split(idList, ',');
		String params = StringUtil.repeat("?,", list.length);
		int index = 1;
		JSONObject result = new JSONObject();
		String user = request == null ? null : WebUtil.fetch(request, "sys.user");
		params = params.substring(0, params.length() - 1);

		try {
			conn = DbUtil.getConnection();
			st = conn.prepareStatement("select VAL_ID,VAL_CONTENT from WB_VALUE where VAL_ID in (" + params + ")");
			String[] var14 = list;
			int var13 = list.length;

			for (int var12 = 0; var12 < var13; ++var12) {
				String id = var14[var12];
				if (user != null) {
					id = StringUtil.concat(new String[]{user, "@", id});
				}

				st.setString(index++, id);
			}

			String name;
			for (rs = st.executeQuery(); rs.next(); result.put(name, rs.getString(2))) {
				name = rs.getString(1);
				if (user != null) {
					name = name.substring(name.indexOf(64) + 1);
				}
			}
		} catch (Throwable var18) {
			throw new RuntimeException(var18);
		} finally {
			DbUtil.close(rs);
			DbUtil.close(st);
			DbUtil.close(conn);
		}

		return result;
	}

	public static JSONObject getValues(String idList) {
		return getValues((HttpServletRequest) null, idList);
	}

	public static void set(String id, int value) {
		set(id, Integer.toString(value));
	}

	public static void set(HttpServletRequest request, String id, int value) {
		set(WebUtil.getIdWithUser(request, id), value);
	}

	public static void set(String id, float value) {
		set(id, Float.toString(value));
	}

	public static void set(HttpServletRequest request, String id, float value) {
		set(WebUtil.getIdWithUser(request, id), value);
	}

	public static void set(String id, long value) {
		set(id, Long.toString(value));
	}

	public static void set(HttpServletRequest request, String id, long value) {
		set(WebUtil.getIdWithUser(request, id), value);
	}

	public static void set(String id, double value) {
		set(id, Double.toString(value));
	}

	public static void set(HttpServletRequest request, String id, double value) {
		set(WebUtil.getIdWithUser(request, id), value);
	}

	public static void set(String id, boolean value) {
		set(id, Boolean.toString(value));
	}

	public static void set(HttpServletRequest request, String id, boolean value) {
		set(WebUtil.getIdWithUser(request, id), value);
	}

	public static void set(String id, Date value) {
		String v;
		if (value == null) {
			v = null;
		} else {
			v = DateUtil.dateToStr(value);
		}

		set(id, v);
	}

	public static void set(HttpServletRequest request, String id, Date value) {
		set(WebUtil.getIdWithUser(request, id), value);
	}

	public static void remove(HttpServletRequest request, String id) {
		set(request, id, (String) null);
	}

	public static void remove(String id) {
		set(id, (String) null);
	}

	public static void set(HttpServletRequest request, String id, String value) {
		set(WebUtil.getIdWithUser(request, id), value);
	}

	public static void set(String id, String value) {
		Connection conn = null;
		PreparedStatement st = null;
		boolean hasValue = value != null;

		try {
			conn = DbUtil.getConnection();
			if (hasValue) {
				conn.setAutoCommit(false);
			}

			st = conn.prepareStatement("delete from WB_VALUE where VAL_ID=?");
			st.setString(1, id);
			st.executeUpdate();
			DbUtil.close(st);
			st = null;
			if (hasValue) {
				st = conn.prepareStatement("insert into WB_VALUE values(?,?)");
				st.setString(1, id);
				st.setString(2, value);
				st.executeUpdate();
				conn.commit();
			}
		} catch (Throwable var9) {
			throw new RuntimeException(var9);
		} finally {
			DbUtil.close(st);
			DbUtil.close(conn);
		}

	}
}