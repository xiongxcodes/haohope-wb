/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.regexp;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.regexp.RENode;

class CompilerState {
	Context cx;
	char[] cpbegin;
	int cpend;
	int cp;
	int flags;
	int parenCount;
	int parenNesting;
	int classCount;
	int progLength;
	RENode result;

	CompilerState(Context cx, char[] source, int length, int flags) {
		this.cx = cx;
		this.cpbegin = source;
		this.cp = 0;
		this.cpend = length;
		this.flags = flags;
		this.parenCount = 0;
		this.classCount = 0;
		this.progLength = 0;
	}
}