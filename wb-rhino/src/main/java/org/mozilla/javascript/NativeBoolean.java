/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

final class NativeBoolean extends IdScriptableObject {
	static final long serialVersionUID = -3716996899943880933L;
	private static final Object BOOLEAN_TAG = "Boolean";
	private static final int Id_constructor = 1;
	private static final int Id_toString = 2;
	private static final int Id_toSource = 3;
	private static final int Id_valueOf = 4;
	private static final int MAX_PROTOTYPE_ID = 4;
	private boolean booleanValue;

	static void init(Scriptable scope, boolean sealed) {
		NativeBoolean obj = new NativeBoolean(false);
		obj.exportAsJSClass(4, scope, sealed);
	}

	private NativeBoolean(boolean b) {
		this.booleanValue = b;
	}

	public String getClassName() {
		return "Boolean";
	}

	public Object getDefaultValue(Class<?> typeHint) {
		return typeHint == ScriptRuntime.BooleanClass ? ScriptRuntime
				.wrapBoolean(this.booleanValue) : super
				.getDefaultValue(typeHint);
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
			case 4 :
				arity = 0;
				s = "valueOf";
				break;
			default :
				throw new IllegalArgumentException(String.valueOf(id));
		}

		this.initPrototypeMethod(BOOLEAN_TAG, id, s, arity);
	}

	public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
			Scriptable thisObj, Object[] args) {
		if (!f.hasTag(BOOLEAN_TAG)) {
			return super.execIdCall(f, cx, scope, thisObj, args);
		} else {
			int id = f.methodId();
			boolean value;
			if (id != 1) {
				if (!(thisObj instanceof NativeBoolean)) {
					throw incompatibleCallError(f);
				} else {
					value = ((NativeBoolean) thisObj).booleanValue;
					switch (id) {
						case 2 :
							return value ? "true" : "false";
						case 3 :
							return value
									? "(new Boolean(true))"
									: "(new Boolean(false))";
						case 4 :
							return ScriptRuntime.wrapBoolean(value);
						default :
							throw new IllegalArgumentException(
									String.valueOf(id));
					}
				}
			} else {
				if (args.length == 0) {
					value = false;
				} else {
					value = args[0] instanceof ScriptableObject
							&& ((ScriptableObject) args[0])
									.avoidObjectDetection()
							? true
							: ScriptRuntime.toBoolean(args[0]);
				}

				return thisObj == null
						? new NativeBoolean(value)
						: ScriptRuntime.wrapBoolean(value);
			}
		}
	}

	protected int findPrototypeId(String s) {
		byte id = 0;
		String X = null;
		int s_length = s.length();
		if (s_length == 7) {
			X = "valueOf";
			id = 4;
		} else if (s_length == 8) {
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