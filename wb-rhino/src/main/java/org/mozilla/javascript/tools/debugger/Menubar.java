/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.tools.debugger;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.mozilla.javascript.tools.debugger.SwingGui;

class Menubar extends JMenuBar implements ActionListener {
	private static final long serialVersionUID = 3217170497245911461L;
	private List<JMenuItem> interruptOnlyItems = Collections
			.synchronizedList(new ArrayList());
	private List<JMenuItem> runOnlyItems = Collections
			.synchronizedList(new ArrayList());
	private SwingGui debugGui;
	private JMenu windowMenu;
	private JCheckBoxMenuItem breakOnExceptions;
	private JCheckBoxMenuItem breakOnEnter;
	private JCheckBoxMenuItem breakOnReturn;

	Menubar(SwingGui debugGui) {
		this.debugGui = debugGui;
		String[] fileItems = new String[]{"Open...", "Run...", "", "Exit"};
		String[] fileCmds = new String[]{"Open", "Load", "", "Exit"};
		char[] fileShortCuts = new char[]{'0', 'N', ' ', 'X'};
		int[] fileAccelerators = new int[]{79, 78, 0, 81};
		String[] editItems = new String[]{"Cut", "Copy", "Paste",
				"Go to function..."};
		char[] editShortCuts = new char[]{'T', 'C', 'P', 'F'};
		String[] debugItems = new String[]{"Break", "Go", "Step Into",
				"Step Over", "Step Out"};
		char[] debugShortCuts = new char[]{'B', 'G', 'I', 'O', 'T'};
		String[] plafItems = new String[]{"Metal", "Windows", "Motif"};
		char[] plafShortCuts = new char[]{'M', 'W', 'F'};
		int[] debugAccelerators = new int[]{19, 116, 122, 118, 119, 0, 0};
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic('F');
		JMenu editMenu = new JMenu("Edit");
		editMenu.setMnemonic('E');
		JMenu plafMenu = new JMenu("Platform");
		plafMenu.setMnemonic('P');
		JMenu debugMenu = new JMenu("Debug");
		debugMenu.setMnemonic('D');
		this.windowMenu = new JMenu("Window");
		this.windowMenu.setMnemonic('W');

		int item;
		JMenuItem item1;
		KeyStroke k;
		for (item = 0; item < fileItems.length; ++item) {
			if (fileItems[item].length() == 0) {
				fileMenu.addSeparator();
			} else {
				item1 = new JMenuItem(fileItems[item], fileShortCuts[item]);
				item1.setActionCommand(fileCmds[item]);
				item1.addActionListener(this);
				fileMenu.add(item1);
				if (fileAccelerators[item] != 0) {
					k = KeyStroke.getKeyStroke(fileAccelerators[item], 2);
					item1.setAccelerator(k);
				}
			}
		}

		for (item = 0; item < editItems.length; ++item) {
			item1 = new JMenuItem(editItems[item], editShortCuts[item]);
			item1.addActionListener(this);
			editMenu.add(item1);
		}

		for (item = 0; item < plafItems.length; ++item) {
			item1 = new JMenuItem(plafItems[item], plafShortCuts[item]);
			item1.addActionListener(this);
			plafMenu.add(item1);
		}

		for (item = 0; item < debugItems.length; ++item) {
			item1 = new JMenuItem(debugItems[item], debugShortCuts[item]);
			item1.addActionListener(this);
			if (debugAccelerators[item] != 0) {
				k = KeyStroke.getKeyStroke(debugAccelerators[item], 0);
				item1.setAccelerator(k);
			}

			if (item != 0) {
				this.interruptOnlyItems.add(item1);
			} else {
				this.runOnlyItems.add(item1);
			}

			debugMenu.add(item1);
		}

		this.breakOnExceptions = new JCheckBoxMenuItem("Break on Exceptions");
		this.breakOnExceptions.setMnemonic('X');
		this.breakOnExceptions.addActionListener(this);
		this.breakOnExceptions.setSelected(false);
		debugMenu.add(this.breakOnExceptions);
		this.breakOnEnter = new JCheckBoxMenuItem("Break on Function Enter");
		this.breakOnEnter.setMnemonic('E');
		this.breakOnEnter.addActionListener(this);
		this.breakOnEnter.setSelected(false);
		debugMenu.add(this.breakOnEnter);
		this.breakOnReturn = new JCheckBoxMenuItem("Break on Function Return");
		this.breakOnReturn.setMnemonic('R');
		this.breakOnReturn.addActionListener(this);
		this.breakOnReturn.setSelected(false);
		debugMenu.add(this.breakOnReturn);
		this.add(fileMenu);
		this.add(editMenu);
		this.add(debugMenu);
		JMenuItem arg19;
		this.windowMenu.add(arg19 = new JMenuItem("Cascade", 65));
		arg19.addActionListener(this);
		this.windowMenu.add(arg19 = new JMenuItem("Tile", 84));
		arg19.addActionListener(this);
		this.windowMenu.addSeparator();
		this.windowMenu.add(arg19 = new JMenuItem("Console", 67));
		arg19.addActionListener(this);
		this.add(this.windowMenu);
		this.updateEnabled(false);
	}

