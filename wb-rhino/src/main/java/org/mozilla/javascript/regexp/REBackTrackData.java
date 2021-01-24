/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.regexp;

import org.mozilla.javascript.regexp.REGlobalData;
import org.mozilla.javascript.regexp.REProgState;

class REBackTrackData {
	REBackTrackData previous;
	int continuation_op;
	int continuation_pc;
	int lastParen;
	long[] parens;
	int cp;
	REProgState stateStackTop;

	REBackTrackData(REGlobalData gData, int op, int pc) {
		this.previous = gData.backTrackStackTop;
		this.continuation_op = op;
		this.continuation_pc = pc;
		this.lastParen = gData.lastParen;
		if (gData.parens != null) {
			this.parens = (long[]) gData.parens.clone();
		}

		this.cp = gData.cp;
		this.stateStackTop = gData.stateStackTop;
	}
}