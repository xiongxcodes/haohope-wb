/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import java.io.Serializable;
import org.mozilla.javascript.Context;

public abstract class Ref implements Serializable {
	public boolean has(Context cx) {
		return true;
	}

	public abstract Object get(Context arg0);

	public abstract Object set(Context arg0, Object arg1);

	public boolean delete(Context cx) {
		return false;
	}
}