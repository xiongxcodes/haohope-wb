package com.wb.interact;

import com.wb.common.Base;
import com.wb.common.Session;
import com.wb.common.Str;
import com.wb.common.Var;
import com.wb.util.FileUtil;
import com.wb.util.JsonUtil;
import com.wb.util.SortUtil;
import com.wb.util.StringUtil;
import com.wb.util.SysUtil;
import com.wb.util.WebUtil;
import java.io.File;
import java.util.Date;
import java.util.HashSet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;

public class ResourceManager {
	public static final File basePath;
	private static final String[] resourceExt;

	static {
		basePath = new File(Base.path, "wb/system/resource");
		resourceExt = Var.getString("sys.file.resourceFiles").split(",");
	}

	public static void getFileList(HttpServletRequest request, HttpServletResponse response) throws Exception {
		File path = new File(request.getParameter("path"));
		int type = Integer.parseInt(request.getParameter("type"));
		boolean isTree = type == 1;
		if (!FileUtil.isAncestor(basePath, path)) {
			SysUtil.accessDenied(request);
		}

		File[] files = FileUtil.listFiles(path);
		if (isTree) {
			SortUtil.sort(files);
		} else {
			String[] sortInfo = WebUtil.getSortInfo(request);
			String[] fields = new String[]{"text", "size", "type", "date"};
			SortUtil.sort(files, StringUtil.indexOf(fields, sortInfo[0]), sortInfo[1].equalsIgnoreCase("desc"));
		}

		JSONArray rows = new JSONArray();
		File[] var13 = files;
		int var12 = files.length;

		for (int var15 = 0; var15 < var12; ++var15) {
			File file = var13[var15];
			boolean isDir = file.isDirectory();
			if ((!isTree || isDir) && canDisplay(request, file)) {
				String dirPath = FileUtil.getPath(file);
				JSONObject item = new JSONObject();
				item.put("text", StringUtil.select(new String[]{file.getName(), dirPath}));
				item.put("path", dirPath);
				item.put("leaf", !isDir);
				if (isTree) {
					if (isLeafFolder(request, file)) {
						item.put("children", new JSONArray());
					}
				} else {
					item.put("icon", WebUtil.encode(dirPath));
					item.put("date", new Date(file.lastModified()));
					item.put("size", file.length());
					item.put("type", isDir ? Str.format(request, "folder", new Object[0]) : FileUtil.getFileType(file));
				}

				rows.put(item);
			}
		}

		WebUtil.send(response, (new JSONObject()).put(isTree ? "children" : "rows", rows));
	}

	public static void getPerm(HttpServletRequest request, HttpServletResponse response) throws Exception {
		File file = new File(basePath, request.getParameter("fileName"));
		if (!FileUtil.isAncestor(basePath, file)) {
			SysUtil.accessDenied(request);
		}

		JSONObject content = JsonUtil.readObject(file);
		JSONObject rolePerms = (JSONObject) content.opt("roles");
		JSONObject result;
		if (rolePerms == null) {
			result = new JSONObject();
		} else {
			checkPerm(request, content, "get");
			result = rolePerms;
		}

		WebUtil.send(response, result);
	}

	private static boolean hasPerm(HttpServletRequest request, JSONObject content, String type) throws Exception {
		String[] userRoles = Session.getRoles(request);
		String userId = Session.getUserId(request);
		if (userRoles != null && userId != null) {
			if (StringUtil.indexOf(userRoles, "admin") != -1) {
				return true;
			} else {
				JSONObject permRoles = content.optJSONObject("roles");
				if (permRoles == null) {
					return false;
				} else {
					JSONArray items = permRoles.optJSONArray(type);
					HashSet hs;
					if (items != null) {
						hs = JsonUtil.toHashSet(items);
						String[] var11 = userRoles;
						int var10 = userRoles.length;

						for (int var9 = 0; var9 < var10; ++var9) {
							String role = var11[var9];
							if (hs.contains(role)) {
								return true;
							}
						}
					}

					items = permRoles.optJSONArray(type + "User");
					if (items == null) {
						return false;
					} else {
						hs = JsonUtil.toHashSet(items);
						return hs.contains(userId);
					}
				}
			}
		} else {
			return false;
		}
	}

	private static void checkPerm(HttpServletRequest request, JSONObject content, String type) throws Exception {
		if (!hasPerm(request, content, type)) {
			SysUtil.accessDenied(request);
		}

	}

