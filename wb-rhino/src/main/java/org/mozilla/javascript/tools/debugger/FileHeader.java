/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.tools.debugger;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JPanel;
import javax.swing.text.BadLocationException;
import org.mozilla.javascript.tools.debugger.FileTextArea;
import org.mozilla.javascript.tools.debugger.FileWindow;

class FileHeader extends JPanel implements MouseListener {
	private static final long serialVersionUID = -2858905404778259127L;
	private int pressLine = -1;
	private FileWindow fileWindow;

	public FileHeader(FileWindow fileWindow) {
		this.fileWindow = fileWindow;
		this.addMouseListener(this);
		this.update();
	}

	public void update() {
		FileTextArea textArea = this.fileWindow.textArea;
		Font font = textArea.getFont();
		this.setFont(font);
		FontMetrics metrics = this.getFontMetrics(font);
		int h = metrics.getHeight();
		int lineCount = textArea.getLineCount() + 1;
		String dummy = Integer.toString(lineCount);
		if (dummy.length() < 2) {
			dummy = "99";
		}

		Dimension d = new Dimension();
		d.width = metrics.stringWidth(dummy) + 16;
		d.height = lineCount * h + 100;
		this.setPreferredSize(d);
		this.setSize(d);
	}

	public void paint(Graphics g) {
		super.paint(g);
		FileTextArea textArea = this.fileWindow.textArea;
		Font font = textArea.getFont();
		g.setFont(font);
		FontMetrics metrics = this.getFontMetrics(font);
		Rectangle clip = g.getClipBounds();
		g.setColor(this.getBackground());
		g.fillRect(clip.x, clip.y, clip.width, clip.height);
		int ascent = metrics.getMaxAscent();
		int h = metrics.getHeight();
		int lineCount = textArea.getLineCount() + 1;
		String dummy = Integer.toString(lineCount);
		if (dummy.length() < 2) {
			dummy = "99";
		}

		int startLine = clip.y / h;
		int endLine = (clip.y + clip.height) / h + 1;
		int width = this.getWidth();
		if (endLine > lineCount) {
			endLine = lineCount;
		}

		for (int i = startLine; i < endLine; ++i) {
			int pos = -2;

			try {
				pos = textArea.getLineStartOffset(i);
			} catch (BadLocationException arg21) {
				;
			}

			boolean isBreakPoint = this.fileWindow.isBreakPoint(i + 1);
			String text = Integer.toString(i + 1) + " ";
			int y = i * h;
			g.setColor(Color.blue);
			g.drawString(text, 0, y + ascent);
			int x = width - ascent;
			if (isBreakPoint) {
				g.setColor(new Color(128, 0, 0));
				int arrow = y + ascent - 9;
				g.fillOval(x, arrow, 9, 9);
				g.drawOval(x, arrow, 8, 8);
				g.drawOval(x, arrow, 9, 9);
			}

			if (pos == this.fileWindow.currentPos) {
				Polygon arg22 = new Polygon();
				int dx = x;
				y += ascent - 10;
				arg22.addPoint(x, y + 3);
				arg22.addPoint(x + 5, y + 3);

				for (x += 5; x <= dx + 10; ++y) {
					arg22.addPoint(x, y);
					++x;
				}

				for (x = dx + 9; x >= dx + 5; ++y) {
					arg22.addPoint(x, y);
					--x;
				}

				arg22.addPoint(dx + 5, y + 7);
				arg22.addPoint(dx, y + 7);
				g.setColor(Color.yellow);
				g.fillPolygon(arg22);
				g.setColor(Color.black);
				g.drawPolygon(arg22);
			}
		}

	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
		Font font = this.fileWindow.textArea.getFont();
		FontMetrics metrics = this.getFontMetrics(font);
		int h = metrics.getHeight();
		this.pressLine = e.getY() / h;
	}

	public void mouseClicked(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mouseReleased(MouseEvent e) {
		if (e.getComponent() == this && (e.getModifiers() & 16) != 0) {
			int y = e.getY();
			Font font = this.fileWindow.textArea.getFont();
			FontMetrics metrics = this.getFontMetrics(font);
			int h = metrics.getHeight();
			int line = y / h;
			if (line == this.pressLine) {
				this.fileWindow.toggleBreakPoint(line + 1);
			} else {
				this.pressLine = -1;
			}
		}

	}
}