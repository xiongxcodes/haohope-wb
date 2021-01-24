/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.regexp;

import java.io.Serializable;
import org.mozilla.javascript.regexp.RECharSet;

class RECompiled implements Serializable {
	static final long serialVersionUID = -6144956577595844213L;
	char[] source;
	int parenCount;
	int flags;
	byte[] program;
	int classCount;
	RECharSet[] classList;
	int anchorCh = -1;
}