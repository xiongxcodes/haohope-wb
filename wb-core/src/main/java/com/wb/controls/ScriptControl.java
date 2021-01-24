package com.wb.controls;

public class ScriptControl extends Control {
	protected StringBuilder headerHtml = new StringBuilder();
	protected StringBuilder footerHtml = new StringBuilder();
	protected StringBuilder headerScript = new StringBuilder();
	protected StringBuilder footerScript = new StringBuilder();

	public String getHeaderHtml() {
		return this.headerHtml.toString();
	}

	public String getFooterHtml() {
		return this.footerHtml.toString();
	}

	public String getHeaderScript() {
		return this.headerScript.toString();
	}

	public String getFooterScript() {
		return this.footerScript.toString();
	}
}