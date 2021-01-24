/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.Interpreter;
import org.mozilla.javascript.Scriptable;

public final class NativeContinuation extends IdScriptableObject
		implements
			Function {
	static final long serialVersionUID = 1794167133757605367L;
	private static final Object FTAG = "Continuation";
	private Object implementation;
	private static final int Id_constructor = 1;
	private static final int MAX_PROTOTYPE_ID = 1;

	public static void init(Context cx, Scriptable scope, boolean sealed) {
		NativeContinuation obj = new NativeContinuation();
		obj.exportAsJSClass(1, scope, sealed);
	}

	public Object getImplementation() {
		return this.implementation;
	}

	public void initImplementation(Object implementation) {
		this.implementation = implementation;
	}

	public String getClassName() {
		return "Continuation";
	}

	public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
		throw Context.reportRuntimeError("Direct call is not supported");
	}

	public Object call(Context cx, Scriptable scope, Scriptable thisObj,
			Object[] args) {
		return Interpreter.restartContinuation(this, cx, scope, args);
	}

	public static boolean isContinuationConstructor(IdFunctionObject f) {
		return f.hasTag(FTAG) && f.methodId() == 1;
	}

	protected void initPrototypeId(int id) {
		switch (id) {
			case 1 :
				byte arity = 0;
				String s = "constructor";
				this.initPrototypeMethod(FTAG, id, s, arity);
				return;
			default :
				throw new IllegalArgumentException(String.valueOf(id));
		}
	}

	public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
			Scriptable thisObj, Object[] args) {
		if (!f.hasTag(FTAG)) {
			return super.execIdCall(f, cx, scope, thisObj, args);
		} else {
			int id = f.methodId();
			switch (id) {
				case 1 :
					throw Context
							.reportRuntimeError("Direct call is not supported");
				default :
					throw new IllegalArgumentException(String.valueOf(id));
			}
		}
	}

	protected int findPrototypeId(String s) {
		byte id = 0;
		String X = null;
		if (s.length() == 11) {
			X = "constructor";
			id = 1;
		}

		if (X != null && X != s && !X.equals(s)) {
			id = 0;
		}

		return id;
	}
}