package com.wb.controls;

import com.wb.common.Var;
import com.wb.tool.Updater;
import org.json.JSONObject;

public class UpdaterControl extends Control {
	public void create() throws Exception {
		if (!this.gb("disabled", false)) {
			Updater updater = new Updater();
			updater.request = this.request;
			updater.jndi = this.gs("jndi");
			updater.tableName = this.gs("tableName");
			updater.transaction = this.gs("transaction");
			updater.isolation = this.gs("isolation");
			updater.type = this.gs("type");
			updater.batchUpdate = this.gb("batchUpdate", Var.batchUpdate);
			updater.uniqueUpdate = this.gb("uniqueUpdate", false);
			updater.sqlDelete = this.gs("sqlDelete");
			updater.sqlInsert = this.gs("sqlInsert");
			updater.sqlUpdate = this.gs("sqlUpdate");
			updater.paramDelete = this.gs("paramDelete");
			updater.paramInsert = this.gs("paramInsert");
			updater.paramUpdate = this.gs("paramUpdate");
			updater.useFields = this.gs("useFields");
			updater.whereFields = this.gs("whereFields");
			updater.useExistFields = this.gb("useExistFields", true);
			updater.mode = this.gs("mode");
			String value = this.gs("fieldsMap");
			updater.fieldsMap = value.isEmpty() ? null : new JSONObject(value);
			updater.ignoreBlob = this.gb("ignoreBlob", false);
			updater.run();
		}
	}
}