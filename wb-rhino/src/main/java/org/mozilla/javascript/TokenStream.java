/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import java.io.IOException;
import java.io.Reader;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.ObjToIntMap;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ScriptRuntime;

class TokenStream {
	private static final int EOF_CHAR = -1;
	private boolean dirtyLine;
	String regExpFlags;
	private String string = "";
	private double number;
	private char[] stringBuffer = new char[128];
	private int stringBufferTop;
	private ObjToIntMap allStrings = new ObjToIntMap(50);
	private final int[] ungetBuffer = new int[3];
	private int ungetCursor;
	private boolean hitEOF = false;
	private int lineStart = 0;
	private int lineno;
	private int lineEndChar = -1;
	private String sourceString;
	private Reader sourceReader;
	private char[] sourceBuffer;
	private int sourceEnd;
	private int sourceCursor;
	private boolean xmlIsAttribute;
	private boolean xmlIsTagContent;
	private int xmlOpenTagsCount;
	private Parser parser;

	TokenStream(Parser parser, Reader sourceReader, String sourceString,
			int lineno) {
		this.parser = parser;
		this.lineno = lineno;
		if (sourceReader != null) {
			if (sourceString != null) {
				Kit.codeBug();
			}

			this.sourceReader = sourceReader;
			this.sourceBuffer = new char[512];
			this.sourceEnd = 0;
		} else {
			if (sourceString == null) {
				Kit.codeBug();
			}

			this.sourceString = sourceString;
			this.sourceEnd = sourceString.length();
		}

		this.sourceCursor = 0;
	}

	String tokenToString(int token) {
		return "";
	}

	static boolean isKeyword(String s) {
		return 0 != stringToKeyword(s);
	}

