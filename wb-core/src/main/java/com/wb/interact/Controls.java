package com.wb.interact;

import com.wb.common.Base;
import com.wb.util.FileUtil;
import com.wb.util.JsonUtil;
import com.wb.util.StringUtil;
import com.wb.util.SysUtil;
import com.wb.util.WebUtil;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;

public class Controls {
	private static ConcurrentHashMap<String, JSONObject> buffer;
	private static final File file;

	static {
		file = new File(Base.path, "wb/system/controls.json");
	}

	public static void open(HttpServletRequest request, HttpServletResponse response) throws Exception {
		JSONObject json = JsonUtil.readObject(file);
		JSONArray data = new JSONArray();
		JSONArray nodes = new JSONArray(request.getParameter("controls"));
		int j = nodes.length();

		for (int i = 0; i < j; ++i) {
			String id = nodes.getString(i);
			JSONObject control = JsonUtil.findObject(json, "children", "id", id);
			if (control == null) {
				throw new IOException("\"" + id + "\" 没有找到。");
			}

			data.put(control);
		}

		WebUtil.send(response, data);
	}

	public static void getControlTree(HttpServletRequest request, HttpServletResponse response) throws Exception {
		JSONObject json = JsonUtil.readObject(file);
		if ("ide".equals(request.getParameter("type"))) {
			setControlNode(json);
		}

		WebUtil.send(response, json);
	}

	private static void setControlNode(JSONObject node) {
		if (node.has("leaf")) {
			node.remove("leaf");
			node.put("type", node.get("id"));
			node.put("control", true);
			node.put("children", new JSONArray());
		} else {
			JSONArray children = node.getJSONArray("children");
			int j = children.length();

			for (int i = 0; i < j; ++i) {
				setControlNode(children.getJSONObject(i));
			}
		}

	}

	public static synchronized void addControl(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		JSONObject json = JsonUtil.readObject(file);
		String name = request.getParameter("name");
		String parent = request.getParameter("parent");
		String selNode = request.getParameter("selNode");
		String folderId = "";
		boolean isFolder = Boolean.parseBoolean(request.getParameter("isFolder"));
		folderId = StringUtil.concat(new String[]{"n", SysUtil.getId()});
		if (JsonUtil.findObject(json, "children", "id", isFolder ? folderId : name) != null) {
			throw new IOException("名称 \"" + name + "\" 已经存在。");
		} else {
			JSONObject parentNode = JsonUtil.findObject(json, "children", "id", parent);
			JSONObject newNode = new JSONObject();
			newNode.put("text", name);
			if (isFolder) {
				newNode.put("id", folderId);
				newNode.put("children", new JSONArray());
			} else {
				newNode.put("id", name);
				newNode.put("iconCls", "item_icon");
				newNode.put("leaf", true);
				newNode.put("general", new JSONObject("{iconCls:'item_icon'}"));
				newNode.put("configs", new JSONObject("{itemId:{type:'string'}}"));
				newNode.put("events", new JSONObject());
			}

			JSONArray children = parentNode.getJSONArray("children");
			if (selNode.isEmpty()) {
				children.put(newNode);
			} else {
				JSONObject node = JsonUtil.findObject(json, "children", "id", selNode);
				int index = children.indexOf(node);
				++index;
				children.add(index, newNode);
			}

			save(json);
			if (!isFolder) {
				buffer.put("name", new JSONObject(newNode.toString()));
			}

			WebUtil.send(response, folderId);
		}
	}

	public static synchronized void saveControls(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		JSONObject json = JsonUtil.readObject(file);
		JSONArray nodes = new JSONArray(StringUtil.getString(request.getInputStream()));
		int j = nodes.length();

		JSONObject control;
		int i;
		for (i = 0; i < j; ++i) {
			control = nodes.getJSONObject(i);
			String id = control.getString("id");
			if (!JsonUtil.replace(json, "children", "id", id, control.get("data"))) {
				throw new IOException("\"" + id + "\" 没有找到。");
			}
		}

		save(json);

		for (i = 0; i < j; ++i) {
			control = nodes.getJSONObject(i);
			buffer.put(control.getString("id"), new JSONObject(control.get("data").toString()));
		}

	}

