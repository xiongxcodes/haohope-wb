/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.tools.debugger;

import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.BadLocationException;
import org.mozilla.javascript.tools.debugger.FilePopupMenu;
import org.mozilla.javascript.tools.debugger.FileWindow;

class FileTextArea extends JTextArea
		implements
			ActionListener,
			PopupMenuListener,
			KeyListener,
			MouseListener {
	private static final long serialVersionUID = -25032065448563720L;
	private FileWindow w;
	private FilePopupMenu popup;

	public FileTextArea(FileWindow w) {
		this.w = w;
		this.popup = new FilePopupMenu(this);
		this.popup.addPopupMenuListener(this);
		this.addMouseListener(this);
		this.addKeyListener(this);
		this.setFont(new Font("Monospaced", 0, 12));
	}

	public void select(int pos) {
		if (pos >= 0) {
			try {
				int exc = this.getLineOfOffset(pos);
				Rectangle rect = this.modelToView(pos);
				if (rect == null) {
					this.select(pos, pos);
				} else {
					try {
						Rectangle vp = this.modelToView(this
								.getLineStartOffset(exc + 1));
						if (vp != null) {
							rect = vp;
						}
					} catch (Exception arg5) {
						;
					}

					JViewport vp1 = (JViewport) this.getParent();
					Rectangle viewRect = vp1.getViewRect();
					if (viewRect.y + viewRect.height > rect.y) {
						this.select(pos, pos);
					} else {
						rect.y += (viewRect.height - rect.height) / 2;
						this.scrollRectToVisible(rect);
						this.select(pos, pos);
					}
				}
			} catch (BadLocationException arg6) {
				this.select(pos, pos);
			}
		}

	}

	private void checkPopup(MouseEvent e) {
		if (e.isPopupTrigger()) {
			this.popup.show(this, e.getX(), e.getY());
		}

	}

	public void mousePressed(MouseEvent e) {
		this.checkPopup(e);
	}

	public void mouseClicked(MouseEvent e) {
		this.checkPopup(e);
		this.requestFocus();
		this.getCaret().setVisible(true);
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mouseReleased(MouseEvent e) {
		this.checkPopup(e);
	}

	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
	}

	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
	}

	public void popupMenuCanceled(PopupMenuEvent e) {
	}

	public void actionPerformed(ActionEvent e) {
		int pos = this.viewToModel(new Point(this.popup.x, this.popup.y));
		this.popup.setVisible(false);
		String cmd = e.getActionCommand();
		int line = -1;

		try {
			line = this.getLineOfOffset(pos);
		} catch (Exception arg5) {
			;
		}

		if (cmd.equals("Set Breakpoint")) {
			this.w.setBreakPoint(line + 1);
		} else if (cmd.equals("Clear Breakpoint")) {
			this.w.clearBreakPoint(line + 1);
		} else if (cmd.equals("Run")) {
			this.w.load();
		}

	}

	public void keyPressed(KeyEvent e) {
		switch (e.getKeyCode()) {
			case 8 :
			case 9 :
			case 10 :
			case 127 :
				e.consume();
			default :
		}
	}

	public void keyTyped(KeyEvent e) {
		e.consume();
	}

	public void keyReleased(KeyEvent e) {
		e.consume();
	}
}