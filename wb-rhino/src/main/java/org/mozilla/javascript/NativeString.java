/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import java.text.Collator;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.RegExpProxy;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

final class NativeString extends IdScriptableObject {
	static final long serialVersionUID = 920268368584188687L;
	private static final Object STRING_TAG = "String";
	private static final int Id_length = 1;
	private static final int MAX_INSTANCE_ID = 1;
	private static final int ConstructorId_fromCharCode = -1;
	private static final int Id_constructor = 1;
	private static final int Id_toString = 2;
	private static final int Id_toSource = 3;
	private static final int Id_valueOf = 4;
	private static final int Id_charAt = 5;
	private static final int Id_charCodeAt = 6;
	private static final int Id_indexOf = 7;
	private static final int Id_lastIndexOf = 8;
	private static final int Id_split = 9;
	private static final int Id_substring = 10;
	private static final int Id_toLowerCase = 11;
	private static final int Id_toUpperCase = 12;
	private static final int Id_substr = 13;
	private static final int Id_concat = 14;
	private static final int Id_slice = 15;
	private static final int Id_bold = 16;
	private static final int Id_italics = 17;
	private static final int Id_fixed = 18;
	private static final int Id_strike = 19;
	private static final int Id_small = 20;
	private static final int Id_big = 21;
	private static final int Id_blink = 22;
	private static final int Id_sup = 23;
	private static final int Id_sub = 24;
	private static final int Id_fontsize = 25;
	private static final int Id_fontcolor = 26;
	private static final int Id_link = 27;
	private static final int Id_anchor = 28;
	private static final int Id_equals = 29;
	private static final int Id_equalsIgnoreCase = 30;
	private static final int Id_match = 31;
	private static final int Id_search = 32;
	private static final int Id_replace = 33;
	private static final int Id_localeCompare = 34;
	private static final int Id_toLocaleLowerCase = 35;
	private static final int Id_toLocaleUpperCase = 36;
	private static final int MAX_PROTOTYPE_ID = 36;
	private static final int ConstructorId_charAt = -5;
	private static final int ConstructorId_charCodeAt = -6;
	private static final int ConstructorId_indexOf = -7;
	private static final int ConstructorId_lastIndexOf = -8;
	private static final int ConstructorId_split = -9;
	private static final int ConstructorId_substring = -10;
	private static final int ConstructorId_toLowerCase = -11;
	private static final int ConstructorId_toUpperCase = -12;
	private static final int ConstructorId_substr = -13;
	private static final int ConstructorId_concat = -14;
	private static final int ConstructorId_slice = -15;
	private static final int ConstructorId_equalsIgnoreCase = -30;
	private static final int ConstructorId_match = -31;
	private static final int ConstructorId_search = -32;
	private static final int ConstructorId_replace = -33;
	private static final int ConstructorId_localeCompare = -34;
	private static final int ConstructorId_toLocaleLowerCase = -35;
	private String string;

	static void init(Scriptable scope, boolean sealed) {
		NativeString obj = new NativeString("");
		obj.exportAsJSClass(36, scope, sealed);
	}

	private NativeString(String s) {
		this.string = s;
	}

	public String getClassName() {
		return "String";
	}

	protected int getMaxInstanceId() {
		return 1;
	}

	protected int findInstanceIdInfo(String s) {
		return s.equals("length") ? instanceIdInfo(7, 1) : super
				.findInstanceIdInfo(s);
	}

	protected String getInstanceIdName(int id) {
		return id == 1 ? "length" : super.getInstanceIdName(id);
	}

	protected Object getInstanceIdValue(int id) {
		return id == 1 ? ScriptRuntime.wrapInt(this.string.length()) : super
				.getInstanceIdValue(id);
	}

