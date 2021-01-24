/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.tools.debugger;

import org.mozilla.javascript.tools.debugger.Dim.SourceInfo;
import org.mozilla.javascript.tools.debugger.Dim.StackFrame;

public interface GuiCallback {
	void updateSourceText(SourceInfo arg0);

	void enterInterrupt(StackFrame arg0, String arg1, String arg2);

	boolean isGuiEventThread();

	void dispatchNextGuiEvent() throws InterruptedException;
}