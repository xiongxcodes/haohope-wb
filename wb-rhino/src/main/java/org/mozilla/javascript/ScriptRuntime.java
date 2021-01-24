/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.mozilla.javascript.Arguments;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.ClassCache;
import org.mozilla.javascript.ClassShutter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.DToA;
import org.mozilla.javascript.DefaultErrorReporter;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.Evaluator;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.LazilyLoadedCtor;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeBoolean;
import org.mozilla.javascript.NativeCall;
import org.mozilla.javascript.NativeDate;
import org.mozilla.javascript.NativeError;
import org.mozilla.javascript.NativeFunction;
import org.mozilla.javascript.NativeGlobal;
import org.mozilla.javascript.NativeIterator;
import org.mozilla.javascript.NativeMath;
import org.mozilla.javascript.NativeNumber;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.NativeScript;
import org.mozilla.javascript.NativeString;
import org.mozilla.javascript.NativeWith;
import org.mozilla.javascript.ObjToIntMap;
import org.mozilla.javascript.Ref;
import org.mozilla.javascript.RefCallable;
import org.mozilla.javascript.RegExpProxy;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.SpecialRef;
import org.mozilla.javascript.TokenStream;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.WrappedException;
import org.mozilla.javascript.Wrapper;
import org.mozilla.javascript.NativeIterator.StopIteration;
import org.mozilla.javascript.xml.XMLLib;
import org.mozilla.javascript.xml.XMLObject;

public class ScriptRuntime {
	public static final Class<?> BooleanClass = Kit
			.classOrNull("java.lang.Boolean");
	public static final Class<?> ByteClass = Kit.classOrNull("java.lang.Byte");
	public static final Class<?> CharacterClass = Kit
			.classOrNull("java.lang.Character");
	public static final Class<?> ClassClass = Kit
			.classOrNull("java.lang.Class");
	public static final Class<?> DoubleClass = Kit
			.classOrNull("java.lang.Double");
	public static final Class<?> FloatClass = Kit
			.classOrNull("java.lang.Float");
	public static final Class<?> IntegerClass = Kit
			.classOrNull("java.lang.Integer");
	public static final Class<?> LongClass = Kit.classOrNull("java.lang.Long");
	public static final Class<?> NumberClass = Kit
			.classOrNull("java.lang.Number");
	public static final Class<?> ObjectClass = Kit
			.classOrNull("java.lang.Object");
	public static final Class<?> ShortClass = Kit
			.classOrNull("java.lang.Short");
	public static final Class<?> StringClass = Kit
			.classOrNull("java.lang.String");
	public static final Class<?> DateClass = Kit.classOrNull("java.util.Date");
	public static final Class<?> ContextClass = Kit
			.classOrNull("org.mozilla.javascript.Context");
	public static final Class<?> ContextFactoryClass = Kit
			.classOrNull("org.mozilla.javascript.ContextFactory");
	public static final Class<?> FunctionClass = Kit
			.classOrNull("org.mozilla.javascript.Function");
	public static final Class<?> ScriptableObjectClass = Kit
			.classOrNull("org.mozilla.javascript.ScriptableObject");
	public static final Class<Scriptable> ScriptableClass = Scriptable.class;
	private static final String[] lazilyNames = new String[]{"RegExp",
			"org.mozilla.javascript.regexp.NativeRegExp", "Packages",
			"org.mozilla.javascript.NativeJavaTopPackage", "java",
			"org.mozilla.javascript.NativeJavaTopPackage", "javax",
			"org.mozilla.javascript.NativeJavaTopPackage", "org",
			"org.mozilla.javascript.NativeJavaTopPackage", "com",
			"org.mozilla.javascript.NativeJavaTopPackage", "edu",
			"org.mozilla.javascript.NativeJavaTopPackage", "net",
			"org.mozilla.javascript.NativeJavaTopPackage", "getClass",
			"org.mozilla.javascript.NativeJavaTopPackage", "JavaAdapter",
			"org.mozilla.javascript.JavaAdapter", "JavaImporter",
			"org.mozilla.javascript.ImporterTopLevel", "Continuation",
			"org.mozilla.javascript.NativeContinuation", "XML", "(xml)",
			"XMLList", "(xml)", "Namespace", "(xml)", "QName", "(xml)"};
	private static final Object LIBRARY_SCOPE_KEY = "LIBRARY_SCOPE";
	public static final double NaN = Double
			.longBitsToDouble(9221120237041090560L);
	public static final double negativeZero = Double
			.longBitsToDouble(Long.MIN_VALUE);
	public static final Double NaNobj;
	private static final boolean MSJVM_BUG_WORKAROUNDS = true;
	private static final String DEFAULT_NS_TAG = "__default_namespace__";
	public static final int ENUMERATE_KEYS = 0;
	public static final int ENUMERATE_VALUES = 1;
	public static final int ENUMERATE_ARRAY = 2;
	public static final int ENUMERATE_KEYS_NO_ITERATOR = 3;
	public static final int ENUMERATE_VALUES_NO_ITERATOR = 4;
	public static final int ENUMERATE_ARRAY_NO_ITERATOR = 5;
	public static ScriptRuntime.MessageProvider messageProvider;
	public static final Object[] emptyArgs;
	public static final String[] emptyStrings;

	public static boolean isRhinoRuntimeType(Class<?> cl) {
		return cl.isPrimitive() ? cl != Character.TYPE : cl == StringClass
				|| cl == BooleanClass || NumberClass.isAssignableFrom(cl)
				|| ScriptableClass.isAssignableFrom(cl);
	}

	public static ScriptableObject initStandardObjects(Context cx,
			ScriptableObject scope, boolean sealed) {
		if (scope == null) {
			scope = new NativeObject();
		}

		((ScriptableObject) scope).associateValue(LIBRARY_SCOPE_KEY, scope);
		(new ClassCache()).associate((ScriptableObject) scope);
		BaseFunction.init((Scriptable) scope, sealed);
		NativeObject.init((Scriptable) scope, sealed);
		Scriptable objectProto = ScriptableObject
				.getObjectPrototype((Scriptable) scope);
		Scriptable functionProto = ScriptableObject
				.getFunctionPrototype((Scriptable) scope);
		functionProto.setPrototype(objectProto);
		if (((ScriptableObject) scope).getPrototype() == null) {
			((ScriptableObject) scope).setPrototype(objectProto);
		}

		NativeError.init((Scriptable) scope, sealed);
		NativeGlobal.init(cx, (Scriptable) scope, sealed);
		NativeArray.init((Scriptable) scope, sealed);
		if (cx.getOptimizationLevel() > 0) {
			NativeArray.setMaximumInitialCapacity(200000);
		}

		NativeString.init((Scriptable) scope, sealed);
		NativeBoolean.init((Scriptable) scope, sealed);
		NativeNumber.init((Scriptable) scope, sealed);
		NativeDate.init((Scriptable) scope, sealed);
		NativeMath.init((Scriptable) scope, sealed);
		NativeWith.init((Scriptable) scope, sealed);
		NativeCall.init((Scriptable) scope, sealed);
		NativeScript.init((Scriptable) scope, sealed);
		NativeIterator.init((ScriptableObject) scope, sealed);
		boolean withXml = cx.hasFeature(6)
				&& cx.getE4xImplementationFactory() != null;

		for (int i = 0; i != lazilyNames.length; i += 2) {
			String topProperty = lazilyNames[i];
			String className = lazilyNames[i + 1];
			if (withXml || !className.equals("(xml)")) {
				if (withXml && className.equals("(xml)")) {
					className = cx.getE4xImplementationFactory()
							.getImplementationClassName();
				}

				new LazilyLoadedCtor((ScriptableObject) scope, topProperty,
						className, sealed);
			}
		}

		return (ScriptableObject) scope;
	}

	public static ScriptableObject getLibraryScopeOrNull(Scriptable scope) {
		ScriptableObject libScope = (ScriptableObject) ScriptableObject
				.getTopScopeValue(scope, LIBRARY_SCOPE_KEY);
		return libScope;
	}

	public static boolean isJSLineTerminator(int c) {
		return (c & '?') != 0 ? false : c == 10 || c == 13 || c == 8232
				|| c == 8233;
	}

	public static Boolean wrapBoolean(boolean b) {
		return b ? Boolean.TRUE : Boolean.FALSE;
	}

	public static Integer wrapInt(int i) {
		return new Integer(i);
	}

	public static Number wrapNumber(double x) {
		return x != x ? NaNobj : new Double(x);
	}

	public static boolean toBoolean(Object val) {
		while (!(val instanceof Boolean)) {
			if (val != null && val != Undefined.instance) {
				if (val instanceof String) {
					return ((String) val).length() != 0;
				}

				if (!(val instanceof Number)) {
					if (val instanceof Scriptable) {
						if (val instanceof ScriptableObject
								&& ((ScriptableObject) val)
										.avoidObjectDetection()) {
							return false;
						}

						if (Context.getContext().isVersionECMA1()) {
							return true;
						}

						val = ((Scriptable) val).getDefaultValue(BooleanClass);
						if (!(val instanceof Scriptable)) {
							continue;
						}

						throw errorWithClassName("msg.primitive.expected", val);
					}

					warnAboutNonJSObject(val);
					return true;
				}

				double d = ((Number) val).doubleValue();
				return d == d && d != 0.0D;
			}

			return false;
		}

		return ((Boolean) val).booleanValue();
	}

	public static double toNumber(Object val) {
		while (!(val instanceof Number)) {
			if (val == null) {
				return 0.0D;
			}

			if (val == Undefined.instance) {
				return NaN;
			}

			if (val instanceof String) {
				return toNumber((String) val);
			}

			if (val instanceof Boolean) {
				return ((Boolean) val).booleanValue() ? 1.0D : 0.0D;
			}

			if (val instanceof Scriptable) {
				val = ((Scriptable) val).getDefaultValue(NumberClass);
				if (!(val instanceof Scriptable)) {
					continue;
				}

				throw errorWithClassName("msg.primitive.expected", val);
			}

			warnAboutNonJSObject(val);
			return NaN;
		}

		return ((Number) val).doubleValue();
	}

	public static double toNumber(Object[] args, int index) {
		return index < args.length ? toNumber(args[index]) : NaN;
	}

	static double stringToNumber(String s, int start, int radix) {
		char digitMax = 57;
		char lowerCaseBound = 97;
		char upperCaseBound = 65;
		int len = s.length();
		if (radix < 10) {
			digitMax = (char) (48 + radix - 1);
		}

		if (radix > 10) {
			lowerCaseBound = (char) (97 + radix - 10);
			upperCaseBound = (char) (65 + radix - 10);
		}

		double sum = 0.0D;

		int end;
		int digit;
		for (end = start; end < len; ++end) {
			char bitShiftInChar = s.charAt(end);
			if (48 <= bitShiftInChar && bitShiftInChar <= digitMax) {
				digit = bitShiftInChar - 48;
			} else if (97 <= bitShiftInChar && bitShiftInChar < lowerCaseBound) {
				digit = bitShiftInChar - 97 + 10;
			} else {
				if (65 > bitShiftInChar || bitShiftInChar >= upperCaseBound) {
					break;
				}

				digit = bitShiftInChar - 65 + 10;
			}

			sum = sum * (double) radix + (double) digit;
		}

		if (start == end) {
			return NaN;
		} else {
			if (sum >= 9.007199254740992E15D) {
				if (radix == 10) {
					try {
						return Double.valueOf(s.substring(start, end))
								.doubleValue();
					} catch (NumberFormatException arg23) {
						return NaN;
					}
				}

				if (radix == 2 || radix == 4 || radix == 8 || radix == 16
						|| radix == 32) {
					int arg24 = 1;
					digit = 0;
					boolean SKIP_LEADING_ZEROS = false;
					boolean FIRST_EXACT_53_BITS = true;
					boolean AFTER_BIT_53 = true;
					boolean ZEROS_AFTER_54 = true;
					boolean MIXED_AFTER_54 = true;
					byte state = 0;
					int exactBitsLimit = 53;
					double factor = 0.0D;
					boolean bit53 = false;
					boolean bit54 = false;

					while (true) {
						if (arg24 == 1) {
							if (start == end) {
								switch (state) {
									case 0 :
										sum = 0.0D;
										return sum;
									case 1 :
									case 2 :
									default :
										return sum;
									case 3 :
										if (bit54 & bit53) {
											++sum;
										}

										sum *= factor;
										return sum;
									case 4 :
										if (bit54) {
											++sum;
										}

										sum *= factor;
										return sum;
								}
							}

							char arg25 = s.charAt(start++);
							if (48 <= arg25 && arg25 <= 57) {
								digit = arg25 - 48;
							} else if (97 <= arg25 && arg25 <= 122) {
								digit = arg25 - 87;
							} else {
								digit = arg25 - 55;
							}

							arg24 = radix;
						}

						arg24 >>= 1;
						boolean bit = (digit & arg24) != 0;
						switch (state) {
							case 0 :
								if (bit) {
									--exactBitsLimit;
									sum = 1.0D;
									state = 1;
								}
								break;
							case 1 :
								sum *= 2.0D;
								if (bit) {
									++sum;
								}

								--exactBitsLimit;
								if (exactBitsLimit == 0) {
									bit53 = bit;
									state = 2;
								}
								break;
							case 2 :
								bit54 = bit;
								factor = 2.0D;
								state = 3;
								break;
							case 3 :
								if (bit) {
									state = 4;
								}
							case 4 :
								factor *= 2.0D;
						}
					}
				}
			}

			return sum;
		}
	}

