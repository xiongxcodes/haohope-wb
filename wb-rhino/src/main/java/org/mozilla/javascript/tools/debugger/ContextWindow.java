/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.tools.debugger;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventListener;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import org.mozilla.javascript.tools.debugger.EvalTextArea;
import org.mozilla.javascript.tools.debugger.Evaluator;
import org.mozilla.javascript.tools.debugger.MyTableModel;
import org.mozilla.javascript.tools.debugger.MyTreeTable;
import org.mozilla.javascript.tools.debugger.SwingGui;
import org.mozilla.javascript.tools.debugger.VariableModel;
import org.mozilla.javascript.tools.debugger.Dim.ContextData;
import org.mozilla.javascript.tools.debugger.Dim.StackFrame;

class ContextWindow extends JPanel implements ActionListener {
	private static final long serialVersionUID = 2306040975490228051L;
	private SwingGui debugGui;
	JComboBox context;
	List<String> toolTips;
	private JTabbedPane tabs;
	private JTabbedPane tabs2;
	private MyTreeTable thisTable;
	private MyTreeTable localsTable;
	private MyTableModel tableModel;
	private Evaluator evaluator;
	private EvalTextArea cmdLine;
	JSplitPane split;
	private boolean enabled;

	public ContextWindow(final SwingGui debugGui) {
		this.debugGui = debugGui;
		this.enabled = false;
		JPanel left = new JPanel();
		final JToolBar t1 = new JToolBar();
		t1.setName("Variables");
		t1.setLayout(new GridLayout());
		t1.add(left);
		final JPanel p1 = new JPanel();
		p1.setLayout(new GridLayout());
		final JPanel p2 = new JPanel();
		p2.setLayout(new GridLayout());
		p1.add(t1);
		JLabel label = new JLabel("Context:");
		this.context = new JComboBox();
		this.context.setLightWeightPopupEnabled(false);
		this.toolTips = Collections.synchronizedList(new ArrayList());
		label.setBorder(this.context.getBorder());
		this.context.addActionListener(this);
		this.context.setActionCommand("ContextSwitch");
		GridBagLayout layout = new GridBagLayout();
		left.setLayout(layout);
		GridBagConstraints lc = new GridBagConstraints();
		lc.insets.left = 5;
		lc.anchor = 17;
		lc.ipadx = 5;
		layout.setConstraints(label, lc);
		left.add(label);
		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 0;
		c.fill = 2;
		c.anchor = 17;
		layout.setConstraints(this.context, c);
		left.add(this.context);
		this.tabs = new JTabbedPane(3);
		this.tabs.setPreferredSize(new Dimension(500, 300));
		this.thisTable = new MyTreeTable(new VariableModel());
		JScrollPane jsp = new JScrollPane(this.thisTable);
		jsp.getViewport().setViewSize(new Dimension(5, 2));
		this.tabs.add("this", jsp);
		this.localsTable = new MyTreeTable(new VariableModel());
		this.localsTable.setAutoResizeMode(4);
		this.localsTable.setPreferredSize((Dimension) null);
		jsp = new JScrollPane(this.localsTable);
		this.tabs.add("Locals", jsp);
		c.weightx = c.weighty = 1.0D;
		c.gridheight = 0;
		c.fill = 1;
		c.anchor = 17;
		layout.setConstraints(this.tabs, c);
		left.add(this.tabs);
		this.evaluator = new Evaluator(debugGui);
		this.cmdLine = new EvalTextArea(debugGui);
		this.tableModel = this.evaluator.tableModel;
		jsp = new JScrollPane(this.evaluator);
		final JToolBar t2 = new JToolBar();
		t2.setName("Evaluate");
		this.tabs2 = new JTabbedPane(3);
		this.tabs2.add("Watch", jsp);
		this.tabs2.add("Evaluate", new JScrollPane(this.cmdLine));
		this.tabs2.setPreferredSize(new Dimension(500, 300));
		t2.setLayout(new GridLayout());
		t2.add(this.tabs2);
		p2.add(t2);
		this.evaluator.setAutoResizeMode(4);
		this.split = new JSplitPane(1, p1, p2);
		this.split.setOneTouchExpandable(true);
		SwingGui.setResizeWeight(this.split, 0.5D);
		this.setLayout(new BorderLayout());
		this.add(this.split, "Center");
		final JSplitPane finalSplit = this.split;
		ComponentListener clistener = new ComponentListener() {
			boolean t2Docked = true;

			void check(Component comp) {
				Container thisParent = ContextWindow.this.getParent();
				if (thisParent != null) {
					Container parent = t1.getParent();
					boolean leftDocked = true;
					boolean rightDocked = true;
					boolean adjustVerticalSplit = false;
					JFrame split;
					if (parent != null) {
						if (parent == p1) {
							leftDocked = true;
						} else {
							while (!(parent instanceof JFrame)) {
								parent = parent.getParent();
							}

							split = (JFrame) parent;
							debugGui.addTopLevel("Variables", split);
							if (!split.isResizable()) {
								split.setResizable(true);
								split.setDefaultCloseOperation(0);
								final EventListener[] l = split
										.getListeners(WindowListener.class);
								split.removeWindowListener((WindowListener) l[0]);
								split.addWindowListener(new WindowAdapter() {
									public void windowClosing(WindowEvent e) {
										ContextWindow.this.context.hidePopup();
										((WindowListener) l[0])
												.windowClosing(e);
									}
								});
							}

							leftDocked = false;
						}
					}

					parent = t2.getParent();
					if (parent != null) {
						if (parent == p2) {
							rightDocked = true;
						} else {
							while (!(parent instanceof JFrame)) {
								parent = parent.getParent();
							}

							split = (JFrame) parent;
							debugGui.addTopLevel("Evaluate", split);
							split.setResizable(true);
							rightDocked = false;
						}
					}

					if (!leftDocked || !this.t2Docked || !rightDocked
							|| !this.t2Docked) {
						this.t2Docked = rightDocked;
						JSplitPane split1 = (JSplitPane) thisParent;
						if (leftDocked) {
							if (rightDocked) {
								finalSplit.setDividerLocation(0.5D);
							} else {
								finalSplit.setDividerLocation(1.0D);
							}

							if (adjustVerticalSplit) {
								split1.setDividerLocation(0.66D);
							}
						} else if (rightDocked) {
							finalSplit.setDividerLocation(0.0D);
							split1.setDividerLocation(0.66D);
						} else {
							split1.setDividerLocation(1.0D);
						}

					}
				}
			}

			public void componentHidden(ComponentEvent e) {
				this.check(e.getComponent());
			}

			public void componentMoved(ComponentEvent e) {
				this.check(e.getComponent());
			}

			public void componentResized(ComponentEvent e) {
				this.check(e.getComponent());
			}

			public void componentShown(ComponentEvent e) {
				this.check(e.getComponent());
			}
		};
		p1.addContainerListener(new ContainerListener() {
			public void componentAdded(ContainerEvent e) {
				Container thisParent = ContextWindow.this.getParent();
				JSplitPane split = (JSplitPane) thisParent;
				if (e.getChild() == t1) {
					if (t2.getParent() == p2) {
						finalSplit.setDividerLocation(0.5D);
					} else {
						finalSplit.setDividerLocation(1.0D);
					}

					split.setDividerLocation(0.66D);
				}

			}

			public void componentRemoved(ContainerEvent e) {
				Container thisParent = ContextWindow.this.getParent();
				JSplitPane split = (JSplitPane) thisParent;
				if (e.getChild() == t1) {
					if (t2.getParent() == p2) {
						finalSplit.setDividerLocation(0.0D);
						split.setDividerLocation(0.66D);
					} else {
						split.setDividerLocation(1.0D);
					}
				}

			}
		});
		t1.addComponentListener(clistener);
		t2.addComponentListener(clistener);
		this.disable();
	}

