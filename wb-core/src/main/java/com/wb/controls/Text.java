package com.wb.controls;

import com.wb.util.StringUtil;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

public class Text extends Control {
	public void create() throws Exception {
		Set<Entry<String, Object>> es = this.configs.entrySet();
		Iterator var5 = es.iterator();

		String key;
		do {
			if (!var5.hasNext()) {
				return;
			}

			Entry<String, Object> entry = (Entry) var5.next();
			key = (String) entry.getKey();
		} while (key.equals("itemId") || key.equals("quoted"));

		String text = this.gs(key);
		if (this.gb("quoted")) {
			text = StringUtil.text(text);
		}

		this.request.setAttribute(this.gs("itemId"), text);
	}
}