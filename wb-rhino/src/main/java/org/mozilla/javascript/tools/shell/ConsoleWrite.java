/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.tools.shell;

import org.mozilla.javascript.tools.shell.ConsoleTextArea;

class ConsoleWrite implements Runnable {
	private ConsoleTextArea textArea;
	private String str;

	public ConsoleWrite(ConsoleTextArea textArea, String str) {
		this.textArea = textArea;
		this.str = str;
	}

	public void run() {
		this.textArea.write(this.str);
	}
}