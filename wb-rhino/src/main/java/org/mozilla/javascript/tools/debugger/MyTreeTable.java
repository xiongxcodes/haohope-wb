/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.tools.debugger;

import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import org.mozilla.javascript.tools.debugger.VariableModel;
import org.mozilla.javascript.tools.debugger.treetable.JTreeTable;
import org.mozilla.javascript.tools.debugger.treetable.TreeTableModel;
import org.mozilla.javascript.tools.debugger.treetable.TreeTableModelAdapter;
import org.mozilla.javascript.tools.debugger.treetable.JTreeTable.ListToTreeSelectionModelWrapper;
import org.mozilla.javascript.tools.debugger.treetable.JTreeTable.TreeTableCellEditor;
import org.mozilla.javascript.tools.debugger.treetable.JTreeTable.TreeTableCellRenderer;

class MyTreeTable extends JTreeTable {
	private static final long serialVersionUID = 3457265548184453049L;

	public MyTreeTable(VariableModel model) {
		super(model);
	}

	public JTree resetTree(TreeTableModel treeTableModel) {
		this.tree = new TreeTableCellRenderer(treeTableModel);
		super.setModel(new TreeTableModelAdapter(treeTableModel, this.tree));
		ListToTreeSelectionModelWrapper selectionWrapper = new ListToTreeSelectionModelWrapper();
		this.tree.setSelectionModel(selectionWrapper);
		this.setSelectionModel(selectionWrapper.getListSelectionModel());
		if (this.tree.getRowHeight() < 1) {
			this.setRowHeight(18);
		}

		this.setDefaultRenderer(TreeTableModel.class, this.tree);
		this.setDefaultEditor(TreeTableModel.class, new TreeTableCellEditor());
		this.setShowGrid(true);
		this.setIntercellSpacing(new Dimension(1, 1));
		this.tree.setRootVisible(false);
		this.tree.setShowsRootHandles(true);
		DefaultTreeCellRenderer r = (DefaultTreeCellRenderer) this.tree
				.getCellRenderer();
		r.setOpenIcon((Icon) null);
		r.setClosedIcon((Icon) null);
		r.setLeafIcon((Icon) null);
		return this.tree;
	}

	public boolean isCellEditable(EventObject e) {
		if (!(e instanceof MouseEvent)) {
			return e == null;
		} else {
			MouseEvent me = (MouseEvent) e;
			if (me.getModifiers() == 0 || (me.getModifiers() & 1040) != 0
					&& (me.getModifiers() & 6863) == 0) {
				int row = this.rowAtPoint(me.getPoint());

				for (int counter = this.getColumnCount() - 1; counter >= 0; --counter) {
					if (TreeTableModel.class == this.getColumnClass(counter)) {
						MouseEvent newME = new MouseEvent(
								this.tree,
								me.getID(),
								me.getWhen(),
								me.getModifiers(),
								me.getX()
										- this.getCellRect(row, counter, true).x,
								me.getY(), me.getClickCount(), me
										.isPopupTrigger());
						this.tree.dispatchEvent(newME);
						break;
					}
				}
			}

			return me.getClickCount() >= 3;
		}
	}
}