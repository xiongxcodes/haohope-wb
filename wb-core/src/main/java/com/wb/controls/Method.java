package com.wb.controls;

import com.wb.util.SysUtil;

public class Method extends Control {
	public void create() throws Exception {
		String method = this.gs("method");
		SysUtil.executeMethod(method, this.request, this.response);
	}
}