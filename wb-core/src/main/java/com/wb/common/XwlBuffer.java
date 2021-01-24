package com.wb.common;

import com.wb.interact.Controls;
import com.wb.util.FileUtil;
import com.wb.util.JsonUtil;
import com.wb.util.StringUtil;
import com.wb.util.SysUtil;
import com.wb.util.WbUtil;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONArray;
import org.json.JSONObject;

public class XwlBuffer {
	private static ConcurrentHashMap<String, Object[]> buffer;

	public static JSONObject get(String path) throws Exception {
		return get(path, false);
	}

	public static JSONObject get(File file) throws Exception {
		return get(file, false);
	}

	public static JSONObject get(File file, boolean silent) throws Exception {
		return get(FileUtil.getModulePath(file), silent);
	}

	public static JSONObject get(String path, boolean silent) throws Exception {
		if (path == null) {
			if (silent) {
				return null;
			} else {
				throw new NullPointerException("Module path is not specified.");
			}
		} else {
			String pathKey = path.toLowerCase();
			File file;
			long lastModified;
			if (Var.uncheckModified) {
				file = null;
				lastModified = -1L;
			} else {
				file = new File(Base.modulePath, path);
				lastModified = file.lastModified();
			}

			Object[] obj = (Object[]) buffer.get(pathKey);
			if (obj != null && (Var.uncheckModified || lastModified == (Long) obj[1])) {
				return (JSONObject) obj[0];
			} else {
				if (Var.uncheckModified) {
					file = new File(Base.modulePath, path);
					lastModified = file.lastModified();
				}

				if (lastModified == 0L) {
					if (silent) {
						return null;
					} else {
						throw new IllegalArgumentException("Module \"" + path + "\" is not found.");
					}
				} else {
					JSONObject root;
					try {
						root = JsonUtil.readObject(file);
					} catch (Exception var10) {
						throw new RuntimeException(
								"Module \"" + FileUtil.getRelativePath(Base.modulePath, file) + "\" is invalid.");
					}

					if (!root.has("children")) {
						throw new RuntimeException(
								"Module \"" + FileUtil.getRelativePath(Base.modulePath, file) + "\" is empty.");
					} else {
						JSONObject moduleNode = root.getJSONArray("children").getJSONObject(0).getJSONObject("configs");
						root.getJSONObject("roles").put("admin", 1);
						root.put("loginRequired", !"false".equals(moduleNode.opt("loginRequired")));
						root.put("internalCall", "true".equals(moduleNode.opt("internalCall")));
						boolean[] libTypes = optimize(root, true, SysUtil.getId() + ".");
						autoSetConfig(moduleNode, libTypes);
						if (moduleNode.optString("loadJS").indexOf("touch") != -1) {
							root.put("hasTouch", true);
						}

						obj = new Object[]{root, lastModified};
						buffer.put(pathKey, obj);
						return root;
					}
				}
			}
		}
	}

	private static void autoSetConfig(JSONObject moduleNode, boolean[] libTypes) {
		String loadJS = (String) moduleNode.opt("loadJS");
		if (loadJS == null) {
			ArrayList<String> libs = new ArrayList();
			if (libTypes[1]) {
				libs.add("ext");
			}

			if (libTypes[2]) {
				libs.add("touch");
			}

			if (libTypes[3]) {
				libs.add("bootstrap");
			}

			moduleNode.put("loadJS", StringUtil.join(libs, "+"));
		}

	}

	public static void clear(String path) {
		Set<Entry<String, Object[]>> es = buffer.entrySet();
		String delPath = StringUtil.concat(new String[]{path, "/"}).toLowerCase();
		Iterator var7 = es.iterator();

		while (var7.hasNext()) {
			Entry<String, Object[]> e = (Entry) var7.next();
			String key = (String) e.getKey();
			String modulePath = StringUtil.concat(new String[]{key, "/"});
			if (modulePath.startsWith(delPath)) {
				Object[] value = (Object[]) e.getValue();
				ScriptBuffer.remove(((JSONObject) value[0]).getJSONArray("children").getJSONObject(0)
						.getJSONObject("configs").getString("id"));
				buffer.remove(key);
			}
		}

	}

