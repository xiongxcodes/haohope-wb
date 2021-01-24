/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.regexp;

import org.mozilla.javascript.regexp.REBackTrackData;
import org.mozilla.javascript.regexp.RECompiled;
import org.mozilla.javascript.regexp.REProgState;

class REGlobalData {
	boolean multiline;
	RECompiled regexp;
	int lastParen;
	int skipped;
	int cp;
	long[] parens;
	REProgState stateStackTop;
	REBackTrackData backTrackStackTop;

	int parens_index(int i) {
		return (int) this.parens[i];
	}

	int parens_length(int i) {
		return (int) (this.parens[i] >>> 32);
	}

	void set_parens(int i, int index, int length) {
		this.parens[i] = (long) index & 4294967295L | (long) length << 32;
	}
}