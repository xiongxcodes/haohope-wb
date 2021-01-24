package com.wb.controls;

import com.wb.common.KVBuffer;

public class TSelect extends ExtControl {
	protected void extendConfig() {
		String keyName = this.gs("keyName");
		if (!keyName.isEmpty()) {
			if (this.hasItems) {
				this.headerScript.append(',');
			} else {
				this.hasItems = true;
			}

			this.headerScript
					.append("displayField:\"V\",valueField:\"K\",store:{fields:[\"K\",\"V\"],sorters:\"K\",data:");
			this.headerScript.append(KVBuffer.getList(keyName));
			this.headerScript.append('}');
		}

	}
}