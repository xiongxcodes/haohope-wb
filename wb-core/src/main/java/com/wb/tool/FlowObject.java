package com.wb.tool;

import com.wb.common.ScriptBuffer;
import com.wb.common.Str;
import com.wb.util.DateUtil;
import com.wb.util.DbUtil;
import com.wb.util.JsonUtil;
import com.wb.util.StringUtil;
import com.wb.util.SysUtil;
import com.wb.util.WbUtil;
import com.wb.util.WebUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.json.JSONArray;
import org.json.JSONObject;

public class FlowObject {
	private static final String startName = "开始";
	private static final String endName = "结束";
	public JSONObject flowJson;
	private JSONObject flowProp;
	private JSONObject params;
	private String activeNodeName;
	private JSONObject activeNode;
	private JSONArray history;
	private HttpServletRequest request;
	private String userId;
	private String userName;
	private String userDispName;
	private String flowId;
	private JSONArray nodes;
	private JSONArray links;
	private Timestamp actionDate = DateUtil.now();
	private boolean nodeChanged;
	private boolean setProcessed = true;
	private String startFlowId;
	private ArrayList<String> newUserList = new ArrayList();

	public FlowObject(JSONObject flowJson, JSONObject extraParams, HttpServletRequest request, String flowName,
			boolean isStart, String startFlowId) {
		this.flowJson = flowJson;
		this.startFlowId = startFlowId;
		this.flowProp = flowJson.getJSONObject("flow");
		if (isStart && StringUtil.isEmpty(this.flowProp.optString("flowName")) && flowName != null) {
			this.flowProp.put("flowName", flowName);
		}

		this.nodes = flowJson.getJSONArray("nodes");
		this.links = flowJson.getJSONArray("links");
		this.history = flowJson.optJSONArray("history");
		if (this.history == null) {
			this.history = new JSONArray();
			flowJson.put("history", this.history);
		}

		this.params = flowJson.optJSONObject("params");
		if (this.params == null) {
			this.params = new JSONObject();
			flowJson.put("params", this.params);
		}

		if (extraParams != null) {
			this.applyExtraParams(extraParams);
		}

		this.activeNodeName = flowJson.optString("activeNode", "开始");
		this.activeNode = this.getNode(this.activeNodeName);
		this.request = request;
		this.setUserVar();
		if (isStart) {
			this.verifyStartNode();
			this.setStartParams();
		}

		this.params.put("flow.lastDate", this.actionDate);
		this.params.put("flow.lastUserId", this.userId);
		this.params.put("flow.lastUserName", this.userName);
		this.params.put("flow.lastUserDispName", this.userDispName);
		this.params.put("flow.lastNodeName", this.activeNodeName);
		this.params.put("flow.lastNodeTitle", this.getTitle(this.activeNode));
		this.flowId = this.params.getString("flow.id");
	}

	private void applyExtraParams(JSONObject extraParams) {
		Set<Entry<String, Object>> es = extraParams.entrySet();
		Iterator var5 = es.iterator();

		while (var5.hasNext()) {
			Entry<String, Object> e = (Entry) var5.next();
			String key = (String) e.getKey();
			if (key.indexOf(46) != -1) {
				throw new IllegalArgumentException("Invalid parameter name \"" + key + "\".");
			}

			this.params.put(key, e.getValue());
		}

	}

	private void verifyStartNode() {
		JSONObject startNode = this.getNode("开始");
		if (!StringUtil.isEmpty(startNode.optString("doUser"))) {
			throw new IllegalArgumentException("开始节点不能指定处理人员。");
		} else if (!StringUtil.isEmpty(startNode.optString("passUsers"))) {
			throw new IllegalArgumentException("开始节点不能指定通过人数。");
		} else {
			JSONObject doUser = new JSONObject();
			doUser.put("userId", (new JSONArray()).put(this.userId));
			startNode.put("doUser", doUser);
		}
	}

	private void setStartParams() {
		this.params.put("flow.id", this.startFlowId);
		this.params.put("flow.nodeName", this.activeNodeName);
		this.params.put("flow.nodeTitle", this.getTitle(this.activeNode));
		this.params.put("flow.startDate", this.actionDate);
		this.params.put("flow.userId", this.userId);
		this.params.put("flow.userName", this.userName);
		this.params.put("flow.userDispName", this.userDispName);
		this.params.put("flow.flowName", this.getFlowAttribute("flowName"));
		this.params.put("flow.action", "pass");
		int j = this.nodes.length();

		for (int i = 0; i < j; ++i) {
			JSONObject node = this.nodes.getJSONObject(i);
			node.put("doUserCount", 0);
		}

		this.flowJson.put("rawLinks", this.links.toString());
	}

