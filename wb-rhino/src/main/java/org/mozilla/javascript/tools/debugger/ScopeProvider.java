/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.tools.debugger;

import org.mozilla.javascript.Scriptable;

public interface ScopeProvider {
	Scriptable getScope();
}