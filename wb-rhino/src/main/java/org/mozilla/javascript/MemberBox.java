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
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContinuationPending;
import org.mozilla.javascript.JavaMembers;
import org.mozilla.javascript.VMBridge;

final class MemberBox implements Serializable {
	static final long serialVersionUID = 6358550398665688245L;
	private transient Member memberObject;
	transient Class<?>[] argTypes;
	transient Object delegateTo;
	transient boolean vararg;
	private static final Class<?>[] primitives;

	MemberBox(Method method) {
		this.init(method);
	}

	MemberBox(Constructor<?> constructor) {
		this.init(constructor);
	}

	private void init(Method method) {
		this.memberObject = method;
		this.argTypes = method.getParameterTypes();
		this.vararg = VMBridge.instance.isVarArgs(method);
	}

	private void init(Constructor<?> constructor) {
		this.memberObject = constructor;
		this.argTypes = constructor.getParameterTypes();
		this.vararg = VMBridge.instance.isVarArgs(constructor);
	}

	Method method() {
		return (Method) this.memberObject;
	}

	Constructor<?> ctor() {
		return (Constructor) this.memberObject;
	}

	Member member() {
		return this.memberObject;
	}

	boolean isMethod() {
		return this.memberObject instanceof Method;
	}

	boolean isCtor() {
		return this.memberObject instanceof Constructor;
	}

	boolean isStatic() {
		return Modifier.isStatic(this.memberObject.getModifiers());
	}

	String getName() {
		return this.memberObject.getName();
	}

	Class<?> getDeclaringClass() {
		return this.memberObject.getDeclaringClass();
	}

	String toJavaDeclaration() {
		StringBuffer sb = new StringBuffer();
		if (this.isMethod()) {
			Method ctor = this.method();
			sb.append(ctor.getReturnType());
			sb.append(' ');
			sb.append(ctor.getName());
		} else {
			Constructor ctor1 = this.ctor();
			String name = ctor1.getDeclaringClass().getName();
			int lastDot = name.lastIndexOf(46);
			if (lastDot >= 0) {
				name = name.substring(lastDot + 1);
			}

			sb.append(name);
		}

		sb.append(JavaMembers.liveConnectSignature(this.argTypes));
		return sb.toString();
	}

	public String toString() {
		return this.memberObject.toString();
	}

	Object invoke(Object target, Object[] args) {
		Method method = this.method();

		try {
			try {
				return method.invoke(target, args);
			} catch (IllegalAccessException arg5) {
				Method e1 = searchAccessibleMethod(method, this.argTypes);
				if (e1 != null) {
					this.memberObject = e1;
					method = e1;
				} else if (!VMBridge.instance.tryToMakeAccessible(method)) {
					throw Context.throwAsScriptRuntimeEx(arg5);
				}

				return method.invoke(target, args);
			}
		} catch (InvocationTargetException arg6) {
			Object e = arg6;

			do {
				e = ((InvocationTargetException) e).getTargetException();
			} while (e instanceof InvocationTargetException);

			if (e instanceof ContinuationPending) {
				throw (ContinuationPending) e;
			} else {
				throw Context.throwAsScriptRuntimeEx((Throwable) e);
			}
		} catch (Exception arg7) {
			throw Context.throwAsScriptRuntimeEx(arg7);
		}
	}

	Object newInstance(Object[] args) {
		Constructor ctor = this.ctor();

		try {
			try {
				return ctor.newInstance(args);
			} catch (IllegalAccessException arg3) {
				if (!VMBridge.instance.tryToMakeAccessible(ctor)) {
					throw Context.throwAsScriptRuntimeEx(arg3);
				} else {
					return ctor.newInstance(args);
				}
			}
		} catch (Exception arg4) {
			throw Context.throwAsScriptRuntimeEx(arg4);
		}
	}

