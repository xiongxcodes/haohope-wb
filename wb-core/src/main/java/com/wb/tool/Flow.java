package com.wb.tool;

import com.wb.common.Base;
import com.wb.interact.ResourceManager;
import com.wb.util.DbUtil;
import com.wb.util.JsonUtil;
import com.wb.util.StringUtil;
import com.wb.util.SysUtil;
import com.wb.util.WbUtil;
import com.wb.util.WebUtil;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

public class Flow {
	public static FlowObject start(String filename, JSONObject params, Connection conn) throws Exception {
		FlowObject flowObject = getFlowObject(filename, params, (HttpServletRequest) null);
		flowObject.forward();
		flowObject.insert(conn);
		return flowObject;
	}

	public static FlowObject start(String filename) throws Exception {
		return start(filename, (JSONObject) null, (Connection) null);
	}

	public static void start(HttpServletRequest request, HttpServletResponse response) throws Exception {
		doOpen(request, response, 0);
	}

	public static void open(HttpServletRequest request, HttpServletResponse response) throws Exception {
		doOpen(request, response, 1);
	}

	public static void view(HttpServletRequest request, HttpServletResponse response) throws Exception {
		doOpen(request, response, 2);
	}

	private static void doOpen(HttpServletRequest request, HttpServletResponse response, int mode) throws Exception {
		JSONObject params = JsonUtil.getObject(request.getParameter("params"));
		FlowObject flowObject;
		if (mode == 0) {
			flowObject = getFlowObject(request.getParameter("filename"), params, request);
		} else {
			flowObject = new FlowObject(getFlowData(request.getParameter("flowId"), request), params, request,
					(String) null, false, (String) null);
		}

		if (mode != 0) {
			flowObject.checkActiveNode(request.getParameter("nodeName"));
		}

		JSONObject dialog;
		if (mode == 2) {
			dialog = flowObject.getViewDialog();
			if (params.has("_viewDone")) {
				flowObject.viewDone();
			}

			if (dialog == null) {
				throw new NullPointerException("流程当前节点没有配置查看对话框属性。");
			}
		} else {
			dialog = flowObject.getDialog();
			if (dialog == null) {
				throw new NullPointerException("流程当前节点没有配置处理对话框属性。");
			}
		}

		WebUtil.send(response, dialog);
	}

	private static void doAction(HttpServletRequest request, HttpServletResponse response, String actionName)
			throws Exception {
		JSONObject flowParams = new JSONObject(WebUtil.fetch(request, "xFlowParams"));
		JSONObject extraParams = JsonUtil.getObject(WebUtil.fetch(request, "params"));
		JSONObject result = null;
		JSONObject userData = flowParams.optJSONObject("userData");
		String flowId = flowParams.optString("flowId");
		String newNodeName = flowParams.optString("newNodeName");
		String passName = flowParams.optString("passName");
		String nodeTitle = flowParams.optString("nodeTitle");
		String passUsers = flowParams.optString("passUsers");
		boolean isStart = StringUtil.isEmpty(flowId);
		JSONObject paramsObject;
		FlowObject flowObject;
		String module;
		if (isStart) {
			String filename = flowParams.optString("filename");
			if (StringUtil.isEmpty(filename)) {
				throw new RuntimeException("Neither filename nor flowId is specified.");
			}

			flowObject = getFlowObject(filename, extraParams, request);
			paramsObject = flowObject.getParams();
			module = flowObject.getFlowAttribute("beforeModule");
			if (!StringUtil.isEmpty(module)) {
				executeModule(module, request, paramsObject, false);
			}

			flowObject.forward();
		} else {
			flowObject = new FlowObject(getFlowData(flowId, request), extraParams, request, (String) null, false,
					(String) null);
			flowObject.checkActiveNode(flowParams.getString("nodeName"));
			paramsObject = flowObject.getParams();
			module = flowObject.getFlowAttribute("beforeModule");
			if (!StringUtil.isEmpty(module)) {
				executeModule(module, request, paramsObject, false);
			}

			if ("pass".equals(actionName)) {
				flowObject.forward();
			} else if ("reject".equals(actionName)) {
				flowObject.reject(flowParams.getString("rejectTo"));
			} else if ("beforeSign".equals(actionName)) {
				flowObject.beforeSign(newNodeName, passName, nodeTitle, passUsers, userData);
			} else if ("afterSign".equals(actionName)) {
				flowObject.afterSign(newNodeName, passName, nodeTitle, passUsers, userData);
			} else if ("plusSign".equals(actionName)) {
				flowObject.plusSign(userData);
			} else if ("turn".equals(actionName)) {
				flowObject.turn(userData);
			}
		}

		module = flowParams.optString("module");
		if (!StringUtil.isEmpty(module)) {
			executeModule(module, request, paramsObject, false);
		}

		module = flowObject.getFlowAttribute("afterModule");
		if (!StringUtil.isEmpty(module)) {
			executeModule(module, request, paramsObject, false);
		}

		if (isStart) {
			flowObject.insert();
		} else {
			result = flowObject.update();
		}

		module = flowObject.getFlowAttribute("finalModule");
		if (!StringUtil.isEmpty(module)) {
			executeModule(module, request, paramsObject, false);
		}

		if (WebUtil.jsonResponse(request)) {
			WebUtil.send(response, result, true);
		} else {
			WebUtil.send(response, result);
		}

	}

