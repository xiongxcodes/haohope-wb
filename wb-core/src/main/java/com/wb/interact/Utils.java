package com.wb.interact;

import com.wb.util.DbUtil;
import com.wb.util.StringUtil;
import com.wb.util.WebUtil;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

public class Utils {
	public static void getSQLDefines(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String sql = request.getParameter("sql");
		String type = request.getParameter("type");
		String jndi = request.getParameter("jndi");
		String table = request.getParameter("table");
		String prefix = request.getParameter("prefix");
		String[] sqlTypes = new String[]{"insert", "update", "delete", "select"};
		String[] types = new String[]{"native", "params", "replace"};
		String[] sqls = DbUtil.buildSQLs(jndi, table, false, StringUtil.indexOf(types, type), (HttpServletRequest) null,
				(JSONObject) null, (JSONObject) null, (JSONObject) null);
		boolean isSelect = "select".equals(sql);
		sql = sqls[StringUtil.indexOf(sqlTypes, sql)];
		if (!StringUtil.isEmpty(prefix) && isSelect) {
			sql = StringUtil.concat(new String[]{"select ", prefix, ".",
					StringUtil.replaceAll(sql.substring(7), ",", "," + prefix + ".")});
		}

		WebUtil.send(response, sql);
	}
}