/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.debug;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public interface DebugFrame {
	void onEnter(Context arg0, Scriptable arg1, Scriptable arg2, Object[] arg3);

	void onLineChange(Context arg0, int arg1);

	void onExceptionThrown(Context arg0, Throwable arg1);

	void onExit(Context arg0, boolean arg1, Object arg2);

	void onDebuggerStatement(Context arg0);
}