	public static void pass(HttpServletRequest request, HttpServletResponse response) throws Exception {
		doAction(request, response, "pass");
	}

	public static void reject(HttpServletRequest request, HttpServletResponse response) throws Exception {
		doAction(request, response, "reject");
	}

	public static void beforeSign(HttpServletRequest request, HttpServletResponse response) throws Exception {
		doAction(request, response, "beforeSign");
	}

	public static void afterSign(HttpServletRequest request, HttpServletResponse response) throws Exception {
		doAction(request, response, "afterSign");
	}

	public static void plusSign(HttpServletRequest request, HttpServletResponse response) throws Exception {
		doAction(request, response, "plusSign");
	}

	public static void turn(HttpServletRequest request, HttpServletResponse response) throws Exception {
		doAction(request, response, "turn");
	}

	private static JSONObject getFlowData(String flowId, HttpServletRequest request) throws Exception {
		Connection conn = null;
		PreparedStatement st = null;
		ResultSet rs = null;

		String result;
		try {
			conn = DbUtil.getConnection(request);
			st = conn.prepareStatement("select FLOW_DATA from WB_FLOW where FLOW_ID=?");
			st.setString(1, flowId);
			rs = st.executeQuery();
			if (!rs.next()) {
				throw new IllegalArgumentException("流程“" + flowId + "”不存在。");
			}

			result = (String) DbUtil.getObject(rs, 1, 2011);
		} finally {
			DbUtil.close(rs);
			DbUtil.close(st);
		}

		return new JSONObject(result);
	}

	private static FlowObject getFlowObject(String filename, JSONObject params, HttpServletRequest request)
			throws Exception {
		File file = new File(ResourceManager.basePath, filename);
		if (!file.exists()) {
			file = new File(Base.path, filename);
		}

		String flowId;
		if (request == null) {
			flowId = null;
		} else {
			flowId = WebUtil.fetch(request, "xFlowId");
		}

		if (StringUtil.isEmpty(flowId)) {
			flowId = SysUtil.getId();
		}

		return new FlowObject(ResourceManager.getExecuteObject(request, file), params, request, file.getName(), true,
				flowId);
	}

	public static String executeModule(String url, HttpServletRequest request, JSONObject params, boolean isInvoke)
			throws Exception {
		if (request == null) {
			return WbUtil.run(url, params, isInvoke);
		} else if (WbUtil.canAccess(request, url)) {
			request.setAttribute("sys.params", params);
			return WbUtil.run(url, params, request, isInvoke);
		} else {
			SysUtil.accessDenied(request, url);
			return null;
		}
	}
}