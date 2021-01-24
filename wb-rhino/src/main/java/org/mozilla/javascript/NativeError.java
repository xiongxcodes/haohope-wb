/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

final class NativeError extends IdScriptableObject {
	static final long serialVersionUID = -5338413581437645187L;
	private static final Object ERROR_TAG = "Error";
	private static final int Id_constructor = 1;
	private static final int Id_toString = 2;
	private static final int Id_toSource = 3;
	private static final int MAX_PROTOTYPE_ID = 3;

	static void init(Scriptable scope, boolean sealed) {
		NativeError obj = new NativeError();
		ScriptableObject.putProperty(obj, "name", "Error");
		ScriptableObject.putProperty(obj, "message", "");
		ScriptableObject.putProperty(obj, "fileName", "");
		ScriptableObject.putProperty(obj, "lineNumber", new Integer(0));
		obj.exportAsJSClass(3, scope, sealed);
	}

	static NativeError make(Context cx, Scriptable scope,
			IdFunctionObject ctorObj, Object[] args) {
		Scriptable proto = (Scriptable) ((Scriptable) ctorObj.get("prototype",
				ctorObj));
		NativeError obj = new NativeError();
		obj.setPrototype(proto);
		obj.setParentScope(scope);
		int arglen = args.length;
		if (arglen >= 1) {
			ScriptableObject.putProperty(obj, "message",
					ScriptRuntime.toString(args[0]));
			if (arglen >= 2) {
				ScriptableObject.putProperty(obj, "fileName", args[1]);
				if (arglen >= 3) {
					int linep = ScriptRuntime.toInt32(args[2]);
					ScriptableObject.putProperty(obj, "lineNumber",
							new Integer(linep));
				}
			}
		}

		if (arglen < 3 && cx.hasFeature(10)) {
			int[] linep1 = new int[1];
			String fileName = Context.getSourcePositionFromStack(linep1);
			ScriptableObject.putProperty(obj, "lineNumber", new Integer(
					linep1[0]));
			if (arglen < 2) {
				ScriptableObject.putProperty(obj, "fileName", fileName);
			}
		}

		return obj;
	}

	public String getClassName() {
		return "Error";
	}

	public String toString() {
		return js_toString(this);
	}

	protected void initPrototypeId(int id) {
		String s;
		byte arity;
		switch (id) {
			case 1 :
				arity = 1;
				s = "constructor";
				break;
			case 2 :
				arity = 0;
				s = "toString";
				break;
			case 3 :
				arity = 0;
				s = "toSource";
				break;
			default :
				throw new IllegalArgumentException(String.valueOf(id));
		}

		this.initPrototypeMethod(ERROR_TAG, id, s, arity);
	}

	public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
			Scriptable thisObj, Object[] args) {
		if (!f.hasTag(ERROR_TAG)) {
			return super.execIdCall(f, cx, scope, thisObj, args);
		} else {
			int id = f.methodId();
			switch (id) {
				case 1 :
					return make(cx, scope, f, args);
				case 2 :
					return js_toString(thisObj);
				case 3 :
					return js_toSource(cx, scope, thisObj);
				default :
					throw new IllegalArgumentException(String.valueOf(id));
			}
		}
	}

	private static String js_toString(Scriptable thisObj) {
		return getString(thisObj, "name") + ": "
				+ getString(thisObj, "message");
	}

	private static String js_toSource(Context cx, Scriptable scope,
			Scriptable thisObj) {
		Object name = ScriptableObject.getProperty(thisObj, "name");
		Object message = ScriptableObject.getProperty(thisObj, "message");
		Object fileName = ScriptableObject.getProperty(thisObj, "fileName");
		Object lineNumber = ScriptableObject.getProperty(thisObj, "lineNumber");
		StringBuffer sb = new StringBuffer();
		sb.append("(new ");
		if (name == NOT_FOUND) {
			name = Undefined.instance;
		}

		sb.append(ScriptRuntime.toString(name));
		sb.append("(");
		if (message != NOT_FOUND || fileName != NOT_FOUND
				|| lineNumber != NOT_FOUND) {
			if (message == NOT_FOUND) {
				message = "";
			}

			sb.append(ScriptRuntime.uneval(cx, scope, message));
			if (fileName != NOT_FOUND || lineNumber != NOT_FOUND) {
				sb.append(", ");
				if (fileName == NOT_FOUND) {
					fileName = "";
				}

				sb.append(ScriptRuntime.uneval(cx, scope, fileName));
				if (lineNumber != NOT_FOUND) {
					int line = ScriptRuntime.toInt32(lineNumber);
					if (line != 0) {
						sb.append(", ");
						sb.append(ScriptRuntime.toString((double) line));
					}
				}
			}
		}

		sb.append("))");
		return sb.toString();
	}

	private static String getString(Scriptable obj, String id) {
		Object value = ScriptableObject.getProperty(obj, id);
		return value == NOT_FOUND ? "" : ScriptRuntime.toString(value);
	}

	protected int findPrototypeId(String s) {
		byte id = 0;
		String X = null;
		int s_length = s.length();
		if (s_length == 8) {
			char c = s.charAt(3);
			if (c == 111) {
				X = "toSource";
				id = 3;
			} else if (c == 116) {
				X = "toString";
				id = 2;
			}
		} else if (s_length == 11) {
			X = "constructor";
			id = 1;
		}

		if (X != null && X != s && !X.equals(s)) {
			id = 0;
		}

		return id;
	}
}