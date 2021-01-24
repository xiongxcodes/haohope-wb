package com.wb.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.websocket.CloseReason;
import javax.websocket.Session;
import javax.websocket.CloseReason.CloseCodes;

public class SessionBridge implements HttpSessionBindingListener, Serializable {
	private static final long serialVersionUID = 4199757719833802282L;
	private transient HashMap<String, HashSet<Session>> sessionsMap;

	public void valueBound(HttpSessionBindingEvent arg0) {
	}

	public void valueUnbound(HttpSessionBindingEvent arg0) {
		if (this.sessionsMap != null) {
			this.closeAllSocket();
		}

	}

	private void addSocketSession(Session session, String name) {
		if (this.sessionsMap == null) {
			this.sessionsMap = new HashMap();
		}

		HashSet<Session> sessions = (HashSet) this.sessionsMap.get(name);
		if (sessions == null) {
			sessions = new HashSet();
			this.sessionsMap.put(name, sessions);
		}

		sessions.add(session);
	}

	private void closeAllSocket() {
		try {
			Set<Entry<String, HashSet<Session>>> es = this.sessionsMap.entrySet();
			Iterator var4 = es.iterator();

			while (var4.hasNext()) {
				Entry<String, HashSet<Session>> e = (Entry) var4.next();
				HashSet<Session> list = (HashSet) e.getValue();
				Iterator var6 = list.iterator();

				while (var6.hasNext()) {
					Session sess = (Session) var6.next();

					try {
						sess.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, "Http session timeout"));
					} catch (Throwable var8) {
						;
					}
				}
			}
		} catch (Throwable var9) {
			;
		}

	}

	public static synchronized Session[] getSessions(HttpSession httpSession, String name) {
		SessionBridge sessionBridge = (SessionBridge) httpSession.getAttribute("sysx.socket");
		if (sessionBridge != null && sessionBridge.sessionsMap != null) {
			HashSet<Session> list = (HashSet) sessionBridge.sessionsMap.get(name);
			return list == null ? null : (Session[]) list.toArray(new Session[list.size()]);
		} else {
			return null;
		}
	}

	public static synchronized void addSession(HttpSession httpSession, Session session, String name) {
		SessionBridge sessionBridge = (SessionBridge) httpSession.getAttribute("sysx.socket");
		if (sessionBridge == null) {
			sessionBridge = new SessionBridge();
			httpSession.setAttribute("sysx.socket", sessionBridge);
		}

		sessionBridge.addSocketSession(session, name);
	}

	public static synchronized void removeSession(HttpSession httpSession, Session session, String name) {
		SessionBridge sessionBridge = (SessionBridge) httpSession.getAttribute("sysx.socket");
		if (sessionBridge != null && sessionBridge.sessionsMap != null) {
			HashSet<Session> list = (HashSet) sessionBridge.sessionsMap.get(name);
			if (list != null) {
				list.remove(session);
			}
		}

	}
}