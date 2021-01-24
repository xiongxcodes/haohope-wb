package com.wb.controls;

import com.wb.util.StringUtil;

public class Socket extends ScriptControl {
	public void create() throws Exception {
		boolean parentRoot = Boolean.TRUE.equals(this.parentGeneral.opt("root"));
		if (parentRoot) {
			String protocols = this.gs("protocols");
			String itemId = this.gs("itemId");
			this.headerScript.append("if(!app.");
			this.headerScript.append(itemId);
			this.headerScript.append(")\n");
			this.headerScript.append("app.");
			this.headerScript.append(itemId);
			this.headerScript.append('=');
			this.headerScript.append("new WebSocket(\"ws://localhost:8080/webbuilder/s\"");
			if (!protocols.isEmpty()) {
				this.headerScript.append(',');
				if (protocols.startsWith("[")) {
					this.headerScript.append(protocols);
				} else if (protocols.startsWith("@")) {
					this.headerScript.append(protocols.substring(1));
				} else {
					this.headerScript.append(StringUtil.quote(protocols));
				}
			}

			this.headerScript.append(");");
			this.attachEvent(itemId, "onclose", "e");
			this.attachEvent(itemId, "onerror", "e");
			this.attachEvent(itemId, "onmessage", "e");
			this.attachEvent(itemId, "onopen", "e");
		}

	}

	private void attachEvent(String itemId, String eventName, String args) {
		String event = this.ge(eventName);
		if (!event.isEmpty()) {
			this.headerScript.append("\napp.");
			this.headerScript.append(itemId);
			this.headerScript.append('.');
			this.headerScript.append(eventName);
			this.headerScript.append("=function(");
			if (args != null) {
				this.headerScript.append(args);
			}

			this.headerScript.append("){\n");
			this.headerScript.append(event);
			this.headerScript.append("\n};");
		}

	}
}