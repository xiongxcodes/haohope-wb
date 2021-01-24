/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.MemberBox;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

public class FunctionObject extends BaseFunction {
	static final long serialVersionUID = -5332312783643935019L;
	private static final short VARARGS_METHOD = -1;
	private static final short VARARGS_CTOR = -2;
	private static boolean sawSecurityException;
	public static final int JAVA_UNSUPPORTED_TYPE = 0;
	public static final int JAVA_STRING_TYPE = 1;
	public static final int JAVA_INT_TYPE = 2;
	public static final int JAVA_BOOLEAN_TYPE = 3;
	public static final int JAVA_DOUBLE_TYPE = 4;
	public static final int JAVA_SCRIPTABLE_TYPE = 5;
	public static final int JAVA_OBJECT_TYPE = 6;
	MemberBox member;
	private String functionName;
	private transient byte[] typeTags;
	private int parmsLength;
	private transient boolean hasVoidReturn;
	private transient int returnTypeTag;
	private boolean isStatic;

	public FunctionObject(String name, Member methodOrConstructor,
			Scriptable scope) {
		if (methodOrConstructor instanceof Constructor) {
			this.member = new MemberBox((Constructor) methodOrConstructor);
			this.isStatic = true;
		} else {
			this.member = new MemberBox((Method) methodOrConstructor);
			this.isStatic = this.member.isStatic();
		}

		String methodName = this.member.getName();
		this.functionName = name;
		Class[] types = this.member.argTypes;
		int arity = types.length;
		if (arity != 4 || !types[1].isArray() && !types[2].isArray()) {
			this.parmsLength = arity;
			if (arity > 0) {
				this.typeTags = new byte[arity];

				for (int ctorType = 0; ctorType != arity; ++ctorType) {
					int returnType = getTypeTag(types[ctorType]);
					if (returnType == 0) {
						throw Context.reportRuntimeError2("msg.bad.parms",
								types[ctorType].getName(), methodName);
					}

					this.typeTags[ctorType] = (byte) returnType;
				}
			}
		} else if (types[1].isArray()) {
			if (!this.isStatic || types[0] != ScriptRuntime.ContextClass
					|| types[1].getComponentType() != ScriptRuntime.ObjectClass
					|| types[2] != ScriptRuntime.FunctionClass
					|| types[3] != Boolean.TYPE) {
				throw Context.reportRuntimeError1("msg.varargs.ctor",
						methodName);
			}

			this.parmsLength = -2;
		} else {
			if (!this.isStatic || types[0] != ScriptRuntime.ContextClass
					|| types[1] != ScriptRuntime.ScriptableClass
					|| types[2].getComponentType() != ScriptRuntime.ObjectClass
					|| types[3] != ScriptRuntime.FunctionClass) {
				throw Context
						.reportRuntimeError1("msg.varargs.fun", methodName);
			}

			this.parmsLength = -1;
		}

		if (this.member.isMethod()) {
			Method arg8 = this.member.method();
			Class arg10 = arg8.getReturnType();
			if (arg10 == Void.TYPE) {
				this.hasVoidReturn = true;
			} else {
				this.returnTypeTag = getTypeTag(arg10);
			}
		} else {
			Class arg9 = this.member.getDeclaringClass();
			if (!ScriptRuntime.ScriptableClass.isAssignableFrom(arg9)) {
				throw Context.reportRuntimeError1("msg.bad.ctor.return",
						arg9.getName());
			}
		}

		ScriptRuntime.setFunctionProtoAndParent(this, scope);
	}

	public static int getTypeTag(Class<?> type) {
		return type == ScriptRuntime.StringClass
				? 1
				: (type != ScriptRuntime.IntegerClass && type != Integer.TYPE
						? (type != ScriptRuntime.BooleanClass
								&& type != Boolean.TYPE
								? (type != ScriptRuntime.DoubleClass
										&& type != Double.TYPE
										? (ScriptRuntime.ScriptableClass
												.isAssignableFrom(type)
												? 5
												: (type == ScriptRuntime.ObjectClass
														? 6
														: 0))
										: 4)
								: 3)
						: 2);
	}

