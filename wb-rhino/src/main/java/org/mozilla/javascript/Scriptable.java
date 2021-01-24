/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import org.mozilla.javascript.UniqueTag;

public interface Scriptable {
	Object NOT_FOUND = UniqueTag.NOT_FOUND;

	String getClassName();

	Object get(String arg0, Scriptable arg1);

	Object get(int arg0, Scriptable arg1);

	boolean has(String arg0, Scriptable arg1);

	boolean has(int arg0, Scriptable arg1);

	void put(String arg0, Scriptable arg1, Object arg2);

	void put(int arg0, Scriptable arg1, Object arg2);

	void delete(String arg0);

	void delete(int arg0);

	Scriptable getPrototype();

	void setPrototype(Scriptable arg0);

	Scriptable getParentScope();

	void setParentScope(Scriptable arg0);

	Object[] getIds();

	Object getDefaultValue(Class<?> arg0);

	boolean hasInstance(Scriptable arg0);
}