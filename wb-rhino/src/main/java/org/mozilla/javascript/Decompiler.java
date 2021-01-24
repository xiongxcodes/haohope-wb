/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import org.mozilla.javascript.Kit;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.UintMap;

public class Decompiler {
	public static final int ONLY_BODY_FLAG = 1;
	public static final int TO_SOURCE_FLAG = 2;
	public static final int INITIAL_INDENT_PROP = 1;
	public static final int INDENT_GAP_PROP = 2;
	public static final int CASE_GAP_PROP = 3;
	private static final int FUNCTION_END = 160;
	private char[] sourceBuffer = new char[128];
	private int sourceTop;
	private static final boolean printSource = false;

	String getEncodedSource() {
		return this.sourceToString(0);
	}

	int getCurrentOffset() {
		return this.sourceTop;
	}

	int markFunctionStart(int functionType) {
		int savedOffset = this.getCurrentOffset();
		this.addToken(108);
		this.append((char) functionType);
		return savedOffset;
	}

	int markFunctionEnd(int functionStart) {
		int offset = this.getCurrentOffset();
		this.append(' ');
		return offset;
	}

	void addToken(int token) {
		if (0 <= token && token <= 159) {
			this.append((char) token);
		} else {
			throw new IllegalArgumentException();
		}
	}

	void addEOL(int token) {
		if (0 <= token && token <= 159) {
			this.append((char) token);
			this.append('');
		} else {
			throw new IllegalArgumentException();
		}
	}

	void addName(String str) {
		this.addToken(39);
		this.appendString(str);
	}

	void addString(String str) {
		this.addToken(41);
		this.appendString(str);
	}

	void addRegexp(String regexp, String flags) {
		this.addToken(48);
		this.appendString('/' + regexp + '/' + flags);
	}

	void addNumber(double n) {
		this.addToken(40);
		long lbits = (long) n;
		if ((double) lbits != n) {
			lbits = Double.doubleToLongBits(n);
			this.append('D');
			this.append((char) ((int) (lbits >> 48)));
			this.append((char) ((int) (lbits >> 32)));
			this.append((char) ((int) (lbits >> 16)));
			this.append((char) ((int) lbits));
		} else {
			if (lbits < 0L) {
				Kit.codeBug();
			}

			if (lbits <= 65535L) {
				this.append('S');
				this.append((char) ((int) lbits));
			} else {
				this.append('J');
				this.append((char) ((int) (lbits >> 48)));
				this.append((char) ((int) (lbits >> 32)));
				this.append((char) ((int) (lbits >> 16)));
				this.append((char) ((int) lbits));
			}
		}

	}

	private void appendString(String str) {
		int L = str.length();
		byte lengthEncodingSize = 1;
		if (L >= '耀') {
			lengthEncodingSize = 2;
		}

		int nextTop = this.sourceTop + lengthEncodingSize + L;
		if (nextTop > this.sourceBuffer.length) {
			this.increaseSourceCapacity(nextTop);
		}

		if (L >= '耀') {
			this.sourceBuffer[this.sourceTop] = (char) ('耀' | L >>> 16);
			++this.sourceTop;
		}

		this.sourceBuffer[this.sourceTop] = (char) L;
		++this.sourceTop;
		str.getChars(0, L, this.sourceBuffer, this.sourceTop);
		this.sourceTop = nextTop;
	}

	private void append(char c) {
		if (this.sourceTop == this.sourceBuffer.length) {
			this.increaseSourceCapacity(this.sourceTop + 1);
		}

		this.sourceBuffer[this.sourceTop] = c;
		++this.sourceTop;
	}

	private void increaseSourceCapacity(int minimalCapacity) {
		if (minimalCapacity <= this.sourceBuffer.length) {
			Kit.codeBug();
		}

		int newCapacity = this.sourceBuffer.length * 2;
		if (newCapacity < minimalCapacity) {
			newCapacity = minimalCapacity;
		}

		char[] tmp = new char[newCapacity];
		System.arraycopy(this.sourceBuffer, 0, tmp, 0, this.sourceTop);
		this.sourceBuffer = tmp;
	}

	private String sourceToString(int offset) {
		if (offset < 0 || this.sourceTop < offset) {
			Kit.codeBug();
		}

		return new String(this.sourceBuffer, offset, this.sourceTop - offset);
	}