	public void disable() {
		this.context.setEnabled(false);
		this.thisTable.setEnabled(false);
		this.localsTable.setEnabled(false);
		this.evaluator.setEnabled(false);
		this.cmdLine.setEnabled(false);
	}

	public void enable() {
		this.context.setEnabled(true);
		this.thisTable.setEnabled(true);
		this.localsTable.setEnabled(true);
		this.evaluator.setEnabled(true);
		this.cmdLine.setEnabled(true);
	}

	public void disableUpdate() {
		this.enabled = false;
	}

	public void enableUpdate() {
		this.enabled = true;
	}

	public void actionPerformed(ActionEvent e) {
		if (this.enabled) {
			if (e.getActionCommand().equals("ContextSwitch")) {
				ContextData contextData = this.debugGui.dim
						.currentContextData();
				if (contextData == null) {
					return;
				}

				int frameIndex = this.context.getSelectedIndex();
				this.context.setToolTipText((String) this.toolTips
						.get(frameIndex));
				int frameCount = contextData.frameCount();
				if (frameIndex >= frameCount) {
					return;
				}

				StackFrame frame = contextData.getFrame(frameIndex);
				Object scope = frame.scope();
				Object thisObj = frame.thisObj();
				this.thisTable.resetTree(new VariableModel(this.debugGui.dim,
						thisObj));
				VariableModel scopeModel;
				if (scope != thisObj) {
					scopeModel = new VariableModel(this.debugGui.dim, scope);
				} else {
					scopeModel = new VariableModel();
				}

				this.localsTable.resetTree(scopeModel);
				this.debugGui.dim.contextSwitch(frameIndex);
				this.debugGui.showStopLine(frame);
				this.tableModel.updateModel();
			}

		}
	}
}