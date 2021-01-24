package com.wb.common;

import com.wb.util.FileUtil;
import com.wb.util.JsonUtil;
import com.wb.util.StringUtil;
import java.io.File;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.json.JSONObject;

public class Str {
	private static ConcurrentHashMap<String, ConcurrentHashMap<String, String>> wbLang;
	private static ConcurrentHashMap<String, String> extLang;
	private static ConcurrentHashMap<String, String> touchLangList;
	private static ConcurrentHashMap<String, String> langMap;

	public static String format(String key, Object... args) {
		return langFormat(Var.defaultLanguage, key, args);
	}

	public static String format(HttpServletRequest request, String key, Object... args) {
		return langFormat(getLanguage(request), key, args);
	}

	public static String optLanguage(String lang) {
		return optLang(wbLang, lang);
	}

	public static String optExtLanguage(String lang) {
		return optLang(extLang, lang);
	}

	public static String optTouchLanguage(String lang) {
		return optLang(touchLangList, lang);
	}

	public static String getLanguage(HttpServletRequest request) {
		String useLang = (String) request.getAttribute("sys.useLang");
		if (useLang != null) {
			return useLang;
		} else {
			useLang = optLanguage(getClientLanguage(request));
			request.setAttribute("sys.useLang", useLang);
			return useLang;
		}
	}

	public static String getText(HttpServletRequest request, String string) {
		return string != null && string.startsWith("Str.") ? format(request, string.substring(4)) : string;
	}

	public static String getClientLanguage(HttpServletRequest request) {
		String setLan = (String) request.getAttribute("sys.clientLang");
		if (setLan != null) {
			return setLan;
		} else {
			HttpSession session = request.getSession(false);
			setLan = Var.language;
			if (session != null) {
				String sessionLan = (String) session.getAttribute("sys.lang");
				if (!StringUtil.isEmpty(sessionLan)) {
					setLan = sessionLan;
				}
			}

			if (setLan.equals("auto")) {
				setLan = Var.defaultLanguage;
				String acceptLang = request.getHeader("Accept-Language");
				if (acceptLang != null) {
					int pos = acceptLang.indexOf(44);
					if (pos != -1) {
						acceptLang = acceptLang.substring(0, pos);
					}

					pos = acceptLang.indexOf(59);
					if (pos != -1) {
						acceptLang = acceptLang.substring(0, pos);
					}

					pos = acceptLang.indexOf(45);
					if (pos == -1) {
						setLan = acceptLang.toLowerCase();
					} else {
						String language = StringUtil.concat(new String[]{acceptLang.substring(0, pos).toLowerCase(),
								"_", acceptLang.substring(pos + 1).toUpperCase()});
						String mappedLang = (String) langMap.get(language);
						if (mappedLang == null) {
							setLan = language;
						} else {
							setLan = mappedLang;
						}
					}
				}
			}

			request.setAttribute("sys.clientLang", setLan);
			return setLan;
		}
	}

	private static String langFormat(String lang, String key, Object... args) {
		ConcurrentHashMap<String, String> buffer = (ConcurrentHashMap) wbLang.get(optLanguage(lang));
		if (buffer == null) {
			return key;
		} else {
			String str = (String) buffer.get(key);
			if (str == null) {
				return key;
			} else {
				int i = 0;
				Object[] var9 = args;
				int var8 = args.length;

				for (int var7 = 0; var7 < var8; ++var7) {
					Object object = var9[var7];
					str = StringUtil.replaceAll(str, "{" + i++ + "}", object == null ? "null" : object.toString());
				}

				return str;
			}
		}
	}

	private static String optLang(ConcurrentHashMap<String, ?> map, String lang) {
		if (!StringUtil.isEmpty(lang)) {
			if (map.containsKey(lang)) {
				return lang;
			}

			int pos = lang.indexOf(95);
			if (pos != -1) {
				lang = lang.substring(0, pos);
				if (map.containsKey(lang)) {
					return lang;
				}
			}
		}

		return Var.defaultLanguage;
	}

	public static synchronized void load() {
		try {
			langMap = new ConcurrentHashMap();
			wbLang = new ConcurrentHashMap();
			File[] fs = FileUtil.listFiles(new File(Base.path, "wb/script/locale"));
			JSONObject jo = JsonUtil.readObject(new File(Base.path, "wb/system/language.json"));
			Set<Entry<String, Object>> es = jo.entrySet();
			Iterator var7 = es.iterator();

			while (var7.hasNext()) {
				Entry<String, Object> e = (Entry) var7.next();
				String[] langList = StringUtil.split((String) e.getValue(), ',', true);
				String[] var11 = langList;
				int var10 = langList.length;

				for (int var9 = 0; var9 < var10; ++var9) {
					String ln = var11[var9];
					langMap.put(ln, (String) e.getKey());
				}
			}

			File[] var16 = fs;
			int var15 = fs.length;

			String name;
			File file;
			int var14;
			for (var14 = 0; var14 < var15; ++var14) {
				file = var16[var14];
				name = file.getName().toLowerCase();
				if (name.endsWith(".js")) {
					if (name.endsWith("-debug.js")) {
						name = name.substring(8, name.length() - 9);
					} else {
						name = name.substring(8, name.length() - 3);
					}

					if (!wbLang.containsKey(name)) {
						ConcurrentHashMap<String, String> buffer = new ConcurrentHashMap();
						jo = JsonUtil.readObject(file);
						es = jo.entrySet();
						Iterator var18 = es.iterator();

						while (var18.hasNext()) {
							Entry<String, Object> e = (Entry) var18.next();
							buffer.put((String) e.getKey(), (String) e.getValue());
						}

						wbLang.put(name, buffer);
					}
				}
			}

			extLang = new ConcurrentHashMap();
			fs = FileUtil.listFiles(new File(Base.path, "wb/libs/ext/locale"));
			var16 = fs;
			var15 = fs.length;

			for (var14 = 0; var14 < var15; ++var14) {
				file = var16[var14];
				name = file.getName();
				if (!name.endsWith("-debug.js")) {
					name = name.substring(9, name.length() - 3);
					extLang.put(name, name);
				}
			}

			touchLangList = new ConcurrentHashMap();
			fs = FileUtil.listFiles(new File(Base.path, "wb/libs/touch/locale"));
			var16 = fs;
			var15 = fs.length;

			for (var14 = 0; var14 < var15; ++var14) {
				file = var16[var14];
				name = file.getName();
				if (!name.endsWith("-debug.js")) {
					name = name.substring(7, name.length() - 3);
					touchLangList.put(name, name);
				}
			}

		} catch (Throwable var12) {
			throw new RuntimeException(var12);
		}
	}
}