	private static int stringToKeyword(String name) {
		short id;
		String X;
		boolean Id_break = true;
		boolean Id_case = true;
		boolean Id_continue = true;
		boolean Id_default = true;
		boolean Id_delete = true;
		boolean Id_do = true;
		boolean Id_else = true;
		boolean Id_export = true;
		boolean Id_false = true;
		boolean Id_for = true;
		boolean Id_function = true;
		boolean Id_if = true;
		boolean Id_in = true;
		boolean Id_let = true;
		boolean Id_new = true;
		boolean Id_null = true;
		boolean Id_return = true;
		boolean Id_switch = true;
		boolean Id_this = true;
		boolean Id_true = true;
		boolean Id_typeof = true;
		boolean Id_var = true;
		boolean Id_void = true;
		boolean Id_while = true;
		boolean Id_with = true;
		boolean Id_yield = true;
		boolean Id_abstract = true;
		boolean Id_boolean = true;
		boolean Id_byte = true;
		boolean Id_catch = true;
		boolean Id_char = true;
		boolean Id_class = true;
		boolean Id_const = true;
		boolean Id_debugger = true;
		boolean Id_double = true;
		boolean Id_enum = true;
		boolean Id_extends = true;
		boolean Id_final = true;
		boolean Id_finally = true;
		boolean Id_float = true;
		boolean Id_goto = true;
		boolean Id_implements = true;
		boolean Id_import = true;
		boolean Id_instanceof = true;
		boolean Id_int = true;
		boolean Id_interface = true;
		boolean Id_long = true;
		boolean Id_native = true;
		boolean Id_package = true;
		boolean Id_private = true;
		boolean Id_protected = true;
		boolean Id_public = true;
		boolean Id_short = true;
		boolean Id_static = true;
		boolean Id_super = true;
		boolean Id_synchronized = true;
		boolean Id_throw = true;
		boolean Id_throws = true;
		boolean Id_transient = true;
		boolean Id_try = true;
		boolean Id_volatile = true;
		id = 0;
		X = null;
		char c;
		label207 : switch (name.length()) {
			case 2 :
				c = name.charAt(1);
				if (c == 102) {
					if (name.charAt(0) == 105) {
						id = 111;
						return id == 0 ? 0 : id & 255;
					}
				} else if (c == 110) {
					if (name.charAt(0) == 105) {
						id = 52;
						return id == 0 ? 0 : id & 255;
					}
				} else if (c == 111 && name.charAt(0) == 100) {
					id = 117;
					return id == 0 ? 0 : id & 255;
				}
				break;
			case 3 :
				switch (name.charAt(0)) {
					case 'f' :
						if (name.charAt(2) == 114 && name.charAt(1) == 111) {
							id = 118;
							return id == 0 ? 0 : id & 255;
						}
					case 'g' :
					case 'h' :
					case 'j' :
					case 'k' :
					case 'm' :
					case 'o' :
					case 'p' :
					case 'q' :
					case 'r' :
					case 's' :
					case 'u' :
					default :
						break label207;
					case 'i' :
						if (name.charAt(2) == 116 && name.charAt(1) == 110) {
							id = 126;
							return id == 0 ? 0 : id & 255;
						}
						break label207;
					case 'l' :
						if (name.charAt(2) == 116 && name.charAt(1) == 101) {
							id = 152;
							return id == 0 ? 0 : id & 255;
						}
						break label207;
					case 'n' :
						if (name.charAt(2) == 119 && name.charAt(1) == 101) {
							id = 30;
							return id == 0 ? 0 : id & 255;
						}
						break label207;
					case 't' :
						if (name.charAt(2) == 121 && name.charAt(1) == 114) {
							id = 80;
							return id == 0 ? 0 : id & 255;
						}
						break label207;
					case 'v' :
						if (name.charAt(2) == 114 && name.charAt(1) == 97) {
							id = 121;
							return id == 0 ? 0 : id & 255;
						}
						break label207;
				}
			case 4 :
				switch (name.charAt(0)) {
					case 'b' :
						X = "byte";
						id = 126;
						break label207;
					case 'c' :
						c = name.charAt(3);
						if (c == 101) {
							if (name.charAt(2) == 115 && name.charAt(1) == 97) {
								id = 114;
								return id == 0 ? 0 : id & 255;
							}
						} else if (c == 114 && name.charAt(2) == 97
								&& name.charAt(1) == 104) {
							id = 126;
							return id == 0 ? 0 : id & 255;
						}
					case 'd' :
					case 'f' :
					case 'h' :
					case 'i' :
					case 'j' :
					case 'k' :
					case 'm' :
					case 'o' :
					case 'p' :
					case 'q' :
					case 'r' :
					case 's' :
					case 'u' :
					default :
						break label207;
					case 'e' :
						c = name.charAt(3);
						if (c == 101) {
							if (name.charAt(2) == 115 && name.charAt(1) == 108) {
								id = 112;
								return id == 0 ? 0 : id & 255;
							}
						} else if (c == 109 && name.charAt(2) == 117
								&& name.charAt(1) == 110) {
							id = 126;
							return id == 0 ? 0 : id & 255;
						}
						break label207;
					case 'g' :
						X = "goto";
						id = 126;
						break label207;
					case 'l' :
						X = "long";
						id = 126;
						break label207;
					case 'n' :
						X = "null";
						id = 42;
						break label207;
					case 't' :
						c = name.charAt(3);
						if (c == 101) {
							if (name.charAt(2) == 117 && name.charAt(1) == 114) {
								id = 45;
								return id == 0 ? 0 : id & 255;
							}
						} else if (c == 115 && name.charAt(2) == 105
								&& name.charAt(1) == 104) {
							id = 43;
							return id == 0 ? 0 : id & 255;
						}
						break label207;
					case 'v' :
						X = "void";
						id = 125;
						break label207;
					case 'w' :
						X = "with";
						id = 122;
						break label207;
				}
			case 5 :
				switch (name.charAt(2)) {
					case 'a' :
						X = "class";
						id = 126;
					case 'b' :
					case 'c' :
					case 'd' :
					case 'f' :
					case 'g' :
					case 'h' :
					case 'j' :
					case 'k' :
					case 'm' :
					case 'q' :
					case 's' :
					default :
						break label207;
					case 'e' :
						c = name.charAt(0);
						if (c == 98) {
							X = "break";
							id = 119;
						} else if (c == 121) {
							X = "yield";
							id = 72;
						}
						break label207;
					case 'i' :
						X = "while";
						id = 116;
						break label207;
					case 'l' :
						X = "false";
						id = 44;
						break label207;
					case 'n' :
						c = name.charAt(0);
						if (c == 99) {
							X = "const";
							id = 153;
						} else if (c == 102) {
							X = "final";
							id = 126;
						}
						break label207;
					case 'o' :
						c = name.charAt(0);
						if (c == 102) {
							X = "float";
							id = 126;
						} else if (c == 115) {
							X = "short";
							id = 126;
						}
						break label207;
					case 'p' :
						X = "super";
						id = 126;
						break label207;
					case 'r' :
						X = "throw";
						id = 50;
						break label207;
					case 't' :
						X = "catch";
						id = 123;
						break label207;
				}
			case 6 :
				switch (name.charAt(1)) {
					case 'a' :
						X = "native";
						id = 126;
					case 'b' :
					case 'c' :
					case 'd' :
					case 'f' :
					case 'g' :
					case 'i' :
					case 'j' :
					case 'k' :
					case 'l' :
					case 'n' :
					case 'p' :
					case 'q' :
					case 'r' :
					case 's' :
					case 'v' :
					default :
						break label207;
					case 'e' :
						c = name.charAt(0);
						if (c == 100) {
							X = "delete";
							id = 31;
						} else if (c == 114) {
							X = "return";
							id = 4;
						}
						break label207;
					case 'h' :
						X = "throws";
						id = 126;
						break label207;
					case 'm' :
						X = "import";
						id = 110;
						break label207;
					case 'o' :
						X = "double";
						id = 126;
						break label207;
					case 't' :
						X = "static";
						id = 126;
						break label207;
					case 'u' :
						X = "public";
						id = 126;
						break label207;
					case 'w' :
						X = "switch";
						id = 113;
						break label207;
					case 'x' :
						X = "export";
						id = 109;
						break label207;
					case 'y' :
						X = "typeof";
						id = 32;
						break label207;
				}
			case 7 :
				switch (name.charAt(1)) {
					case 'a' :
						X = "package";
						id = 126;
						break label207;
					case 'e' :
						X = "default";
						id = 115;
						break label207;
					case 'i' :
						X = "finally";
						id = 124;
						break label207;
					case 'o' :
						X = "boolean";
						id = 126;
						break label207;
					case 'r' :
						X = "private";
						id = 126;
						break label207;
					case 'x' :
						X = "extends";
						id = 126;
					default :
						break label207;
				}
			case 8 :
				switch (name.charAt(0)) {
					case 'a' :
						X = "abstract";
						id = 126;
						break label207;
					case 'c' :
						X = "continue";
						id = 120;
						break label207;
					case 'd' :
						X = "debugger";
						id = 159;
						break label207;
					case 'f' :
						X = "function";
						id = 108;
						break label207;
					case 'v' :
						X = "volatile";
						id = 126;
					default :
						break label207;
				}
			case 9 :
				c = name.charAt(0);
				if (c == 105) {
					X = "interface";
					id = 126;
				} else if (c == 112) {
					X = "protected";
					id = 126;
				} else if (c == 116) {
					X = "transient";
					id = 126;
				}
				break;
			case 10 :
				c = name.charAt(1);
				if (c == 109) {
					X = "implements";
					id = 126;
				} else if (c == 110) {
					X = "instanceof";
					id = 53;
				}
			case 11 :
			default :
				break;
			case 12 :
				X = "synchronized";
				id = 126;
		}

		if (X != null && X != name && !X.equals(name)) {
			id = 0;
		}

		return id == 0 ? 0 : id & 255;
	}

