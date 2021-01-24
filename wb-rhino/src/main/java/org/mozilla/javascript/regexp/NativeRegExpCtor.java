/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.regexp;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.regexp.NativeRegExp;
import org.mozilla.javascript.regexp.RegExpImpl;

class NativeRegExpCtor extends BaseFunction {
	static final long serialVersionUID = -5733330028285400526L;
	private static final int Id_multiline = 1;
	private static final int Id_STAR = 2;
	private static final int Id_input = 3;
	private static final int Id_UNDERSCORE = 4;
	private static final int Id_lastMatch = 5;
	private static final int Id_AMPERSAND = 6;
	private static final int Id_lastParen = 7;
	private static final int Id_PLUS = 8;
	private static final int Id_leftContext = 9;
	private static final int Id_BACK_QUOTE = 10;
	private static final int Id_rightContext = 11;
	private static final int Id_QUOTE = 12;
	private static final int DOLLAR_ID_BASE = 12;
	private static final int Id_DOLLAR_1 = 13;
	private static final int Id_DOLLAR_2 = 14;
	private static final int Id_DOLLAR_3 = 15;
	private static final int Id_DOLLAR_4 = 16;
	private static final int Id_DOLLAR_5 = 17;
	private static final int Id_DOLLAR_6 = 18;
	private static final int Id_DOLLAR_7 = 19;
	private static final int Id_DOLLAR_8 = 20;
	private static final int Id_DOLLAR_9 = 21;
	private static final int MAX_INSTANCE_ID = 21;

	public String getFunctionName() {
		return "RegExp";
	}

	public Object call(Context cx, Scriptable scope, Scriptable thisObj,
			Object[] args) {
		return args.length <= 0 || !(args[0] instanceof NativeRegExp)
				|| args.length != 1 && args[1] != Undefined.instance ? this
				.construct(cx, scope, args) : args[0];
	}

