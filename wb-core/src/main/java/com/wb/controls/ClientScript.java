package com.wb.controls;

public class ClientScript extends ScriptControl {
	public void create() throws Exception {
		this.headerHtml.append(this.gs("headerHtml"));
		this.footerHtml.insert(0, this.gs("footerHtml"));
		this.headerScript.append(this.gs("headerScript"));
		this.footerScript.insert(0, this.gs("footerScript"));
	}
}