	final int getLineno() {
		return this.lineno;
	}

	final String getString() {
		return this.string;
	}

	final double getNumber() {
		return this.number;
	}

	final boolean eof() {
		return this.hitEOF;
	}

	final int getToken() throws IOException {
		label474 : while (true) {
			int c = this.getChar();
			if (c == -1) {
				return 0;
			}

			if (c == 10) {
				this.dirtyLine = false;
				return 1;
			}

			if (!isJSSpace(c)) {
				if (c != 45) {
					this.dirtyLine = true;
				}

				if (c == 64) {
					return 146;
				}

				boolean isUnicodeEscapeStart = false;
				boolean identifierStart;
				if (c == 92) {
					c = this.getChar();
					if (c == 117) {
						identifierStart = true;
						isUnicodeEscapeStart = true;
						this.stringBufferTop = 0;
					} else {
						identifierStart = false;
						this.ungetChar(c);
						c = 92;
					}
				} else {
					identifierStart = Character.isJavaIdentifierStart((char) c);
					if (identifierStart) {
						this.stringBufferTop = 0;
						this.addToString(c);
					}
				}

				int str;
				int escapeStart;
				boolean arg10;
				String arg12;
				if (identifierStart) {
					arg10 = isUnicodeEscapeStart;

					while (true) {
						while (true) {
							while (isUnicodeEscapeStart) {
								str = 0;

								for (escapeStart = 0; escapeStart != 4; ++escapeStart) {
									c = this.getChar();
									str = Kit.xDigitToInt(c, str);
									if (str < 0) {
										break;
									}
								}

								if (str < 0) {
									this.parser.addError("msg.invalid.escape");
									return -1;
								}

								this.addToString(str);
								isUnicodeEscapeStart = false;
							}

							c = this.getChar();
							if (c != 92) {
								if (c == -1
										|| !Character
												.isJavaIdentifierPart((char) c)) {
									this.ungetChar(c);
									arg12 = this.getStringFromBuffer();
									if (!arg10) {
										escapeStart = stringToKeyword(arg12);
										if (escapeStart != 0) {
											if ((escapeStart == 152 || escapeStart == 72)
													&& this.parser.compilerEnv
															.getLanguageVersion() < 170) {
												this.string = escapeStart == 152
														? "let"
														: "yield";
												escapeStart = 39;
											}

											if (escapeStart != 126) {
												return escapeStart;
											}

											if (!this.parser.compilerEnv
													.isReservedKeywordAsIdentifier()) {
												return escapeStart;
											}

											this.parser.addWarning(
													"msg.reserved.keyword",
													arg12);
										}
									}

									this.string = (String) this.allStrings
											.intern(arg12);
									return 39;
								}

								this.addToString(c);
							} else {
								c = this.getChar();
								if (c != 117) {
									this.parser
											.addError("msg.illegal.character");
									return -1;
								}

								isUnicodeEscapeStart = true;
								arg10 = true;
							}
						}
					}
				}

				if (isDigit(c) || c == 46 && isDigit(this.peekChar())) {
					this.stringBufferTop = 0;
					byte arg11 = 10;
					if (c == 48) {
						c = this.getChar();
						if (c != 120 && c != 88) {
							if (isDigit(c)) {
								arg11 = 8;
							} else {
								this.addToString(48);
							}
						} else {
							arg11 = 16;
							c = this.getChar();
						}
					}

					if (arg11 == 16) {
						while (0 <= Kit.xDigitToInt(c, 0)) {
							this.addToString(c);
							c = this.getChar();
						}
					} else {
						while (48 <= c && c <= 57) {
							if (arg11 == 8 && c >= 56) {
								this.parser.addWarning("msg.bad.octal.literal",
										c == 56 ? "8" : "9");
								arg11 = 10;
							}

							this.addToString(c);
							c = this.getChar();
						}
					}

					boolean arg14 = true;
					if (arg11 == 10 && (c == 46 || c == 101 || c == 69)) {
						arg14 = false;
						if (c == 46) {
							do {
								this.addToString(c);
								c = this.getChar();
							} while (isDigit(c));
						}

						if (c == 101 || c == 69) {
							this.addToString(c);
							c = this.getChar();
							if (c == 43 || c == 45) {
								this.addToString(c);
								c = this.getChar();
							}

							if (!isDigit(c)) {
								this.parser.addError("msg.missing.exponent");
								return -1;
							}

							do {
								this.addToString(c);
								c = this.getChar();
							} while (isDigit(c));
						}
					}

					this.ungetChar(c);
					String arg13 = this.getStringFromBuffer();
					double arg15;
					if (arg11 == 10 && !arg14) {
						try {
							arg15 = Double.valueOf(arg13).doubleValue();
						} catch (NumberFormatException arg9) {
							this.parser.addError("msg.caught.nfe");
							return -1;
						}
					} else {
						arg15 = ScriptRuntime.stringToNumber(arg13, 0, arg11);
					}

					this.number = arg15;
					return 40;
				}

				if (c != 34 && c != 39) {
					switch (c) {
						case 33 :
							if (this.matchChar(61)) {
								if (this.matchChar(61)) {
									return 47;
								}

								return 13;
							}

							return 26;
						case 34 :
						case 35 :
						case 36 :
						case 39 :
						case 48 :
						case 49 :
						case 50 :
						case 51 :
						case 52 :
						case 53 :
						case 54 :
						case 55 :
						case 56 :
						case 57 :
						case 64 :
						case 65 :
						case 66 :
						case 67 :
						case 68 :
						case 69 :
						case 70 :
						case 71 :
						case 72 :
						case 73 :
						case 74 :
						case 75 :
						case 76 :
						case 77 :
						case 78 :
						case 79 :
						case 80 :
						case 81 :
						case 82 :
						case 83 :
						case 84 :
						case 85 :
						case 86 :
						case 87 :
						case 88 :
						case 89 :
						case 90 :
						case 92 :
						case 95 :
						case 96 :
						case 97 :
						case 98 :
						case 99 :
						case 100 :
						case 101 :
						case 102 :
						case 103 :
						case 104 :
						case 105 :
						case 106 :
						case 107 :
						case 108 :
						case 109 :
						case 110 :
						case 111 :
						case 112 :
						case 113 :
						case 114 :
						case 115 :
						case 116 :
						case 117 :
						case 118 :
						case 119 :
						case 120 :
						case 121 :
						case 122 :
						default :
							this.parser.addError("msg.illegal.character");
							return -1;
						case 37 :
							if (this.matchChar(61)) {
								return 100;
							}

							return 25;
						case 38 :
							if (this.matchChar(38)) {
								return 104;
							}

							if (this.matchChar(61)) {
								return 92;
							}

							return 11;
						case 40 :
							return 86;
						case 41 :
							return 87;
						case 42 :
							if (this.matchChar(61)) {
								return 98;
							}

							return 23;
						case 43 :
							if (this.matchChar(61)) {
								return 96;
							}

							if (this.matchChar(43)) {
								return 105;
							}

							return 21;
						case 44 :
							return 88;
						case 45 :
							byte arg16;
							if (this.matchChar(61)) {
								arg16 = 97;
							} else if (this.matchChar(45)) {
								if (!this.dirtyLine && this.matchChar(62)) {
									this.skipLine();
									continue;
								}

								arg16 = 106;
							} else {
								arg16 = 22;
							}

							this.dirtyLine = true;
							return arg16;
						case 46 :
							if (this.matchChar(46)) {
								return 142;
							}

							if (this.matchChar(40)) {
								return 145;
							}

							return 107;
						case 47 :
							if (this.matchChar(47)) {
								this.skipLine();
								continue;
							}

							if (this.matchChar(42)) {
								arg10 = false;

								while (true) {
									c = this.getChar();
									if (c == -1) {
										this.parser
												.addError("msg.unterminated.comment");
										return -1;
									}

									if (c == 42) {
										arg10 = true;
									} else if (c == 47) {
										if (arg10) {
											continue label474;
										}
									} else {
										arg10 = false;
									}
								}
							}

							if (this.matchChar(61)) {
								return 99;
							}

							return 24;
						case 58 :
							if (this.matchChar(58)) {
								return 143;
							}

							return 102;
						case 59 :
							return 81;
						case 60 :
							if (this.matchChar(33)) {
								if (this.matchChar(45)) {
									if (this.matchChar(45)) {
										this.skipLine();
										continue;
									}

									this.ungetCharIgnoreLineEnd(45);
								}

								this.ungetCharIgnoreLineEnd(33);
							}

							if (this.matchChar(60)) {
								if (this.matchChar(61)) {
									return 93;
								}

								return 18;
							}

							if (this.matchChar(61)) {
								return 15;
							}

							return 14;
						case 61 :
							if (this.matchChar(61)) {
								if (this.matchChar(61)) {
									return 46;
								}

								return 12;
							}

							return 89;
						case 62 :
							if (this.matchChar(62)) {
								if (this.matchChar(62)) {
									if (this.matchChar(61)) {
										return 95;
									}

									return 20;
								}

								if (this.matchChar(61)) {
									return 94;
								}

								return 19;
							}

							if (this.matchChar(61)) {
								return 17;
							}

							return 16;
						case 63 :
							return 101;
						case 91 :
							return 82;
						case 93 :
							return 83;
						case 94 :
							if (this.matchChar(61)) {
								return 91;
							}

							return 10;
						case 123 :
							return 84;
						case 124 :
							if (this.matchChar(124)) {
								return 103;
							}

							if (this.matchChar(61)) {
								return 90;
							}

							return 9;
						case 125 :
							return 85;
						case 126 :
							return 27;
					}
				}

				int lookForSlash = c;
				this.stringBufferTop = 0;
				c = this.getChar();

				label407 : while (c != lookForSlash) {
					if (c == 10 || c == -1) {
						this.ungetChar(c);
						this.parser.addError("msg.unterminated.string.lit");
						return -1;
					}

					if (c == 92) {
						c = this.getChar();
						int val;
						switch (c) {
							case 10 :
								c = this.getChar();
								continue;
							case 98 :
								c = 8;
								break;
							case 102 :
								c = 12;
								break;
							case 110 :
								c = 10;
								break;
							case 114 :
								c = 13;
								break;
							case 116 :
								c = 9;
								break;
							case 117 :
								escapeStart = this.stringBufferTop;
								this.addToString(117);
								str = 0;

								for (val = 0; val != 4; ++val) {
									c = this.getChar();
									str = Kit.xDigitToInt(c, str);
									if (str < 0) {
										continue label407;
									}

									this.addToString(c);
								}

								this.stringBufferTop = escapeStart;
								c = str;
								break;
							case 118 :
								c = 11;
								break;
							case 120 :
								c = this.getChar();
								str = Kit.xDigitToInt(c, 0);
								if (str < 0) {
									this.addToString(120);
									continue;
								}

								val = c;
								c = this.getChar();
								str = Kit.xDigitToInt(c, str);
								if (str < 0) {
									this.addToString(120);
									this.addToString(val);
									continue;
								}

								c = str;
								break;
							default :
								if (48 <= c && c < 56) {
									val = c - 48;
									c = this.getChar();
									if (48 <= c && c < 56) {
										val = 8 * val + c - 48;
										c = this.getChar();
										if (48 <= c && c < 56 && val <= 31) {
											val = 8 * val + c - 48;
											c = this.getChar();
										}
									}

									this.ungetChar(c);
									c = val;
								}
						}
					}

					this.addToString(c);
					c = this.getChar();
				}

				arg12 = this.getStringFromBuffer();
				this.string = (String) this.allStrings.intern(arg12);
				return 41;
			}
		}
	}

