/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.regexp;

import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.regexp.NativeRegExp;

final class GlobData {
	int mode;
	int optarg;
	boolean global;
	String str;
	NativeRegExp regexp;
	Scriptable arrayobj;
	Function lambda;
	String repstr;
	int dollar = -1;
	StringBuffer charBuf;
	int leftIndex;
}