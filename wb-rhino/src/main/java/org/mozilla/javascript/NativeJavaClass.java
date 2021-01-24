/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.Map;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.FieldAndMethods;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.JavaMembers;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.MemberBox;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Wrapper;

public class NativeJavaClass extends NativeJavaObject implements Function {
	static final long serialVersionUID = -6460763940409461664L;
	static final String javaClassPropertyName = "__javaObject__";
	private Map<String, FieldAndMethods> staticFieldAndMethods;

	public NativeJavaClass() {
	}

	public NativeJavaClass(Scriptable scope, Class<?> cl) {
		this.parent = scope;
		this.javaObject = cl;
		this.initMembers();
	}

	protected void initMembers() {
		Class cl = (Class) this.javaObject;
		this.members = JavaMembers.lookupClass(this.parent, cl, cl, false);
		this.staticFieldAndMethods = this.members.getFieldAndMethodsObjects(
				this, cl, true);
	}

	public String getClassName() {
		return "JavaClass";
	}

	public boolean has(String name, Scriptable start) {
		return this.members.has(name, true) || "__javaObject__".equals(name);
	}

	public Object get(String name, Scriptable start) {
		if (name.equals("prototype")) {
			return null;
		} else {
			if (this.staticFieldAndMethods != null) {
				Object nestedClass = this.staticFieldAndMethods.get(name);
				if (nestedClass != null) {
					return nestedClass;
				}
			}

			if (this.members.has(name, true)) {
				return this.members.get(this, name, this.javaObject, true);
			} else if ("__javaObject__".equals(name)) {
				Context nestedClass2 = Context.getContext();
				Scriptable nestedValue1 = ScriptableObject
						.getTopLevelScope(start);
				return nestedClass2.getWrapFactory()
						.wrap(nestedClass2, nestedValue1, this.javaObject,
								ScriptRuntime.ClassClass);
			} else {
				Class nestedClass1 = findNestedClass(this.getClassObject(),
						name);
				if (nestedClass1 != null) {
					NativeJavaClass nestedValue = new NativeJavaClass(
							ScriptableObject.getTopLevelScope(this),
							nestedClass1);
					nestedValue.setParentScope(this);
					return nestedValue;
				} else {
					throw this.members.reportMemberNotFound(name);
				}
			}
		}
	}

	public void put(String name, Scriptable start, Object value) {
		this.members.put(this, name, this.javaObject, value, true);
	}

	public Object[] getIds() {
		return this.members.getIds(true);
	}

	public Class<?> getClassObject() {
		return (Class) super.unwrap();
	}

	public Object getDefaultValue(Class<?> hint) {
		return hint != null && hint != ScriptRuntime.StringClass
				? (hint == ScriptRuntime.BooleanClass
						? Boolean.TRUE
						: (hint == ScriptRuntime.NumberClass
								? ScriptRuntime.NaNobj
								: this)) : this.toString();
	}

	public Object call(Context cx, Scriptable scope, Scriptable thisObj,
			Object[] args) {
		if (args.length == 1 && args[0] instanceof Scriptable) {
			Class c = this.getClassObject();
			Scriptable p = (Scriptable) args[0];

			do {
				if (p instanceof Wrapper) {
					Object o = ((Wrapper) p).unwrap();
					if (c.isInstance(o)) {
						return p;
					}
				}

				p = p.getPrototype();
			} while (p != null);
		}

		return this.construct(cx, scope, args);
	}

	public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
		Class classObject = this.getClassObject();
		int modifiers = classObject.getModifiers();
		if (!Modifier.isInterface(modifiers) && !Modifier.isAbstract(modifiers)) {
			MemberBox[] topLevel1 = this.members.ctors;
			int msg1 = NativeJavaMethod.findFunction(cx, topLevel1, args);
			if (msg1 < 0) {
				String ex1 = NativeJavaMethod.scriptSignature(args);
				throw Context.reportRuntimeError2("msg.no.java.ctor",
						classObject.getName(), ex1);
			} else {
				return constructSpecific(cx, scope, args, topLevel1[msg1]);
			}
		} else {
			Scriptable topLevel = ScriptableObject.getTopLevelScope(this);
			String msg = "";

			try {
				Object ex = topLevel.get("JavaAdapter", topLevel);
				if (ex != NOT_FOUND) {
					Function m1 = (Function) ex;
					Object[] adapterArgs = new Object[]{this, args[0]};
					return m1.construct(cx, topLevel, adapterArgs);
				}
			} catch (Exception arg10) {
				String m = arg10.getMessage();
				if (m != null) {
					msg = m;
				}
			}

			throw Context.reportRuntimeError2("msg.cant.instantiate", msg,
					classObject.getName());
		}
	}

	static Scriptable constructSpecific(Context cx, Scriptable scope,
			Object[] args, MemberBox ctor) {
		Scriptable topLevel = ScriptableObject.getTopLevelScope(scope);
		Class[] argTypes = ctor.argTypes;
		Object[] instance;
		int i;
		if (ctor.vararg) {
			instance = new Object[argTypes.length];

			for (i = 0; i < argTypes.length - 1; ++i) {
				instance[i] = Context.jsToJava(args[i], argTypes[i]);
			}

			Object arg11;
			if (args.length == argTypes.length
					&& (args[args.length - 1] == null
							|| args[args.length - 1] instanceof NativeArray || args[args.length - 1] instanceof NativeJavaArray)) {
				arg11 = Context.jsToJava(args[args.length - 1],
						argTypes[argTypes.length - 1]);
			} else {
				Class arg = argTypes[argTypes.length - 1].getComponentType();
				arg11 = Array.newInstance(arg, args.length - argTypes.length
						+ 1);

				for (int x = 0; x < Array.getLength(arg11); ++x) {
					Object value = Context.jsToJava(args[argTypes.length - 1
							+ x], arg);
					Array.set(arg11, x, value);
				}
			}

			instance[argTypes.length - 1] = arg11;
			args = instance;
		} else {
			instance = args;

			for (i = 0; i < args.length; ++i) {
				Object arg12 = args[i];
				Object arg13 = Context.jsToJava(arg12, argTypes[i]);
				if (arg13 != arg12) {
					if (args == instance) {
						args = (Object[]) instance.clone();
					}

					args[i] = arg13;
				}
			}
		}

		Object arg10 = ctor.newInstance(args);
		return cx.getWrapFactory().wrapNewObject(cx, topLevel, arg10);
	}

	public String toString() {
		return "[JavaClass " + this.getClassObject().getName() + "]";
	}

	public boolean hasInstance(Scriptable value) {
		if (value instanceof Wrapper && !(value instanceof NativeJavaClass)) {
			Object instance = ((Wrapper) value).unwrap();
			return this.getClassObject().isInstance(instance);
		} else {
			return false;
		}
	}

	private static Class<?> findNestedClass(Class<?> parentClass, String name) {
		String nestedClassName = parentClass.getName() + '$' + name;
		ClassLoader loader = parentClass.getClassLoader();
		return loader == null ? Kit.classOrNull(nestedClassName) : Kit
				.classOrNull(loader, nestedClassName);
	}
}