	public static double toNumber(String s) {
		int len = s.length();

		for (int start = 0; start != len; ++start) {
			char startChar = s.charAt(start);
			if (!Character.isWhitespace(startChar)) {
				char end;
				if (startChar == 48) {
					if (start + 2 < len) {
						end = s.charAt(start + 1);
						if (end == 120 || end == 88) {
							return stringToNumber(s, start + 2, 16);
						}
					}
				} else if ((startChar == 43 || startChar == 45)
						&& start + 3 < len && s.charAt(start + 1) == 48) {
					end = s.charAt(start + 2);
					if (end == 120 || end == 88) {
						double endChar = stringToNumber(s, start + 3, 16);
						return startChar == 45 ? -endChar : endChar;
					}
				}

				int arg9;
				char arg10;
				for (arg9 = len - 1; Character.isWhitespace(arg10 = s
						.charAt(arg9)); --arg9) {
					;
				}

				if (arg10 == 121) {
					if (startChar == 43 || startChar == 45) {
						++start;
					}

					if (start + 7 == arg9
							&& s.regionMatches(start, "Infinity", 0, 8)) {
						return startChar == 45
								? Double.NEGATIVE_INFINITY
								: Double.POSITIVE_INFINITY;
					}

					return NaN;
				}

				String sub = s.substring(start, arg9 + 1);

				for (int ex = sub.length() - 1; ex >= 0; --ex) {
					char c = sub.charAt(ex);
					if ((48 > c || c > 57) && c != 46 && c != 101 && c != 69
							&& c != 43 && c != 45) {
						return NaN;
					}
				}

				try {
					return Double.valueOf(sub).doubleValue();
				} catch (NumberFormatException arg8) {
					return NaN;
				}
			}
		}

		return 0.0D;
	}

	public static Object[] padArguments(Object[] args, int count) {
		if (count < args.length) {
			return args;
		} else {
			Object[] result = new Object[count];

			int i;
			for (i = 0; i < args.length; ++i) {
				result[i] = args[i];
			}

			while (i < count) {
				result[i] = Undefined.instance;
				++i;
			}

			return result;
		}
	}

	public static String escapeString(String s) {
		return escapeString(s, '\"');
	}

	public static String escapeString(String s, char escapeQuote) {
		if (escapeQuote != 34 && escapeQuote != 39) {
			Kit.codeBug();
		}

		StringBuffer sb = null;
		int i = 0;

		for (int L = s.length(); i != L; ++i) {
			char c = s.charAt(i);
			if (32 <= c && c <= 126 && c != escapeQuote && c != 92) {
				if (sb != null) {
					sb.append((char) c);
				}
			} else {
				if (sb == null) {
					sb = new StringBuffer(L + 3);
					sb.append(s);
					sb.setLength(i);
				}

				byte escape = -1;
				switch (c) {
					case '\b' :
						escape = 98;
						break;
					case '\t' :
						escape = 116;
						break;
					case '\n' :
						escape = 110;
						break;
					case '' :
						escape = 118;
						break;
					case '\f' :
						escape = 102;
						break;
					case '\r' :
						escape = 114;
						break;
					case ' ' :
						escape = 32;
						break;
					case '\\' :
						escape = 92;
				}

				if (escape >= 0) {
					sb.append('\\');
					sb.append((char) escape);
				} else if (c == escapeQuote) {
					sb.append('\\');
					sb.append(escapeQuote);
				} else {
					byte hexSize;
					if (c < 256) {
						sb.append("\\x");
						hexSize = 2;
					} else {
						sb.append("\\u");
						hexSize = 4;
					}

					for (int shift = (hexSize - 1) * 4; shift >= 0; shift -= 4) {
						int digit = 15 & c >> shift;
						int hc = digit < 10 ? 48 + digit : 87 + digit;
						sb.append((char) hc);
					}
				}
			}
		}

		return sb == null ? s : sb.toString();
	}

	static boolean isValidIdentifierName(String s) {
		int L = s.length();
		if (L == 0) {
			return false;
		} else if (!Character.isJavaIdentifierStart(s.charAt(0))) {
			return false;
		} else {
			for (int i = 1; i != L; ++i) {
				if (!Character.isJavaIdentifierPart(s.charAt(i))) {
					return false;
				}
			}

			return !TokenStream.isKeyword(s);
		}
	}

	public static String toString(Object val) {
		while (val != null) {
			if (val == Undefined.instance) {
				return "undefined";
			}

			if (val instanceof String) {
				return (String) val;
			}

			if (val instanceof Number) {
				return numberToString(((Number) val).doubleValue(), 10);
			}

			if (val instanceof Scriptable) {
				val = ((Scriptable) val).getDefaultValue(StringClass);
				if (!(val instanceof Scriptable)) {
					continue;
				}

				throw errorWithClassName("msg.primitive.expected", val);
			}

			return val.toString();
		}

		return "null";
	}

	static String defaultObjectToString(Scriptable obj) {
		return "[object " + obj.getClassName() + ']';
	}

	public static String toString(Object[] args, int index) {
		return index < args.length ? toString(args[index]) : "undefined";
	}

	public static String toString(double val) {
		return numberToString(val, 10);
	}

	public static String numberToString(double d, int base) {
		if (d != d) {
			return "NaN";
		} else if (d == Double.POSITIVE_INFINITY) {
			return "Infinity";
		} else if (d == Double.NEGATIVE_INFINITY) {
			return "-Infinity";
		} else if (d == 0.0D) {
			return "0";
		} else if (base >= 2 && base <= 36) {
			if (base != 10) {
				return DToA.JS_dtobasestr(base, d);
			} else {
				StringBuffer result = new StringBuffer();
				DToA.JS_dtostr(result, 0, 0, d);
				return result.toString();
			}
		} else {
			throw Context.reportRuntimeError1("msg.bad.radix",
					Integer.toString(base));
		}
	}

	static String uneval(Context cx, Scriptable scope, Object value) {
		if (value == null) {
			return "null";
		} else if (value == Undefined.instance) {
			return "undefined";
		} else if (value instanceof String) {
			String obj2 = escapeString((String) value);
			StringBuffer v1 = new StringBuffer(obj2.length() + 2);
			v1.append('\"');
			v1.append(obj2);
			v1.append('\"');
			return v1.toString();
		} else if (value instanceof Number) {
			double obj1 = ((Number) value).doubleValue();
			return obj1 == 0.0D && 1.0D / obj1 < 0.0D ? "-0" : toString(obj1);
		} else if (value instanceof Boolean) {
			return toString(value);
		} else if (value instanceof Scriptable) {
			Scriptable obj = (Scriptable) value;
			if (ScriptableObject.hasProperty(obj, "toSource")) {
				Object v = ScriptableObject.getProperty(obj, "toSource");
				if (v instanceof Function) {
					Function f = (Function) v;
					return toString(f.call(cx, scope, obj, emptyArgs));
				}
			}

			return toString(value);
		} else {
			warnAboutNonJSObject(value);
			return value.toString();
		}
	}

	static String defaultObjectToSource(Context cx, Scriptable scope,
			Scriptable thisObj, Object[] args) {
		boolean toplevel;
		boolean iterating;
		if (cx.iterating == null) {
			toplevel = true;
			iterating = false;
			cx.iterating = new ObjToIntMap(31);
		} else {
			toplevel = false;
			iterating = cx.iterating.has(thisObj);
		}

		StringBuffer result = new StringBuffer(128);
		if (toplevel) {
			result.append("(");
		}

		result.append('{');

		try {
			if (!iterating) {
				cx.iterating.intern(thisObj);
				Object[] ids = thisObj.getIds();

				for (int i = 0; i < ids.length; ++i) {
					Object id = ids[i];
					Object value;
					if (id instanceof Integer) {
						int strId = ((Integer) id).intValue();
						value = thisObj.get(strId, thisObj);
						if (value == Scriptable.NOT_FOUND) {
							continue;
						}

						if (i > 0) {
							result.append(", ");
						}

						result.append(strId);
					} else {
						String arg14 = (String) id;
						value = thisObj.get(arg14, thisObj);
						if (value == Scriptable.NOT_FOUND) {
							continue;
						}

						if (i > 0) {
							result.append(", ");
						}

						if (isValidIdentifierName(arg14)) {
							result.append(arg14);
						} else {
							result.append('\'');
							result.append(escapeString(arg14, '\''));
							result.append('\'');
						}
					}

					result.append(':');
					result.append(uneval(cx, scope, value));
				}
			}
		} finally {
			if (toplevel) {
				cx.iterating = null;
			}

		}

		result.append('}');
		if (toplevel) {
			result.append(')');
		}

		return result.toString();
	}

	public static Scriptable toObject(Scriptable scope, Object val) {
		return val instanceof Scriptable ? (Scriptable) val : toObject(
				Context.getContext(), scope, val);
	}

	public static Scriptable toObjectOrNull(Context cx, Object obj) {
		return obj instanceof Scriptable ? (Scriptable) obj : (obj != null
				&& obj != Undefined.instance ? toObject(cx,
				getTopCallScope(cx), obj) : null);
	}

	public static Scriptable toObjectOrNull(Context cx, Object obj,
			Scriptable scope) {
		return obj instanceof Scriptable ? (Scriptable) obj : (obj != null
				&& obj != Undefined.instance ? toObject(cx, scope, obj) : null);
	}

	public static Scriptable toObject(Scriptable scope, Object val,
			Class<?> staticClass) {
		return val instanceof Scriptable ? (Scriptable) val : toObject(
				Context.getContext(), scope, val);
	}

	public static Scriptable toObject(Context cx, Scriptable scope, Object val) {
		if (val instanceof Scriptable) {
			return (Scriptable) val;
		} else if (val == null) {
			throw typeError0("msg.null.to.object");
		} else if (val == Undefined.instance) {
			throw typeError0("msg.undef.to.object");
		} else {
			String className = val instanceof String
					? "String"
					: (val instanceof Number
							? "Number"
							: (val instanceof Boolean ? "Boolean" : null));
			if (className != null) {
				Object[] wrapped1 = new Object[]{val};
				scope = ScriptableObject.getTopLevelScope(scope);
				return newObject(cx, scope, className, wrapped1);
			} else {
				Object wrapped = cx.getWrapFactory().wrap(cx, scope, val,
						(Class) null);
				if (wrapped instanceof Scriptable) {
					return (Scriptable) wrapped;
				} else {
					throw errorWithClassName("msg.invalid.type", val);
				}
			}
		}
	}

	public static Scriptable toObject(Context cx, Scriptable scope, Object val,
			Class<?> staticClass) {
		return toObject(cx, scope, val);
	}

