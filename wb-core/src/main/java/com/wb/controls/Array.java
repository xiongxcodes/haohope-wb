package com.wb.controls;

public class Array extends ScriptControl {
	public void create() throws Exception {
		boolean parentRoot = Boolean.TRUE.equals(this.parentGeneral.opt("root"));
		if (parentRoot) {
			this.headerScript.append("app.");
			this.headerScript.append(this.gs("itemId"));
			this.headerScript.append("=[");
			this.footerScript.append("];");
		} else {
			if ("Array".equals(this.parentGeneral.opt("type"))) {
				this.headerScript.append("[");
			} else {
				this.headerScript.append(this.gs("itemId"));
				this.headerScript.append(":[");
			}

			if (this.lastNode) {
				this.footerScript.append("]");
			} else {
				this.footerScript.append("],");
			}
		}

	}
}