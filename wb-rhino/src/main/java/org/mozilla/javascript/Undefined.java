/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import java.io.Serializable;

public class Undefined implements Serializable {
	static final long serialVersionUID = 9195680630202616767L;
	public static final Object instance = new Undefined();

	public Object readResolve() {
		return instance;
	}
}