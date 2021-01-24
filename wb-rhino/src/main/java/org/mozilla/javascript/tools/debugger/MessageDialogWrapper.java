/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.tools.debugger;

import java.awt.Component;
import javax.swing.JOptionPane;

class MessageDialogWrapper {
	public static void showMessageDialog(Component parent, String msg,
			String title, int flags) {
		if (msg.length() > 60) {
			StringBuffer buf = new StringBuffer();
			int len = msg.length();
			int j = 0;

			for (int i = 0; i < len; ++j) {
				char c = msg.charAt(i);
				buf.append(c);
				if (Character.isWhitespace(c)) {
					int k;
					for (k = i + 1; k < len
							&& !Character.isWhitespace(msg.charAt(k)); ++k) {
						;
					}

					if (k < len) {
						int nextWordLen = k - i;
						if (j + nextWordLen > 60) {
							buf.append('\n');
							j = 0;
						}
					}
				}

				++i;
			}

			msg = buf.toString();
		}

		JOptionPane.showMessageDialog(parent, msg, title, flags);
	}
}