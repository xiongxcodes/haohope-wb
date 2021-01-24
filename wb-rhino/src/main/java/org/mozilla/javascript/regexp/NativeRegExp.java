/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.regexp;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.regexp.CompilerState;
import org.mozilla.javascript.regexp.NativeRegExpCtor;
import org.mozilla.javascript.regexp.REBackTrackData;
import org.mozilla.javascript.regexp.RECharSet;
import org.mozilla.javascript.regexp.RECompiled;
import org.mozilla.javascript.regexp.REGlobalData;
import org.mozilla.javascript.regexp.RENode;
import org.mozilla.javascript.regexp.REProgState;
import org.mozilla.javascript.regexp.RegExpImpl;
import org.mozilla.javascript.regexp.SubString;

public class NativeRegExp extends IdScriptableObject implements Function {
	static final long serialVersionUID = 4965263491464903264L;
	private static final Object REGEXP_TAG = new Object();
	public static final int JSREG_GLOB = 1;
	public static final int JSREG_FOLD = 2;
	public static final int JSREG_MULTILINE = 4;
	public static final int TEST = 0;
	public static final int MATCH = 1;
	public static final int PREFIX = 2;
	private static final boolean debug = false;
	private static final byte REOP_EMPTY = 0;
	private static final byte REOP_ALT = 1;
	private static final byte REOP_BOL = 2;
	private static final byte REOP_EOL = 3;
	private static final byte REOP_WBDRY = 4;
	private static final byte REOP_WNONBDRY = 5;
	private static final byte REOP_QUANT = 6;
	private static final byte REOP_STAR = 7;
	private static final byte REOP_PLUS = 8;
	private static final byte REOP_OPT = 9;
	private static final byte REOP_LPAREN = 10;
	private static final byte REOP_RPAREN = 11;
	private static final byte REOP_DOT = 12;
	private static final byte REOP_DIGIT = 14;
	private static final byte REOP_NONDIGIT = 15;
	private static final byte REOP_ALNUM = 16;
	private static final byte REOP_NONALNUM = 17;
	private static final byte REOP_SPACE = 18;
	private static final byte REOP_NONSPACE = 19;
	private static final byte REOP_BACKREF = 20;
	private static final byte REOP_FLAT = 21;
	private static final byte REOP_FLAT1 = 22;
	private static final byte REOP_JUMP = 23;
	private static final byte REOP_UCFLAT1 = 28;
	private static final byte REOP_FLATi = 32;
	private static final byte REOP_FLAT1i = 33;
	private static final byte REOP_UCFLAT1i = 35;
	private static final byte REOP_ASSERT = 41;
	private static final byte REOP_ASSERT_NOT = 42;
	private static final byte REOP_ASSERTTEST = 43;
	private static final byte REOP_ASSERTNOTTEST = 44;
	private static final byte REOP_MINIMALSTAR = 45;
	private static final byte REOP_MINIMALPLUS = 46;
	private static final byte REOP_MINIMALOPT = 47;
	private static final byte REOP_MINIMALQUANT = 48;
	private static final byte REOP_ENDCHILD = 49;
	private static final byte REOP_CLASS = 50;
	private static final byte REOP_REPEAT = 51;
	private static final byte REOP_MINIMALREPEAT = 52;
	private static final byte REOP_END = 53;
	private static final int OFFSET_LEN = 2;
	private static final int INDEX_LEN = 2;
	private static final int Id_lastIndex = 1;
	private static final int Id_source = 2;
	private static final int Id_global = 3;
	private static final int Id_ignoreCase = 4;
	private static final int Id_multiline = 5;
	private static final int MAX_INSTANCE_ID = 5;
	private static final int Id_compile = 1;
	private static final int Id_toString = 2;
	private static final int Id_toSource = 3;
	private static final int Id_exec = 4;
	private static final int Id_test = 5;
	private static final int Id_prefix = 6;
	private static final int MAX_PROTOTYPE_ID = 6;
	private RECompiled re;
	double lastIndex;

	public static void init(Context cx, Scriptable scope, boolean sealed) {
		NativeRegExp proto = new NativeRegExp();
		proto.re = (RECompiled) compileRE(cx, "", (String) null, false);
		proto.activatePrototypeMap(6);
		proto.setParentScope(scope);
		proto.setPrototype(getObjectPrototype(scope));
		NativeRegExpCtor ctor = new NativeRegExpCtor();
		proto.put("constructor", proto, ctor);
		ScriptRuntime.setFunctionProtoAndParent(ctor, scope);
		ctor.setImmunePrototypeProperty(proto);
		if (sealed) {
			proto.sealObject();
			ctor.sealObject();
		}

		defineProperty(scope, "RegExp", ctor, 2);
	}

	NativeRegExp(Scriptable scope, Object regexpCompiled) {
		this.re = (RECompiled) regexpCompiled;
		this.lastIndex = 0.0D;
		ScriptRuntime.setObjectProtoAndParent(this, scope);
	}

	public String getClassName() {
		return "RegExp";
	}

	public Object call(Context cx, Scriptable scope, Scriptable thisObj,
			Object[] args) {
		return this.execSub(cx, scope, args, 1);
	}