	private static boolean isAlpha(int c) {
		return c <= 90 ? 65 <= c : 97 <= c && c <= 122;
	}

	static boolean isDigit(int c) {
		return 48 <= c && c <= 57;
	}

	static boolean isJSSpace(int c) {
		return c <= 127 ? c == 32 || c == 9 || c == 12 || c == 11 : c == 160
				|| Character.getType((char) c) == 12;
	}

	private static boolean isJSFormatChar(int c) {
		return c > 127 && Character.getType((char) c) == 16;
	}

	void readRegExp(int startToken) throws IOException {
		this.stringBufferTop = 0;
		if (startToken == 99) {
			this.addToString(61);
		} else if (startToken != 24) {
			Kit.codeBug();
		}

		int c;
		for (boolean inCharSet = false; (c = this.getChar()) != 47 || inCharSet; this
				.addToString(c)) {
			if (c == 10 || c == -1) {
				this.ungetChar(c);
				throw this.parser.reportError("msg.unterminated.re.lit");
			}

			if (c == 92) {
				this.addToString(c);
				c = this.getChar();
			} else if (c == 91) {
				inCharSet = true;
			} else if (c == 93) {
				inCharSet = false;
			}
		}

		int reEnd = this.stringBufferTop;

		while (true) {
			while (!this.matchChar(103)) {
				if (this.matchChar(105)) {
					this.addToString(105);
				} else {
					if (!this.matchChar(109)) {
						if (isAlpha(this.peekChar())) {
							throw this.parser
									.reportError("msg.invalid.re.flag");
						}

						this.string = new String(this.stringBuffer, 0, reEnd);
						this.regExpFlags = new String(this.stringBuffer, reEnd,
								this.stringBufferTop - reEnd);
						return;
					}

					this.addToString(109);
				}
			}

			this.addToString(103);
		}
	}

