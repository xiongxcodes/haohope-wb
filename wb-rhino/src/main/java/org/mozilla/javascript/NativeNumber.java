/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.DToA;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;

final class NativeNumber extends IdScriptableObject {
	static final long serialVersionUID = 3504516769741512101L;
	private static final Object NUMBER_TAG = "Number";
	private static final int MAX_PRECISION = 100;
	private static final int Id_constructor = 1;
	private static final int Id_toString = 2;
	private static final int Id_toLocaleString = 3;
	private static final int Id_toSource = 4;
	private static final int Id_valueOf = 5;
	private static final int Id_toFixed = 6;
	private static final int Id_toExponential = 7;
	private static final int Id_toPrecision = 8;
	private static final int MAX_PROTOTYPE_ID = 8;
	private double doubleValue;

	static void init(Scriptable scope, boolean sealed) {
		NativeNumber obj = new NativeNumber(0.0D);
		obj.exportAsJSClass(8, scope, sealed);
	}

	private NativeNumber(double number) {
		this.doubleValue = number;
	}

	public String getClassName() {
		return "Number";
	}

	protected void fillConstructorProperties(IdFunctionObject ctor) {
		boolean attr = true;
		ctor.defineProperty("NaN", ScriptRuntime.NaNobj, 7);
		ctor.defineProperty("POSITIVE_INFINITY",
				ScriptRuntime.wrapNumber(Double.POSITIVE_INFINITY), 7);
		ctor.defineProperty("NEGATIVE_INFINITY",
				ScriptRuntime.wrapNumber(Double.NEGATIVE_INFINITY), 7);
		ctor.defineProperty("MAX_VALUE",
				ScriptRuntime.wrapNumber(Double.MAX_VALUE), 7);
		ctor.defineProperty("MIN_VALUE",
				ScriptRuntime.wrapNumber(Double.MIN_VALUE), 7);
		super.fillConstructorProperties(ctor);
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
				arity = 1;
				s = "toString";
				break;
			case 3 :
				arity = 1;
				s = "toLocaleString";
				break;
			case 4 :
				arity = 0;
				s = "toSource";
				break;
			case 5 :
				arity = 0;
				s = "valueOf";
				break;
			case 6 :
				arity = 1;
				s = "toFixed";
				break;
			case 7 :
				arity = 1;
				s = "toExponential";
				break;
			case 8 :
				arity = 1;
				s = "toPrecision";
				break;
			default :
				throw new IllegalArgumentException(String.valueOf(id));
		}

		this.initPrototypeMethod(NUMBER_TAG, id, s, arity);
	}

	public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
			Scriptable thisObj, Object[] args) {
		if (!f.hasTag(NUMBER_TAG)) {
			return super.execIdCall(f, cx, scope, thisObj, args);
		} else {
			int id = f.methodId();
			double value;
			if (id == 1) {
				value = args.length >= 1
						? ScriptRuntime.toNumber(args[0])
						: 0.0D;
				return thisObj == null
						? new NativeNumber(value)
						: ScriptRuntime.wrapNumber(value);
			} else if (!(thisObj instanceof NativeNumber)) {
				throw incompatibleCallError(f);
			} else {
				value = ((NativeNumber) thisObj).doubleValue;
				switch (id) {
					case 2 :
					case 3 :
						int base = args.length == 0 ? 10 : ScriptRuntime
								.toInt32(args[0]);
						return ScriptRuntime.numberToString(value, base);
					case 4 :
						return "(new Number(" + ScriptRuntime.toString(value)
								+ "))";
					case 5 :
						return ScriptRuntime.wrapNumber(value);
					case 6 :
						return num_to(value, args, 2, 2, -20, 0);
					case 7 :
						return num_to(value, args, 1, 3, 0, 1);
					case 8 :
						return num_to(value, args, 0, 4, 1, 0);
					default :
						throw new IllegalArgumentException(String.valueOf(id));
				}
			}
		}
	}

	public String toString() {
		return ScriptRuntime.numberToString(this.doubleValue, 10);
	}

	private static String num_to(double val, Object[] args, int zeroArgMode,
			int oneArgMode, int precisionMin, int precisionOffset) {
		int precision;
		if (args.length == 0) {
			precision = 0;
			oneArgMode = zeroArgMode;
		} else {
			precision = ScriptRuntime.toInt32(args[0]);
			if (precision < precisionMin || precision > 100) {
				String sb1 = ScriptRuntime.getMessage1("msg.bad.precision",
						ScriptRuntime.toString(args[0]));
				throw ScriptRuntime.constructError("RangeError", sb1);
			}
		}

		StringBuffer sb = new StringBuffer();
		DToA.JS_dtostr(sb, oneArgMode, precision + precisionOffset, val);
		return sb.toString();
	}

	protected int findPrototypeId(String s) {
		byte id = 0;
		String X = null;
		char c;
		switch (s.length()) {
			case 7 :
				c = s.charAt(0);
				if (c == 116) {
					X = "toFixed";
					id = 6;
				} else if (c == 118) {
					X = "valueOf";
					id = 5;
				}
				break;
			case 8 :
				c = s.charAt(3);
				if (c == 111) {
					X = "toSource";
					id = 4;
				} else if (c == 116) {
					X = "toString";
					id = 2;
				}
			case 9 :
			case 10 :
			case 12 :
			default :
				break;
			case 11 :
				c = s.charAt(0);
				if (c == 99) {
					X = "constructor";
					id = 1;
				} else if (c == 116) {
					X = "toPrecision";
					id = 8;
				}
				break;
			case 13 :
				X = "toExponential";
				id = 7;
				break;
			case 14 :
				X = "toLocaleString";
				id = 3;
		}

		if (X != null && X != s && !X.equals(s)) {
			id = 0;
		}

		return id;
	}
}