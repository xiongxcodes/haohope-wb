package com.wb.controls;

public class TViewport extends ScriptControl {
	public void create() throws Exception {
		if (this.normalRunType) {
			this.headerScript.append("this.add([");
			this.footerScript.insert(0, "]);");
		} else {
			ExtControl control = new ExtControl();
			control.init(this.request, this.response, this.controlData, this.controlMeta, this.parentGeneral,
					this.lastNode, this.normalRunType);
			control.create();
			this.headerScript.append(control.getHeaderScript());
			this.footerScript.insert(0, control.getFooterScript());
		}

	}
}