	boolean isXMLAttribute() {
		return this.xmlIsAttribute;
	}

	int getFirstXMLToken() throws IOException {
		this.xmlOpenTagsCount = 0;
		this.xmlIsAttribute = false;
		this.xmlIsTagContent = false;
		this.ungetChar(60);
		return this.getNextXMLToken();
	}

	int getNextXMLToken() throws IOException {
		this.stringBufferTop = 0;

		for (int c = this.getChar(); c != -1; c = this.getChar()) {
			if (this.xmlIsTagContent) {
				switch (c) {
					case 9 :
					case 10 :
					case 13 :
					case 32 :
						this.addToString(c);
						break;
					case 34 :
					case 39 :
						this.addToString(c);
						if (!this.readQuotedString(c)) {
							return -1;
						}
						break;
					case 47 :
						this.addToString(c);
						if (this.peekChar() == 62) {
							c = this.getChar();
							this.addToString(c);
							this.xmlIsTagContent = false;
							--this.xmlOpenTagsCount;
						}
						break;
					case 61 :
						this.addToString(c);
						this.xmlIsAttribute = true;
						break;
					case 62 :
						this.addToString(c);
						this.xmlIsTagContent = false;
						this.xmlIsAttribute = false;
						break;
					case 123 :
						this.ungetChar(c);
						this.string = this.getStringFromBuffer();
						return 144;
					default :
						this.addToString(c);
						this.xmlIsAttribute = false;
				}

				if (!this.xmlIsTagContent && this.xmlOpenTagsCount == 0) {
					this.string = this.getStringFromBuffer();
					return 147;
				}
			} else {
				switch (c) {
					case 60 :
						this.addToString(c);
						c = this.peekChar();
						switch (c) {
							case 33 :
								c = this.getChar();
								this.addToString(c);
								c = this.peekChar();
								switch (c) {
									case 45 :
										c = this.getChar();
										this.addToString(c);
										c = this.getChar();
										if (c != 45) {
											this.stringBufferTop = 0;
											this.string = null;
											this.parser
													.addError("msg.XML.bad.form");
											return -1;
										}

										this.addToString(c);
										if (!this.readXmlComment()) {
											return -1;
										}
										continue;
									case 91 :
										c = this.getChar();
										this.addToString(c);
										if (this.getChar() != 67
												|| this.getChar() != 68
												|| this.getChar() != 65
												|| this.getChar() != 84
												|| this.getChar() != 65
												|| this.getChar() != 91) {
											this.stringBufferTop = 0;
											this.string = null;
											this.parser
													.addError("msg.XML.bad.form");
											return -1;
										}

										this.addToString(67);
										this.addToString(68);
										this.addToString(65);
										this.addToString(84);
										this.addToString(65);
										this.addToString(91);
										if (!this.readCDATA()) {
											return -1;
										}
										continue;
									default :
										if (!this.readEntity()) {
											return -1;
										}
										continue;
								}
							case 47 :
								c = this.getChar();
								this.addToString(c);
								if (this.xmlOpenTagsCount == 0) {
									this.stringBufferTop = 0;
									this.string = null;
									this.parser.addError("msg.XML.bad.form");
									return -1;
								}

								this.xmlIsTagContent = true;
								--this.xmlOpenTagsCount;
								continue;
							case 63 :
								c = this.getChar();
								this.addToString(c);
								if (!this.readPI()) {
									return -1;
								}
								continue;
							default :
								this.xmlIsTagContent = true;
								++this.xmlOpenTagsCount;
								continue;
						}
					case 123 :
						this.ungetChar(c);
						this.string = this.getStringFromBuffer();
						return 144;
					default :
						this.addToString(c);
				}
			}
		}

		this.stringBufferTop = 0;
		this.string = null;
		this.parser.addError("msg.XML.bad.form");
		return -1;
	}

