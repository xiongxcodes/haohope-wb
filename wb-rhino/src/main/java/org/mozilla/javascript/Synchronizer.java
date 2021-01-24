/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Delegator;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;

public class Synchronizer extends Delegator {
	public Synchronizer(Scriptable obj) {
		super(obj);
	}

	public Object call(Context cx, Scriptable scope, Scriptable thisObj,
			Object[] args) {
		synchronized (thisObj instanceof Wrapper
				? ((Wrapper) thisObj).unwrap()
				: thisObj) {
			return ((Function) this.obj).call(cx, scope, thisObj, args);
		}
	}
}