	public static Object call(Context cx, Object fun, Object thisArg,
			Object[] args, Scriptable scope) {
		if (!(fun instanceof Function)) {
			throw notFunctionError(toString(fun));
		} else {
			Function function = (Function) fun;
			Scriptable thisObj = toObjectOrNull(cx, thisArg);
			if (thisObj == null) {
				throw undefCallError(thisObj, "function");
			} else {
				return function.call(cx, scope, thisObj, args);
			}
		}
	}

	public static Scriptable newObject(Context cx, Scriptable scope,
			String constructorName, Object[] args) {
		scope = ScriptableObject.getTopLevelScope(scope);
		Function ctor = getExistingCtor(cx, scope, constructorName);
		if (args == null) {
			args = emptyArgs;
		}

		return ctor.construct(cx, scope, args);
	}

	public static double toInteger(Object val) {
		return toInteger(toNumber(val));
	}

	public static double toInteger(double d) {
		return d != d ? 0.0D : (d != 0.0D && d != Double.POSITIVE_INFINITY
				&& d != Double.NEGATIVE_INFINITY ? (d > 0.0D
				? Math.floor(d)
				: Math.ceil(d)) : d);
	}

	public static double toInteger(Object[] args, int index) {
		return index < args.length ? toInteger(args[index]) : 0.0D;
	}

	public static int toInt32(Object val) {
		return val instanceof Integer
				? ((Integer) val).intValue()
				: toInt32(toNumber(val));
	}

	public static int toInt32(Object[] args, int index) {
		return index < args.length ? toInt32(args[index]) : 0;
	}

	public static int toInt32(double d) {
		int id = (int) d;
		if ((double) id == d) {
			return id;
		} else if (d == d && d != Double.POSITIVE_INFINITY
				&& d != Double.NEGATIVE_INFINITY) {
			d = d >= 0.0D ? Math.floor(d) : Math.ceil(d);
			double two32 = 4.294967296E9D;
			d = Math.IEEEremainder(d, two32);
			long l = (long) d;
			return (int) l;
		} else {
			return 0;
		}
	}

	public static long toUint32(double d) {
		long l = (long) d;
		if ((double) l == d) {
			return l & 4294967295L;
		} else if (d == d && d != Double.POSITIVE_INFINITY
				&& d != Double.NEGATIVE_INFINITY) {
			d = d >= 0.0D ? Math.floor(d) : Math.ceil(d);
			double two32 = 4.294967296E9D;
			l = (long) Math.IEEEremainder(d, two32);
			return l & 4294967295L;
		} else {
			return 0L;
		}
	}

	public static long toUint32(Object val) {
		return toUint32(toNumber(val));
	}

	public static char toUint16(Object val) {
		double d = toNumber(val);
		int i = (int) d;
		if ((double) i == d) {
			return (char) i;
		} else if (d == d && d != Double.POSITIVE_INFINITY
				&& d != Double.NEGATIVE_INFINITY) {
			d = d >= 0.0D ? Math.floor(d) : Math.ceil(d);
			int int16 = 65536;
			i = (int) Math.IEEEremainder(d, (double) int16);
			return (char) i;
		} else {
			return ' ';
		}
	}

	public static Object setDefaultNamespace(Object namespace, Context cx) {
		Object scope = cx.currentActivationCall;
		if (scope == null) {
			scope = getTopCallScope(cx);
		}

		XMLLib xmlLib = currentXMLLib(cx);
		Object ns = xmlLib.toDefaultXmlNamespace(cx, namespace);
		if (!((Scriptable) scope).has("__default_namespace__",
				(Scriptable) scope)) {
			ScriptableObject.defineProperty((Scriptable) scope,
					"__default_namespace__", ns, 6);
		} else {
			((Scriptable) scope).put("__default_namespace__",
					(Scriptable) scope, ns);
		}

		return Undefined.instance;
	}

	public static Object searchDefaultNamespace(Context cx) {
		Object scope = cx.currentActivationCall;
		if (scope == null) {
			scope = getTopCallScope(cx);
		}

		Object nsObject;
		while (true) {
			Scriptable parent = ((Scriptable) scope).getParentScope();
			if (parent == null) {
				nsObject = ScriptableObject.getProperty((Scriptable) scope,
						"__default_namespace__");
				if (nsObject == Scriptable.NOT_FOUND) {
					return null;
				}
				break;
			}

			nsObject = ((Scriptable) scope).get("__default_namespace__",
					(Scriptable) scope);
			if (nsObject != Scriptable.NOT_FOUND) {
				break;
			}

			scope = parent;
		}

		return nsObject;
	}

	public static Object getTopLevelProp(Scriptable scope, String id) {
		scope = ScriptableObject.getTopLevelScope(scope);
		return ScriptableObject.getProperty(scope, id);
	}

	static Function getExistingCtor(Context cx, Scriptable scope,
			String constructorName) {
		Object ctorVal = ScriptableObject.getProperty(scope, constructorName);
		if (ctorVal instanceof Function) {
			return (Function) ctorVal;
		} else if (ctorVal == Scriptable.NOT_FOUND) {
			throw Context.reportRuntimeError1("msg.ctor.not.found",
					constructorName);
		} else {
			throw Context.reportRuntimeError1("msg.not.ctor", constructorName);
		}
	}

	private static long indexFromString(String str) {
		boolean MAX_VALUE_LENGTH = true;
		int len = str.length();
		if (len > 0) {
			byte i = 0;
			boolean negate = false;
			char c = str.charAt(0);
			if (c == 45 && len > 1) {
				c = str.charAt(1);
				i = 1;
				negate = true;
			}

			int arg8 = c - 48;
			if (0 <= arg8 && arg8 <= 9 && len <= (negate ? 11 : 10)) {
				int index = -arg8;
				int oldIndex = 0;
				int arg7 = i + 1;
				if (index != 0) {
					while (arg7 != len && 0 <= (arg8 = str.charAt(arg7) - 48)
							&& arg8 <= 9) {
						oldIndex = index;
						index = 10 * index - arg8;
						++arg7;
					}
				}

				if (arg7 == len
						&& (oldIndex > -214748364 || oldIndex == -214748364
								&& arg8 <= (negate ? 8 : 7))) {
					return 4294967295L & (long) (negate ? index : -index);
				}
			}
		}

		return -1L;
	}

	public static long testUint32String(String str) {
		boolean MAX_VALUE_LENGTH = true;
		int len = str.length();
		if (1 <= len && len <= 10) {
			char c = str.charAt(0);
			int arg6 = c - 48;
			if (arg6 == 0) {
				return len == 1 ? 0L : -1L;
			}

			if (1 <= arg6 && arg6 <= 9) {
				long v = (long) arg6;
				int i = 1;

				while (true) {
					if (i == len) {
						if (v >>> 32 == 0L) {
							return v;
						}
						break;
					}

					arg6 = str.charAt(i) - 48;
					if (0 > arg6 || arg6 > 9) {
						return -1L;
					}

					v = 10L * v + (long) arg6;
					++i;
				}
			}
		}

		return -1L;
	}

	static Object getIndexObject(String s) {
		long indexTest = indexFromString(s);
		return indexTest >= 0L ? new Integer((int) indexTest) : s;
	}

	static Object getIndexObject(double d) {
		int i = (int) d;
		return (double) i == d ? new Integer(i) : toString(d);
	}

	static String toStringIdOrIndex(Context cx, Object id) {
		if (id instanceof Number) {
			double s1 = ((Number) id).doubleValue();
			int index = (int) s1;
			if ((double) index == s1) {
				storeIndexResult(cx, index);
				return null;
			} else {
				return toString(id);
			}
		} else {
			String s;
			if (id instanceof String) {
				s = (String) id;
			} else {
				s = toString(id);
			}

			long indexTest = indexFromString(s);
			if (indexTest >= 0L) {
				storeIndexResult(cx, (int) indexTest);
				return null;
			} else {
				return s;
			}
		}
	}

	public static Object getObjectElem(Object obj, Object elem, Context cx) {
		return getObjectElem(obj, elem, cx, getTopCallScope(cx));
	}

	public static Object getObjectElem(Object obj, Object elem, Context cx,
			Scriptable scope) {
		Scriptable sobj = toObjectOrNull(cx, obj, scope);
		if (sobj == null) {
			throw undefReadError(obj, elem);
		} else {
			return getObjectElem(sobj, elem, cx);
		}
	}

	public static Object getObjectElem(Scriptable obj, Object elem, Context cx) {
		if (obj instanceof XMLObject) {
			XMLObject result1 = (XMLObject) obj;
			return result1.ecmaGet(cx, elem);
		} else {
			String s = toStringIdOrIndex(cx, elem);
			Object result;
			if (s == null) {
				int index = lastIndexResult(cx);
				result = ScriptableObject.getProperty(obj, index);
			} else {
				result = ScriptableObject.getProperty(obj, s);
			}

			if (result == Scriptable.NOT_FOUND) {
				result = Undefined.instance;
			}

			return result;
		}
	}

	public static Object getObjectProp(Object obj, String property, Context cx) {
		Scriptable sobj = toObjectOrNull(cx, obj);
		if (sobj == null) {
			throw undefReadError(obj, property);
		} else {
			return getObjectProp(sobj, property, cx);
		}
	}

	public static Object getObjectProp(Object obj, String property, Context cx,
			Scriptable scope) {
		Scriptable sobj = toObjectOrNull(cx, obj, scope);
		if (sobj == null) {
			throw undefReadError(obj, property);
		} else {
			return getObjectProp(sobj, property, cx);
		}
	}

	public static Object getObjectProp(Scriptable obj, String property,
			Context cx) {
		if (obj instanceof XMLObject) {
			XMLObject result1 = (XMLObject) obj;
			return result1.ecmaGet(cx, property);
		} else {
			Object result = ScriptableObject.getProperty(obj, property);
			if (result == Scriptable.NOT_FOUND) {
				if (cx.hasFeature(11)) {
					Context.reportWarning(getMessage1("msg.ref.undefined.prop",
							property));
				}

				result = Undefined.instance;
			}

			return result;
		}
	}

	public static Object getObjectPropNoWarn(Object obj, String property,
			Context cx) {
		Scriptable sobj = toObjectOrNull(cx, obj);
		if (sobj == null) {
			throw undefReadError(obj, property);
		} else {
			if (obj instanceof XMLObject) {
				getObjectProp(sobj, property, cx);
			}

			Object result = ScriptableObject.getProperty(sobj, property);
			return result == Scriptable.NOT_FOUND ? Undefined.instance : result;
		}
	}

	public static Object getObjectIndex(Object obj, double dblIndex, Context cx) {
		Scriptable sobj = toObjectOrNull(cx, obj);
		if (sobj == null) {
			throw undefReadError(obj, toString(dblIndex));
		} else {
			int index = (int) dblIndex;
			if ((double) index == dblIndex) {
				return getObjectIndex(sobj, index, cx);
			} else {
				String s = toString(dblIndex);
				return getObjectProp(sobj, s, cx);
			}
		}
	}

	public static Object getObjectIndex(Scriptable obj, int index, Context cx) {
		if (obj instanceof XMLObject) {
			XMLObject result1 = (XMLObject) obj;
			return result1.ecmaGet(cx, new Integer(index));
		} else {
			Object result = ScriptableObject.getProperty(obj, index);
			if (result == Scriptable.NOT_FOUND) {
				result = Undefined.instance;
			}

			return result;
		}
	}

	public static Object setObjectElem(Object obj, Object elem, Object value,
			Context cx) {
		Scriptable sobj = toObjectOrNull(cx, obj);
		if (sobj == null) {
			throw undefWriteError(obj, elem, value);
		} else {
			return setObjectElem(sobj, elem, value, cx);
		}
	}

	public static Object setObjectElem(Scriptable obj, Object elem,
			Object value, Context cx) {
		if (obj instanceof XMLObject) {
			XMLObject s1 = (XMLObject) obj;
			s1.ecmaPut(cx, elem, value);
			return value;
		} else {
			String s = toStringIdOrIndex(cx, elem);
			if (s == null) {
				int index = lastIndexResult(cx);
				ScriptableObject.putProperty(obj, index, value);
			} else {
				ScriptableObject.putProperty(obj, s, value);
			}

			return value;
		}
	}

