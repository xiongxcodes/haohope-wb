package com.wb.controls;

import com.wb.util.WebUtil;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

public class Response extends Control {
	public void create() throws Exception {
		Set<Entry<String, Object>> es = this.configs.entrySet();
		Iterator var4 = es.iterator();

		while (var4.hasNext()) {
			Entry<String, Object> entry = (Entry) var4.next();
			String key = (String) entry.getKey();
			if (!key.equals("itemId")) {
				WebUtil.send(this.response, WebUtil.replaceParams(this.request, (String) entry.getValue()));
				return;
			}
		}

	}
}