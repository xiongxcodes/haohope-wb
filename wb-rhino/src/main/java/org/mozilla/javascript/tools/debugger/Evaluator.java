/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.tools.debugger;

import javax.swing.JTable;
import org.mozilla.javascript.tools.debugger.MyTableModel;
import org.mozilla.javascript.tools.debugger.SwingGui;

class Evaluator extends JTable {
	private static final long serialVersionUID = 8133672432982594256L;
	MyTableModel tableModel = (MyTableModel) this.getModel();

	public Evaluator(SwingGui debugGui) {
		super(new MyTableModel(debugGui));
	}
}