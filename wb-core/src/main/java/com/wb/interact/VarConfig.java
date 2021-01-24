package com.wb.interact;

import com.wb.common.Var;
import com.wb.util.FileUtil;
import com.wb.util.JsonUtil;
import com.wb.util.StringUtil;
import com.wb.util.WebUtil;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;

public class VarConfig {
	public static void getTree(HttpServletRequest request, HttpServletResponse response) throws Exception {
		JSONObject jo = JsonUtil.readObject(Var.file);
		JSONObject tree = new JSONObject();
		buildTree(jo, tree);
		WebUtil.send(response, tree);
	}

	public static synchronized void setVar(HttpServletRequest request, HttpServletResponse response) throws Exception {
		JSONObject object = JsonUtil.readObject(Var.file);
		String name = request.getParameter("name");
		String path = request.getParameter("path");
		String type = request.getParameter("type");
		String valueStr = request.getParameter("value");
		String configStr = request.getParameter("config");
		boolean isNew = Boolean.parseBoolean(request.getParameter("isNew"));
		if (name.indexOf(46) != -1) {
			throw new RuntimeException("名称 \"" + name + "\" 不能包含符号 “.”。");
		} else {
			Object folderObject = JsonUtil.getValue(object, path, '.');
			if (folderObject instanceof JSONObject) {
				JSONObject folder = (JSONObject) folderObject;
				if (folder.has(name)) {
					if (isNew) {
						throw new RuntimeException("名称 \"" + name + "\" 已经存在。");
					}
				} else if (!isNew) {
					throw new RuntimeException("名称 \"" + name + "\" 不存在。");
				}

				Object primativeVal;
				if (type.equals("int")) {
					primativeVal = Integer.parseInt(valueStr);
				} else if (type.equals("bool")) {
					primativeVal = Boolean.parseBoolean(valueStr);
				} else if (type.equals("double")) {
					primativeVal = Double.parseDouble(valueStr);
				} else {
					primativeVal = valueStr;
				}

				JSONArray value;
				if (isNew) {
					value = new JSONArray();
					value.put(primativeVal);
					value.put(request.getParameter("remark"));
					JSONObject config;
					if (configStr.isEmpty()) {
						config = new JSONObject();
					} else {
						config = new JSONObject(configStr);
					}

					config.put("type", request.getParameter("type"));
					value.put(config);
					folder.put(name, value);
				} else {
					value = folder.getJSONArray(name);
					value.put(0, primativeVal);
				}

				FileUtil.syncSave(Var.file, object.toString(2));
				Var.buffer.put(path + '.' + name, primativeVal);
				Var.loadBasicVars();
			} else {
				throw new RuntimeException("目录 \"" + path + "\" 不存在或不是一个目录。");
			}
		}
	}

	public static synchronized void delVar(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String path = request.getParameter("path");
		if (StringUtil.isEmpty(path)) {
			throw new RuntimeException("Empty path value.");
		} else {
			JSONArray names = new JSONArray(request.getParameter("names"));
			JSONObject object = JsonUtil.readObject(Var.file);
			JSONObject selFolder = (JSONObject) JsonUtil.getValue(object, path, '.');
			int j = names.length();

			int i;
			for (i = 0; i < j; ++i) {
				selFolder.remove(names.optString(i));
			}

			FileUtil.syncSave(Var.file, object.toString(2));

			for (i = 0; i < j; ++i) {
				Var.buffer.remove(StringUtil.concat(new String[]{path, ".", names.optString(i)}));
			}

		}
	}

