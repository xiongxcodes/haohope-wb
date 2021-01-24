/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.tools.debugger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import org.mozilla.javascript.tools.debugger.SwingGui;

class MyTableModel extends AbstractTableModel {
	private static final long serialVersionUID = 2971618907207577000L;
	private SwingGui debugGui;
	private List<String> expressions;
	private List<String> values;

	public MyTableModel(SwingGui debugGui) {
		this.debugGui = debugGui;
		this.expressions = Collections.synchronizedList(new ArrayList());
		this.values = Collections.synchronizedList(new ArrayList());
		this.expressions.add("");
		this.values.add("");
	}

	public int getColumnCount() {
		return 2;
	}

	public int getRowCount() {
		return this.expressions.size();
	}

	public String getColumnName(int column) {
		switch (column) {
			case 0 :
				return "Expression";
			case 1 :
				return "Value";
			default :
				return null;
		}
	}

	public boolean isCellEditable(int row, int column) {
		return true;
	}

	public Object getValueAt(int row, int column) {
		switch (column) {
			case 0 :
				return this.expressions.get(row);
			case 1 :
				return this.values.get(row);
			default :
				return "";
		}
	}

	public void setValueAt(Object value, int row, int column) {
		switch (column) {
			case 0 :
				String expr = value.toString();
				this.expressions.set(row, expr);
				String result = "";
				if (expr.length() > 0) {
					result = this.debugGui.dim.eval(expr);
					if (result == null) {
						result = "";
					}
				}

				this.values.set(row, result);
				this.updateModel();
				if (row + 1 == this.expressions.size()) {
					this.expressions.add("");
					this.values.add("");
					this.fireTableRowsInserted(row + 1, row + 1);
				}
				break;
			case 1 :
				this.fireTableDataChanged();
		}

	}

	void updateModel() {
		for (int i = 0; i < this.expressions.size(); ++i) {
			String expr = (String) this.expressions.get(i);
			String result = "";
			if (expr.length() > 0) {
				result = this.debugGui.dim.eval(expr);
				if (result == null) {
					result = "";
				}
			} else {
				result = "";
			}

			result = result.replace('\n', ' ');
			this.values.set(i, result);
		}

		this.fireTableDataChanged();
	}
}