	protected void fillConstructorProperties(IdFunctionObject ctor) {
		this.addIdFunctionProperty(ctor, STRING_TAG, -1, "fromCharCode", 1);
		this.addIdFunctionProperty(ctor, STRING_TAG, -5, "charAt", 2);
		this.addIdFunctionProperty(ctor, STRING_TAG, -6, "charCodeAt", 2);
		this.addIdFunctionProperty(ctor, STRING_TAG, -7, "indexOf", 2);
		this.addIdFunctionProperty(ctor, STRING_TAG, -8, "lastIndexOf", 2);
		this.addIdFunctionProperty(ctor, STRING_TAG, -9, "split", 3);
		this.addIdFunctionProperty(ctor, STRING_TAG, -10, "substring", 3);
		this.addIdFunctionProperty(ctor, STRING_TAG, -11, "toLowerCase", 1);
		this.addIdFunctionProperty(ctor, STRING_TAG, -12, "toUpperCase", 1);
		this.addIdFunctionProperty(ctor, STRING_TAG, -13, "substr", 3);
		this.addIdFunctionProperty(ctor, STRING_TAG, -14, "concat", 2);
		this.addIdFunctionProperty(ctor, STRING_TAG, -15, "slice", 3);
		this.addIdFunctionProperty(ctor, STRING_TAG, -30, "equalsIgnoreCase", 2);
		this.addIdFunctionProperty(ctor, STRING_TAG, -31, "match", 2);
		this.addIdFunctionProperty(ctor, STRING_TAG, -32, "search", 2);
		this.addIdFunctionProperty(ctor, STRING_TAG, -33, "replace", 2);
		this.addIdFunctionProperty(ctor, STRING_TAG, -34, "localeCompare", 2);
		this.addIdFunctionProperty(ctor, STRING_TAG, -35, "toLocaleLowerCase",
				1);
		this.addIdFunctionProperty(ctor, STRING_TAG, -1, "fromCharCode", 1);
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
			case 5 :
				arity = 1;
				s = "charAt";
				break;
			case 6 :
				arity = 1;
				s = "charCodeAt";
				break;
			case 7 :
				arity = 1;
				s = "indexOf";
				break;
			case 8 :
				arity = 1;
				s = "lastIndexOf";
				break;
			case 9 :
				arity = 2;
				s = "split";
				break;
			case 10 :
				arity = 2;
				s = "substring";
				break;
			case 11 :
				arity = 0;
				s = "toLowerCase";
				break;
			case 12 :
				arity = 0;
				s = "toUpperCase";
				break;
			case 13 :
				arity = 2;
				s = "substr";
				break;
			case 14 :
				arity = 1;
				s = "concat";
				break;
			case 15 :
				arity = 2;
				s = "slice";
				break;
			case 16 :
				arity = 0;
				s = "bold";
				break;
			case 17 :
				arity = 0;
				s = "italics";
				break;
			case 18 :
				arity = 0;
				s = "fixed";
				break;
			case 19 :
				arity = 0;
				s = "strike";
				break;
			case 20 :
				arity = 0;
				s = "small";
				break;
			case 21 :
				arity = 0;
				s = "big";
				break;
			case 22 :
				arity = 0;
				s = "blink";
				break;
			case 23 :
				arity = 0;
				s = "sup";
				break;
			case 24 :
				arity = 0;
				s = "sub";
				break;
			case 25 :
				arity = 0;
				s = "fontsize";
				break;
			case 26 :
				arity = 0;
				s = "fontcolor";
				break;
			case 27 :
				arity = 0;
				s = "link";
				break;
			case 28 :
				arity = 0;
				s = "anchor";
				break;
			case 29 :
				arity = 1;
				s = "equals";
				break;
			case 30 :
				arity = 1;
				s = "equalsIgnoreCase";
				break;
			case 31 :
				arity = 1;
				s = "match";
				break;
			case 32 :
				arity = 1;
				s = "search";
				break;
			case 33 :
				arity = 1;
				s = "replace";
				break;
			case 34 :
				arity = 1;
				s = "localeCompare";
				break;
			case 35 :
				arity = 0;
				s = "toLocaleLowerCase";
				break;
			case 36 :
				arity = 0;
				s = "toLocaleUpperCase";
				break;
			default :
				throw new IllegalArgumentException(String.valueOf(id));
		}

