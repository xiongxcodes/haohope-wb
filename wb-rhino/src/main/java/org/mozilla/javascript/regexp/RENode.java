/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.regexp;

class RENode {
	byte op;
	RENode next;
	RENode kid;
	RENode kid2;
	int num;
	int parenIndex;
	int min;
	int max;
	int parenCount;
	boolean greedy;
	int startIndex;
	int kidlen;
	int bmsize;
	int index;
	char chr;
	int length;
	int flatIndex;

	RENode(int i) {
		this.op = (byte) i;
	}
}