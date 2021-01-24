package com.wb.common;

import com.wb.util.DbUtil;
import com.wb.util.WebUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.IOUtils;

public class Resource {
	public static String getString(HttpServletRequest request, String id, String defaultValue) {
		return getString(WebUtil.getIdWithUser(request, id), defaultValue);
	}

	public static String getString(String id, String defaultValue) {
		try {
			byte[] bytes = getBytes(id, (byte[]) null);
			return bytes == null ? defaultValue : new String(bytes, "utf-8");
		} catch (Throwable var3) {
			throw new RuntimeException(var3);
		}
	}

	public static String getString(String id) {
		return getString(id, (String) null);
	}

	public static byte[] getBytes(HttpServletRequest request, String id, byte[] defaultValue) {
		return getBytes(WebUtil.getIdWithUser(request, id), defaultValue);
	}

	public static byte[] getBytes(String id, byte[] defaultValue) {
		Connection conn = null;
		PreparedStatement st = null;
		ResultSet rs = null;

		try {
			conn = DbUtil.getConnection();
			st = conn.prepareStatement("select RES_CONTENT from WB_RESOURCE where RES_ID=?");
			st.setString(1, id);
			rs = st.executeQuery();
			if (rs.next()) {
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				InputStream is = rs.getBinaryStream(1);
				if (is != null) {
					try {
						IOUtils.copy(is, os);
					} finally {
						is.close();
					}

					byte[] var9 = os.toByteArray();
					return var9;
				}
			}
		} catch (Throwable var17) {
			throw new RuntimeException(var17);
		} finally {
			DbUtil.close(rs);
			DbUtil.close(st);
			DbUtil.close(conn);
		}

		return defaultValue;
	}

	public static void set(HttpServletRequest request, String id, String data) {
		set(WebUtil.getIdWithUser(request, id), data);
	}

	public static void set(String id, String data) {
		try {
			byte[] bytes;
			if (data == null) {
				bytes = null;
			} else {
				bytes = data.getBytes("utf-8");
			}

			set(id, bytes);
		} catch (Throwable var3) {
			throw new RuntimeException(var3);
		}
	}

	public static void remove(String id) {
		set(id, (byte[]) null);
	}

	public static void remove(HttpServletRequest request, String id) {
		set(request, id, (byte[]) null);
	}

	public static void set(String id, byte[] data) {
		Connection conn = null;
		PreparedStatement st = null;
		boolean hasData = data != null;

		try {
			conn = DbUtil.getConnection();
			if (hasData) {
				conn.setAutoCommit(false);
			}

			st = conn.prepareStatement("delete from WB_RESOURCE where RES_ID=?");
			st.setString(1, id);
			st.executeUpdate();
			DbUtil.close(st);
			st = null;
			if (hasData) {
				st = conn.prepareStatement("insert into WB_RESOURCE values(?,?)");
				st.setString(1, id);
				st.setBinaryStream(2, new ByteArrayInputStream(data), data.length);
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

	public static void set(HttpServletRequest request, String id, byte[] data) {
		set(WebUtil.getIdWithUser(request, id), data);
	}
}