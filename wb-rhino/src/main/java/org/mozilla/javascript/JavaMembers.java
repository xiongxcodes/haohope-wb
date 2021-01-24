/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.mozilla.javascript.BeanProperty;
import org.mozilla.javascript.ClassCache;
import org.mozilla.javascript.ClassShutter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.FieldAndMethods;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.MemberBox;
import org.mozilla.javascript.NativeJavaConstructor;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.ObjArray;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

class JavaMembers {
	private Class<?> cl;
	private Map<String, Object> members;
	private Map<String, FieldAndMethods> fieldAndMethods;
	private Map<String, Object> staticMembers;
	private Map<String, FieldAndMethods> staticFieldAndMethods;
	MemberBox[] ctors;
	private boolean includePrivate;

	JavaMembers(Scriptable scope, Class<?> cl) {
		this(scope, cl, false);
	}

	JavaMembers(Scriptable scope, Class<?> cl, boolean includeProtected) {
		try {
			Context cx = ContextFactory.getGlobal().enterContext();
			ClassShutter shutter = cx.getClassShutter();
			if (shutter != null && !shutter.visibleToScripts(cl.getName())) {
				throw Context.reportRuntimeError1("msg.access.prohibited",
						cl.getName());
			}

			this.includePrivate = cx.hasFeature(13);
			this.members = new HashMap();
			this.staticMembers = new HashMap();
			this.cl = cl;
			this.reflect(scope, includeProtected);
		} finally {
			Context.exit();
		}

	}

	boolean has(String name, boolean isStatic) {
		Map ht = isStatic ? this.staticMembers : this.members;
		Object obj = ht.get(name);
		return obj != null
				? true
				: this.findExplicitFunction(name, isStatic) != null;
	}

	Object get(Scriptable scope, String name, Object javaObject,
			boolean isStatic) {
		Map ht = isStatic ? this.staticMembers : this.members;
		Object member = ht.get(name);
		if (!isStatic && member == null) {
			member = this.staticMembers.get(name);
		}

		if (member == null) {
			member = this
					.getExplicitFunction(scope, name, javaObject, isStatic);
			if (member == null) {
				return Scriptable.NOT_FOUND;
			}
		}

		if (member instanceof Scriptable) {
			return member;
		} else {
			Context cx = Context.getContext();

			Object rval;
			Class type;
			try {
				if (member instanceof BeanProperty) {
					BeanProperty ex = (BeanProperty) member;
					if (ex.getter == null) {
						return Scriptable.NOT_FOUND;
					}

					rval = ex.getter.invoke(javaObject, Context.emptyArgs);
					type = ex.getter.method().getReturnType();
				} else {
					Field ex1 = (Field) member;
					rval = ex1.get(isStatic ? null : javaObject);
					type = ex1.getType();
				}
			} catch (Exception arg10) {
				throw Context.throwAsScriptRuntimeEx(arg10);
			}

			scope = ScriptableObject.getTopLevelScope(scope);
			return cx.getWrapFactory().wrap(cx, scope, rval, type);
		}
	}

