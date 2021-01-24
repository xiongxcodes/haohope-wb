package com.wb.tool;

import com.wb.util.DbUtil;
import com.wb.util.JsonUtil;
import com.wb.util.StringUtil;
import com.wb.util.WebUtil;
import java.sql.Connection;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONArray;
import org.json.JSONObject;

public class Updater {
	public HttpServletRequest request;
	public String jndi;
	public String type;
	public String transaction;
	public boolean batchUpdate;
	public boolean uniqueUpdate = false;
	public String isolation;
	public String tableName;
	public String sqlInsert;
	public String sqlUpdate;
	public String sqlDelete;
	public String paramInsert;
	public String paramUpdate;
	public String paramDelete;
	public String mode;
	public boolean ignoreBlob = false;
	public boolean useExistFields = true;
	public String useFields;
	public String whereFields;
	public JSONObject fieldsMap;

	public void run() throws Exception {
		JSONArray destroyData = null;
		JSONArray createData = null;
		JSONArray updateData = null;
		JSONObject useFieldsObj = null;
		JSONObject whereFieldsObj = null;
		boolean specifyTableName = !StringUtil.isEmpty(this.tableName);
		if (specifyTableName) {
			boolean notHasUseFields = StringUtil.isEmpty(this.useFields);
			if (notHasUseFields) {
				useFieldsObj = null;
			} else {
				useFieldsObj = JsonUtil.fromCSV(this.useFields);
			}

			boolean notHasWhereFields = StringUtil.isEmpty(this.whereFields);
			if (notHasWhereFields) {
				whereFieldsObj = null;
			} else {
				whereFieldsObj = JsonUtil.fromCSV(this.whereFields);
			}

			JSONObject fieldsObj;
			if (StringUtil.isEmpty(this.mode)) {
				Object params = WebUtil.fetchObject(this.request,
						StringUtil.select(new String[]{this.paramDelete, "destroy"}));
				destroyData = JsonUtil.getArray(params);
				if (this.useExistFields && destroyData != null && destroyData.length() > 0) {
					fieldsObj = destroyData.getJSONObject(0);
					if (notHasUseFields) {
						useFieldsObj = JsonUtil.clone(fieldsObj);
					}

					if (notHasWhereFields) {
						whereFieldsObj = JsonUtil.clone(fieldsObj);
					}
				}

				params = WebUtil.fetchObject(this.request, StringUtil.select(new String[]{this.paramInsert, "create"}));
				createData = JsonUtil.getArray(params);
				if (this.useExistFields && createData != null && createData.length() > 0) {
					fieldsObj = createData.getJSONObject(0);
					if (notHasUseFields) {
						if (useFieldsObj == null) {
							useFieldsObj = JsonUtil.clone(fieldsObj);
						} else {
							JsonUtil.apply(useFieldsObj, fieldsObj);
						}
					}

					if (notHasWhereFields) {
						if (whereFieldsObj == null) {
							whereFieldsObj = JsonUtil.clone(fieldsObj);
						} else {
							JsonUtil.apply(whereFieldsObj, fieldsObj);
						}
					}
				}

				params = WebUtil.fetchObject(this.request, StringUtil.select(new String[]{this.paramUpdate, "update"}));
				updateData = JsonUtil.getArray(params);
				if (this.useExistFields && updateData != null && updateData.length() > 0) {
					fieldsObj = updateData.getJSONObject(0);
					if (notHasUseFields) {
						if (useFieldsObj == null) {
							useFieldsObj = JsonUtil.clone(fieldsObj);
						} else {
							JsonUtil.apply(useFieldsObj, fieldsObj);
						}
					}

					if (notHasWhereFields) {
						if (whereFieldsObj == null) {
							whereFieldsObj = JsonUtil.clone(fieldsObj);
						} else {
							JsonUtil.apply(whereFieldsObj, fieldsObj);
						}
					}
				}

				if ((destroyData == null ? 0 : destroyData.length()) + (createData == null ? 0 : createData.length())
						+ (updateData == null ? 0 : updateData.length()) == 0) {
					return;
				}
			} else if (this.useExistFields) {
				fieldsObj = WebUtil.fetch(this.request);
				if (notHasUseFields) {
					useFieldsObj = fieldsObj;
				}

				if (notHasWhereFields) {
					whereFieldsObj = fieldsObj;
				}
			}
		}

		Query query = new Query();
		Connection connection = DbUtil.getConnection(this.request, this.jndi);
		boolean isCommit = "commit".equals(this.transaction);
		if ((isCommit || StringUtil.isEmpty(this.transaction)) && connection.getAutoCommit()) {
			this.transaction = "start";
		}

		if ("start".equals(this.transaction)) {
			DbUtil.startTransaction(connection, this.isolation);
		}

		query.request = this.request;
		query.jndi = this.jndi;
		query.type = this.type;
		query.batchUpdate = this.batchUpdate;
		query.transaction = "none";
		query.uniqueUpdate = this.uniqueUpdate;
		if (specifyTableName) {
			String[] sqls = DbUtil.buildSQLs(this.jndi, this.tableName, this.ignoreBlob, 1, this.request, useFieldsObj,
					whereFieldsObj, this.fieldsMap);
			if (StringUtil.isEmpty(this.sqlInsert)) {
				this.sqlInsert = sqls[0];
			}

			if (StringUtil.isEmpty(this.sqlUpdate)) {
				this.sqlUpdate = sqls[1];
			}

			if (StringUtil.isEmpty(this.sqlDelete)) {
				this.sqlDelete = sqls[2];
			}
		}

		if (StringUtil.isEmpty(this.mode)) {
			if (!StringUtil.isEmpty(this.sqlDelete) && !"-".equals(this.sqlDelete) && destroyData != null
					&& destroyData.length() > 0 && !this.sqlDelete.isEmpty()) {
				query.sql = this.sqlDelete;
				query.arrayData = destroyData;
				query.run();
			}

			if (!StringUtil.isEmpty(this.sqlUpdate) && !"-".equals(this.sqlUpdate) && updateData != null
					&& updateData.length() > 0 && !this.sqlUpdate.isEmpty()) {
				query.sql = this.sqlUpdate;
				query.arrayData = updateData;
				query.run();
			}

			if (!StringUtil.isEmpty(this.sqlInsert) && !"-".equals(this.sqlInsert) && createData != null
					&& createData.length() > 0 && !this.sqlInsert.isEmpty()) {
				query.sql = this.sqlInsert;
				query.arrayData = createData;
				query.run();
			}
		} else {
			if (this.mode.equals("delete")) {
				query.sql = this.sqlDelete;
			} else if (this.mode.equals("update")) {
				query.sql = this.sqlUpdate;
			} else {
				query.sql = this.sqlInsert;
			}

			query.run();
		}

		if (isCommit) {
			connection.commit();
			connection.setAutoCommit(true);
		}

	}
}