	public static String decompile(String source, int flags, UintMap properties) {
		int length = source.length();
		if (length == 0) {
			return "";
		} else {
			int indent = properties.getInt(1, 0);
			if (indent < 0) {
				throw new IllegalArgumentException();
			} else {
				int indentGap = properties.getInt(2, 4);
				if (indentGap < 0) {
					throw new IllegalArgumentException();
				} else {
					int caseGap = properties.getInt(3, 2);
					if (caseGap < 0) {
						throw new IllegalArgumentException();
					} else {
						StringBuffer result = new StringBuffer();
						boolean justFunctionBody = 0 != (flags & 1);
						boolean toSource = 0 != (flags & 2);
						int braceNesting = 0;
						boolean afterFirstEOL = false;
						int i = 0;
						int topFunctionType;
						if (source.charAt(i) == 135) {
							++i;
							topFunctionType = -1;
						} else {
							topFunctionType = source.charAt(i + 1);
						}

						if (!toSource) {
							result.append('\n');

							for (int newLine = 0; newLine < indent; ++newLine) {
								result.append(' ');
							}
						} else if (topFunctionType == 2) {
							result.append('(');
						}

						while (i < length) {
							switch (source.charAt(i)) {
								case '' :
									if (!toSource) {
										boolean arg17 = true;
										if (!afterFirstEOL) {
											afterFirstEOL = true;
											if (justFunctionBody) {
												result.setLength(0);
												indent -= indentGap;
												arg17 = false;
											}
										}

										if (arg17) {
											result.append('\n');
										}

										if (i + 1 < length) {
											int less = 0;
											char nextToken = source
													.charAt(i + 1);
											if (nextToken != 114
													&& nextToken != 115) {
												if (nextToken == 85) {
													less = indentGap;
												} else if (nextToken == 39) {
													int afterName = getSourceStringEnd(
															source, i + 2);
													if (source
															.charAt(afterName) == 102) {
														less = indentGap;
													}
												}
											} else {
												less = indentGap - caseGap;
											}

											while (less < indent) {
												result.append(' ');
												++less;
											}
										}
									}
									break;
								case '' :
								case '' :
								case '' :
								case '' :
								case '' :
								case '\b' :
								case '!' :
								case '\"' :
								case '#' :
								case '$' :
								case '%' :
								case '&' :
								case '1' :
								case '3' :
								case '6' :
								case '7' :
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
								case 'C' :
								case 'D' :
								case 'E' :
								case 'F' :
								case 'G' :
								case 'I' :
								case 'J' :
								case 'K' :
								case 'L' :
								case 'M' :
								case 'N' :
								case 'O' :
								case 'm' :
								case 'n' :
								case '~' :
								case '' :
								case '' :
								case '' :
								case '' :
								case '' :
								case '' :
								case '' :
								case '' :
								case '' :
								case '' :
								case '' :
								case '' :
								case '' :
								case '' :
								case '' :
								case '' :
								case '' :
								case '' :
								case '' :
								case '' :
								case '' :
								case '' :
								case '' :
								case '' :
								case '' :
								default :
									throw new RuntimeException("Token: "
											+ Token.name(source.charAt(i)));
								case '' :
									result.append("return");
									if (81 != getNext(source, length, i)) {
										result.append(' ');
									}
									break;
								case '\t' :
									result.append(" | ");
									break;
								case '\n' :
									result.append(" ^ ");
									break;
								case '' :
									result.append(" & ");
									break;
								case '\f' :
									result.append(" == ");
									break;
								case '\r' :
									result.append(" != ");
									break;
								case '' :
									result.append(" < ");
									break;
								case '' :
									result.append(" <= ");
									break;
								case '' :
									result.append(" > ");
									break;
								case '' :
									result.append(" >= ");
									break;
								case '' :
									result.append(" << ");
									break;
								case '' :
									result.append(" >> ");
									break;
								case '' :
									result.append(" >>> ");
									break;
								case '' :
									result.append(" + ");
									break;
								case '' :
									result.append(" - ");
									break;
								case '' :
									result.append(" * ");
									break;
								case '' :
									result.append(" / ");
									break;
								case '' :
									result.append(" % ");
									break;
								case '' :
									result.append('!');
									break;
								case '' :
									result.append('~');
									break;
								case '' :
									result.append('+');
									break;
								case '' :
									result.append('-');
									break;
								case '' :
									result.append("new ");
									break;
								case '' :
									result.append("delete ");
									break;
								case ' ' :
									result.append("typeof ");
									break;
								case '\'' :
								case '0' :
									i = printSourceString(source, i + 1, false,
											result);
									continue;
								case '(' :
									i = printSourceNumber(source, i + 1, result);
									continue;
								case ')' :
									i = printSourceString(source, i + 1, true,
											result);
									continue;
								case '*' :
									result.append("null");
									break;
								case '+' :
									result.append("this");
									break;
								case ',' :
									result.append("false");
									break;
								case '-' :
									result.append("true");
									break;
								case '.' :
									result.append(" === ");
									break;
								case '/' :
									result.append(" !== ");
									break;
								case '2' :
									result.append("throw ");
									break;
								case '4' :
									result.append(" in ");
									break;
								case '5' :
									result.append(" instanceof ");
									break;
								case 'B' :
									result.append(':');
									break;
								case 'H' :
									result.append("yield ");
									break;
								case 'P' :
									result.append("try ");
									break;
								case 'Q' :
									result.append(';');
									if (1 != getNext(source, length, i)) {
										result.append(' ');
									}
									break;
								case 'R' :
									result.append('[');
									break;
								case 'S' :
									result.append(']');
									break;
								case 'T' :
									++braceNesting;
									if (1 == getNext(source, length, i)) {
										indent += indentGap;
									}

									result.append('{');
									break;
								case 'U' :
									--braceNesting;
									if (!justFunctionBody || braceNesting != 0) {
										result.append('}');
										switch (getNext(source, length, i)) {
											case 1 :
											case 160 :
												indent -= indentGap;
												break;
											case 112 :
											case 116 :
												indent -= indentGap;
												result.append(' ');
										}
									}
									break;
								case 'V' :
									result.append('(');
									break;
								case 'W' :
									result.append(')');
									if (84 == getNext(source, length, i)) {
										result.append(' ');
									}
									break;
								case 'X' :
									result.append(", ");
									break;
								case 'Y' :
									result.append(" = ");
									break;
								case 'Z' :
									result.append(" |= ");
									break;
								case '[' :
									result.append(" ^= ");
									break;
								case '\\' :
									result.append(" &= ");
									break;
								case ']' :
									result.append(" <<= ");
									break;
								case '^' :
									result.append(" >>= ");
									break;
								case '_' :
									result.append(" >>>= ");
									break;
								case '`' :
									result.append(" += ");
									break;
								case 'a' :
									result.append(" -= ");
									break;
								case 'b' :
									result.append(" *= ");
									break;
								case 'c' :
									result.append(" /= ");
									break;
								case 'd' :
									result.append(" %= ");
									break;
								case 'e' :
									result.append(" ? ");
									break;
								case 'f' :
									if (1 == getNext(source, length, i)) {
										result.append(':');
									} else {
										result.append(" : ");
									}
									break;
								case 'g' :
									result.append(" || ");
									break;
								case 'h' :
									result.append(" && ");
									break;
								case 'i' :
									result.append("++");
									break;
								case 'j' :
									result.append("--");
									break;
								case 'k' :
									result.append('.');
									break;
								case 'l' :
									++i;
									result.append("function ");
									break;
								case 'o' :
									result.append("if ");
									break;
								case 'p' :
									result.append("else ");
									break;
								case 'q' :
									result.append("switch ");
									break;
								case 'r' :
									result.append("case ");
									break;
								case 's' :
									result.append("default");
									break;
								case 't' :
									result.append("while ");
									break;
								case 'u' :
									result.append("do ");
									break;
								case 'v' :
									result.append("for ");
									break;
								case 'w' :
									result.append("break");
									if (39 == getNext(source, length, i)) {
										result.append(' ');
									}
									break;
								case 'x' :
									result.append("continue");
									if (39 == getNext(source, length, i)) {
										result.append(' ');
									}
									break;
								case 'y' :
									result.append("var ");
									break;
								case 'z' :
									result.append("with ");
									break;
								case '{' :
									result.append("catch ");
									break;
								case '|' :
									result.append("finally ");
									break;
								case '}' :
									result.append("void ");
									break;
								case '' :
									result.append("..");
									break;
								case '' :
									result.append("::");
									break;
								case '' :
									result.append(".(");
									break;
								case '' :
									result.append('@');
									break;
								case '' :
								case '' :
									result.append(source.charAt(i) == 150
											? "get "
											: "set ");
									++i;
									i = printSourceString(source, i + 1, false,
											result);
									++i;
									break;
								case '' :
									result.append("let ");
									break;
								case '' :
									result.append("const ");
								case ' ' :
							}

							++i;
						}

						if (!toSource) {
							if (!justFunctionBody) {
								result.append('\n');
							}
						} else if (topFunctionType == 2) {
							result.append(')');
						}

						return result.toString();
					}
				}
			}
		}
	}

