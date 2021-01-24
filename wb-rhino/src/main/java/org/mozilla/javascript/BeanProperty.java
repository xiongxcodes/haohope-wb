/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import org.mozilla.javascript.MemberBox;
import org.mozilla.javascript.NativeJavaMethod;

class BeanProperty {
	MemberBox getter;
	MemberBox setter;
	NativeJavaMethod setters;

	BeanProperty(MemberBox getter, MemberBox setter, NativeJavaMethod setters) {
		this.getter = getter;
		this.setter = setter;
		this.setters = setters;
	}
}