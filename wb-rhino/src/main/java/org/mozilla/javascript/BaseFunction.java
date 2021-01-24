/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.DefaultErrorReporter;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.Evaluator;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.NativeCall;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.UniqueTag;

public class BaseFunction extends IdScriptableObject implements Function {
	static final long serialVersionUID = 5311394446546053859L;
	private static final Object FUNCTION_TAG = "Function";
	private static final int Id_length = 1;
	private static final int Id_arity = 2;
	private static final int Id_name = 3;
	private static final int Id_prototype = 4;
	private static final int Id_arguments = 5;
	private static final int MAX_INSTANCE_ID = 5;
	private static final int Id_constructor = 1;
	private static final int Id_toString = 2;
	private static final int Id_toSource = 3;
	private static final int Id_apply = 4;
	private static final int Id_call = 5;
	private static final int MAX_PROTOTYPE_ID = 5;
	private Object prototypeProperty;
	private int prototypePropertyAttributes = 4;

	static void init(Scriptable scope, boolean sealed) {
		BaseFunction obj = new BaseFunction();
		obj.prototypePropertyAttributes = 7;
		obj.exportAsJSClass(5, scope, sealed);
	}

	public BaseFunction() {
	}

	public BaseFunction(Scriptable scope, Scriptable prototype) {
		super(scope, prototype);
	}

	public String getClassName() {
		return "Function";
	}

	public boolean hasInstance(Scriptable instance) {
		Object protoProp = ScriptableObject.getProperty(this, "prototype");
		if (protoProp instanceof Scriptable) {
			return ScriptRuntime
					.jsDelegatesTo(instance, (Scriptable) protoProp);
		} else {
			throw ScriptRuntime.typeError1("msg.instanceof.bad.prototype",
					this.getFunctionName());
		}
	}

	protected int getMaxInstanceId() {
		return 5;
	}

	protected int findInstanceIdInfo(String s) {
		byte id = 0;
		String attr = null;
		switch (s.length()) {
			case 4 :
				attr = "name";
				id = 3;
				break;
			case 5 :
				attr = "arity";
				id = 2;
				break;
			case 6 :
				attr = "length";
				id = 1;
			case 7 :
			case 8 :
			default :
				break;
			case 9 :
				char c = s.charAt(0);
				if (c == 97) {
					attr = "arguments";
					id = 5;
				} else if (c == 112) {
					attr = "prototype";
					id = 4;
				}
		}

		if (attr != null && attr != s && !attr.equals(s)) {
			id = 0;
		}

		if (id == 0) {
			return super.findInstanceIdInfo(s);
		} else {
			int attr1;
			switch (id) {
				case 1 :
				case 2 :
				case 3 :
					attr1 = 7;
					break;
				case 4 :
					attr1 = this.prototypePropertyAttributes;
					break;
				case 5 :
					attr1 = 6;
					break;
				default :
					throw new IllegalStateException();
			}

			return instanceIdInfo(attr1, id);
		}
	}

	protected String getInstanceIdName(int id) {
		switch (id) {
			case 1 :
				return "length";
			case 2 :
				return "arity";
			case 3 :
				return "name";
			case 4 :
				return "prototype";
			case 5 :
				return "arguments";
			default :
				return super.getInstanceIdName(id);
		}
	}

	protected Object getInstanceIdValue(int id) {
		switch (id) {
			case 1 :
				return ScriptRuntime.wrapInt(this.getLength());
			case 2 :
				return ScriptRuntime.wrapInt(this.getArity());
			case 3 :
				return this.getFunctionName();
			case 4 :
				return this.getPrototypeProperty();
			case 5 :
				return this.getArguments();
			default :
				return super.getInstanceIdValue(id);
		}
	}

	protected void setInstanceIdValue(int id, Object value) {
		if (id == 4) {
			if ((this.prototypePropertyAttributes & 1) == 0) {
				this.prototypeProperty = value != null
						? value
						: UniqueTag.NULL_VALUE;
			}

		} else {
			if (id == 5) {
				if (value == NOT_FOUND) {
					Kit.codeBug();
				}

				this.defaultPut("arguments", value);
			}

			super.setInstanceIdValue(id, value);
		}
	}

