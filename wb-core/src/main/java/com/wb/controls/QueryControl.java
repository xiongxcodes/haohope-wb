package com.wb.controls;

import com.wb.common.Var;
import com.wb.tool.Query;
import com.wb.util.DbUtil;
import com.wb.util.StringUtil;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

public class QueryControl extends Control {
	public void create() throws Exception {
		if (!this.gb("disabled", false)) {
			String itemId = this.gs("itemId");
			Query query = new Query();
			query.sql = this.gs("sql");
			query.request = this.request;
			query.jndi = this.gs("jndi");
			query.arrayName = this.gs("arrayName");
			query.batchUpdate = this.gb("batchUpdate", Var.batchUpdate);
			query.type = this.gs("type");
			query.errorText = this.gs("errorText");
			query.transaction = this.gs("transaction");
			query.loadParams = this.gs("loadParams");
			query.isolation = this.gs("isolation");
			query.uniqueUpdate = this.gb("uniqueUpdate", false);
			query.returnStatement = this.gb("returnStatement", false);
			Object result = query.run();
			boolean isHashMap = result instanceof HashMap;
			if (!isHashMap) {
				this.request.setAttribute(itemId, result);
			}

			if (result instanceof ResultSet) {
				if (this.gb("loadData", false)) {
					DbUtil.loadFirstRow(this.request, (ResultSet) result, itemId);
				}
			} else if (isHashMap) {
				HashMap<?, ?> map = (HashMap) result;
				Set<?> es = map.entrySet();

				String name;
				Entry entry;
				for (Iterator var10 = es.iterator(); var10.hasNext(); this.request.setAttribute(name,
						entry.getValue())) {
					Object e = var10.next();
					entry = (Entry) e;
					name = (String) entry.getKey();
					if (name.equals("return")) {
						name = itemId;
					} else {
						name = StringUtil.concat(new String[]{itemId, ".", name});
					}
				}
			}

		}
	}
}