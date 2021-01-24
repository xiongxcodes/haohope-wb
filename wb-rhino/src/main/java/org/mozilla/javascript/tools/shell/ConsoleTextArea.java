/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.tools.shell;

import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Segment;
import org.mozilla.javascript.tools.shell.ConsoleWriter;

public class ConsoleTextArea extends JTextArea
		implements
			KeyListener,
			DocumentListener {
	static final long serialVersionUID = 8557083244830872961L;
	private ConsoleWriter console1 = new ConsoleWriter(this);
	private ConsoleWriter console2 = new ConsoleWriter(this);
	private PrintStream out;
	private PrintStream err;
	private PrintWriter inPipe;
	private PipedInputStream in;
	private List<String> history = new ArrayList();
	private int historyIndex = -1;
	private int outputMark = 0;

	public void select(int start, int end) {
		this.requestFocus();
		super.select(start, end);
	}

	public ConsoleTextArea(String[] argv) {
		this.out = new PrintStream(this.console1, true);
		this.err = new PrintStream(this.console2, true);
		PipedOutputStream outPipe = new PipedOutputStream();
		this.inPipe = new PrintWriter(outPipe);
		this.in = new PipedInputStream();

		try {
			outPipe.connect(this.in);
		} catch (IOException arg3) {
			arg3.printStackTrace();
		}

		this.getDocument().addDocumentListener(this);
		this.addKeyListener(this);
		this.setLineWrap(true);
		this.setFont(new Font("Monospaced", 0, 12));
	}

	synchronized void returnPressed() {
		Document doc = this.getDocument();
		int len = doc.getLength();
		Segment segment = new Segment();

		try {
			doc.getText(this.outputMark, len - this.outputMark, segment);
		} catch (BadLocationException arg4) {
			arg4.printStackTrace();
		}

		if (segment.count > 0) {
			this.history.add(segment.toString());
		}

		this.historyIndex = this.history.size();
		this.inPipe.write(segment.array, segment.offset, segment.count);
		this.append("\n");
		this.outputMark = doc.getLength();
		this.inPipe.write("\n");
		this.inPipe.flush();
		this.console1.flush();
	}

	public void eval(String str) {
		this.inPipe.write(str);
		this.inPipe.write("\n");
		this.inPipe.flush();
		this.console1.flush();
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

	public synchronized void write(String str) {
		this.insert(str, this.outputMark);
		int len = str.length();
		this.outputMark += len;
		this.select(this.outputMark, this.outputMark);
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
		this.requestFocus();
		this.setCaret(this.getCaret());
		this.select(this.outputMark, this.outputMark);
	}

	public synchronized void changedUpdate(DocumentEvent e) {
	}

	public InputStream getIn() {
		return this.in;
	}

	public PrintStream getOut() {
		return this.out;
	}

	public PrintStream getErr() {
		return this.err;
	}
}