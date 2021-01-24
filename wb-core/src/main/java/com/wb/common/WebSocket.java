package com.wb.common;

import com.wb.tool.Console;
import com.wb.tool.HttpSessionConfig;
import com.wb.util.SysUtil;
import com.wb.util.WbUtil;
import com.wb.util.WebUtil;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpSession;
import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.server.ServerEndpoint;
import org.json.JSONObject;

@ServerEndpoint(value = "/s", configurator = HttpSessionConfig.class)
public class WebSocket {
	private HttpSession httpSession;
	private String xwl;
	private String name;

	@OnMessage
	public void onMessage(String message, Session session) throws Throwable {
		try {
			JSONObject params = new JSONObject();
			params.put("data", message);
			params.put("xwl", this.xwl);
			params.put("session", session);
			params.put("httpSession", this.httpSession);
			WebUtil.sendSocketText(session, WbUtil.run(this.xwl, params, true), true, true);
		} catch (Throwable var4) {
			this.showException(var4);
			WebUtil.sendSocketText(session, SysUtil.getRootError(var4), false, true);
		}

	}

	@OnOpen
	public void onOpen(Session session, EndpointConfig conf) {
		try {
			Map<String, List<String>> map = session.getRequestParameterMap();
			Object value = conf.getUserProperties().get("WbHttpSession");
			List<String> paramList = (List) map.get("xwl");
			if (paramList != null && paramList.size() > 0) {
				this.xwl = (String) paramList.get(0) + ".xwl";
			} else {
				paramList = (List) map.get("xwls");
				if (paramList != null && paramList.size() > 0) {
					this.xwl = (String) paramList.get(0);
				} else {
					this.xwl = null;
				}
			}

			paramList = (List) map.get("name");
			if (paramList != null && paramList.size() > 0) {
				this.name = (String) paramList.get(0);
			} else {
				this.name = this.xwl;
			}

			if (value instanceof HttpSession) {
				this.httpSession = (HttpSession) value;
			} else {
				this.httpSession = null;
			}

			if (WbUtil.canAccess(this.httpSession, this.xwl)) {
				SessionBridge.addSession(this.httpSession, session, this.name);
				session.getUserProperties().put("xwl", this.xwl);
			} else {
				session.close(new CloseReason(CloseCodes.CANNOT_ACCEPT,
						"You do not have permission to access \"" + this.xwl + "\"."));
			}
		} catch (Throwable var7) {
			Throwable e = var7;

			try {
				session.close(new CloseReason(CloseCodes.CANNOT_ACCEPT, SysUtil.getRootError(e)));
			} catch (Throwable var6) {
				;
			}

			this.showException(var7);
		}

	}

	@OnClose
	public void onClose(Session session, CloseReason reason) {
		if (this.httpSession != null) {
			try {
				SessionBridge.removeSession(this.httpSession, session, this.name);
			} catch (Throwable var5) {
				;
			}

			if (Var.autoLogout && "sys.home".equals(this.name)) {
				try {
					this.httpSession.invalidate();
				} catch (Throwable var4) {
					;
				}
			}

			this.httpSession = null;
		}

	}

	@OnError
	public void onError(Session session, Throwable error) {
		if (Var.showSocketError) {
			this.showException(error);
		}

	}

	private void showException(Throwable exception) {
		StringWriter writer = new StringWriter();
		PrintWriter pwriter = new PrintWriter(writer, true);
		exception.printStackTrace(pwriter);
		pwriter.close();
		String errorMessage = writer.toString();
		Console.printToClient(this.httpSession, errorMessage, "error", false);
		if (Var.printError) {
			System.err.println(errorMessage);
		}

	}
}