	private static Method searchAccessibleMethod(Method method,
			Class<?>[] params) {
		int modifiers = method.getModifiers();
		if (Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers)) {
			Class c = method.getDeclaringClass();
			if (!Modifier.isPublic(c.getModifiers())) {
				String name = method.getName();
				Class[] intfs = c.getInterfaces();
				int ex = 0;

				int mModifiers;
				for (mModifiers = intfs.length; ex != mModifiers; ++ex) {
					Class intf = intfs[ex];
					if (Modifier.isPublic(intf.getModifiers())) {
						try {
							return intf.getMethod(name, params);
						} catch (NoSuchMethodException arg11) {
							;
						} catch (SecurityException arg12) {
							;
						}
					}
				}

				while (true) {
					c = c.getSuperclass();
					if (c == null) {
						break;
					}

					if (Modifier.isPublic(c.getModifiers())) {
						try {
							Method arg13 = c.getMethod(name, params);
							mModifiers = arg13.getModifiers();
							if (Modifier.isPublic(mModifiers)
									&& !Modifier.isStatic(mModifiers)) {
								return arg13;
							}
						} catch (NoSuchMethodException arg9) {
							;
						} catch (SecurityException arg10) {
							;
						}
					}
				}
			}
		}

		return null;
	}

	private void readObject(ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		in.defaultReadObject();
		Member member = readMember(in);
		if (member instanceof Method) {
			this.init((Method) member);
		} else {
			this.init((Constructor) member);
		}

	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		writeMember(out, this.memberObject);
	}

	private static void writeMember(ObjectOutputStream out, Member member)
			throws IOException {
		if (member == null) {
			out.writeBoolean(false);
		} else {
			out.writeBoolean(true);
			if (!(member instanceof Method) && !(member instanceof Constructor)) {
				throw new IllegalArgumentException("not Method or Constructor");
			} else {
				out.writeBoolean(member instanceof Method);
				out.writeObject(member.getName());
				out.writeObject(member.getDeclaringClass());
				if (member instanceof Method) {
					writeParameters(out, ((Method) member).getParameterTypes());
				} else {
					writeParameters(out,
							((Constructor) member).getParameterTypes());
				}

			}
		}
	}

	private static Member readMember(ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		if (!in.readBoolean()) {
			return null;
		} else {
			boolean isMethod = in.readBoolean();
			String name = (String) in.readObject();
			Class declaring = (Class) in.readObject();
			Class[] parms = readParameters(in);

			try {
				return (Member) (isMethod
						? declaring.getMethod(name, parms)
						: declaring.getConstructor(parms));
			} catch (NoSuchMethodException arg5) {
				throw new IOException("Cannot find member: " + arg5);
			}
		}
	}

	private static void writeParameters(ObjectOutputStream out, Class<?>[] parms)
			throws IOException {
		out.writeShort(parms.length);

		label28 : for (int i = 0; i < parms.length; ++i) {
			Class parm = parms[i];
			boolean primitive = parm.isPrimitive();
			out.writeBoolean(primitive);
			if (!primitive) {
				out.writeObject(parm);
			} else {
				for (int j = 0; j < primitives.length; ++j) {
					if (parm.equals(primitives[j])) {
						out.writeByte(j);
						continue label28;
					}
				}

				throw new IllegalArgumentException("Primitive " + parm
						+ " not found");
			}
		}

	}

	private static Class<?>[] readParameters(ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		Class[] result = new Class[in.readShort()];

		for (int i = 0; i < result.length; ++i) {
			if (!in.readBoolean()) {
				result[i] = (Class) in.readObject();
			} else {
				result[i] = primitives[in.readByte()];
			}
		}

		return result;
	}

	static {
		primitives = new Class[]{Boolean.TYPE, Byte.TYPE, Character.TYPE,
				Double.TYPE, Float.TYPE, Integer.TYPE, Long.TYPE, Short.TYPE,
				Void.TYPE};
	}
}