	public static synchronized void renameFolder(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		JSONObject json = JsonUtil.readObject(file);
		String id = request.getParameter("id");
		JSONObject folder = JsonUtil.findObject(json, "children", "id", id);
		if (folder == null) {
			throw new IOException("目录 \"" + id + "\" 已经被删除。");
		} else {
			folder.put("text", request.getParameter("newName"));
			save(json);
		}
	}

	public static synchronized void deleteControls(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		JSONObject json = JsonUtil.readObject(file);
		JSONArray nodes = new JSONArray(request.getParameter("controls"));
		int j = nodes.length();

		for (int i = 0; i < j; ++i) {
			JsonUtil.remove(json, "children", "id", nodes.getString(i));
		}

		save(json);
		buffer.clear();
		saveToBuffer(json);
	}

	public static synchronized void moveControls(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		JSONObject json = JsonUtil.readObject(file);
		JSONArray sourceNodes = new JSONArray(request.getParameter("sourceNodes"));
		String destNodeId = request.getParameter("destNode");
		JSONObject destNode = JsonUtil.findObject(json, "children", "id", destNodeId);
		String dropPosition = request.getParameter("dropPosition");
		int j = sourceNodes.length();

		for (int i = 0; i < j; ++i) {
			String id = sourceNodes.getString(i);
			String movedNode = JsonUtil.remove(json, "children", "id", id);
			if (dropPosition.equals("append")) {
				JSONArray destChildren = destNode.getJSONArray("children");
				destChildren.put(new JSONObject(movedNode));
			} else {
				JSONArray parentNode = JsonUtil.findArray(json, "children", "id", destNodeId);
				int index = parentNode.indexOf(destNode);
				if (dropPosition.equals("after")) {
					++index;
				}

				parentNode.add(index, new JSONObject(movedNode));
			}
		}

		save(json);
	}

	public static synchronized void copyControl(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		JSONObject json = JsonUtil.readObject(file);
		String source = request.getParameter("source");
		String dest = request.getParameter("dest");
		JSONObject control = JsonUtil.findObject(json, "children", "id", source);
		if (control == null) {
			throw new IOException("控件 \"" + source + "\" 没有找到。");
		} else if (JsonUtil.findObject(json, "children", "id", dest) != null) {
			throw new IOException("名称 \"" + dest + "\" 已经存在。");
		} else {
			JSONArray parentNode = JsonUtil.findArray(json, "children", "id", source);
			int index = parentNode.indexOf(control);
			JSONObject newControl = (new JSONObject(control.toString())).put("id", dest).put("text", dest);
			parentNode.add(index + 1, newControl);
			save(json);
			buffer.put("name", new JSONObject(newControl.toString()));
		}
	}

	public static JSONObject get(String id) throws IOException {
		JSONObject object = (JSONObject) buffer.get(id);
		if (object == null) {
			throw new IOException("控件 \"" + id + "\" 没有找到。");
		} else {
			return object;
		}
	}

	private static void save(JSONObject json) throws Exception {
		FileUtil.syncSave(file, json.toString());
	}

	public static synchronized void load() {
		try {
			buffer = new ConcurrentHashMap();
			saveToBuffer(new JSONObject(FileUtil.readString(file)));
		} catch (Throwable var1) {
			throw new RuntimeException(var1);
		}
	}

	private static void saveToBuffer(JSONObject json) {
		JSONArray ja = json.getJSONArray("children");
		int j = ja.length();

		for (int i = 0; i < j; ++i) {
			JSONObject jo = ja.getJSONObject(i);
			if (jo.optBoolean("leaf")) {
				buffer.put(jo.getString("id"), new JSONObject(jo.toString()));
			} else {
				saveToBuffer(jo);
			}
		}

	}
}