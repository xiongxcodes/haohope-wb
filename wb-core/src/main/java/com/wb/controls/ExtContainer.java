package com.wb.controls;

import com.wb.common.Base;
import com.wb.tool.ExcelForm;
import com.wb.util.StringUtil;
import com.wb.util.WebUtil;
import java.io.File;

public class ExtContainer extends ExtControl {
	protected void extendConfig() throws Exception {
		String excelForm = this.gs("excelForm");
		String excelFormAlign = this.gs("excelFormAlign");
		if (!excelForm.isEmpty()) {
			if (this.hasItems) {
				this.headerScript.append(',');
			} else {
				this.hasItems = true;
			}

			if (this.gs("autoScroll").isEmpty()) {
				this.headerScript.append("autoScroll:true,createObject:true,");
			} else {
				this.headerScript.append("createObject:true,");
			}

			this.headerScript.append("html:");
			int sheetIndex;
			if (excelForm.indexOf(124) == -1) {
				sheetIndex = 0;
			} else {
				sheetIndex = Integer.parseInt(StringUtil.getValuePart(excelForm, '|'));
				excelForm = StringUtil.getNamePart(excelForm, '|');
			}

			File file = new File(Base.path, excelForm);
			this.headerScript.append(
					StringUtil.quote(ExcelForm.getHtml(WebUtil.fetch(this.request), file, sheetIndex, excelFormAlign)));
		}

	}
}