	public static Object setObjectProp(Object obj, String property,
			Object value, Context cx) {
		Scriptable sobj = toObjectOrNull(cx, obj);
		if (sobj == null) {
			throw undefWriteError(obj, property, value);
		} else {
			return setObjectProp(sobj, property, value, cx);
		}
	}

	public static Object setObjectProp(Scriptable obj, String property,
			Object value, Context cx) {
		if (obj instanceof XMLObject) {
			XMLObject xmlObject = (XMLObject) obj;
			xmlObject.ecmaPut(cx, property, value);
		} else {
			ScriptableObject.putProperty(obj, property, value);
		}

		return value;
	}

	public static Object setObjectIndex(Object obj, double dblIndex,
			Object value, Context cx) {
		Scriptable sobj = toObjectOrNull(cx, obj);
		if (sobj == null) {
			throw undefWriteError(obj, String.valueOf(dblIndex), value);
		} else {
			int index = (int) dblIndex;
			if ((double) index == dblIndex) {
				return setObjectIndex(sobj, index, value, cx);
			} else {
				String s = toString(dblIndex);
				return setObjectProp(sobj, s, value, cx);
			}
		}
	}

	public static Object setObjectIndex(Scriptable obj, int index,
			Object value, Context cx) {
		if (obj instanceof XMLObject) {
			XMLObject xmlObject = (XMLObject) obj;
			xmlObject.ecmaPut(cx, new Integer(index), value);
		} else {
			ScriptableObject.putProperty(obj, index, value);
		}

		return value;
	}

	public static boolean deleteObjectElem(Scriptable target, Object elem,
			Context cx) {
		boolean result;
		if (target instanceof XMLObject) {
			XMLObject s = (XMLObject) target;
			result = s.ecmaDelete(cx, elem);
		} else {
			String s1 = toStringIdOrIndex(cx, elem);
			if (s1 == null) {
				int index = lastIndexResult(cx);
				result = ScriptableObject.deleteProperty(target, index);
			} else {
				result = ScriptableObject.deleteProperty(target, s1);
			}
		}

		return result;
	}

	public static boolean hasObjectElem(Scriptable target, Object elem,
			Context cx) {
		boolean result;
		if (target instanceof XMLObject) {
			XMLObject s = (XMLObject) target;
			result = s.ecmaHas(cx, elem);
		} else {
			String s1 = toStringIdOrIndex(cx, elem);
			if (s1 == null) {
				int index = lastIndexResult(cx);
				result = ScriptableObject.hasProperty(target, index);
			} else {
				result = ScriptableObject.hasProperty(target, s1);
			}
		}

		return result;
	}

	public static Object refGet(Ref ref, Context cx) {
		return ref.get(cx);
	}

	public static Object refSet(Ref ref, Object value, Context cx) {
		return ref.set(cx, value);
	}

	public static Object refDel(Ref ref, Context cx) {
		return wrapBoolean(ref.delete(cx));
	}

	static boolean isSpecialProperty(String s) {
		return s.equals("__proto__") || s.equals("__parent__");
	}

	public static Ref specialRef(Object obj, String specialProperty, Context cx) {
		return SpecialRef.createSpecial(cx, obj, specialProperty);
	}

	public static Object delete(Object obj, Object id, Context cx) {
		Scriptable sobj = toObjectOrNull(cx, obj);
		if (sobj == null) {
			String result1 = id == null ? "null" : id.toString();
			throw typeError2("msg.undef.prop.delete", toString(obj), result1);
		} else {
			boolean result = deleteObjectElem(sobj, id, cx);
			return wrapBoolean(result);
		}
	}

	public static Object name(Context cx, Scriptable scope, String name) {
		Scriptable parent = scope.getParentScope();
		if (parent == null) {
			Object result = topScopeName(cx, scope, name);
			if (result == Scriptable.NOT_FOUND) {
				throw notFoundError(scope, name);
			} else {
				return result;
			}
		} else {
			return nameOrFunction(cx, scope, parent, name, false);
		}
	}

	private static Object nameOrFunction(Context cx, Scriptable scope,
			Scriptable parentScope, String name, boolean asFunctionCall) {
		Object thisObj = scope;
		XMLObject firstXMLObject = null;

		Object result;
		while (true) {
			if (scope instanceof NativeWith) {
				Scriptable withObj = scope.getPrototype();
				if (withObj instanceof XMLObject) {
					XMLObject xmlObj = (XMLObject) withObj;
					if (xmlObj.ecmaHas(cx, name)) {
						thisObj = xmlObj;
						result = xmlObj.ecmaGet(cx, name);
						break;
					}

					if (firstXMLObject == null) {
						firstXMLObject = xmlObj;
					}
				} else {
					result = ScriptableObject.getProperty(withObj, name);
					if (result != Scriptable.NOT_FOUND) {
						thisObj = withObj;
						break;
					}
				}
			} else if (scope instanceof NativeCall) {
				result = scope.get(name, scope);
				if (result != Scriptable.NOT_FOUND) {
					if (asFunctionCall) {
						thisObj = ScriptableObject
								.getTopLevelScope(parentScope);
					}
					break;
				}
			} else {
				result = ScriptableObject.getProperty(scope, name);
				if (result != Scriptable.NOT_FOUND) {
					thisObj = scope;
					break;
				}
			}

			scope = parentScope;
			parentScope = parentScope.getParentScope();
			if (parentScope == null) {
				result = topScopeName(cx, scope, name);
				if (result == Scriptable.NOT_FOUND) {
					if (firstXMLObject == null || asFunctionCall) {
						throw notFoundError(scope, name);
					}

					result = firstXMLObject.ecmaGet(cx, name);
				}

				thisObj = scope;
				break;
			}
		}

		if (asFunctionCall) {
			if (!(result instanceof Callable)) {
				throw notFunctionError(result, name);
			}

			storeScriptable(cx, (Scriptable) thisObj);
		}

		return result;
	}

	private static Object topScopeName(Context cx, Scriptable scope, String name) {
		if (cx.useDynamicScope) {
			scope = checkDynamicScope(cx.topCallScope, scope);
		}

		return ScriptableObject.getProperty(scope, name);
	}

	public static Scriptable bind(Context cx, Scriptable scope, String id) {
		XMLObject firstXMLObject = null;
		Scriptable parent = scope.getParentScope();
		if (parent != null) {
			label47 : do {
				if (!(scope instanceof NativeWith)) {
					while (!ScriptableObject.hasProperty(scope, id)) {
						scope = parent;
						parent = parent.getParentScope();
						if (parent == null) {
							break label47;
						}
					}

					return scope;
				}

				Scriptable withObj = scope.getPrototype();
				if (withObj instanceof XMLObject) {
					XMLObject xmlObject = (XMLObject) withObj;
					if (xmlObject.ecmaHas(cx, id)) {
						return xmlObject;
					}

					if (firstXMLObject == null) {
						firstXMLObject = xmlObject;
					}
				} else if (ScriptableObject.hasProperty(withObj, id)) {
					return withObj;
				}

				scope = parent;
				parent = parent.getParentScope();
			} while (parent != null);
		}

		if (cx.useDynamicScope) {
			scope = checkDynamicScope(cx.topCallScope, scope);
		}

		return (Scriptable) (ScriptableObject.hasProperty(scope, id)
				? scope
				: firstXMLObject);
	}

	public static Object setName(Scriptable bound, Object value, Context cx,
			Scriptable scope, String id) {
		if (bound != null) {
			if (bound instanceof XMLObject) {
				XMLObject xmlObject = (XMLObject) bound;
				xmlObject.ecmaPut(cx, id, value);
			} else {
				ScriptableObject.putProperty(bound, id, value);
			}
		} else {
			if (cx.hasFeature(11) || cx.hasFeature(8)) {
				Context.reportWarning(getMessage1("msg.assn.create.strict", id));
			}

			bound = ScriptableObject.getTopLevelScope(scope);
			if (cx.useDynamicScope) {
				bound = checkDynamicScope(cx.topCallScope, bound);
			}

			bound.put(id, bound, value);
		}

		return value;
	}

	public static Object setConst(Scriptable bound, Object value, Context cx,
			String id) {
		if (bound instanceof XMLObject) {
			XMLObject xmlObject = (XMLObject) bound;
			xmlObject.ecmaPut(cx, id, value);
		} else {
			ScriptableObject.putConstProperty(bound, id, value);
		}

		return value;
	}

	public static Scriptable toIterator(Context cx, Scriptable scope,
			Scriptable obj, boolean keyOnly) {
		if (ScriptableObject.hasProperty(obj, "__iterator__")) {
			Object v = ScriptableObject.getProperty(obj, "__iterator__");
			if (!(v instanceof Callable)) {
				throw typeError0("msg.invalid.iterator");
			} else {
				Callable f = (Callable) v;
				Object[] args = new Object[]{keyOnly
						? Boolean.TRUE
						: Boolean.FALSE};
				v = f.call(cx, scope, obj, args);
				if (!(v instanceof Scriptable)) {
					throw typeError0("msg.iterator.primitive");
				} else {
					return (Scriptable) v;
				}
			}
		} else {
			return null;
		}
	}

	public static Object enumInit(Object value, Context cx, boolean enumValues) {
		return enumInit(value, cx, enumValues ? 1 : 0);
	}

	public static Object enumInit(Object value, Context cx, int enumType) {
		ScriptRuntime.IdEnumeration x = new ScriptRuntime.IdEnumeration();
		x.obj = toObjectOrNull(cx, value);
		if (x.obj == null) {
			return x;
		} else {
			x.enumType = enumType;
			x.iterator = null;
			if (enumType != 3 && enumType != 4 && enumType != 5) {
				x.iterator = toIterator(cx, x.obj.getParentScope(), x.obj, true);
			}

			if (x.iterator == null) {
				enumChangeObject(x);
			}

			return x;
		}
	}

	public static void setEnumNumbers(Object enumObj, boolean enumNumbers) {
		((ScriptRuntime.IdEnumeration) enumObj).enumNumbers = enumNumbers;
	}

	public static Boolean enumNext(Object enumObj) {
		ScriptRuntime.IdEnumeration x = (ScriptRuntime.IdEnumeration) enumObj;
		Object id;
		if (x.iterator != null) {
			id = ScriptableObject.getProperty(x.iterator, "next");
			if (!(id instanceof Callable)) {
				return Boolean.FALSE;
			} else {
				Callable intId2 = (Callable) id;
				Context cx = Context.getContext();

				try {
					x.currentId = intId2.call(cx, x.iterator.getParentScope(),
							x.iterator, emptyArgs);
					return Boolean.TRUE;
				} catch (JavaScriptException arg5) {
					if (arg5.getValue() instanceof StopIteration) {
						return Boolean.FALSE;
					} else {
						throw arg5;
					}
				}
			}
		} else {
			while (true) {
				label54 : do {
					while (x.obj != null) {
						if (x.index != x.ids.length) {
							id = x.ids[x.index++];
							continue label54;
						}

						x.obj = x.obj.getPrototype();
						enumChangeObject(x);
					}

					return Boolean.FALSE;
				} while (x.used != null && x.used.has(id));

				if (id instanceof String) {
					String intId1 = (String) id;
					if (x.obj.has(intId1, x.obj)) {
						x.currentId = intId1;
						break;
					}
				} else {
					int intId = ((Number) id).intValue();
					if (x.obj.has(intId, x.obj)) {
						x.currentId = x.enumNumbers
								? new Integer(intId)
								: String.valueOf(intId);
						break;
					}
				}
			}

			return Boolean.TRUE;
		}
	}

	public static Object enumId(Object enumObj, Context cx) {
		ScriptRuntime.IdEnumeration x = (ScriptRuntime.IdEnumeration) enumObj;
		if (x.iterator != null) {
			return x.currentId;
		} else {
			switch (x.enumType) {
				case 0 :
				case 3 :
					return x.currentId;
				case 1 :
				case 4 :
					return enumValue(enumObj, cx);
				case 2 :
				case 5 :
					Object[] elements = new Object[]{x.currentId,
							enumValue(enumObj, cx)};
					return cx.newArray(x.obj, elements);
				default :
					throw Kit.codeBug();
			}
		}
	}

