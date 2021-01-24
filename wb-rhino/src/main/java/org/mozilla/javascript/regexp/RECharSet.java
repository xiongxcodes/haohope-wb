/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.regexp;

import java.io.Serializable;

final class RECharSet implements Serializable {
	static final long serialVersionUID = 7931787979395898394L;
	int length;
	int startIndex;
	int strlength;
	transient volatile boolean converted;
	transient volatile boolean sense;
	transient volatile byte[] bits;

	RECharSet(int length, int startIndex, int strlength) {
		this.length = length;
		this.startIndex = startIndex;
		this.strlength = strlength;
	}
}