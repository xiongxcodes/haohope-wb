/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.regexp;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.RegExpProxy;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.regexp.GlobData;
import org.mozilla.javascript.regexp.NativeRegExp;
import org.mozilla.javascript.regexp.SubString;

public class RegExpImpl implements RegExpProxy {
	String input;
	boolean multiline;
	SubString[] parens;
	SubString lastMatch;
	SubString lastParen;
	SubString leftContext;
	SubString rightContext;

	public boolean isRegExp(Scriptable obj) {
		return obj instanceof NativeRegExp;
	}

	public Object compileRegExp(Context cx, String source, String flags) {
		return NativeRegExp.compileRE(cx, source, flags, false);
	}

	public Scriptable wrapRegExp(Context cx, Scriptable scope, Object compiled) {
		return new NativeRegExp(scope, compiled);
	}

	public Object action(Context cx, Scriptable scope, Scriptable thisObj,
			Object[] args, int actionType) {
		GlobData data = new GlobData();
		data.mode = actionType;
		Object arg1;
		switch (actionType) {
			case 1 :
				data.optarg = 1;
				arg1 = matchOrReplace(cx, scope, thisObj, args, this, data,
						false);
				return data.arrayobj == null ? arg1 : data.arrayobj;
			case 2 :
				arg1 = args.length < 2 ? Undefined.instance : args[1];
				String repstr = null;
				Function lambda = null;
				if (arg1 instanceof Function) {
					lambda = (Function) arg1;
				} else {
					repstr = ScriptRuntime.toString(arg1);
				}

				data.optarg = 2;
				data.lambda = lambda;
				data.repstr = repstr;
				data.dollar = repstr == null ? -1 : repstr.indexOf(36);
				data.charBuf = null;
				data.leftIndex = 0;
				Object val = matchOrReplace(cx, scope, thisObj, args, this,
						data, true);
				SubString rc = this.rightContext;
				if (data.charBuf == null) {
					if (data.global || val == null || !val.equals(Boolean.TRUE)) {
						return data.str;
					}

					SubString lc = this.leftContext;
					replace_glob(data, cx, scope, this, lc.index, lc.length);
				}

				data.charBuf.append(rc.charArray, rc.index, rc.length);
				return data.charBuf.toString();
			case 3 :
				data.optarg = 1;
				return matchOrReplace(cx, scope, thisObj, args, this, data,
						false);
			default :
				throw Kit.codeBug();
		}
	}

	private static Object matchOrReplace(Context cx, Scriptable scope,
			Scriptable thisObj, Object[] args, RegExpImpl reImpl,
			GlobData data, boolean forceFlat) {
		String str = ScriptRuntime.toString(thisObj);
		data.str = str;
		Scriptable topScope = ScriptableObject.getTopLevelScope(scope);
		NativeRegExp re;
		if (args.length == 0) {
			Object indexp = NativeRegExp.compileRE(cx, "", "", false);
			re = new NativeRegExp(topScope, indexp);
		} else if (args[0] instanceof NativeRegExp) {
			re = (NativeRegExp) args[0];
		} else {
			String arg15 = ScriptRuntime.toString(args[0]);
			String result;
			if (data.optarg < args.length) {
				args[0] = arg15;
				result = ScriptRuntime.toString(args[data.optarg]);
			} else {
				result = null;
			}

			Object count = NativeRegExp.compileRE(cx, arg15, result, forceFlat);
			re = new NativeRegExp(topScope, count);
		}

		data.regexp = re;
		data.global = (re.getFlags() & 1) != 0;
		int[] arg16 = new int[]{0};
		Object arg17 = null;
		if (data.mode == 3) {
			arg17 = re.executeRegExp(cx, scope, reImpl, str, arg16, 0);
			if (arg17 != null && arg17.equals(Boolean.TRUE)) {
				arg17 = new Integer(reImpl.leftContext.length);
			} else {
				arg17 = new Integer(-1);
			}
		} else if (data.global) {
			re.lastIndex = 0.0D;

			for (int arg18 = 0; arg16[0] <= str.length(); ++arg18) {
				arg17 = re.executeRegExp(cx, scope, reImpl, str, arg16, 0);
				if (arg17 == null || !arg17.equals(Boolean.TRUE)) {
					break;
				}

				if (data.mode == 1) {
					match_glob(data, cx, scope, arg18, reImpl);
				} else {
					if (data.mode != 2) {
						Kit.codeBug();
					}

					SubString lastMatch = reImpl.lastMatch;
					int leftIndex = data.leftIndex;
					int leftlen = lastMatch.index - leftIndex;
					data.leftIndex = lastMatch.index + lastMatch.length;
					replace_glob(data, cx, scope, reImpl, leftIndex, leftlen);
				}

				if (reImpl.lastMatch.length == 0) {
					if (arg16[0] == str.length()) {
						break;
					}

					++arg16[0];
				}
			}
		} else {
			arg17 = re.executeRegExp(cx, scope, reImpl, str, arg16,
					data.mode == 2 ? 0 : 1);
		}

		return arg17;
	}

