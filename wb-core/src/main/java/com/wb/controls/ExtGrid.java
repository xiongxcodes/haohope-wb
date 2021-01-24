package com.wb.controls;

import com.wb.common.Resource;
import com.wb.util.StringUtil;
import com.wb.util.WbUtil;

public class ExtGrid extends ExtControl {
	protected void extendConfig() throws Exception {
		String syncStateId = this.gs("syncStateId");
		String allowExportUrl = this.gs("allowExportUrl");
		if (!allowExportUrl.isEmpty() && !WbUtil.canAccess(this.request, allowExportUrl)) {
			if (this.hasItems) {
				this.headerScript.append(',');
			} else {
				this.hasItems = true;
			}

			this.headerScript.append("hideExportBtns:true");
		}

		if (!syncStateId.isEmpty()) {
			String headersData = this.getHeadersData(syncStateId);
			if (!StringUtil.isEmpty(headersData)) {
				if (this.hasItems) {
					this.headerScript.append(',');
				} else {
					this.hasItems = true;
				}

				this.headerScript.append("syncHeadersData:");
				this.headerScript.append(headersData);
			}
		}

	}

	public String getHeadersData(String syncStateId) {
		return Resource.getString(this.request, "gridState#" + syncStateId, (String) null);
	}
}