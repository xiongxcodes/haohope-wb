/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

public class NativeObject extends IdScriptableObject {
	static final long serialVersionUID = -6345305608474346996L;
	private static final Object OBJECT_TAG = "Object";
	private static final int Id_constructor = 1;
	private static final int Id_toString = 2;
	private static final int Id_toLocaleString = 3;
	private static final int Id_valueOf = 4;
	private static final int Id_hasOwnProperty = 5;
	private static final int Id_propertyIsEnumerable = 6;
	private static final int Id_isPrototypeOf = 7;
	private static final int Id_toSource = 8;
	private static final int Id___defineGetter__ = 9;
	private static final int Id___defineSetter__ = 10;
	private static final int Id___lookupGetter__ = 11;
	private static final int Id___lookupSetter__ = 12;
	private static final int MAX_PROTOTYPE_ID = 12;

	static void init(Scriptable scope, boolean sealed) {
		NativeObject obj = new NativeObject();
		obj.exportAsJSClass(12, scope, sealed);
	}

	public String getClassName() {
		return "Object";
	}

	public String toString() {
		return ScriptRuntime.defaultObjectToString(this);
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
				s = "toLocaleString";
				break;
			case 4 :
				arity = 0;
				s = "valueOf";
				break;
			case 5 :
				arity = 1;
				s = "hasOwnProperty";
				break;
			case 6 :
				arity = 1;
				s = "propertyIsEnumerable";
				break;
			case 7 :
				arity = 1;
				s = "isPrototypeOf";
				break;
			case 8 :
				arity = 0;
				s = "toSource";
				break;
			case 9 :
				arity = 2;
				s = "__defineGetter__";
				break;
			case 10 :
				arity = 2;
				s = "__defineSetter__";
				break;
			case 11 :
				arity = 1;
				s = "__lookupGetter__";
				break;
			case 12 :
				arity = 1;
				s = "__lookupSetter__";
				break;
			default :
				throw new IllegalArgumentException(String.valueOf(id));
		}

