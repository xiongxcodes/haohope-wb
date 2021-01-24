/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.tools.shell;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;

class Runner implements Runnable, ContextAction {
	ContextFactory factory;
	private Scriptable scope;
	private Function f;
	private Script s;
	private Object[] args;

	Runner(Scriptable scope, Function func, Object[] args) {
		this.scope = scope;
		this.f = func;
		this.args = args;
	}

	Runner(Scriptable scope, Script script) {
		this.scope = scope;
		this.s = script;
	}

	public void run() {
		this.factory.call(this);
	}

	public Object run(Context cx) {
		return this.f != null ? this.f.call(cx, this.scope, this.scope,
				this.args) : this.s.exec(cx, this.scope);
	}
}