/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.tools.debugger.treetable;

import javax.swing.tree.TreeModel;

public interface TreeTableModel extends TreeModel {
	int getColumnCount();

	String getColumnName(int arg0);

	Class<?> getColumnClass(int arg0);

	Object getValueAt(Object arg0, int arg1);

	boolean isCellEditable(Object arg0, int arg1);

	void setValueAt(Object arg0, Object arg1, int arg2);
}