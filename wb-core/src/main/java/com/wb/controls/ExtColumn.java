package com.wb.controls;

import com.wb.common.KVBuffer;

public class ExtColumn extends ExtControl {
	protected void extendConfig() {
		String keyName = this.gs("keyName");
		if (!keyName.isEmpty()) {
			if (this.hasItems) {
				this.headerScript.append(',');
			} else {
				this.hasItems = true;
			}

			this.headerScript.append("renderer:Wb.kvRenderer,keyItems:");
			this.headerScript.append(KVBuffer.getList(keyName));
		}

	}
}