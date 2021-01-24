/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.IdFunctionCall;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.NativeJavaPackage;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Wrapper;

public class NativeJavaTopPackage extends NativeJavaPackage
		implements
			Function,
			IdFunctionCall {
	static final long serialVersionUID = -1455787259477709999L;
	private static final String[][] commonPackages = new String[][]{
			{"java", "lang", "reflect"}, {"java", "io"}, {"java", "math"},
			{"java", "net"}, {"java", "util", "zip"},
			{"java", "text", "resources"}, {"java", "applet"},
			{"javax", "swing"}};
	private static final Object FTAG = "JavaTopPackage";
	private static final int Id_getClass = 1;

	NativeJavaTopPackage(ClassLoader loader) {
		super(true, "", loader);
	}

	public Object call(Context cx, Scriptable scope, Scriptable thisObj,
			Object[] args) {
		return this.construct(cx, scope, args);
	}

	public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
		ClassLoader loader = null;
		if (args.length != 0) {
			Object arg = args[0];
			if (arg instanceof Wrapper) {
				arg = ((Wrapper) arg).unwrap();
			}

			if (arg instanceof ClassLoader) {
				loader = (ClassLoader) arg;
			}
		}

		if (loader == null) {
			Context.reportRuntimeError0("msg.not.classloader");
			return null;
		} else {
			return new NativeJavaPackage(true, "", loader);
		}
	}

	public static void init(Context cx, Scriptable scope, boolean sealed) {
		ClassLoader loader = cx.getApplicationClassLoader();
		NativeJavaTopPackage top = new NativeJavaTopPackage(loader);
		top.setPrototype(getObjectPrototype(scope));
		top.setParentScope(scope);

		for (int getClass = 0; getClass != commonPackages.length; ++getClass) {
			Object topNames = top;

			for (int topPackages = 0; topPackages != commonPackages[getClass].length; ++topPackages) {
				topNames = ((NativeJavaPackage) topNames).forcePackage(
						commonPackages[getClass][topPackages], scope);
			}
		}

		IdFunctionObject arg9 = new IdFunctionObject(top, FTAG, 1, "getClass",
				1, scope);
		String[] arg10 = new String[]{"java", "javax", "org", "com", "edu",
				"net"};
		NativeJavaPackage[] arg11 = new NativeJavaPackage[arg10.length];

		for (int global = 0; global < arg10.length; ++global) {
			arg11[global] = (NativeJavaPackage) top.get(arg10[global], top);
		}

		ScriptableObject arg12 = (ScriptableObject) scope;
		if (sealed) {
			arg9.sealObject();
		}

		arg9.exportAsScopeProperty();
		arg12.defineProperty("Packages", top, 2);

		for (int i = 0; i < arg10.length; ++i) {
			arg12.defineProperty(arg10[i], arg11[i], 2);
		}

	}

	public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
			Scriptable thisObj, Object[] args) {
		if (f.hasTag(FTAG) && f.methodId() == 1) {
			return this.js_getClass(cx, scope, args);
		} else {
			throw f.unknown();
		}
	}

	private Scriptable js_getClass(Context cx, Scriptable scope, Object[] args) {
		if (args.length > 0 && args[0] instanceof Wrapper) {
			Object result = this;
			Class cl = ((Wrapper) args[0]).unwrap().getClass();
			String name = cl.getName();
			int offset = 0;

			while (true) {
				int index = name.indexOf(46, offset);
				String propName = index == -1 ? name.substring(offset) : name
						.substring(offset, index);
				Object prop = ((Scriptable) result).get(propName,
						(Scriptable) result);
				if (!(prop instanceof Scriptable)) {
					break;
				}

				result = (Scriptable) prop;
				if (index == -1) {
					return (Scriptable) result;
				}

				offset = index + 1;
			}
		}

		throw Context.reportRuntimeError0("msg.not.java.obj");
	}
}