	void put(Scriptable scope, String name, Object javaObject, Object value,
			boolean isStatic) {
		Map ht = isStatic ? this.staticMembers : this.members;
		Object member = ht.get(name);
		if (!isStatic && member == null) {
			member = this.staticMembers.get(name);
		}

		if (member == null) {
			throw this.reportMemberNotFound(name);
		} else {
			if (member instanceof FieldAndMethods) {
				FieldAndMethods field = (FieldAndMethods) ht.get(name);
				member = field.field;
			}

			if (member instanceof BeanProperty) {
				BeanProperty field1 = (BeanProperty) member;
				if (field1.setter == null) {
					throw this.reportMemberNotFound(name);
				}

				if (field1.setters != null && value != null) {
					Object[] javaValue1 = new Object[]{value};
					field1.setters.call(Context.getContext(),
							ScriptableObject.getTopLevelScope(scope), scope,
							javaValue1);
				} else {
					Class javaValue = field1.setter.argTypes[0];
					Object[] argEx = new Object[]{Context.jsToJava(value,
							javaValue)};

					try {
						field1.setter.invoke(javaObject, argEx);
					} catch (Exception arg11) {
						throw Context.throwAsScriptRuntimeEx(arg11);
					}
				}
			} else {
				if (!(member instanceof Field)) {
					String field3 = member == null
							? "msg.java.internal.private"
							: "msg.java.method.assign";
					throw Context.reportRuntimeError1(field3, name);
				}

				Field field2 = (Field) member;
				Object javaValue2 = Context.jsToJava(value, field2.getType());

				try {
					field2.set(javaObject, javaValue2);
				} catch (IllegalAccessException arg12) {
					if ((field2.getModifiers() & 16) != 0) {
						return;
					}

					throw Context.throwAsScriptRuntimeEx(arg12);
				} catch (IllegalArgumentException arg13) {
					throw Context.reportRuntimeError3(
							"msg.java.internal.field.type", value.getClass()
									.getName(), field2, javaObject.getClass()
									.getName());
				}
			}

		}
	}

	Object[] getIds(boolean isStatic) {
		Map map = isStatic ? this.staticMembers : this.members;
		return map.keySet().toArray(new Object[map.size()]);
	}

	static String javaSignature(Class<?> type) {
		if (!type.isArray()) {
			return type.getName();
		} else {
			int arrayDimension = 0;

			do {
				++arrayDimension;
				type = type.getComponentType();
			} while (type.isArray());

			String name = type.getName();
			String suffix = "[]";
			if (arrayDimension == 1) {
				return name.concat(suffix);
			} else {
				int length = name.length() + arrayDimension * suffix.length();
				StringBuffer sb = new StringBuffer(length);
				sb.append(name);

				while (arrayDimension != 0) {
					--arrayDimension;
					sb.append(suffix);
				}

				return sb.toString();
			}
		}
	}

	static String liveConnectSignature(Class<?>[] argTypes) {
		int N = argTypes.length;
		if (N == 0) {
			return "()";
		} else {
			StringBuffer sb = new StringBuffer();
			sb.append('(');

			for (int i = 0; i != N; ++i) {
				if (i != 0) {
					sb.append(',');
				}

				sb.append(javaSignature(argTypes[i]));
			}

			sb.append(')');
			return sb.toString();
		}
	}

	private MemberBox findExplicitFunction(String name, boolean isStatic) {
		int sigStart = name.indexOf(40);
		if (sigStart < 0) {
			return null;
		} else {
			Map ht = isStatic ? this.staticMembers : this.members;
			MemberBox[] methodsOrCtors = null;
			boolean isCtor = isStatic && sigStart == 0;
			if (isCtor) {
				methodsOrCtors = this.ctors;
			} else {
				String i = name.substring(0, sigStart);
				Object type = ht.get(i);
				if (!isStatic && type == null) {
					type = this.staticMembers.get(i);
				}

				if (type instanceof NativeJavaMethod) {
					NativeJavaMethod sig = (NativeJavaMethod) type;
					methodsOrCtors = sig.methods;
				}
			}

			if (methodsOrCtors != null) {
				for (int arg9 = 0; arg9 < methodsOrCtors.length; ++arg9) {
					Class[] arg10 = methodsOrCtors[arg9].argTypes;
					String arg11 = liveConnectSignature(arg10);
					if (sigStart + arg11.length() == name.length()
							&& name.regionMatches(sigStart, arg11, 0,
									arg11.length())) {
						return methodsOrCtors[arg9];
					}
				}
			}

			return null;
		}
	}