	public int find_split(Context cx, Scriptable scope, String target,
			String separator, Scriptable reObj, int[] ip, int[] matchlen,
			boolean[] matched, String[][] parensp) {
		int i = ip[0];
		int length = target.length();
		int version = cx.getLanguageVersion();
		NativeRegExp re = (NativeRegExp) reObj;

		int result;
		int size;
		SubString parsub;
		while (true) {
			size = ip[0];
			ip[0] = i;
			Object num = re.executeRegExp(cx, scope, this, target, ip, 0);
			if (num != Boolean.TRUE) {
				ip[0] = size;
				matchlen[0] = 1;
				matched[0] = false;
				return length;
			}

			i = ip[0];
			ip[0] = size;
			matched[0] = true;
			parsub = this.lastMatch;
			matchlen[0] = parsub.length;
			if (matchlen[0] != 0 || i != ip[0]) {
				result = i - matchlen[0];
				break;
			}

			if (i == length) {
				if (version == 120) {
					matchlen[0] = 1;
					result = i;
				} else {
					result = -1;
				}
				break;
			}

			++i;
		}

		size = this.parens == null ? 0 : this.parens.length;
		parensp[0] = new String[size];

		for (int arg17 = 0; arg17 < size; ++arg17) {
			parsub = this.getParenSubString(arg17);
			parensp[0][arg17] = parsub.toString();
		}

		return result;
	}

	SubString getParenSubString(int i) {
		if (this.parens != null && i < this.parens.length) {
			SubString parsub = this.parens[i];
			if (parsub != null) {
				return parsub;
			}
		}

		return SubString.emptySubString;
	}

	private static void match_glob(GlobData mdata, Context cx,
			Scriptable scope, int count, RegExpImpl reImpl) {
		if (mdata.arrayobj == null) {
			Scriptable matchsub = ScriptableObject.getTopLevelScope(scope);
			mdata.arrayobj = ScriptRuntime.newObject(cx, matchsub, "Array",
					(Object[]) null);
		}

		SubString matchsub1 = reImpl.lastMatch;
		String matchstr = matchsub1.toString();
		mdata.arrayobj.put(count, mdata.arrayobj, matchstr);
	}

	private static void replace_glob(GlobData rdata, Context cx,
			Scriptable scope, RegExpImpl reImpl, int leftIndex, int leftlen) {
		int replen;
		String lambdaStr;
		int charBuf;
		if (rdata.lambda != null) {
			SubString[] growth = reImpl.parens;
			charBuf = growth == null ? 0 : growth.length;
			Object[] sub = new Object[charBuf + 3];
			sub[0] = reImpl.lastMatch.toString();

			for (int re2 = 0; re2 < charBuf; ++re2) {
				SubString parent = growth[re2];
				if (parent != null) {
					sub[re2 + 1] = parent.toString();
				} else {
					sub[re2 + 1] = Undefined.instance;
				}
			}

			sub[charBuf + 1] = new Integer(reImpl.leftContext.length);
			sub[charBuf + 2] = rdata.str;
			if (reImpl != ScriptRuntime.getRegExpProxy(cx)) {
				Kit.codeBug();
			}

			RegExpImpl arg20 = new RegExpImpl();
			arg20.multiline = reImpl.multiline;
			arg20.input = reImpl.input;
			ScriptRuntime.setRegExpProxy(cx, arg20);

			try {
				Scriptable arg21 = ScriptableObject.getTopLevelScope(scope);
				Object result = rdata.lambda.call(cx, arg21, arg21, sub);
				lambdaStr = ScriptRuntime.toString(result);
			} finally {
				ScriptRuntime.setRegExpProxy(cx, reImpl);
			}

			replen = lambdaStr.length();
		} else {
			lambdaStr = null;
			replen = rdata.repstr.length();
			if (rdata.dollar >= 0) {
				int[] arg16 = new int[1];
				charBuf = rdata.dollar;

				do {
					SubString arg18 = interpretDollar(cx, reImpl, rdata.repstr,
							charBuf, arg16);
					if (arg18 != null) {
						replen += arg18.length - arg16[0];
						charBuf += arg16[0];
					} else {
						++charBuf;
					}

					charBuf = rdata.repstr.indexOf(36, charBuf);
				} while (charBuf >= 0);
			}
		}

		int arg17 = leftlen + replen + reImpl.rightContext.length;
		StringBuffer arg19 = rdata.charBuf;
		if (arg19 == null) {
			arg19 = new StringBuffer(arg17);
			rdata.charBuf = arg19;
		} else {
			arg19.ensureCapacity(rdata.charBuf.length() + arg17);
		}

		arg19.append(reImpl.leftContext.charArray, leftIndex, leftlen);
		if (rdata.lambda != null) {
			arg19.append(lambdaStr);
		} else {
			do_replace(rdata, cx, reImpl);
		}

	}

