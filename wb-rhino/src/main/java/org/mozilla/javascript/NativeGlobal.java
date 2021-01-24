/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import java.io.Serializable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.IdFunctionCall;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.NativeError;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.TokenStream;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.xml.XMLLib;

public class NativeGlobal implements Serializable, IdFunctionCall {
	static final long serialVersionUID = 6080442165748707530L;
	private static final String URI_DECODE_RESERVED = ";/?:@&=+$,#";
	private static final Object FTAG = "Global";
	private static final int Id_decodeURI = 1;
	private static final int Id_decodeURIComponent = 2;
	private static final int Id_encodeURI = 3;
	private static final int Id_encodeURIComponent = 4;
	private static final int Id_escape = 5;
	private static final int Id_eval = 6;
	private static final int Id_isFinite = 7;
	private static final int Id_isNaN = 8;
	private static final int Id_isXMLName = 9;
	private static final int Id_parseFloat = 10;
	private static final int Id_parseInt = 11;
	private static final int Id_unescape = 12;
	private static final int Id_uneval = 13;
	private static final int LAST_SCOPE_FUNCTION_ID = 13;
	private static final int Id_new_CommonError = 14;

	public static void init(Context cx, Scriptable scope, boolean sealed) {
		NativeGlobal obj = new NativeGlobal();

		for (int errorMethods = 1; errorMethods <= 13; ++errorMethods) {
			byte name = 1;
			String i;
			switch (errorMethods) {
				case 1 :
					i = "decodeURI";
					break;
				case 2 :
					i = "decodeURIComponent";
					break;
				case 3 :
					i = "encodeURI";
					break;
				case 4 :
					i = "encodeURIComponent";
					break;
				case 5 :
					i = "escape";
					break;
				case 6 :
					i = "eval";
					break;
				case 7 :
					i = "isFinite";
					break;
				case 8 :
					i = "isNaN";
					break;
				case 9 :
					i = "isXMLName";
					break;
				case 10 :
					i = "parseFloat";
					break;
				case 11 :
					i = "parseInt";
					name = 2;
					break;
				case 12 :
					i = "unescape";
					break;
				case 13 :
					i = "uneval";
					break;
				default :
					throw Kit.codeBug();
			}

			IdFunctionObject errorProto = new IdFunctionObject(obj, FTAG,
					errorMethods, i, name, scope);
			if (sealed) {
				errorProto.sealObject();
			}

			errorProto.exportAsScopeProperty();
		}

		ScriptableObject.defineProperty(scope, "NaN", ScriptRuntime.NaNobj, 2);
		ScriptableObject.defineProperty(scope, "Infinity",
				ScriptRuntime.wrapNumber(Double.POSITIVE_INFINITY), 2);
		ScriptableObject.defineProperty(scope, "undefined", Undefined.instance,
				2);
		String[] arg8 = new String[]{"ConversionError", "EvalError",
				"RangeError", "ReferenceError", "SyntaxError", "TypeError",
				"URIError", "InternalError", "JavaException"};

		for (int arg11 = 0; arg11 < arg8.length; ++arg11) {
			String arg9 = arg8[arg11];
			Scriptable arg10 = ScriptRuntime.newObject(cx, scope, "Error",
					ScriptRuntime.emptyArgs);
			arg10.put("name", arg10, arg9);
			if (sealed && arg10 instanceof ScriptableObject) {
				((ScriptableObject) arg10).sealObject();
			}

			IdFunctionObject ctor = new IdFunctionObject(obj, FTAG, 14, arg9,
					1, scope);
			ctor.markAsConstructor(arg10);
			if (sealed) {
				ctor.sealObject();
			}

			ctor.exportAsScopeProperty();
		}

	}

