/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.ConstProperties;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.FunctionObject;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.LazilyLoadedCtor;
import org.mozilla.javascript.MemberBox;
import org.mozilla.javascript.ObjToIntMap;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.Wrapper;
import org.mozilla.javascript.debug.DebuggableObject;

public abstract class ScriptableObject
		implements
			Scriptable,
			Serializable,
			DebuggableObject,
			ConstProperties {
	public static final int EMPTY = 0;
	public static final int READONLY = 1;
	public static final int DONTENUM = 2;
	public static final int PERMANENT = 4;
	public static final int UNINITIALIZED_CONST = 8;
	public static final int CONST = 13;
	private Scriptable prototypeObject;
	private Scriptable parentScopeObject;
	private static final ScriptableObject.Slot REMOVED = new ScriptableObject.Slot(
			(String) null, 0, 1);
	private transient ScriptableObject.Slot[] slots;
	private int count;
	private transient ScriptableObject.Slot firstAdded;
	private transient ScriptableObject.Slot lastAdded;
	private transient ScriptableObject.Slot lastAccess;
	private volatile Map<Object, Object> associatedValues;
	private static final int SLOT_QUERY = 1;
	private static final int SLOT_MODIFY = 2;
	private static final int SLOT_REMOVE = 3;
	private static final int SLOT_MODIFY_GETTER_SETTER = 4;
	private static final int SLOT_MODIFY_CONST = 5;

	static void checkValidAttributes(int attributes) {
		boolean mask = true;
		if ((attributes & -16) != 0) {
			throw new IllegalArgumentException(String.valueOf(attributes));
		}
	}

	public ScriptableObject() {
		this.lastAccess = REMOVED;
	}

	public ScriptableObject(Scriptable scope, Scriptable prototype) {
		this.lastAccess = REMOVED;
		if (scope == null) {
			throw new IllegalArgumentException();
		} else {
			this.parentScopeObject = scope;
			this.prototypeObject = prototype;
		}
	}

	public abstract String getClassName();

	public boolean has(String name, Scriptable start) {
		return null != this.getSlot(name, 0, 1);
	}

	public boolean has(int index, Scriptable start) {
		return null != this.getSlot((String) null, index, 1);
	}

	public Object get(String name, Scriptable start) {
		return this.getImpl(name, 0, start);
	}

	public Object get(int index, Scriptable start) {
		return this.getImpl((String) null, index, start);
	}

	public void put(String name, Scriptable start, Object value) {
		if (!this.putImpl(name, 0, start, value, 0)) {
			if (start == this) {
				throw Kit.codeBug();
			} else {
				start.put(name, start, value);
			}
		}
	}

	public void put(int index, Scriptable start, Object value) {
		if (!this.putImpl((String) null, index, start, value, 0)) {
			if (start == this) {
				throw Kit.codeBug();
			} else {
				start.put(index, start, value);
			}
		}
	}

	public void delete(String name) {
		this.checkNotSealed(name, 0);
		this.accessSlot(name, 0, 3);
	}

	public void delete(int index) {
		this.checkNotSealed((String) null, index);
		this.accessSlot((String) null, index, 3);
	}

	public void putConst(String name, Scriptable start, Object value) {
		if (!this.putImpl(name, 0, start, value, 1)) {
			if (start == this) {
				throw Kit.codeBug();
			} else {
				if (start instanceof ConstProperties) {
					((ConstProperties) start).putConst(name, start, value);
				} else {
					start.put(name, start, value);
				}

			}
		}
	}

	public void defineConst(String name, Scriptable start) {
		if (!this.putImpl(name, 0, start, Undefined.instance, 8)) {
			if (start == this) {
				throw Kit.codeBug();
			} else {
				if (start instanceof ConstProperties) {
					((ConstProperties) start).defineConst(name, start);
				}

			}
		}
	}

	public boolean isConst(String name) {
		ScriptableObject.Slot slot = this.getSlot(name, 0, 1);
		return slot == null ? false : (slot.getAttributes() & 5) == 5;
	}

	public final int getAttributes(String name, Scriptable start) {
		return this.getAttributes(name);
	}

	public final int getAttributes(int index, Scriptable start) {
		return this.getAttributes(index);
	}

	public final void setAttributes(String name, Scriptable start,
			int attributes) {
		this.setAttributes(name, attributes);
	}

	public void setAttributes(int index, Scriptable start, int attributes) {
		this.setAttributes(index, attributes);
	}

	public int getAttributes(String name) {
		return this.findAttributeSlot(name, 0, 1).getAttributes();
	}

	public int getAttributes(int index) {
		return this.findAttributeSlot((String) null, index, 1).getAttributes();
	}

	public void setAttributes(String name, int attributes) {
		this.checkNotSealed(name, 0);
		this.findAttributeSlot(name, 0, 2).setAttributes(attributes);
	}

	public void setAttributes(int index, int attributes) {
		this.checkNotSealed((String) null, index);
		this.findAttributeSlot((String) null, index, 2).setAttributes(
				attributes);
	}

	public void setGetterOrSetter(String name, int index,
			Callable getterOrSetter, boolean isSetter) {
		if (name != null && index != 0) {
			throw new IllegalArgumentException(name);
		} else {
			this.checkNotSealed(name, index);
			ScriptableObject.GetterSlot gslot = (ScriptableObject.GetterSlot) this
					.getSlot(name, index, 4);
			gslot.checkNotReadonly();
			if (isSetter) {
				gslot.setter = getterOrSetter;
			} else {
				gslot.getter = getterOrSetter;
			}

			gslot.value = Undefined.instance;
		}
	}

	public Object getGetterOrSetter(String name, int index, boolean isSetter) {
		if (name != null && index != 0) {
			throw new IllegalArgumentException(name);
		} else {
			ScriptableObject.Slot slot = this.getSlot(name, index, 1);
			if (slot == null) {
				return null;
			} else if (slot instanceof ScriptableObject.GetterSlot) {
				ScriptableObject.GetterSlot gslot = (ScriptableObject.GetterSlot) slot;
				Object result = isSetter ? gslot.setter : gslot.getter;
				return result != null ? result : Undefined.instance;
			} else {
				return Undefined.instance;
			}
		}
	}

	protected boolean isGetterOrSetter(String name, int index, boolean setter) {
		ScriptableObject.Slot slot = this.getSlot(name, index, 1);
		if (slot instanceof ScriptableObject.GetterSlot) {
			if (setter && ((ScriptableObject.GetterSlot) slot).setter != null) {
				return true;
			}

			if (!setter && ((ScriptableObject.GetterSlot) slot).getter != null) {
				return true;
			}
		}

		return false;
	}

	void addLazilyInitializedValue(String name, int index,
			LazilyLoadedCtor init, int attributes) {
		if (name != null && index != 0) {
			throw new IllegalArgumentException(name);
		} else {
			this.checkNotSealed(name, index);
			ScriptableObject.GetterSlot gslot = (ScriptableObject.GetterSlot) this
					.getSlot(name, index, 4);
			gslot.setAttributes(attributes);
			gslot.getter = null;
			gslot.setter = null;
			gslot.value = init;
		}
	}

	public Scriptable getPrototype() {
		return this.prototypeObject;
	}

	public void setPrototype(Scriptable m) {
		this.prototypeObject = m;
	}

	public Scriptable getParentScope() {
		return this.parentScopeObject;
	}

	public void setParentScope(Scriptable m) {
		this.parentScopeObject = m;
	}

	public Object[] getIds() {
		return this.getIds(false);
	}

	public Object[] getAllIds() {
		return this.getIds(true);
	}

	public Object getDefaultValue(Class<?> typeHint) {
		return getDefaultValue(this, typeHint);
	}

	public static Object getDefaultValue(Scriptable object, Class<?> typeHint) {
		Context cx = null;

		for (int arg = 0; arg < 2; ++arg) {
			boolean tryToString;
			if (typeHint == ScriptRuntime.StringClass) {
				tryToString = arg == 0;
			} else {
				tryToString = arg == 1;
			}

			String methodName;
			Object[] args;
			if (tryToString) {
				methodName = "toString";
				args = ScriptRuntime.emptyArgs;
			} else {
				methodName = "valueOf";
				args = new Object[1];
				String v;
				if (typeHint == null) {
					v = "undefined";
				} else if (typeHint == ScriptRuntime.StringClass) {
					v = "string";
				} else if (typeHint == ScriptRuntime.ScriptableClass) {
					v = "object";
				} else if (typeHint == ScriptRuntime.FunctionClass) {
					v = "function";
				} else if (typeHint != ScriptRuntime.BooleanClass
						&& typeHint != Boolean.TYPE) {
					if (typeHint != ScriptRuntime.NumberClass
							&& typeHint != ScriptRuntime.ByteClass
							&& typeHint != Byte.TYPE
							&& typeHint != ScriptRuntime.ShortClass
							&& typeHint != Short.TYPE
							&& typeHint != ScriptRuntime.IntegerClass
							&& typeHint != Integer.TYPE
							&& typeHint != ScriptRuntime.FloatClass
							&& typeHint != Float.TYPE
							&& typeHint != ScriptRuntime.DoubleClass
							&& typeHint != Double.TYPE) {
						throw Context.reportRuntimeError1("msg.invalid.type",
								typeHint.toString());
					}

					v = "number";
				} else {
					v = "boolean";
				}

				args[0] = v;
			}

			Object arg10 = getProperty(object, methodName);
			if (arg10 instanceof Function) {
				Function fun = (Function) arg10;
				if (cx == null) {
					cx = Context.getContext();
				}

				arg10 = fun.call(cx, fun.getParentScope(), object, args);
				if (arg10 != null) {
					if (!(arg10 instanceof Scriptable)) {
						return arg10;
					}

					if (typeHint == ScriptRuntime.ScriptableClass
							|| typeHint == ScriptRuntime.FunctionClass) {
						return arg10;
					}

					if (tryToString && arg10 instanceof Wrapper) {
						Object u = ((Wrapper) arg10).unwrap();
						if (u instanceof String) {
							return u;
						}
					}
				}
			}
		}

		String arg9 = typeHint == null ? "undefined" : typeHint.getName();
		throw ScriptRuntime.typeError1("msg.default.value", arg9);
	}

	public boolean hasInstance(Scriptable instance) {
		return ScriptRuntime.jsDelegatesTo(instance, this);
	}

	public boolean avoidObjectDetection() {
		return false;
	}

	protected Object equivalentValues(Object value) {
		return this == value ? Boolean.TRUE : Scriptable.NOT_FOUND;
	}

	public static <T extends Scriptable> void defineClass(Scriptable scope,
			Class<T> clazz) throws IllegalAccessException,
			InstantiationException, InvocationTargetException {
		defineClass(scope, clazz, false, false);
	}

	public static <T extends Scriptable> void defineClass(Scriptable scope,
			Class<T> clazz, boolean sealed) throws IllegalAccessException,
			InstantiationException, InvocationTargetException {
		defineClass(scope, clazz, sealed, false);
	}

	public static <T extends Scriptable> String defineClass(Scriptable scope,
			Class<T> clazz, boolean sealed, boolean mapInheritance)
			throws IllegalAccessException, InstantiationException,
			InvocationTargetException {
		BaseFunction ctor = buildClassCtor(scope, clazz, sealed, mapInheritance);
		if (ctor == null) {
			return null;
		} else {
			String name = ctor.getClassPrototype().getClassName();
			defineProperty(scope, name, ctor, 2);
			return name;
		}
	}

	static <T extends Scriptable> BaseFunction buildClassCtor(Scriptable scope,
			Class<T> clazz, boolean sealed, boolean mapInheritance)
			throws IllegalAccessException, InstantiationException,
			InvocationTargetException {
		Method[] methods = FunctionObject.getMethodList(clazz);

		for (int ctors = 0; ctors < methods.length; ++ctors) {
			Method protoCtor = methods[ctors];
			if (protoCtor.getName().equals("init")) {
				Class[] proto = protoCtor.getParameterTypes();
				Object[] className;
				if (proto.length == 3 && proto[0] == ScriptRuntime.ContextClass
						&& proto[1] == ScriptRuntime.ScriptableClass
						&& proto[2] == Boolean.TYPE
						&& Modifier.isStatic(protoCtor.getModifiers())) {
					className = new Object[]{Context.getContext(), scope,
							sealed ? Boolean.TRUE : Boolean.FALSE};
					protoCtor.invoke((Object) null, className);
					return null;
				}

				if (proto.length == 1
						&& proto[0] == ScriptRuntime.ScriptableClass
						&& Modifier.isStatic(protoCtor.getModifiers())) {
					className = new Object[]{scope};
					protoCtor.invoke((Object) null, className);
					return null;
				}
			}
		}

		Constructor[] arg24 = clazz.getConstructors();
		Constructor arg25 = null;

		for (int arg26 = 0; arg26 < arg24.length; ++arg26) {
			if (arg24[arg26].getParameterTypes().length == 0) {
				arg25 = arg24[arg26];
				break;
			}
		}

		if (arg25 == null) {
			throw Context.reportRuntimeError1("msg.zero.arg.ctor",
					clazz.getName());
		} else {
			Scriptable arg27 = (Scriptable) arg25
					.newInstance(ScriptRuntime.emptyArgs);
			String arg28 = arg27.getClassName();
			Scriptable superProto = null;
			String getterPrefix;
			if (mapInheritance) {
				Class functionPrefix = clazz.getSuperclass();
				if (ScriptRuntime.ScriptableClass
						.isAssignableFrom(functionPrefix)
						&& !Modifier.isAbstract(functionPrefix.getModifiers())) {
					Class staticFunctionPrefix = extendsScriptable(functionPrefix);
					getterPrefix = defineClass(scope, staticFunctionPrefix,
							sealed, mapInheritance);
					if (getterPrefix != null) {
						superProto = getClassPrototype(scope, getterPrefix);
					}
				}
			}

			if (superProto == null) {
				superProto = getObjectPrototype(scope);
			}

			arg27.setPrototype(superProto);
			String arg29 = "jsFunction_";
			String arg30 = "jsStaticFunction_";
			getterPrefix = "jsGet_";
			String setterPrefix = "jsSet_";
			String ctorName = "jsConstructor";
			Object ctorMember = FunctionObject.findSingleMethod(methods,
					"jsConstructor");
			if (ctorMember == null) {
				if (arg24.length == 1) {
					ctorMember = arg24[0];
				} else if (arg24.length == 2) {
					if (arg24[0].getParameterTypes().length == 0) {
						ctorMember = arg24[1];
					} else if (arg24[1].getParameterTypes().length == 0) {
						ctorMember = arg24[0];
					}
				}

				if (ctorMember == null) {
					throw Context.reportRuntimeError1(
							"msg.ctor.multiple.parms", clazz.getName());
				}
			}

			FunctionObject ctor = new FunctionObject(arg28,
					(Member) ctorMember, scope);
			if (ctor.isVarArgsMethod()) {
				throw Context.reportRuntimeError1("msg.varargs.ctor",
						((Member) ctorMember).getName());
			} else {
				ctor.initAsConstructor(scope, arg27);
				Method finishInit = null;
				HashSet names = new HashSet(methods.length);

				for (int finishArgs = 0; finishArgs < methods.length; ++finishArgs) {
					if (methods[finishArgs] != ctorMember) {
						String name = methods[finishArgs].getName();
						Class[] prefix;
						if (name.equals("finishInit")) {
							prefix = methods[finishArgs].getParameterTypes();
							if (prefix.length == 3
									&& prefix[0] == ScriptRuntime.ScriptableClass
									&& prefix[1] == FunctionObject.class
									&& prefix[2] == ScriptRuntime.ScriptableClass
									&& Modifier.isStatic(methods[finishArgs]
											.getModifiers())) {
								finishInit = methods[finishArgs];
								continue;
							}
						}

						if (name.indexOf(36) == -1
								&& !name.equals("jsConstructor")) {
							prefix = null;
							String arg32;
							if (name.startsWith("jsFunction_")) {
								arg32 = "jsFunction_";
							} else if (name.startsWith("jsStaticFunction_")) {
								arg32 = "jsStaticFunction_";
								if (!Modifier.isStatic(methods[finishArgs]
										.getModifiers())) {
									throw Context
											.reportRuntimeError("jsStaticFunction must be used with static method.");
								}
							} else {
								if (!name.startsWith("jsGet_")) {
									continue;
								}

								arg32 = "jsGet_";
							}

							String propName = name.substring(arg32.length());
							if (names.contains(propName)) {
								throw Context.reportRuntimeError2(
										"duplicate.defineClass.name", name,
										propName);
							}

							names.add(propName);
							name = name.substring(arg32.length());
							if (arg32 == "jsGet_") {
								if (!(arg27 instanceof ScriptableObject)) {
									throw Context.reportRuntimeError2(
											"msg.extend.scriptable", arg27
													.getClass().toString(),
											name);
								}

								Method f = FunctionObject.findSingleMethod(
										methods, "jsSet_" + name);
								int dest = 6 | (f != null ? 0 : 1);
								((ScriptableObject) arg27).defineProperty(name,
										(Object) null, methods[finishArgs], f,
										dest);
							} else {
								FunctionObject arg33 = new FunctionObject(name,
										methods[finishArgs], arg27);
								if (arg33.isVarArgsConstructor()) {
									throw Context.reportRuntimeError1(
											"msg.varargs.fun",
											((Member) ctorMember).getName());
								}

								Object arg34 = arg32 == "jsStaticFunction_"
										? ctor
										: arg27;
								defineProperty((Scriptable) arg34, name, arg33,
										2);
								if (sealed) {
									arg33.sealObject();
								}
							}
						}
					}
				}

				if (finishInit != null) {
					Object[] arg31 = new Object[]{scope, ctor, arg27};
					finishInit.invoke((Object) null, arg31);
				}

				if (sealed) {
					ctor.sealObject();
					if (arg27 instanceof ScriptableObject) {
						((ScriptableObject) arg27).sealObject();
					}
				}

				return ctor;
			}
		}
	}

	private static <T extends Scriptable> Class<T> extendsScriptable(Class<T> c) {
		return ScriptRuntime.ScriptableClass.isAssignableFrom(c) ? c : null;
	}

	public void defineProperty(String propertyName, Object value, int attributes) {
		this.checkNotSealed(propertyName, 0);
		this.put(propertyName, this, value);
		this.setAttributes(propertyName, attributes);
	}

	public static void defineProperty(Scriptable destination,
			String propertyName, Object value, int attributes) {
		if (!(destination instanceof ScriptableObject)) {
			destination.put(propertyName, destination, value);
		} else {
			ScriptableObject so = (ScriptableObject) destination;
			so.defineProperty(propertyName, value, attributes);
		}
	}

	public static void defineConstProperty(Scriptable destination,
			String propertyName) {
		if (destination instanceof ConstProperties) {
			ConstProperties cp = (ConstProperties) destination;
			cp.defineConst(propertyName, destination);
		} else {
			defineProperty(destination, propertyName, Undefined.instance, 13);
		}

	}

	public void defineProperty(String propertyName, Class<?> clazz,
			int attributes) {
		int length = propertyName.length();
		if (length == 0) {
			throw new IllegalArgumentException();
		} else {
			char[] buf = new char[3 + length];
			propertyName.getChars(0, length, buf, 3);
			buf[3] = Character.toUpperCase(buf[3]);
			buf[0] = 103;
			buf[1] = 101;
			buf[2] = 116;
			String getterName = new String(buf);
			buf[0] = 115;
			String setterName = new String(buf);
			Method[] methods = FunctionObject.getMethodList(clazz);
			Method getter = FunctionObject
					.findSingleMethod(methods, getterName);
			Method setter = FunctionObject
					.findSingleMethod(methods, setterName);
			if (setter == null) {
				attributes |= 1;
			}

			this.defineProperty(propertyName, (Object) null, getter,
					setter == null ? null : setter, attributes);
		}
	}

	public void defineProperty(String propertyName, Object delegateTo,
			Method getter, Method setter, int attributes) {
		MemberBox getterBox = null;
		if (getter != null) {
			getterBox = new MemberBox(getter);
			boolean setterBox;
			if (!Modifier.isStatic(getter.getModifiers())) {
				setterBox = delegateTo != null;
				getterBox.delegateTo = delegateTo;
			} else {
				setterBox = true;
				getterBox.delegateTo = Void.TYPE;
			}

			String gslot = null;
			Class[] errorId = getter.getParameterTypes();
			if (errorId.length == 0) {
				if (setterBox) {
					gslot = "msg.obj.getter.parms";
				}
			} else if (errorId.length == 1) {
				Class parmTypes = errorId[0];
				if (parmTypes != ScriptRuntime.ScriptableClass
						&& parmTypes != ScriptRuntime.ScriptableObjectClass) {
					gslot = "msg.bad.getter.parms";
				} else if (!setterBox) {
					gslot = "msg.bad.getter.parms";
				}
			} else {
				gslot = "msg.bad.getter.parms";
			}

			if (gslot != null) {
				throw Context.reportRuntimeError1(gslot, getter.toString());
			}
		}

		MemberBox setterBox1 = null;
		if (setter != null) {
			if (setter.getReturnType() != Void.TYPE) {
				throw Context.reportRuntimeError1("msg.setter.return",
						setter.toString());
			}

			setterBox1 = new MemberBox(setter);
			boolean gslot1;
			if (!Modifier.isStatic(setter.getModifiers())) {
				gslot1 = delegateTo != null;
				setterBox1.delegateTo = delegateTo;
			} else {
				gslot1 = true;
				setterBox1.delegateTo = Void.TYPE;
			}

			String errorId1 = null;
			Class[] parmTypes1 = setter.getParameterTypes();
			if (parmTypes1.length == 1) {
				if (gslot1) {
					errorId1 = "msg.setter2.expected";
				}
			} else if (parmTypes1.length == 2) {
				Class argType = parmTypes1[0];
				if (argType != ScriptRuntime.ScriptableClass
						&& argType != ScriptRuntime.ScriptableObjectClass) {
					errorId1 = "msg.setter2.parms";
				} else if (!gslot1) {
					errorId1 = "msg.setter1.parms";
				}
			} else {
				errorId1 = "msg.setter.parms";
			}

			if (errorId1 != null) {
				throw Context.reportRuntimeError1(errorId1, setter.toString());
			}
		}

		ScriptableObject.GetterSlot gslot2 = (ScriptableObject.GetterSlot) this
				.getSlot(propertyName, 0, 4);
		gslot2.setAttributes(attributes);
		gslot2.getter = getterBox;
		gslot2.setter = setterBox1;
	}

	public void defineFunctionProperties(String[] names, Class<?> clazz,
			int attributes) {
		Method[] methods = FunctionObject.getMethodList(clazz);

		for (int i = 0; i < names.length; ++i) {
			String name = names[i];
			Method m = FunctionObject.findSingleMethod(methods, name);
			if (m == null) {
				throw Context.reportRuntimeError2("msg.method.not.found", name,
						clazz.getName());
			}

			FunctionObject f = new FunctionObject(name, m, this);
			this.defineProperty(name, (Object) f, attributes);
		}

	}

	public static Scriptable getObjectPrototype(Scriptable scope) {
		return getClassPrototype(scope, "Object");
	}

	public static Scriptable getFunctionPrototype(Scriptable scope) {
		return getClassPrototype(scope, "Function");
	}

	public static Scriptable getClassPrototype(Scriptable scope,
			String className) {
		scope = getTopLevelScope(scope);
		Object ctor = getProperty(scope, className);
		Object proto;
		if (ctor instanceof BaseFunction) {
			proto = ((BaseFunction) ctor).getPrototypeProperty();
		} else {
			if (!(ctor instanceof Scriptable)) {
				return null;
			}

			Scriptable ctorObj = (Scriptable) ctor;
			proto = ctorObj.get("prototype", ctorObj);
		}

		return proto instanceof Scriptable ? (Scriptable) proto : null;
	}

	public static Scriptable getTopLevelScope(Scriptable obj) {
		while (true) {
			Scriptable parent = obj.getParentScope();
			if (parent == null) {
				return obj;
			}

			obj = parent;
		}
	}

	public synchronized void sealObject() {
		if (this.count >= 0) {
			for (ScriptableObject.Slot slot = this.firstAdded; slot != null; slot = slot.orderedNext) {
				if (slot.value instanceof LazilyLoadedCtor) {
					LazilyLoadedCtor initializer = (LazilyLoadedCtor) slot.value;

					try {
						initializer.init();
					} finally {
						slot.value = initializer.getValue();
					}
				}
			}

			this.count = ~this.count;
		}

	}

	public final boolean isSealed() {
		return this.count < 0;
	}

	private void checkNotSealed(String name, int index) {
		if (this.isSealed()) {
			String str = name != null ? name : Integer.toString(index);
			throw Context.reportRuntimeError1("msg.modify.sealed", str);
		}
	}

	public static Object getProperty(Scriptable obj, String name) {
		Scriptable start = obj;

		Object result;
		do {
			result = obj.get(name, start);
			if (result != Scriptable.NOT_FOUND) {
				break;
			}

			obj = obj.getPrototype();
		} while (obj != null);

		return result;
	}

	public static Object getProperty(Scriptable obj, int index) {
		Scriptable start = obj;

		Object result;
		do {
			result = obj.get(index, start);
			if (result != Scriptable.NOT_FOUND) {
				break;
			}

			obj = obj.getPrototype();
		} while (obj != null);

		return result;
	}

	public static boolean hasProperty(Scriptable obj, String name) {
		return null != getBase(obj, name);
	}

	public static void redefineProperty(Scriptable obj, String name,
			boolean isConst) {
		Scriptable base = getBase(obj, name);
		if (base != null) {
			if (base instanceof ConstProperties) {
				ConstProperties cp = (ConstProperties) base;
				if (cp.isConst(name)) {
					throw Context.reportRuntimeError1("msg.const.redecl", name);
				}
			}

			if (isConst) {
				throw Context.reportRuntimeError1("msg.var.redecl", name);
			}
		}
	}

	public static boolean hasProperty(Scriptable obj, int index) {
		return null != getBase(obj, index);
	}

	public static void putProperty(Scriptable obj, String name, Object value) {
		Scriptable base = getBase(obj, name);
		if (base == null) {
			base = obj;
		}

		base.put(name, obj, value);
	}

	public static void putConstProperty(Scriptable obj, String name,
			Object value) {
		Scriptable base = getBase(obj, name);
		if (base == null) {
			base = obj;
		}

		if (base instanceof ConstProperties) {
			((ConstProperties) base).putConst(name, obj, value);
		}

	}

	public static void putProperty(Scriptable obj, int index, Object value) {
		Scriptable base = getBase(obj, index);
		if (base == null) {
			base = obj;
		}

		base.put(index, obj, value);
	}

	public static boolean deleteProperty(Scriptable obj, String name) {
		Scriptable base = getBase(obj, name);
		if (base == null) {
			return true;
		} else {
			base.delete(name);
			return !base.has(name, obj);
		}
	}

	public static boolean deleteProperty(Scriptable obj, int index) {
		Scriptable base = getBase(obj, index);
		if (base == null) {
			return true;
		} else {
			base.delete(index);
			return !base.has(index, obj);
		}
	}

	public static Object[] getPropertyIds(Scriptable obj) {
		if (obj == null) {
			return ScriptRuntime.emptyArgs;
		} else {
			Object[] result = obj.getIds();
			ObjToIntMap map = null;

			while (true) {
				while (true) {
					Object[] ids;
					do {
						obj = obj.getPrototype();
						if (obj == null) {
							if (map != null) {
								result = map.getKeys();
							}

							return result;
						}

						ids = obj.getIds();
					} while (ids.length == 0);

					int i;
					if (map == null) {
						if (result.length == 0) {
							result = ids;
							continue;
						}

						map = new ObjToIntMap(result.length + ids.length);

						for (i = 0; i != result.length; ++i) {
							map.intern(result[i]);
						}

						result = null;
					}

					for (i = 0; i != ids.length; ++i) {
						map.intern(ids[i]);
					}
				}
			}
		}
	}

	public static Object callMethod(Scriptable obj, String methodName,
			Object[] args) {
		return callMethod((Context) null, obj, methodName, args);
	}

	public static Object callMethod(Context cx, Scriptable obj,
			String methodName, Object[] args) {
		Object funObj = getProperty(obj, methodName);
		if (!(funObj instanceof Function)) {
			throw ScriptRuntime.notFunctionError(obj, methodName);
		} else {
			Function fun = (Function) funObj;
			Scriptable scope = getTopLevelScope(obj);
			return cx != null ? fun.call(cx, scope, obj, args) : Context.call(
					(ContextFactory) null, fun, scope, obj, args);
		}
	}

	private static Scriptable getBase(Scriptable obj, String name) {
		while (true) {
			if (!obj.has(name, obj)) {
				obj = obj.getPrototype();
				if (obj != null) {
					continue;
				}
			}

			return obj;
		}
	}

	private static Scriptable getBase(Scriptable obj, int index) {
		while (true) {
			if (!obj.has(index, obj)) {
				obj = obj.getPrototype();
				if (obj != null) {
					continue;
				}
			}

			return obj;
		}
	}

	public final Object getAssociatedValue(Object key) {
		Map h = this.associatedValues;
		return h == null ? null : h.get(key);
	}

	public static Object getTopScopeValue(Scriptable scope, Object key) {
		scope = getTopLevelScope(scope);

		do {
			if (scope instanceof ScriptableObject) {
				ScriptableObject so = (ScriptableObject) scope;
				Object value = so.getAssociatedValue(key);
				if (value != null) {
					return value;
				}
			}

			scope = scope.getPrototype();
		} while (scope != null);

		return null;
	}

	public final synchronized Object associateValue(Object key, Object value) {
		if (value == null) {
			throw new IllegalArgumentException();
		} else {
			Object h = this.associatedValues;
			if (h == null) {
				h = this.associatedValues;
				if (h == null) {
					h = new HashMap();
					this.associatedValues = (Map) h;
				}
			}

			return Kit.initHash((Map) h, key, value);
		}
	}

	private Object getImpl(String name, int index, Scriptable start) {
		ScriptableObject.Slot slot = this.getSlot(name, index, 1);
		if (slot == null) {
			return Scriptable.NOT_FOUND;
		} else if (!(slot instanceof ScriptableObject.GetterSlot)) {
			return slot.value;
		} else {
			Object getterObj = ((ScriptableObject.GetterSlot) slot).getter;
			if (getterObj != null) {
				if (getterObj instanceof MemberBox) {
					MemberBox value2 = (MemberBox) getterObj;
					Object[] args;
					Object initializer2;
					if (value2.delegateTo == null) {
						initializer2 = start;
						args = ScriptRuntime.emptyArgs;
					} else {
						initializer2 = value2.delegateTo;
						args = new Object[]{start};
					}

					return value2.invoke(initializer2, args);
				} else {
					Function value1 = (Function) getterObj;
					Context initializer1 = Context.getContext();
					return value1.call(initializer1, value1.getParentScope(),
							start, ScriptRuntime.emptyArgs);
				}
			} else {
				Object value = slot.value;
				if (value instanceof LazilyLoadedCtor) {
					LazilyLoadedCtor initializer = (LazilyLoadedCtor) value;

					try {
						initializer.init();
					} finally {
						value = initializer.getValue();
						slot.value = value;
					}
				}

				return value;
			}
		}
	}

	private boolean putImpl(String name, int index, Scriptable start,
			Object value, int constFlag) {
		ScriptableObject.Slot slot;
		if (this != start) {
			slot = this.getSlot(name, index, 1);
			if (slot == null) {
				return false;
			}
		} else {
			this.checkNotSealed(name, index);
			if (constFlag != 0) {
				slot = this.getSlot(name, index, 5);
				int setterObj1 = slot.getAttributes();
				if ((setterObj1 & 1) == 0) {
					throw Context.reportRuntimeError1("msg.var.redecl", name);
				}

				if ((setterObj1 & 8) != 0) {
					slot.value = value;
					if (constFlag != 8) {
						slot.setAttributes(setterObj1 & -9);
					}
				}

				return true;
			}

			slot = this.getSlot(name, index, 2);
		}

		if ((slot.getAttributes() & 1) != 0) {
			return true;
		} else {
			if (slot instanceof ScriptableObject.GetterSlot) {
				Object setterObj = ((ScriptableObject.GetterSlot) slot).setter;
				if (setterObj != null) {
					Context cx = Context.getContext();
					if (setterObj instanceof MemberBox) {
						MemberBox f = (MemberBox) setterObj;
						Class[] pTypes = f.argTypes;
						Class valueType = pTypes[pTypes.length - 1];
						int tag = FunctionObject.getTypeTag(valueType);
						Object actualArg = FunctionObject.convertArg(cx, start,
								value, tag);
						Object setterThis;
						Object[] args;
						if (f.delegateTo == null) {
							setterThis = start;
							args = new Object[]{actualArg};
						} else {
							setterThis = f.delegateTo;
							args = new Object[]{start, actualArg};
						}

						f.invoke(setterThis, args);
					} else {
						Function f1 = (Function) setterObj;
						f1.call(cx, f1.getParentScope(), start,
								new Object[]{value});
					}

					return true;
				}

				if (((ScriptableObject.GetterSlot) slot).getter != null) {
					throw ScriptRuntime.typeError1("msg.set.prop.no.setter",
							name);
				}
			}

			if (this == start) {
				slot.value = value;
				return true;
			} else {
				return false;
			}
		}
	}

	private ScriptableObject.Slot findAttributeSlot(String name, int index,
			int accessType) {
		ScriptableObject.Slot slot = this.getSlot(name, index, accessType);
		if (slot == null) {
			String str = name != null ? name : Integer.toString(index);
			throw Context.reportRuntimeError1("msg.prop.not.found", str);
		} else {
			return slot;
		}
	}

	private ScriptableObject.Slot getSlot(String name, int index, int accessType) {
		ScriptableObject.Slot slot;
		label35 : {
			slot = this.lastAccess;
			if (name != null) {
				if (name != slot.name) {
					break label35;
				}
			} else if (slot.name != null || index != slot.indexOrHash) {
				break label35;
			}

			if (!slot.wasDeleted
					&& (accessType != 4 || slot instanceof ScriptableObject.GetterSlot)) {
				return slot;
			}
		}

		slot = this.accessSlot(name, index, accessType);
		if (slot != null) {
			this.lastAccess = slot;
		}

		return slot;
	}

	private ScriptableObject.Slot accessSlot(String name, int index,
			int accessType) {
		int indexOrHash = name != null ? name.hashCode() : index;
		int tableSize;
		int arg16;
		if (accessType != 1 && accessType != 2 && accessType != 5
				&& accessType != 4) {
			if (accessType == 3) {
				synchronized (this) {
					ScriptableObject.Slot[] arg15 = this.slots;
					if (this.count != 0) {
						tableSize = this.slots.length;
						arg16 = getSlotIndex(tableSize, indexOrHash);
						ScriptableObject.Slot arg19 = arg15[arg16];

						ScriptableObject.Slot arg20;
						for (arg20 = arg19; arg20 != null
								&& (arg20.indexOrHash != indexOrHash || arg20.name != name
										&& (name == null || !name
												.equals(arg20.name))); arg20 = arg20.next) {
							arg19 = arg20;
						}

						if (arg20 != null && (arg20.getAttributes() & 4) == 0) {
							--this.count;
							if (arg19 == arg20) {
								arg15[arg16] = arg20.next;
							} else {
								arg19.next = arg20.next;
							}

							arg20.wasDeleted = true;
							arg20.value = null;
							arg20.name = null;
							if (arg20 == this.lastAccess) {
								this.lastAccess = REMOVED;
							}
						}
					}

					return null;
				}
			} else {
				throw Kit.codeBug();
			}
		} else {
			ScriptableObject.Slot[] slotsLocalRef = this.slots;
			if (slotsLocalRef == null) {
				if (accessType == 1) {
					return null;
				}
			} else {
				int slotsLocalRef1 = slotsLocalRef.length;
				tableSize = getSlotIndex(slotsLocalRef1, indexOrHash);

				ScriptableObject.Slot slotIndex;
				for (slotIndex = slotsLocalRef[tableSize]; slotIndex != null; slotIndex = slotIndex.next) {
					String prev = slotIndex.name;
					if (prev != null) {
						if (prev == name) {
							break;
						}

						if (name != null
								&& indexOrHash == slotIndex.indexOrHash
								&& name.equals(prev)) {
							slotIndex.name = name;
							break;
						}
					} else if (name == null
							&& indexOrHash == slotIndex.indexOrHash) {
						break;
					}
				}

				if (accessType == 1) {
					return slotIndex;
				}

				if (accessType == 2) {
					if (slotIndex != null) {
						return slotIndex;
					}
				} else if (accessType == 4) {
					if (slotIndex instanceof ScriptableObject.GetterSlot) {
						return slotIndex;
					}
				} else if (accessType == 5 && slotIndex != null) {
					return slotIndex;
				}
			}

			synchronized (this) {
				slotsLocalRef = this.slots;
				if (this.count == 0) {
					slotsLocalRef = new ScriptableObject.Slot[5];
					this.slots = slotsLocalRef;
					tableSize = getSlotIndex(slotsLocalRef.length, indexOrHash);
				} else {
					arg16 = slotsLocalRef.length;
					tableSize = getSlotIndex(arg16, indexOrHash);
					Object arg18 = slotsLocalRef[tableSize];

					Object slot;
					for (slot = arg18; slot != null
							&& (((ScriptableObject.Slot) slot).indexOrHash != indexOrHash || ((ScriptableObject.Slot) slot).name != name
									&& (name == null || !name
											.equals(((ScriptableObject.Slot) slot).name))); slot = ((ScriptableObject.Slot) slot).next) {
						arg18 = slot;
					}

					if (slot != null) {
						if (accessType == 4
								&& !(slot instanceof ScriptableObject.GetterSlot)) {
							ScriptableObject.GetterSlot newSlot = new ScriptableObject.GetterSlot(
									name, indexOrHash,
									((ScriptableObject.Slot) slot)
											.getAttributes());
							newSlot.value = ((ScriptableObject.Slot) slot).value;
							newSlot.next = ((ScriptableObject.Slot) slot).next;
							if (this.lastAdded != null) {
								this.lastAdded.orderedNext = newSlot;
							}

							if (this.firstAdded == null) {
								this.firstAdded = newSlot;
							}

							this.lastAdded = newSlot;
							if (arg18 == slot) {
								slotsLocalRef[tableSize] = newSlot;
							} else {
								((ScriptableObject.Slot) arg18).next = newSlot;
							}

							((ScriptableObject.Slot) slot).wasDeleted = true;
							((ScriptableObject.Slot) slot).value = null;
							((ScriptableObject.Slot) slot).name = null;
							if (slot == this.lastAccess) {
								this.lastAccess = REMOVED;
							}

							slot = newSlot;
						} else if (accessType == 5) {
							return null;
						}

						return (ScriptableObject.Slot) slot;
					}

					if (4 * (this.count + 1) > 3 * slotsLocalRef.length) {
						slotsLocalRef = new ScriptableObject.Slot[slotsLocalRef.length * 2 + 1];
						copyTable(this.slots, slotsLocalRef, this.count);
						this.slots = slotsLocalRef;
						tableSize = getSlotIndex(slotsLocalRef.length,
								indexOrHash);
					}
				}

				Object arg17 = accessType == 4
						? new ScriptableObject.GetterSlot(name, indexOrHash, 0)
						: new ScriptableObject.Slot(name, indexOrHash, 0);
				if (accessType == 5) {
					((ScriptableObject.Slot) arg17).setAttributes(13);
				}

				++this.count;
				if (this.lastAdded != null) {
					this.lastAdded.orderedNext = (ScriptableObject.Slot) arg17;
				}

				if (this.firstAdded == null) {
					this.firstAdded = (ScriptableObject.Slot) arg17;
				}

				this.lastAdded = (ScriptableObject.Slot) arg17;
				addKnownAbsentSlot(slotsLocalRef,
						(ScriptableObject.Slot) arg17, tableSize);
				return (ScriptableObject.Slot) arg17;
			}
		}
	}

	private static int getSlotIndex(int tableSize, int indexOrHash) {
		return (indexOrHash & Integer.MAX_VALUE) % tableSize;
	}

	private static void copyTable(ScriptableObject.Slot[] slots,
			ScriptableObject.Slot[] newSlots, int count) {
		if (count == 0) {
			throw Kit.codeBug();
		} else {
			int tableSize = newSlots.length;
			int i = slots.length;

			while (true) {
				--i;
				ScriptableObject.Slot slot = slots[i];

				while (slot != null) {
					int insertPos = getSlotIndex(tableSize, slot.indexOrHash);
					ScriptableObject.Slot next = slot.next;
					addKnownAbsentSlot(newSlots, slot, insertPos);
					slot.next = null;
					slot = next;
					--count;
					if (count == 0) {
						return;
					}
				}
			}
		}
	}

	private static void addKnownAbsentSlot(ScriptableObject.Slot[] slots,
			ScriptableObject.Slot slot, int insertPos) {
		if (slots[insertPos] == null) {
			slots[insertPos] = slot;
		} else {
			ScriptableObject.Slot prev;
			for (prev = slots[insertPos]; prev.next != null; prev = prev.next) {
				;
			}

			prev.next = slot;
		}

	}

	Object[] getIds(boolean getAll) {
		ScriptableObject.Slot[] s = this.slots;
		Object[] a = ScriptRuntime.emptyArgs;
		if (s == null) {
			return a;
		} else {
			int c = 0;

			ScriptableObject.Slot slot;
			for (slot = this.firstAdded; slot != null && slot.wasDeleted; slot = slot.orderedNext) {
				;
			}

			this.firstAdded = slot;
			if (slot != null) {
				while (true) {
					if (getAll || (slot.getAttributes() & 2) == 0) {
						if (c == 0) {
							a = new Object[s.length];
						}

						a[c++] = slot.name != null ? slot.name : Integer
								.valueOf(slot.indexOrHash);
					}

					ScriptableObject.Slot result;
					for (result = slot.orderedNext; result != null
							&& result.wasDeleted; result = result.orderedNext) {
						;
					}

					slot.orderedNext = result;
					if (result == null) {
						break;
					}

					slot = result;
				}
			}

			this.lastAdded = slot;
			if (c == a.length) {
				return a;
			} else {
				Object[] arg6 = new Object[c];
				System.arraycopy(a, 0, arg6, 0, c);
				return arg6;
			}
		}
	}

	private synchronized void writeObject(ObjectOutputStream out)
			throws IOException {
		out.defaultWriteObject();
		int objectsCount = this.count;
		if (objectsCount < 0) {
			objectsCount = ~objectsCount;
		}

		if (objectsCount == 0) {
			out.writeInt(0);
		} else {
			out.writeInt(this.slots.length);

			ScriptableObject.Slot slot;
			for (slot = this.firstAdded; slot != null && slot.wasDeleted; slot = slot.orderedNext) {
				;
			}

			ScriptableObject.Slot next;
			for (this.firstAdded = slot; slot != null; slot = next) {
				out.writeObject(slot);

				for (next = slot.orderedNext; next != null && next.wasDeleted; next = next.orderedNext) {
					;
				}

				slot.orderedNext = next;
			}
		}

	}

	private void readObject(ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		in.defaultReadObject();
		this.lastAccess = REMOVED;
		int tableSize = in.readInt();
		if (tableSize != 0) {
			this.slots = new ScriptableObject.Slot[tableSize];
			int objectsCount = this.count;
			if (objectsCount < 0) {
				objectsCount = ~objectsCount;
			}

			ScriptableObject.Slot prev = null;

			for (int i = 0; i != objectsCount; ++i) {
				this.lastAdded = (ScriptableObject.Slot) in.readObject();
				if (i == 0) {
					this.firstAdded = this.lastAdded;
				} else {
					prev.orderedNext = this.lastAdded;
				}

				int slotIndex = getSlotIndex(tableSize,
						this.lastAdded.indexOrHash);
				addKnownAbsentSlot(this.slots, this.lastAdded, slotIndex);
				prev = this.lastAdded;
			}
		}

	}

	static {
		REMOVED.wasDeleted = true;
	}

	private static final class GetterSlot extends ScriptableObject.Slot {
		static final long serialVersionUID = -4900574849788797588L;
		Object getter;
		Object setter;

		GetterSlot(String name, int indexOrHash, int attributes) {
			super(name, indexOrHash, attributes);
		}
	}

	private static class Slot implements Serializable {
		private static final long serialVersionUID = -6090581677123995491L;
		String name;
		int indexOrHash;
		private volatile short attributes;
		transient volatile boolean wasDeleted;
		volatile Object value;
		transient volatile ScriptableObject.Slot next;
		transient volatile ScriptableObject.Slot orderedNext;

		Slot(String name, int indexOrHash, int attributes) {
			this.name = name;
			this.indexOrHash = indexOrHash;
			this.attributes = (short) attributes;
		}

		private void readObject(ObjectInputStream in) throws IOException,
				ClassNotFoundException {
			in.defaultReadObject();
			if (this.name != null) {
				this.indexOrHash = this.name.hashCode();
			}

		}

		final int getAttributes() {
			return this.attributes;
		}

		final synchronized void setAttributes(int value) {
			ScriptableObject.checkValidAttributes(value);
			this.attributes = (short) value;
		}

		final void checkNotReadonly() {
			if ((this.attributes & 1) != 0) {
				String str = this.name != null ? this.name : Integer
						.toString(this.indexOrHash);
				throw Context.reportRuntimeError1("msg.modify.readonly", str);
			}
		}
	}
}