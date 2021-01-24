/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.tools.debugger;

import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Segment;
import org.mozilla.javascript.tools.debugger.SwingGui;

class EvalTextArea extends JTextArea implements KeyListener, DocumentListener {
	private static final long serialVersionUID = -3918033649601064194L;
	private SwingGui debugGui;
	private List<String> history;
	private int historyIndex = -1;
	private int outputMark;

	public EvalTextArea(SwingGui debugGui) {
		this.debugGui = debugGui;
		this.history = Collections.synchronizedList(new ArrayList());
		Document doc = this.getDocument();
		doc.addDocumentListener(this);
		this.addKeyListener(this);
		this.setLineWrap(true);
		this.setFont(new Font("Monospaced", 0, 12));
		this.append("% ");
		this.outputMark = doc.getLength();
	}

	public void select(int start, int end) {
		super.select(start, end);
	}

	private synchronized void returnPressed() {
		Document doc = this.getDocument();
		int len = doc.getLength();
		Segment segment = new Segment();

		try {
			doc.getText(this.outputMark, len - this.outputMark, segment);
		} catch (BadLocationException arg5) {
			arg5.printStackTrace();
		}

		String text = segment.toString();
		if (this.debugGui.dim.stringIsCompilableUnit(text)) {
			if (text.trim().length() > 0) {
				this.history.add(text);
				this.historyIndex = this.history.size();
			}

			this.append("\n");
			String result = this.debugGui.dim.eval(text);
			if (result.length() > 0) {
				this.append(result);
				this.append("\n");
			}

			this.append("% ");
			this.outputMark = doc.getLength();
		} else {
			this.append("\n");
		}

	}

	public synchronized void write(String str) {
		this.insert(str, this.outputMark);
		int len = str.length();
		this.outputMark += len;
		this.select(this.outputMark, this.outputMark);
	}

	public void keyPressed(KeyEvent e) {
		int code = e.getKeyCode();
		if (code != 8 && code != 37) {
			int caretPos;
			if (code == 36) {
				caretPos = this.getCaretPosition();
				if (caretPos == this.outputMark) {
					e.consume();
				} else if (caretPos > this.outputMark && !e.isControlDown()) {
					if (e.isShiftDown()) {
						this.moveCaretPosition(this.outputMark);
					} else {
						this.setCaretPosition(this.outputMark);
					}

					e.consume();
				}
			} else if (code == 10) {
				this.returnPressed();
				e.consume();
			} else {
				int len;
				if (code == 38) {
					--this.historyIndex;
					if (this.historyIndex >= 0) {
						if (this.historyIndex >= this.history.size()) {
							this.historyIndex = this.history.size() - 1;
						}

						if (this.historyIndex >= 0) {
							String arg5 = (String) this.history
									.get(this.historyIndex);
							len = this.getDocument().getLength();
							this.replaceRange(arg5, this.outputMark, len);
							int str = this.outputMark + arg5.length();
							this.select(str, str);
						} else {
							++this.historyIndex;
						}
					} else {
						++this.historyIndex;
					}

					e.consume();
				} else if (code == 40) {
					caretPos = this.outputMark;
					if (this.history.size() > 0) {
						++this.historyIndex;
						if (this.historyIndex < 0) {
							this.historyIndex = 0;
						}

						len = this.getDocument().getLength();
						if (this.historyIndex < this.history.size()) {
							String arg6 = (String) this.history
									.get(this.historyIndex);
							this.replaceRange(arg6, this.outputMark, len);
							caretPos = this.outputMark + arg6.length();
						} else {
							this.historyIndex = this.history.size();
							this.replaceRange("", this.outputMark, len);
						}
					}

					this.select(caretPos, caretPos);
					e.consume();
				}
			}
		} else if (this.outputMark == this.getCaretPosition()) {
			e.consume();
		}

	}

	public void keyTyped(KeyEvent e) {
		char keyChar = e.getKeyChar();
		if (keyChar == 8) {
			if (this.outputMark == this.getCaretPosition()) {
				e.consume();
			}
		} else if (this.getCaretPosition() < this.outputMark) {
			this.setCaretPosition(this.outputMark);
		}

	}

	public synchronized void keyReleased(KeyEvent e) {
	}

	public synchronized void insertUpdate(DocumentEvent e) {
		int len = e.getLength();
		int off = e.getOffset();
		if (this.outputMark > off) {
			this.outputMark += len;
		}

	}

	public synchronized void removeUpdate(DocumentEvent e) {
		int len = e.getLength();
		int off = e.getOffset();
		if (this.outputMark > off) {
			if (this.outputMark >= off + len) {
				this.outputMark -= len;
			} else {
				this.outputMark = off;
			}
		}

	}

	public synchronized void postUpdateUI() {
		this.setCaret(this.getCaret());
		this.select(this.outputMark, this.outputMark);
	}

	public synchronized void changedUpdate(DocumentEvent e) {
	}
}