	public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
		return (Scriptable) this.execSub(cx, scope, args, 1);
	}

	Scriptable compile(Context cx, Scriptable scope, Object[] args) {
		if (args.length > 0 && args[0] instanceof NativeRegExp) {
			if (args.length > 1 && args[1] != Undefined.instance) {
				throw ScriptRuntime.typeError0("msg.bad.regexp.compile");
			} else {
				NativeRegExp s1 = (NativeRegExp) args[0];
				this.re = s1.re;
				this.lastIndex = s1.lastIndex;
				return this;
			}
		} else {
			String s = args.length == 0 ? "" : ScriptRuntime.toString(args[0]);
			String global = args.length > 1 && args[1] != Undefined.instance
					? ScriptRuntime.toString(args[1])
					: null;
			this.re = (RECompiled) compileRE(cx, s, global, false);
			this.lastIndex = 0.0D;
			return this;
		}
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append('/');
		if (this.re.source.length != 0) {
			buf.append(this.re.source);
		} else {
			buf.append("(?:)");
		}

		buf.append('/');
		if ((this.re.flags & 1) != 0) {
			buf.append('g');
		}

		if ((this.re.flags & 2) != 0) {
			buf.append('i');
		}

		if ((this.re.flags & 4) != 0) {
			buf.append('m');
		}

		return buf.toString();
	}

	NativeRegExp() {
	}

	private static RegExpImpl getImpl(Context cx) {
		return (RegExpImpl) ScriptRuntime.getRegExpProxy(cx);
	}

	private Object execSub(Context cx, Scriptable scopeObj, Object[] args,
			int matchType) {
		RegExpImpl reImpl = getImpl(cx);
		String str;
		if (args.length == 0) {
			str = reImpl.input;
			if (str == null) {
				reportError("msg.no.re.input.for", this.toString());
			}
		} else {
			str = ScriptRuntime.toString(args[0]);
		}

		double d = (this.re.flags & 1) != 0 ? this.lastIndex : 0.0D;
		Object rval;
		if (d >= 0.0D && (double) str.length() >= d) {
			int[] indexp = new int[]{(int) d};
			rval = this.executeRegExp(cx, scopeObj, reImpl, str, indexp,
					matchType);
			if ((this.re.flags & 1) != 0) {
				this.lastIndex = rval != null && rval != Undefined.instance
						? (double) indexp[0]
						: 0.0D;
			}
		} else {
			this.lastIndex = 0.0D;
			rval = null;
		}

		return rval;
	}

	static Object compileRE(Context cx, String str, String global, boolean flat) {
		RECompiled regexp = new RECompiled();
		regexp.source = str.toCharArray();
		int length = str.length();
		int flags = 0;
		if (global != null) {
			for (int state = 0; state < global.length(); ++state) {
				char endPC = global.charAt(state);
				if (endPC == 103) {
					flags |= 1;
				} else if (endPC == 105) {
					flags |= 2;
				} else if (endPC == 109) {
					flags |= 4;
				} else {
					reportError("msg.invalid.re.flag", String.valueOf(endPC));
				}
			}
		}

		regexp.flags = flags;
		CompilerState arg9 = new CompilerState(cx, regexp.source, length, flags);
		if (flat && length > 0) {
			arg9.result = new RENode(21);
			arg9.result.chr = arg9.cpbegin[0];
			arg9.result.length = length;
			arg9.result.flatIndex = 0;
			arg9.progLength += 5;
		} else if (!parseDisjunction(arg9)) {
			return null;
		}

		regexp.program = new byte[arg9.progLength + 1];
		if (arg9.classCount != 0) {
			regexp.classList = new RECharSet[arg9.classCount];
			regexp.classCount = arg9.classCount;
		}

		int arg10 = emitREBytecode(arg9, regexp, 0, arg9.result);
		regexp.program[arg10++] = 53;
		regexp.parenCount = arg9.parenCount;
		switch (regexp.program[0]) {
			case 21 :
			case 32 :
				int k = getIndex(regexp.program, 1);
				regexp.anchorCh = regexp.source[k];
				break;
			case 22 :
			case 33 :
				regexp.anchorCh = (char) (regexp.program[1] & 255);
			case 23 :
			case 24 :
			case 25 :
			case 26 :
			case 27 :
			case 29 :
			case 30 :
			case 31 :
			case 34 :
			default :
				break;
			case 28 :
			case 35 :
				regexp.anchorCh = (char) getIndex(regexp.program, 1);
		}

		return regexp;
	}

	static boolean isDigit(char c) {
		return 48 <= c && c <= 57;
	}

	private static boolean isWord(char c) {
		return Character.isLetter(c) || isDigit(c) || c == 95;
	}

	private static boolean isLineTerm(char c) {
		return ScriptRuntime.isJSLineTerminator(c);
	}

	private static boolean isREWhiteSpace(int c) {
		return c == 32 || c == 9 || c == 10 || c == 13 || c == 8232
				|| c == 8233 || c == 12 || c == 11 || c == 160
				|| Character.getType((char) c) == 12;
	}

	private static char upcase(char ch) {
		if (ch < 128) {
			return 97 <= ch && ch <= 122 ? (char) (ch + -32) : ch;
		} else {
			char cu = Character.toUpperCase(ch);
			return ch >= 128 && cu < 128 ? ch : cu;
		}
	}

	private static char downcase(char ch) {
		if (ch < 128) {
			return 65 <= ch && ch <= 90 ? (char) (ch + 32) : ch;
		} else {
			char cl = Character.toLowerCase(ch);
			return ch >= 128 && cl < 128 ? ch : cl;
		}
	}

	private static int toASCIIHexDigit(int c) {
		if (c < 48) {
			return -1;
		} else if (c <= 57) {
			return c - 48;
		} else {
			c |= 32;
			return 97 <= c && c <= 102 ? c - 97 + 10 : -1;
		}
	}

	private static boolean parseDisjunction(CompilerState state) {
		if (!parseAlternative(state)) {
			return false;
		} else {
			char[] source = state.cpbegin;
			int index = state.cp;
			if (index != source.length && source[index] == 124) {
				++state.cp;
				RENode altResult = new RENode(1);
				altResult.kid = state.result;
				if (!parseDisjunction(state)) {
					return false;
				}

				altResult.kid2 = state.result;
				state.result = altResult;
				state.progLength += 9;
			}

			return true;
		}
	}

	private static boolean parseAlternative(CompilerState state) {
		RENode headTerm = null;
		RENode tailTerm = null;
		char[] source = state.cpbegin;

		while (state.cp != state.cpend && source[state.cp] != 124
				&& (state.parenNesting == 0 || source[state.cp] != 41)) {
			if (!parseTerm(state)) {
				return false;
			}

			if (headTerm == null) {
				headTerm = state.result;
			} else if (tailTerm == null) {
				headTerm.next = state.result;

				for (tailTerm = state.result; tailTerm.next != null; tailTerm = tailTerm.next) {
					;
				}
			} else {
				tailTerm.next = state.result;

				for (tailTerm = tailTerm.next; tailTerm.next != null; tailTerm = tailTerm.next) {
					;
				}
			}
		}

		if (headTerm == null) {
			state.result = new RENode(0);
		} else {
			state.result = headTerm;
		}

		return true;
	}

	private static boolean calculateBitmapSize(CompilerState state,
			RENode target, char[] src, int index, int end) {
		char rangeStart = 0;
		int max = 0;
		boolean inRange = false;
		target.bmsize = 0;
		if (index == end) {
			return true;
		} else {
			if (src[index] == 94) {
				++index;
			}

			while (index != end) {
				int arg14;
				boolean localMax = false;
				int nDigits = 2;
				label104 : switch (src[index]) {
					case '\\' :
						++index;
						char c = src[index++];
						int n;
						int i;
						switch (c) {
							case '0' :
							case '1' :
							case '2' :
							case '3' :
							case '4' :
							case '5' :
							case '6' :
							case '7' :
								n = c - 48;
								c = src[index];
								if (48 <= c && c <= 55) {
									++index;
									n = 8 * n + (c - 48);
									c = src[index];
									if (48 <= c && c <= 55) {
										++index;
										i = 8 * n + (c - 48);
										if (i <= 255) {
											n = i;
										} else {
											--index;
										}
									}
								}

								arg14 = n;
								break label104;
							case '8' :
							case '9' :
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
							case 'T' :
							case 'U' :
							case 'V' :
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
							case 'e' :
							case 'g' :
							case 'h' :
							case 'i' :
							case 'j' :
							case 'k' :
							case 'l' :
							case 'm' :
							case 'o' :
							case 'p' :
							case 'q' :
							default :
								arg14 = c;
								break label104;
							case 'D' :
							case 'S' :
							case 'W' :
							case 's' :
							case 'w' :
								if (inRange) {
									reportError("msg.bad.range", "");
									return false;
								} else {
									target.bmsize = '￿';
									return true;
								}
							case 'b' :
								arg14 = 8;
								break label104;
							case 'c' :
								if (index + 1 < end
										&& Character.isLetter(src[index + 1])) {
									arg14 = (char) (src[index++] & 31);
								} else {
									arg14 = 92;
								}
								break label104;
							case 'd' :
								if (inRange) {
									reportError("msg.bad.range", "");
									return false;
								}

								arg14 = 57;
								break label104;
							case 'f' :
								arg14 = 12;
								break label104;
							case 'n' :
								arg14 = 10;
								break label104;
							case 'r' :
								arg14 = 13;
								break label104;
							case 't' :
								arg14 = 9;
								break label104;
							case 'u' :
								nDigits += 2;
							case 'x' :
								n = 0;

								for (i = 0; i < nDigits && index < end; ++i) {
									c = src[index++];
									n = Kit.xDigitToInt(c, n);
									if (n < 0) {
										index -= i + 1;
										n = 92;
										break;
									}
								}

								arg14 = n;
								break label104;
							case 'v' :
								arg14 = 11;
								break label104;
						}
					default :
						arg14 = src[index++];
				}

				if (inRange) {
					if (rangeStart > arg14) {
						reportError("msg.bad.range", "");
						return false;
					}

					inRange = false;
				} else if (index < end - 1 && src[index] == 45) {
					++index;
					inRange = true;
					rangeStart = (char) arg14;
					continue;
				}

				if ((state.flags & 2) != 0) {
					char cu = upcase((char) arg14);
					char cd = downcase((char) arg14);
					arg14 = cu >= cd ? cu : cd;
				}

				if (arg14 > max) {
					max = arg14;
				}
			}

			target.bmsize = max;
			return true;
		}
	}

	private static void doFlat(CompilerState state, char c) {
		state.result = new RENode(21);
		state.result.chr = c;
		state.result.length = 1;
		state.result.flatIndex = -1;
		state.progLength += 3;
	}

	private static int getDecimalValue(char c, CompilerState state,
			int maxValue, String overflowMessageId) {
		boolean overflow = false;
		int start = state.cp;
		char[] src = state.cpbegin;

		int value;
		for (value = c - 48; state.cp != state.cpend; ++state.cp) {
			c = src[state.cp];
			if (!isDigit(c)) {
				break;
			}

			if (!overflow) {
				int digit = c - 48;
				if (value < (maxValue - digit) / 10) {
					value = value * 10 + digit;
				} else {
					overflow = true;
					value = maxValue;
				}
			}
		}

		if (overflow) {
			reportError(overflowMessageId,
					String.valueOf(src, start, state.cp - start));
		}

		return value;
	}

	private static boolean parseTerm(CompilerState state) {
		char[] src;
		char c;
		int parenBaseCount;
		int min;
		src = state.cpbegin;
		c = src[state.cp++];
		int nDigits = 2;
		parenBaseCount = state.parenCount;
		int termStart;
		label208 : switch (c) {
			case '$' :
				state.result = new RENode(3);
				++state.progLength;
				return true;
			case '(' :
				RENode arg13 = null;
				termStart = state.cp;
				if (state.cp + 1 < state.cpend
						&& src[state.cp] == 63
						&& ((c = src[state.cp + 1]) == 61 || c == 33 || c == 58)) {
					state.cp += 2;
					if (c == 61) {
						arg13 = new RENode(41);
						state.progLength += 4;
					} else if (c == 33) {
						arg13 = new RENode(42);
						state.progLength += 4;
					}
				} else {
					arg13 = new RENode(10);
					state.progLength += 6;
					arg13.parenIndex = state.parenCount++;
				}

				++state.parenNesting;
				if (!parseDisjunction(state)) {
					return false;
				}

				if (state.cp == state.cpend || src[state.cp] != 41) {
					reportError("msg.unterm.paren", "");
					return false;
				}

				++state.cp;
				--state.parenNesting;
				if (arg13 != null) {
					arg13.kid = state.result;
					state.result = arg13;
				}
				break;
			case ')' :
				reportError("msg.re.unmatched.right.paren", "");
				return false;
			case '*' :
			case '+' :
			case '?' :
				reportError("msg.bad.quant", String.valueOf(src[state.cp - 1]));
				return false;
			case '.' :
				state.result = new RENode(12);
				++state.progLength;
				break;
			case '[' :
				state.result = new RENode(50);
				termStart = state.cp;

				for (state.result.startIndex = termStart; state.cp != state.cpend; ++state.cp) {
					if (src[state.cp] == 92) {
						++state.cp;
					} else if (src[state.cp] == 93) {
						state.result.kidlen = state.cp - termStart;
						state.result.index = state.classCount++;
						if (!calculateBitmapSize(state, state.result, src,
								termStart, state.cp++)) {
							return false;
						}

						state.progLength += 3;
						break label208;
					}
				}

				reportError("msg.unterm.class", "");
				return false;
			case '\\' :
				if (state.cp >= state.cpend) {
					reportError("msg.trail.backslash", "");
					return false;
				}

				c = src[state.cp++];
				int num;
				int tmp;
				char arg12;
				switch (c) {
					case '0' :
						reportWarning(state.cx, "msg.bad.backref", "");

						for (num = 0; state.cp < state.cpend; num = tmp) {
							c = src[state.cp];
							if (c < 48 || c > 55) {
								break;
							}

							++state.cp;
							tmp = 8 * num + (c - 48);
							if (tmp > 255) {
								break;
							}
						}

						c = (char) num;
						doFlat(state, c);
						break label208;
					case '1' :
					case '2' :
					case '3' :
					case '4' :
					case '5' :
					case '6' :
					case '7' :
					case '8' :
					case '9' :
						termStart = state.cp - 1;
						num = getDecimalValue(c, state, '￿',
								"msg.overlarge.backref");
						if (num > state.parenCount) {
							reportWarning(state.cx, "msg.bad.backref", "");
						}

						if (num > 9 && num > state.parenCount) {
							state.cp = termStart;

							for (num = 0; state.cp < state.cpend; num = tmp) {
								c = src[state.cp];
								if (c < 48 || c > 55) {
									break;
								}

								++state.cp;
								tmp = 8 * num + (c - 48);
								if (tmp > 255) {
									break;
								}
							}

							c = (char) num;
							doFlat(state, c);
						} else {
							state.result = new RENode(20);
							state.result.parenIndex = num - 1;
							state.progLength += 3;
						}
						break label208;
					case ':' :
					case ';' :
					case '<' :
					case '=' :
					case '>' :
					case '?' :
					case '@' :
					case 'A' :
					case 'C' :
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
					case 'T' :
					case 'U' :
					case 'V' :
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
					case 'e' :
					case 'g' :
					case 'h' :
					case 'i' :
					case 'j' :
					case 'k' :
					case 'l' :
					case 'm' :
					case 'o' :
					case 'p' :
					case 'q' :
					default :
						state.result = new RENode(21);
						state.result.chr = c;
						state.result.length = 1;
						state.result.flatIndex = state.cp - 1;
						state.progLength += 3;
						break label208;
					case 'B' :
						state.result = new RENode(5);
						++state.progLength;
						return true;
					case 'D' :
						state.result = new RENode(15);
						++state.progLength;
						break label208;
					case 'S' :
						state.result = new RENode(19);
						++state.progLength;
						break label208;
					case 'W' :
						state.result = new RENode(17);
						++state.progLength;
						break label208;
					case 'b' :
						state.result = new RENode(4);
						++state.progLength;
						return true;
					case 'c' :
						if (state.cp + 1 < state.cpend
								&& Character.isLetter(src[state.cp + 1])) {
							c = (char) (src[state.cp++] & 31);
						} else {
							--state.cp;
							c = 92;
						}

						doFlat(state, c);
						break label208;
					case 'd' :
						state.result = new RENode(14);
						++state.progLength;
						break label208;
					case 'f' :
						arg12 = 12;
						doFlat(state, arg12);
						break label208;
					case 'n' :
						arg12 = 10;
						doFlat(state, arg12);
						break label208;
					case 'r' :
						arg12 = 13;
						doFlat(state, arg12);
						break label208;
					case 's' :
						state.result = new RENode(18);
						++state.progLength;
						break label208;
					case 't' :
						arg12 = 9;
						doFlat(state, arg12);
						break label208;
					case 'u' :
						nDigits += 2;
					case 'x' :
						int hasQ = 0;

						for (min = 0; min < nDigits && state.cp < state.cpend; ++min) {
							c = src[state.cp++];
							hasQ = Kit.xDigitToInt(c, hasQ);
							if (hasQ < 0) {
								state.cp -= min + 2;
								hasQ = src[state.cp++];
								break;
							}
						}

						c = (char) hasQ;
						doFlat(state, c);
						break label208;
					case 'v' :
						arg12 = 11;
						doFlat(state, arg12);
						break label208;
					case 'w' :
						state.result = new RENode(16);
						++state.progLength;
						break label208;
				}
			case '^' :
				state.result = new RENode(2);
				++state.progLength;
				return true;
			default :
				state.result = new RENode(21);
				state.result.chr = c;
				state.result.length = 1;
				state.result.flatIndex = state.cp - 1;
				state.progLength += 3;
		}

		RENode term = state.result;
		if (state.cp == state.cpend) {
			return true;
		} else {
			boolean arg15 = false;
			switch (src[state.cp]) {
				case '*' :
					state.result = new RENode(6);
					state.result.min = 0;
					state.result.max = -1;
					state.progLength += 8;
					arg15 = true;
					break;
				case '+' :
					state.result = new RENode(6);
					state.result.min = 1;
					state.result.max = -1;
					state.progLength += 8;
					arg15 = true;
					break;
				case '?' :
					state.result = new RENode(6);
					state.result.min = 0;
					state.result.max = 1;
					state.progLength += 8;
					arg15 = true;
					break;
				case '{' :
					boolean arg14 = false;
					int max = -1;
					int leftCurl = state.cp;
					c = src[++state.cp];
					if (isDigit(c)) {
						++state.cp;
						min = getDecimalValue(c, state, '￿',
								"msg.overlarge.min");
						c = src[state.cp];
						if (c == 44) {
							c = src[++state.cp];
							if (isDigit(c)) {
								++state.cp;
								max = getDecimalValue(c, state, '￿',
										"msg.overlarge.max");
								c = src[state.cp];
								if (min > max) {
									reportError("msg.max.lt.min",
											String.valueOf(src[state.cp]));
									return false;
								}
							}
						} else {
							max = min;
						}

						if (c == 125) {
							state.result = new RENode(6);
							state.result.min = min;
							state.result.max = max;
							state.progLength += 12;
							arg15 = true;
						}
					}

					if (!arg15) {
						state.cp = leftCurl;
					}
			}

			if (!arg15) {
				return true;
			} else {
				++state.cp;
				state.result.kid = term;
				state.result.parenIndex = parenBaseCount;
				state.result.parenCount = state.parenCount - parenBaseCount;
				if (state.cp < state.cpend && src[state.cp] == 63) {
					++state.cp;
					state.result.greedy = false;
				} else {
					state.result.greedy = true;
				}

				return true;
			}
		}
	}

	private static void resolveForwardJump(byte[] array, int from, int pc) {
		if (from > pc) {
			throw Kit.codeBug();
		} else {
			addIndex(array, from, pc - from);
		}
	}

	private static int getOffset(byte[] array, int pc) {
		return getIndex(array, pc);
	}

	private static int addIndex(byte[] array, int pc, int index) {
		if (index < 0) {
			throw Kit.codeBug();
		} else if (index > '￿') {
			throw Context.reportRuntimeError("Too complex regexp");
		} else {
			array[pc] = (byte) (index >> 8);
			array[pc + 1] = (byte) index;
			return pc + 2;
		}
	}

	private static int getIndex(byte[] array, int pc) {
		return (array[pc] & 255) << 8 | array[pc + 1] & 255;
	}

	private static int emitREBytecode(CompilerState state, RECompiled re,
			int pc, RENode t) {
		for (byte[] program = re.program; t != null; t = t.next) {
			program[pc++] = t.op;
			int nextTermFixup;
			switch (t.op) {
				case 0 :
					--pc;
					break;
				case 1 :
					RENode nextAlt = t.kid2;
					int nextAltFixup = pc;
					pc += 2;
					pc = emitREBytecode(state, re, pc, t.kid);
					program[pc++] = 23;
					nextTermFixup = pc;
					pc += 2;
					resolveForwardJump(program, nextAltFixup, pc);
					pc = emitREBytecode(state, re, pc, nextAlt);
					program[pc++] = 23;
					nextAltFixup = pc;
					pc += 2;
					resolveForwardJump(program, nextTermFixup, pc);
					resolveForwardJump(program, nextAltFixup, pc);
					break;
				case 6 :
					if (t.min == 0 && t.max == -1) {
						program[pc - 1] = (byte) (t.greedy ? 7 : 45);
					} else if (t.min == 0 && t.max == 1) {
						program[pc - 1] = (byte) (t.greedy ? 9 : 47);
					} else if (t.min == 1 && t.max == -1) {
						program[pc - 1] = (byte) (t.greedy ? 8 : 46);
					} else {
						if (!t.greedy) {
							program[pc - 1] = 48;
						}

						pc = addIndex(program, pc, t.min);
						pc = addIndex(program, pc, t.max + 1);
					}

					pc = addIndex(program, pc, t.parenCount);
					pc = addIndex(program, pc, t.parenIndex);
					nextTermFixup = pc;
					pc += 2;
					pc = emitREBytecode(state, re, pc, t.kid);
					program[pc++] = 49;
					resolveForwardJump(program, nextTermFixup, pc);
					break;
				case 10 :
					pc = addIndex(program, pc, t.parenIndex);
					pc = emitREBytecode(state, re, pc, t.kid);
					program[pc++] = 11;
					pc = addIndex(program, pc, t.parenIndex);
					break;
				case 20 :
					pc = addIndex(program, pc, t.parenIndex);
					break;
				case 21 :
					if (t.flatIndex != -1) {
						while (t.next != null && t.next.op == 21
								&& t.flatIndex + t.length == t.next.flatIndex) {
							t.length += t.next.length;
							t.next = t.next.next;
						}
					}

					if (t.flatIndex != -1 && t.length > 1) {
						if ((state.flags & 2) != 0) {
							program[pc - 1] = 32;
						} else {
							program[pc - 1] = 21;
						}

						pc = addIndex(program, pc, t.flatIndex);
						pc = addIndex(program, pc, t.length);
					} else if (t.chr < 256) {
						if ((state.flags & 2) != 0) {
							program[pc - 1] = 33;
						} else {
							program[pc - 1] = 22;
						}

						program[pc++] = (byte) t.chr;
					} else {
						if ((state.flags & 2) != 0) {
							program[pc - 1] = 35;
						} else {
							program[pc - 1] = 28;
						}

						pc = addIndex(program, pc, t.chr);
					}
					break;
				case 41 :
					nextTermFixup = pc;
					pc += 2;
					pc = emitREBytecode(state, re, pc, t.kid);
					program[pc++] = 43;
					resolveForwardJump(program, nextTermFixup, pc);
					break;
				case 42 :
					nextTermFixup = pc;
					pc += 2;
					pc = emitREBytecode(state, re, pc, t.kid);
					program[pc++] = 44;
					resolveForwardJump(program, nextTermFixup, pc);
					break;
				case 50 :
					pc = addIndex(program, pc, t.index);
					re.classList[t.index] = new RECharSet(t.bmsize,
							t.startIndex, t.kidlen);
			}
		}

		return pc;
	}

	private static void pushProgState(REGlobalData gData, int min, int max,
			REBackTrackData backTrackLastToSave, int continuation_pc,
			int continuation_op) {
		gData.stateStackTop = new REProgState(gData.stateStackTop, min, max,
				gData.cp, backTrackLastToSave, continuation_pc, continuation_op);
	}

	private static REProgState popProgState(REGlobalData gData) {
		REProgState state = gData.stateStackTop;
		gData.stateStackTop = state.previous;
		return state;
	}

	private static void pushBackTrackState(REGlobalData gData, int i,
			int target) {
		gData.backTrackStackTop = new REBackTrackData(gData, i, target);
	}

	private static boolean flatNMatcher(REGlobalData gData, int matchChars,
			int length, char[] chars, int end) {
		if (gData.cp + length > end) {
			return false;
		} else {
			for (int i = 0; i < length; ++i) {
				if (gData.regexp.source[matchChars + i] != chars[gData.cp + i]) {
					return false;
				}
			}

			gData.cp += length;
			return true;
		}
	}

	private static boolean flatNIMatcher(REGlobalData gData, int matchChars,
			int length, char[] chars, int end) {
		if (gData.cp + length > end) {
			return false;
		} else {
			for (int i = 0; i < length; ++i) {
				if (upcase(gData.regexp.source[matchChars + i]) != upcase(chars[gData.cp
						+ i])) {
					return false;
				}
			}

			gData.cp += length;
			return true;
		}
	}

	private static boolean backrefMatcher(REGlobalData gData, int parenIndex,
			char[] chars, int end) {
		int parenContent = gData.parens_index(parenIndex);
		if (parenContent == -1) {
			return true;
		} else {
			int len = gData.parens_length(parenIndex);
			if (gData.cp + len > end) {
				return false;
			} else {
				int i;
				if ((gData.regexp.flags & 2) != 0) {
					for (i = 0; i < len; ++i) {
						if (upcase(chars[parenContent + i]) != upcase(chars[gData.cp
								+ i])) {
							return false;
						}
					}
				} else {
					for (i = 0; i < len; ++i) {
						if (chars[parenContent + i] != chars[gData.cp + i]) {
							return false;
						}
					}
				}

				gData.cp += len;
				return true;
			}
		}
	}

	private static void addCharacterToCharSet(RECharSet cs, char c) {
		int byteIndex = c / 8;
		if (c > cs.length) {
			throw new RuntimeException();
		} else {
			cs.bits[byteIndex] = (byte) (cs.bits[byteIndex] | 1 << (c & 7));
		}
	}

	private static void addCharacterRangeToCharSet(RECharSet cs, char c1,
			char c2) {
		int byteIndex1 = c1 / 8;
		int byteIndex2 = c2 / 8;
		if (c2 <= cs.length && c1 <= c2) {
			c1 = (char) (c1 & 7);
			c2 = (char) (c2 & 7);
			if (byteIndex1 == byteIndex2) {
				cs.bits[byteIndex1] = (byte) (cs.bits[byteIndex1] | 255 >> 7 - (c2 - c1) << c1);
			} else {
				cs.bits[byteIndex1] = (byte) (cs.bits[byteIndex1] | 255 << c1);

				for (int i = byteIndex1 + 1; i < byteIndex2; ++i) {
					cs.bits[i] = -1;
				}

				cs.bits[byteIndex2] = (byte) (cs.bits[byteIndex2] | 255 >> 7 - c2);
			}

		} else {
			throw new RuntimeException();
		}
	}

	private static void processCharSet(REGlobalData gData, RECharSet charSet) {
		synchronized (charSet) {
			if (!charSet.converted) {
				processCharSetImpl(gData, charSet);
				charSet.converted = true;
			}

		}
	}

	private static void processCharSetImpl(REGlobalData gData, RECharSet charSet) {
		int src = charSet.startIndex;
		int end = src + charSet.strlength;
		char rangeStart = 0;
		boolean inRange = false;
		charSet.sense = true;
		int byteLength = charSet.length / 8 + 1;
		charSet.bits = new byte[byteLength];
		if (src != end) {
			if (gData.regexp.source[src] == 94) {
				charSet.sense = false;
				++src;
			}

			while (true) {
				label131 : while (src != end) {
					char thisCh;
					int nDigits = 2;
					label128 : switch (gData.regexp.source[src]) {
						case '\\' :
							++src;
							char c = gData.regexp.source[src++];
							int n;
							int i;
							switch (c) {
								case '0' :
								case '1' :
								case '2' :
								case '3' :
								case '4' :
								case '5' :
								case '6' :
								case '7' :
									n = c - 48;
									c = gData.regexp.source[src];
									if (48 <= c && c <= 55) {
										++src;
										n = 8 * n + (c - 48);
										c = gData.regexp.source[src];
										if (48 <= c && c <= 55) {
											++src;
											i = 8 * n + (c - 48);
											if (i <= 255) {
												n = i;
											} else {
												--src;
											}
										}
									}

									thisCh = (char) n;
									break label128;
								case '8' :
								case '9' :
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
								case 'T' :
								case 'U' :
								case 'V' :
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
								case 'e' :
								case 'g' :
								case 'h' :
								case 'i' :
								case 'j' :
								case 'k' :
								case 'l' :
								case 'm' :
								case 'o' :
								case 'p' :
								case 'q' :
								default :
									thisCh = c;
									break label128;
								case 'D' :
									addCharacterRangeToCharSet(charSet, ' ',
											'/');
									addCharacterRangeToCharSet(charSet, ':',
											(char) charSet.length);
									continue;
								case 'S' :
									i = charSet.length;

									while (true) {
										if (i < 0) {
											continue label131;
										}

										if (!isREWhiteSpace(i)) {
											addCharacterToCharSet(charSet,
													(char) i);
										}

										--i;
									}
								case 'W' :
									i = charSet.length;

									while (true) {
										if (i < 0) {
											continue label131;
										}

										if (!isWord((char) i)) {
											addCharacterToCharSet(charSet,
													(char) i);
										}

										--i;
									}
								case 'b' :
									thisCh = 8;
									break label128;
								case 'c' :
									if (src + 1 < end
											&& isWord(gData.regexp.source[src + 1])) {
										thisCh = (char) (gData.regexp.source[src++] & 31);
									} else {
										--src;
										thisCh = 92;
									}
									break label128;
								case 'd' :
									addCharacterRangeToCharSet(charSet, '0',
											'9');
									continue;
								case 'f' :
									thisCh = 12;
									break label128;
								case 'n' :
									thisCh = 10;
									break label128;
								case 'r' :
									thisCh = 13;
									break label128;
								case 's' :
									i = charSet.length;

									while (true) {
										if (i < 0) {
											continue label131;
										}

										if (isREWhiteSpace(i)) {
											addCharacterToCharSet(charSet,
													(char) i);
										}

										--i;
									}
								case 't' :
									thisCh = 9;
									break label128;
								case 'u' :
									nDigits += 2;
								case 'x' :
									n = 0;

									for (i = 0; i < nDigits && src < end; ++i) {
										c = gData.regexp.source[src++];
										int digit = toASCIIHexDigit(c);
										if (digit < 0) {
											src -= i + 1;
											n = 92;
											break;
										}

										n = n << 4 | digit;
									}

									thisCh = (char) n;
									break label128;
								case 'v' :
									thisCh = 11;
									break label128;
								case 'w' :
									for (i = charSet.length; i >= 0; --i) {
										if (isWord((char) i)) {
											addCharacterToCharSet(charSet,
													(char) i);
										}
									}
									continue;
							}
						default :
							thisCh = gData.regexp.source[src++];
					}

					if (inRange) {
						if ((gData.regexp.flags & 2) != 0) {
							addCharacterRangeToCharSet(charSet,
									upcase(rangeStart), upcase(thisCh));
							addCharacterRangeToCharSet(charSet,
									downcase(rangeStart), downcase(thisCh));
						} else {
							addCharacterRangeToCharSet(charSet, rangeStart,
									thisCh);
						}

						inRange = false;
					} else {
						if ((gData.regexp.flags & 2) != 0) {
							addCharacterToCharSet(charSet, upcase(thisCh));
							addCharacterToCharSet(charSet, downcase(thisCh));
						} else {
							addCharacterToCharSet(charSet, thisCh);
						}

						if (src < end - 1 && gData.regexp.source[src] == 45) {
							++src;
							inRange = true;
							rangeStart = thisCh;
						}
					}
				}

				return;
			}
		}
	}

	private static boolean classMatcher(REGlobalData gData, RECharSet charSet,
			char ch) {
		if (!charSet.converted) {
			processCharSet(gData, charSet);
		}

		int byteIndex = ch / 8;
		if (charSet.sense) {
			if (charSet.length == 0 || ch > charSet.length
					|| (charSet.bits[byteIndex] & 1 << (ch & 7)) == 0) {
				return false;
			}
		} else if (charSet.length != 0 && ch <= charSet.length
				&& (charSet.bits[byteIndex] & 1 << (ch & 7)) != 0) {
			return false;
		}

		return true;
	}

	private static boolean executeREBytecode(REGlobalData gData, char[] chars,
			int end) {
		byte pc = 0;
		byte[] program = gData.regexp.program;
		boolean result = false;
		int currentContinuation_pc = 0;
		int currentContinuation_op = 53;
		int arg14 = pc + 1;
		int op = program[pc];

		while (true) {
			while (true) {
				while (true) {
					while (true) {
						while (true) {
							while (true) {
								label378 : while (true) {
									REProgState backTrackData;
									int new_min;
									int new_max;
									int parenCount;
									int parenIndex;
									int k;
									int arg15;
									char arg17;
									switch (op) {
										case 0 :
											result = true;
											break label378;
										case 1 :
											pushProgState(gData, 0, 0,
													(REBackTrackData) null,
													currentContinuation_pc,
													currentContinuation_op);
											arg15 = arg14
													+ getOffset(program, arg14);
											byte arg20 = program[arg15++];
											pushBackTrackState(gData, arg20,
													arg15);
											arg14 += 2;
											op = program[arg14++];
											break;
										case 2 :
											if (gData.cp != 0) {
												if (!gData.multiline
														&& (gData.regexp.flags & 4) == 0) {
													result = false;
													break label378;
												}

												if (!isLineTerm(chars[gData.cp - 1])) {
													result = false;
													break label378;
												}
											}

											result = true;
											break label378;
										case 3 :
											if (gData.cp != end) {
												if (!gData.multiline
														&& (gData.regexp.flags & 4) == 0) {
													result = false;
													break label378;
												}

												if (!isLineTerm(chars[gData.cp])) {
													result = false;
													break label378;
												}
											}

											result = true;
											break label378;
										case 4 :
											result = (gData.cp == 0 || !isWord(chars[gData.cp - 1]))
													^ (gData.cp >= end || !isWord(chars[gData.cp]));
											break label378;
										case 5 :
											result = (gData.cp == 0 || !isWord(chars[gData.cp - 1]))
													^ (gData.cp < end && isWord(chars[gData.cp]));
											break label378;
										case 6 :
										case 7 :
										case 8 :
										case 9 :
										case 45 :
										case 46 :
										case 47 :
										case 48 :
											boolean arg19 = false;
											switch (op) {
												case 6 :
													arg19 = true;
												case 48 :
													arg15 = getOffset(program,
															arg14);
													arg14 += 2;
													new_min = getOffset(
															program, arg14) - 1;
													arg14 += 2;
													break;
												case 7 :
													arg19 = true;
												case 45 :
													arg15 = 0;
													new_min = -1;
													break;
												case 8 :
													arg19 = true;
												case 46 :
													arg15 = 1;
													new_min = -1;
													break;
												case 9 :
													arg19 = true;
												case 47 :
													arg15 = 0;
													new_min = 1;
													break;
												default :
													throw Kit.codeBug();
											}

											pushProgState(gData, arg15,
													new_min,
													(REBackTrackData) null,
													currentContinuation_pc,
													currentContinuation_op);
											if (arg19) {
												currentContinuation_op = 51;
												currentContinuation_pc = arg14;
												pushBackTrackState(gData, 51,
														arg14);
												arg14 += 6;
												op = program[arg14++];
											} else if (arg15 != 0) {
												currentContinuation_op = 52;
												currentContinuation_pc = arg14;
												arg14 += 6;
												op = program[arg14++];
											} else {
												pushBackTrackState(gData, 52,
														arg14);
												popProgState(gData);
												arg14 += 4;
												arg14 += getOffset(program,
														arg14);
												op = program[arg14++];
											}
											break;
										case 10 :
											arg15 = getIndex(program, arg14);
											arg14 += 2;
											gData.set_parens(arg15, gData.cp, 0);
											op = program[arg14++];
											break;
										case 11 :
											new_min = getIndex(program, arg14);
											arg14 += 2;
											arg15 = gData.parens_index(new_min);
											gData.set_parens(new_min, arg15,
													gData.cp - arg15);
											if (new_min > gData.lastParen) {
												gData.lastParen = new_min;
											}

											op = program[arg14++];
											break;
										case 12 :
											result = gData.cp != end
													&& !isLineTerm(chars[gData.cp]);
											if (result) {
												++gData.cp;
											}
											break label378;
										case 13 :
										case 24 :
										case 25 :
										case 26 :
										case 27 :
										case 29 :
										case 30 :
										case 31 :
										case 34 :
										case 36 :
										case 37 :
										case 38 :
										case 39 :
										case 40 :
										default :
											throw Kit.codeBug();
										case 14 :
											result = gData.cp != end
													&& isDigit(chars[gData.cp]);
											if (result) {
												++gData.cp;
											}
											break label378;
										case 15 :
											result = gData.cp != end
													&& !isDigit(chars[gData.cp]);
											if (result) {
												++gData.cp;
											}
											break label378;
										case 16 :
											result = gData.cp != end
													&& isWord(chars[gData.cp]);
											if (result) {
												++gData.cp;
											}
											break label378;
										case 17 :
											result = gData.cp != end
													&& !isWord(chars[gData.cp]);
											if (result) {
												++gData.cp;
											}
											break label378;
										case 18 :
											result = gData.cp != end
													&& isREWhiteSpace(chars[gData.cp]);
											if (result) {
												++gData.cp;
											}
											break label378;
										case 19 :
											result = gData.cp != end
													&& !isREWhiteSpace(chars[gData.cp]);
											if (result) {
												++gData.cp;
											}
											break label378;
										case 20 :
											arg15 = getIndex(program, arg14);
											arg14 += 2;
											result = backrefMatcher(gData,
													arg15, chars, end);
											break label378;
										case 21 :
											arg15 = getIndex(program, arg14);
											arg14 += 2;
											new_min = getIndex(program, arg14);
											arg14 += 2;
											result = flatNMatcher(gData, arg15,
													new_min, chars, end);
											break label378;
										case 22 :
											arg17 = (char) (program[arg14++] & 255);
											result = gData.cp != end
													&& chars[gData.cp] == arg17;
											if (result) {
												++gData.cp;
											}
											break label378;
										case 23 :
											REProgState arg18 = popProgState(gData);
											currentContinuation_pc = arg18.continuation_pc;
											currentContinuation_op = arg18.continuation_op;
											arg15 = getOffset(program, arg14);
											arg14 += arg15;
											op = program[arg14++];
											break;
										case 28 :
											arg17 = (char) getIndex(program,
													arg14);
											arg14 += 2;
											result = gData.cp != end
													&& chars[gData.cp] == arg17;
											if (result) {
												++gData.cp;
											}
											break label378;
										case 32 :
											arg15 = getIndex(program, arg14);
											arg14 += 2;
											new_min = getIndex(program, arg14);
											arg14 += 2;
											result = flatNIMatcher(gData,
													arg15, new_min, chars, end);
											break label378;
										case 33 :
											arg17 = (char) (program[arg14++] & 255);
											result = gData.cp != end
													&& upcase(chars[gData.cp]) == upcase(arg17);
											if (result) {
												++gData.cp;
											}
											break label378;
										case 35 :
											arg17 = (char) getIndex(program,
													arg14);
											arg14 += 2;
											result = gData.cp != end
													&& upcase(chars[gData.cp]) == upcase(arg17);
											if (result) {
												++gData.cp;
											}
											break label378;
										case 41 :
										case 42 :
											pushProgState(gData, 0, 0,
													gData.backTrackStackTop,
													currentContinuation_pc,
													currentContinuation_op);
											byte arg16;
											if (op == 41) {
												arg16 = 43;
											} else {
												arg16 = 44;
											}

											pushBackTrackState(
													gData,
													arg16,
													arg14
															+ getOffset(
																	program,
																	arg14));
											arg14 += 2;
											op = program[arg14++];
											break;
										case 43 :
										case 44 :
											backTrackData = popProgState(gData);
											gData.cp = backTrackData.index;
											gData.backTrackStackTop = backTrackData.backTrack;
											currentContinuation_pc = backTrackData.continuation_pc;
											currentContinuation_op = backTrackData.continuation_op;
											if (result) {
												if (op == 43) {
													result = true;
												} else {
													result = false;
												}
											} else if (op != 43) {
												result = true;
											}
											break label378;
										case 49 :
											arg14 = currentContinuation_pc;
											op = currentContinuation_op;
											break;
										case 50 :
											arg15 = getIndex(program, arg14);
											arg14 += 2;
											if (gData.cp != end
													&& classMatcher(
															gData,
															gData.regexp.classList[arg15],
															chars[gData.cp])) {
												++gData.cp;
												result = true;
											} else {
												result = false;
											}
											break label378;
										case 51 :
											backTrackData = popProgState(gData);
											if (!result) {
												if (backTrackData.min == 0) {
													result = true;
												}

												currentContinuation_pc = backTrackData.continuation_pc;
												currentContinuation_op = backTrackData.continuation_op;
												arg14 += 4;
												arg14 += getOffset(program,
														arg14);
											} else if (backTrackData.min == 0
													&& gData.cp == backTrackData.index) {
												result = false;
												currentContinuation_pc = backTrackData.continuation_pc;
												currentContinuation_op = backTrackData.continuation_op;
												arg14 += 4;
												arg14 += getOffset(program,
														arg14);
											} else {
												new_min = backTrackData.min;
												new_max = backTrackData.max;
												if (new_min != 0) {
													--new_min;
												}

												if (new_max != -1) {
													--new_max;
												}

												if (new_max != 0) {
													pushProgState(
															gData,
															new_min,
															new_max,
															(REBackTrackData) null,
															backTrackData.continuation_pc,
															backTrackData.continuation_op);
													currentContinuation_op = 51;
													currentContinuation_pc = arg14;
													pushBackTrackState(gData,
															51, arg14);
													parenCount = getIndex(
															program, arg14);
													arg14 += 2;
													parenIndex = getIndex(
															program, arg14);
													arg14 += 4;
													op = program[arg14++];
													k = 0;

													while (true) {
														if (k >= parenCount) {
															continue label378;
														}

														gData.set_parens(
																parenIndex + k,
																-1, 0);
														++k;
													}
												} else {
													result = true;
													currentContinuation_pc = backTrackData.continuation_pc;
													currentContinuation_op = backTrackData.continuation_op;
													arg14 += 4;
													arg14 += getOffset(program,
															arg14);
												}
											}
											break label378;
										case 52 :
											backTrackData = popProgState(gData);
											if (!result) {
												if (backTrackData.max == -1
														|| backTrackData.max > 0) {
													pushProgState(
															gData,
															backTrackData.min,
															backTrackData.max,
															(REBackTrackData) null,
															backTrackData.continuation_pc,
															backTrackData.continuation_op);
													currentContinuation_op = 52;
													currentContinuation_pc = arg14;
													new_min = getIndex(program,
															arg14);
													arg14 += 2;
													new_max = getIndex(program,
															arg14);
													arg14 += 4;

													for (parenCount = 0; parenCount < new_min; ++parenCount) {
														gData.set_parens(
																new_max
																		+ parenCount,
																-1, 0);
													}

													op = program[arg14++];
													break;
												}

												currentContinuation_pc = backTrackData.continuation_pc;
												currentContinuation_op = backTrackData.continuation_op;
												break label378;
											} else {
												if (backTrackData.min == 0
														&& gData.cp == backTrackData.index) {
													result = false;
													currentContinuation_pc = backTrackData.continuation_pc;
													currentContinuation_op = backTrackData.continuation_op;
													break label378;
												}

												new_min = backTrackData.min;
												new_max = backTrackData.max;
												if (new_min != 0) {
													--new_min;
												}

												if (new_max != -1) {
													--new_max;
												}

												pushProgState(
														gData,
														new_min,
														new_max,
														(REBackTrackData) null,
														backTrackData.continuation_pc,
														backTrackData.continuation_op);
												if (new_min == 0) {
													currentContinuation_pc = backTrackData.continuation_pc;
													currentContinuation_op = backTrackData.continuation_op;
													pushBackTrackState(gData,
															52, arg14);
													popProgState(gData);
													arg14 += 4;
													arg14 += getOffset(program,
															arg14);
													op = program[arg14++];
													break;
												}

												currentContinuation_op = 52;
												currentContinuation_pc = arg14;
												parenCount = getIndex(program,
														arg14);
												arg14 += 2;
												parenIndex = getIndex(program,
														arg14);
												arg14 += 4;

												for (k = 0; k < parenCount; ++k) {
													gData.set_parens(parenIndex
															+ k, -1, 0);
												}

												op = program[arg14++];
												break;
											}
										case 53 :
											return true;
									}
								}

								if (!result) {
									REBackTrackData arg21 = gData.backTrackStackTop;
									if (arg21 == null) {
										return false;
									}

									gData.backTrackStackTop = arg21.previous;
									gData.lastParen = arg21.lastParen;
									if (arg21.parens != null) {
										gData.parens = (long[]) arg21.parens
												.clone();
									}

									gData.cp = arg21.cp;
									gData.stateStackTop = arg21.stateStackTop;
									currentContinuation_op = gData.stateStackTop.continuation_op;
									currentContinuation_pc = gData.stateStackTop.continuation_pc;
									arg14 = arg21.continuation_pc;
									op = arg21.continuation_op;
								} else {
									op = program[arg14++];
								}
							}
						}
					}
				}
			}
		}
	}

	private static boolean matchRegExp(REGlobalData gData, RECompiled re,
			char[] chars, int start, int end, boolean multiline) {
		if (re.parenCount != 0) {
			gData.parens = new long[re.parenCount];
		} else {
			gData.parens = null;
		}

		gData.backTrackStackTop = null;
		gData.stateStackTop = null;
		gData.multiline = multiline;
		gData.regexp = re;
		gData.lastParen = 0;
		int anchorCh = gData.regexp.anchorCh;

		for (int i = start; i <= end; ++i) {
			if (anchorCh >= 0) {
				while (true) {
					if (i == end) {
						return false;
					}

					char result = chars[i];
					if (result == anchorCh || (gData.regexp.flags & 2) != 0
							&& upcase(result) == upcase((char) anchorCh)) {
						break;
					}

					++i;
				}
			}

			gData.cp = i;

			for (int arg8 = 0; arg8 < re.parenCount; ++arg8) {
				gData.set_parens(arg8, -1, 0);
			}

			boolean arg9 = executeREBytecode(gData, chars, end);
			gData.backTrackStackTop = null;
			gData.stateStackTop = null;
			if (arg9) {
				gData.skipped = i - start;
				return true;
			}
		}

		return false;
	}

	Object executeRegExp(Context cx, Scriptable scopeObj, RegExpImpl res,
			String str, int[] indexp, int matchType) {
		REGlobalData gData = new REGlobalData();
		int start = indexp[0];
		char[] charArray = str.toCharArray();
		int end = charArray.length;
		if (start > end) {
			start = end;
		}

		boolean matches = matchRegExp(gData, this.re, charArray, start, end,
				res.multiline);
		if (!matches) {
			return matchType != 2 ? null : Undefined.instance;
		} else {
			int index = gData.cp;
			indexp[0] = index;
			int matchlen = index - (start + gData.skipped);
			int ep = index;
			index -= matchlen;
			Object result;
			Scriptable obj;
			if (matchType == 0) {
				result = Boolean.TRUE;
				obj = null;
			} else {
				Scriptable parsub = getTopLevelScope(scopeObj);
				result = ScriptRuntime.newObject(cx, parsub, "Array",
						(Object[]) null);
				obj = (Scriptable) result;
				String num = new String(charArray, index, matchlen);
				obj.put(0, obj, num);
			}

			if (this.re.parenCount == 0) {
				res.parens = null;
				res.lastParen = SubString.emptySubString;
			} else {
				SubString arg22 = null;
				res.parens = new SubString[this.re.parenCount];

				for (int arg23 = 0; arg23 < this.re.parenCount; ++arg23) {
					int cap_index = gData.parens_index(arg23);
					if (cap_index != -1) {
						int cap_length = gData.parens_length(arg23);
						arg22 = new SubString(charArray, cap_index, cap_length);
						res.parens[arg23] = arg22;
						if (matchType != 0) {
							String parstr = arg22.toString();
							obj.put(arg23 + 1, obj, parstr);
						}
					} else if (matchType != 0) {
						obj.put(arg23 + 1, obj, Undefined.instance);
					}
				}

				res.lastParen = arg22;
			}

			if (matchType != 0) {
				obj.put("index", obj, new Integer(start + gData.skipped));
				obj.put("input", obj, str);
			}

			if (res.lastMatch == null) {
				res.lastMatch = new SubString();
				res.leftContext = new SubString();
				res.rightContext = new SubString();
			}

			res.lastMatch.charArray = charArray;
			res.lastMatch.index = index;
			res.lastMatch.length = matchlen;
			res.leftContext.charArray = charArray;
			if (cx.getLanguageVersion() == 120) {
				res.leftContext.index = start;
				res.leftContext.length = gData.skipped;
			} else {
				res.leftContext.index = 0;
				res.leftContext.length = start + gData.skipped;
			}

			res.rightContext.charArray = charArray;
			res.rightContext.index = ep;
			res.rightContext.length = end - ep;
			return result;
		}
	}

	int getFlags() {
		return this.re.flags;
	}

	private static void reportWarning(Context cx, String messageId, String arg) {
		if (cx.hasFeature(11)) {
			String msg = ScriptRuntime.getMessage1(messageId, arg);
			Context.reportWarning(msg);
		}

	}

	private static void reportError(String messageId, String arg) {
		String msg = ScriptRuntime.getMessage1(messageId, arg);
		throw ScriptRuntime.constructError("SyntaxError", msg);
	}

	protected int getMaxInstanceId() {
		return 5;
	}

	protected int findInstanceIdInfo(String s) {
		byte id = 0;
		String attr = null;
		int s_length = s.length();
		char c;
		if (s_length == 6) {
			c = s.charAt(0);
			if (c == 103) {
				attr = "global";
				id = 3;
			} else if (c == 115) {
				attr = "source";
				id = 2;
			}
		} else if (s_length == 9) {
			c = s.charAt(0);
			if (c == 108) {
				attr = "lastIndex";
				id = 1;
			} else if (c == 109) {
				attr = "multiline";
				id = 5;
			}
		} else if (s_length == 10) {
			attr = "ignoreCase";
			id = 4;
		}

		if (attr != null && attr != s && !attr.equals(s)) {
			id = 0;
		}

		if (id == 0) {
			return super.findInstanceIdInfo(s);
		} else {
			byte attr1;
			switch (id) {
				case 1 :
					attr1 = 6;
					break;
				case 2 :
				case 3 :
				case 4 :
				case 5 :
					attr1 = 7;
					break;
				default :
					throw new IllegalStateException();
			}

			return instanceIdInfo(attr1, id);
		}
	}

	protected String getInstanceIdName(int id) {
		switch (id) {
			case 1 :
				return "lastIndex";
			case 2 :
				return "source";
			case 3 :
				return "global";
			case 4 :
				return "ignoreCase";
			case 5 :
				return "multiline";
			default :
				return super.getInstanceIdName(id);
		}
	}

	protected Object getInstanceIdValue(int id) {
		switch (id) {
			case 1 :
				return ScriptRuntime.wrapNumber(this.lastIndex);
			case 2 :
				return new String(this.re.source);
			case 3 :
				return ScriptRuntime.wrapBoolean((this.re.flags & 1) != 0);
			case 4 :
				return ScriptRuntime.wrapBoolean((this.re.flags & 2) != 0);
			case 5 :
				return ScriptRuntime.wrapBoolean((this.re.flags & 4) != 0);
			default :
				return super.getInstanceIdValue(id);
		}
	}

	protected void setInstanceIdValue(int id, Object value) {
		if (id == 1) {
			this.lastIndex = ScriptRuntime.toNumber(value);
		} else {
			super.setInstanceIdValue(id, value);
		}
	}

	protected void initPrototypeId(int id) {
		String s;
		byte arity;
		switch (id) {
			case 1 :
				arity = 1;
				s = "compile";
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
				arity = 1;
				s = "exec";
				break;
			case 5 :
				arity = 1;
				s = "test";
				break;
			case 6 :
				arity = 1;
				s = "prefix";
				break;
			default :
				throw new IllegalArgumentException(String.valueOf(id));
		}

		this.initPrototypeMethod(REGEXP_TAG, id, s, arity);
	}

	public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
			Scriptable thisObj, Object[] args) {
		if (!f.hasTag(REGEXP_TAG)) {
			return super.execIdCall(f, cx, scope, thisObj, args);
		} else {
			int id = f.methodId();
			switch (id) {
				case 1 :
					return realThis(thisObj, f).compile(cx, scope, args);
				case 2 :
				case 3 :
					return realThis(thisObj, f).toString();
				case 4 :
					return realThis(thisObj, f).execSub(cx, scope, args, 1);
				case 5 :
					Object x = realThis(thisObj, f).execSub(cx, scope, args, 0);
					return Boolean.TRUE.equals(x)
							? Boolean.TRUE
							: Boolean.FALSE;
				case 6 :
					return realThis(thisObj, f).execSub(cx, scope, args, 2);
				default :
					throw new IllegalArgumentException(String.valueOf(id));
			}
		}
	}

	private static NativeRegExp realThis(Scriptable thisObj, IdFunctionObject f) {
		if (!(thisObj instanceof NativeRegExp)) {
			throw incompatibleCallError(f);
		} else {
			return (NativeRegExp) thisObj;
		}
	}

	protected int findPrototypeId(String s) {
		byte id = 0;
		String X = null;
		char c;
		switch (s.length()) {
			case 4 :
				c = s.charAt(0);
				if (c == 101) {
					X = "exec";
					id = 4;
				} else if (c == 116) {
					X = "test";
					id = 5;
				}
			case 5 :
			default :
				break;
			case 6 :
				X = "prefix";
				id = 6;
				break;
			case 7 :
				X = "compile";
				id = 1;
				break;
			case 8 :
				c = s.charAt(3);
				if (c == 111) {
					X = "toSource";
					id = 3;
				} else if (c == 116) {
					X = "toString";
					id = 2;
				}
		}

		if (X != null && X != s && !X.equals(s)) {
			id = 0;
		}

		return id;
	}
}