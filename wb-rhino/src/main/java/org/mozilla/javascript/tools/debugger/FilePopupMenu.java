/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.tools.debugger;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.mozilla.javascript.tools.debugger.FileTextArea;

class FilePopupMenu extends JPopupMenu {
	private static final long serialVersionUID = 3589525009546013565L;
	int x;
	int y;

	public FilePopupMenu(FileTextArea w) {
		JMenuItem item;
		this.add(item = new JMenuItem("Set Breakpoint"));
		item.addActionListener(w);
		this.add(item = new JMenuItem("Clear Breakpoint"));
		item.addActionListener(w);
		this.add(item = new JMenuItem("Run"));
		item.addActionListener(w);
	}

	public void show(JComponent comp, int x, int y) {
		this.x = x;
		this.y = y;
		super.show(comp, x, y);
	}
}