	private boolean readQuotedString(int quote) throws IOException {
		for (int c = this.getChar(); c != -1; c = this.getChar()) {
			this.addToString(c);
			if (c == quote) {
				return true;
			}
		}

		this.stringBufferTop = 0;
		this.string = null;
		this.parser.addError("msg.XML.bad.form");
		return false;
	}

	private boolean readXmlComment() throws IOException {
		int c = this.getChar();

		label21 : do {
			while (c != -1) {
				this.addToString(c);
				if (c == 45 && this.peekChar() == 45) {
					c = this.getChar();
					this.addToString(c);
					continue label21;
				}

				c = this.getChar();
			}

			this.stringBufferTop = 0;
			this.string = null;
			this.parser.addError("msg.XML.bad.form");
			return false;
		} while (this.peekChar() != 62);

		c = this.getChar();
		this.addToString(c);
		return true;
	}

	private boolean readCDATA() throws IOException {
		int c = this.getChar();

		label21 : do {
			while (c != -1) {
				this.addToString(c);
				if (c == 93 && this.peekChar() == 93) {
					c = this.getChar();
					this.addToString(c);
					continue label21;
				}

				c = this.getChar();
			}

			this.stringBufferTop = 0;
			this.string = null;
			this.parser.addError("msg.XML.bad.form");
			return false;
		} while (this.peekChar() != 62);

		c = this.getChar();
		this.addToString(c);
		return true;
	}

