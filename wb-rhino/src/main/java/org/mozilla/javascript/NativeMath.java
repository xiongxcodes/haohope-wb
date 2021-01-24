/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

final class NativeMath extends IdScriptableObject {
	static final long serialVersionUID = -8838847185801131569L;
	private static final Object MATH_TAG = "Math";
	private static final int Id_toSource = 1;
	private static final int Id_abs = 2;
	private static final int Id_acos = 3;
	private static final int Id_asin = 4;
	private static final int Id_atan = 5;
	private static final int Id_atan2 = 6;
	private static final int Id_ceil = 7;
	private static final int Id_cos = 8;
	private static final int Id_exp = 9;
	private static final int Id_floor = 10;
	private static final int Id_log = 11;
	private static final int Id_max = 12;
	private static final int Id_min = 13;
	private static final int Id_pow = 14;
	private static final int Id_random = 15;
	private static final int Id_round = 16;
	private static final int Id_sin = 17;
	private static final int Id_sqrt = 18;
	private static final int Id_tan = 19;
	private static final int LAST_METHOD_ID = 19;
	private static final int Id_E = 20;
	private static final int Id_PI = 21;
	private static final int Id_LN10 = 22;
	private static final int Id_LN2 = 23;
	private static final int Id_LOG2E = 24;
	private static final int Id_LOG10E = 25;
	private static final int Id_SQRT1_2 = 26;
	private static final int Id_SQRT2 = 27;
	private static final int MAX_ID = 27;

	static void init(Scriptable scope, boolean sealed) {
		NativeMath obj = new NativeMath();
		obj.activatePrototypeMap(27);
		obj.setPrototype(getObjectPrototype(scope));
		obj.setParentScope(scope);
		if (sealed) {
			obj.sealObject();
		}

		ScriptableObject.defineProperty(scope, "Math", obj, 2);
	}

	public String getClassName() {
		return "Math";
	}

	protected void initPrototypeId(int id) {
		String name;
		if (id <= 19) {
			byte x;
			switch (id) {
				case 1 :
					x = 0;
					name = "toSource";
					break;
				case 2 :
					x = 1;
					name = "abs";
					break;
				case 3 :
					x = 1;
					name = "acos";
					break;
				case 4 :
					x = 1;
					name = "asin";
					break;
				case 5 :
					x = 1;
					name = "atan";
					break;
				case 6 :
					x = 2;
					name = "atan2";
					break;
				case 7 :
					x = 1;
					name = "ceil";
					break;
				case 8 :
					x = 1;
					name = "cos";
					break;
				case 9 :
					x = 1;
					name = "exp";
					break;
				case 10 :
					x = 1;
					name = "floor";
					break;
				case 11 :
					x = 1;
					name = "log";
					break;
				case 12 :
					x = 2;
					name = "max";
					break;
				case 13 :
					x = 2;
					name = "min";
					break;
				case 14 :
					x = 2;
					name = "pow";
					break;
				case 15 :
					x = 0;
					name = "random";
					break;
				case 16 :
					x = 1;
					name = "round";
					break;
				case 17 :
					x = 1;
					name = "sin";
					break;
				case 18 :
					x = 1;
					name = "sqrt";
					break;
				case 19 :
					x = 1;
					name = "tan";
					break;
				default :
					throw new IllegalStateException(String.valueOf(id));
			}

			this.initPrototypeMethod(MATH_TAG, id, name, x);
		} else {
			double x1;
			switch (id) {
				case 20 :
					x1 = 2.718281828459045D;
					name = "E";
					break;
				case 21 :
					x1 = 3.141592653589793D;
					name = "PI";
					break;
				case 22 :
					x1 = 2.302585092994046D;
					name = "LN10";
					break;
				case 23 :
					x1 = 0.6931471805599453D;
					name = "LN2";
					break;
				case 24 :
					x1 = 1.4426950408889634D;
					name = "LOG2E";
					break;
				case 25 :
					x1 = 0.4342944819032518D;
					name = "LOG10E";
					break;
				case 26 :
					x1 = 0.7071067811865476D;
					name = "SQRT1_2";
					break;
				case 27 :
					x1 = 1.4142135623730951D;
					name = "SQRT2";
					break;
				default :
					throw new IllegalStateException(String.valueOf(id));
			}

			this.initPrototypeValue(id, name, ScriptRuntime.wrapNumber(x1), 7);
		}

	}

