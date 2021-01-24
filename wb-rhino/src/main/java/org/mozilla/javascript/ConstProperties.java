/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import org.mozilla.javascript.Scriptable;

public interface ConstProperties {
	void putConst(String arg0, Scriptable arg1, Object arg2);

	void defineConst(String arg0, Scriptable arg1);

	boolean isConst(String arg0);
}