package com.wb.controls;

import com.wb.common.ScriptBuffer;
import org.json.JSONObject;

public class ServerScript extends Control {
	public void create() throws Exception {
		String script = getScript(this.configs, "script");
		if (!script.isEmpty()) {
			ScriptBuffer.run(this.gs("id"), script, this.request, this.response, this.gs("sourceURL"));
		}

	}

	public static String getScript(JSONObject object, String name) {
		Object value = object.opt(name);
		if (value == null) {
			return "";
		} else {
			String script = (String) value;
			if (script.indexOf("{#") != -1) {
				throw new IllegalArgumentException(
						"ServerScript does not support {#param#} feature, please use app.get(param) instead.");
			} else {
				return script;
			}
		}
	}
}