	private static int getNext(String source, int length, int i) {
		return i + 1 < length ? source.charAt(i + 1) : 0;
	}

	private static int getSourceStringEnd(String source, int offset) {
		return printSourceString(source, offset, false, (StringBuffer) null);
	}

	private static int printSourceString(String source, int offset,
			boolean asQuotedString, StringBuffer sb) {
		int length = source.charAt(offset);
		++offset;
		if (('耀' & length) != 0) {
			length = (32767 & length) << 16 | source.charAt(offset);
			++offset;
		}

		if (sb != null) {
			String str = source.substring(offset, offset + length);
			if (!asQuotedString) {
				sb.append(str);
			} else {
				sb.append('\"');
				sb.append(ScriptRuntime.escapeString(str));
				sb.append('\"');
			}
		}

		return offset + length;
	}

	private static int printSourceNumber(String source, int offset,
			StringBuffer sb) {
		double number = 0.0D;
		char type = source.charAt(offset);
		++offset;
		if (type == 83) {
			if (sb != null) {
				char lbits = source.charAt(offset);
				number = (double) lbits;
			}

			++offset;
		} else {
			if (type != 74 && type != 68) {
				throw new RuntimeException();
			}

			if (sb != null) {
				long arg7 = (long) source.charAt(offset) << 48;
				arg7 |= (long) source.charAt(offset + 1) << 32;
				arg7 |= (long) source.charAt(offset + 2) << 16;
				arg7 |= (long) source.charAt(offset + 3);
				if (type == 74) {
					number = (double) arg7;
				} else {
					number = Double.longBitsToDouble(arg7);
				}
			}

			offset += 4;
		}

		if (sb != null) {
			sb.append(ScriptRuntime.numberToString(number, 10));
		}

		return offset;
	}
}