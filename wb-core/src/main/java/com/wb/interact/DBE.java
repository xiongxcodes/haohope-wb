package com.wb.interact;

import com.wb.common.Base;
import com.wb.util.DbUtil;
import com.wb.util.JsonUtil;
import com.wb.util.SortUtil;
import com.wb.util.StringUtil;
import com.wb.util.SysUtil;
import com.wb.util.WebUtil;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;

public class DBE {
	public static void getTree(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String type = request.getParameter("type");
		String jndi = request.getParameter("jndi");
		String schema = request.getParameter("schema");
		String result;
		if ("db".equals(type)) {
			result = getSchemaList(jndi, (HashSet) null);
			if (result == null) {
				result = getTableList(jndi, (String) null, (HashSet) null);
			}
		} else if ("schema".equals(type)) {
			result = getTableList(jndi, schema, (HashSet) null);
		} else {
			result = getDbList();
		}

		WebUtil.send(response, result);
	}

	public static void checkSelectSql(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String sql = request.getParameter("sql").toUpperCase();
		String[] roles = (String[]) WebUtil.fetchObject(request, "sys.roles");
		if (StringUtil.indexOf(roles, "demo") != -1 && StringUtil.indexOf(roles, "admin") == -1
				&& (!sql.startsWith("SELECT * FROM ")
						|| !StringUtil.checkName(StringUtil.replaceAll(sql.substring(14), ".", "a"))
						|| sql.endsWith("WB_USER") || sql.endsWith("WB_SN") || sql.endsWith("WB_SYS1")
						|| sql.endsWith("WB_SYS2"))) {
			SysUtil.accessDenied(request);
		}

	}

	public static String getDbList() throws Exception {
		JSONObject config = JsonUtil.readObject(new File(Base.path, "wb/system/var.json"));
		HashMap<String, Object> map = new HashMap();
		config = config.optJSONObject("sys").optJSONObject("jndi");
		Set<Entry<String, Object>> es = config.entrySet();
		JSONArray ja = new JSONArray();
		config.remove("default");
		Iterator var8 = es.iterator();

		Entry e;
		while (var8.hasNext()) {
			e = (Entry) var8.next();
			String key = (String) e.getKey();
			map.put(key, ((JSONArray) e.getValue()).optString(0));
		}

		ArrayList<Entry<String, Object>> sortedItems = SortUtil.sortKey(map);
		JSONObject jo = new JSONObject();
		jo.put("text", "default");
		jo.put("jndi", "default");
		jo.put("type", "db");
		jo.put("iconCls", "db_icon");
		ja.put(jo);
		var8 = sortedItems.iterator();

		while (var8.hasNext()) {
			e = (Entry) var8.next();
			jo = new JSONObject();
			jo.put("text", e.getKey());
			jo.put("jndi", e.getKey());
			jo.put("type", "db");
			jo.put("iconCls", "db_icon");
			ja.put(jo);
		}

		return ja.toString();
	}

	public static String getTableList(String jndi, String schema, HashSet<String> tables) throws Exception {
		Connection conn = null;
		ResultSet rs = null;
		boolean isFirst = true;
		boolean hasTableDefine = tables != null;
		String[] types = new String[]{"TABLE"};
		String jndiText = StringUtil.quote(jndi);
		StringBuilder buf = new StringBuilder();
		HashMap tableMap = new HashMap();

		try {
			conn = DbUtil.getConnection(jndi);
			rs = conn.getMetaData().getTables((String) null, schema, (String) null, types);

			String tableSchema;
			String tableName;
			while (rs.next()) {
				tableSchema = StringUtil.opt(rs.getString(2));
				tableName = rs.getString(3);
				tableMap.put(tableName, tableSchema);
			}

			ArrayList<Entry<String, String>> sortedEntries = SortUtil.sortKey(tableMap);
			buf.append('[');
			Iterator var17 = sortedEntries.iterator();

			while (true) {
				while (var17.hasNext()) {
					Entry<String, String> entry = (Entry) var17.next();
					if (isFirst) {
						isFirst = false;
					} else {
						buf.append(',');
					}

					tableName = (String) entry.getKey();
					String tableText = StringUtil.quote(tableName);
					tableSchema = StringUtil.quote((String) entry.getValue());
					buf.append("{\"text\":");
					buf.append(tableText);
					buf.append(",\"type\":\"table\",\"table\":");
					buf.append(tableText);
					buf.append(",\"schema\":");
					buf.append(tableSchema);
					buf.append(",\"jndi\":");
					buf.append(jndiText);
					buf.append(",\"leaf\":true,\"iconCls\":\"");
					String upperTableName = tableName.toUpperCase();
					if (hasTableDefine && tables.contains(upperTableName)) {
						tables.remove(upperTableName);
						buf.append("table_add_icon\"}");
					} else {
						buf.append("table_icon\"}");
					}
				}

				if (hasTableDefine && schema == null) {
					var17 = tables.iterator();

					while (var17.hasNext()) {
						String fullName = (String) var17.next();
						fullName = StringUtil.quote(fullName);
						buf.append(",{\"text\":");
						buf.append(fullName);
						buf.append(",\"type\":\"table\",\"table\":");
						buf.append(fullName);
						buf.append(",\"schema\":\"\",\"jndi\":");
						buf.append(jndiText);
						buf.append(",\"leaf\":true,\"iconCls\":\"table_add_icon\"}");
					}
				}

				buf.append(']');
				String var19 = buf.toString();
				return var19;
			}
		} finally {
			DbUtil.close(rs);
			DbUtil.close(conn);
		}
	}

