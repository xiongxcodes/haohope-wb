/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public interface Callable {
	Object call(Context arg0, Scriptable arg1, Scriptable arg2, Object[] arg3);
}