	public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
			Scriptable thisObj, Object[] args) {
		if (!f.hasTag(MATH_TAG)) {
			return super.execIdCall(f, cx, scope, thisObj, args);
		} else {
			int methodId = f.methodId();
			double x;
			switch (methodId) {
				case 1 :
					return "Math";
				case 2 :
					x = ScriptRuntime.toNumber(args, 0);
					x = x == 0.0D ? 0.0D : (x < 0.0D ? -x : x);
					break;
				case 3 :
				case 4 :
					x = ScriptRuntime.toNumber(args, 0);
					if (x == x && -1.0D <= x && x <= 1.0D) {
						x = methodId == 3 ? Math.acos(x) : Math.asin(x);
					} else {
						x = Double.NaN;
					}
					break;
				case 5 :
					x = ScriptRuntime.toNumber(args, 0);
					x = Math.atan(x);
					break;
				case 6 :
					x = ScriptRuntime.toNumber(args, 0);
					x = Math.atan2(x, ScriptRuntime.toNumber(args, 1));
					break;
				case 7 :
					x = ScriptRuntime.toNumber(args, 0);
					x = Math.ceil(x);
					break;
				case 8 :
					x = ScriptRuntime.toNumber(args, 0);
					x = x != Double.POSITIVE_INFINITY
							&& x != Double.NEGATIVE_INFINITY
							? Math.cos(x)
							: Double.NaN;
					break;
				case 9 :
					x = ScriptRuntime.toNumber(args, 0);
					x = x == Double.POSITIVE_INFINITY
							? x
							: (x == Double.NEGATIVE_INFINITY ? 0.0D : Math
									.exp(x));
					break;
				case 10 :
					x = ScriptRuntime.toNumber(args, 0);
					x = Math.floor(x);
					break;
				case 11 :
					x = ScriptRuntime.toNumber(args, 0);
					x = x < 0.0D ? Double.NaN : Math.log(x);
					break;
				case 12 :
				case 13 :
					x = methodId == 12
							? Double.NEGATIVE_INFINITY
							: Double.POSITIVE_INFINITY;

					for (int arg11 = 0; arg11 != args.length; ++arg11) {
						double d = ScriptRuntime.toNumber(args[arg11]);
						if (d != d) {
							x = d;
							return ScriptRuntime.wrapNumber(x);
						}

						if (methodId == 12) {
							x = Math.max(x, d);
						} else {
							x = Math.min(x, d);
						}
					}

					return ScriptRuntime.wrapNumber(x);
				case 14 :
					x = ScriptRuntime.toNumber(args, 0);
					x = this.js_pow(x, ScriptRuntime.toNumber(args, 1));
					break;
				case 15 :
					x = Math.random();
					break;
				case 16 :
					x = ScriptRuntime.toNumber(args, 0);
					if (x == x && x != Double.POSITIVE_INFINITY
							&& x != Double.NEGATIVE_INFINITY) {
						long l = Math.round(x);
						if (l != 0L) {
							x = (double) l;
						} else if (x < 0.0D) {
							x = ScriptRuntime.negativeZero;
						} else if (x != 0.0D) {
							x = 0.0D;
						}
					}
					break;
				case 17 :
					x = ScriptRuntime.toNumber(args, 0);
					x = x != Double.POSITIVE_INFINITY
							&& x != Double.NEGATIVE_INFINITY
							? Math.sin(x)
							: Double.NaN;
					break;
				case 18 :
					x = ScriptRuntime.toNumber(args, 0);
					x = Math.sqrt(x);
					break;
				case 19 :
					x = ScriptRuntime.toNumber(args, 0);
					x = Math.tan(x);
					break;
				default :
					throw new IllegalStateException(String.valueOf(methodId));
			}

			return ScriptRuntime.wrapNumber(x);
		}
	}

	private double js_pow(double x, double y) {
		double result;
		if (y != y) {
			result = y;
		} else if (y == 0.0D) {
			result = 1.0D;
		} else {
			long y_long;
			if (x == 0.0D) {
				if (1.0D / x > 0.0D) {
					result = y > 0.0D ? 0.0D : Double.POSITIVE_INFINITY;
				} else {
					y_long = (long) y;
					if ((double) y_long == y && (y_long & 1L) != 0L) {
						result = y > 0.0D ? -0.0D : Double.NEGATIVE_INFINITY;
					} else {
						result = y > 0.0D ? 0.0D : Double.POSITIVE_INFINITY;
					}
				}
			} else {
				result = Math.pow(x, y);
				if (result != result) {
					if (y == Double.POSITIVE_INFINITY) {
						if (x >= -1.0D && 1.0D >= x) {
							if (-1.0D < x && x < 1.0D) {
								result = 0.0D;
							}
						} else {
							result = Double.POSITIVE_INFINITY;
						}
					} else if (y == Double.NEGATIVE_INFINITY) {
						if (x >= -1.0D && 1.0D >= x) {
							if (-1.0D < x && x < 1.0D) {
								result = Double.POSITIVE_INFINITY;
							}
						} else {
							result = 0.0D;
						}
					} else if (x == Double.POSITIVE_INFINITY) {
						result = y > 0.0D ? Double.POSITIVE_INFINITY : 0.0D;
					} else if (x == Double.NEGATIVE_INFINITY) {
						y_long = (long) y;
						if ((double) y_long == y && (y_long & 1L) != 0L) {
							result = y > 0.0D
									? Double.NEGATIVE_INFINITY
									: -0.0D;
						} else {
							result = y > 0.0D ? Double.POSITIVE_INFINITY : 0.0D;
						}
					}
				}
			}
		}

		return result;
	}

	protected int findPrototypeId(String s) {
		byte id;
		String X;
		id = 0;
		X = null;
		char c;
		label106 : switch (s.length()) {
			case 1 :
				if (s.charAt(0) == 69) {
					id = 20;
					return id;
				}
				break;
			case 2 :
				if (s.charAt(0) == 80 && s.charAt(1) == 73) {
					id = 21;
					return id;
				}
				break;
			case 3 :
				switch (s.charAt(0)) {
					case 'L' :
						if (s.charAt(2) == 50 && s.charAt(1) == 78) {
							id = 23;
							return id;
						}
						break label106;
					case 'a' :
						if (s.charAt(2) == 115 && s.charAt(1) == 98) {
							id = 2;
							return id;
						}
						break label106;
					case 'c' :
						if (s.charAt(2) == 115 && s.charAt(1) == 111) {
							id = 8;
							return id;
						}
						break label106;
					case 'e' :
						if (s.charAt(2) == 112 && s.charAt(1) == 120) {
							id = 9;
							return id;
						}
						break label106;
					case 'l' :
						if (s.charAt(2) == 103 && s.charAt(1) == 111) {
							id = 11;
							return id;
						}
						break label106;
					case 'm' :
						c = s.charAt(2);
						if (c == 110) {
							if (s.charAt(1) == 105) {
								id = 13;
								return id;
							}
						} else if (c == 120 && s.charAt(1) == 97) {
							id = 12;
							return id;
						}
						break label106;
					case 'p' :
						if (s.charAt(2) == 119 && s.charAt(1) == 111) {
							id = 14;
							return id;
						}
						break label106;
					case 's' :
						if (s.charAt(2) == 110 && s.charAt(1) == 105) {
							id = 17;
							return id;
						}
						break label106;
					case 't' :
						if (s.charAt(2) == 110 && s.charAt(1) == 97) {
							id = 19;
							return id;
						}
					default :
						break label106;
				}
			case 4 :
				switch (s.charAt(1)) {
					case 'N' :
						X = "LN10";
						id = 22;
						break label106;
					case 'c' :
						X = "acos";
						id = 3;
						break label106;
					case 'e' :
						X = "ceil";
						id = 7;
						break label106;
					case 'q' :
						X = "sqrt";
						id = 18;
						break label106;
					case 's' :
						X = "asin";
						id = 4;
						break label106;
					case 't' :
						X = "atan";
						id = 5;
					default :
						break label106;
				}
			case 5 :
				switch (s.charAt(0)) {
					case 'L' :
						X = "LOG2E";
						id = 24;
						break label106;
					case 'S' :
						X = "SQRT2";
						id = 27;
						break label106;
					case 'a' :
						X = "atan2";
						id = 6;
						break label106;
					case 'f' :
						X = "floor";
						id = 10;
						break label106;
					case 'r' :
						X = "round";
						id = 16;
					default :
						break label106;
				}
			case 6 :
				c = s.charAt(0);
				if (c == 76) {
					X = "LOG10E";
					id = 25;
				} else if (c == 114) {
					X = "random";
					id = 15;
				}
				break;
			case 7 :
				X = "SQRT1_2";
				id = 26;
				break;
			case 8 :
				X = "toSource";
				id = 1;
		}

		if (X != null && X != s && !X.equals(s)) {
			id = 0;
		}

		return id;
	}
}