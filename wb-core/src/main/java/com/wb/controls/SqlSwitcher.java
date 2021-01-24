package com.wb.controls;

import com.wb.common.Var;

public class SqlSwitcher extends Control {
	public void create() throws Exception {
		String varName = this.gs("varName");
		String sql = this.gs(Var.getString(varName.isEmpty() ? "sys.db.defaultType" : varName));
		boolean emptySql = sql.isEmpty();
		this.request.setAttribute("sql.autoPage", emptySql);
		if (emptySql) {
			sql = this.gs("default");
		}

		this.request.setAttribute(this.gs("itemId"), sql);
	}
}