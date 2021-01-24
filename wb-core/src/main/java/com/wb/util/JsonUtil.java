package com.wb.util;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonUtil {
	public static Object getValue(JSONObject object, String path, char separator) {
		if (path == null) {
			throw new RuntimeException("null path value");
		} else if (path.isEmpty()) {
			return object;
		} else {
			String[] items = StringUtil.split(path, separator);
			JSONObject obj = object;
			int j = items.length - 1;

			for (int i = 0; i < j; ++i) {
				String item = items[i];
				obj = obj.optJSONObject(item);
				if (obj == null) {
					return null;
				}
			}

			return obj.opt(items[j]);
		}
	}

	public static JSONObject setValue(JSONObject object, String path, char separator, Object value) {
		if (StringUtil.isEmpty(path)) {
			throw new RuntimeException("Path is null or empty");
		} else {
			String[] items = StringUtil.split(path, separator);
			JSONObject obj = object;
			int j = items.length - 1;

			for (int i = 0; i < j; ++i) {
				String item = items[i];
				obj = obj.optJSONObject(item);
				if (obj == null) {
					throw new RuntimeException("Path \"" + path + "\" does not exist.");
				}
			}

			obj.put(items[j], value);
			return object;
		}
	}

	public static JSONObject readObject(File file) throws Exception {
		String text = FileUtil.readString(file);
		if (text.isEmpty()) {
			return new JSONObject();
		} else {
			try {
				return new JSONObject(text.substring(text.indexOf(123)));
			} catch (Throwable var3) {
				throw new JSONException("Invalid JSONObject: " + StringUtil.ellipsis(text, 50));
			}
		}
	}

	public static JSONArray readArray(File file) throws Exception {
		String text = FileUtil.readString(file);
		if (text.isEmpty()) {
			return new JSONArray();
		} else {
			try {
				return new JSONArray(text.substring(text.indexOf(91)));
			} catch (Throwable var3) {
				throw new JSONException("Invalid JSONArray: " + StringUtil.ellipsis(text, 30));
			}
		}
	}

	public static JSONObject findObject(JSONObject jo, String itemsKey, String key, String value) {
		if (jo.optString(key).equals(value)) {
			return jo;
		} else {
			JSONArray ja = jo.optJSONArray(itemsKey);
			if (ja != null) {
				int j = ja.length();

				for (int i = 0; i < j; ++i) {
					JSONObject item = ja.optJSONObject(i);
					if (item != null) {
						JSONObject result = findObject(item, itemsKey, key, value);
						if (result != null) {
							return result;
						}
					}
				}
			}

			return null;
		}
	}

	public static JSONObject findObject(JSONArray ja, String key, String text) {
		int j = ja.length();

		for (int i = 0; i < j; ++i) {
			JSONObject jo = ja.getJSONObject(i);
			if (jo.optString(key).equals(text)) {
				return jo;
			}
		}

		return null;
	}

	public static JSONArray findArray(JSONObject jo, String itemsKey, String key, String value) {
		JSONArray ja = jo.optJSONArray(itemsKey);
		if (ja != null) {
			int j = ja.length();

			for (int i = 0; i < j; ++i) {
				JSONObject item = ja.optJSONObject(i);
				if (item != null) {
					if (item.optString(key).equals(value)) {
						return ja;
					}

					JSONArray result = findArray(item, itemsKey, key, value);
					if (result != null) {
						return result;
					}
				}
			}
		}

		return null;
	}

	public static String remove(JSONObject jo, String itemsKey, String key, String value) {
		JSONArray ja = jo.optJSONArray(itemsKey);
		if (ja != null) {
			int j = ja.length();

			for (int i = j - 1; i >= 0; --i) {
				JSONObject item = ja.optJSONObject(i);
				if (item != null) {
					String result;
					if (StringUtil.isEqual(item.optString(key, (String) null), value)) {
						result = item.toString();
						ja.remove(i);
						return result;
					}

					result = remove(item, itemsKey, key, value);
					if (result != null) {
						return result;
					}
				}
			}
		}

		return null;
	}

	public static boolean replace(JSONObject jo, String itemsKey, String key, String value, Object data) {
		JSONArray ja = jo.optJSONArray(itemsKey);
		if (ja != null) {
			int j = ja.length();

			for (int i = 0; i < j; ++i) {
				JSONObject item = ja.optJSONObject(i);
				if (item != null) {
					if (StringUtil.isEqual(item.optString(key, (String) null), value)) {
						ja.put(i, data);
						return true;
					}

					if (replace(item, itemsKey, key, value, data)) {
						return true;
					}
				}
			}
		}

		return false;
	}

	public static String join(JSONArray array, String separator) {
		int j = array.length();
		if (j == 0) {
			return "";
		} else {
			StringBuilder buf = new StringBuilder();

			for (int i = 0; i < j; ++i) {
				if (i > 0) {
					buf.append(separator);
				}

				buf.append(array.getString(i));
			}

			return buf.toString();
		}
	}

	public static JSONObject fromCSV(String text) {
		JSONObject jo = new JSONObject();
		if (StringUtil.isEmpty(text)) {
			return jo;
		} else {
			String[] items = StringUtil.split(text, ',', true);
			String[] var6 = items;
			int var5 = items.length;

			for (int var4 = 0; var4 < var5; ++var4) {
				String item = var6[var4];
				jo.put(item, true);
			}

			return jo;
		}
	}

	public static boolean addAll(Collection<String> list, JSONArray array) {
		int j = array.length();

		for (int i = 0; i < j; ++i) {
			list.add(array.optString(i, (String) null));
		}

		return j != 0;
	}

	public static boolean addAll(JSONArray dest, JSONArray source) {
		int j = source.length();

		for (int i = 0; i < j; ++i) {
			dest.put(source.get(i));
		}

		return j != 0;
	}

	public static Object opt(JSONObject jo, String key) {
		return jo.isNull(key) ? null : jo.opt(key);
	}

	public static Object opt(JSONArray ja, int index) {
		return ja.isNull(index) ? null : ja.opt(index);
	}

	public static JSONObject clone(JSONObject source) {
		return new JSONObject(source.toString());
	}

	public static JSONArray clone(JSONArray source) {
		return new JSONArray(source.toString());
	}

	public static JSONObject apply(JSONObject source, JSONObject dest) {
		Set<Entry<String, Object>> es = dest.entrySet();
		Iterator var4 = es.iterator();

		while (var4.hasNext()) {
			Entry<String, Object> e = (Entry) var4.next();
			source.put((String) e.getKey(), e.getValue());
		}

		return source;
	}

	public static JSONObject applyIf(JSONObject source, JSONObject dest) {
		Set<Entry<String, Object>> es = dest.entrySet();
		Iterator var5 = es.iterator();

		while (var5.hasNext()) {
			Entry<String, Object> e = (Entry) var5.next();
			String key = (String) e.getKey();
			if (!source.has(key)) {
				source.put(key, e.getValue());
			}
		}

		return source;
	}

	public static JSONObject copy(JSONObject source, String[] names) {
		JSONObject jo = new JSONObject();
		String[] var6 = names;
		int var5 = names.length;

		for (int var4 = 0; var4 < var5; ++var4) {
			String name = var6[var4];
			jo.put(name, source.opt(name));
		}

		return jo;
	}

	public static JSONArray getArray(Object value) {
		if (value instanceof JSONArray) {
			return (JSONArray) value;
		} else {
			String stringValue = value == null ? null : value.toString();
			return StringUtil.isEmpty(stringValue) ? null : new JSONArray(stringValue);
		}
	}

	public static JSONObject getObject(Object value) {
		if (value instanceof JSONObject) {
			return (JSONObject) value;
		} else {
			String stringValue = value == null ? null : value.toString();
			return StringUtil.isEmpty(stringValue) ? null : new JSONObject(stringValue);
		}
	}

	public static JSONArray toArray(Object value) {
		return value != null && !(value instanceof JSONArray) ? (new JSONArray()).put(value) : (JSONArray) value;
	}

	public static HashSet<String> toHashSet(JSONArray array) {
		if (array == null) {
			return null;
		} else {
			int j = array.length();
			HashSet<String> hs = new HashSet();

			for (int i = 0; i < j; ++i) {
				hs.add(array.optString(i));
			}

			return hs;
		}
	}

	public static String[] createArray(JSONArray array) {
		if (array == null) {
			return null;
		} else {
			int j = array.length();
			String[] result = new String[j];

			for (int i = 0; i < j; ++i) {
				result[i] = array.getString(i);
			}

			return result;
		}
	}
}