	private Object getExplicitFunction(Scriptable scope, String name,
			Object javaObject, boolean isStatic) {
		Map ht = isStatic ? this.staticMembers : this.members;
		Object member = null;
		MemberBox methodOrCtor = this.findExplicitFunction(name, isStatic);
		if (methodOrCtor != null) {
			Scriptable prototype = ScriptableObject.getFunctionPrototype(scope);
			if (methodOrCtor.isCtor()) {
				NativeJavaConstructor trueName = new NativeJavaConstructor(
						methodOrCtor);
				trueName.setPrototype(prototype);
				member = trueName;
				ht.put(name, trueName);
			} else {
				String trueName1 = methodOrCtor.getName();
				member = ht.get(trueName1);
				if (member instanceof NativeJavaMethod
						&& ((NativeJavaMethod) member).methods.length > 1) {
					NativeJavaMethod fun = new NativeJavaMethod(methodOrCtor,
							name);
					fun.setPrototype(prototype);
					ht.put(name, fun);
					member = fun;
				}
			}
		}

		return member;
	}

	private static Method[] discoverAccessibleMethods(Class<?> clazz,
			boolean includeProtected, boolean includePrivate) {
		HashMap map = new HashMap();
		discoverAccessibleMethods(clazz, map, includeProtected, includePrivate);
		return (Method[]) map.values().toArray(new Method[map.size()]);
	}

	private static void discoverAccessibleMethods(Class<?> clazz,
			Map<JavaMembers.MethodSignature, Method> map,
			boolean includeProtected, boolean includePrivate) {
		int superclass;
		if (Modifier.isPublic(clazz.getModifiers()) || includePrivate) {
			try {
				Method[] arg10;
				Method arg13;
				if (!includeProtected && !includePrivate) {
					arg10 = clazz.getMethods();

					for (superclass = 0; superclass < arg10.length; ++superclass) {
						arg13 = arg10[superclass];
						JavaMembers.MethodSignature arg15 = new JavaMembers.MethodSignature(
								arg13);
						map.put(arg15, arg13);
					}
				} else {
					while (clazz != null) {
						try {
							arg10 = clazz.getDeclaredMethods();

							for (superclass = 0; superclass < arg10.length; ++superclass) {
								arg13 = arg10[superclass];
								int arg14 = arg13.getModifiers();
								if (Modifier.isPublic(arg14)
										|| Modifier.isProtected(arg14)
										|| includePrivate) {
									if (includePrivate) {
										arg13.setAccessible(true);
									}

									map.put(new JavaMembers.MethodSignature(
											arg13), arg13);
								}
							}

							clazz = clazz.getSuperclass();
						} catch (SecurityException arg8) {
							Method[] arg12 = clazz.getMethods();

							for (int method = 0; method < arg12.length; ++method) {
								Method sig = arg12[method];
								JavaMembers.MethodSignature sig1 = new JavaMembers.MethodSignature(
										sig);
								if (map.get(sig1) == null) {
									map.put(sig1, sig);
								}
							}

							return;
						}
					}
				}

				return;
			} catch (SecurityException arg9) {
				Context.reportWarning("Could not discover accessible methods of class "
						+ clazz.getName()
						+ " due to lack of privileges, "
						+ "attemping superclasses/interfaces.");
			}
		}

		Class[] interfaces = clazz.getInterfaces();

		for (superclass = 0; superclass < interfaces.length; ++superclass) {
			discoverAccessibleMethods(interfaces[superclass], map,
					includeProtected, includePrivate);
		}

		Class arg11 = clazz.getSuperclass();
		if (arg11 != null) {
			discoverAccessibleMethods(arg11, map, includeProtected,
					includePrivate);
		}

	}