	public static synchronized void setFolder(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		String type = request.getParameter("type");
		JSONObject object = JsonUtil.readObject(Var.file);
		String path = request.getParameter("path");
		String name = request.getParameter("name");
		if (path == null) {
			throw new RuntimeException("null path parameter");
		} else {
			JSONObject folder;
			if (type.equals("add")) {
				if (name == null) {
					throw new RuntimeException("null name parameter");
				}

				if (name.indexOf(46) != -1) {
					throw new RuntimeException("名称 \"" + name + "\" 不能包含符号 “.”。");
				}

				folder = (JSONObject) JsonUtil.getValue(object, path, '.');
				if (folder.has(name)) {
					throw new RuntimeException("名称 \"" + name + "\" 已经存在。");
				}

				folder.put(name, new JSONObject());
			} else {
				String newName;
				if (type.equals("delete")) {
					JsonUtil.setValue(object, path, '.', (Object) null);
					path = path + '.';
					Set<Entry<String, Object>> es = Var.buffer.entrySet();
					Iterator var10 = es.iterator();

					while (var10.hasNext()) {
						Entry<String, Object> e = (Entry) var10.next();
						newName = (String) e.getKey();
						if (newName.startsWith(path)) {
							Var.buffer.remove(newName);
						}
					}
				} else if (type.equals("update")) {
					newName = request.getParameter("newName");
					if (newName.indexOf(46) != -1) {
						throw new RuntimeException("名称 \"" + newName + "\" 不能包含符号 “.”。");
					}

					folder = (JSONObject) JsonUtil.getValue(object, path, '.');
					if (folder.has(newName)) {
						throw new RuntimeException("名称 \"" + newName + "\" 已经存在。");
					}

					JSONObject jo = folder.getJSONObject(name);
					folder.remove(name);
					folder.put(newName, jo);
					String newPath = StringUtil.concat(new String[]{path, ".", newName, "."});
					path = StringUtil.concat(new String[]{path, ".", name, "."});
					Set<Entry<String, Object>> es = Var.buffer.entrySet();
					int oldPathLen = path.length();
					Iterator var14 = es.iterator();

					while (var14.hasNext()) {
						Entry<String, Object> e = (Entry) var14.next();
						String key = (String) e.getKey();
						if (key.startsWith(path)) {
							Var.buffer.remove(key);
							Var.buffer.put(newPath + key.substring(oldPathLen), e.getValue());
						}
					}
				}
			}

			FileUtil.syncSave(Var.file, object.toString(2));
		}
	}

	private static void buildTree(JSONObject jo, JSONObject tree) throws IOException {
		Set<Entry<String, Object>> entrySet = jo.entrySet();
		JSONArray children = new JSONArray();
		tree.put("children", children);
		Iterator var8 = entrySet.iterator();

		while (var8.hasNext()) {
			Entry<String, Object> entry = (Entry) var8.next();
			String key = (String) entry.getKey();
			Object object = jo.opt(key);
			if (object instanceof JSONObject) {
				JSONObject node = new JSONObject();
				node.put("text", key);
				children.put(node);
				buildTree((JSONObject) object, node);
			}
		}

	}

	public static void getVars(HttpServletRequest request, HttpServletResponse response) throws Exception {
		JSONObject jo = JsonUtil.readObject(Var.file);
		JSONObject folder = (JSONObject) JsonUtil.getValue(jo, request.getParameter("path"), '.');
		if (folder == null) {
			throw new IllegalArgumentException("指定路径变量不存在。");
		} else {
			Set<Entry<String, Object>> entrySet = folder.entrySet();
			JSONArray items = new JSONArray();
			Iterator var10 = entrySet.iterator();

			while (var10.hasNext()) {
				Entry<String, Object> entry = (Entry) var10.next();
				Object value = entry.getValue();
				if (value instanceof JSONArray) {
					JSONArray jsonValue = (JSONArray) value;
					JSONObject item = new JSONObject();
					item.put("name", entry.getKey());
					item.put("value", jsonValue.opt(0));
					item.put("remark", jsonValue.opt(1));
					item.put("meta", jsonValue.opt(2));
					items.put(item);
				}
			}

			WebUtil.send(response, (new JSONObject()).put("rows", items));
		}
	}
}