	public static void saveResource(HttpServletRequest request, HttpServletResponse response) throws Exception {
		JSONArray array = new JSONArray(request.getParameter("data"));
		int j = array.length();

		for (int i = 0; i < j; ++i) {
			JSONObject object = array.getJSONObject(i);
			saveFile(request, new File(object.getString("file")), object.getJSONObject("data"), true);
		}

	}

	public static void openResource(HttpServletRequest request, HttpServletResponse response) throws Exception {
		JSONArray result = new JSONArray();
		JSONArray items = new JSONArray(request.getParameter("filenames"));
		int j = items.length();

		for (int i = 0; i < j; ++i) {
			String filename = items.getString(i);
			File file = new File(filename);
			if (!file.exists()) {
				String.format("文件 “%s” 不存在。", file.getName());
			}

			if (!FileUtil.isAncestor(basePath, file)) {
				SysUtil.accessDenied(request);
			}

			String fileData = FileUtil.readString(file);
			if (StringUtil.isEmpty(fileData)) {
				fileData = "{}";
			}

			checkPerm(request, new JSONObject(fileData), "read");
			JSONObject content = new JSONObject();
			content.put("file", FileUtil.getPath(file));
			content.put("data", fileData);
			result.put(content);
		}

		WebUtil.send(response, result);
	}

	public static void setPerm(HttpServletRequest request, HttpServletResponse response) throws Exception {
		JSONArray fileNames = new JSONArray(request.getParameter("fileNames"));
		JSONObject perms = new JSONObject(request.getParameter("perms"));
		int j = fileNames.length();

		for (int i = 0; i < j; ++i) {
			File file = new File(basePath, fileNames.getString(i));
			doSetPerm(request, file, perms);
		}

	}

	private static void doSetPerm(HttpServletRequest request, File file, JSONObject perms) throws Exception {
		if (file.isDirectory()) {
			File[] fs = file.listFiles();
			File[] var7 = fs;
			int var6 = fs.length;

			for (int var5 = 0; var5 < var6; ++var5) {
				File f = var7[var5];
				doSetPerm(request, f, perms);
			}
		} else {
			JSONObject content = JsonUtil.readObject(file);
			content.put("roles", perms);
			saveFile(request, file, content, false);
		}

	}

	public static JSONObject getExecuteObject(HttpServletRequest request, File file) throws Exception {
		if (!FileUtil.isAncestor(basePath, file)) {
			SysUtil.accessDenied(request);
		}

		JSONObject content = JsonUtil.readObject(file);
		checkPerm(request, content, "execute");
		return content;
	}

	private static void saveFile(HttpServletRequest request, File file, JSONObject content, boolean reservePerm)
			throws Exception {
		if (!FileUtil.isAncestor(basePath, file)) {
			SysUtil.accessDenied(request);
		}

		if (file.exists()) {
			JSONObject oldContent = JsonUtil.readObject(file);
			checkPerm(request, oldContent, "write");
			if (reservePerm) {
				content.put("roles", oldContent.opt("roles"));
			}
		}

		FileUtil.syncSave(file, content.toString());
	}

	private static boolean isLeafFolder(HttpServletRequest request, File file) throws Exception {
		File[] files = FileUtil.listFiles(file);
		File[] var6 = files;
		int var5 = files.length;

		for (int var4 = 0; var4 < var5; ++var4) {
			File subFile = var6[var4];
			if (subFile.isDirectory() && canDisplay(request, subFile)) {
				return false;
			}
		}

		return true;
	}

	private static boolean canDisplay(HttpServletRequest request, File file) throws Exception {
		if (WebUtil.hasRole(request, "admin")) {
			return true;
		} else {
			boolean forceShowFolder = Boolean.parseBoolean(request.getParameter("forceShowFolder"));
			if (file.isDirectory()) {
				if (forceShowFolder) {
					return true;
				}

				File[] files = FileUtil.listFiles(file);
				File[] var7 = files;
				int var6 = files.length;

				for (int var5 = 0; var5 < var6; ++var5) {
					File subFile = var7[var5];
					if (canDisplay(request, subFile)) {
						return true;
					}
				}
			} else {
				String ext = FileUtil.getFileExt(file);
				if (StringUtil.indexOf(resourceExt, ext) == -1) {
					return true;
				}

				JSONObject content = JsonUtil.readObject(file);
				if (hasPerm(request, content, "get")) {
					return true;
				}
			}

			return false;
		}
	}
}