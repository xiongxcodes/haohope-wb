/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;

public class Delegator implements Function {
	protected Scriptable obj = null;

	public Delegator() {
	}

	public Delegator(Scriptable obj) {
		this.obj = obj;
	}

	protected Delegator newInstance() {
		try {
			return (Delegator) this.getClass().newInstance();
		} catch (Exception arg1) {
			throw Context.throwAsScriptRuntimeEx(arg1);
		}
	}

	public Scriptable getDelegee() {
		return this.obj;
	}

	public void setDelegee(Scriptable obj) {
		this.obj = obj;
	}

	public String getClassName() {
		return this.obj.getClassName();
	}

	public Object get(String name, Scriptable start) {
		return this.obj.get(name, start);
	}

	public Object get(int index, Scriptable start) {
		return this.obj.get(index, start);
	}

	public boolean has(String name, Scriptable start) {
		return this.obj.has(name, start);
	}

	public boolean has(int index, Scriptable start) {
		return this.obj.has(index, start);
	}

	public void put(String name, Scriptable start, Object value) {
		this.obj.put(name, start, value);
	}

	public void put(int index, Scriptable start, Object value) {
		this.obj.put(index, start, value);
	}

	public void delete(String name) {
		this.obj.delete(name);
	}

	public void delete(int index) {
		this.obj.delete(index);
	}

	public Scriptable getPrototype() {
		return this.obj.getPrototype();
	}

	public void setPrototype(Scriptable prototype) {
		this.obj.setPrototype(prototype);
	}

	public Scriptable getParentScope() {
		return this.obj.getParentScope();
	}

	public void setParentScope(Scriptable parent) {
		this.obj.setParentScope(parent);
	}

	public Object[] getIds() {
		return this.obj.getIds();
	}

	public Object getDefaultValue(Class<?> hint) {
		return hint != null && hint != ScriptRuntime.ScriptableClass
				&& hint != ScriptRuntime.FunctionClass ? this.obj
				.getDefaultValue(hint) : this;
	}

	public boolean hasInstance(Scriptable instance) {
		return this.obj.hasInstance(instance);
	}

	public Object call(Context cx, Scriptable scope, Scriptable thisObj,
			Object[] args) {
		return ((Function) this.obj).call(cx, scope, thisObj, args);
	}

	public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
		if (this.obj == null) {
			Delegator n = this.newInstance();
			Object delegee;
			if (args.length == 0) {
				delegee = new NativeObject();
			} else {
				delegee = ScriptRuntime.toObject(cx, scope, args[0]);
			}

			n.setDelegee((Scriptable) delegee);
			return n;
		} else {
			return ((Function) this.obj).construct(cx, scope, args);
		}
	}
}