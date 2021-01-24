/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.xml;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.NativeWith;
import org.mozilla.javascript.Ref;
import org.mozilla.javascript.Scriptable;

public abstract class XMLObject extends IdScriptableObject {
	public XMLObject() {
	}

	public XMLObject(Scriptable scope, Scriptable prototype) {
		super(scope, prototype);
	}

	public abstract boolean ecmaHas(Context arg0, Object arg1);

	public abstract Object ecmaGet(Context arg0, Object arg1);

	public abstract void ecmaPut(Context arg0, Object arg1, Object arg2);

	public abstract boolean ecmaDelete(Context arg0, Object arg1);

	public abstract Scriptable getExtraMethodSource(Context arg0);

	public abstract Ref memberRef(Context arg0, Object arg1, int arg2);

	public abstract Ref memberRef(Context arg0, Object arg1, Object arg2,
			int arg3);

	public abstract NativeWith enterWith(Scriptable arg0);

	public abstract NativeWith enterDotQuery(Scriptable arg0);

	public Object addValues(Context cx, boolean thisIsLeft, Object value) {
		return Scriptable.NOT_FOUND;
	}
}