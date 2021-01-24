package com.wb.controls;

import com.wb.util.WbUtil;

public class ExtActionUI extends ExtControl {
	protected void extendConfig() throws Exception {
		String bindModule = this.gs("bindModule");
		if (!bindModule.isEmpty() && !WbUtil.canAccess(this.request, bindModule)) {
			if (this.hasItems) {
				this.headerScript.append(',');
			} else {
				this.hasItems = true;
			}

			this.headerScript.append("hidden:true");
		}

	}
}