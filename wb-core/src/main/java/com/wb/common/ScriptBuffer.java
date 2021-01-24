package com.wb.common;

import com.wb.util.FileUtil;
import com.wb.util.StringUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.quartz.JobExecutionContext;

public class ScriptBuffer {
	public static ArrayList<String> globalMembers;
	private static ScriptEngineManager manager;
	private static ScriptEngine engine;
	private static Compilable compilable;
	private static ConcurrentHashMap<String, CompiledScript> buffer;

	public static void run(String id, String scriptText, HttpServletRequest request, HttpServletResponse response,
			String sourceURL) throws Exception {
		CompiledScript script = (CompiledScript) buffer.get(id);
		if (script == null) {
			script = compilable.compile(encScript(scriptText, sourceURL, true));
			buffer.put(id, script);
		}

		Bindings bindings = engine.createBindings();
		bindings.put("request", request);
		bindings.put("response", response);
		script.eval(bindings);
	}

	private static String encScript(String scriptText, String sourceURL, boolean addApp) {
		StringBuilder buf = new StringBuilder(scriptText.length() + 100);
		buf.append("(function main(){");
		if (addApp) {
			buf.append("var app=Wb.getApp(request,response);");
		}

		buf.append(scriptText);
		buf.append("\n})();");
		if (!StringUtil.isEmpty(sourceURL)) {
			buf.append("\n//# sourceURL=");
			buf.append(sourceURL);
		}

		return buf.toString();
	}

	public static Object run(String scriptText, JSONObject params) throws Exception {
		CompiledScript script = compilable.compile(encScript(scriptText, (String) null, false));
		if (params == null) {
			return script.eval();
		} else {
			Bindings bindings = engine.createBindings();
			Set<Entry<String, Object>> es = params.entrySet();
			Iterator var6 = es.iterator();

			while (var6.hasNext()) {
				Entry<String, Object> e = (Entry) var6.next();
				bindings.put((String) e.getKey(), e.getValue());
			}

			return script.eval(bindings);
		}
	}

	public static Object run(String id, String scriptText, JSONObject params) throws Exception {
		CompiledScript script = (CompiledScript) buffer.get(id);
		if (script == null) {
			script = compilable.compile(encScript(scriptText, id, false));
			buffer.put(id, script);
		}

		if (params == null) {
			return script.eval();
		} else {
			Bindings bindings = engine.createBindings();
			Set<Entry<String, Object>> es = params.entrySet();
			Iterator var7 = es.iterator();

			while (var7.hasNext()) {
				Entry<String, Object> e = (Entry) var7.next();
				bindings.put((String) e.getKey(), e.getValue());
			}

			return script.eval(bindings);
		}
	}

	public static boolean evalCondition(String condition, JSONObject params) throws Exception {
		Object value = run(condition, params);
		if (value == null) {
			return false;
		} else if (value instanceof Boolean) {
			return (Boolean) value;
		} else if (value instanceof Number) {
			return ((Number) value).intValue() != 0;
		} else {
			String str = value.toString();
			return !str.isEmpty();
		}
	}

	public static Object run(String scriptText, String sourceURL) throws Exception {
		CompiledScript script = compilable.compile(encScript(scriptText, sourceURL, false));
		return script.eval();
	}

	public static void run(String id, String scriptText, JobExecutionContext jobContext) throws Exception {
		CompiledScript script = (CompiledScript) buffer.get(id);
		if (script == null) {
			script = compilable.compile(encScript(scriptText, id, false));
			buffer.put(id, script);
		}

		Bindings bindings = engine.createBindings();
		bindings.put("jobContext", jobContext);
		script.eval(bindings);
	}

	public static synchronized void load() {
		try {
			manager = new ScriptEngineManager();
			engine = manager.getEngineByName("javascript");
			compilable = (Compilable) engine;
			buffer = new ConcurrentHashMap();
			globalMembers = new ArrayList();
			loadUtils();
		} catch (Throwable var1) {
			throw new RuntimeException(var1);
		}
	}

	public static void remove(String id) {
		Set<Entry<String, CompiledScript>> es = buffer.entrySet();
		Iterator var4 = es.iterator();

		while (var4.hasNext()) {
			Entry<String, CompiledScript> e = (Entry) var4.next();
			String k = (String) e.getKey();
			if (k.startsWith(id)) {
				buffer.remove(k);
			}
		}

	}

	private static void loadUtils() throws Exception {
		String text = FileUtil.readString(new File(Base.path, "wb/system/server.js"));
		text = text + "\n//# sourceURL=server.js";
		CompiledScript script = compilable.compile(text);
		Bindings bindings = engine.createBindings();
		script.eval(bindings);
		Set<Entry<String, Object>> es = bindings.entrySet();
		Iterator var6 = es.iterator();

		while (var6.hasNext()) {
			Entry<String, Object> e = (Entry) var6.next();
			String key = (String) e.getKey();
			manager.put((String) e.getKey(), e.getValue());
			globalMembers.add(key);
		}

	}
}