		this.initPrototypeMethod(OBJECT_TAG, id, s, arity);
	}

	public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
			Scriptable thisObj, Object[] args) {
		if (!f.hasTag(OBJECT_TAG)) {
			return super.execIdCall(f, cx, scope, thisObj, args);
		} else {
			int id = f.methodId();
			ScriptableObject so;
			String name;
			int index;
			boolean so2;
			switch (id) {
				case 1 :
					if (thisObj != null) {
						return f.construct(cx, scope, args);
					} else {
						if (args.length != 0 && args[0] != null
								&& args[0] != Undefined.instance) {
							return ScriptRuntime.toObject(cx, scope, args[0]);
						}

						return new NativeObject();
					}
				case 2 :
				case 3 :
					if (cx.hasFeature(4)) {
						String so3 = ScriptRuntime.defaultObjectToSource(cx,
								scope, thisObj, args);
						int name2 = so3.length();
						if (name2 != 0 && so3.charAt(0) == 40
								&& so3.charAt(name2 - 1) == 41) {
							so3 = so3.substring(1, name2 - 1);
						}

						return so3;
					}

					return ScriptRuntime.defaultObjectToString(thisObj);
				case 4 :
					return thisObj;
				case 5 :
					if (args.length == 0) {
						so2 = false;
					} else {
						name = ScriptRuntime.toStringIdOrIndex(cx, args[0]);
						if (name == null) {
							index = ScriptRuntime.lastIndexResult(cx);
							so2 = thisObj.has(index, thisObj);
						} else {
							so2 = thisObj.has(name, thisObj);
						}
					}

					return ScriptRuntime.wrapBoolean(so2);
				case 6 :
					if (args.length == 0) {
						so2 = false;
					} else {
						name = ScriptRuntime.toStringIdOrIndex(cx, args[0]);
						if (name == null) {
							index = ScriptRuntime.lastIndexResult(cx);
							so2 = thisObj.has(index, thisObj);
							if (so2 && thisObj instanceof ScriptableObject) {
								ScriptableObject isSetter2 = (ScriptableObject) thisObj;
								int gs2 = isSetter2.getAttributes(index);
								so2 = (gs2 & 2) == 0;
							}
						} else {
							so2 = thisObj.has(name, thisObj);
							if (so2 && thisObj instanceof ScriptableObject) {
								ScriptableObject index1 = (ScriptableObject) thisObj;
								int isSetter3 = index1.getAttributes(name);
								so2 = (isSetter3 & 2) == 0;
							}
						}
					}

					return ScriptRuntime.wrapBoolean(so2);
				case 7 :
					so2 = false;
					if (args.length != 0 && args[0] instanceof Scriptable) {
						Scriptable name1 = (Scriptable) args[0];

						do {
							name1 = name1.getPrototype();
							if (name1 == thisObj) {
								so2 = true;
								break;
							}
						} while (name1 != null);
					}

					return ScriptRuntime.wrapBoolean(so2);
				case 8 :
					return ScriptRuntime.defaultObjectToSource(cx, scope,
							thisObj, args);
				case 9 :
				case 10 :
					if (args.length >= 2 && args[1] instanceof Callable) {
						if (!(thisObj instanceof ScriptableObject)) {
							throw Context
									.reportRuntimeError2(
											"msg.extend.scriptable", thisObj
													.getClass().getName(),
											String.valueOf(args[0]));
						}

						so = (ScriptableObject) thisObj;
						name = ScriptRuntime.toStringIdOrIndex(cx, args[0]);
						index = name != null ? 0 : ScriptRuntime
								.lastIndexResult(cx);
						Callable isSetter1 = (Callable) args[1];
						boolean gs1 = id == 10;
						so.setGetterOrSetter(name, index, isSetter1, gs1);
						if (so instanceof NativeArray) {
							((NativeArray) so).setDenseOnly(false);
						}

						return Undefined.instance;
					}

					Object so1 = args.length >= 2
							? args[1]
							: Undefined.instance;
					throw ScriptRuntime.notFunctionError(so1);
				case 11 :
				case 12 :
					if (args.length >= 1 && thisObj instanceof ScriptableObject) {
						so = (ScriptableObject) thisObj;
						name = ScriptRuntime.toStringIdOrIndex(cx, args[0]);
						index = name != null ? 0 : ScriptRuntime
								.lastIndexResult(cx);
						boolean isSetter = id == 12;

						Object gs;
						while (true) {
							gs = so.getGetterOrSetter(name, index, isSetter);
							if (gs != null) {
								break;
							}

							Scriptable v = so.getPrototype();
							if (v == null || !(v instanceof ScriptableObject)) {
								break;
							}

							so = (ScriptableObject) v;
						}

						if (gs != null) {
							return gs;
						}

						return Undefined.instance;
					}

					return Undefined.instance;
				default :
					throw new IllegalArgumentException(String.valueOf(id));
			}
		}
	}

	protected int findPrototypeId(String s) {
		byte id = 0;
		String X = null;
		char c;
		switch (s.length()) {
			case 7 :
				X = "valueOf";
				id = 4;
				break;
			case 8 :
				c = s.charAt(3);
				if (c == 111) {
					X = "toSource";
					id = 8;
				} else if (c == 116) {
					X = "toString";
					id = 2;
				}
			case 9 :
			case 10 :
			case 12 :
			case 15 :
			case 17 :
			case 18 :
			case 19 :
			default :
				break;
			case 11 :
				X = "constructor";
				id = 1;
				break;
			case 13 :
				X = "isPrototypeOf";
				id = 7;
				break;
			case 14 :
				c = s.charAt(0);
				if (c == 104) {
					X = "hasOwnProperty";
					id = 5;
				} else if (c == 116) {
					X = "toLocaleString";
					id = 3;
				}
				break;
			case 16 :
				c = s.charAt(2);
				if (c == 100) {
					c = s.charAt(8);
					if (c == 71) {
						X = "__defineGetter__";
						id = 9;
					} else if (c == 83) {
						X = "__defineSetter__";
						id = 10;
					}
				} else if (c == 108) {
					c = s.charAt(8);
					if (c == 71) {
						X = "__lookupGetter__";
						id = 11;
					} else if (c == 83) {
						X = "__lookupSetter__";
						id = 12;
					}
				}
				break;
			case 20 :
				X = "propertyIsEnumerable";
				id = 6;
		}

		if (X != null && X != s && !X.equals(s)) {
			id = 0;
		}

		return id;
	}
}