	private boolean readEntity() throws IOException {
		int declTags = 1;

		for (int c = this.getChar(); c != -1; c = this.getChar()) {
			this.addToString(c);
			switch (c) {
				case 60 :
					++declTags;
					break;
				case 62 :
					--declTags;
					if (declTags == 0) {
						return true;
					}
			}
		}

		this.stringBufferTop = 0;
		this.string = null;
		this.parser.addError("msg.XML.bad.form");
		return false;
	}

	private boolean readPI() throws IOException {
		for (int c = this.getChar(); c != -1; c = this.getChar()) {
			this.addToString(c);
			if (c == 63 && this.peekChar() == 62) {
				c = this.getChar();
				this.addToString(c);
				return true;
			}
		}

		this.stringBufferTop = 0;
		this.string = null;
		this.parser.addError("msg.XML.bad.form");
		return false;
	}

	private String getStringFromBuffer() {
		return new String(this.stringBuffer, 0, this.stringBufferTop);
	}

	private void addToString(int c) {
		int N = this.stringBufferTop;
		if (N == this.stringBuffer.length) {
			char[] tmp = new char[this.stringBuffer.length * 2];
			System.arraycopy(this.stringBuffer, 0, tmp, 0, N);
			this.stringBuffer = tmp;
		}

		this.stringBuffer[N] = (char) c;
		this.stringBufferTop = N + 1;
	}

	private void ungetChar(int c) {
		if (this.ungetCursor != 0
				&& this.ungetBuffer[this.ungetCursor - 1] == 10) {
			Kit.codeBug();
		}

		this.ungetBuffer[this.ungetCursor++] = c;
	}

	private boolean matchChar(int test) throws IOException {
		int c = this.getCharIgnoreLineEnd();
		if (c == test) {
			return true;
		} else {
			this.ungetCharIgnoreLineEnd(c);
			return false;
		}
	}

	private int peekChar() throws IOException {
		int c = this.getChar();
		this.ungetChar(c);
		return c;
	}

