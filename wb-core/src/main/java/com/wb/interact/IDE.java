package com.wb.interact;

import com.wb.common.Base;
import com.wb.common.Session;
import com.wb.common.UrlBuffer;
import com.wb.common.Value;
import com.wb.common.Var;
import com.wb.common.XwlBuffer;
import com.wb.tool.Console;
import com.wb.tool.Encrypter;
import com.wb.tool.ScriptCompressor;
import com.wb.util.DateUtil;
import com.wb.util.FileUtil;
import com.wb.util.JsonUtil;
import com.wb.util.SortUtil;
import com.wb.util.StringUtil;
import com.wb.util.SysUtil;
import com.wb.util.WebUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.CollationKey;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class IDE {
	private static final String[] imageTypes = new String[]{"gif", "jpg", "png", "bmp"};

	public static void getList(HttpServletRequest request, HttpServletResponse response) throws Exception {
		if ("root".equals(request.getParameter("node"))) {
			getBaseList(request, response);
		} else if ("module".equals(request.getParameter("type"))) {
			getModuleList(request, response);
		} else {
			getFileList(request, response);
		}

	}

	public static void getFileList(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String path = request.getParameter("path");
		String type = request.getParameter("type");
		JSONArray fileArray = new JSONArray();
		String mode = request.getParameter("mode");
		boolean isIde = StringUtil.isEmpty(mode);
		boolean isTouch = "1".equals(mode);
		boolean isTree = "2".equals(mode);
		boolean isGrid = "3".equals(mode);
		File base;
		File[] files;
		if (StringUtil.isEmpty(path)) {
			limitDemoUser(request, (File) null);
			base = null;
			files = File.listRoots();
		} else {
			base = new File(path);
			limitDemoUser(request, base);
			files = FileUtil.listFiles(base);
		}

		if (isGrid) {
			String[] sortInfo = WebUtil.getSortInfo(request);
			String[] fields = new String[]{"text", "size", "type", "date"};
			SortUtil.sort(files, StringUtil.indexOf(fields, sortInfo[0]), sortInfo[1].equalsIgnoreCase("desc"));
		} else {
			SortUtil.sort(files);
		}

		File[] var18 = files;
		int var17 = files.length;

		for (int var20 = 0; var20 < var17; ++var20) {
			File file = var18[var20];
			boolean isDir = file.isDirectory();
			if (!isTree || isDir) {
				String fileDir = FileUtil.getPath(file);
				if ((!isIde || !"app".equals(type) || !file.equals(Base.modulePath))
						&& (!"file".equals(type) || !file.equals(Base.path))) {
					JSONObject fileObject = new JSONObject();
					fileObject.put("text", StringUtil.select(new String[]{file.getName(), fileDir}));
					if (isDir) {
						if (isTouch || isGrid) {
							fileObject.put("icon", "wb/images/folder.png");
						}

						if (isIde && FileUtil.isEmpty(file) || isTree && !FileUtil.hasFolder(file)) {
							fileObject.put("children", new JSONArray());
						}
					} else {
						fileObject.put("size", file.length());
						fileObject.put("leaf", true);
						if (isGrid) {
							fileObject.put("icon", WebUtil.encode(fileDir));
						} else if (!isTree) {
							fileObject.put("icon", "m?xwl=dev/ide/get-file-icon&file=" + WebUtil.encode(fileDir));
						}
					}

					if (isGrid || isTouch) {
						fileObject.put("date", new Date(file.lastModified()));
						fileObject.put("type", isDir ? "文件夹" : FileUtil.getFileType(file));
					}

					fileArray.put(fileObject);
				}
			}
		}

		WebUtil.send(response, (new JSONObject()).put(isGrid ? "rows" : "children", fileArray));
	}

	private static void getModuleList(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String path = request.getParameter("path");
		String[] roles = Session.getRoles(request);
		File base = new File(path);
		if (!FileUtil.isAncestor(Base.path, base)) {
			SysUtil.accessDenied(request);
		}

		ArrayList<Entry<String, Integer>> fileNames = getSortedFile(base);
		JSONArray fileArray = new JSONArray();
		Iterator var16 = fileNames.iterator();

		while (var16.hasNext()) {
			Entry<String, Integer> entry = (Entry) var16.next();
			String fileName = (String) entry.getKey();
			if (!"folder.json".equalsIgnoreCase(fileName)) {
				File file = new File(base, fileName);
				if (file.exists() && XwlBuffer.canDisplay(file, roles, 5)) {
					boolean isFolder = file.isDirectory();
					JSONObject content = null;
					if (isFolder) {
						File configFile = new File(file, "folder.json");
						if (configFile.exists()) {
							content = JsonUtil.readObject(configFile);
						}
					} else if (fileName.endsWith(".xwl")) {
						content = XwlBuffer.get(file);
					}

					if (content == null) {
						content = new JSONObject();
						if (!isFolder) {
							content.put("icon",
									"m?xwl=dev/ide/get-file-icon&file=" + WebUtil.encode(FileUtil.getPath(file)));
						}
					}

					JSONObject fileObject = new JSONObject();
					fileObject.put("text", fileName);
					fileObject.put("title", content.optString("title"));
					boolean hidden = Boolean.TRUE.equals(content.opt("hidden"));
					fileObject.put("hidden", hidden);
					if (hidden) {
						fileObject.put("cls", "x-highlight");
					}

					fileObject.put("inframe", Boolean.TRUE.equals(content.opt("inframe")));
					fileObject.put("pageLink", content.optString("pageLink"));
					String iconCls = content.optString("iconCls");
					if (!StringUtil.isEmpty(iconCls)) {
						fileObject.put("iconCls", iconCls);
					}

					String icon = content.optString("icon");
					if (!StringUtil.isEmpty(icon)) {
						fileObject.put("icon", icon);
					}

					if (isFolder) {
						if (!hasChildren(file)) {
							fileObject.put("children", new JSONArray());
						}
					} else {
						fileObject.put("leaf", true);
					}

					fileArray.put(fileObject);
				}
			}
		}

		WebUtil.send(response, (new JSONObject()).put("children", fileArray));
	}

	private static void getBaseList(HttpServletRequest request, HttpServletResponse response) throws Exception {
		JSONArray list = new JSONArray();
		JSONObject node = new JSONObject();
		String sysFolderBase = Var.getString("sys.ide.sysFolderBase");
		node.put("text", "模块");
		node.put("iconCls", "module_icon");
		node.put("expanded", true);
		node.put("base", FileUtil.getPath(Base.modulePath) + '/');
		node.put("type", "module");
		list.put(node);
		node = new JSONObject();
		node.put("text", "应用");
		node.put("iconCls", "application_icon");
		node.put("base", FileUtil.getPath(Base.path) + '/');
		node.put("type", "app");
		list.put(node);
		if (!sysFolderBase.equals("app")) {
			node = new JSONObject();
			node.put("text", "系统");
			node.put("iconCls", "system_icon");
			if (sysFolderBase.equals("server")) {
				sysFolderBase = FileUtil.getPath(Base.path.getParentFile().getParent()) + '/';
			} else {
				sysFolderBase = "";
			}

			node.put("base", sysFolderBase);
			node.put("type", "file");
			list.put(node);
		}

		WebUtil.send(response, list);
	}

	public static synchronized void setProperty(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		JSONObject map = WebUtil.fetch(request);
		File configFile = null;
		File oldFile = new File(map.getString("path"));
		String url = map.optString("url");
		String oldName = oldFile.getName();
		String newName = map.getString("text");
		boolean nameModified = !oldName.equals(newName);
		boolean urlValid = map.optBoolean("urlValid");
		JSONObject indexContent = null;
		boolean isDir = oldFile.isDirectory();
		if (urlValid && !UrlBuffer.exists(url, oldFile)) {
			throw new IllegalArgumentException("URL捷径 \"" + url + "\" 已经存在。");
		} else {
			File newFile;
			if (nameModified) {
				newFile = new File(oldFile.getParent(), newName);
			} else {
				newFile = oldFile;
			}

			if (nameModified) {
				FileUtil.syncRename(oldFile, newFile);
			}

			File indexFile = new File(newFile.getParentFile(), "folder.json");
			boolean indexFileExists = indexFile.exists();
			boolean isModule = map.getBoolean("isModule");
			boolean needConfig = isModule || indexFileExists;
			if (needConfig) {
				if (indexFileExists) {
					indexContent = JsonUtil.readObject(indexFile);
				} else if (isModule) {
					indexContent = new JSONObject();
					indexContent.put("index", new JSONArray());
				}

				if (isModule) {
					JSONObject content;
					if (isDir) {
						configFile = new File(newFile, "folder.json");
						if (configFile.exists()) {
							content = JsonUtil.readObject(configFile);
						} else {
							content = new JSONObject();
							content.put("index", new JSONArray());
						}
					} else {
						content = JsonUtil.readObject(newFile);
					}

					content.put("title", map.getString("title"));
					content.put("iconCls", map.getString("iconCls"));
					content.put("hidden", map.getBoolean("hidden"));
					if (isDir) {
						FileUtil.syncSave(configFile, content.toString());
					} else {
						content.put("inframe", map.getBoolean("inframe"));
						content.put("pageLink", map.getString("pageLink"));
						updateModule(newFile, content, (String[]) null, false);
					}
				}

				if (nameModified && indexFileExists) {
					JSONArray idxArray = indexContent.getJSONArray("index");
					int index = idxArray.indexOf(oldName);
					if (index != -1) {
						idxArray.put(index, newName);
						FileUtil.syncSave(indexFile, indexContent.toString());
					}
				}
			}

			if (isModule) {
				String newRelPath = FileUtil.getModulePath(newFile);
				String oldRelPath = nameModified ? FileUtil.getModulePath(oldFile) : newRelPath;
				boolean changed = false;
				if (isDir) {
					if (nameModified && UrlBuffer.change(oldRelPath, newRelPath, isDir)) {
						changed = true;
					}
				} else {
					if (UrlBuffer.remove(oldRelPath)) {
						changed = true;
					}

					if (urlValid && !url.isEmpty()) {
						UrlBuffer.put('/' + url, newRelPath);
						changed = true;
					}
				}

				if (changed) {
					UrlBuffer.save();
				}
			}

			JSONObject result = new JSONObject();
			result.put("lastModified", DateUtil.getTimestamp(newFile.lastModified()));
			result.put("path", FileUtil.getPath(newFile));
			if (nameModified) {
				JSONObject resp = new JSONObject();
				JSONArray src = new JSONArray();
				JSONArray moveTo = new JSONArray();
				src.put(FileUtil.getPath(oldFile));
				moveTo.put(FileUtil.getPath(newFile));
				Object[] changeInfo = changePath(src, moveTo);
				resp.put("files", changeInfo[0]);
				resp.put("change", changeInfo[1]);
				resp.put("moveTo", moveTo);
				result.put("refactorInfo", resp);
			}

			WebUtil.send(response, result);
		}
	}

	public static void total(HttpServletRequest request, HttpServletResponse response) throws Exception {
		File file = new File(request.getParameter("path"));
		JSONObject result = new JSONObject();
		boolean isDir = file.isDirectory();
		result.put("lastModified", DateUtil.getTimestamp(file.lastModified()));
		result.put("fileSize", file.length());
		if (isDir && FileUtil.isAncestor(Base.path, file)) {
			int[] info = new int[4];
			total(file, info);
			result.put("total", info);
		}

		if (!isDir && FileUtil.isAncestor(Base.modulePath, file) && file.getName().endsWith(".xwl")) {
			result.put("url", UrlBuffer.find(file));
		}

		WebUtil.send(response, result);
	}

	private static void total(File folder, int[] info) {
		File[] files = FileUtil.listFiles(folder);
		File[] var6 = files;
		int var5 = files.length;

		for (int var4 = 0; var4 < var5; ++var4) {
			File file = var6[var4];
			if (file.isDirectory()) {
				++info[2];
				total(file, info);
			} else {
				if (file.getName().endsWith(".xwl")) {
					++info[0];
				}

				++info[1];
				info[3] = (int) ((long) info[3] + file.length());
			}
		}

	}

	public static void search(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String searchType = request.getParameter("searchType");
		if ("shortcut".equals(searchType)) {
			searchShortcut(request, response);
		} else if ("bug".equals(searchType)) {
			searchBug(request, response);
		} else if ("filelist".equals(searchType)) {
			searchFile(request, response);
		} else if ("duplicate".equals(searchType)) {
			searchDuplicateFiles(request, response);
		} else {
			JSONArray pathList = new JSONArray(request.getParameter("pathList"));
			String searchText = request.getParameter("search");
			String[] filePatterns = StringUtil.split(request.getParameter("filePatterns"), ',', true);
			boolean whole = Boolean.parseBoolean(request.getParameter("whole"));
			boolean isReplace = Boolean.parseBoolean(request.getParameter("isReplace"));
			Pattern searchPattern = null;
			Pattern xwlPattern = null;
			int pathListLen = pathList.length();
			ArrayList<File> searchedFiles = new ArrayList(pathListLen);
			JSONArray rows = new JSONArray();
			if (Boolean.parseBoolean(request.getParameter("regularExp"))) {
				searchPattern = Pattern.compile(searchText);
				xwlPattern = Pattern.compile(StringUtil.text(searchText));
			} else {
				searchPattern = Pattern.compile(StringUtil
						.concat(new String[]{Boolean.parseBoolean(request.getParameter("caseSensitive")) ? "" : "(?i)",
								whole ? "\\b" : "", "\\Q", searchText, "\\E", whole ? "\\b" : ""}));
				xwlPattern = Pattern.compile(StringUtil
						.concat(new String[]{Boolean.parseBoolean(request.getParameter("caseSensitive")) ? "" : "(?i)",
								whole ? "\\b" : "", "\\Q", StringUtil.text(searchText), "\\E", whole ? "\\b" : ""}));
			}

			for (int i = 0; i < pathListLen; ++i) {
				File file = new File(pathList.getString(i));
				if (!FileUtil.isAncestor(Base.path, file)) {
					throw new IllegalArgumentException("禁止检索应用目录之外的文件。");
				}

				boolean searched = false;
				Iterator var17 = searchedFiles.iterator();

				while (var17.hasNext()) {
					File f = (File) var17.next();
					if (FileUtil.isAncestor(f, file)) {
						searched = true;
						break;
					}
				}

				if (!searched) {
					doSearch(file, rows, searchPattern, xwlPattern, filePatterns,
							isReplace ? request.getParameter("replace") : null);
					searchedFiles.add(file);
				}
			}

			WebUtil.send(response, (new JSONObject()).put("rows", rows));
		}
	}

	private static boolean doSearch(File file, JSONArray rows, Pattern pattern, Pattern xwlPattern,
			String[] filePatterns, String replaceText) throws Exception {
		if (file.isDirectory()) {
			File[] files = FileUtil.listFiles(file);
			File[] var11 = files;
			int var10 = files.length;

			for (int var9 = 0; var9 < var10; ++var9) {
				File f = var11[var9];
				boolean match;
				if (filePatterns.length != 0 && !f.isDirectory()) {
					match = false;
					String[] var15 = filePatterns;
					int var14 = filePatterns.length;

					for (int var13 = 0; var13 < var14; ++var13) {
						String filePattern = var15[var13];
						if (FilenameUtils.wildcardMatch(f.getName(), filePattern, IOCase.SYSTEM)) {
							match = true;
							break;
						}
					}
				} else {
					match = true;
				}

				if (match && !doSearch(f, rows, pattern, xwlPattern, filePatterns, replaceText)) {
					return false;
				}
			}
		} else {
			if (debugVersionExists(file)) {
				return true;
			}

			String text = FileUtil.readString(file);
			String path = FileUtil.getPath(file);
			if (replaceText == null) {
				if (path.endsWith(".xwl")) {
					if (!searchXwl(text, path, pattern, rows)) {
						return false;
					}
				} else if (!searchText(text, path, pattern, rows, (String) null, (String) null)) {
					return false;
				}
			} else {
				String replacedText;
				if (path.endsWith(".xwl")) {
					replacedText = xwlPattern.matcher(text)
							.replaceAll(Matcher.quoteReplacement(StringUtil.text(replaceText)));
				} else {
					replacedText = pattern.matcher(text).replaceAll(Matcher.quoteReplacement(replaceText));
				}

				if (!replacedText.equals(text)) {
					FileUtil.syncSave(file, replacedText);
					JSONObject row = new JSONObject();
					row.put("content", "替换：" + replaceText);
					row.put("path", path);
					row.put("lastModified", DateUtil.getTimestamp(file.lastModified()));
					rows.put(row);
				}
			}
		}

		return true;
	}

	private static boolean searchText(String text, String path, Pattern pattern, JSONArray rows, String nodePath,
			String itemName) {
		int lastPos = 0;
		int line = 1;
		int textLength = text.length();

		int pos;
		int matchTextLength;
		int var10000;
		for (Matcher matcher = pattern.matcher(text); matcher.find(); var10000 = pos + matchTextLength) {
			JSONObject row = new JSONObject();
			if (checkOutOfRange(rows, row)) {
				return false;
			}

			pos = matcher.start();
			matchTextLength = matcher.end() - pos;
			int afterTextPos = pos + matchTextLength;
			int minPos = Math.min(textLength, pos + 70);
			if (afterTextPos > minPos) {
				return true;
			}

			row.put("content",
					StringUtil.concat(new String[]{
							StringUtil.toHTML(text.substring(Math.max(0, pos - 30), pos), false, false), "<strong>",
							StringUtil.toHTML(text.substring(pos, afterTextPos), false, false), "</strong>",
							afterTextPos >= textLength
									? ""
									: StringUtil.toHTML(text.substring(afterTextPos, minPos), false, false)}));
			int[] lineInfo = StringUtil.stringOccur(text, '\n', lastPos, pos);
			line += lineInfo[0];
			lastPos = pos;
			row.put("path", path);
			row.put("line", line);
			row.put("ch", pos - lineInfo[1]);
			row.put("nodePath", nodePath);
			row.put("itemName", itemName);
			rows.put(row);
		}

		return true;
	}

	public static void searchShortcut(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String shortcut = request.getParameter("shortcut");
		Set<Entry<String, String>> es = UrlBuffer.buffer.entrySet();
		JSONArray ja = new JSONArray();
		Iterator var8 = es.iterator();

		while (var8.hasNext()) {
			Entry<String, String> e = (Entry) var8.next();
			String key = ((String) e.getKey()).substring(1);
			if (FilenameUtils.wildcardMatch(key, shortcut, IOCase.INSENSITIVE)) {
				JSONObject jo = new JSONObject();
				if (checkOutOfRange(ja, jo)) {
					break;
				}

				jo.put("content", key);
				jo.put("path", FileUtil.getPath(new File(Base.modulePath, (String) e.getValue())));
				jo.put("line", 1);
				jo.put("ch", 0);
				ja.put(jo);
			}
		}

		WebUtil.send(response, (new JSONObject()).put("rows", ja));
	}

	public static void searchBug(HttpServletRequest request, HttpServletResponse response) throws Exception {
		JSONArray ja = new JSONArray();
		scanBug(Base.modulePath, ja);
		WebUtil.send(response, (new JSONObject()).put("rows", ja));
	}

	private static boolean scanBug(File path, JSONArray ja) throws Exception {
		File[] fs = FileUtil.listFiles(path);
		File[] var7 = fs;
		int var6 = fs.length;

		for (int var5 = 0; var5 < var6; ++var5) {
			File file = var7[var5];
			if (file.getName().endsWith(".xwl")) {
				JSONObject fileObj = XwlBuffer.get(file);
				if (scanProp(ja, fileObj.getJSONArray("children").getJSONObject(0), FileUtil.getPath(file), "")) {
					return true;
				}
			}

			if (file.isDirectory() && scanBug(file, ja)) {
				return true;
			}
		}

		return false;
	}

	private static boolean scanProp(JSONArray ja, JSONObject control, String path, String nodePath) {
		JSONObject configs = (JSONObject) control.opt("configs");
		String[] sqlProps = new String[]{"sql", "totalSql"};
		nodePath = nodePath + "/" + configs.getString("itemId");
		if (configs != null) {
			String[] var15 = sqlProps;
			int var14 = sqlProps.length;

			for (int var13 = 0; var13 < var14; ++var13) {
				String sqlProp = var15[var13];
				String val = configs.optString(sqlProp);
				if (!val.isEmpty()) {
					String rawVal = val;
					val = StringUtil.replaceAll(val, "{#sql.orderBy#}", "");
					val = StringUtil.replaceAll(val, "{#sql.orderFields#}", "");
					if (val.indexOf("{#") != -1) {
						JSONObject jo = new JSONObject();
						if (checkOutOfRange(ja, jo)) {
							return true;
						}

						jo.put("content", "(SQL注入漏洞) " + rawVal);
						jo.put("path", path);
						jo.put("line", 1);
						jo.put("ch", 0);
						jo.put("nodePath", nodePath);
						jo.put("itemName", "Configs=" + sqlProp);
						ja.put(jo);
					}
				}
			}
		}

		JSONArray children = (JSONArray) control.opt("children");
		if (children != null) {
			int j = children.length();

			for (int i = 0; i < j; ++i) {
				if (scanProp(ja, children.optJSONObject(i), path, nodePath)) {
					return true;
				}
			}
		}

		return false;
	}

	private static boolean checkOutOfRange(JSONArray ja, JSONObject jo) {
		if (ja.length() > 998) {
			jo.put("content", "超过1000项被搜索到，停止搜索。");
			ja.put(jo);
			return true;
		} else {
			return false;
		}
	}

	public static void searchFile(HttpServletRequest request, HttpServletResponse response) throws Exception {
		JSONArray array = new JSONArray();
		String query = request.getParameter("query").toLowerCase();
		boolean searchList = Boolean.parseBoolean(request.getParameter("searchList"));
		String lastModifiedStr = searchList ? request.getParameter("lastModified") : "-1";
		long lastModified = "-1".equals(lastModifiedStr) ? -1L : DateUtil.strToDate(lastModifiedStr).getTime();
		if (query.isEmpty()) {
			query = "*.xwl";
		}

		if (!searchList) {
			query = "*" + query + "*";
		}

		doSearchFile(Base.path, query, lastModified, array);
		WebUtil.send(response, (new JSONObject()).put("rows", array));
	}

	private static boolean doSearchFile(File folder, String searchName, long lastModified, JSONArray array)
			throws Exception {
		File[] files = FileUtil.listFiles(folder);
		File[] var11 = files;
		int var10 = files.length;

		for (int var9 = 0; var9 < var10; ++var9) {
			File file = var11[var9];
			if (file.isDirectory()) {
				if (doSearchFile(file, searchName, lastModified, array)) {
					return true;
				}
			} else {
				String path = FileUtil.getPath(file);
				String name = path.substring(path.lastIndexOf(47) + 1);
				if (!name.equals("folder.json")
						&& (searchName.isEmpty() || FilenameUtils.wildcardMatch(name, searchName, IOCase.SYSTEM))
						&& (lastModified == -1L || file.lastModified() >= lastModified)) {
					JSONObject jo = new JSONObject();
					jo.put("path", path);
					jo.put("content", name);
					jo.put("line", 1);
					jo.put("ch", 0);
					array.put(jo);
					if (array.length() > 99) {
						return true;
					}
				}
			}
		}

		return false;
	}

	private static boolean debugVersionExists(File file) {
		if (Var.getBool("sys.ide.searchIgnoreRelease")) {
			String filename = file.getName();
			File debugFile = new File(file.getParentFile(),
					FileUtil.removeExtension(filename) + "-debug." + FileUtil.getFileExt(filename));
			return debugFile.exists();
		} else {
			return false;
		}
	}

	private static boolean searchXwl(String text, String path, Pattern pattern, JSONArray rows) {
		JSONObject xwl;
		try {
			xwl = new JSONObject(text);
		} catch (Exception var6) {
			throw new RuntimeException("文件“" + path + "”不是一个有效的模块文件。");
		}

		return scanXwl(xwl, path, pattern, rows, "");
	}

	private static boolean scanXwl(JSONObject xwl, String path, Pattern pattern, JSONArray rows, String nodePath) {
		JSONArray children = xwl.optJSONArray("children");
		if (children != null) {
			int j = children.length();

			for (int i = 0; i < j; ++i) {
				JSONObject jo = children.getJSONObject(i);
				JSONObject configs = jo.getJSONObject("configs");
				String currentPath = nodePath + "/" + configs.getString("itemId");
				if (!scanItems(configs, path, pattern, rows, currentPath, "Configs")) {
					return false;
				}

				if (jo.has("events")
						&& !scanItems(jo.getJSONObject("events"), path, pattern, rows, currentPath, "Events")) {
					return false;
				}

				if (!scanXwl(jo, path, pattern, rows, currentPath)) {
					return false;
				}
			}
		}

		return true;
	}

	private static boolean scanItems(JSONObject jo, String path, Pattern pattern, JSONArray rows, String nodePath,
			String type) {
		Set<Entry<String, Object>> items = jo.entrySet();
		Iterator var9 = items.iterator();

		while (var9.hasNext()) {
			Entry<String, Object> item = (Entry) var9.next();
			String key = (String) item.getKey();
			if (!searchText(key, path, pattern, rows, nodePath, type + "=" + key)) {
				return false;
			}

			if (!searchText(item.getValue().toString(), path, pattern, rows, nodePath, type + "=" + key)) {
				return false;
			}
		}

		return true;
	}

	private static boolean hasChildren(File dir) {
		File[] files = FileUtil.listFiles(dir);
		if (files == null) {
			return false;
		} else {
			File[] var5 = files;
			int var4 = files.length;

			for (int var3 = 0; var3 < var4; ++var3) {
				File file = var5[var3];
				if (file.isDirectory()) {
					return true;
				}

				if (!file.getName().equals("folder.json")) {
					return true;
				}
			}

			return false;
		}
	}

	public static synchronized void addModule(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		String name = request.getParameter("name");
		String title = request.getParameter("title");
		String iconCls = request.getParameter("iconCls");
		String url = null;
		boolean hidden = Boolean.parseBoolean(request.getParameter("hidden"));
		boolean isDir = Boolean.parseBoolean(request.getParameter("isDir"));
		JSONObject content = new JSONObject();
		JSONObject moduleMeta = Controls.get("module").optJSONObject("configs");
		content.put("title", title);
		content.put("iconCls", iconCls);
		content.put("hidden", hidden);
		if (!isDir) {
			content.put("roles", (new JSONObject()).put("default", 1));
			content.put("inframe", Boolean.parseBoolean(request.getParameter("inframe")));
			content.put("pageLink", request.getParameter("pageLink"));
			url = request.getParameter("url");
			if (!UrlBuffer.exists(url, (File) null)) {
				throw new IllegalArgumentException("URL捷径 \"" + url + "\" 已经存在。");
			}

			if (!name.endsWith(".xwl")) {
				if (name.toLowerCase().endsWith(".xwl")) {
					name = name.substring(0, name.length() - 3) + "xwl";
				} else {
					name = name + ".xwl";
				}
			}

			JSONObject module = new JSONObject();
			JSONObject moduleConfigs = new JSONObject();
			moduleConfigs.put("itemId", "module");
			Set<Entry<String, Object>> moduleConfigEntries = moduleMeta.entrySet();
			Iterator var16 = moduleConfigEntries.iterator();

			while (var16.hasNext()) {
				Entry<String, Object> entry = (Entry) var16.next();
				Object value = ((JSONObject) entry.getValue()).opt("value");
				if (value != null) {
					moduleConfigs.put((String) entry.getKey(), value.toString());
				}
			}

			module.put("children", new JSONArray());
			module.put("configs", moduleConfigs);
			module.put("type", "module");
			content.put("children", (new JSONArray()).put(module));
		}

		File base = new File(request.getParameter("path"));
		File file = addModule(base, name, isDir, content);
		setFileIndex(base, request.getParameter("indexName"), (new JSONArray()).put(name),
				request.getParameter("type"));
		if (isDir) {
			JSONObject fileInfo = new JSONObject();
			fileInfo.put("file", name);
			fileInfo.put("title", title);
			fileInfo.put("iconCls", iconCls);
			fileInfo.put("hidden", hidden);
			WebUtil.send(response, fileInfo);
		} else {
			if (!url.isEmpty()) {
				UrlBuffer.put('/' + url, FileUtil.getModulePath(file));
				UrlBuffer.save();
			}

			doOpen((new JSONArray()).put(FileUtil.getPath(file)), (String) null, (String) null, request, response);
		}

	}

	private static void addCRUDFrame(String name, String title, String iconCls, File path, String tableName,
			String keyField, String codeField, String nameField, boolean isDialog, boolean hasUpload) throws Exception {
		File destFolder = new File(path, name);
		File destModule = new File(path, name + ".xwl");
		if (destModule.exists()) {
			throw new RuntimeException("模块“" + name + ".xwl”已经存在。");
		} else if (destFolder.exists()) {
			throw new RuntimeException("目录“" + name + "”已经存在。");
		} else {
			if (!StringUtil.isEmpty(codeField) && StringUtil.isEmpty(nameField)) {
				nameField = codeField;
				codeField = null;
			}

			File sourcePath;
			if (isDialog) {
				if (hasUpload) {
					sourcePath = new File(Base.path, "wb/system/template/dialogEditFile");
				} else {
					sourcePath = new File(Base.path, "wb/system/template/dialogEdit");
				}
			} else {
				sourcePath = new File(Base.path, "wb/system/template/gridEdit");
			}

			FileUtil.syncCopyA(new File(sourcePath, "main.xwl"), destModule);
			FileUtil.syncCopyA(new File(sourcePath, "main"), destFolder);
			File file = new File(path, "folder.json");
			JSONObject json = JsonUtil.readObject(file);
			JSONArray array = json.optJSONArray("index");
			array.put(name + ".xwl");
			array.put(name);
			FileUtil.syncSave(file, json.toString());
			file = new File(destFolder, "folder.json");
			json = JsonUtil.readObject(file);
			json.put("title", title);
			FileUtil.syncSave(file, json.toString());
			String baseUrl = FileUtil.getModuleUrl(destModule) + "/";
			String content = FileUtil.readString(destModule);
			json = new JSONObject();
			json.put("selectUrl", baseUrl + "select");
			json.put("searchUrl", baseUrl + "search");
			json.put("title", title);
			json.put("iconCls", iconCls);
			if (isDialog) {
				json.put("insertUrl", baseUrl + "insert");
				json.put("updateUrl", baseUrl + "update");
				json.put("deleteUrl", baseUrl + "delete");
			} else {
				json.put("saveUrl", baseUrl + "save");
			}

			json.put("nameField", nameField);
			json.put("codeField", codeField);
			boolean emptyCodeField = StringUtil.isEmpty(codeField);
			boolean hasSearch = !emptyCodeField || !StringUtil.isEmpty(nameField);
			if (hasSearch) {
				if (emptyCodeField) {
					json.put("searchKeyField", nameField);
				} else {
					json.put("searchKeyField", codeField);
					json.put("codeFieldExp", codeField + ", ");
				}
			}

			content = StringUtil.replaceParams(json, content);
			JSONObject module = new JSONObject(content);
			JSONArray mainBar = module.getJSONArray("children").getJSONObject(0).optJSONArray("children")
					.getJSONObject(isDialog ? 1 : 0).optJSONArray("children").getJSONObject(0).optJSONArray("children");
			if (hasSearch) {
				if (emptyCodeField) {
					JSONObject comboNode = mainBar.getJSONObject(isDialog ? 4 : 5);
					JSONObject configs = comboNode.getJSONObject("configs");
					configs.remove("tpl");
					configs.put("emptyText", "名称");
					json.put("whereClause", " where " + nameField + " like {?searchText?}");
				} else {
					json.put("whereClause",
							" where " + codeField + " like {?searchText?} or " + nameField + " like {?searchText?}");
				}
			} else {
				if (!isDialog) {
					mainBar.remove(6);
				}

				mainBar.remove(5);
				mainBar.remove(4);
				if (isDialog) {
					mainBar.remove(3);
				}

				(new File(destFolder, "search.xwl")).delete();
			}

			FileUtil.syncSave(destModule, module.toString());
			json.put("tableName", tableName);
			json.put("keyField", keyField);
			if (hasSearch) {
				file = new File(destFolder, "search.xwl");
				FileUtil.syncSave(file, StringUtil.replaceParams(json, FileUtil.readString(file)));
			}

			file = new File(destFolder, "select.xwl");
			FileUtil.syncSave(file, StringUtil.replaceParams(json, FileUtil.readString(file)));
			if (isDialog) {
				file = new File(destFolder, "delete.xwl");
				FileUtil.syncSave(file, StringUtil.replaceParams(json, FileUtil.readString(file)));
				file = new File(destFolder, "insert.xwl");
				FileUtil.syncSave(file, StringUtil.replaceParams(json, FileUtil.readString(file)));
				file = new File(destFolder, "update.xwl");
				FileUtil.syncSave(file, StringUtil.replaceParams(json, FileUtil.readString(file)));
			} else {
				file = new File(destFolder, "save.xwl");
				FileUtil.syncSave(file, StringUtil.replaceParams(json, FileUtil.readString(file)));
			}

		}
	}

	private static void addSimpleFrame(String name, String title, String iconCls, File path, String baseUrl)
			throws Exception {
		File destModule = new File(path, name + ".xwl");
		File sourcePath = new File(Base.path, "wb/system/template/" + baseUrl);
		if (destModule.exists()) {
			throw new RuntimeException("模块“" + name + ".xwl”已经存在。");
		} else {
			FileUtil.syncCopyA(new File(sourcePath, "main.xwl"), destModule);
			File file = new File(path, "folder.json");
			JSONObject json = JsonUtil.readObject(file);
			JSONArray array = json.optJSONArray("index");
			array.put(name + ".xwl");
			array.put(name);
			FileUtil.syncSave(file, json.toString());
			String content = FileUtil.readString(destModule);
			json = new JSONObject();
			json.put("title", title);
			json.put("iconCls", iconCls);
			content = StringUtil.replaceParams(json, content);
			FileUtil.syncSave(destModule, content);
		}
	}

	public static synchronized void addFrame(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		String name = request.getParameter("name");
		String title = request.getParameter("title");
		String iconCls = request.getParameter("iconCls");
		File path = new File(request.getParameter("path"));
		String frameType = request.getParameter("frameType");
		if (!FileUtil.isAncestor(Base.modulePath, path)) {
			throw new Exception("模块文件必须创建于模块目录。");
		} else {
			if ("crud".equals(frameType)) {
				addCRUDFrame(name, title, iconCls, path, request.getParameter("tableName"),
						request.getParameter("keyField"), request.getParameter("codeField"),
						request.getParameter("nameField"), Boolean.parseBoolean(request.getParameter("dialog")),
						Boolean.parseBoolean(request.getParameter("hasUpload")));
			} else if (frameType != null && frameType.startsWith("x")) {
				addSimpleFrame(name, title, iconCls, path, frameType.substring(1));
			}

		}
	}

	private static void setFileIndex(File folder, String indexFileName, JSONArray insertFileNames, String type)
			throws Exception {
		File file = new File(folder, "folder.json");
		int j = insertFileNames.length();
		JSONObject content;
		JSONArray indexArray;
		int index;
		int i;
		if (file.exists()) {
			content = JsonUtil.readObject(file);
			indexArray = content.getJSONArray("index");

			for (i = 0; i < j; ++i) {
				index = indexArray.indexOf(insertFileNames.getString(i));
				if (index != -1) {
					indexArray.remove(index);
				}
			}

			int k = indexArray.length();

			for (i = k - 1; i >= 0; --i) {
				File checkFile = new File(folder, indexArray.getString(i));
				if (!checkFile.exists()) {
					indexArray.remove(i);
				}
			}

			if (!StringUtil.isEmpty(indexFileName) && !"append".equals(type)) {
				index = indexArray.indexOf(indexFileName);
				if (index != -1 && "after".equals(type)) {
					++index;
				}
			} else {
				index = -1;
			}
		} else {
			content = new JSONObject();
			indexArray = new JSONArray();
			content.put("index", indexArray);
			index = -1;
		}

		if (index == -1) {
			for (i = j - 1; i >= 0; --i) {
				indexArray.put(insertFileNames.getString(i));
			}
		} else {
			for (i = 0; i < j; ++i) {
				indexArray.add(index, insertFileNames.getString(i));
			}
		}

		FileUtil.syncSave(file, content.toString());
	}

	private static File addModule(File base, String name, boolean isDir, JSONObject content) throws Exception {
		File file = new File(base, name);
		if (isDir) {
			FileUtil.syncCreate(file, true);
			File configFile = new File(file, "folder.json");
			content.put("index", new JSONArray());
			FileUtil.syncSave(configFile, content.toString());
		} else {
			FileUtil.syncCreate(file, false);
			FileUtil.syncSave(file, content.toString());
		}

		return file;
	}

	public static void getThreadList(HttpServletRequest request, HttpServletResponse response) throws Exception {
      Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
      Thread[] threadArray = (Thread[])threadSet.toArray(new Thread[threadSet.size()]);
      StringBuilder buf = new StringBuilder();
      final Collator collator = Collator.getInstance();
      int daemonThreads = 0;
      Arrays.sort(threadArray, new Comparator<Thread>() {
			public int compare(Thread t1, Thread t2) {
				CollationKey k1 = collator.getCollationKey(StringUtil.opt(
						t1.getName()).toLowerCase());
				CollationKey k2 = collator.getCollationKey(StringUtil.opt(
						t2.getName()).toLowerCase());
				return k1.compareTo(k2);
			}
	  });
      buf.append("<table><tr><td>合计线程：");
      buf.append(threadArray.length);
      buf.append("，守护线程：");
      buf.append(daemonThreads);
      buf.append("，普通线程：");
      buf.append(threadArray.length - daemonThreads);
      buf.append("</td></tr>");
      Thread[] var10 = threadArray;
      int var9 = threadArray.length;

      for(int var8 = 0; var8 < var9; ++var8) {
         Thread thread = var10[var8];
         buf.append("<tr><td");
         if (thread.isDaemon()) {
            ++daemonThreads;
            buf.append(" style=\"color:blue\">");
         } else {
            buf.append(">");
         }

         buf.append(thread.getName());
         buf.append(" (");
         buf.append(thread.getPriority());
         buf.append(")</td></tr>");
      }

      buf.append("</table>");
      WebUtil.send(response, buf);
   }

	public static synchronized void addFile(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String name = request.getParameter("name");
		String path = request.getParameter("path");
		if (path.isEmpty()) {
			throw new IllegalArgumentException("父目录为空，无法创建文件。");
		} else {
			boolean isDir = Boolean.parseBoolean(request.getParameter("isDir"));
			JSONObject fileInfo = new JSONObject();
			File file = addFile(new File(path), name, isDir);
			if (isDir) {
				fileInfo.put("children", new JSONArray());
			} else {
				fileInfo.put("leaf", true);
				fileInfo.put("icon", "m?xwl=dev/ide/get-file-icon&file=" + WebUtil.encode(FileUtil.getPath(file)));
			}

			fileInfo.put("text", name);
			WebUtil.send(response, fileInfo);
		}
	}

	public static void reload(HttpServletRequest request, HttpServletResponse response) {
		SysUtil.reload(1);
	}

	private static File addFile(File base, String name, boolean isDir) throws Exception {
		File file = new File(base, name);
		if (isDir) {
			FileUtil.syncCreate(file, true);
		} else {
			FileUtil.syncCreate(file, false);
		}

		return file;
	}

	public static ArrayList<Entry<String, Integer>> getSortedFile(File dir) throws Exception {
		HashMap<String, Integer> jsonMap = new HashMap();
		String[] fileNames = dir.list();
		SortUtil.sort(fileNames);
		int j = fileNames.length;

		int i;
		for (i = 0; i < j; ++i) {
			jsonMap.put(fileNames[j - i - 1], Integer.MAX_VALUE - i);
		}

		File configFile = new File(dir, "folder.json");
		if (configFile.exists()) {
			JSONArray indexArray = JsonUtil.readObject(configFile).getJSONArray("index");
			j = indexArray.length();

			for (i = 0; i < j; ++i) {
				jsonMap.put(indexArray.getString(i), i);
			}
		}

		return SortUtil.sortValue(jsonMap, true);
	}

	public static synchronized void saveFile(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		boolean confirm = !Boolean.parseBoolean(request.getParameter("noConfirm"));
		JSONArray files = new JSONArray(StringUtil.getString(request.getInputStream()));
		JSONArray lastModifiedData = new JSONArray();
		String filename = null;
		int j = files.length();
		int modifiedCount = 0;

		JSONObject content;
		File file;
		int i;
		for (i = 0; i < j; ++i) {
			content = files.getJSONObject(i);
			file = new File(content.getString("file"));
			if (confirm && file.lastModified() != content.getTimestamp("lastModified").getTime()) {
				if (filename == null) {
					filename = content.getString("file");
				}

				++modifiedCount;
			}
		}

		if (modifiedCount > 0) {
			SysUtil.error("文件 \"" + FileUtil.getFilename(filename) + "\""
					+ (modifiedCount > 1 ? " 等 " + modifiedCount + " 项" : " ") + "已经被修改，确定要保存吗？", "101");
		}

		for (i = 0; i < j; ++i) {
			content = files.getJSONObject(i);
			filename = content.getString("file");
			String saveContent = content.getString("content");
			file = new File(filename);
			String fileExt = FileUtil.getFileExt(filename).toLowerCase();
			if (StringUtil.indexOf(imageTypes, fileExt) == -1) {
				if ("xwl".equals(fileExt)) {
					updateModule(file, new JSONObject(saveContent), (String[]) null, false);
				} else {
					FileUtil.syncSave(file, saveContent, content.optString("charset"));
				}
			} else {
				FileUtil.syncSave(file, StringUtil.decodeBase64(saveContent));
			}

			lastModifiedData.put(DateUtil.getTimestamp(file.lastModified()));
		}

		WebUtil.send(response, lastModifiedData);
	}

	public static synchronized void updateModule(File file, JSONObject moduleData, String[] roles, boolean addRoles)
			throws Exception {
		if (moduleData != null || roles != null) {
			JSONObject data = JsonUtil.readObject(file);
			JSONObject oldRoles = (JSONObject) data.opt("roles");
			if (moduleData != null) {
				data = moduleData;
				moduleData.put("roles", oldRoles);
			}

			if (roles != null) {
				String[] var9 = roles;
				int var8 = roles.length;

				for (int var7 = 0; var7 < var8; ++var7) {
					String role = var9[var7];
					if (addRoles) {
						oldRoles.put(role, 1);
					} else {
						oldRoles.remove(role);
					}
				}
			}

			FileUtil.syncSave(file, data.toString());
		}
	}

	public static void openFile(HttpServletRequest request, HttpServletResponse response) throws Exception {
		doOpen(new JSONArray(request.getParameter("fileNames")), request.getParameter("charset"),
				request.getParameter("type"), request, response);
	}

	private static void doOpen(JSONArray fileNames, String charset, String type, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		JSONArray result = new JSONArray();
		boolean fromEditor = "1".equals(type);
		int j = fileNames.length();

		for (int i = 0; i < j; ++i) {
			String filename = fileNames.getString(i);
			String shortFilename = FileUtil.getFilename(filename);
			String fileExt = FileUtil.getFileExt(filename).toLowerCase();
			File file = new File(filename);
			limitDemoUser(request, file);
			JSONObject content;
			if (!fromEditor && fileExt.equals("xwl")) {
				content = new JSONObject(FileUtil.readString(file));
				fillProperties(content);
				content.put("file", shortFilename);
			} else {
				content = new JSONObject();
				content.put("file", shortFilename);
				content.put("icon", "m?xwl=dev/ide/get-file-icon&file=" + WebUtil.encode(filename));
				String fileText;
				if (StringUtil.indexOf(imageTypes, fileExt) == -1) {
					if (StringUtil.isEmpty(charset)) {
						if (FileUtil.isAncestor(Base.path, file)) {
							charset = "utf-8";
						} else {
							charset = Var.getString("sys.locale.charset");
						}
					} else if ("default".equals(charset)) {
						charset = null;
					}

					if (StringUtil.isEmpty(charset)) {
						fileText = FileUtils.readFileToString(file);
						content.put("charset", "default");
					} else {
						fileText = FileUtils.readFileToString(file, charset);
						content.put("charset", charset);
					}
				} else {
					fileText = StringUtil.encodeBase64(new FileInputStream(file));
				}

				content.put("content", fileText);
			}

			content.put("lastModified", DateUtil.getTimestamp(file.lastModified()));
			content.put("path", filename);
			result.put(content);
		}

		WebUtil.send(response, result);
	}

	private static void fillProperties(JSONObject json) throws IOException {
		JSONArray controls = json.getJSONArray("children");
		int j = controls.length();

		for (int i = 0; i < j; ++i) {
			JSONObject control = controls.getJSONObject(i);
			String type = control.getString("type");
			JSONObject fillControl = Controls.get(type);
			if (fillControl == null) {
				throw new NullPointerException("控件 \"" + type + "\" 没有找到。");
			}

			JSONObject configs = control.optJSONObject("configs");
			if (configs != null) {
				control.put("text", configs.optString("itemId"));
			}

			JSONObject fillGeneral = fillControl.getJSONObject("general");
			control.put("iconCls", fillGeneral.getString("iconCls"));
			if (control.has("children") && control.length() > 0) {
				fillProperties(control);
			}
		}

	}

	public static void getFileIcon(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String fileName = WebUtil.decode(request.getParameter("file"));
		String fileExt = FileUtil.getFileExt(fileName).toLowerCase();
		String[] zipTypes = new String[]{"zip", "rar", "gzip", "gz", "tar", "cab"};
		File file = null;
		if (StringUtil.indexOf(imageTypes, fileExt) != -1) {
			file = new File(fileName);
			limitDemoUser(request, file);
			long fileLen = file.length();
			if (fileLen > 10240L || fileLen == 0L) {
				file = null;
			}
		}

		if (file == null) {
			if (!fileExt.equals("doc") && !fileExt.equals("docx")) {
				if (!fileExt.equals("xls") && !fileExt.equals("xlsx")) {
					if (!fileExt.equals("ppt") && !fileExt.equals("pptx")) {
						if (!fileExt.equals("htm") && !fileExt.equals("html")) {
							if (!fileExt.equals("jar") && !fileExt.equals("war")) {
								if (StringUtil.indexOf(zipTypes, fileExt) != -1) {
									fileName = "file_zip";
								} else if (fileExt.equals("txt")) {
									fileName = "file_txt";
								} else if (fileExt.equals("js")) {
									fileName = "file_js";
								} else if (fileExt.equals("css")) {
									fileName = "file_css";
								} else if (fileExt.equals("java")) {
									fileName = "file_java";
								} else if (fileExt.equals("jsp")) {
									fileName = "file_jsp";
								} else if (fileExt.equals("xml")) {
									fileName = "file_xml";
								} else if (fileExt.equals("xwl")) {
									fileName = "file_xwl";
								} else if (fileExt.equals("flw")) {
									fileName = "workflow";
								} else if (fileExt.equals("rpt")) {
									fileName = "report";
								} else {
									fileName = "file_default";
								}
							} else {
								fileName = "file_jar";
							}
						} else {
							fileName = "web";
						}
					} else {
						fileName = "file_ppt";
					}
				} else {
					fileName = "file_xls";
				}
			} else {
				fileName = "file_doc";
			}

			file = new File(Base.path, StringUtil.concat(new String[]{"wb/images/", fileName + ".png"}));
			response.setContentType("image/gif");
		} else {
			response.setContentType("image/" + fileExt);
		}

		FileInputStream is = new FileInputStream(file);

		try {
			IOUtils.copy(is, response.getOutputStream());
		} finally {
			is.close();
		}

		response.flushBuffer();
	}

	public static synchronized void deleteFiles(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		JSONArray files = new JSONArray(request.getParameter("files"));
		int j = files.length();

		for (int i = 0; i < j; ++i) {
			String filename = files.getString(i);
			File file = new File(filename);
			FileUtil.syncDelete(file, true);
		}

	}

	public static synchronized void setData(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String name = request.getParameter("name");
		String value = request.getParameter("value");
		String sessionName = request.getParameter("sessionName");
		Value.set(request, name, value);
		if (sessionName != null) {
			WebUtil.setSessionValue(request, sessionName, value);
		}

	}

	public static synchronized void moveFiles(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		String isCopyStr = request.getParameter("isCopy");
		boolean fromPaste = !StringUtil.isEmpty(isCopyStr);
		boolean isCopy = "true".equals(isCopyStr);
		boolean confirm = !Boolean.parseBoolean(request.getParameter("noConfirm"));
		JSONArray src = new JSONArray(request.getParameter("src"));
		JSONArray moveTo = null;
		File dstFile = new File(request.getParameter("dst"));
		String dropPosition = request.getParameter("dropPosition");
		String overwriteFilename = null;
		int j = src.length();
		int overwriteCount = 0;
		File folder;
		if (dropPosition.equals("append")) {
			folder = dstFile;
		} else {
			folder = dstFile.getParentFile();
		}

		if (folder == null) {
			throw new Exception("无法复制到此目录。");
		} else {
			int i;
			for (i = 0; i < j; ++i) {
				if (src.getString(i).isEmpty()) {
					throw new IllegalArgumentException("复制源含无效目录。");
				}

				File srcFile = new File(src.getString(i));
				String filename = srcFile.getName();
				File newDstFile = new File(folder, filename);
				if (FileUtil.isAncestor(srcFile, newDstFile, false)) {
					throw new IllegalArgumentException("上级目录 \"" + filename + "\" 不能复制到下级目录。");
				}

				if (newDstFile.exists()) {
					boolean sameFolder = folder.equals(srcFile.getParentFile());
					if (fromPaste) {
						if (!isCopy && sameFolder) {
							throw new IllegalArgumentException("同一目录内剪切 \"" + filename + "\" 无效。");
						}

						if (confirm && !sameFolder) {
							if (overwriteFilename == null) {
								overwriteFilename = filename;
							}

							++overwriteCount;
						}
					} else if (!sameFolder) {
						throw new IllegalArgumentException("\"" + filename + "\" 已经存在。");
					}
				}
			}

			if (overwriteCount > 0) {
				SysUtil.error("\"" + overwriteFilename + "\""
						+ (overwriteCount > 1 ? " 等 " + overwriteCount + " 项" : " ") + "已经存在，确定要覆盖吗？", "101");
			}

			JSONArray srcNames = new JSONArray();
			if (fromPaste) {
				moveTo = copyFiles(src, folder, !isCopy);
				j = moveTo.length();

				for (i = j - 1; i >= 0; --i) {
					Object[] moveInfo = (Object[]) moveTo.get(i);
					if (!(Boolean) moveInfo[1]) {
						srcNames.put(FileUtil.getFilename((String) moveInfo[0]));
					}
				}
			} else {
				for (i = j - 1; i >= 0; --i) {
					srcNames.put(FileUtil.getFilename(src.getString(i)));
					if (folder.equals((new File(src.getString(i))).getParentFile())) {
						src.remove(i);
					}
				}

				moveTo = doMoveFiles(src, folder);
			}

			if ("module".equals(request.getParameter("type"))) {
				setFileIndex(folder, dstFile.getName(), srcNames, dropPosition);
			}

			JSONObject resp = new JSONObject();
			if (!isCopy) {
				Object[] result = changePath(src, moveTo);
				resp.put("files", result[0]);
				resp.put("change", result[1]);
			}

			resp.put("moveTo", moveTo);
			WebUtil.send(response, resp);
		}
	}

	private static Object[] changePath(JSONArray source, JSONArray dest) throws Exception {
		JSONArray rows = new JSONArray();
		ArrayList<Object[]> changes = new ArrayList();
		boolean changed = false;
		int j = source.length();

		for (int i = 0; i < j; ++i) {
			File srcFile = new File(source.getString(i));
			Object value = dest.opt(i);
			File dstFile;
			if (value instanceof String) {
				dstFile = new File((String) value);
			} else {
				dstFile = new File((String) ((Object[]) value)[0]);
			}

			boolean isDir = dstFile.isDirectory();
			if (FileUtil.isAncestor(Base.modulePath, srcFile) && FileUtil.isAncestor(Base.modulePath, dstFile)
					&& (isDir || srcFile.getName().endsWith(".xwl") && dstFile.getName().endsWith(".xwl"))) {
				String srcPath = FileUtil.getModulePath(srcFile);
				String dstPath = FileUtil.getModulePath(dstFile);
				if (UrlBuffer.change(srcPath, dstPath, isDir)) {
					changed = true;
				}

				if (!isDir) {
					srcPath = srcPath.substring(0, srcPath.length() - 4);
					dstPath = dstPath.substring(0, dstPath.length() - 4);
				}

				Object[] change = new Object[2];
				if (isDir) {
					change[0] = Pattern.compile("\\bxwl=" + srcPath + "\\b/");
					change[1] = "xwl=" + dstPath + "/";
				} else {
					change[0] = Pattern.compile("(\\bxwl=" + srcPath + "\\b)(?![/\\-\\.])");
					change[1] = "xwl=" + dstPath;
				}

				changes.add(change);
			}
		}

		if (changed) {
			UrlBuffer.save();
		}

		doChangePath(Base.modulePath, changes, rows);
		doChangePath(new File(Base.path, "wb/script"), changes, rows);
		Object[] result = new Object[]{rows, changes};
		return result;
	}

	private static void doChangePath(File folder, ArrayList<Object[]> changes, JSONArray rows) throws Exception {
		File[] files = FileUtil.listFiles(folder);
		File[] var8 = files;
		int var7 = files.length;

		for (int var6 = 0; var6 < var7; ++var6) {
			File file = var8[var6];
			if (file.isDirectory()) {
				doChangePath(file, changes, rows);
			} else {
				String fileExt = FileUtil.getFileExt(file).toLowerCase();
				if (fileExt.equals("xwl") || fileExt.equals("js")) {
					String text = FileUtil.readString(file);
					String replacedText = text;

					Object[] change;
					for (Iterator var12 = changes.iterator(); var12.hasNext(); replacedText = ((Pattern) change[0])
							.matcher(replacedText).replaceAll(Matcher.quoteReplacement((String) change[1]))) {
						change = (Object[]) var12.next();
					}

					if (!replacedText.equals(text)) {
						FileUtil.syncSave(file, replacedText);
						JSONObject row = new JSONObject();
						row.put("path", FileUtil.getPath(file));
						row.put("lastModified", DateUtil.getTimestamp(file.lastModified()));
						rows.put(row);
					}
				}
			}
		}

	}

	private static JSONArray copyFiles(JSONArray src, File dstFolder, boolean deleteOld) throws Exception {
		int j = src.length();
		JSONArray newNames = new JSONArray();

		for (int i = 0; i < j; ++i) {
			File file = new File(src.getString(i));
			Object[] object = FileUtil.syncCopy(file, dstFolder);
			if (deleteOld) {
				FileUtil.syncDelete(file, false);
			}

			newNames.put(object);
		}

		return newNames;
	}

	private static JSONArray doMoveFiles(JSONArray src, File dstFile) throws Exception {
		int j = src.length();
		JSONArray result = new JSONArray();

		for (int i = 0; i < j; ++i) {
			File file = new File(src.getString(i));
			FileUtil.syncMove(file, dstFile);
			Object[] object = new Object[]{FileUtil.getPath(new File(dstFile, file.getName())), false};
			result.put(object);
		}

		return result;
	}

	public static void initIDE(HttpServletRequest request, HttpServletResponse response) {
		JSONObject config = new JSONObject();
		config.put("fileTitle", Var.get("sys.ide.fileTitle"));
		request.setAttribute("initParams", StringUtil.text(config.toString()));
	}

	public static ArrayList<String> getIconList() {
		File iconPath = new File(Base.path, "wb/images");
		File[] files = FileUtil.listFiles(iconPath);
		ArrayList<String> list = new ArrayList();
		SortUtil.sort(files);
		File[] var6 = files;
		int var5 = files.length;

		for (int var4 = 0; var4 < var5; ++var4) {
			File file = var6[var4];
			if (!file.isDirectory()) {
				list.add(FileUtil.removeExtension(file.getName()) + "_icon");
			}
		}

		return list;
	}

	public static synchronized void compressScriptFile(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		boolean compressAll = Boolean.parseBoolean(request.getParameter("compressAll"));
		makeFile(request, Base.path, compressAll);
	}

	private static void limitDemoUser(HttpServletRequest request, File path) throws IOException {
		if (!WebUtil.hasRole(request, "admin") && WebUtil.hasRole(request, "demo")) {
			String sysFolderBase = Var.getString("sys.ide.sysFolderBase");
			if (path == null && !"root".equals(sysFolderBase)) {
				SysUtil.accessDenied(request);
			}

			File base;
			if ("server".equals(sysFolderBase)) {
				base = Base.path.getParentFile().getParentFile();
			} else if ("app".equals(sysFolderBase)) {
				FileUtil.checkProctected(request, path, false);
				base = Base.path;
			} else {
				base = null;
			}

			if (base != null && !FileUtil.isAncestor(base, path)) {
				SysUtil.accessDenied(request);
			}

		}
	}

	public static void searchDuplicateFiles(HttpServletRequest request, HttpServletResponse response) throws Exception {
		HashMap<String, String> map = new HashMap();
		String lastValue = null;
		JSONArray rows = new JSONArray();
		JSONObject lastRow = null;
		JSONObject lastAddRow = null;
		getFileMd5(Base.modulePath, map);
		ArrayList<Entry<String, String>> sortedList = SortUtil.sortValue(map);

		JSONObject row;
		for (Iterator var12 = sortedList.iterator(); var12.hasNext(); lastRow = row) {
			Entry<String, String> entry = (Entry) var12.next();
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();
			row = new JSONObject();
			row.put("content", FileUtil.getFilename(key));
			row.put("path", key);
			row.put("line", 1);
			row.put("ch", 0);
			row.put("nodePath", "/module");
			row.put("itemName", "Configs=itemId");
			if (value.equals(lastValue)) {
				if (lastAddRow != lastRow) {
					rows.put(lastRow);
				}

				rows.put(row);
				lastAddRow = row;
			}

			lastValue = value;
		}

		WebUtil.send(response, (new JSONObject()).put("rows", rows));
	}

	public static void getFileMd5(File folder, HashMap<String, String> map) throws Exception {
		File[] files = folder.listFiles();
		File[] var6 = files;
		int var5 = files.length;

		for (int var4 = 0; var4 < var5; ++var4) {
			File file = var6[var4];
			if (file.isDirectory()) {
				getFileMd5(file, map);
			} else if ("xwl".equals(FileUtil.getFileExt(file))) {
				map.put(FileUtil.getPath(file), Encrypter.getMD5(JsonUtil.readObject(file).opt("children").toString()));
			}
		}

	}

	public static void makeFile(HttpServletRequest request, File folder, boolean compressAll) throws IOException {
		File[] files = FileUtil.listFiles(folder);
		File[] var11 = files;
		int var10 = files.length;

		for (int var9 = 0; var9 < var10; ++var9) {
			File file = var11[var9];
			if (file.isDirectory()) {
				makeFile(request, file, compressAll);
			} else {
				String name = file.getName();
				String ext = FileUtil.getFileExt(name).toLowerCase();
				if (ext.equals("js") || ext.equals("css")) {
					name = FileUtil.removeExtension(name);
					if (name.endsWith("-debug")) {
						File newFile = new File(file.getParent(), name.substring(0, name.length() - 6) + "." + ext);
						if (compressAll || file.lastModified() != newFile.lastModified()) {
							Console.log(request, "Compressing " + file.toString());
							if (ext.equals("js")) {
								ScriptCompressor.compressJs(file, newFile);
							} else {
								ScriptCompressor.compressCss(file, newFile);
							}

							newFile.setLastModified(file.lastModified());
							File syncFile = FileUtil.getSyncPath(newFile);
							if (syncFile != null) {
								FileUtils.copyFile(newFile, syncFile);
							}
						}
					}
				}
			}
		}

	}
}