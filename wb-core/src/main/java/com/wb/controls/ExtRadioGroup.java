package com.wb.controls;

import com.wb.common.KVBuffer;

public class ExtRadioGroup extends ExtControl {
	protected void extendConfig() throws Exception {
		String keyName = this.gs("keyName");
		if (!keyName.isEmpty()) {
			if (this.hasItems) {
				this.headerScript.append(',');
			} else {
				this.hasItems = true;
			}

			this.headerScript.append("items:");
			this.headerScript.append(KVBuffer.getList(keyName, "indexValue", "boxLabel"));
		}

	}
}