	public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
			Scriptable thisObj, Object[] args) {
		if (f.hasTag(FTAG)) {
			int methodId = f.methodId();
			Object value;
			boolean value1;
			double xmlLib1;
			String value2;
			switch (methodId) {
				case 1 :
				case 2 :
					value2 = ScriptRuntime.toString(args, 0);
					return decode(value2, methodId == 1);
				case 3 :
				case 4 :
					value2 = ScriptRuntime.toString(args, 0);
					return encode(value2, methodId == 3);
				case 5 :
					return this.js_escape(args);
				case 6 :
					return this.js_eval(cx, scope, thisObj, args);
				case 7 :
					if (args.length < 1) {
						value1 = false;
					} else {
						xmlLib1 = ScriptRuntime.toNumber(args[0]);
						value1 = xmlLib1 == xmlLib1
								&& xmlLib1 != Double.POSITIVE_INFINITY
								&& xmlLib1 != Double.NEGATIVE_INFINITY;
					}

					return ScriptRuntime.wrapBoolean(value1);
				case 8 :
					if (args.length < 1) {
						value1 = true;
					} else {
						xmlLib1 = ScriptRuntime.toNumber(args[0]);
						value1 = xmlLib1 != xmlLib1;
					}

					return ScriptRuntime.wrapBoolean(value1);
				case 9 :
					value = args.length == 0 ? Undefined.instance : args[0];
					XMLLib xmlLib = XMLLib.extractFromScope(scope);
					return ScriptRuntime.wrapBoolean(xmlLib
							.isXMLName(cx, value));
				case 10 :
					return this.js_parseFloat(args);
				case 11 :
					return this.js_parseInt(args);
				case 12 :
					return this.js_unescape(args);
				case 13 :
					value = args.length != 0 ? args[0] : Undefined.instance;
					return ScriptRuntime.uneval(cx, scope, value);
				case 14 :
					return NativeError.make(cx, scope, f, args);
			}
		}

		throw f.unknown();
	}

	private Object js_parseInt(Object[] args) {
		String s = ScriptRuntime.toString(args, 0);
		int radix = ScriptRuntime.toInt32(args, 1);
		int len = s.length();
		if (len == 0) {
			return ScriptRuntime.NaNobj;
		} else {
			boolean negative = false;
			int start = 0;

			char c;
			do {
				c = s.charAt(start);
				if (!Character.isWhitespace(c)) {
					break;
				}

				++start;
			} while (start < len);

			if (c == 43 || (negative = c == 45)) {
				++start;
			}

			boolean NO_RADIX = true;
			if (radix == 0) {
				radix = -1;
			} else {
				if (radix < 2 || radix > 36) {
					return ScriptRuntime.NaNobj;
				}

				if (radix == 16 && len - start > 1 && s.charAt(start) == 48) {
					c = s.charAt(start + 1);
					if (c == 120 || c == 88) {
						start += 2;
					}
				}
			}

			if (radix == -1) {
				radix = 10;
				if (len - start > 1 && s.charAt(start) == 48) {
					c = s.charAt(start + 1);
					if (c != 120 && c != 88) {
						if (48 <= c && c <= 57) {
							radix = 8;
							++start;
						}
					} else {
						radix = 16;
						start += 2;
					}
				}
			}

			double d = ScriptRuntime.stringToNumber(s, start, radix);
			return ScriptRuntime.wrapNumber(negative ? -d : d);
		}
	}

	private Object js_parseFloat(Object[] args) {
		if (args.length < 1) {
			return ScriptRuntime.NaNobj;
		} else {
			String s = ScriptRuntime.toString(args[0]);
			int len = s.length();

			for (int start = 0; start != len; ++start) {
				char c = s.charAt(start);
				if (!TokenStream.isJSSpace(c)) {
					int i = start;
					if (c == 43 || c == 45) {
						i = start + 1;
						if (i == len) {
							return ScriptRuntime.NaNobj;
						}

						c = s.charAt(i);
					}

					if (c == 73) {
						if (i + 8 <= len
								&& s.regionMatches(i, "Infinity", 0, 8)) {
							double arg10;
							if (s.charAt(start) == 45) {
								arg10 = Double.NEGATIVE_INFINITY;
							} else {
								arg10 = Double.POSITIVE_INFINITY;
							}

							return ScriptRuntime.wrapNumber(arg10);
						}

						return ScriptRuntime.NaNobj;
					}

					int decimal = -1;

					label73 : for (int exponent = -1; i < len; ++i) {
						switch (s.charAt(i)) {
							case '+' :
							case '-' :
								if (exponent != i - 1) {
									break label73;
								}
								break;
							case ',' :
							case '/' :
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
							case '_' :
							case '`' :
							case 'a' :
							case 'b' :
							case 'c' :
							case 'd' :
							default :
								break label73;
							case '.' :
								if (decimal != -1) {
									break label73;
								}

								decimal = i;
							case '0' :
							case '1' :
							case '2' :
							case '3' :
							case '4' :
							case '5' :
							case '6' :
							case '7' :
							case '8' :
							case '9' :
								break;
							case 'E' :
							case 'e' :
								if (exponent != -1) {
									break label73;
								}

								exponent = i;
						}
					}

					s = s.substring(start, i);

					try {
						return Double.valueOf(s);
					} catch (NumberFormatException arg9) {
						return ScriptRuntime.NaNobj;
					}
				}
			}

			return ScriptRuntime.NaNobj;
		}
	}

	private Object js_escape(Object[] args) {
		boolean URL_XALPHAS = true;
		boolean URL_XPALPHAS = true;
		boolean URL_PATH = true;
		String s = ScriptRuntime.toString(args, 0);
		int mask = 7;
		if (args.length > 1) {
			double sb = ScriptRuntime.toNumber(args[1]);
			if (sb != sb || (double) (mask = (int) sb) != sb
					|| 0 != (mask & -8)) {
				throw Context.reportRuntimeError0("msg.bad.esc.mask");
			}
		}

		StringBuffer arg14 = null;
		int k = 0;

		for (int L = s.length(); k != L; ++k) {
			char c = s.charAt(k);
			if (mask != 0
					&& (c >= 48 && c <= 57 || c >= 65 && c <= 90 || c >= 97
							&& c <= 122 || c == 64 || c == 42 || c == 95
							|| c == 45 || c == 46 || 0 != (mask & 4)
							&& (c == 47 || c == 43))) {
				if (arg14 != null) {
					arg14.append((char) c);
				}
			} else {
				if (arg14 == null) {
					arg14 = new StringBuffer(L + 3);
					arg14.append(s);
					arg14.setLength(k);
				}

				byte hexSize;
				if (c < 256) {
					if (c == 32 && mask == 2) {
						arg14.append('+');
						continue;
					}

					arg14.append('%');
					hexSize = 2;
				} else {
					arg14.append('%');
					arg14.append('u');
					hexSize = 4;
				}

				for (int shift = (hexSize - 1) * 4; shift >= 0; shift -= 4) {
					int digit = 15 & c >> shift;
					int hc = digit < 10 ? 48 + digit : 55 + digit;
					arg14.append((char) hc);
				}
			}
		}

		return arg14 == null ? s : arg14.toString();
	}

	private Object js_unescape(Object[] args) {
		String s = ScriptRuntime.toString(args, 0);
		int firstEscapePos = s.indexOf(37);
		if (firstEscapePos >= 0) {
			int L = s.length();
			char[] buf = s.toCharArray();
			int destination = firstEscapePos;

			for (int k = firstEscapePos; k != L; ++destination) {
				char c = buf[k];
				++k;
				if (c == 37 && k != L) {
					int end;
					int start;
					if (buf[k] == 117) {
						start = k + 1;
						end = k + 5;
					} else {
						start = k;
						end = k + 2;
					}

					if (end <= L) {
						int x = 0;

						for (int i = start; i != end; ++i) {
							x = Kit.xDigitToInt(buf[i], x);
						}

						if (x >= 0) {
							c = (char) x;
							k = end;
						}
					}
				}

				buf[destination] = c;
			}

			s = new String(buf, 0, destination);
		}

		return s;
	}

	private Object js_eval(Context cx, Scriptable scope, Scriptable thisObj,
			Object[] args) {
		if (thisObj.getParentScope() == null) {
			return ScriptRuntime.evalSpecial(cx, scope, thisObj, args,
					"eval code", 1);
		} else {
			String m = ScriptRuntime.getMessage1("msg.cant.call.indirect",
					"eval");
			throw constructError(cx, "EvalError", m, scope);
		}
	}

	static boolean isEvalFunction(Object functionObj) {
		if (functionObj instanceof IdFunctionObject) {
			IdFunctionObject function = (IdFunctionObject) functionObj;
			if (function.hasTag(FTAG) && function.methodId() == 6) {
				return true;
			}
		}

		return false;
	}

	public static EcmaError constructError(Context cx, String error,
			String message, Scriptable scope) {
		return ScriptRuntime.constructError(error, message);
	}

	public static EcmaError constructError(Context cx, String error,
			String message, Scriptable scope, String sourceName,
			int lineNumber, int columnNumber, String lineSource) {
		return ScriptRuntime.constructError(error, message, sourceName,
				lineNumber, lineSource, columnNumber);
	}

	private static String encode(String str, boolean fullUri) {
		byte[] utf8buf = null;
		StringBuffer sb = null;
		int k = 0;

		for (int length = str.length(); k != length; ++k) {
			char C = str.charAt(k);
			if (encodeUnescaped(C, fullUri)) {
				if (sb != null) {
					sb.append(C);
				}
			} else {
				if (sb == null) {
					sb = new StringBuffer(length + 3);
					sb.append(str);
					sb.setLength(k);
					utf8buf = new byte[6];
				}

				if ('?' <= C && C <= '?') {
					throw Context.reportRuntimeError0("msg.bad.uri");
				}

				int V;
				if (C >= '?' && '?' >= C) {
					++k;
					if (k == length) {
						throw Context.reportRuntimeError0("msg.bad.uri");
					}

					char L = str.charAt(k);
					if ('?' > L || L > '?') {
						throw Context.reportRuntimeError0("msg.bad.uri");
					}

					V = (C - '?' << 10) + (L - '?') + 65536;
				} else {
					V = C;
				}

				int arg10 = oneUcs4ToUtf8Char(utf8buf, V);

				for (int j = 0; j < arg10; ++j) {
					int d = 255 & utf8buf[j];
					sb.append('%');
					sb.append(toHexChar(d >>> 4));
					sb.append(toHexChar(d & 15));
				}
			}
		}

		return sb == null ? str : sb.toString();
	}

	private static char toHexChar(int i) {
		if (i >> 4 != 0) {
			Kit.codeBug();
		}

		return (char) (i < 10 ? i + 48 : i - 10 + 65);
	}

	private static int unHex(char c) {
		return 65 <= c && c <= 70 ? c - 65 + 10 : (97 <= c && c <= 102
				? c - 97 + 10
				: (48 <= c && c <= 57 ? c - 48 : -1));
	}

	private static int unHex(char c1, char c2) {
		int i1 = unHex(c1);
		int i2 = unHex(c2);
		return i1 >= 0 && i2 >= 0 ? i1 << 4 | i2 : -1;
	}

	private static String decode(String str, boolean fullUri) {
		char[] buf = null;
		int bufTop = 0;
		int k = 0;
		int length = str.length();

		while (true) {
			while (k != length) {
				char C = str.charAt(k);
				if (C == 37) {
					if (buf == null) {
						buf = new char[length];
						str.getChars(0, k, buf, 0);
						bufTop = k;
					}

					int start = k;
					if (k + 3 > length) {
						throw Context.reportRuntimeError0("msg.bad.uri");
					}

					int B = unHex(str.charAt(k + 1), str.charAt(k + 2));
					if (B < 0) {
						throw Context.reportRuntimeError0("msg.bad.uri");
					}

					k += 3;
					if ((B & 128) == 0) {
						C = (char) B;
					} else {
						if ((B & 192) == 128) {
							throw Context.reportRuntimeError0("msg.bad.uri");
						}

						byte x;
						int ucs4Char;
						int minUcs4Char;
						if ((B & 32) == 0) {
							x = 1;
							ucs4Char = B & 31;
							minUcs4Char = 128;
						} else if ((B & 16) == 0) {
							x = 2;
							ucs4Char = B & 15;
							minUcs4Char = 2048;
						} else if ((B & 8) == 0) {
							x = 3;
							ucs4Char = B & 7;
							minUcs4Char = 65536;
						} else if ((B & 4) == 0) {
							x = 4;
							ucs4Char = B & 3;
							minUcs4Char = 2097152;
						} else {
							if ((B & 2) != 0) {
								throw Context
										.reportRuntimeError0("msg.bad.uri");
							}

							x = 5;
							ucs4Char = B & 1;
							minUcs4Char = 67108864;
						}

						if (k + 3 * x > length) {
							throw Context.reportRuntimeError0("msg.bad.uri");
						}

						int H = 0;

						while (true) {
							if (H != x) {
								if (str.charAt(k) != 37) {
									throw Context
											.reportRuntimeError0("msg.bad.uri");
								}

								B = unHex(str.charAt(k + 1), str.charAt(k + 2));
								if (B >= 0 && (B & 192) == 128) {
									ucs4Char = ucs4Char << 6 | B & 63;
									k += 3;
									++H;
									continue;
								}

								throw Context
										.reportRuntimeError0("msg.bad.uri");
							}

							if (ucs4Char < minUcs4Char || ucs4Char == '￾'
									|| ucs4Char == '￿') {
								ucs4Char = '�';
							}

							if (ucs4Char >= 65536) {
								ucs4Char -= 65536;
								if (ucs4Char > 1048575) {
									throw Context
											.reportRuntimeError0("msg.bad.uri");
								}

								char arg13 = (char) ((ucs4Char >>> 10) + '?');
								C = (char) ((ucs4Char & 1023) + '?');
								buf[bufTop++] = arg13;
							} else {
								C = (char) ucs4Char;
							}
							break;
						}
					}

					if (fullUri && ";/?:@&=+$,#".indexOf(C) >= 0) {
						for (int arg12 = start; arg12 != k; ++arg12) {
							buf[bufTop++] = str.charAt(arg12);
						}
					} else {
						buf[bufTop++] = C;
					}
				} else {
					if (buf != null) {
						buf[bufTop++] = C;
					}

					++k;
				}
			}

			return buf == null ? str : new String(buf, 0, bufTop);
		}
	}

	private static boolean encodeUnescaped(char c, boolean fullUri) {
		return (65 > c || c > 90) && (97 > c || c > 122) && (48 > c || c > 57)
				? ("-_.!~*\'()".indexOf(c) >= 0 ? true : (fullUri
						? ";/?:@&=+$,#".indexOf(c) >= 0
						: false)) : true;
	}

	private static int oneUcs4ToUtf8Char(byte[] utf8Buffer, int ucs4Char) {
		int utf8Length = 1;
		if ((ucs4Char & -128) == 0) {
			utf8Buffer[0] = (byte) ucs4Char;
		} else {
			int a = ucs4Char >>> 11;

			for (utf8Length = 2; a != 0; ++utf8Length) {
				a >>>= 5;
			}

			int i = utf8Length;

			while (true) {
				--i;
				if (i <= 0) {
					utf8Buffer[0] = (byte) (256 - (1 << 8 - utf8Length) + ucs4Char);
					break;
				}

				utf8Buffer[i] = (byte) (ucs4Char & 63 | 128);
				ucs4Char >>>= 6;
			}
		}

		return utf8Length;
	}
}