	private void reflect(Scriptable scope, boolean includeProtected) {
		Method[] methods = discoverAccessibleMethods(this.cl, includeProtected,
				this.includePrivate);

		int fields;
		int i;
		Object key;
		ObjArray value;
		for (fields = 0; fields < methods.length; ++fields) {
			Method constructors = methods[fields];
			i = constructors.getModifiers();
			boolean ht = Modifier.isStatic(i);
			Map toAdd = ht ? this.staticMembers : this.members;
			String i$ = constructors.getName();
			key = toAdd.get(i$);
			if (key == null) {
				toAdd.put(i$, constructors);
			} else {
				if (key instanceof ObjArray) {
					value = (ObjArray) key;
				} else {
					if (!(key instanceof Method)) {
						Kit.codeBug();
					}

					value = new ObjArray();
					value.add(key);
					toAdd.put(i$, value);
				}

				value.add(constructors);
			}
		}

		for (fields = 0; fields != 2; ++fields) {
			boolean arg26 = fields == 0;
			Map arg28 = arg26 ? this.staticMembers : this.members;

			String arg35;
			NativeJavaMethod arg43;
			for (Iterator arg32 = arg28.keySet().iterator(); arg32.hasNext(); arg28
					.put(arg35, arg43)) {
				arg35 = (String) arg32.next();
				key = arg28.get(arg35);
				MemberBox[] arg38;
				if (key instanceof Method) {
					arg38 = new MemberBox[]{new MemberBox((Method) key)};
				} else {
					value = (ObjArray) key;
					int memberIsSetMethod = value.size();
					if (memberIsSetMethod < 2) {
						Kit.codeBug();
					}

					arg38 = new MemberBox[memberIsSetMethod];

					for (int memberIsIsMethod = 0; memberIsIsMethod != memberIsSetMethod; ++memberIsIsMethod) {
						Method nameComponent = (Method) value
								.get(memberIsIsMethod);
						arg38[memberIsIsMethod] = new MemberBox(nameComponent);
					}
				}

				arg43 = new NativeJavaMethod(arg38);
				if (scope != null) {
					ScriptRuntime.setFunctionProtoAndParent(arg43, scope);
				}
			}
		}

		Field[] arg25 = this.getAccessibleFields();

		int arg27;
		Object arg45;
		for (arg27 = 0; arg27 < arg25.length; ++arg27) {
			Field arg29 = arg25[arg27];
			String arg33 = arg29.getName();
			int arg36 = arg29.getModifiers();
			if (this.includePrivate || Modifier.isPublic(arg36)) {
				try {
					boolean arg39 = Modifier.isStatic(arg36);
					Map arg41 = arg39 ? this.staticMembers : this.members;
					arg45 = arg41.get(arg33);
					if (arg45 == null) {
						arg41.put(arg33, arg29);
					} else if (arg45 instanceof NativeJavaMethod) {
						NativeJavaMethod arg44 = (NativeJavaMethod) arg45;
						FieldAndMethods arg49 = new FieldAndMethods(scope,
								arg44.methods, arg29);
						Object arg51 = arg39
								? this.staticFieldAndMethods
								: this.fieldAndMethods;
						if (arg51 == null) {
							arg51 = new HashMap();
							if (arg39) {
								this.staticFieldAndMethods = (Map) arg51;
							} else {
								this.fieldAndMethods = (Map) arg51;
							}
						}

						((Map) arg51).put(arg33, arg49);
						arg41.put(arg33, arg49);
					} else if (arg45 instanceof Field) {
						Field arg46 = (Field) arg45;
						if (arg46.getDeclaringClass().isAssignableFrom(
								arg29.getDeclaringClass())) {
							arg41.put(arg33, arg29);
						}
					} else {
						Kit.codeBug();
					}
				} catch (SecurityException arg24) {
					Context.reportWarning("Could not access field " + arg33
							+ " of class " + this.cl.getName()
							+ " due to lack of privileges.");
				}
			}
		}

		label209 : for (arg27 = 0; arg27 != 2; ++arg27) {
			boolean arg31 = arg27 == 0;
			Map arg34 = arg31 ? this.staticMembers : this.members;
			HashMap arg37 = new HashMap();
			Iterator arg40 = arg34.keySet().iterator();

			while (true) {
				String beanPropertyName;
				String arg52;
				Object arg53;
				do {
					do {
						do {
							String arg42;
							boolean arg47;
							boolean arg48;
							boolean arg50;
							do {
								if (!arg40.hasNext()) {
									arg40 = arg37.keySet().iterator();

									while (arg40.hasNext()) {
										arg42 = (String) arg40.next();
										arg45 = arg37.get(arg42);
										arg34.put(arg42, arg45);
									}
									continue label209;
								}

								arg42 = (String) arg40.next();
								arg47 = arg42.startsWith("get");
								arg48 = arg42.startsWith("set");
								arg50 = arg42.startsWith("is");
							} while (!arg47 && !arg50 && !arg48);

							arg52 = arg42.substring(arg50 ? 2 : 3);
						} while (arg52.length() == 0);

						beanPropertyName = arg52;
						char ch0 = arg52.charAt(0);
						if (Character.isUpperCase(ch0)) {
							if (arg52.length() == 1) {
								beanPropertyName = arg52.toLowerCase();
							} else {
								char v = arg52.charAt(1);
								if (!Character.isUpperCase(v)) {
									beanPropertyName = Character
											.toLowerCase(ch0)
											+ arg52.substring(1);
								}
							}
						}
					} while (arg37.containsKey(beanPropertyName));

					arg53 = arg34.get(beanPropertyName);
				} while (arg53 != null
						&& (!this.includePrivate || !(arg53 instanceof Member) || !Modifier
								.isPrivate(((Member) arg53).getModifiers())));

				MemberBox getter = null;
				getter = this.findGetter(arg31, arg34, "get", arg52);
				if (getter == null) {
					getter = this.findGetter(arg31, arg34, "is", arg52);
				}

				MemberBox setter = null;
				NativeJavaMethod setters = null;
				String setterName = "set".concat(arg52);
				if (arg34.containsKey(setterName)) {
					Object bp = arg34.get(setterName);
					if (bp instanceof NativeJavaMethod) {
						NativeJavaMethod njmSet = (NativeJavaMethod) bp;
						if (getter != null) {
							Class type = getter.method().getReturnType();
							setter = extractSetMethod(type, njmSet.methods,
									arg31);
						} else {
							setter = extractSetMethod(njmSet.methods, arg31);
						}

						if (njmSet.methods.length > 1) {
							setters = njmSet;
						}
					}
				}

				BeanProperty arg54 = new BeanProperty(getter, setter, setters);
				arg37.put(beanPropertyName, arg54);
			}
		}

		Constructor[] arg30 = this.getAccessibleConstructors();
		this.ctors = new MemberBox[arg30.length];

		for (i = 0; i != arg30.length; ++i) {
			this.ctors[i] = new MemberBox(arg30[i]);
		}

	}