	private void setUserVar() {
		if (this.request == null) {
			this.userId = "-";
			this.userName = "-";
			this.userDispName = "-";
		} else {
			HttpSession session = this.request.getSession(false);
			if (session == null) {
				return;
			}

			this.userId = (String) session.getAttribute("sys.user");
			this.userName = (String) session.getAttribute("sys.username");
			this.userDispName = (String) session.getAttribute("sys.dispname");
		}

	}

	public JSONObject getParams() {
		return this.params;
	}

	public void forward() throws Exception {
		this.checkEnd();
		String passUsers = this.getAttribute(this.activeNode, "passUsers");
		int doUserCount = this.activeNode.getInt("doUserCount");
		this.recordHistory("pass");
		++doUserCount;
		this.activeNode.put("doUserCount", doUserCount);
		if (!StringUtil.isEmpty(passUsers)) {
			boolean isPercent = passUsers.endsWith("%");
			String passValue;
			if (isPercent) {
				passValue = passUsers.substring(0, passUsers.length() - 1);
			} else {
				passValue = passUsers;
			}

			if (!StringUtil.isInteger(passValue)) {
				throw new RuntimeException("节点“" + this.activeNode + "”的通过人数属性值“" + passUsers + "”无效。");
			}

			int countValue = Integer.parseInt(passValue);
			if (isPercent) {
				int totalUserCount = this.activeNode.getInt("totalUserCount");
				if ((double) doUserCount * 100.0D / (double) totalUserCount < (double) countValue) {
					return;
				}
			} else if (doUserCount < countValue) {
				return;
			}

			this.activeNode.put("doUserCount", 0);
		}

		this.doForward(new HashMap());
	}

	private void doForward(HashMap<String, Integer> forwardNodes) throws Exception {
		int j = this.links.length();
		boolean forwarded = false;
		boolean hasDefaultDialog = !StringUtil.isEmpty(this.getFlowAttribute("defaultDialog"));
		String fromNodeName = this.activeNodeName;

		for (int i = 0; i < j; ++i) {
			JSONObject link = this.links.getJSONObject(i);
			if (fromNodeName.equals(link.optString("from"))) {
				String condition = link.optString("condition");
				if (StringUtil.isEmpty(condition) || ScriptBuffer.evalCondition(condition, this.params)) {
					if (forwarded) {
						throw new RuntimeException("节点 “" + fromNodeName + "”可以流转到多个节点。");
					}

					String toNodeName = link.optString("to");
					this.setActiveNode(toNodeName);
					forwarded = true;
					String dialog = this.activeNode.optString("dialog");
					if (("-".equals(dialog) || !hasDefaultDialog && StringUtil.isEmpty(dialog))
							&& !"结束".endsWith(toNodeName)) {
						Integer forwardTimes = (Integer) forwardNodes.get(toNodeName);
						if (forwardTimes == null) {
							forwardTimes = 0;
						}

						forwardTimes = forwardTimes + 1;
						if (forwardTimes > 10) {
							throw new RuntimeException(
									"节点“" + fromNodeName + "”到“" + toNodeName + "”的流转已经多次，请检查是否存在无限循环。");
						}

						forwardNodes.put(toNodeName, forwardTimes);
						this.doForward(forwardNodes);
					}
				}
			}
		}

		if (!forwarded) {
			throw new RuntimeException("没有找到节点 “" + fromNodeName + "”的下一个节点。");
		}
	}

	private void setActiveNode(String nodeName) {
		if (!nodeName.equals(this.activeNodeName)) {
			this.activeNode = this.getNode(nodeName);
			this.activeNodeName = nodeName;
			this.flowJson.put("activeNode", nodeName);
			this.params.put("flow.nodeName", nodeName);
			this.params.put("flow.nodeTitle", this.getTitle(this.activeNode));
			this.nodeChanged = true;
		}
	}

