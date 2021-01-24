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
import java.util.Arrays;
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
import org.mozilla.javascript.tools.debugger.SwingGui;
import org.mozilla.javascript.tools.debugger.Dim.FunctionSource;
import org.mozilla.javascript.tools.debugger.Dim.SourceInfo;

class FindFunction extends JDialog implements ActionListener {
	private static final long serialVersionUID = 559491015232880916L;
	private String value;
	private JList list;
	private SwingGui debugGui;
	private JButton setButton;
	private JButton cancelButton;

	public FindFunction(SwingGui debugGui, String title, String labelText) {
		super(debugGui, title, true);
		this.debugGui = debugGui;
		this.cancelButton = new JButton("Cancel");
		this.setButton = new JButton("Select");
		this.cancelButton.addActionListener(this);
		this.setButton.addActionListener(this);
		this.getRootPane().setDefaultButton(this.setButton);
		this.list = new JList(new DefaultListModel());
		DefaultListModel model = (DefaultListModel) this.list.getModel();
		model.clear();
		String[] a = debugGui.dim.functionNames();
		Arrays.sort(a);

		for (int listScroller = 0; listScroller < a.length; ++listScroller) {
			model.addElement(a[listScroller]);
		}

		this.list.setSelectedIndex(0);
		this.setButton.setEnabled(a.length > 0);
		this.list.setSelectionMode(1);
		this.list.addMouseListener(new FindFunction.MouseHandler());
		JScrollPane arg10 = new JScrollPane(this.list);
		arg10.setPreferredSize(new Dimension(320, 240));
		arg10.setMinimumSize(new Dimension(250, 80));
		arg10.setAlignmentX(0.0F);
		JPanel listPane = new JPanel();
		listPane.setLayout(new BoxLayout(listPane, 1));
		JLabel label = new JLabel(labelText);
		label.setLabelFor(this.list);
		listPane.add(label);
		listPane.add(Box.createRigidArea(new Dimension(0, 5)));
		listPane.add(arg10);
		listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, 0));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(this.cancelButton);
		buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPane.add(this.setButton);
		Container contentPane = this.getContentPane();
		contentPane.add(listPane, "Center");
		contentPane.add(buttonPane, "South");
		this.pack();
		this.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent ke) {
				int code = ke.getKeyCode();
				if (code == 27) {
					ke.consume();
					FindFunction.this.value = null;
					FindFunction.this.setVisible(false);
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
			if (this.list.getSelectedIndex() < 0) {
				return;
			}

			try {
				this.value = (String) this.list.getSelectedValue();
			} catch (ArrayIndexOutOfBoundsException arg6) {
				return;
			}

			this.setVisible(false);
			FunctionSource item = this.debugGui.dim
					.functionSourceByName(this.value);
			if (item != null) {
				SourceInfo si = item.sourceInfo();
				String url = si.url();
				int lineNumber = item.firstLine();
				this.debugGui.showFileWindow(url, lineNumber);
			}
		}

	}

	class MouseHandler extends MouseAdapter {
		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2) {
				FindFunction.this.setButton.doClick();
			}

		}
	}
}