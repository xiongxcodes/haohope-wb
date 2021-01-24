/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.tools.shell;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import org.mozilla.javascript.SecurityUtilities;
import org.mozilla.javascript.tools.shell.ConsoleTextArea;
import org.mozilla.javascript.tools.shell.Main;

public class JSConsole extends JFrame implements ActionListener {
	static final long serialVersionUID = 2551225560631876300L;
	private File CWD;
	private JFileChooser dlg;
	private ConsoleTextArea consoleTextArea;

	public String chooseFile() {
		if (this.CWD == null) {
			String returnVal = SecurityUtilities.getSystemProperty("user.dir");
			if (returnVal != null) {
				this.CWD = new File(returnVal);
			}
		}

		if (this.CWD != null) {
			this.dlg.setCurrentDirectory(this.CWD);
		}

		this.dlg.setDialogTitle("Select a file to load");
		int returnVal1 = this.dlg.showOpenDialog(this);
		if (returnVal1 == 0) {
			String result = this.dlg.getSelectedFile().getPath();
			this.CWD = new File(this.dlg.getSelectedFile().getParent());
			return result;
		} else {
			return null;
		}
	}

	public static void main(String[] args) {
		new JSConsole(args);
	}

	public void createFileChooser() {
		this.dlg = new JFileChooser();
		FileFilter filter = new FileFilter() {
			public boolean accept(File f) {
				if (f.isDirectory()) {
					return true;
				} else {
					String name = f.getName();
					int i = name.lastIndexOf(46);
					if (i > 0 && i < name.length() - 1) {
						String ext = name.substring(i + 1).toLowerCase();
						if (ext.equals("js")) {
							return true;
						}
					}

					return false;
				}
			}

			public String getDescription() {
				return "JavaScript Files (*.js)";
			}
		};
		this.dlg.addChoosableFileFilter(filter);
	}

	public JSConsole(String[] args) {
		super("Rhino JavaScript Console");
		JMenuBar menubar = new JMenuBar();
		this.createFileChooser();
		String[] fileItems = new String[]{"Load...", "Exit"};
		String[] fileCmds = new String[]{"Load", "Exit"};
		char[] fileShortCuts = new char[]{'L', 'X'};
		String[] editItems = new String[]{"Cut", "Copy", "Paste"};
		char[] editShortCuts = new char[]{'T', 'C', 'P'};
		String[] plafItems = new String[]{"Metal", "Windows", "Motif"};
		boolean[] plafState = new boolean[]{true, false, false};
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic('F');
		JMenu editMenu = new JMenu("Edit");
		editMenu.setMnemonic('E');
		JMenu plafMenu = new JMenu("Platform");
		plafMenu.setMnemonic('P');

		int group;
		JMenuItem scroller;
		for (group = 0; group < fileItems.length; ++group) {
			scroller = new JMenuItem(fileItems[group], fileShortCuts[group]);
			scroller.setActionCommand(fileCmds[group]);
			scroller.addActionListener(this);
			fileMenu.add(scroller);
		}

		for (group = 0; group < editItems.length; ++group) {
			scroller = new JMenuItem(editItems[group], editShortCuts[group]);
			scroller.addActionListener(this);
			editMenu.add(scroller);
		}

		ButtonGroup arg15 = new ButtonGroup();

		for (int arg16 = 0; arg16 < plafItems.length; ++arg16) {
			JRadioButtonMenuItem item = new JRadioButtonMenuItem(
					plafItems[arg16], plafState[arg16]);
			arg15.add(item);
			item.addActionListener(this);
			plafMenu.add(item);
		}

		menubar.add(fileMenu);
		menubar.add(editMenu);
		menubar.add(plafMenu);
		this.setJMenuBar(menubar);
		this.consoleTextArea = new ConsoleTextArea(args);
		JScrollPane arg17 = new JScrollPane(this.consoleTextArea);
		this.setContentPane(arg17);
		this.consoleTextArea.setRows(24);
		this.consoleTextArea.setColumns(80);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		this.pack();
		this.setVisible(true);
		Main.setIn(this.consoleTextArea.getIn());
		Main.setOut(this.consoleTextArea.getOut());
		Main.setErr(this.consoleTextArea.getErr());
		Main.main(args);
	}

	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		String plaf_name = null;
		if (cmd.equals("Load")) {
			String exc = this.chooseFile();
			if (exc != null) {
				exc = exc.replace('\\', '/');
				this.consoleTextArea.eval("load(\"" + exc + "\");");
			}
		} else if (cmd.equals("Exit")) {
			System.exit(0);
		} else if (cmd.equals("Cut")) {
			this.consoleTextArea.cut();
		} else if (cmd.equals("Copy")) {
			this.consoleTextArea.copy();
		} else if (cmd.equals("Paste")) {
			this.consoleTextArea.paste();
		} else {
			if (cmd.equals("Metal")) {
				plaf_name = "javax.swing.plaf.metal.MetalLookAndFeel";
			} else if (cmd.equals("Windows")) {
				plaf_name = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
			} else if (cmd.equals("Motif")) {
				plaf_name = "com.sun.java.swing.plaf.motif.MotifLookAndFeel";
			}

			if (plaf_name != null) {
				try {
					UIManager.setLookAndFeel(plaf_name);
					SwingUtilities.updateComponentTreeUI(this);
					this.consoleTextArea.postUpdateUI();
					this.createFileChooser();
				} catch (Exception arg4) {
					JOptionPane.showMessageDialog(this, arg4.getMessage(),
							"Platform", 0);
				}
			}
		}

	}
}