	public static Object convertArg(Context cx, Scriptable scope, Object arg,
			int typeTag) {
		switch (typeTag) {
			case 1 :
				if (arg instanceof String) {
					return arg;
				}

				return ScriptRuntime.toString(arg);
			case 2 :
				if (arg instanceof Integer) {
					return arg;
				}

				return new Integer(ScriptRuntime.toInt32(arg));
			case 3 :
				if (arg instanceof Boolean) {
					return arg;
				}

				return ScriptRuntime.toBoolean(arg)
						? Boolean.TRUE
						: Boolean.FALSE;
			case 4 :
				if (arg instanceof Double) {
					return arg;
				}

				return new Double(ScriptRuntime.toNumber(arg));
			case 5 :
				return ScriptRuntime.toObjectOrNull(cx, arg, scope);
			case 6 :
				return arg;
			default :
				throw new IllegalArgumentException();
		}
	}

	public int getArity() {
		return this.parmsLength < 0 ? 1 : this.parmsLength;
	}

	public int getLength() {
		return this.getArity();
	}

	public String getFunctionName() {
		return this.functionName == null ? "" : this.functionName;
	}

	public Member getMethodOrConstructor() {
		return (Member) (this.member.isMethod()
				? this.member.method()
				: this.member.ctor());
	}

	static Method findSingleMethod(Method[] methods, String name) {
		Method found = null;
		int i = 0;

		for (int N = methods.length; i != N; ++i) {
			Method method = methods[i];
			if (method != null && name.equals(method.getName())) {
				if (found != null) {
					throw Context.reportRuntimeError2("msg.no.overload", name,
							method.getDeclaringClass().getName());
				}

				found = method;
			}
		}

		return found;
	}

	static Method[] getMethodList(Class<?> clazz) {
		Method[] methods = null;

		try {
			if (!sawSecurityException) {
				methods = clazz.getDeclaredMethods();
			}
		} catch (SecurityException arg5) {
			sawSecurityException = true;
		}

		if (methods == null) {
			methods = clazz.getMethods();
		}

		int count = 0;

		for (int result = 0; result < methods.length; ++result) {
			label47 : {
				if (sawSecurityException) {
					if (methods[result].getDeclaringClass() != clazz) {
						break label47;
					}
				} else if (!Modifier.isPublic(methods[result].getModifiers())) {
					break label47;
				}

				++count;
				continue;
			}

			methods[result] = null;
		}

		Method[] arg6 = new Method[count];
		int j = 0;

		for (int i = 0; i < methods.length; ++i) {
			if (methods[i] != null) {
				arg6[j++] = methods[i];
			}
		}

		return arg6;
	}

	public void addAsConstructor(Scriptable scope, Scriptable prototype) {
		this.initAsConstructor(scope, prototype);
		defineProperty(scope, prototype.getClassName(), this, 2);
	}

	void initAsConstructor(Scriptable scope, Scriptable prototype) {
		ScriptRuntime.setFunctionProtoAndParent(this, scope);
		this.setImmunePrototypeProperty(prototype);
		prototype.setParentScope(this);
		defineProperty(prototype, "constructor", this, 7);
		this.setParentScope(scope);
	}

	public static Object convertArg(Context cx, Scriptable scope, Object arg,
			Class<?> desired) {
		int tag = getTypeTag(desired);
		if (tag == 0) {
			throw Context.reportRuntimeError1("msg.cant.convert",
					desired.getName());
		} else {
			return convertArg(cx, scope, arg, tag);
		}
	}

