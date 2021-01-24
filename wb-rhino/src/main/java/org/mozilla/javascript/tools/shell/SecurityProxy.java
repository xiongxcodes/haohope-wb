/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.tools.shell;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.SecurityController;

public abstract class SecurityProxy extends SecurityController {
	protected abstract void callProcessFileSecure(Context arg0,
			Scriptable arg1, String arg2);
}