	private int getChar() throws IOException {
		if (this.ungetCursor != 0) {
			return this.ungetBuffer[--this.ungetCursor];
		} else {
			char c;
			while (true) {
				while (true) {
					if (this.sourceString != null) {
						if (this.sourceCursor == this.sourceEnd) {
							this.hitEOF = true;
							return -1;
						}

						c = this.sourceString.charAt(this.sourceCursor++);
					} else {
						if (this.sourceCursor == this.sourceEnd
								&& !this.fillSourceBuffer()) {
							this.hitEOF = true;
							return -1;
						}

						c = this.sourceBuffer[this.sourceCursor++];
					}

					if (this.lineEndChar < 0) {
						break;
					}

					if (this.lineEndChar != 13 || c != 10) {
						this.lineEndChar = -1;
						this.lineStart = this.sourceCursor - 1;
						++this.lineno;
						break;
					}

					this.lineEndChar = 10;
				}

				if (c <= 127) {
					if (c == 10 || c == 13) {
						this.lineEndChar = c;
						c = 10;
					}
					break;
				}

				if (!isJSFormatChar(c)) {
					if (ScriptRuntime.isJSLineTerminator(c)) {
						this.lineEndChar = c;
						c = 10;
					}
					break;
				}
			}

			return c;
		}
	}

	private int getCharIgnoreLineEnd() throws IOException {
		if (this.ungetCursor != 0) {
			return this.ungetBuffer[--this.ungetCursor];
		} else {
			char c;
			while (true) {
				if (this.sourceString != null) {
					if (this.sourceCursor == this.sourceEnd) {
						this.hitEOF = true;
						return -1;
					}

					c = this.sourceString.charAt(this.sourceCursor++);
				} else {
					if (this.sourceCursor == this.sourceEnd
							&& !this.fillSourceBuffer()) {
						this.hitEOF = true;
						return -1;
					}

					c = this.sourceBuffer[this.sourceCursor++];
				}

				if (c <= 127) {
					if (c == 10 || c == 13) {
						this.lineEndChar = c;
						c = 10;
					}
					break;
				}

				if (!isJSFormatChar(c)) {
					if (ScriptRuntime.isJSLineTerminator(c)) {
						this.lineEndChar = c;
						c = 10;
					}
					break;
				}
			}

			return c;
		}
	}

	private void ungetCharIgnoreLineEnd(int c) {
		this.ungetBuffer[this.ungetCursor++] = c;
	}

	private void skipLine() throws IOException {
		int c;
		while ((c = this.getChar()) != -1 && c != 10) {
			;
		}

		this.ungetChar(c);
	}

	final int getOffset() {
		int n = this.sourceCursor - this.lineStart;
		if (this.lineEndChar >= 0) {
			--n;
		}

		return n;
	}

	final String getLine() {
		int lineLength;
		if (this.sourceString != null) {
			lineLength = this.sourceCursor;
			if (this.lineEndChar >= 0) {
				--lineLength;
			} else {
				while (lineLength != this.sourceEnd) {
					char arg4 = this.sourceString.charAt(lineLength);
					if (ScriptRuntime.isJSLineTerminator(arg4)) {
						break;
					}

					++lineLength;
				}
			}

			return this.sourceString.substring(this.lineStart, lineLength);
		} else {
			lineLength = this.sourceCursor - this.lineStart;
			if (this.lineEndChar >= 0) {
				--lineLength;
			} else {
				while (true) {
					int i = this.lineStart + lineLength;
					if (i == this.sourceEnd) {
						try {
							if (!this.fillSourceBuffer()) {
								break;
							}
						} catch (IOException arg3) {
							break;
						}

						i = this.lineStart + lineLength;
					}

					char c = this.sourceBuffer[i];
					if (ScriptRuntime.isJSLineTerminator(c)) {
						break;
					}

					++lineLength;
				}
			}

			return new String(this.sourceBuffer, this.lineStart, lineLength);
		}
	}

	private boolean fillSourceBuffer() throws IOException {
		if (this.sourceString != null) {
			Kit.codeBug();
		}

		if (this.sourceEnd == this.sourceBuffer.length) {
			if (this.lineStart != 0) {
				System.arraycopy(this.sourceBuffer, this.lineStart,
						this.sourceBuffer, 0, this.sourceEnd - this.lineStart);
				this.sourceEnd -= this.lineStart;
				this.sourceCursor -= this.lineStart;
				this.lineStart = 0;
			} else {
				char[] n = new char[this.sourceBuffer.length * 2];
				System.arraycopy(this.sourceBuffer, 0, n, 0, this.sourceEnd);
				this.sourceBuffer = n;
			}
		}

		int n1 = this.sourceReader.read(this.sourceBuffer, this.sourceEnd,
				this.sourceBuffer.length - this.sourceEnd);
		if (n1 < 0) {
			return false;
		} else {
			this.sourceEnd += n1;
			return true;
		}
	}
}