		this.initPrototypeMethod(STRING_TAG, id, s, arity);
	}

	public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
			Scriptable thisObj, Object[] args) {
		if (!f.hasTag(STRING_TAG)) {
			return super.execIdCall(f, cx, scope, thisObj, args);
		} else {
			int id = f.methodId();

			while (true) {
				String arg12;
				switch (id) {
					case -35 :
					case -34 :
					case -33 :
					case -32 :
					case -31 :
					case -30 :
					case -15 :
					case -14 :
					case -13 :
					case -12 :
					case -11 :
					case -10 :
					case -9 :
					case -8 :
					case -7 :
					case -6 :
					case -5 :
						thisObj = ScriptRuntime.toObject(scope,
								ScriptRuntime.toString(args[0]));
						Object[] arg16 = new Object[args.length - 1];

						for (int arg14 = 0; arg14 < arg16.length; ++arg14) {
							arg16[arg14] = args[arg14 + 1];
						}

						args = arg16;
						id = -id;
						break;
					case -29 :
					case -28 :
					case -27 :
					case -26 :
					case -25 :
					case -24 :
					case -23 :
					case -22 :
					case -21 :
					case -20 :
					case -19 :
					case -18 :
					case -17 :
					case -16 :
					case -4 :
					case -3 :
					case -2 :
					case 0 :
					default :
						throw new IllegalArgumentException(String.valueOf(id));
					case -1 :
						int arg15 = args.length;
						if (arg15 < 1) {
							return "";
						}

						StringBuffer arg13 = new StringBuffer(arg15);

						for (int i = 0; i != arg15; ++i) {
							arg13.append(ScriptRuntime.toUint16(args[i]));
						}

						return arg13.toString();
					case 1 :
						arg12 = args.length >= 1 ? ScriptRuntime
								.toString(args[0]) : "";
						if (thisObj == null) {
							return new NativeString(arg12);
						}

						return arg12;
					case 2 :
					case 4 :
						return realThis(thisObj, f).string;
					case 3 :
						arg12 = realThis(thisObj, f).string;
						return "(new String(\""
								+ ScriptRuntime.escapeString(arg12) + "\"))";
					case 5 :
					case 6 :
						arg12 = ScriptRuntime.toString(thisObj);
						double arg11 = ScriptRuntime.toInteger(args, 0);
						if (arg11 >= 0.0D && arg11 < (double) arg12.length()) {
							char c = arg12.charAt((int) arg11);
							if (id == 5) {
								return String.valueOf(c);
							}

							return ScriptRuntime.wrapInt(c);
						}

						if (id == 5) {
							return "";
						}

						return ScriptRuntime.NaNobj;
					case 7 :
						return ScriptRuntime.wrapInt(js_indexOf(
								ScriptRuntime.toString(thisObj), args));
					case 8 :
						return ScriptRuntime.wrapInt(js_lastIndexOf(
								ScriptRuntime.toString(thisObj), args));
					case 9 :
						return js_split(cx, scope,
								ScriptRuntime.toString(thisObj), args);
					case 10 :
						return js_substring(cx,
								ScriptRuntime.toString(thisObj), args);
					case 11 :
						return ScriptRuntime.toString(thisObj).toLowerCase();
					case 12 :
						return ScriptRuntime.toString(thisObj).toUpperCase();
					case 13 :
						return js_substr(ScriptRuntime.toString(thisObj), args);
					case 14 :
						return js_concat(ScriptRuntime.toString(thisObj), args);
					case 15 :
						return js_slice(ScriptRuntime.toString(thisObj), args);
					case 16 :
						return tagify(thisObj, "b", (String) null,
								(Object[]) null);
					case 17 :
						return tagify(thisObj, "i", (String) null,
								(Object[]) null);
					case 18 :
						return tagify(thisObj, "tt", (String) null,
								(Object[]) null);
					case 19 :
						return tagify(thisObj, "strike", (String) null,
								(Object[]) null);
					case 20 :
						return tagify(thisObj, "small", (String) null,
								(Object[]) null);
					case 21 :
						return tagify(thisObj, "big", (String) null,
								(Object[]) null);
					case 22 :
						return tagify(thisObj, "blink", (String) null,
								(Object[]) null);
					case 23 :
						return tagify(thisObj, "sup", (String) null,
								(Object[]) null);
					case 24 :
						return tagify(thisObj, "sub", (String) null,
								(Object[]) null);
					case 25 :
						return tagify(thisObj, "font", "size", args);
					case 26 :
						return tagify(thisObj, "font", "color", args);
					case 27 :
						return tagify(thisObj, "a", "href", args);
					case 28 :
						return tagify(thisObj, "a", "name", args);
					case 29 :
					case 30 :
						arg12 = ScriptRuntime.toString(thisObj);
						String s2 = ScriptRuntime.toString(args, 0);
						return ScriptRuntime.wrapBoolean(id == 29 ? arg12
								.equals(s2) : arg12.equalsIgnoreCase(s2));
					case 31 :
					case 32 :
					case 33 :
						byte arg10;
						if (id == 31) {
							arg10 = 1;
						} else if (id == 32) {
							arg10 = 3;
						} else {
							arg10 = 2;
						}

						return ScriptRuntime.checkRegExpProxy(cx).action(cx,
								scope, thisObj, args, arg10);
					case 34 :
						Collator collator = Collator
								.getInstance(cx.getLocale());
						collator.setStrength(3);
						collator.setDecomposition(1);
						return ScriptRuntime.wrapNumber((double) collator
								.compare(ScriptRuntime.toString(thisObj),
										ScriptRuntime.toString(args, 0)));
					case 35 :
						return ScriptRuntime.toString(thisObj).toLowerCase(
								cx.getLocale());
					case 36 :
						return ScriptRuntime.toString(thisObj).toUpperCase(
								cx.getLocale());
				}
			}
		}
	}

	private static NativeString realThis(Scriptable thisObj, IdFunctionObject f) {
		if (!(thisObj instanceof NativeString)) {
			throw incompatibleCallError(f);
		} else {
			return (NativeString) thisObj;
		}
	}

	private static String tagify(Object thisObj, String tag, String attribute,
			Object[] args) {
		String str = ScriptRuntime.toString(thisObj);
		StringBuffer result = new StringBuffer();
		result.append('<');
		result.append(tag);
		if (attribute != null) {
			result.append(' ');
			result.append(attribute);
			result.append("=\"");
			result.append(ScriptRuntime.toString(args, 0));
			result.append('\"');
		}

		result.append('>');
		result.append(str);
		result.append("</");
		result.append(tag);
		result.append('>');
		return result.toString();
	}

	public String toString() {
		return this.string;
	}

	public Object get(int index, Scriptable start) {
		return 0 <= index && index < this.string.length() ? this.string
				.substring(index, index + 1) : super.get(index, start);
	}

	public void put(int index, Scriptable start, Object value) {
		if (0 > index || index >= this.string.length()) {
			super.put(index, start, value);
		}
	}

	private static int js_indexOf(String target, Object[] args) {
		String search = ScriptRuntime.toString(args, 0);
		double begin = ScriptRuntime.toInteger(args, 1);
		if (begin > (double) target.length()) {
			return -1;
		} else {
			if (begin < 0.0D) {
				begin = 0.0D;
			}

			return target.indexOf(search, (int) begin);
		}
	}

	private static int js_lastIndexOf(String target, Object[] args) {
		String search = ScriptRuntime.toString(args, 0);
		double end = ScriptRuntime.toNumber(args, 1);
		if (end == end && end <= (double) target.length()) {
			if (end < 0.0D) {
				end = 0.0D;
			}
		} else {
			end = (double) target.length();
		}

		return target.lastIndexOf(search, (int) end);
	}

	private static int find_split(Context cx, Scriptable scope, String target,
			String separator, int version, RegExpProxy reProxy, Scriptable re,
			int[] ip, int[] matchlen, boolean[] matched, String[][] parensp) {
		int i = ip[0];
		int length = target.length();
		if (version == 120 && re == null && separator.length() == 1
				&& separator.charAt(0) == 32) {
			if (i == 0) {
				while (i < length && Character.isWhitespace(target.charAt(i))) {
					++i;
				}

				ip[0] = i;
			}

			if (i == length) {
				return -1;
			} else {
				while (i < length && !Character.isWhitespace(target.charAt(i))) {
					++i;
				}

				int j;
				for (j = i; j < length
						&& Character.isWhitespace(target.charAt(j)); ++j) {
					;
				}

				matchlen[0] = j - i;
				return i;
			}
		} else if (i > length) {
			return -1;
		} else if (re != null) {
			return reProxy.find_split(cx, scope, target, separator, re, ip,
					matchlen, matched, parensp);
		} else if (version != 0 && version < 130 && length == 0) {
			return -1;
		} else if (separator.length() == 0) {
			if (version == 120) {
				if (i == length) {
					matchlen[0] = 1;
					return i;
				} else {
					return i + 1;
				}
			} else {
				return i == length ? -1 : i + 1;
			}
		} else if (ip[0] >= length) {
			return length;
		} else {
			i = target.indexOf(separator, ip[0]);
			return i != -1 ? i : length;
		}
	}

	private static Object js_split(Context cx, Scriptable scope, String target,
			Object[] args) {
		Scriptable top = getTopLevelScope(scope);
		Scriptable result = ScriptRuntime.newObject(cx, top, "Array",
				(Object[]) null);
		if (args.length < 1) {
			result.put(0, result, target);
			return result;
		} else {
			boolean limited = args.length > 1 && args[1] != Undefined.instance;
			long limit = 0L;
			if (limited) {
				limit = ScriptRuntime.toUint32(args[1]);
				if (limit > (long) target.length()) {
					limit = (long) (1 + target.length());
				}
			}

			String separator = null;
			int[] matchlen = new int[1];
			Scriptable re = null;
			RegExpProxy reProxy = null;
			if (args[0] instanceof Scriptable) {
				reProxy = ScriptRuntime.getRegExpProxy(cx);
				if (reProxy != null) {
					Scriptable ip = (Scriptable) args[0];
					if (reProxy.isRegExp(ip)) {
						re = ip;
					}
				}
			}

			if (re == null) {
				separator = ScriptRuntime.toString(args[0]);
				matchlen[0] = separator.length();
			}

			int[] arg21 = new int[]{0};
			int len = 0;
			boolean[] matched = new boolean[]{false};
			String[][] parens = new String[][]{null};
			int version = cx.getLanguageVersion();

			int match;
			while ((match = find_split(cx, scope, target, separator, version,
					reProxy, re, arg21, matchlen, matched, parens)) >= 0
					&& (!limited || (long) len < limit)
					&& match <= target.length()) {
				String substr;
				if (target.length() == 0) {
					substr = target;
				} else {
					substr = target.substring(arg21[0], match);
				}

				result.put(len, result, substr);
				++len;
				if (re != null && matched[0]) {
					int size = parens[0].length;

					for (int num = 0; num < size
							&& (!limited || (long) len < limit); ++num) {
						result.put(len, result, parens[0][num]);
						++len;
					}

					matched[0] = false;
				}

				arg21[0] = match + matchlen[0];
				if (version < 130 && version != 0 && !limited
						&& arg21[0] == target.length()) {
					break;
				}
			}

			return result;
		}
	}

	private static String js_substring(Context cx, String target, Object[] args) {
		int length = target.length();
		double start = ScriptRuntime.toInteger(args, 0);
		if (start < 0.0D) {
			start = 0.0D;
		} else if (start > (double) length) {
			start = (double) length;
		}

		double end;
		if (args.length > 1 && args[1] != Undefined.instance) {
			end = ScriptRuntime.toInteger(args[1]);
			if (end < 0.0D) {
				end = 0.0D;
			} else if (end > (double) length) {
				end = (double) length;
			}

			if (end < start) {
				if (cx.getLanguageVersion() != 120) {
					double temp = start;
					start = end;
					end = temp;
				} else {
					end = start;
				}
			}
		} else {
			end = (double) length;
		}

		return target.substring((int) start, (int) end);
	}

	int getLength() {
		return this.string.length();
	}

	private static String js_substr(String target, Object[] args) {
		if (args.length < 1) {
			return target;
		} else {
			double begin = ScriptRuntime.toInteger(args[0]);
			int length = target.length();
			if (begin < 0.0D) {
				begin += (double) length;
				if (begin < 0.0D) {
					begin = 0.0D;
				}
			} else if (begin > (double) length) {
				begin = (double) length;
			}

			double end;
			if (args.length == 1) {
				end = (double) length;
			} else {
				end = ScriptRuntime.toInteger(args[1]);
				if (end < 0.0D) {
					end = 0.0D;
				}

				end += begin;
				if (end > (double) length) {
					end = (double) length;
				}
			}

			return target.substring((int) begin, (int) end);
		}
	}

	private static String js_concat(String target, Object[] args) {
		int N = args.length;
		if (N == 0) {
			return target;
		} else if (N == 1) {
			String arg6 = ScriptRuntime.toString(args[0]);
			return target.concat(arg6);
		} else {
			int size = target.length();
			String[] argsAsStrings = new String[N];

			for (int result = 0; result != N; ++result) {
				String i = ScriptRuntime.toString(args[result]);
				argsAsStrings[result] = i;
				size += i.length();
			}

			StringBuffer arg7 = new StringBuffer(size);
			arg7.append(target);

			for (int arg8 = 0; arg8 != N; ++arg8) {
				arg7.append(argsAsStrings[arg8]);
			}

			return arg7.toString();
		}
	}

	private static String js_slice(String target, Object[] args) {
		if (args.length != 0) {
			double begin = ScriptRuntime.toInteger(args[0]);
			int length = target.length();
			if (begin < 0.0D) {
				begin += (double) length;
				if (begin < 0.0D) {
					begin = 0.0D;
				}
			} else if (begin > (double) length) {
				begin = (double) length;
			}

			double end;
			if (args.length == 1) {
				end = (double) length;
			} else {
				end = ScriptRuntime.toInteger(args[1]);
				if (end < 0.0D) {
					end += (double) length;
					if (end < 0.0D) {
						end = 0.0D;
					}
				} else if (end > (double) length) {
					end = (double) length;
				}

				if (end < begin) {
					end = begin;
				}
			}

			return target.substring((int) begin, (int) end);
		} else {
			return target;
		}
	}

	protected int findPrototypeId(String s) {
		byte id;
		String X;
		id = 0;
		X = null;
		char c;
		label106 : switch (s.length()) {
			case 3 :
				c = s.charAt(2);
				if (c == 98) {
					if (s.charAt(0) == 115 && s.charAt(1) == 117) {
						id = 24;
						return id;
					}
				} else if (c == 103) {
					if (s.charAt(0) == 98 && s.charAt(1) == 105) {
						id = 21;
						return id;
					}
				} else if (c == 112 && s.charAt(0) == 115 && s.charAt(1) == 117) {
					id = 23;
					return id;
				}
				break;
			case 4 :
				c = s.charAt(0);
				if (c == 98) {
					X = "bold";
					id = 16;
				} else if (c == 108) {
					X = "link";
					id = 27;
				}
				break;
			case 5 :
				switch (s.charAt(4)) {
					case 'd' :
						X = "fixed";
						id = 18;
						break label106;
					case 'e' :
						X = "slice";
						id = 15;
					case 'f' :
					case 'g' :
					case 'i' :
					case 'j' :
					case 'm' :
					case 'n' :
					case 'o' :
					case 'p' :
					case 'q' :
					case 'r' :
					case 's' :
					default :
						break label106;
					case 'h' :
						X = "match";
						id = 31;
						break label106;
					case 'k' :
						X = "blink";
						id = 22;
						break label106;
					case 'l' :
						X = "small";
						id = 20;
						break label106;
					case 't' :
						X = "split";
						id = 9;
						break label106;
				}
			case 6 :
				switch (s.charAt(1)) {
					case 'e' :
						X = "search";
						id = 32;
					case 'f' :
					case 'g' :
					case 'i' :
					case 'j' :
					case 'k' :
					case 'l' :
					case 'm' :
					case 'p' :
					case 'r' :
					case 's' :
					default :
						break label106;
					case 'h' :
						X = "charAt";
						id = 5;
						break label106;
					case 'n' :
						X = "anchor";
						id = 28;
						break label106;
					case 'o' :
						X = "concat";
						id = 14;
						break label106;
					case 'q' :
						X = "equals";
						id = 29;
						break label106;
					case 't' :
						X = "strike";
						id = 19;
						break label106;
					case 'u' :
						X = "substr";
						id = 13;
						break label106;
				}
			case 7 :
				switch (s.charAt(1)) {
					case 'a' :
						X = "valueOf";
						id = 4;
						break label106;
					case 'e' :
						X = "replace";
						id = 33;
						break label106;
					case 'n' :
						X = "indexOf";
						id = 7;
						break label106;
					case 't' :
						X = "italics";
						id = 17;
					default :
						break label106;
				}
			case 8 :
				c = s.charAt(4);
				if (c == 114) {
					X = "toString";
					id = 2;
				} else if (c == 115) {
					X = "fontsize";
					id = 25;
				} else if (c == 117) {
					X = "toSource";
					id = 3;
				}
				break;
			case 9 :
				c = s.charAt(0);
				if (c == 102) {
					X = "fontcolor";
					id = 26;
				} else if (c == 115) {
					X = "substring";
					id = 10;
				}
				break;
			case 10 :
				X = "charCodeAt";
				id = 6;
				break;
			case 11 :
				switch (s.charAt(2)) {
					case 'L' :
						X = "toLowerCase";
						id = 11;
						break;
					case 'U' :
						X = "toUpperCase";
						id = 12;
						break;
					case 'n' :
						X = "constructor";
						id = 1;
						break;
					case 's' :
						X = "lastIndexOf";
						id = 8;
				}
			case 12 :
			case 14 :
			case 15 :
			default :
				break;
			case 13 :
				X = "localeCompare";
				id = 34;
				break;
			case 16 :
				X = "equalsIgnoreCase";
				id = 30;
				break;
			case 17 :
				c = s.charAt(8);
				if (c == 76) {
					X = "toLocaleLowerCase";
					id = 35;
				} else if (c == 85) {
					X = "toLocaleUpperCase";
					id = 36;
				}
		}

		if (X != null && X != s && !X.equals(s)) {
			id = 0;
		}

		return id;
	}
}