	public static String getSchemaList(String jndi, HashSet<String> tables) throws Exception {
		Connection conn = null;
		ResultSet rs = null;
		String[] types = new String[]{"TABLE"};
		String jndiText = StringUtil.quote(jndi);
		StringBuilder buf = new StringBuilder();
		HashMap<String, Boolean> schemaMap = new HashMap();
		boolean isFirst = true;
		boolean hasTableDefine = tables != null;

		try {
			conn = DbUtil.getConnection(jndi);
			rs = conn.getMetaData().getTables((String) null, (String) null, (String) null, types);

			String schema;
			while (rs.next()) {
				schema = StringUtil.opt(rs.getString(2));
				String tableName = rs.getString(3);
				String upperTableName = tableName.toUpperCase();
				schemaMap.put(schema, true);
				if (hasTableDefine && tables.contains(upperTableName)) {
					tables.remove(upperTableName);
				}
			}

			if (!schemaMap.isEmpty() && (schemaMap.size() != 1 || !schemaMap.containsKey(""))) {
				ArrayList<Entry<String, Boolean>> entryList = SortUtil.sortKey(schemaMap);
				buf.append('[');
				Iterator var15 = entryList.iterator();

				while (var15.hasNext()) {
					Entry<String, Boolean> entry = (Entry) var15.next();
					schema = StringUtil.quote((String) entry.getKey());
					if (isFirst) {
						isFirst = false;
					} else {
						buf.append(',');
					}

					buf.append("{\"text\":");
					buf.append(schema);
					buf.append(",\"jndi\":");
					buf.append(jndiText);
					buf.append(",\"schema\":");
					buf.append(schema);
					buf.append(",\"type\":\"schema\",\"iconCls\":\"db_form_icon\"}");
				}

				if (hasTableDefine) {
					var15 = tables.iterator();

					while (var15.hasNext()) {
						String fullName = (String) var15.next();
						fullName = StringUtil.quote(fullName);
						buf.append(",{\"text\":");
						buf.append(fullName);
						buf.append(",\"type\":\"table\",\"table\":");
						buf.append(fullName);
						buf.append(",\"schema\":\"\",\"jndi\":");
						buf.append(jndiText);
						buf.append(",\"leaf\":true,\"iconCls\":\"table_add_icon\"}");
					}
				}

				buf.append(']');
				String var17 = buf.toString();
				return var17;
			}
		} finally {
			DbUtil.close(rs);
			DbUtil.close(conn);
		}

		return null;
	}

	public static void downloadBlob(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String jndi = request.getParameter("__jndi");
		String tableName = request.getParameter("__tableName");
		String fieldName = request.getParameter("__fieldName");
		String selectSql = DbUtil.buildSQLs(jndi, tableName, false, 1, (HttpServletRequest) null,
				(new JSONObject()).put(fieldName, 1), (JSONObject) null, (JSONObject) null)[3];
		ResultSet rs = (ResultSet) DbUtil.run(request, selectSql, jndi);
		DbUtil.outputBlob(rs, request, response, "download");
	}

	public static void uploadBlob(HttpServletRequest request, HttpServletResponse response) throws Exception {
		setBlob(request, false);
	}

	public static void clearBlob(HttpServletRequest request, HttpServletResponse response) throws Exception {
		setBlob(request, true);
	}

	private static void setBlob(HttpServletRequest request, boolean isClear) throws Exception {
		String jndi = WebUtil.fetch(request, "__jndi");
		String tableName = WebUtil.fetch(request, "__tableName");
		String fieldName = WebUtil.fetch(request, "__fieldName");
		if (isClear) {
			request.setAttribute(fieldName, "");
		} else {
			request.setAttribute(fieldName, request.getAttribute("file"));
		}

		String updateSql = DbUtil.buildSQLs(jndi, tableName, false, 1, (HttpServletRequest) null,
				(new JSONObject()).put(fieldName, 1), (JSONObject) null, (JSONObject) null)[1];
		DbUtil.run(request, updateSql, jndi);
	}
}