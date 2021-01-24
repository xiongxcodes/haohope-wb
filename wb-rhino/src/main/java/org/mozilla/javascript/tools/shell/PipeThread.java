/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.tools.shell;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.tools.shell.Global;

class PipeThread extends Thread {
	private boolean fromProcess;
	private InputStream from;
	private OutputStream to;

	PipeThread(boolean fromProcess, InputStream from, OutputStream to) {
		this.setDaemon(true);
		this.fromProcess = fromProcess;
		this.from = from;
		this.to = to;
	}

	public void run() {
		try {
			Global.pipe(this.fromProcess, this.from, this.to);
		} catch (IOException arg1) {
			throw Context.throwAsScriptRuntimeEx(arg1);
		}
	}
}