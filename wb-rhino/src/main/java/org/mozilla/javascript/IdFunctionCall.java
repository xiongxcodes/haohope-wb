/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.Scriptable;

public interface IdFunctionCall {
	Object execIdCall(IdFunctionObject arg0, Context arg1, Scriptable arg2,
			Scriptable arg3, Object[] arg4);
}