	public static Object enumValue(Object enumObj, Context cx) {
		ScriptRuntime.IdEnumeration x = (ScriptRuntime.IdEnumeration) enumObj;
		String s = toStringIdOrIndex(cx, x.currentId);
		Object result;
		if (s == null) {
			int index = lastIndexResult(cx);
			result = x.obj.get(index, x.obj);
		} else {
			result = x.obj.get(s, x.obj);
		}

		return result;
	}

	private static void enumChangeObject(ScriptRuntime.IdEnumeration x) {
		Object[] ids;
		for (ids = null; x.obj != null; x.obj = x.obj.getPrototype()) {
			ids = x.obj.getIds();
			if (ids.length != 0) {
				break;
			}
		}

		if (x.obj != null && x.ids != null) {
			Object[] previous = x.ids;
			int L = previous.length;
			if (x.used == null) {
				x.used = new ObjToIntMap(L);
			}

			for (int i = 0; i != L; ++i) {
				x.used.intern(previous[i]);
			}
		}

		x.ids = ids;
		x.index = 0;
	}

	public static Callable getNameFunctionAndThis(String name, Context cx,
			Scriptable scope) {
		Scriptable parent = scope.getParentScope();
		if (parent == null) {
			Object result = topScopeName(cx, scope, name);
			if (!(result instanceof Callable)) {
				if (result == Scriptable.NOT_FOUND) {
					throw notFoundError(scope, name);
				} else {
					throw notFunctionError(result, name);
				}
			} else {
				storeScriptable(cx, scope);
				return (Callable) result;
			}
		} else {
			return (Callable) nameOrFunction(cx, scope, parent, name, true);
		}
	}

	public static Callable getElemFunctionAndThis(Object obj, Object elem,
			Context cx) {
		String s = toStringIdOrIndex(cx, elem);
		if (s != null) {
			return getPropFunctionAndThis(obj, s, cx);
		} else {
			int index = lastIndexResult(cx);
			Scriptable thisObj = toObjectOrNull(cx, obj);
			if (thisObj == null) {
				throw undefCallError(obj, String.valueOf(index));
			} else {
				Object value;
				while (true) {
					value = ScriptableObject.getProperty(thisObj, index);
					if (value != Scriptable.NOT_FOUND
							|| !(thisObj instanceof XMLObject)) {
						break;
					}

					XMLObject xmlObject = (XMLObject) thisObj;
					Scriptable extra = xmlObject.getExtraMethodSource(cx);
					if (extra == null) {
						break;
					}

					thisObj = extra;
				}

				if (!(value instanceof Callable)) {
					throw notFunctionError(value, elem);
				} else {
					storeScriptable(cx, thisObj);
					return (Callable) value;
				}
			}
		}
	}

	public static Callable getPropFunctionAndThis(Object obj, String property,
			Context cx) {
		Scriptable thisObj = toObjectOrNull(cx, obj);
		return getPropFunctionAndThisHelper(obj, property, cx, thisObj);
	}

	public static Callable getPropFunctionAndThis(Object obj, String property,
			Context cx, Scriptable scope) {
		Scriptable thisObj = toObjectOrNull(cx, obj, scope);
		return getPropFunctionAndThisHelper(obj, property, cx, thisObj);
	}

	private static Callable getPropFunctionAndThisHelper(Object obj,
			String property, Context cx, Scriptable thisObj) {
		if (thisObj == null) {
			throw undefCallError(obj, property);
		} else {
			Object value;
			while (true) {
				value = ScriptableObject.getProperty(thisObj, property);
				if (value != Scriptable.NOT_FOUND
						|| !(thisObj instanceof XMLObject)) {
					break;
				}

				XMLObject noSuchMethod = (XMLObject) thisObj;
				Scriptable extra = noSuchMethod.getExtraMethodSource(cx);
				if (extra == null) {
					break;
				}

				thisObj = extra;
			}

			if (!(value instanceof Callable)) {
				Object noSuchMethod1 = ScriptableObject.getProperty(thisObj,
						"__noSuchMethod__");
				if (!(noSuchMethod1 instanceof Callable)) {
					throw notFunctionError(thisObj, value, property);
				}

				value = new ScriptRuntime.NoSuchMethodShim(
						(Callable) noSuchMethod1, property);
			}

			storeScriptable(cx, thisObj);
			return (Callable) value;
		}
	}

	public static Callable getValueFunctionAndThis(Object value, Context cx) {
		if (!(value instanceof Callable)) {
			throw notFunctionError(value);
		} else {
			Callable f = (Callable) value;
			Scriptable thisObj = null;
			if (f instanceof Scriptable) {
				thisObj = ((Scriptable) f).getParentScope();
			}

			if (thisObj == null) {
				if (cx.topCallScope == null) {
					throw new IllegalStateException();
				}

				thisObj = cx.topCallScope;
			}

			if (thisObj.getParentScope() != null
					&& !(thisObj instanceof NativeWith)
					&& thisObj instanceof NativeCall) {
				thisObj = ScriptableObject.getTopLevelScope(thisObj);
			}

			storeScriptable(cx, thisObj);
			return f;
		}
	}

	public static Ref callRef(Callable function, Scriptable thisObj,
			Object[] args, Context cx) {
		if (function instanceof RefCallable) {
			RefCallable msg1 = (RefCallable) function;
			Ref ref = msg1.refCall(cx, thisObj, args);
			if (ref == null) {
				throw new IllegalStateException(msg1.getClass().getName()
						+ ".refCall() returned null");
			} else {
				return ref;
			}
		} else {
			String msg = getMessage1("msg.no.ref.from.function",
					toString(function));
			throw constructError("ReferenceError", msg);
		}
	}

	public static Scriptable newObject(Object fun, Context cx,
			Scriptable scope, Object[] args) {
		if (!(fun instanceof Function)) {
			throw notFunctionError(fun);
		} else {
			Function function = (Function) fun;
			return function.construct(cx, scope, args);
		}
	}

	public static Object callSpecial(Context cx, Callable fun,
			Scriptable thisObj, Object[] args, Scriptable scope,
			Scriptable callerThis, int callType, String filename, int lineNumber) {
		if (callType == 1) {
			if (NativeGlobal.isEvalFunction(fun)) {
				return evalSpecial(cx, scope, callerThis, args, filename,
						lineNumber);
			}
		} else {
			if (callType != 2) {
				throw Kit.codeBug();
			}

			if (NativeWith.isWithFunction(fun)) {
				throw Context.reportRuntimeError1("msg.only.from.new", "With");
			}
		}

		return fun.call(cx, scope, thisObj, args);
	}

	public static Object newSpecial(Context cx, Object fun, Object[] args,
			Scriptable scope, int callType) {
		if (callType == 1) {
			if (NativeGlobal.isEvalFunction(fun)) {
				throw typeError1("msg.not.ctor", "eval");
			}
		} else {
			if (callType != 2) {
				throw Kit.codeBug();
			}

			if (NativeWith.isWithFunction(fun)) {
				return NativeWith.newWithSpecial(cx, scope, args);
			}
		}

		return newObject(fun, cx, scope, args);
	}

	public static Object applyOrCall(boolean isApply, Context cx,
			Scriptable scope, Scriptable thisObj, Object[] args) {
		int L = args.length;
		Callable function = getCallable(thisObj);
		Scriptable callThis = null;
		if (L != 0) {
			callThis = toObjectOrNull(cx, args[0]);
		}

		if (callThis == null) {
			callThis = getTopCallScope(cx);
		}

		Object[] callArgs;
		if (isApply) {
			callArgs = L <= 1 ? emptyArgs : getApplyArguments(cx, args[1]);
		} else if (L <= 1) {
			callArgs = emptyArgs;
		} else {
			callArgs = new Object[L - 1];
			System.arraycopy(args, 1, callArgs, 0, L - 1);
		}

		return function.call(cx, scope, callThis, callArgs);
	}

	static Object[] getApplyArguments(Context cx, Object arg1) {
		if (arg1 != null && arg1 != Undefined.instance) {
			if (!(arg1 instanceof NativeArray) && !(arg1 instanceof Arguments)) {
				throw typeError0("msg.arg.isnt.array");
			} else {
				return cx.getElements((Scriptable) arg1);
			}
		} else {
			return emptyArgs;
		}
	}

	static Callable getCallable(Scriptable thisObj) {
		Callable function;
		if (thisObj instanceof Callable) {
			function = (Callable) thisObj;
		} else {
			Object value = thisObj.getDefaultValue(FunctionClass);
			if (!(value instanceof Callable)) {
				throw notFunctionError(value, thisObj);
			}

			function = (Callable) value;
		}

		return function;
	}

	public static Object evalSpecial(Context cx, Scriptable scope,
			Object thisArg, Object[] args, String filename, int lineNumber) {
		if (args.length < 1) {
			return Undefined.instance;
		} else {
			Object x = args[0];
			String sourceName1;
			if (!(x instanceof String)) {
				if (!cx.hasFeature(11) && !cx.hasFeature(9)) {
					sourceName1 = getMessage0("msg.eval.nonstring");
					Context.reportWarning(sourceName1);
					return x;
				} else {
					throw Context
							.reportRuntimeError0("msg.eval.nonstring.strict");
				}
			} else {
				if (filename == null) {
					int[] sourceName = new int[1];
					filename = Context.getSourcePositionFromStack(sourceName);
					if (filename != null) {
						lineNumber = sourceName[0];
					} else {
						filename = "";
					}
				}

				sourceName1 = makeUrlForGeneratedScript(true, filename,
						lineNumber);
				ErrorReporter reporter = DefaultErrorReporter.forEval(cx
						.getErrorReporter());
				Evaluator evaluator = Context.createInterpreter();
				if (evaluator == null) {
					throw new JavaScriptException("Interpreter not present",
							filename, lineNumber);
				} else {
					Script script = cx.compileString((String) x, evaluator,
							reporter, sourceName1, 1, (Object) null);
					evaluator.setEvalScriptFlag(script);
					Callable c = (Callable) script;
					return c.call(cx, scope, (Scriptable) thisArg, emptyArgs);
				}
			}
		}
	}

	public static String typeof(Object value) {
		if (value == null) {
			return "object";
		} else if (value == Undefined.instance) {
			return "undefined";
		} else if (value instanceof Scriptable) {
			return value instanceof ScriptableObject
					&& ((ScriptableObject) value).avoidObjectDetection()
					? "undefined"
					: (value instanceof XMLObject
							? "xml"
							: (value instanceof Callable
									? "function"
									: "object"));
		} else if (value instanceof String) {
			return "string";
		} else if (value instanceof Number) {
			return "number";
		} else if (value instanceof Boolean) {
			return "boolean";
		} else {
			throw errorWithClassName("msg.invalid.type", value);
		}
	}

	public static String typeofName(Scriptable scope, String id) {
		Context cx = Context.getContext();
		Scriptable val = bind(cx, scope, id);
		return val == null ? "undefined" : typeof(getObjectProp(val, id, cx));
	}

	public static Object add(Object val1, Object val2, Context cx) {
		if (val1 instanceof Number && val2 instanceof Number) {
			return wrapNumber(((Number) val1).doubleValue()
					+ ((Number) val2).doubleValue());
		} else {
			Object test;
			if (val1 instanceof XMLObject) {
				test = ((XMLObject) val1).addValues(cx, true, val2);
				if (test != Scriptable.NOT_FOUND) {
					return test;
				}
			}

			if (val2 instanceof XMLObject) {
				test = ((XMLObject) val2).addValues(cx, false, val1);
				if (test != Scriptable.NOT_FOUND) {
					return test;
				}
			}

			if (val1 instanceof Scriptable) {
				val1 = ((Scriptable) val1).getDefaultValue((Class) null);
			}

			if (val2 instanceof Scriptable) {
				val2 = ((Scriptable) val2).getDefaultValue((Class) null);
			}

			return !(val1 instanceof String) && !(val2 instanceof String)
					? (val1 instanceof Number && val2 instanceof Number
							? wrapNumber(((Number) val1).doubleValue()
									+ ((Number) val2).doubleValue())
							: wrapNumber(toNumber(val1) + toNumber(val2)))
					: toString(val1).concat(toString(val2));
		}
	}

