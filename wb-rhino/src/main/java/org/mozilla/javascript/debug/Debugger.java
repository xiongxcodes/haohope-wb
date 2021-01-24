/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.debug;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.debug.DebugFrame;
import org.mozilla.javascript.debug.DebuggableScript;

public interface Debugger {
	void handleCompilationDone(Context arg0, DebuggableScript arg1, String arg2);

	DebugFrame getFrame(Context arg0, DebuggableScript arg1);
}