	public static synchronized void load() {
		buffer = new ConcurrentHashMap();
	}

	private static boolean[] optimize(JSONObject data, boolean parentRoot, String moduleId) throws IOException {
		JSONArray ja = data.getJSONArray("children");
		int j = ja.length();
		boolean[] libTypes = new boolean[4];
		int l = libTypes.length;
		if (j == 0) {
			data.remove("children");
		} else {
			String parentType = (String) data.opt("type");

			for (int i = 0; i < j; ++i) {
				JSONObject jo = ja.getJSONObject(i);
				String type = (String) jo.opt("type");
				JSONObject configs = (JSONObject) jo.opt("configs");
				JSONObject meta = Controls.get(type);
				JSONObject general = (JSONObject) meta.opt("general");
				JSONObject tag = (JSONObject) general.opt("tag");
				if (tag != null) {
					Integer lib = (Integer) tag.opt("lib");
					if (lib == null) {
						lib = 0;
					} else {
						lib = lib;
					}

					libTypes[lib] = true;
				}

				JSONObject autoNames = (JSONObject) general.opt("autoNames");
				Object isConfig = configs.opt("isConfig");
				boolean asConfig;
				if (parentRoot || autoNames == null || isConfig != null
						|| !autoNames.has(parentType) && !autoNames.has("any")) {
					asConfig = "true".equals(isConfig);
				} else {
					asConfig = true;
				}

				if (asConfig) {
					JSONObject configItems = (JSONObject) data.opt("__configs");
					JSONArray children;
					if (configItems == null) {
						configItems = new JSONObject();
						children = new JSONArray();
						configItems.put("children", children);
						data.put("__configs", configItems);
					} else {
						children = (JSONArray) configItems.opt("children");
					}

					children.put(jo);
					ja.remove(i);
					--i;
					--j;
				}

				if ("module".equals(type)) {
					configs.put("id", moduleId);
				} else if ("serverscript".equals(type)) {
					configs.put("id", StringUtil.concat(new String[]{moduleId, SysUtil.getId()}));
				}

				jo.remove("expanded");
				boolean[] subLibTypes = optimize(jo, Boolean.TRUE.equals(general.opt("root")), moduleId);

				for (int k = 0; k < l; ++k) {
					if (subLibTypes[k]) {
						libTypes[k] = true;
					}
				}
			}

			if (j == 0) {
				data.remove("children");
			}
		}

		return libTypes;
	}

	public static boolean canDisplay(File file, String[] roles, int type) throws Exception {
		if (file.isDirectory()) {
			if (type == 5) {
				return true;
			}

			File configFile = new File(file, "folder.json");
			if (configFile.exists()) {
				JSONObject content = JsonUtil.readObject(configFile);
				if (type < 3 && Boolean.TRUE.equals(content.opt("hidden"))) {
					return false;
				}
			}

			File[] files = FileUtil.listFiles(file);
			File[] var8 = files;
			int var7 = files.length;

			for (int var6 = 0; var6 < var7; ++var6) {
				File subFile = var8[var6];
				if (canDisplay(subFile, roles, type)) {
					return true;
				}
			}
		} else if (file.getName().endsWith(".xwl")) {
			JSONObject content = get(file);
			if (type != 4 && type != 5) {
				if (type == 3) {
					if (Boolean.FALSE.equals(content.opt("loginRequired"))
							|| Boolean.TRUE.equals(content.opt("internalCall"))) {
						return false;
					}
				} else if (content.has("hasTouch")) {
					if (type == 1 && !Var.homeShowApp) {
						return false;
					}
				} else if (type == 2) {
					return false;
				}

				if (Boolean.FALSE.equals(content.opt("hidden")) && WbUtil.canAccess(content, roles)) {
					return true;
				}
			} else if (WbUtil.canAccess(content, roles)) {
				return true;
			}
		}

		return false;
	}
}