/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.regexp;

import org.mozilla.javascript.regexp.REBackTrackData;

class REProgState {
	REProgState previous;
	int min;
	int max;
	int index;
	int continuation_op;
	int continuation_pc;
	REBackTrackData backTrack;

	REProgState(REProgState previous, int min, int max, int index,
			REBackTrackData backTrack, int continuation_pc, int continuation_op) {
		this.previous = previous;
		this.min = min;
		this.max = max;
		this.index = index;
		this.continuation_op = continuation_op;
		this.continuation_pc = continuation_pc;
		this.backTrack = backTrack;
	}
}