	private HashSet<String> getUsers(Connection conn, boolean isCc) throws Exception {
		String users = this.replaceParams(this.activeNode.optString(isCc ? "ccUser" : "doUser"));
		HashSet<String> hs = new HashSet();
		String errorMsg = "节点“" + this.activeNodeName + "”无有效处理人员";
		if (StringUtil.isEmpty(users)) {
			if (!isCc && !"结束".equals(this.activeNodeName)) {
				throw new IllegalArgumentException(errorMsg);
			} else {
				return null;
			}
		} else {
			JSONObject userData = new JSONObject(users);
			JSONArray ja = userData.optJSONArray("userId");
			if (ja != null) {
				JsonUtil.addAll(hs, ja);
			}

			ja = userData.optJSONArray("roleId");
			int i;
			int j;
			if (ja != null) {
				j = ja.length();

				for (i = 0; i < j; ++i) {
					this.addUsers(conn, hs, "select distinct USER_ID from WB_USER_ROLE where ROLE_ID in ("
							+ StringUtil.joinQuote(JsonUtil.createArray(ja)) + ")");
				}
			}

			ja = userData.optJSONArray("deptId");
			if (ja != null) {
				j = ja.length();

				for (i = 0; i < j; ++i) {
					this.addUsers(conn, hs, "select distinct USER_ID from WB_PERSON where DEPT_ID in ("
							+ StringUtil.joinQuote(JsonUtil.createArray(ja)) + ")");
				}
			}

			String module = userData.optString("module");
			if (!StringUtil.isEmpty(module)) {
				JSONObject allParams = this.params;
				if (module.endsWith("}")) {
					int pos = module.lastIndexOf(123);
					String extraParam = module.substring(pos);
					module = module.substring(0, pos);
					allParams = JsonUtil.applyIf(new JSONObject(extraParam), this.params);
				}

				String respText;
				if (this.request == null) {
					respText = WbUtil.run(module, allParams, false);
				} else {
					String jsonresp = WebUtil.fetch(this.request, "_jsonresp");
					this.request.setAttribute("_jsonresp", 0);
					respText = Flow.executeModule(module, this.request, allParams, false);
					this.request.setAttribute("_jsonresp", jsonresp);
				}

				if (!"undefined".equals(respText) && !StringUtil.isEmpty(respText)) {
					JsonUtil.addAll(hs, new JSONArray(respText));
				}
			}

			if (!isCc && hs.isEmpty() && !"结束".equals(this.activeNodeName)) {
				throw new IllegalArgumentException(errorMsg);
			} else {
				return hs;
			}
		}
	}

	private void addUsers(Connection conn, HashSet<String> users, String sql) throws Exception {
		Statement st = null;
		ResultSet rs = null;

		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql);