	public JCheckBoxMenuItem getBreakOnExceptions() {
		return this.breakOnExceptions;
	}

	public JCheckBoxMenuItem getBreakOnEnter() {
		return this.breakOnEnter;
	}

	public JCheckBoxMenuItem getBreakOnReturn() {
		return this.breakOnReturn;
	}

	public JMenu getDebugMenu() {
		return this.getMenu(2);
	}

	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		String plaf_name = null;
		if (cmd.equals("Metal")) {
			plaf_name = "javax.swing.plaf.metal.MetalLookAndFeel";
		} else if (cmd.equals("Windows")) {
			plaf_name = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
		} else {
			if (!cmd.equals("Motif")) {
				Object ignored = e.getSource();
				if (ignored == this.breakOnExceptions) {
					this.debugGui.dim
							.setBreakOnExceptions(this.breakOnExceptions
									.isSelected());
				} else if (ignored == this.breakOnEnter) {
					this.debugGui.dim.setBreakOnEnter(this.breakOnEnter
							.isSelected());
				} else if (ignored == this.breakOnReturn) {
					this.debugGui.dim.setBreakOnReturn(this.breakOnReturn
							.isSelected());
				} else {
					this.debugGui.actionPerformed(e);
				}

				return;
			}

			plaf_name = "com.sun.java.swing.plaf.motif.MotifLookAndFeel";
		}

		try {
			UIManager.setLookAndFeel(plaf_name);
			SwingUtilities.updateComponentTreeUI(this.debugGui);
			SwingUtilities.updateComponentTreeUI(this.debugGui.dlg);
		} catch (Exception arg4) {
			;
		}

	}

	public void addFile(String url) {
		int count = this.windowMenu.getItemCount();
		if (count == 4) {
			this.windowMenu.addSeparator();
			++count;
		}

		JMenuItem lastItem = this.windowMenu.getItem(count - 1);
		boolean hasMoreWin = false;
		int maxWin = 5;
		if (lastItem != null && lastItem.getText().equals("More Windows...")) {
			hasMoreWin = true;
			++maxWin;
		}

		JMenuItem item;
		if (!hasMoreWin && count - 4 == 5) {
			this.windowMenu.add(item = new JMenuItem("More Windows...", 77));
			item.setActionCommand("More Windows...");
			item.addActionListener(this);
		} else if (count - 4 <= maxWin) {
			if (hasMoreWin) {
				--count;
				this.windowMenu.remove(lastItem);
			}

			String shortName = SwingGui.getShortName(url);
			this.windowMenu.add(item = new JMenuItem((char) (48 + (count - 4))
					+ " " + shortName, 48 + (count - 4)));
			if (hasMoreWin) {
				this.windowMenu.add(lastItem);
			}

			item.setActionCommand(url);
			item.addActionListener(this);
		}
	}

	public void updateEnabled(boolean interrupted) {
		int i;
		JMenuItem item;
		for (i = 0; i != this.interruptOnlyItems.size(); ++i) {
			item = (JMenuItem) this.interruptOnlyItems.get(i);
			item.setEnabled(interrupted);
		}

		for (i = 0; i != this.runOnlyItems.size(); ++i) {
			item = (JMenuItem) this.runOnlyItems.get(i);
			item.setEnabled(!interrupted);
		}

	}
}