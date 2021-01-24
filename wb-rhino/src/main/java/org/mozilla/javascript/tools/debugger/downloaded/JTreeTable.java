/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.tools.debugger.downloaded;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import org.mozilla.javascript.tools.debugger.downloaded.AbstractCellEditor;
import org.mozilla.javascript.tools.debugger.downloaded.TreeTableModel;
import org.mozilla.javascript.tools.debugger.downloaded.TreeTableModelAdapter;

public class JTreeTable extends JTable {
	protected JTreeTable.TreeTableCellRenderer tree;

	public JTreeTable(TreeTableModel treeTableModel) {
		this.tree = new JTreeTable.TreeTableCellRenderer(treeTableModel);
		super.setModel(new TreeTableModelAdapter(treeTableModel, this.tree));
		JTreeTable.ListToTreeSelectionModelWrapper selectionWrapper = new JTreeTable.ListToTreeSelectionModelWrapper();
		this.tree.setSelectionModel(selectionWrapper);
		this.setSelectionModel(selectionWrapper.getListSelectionModel());
		this.setDefaultRenderer(TreeTableModel.class, this.tree);
		this.setDefaultEditor(TreeTableModel.class,
				new JTreeTable.TreeTableCellEditor());
		this.setShowGrid(false);
		this.setIntercellSpacing(new Dimension(0, 0));
		if (this.tree.getRowHeight() < 1) {
			this.setRowHeight(18);
		}

	}

	public void updateUI() {
		super.updateUI();
		if (this.tree != null) {
			this.tree.updateUI();
		}

		LookAndFeel.installColorsAndFont(this, "Tree.background",
				"Tree.foreground", "Tree.font");
	}

	public int getEditingRow() {
		return this.getColumnClass(this.editingColumn) == TreeTableModel.class
				? -1
				: this.editingRow;
	}

	public void setRowHeight(int rowHeight) {
		super.setRowHeight(rowHeight);
		if (this.tree != null && this.tree.getRowHeight() != rowHeight) {
			this.tree.setRowHeight(this.getRowHeight());
		}

	}

	public JTree getTree() {
		return this.tree;
	}

	public class ListToTreeSelectionModelWrapper
			extends
				DefaultTreeSelectionModel {
		protected boolean updatingListSelectionModel;

		public ListToTreeSelectionModelWrapper() {
			this.getListSelectionModel().addListSelectionListener(
					this.createListSelectionListener());
		}

		public ListSelectionModel getListSelectionModel() {
			return this.listSelectionModel;
		}

		public void resetRowSelection() {
			if (!this.updatingListSelectionModel) {
				this.updatingListSelectionModel = true;

				try {
					super.resetRowSelection();
				} finally {
					this.updatingListSelectionModel = false;
				}
			}

		}

		protected ListSelectionListener createListSelectionListener() {
			return new JTreeTable.ListToTreeSelectionModelWrapper.ListSelectionHandler();
		}

		protected void updateSelectedPathsFromSelectedRows() {
			if (!this.updatingListSelectionModel) {
				this.updatingListSelectionModel = true;

				try {
					int min = this.listSelectionModel.getMinSelectionIndex();
					int max = this.listSelectionModel.getMaxSelectionIndex();
					this.clearSelection();
					if (min != -1 && max != -1) {
						for (int counter = min; counter <= max; ++counter) {
							if (this.listSelectionModel
									.isSelectedIndex(counter)) {
								TreePath selPath = JTreeTable.this.tree
										.getPathForRow(counter);
								if (selPath != null) {
									this.addSelectionPath(selPath);
								}
							}
						}
					}
				} finally {
					this.updatingListSelectionModel = false;
				}
			}

		}

		class ListSelectionHandler implements ListSelectionListener {
			public void valueChanged(ListSelectionEvent e) {
				ListToTreeSelectionModelWrapper.this
						.updateSelectedPathsFromSelectedRows();
			}
		}
	}

	public class TreeTableCellEditor extends AbstractCellEditor
			implements
				TableCellEditor {
		public Component getTableCellEditorComponent(JTable table,
				Object value, boolean isSelected, int r, int c) {
			return JTreeTable.this.tree;
		}

		public boolean isCellEditable(EventObject e) {
			if (e instanceof MouseEvent) {
				for (int counter = JTreeTable.this.getColumnCount() - 1; counter >= 0; --counter) {
					if (JTreeTable.this.getColumnClass(counter) == TreeTableModel.class) {
						MouseEvent me = (MouseEvent) e;
						MouseEvent newME = new MouseEvent(JTreeTable.this.tree,
								me.getID(), me.getWhen(), me.getModifiers(),
								me.getX()
										- JTreeTable.this.getCellRect(0,
												counter, true).x, me.getY(),
								me.getClickCount(), me.isPopupTrigger());
						JTreeTable.this.tree.dispatchEvent(newME);
						break;
					}
				}
			}

			return false;
		}
	}

	public class TreeTableCellRenderer extends JTree
			implements
				TableCellRenderer {
		protected int visibleRow;

		public TreeTableCellRenderer(TreeModel model) {
			super(model);
		}

		public void updateUI() {
			super.updateUI();
			TreeCellRenderer tcr = this.getCellRenderer();
			if (tcr instanceof DefaultTreeCellRenderer) {
				DefaultTreeCellRenderer dtcr = (DefaultTreeCellRenderer) tcr;
				dtcr.setTextSelectionColor(UIManager
						.getColor("Table.selectionForeground"));
				dtcr.setBackgroundSelectionColor(UIManager
						.getColor("Table.selectionBackground"));
			}

		}

		public void setRowHeight(int rowHeight) {
			if (rowHeight > 0) {
				super.setRowHeight(rowHeight);
				if (JTreeTable.this != null
						&& JTreeTable.this.getRowHeight() != rowHeight) {
					JTreeTable.this.setRowHeight(this.getRowHeight());
				}
			}

		}

		public void setBounds(int x, int y, int w, int h) {
			super.setBounds(x, 0, w, JTreeTable.this.getHeight());
		}

		public void paint(Graphics g) {
			g.translate(0, -this.visibleRow * this.getRowHeight());
			super.paint(g);
		}

		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			if (isSelected) {
				this.setBackground(table.getSelectionBackground());
			} else {
				this.setBackground(table.getBackground());
			}

			this.visibleRow = row;
			return this;
		}
	}
}