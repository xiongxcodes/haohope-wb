/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import java.util.Iterator;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeGenerator;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.VMBridge;

public final class NativeIterator extends IdScriptableObject {
	private static final long serialVersionUID = -4136968203581667681L;
	private static final Object ITERATOR_TAG = "Iterator";
	private static final String STOP_ITERATION = "StopIteration";
	public static final String ITERATOR_PROPERTY_NAME = "__iterator__";
	private static final int Id_constructor = 1;
	private static final int Id_next = 2;
	private static final int Id___iterator__ = 3;
	private static final int MAX_PROTOTYPE_ID = 3;
	private Object objectIterator;

	static void init(ScriptableObject scope, boolean sealed) {
		NativeIterator iterator = new NativeIterator();
		iterator.exportAsJSClass(3, scope, sealed);
		NativeGenerator.init(scope, sealed);
		NativeIterator.StopIteration obj = new NativeIterator.StopIteration();
		obj.setPrototype(getObjectPrototype(scope));
		obj.setParentScope(scope);
		if (sealed) {
			obj.sealObject();
		}

		ScriptableObject.defineProperty(scope, "StopIteration", obj, 2);
		scope.associateValue(ITERATOR_TAG, obj);
	}

	private NativeIterator() {
	}

	private NativeIterator(Object objectIterator) {
		this.objectIterator = objectIterator;
	}

	public static Object getStopIterationObject(Scriptable scope) {
		Scriptable top = ScriptableObject.getTopLevelScope(scope);
		return ScriptableObject.getTopScopeValue(top, ITERATOR_TAG);
	}

	public String getClassName() {
		return "Iterator";
	}

	protected void initPrototypeId(int id) {
		String s;
		byte arity;
		switch (id) {
			case 1 :
				arity = 2;
				s = "constructor";
				break;
			case 2 :
				arity = 0;
				s = "next";
				break;
			case 3 :
				arity = 1;
				s = "__iterator__";
				break;
			default :
				throw new IllegalArgumentException(String.valueOf(id));
		}

		this.initPrototypeMethod(ITERATOR_TAG, id, s, arity);
	}

	public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
			Scriptable thisObj, Object[] args) {
		if (!f.hasTag(ITERATOR_TAG)) {
			return super.execIdCall(f, cx, scope, thisObj, args);
		} else {
			int id = f.methodId();
			if (id == 1) {
				return jsConstructor(cx, scope, thisObj, args);
			} else if (!(thisObj instanceof NativeIterator)) {
				throw incompatibleCallError(f);
			} else {
				NativeIterator iterator = (NativeIterator) thisObj;
				switch (id) {
					case 2 :
						return iterator.next(cx, scope);
					case 3 :
						return thisObj;
					default :
						throw new IllegalArgumentException(String.valueOf(id));
				}
			}
		}
	}

	private static Object jsConstructor(Context cx, Scriptable scope,
			Scriptable thisObj, Object[] args) {
		if (args.length != 0 && args[0] != null
				&& args[0] != Undefined.instance) {
			Scriptable obj = ScriptRuntime.toObject(scope, args[0]);
			boolean keyOnly = args.length > 1
					&& ScriptRuntime.toBoolean(args[1]);
			if (thisObj != null) {
				Iterator objectIterator = VMBridge.instance.getJavaIterator(cx,
						scope, obj);
				if (objectIterator != null) {
					scope = ScriptableObject.getTopLevelScope(scope);
					return cx.getWrapFactory().wrap(
							cx,
							scope,
							new NativeIterator.WrappedJavaIterator(
									objectIterator, scope),
							NativeIterator.WrappedJavaIterator.class);
				}

				Scriptable result = ScriptRuntime.toIterator(cx, scope, obj,
						keyOnly);
				if (result != null) {
					return result;
				}
			}

			Object objectIterator1 = ScriptRuntime.enumInit(obj, cx, keyOnly
					? 3
					: 5);
			ScriptRuntime.setEnumNumbers(objectIterator1, true);
			NativeIterator result1 = new NativeIterator(objectIterator1);
			result1.setPrototype(getClassPrototype(scope,
					result1.getClassName()));
			result1.setParentScope(scope);
			return result1;
		} else {
			throw ScriptRuntime.typeError1("msg.no.properties",
					ScriptRuntime.toString(args[0]));
		}
	}

	private Object next(Context cx, Scriptable scope) {
		Boolean b = ScriptRuntime.enumNext(this.objectIterator);
		if (!b.booleanValue()) {
			throw new JavaScriptException(getStopIterationObject(scope),
					(String) null, 0);
		} else {
			return ScriptRuntime.enumId(this.objectIterator, cx);
		}
	}

	protected int findPrototypeId(String s) {
		byte id = 0;
		String X = null;
		int s_length = s.length();
		if (s_length == 4) {
			X = "next";
			id = 2;
		} else if (s_length == 11) {
			X = "constructor";
			id = 1;
		} else if (s_length == 12) {
			X = "__iterator__";
			id = 3;
		}

		if (X != null && X != s && !X.equals(s)) {
			id = 0;
		}

		return id;
	}

	public static class WrappedJavaIterator {
		private Iterator<?> iterator;
		private Scriptable scope;

		WrappedJavaIterator(Iterator<?> iterator, Scriptable scope) {
			this.iterator = iterator;
			this.scope = scope;
		}

		public Object next() {
			if (!this.iterator.hasNext()) {
				throw new JavaScriptException(
						NativeIterator.getStopIterationObject(this.scope),
						(String) null, 0);
			} else {
				return this.iterator.next();
			}
		}

		public Object __iterator__(boolean b) {
			return this;
		}
	}

	static class StopIteration extends NativeObject {
		private static final long serialVersionUID = 2485151085722377663L;

		public String getClassName() {
			return "StopIteration";
		}

		public boolean hasInstance(Scriptable instance) {
			return instance instanceof NativeIterator.StopIteration;
		}
	}
}