	private Constructor<?>[] getAccessibleConstructors() {
		if (this.includePrivate && this.cl != ScriptRuntime.ClassClass) {
			try {
				Constructor[] e = this.cl.getDeclaredConstructors();
				Constructor.setAccessible(e, true);
				return e;
			} catch (SecurityException arg1) {
				Context.reportWarning("Could not access constructor  of class "
						+ this.cl.getName() + " due to lack of privileges.");
			}
		}

		return this.cl.getConstructors();
	}

	private Field[] getAccessibleFields() {
		if (this.includePrivate) {
			try {
				ArrayList e = new ArrayList();

				for (Class currentClass = this.cl; currentClass != null; currentClass = currentClass
						.getSuperclass()) {
					Field[] declared = currentClass.getDeclaredFields();

					for (int i = 0; i < declared.length; ++i) {
						declared[i].setAccessible(true);
						e.add(declared[i]);
					}
				}

				return (Field[]) e.toArray(new Field[e.size()]);
			} catch (SecurityException arg4) {
				;
			}
		}

		return this.cl.getFields();
	}

	private MemberBox findGetter(boolean isStatic, Map<String, Object> ht,
			String prefix, String propertyName) {
		String getterName = prefix.concat(propertyName);
		if (ht.containsKey(getterName)) {
			Object member = ht.get(getterName);
			if (member instanceof NativeJavaMethod) {
				NativeJavaMethod njmGet = (NativeJavaMethod) member;
				return extractGetMethod(njmGet.methods, isStatic);
			}
		}

		return null;
	}

