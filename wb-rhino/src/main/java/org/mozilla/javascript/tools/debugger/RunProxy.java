/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.tools.debugger;

import org.mozilla.javascript.tools.debugger.MessageDialogWrapper;
import org.mozilla.javascript.tools.debugger.SwingGui;
import org.mozilla.javascript.tools.debugger.Dim.SourceInfo;
import org.mozilla.javascript.tools.debugger.Dim.StackFrame;

class RunProxy implements Runnable {
	static final int OPEN_FILE = 1;
	static final int LOAD_FILE = 2;
	static final int UPDATE_SOURCE_TEXT = 3;
	static final int ENTER_INTERRUPT = 4;
	private SwingGui debugGui;
	private int type;
	String fileName;
	String text;
	SourceInfo sourceInfo;
	StackFrame lastFrame;
	String threadTitle;
	String alertMessage;

	public RunProxy(SwingGui debugGui, int type) {
		this.debugGui = debugGui;
		this.type = type;
	}

	public void run() {
		switch (this.type) {
			case 1 :
				try {
					this.debugGui.dim.compileScript(this.fileName, this.text);
				} catch (RuntimeException arg2) {
					MessageDialogWrapper.showMessageDialog(this.debugGui,
							arg2.getMessage(), "Error Compiling "
									+ this.fileName, 0);
				}
				break;
			case 2 :
				try {
					this.debugGui.dim.evalScript(this.fileName, this.text);
				} catch (RuntimeException arg1) {
					MessageDialogWrapper.showMessageDialog(this.debugGui,
							arg1.getMessage(),
							"Run error for " + this.fileName, 0);
				}
				break;
			case 3 :
				String fileName = this.sourceInfo.url();
				if (!this.debugGui.updateFileWindow(this.sourceInfo)
						&& !fileName.equals("<stdin>")) {
					this.debugGui.createFileWindow(this.sourceInfo, -1);
				}
				break;
			case 4 :
				this.debugGui.enterInterruptImpl(this.lastFrame,
						this.threadTitle, this.alertMessage);
				break;
			default :
				throw new IllegalArgumentException(String.valueOf(this.type));
		}

	}
}