	protected void fillConstructorProperties(IdFunctionObject ctor) {
		ctor.setPrototype(this);
		super.fillConstructorProperties(ctor);
	}

	protected void initPrototypeId(int id) {
		String s;
		byte arity;
		switch (id) {
			case 1 :
				arity = 1;
				s = "constructor";
				break;
			case 2 :
				arity = 1;
				s = "toString";
				break;
			case 3 :
				arity = 1;
				s = "toSource";
				break;
			case 4 :
				arity = 2;
				s = "apply";
				break;
			case 5 :
				arity = 1;
				s = "call";
				break;
			default :
				throw new IllegalArgumentException(String.valueOf(id));
		}

		this.initPrototypeMethod(FUNCTION_TAG, id, s, arity);
	}

	static boolean isApply(IdFunctionObject f) {
		return f.hasTag(FUNCTION_TAG) && f.methodId() == 4;
	}

	static boolean isApplyOrCall(IdFunctionObject f) {
		if (f.hasTag(FUNCTION_TAG)) {
			switch (f.methodId()) {
				case 4 :
				case 5 :
					return true;
			}
		}

		return false;
	}

	public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
			Scriptable thisObj, Object[] args) {
		if (!f.hasTag(FUNCTION_TAG)) {
			return super.execIdCall(f, cx, scope, thisObj, args);
		} else {
			int id = f.methodId();
			BaseFunction realf;
			int indent;
			switch (id) {
				case 1 :
					return jsConstructor(cx, scope, args);
				case 2 :
					realf = this.realFunction(thisObj, f);
					indent = ScriptRuntime.toInt32(args, 0);
					return realf.decompile(indent, 0);
				case 3 :
					realf = this.realFunction(thisObj, f);
					indent = 0;
					byte flags = 2;
					if (args.length != 0) {
						indent = ScriptRuntime.toInt32(args[0]);
						if (indent >= 0) {
							flags = 0;
						} else {
							indent = 0;
						}
					}

					return realf.decompile(indent, flags);
				case 4 :
				case 5 :
					return ScriptRuntime.applyOrCall(id == 4, cx, scope,
							thisObj, args);
				default :
					throw new IllegalArgumentException(String.valueOf(id));
			}
		}
	}

	private BaseFunction realFunction(Scriptable thisObj, IdFunctionObject f) {
		Object x = thisObj.getDefaultValue(ScriptRuntime.FunctionClass);
		if (x instanceof BaseFunction) {
			return (BaseFunction) x;
		} else {
			throw ScriptRuntime.typeError1("msg.incompat.call",
					f.getFunctionName());
		}
	}

	public void setImmunePrototypeProperty(Object value) {
		if ((this.prototypePropertyAttributes & 1) != 0) {
			throw new IllegalStateException();
		} else {
			this.prototypeProperty = value != null
					? value
					: UniqueTag.NULL_VALUE;
			this.prototypePropertyAttributes = 7;
		}
	}

	protected Scriptable getClassPrototype() {
		Object protoVal = this.getPrototypeProperty();
		return protoVal instanceof Scriptable
				? (Scriptable) protoVal
				: getClassPrototype(this, "Object");
	}

	public Object call(Context cx, Scriptable scope, Scriptable thisObj,
			Object[] args) {
		return Undefined.instance;
	}

	public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
		Scriptable result = this.createObject(cx, scope);
		Object val;
		if (result != null) {
			val = this.call(cx, scope, result, args);
			if (val instanceof Scriptable) {
				result = (Scriptable) val;
			}
		} else {
			val = this.call(cx, scope, (Scriptable) null, args);
			if (!(val instanceof Scriptable)) {
				throw new IllegalStateException(
						"Bad implementaion of call as constructor, name="
								+ this.getFunctionName() + " in "
								+ this.getClass().getName());
			}

			result = (Scriptable) val;
			if (result.getPrototype() == null) {
				result.setPrototype(this.getClassPrototype());
			}

			if (result.getParentScope() == null) {
				Scriptable parent = this.getParentScope();
				if (result != parent) {
					result.setParentScope(parent);
				}
			}
		}

		return result;
	}

	public Scriptable createObject(Context cx, Scriptable scope) {
		NativeObject newInstance = new NativeObject();
		newInstance.setPrototype(this.getClassPrototype());
		newInstance.setParentScope(this.getParentScope());
		return newInstance;
	}

	String decompile(int indent, int flags) {
		StringBuffer sb = new StringBuffer();
		boolean justbody = 0 != (flags & 1);
		if (!justbody) {
			sb.append("function ");
			sb.append(this.getFunctionName());
			sb.append("() {\n\t");
		}

		sb.append("[native code, arity=");
		sb.append(this.getArity());
		sb.append("]\n");
		if (!justbody) {
			sb.append("}\n");
		}

		return sb.toString();
	}

	public int getArity() {
		return 0;
	}

	public int getLength() {
		return 0;
	}

	public String getFunctionName() {
		return "";
	}

	final Object getPrototypeProperty() {
		Object result = this.prototypeProperty;
		if (result == null) {
			synchronized (this) {
				result = this.prototypeProperty;
				if (result == null) {
					this.setupDefaultPrototype();
					result = this.prototypeProperty;
				}
			}
		} else if (result == UniqueTag.NULL_VALUE) {
			result = null;
		}

		return result;
	}

	private void setupDefaultPrototype() {
		NativeObject obj = new NativeObject();
		boolean attr = true;
		obj.defineProperty("constructor", this, 2);
		this.prototypeProperty = obj;
		Scriptable proto = getObjectPrototype(this);
		if (proto != obj) {
			obj.setPrototype(proto);
		}

	}

	private Object getArguments() {
		Object value = this.defaultGet("arguments");
		if (value != NOT_FOUND) {
			return value;
		} else {
			Context cx = Context.getContext();
			NativeCall activation = ScriptRuntime.findFunctionActivation(cx,
					this);
			return activation == null ? null : activation.get("arguments",
					activation);
		}
	}

	private static Object jsConstructor(Context cx, Scriptable scope,
			Object[] args) {
		int arglen = args.length;
		StringBuffer sourceBuf = new StringBuffer();
		sourceBuf.append("function ");
		if (cx.getLanguageVersion() != 120) {
			sourceBuf.append("anonymous");
		}

		sourceBuf.append('(');

		for (int source = 0; source < arglen - 1; ++source) {
			if (source > 0) {
				sourceBuf.append(',');
			}

			sourceBuf.append(ScriptRuntime.toString(args[source]));
		}

		sourceBuf.append(") {");
		String arg11;
		if (arglen != 0) {
			arg11 = ScriptRuntime.toString(args[arglen - 1]);
			sourceBuf.append(arg11);
		}

		sourceBuf.append('}');
		arg11 = sourceBuf.toString();
		int[] linep = new int[1];
		String filename = Context.getSourcePositionFromStack(linep);
		if (filename == null) {
			filename = "<eval\'ed string>";
			linep[0] = 1;
		}

		String sourceURI = ScriptRuntime.makeUrlForGeneratedScript(false,
				filename, linep[0]);
		Scriptable global = ScriptableObject.getTopLevelScope(scope);
		ErrorReporter reporter = DefaultErrorReporter.forEval(cx
				.getErrorReporter());
		Evaluator evaluator = Context.createInterpreter();
		if (evaluator == null) {
			throw new JavaScriptException("Interpreter not present", filename,
					linep[0]);
		} else {
			return cx.compileFunction(global, arg11, evaluator, reporter,
					sourceURI, 1, (Object) null);
		}
	}

	protected int findPrototypeId(String s) {
		byte id = 0;
		String X = null;
		switch (s.length()) {
			case 4 :
				X = "call";
				id = 5;
				break;
			case 5 :
				X = "apply";
				id = 4;
			case 6 :
			case 7 :
			case 9 :
			case 10 :
			default :
				break;
			case 8 :
				char c = s.charAt(3);
				if (c == 111) {
					X = "toSource";
					id = 3;
				} else if (c == 116) {
					X = "toString";
					id = 2;
				}
				break;
			case 11 :
				X = "constructor";
				id = 1;
		}

		if (X != null && X != s && !X.equals(s)) {
			id = 0;
		}

		return id;
	}
}