			while (rs.next()) {
				users.add(rs.getString(1));
			}
		} finally {
			DbUtil.close(rs);
			DbUtil.close(st);
		}

	}

	private JSONObject getNode(String nodeName, boolean silent) {
		if (StringUtil.isEmpty(nodeName)) {
			throw new NullPointerException("Node name is empty.");
		} else {
			int j = this.nodes.length();

			for (int i = 0; i < j; ++i) {
				JSONObject node = this.nodes.getJSONObject(i);
				if (nodeName.equals(node.getString("name"))) {
					return node;
				}
			}

			if (silent) {
				return null;
			} else {
				throw new NullPointerException("Node name \"" + nodeName + "\" is not found.");
			}
		}
	}

	public JSONObject getNode(String nodeName) {
		return this.getNode(nodeName, false);
	}

	private String replaceParams(String value) {
		return StringUtil.replaceParams(this.params, value);
	}

	public String getAttribute(JSONObject node, String attributeName) {
		return this.replaceParams(node.optString(attributeName, (String) null));
	}

	public String getFlowAttribute(String attributeName) {
		return this.replaceParams(this.flowProp.optString(attributeName, (String) null));
	}

	private String getTitle(JSONObject node) {
		String title = this.getAttribute(node, "nodeTitle");
		return StringUtil.select(new String[]{title, node.getString("name")});
	}

	private Connection getRequestConnection() throws Exception {
		if (this.request == null) {
			return null;
		} else {
			Connection conn = DbUtil.getConnection(this.request);
			if (conn.getAutoCommit()) {
				conn.setAutoCommit(false);
			}

			return conn;
		}
	}

	public void insert(Connection conn) throws Exception {
		PreparedStatement st = null;
		boolean newConn;
		if (conn == null) {
			newConn = true;
			conn = DbUtil.getConnection();
		} else {
			newConn = false;
		}

		try {
			if (newConn) {
				conn.setAutoCommit(false);
			}

			st = conn.prepareStatement("insert into WB_FLOW_LIST values(?,?,?,?,?,?,?,?,?,?,?,?)");
			st.setString(1, SysUtil.getId());
			st.setString(2, this.flowId);
			st.setString(3, this.activeNodeName);
			st.setString(4, this.getTitle(this.activeNode));
			st.setTimestamp(5, this.actionDate);
			DbUtil.setObject(st, 6, 12, this.userId);
			DbUtil.setObject(st, 7, 12, this.userDispName);
			st.setTimestamp(8, this.actionDate);
			DbUtil.setObject(st, 9, 12, this.userId);
			DbUtil.setObject(st, 10, 12, this.userDispName);
			st.setString(11, this.getFlowAttribute("flowName"));
			st.setInt(12, 1);
			st.executeUpdate();
			st.close();
			this.reloadUsers(conn, (JSONObject) null, true);
			st = conn.prepareStatement("insert into WB_FLOW values(?,?)");
			st.setString(1, this.flowId);
			DbUtil.setText(st, 2, this.flowJson.toString());
			st.executeUpdate();
			st.close();
			if (newConn) {
				conn.commit();
			}
		} finally {
			DbUtil.close(st);
			if (newConn) {
				DbUtil.close(conn);
			}

		}

	}

	public void viewDone() throws Exception {
		Connection conn = null;
		PreparedStatement st = null;

		try {
			conn = DbUtil.getConnection();
			st = conn.prepareStatement(
					"update WB_FLOW_USER set NEED_PROCESS=0 where FLOW_ID=? and USER_ID=? and IS_CC=1");
			st.setString(1, this.flowId);
			st.setString(2, this.userId);
			st.executeUpdate();
		} finally {
			DbUtil.close(st);
			DbUtil.close(conn);
		}

	}

	public JSONObject update(Connection conn) throws Exception {
		PreparedStatement st = null;
		PreparedStatement delSt = null;
		JSONObject result = new JSONObject();
		boolean newConn;
		if (conn == null) {
			newConn = true;
			conn = DbUtil.getConnection();
		} else {
			newConn = false;
		}

		try {
			if (newConn) {
				conn.setAutoCommit(false);
			}

			st = conn.prepareStatement(
					"update WB_FLOW_LIST set NODE_NAME=?,TITLE=?,LAST_MODIFY_DATE=?, LAST_USER_ID=?, LAST_USER_DISP_NAME=?, STATUS=? where FLOW_ID=?");
			st.setString(1, this.activeNodeName);
			String title = this.getTitle(this.activeNode);
			st.setString(2, title);
			st.setTimestamp(3, this.actionDate);
			DbUtil.setObject(st, 4, 12, this.userId);
			DbUtil.setObject(st, 5, 12, this.userDispName);
			int status = "结束".equals(this.activeNodeName) ? 2 : 1;
			st.setInt(6, status);
			st.setString(7, this.flowId);
			st.executeUpdate();
			st.close();
			result.put("NODE_NAME", this.activeNodeName);
			result.put("TITLE", title);
			result.put("LAST_MODIFY_DATE", this.actionDate);
			result.put("LAST_USER_ID", this.userId);
			result.put("LAST_USER_DISP_NAME", this.userDispName);
			result.put("STATUS", status);
			if (this.setProcessed) {
				st = conn.prepareStatement(
						"update WB_FLOW_USER set NEED_PROCESS=0,IS_PROCESSED=1 where FLOW_ID=? and USER_ID=? and IS_CC=0");
				st.setString(1, this.flowId);
				st.setString(2, this.userId);
				int k = st.executeUpdate();
				st.close();
				if (k != 1) {
					throw new RuntimeException("您不是该流程的处理人员。");
				}

				result.put("NEED_PROCESS", 0);
				result.put("IS_PROCESSED", 1);
			}

			if (this.nodeChanged) {
				st = conn.prepareStatement("delete from WB_FLOW_USER where FLOW_ID=? and IS_PROCESSED=0");
				st.setString(1, this.flowId);
				st.executeUpdate();
				st.close();
				st = conn.prepareStatement("update WB_FLOW_USER set NEED_PROCESS=0 where FLOW_ID=?");
				st.setString(1, this.flowId);
				st.executeUpdate();
				st.close();
				this.reloadUsers(conn, result, false);
			} else {
				int j = this.newUserList.size();
				if (j > 0) {
					delSt = conn.prepareStatement("delete from WB_FLOW_USER where FLOW_ID=? and USER_ID=?");
					delSt.setString(1, this.flowId);
					st = conn.prepareStatement("insert into WB_FLOW_USER values(?,?,?,1,0,0)");
					st.setString(2, this.flowId);

					for (int i = 0; i < j; ++i) {
						String newUserId = (String) this.newUserList.get(i);
						if (newUserId.equals(this.userId)) {
							result.put("NEED_PROCESS", 1);
						}

						delSt.setString(2, newUserId);
						delSt.addBatch();
						st.setString(1, SysUtil.getId());
						st.setString(3, newUserId);
						st.addBatch();
						if (i % 1000 == 999) {
							delSt.executeBatch();
							st.executeBatch();
						}
					}

					delSt.executeBatch();
					st.executeBatch();
					delSt.close();
					st.close();
				}
			}

			st = conn.prepareStatement("update WB_FLOW set FLOW_DATA=? where FLOW_ID=?");
			DbUtil.setText(st, 1, this.flowJson.toString());
			st.setString(2, this.flowId);
			st.executeUpdate();
			st.close();
			if (newConn) {
				conn.commit();
			}
		} finally {
			DbUtil.close(st);
			DbUtil.close(delSt);
			if (newConn) {
				DbUtil.close(conn);
			}

		}

		return result;
	}

	public void remove(Connection conn) throws Exception {
		PreparedStatement st = null;
		boolean newConn;
		if (conn == null) {
			newConn = true;
			conn = DbUtil.getConnection();
		} else {
			newConn = false;
		}

		try {
			if (newConn) {
				conn.setAutoCommit(false);
			}

			st = conn.prepareStatement("delete from  WB_FLOW_LIST where FLOW_ID=?");
			st.setString(1, this.flowId);
			st.executeUpdate();
			st.close();
			st = conn.prepareStatement("delete from WB_FLOW_USER where FLOW_ID=?");
			st.setString(1, this.flowId);
			st.executeUpdate();
			st.close();
			st = conn.prepareStatement("delete from WB_FLOW where FLOW_ID=?");
			st.setString(1, this.flowId);
			st.executeUpdate();
			st.close();
			if (newConn) {
				conn.commit();
			}
		} finally {
			DbUtil.close(st);
			if (newConn) {
				DbUtil.close(conn);
			}

		}

	}

	private void reloadUsers(Connection conn, JSONObject result, boolean isInsert) throws Exception {
		HashSet<String> doUsers = null;
		int index = 1;
		HashSet<String> updateUsers = new HashSet();
		PreparedStatement st1 = null;
		PreparedStatement st2 = null;
		PreparedStatement st3 = null;
		ResultSet rs = null;
		boolean hasResult = result != null;

		try {
			boolean needUpdate;
			if (isInsert) {
				needUpdate = false;
			} else {
				st3 = conn.prepareStatement("select USER_ID from WB_FLOW_USER where FLOW_ID=?");
				st3.setString(1, this.flowId);
				rs = st3.executeQuery();

				while (rs.next()) {
					updateUsers.add(rs.getString(1));
				}

				rs.close();
				st3.close();
				needUpdate = !updateUsers.isEmpty();
			}

			st1 = conn.prepareStatement("insert into WB_FLOW_USER values(?,?,?,?,?,?)");
			st1.setString(2, this.flowId);
			if (needUpdate) {
				st2 = conn.prepareStatement(
						"update WB_FLOW_USER set NEED_PROCESS=?,IS_CC=? where FLOW_ID=? and USER_ID=?");
				st2.setString(3, this.flowId);
			}

			int i = 0;

			while (true) {
				if (i >= 2) {
					st1.executeBatch();
					if (needUpdate) {
						st2.executeBatch();
					}
					break;
				}

				st1.setInt(6, i);
				st1.setInt(4, 1);
				st1.setInt(5, i);
				if (needUpdate) {
					st2.setInt(1, i == 0 ? 1 : 0);
					st2.setInt(2, i);
				}

				HashSet<String> users = this.getUsers(conn, i != 0);
				if (isInsert && i == 1) {
					if (users == null) {
						users = new HashSet();
					}

					users.add(this.userId);
				}

				if (i == 0) {
					doUsers = users;
				} else if (users != null && doUsers != null) {
					users.removeAll(doUsers);
				}

				if (users != null) {
					if (i == 0) {
						this.activeNode.put("totalUserCount", users.size());
					}

					for (Iterator var16 = users.iterator(); var16.hasNext(); ++index) {
						String user = (String) var16.next();
						if (needUpdate && updateUsers.contains(user)) {
							if (hasResult && user.equals(this.userId)) {
								result.put("NEED_PROCESS", 1);
								result.put("IS_CC", i);
							}

							st2.setString(4, user);
							st2.addBatch();
							if (index % 1000 == 0) {
								st2.executeBatch();
							}
						} else {
							st1.setString(1, SysUtil.getId());
							st1.setString(3, user);
							if (isInsert && i == 1 && user.equals(this.userId)) {
								st1.setInt(4, 0);
							} else {
								st1.setInt(4, 1);
							}

							st1.addBatch();
							if (index % 1000 == 0) {
								st1.executeBatch();
							}
						}
					}
				}

				++i;
			}
		} finally {
			DbUtil.close(rs);
			DbUtil.close(st1);
			DbUtil.close(st2);
			DbUtil.close(st3);
		}

	}

	private void checkEnd() {
		if ("结束".equals(this.activeNodeName)) {
			throw new RuntimeException("该流程已经结束。");
		}
	}

	public void insert() throws Exception {
		this.insert(this.getRequestConnection());
	}

	public JSONObject update() throws Exception {
		return this.update(this.getRequestConnection());
	}

	public JSONObject getDialog() throws Exception {
		String dialog = this.getAttribute(this.activeNode, "dialog");
		if (StringUtil.isEmpty(dialog)) {
			dialog = this.getFlowAttribute("defaultDialog");
			if (StringUtil.isEmpty(dialog)) {
				return null;
			}
		} else if ("-".equals(dialog)) {
			return null;
		}

		JSONObject result = new JSONObject();
		JSONArray selActions = new JSONArray();
		JSONArray allActions = JsonUtil.getArray(this.getAttribute(this.activeNode, "actionType"));
		if (allActions != null) {
			int j = allActions.length();

			for (int i = 0; i < j; ++i) {
				JSONObject action = allActions.getJSONObject(i);
				String iconCls = action.optString("iconCls");
				if (!iconCls.isEmpty()) {
					action.put("iconCls", iconCls + "_icon");
				}

				if (action.getInt("selected") == 1) {
					action.remove("selected");
					selActions.put(action);
				}
			}
		}

		result.put("module", Flow.executeModule(dialog, this.request, this.params, true));
		result.put("path", dialog);
		Integer minWidth = (Integer) JsonUtil.opt(this.activeNode, "minWidth");
		if (minWidth == null) {
			minWidth = (Integer) JsonUtil.opt(this.flowProp, "defaultMinWidth");
		}

		if (minWidth != null) {
			result.put("minWidth", minWidth);
		}

		result.put("actions", selActions);
		result.put("flowParams", this.params);
		result.put("history", this.history);
		return result;
	}

	public JSONObject getViewDialog() throws Exception {
		String dialog = this.getAttribute(this.activeNode, "viewDialog");
		if (StringUtil.isEmpty(dialog)) {
			dialog = this.getFlowAttribute("defaultViewDialog");
			if (StringUtil.isEmpty(dialog)) {
				return null;
			}
		} else if ("-".equals(dialog)) {
			return null;
		}

		JSONObject result = new JSONObject();
		result.put("module", Flow.executeModule(dialog, this.request, this.params, true));
		result.put("path", dialog);
		result.put("flowParams", this.params);
		return result;
	}

	public void checkActiveNode(String nodeName) {
		if (!this.activeNodeName.equals(nodeName)) {
			throw new IllegalArgumentException("流程当前节点不是“" + nodeName + "”。");
		}
	}

	public void reject(String itemId) {
		JSONObject item = JsonUtil.findObject(this.history, "itemId", itemId);
		if (item == null) {
			throw new RuntimeException("无法退回到指定节点，因为该节点不存在。");
		} else {
			String nodeName = item.getString("node");
			if (!this.inHistory(nodeName)) {
				throw new RuntimeException("无法退回到指定节点，因为该节点不在处理历史记录中。");
			} else if (nodeName.equals(this.activeNodeName)) {
				throw new RuntimeException("无法退回到当前节点。");
			} else {
				this.recordHistory("reject");
				this.setActiveNode(nodeName);
			}
		}
	}

	private void recordHistory(String actionName) {
		JSONObject item = new JSONObject();
		item.put("itemId", SysUtil.getId());
		item.put("date", this.actionDate);
		item.put("userId", this.userId);
		item.put("userName", this.userName);
		item.put("userDispName", this.userDispName);
		item.put("node", this.activeNodeName);
		item.put("title", this.getTitle(this.activeNode));
		item.put("action", actionName);
		this.history.put(item);
		this.params.put("flow.action", actionName);
	}

	private boolean inHistory(String nodeName) {
		int j = this.history.length();

		for (int i = 0; i < j; ++i) {
			if (nodeName.equals(this.history.getJSONObject(i).getString("node"))) {
				return true;
			}
		}

		return false;
	}

	private void insertNode(String newNodeName, String actionText, String nodeTitle, String passUsers,
			JSONObject userData, boolean insertBefore) {
		JSONObject node = this.getNode(newNodeName, true);
		if (node != null) {
			throw new RuntimeException("节点名称“" + newNodeName + "”已经存在，请重新指定一个节点名称。");
		} else {
			node = new JSONObject();
			this.nodes.put(node);
			JSONArray actions = new JSONArray();
			JSONObject action = new JSONObject();
			action.put("selected", 1);
			action.put("name", "pass");
			action.put("text", actionText);
			action.put("iconCls", "ok");
			actions.put(action);
			action = new JSONObject();
			action.put("selected", 1);
			action.put("name", "cancel");
			action.put("text", Str.format(this.request, "cancel", new Object[0]));
			action.put("iconCls", "cancel");
			actions.put(action);
			node.put("actionType", actions);
			node.put("dialog", this.getAttribute(this.activeNode, "dialog"));
			node.put("doUser", userData);
			node.put("name", newNodeName);
			node.put("nodeTitle", nodeTitle);
			node.put("passUsers", passUsers);
			node.put("doUserCount", 0);
			this.insertLink(newNodeName, insertBefore);
		}
	}

	private void insertLink(String newNodeName, boolean insertBefore) {
		int j = this.links.length();

		JSONObject link;
		for (int i = 0; i < j; ++i) {
			link = this.links.getJSONObject(i);
			if (insertBefore) {
				if (this.activeNodeName.equals(link.getString("to"))) {
					link.put("to", newNodeName);
				}
			} else if (this.activeNodeName.equals(link.getString("from"))) {
				link.put("from", newNodeName);
			}
		}

		link = new JSONObject();
		this.links.put(link);
		if (insertBefore) {
			link.put("from", newNodeName);
			link.put("to", this.activeNodeName);
		} else {
			link.put("from", this.activeNodeName);
			link.put("to", newNodeName);
		}

	}

	public void beforeSign(String newNodeName, String passName, String nodeTitle, String passUsers,
			JSONObject userData) {
		this.recordHistory("beforeSign");
		this.insertNode(newNodeName, passName, nodeTitle, passUsers, userData, true);
		this.setActiveNode(newNodeName);
	}

	public void afterSign(String newNodeName, String passName, String nodeTitle, String passUsers,
			JSONObject userData) {
		this.recordHistory("afterSign");
		this.setProcessed = false;
		this.insertNode(newNodeName, passName, nodeTitle, passUsers, userData, false);
	}

	private void addNewUsers(JSONObject userData) {
		JSONArray newUsers = userData.getJSONArray("userId");
		JsonUtil.addAll(this.newUserList, newUsers);
		this.activeNode.put("totalUserCount", this.activeNode.optInt("totalUserCount", 0) + newUsers.length());
	}

	public void plusSign(JSONObject userData) {
		this.recordHistory("plusSign");
		this.setProcessed = false;
		this.addNewUsers(userData);
	}

	public void turn(JSONObject userData) {
		this.recordHistory("turn");
		this.addNewUsers(userData);
	}
}