	public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
		NativeRegExp re = new NativeRegExp();
		re.compile(cx, scope, args);
		ScriptRuntime.setObjectProtoAndParent(re, scope);
		return re;
	}

	private static RegExpImpl getImpl() {
		Context cx = Context.getCurrentContext();
		return (RegExpImpl) ScriptRuntime.getRegExpProxy(cx);
	}

	protected int getMaxInstanceId() {
		return super.getMaxInstanceId() + 21;
	}

	protected int findInstanceIdInfo(String s) {
		byte id;
		label88 : {
			id = 0;
			String attr = null;
			switch (s.length()) {
				case 2 :
					switch (s.charAt(1)) {
						case '&' :
							if (s.charAt(0) == 36) {
								id = 6;
								break label88;
							}
							break;
						case '\'' :
							if (s.charAt(0) == 36) {
								id = 12;
								break label88;
							}
						case '(' :
						case ')' :
						case ',' :
						case '-' :
						case '.' :
						case '/' :
						case '0' :
						case ':' :
						case ';' :
						case '<' :
						case '=' :
						case '>' :
						case '?' :
						case '@' :
						case 'A' :
						case 'B' :
						case 'C' :
						case 'D' :
						case 'E' :
						case 'F' :
						case 'G' :
						case 'H' :
						case 'I' :
						case 'J' :
						case 'K' :
						case 'L' :
						case 'M' :
						case 'N' :
						case 'O' :
						case 'P' :
						case 'Q' :
						case 'R' :
						case 'S' :
						case 'T' :
						case 'U' :
						case 'V' :
						case 'W' :
						case 'X' :
						case 'Y' :
						case 'Z' :
						case '[' :
						case '\\' :
						case ']' :
						case '^' :
						default :
							break;
						case '*' :
							if (s.charAt(0) == 36) {
								id = 2;
								break label88;
							}
							break;
						case '+' :
							if (s.charAt(0) == 36) {
								id = 8;
								break label88;
							}
							break;
						case '1' :
							if (s.charAt(0) == 36) {
								id = 13;
								break label88;
							}
							break;
						case '2' :
							if (s.charAt(0) == 36) {
								id = 14;
								break label88;
							}
							break;
						case '3' :
							if (s.charAt(0) == 36) {
								id = 15;
								break label88;
							}
							break;
						case '4' :
							if (s.charAt(0) == 36) {
								id = 16;
								break label88;
							}
							break;
						case '5' :
							if (s.charAt(0) == 36) {
								id = 17;
								break label88;
							}
							break;
						case '6' :
							if (s.charAt(0) == 36) {
								id = 18;
								break label88;
							}
							break;
						case '7' :
							if (s.charAt(0) == 36) {
								id = 19;
								break label88;
							}
							break;
						case '8' :
							if (s.charAt(0) == 36) {
								id = 20;
								break label88;
							}
							break;
						case '9' :
							if (s.charAt(0) == 36) {
								id = 21;
								break label88;
							}
							break;
						case '_' :
							if (s.charAt(0) == 36) {
								id = 4;
								break label88;
							}
							break;
						case '`' :
							if (s.charAt(0) == 36) {
								id = 10;
								break label88;
							}
					}
				case 3 :
				case 4 :
				case 6 :
				case 7 :
				case 8 :
				case 10 :
				default :
					break;
				case 5 :
					attr = "input";
					id = 3;
					break;
				case 9 :
					char c = s.charAt(4);
					if (c == 77) {
						attr = "lastMatch";
						id = 5;
					} else if (c == 80) {
						attr = "lastParen";
						id = 7;
					} else if (c == 105) {
						attr = "multiline";
						id = 1;
					}
					break;
				case 11 :
					attr = "leftContext";
					id = 9;
					break;
				case 12 :
					attr = "rightContext";
					id = 11;
			}

			if (attr != null && attr != s && !attr.equals(s)) {
				id = 0;
			}
		}

		if (id == 0) {
			return super.findInstanceIdInfo(s);
		} else {
			byte attr1;
			switch (id) {
				case 1 :
				case 2 :
				case 3 :
				case 4 :
					attr1 = 4;
					break;
				default :
					attr1 = 5;
			}

			return instanceIdInfo(attr1, super.getMaxInstanceId() + id);
		}
	}

	protected String getInstanceIdName(int id) {
		int shifted = id - super.getMaxInstanceId();
		if (1 <= shifted && shifted <= 21) {
			switch (shifted) {
				case 1 :
					return "multiline";
				case 2 :
					return "$*";
				case 3 :
					return "input";
				case 4 :
					return "$_";
				case 5 :
					return "lastMatch";
				case 6 :
					return "$&";
				case 7 :
					return "lastParen";
				case 8 :
					return "$+";
				case 9 :
					return "leftContext";
				case 10 :
					return "$`";
				case 11 :
					return "rightContext";
				case 12 :
					return "$\'";
				default :
					int substring_number = shifted - 12 - 1;
					char[] buf = new char[]{'$', (char) (49 + substring_number)};
					return new String(buf);
			}
		} else {
			return super.getInstanceIdName(id);
		}
	}

	protected Object getInstanceIdValue(int id) {
		int shifted = id - super.getMaxInstanceId();
		if (1 <= shifted && shifted <= 21) {
			RegExpImpl impl = getImpl();
			Object stringResult;
			switch (shifted) {
				case 1 :
				case 2 :
					return ScriptRuntime.wrapBoolean(impl.multiline);
				case 3 :
				case 4 :
					stringResult = impl.input;
					break;
				case 5 :
				case 6 :
					stringResult = impl.lastMatch;
					break;
				case 7 :
				case 8 :
					stringResult = impl.lastParen;
					break;
				case 9 :
				case 10 :
					stringResult = impl.leftContext;
					break;
				case 11 :
				case 12 :
					stringResult = impl.rightContext;
					break;
				default :
					int substring_number = shifted - 12 - 1;
					stringResult = impl.getParenSubString(substring_number);
			}

			return stringResult == null ? "" : stringResult.toString();
		} else {
			return super.getInstanceIdValue(id);
		}
	}

	protected void setInstanceIdValue(int id, Object value) {
		int shifted = id - super.getMaxInstanceId();
		switch (shifted) {
			case 1 :
			case 2 :
				getImpl().multiline = ScriptRuntime.toBoolean(value);
				return;
			case 3 :
			case 4 :
				getImpl().input = ScriptRuntime.toString(value);
				return;
			default :
				super.setInstanceIdValue(id, value);
		}
	}
}