	public Object call(Context cx, Scriptable scope, Scriptable thisObj,
			Object[] args) {
		boolean checkMethodResult = false;
		Object result;
		Object[] invokeArgs;
		if (this.parmsLength < 0) {
			if (this.parmsLength == -1) {
				invokeArgs = new Object[]{cx, thisObj, args, this};
				result = this.member.invoke((Object) null, invokeArgs);
				checkMethodResult = true;
			} else {
				boolean arg10 = thisObj == null;
				Boolean i = arg10 ? Boolean.TRUE : Boolean.FALSE;
				Object[] arg = new Object[]{cx, args, this, i};
				result = this.member.isCtor()
						? this.member.newInstance(arg)
						: this.member.invoke((Object) null, arg);
			}
		} else {
			if (!this.isStatic) {
				Class arg11 = this.member.getDeclaringClass();
				if (!arg11.isInstance(thisObj)) {
					boolean arg12 = false;
					if (thisObj == scope) {
						Scriptable arg13 = this.getParentScope();
						if (scope != arg13) {
							arg12 = arg11.isInstance(arg13);
							if (arg12) {
								thisObj = arg13;
							}
						}
					}

					if (!arg12) {
						throw ScriptRuntime.typeError1("msg.incompat.call",
								this.functionName);
					}
				}
			}

			int arg14;
			Object arg15;
			if (this.parmsLength == args.length) {
				invokeArgs = args;

				for (arg14 = 0; arg14 != this.parmsLength; ++arg14) {
					arg15 = args[arg14];
					Object converted = convertArg(cx, scope, arg15,
							this.typeTags[arg14]);
					if (arg15 != converted) {
						if (invokeArgs == args) {
							invokeArgs = (Object[]) args.clone();
						}

						invokeArgs[arg14] = converted;
					}
				}
			} else if (this.parmsLength == 0) {
				invokeArgs = ScriptRuntime.emptyArgs;
			} else {
				invokeArgs = new Object[this.parmsLength];

				for (arg14 = 0; arg14 != this.parmsLength; ++arg14) {
					arg15 = arg14 < args.length
							? args[arg14]
							: Undefined.instance;
					invokeArgs[arg14] = convertArg(cx, scope, arg15,
							this.typeTags[arg14]);
				}
			}

			if (this.member.isMethod()) {
				result = this.member.invoke(thisObj, invokeArgs);
				checkMethodResult = true;
			} else {
				result = this.member.newInstance(invokeArgs);
			}
		}

		if (checkMethodResult) {
			if (this.hasVoidReturn) {
				result = Undefined.instance;
			} else if (this.returnTypeTag == 0) {
				result = cx.getWrapFactory().wrap(cx, scope, result,
						(Class) null);
			}
		}

		return result;
	}

	public Scriptable createObject(Context cx, Scriptable scope) {
		if (!this.member.isCtor() && this.parmsLength != -2) {
			Scriptable result;
			try {
				result = (Scriptable) this.member.getDeclaringClass()
						.newInstance();
			} catch (Exception arg4) {
				throw Context.throwAsScriptRuntimeEx(arg4);
			}

			result.setPrototype(this.getClassPrototype());
			result.setParentScope(this.getParentScope());
			return result;
		} else {
			return null;
		}
	}

	boolean isVarArgsMethod() {
		return this.parmsLength == -1;
	}

	boolean isVarArgsConstructor() {
		return this.parmsLength == -2;
	}

	private void readObject(ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		in.defaultReadObject();
		if (this.parmsLength > 0) {
			Class[] method = this.member.argTypes;
			this.typeTags = new byte[this.parmsLength];

			for (int returnType = 0; returnType != this.parmsLength; ++returnType) {
				this.typeTags[returnType] = (byte) getTypeTag(method[returnType]);
			}
		}

		if (this.member.isMethod()) {
			Method arg3 = this.member.method();
			Class arg4 = arg3.getReturnType();
			if (arg4 == Void.TYPE) {
				this.hasVoidReturn = true;
			} else {
				this.returnTypeTag = getTypeTag(arg4);
			}
		}

	}
}