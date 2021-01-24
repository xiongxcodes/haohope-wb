package com.wb.common;

import com.wb.util.LogUtil;
import java.io.Serializable;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

public class SimpleListener implements HttpSessionBindingListener, Serializable {
	private static final long serialVersionUID = 4490661288665900556L;
	private transient HttpSession session;
	private String userId;
	private String username;
	private String ip;

	public void valueBound(HttpSessionBindingEvent event) {
		this.session = event.getSession();
		this.userId = (String) this.session.getAttribute("sys.user");
		if (Var.log) {
			this.username = (String) this.session.getAttribute("sys.username");
			this.ip = (String) this.session.getAttribute("sys.ip");
			LogUtil.log(this.username, this.ip, 1, "login");
		}

		UserList.add(this.userId, this.session);
	}

	public void valueUnbound(HttpSessionBindingEvent event) {
		if (Var.log) {
			LogUtil.log(this.username, this.ip, 1, "logout");
		}

		UserList.remove(this.userId, this.session);
	}
}