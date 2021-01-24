/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import org.mozilla.classfile.ClassFileWriter;
import org.mozilla.javascript.ClassCache;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.GeneratedClassLoader;
import org.mozilla.javascript.IdFunctionCall;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.NativeJavaClass;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.ObjToIntMap;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.SecurityController;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.ObjToIntMap.Iterator;

public final class JavaAdapter implements IdFunctionCall {
	private static final Object FTAG = "JavaAdapter";
	private static final int Id_JavaAdapter = 1;

	public static void init(Context cx, Scriptable scope, boolean sealed) {
		JavaAdapter obj = new JavaAdapter();
		IdFunctionObject ctor = new IdFunctionObject(obj, FTAG, 1,
				"JavaAdapter", 1, scope);
		ctor.markAsConstructor((Scriptable) null);
		if (sealed) {
			ctor.sealObject();
		}

		ctor.exportAsScopeProperty();
	}

	public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
			Scriptable thisObj, Object[] args) {
		if (f.hasTag(FTAG) && f.methodId() == 1) {
			return js_createAdapter(cx, scope, args);
		} else {
			throw f.unknown();
		}
	}

	public static Object convertResult(Object result, Class<?> c) {
		return result == Undefined.instance && c != ScriptRuntime.ObjectClass
				&& c != ScriptRuntime.StringClass ? null : Context.jsToJava(
				result, c);
	}

	public static Scriptable createAdapterWrapper(Scriptable obj, Object adapter) {
		Scriptable scope = ScriptableObject.getTopLevelScope(obj);
		NativeJavaObject res = new NativeJavaObject(scope, adapter,
				(Class) null, true);
		res.setPrototype(obj);
		return res;
	}

	public static Object getAdapterSelf(Class<?> adapterClass, Object adapter)
			throws NoSuchFieldException, IllegalAccessException {
		Field self = adapterClass.getDeclaredField("self");
		return self.get(adapter);
	}

	static Object js_createAdapter(Context cx, Scriptable scope, Object[] args) {
		int N = args.length;
		if (N == 0) {
			throw ScriptRuntime.typeError0("msg.adapter.zero.args");
		} else {
			Class superClass = null;
			Class[] intfs = new Class[N - 1];
			int interfaceCount = 0;

			Class adapterClass;
			for (int interfaces = 0; interfaces != N - 1; ++interfaces) {
				Object obj = args[interfaces];
				if (!(obj instanceof NativeJavaClass)) {
					throw ScriptRuntime.typeError2("msg.not.java.class.arg",
							String.valueOf(interfaces),
							ScriptRuntime.toString(obj));
				}

				adapterClass = ((NativeJavaClass) obj).getClassObject();
				if (!adapterClass.isInterface()) {
					if (superClass != null) {
						throw ScriptRuntime.typeError2("msg.only.one.super",
								superClass.getName(), adapterClass.getName());
					}

					superClass = adapterClass;
				} else {
					intfs[interfaceCount++] = adapterClass;
				}
			}

			if (superClass == null) {
				superClass = ScriptRuntime.ObjectClass;
			}

			Class[] arg13 = new Class[interfaceCount];
			System.arraycopy(intfs, 0, arg13, 0, interfaceCount);
			Scriptable arg14 = ScriptRuntime.toObject(cx, scope, args[N - 1]);
			adapterClass = getAdapterClass(scope, superClass, arg13, arg14);
			Class[] ctorParms = new Class[]{ScriptRuntime.ContextFactoryClass,
					ScriptRuntime.ScriptableClass};
			Object[] ctorArgs = new Object[]{cx.getFactory(), arg14};

			try {
				Object ex = adapterClass.getConstructor(ctorParms).newInstance(
						ctorArgs);
				return getAdapterSelf(adapterClass, ex);
			} catch (Exception arg12) {
				throw Context.throwAsScriptRuntimeEx(arg12);
			}
		}
	}

	public static void writeAdapterObject(Object javaObject,
			ObjectOutputStream out) throws IOException {
		Class cl = javaObject.getClass();
		out.writeObject(cl.getSuperclass().getName());
		Class[] interfaces = cl.getInterfaces();
		String[] interfaceNames = new String[interfaces.length];

		for (int e = 0; e < interfaces.length; ++e) {
			interfaceNames[e] = interfaces[e].getName();
		}

		out.writeObject(interfaceNames);

		try {
			Object arg7 = cl.getField("delegee").get(javaObject);
			out.writeObject(arg7);
			return;
		} catch (IllegalAccessException arg5) {
			;
		} catch (NoSuchFieldException arg6) {
			;
		}

		throw new IOException();
	}

	public static Object readAdapterObject(Scriptable self, ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		Context cx = Context.getCurrentContext();
		ContextFactory factory;
		if (cx != null) {
			factory = cx.getFactory();
		} else {
			factory = null;
		}

		Class superClass = Class.forName((String) in.readObject());
		String[] interfaceNames = (String[]) ((String[]) in.readObject());
		Class[] interfaces = new Class[interfaceNames.length];

		for (int delegee = 0; delegee < interfaceNames.length; ++delegee) {
			interfaces[delegee] = Class.forName(interfaceNames[delegee]);
		}

		Scriptable arg15 = (Scriptable) in.readObject();
		Class adapterClass = getAdapterClass(self, superClass, interfaces,
				arg15);
		Class[] ctorParms = new Class[]{ScriptRuntime.ContextFactoryClass,
				ScriptRuntime.ScriptableClass, ScriptRuntime.ScriptableClass};
		Object[] ctorArgs = new Object[]{factory, arg15, self};

		try {
			return adapterClass.getConstructor(ctorParms).newInstance(ctorArgs);
		} catch (InstantiationException arg11) {
			;
		} catch (IllegalAccessException arg12) {
			;
		} catch (InvocationTargetException arg13) {
			;
		} catch (NoSuchMethodException arg14) {
			;
		}

		throw new ClassNotFoundException("adapter");
	}

	private static ObjToIntMap getObjectFunctionNames(Scriptable obj) {
		Object[] ids = ScriptableObject.getPropertyIds(obj);
		ObjToIntMap map = new ObjToIntMap(ids.length);

		for (int i = 0; i != ids.length; ++i) {
			if (ids[i] instanceof String) {
				String id = (String) ids[i];
				Object value = ScriptableObject.getProperty(obj, id);
				if (value instanceof Function) {
					Function f = (Function) value;
					int length = ScriptRuntime.toInt32(ScriptableObject
							.getProperty(f, "length"));
					if (length < 0) {
						length = 0;
					}

					map.put(id, length);
				}
			}
		}

		return map;
	}

	private static Class<?> getAdapterClass(Scriptable scope,
			Class<?> superClass, Class<?>[] interfaces, Scriptable obj) {
		ClassCache cache = ClassCache.get(scope);
		Map generated = cache.getInterfaceAdapterCacheMap();
		ObjToIntMap names = getObjectFunctionNames(obj);
		JavaAdapter.JavaAdapterSignature sig = new JavaAdapter.JavaAdapterSignature(
				superClass, interfaces, names);
		Class adapterClass = (Class) generated.get(sig);
		if (adapterClass == null) {
			String adapterName = "adapter" + cache.newClassSerialNumber();
			byte[] code = createAdapterCode(names, adapterName, superClass,
					interfaces, (String) null);
			adapterClass = loadAdapterClass(adapterName, code);
			if (cache.isCachingEnabled()) {
				generated.put(sig, adapterClass);
			}
		}

		return adapterClass;
	}

	public static byte[] createAdapterCode(ObjToIntMap functionNames,
			String adapterName, Class<?> superClass, Class<?>[] interfaces,
			String scriptClassName) {
		ClassFileWriter cfw = new ClassFileWriter(adapterName,
				superClass.getName(), "<adapter>");
		cfw.addField("factory", "Lorg/mozilla/javascript/ContextFactory;", 17);
		cfw.addField("delegee", "Lorg/mozilla/javascript/Scriptable;", 17);
		cfw.addField("self", "Lorg/mozilla/javascript/Scriptable;", 17);
		int interfacesCount = interfaces == null ? 0 : interfaces.length;

		for (int superName = 0; superName < interfacesCount; ++superName) {
			if (interfaces[superName] != null) {
				cfw.addInterface(interfaces[superName].getName());
			}
		}

		String arg19 = superClass.getName().replace('.', '/');
		generateCtor(cfw, adapterName, arg19);
		generateSerialCtor(cfw, adapterName, arg19);
		if (scriptClassName != null) {
			generateEmptyCtor(cfw, adapterName, arg19, scriptClassName);
		}

		ObjToIntMap generatedOverrides = new ObjToIntMap();
		ObjToIntMap generatedMethods = new ObjToIntMap();

		String k;
		Class[] argTypes;
		String methodSignature;
		String methodKey;
		for (int methods = 0; methods < interfacesCount; ++methods) {
			Method[] iter = interfaces[methods].getMethods();

			for (int functionName = 0; functionName < iter.length; ++functionName) {
				Method length = iter[functionName];
				int parms = length.getModifiers();
				if (!Modifier.isStatic(parms) && !Modifier.isFinal(parms)) {
					k = length.getName();
					argTypes = length.getParameterTypes();
					if (!functionNames.has(k)) {
						try {
							superClass.getMethod(k, argTypes);
							continue;
						} catch (NoSuchMethodException arg18) {
							;
						}
					}

					methodSignature = getMethodSignature(length, argTypes);
					methodKey = k + methodSignature;
					if (!generatedOverrides.has(methodKey)) {
						generateMethod(cfw, adapterName, k, argTypes,
								length.getReturnType());
						generatedOverrides.put(methodKey, 0);
						generatedMethods.put(k, 0);
					}
				}
			}
		}

		Method[] arg20 = getOverridableMethods(superClass);

		int arg25;
		for (int arg21 = 0; arg21 < arg20.length; ++arg21) {
			Method arg23 = arg20[arg21];
			arg25 = arg23.getModifiers();
			boolean arg26 = Modifier.isAbstract(arg25);
			k = arg23.getName();
			if (arg26 || functionNames.has(k)) {
				argTypes = arg23.getParameterTypes();
				methodSignature = getMethodSignature(arg23, argTypes);
				methodKey = k + methodSignature;
				if (!generatedOverrides.has(methodKey)) {
					generateMethod(cfw, adapterName, k, argTypes,
							arg23.getReturnType());
					generatedOverrides.put(methodKey, 0);
					generatedMethods.put(k, 0);
					if (!arg26) {
						generateSuper(cfw, adapterName, arg19, k,
								methodSignature, argTypes,
								arg23.getReturnType());
					}
				}
			}
		}

		Iterator arg22 = new Iterator(functionNames);
		arg22.start();

		for (; !arg22.done(); arg22.next()) {
			String arg24 = (String) arg22.getKey();
			if (!generatedMethods.has(arg24)) {
				arg25 = arg22.getValue();
				Class[] arg27 = new Class[arg25];

				for (int arg28 = 0; arg28 < arg25; ++arg28) {
					arg27[arg28] = ScriptRuntime.ObjectClass;
				}

				generateMethod(cfw, adapterName, arg24, arg27,
						ScriptRuntime.ObjectClass);
			}
		}

		return cfw.toByteArray();
	}

	static Method[] getOverridableMethods(Class<?> c) {
		ArrayList list = new ArrayList();

		for (HashSet skip = new HashSet(); c != null; c = c.getSuperclass()) {
			appendOverridableMethods(c, list, skip);
			Class[] arr$ = c.getInterfaces();
			int len$ = arr$.length;

			for (int i$ = 0; i$ < len$; ++i$) {
				Class intf = arr$[i$];
				appendOverridableMethods(intf, list, skip);
			}
		}

		return (Method[]) list.toArray(new Method[list.size()]);
	}

	private static void appendOverridableMethods(Class<?> c,
			ArrayList<Method> list, HashSet<String> skip) {
		Method[] methods = c.getDeclaredMethods();

		for (int i = 0; i < methods.length; ++i) {
			String methodKey = methods[i].getName()
					+ getMethodSignature(methods[i],
							methods[i].getParameterTypes());
			if (!skip.contains(methodKey)) {
				int mods = methods[i].getModifiers();
				if (!Modifier.isStatic(mods)) {
					if (Modifier.isFinal(mods)) {
						skip.add(methodKey);
					} else if (Modifier.isPublic(mods)
							|| Modifier.isProtected(mods)) {
						list.add(methods[i]);
						skip.add(methodKey);
					}
				}
			}
		}

	}

	static Class<?> loadAdapterClass(String className, byte[] classBytes) {
		Class domainClass = SecurityController.getStaticSecurityDomainClass();
		Object staticDomain;
		if (domainClass != CodeSource.class
				&& domainClass != ProtectionDomain.class) {
			staticDomain = null;
		} else {
			ProtectionDomain loader = JavaAdapter.class.getProtectionDomain();
			if (domainClass == CodeSource.class) {
				staticDomain = loader == null ? null : loader.getCodeSource();
			} else {
				staticDomain = loader;
			}
		}

		GeneratedClassLoader loader1 = SecurityController.createLoader(
				(ClassLoader) null, staticDomain);
		Class result = loader1.defineClass(className, classBytes);
		loader1.linkClass(result);
		return result;
	}

	public static Function getFunction(Scriptable obj, String functionName) {
		Object x = ScriptableObject.getProperty(obj, functionName);
		if (x == Scriptable.NOT_FOUND) {
			return null;
		} else if (!(x instanceof Function)) {
			throw ScriptRuntime.notFunctionError(x, functionName);
		} else {
			return (Function) x;
		}
	}

	public static Object callMethod(ContextFactory factory,
			final Scriptable thisObj, final Function f, final Object[] args,
			final long argsToWrap) {
		if (f == null) {
			return Undefined.instance;
		} else {
			if (factory == null) {
				factory = ContextFactory.getGlobal();
			}

			final Scriptable scope = f.getParentScope();
			if (argsToWrap == 0L) {
				return Context.call(factory, f, scope, thisObj, args);
			} else {
				Context cx = Context.getCurrentContext();
				return cx != null ? doCall(cx, scope, thisObj, f, args,
						argsToWrap) : factory.call(new ContextAction() {
					public Object run(Context cx) {
						return JavaAdapter.doCall(cx, scope, thisObj, f, args,
								argsToWrap);
					}
				});
			}
		}
	}

	private static Object doCall(Context cx, Scriptable scope,
			Scriptable thisObj, Function f, Object[] args, long argsToWrap) {
		for (int i = 0; i != args.length; ++i) {
			if (0L != (argsToWrap & (long) (1 << i))) {
				Object arg = args[i];
				if (!(arg instanceof Scriptable)) {
					args[i] = cx.getWrapFactory().wrap(cx, scope, arg,
							(Class) null);
				}
			}
		}

		return f.call(cx, scope, thisObj, args);
	}

	public static Scriptable runScript(final Script script) {
		return (Scriptable) ContextFactory.getGlobal().call(
				new ContextAction() {
					public Object run(Context cx) {
						ScriptableObject global = ScriptRuntime.getGlobal(cx);
						script.exec(cx, global);
						return global;
					}
				});
	}

	private static void generateCtor(ClassFileWriter cfw, String adapterName,
			String superName) {
		cfw.startMethod(
				"<init>",
				"(Lorg/mozilla/javascript/ContextFactory;Lorg/mozilla/javascript/Scriptable;)V",
				1);
		cfw.add(42);
		cfw.addInvoke(183, superName, "<init>", "()V");
		cfw.add(42);
		cfw.add(43);
		cfw.add(181, adapterName, "factory",
				"Lorg/mozilla/javascript/ContextFactory;");
		cfw.add(42);
		cfw.add(44);
		cfw.add(181, adapterName, "delegee",
				"Lorg/mozilla/javascript/Scriptable;");
		cfw.add(42);
		cfw.add(44);
		cfw.add(42);
		cfw.addInvoke(
				184,
				"org/mozilla/javascript/JavaAdapter",
				"createAdapterWrapper",
				"(Lorg/mozilla/javascript/Scriptable;Ljava/lang/Object;)Lorg/mozilla/javascript/Scriptable;");
		cfw.add(181, adapterName, "self", "Lorg/mozilla/javascript/Scriptable;");
		cfw.add(177);
		cfw.stopMethod(3);
	}

	private static void generateSerialCtor(ClassFileWriter cfw,
			String adapterName, String superName) {
		cfw.startMethod(
				"<init>",
				"(Lorg/mozilla/javascript/ContextFactory;Lorg/mozilla/javascript/Scriptable;Lorg/mozilla/javascript/Scriptable;)V",
				1);
		cfw.add(42);
		cfw.addInvoke(183, superName, "<init>", "()V");
		cfw.add(42);
		cfw.add(43);
		cfw.add(181, adapterName, "factory",
				"Lorg/mozilla/javascript/ContextFactory;");
		cfw.add(42);
		cfw.add(44);
		cfw.add(181, adapterName, "delegee",
				"Lorg/mozilla/javascript/Scriptable;");
		cfw.add(42);
		cfw.add(45);
		cfw.add(181, adapterName, "self", "Lorg/mozilla/javascript/Scriptable;");
		cfw.add(177);
		cfw.stopMethod(4);
	}

	private static void generateEmptyCtor(ClassFileWriter cfw,
			String adapterName, String superName, String scriptClassName) {
		cfw.startMethod("<init>", "()V", 1);
		cfw.add(42);
		cfw.addInvoke(183, superName, "<init>", "()V");
		cfw.add(42);
		cfw.add(1);
		cfw.add(181, adapterName, "factory",
				"Lorg/mozilla/javascript/ContextFactory;");
		cfw.add(187, scriptClassName);
		cfw.add(89);
		cfw.addInvoke(183, scriptClassName, "<init>", "()V");
		cfw.addInvoke(184, "org/mozilla/javascript/JavaAdapter", "runScript",
				"(Lorg/mozilla/javascript/Script;)Lorg/mozilla/javascript/Scriptable;");
		cfw.add(76);
		cfw.add(42);
		cfw.add(43);
		cfw.add(181, adapterName, "delegee",
				"Lorg/mozilla/javascript/Scriptable;");
		cfw.add(42);
		cfw.add(43);
		cfw.add(42);
		cfw.addInvoke(
				184,
				"org/mozilla/javascript/JavaAdapter",
				"createAdapterWrapper",
				"(Lorg/mozilla/javascript/Scriptable;Ljava/lang/Object;)Lorg/mozilla/javascript/Scriptable;");
		cfw.add(181, adapterName, "self", "Lorg/mozilla/javascript/Scriptable;");
		cfw.add(177);
		cfw.stopMethod(2);
	}

	static void generatePushWrappedArgs(ClassFileWriter cfw,
			Class<?>[] argTypes, int arrayLength) {
		cfw.addPush(arrayLength);
		cfw.add(189, "java/lang/Object");
		int paramOffset = 1;

		for (int i = 0; i != argTypes.length; ++i) {
			cfw.add(89);
			cfw.addPush(i);
			paramOffset += generateWrapArg(cfw, paramOffset, argTypes[i]);
			cfw.add(83);
		}

	}

	private static int generateWrapArg(ClassFileWriter cfw, int paramOffset,
			Class<?> argType) {
		byte size = 1;
		if (!argType.isPrimitive()) {
			cfw.add(25, paramOffset);
		} else if (argType == Boolean.TYPE) {
			cfw.add(187, "java/lang/Boolean");
			cfw.add(89);
			cfw.add(21, paramOffset);
			cfw.addInvoke(183, "java/lang/Boolean", "<init>", "(Z)V");
		} else if (argType == Character.TYPE) {
			cfw.add(21, paramOffset);
			cfw.addInvoke(184, "java/lang/String", "valueOf",
					"(C)Ljava/lang/String;");
		} else {
			cfw.add(187, "java/lang/Double");
			cfw.add(89);
			String typeName = argType.getName();
			switch (typeName.charAt(0)) {
				case 'b' :
				case 'i' :
				case 's' :
					cfw.add(21, paramOffset);
					cfw.add(135);
				case 'c' :
				case 'e' :
				case 'g' :
				case 'h' :
				case 'j' :
				case 'k' :
				case 'm' :
				case 'n' :
				case 'o' :
				case 'p' :
				case 'q' :
				case 'r' :
				default :
					break;
				case 'd' :
					cfw.add(24, paramOffset);
					size = 2;
					break;
				case 'f' :
					cfw.add(23, paramOffset);
					cfw.add(141);
					break;
				case 'l' :
					cfw.add(22, paramOffset);
					cfw.add(138);
					size = 2;
			}

			cfw.addInvoke(183, "java/lang/Double", "<init>", "(D)V");
		}

		return size;
	}

	static void generateReturnResult(ClassFileWriter cfw, Class<?> retType,
			boolean callConvertResult) {
		if (retType == Void.TYPE) {
			cfw.add(87);
			cfw.add(177);
		} else if (retType == Boolean.TYPE) {
			cfw.addInvoke(184, "org/mozilla/javascript/Context", "toBoolean",
					"(Ljava/lang/Object;)Z");
			cfw.add(172);
		} else if (retType == Character.TYPE) {
			cfw.addInvoke(184, "org/mozilla/javascript/Context", "toString",
					"(Ljava/lang/Object;)Ljava/lang/String;");
			cfw.add(3);
			cfw.addInvoke(182, "java/lang/String", "charAt", "(I)C");
			cfw.add(172);
		} else {
			String retTypeStr;
			if (retType.isPrimitive()) {
				cfw.addInvoke(184, "org/mozilla/javascript/Context",
						"toNumber", "(Ljava/lang/Object;)D");
				retTypeStr = retType.getName();
				switch (retTypeStr.charAt(0)) {
					case 'b' :
					case 'i' :
					case 's' :
						cfw.add(142);
						cfw.add(172);
						break;
					case 'c' :
					case 'e' :
					case 'g' :
					case 'h' :
					case 'j' :
					case 'k' :
					case 'm' :
					case 'n' :
					case 'o' :
					case 'p' :
					case 'q' :
					case 'r' :
					default :
						throw new RuntimeException("Unexpected return type "
								+ retType.toString());
					case 'd' :
						cfw.add(175);
						break;
					case 'f' :
						cfw.add(144);
						cfw.add(174);
						break;
					case 'l' :
						cfw.add(143);
						cfw.add(173);
				}
			} else {
				retTypeStr = retType.getName();
				if (callConvertResult) {
					cfw.addLoadConstant(retTypeStr);
					cfw.addInvoke(184, "java/lang/Class", "forName",
							"(Ljava/lang/String;)Ljava/lang/Class;");
					cfw.addInvoke(184, "org/mozilla/javascript/JavaAdapter",
							"convertResult",
							"(Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;");
				}

				cfw.add(192, retTypeStr);
				cfw.add(176);
			}
		}

	}

	private static void generateMethod(ClassFileWriter cfw, String genName,
			String methodName, Class<?>[] parms, Class<?> returnType) {
		StringBuffer sb = new StringBuffer();
		int paramsEnd = appendMethodSignature(parms, returnType, sb);
		String methodSignature = sb.toString();
		cfw.startMethod(methodName, methodSignature, 1);
		cfw.add(42);
		cfw.add(180, genName, "factory",
				"Lorg/mozilla/javascript/ContextFactory;");
		cfw.add(42);
		cfw.add(180, genName, "self", "Lorg/mozilla/javascript/Scriptable;");
		cfw.add(42);
		cfw.add(180, genName, "delegee", "Lorg/mozilla/javascript/Scriptable;");
		cfw.addPush(methodName);
		cfw.addInvoke(
				184,
				"org/mozilla/javascript/JavaAdapter",
				"getFunction",
				"(Lorg/mozilla/javascript/Scriptable;Ljava/lang/String;)Lorg/mozilla/javascript/Function;");
		generatePushWrappedArgs(cfw, parms, parms.length);
		if (parms.length > 64) {
			throw Context
					.reportRuntimeError0("JavaAdapter can not subclass methods with more then 64 arguments.");
		} else {
			long convertionMask = 0L;

			for (int i = 0; i != parms.length; ++i) {
				if (!parms[i].isPrimitive()) {
					convertionMask |= (long) (1 << i);
				}
			}

			cfw.addPush(convertionMask);
			cfw.addInvoke(
					184,
					"org/mozilla/javascript/JavaAdapter",
					"callMethod",
					"(Lorg/mozilla/javascript/ContextFactory;Lorg/mozilla/javascript/Scriptable;Lorg/mozilla/javascript/Function;[Ljava/lang/Object;J)Ljava/lang/Object;");
			generateReturnResult(cfw, returnType, true);
			cfw.stopMethod((short) paramsEnd);
		}
	}

	private static int generatePushParam(ClassFileWriter cfw, int paramOffset,
			Class<?> paramType) {
		if (!paramType.isPrimitive()) {
			cfw.addALoad(paramOffset);
			return 1;
		} else {
			String typeName = paramType.getName();
			switch (typeName.charAt(0)) {
				case 'b' :
				case 'c' :
				case 'i' :
				case 's' :
				case 'z' :
					cfw.addILoad(paramOffset);
					return 1;
				case 'd' :
					cfw.addDLoad(paramOffset);
					return 2;
				case 'e' :
				case 'g' :
				case 'h' :
				case 'j' :
				case 'k' :
				case 'm' :
				case 'n' :
				case 'o' :
				case 'p' :
				case 'q' :
				case 'r' :
				case 't' :
				case 'u' :
				case 'v' :
				case 'w' :
				case 'x' :
				case 'y' :
				default :
					throw Kit.codeBug();
				case 'f' :
					cfw.addFLoad(paramOffset);
					return 1;
				case 'l' :
					cfw.addLLoad(paramOffset);
					return 2;
			}
		}
	}

	private static void generatePopResult(ClassFileWriter cfw, Class<?> retType) {
		if (retType.isPrimitive()) {
			String typeName = retType.getName();
			switch (typeName.charAt(0)) {
				case 'b' :
				case 'c' :
				case 'i' :
				case 's' :
				case 'z' :
					cfw.add(172);
					break;
				case 'd' :
					cfw.add(175);
				case 'e' :
				case 'g' :
				case 'h' :
				case 'j' :
				case 'k' :
				case 'm' :
				case 'n' :
				case 'o' :
				case 'p' :
				case 'q' :
				case 'r' :
				case 't' :
				case 'u' :
				case 'v' :
				case 'w' :
				case 'x' :
				case 'y' :
				default :
					break;
				case 'f' :
					cfw.add(174);
					break;
				case 'l' :
					cfw.add(173);
			}
		} else {
			cfw.add(176);
		}

	}

	private static void generateSuper(ClassFileWriter cfw, String genName,
			String superName, String methodName, String methodSignature,
			Class<?>[] parms, Class<?> returnType) {
		cfw.startMethod("super$" + methodName, methodSignature, 1);
		cfw.add(25, 0);
		int paramOffset = 1;

		for (int retType = 0; retType < parms.length; ++retType) {
			paramOffset += generatePushParam(cfw, paramOffset, parms[retType]);
		}

		cfw.addInvoke(183, superName, methodName, methodSignature);
		if (!returnType.equals(Void.TYPE)) {
			generatePopResult(cfw, returnType);
		} else {
			cfw.add(177);
		}

		cfw.stopMethod((short) (paramOffset + 1));
	}

	private static String getMethodSignature(Method method, Class<?>[] argTypes) {
		StringBuffer sb = new StringBuffer();
		appendMethodSignature(argTypes, method.getReturnType(), sb);
		return sb.toString();
	}

	static int appendMethodSignature(Class<?>[] argTypes, Class<?> returnType,
			StringBuffer sb) {
		sb.append('(');
		int firstLocal = 1 + argTypes.length;

		for (int i = 0; i < argTypes.length; ++i) {
			Class type = argTypes[i];
			appendTypeString(sb, type);
			if (type == Long.TYPE || type == Double.TYPE) {
				++firstLocal;
			}
		}

		sb.append(')');
		appendTypeString(sb, returnType);
		return firstLocal;
	}

	private static StringBuffer appendTypeString(StringBuffer sb, Class<?> type) {
		while (type.isArray()) {
			sb.append('[');
			type = type.getComponentType();
		}

		if (type.isPrimitive()) {
			char typeLetter;
			if (type == Boolean.TYPE) {
				typeLetter = 90;
			} else if (type == Long.TYPE) {
				typeLetter = 74;
			} else {
				String typeName = type.getName();
				typeLetter = Character.toUpperCase(typeName.charAt(0));
			}

			sb.append(typeLetter);
		} else {
			sb.append('L');
			sb.append(type.getName().replace('.', '/'));
			sb.append(';');
		}

		return sb;
	}

	static int[] getArgsToConvert(Class<?>[] argTypes) {
		int count = 0;

		for (int array = 0; array != argTypes.length; ++array) {
			if (!argTypes[array].isPrimitive()) {
				++count;
			}
		}

		if (count == 0) {
			return null;
		} else {
			int[] arg3 = new int[count];
			count = 0;

			for (int i = 0; i != argTypes.length; ++i) {
				if (!argTypes[i].isPrimitive()) {
					arg3[count++] = i;
				}
			}

			return arg3;
		}
	}

	static class JavaAdapterSignature {
		Class<?> superClass;
		Class<?>[] interfaces;
		ObjToIntMap names;

		JavaAdapterSignature(Class<?> superClass, Class<?>[] interfaces,
				ObjToIntMap names) {
			this.superClass = superClass;
			this.interfaces = interfaces;
			this.names = names;
		}

		public boolean equals(Object obj) {
			if (!(obj instanceof JavaAdapter.JavaAdapterSignature)) {
				return false;
			} else {
				JavaAdapter.JavaAdapterSignature sig = (JavaAdapter.JavaAdapterSignature) obj;
				if (this.superClass != sig.superClass) {
					return false;
				} else {
					if (this.interfaces != sig.interfaces) {
						if (this.interfaces.length != sig.interfaces.length) {
							return false;
						}

						for (int iter = 0; iter < this.interfaces.length; ++iter) {
							if (this.interfaces[iter] != sig.interfaces[iter]) {
								return false;
							}
						}
					}

					if (this.names.size() != sig.names.size()) {
						return false;
					} else {
						Iterator arg5 = new Iterator(this.names);
						arg5.start();

						while (!arg5.done()) {
							String name = (String) arg5.getKey();
							int arity = arg5.getValue();
							if (arity != this.names.get(name, arity + 1)) {
								return false;
							}

							arg5.next();
						}

						return true;
					}
				}
			}
		}

		public int hashCode() {
			return this.superClass.hashCode() | -1640531527
					* (this.names.size() | this.interfaces.length << 16);
		}
	}
}