	private static MemberBox extractGetMethod(MemberBox[] methods,
			boolean isStatic) {
		for (int methodIdx = 0; methodIdx < methods.length; ++methodIdx) {
			MemberBox method = methods[methodIdx];
			if (method.argTypes.length == 0 && (!isStatic || method.isStatic())) {
				Class type = method.method().getReturnType();
				if (type != Void.TYPE) {
					return method;
				}
				break;
			}
		}

		return null;
	}

	private static MemberBox extractSetMethod(Class<?> type,
			MemberBox[] methods, boolean isStatic) {
		for (int pass = 1; pass <= 2; ++pass) {
			for (int i = 0; i < methods.length; ++i) {
				MemberBox method = methods[i];
				if (!isStatic || method.isStatic()) {
					Class[] params = method.argTypes;
					if (params.length == 1) {
						if (pass == 1) {
							if (params[0] == type) {
								return method;
							}
						} else {
							if (pass != 2) {
								Kit.codeBug();
							}

							if (params[0].isAssignableFrom(type)) {
								return method;
							}
						}
					}
				}
			}
		}

		return null;
	}

	private static MemberBox extractSetMethod(MemberBox[] methods,
			boolean isStatic) {
		for (int i = 0; i < methods.length; ++i) {
			MemberBox method = methods[i];
			if ((!isStatic || method.isStatic())
					&& method.method().getReturnType() == Void.TYPE
					&& method.argTypes.length == 1) {
				return method;
			}
		}

		return null;
	}

	Map<String, FieldAndMethods> getFieldAndMethodsObjects(Scriptable scope,
			Object javaObject, boolean isStatic) {
		Map ht = isStatic ? this.staticFieldAndMethods : this.fieldAndMethods;
		if (ht == null) {
			return null;
		} else {
			int len = ht.size();
			HashMap result = new HashMap(len);
			Iterator i$ = ht.values().iterator();

			while (i$.hasNext()) {
				FieldAndMethods fam = (FieldAndMethods) i$.next();
				FieldAndMethods famNew = new FieldAndMethods(scope,
						fam.methods, fam.field);
				famNew.javaObject = javaObject;
				result.put(fam.field.getName(), famNew);
			}

			return result;
		}
	}

	static JavaMembers lookupClass(Scriptable scope, Class<?> dynamicType,
			Class<?> staticType, boolean includeProtected) {
		scope = ScriptableObject.getTopLevelScope(scope);
		ClassCache cache = ClassCache.get(scope);
		Map ct = cache.getClassCacheMap();
		Class cl = dynamicType;

		while (true) {
			JavaMembers members = (JavaMembers) ct.get(cl);
			if (members != null) {
				return members;
			}

			try {
				members = new JavaMembers(scope, cl, includeProtected);
			} catch (SecurityException arg9) {
				if (staticType != null && staticType.isInterface()) {
					cl = staticType;
					staticType = null;
					continue;
				}

				Class parent = cl.getSuperclass();
				if (parent == null) {
					if (!cl.isInterface()) {
						throw arg9;
					}

					parent = ScriptRuntime.ObjectClass;
				}

				cl = parent;
				continue;
			}

			if (cache.isCachingEnabled()) {
				ct.put(cl, members);
			}

			return members;
		}
	}

	RuntimeException reportMemberNotFound(String memberName) {
		return Context.reportRuntimeError2("msg.java.member.not.found",
				this.cl.getName(), memberName);
	}

	private static final class MethodSignature {
		private final String name;
		private final Class<?>[] args;

		private MethodSignature(String name, Class<?>[] args) {
			this.name = name;
			this.args = args;
		}

		MethodSignature(Method method) {
			this(method.getName(), method.getParameterTypes());
		}

		public boolean equals(Object o) {
			if (!(o instanceof JavaMembers.MethodSignature)) {
				return false;
			} else {
				JavaMembers.MethodSignature ms = (JavaMembers.MethodSignature) o;
				return ms.name.equals(this.name)
						&& Arrays.equals(this.args, ms.args);
			}
		}

		public int hashCode() {
			return this.name.hashCode() ^ this.args.length;
		}
	}
}