	public static String add(String val1, Object val2) {
		return val1.concat(toString(val2));
	}

	public static String add(Object val1, String val2) {
		return toString(val1).concat(val2);
	}

	public static Object nameIncrDecr(Scriptable scopeChain, String id,
			int incrDecrMask) {
		return nameIncrDecr(scopeChain, id, Context.getContext(), incrDecrMask);
	}

	public static Object nameIncrDecr(Scriptable scopeChain, String id,
			Context cx, int incrDecrMask) {
		do {
			if (cx.useDynamicScope && scopeChain.getParentScope() == null) {
				scopeChain = checkDynamicScope(cx.topCallScope, scopeChain);
			}

			Scriptable target = scopeChain;

			do {
				Object value = target.get(id, scopeChain);
				if (value != Scriptable.NOT_FOUND) {
					return doScriptableIncrDecr(target, id, scopeChain, value,
							incrDecrMask);
				}

				target = target.getPrototype();
			} while (target != null);

			scopeChain = scopeChain.getParentScope();
		} while (scopeChain != null);

		throw notFoundError(scopeChain, id);
	}

	public static Object propIncrDecr(Object obj, String id, Context cx,
			int incrDecrMask) {
		Scriptable start = toObjectOrNull(cx, obj);
		if (start == null) {
			throw undefReadError(obj, id);
		} else {
			Scriptable target = start;

			do {
				Object value = target.get(id, start);
				if (value != Scriptable.NOT_FOUND) {
					return doScriptableIncrDecr(target, id, start, value,
							incrDecrMask);
				}

				target = target.getPrototype();
			} while (target != null);

			start.put(id, start, NaNobj);
			return NaNobj;
		}
	}

	private static Object doScriptableIncrDecr(Scriptable target, String id,
			Scriptable protoChainStart, Object value, int incrDecrMask) {
		boolean post = (incrDecrMask & 2) != 0;
		double number;
		if (value instanceof Number) {
			number = ((Number) value).doubleValue();
		} else {
			number = toNumber(value);
			if (post) {
				value = wrapNumber(number);
			}
		}

		if ((incrDecrMask & 1) == 0) {
			++number;
		} else {
			--number;
		}

		Number result = wrapNumber(number);
		target.put(id, protoChainStart, result);
		return post ? value : result;
	}

	public static Object elemIncrDecr(Object obj, Object index, Context cx,
			int incrDecrMask) {
		Object value = getObjectElem(obj, index, cx);
		boolean post = (incrDecrMask & 2) != 0;
		double number;
		if (value instanceof Number) {
			number = ((Number) value).doubleValue();
		} else {
			number = toNumber(value);
			if (post) {
				value = wrapNumber(number);
			}
		}

		if ((incrDecrMask & 1) == 0) {
			++number;
		} else {
			--number;
		}

		Number result = wrapNumber(number);
		setObjectElem((Object) obj, index, result, cx);
		return post ? value : result;
	}

	public static Object refIncrDecr(Ref ref, Context cx, int incrDecrMask) {
		Object value = ref.get(cx);
		boolean post = (incrDecrMask & 2) != 0;
		double number;
		if (value instanceof Number) {
			number = ((Number) value).doubleValue();
		} else {
			number = toNumber(value);
			if (post) {
				value = wrapNumber(number);
			}
		}

		if ((incrDecrMask & 1) == 0) {
			++number;
		} else {
			--number;
		}

		Number result = wrapNumber(number);
		ref.set(cx, result);
		return post ? value : result;
	}

	private static Object toPrimitive(Object val) {
		if (!(val instanceof Scriptable)) {
			return val;
		} else {
			Scriptable s = (Scriptable) val;
			Object result = s.getDefaultValue((Class) null);
			if (result instanceof Scriptable) {
				throw typeError0("msg.bad.default.value");
			} else {
				return result;
			}
		}
	}

	public static boolean eq(Object x, Object y) {
		Object d;
		if (x != null && x != Undefined.instance) {
			if (x instanceof Number) {
				return eqNumber(((Number) x).doubleValue(), y);
			} else if (x instanceof String) {
				return eqString((String) x, y);
			} else {
				Object unwrappedY;
				if (x instanceof Boolean) {
					boolean d2 = ((Boolean) x).booleanValue();
					if (y instanceof Boolean) {
						return d2 == ((Boolean) y).booleanValue();
					} else {
						if (y instanceof ScriptableObject) {
							unwrappedY = ((ScriptableObject) y)
									.equivalentValues(x);
							if (unwrappedY != Scriptable.NOT_FOUND) {
								return ((Boolean) unwrappedY).booleanValue();
							}
						}

						return eqNumber(d2 ? 1.0D : 0.0D, y);
					}
				} else if (!(x instanceof Scriptable)) {
					warnAboutNonJSObject(x);
					return x == y;
				} else if (y instanceof Scriptable) {
					if (x == y) {
						return true;
					} else {
						if (x instanceof ScriptableObject) {
							d = ((ScriptableObject) x).equivalentValues(y);
							if (d != Scriptable.NOT_FOUND) {
								return ((Boolean) d).booleanValue();
							}
						}

						if (y instanceof ScriptableObject) {
							d = ((ScriptableObject) y).equivalentValues(x);
							if (d != Scriptable.NOT_FOUND) {
								return ((Boolean) d).booleanValue();
							}
						}

						if (x instanceof Wrapper && y instanceof Wrapper) {
							d = ((Wrapper) x).unwrap();
							unwrappedY = ((Wrapper) y).unwrap();
							return d == unwrappedY || isPrimitive(d)
									&& isPrimitive(unwrappedY)
									&& eq(d, unwrappedY);
						} else {
							return false;
						}
					}
				} else if (y instanceof Boolean) {
					if (x instanceof ScriptableObject) {
						d = ((ScriptableObject) x).equivalentValues(y);
						if (d != Scriptable.NOT_FOUND) {
							return ((Boolean) d).booleanValue();
						}
					}

					double d1 = ((Boolean) y).booleanValue() ? 1.0D : 0.0D;
					return eqNumber(d1, x);
				} else {
					return y instanceof Number
							? eqNumber(((Number) y).doubleValue(), x)
							: (y instanceof String
									? eqString((String) y, x)
									: false);
				}
			}
		} else if (y != null && y != Undefined.instance) {
			if (y instanceof ScriptableObject) {
				d = ((ScriptableObject) y).equivalentValues(x);
				if (d != Scriptable.NOT_FOUND) {
					return ((Boolean) d).booleanValue();
				}
			}

			return false;
		} else {
			return true;
		}
	}

	private static boolean isPrimitive(Object obj) {
		return obj instanceof Number || obj instanceof String
				|| obj instanceof Boolean;
	}

	static boolean eqNumber(double x, Object y) {
		while (true) {
			if (y != null && y != Undefined.instance) {
				if (y instanceof Number) {
					return x == ((Number) y).doubleValue();
				}

				if (y instanceof String) {
					return x == toNumber(y);
				}

				if (y instanceof Boolean) {
					return x == (((Boolean) y).booleanValue() ? 1.0D : 0.0D);
				}

				if (y instanceof Scriptable) {
					if (y instanceof ScriptableObject) {
						Number xval = wrapNumber(x);
						Object test = ((ScriptableObject) y)
								.equivalentValues(xval);
						if (test != Scriptable.NOT_FOUND) {
							return ((Boolean) test).booleanValue();
						}
					}

					y = toPrimitive(y);
					continue;
				}

				warnAboutNonJSObject(y);
				return false;
			}

			return false;
		}
	}

	private static boolean eqString(String x, Object y) {
		while (true) {
			if (y != null && y != Undefined.instance) {
				if (y instanceof String) {
					return x.equals(y);
				}

				if (y instanceof Number) {
					return toNumber(x) == ((Number) y).doubleValue();
				}

				if (y instanceof Boolean) {
					return toNumber(x) == (((Boolean) y).booleanValue()
							? 1.0D
							: 0.0D);
				}

				if (y instanceof Scriptable) {
					if (y instanceof ScriptableObject) {
						Object test = ((ScriptableObject) y)
								.equivalentValues(x);
						if (test != Scriptable.NOT_FOUND) {
							return ((Boolean) test).booleanValue();
						}
					}

					y = toPrimitive(y);
					continue;
				}

				warnAboutNonJSObject(y);
				return false;
			}

			return false;
		}
	}

	public static boolean shallowEq(Object x, Object y) {
		if (x == y) {
			if (!(x instanceof Number)) {
				return true;
			} else {
				double d = ((Number) x).doubleValue();
				return d == d;
			}
		} else if (x != null && x != Undefined.instance) {
			if (x instanceof Number) {
				if (y instanceof Number) {
					return ((Number) x).doubleValue() == ((Number) y)
							.doubleValue();
				}
			} else if (x instanceof String) {
				if (y instanceof String) {
					return x.equals(y);
				}
			} else if (x instanceof Boolean) {
				if (y instanceof Boolean) {
					return x.equals(y);
				}
			} else {
				if (!(x instanceof Scriptable)) {
					warnAboutNonJSObject(x);
					return x == y;
				}

				if (x instanceof Wrapper && y instanceof Wrapper) {
					return ((Wrapper) x).unwrap() == ((Wrapper) y).unwrap();
				}
			}

			return false;
		} else {
			return false;
		}
	}

	public static boolean instanceOf(Object a, Object b, Context cx) {
		if (!(b instanceof Scriptable)) {
			throw typeError0("msg.instanceof.not.object");
		} else {
			return !(a instanceof Scriptable) ? false : ((Scriptable) b)
					.hasInstance((Scriptable) a);
		}
	}

	public static boolean jsDelegatesTo(Scriptable lhs, Scriptable rhs) {
		for (Scriptable proto = lhs.getPrototype(); proto != null; proto = proto
				.getPrototype()) {
			if (proto.equals(rhs)) {
				return true;
			}
		}

		return false;
	}

	public static boolean in(Object a, Object b, Context cx) {
		if (!(b instanceof Scriptable)) {
			throw typeError0("msg.instanceof.not.object");
		} else {
			return hasObjectElem((Scriptable) b, a, cx);
		}
	}

	public static boolean cmp_LT(Object val1, Object val2) {
		double d1;
		double d2;
		if (val1 instanceof Number && val2 instanceof Number) {
			d1 = ((Number) val1).doubleValue();
			d2 = ((Number) val2).doubleValue();
		} else {
			if (val1 instanceof Scriptable) {
				val1 = ((Scriptable) val1).getDefaultValue(NumberClass);
			}

			if (val2 instanceof Scriptable) {
				val2 = ((Scriptable) val2).getDefaultValue(NumberClass);
			}

			if (val1 instanceof String && val2 instanceof String) {
				return ((String) val1).compareTo((String) val2) < 0;
			}

			d1 = toNumber(val1);
			d2 = toNumber(val2);
		}

		return d1 < d2;
	}

	public static boolean cmp_LE(Object val1, Object val2) {
		double d1;
		double d2;
		if (val1 instanceof Number && val2 instanceof Number) {
			d1 = ((Number) val1).doubleValue();
			d2 = ((Number) val2).doubleValue();
		} else {
			if (val1 instanceof Scriptable) {
				val1 = ((Scriptable) val1).getDefaultValue(NumberClass);
			}

			if (val2 instanceof Scriptable) {
				val2 = ((Scriptable) val2).getDefaultValue(NumberClass);
			}

			if (val1 instanceof String && val2 instanceof String) {
				return ((String) val1).compareTo((String) val2) <= 0;
			}

			d1 = toNumber(val1);
			d2 = toNumber(val2);
		}

		return d1 <= d2;
	}

	public static ScriptableObject getGlobal(Context cx) {
		String GLOBAL_CLASS = "org.mozilla.javascript.tools.shell.Global";
		Class globalClass = Kit
				.classOrNull("org.mozilla.javascript.tools.shell.Global");
		if (globalClass != null) {
			try {
				Class[] e = new Class[]{ContextClass};
				Constructor globalClassCtor = globalClass.getConstructor(e);
				Object[] arg = new Object[]{cx};
				return (ScriptableObject) globalClassCtor.newInstance(arg);
			} catch (Exception arg5) {
				;
			}
		}

		return new ImporterTopLevel(cx);
	}

