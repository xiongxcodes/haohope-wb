/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory.Listener;

public interface ContextListener extends Listener {

	void contextEntered(Context arg0);

	void contextExited(Context arg0);
}