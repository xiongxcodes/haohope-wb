/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdFunctionCall;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class IdFunctionObject extends BaseFunction {
	static final long serialVersionUID = -5332312783643935019L;
	private final IdFunctionCall idcall;
	private final Object tag;
	private final int methodId;
	private int arity;
	private boolean useCallAsConstructor;
	private String functionName;

	public IdFunctionObject(IdFunctionCall idcall, Object tag, int id, int arity) {
		if (arity < 0) {
			throw new IllegalArgumentException();
		} else {
			this.idcall = idcall;
			this.tag = tag;
			this.methodId = id;
			this.arity = arity;
			if (arity < 0) {
				throw new IllegalArgumentException();
			}
		}
	}

	public IdFunctionObject(IdFunctionCall idcall, Object tag, int id,
			String name, int arity, Scriptable scope) {
		super(scope, (Scriptable) null);
		if (arity < 0) {
			throw new IllegalArgumentException();
		} else if (name == null) {
			throw new IllegalArgumentException();
		} else {
			this.idcall = idcall;
			this.tag = tag;
			this.methodId = id;
			this.arity = arity;
			this.functionName = name;
		}
	}

	public void initFunction(String name, Scriptable scope) {
		if (name == null) {
			throw new IllegalArgumentException();
		} else if (scope == null) {
			throw new IllegalArgumentException();
		} else {
			this.functionName = name;
			this.setParentScope(scope);
		}
	}

	public final boolean hasTag(Object tag) {
		return this.tag == tag;
	}

	public final int methodId() {
		return this.methodId;
	}

	public final void markAsConstructor(Scriptable prototypeProperty) {
		this.useCallAsConstructor = true;
		this.setImmunePrototypeProperty(prototypeProperty);
	}

	public final void addAsProperty(Scriptable target) {
		ScriptableObject.defineProperty(target, this.functionName, this, 2);
	}

	public void exportAsScopeProperty() {
		this.addAsProperty(this.getParentScope());
	}

	public Scriptable getPrototype() {
		Scriptable proto = super.getPrototype();
		if (proto == null) {
			proto = getFunctionPrototype(this.getParentScope());
			this.setPrototype(proto);
		}

		return proto;
	}

	public Object call(Context cx, Scriptable scope, Scriptable thisObj,
			Object[] args) {
		return this.idcall.execIdCall(this, cx, scope, thisObj, args);
	}

	public Scriptable createObject(Context cx, Scriptable scope) {
		if (this.useCallAsConstructor) {
			return null;
		} else {
			throw ScriptRuntime.typeError1("msg.not.ctor", this.functionName);
		}
	}

	String decompile(int indent, int flags) {
		StringBuffer sb = new StringBuffer();
		boolean justbody = 0 != (flags & 1);
		if (!justbody) {
			sb.append("function ");
			sb.append(this.getFunctionName());
			sb.append("() { ");
		}

		sb.append("[native code for ");
		if (this.idcall instanceof Scriptable) {
			Scriptable sobj = (Scriptable) this.idcall;
			sb.append(sobj.getClassName());
			sb.append('.');
		}

		sb.append(this.getFunctionName());
		sb.append(", arity=");
		sb.append(this.getArity());
		sb.append(justbody ? "]\n" : "] }\n");
		return sb.toString();
	}

	public int getArity() {
		return this.arity;
	}

	public int getLength() {
		return this.getArity();
	}

	public String getFunctionName() {
		return this.functionName == null ? "" : this.functionName;
	}

	public final RuntimeException unknown() {
		return new IllegalArgumentException("BAD FUNCTION ID=" + this.methodId
				+ " MASTER=" + this.idcall);
	}
}