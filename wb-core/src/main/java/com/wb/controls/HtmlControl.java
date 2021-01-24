package com.wb.controls;

import com.wb.util.StringUtil;
import com.wb.util.WebUtil;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;
import org.json.JSONObject;

public class HtmlControl extends ScriptControl {
	public void create() throws Exception {
		String tag = StringUtil.select(new String[]{this.gs("tagType"), (String) this.generalMeta.opt("type")});
		StringBuilder tagEnd = new StringBuilder(tag.length() + 3);
		String xtype = (String) this.generalMeta.opt("xtype");
		tagEnd.append("</").append(tag).append(">");
		ArrayList<String> classList = new ArrayList();
		ArrayList<String> styleList = new ArrayList();
		this.headerHtml.append('<');
		this.headerHtml.append(tag);
		if (xtype != null) {
			this.headerHtml.append(' ');
			this.headerHtml.append(xtype);
		}

		String value = this.generalMeta.optString("baseClass");
		if (!value.isEmpty()) {
			classList.add(value);
		}

		this.processConfigs(classList, styleList);
		value = this.gs("class");
		if (!value.isEmpty()) {
			classList.add(value);
		}

		value = StringUtil.join(classList, " ");
		if (!value.isEmpty()) {
			this.headerHtml.append(" class=\"");
			this.headerHtml.append(value);
			this.headerHtml.append('"');
		}

		value = this.gs("style");
		if (!value.isEmpty()) {
			styleList.add(value);
		}

		value = StringUtil.join(styleList, ";");
		if (!value.isEmpty()) {
			this.headerHtml.append(" style=\"");
			this.headerHtml.append(value);
			if (!value.endsWith(";")) {
				this.headerHtml.append(';');
			}

			this.headerHtml.append('"');
		}

		this.headerHtml.append('>');
		String glyph = this.gs("glyph");
		if (!glyph.isEmpty()) {
			this.headerHtml.append("<span class=\"wb_glyph\">&#" + Integer.valueOf(glyph, 16) + ";</span> ");
		}

		String html = this.gs("text");
		if (!html.isEmpty()) {
			this.headerHtml.append(html);
		}

		html = this.gs("html");
		if (!html.isEmpty()) {
			this.headerHtml.append(html);
		}

		if (!Boolean.FALSE.equals(this.generalMeta.opt("tagEnd"))) {
			this.footerHtml.insert(0, tagEnd);
		}

	}

	protected void processConfigs(ArrayList<String> classList, ArrayList<String> styleList) {
		Set<Entry<String, Object>> es = this.configs.entrySet();
		boolean hasGroup = classList != null && styleList != null;
		Iterator var14 = es.iterator();

		while (true) {
			while (true) {
				String key;
				String value;
				JSONObject itemObject;
				do {
					do {
						if (!var14.hasNext()) {
							String tagItems = this.gs("tagConfigs");
							if (!tagItems.isEmpty()) {
								this.headerHtml.append(' ');
								this.headerHtml.append(tagItems);
							}

							return;
						}

						Entry<String, Object> entry = (Entry) var14.next();
						key = (String) entry.getKey();
						value = (String) entry.getValue();
						itemObject = (JSONObject) this.configsMeta.opt(key);
					} while (itemObject == null);
				} while (Boolean.TRUE.equals(itemObject.opt("hidden")));

				String rename = (String) itemObject.opt("rename");
				if (rename != null) {
					key = rename;
				}

				if (hasGroup) {
					String group = (String) itemObject.opt("group");
					if (group != null) {
						if ("class".equals(group)) {
							classList.add(value);
						} else {
							styleList.add(StringUtil.concat(new String[]{key, ":", value}));
						}
						continue;
					}
				}

				this.headerHtml.append(' ');
				this.headerHtml.append(key);
				this.headerHtml.append('=');
				char firstChar = value.charAt(0);
				if (firstChar == '@') {
					this.headerHtml.append(WebUtil.replaceParams(this.request, value.substring(1)));
				} else {
					String type = (String) itemObject.opt("type");
					if (type.startsWith("exp")) {
						this.headerHtml.append(WebUtil.replaceParams(this.request, value));
					} else {
						this.headerHtml.append(StringUtil.quote(WebUtil.replaceParams(this.request, value)));
					}
				}
			}
		}
	}
}