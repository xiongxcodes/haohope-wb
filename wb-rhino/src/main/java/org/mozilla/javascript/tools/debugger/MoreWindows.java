/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.tools.debugger;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.mozilla.javascript.tools.debugger.FileWindow;
import org.mozilla.javascript.tools.debugger.SwingGui;

class MoreWindows extends JDialog implements ActionListener {
	private static final long serialVersionUID = 5177066296457377546L;
	private String value;
	private JList list;
	private SwingGui swingGui;
	private JButton setButton;
	private JButton cancelButton;

	MoreWindows(SwingGui frame, Map<String, FileWindow> fileWindows,
			String title, String labelText) {
		super(frame, title, true);
		this.swingGui = frame;
		this.cancelButton = new JButton("Cancel");
		this.setButton = new JButton("Select");
		this.cancelButton.addActionListener(this);
		this.setButton.addActionListener(this);
		this.getRootPane().setDefaultButton(this.setButton);
		this.list = new JList(new DefaultListModel());
		DefaultListModel model = (DefaultListModel) this.list.getModel();
		model.clear();
		Iterator listScroller = fileWindows.keySet().iterator();

		while (listScroller.hasNext()) {
			String listPane = (String) listScroller.next();
			model.addElement(listPane);
		}

		this.list.setSelectedIndex(0);
		this.setButton.setEnabled(true);
		this.list.setSelectionMode(1);
		this.list.addMouseListener(new MoreWindows.MouseHandler(null));
		JScrollPane listScroller1 = new JScrollPane(this.list);
		listScroller1.setPreferredSize(new Dimension(320, 240));
		listScroller1.setMinimumSize(new Dimension(250, 80));
		listScroller1.setAlignmentX(0.0F);
		JPanel listPane1 = new JPanel();
		listPane1.setLayout(new BoxLayout(listPane1, 1));
		JLabel label = new JLabel(labelText);
		label.setLabelFor(this.list);
		listPane1.add(label);
		listPane1.add(Box.createRigidArea(new Dimension(0, 5)));
		listPane1.add(listScroller1);
		listPane1.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, 0));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(this.cancelButton);
		buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPane.add(this.setButton);
		Container contentPane = this.getContentPane();
		contentPane.add(listPane1, "Center");
		contentPane.add(buttonPane, "South");
		this.pack();
		this.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent ke) {
				int code = ke.getKeyCode();
				if (code == 27) {
					ke.consume();
					MoreWindows.this.value = null;
					MoreWindows.this.setVisible(false);
				}

			}
		});
	}

	public String showDialog(Component comp) {
		this.value = null;
		this.setLocationRelativeTo(comp);
		this.setVisible(true);
		return this.value;
	}

	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if (cmd.equals("Cancel")) {
			this.setVisible(false);
			this.value = null;
		} else if (cmd.equals("Select")) {
			this.value = (String) this.list.getSelectedValue();
			this.setVisible(false);
			this.swingGui.showFileWindow(this.value, -1);
		}

	}

	private class MouseHandler extends MouseAdapter {
		private MouseHandler(Object object) {
		}

		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2) {
				MoreWindows.this.setButton.doClick();
			}

		}
	}
}