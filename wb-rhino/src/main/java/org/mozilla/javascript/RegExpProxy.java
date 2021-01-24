/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public interface RegExpProxy {
	int RA_MATCH = 1;
	int RA_REPLACE = 2;
	int RA_SEARCH = 3;

	boolean isRegExp(Scriptable arg0);

	Object compileRegExp(Context arg0, String arg1, String arg2);

	Scriptable wrapRegExp(Context arg0, Scriptable arg1, Object arg2);

	Object action(Context arg0, Scriptable arg1, Scriptable arg2,
			Object[] arg3, int arg4);

	int find_split(Context arg0, Scriptable arg1, String arg2, String arg3,
			Scriptable arg4, int[] arg5, int[] arg6, boolean[] arg7,
			String[][] arg8);
}