	public static boolean hasTopCall(Context cx) {
		return cx.topCallScope != null;
	}

	public static Scriptable getTopCallScope(Context cx) {
		Scriptable scope = cx.topCallScope;
		if (scope == null) {
			throw new IllegalStateException();
		} else {
			return scope;
		}
	}

	public static Object doTopCall(Callable callable, Context cx,
			Scriptable scope, Scriptable thisObj, Object[] args) {
		if (scope == null) {
			throw new IllegalArgumentException();
		} else if (cx.topCallScope != null) {
			throw new IllegalStateException();
		} else {
			cx.topCallScope = ScriptableObject.getTopLevelScope(scope);
			cx.useDynamicScope = cx.hasFeature(7);
			ContextFactory f = cx.getFactory();

			Object result;
			try {
				result = f.doTopCall(callable, cx, scope, thisObj, args);
			} finally {
				cx.topCallScope = null;
				cx.cachedXMLLib = null;
				if (cx.currentActivationCall != null) {
					throw new IllegalStateException();
				}

			}

			return result;
		}
	}

	static Scriptable checkDynamicScope(Scriptable possibleDynamicScope,
			Scriptable staticTopScope) {
		if (possibleDynamicScope == staticTopScope) {
			return possibleDynamicScope;
		} else {
			Scriptable proto = possibleDynamicScope;

			do {
				proto = proto.getPrototype();
				if (proto == staticTopScope) {
					return possibleDynamicScope;
				}
			} while (proto != null);

			return staticTopScope;
		}
	}

	public static void addInstructionCount(Context cx, int instructionsToAdd) {
		cx.instructionCount += instructionsToAdd;
		if (cx.instructionCount > cx.instructionThreshold) {
			cx.observeInstructionCount(cx.instructionCount);
			cx.instructionCount = 0;
		}

	}

	public static void initScript(NativeFunction funObj, Scriptable thisObj,
			Context cx, Scriptable scope, boolean evalScript) {
		if (cx.topCallScope == null) {
			throw new IllegalStateException();
		} else {
			int varCount = funObj.getParamAndVarCount();
			if (varCount != 0) {
				Scriptable varScope;
				for (varScope = scope; varScope instanceof NativeWith; varScope = varScope
						.getParentScope()) {
					;
				}

				int i = varCount;

				while (i-- != 0) {
					String name = funObj.getParamOrVarName(i);
					boolean isConst = funObj.getParamOrVarConst(i);
					if (!ScriptableObject.hasProperty(scope, name)) {
						if (!evalScript) {
							if (isConst) {
								ScriptableObject.defineConstProperty(varScope,
										name);
							} else {
								ScriptableObject.defineProperty(varScope, name,
										Undefined.instance, 4);
							}
						} else {
							varScope.put(name, varScope, Undefined.instance);
						}
					} else {
						ScriptableObject.redefineProperty(scope, name, isConst);
					}
				}
			}

		}
	}

	public static Scriptable createFunctionActivation(NativeFunction funObj,
			Scriptable scope, Object[] args) {
		return new NativeCall(funObj, scope, args);
	}

	public static void enterActivationFunction(Context cx, Scriptable scope) {
		if (cx.topCallScope == null) {
			throw new IllegalStateException();
		} else {
			NativeCall call = (NativeCall) scope;
			call.parentActivationCall = cx.currentActivationCall;
			cx.currentActivationCall = call;
		}
	}

	public static void exitActivationFunction(Context cx) {
		NativeCall call = cx.currentActivationCall;
		cx.currentActivationCall = call.parentActivationCall;
		call.parentActivationCall = null;
	}

	static NativeCall findFunctionActivation(Context cx, Function f) {
		for (NativeCall call = cx.currentActivationCall; call != null; call = call.parentActivationCall) {
			if (call.function == f) {
				return call;
			}
		}

		return null;
	}

	public static Scriptable newCatchScope(Throwable t,
			Scriptable lastCatchScope, String exceptionName, Context cx,
			Scriptable scope) {
		Object obj;
		boolean cacheObj;
		NativeObject catchScopeObject;
		if (t instanceof JavaScriptException) {
			cacheObj = false;
			obj = ((JavaScriptException) t).getValue();
		} else {
			cacheObj = true;
			if (lastCatchScope != null) {
				catchScopeObject = (NativeObject) lastCatchScope;
				obj = catchScopeObject.getAssociatedValue(t);
				if (obj == null) {
					Kit.codeBug();
				}
			} else {
				Throwable javaException = null;
				String errorName;
				String errorMsg;
				Object catchScopeObject1;
				if (t instanceof EcmaError) {
					EcmaError sourceUri = (EcmaError) t;
					catchScopeObject1 = sourceUri;
					errorName = sourceUri.getName();
					errorMsg = sourceUri.getErrorMessage();
				} else if (t instanceof WrappedException) {
					WrappedException sourceUri1 = (WrappedException) t;
					catchScopeObject1 = sourceUri1;
					javaException = sourceUri1.getWrappedException();
					errorName = "JavaException";
					errorMsg = javaException.getClass().getName() + ": "
							+ javaException.getMessage();
				} else if (t instanceof EvaluatorException) {
					EvaluatorException sourceUri2 = (EvaluatorException) t;
					catchScopeObject1 = sourceUri2;
					errorName = "InternalError";
					errorMsg = sourceUri2.getMessage();
				} else {
					if (!cx.hasFeature(13)) {
						throw Kit.codeBug();
					}

					catchScopeObject1 = new WrappedException(t);
					errorName = "JavaException";
					errorMsg = t.toString();
				}

				String sourceUri3 = ((RhinoException) catchScopeObject1)
						.sourceName();
				if (sourceUri3 == null) {
					sourceUri3 = "";
				}

				int line = ((RhinoException) catchScopeObject1).lineNumber();
				Object[] args;
				if (line > 0) {
					args = new Object[]{errorMsg, sourceUri3, new Integer(line)};
				} else {
					args = new Object[]{errorMsg, sourceUri3};
				}

				Scriptable errorObject = cx.newObject(scope, errorName, args);
				ScriptableObject.putProperty(errorObject, "name", errorName);
				Object wrap;
				if (javaException != null && isVisible(cx, javaException)) {
					wrap = cx.getWrapFactory().wrap(cx, scope, javaException,
							(Class) null);
					ScriptableObject.defineProperty(errorObject,
							"javaException", wrap, 5);
				}

				if (isVisible(cx, catchScopeObject1)) {
					wrap = cx.getWrapFactory().wrap(cx, scope,
							catchScopeObject1, (Class) null);
					ScriptableObject.defineProperty(errorObject,
							"rhinoException", wrap, 5);
				}

				obj = errorObject;
			}
		}

		catchScopeObject = new NativeObject();
		catchScopeObject.defineProperty(exceptionName, obj, 4);
		if (isVisible(cx, t)) {
			catchScopeObject.defineProperty("__exception__",
					Context.javaToJS(t, scope), 6);
		}

		if (cacheObj) {
			catchScopeObject.associateValue(t, obj);
		}

		return catchScopeObject;
	}

	private static boolean isVisible(Context cx, Object obj) {
		ClassShutter shutter = cx.getClassShutter();
		return shutter == null
				|| shutter.visibleToScripts(obj.getClass().getName());
	}

	public static Scriptable enterWith(Object obj, Context cx, Scriptable scope) {
		Scriptable sobj = toObjectOrNull(cx, obj);
		if (sobj == null) {
			throw typeError1("msg.undef.with", toString(obj));
		} else if (sobj instanceof XMLObject) {
			XMLObject xmlObject = (XMLObject) sobj;
			return xmlObject.enterWith(scope);
		} else {
			return new NativeWith(scope, sobj);
		}
	}

	public static Scriptable leaveWith(Scriptable scope) {
		NativeWith nw = (NativeWith) scope;
		return nw.getParentScope();
	}

	public static Scriptable enterDotQuery(Object value, Scriptable scope) {
		if (!(value instanceof XMLObject)) {
			throw notXmlError(value);
		} else {
			XMLObject object = (XMLObject) value;
			return object.enterDotQuery(scope);
		}
	}

	public static Object updateDotQuery(boolean value, Scriptable scope) {
		NativeWith nw = (NativeWith) scope;
		return nw.updateDotQuery(value);
	}

	public static Scriptable leaveDotQuery(Scriptable scope) {
		NativeWith nw = (NativeWith) scope;
		return nw.getParentScope();
	}

	public static void setFunctionProtoAndParent(BaseFunction fn,
			Scriptable scope) {
		fn.setParentScope(scope);
		fn.setPrototype(ScriptableObject.getFunctionPrototype(scope));
	}

	public static void setObjectProtoAndParent(ScriptableObject object,
			Scriptable scope) {
		scope = ScriptableObject.getTopLevelScope(scope);
		object.setParentScope(scope);
		Scriptable proto = ScriptableObject.getClassPrototype(scope,
				object.getClassName());
		object.setPrototype(proto);
	}

	public static void initFunction(Context cx, Scriptable scope,
			NativeFunction function, int type, boolean fromEvalCode) {
		String name;
		if (type == 1) {
			name = function.getFunctionName();
			if (name != null && name.length() != 0) {
				if (!fromEvalCode) {
					ScriptableObject.defineProperty(scope, name, function, 4);
				} else {
					scope.put(name, scope, function);
				}
			}
		} else {
			if (type != 3) {
				throw Kit.codeBug();
			}

			name = function.getFunctionName();
			if (name != null && name.length() != 0) {
				while (scope instanceof NativeWith) {
					scope = scope.getParentScope();
				}

				scope.put(name, scope, function);
			}
		}

	}

	public static Scriptable newArrayLiteral(Object[] objects,
			int[] skipIndices, Context cx, Scriptable scope) {
		boolean SKIP_DENSITY = true;
		int count = objects.length;
		int skipCount = 0;
		if (skipIndices != null) {
			skipCount = skipIndices.length;
		}

		int length = count + skipCount;
		int skip;
		int i;
		int j;
		if (length > 1 && skipCount * 2 < length) {
			Object[] arg11;
			if (skipCount == 0) {
				arg11 = objects;
			} else {
				arg11 = new Object[length];
				skip = 0;
				i = 0;

				for (j = 0; i != length; ++i) {
					if (skip != skipCount && skipIndices[skip] == i) {
						arg11[i] = Scriptable.NOT_FOUND;
						++skip;
					} else {
						arg11[i] = objects[j];
						++j;
					}
				}
			}

			return cx.newObject(scope, "Array", arg11);
		} else {
			Scriptable arrayObj = cx.newObject(scope, "Array", emptyArgs);
			skip = 0;
			i = 0;

			for (j = 0; i != length; ++i) {
				if (skip != skipCount && skipIndices[skip] == i) {
					++skip;
				} else {
					ScriptableObject.putProperty(arrayObj, i, objects[j]);
					++j;
				}
			}

			return arrayObj;
		}
	}

	public static Scriptable newObjectLiteral(Object[] propertyIds,
			Object[] propertyValues, Context cx, Scriptable scope) {
		int[] getterSetters = new int[propertyIds.length];
		return newObjectLiteral(propertyIds, propertyValues, getterSetters, cx,
				scope);
	}

	public static Scriptable newObjectLiteral(Object[] propertyIds,
			Object[] propertyValues, int[] getterSetters, Context cx,
			Scriptable scope) {
		Scriptable object = cx.newObject(scope);
		int i = 0;

		for (int end = propertyIds.length; i != end; ++i) {
			Object id = propertyIds[i];
			int getterSetter = getterSetters[i];
			Object value = propertyValues[i];
			if (id instanceof String) {
				if (getterSetter == 0) {
					ScriptableObject.putProperty(object, (String) id, value);
				} else {
					String definer;
					if (getterSetter < 0) {
						definer = "__defineGetter__";
					} else {
						definer = "__defineSetter__";
					}

					Callable index = getPropFunctionAndThis(object, definer, cx);
					lastStoredScriptable(cx);
					Object[] outArgs = new Object[]{id, value};
					index.call(cx, scope, object, outArgs);
				}
			} else {
				int arg13 = ((Integer) id).intValue();
				ScriptableObject.putProperty(object, arg13, value);
			}
		}

		return object;
	}

