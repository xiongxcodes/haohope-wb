package com.wb.controls;

import com.wb.common.KVBuffer;
import com.wb.util.DbUtil;
import com.wb.util.StringUtil;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import org.json.JSONObject;

public class ExtCombo extends ExtControl {
	protected void extendConfig() throws Exception {
		String keyName = this.gs("keyName");
		String queryControl = this.gs("queryControl");
		if (!keyName.isEmpty()) {
			if (this.hasItems) {
				this.headerScript.append(',');
			} else {
				this.hasItems = true;
			}

			this.headerScript.append(getkeyNameScript(keyName));
		}

		if (!queryControl.isEmpty()) {
			if (queryControl.startsWith("app.")) {
				queryControl = queryControl.substring(4);
			}

			if (this.hasItems) {
				this.headerScript.append(',');
			} else {
				this.hasItems = true;
			}

			this.headerScript.append(this.getQueryScript(queryControl));
		}

	}

	public static String getkeyNameScript(String keyName) {
		return StringUtil.concat(new String[]{
				"displayField:\"V\",valueField:\"K\",forceSelection:true,queryMode:\"local\",store:{fields:[\"K\",\"V\"],sorters:\"K\",data:",
				KVBuffer.getList(keyName), "},keyName:\"", keyName, "\""});
	}

	public String getQueryScript(String queryControl) throws Exception {
		ResultSet rs = (ResultSet) this.request.getAttribute(queryControl);
		if (rs == null) {
			throw new IllegalArgumentException("Query \"" + queryControl + "\" is not found.");
		} else {
			ResultSetMetaData meta = rs.getMetaData();
			StringBuilder script = new StringBuilder();
			script.append("queryMode:\"local\",displayField:");
			script.append(StringUtil.quote(DbUtil.getFieldName(meta.getColumnLabel(1))));
			script.append(',');
			if (meta.getColumnCount() > 1) {
				script.append("valueField:");
				script.append(StringUtil.quote(DbUtil.getFieldName(meta.getColumnLabel(2))));
				script.append(',');
			}

			script.append("store:{fields:");
			script.append(DbUtil.getFields(meta, (String[]) null, (JSONObject) null, (JSONObject) null));
			script.append(",data:");
			script.append(DbUtil.getData(rs));
			script.append('}');
			return script.toString();
		}
	}
}