	private static SubString interpretDollar(Context cx, RegExpImpl res,
			String da, int dp, int[] skip) {
		if (da.charAt(dp) != 36) {
			Kit.codeBug();
		}

		int version = cx.getLanguageVersion();
		if (version != 0 && version <= 140 && dp > 0 && da.charAt(dp - 1) == 92) {
			return null;
		} else {
			int daL = da.length();
			if (dp + 1 >= daL) {
				return null;
			} else {
				char dc = da.charAt(dp + 1);
				if (!NativeRegExp.isDigit(dc)) {
					skip[0] = 2;
					switch (dc) {
						case '$' :
							return new SubString("$");
						case '&' :
							return res.lastMatch;
						case '\'' :
							return res.rightContext;
						case '+' :
							return res.lastParen;
						case '`' :
							if (version == 120) {
								res.leftContext.index = 0;
								res.leftContext.length = res.lastMatch.index;
							}

							return res.leftContext;
						default :
							return null;
					}
				} else {
					int num;
					int tmp;
					int cp;
					if (version != 0 && version <= 140) {
						if (dc == 48) {
							return null;
						}

						num = 0;
						cp = dp;

						while (true) {
							++cp;
							if (cp >= daL
									|| !NativeRegExp
											.isDigit(dc = da.charAt(cp))) {
								break;
							}

							tmp = 10 * num + (dc - 48);
							if (tmp < num) {
								break;
							}

							num = tmp;
						}
					} else {
						int parenCount = res.parens == null
								? 0
								: res.parens.length;
						num = dc - 48;
						if (num > parenCount) {
							return null;
						}

						cp = dp + 2;
						if (dp + 2 < daL) {
							dc = da.charAt(dp + 2);
							if (NativeRegExp.isDigit(dc)) {
								tmp = 10 * num + (dc - 48);
								if (tmp <= parenCount) {
									++cp;
									num = tmp;
								}
							}
						}

						if (num == 0) {
							return null;
						}
					}

					--num;
					skip[0] = cp - dp;
					return res.getParenSubString(num);
				}
			}
		}
	}

	private static void do_replace(GlobData rdata, Context cx,
			RegExpImpl regExpImpl) {
		StringBuffer charBuf = rdata.charBuf;
		int cp = 0;
		String da = rdata.repstr;
		int dp = rdata.dollar;
		if (dp != -1) {
			int[] daL = new int[1];

			do {
				int arg9999 = dp - cp;
				charBuf.append(da.substring(cp, dp));
				cp = dp;
				SubString sub = interpretDollar(cx, regExpImpl, da, dp, daL);
				if (sub != null) {
					int len = sub.length;
					if (len > 0) {
						charBuf.append(sub.charArray, sub.index, len);
					}

					cp = dp + daL[0];
					dp += daL[0];
				} else {
					++dp;
				}

				dp = da.indexOf(36, dp);
			} while (dp >= 0);
		}

		int arg9 = da.length();
		if (arg9 > cp) {
			charBuf.append(da.substring(cp, arg9));
		}

	}
}