	public static boolean isArrayObject(Object obj) {
		return obj instanceof NativeArray || obj instanceof Arguments;
	}

	public static Object[] getArrayElements(Scriptable object) {
		Context cx = Context.getContext();
		long longLen = NativeArray.getLengthProperty(cx, object);
		if (longLen > 2147483647L) {
			throw new IllegalArgumentException();
		} else {
			int len = (int) longLen;
			if (len == 0) {
				return emptyArgs;
			} else {
				Object[] result = new Object[len];

				for (int i = 0; i < len; ++i) {
					Object elem = ScriptableObject.getProperty(object, i);
					result[i] = elem == Scriptable.NOT_FOUND
							? Undefined.instance
							: elem;
				}

				return result;
			}
		}
	}

	static void checkDeprecated(Context cx, String name) {
		int version = cx.getLanguageVersion();
		if (version >= 140 || version == 0) {
			String msg = getMessage1("msg.deprec.ctor", name);
			if (version != 0) {
				throw Context.reportRuntimeError(msg);
			}

			Context.reportWarning(msg);
		}

	}

	public static String getMessage0(String messageId) {
		return getMessage(messageId, (Object[]) null);
	}

	public static String getMessage1(String messageId, Object arg1) {
		Object[] arguments = new Object[]{arg1};
		return getMessage(messageId, arguments);
	}

	public static String getMessage2(String messageId, Object arg1, Object arg2) {
		Object[] arguments = new Object[]{arg1, arg2};
		return getMessage(messageId, arguments);
	}

	public static String getMessage3(String messageId, Object arg1,
			Object arg2, Object arg3) {
		Object[] arguments = new Object[]{arg1, arg2, arg3};
		return getMessage(messageId, arguments);
	}

	public static String getMessage4(String messageId, Object arg1,
			Object arg2, Object arg3, Object arg4) {
		Object[] arguments = new Object[]{arg1, arg2, arg3, arg4};
		return getMessage(messageId, arguments);
	}

	public static String getMessage(String messageId, Object[] arguments) {
		return messageProvider.getMessage(messageId, arguments);
	}

	public static EcmaError constructError(String error, String message) {
		int[] linep = new int[1];
		String filename = Context.getSourcePositionFromStack(linep);
		return constructError(error, message, filename, linep[0],
				(String) null, 0);
	}

	public static EcmaError constructError(String error, String message,
			int lineNumberDelta) {
		int[] linep = new int[1];
		String filename = Context.getSourcePositionFromStack(linep);
		if (linep[0] != 0) {
			linep[0] += lineNumberDelta;
		}

		return constructError(error, message, filename, linep[0],
				(String) null, 0);
	}

	public static EcmaError constructError(String error, String message,
			String sourceName, int lineNumber, String lineSource,
			int columnNumber) {
		return new EcmaError(error, message, sourceName, lineNumber,
				lineSource, columnNumber);
	}

	public static EcmaError typeError(String message) {
		return constructError("TypeError", message);
	}

	public static EcmaError typeError0(String messageId) {
		String msg = getMessage0(messageId);
		return typeError(msg);
	}

	public static EcmaError typeError1(String messageId, String arg1) {
		String msg = getMessage1(messageId, arg1);
		return typeError(msg);
	}

	public static EcmaError typeError2(String messageId, String arg1,
			String arg2) {
		String msg = getMessage2(messageId, arg1, arg2);
		return typeError(msg);
	}

	public static EcmaError typeError3(String messageId, String arg1,
			String arg2, String arg3) {
		String msg = getMessage3(messageId, arg1, arg2, arg3);
		return typeError(msg);
	}

	public static RuntimeException undefReadError(Object object, Object id) {
		String idStr = id == null ? "null" : id.toString();
		return typeError2("msg.undef.prop.read", toString(object), idStr);
	}

	public static RuntimeException undefCallError(Object object, Object id) {
		String idStr = id == null ? "null" : id.toString();
		return typeError2("msg.undef.method.call", toString(object), idStr);
	}

	public static RuntimeException undefWriteError(Object object, Object id,
			Object value) {
		String idStr = id == null ? "null" : id.toString();
		String valueStr = value instanceof Scriptable
				? value.toString()
				: toString(value);
		return typeError3("msg.undef.prop.write", toString(object), idStr,
				valueStr);
	}

	public static RuntimeException notFoundError(Scriptable object,
			String property) {
		String msg = getMessage1("msg.is.not.defined", property);
		throw constructError("ReferenceError", msg);
	}

	public static RuntimeException notFunctionError(Object value) {
		return notFunctionError(value, value);
	}

	public static RuntimeException notFunctionError(Object value,
			Object messageHelper) {
		String msg = messageHelper == null ? "null" : messageHelper.toString();
		return value == Scriptable.NOT_FOUND ? typeError1(
				"msg.function.not.found", msg) : typeError2(
				"msg.isnt.function", msg, typeof(value));
	}

	public static RuntimeException notFunctionError(Object obj, Object value,
			String propertyName) {
		String objString = toString(obj);
		return value == Scriptable.NOT_FOUND
				? typeError2("msg.function.not.found.in", propertyName,
						objString) : typeError3("msg.isnt.function.in",
						propertyName, objString, typeof(value));
	}

	private static RuntimeException notXmlError(Object value) {
		throw typeError1("msg.isnt.xml.object", toString(value));
	}

	private static void warnAboutNonJSObject(Object nonJSObject) {
		String message = "RHINO USAGE WARNING: Missed Context.javaToJS() conversion:\nRhino runtime detected object "
				+ nonJSObject
				+ " of class "
				+ nonJSObject.getClass().getName()
				+ " where it expected String, Number, Boolean or Scriptable instance. Please check your code for missing Context.javaToJS() call.";
		Context.reportWarning(message);
		System.err.println(message);
	}

	public static RegExpProxy getRegExpProxy(Context cx) {
		return cx.getRegExpProxy();
	}

	public static void setRegExpProxy(Context cx, RegExpProxy proxy) {
		if (proxy == null) {
			throw new IllegalArgumentException();
		} else {
			cx.regExpProxy = proxy;
		}
	}

	public static RegExpProxy checkRegExpProxy(Context cx) {
		RegExpProxy result = getRegExpProxy(cx);
		if (result == null) {
			throw Context.reportRuntimeError0("msg.no.regexp");
		} else {
			return result;
		}
	}

	private static XMLLib currentXMLLib(Context cx) {
		if (cx.topCallScope == null) {
			throw new IllegalStateException();
		} else {
			XMLLib xmlLib = cx.cachedXMLLib;
			if (xmlLib == null) {
				xmlLib = XMLLib.extractFromScope(cx.topCallScope);
				if (xmlLib == null) {
					throw new IllegalStateException();
				}

				cx.cachedXMLLib = xmlLib;
			}

			return xmlLib;
		}
	}

	public static String escapeAttributeValue(Object value, Context cx) {
		XMLLib xmlLib = currentXMLLib(cx);
		return xmlLib.escapeAttributeValue(value);
	}

	public static String escapeTextValue(Object value, Context cx) {
		XMLLib xmlLib = currentXMLLib(cx);
		return xmlLib.escapeTextValue(value);
	}

	public static Ref memberRef(Object obj, Object elem, Context cx,
			int memberTypeFlags) {
		if (!(obj instanceof XMLObject)) {
			throw notXmlError(obj);
		} else {
			XMLObject xmlObject = (XMLObject) obj;
			return xmlObject.memberRef(cx, elem, memberTypeFlags);
		}
	}

	public static Ref memberRef(Object obj, Object namespace, Object elem,
			Context cx, int memberTypeFlags) {
		if (!(obj instanceof XMLObject)) {
			throw notXmlError(obj);
		} else {
			XMLObject xmlObject = (XMLObject) obj;
			return xmlObject.memberRef(cx, namespace, elem, memberTypeFlags);
		}
	}

	public static Ref nameRef(Object name, Context cx, Scriptable scope,
			int memberTypeFlags) {
		XMLLib xmlLib = currentXMLLib(cx);
		return xmlLib.nameRef(cx, name, scope, memberTypeFlags);
	}

	public static Ref nameRef(Object namespace, Object name, Context cx,
			Scriptable scope, int memberTypeFlags) {
		XMLLib xmlLib = currentXMLLib(cx);
		return xmlLib.nameRef(cx, namespace, name, scope, memberTypeFlags);
	}

	private static void storeIndexResult(Context cx, int index) {
		cx.scratchIndex = index;
	}

	static int lastIndexResult(Context cx) {
		return cx.scratchIndex;
	}

	public static void storeUint32Result(Context cx, long value) {
		if (value >>> 32 != 0L) {
			throw new IllegalArgumentException();
		} else {
			cx.scratchUint32 = value;
		}
	}

	public static long lastUint32Result(Context cx) {
		long value = cx.scratchUint32;
		if (value >>> 32 != 0L) {
			throw new IllegalStateException();
		} else {
			return value;
		}
	}

	private static void storeScriptable(Context cx, Scriptable value) {
		if (cx.scratchScriptable != null) {
			throw new IllegalStateException();
		} else {
			cx.scratchScriptable = value;
		}
	}

	public static Scriptable lastStoredScriptable(Context cx) {
		Scriptable result = cx.scratchScriptable;
		cx.scratchScriptable = null;
		return result;
	}

	static String makeUrlForGeneratedScript(boolean isEval,
			String masterScriptUrl, int masterScriptLine) {
		return isEval
				? masterScriptUrl + '#' + masterScriptLine + "(eval)"
				: masterScriptUrl + '#' + masterScriptLine + "(Function)";
	}

	static boolean isGeneratedScript(String sourceUrl) {
		return sourceUrl.indexOf("(eval)") >= 0
				|| sourceUrl.indexOf("(Function)") >= 0;
	}

	private static RuntimeException errorWithClassName(String msg, Object val) {
		return Context.reportRuntimeError1(msg, val.getClass().getName());
	}

	static {
		NaNobj = new Double(NaN);
		messageProvider = new ScriptRuntime.DefaultMessageProvider();
		emptyArgs = new Object[0];
		emptyStrings = new String[0];
	}

	private static class DefaultMessageProvider
			implements
				ScriptRuntime.MessageProvider {
		private DefaultMessageProvider() {
		}

		public String getMessage(String messageId, Object[] arguments) {
			String defaultResource = "org.mozilla.javascript.resources.Messages";
			Context cx = Context.getCurrentContext();
			Locale locale = cx != null ? cx.getLocale() : Locale.getDefault();
			ResourceBundle rb = ResourceBundle.getBundle(
					"org.mozilla.javascript.resources.Messages", locale);

			String formatString;
			try {
				formatString = rb.getString(messageId);
			} catch (MissingResourceException arg8) {
				throw new RuntimeException(
						"no message resource found for message property "
								+ messageId);
			}

			MessageFormat formatter = new MessageFormat(formatString);
			return formatter.format(arguments);
		}
	}

	public interface MessageProvider {
		String getMessage(String arg0, Object[] arg1);
	}

	private static class IdEnumeration implements Serializable {
		private static final long serialVersionUID = 1L;
		Scriptable obj;
		Object[] ids;
		int index;
		ObjToIntMap used;
		Object currentId;
		int enumType;
		boolean enumNumbers;
		Scriptable iterator;

		private IdEnumeration() {
		}
	}

	static class NoSuchMethodShim implements Callable {
		String methodName;
		Callable noSuchMethodMethod;

		NoSuchMethodShim(Callable noSuchMethodMethod, String methodName) {
			this.noSuchMethodMethod = noSuchMethodMethod;
			this.methodName = methodName;
		}

		public Object call(Context cx, Scriptable scope, Scriptable thisObj,
				Object[] args) {
			Object[] nestedArgs = new Object[]{
					this.methodName,
					ScriptRuntime
							.newArrayLiteral(args, (int[]) null, cx, scope)};
			return this.noSuchMethodMethod.call(cx, scope, thisObj, nestedArgs);
		}
	}
}