/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import java.lang.ref.SoftReference;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.SecureClassLoader;
import java.util.Map;
import java.util.WeakHashMap;
import org.mozilla.classfile.ClassFileWriter;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.GeneratedClassLoader;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.SecurityController;

public class PolicySecurityController extends SecurityController {
	private static final byte[] secureCallerImplBytecode = loadBytecode();
	private static final Map<CodeSource, Map<ClassLoader, SoftReference<PolicySecurityController.SecureCaller>>> callers = new WeakHashMap();

	public Class<?> getStaticSecurityDomainClassInternal() {
		return CodeSource.class;
	}

	public GeneratedClassLoader createClassLoader(final ClassLoader parent,
			final Object securityDomain) {
		return (PolicySecurityController.Loader) AccessController
				.doPrivileged(new PrivilegedAction() {
					public Object run() {
						return new PolicySecurityController.Loader(parent,
								(CodeSource) securityDomain);
					}
				});
	}

	public Object getDynamicSecurityDomain(Object securityDomain) {
		return securityDomain;
	}

	public Object callWithDomain(Object securityDomain, final Context cx,
			Callable callable, Scriptable scope, Scriptable thisObj,
			Object[] args) {
		final ClassLoader classLoader = (ClassLoader) AccessController
				.doPrivileged(new PrivilegedAction() {
					public Object run() {
						return cx.getApplicationClassLoader();
					}
				});
		final CodeSource codeSource = (CodeSource) securityDomain;
		Map caller = callers;
		Map classLoaderMap;
		synchronized (callers) {
			classLoaderMap = (Map) callers.get(codeSource);
			if (classLoaderMap == null) {
				classLoaderMap = new WeakHashMap();
				callers.put(codeSource, classLoaderMap);
			}
		}

		PolicySecurityController.SecureCaller caller1;
		synchronized (classLoaderMap) {
			SoftReference ref = (SoftReference) ((Map) classLoaderMap)
					.get(classLoader);
			if (ref != null) {
				caller1 = (PolicySecurityController.SecureCaller) ref.get();
			} else {
				caller1 = null;
			}

			if (caller1 == null) {
				try {
					caller1 = (PolicySecurityController.SecureCaller) AccessController
							.doPrivileged(new PrivilegedExceptionAction() {
								public Object run() throws Exception {
									PolicySecurityController.Loader loader = new PolicySecurityController.Loader(
											classLoader, codeSource);
									Class c = loader
											.defineClass(
													PolicySecurityController.SecureCaller.class
															.getName() + "Impl",
													PolicySecurityController.secureCallerImplBytecode);
									return c.newInstance();
								}
							});
					((Map) classLoaderMap).put(classLoader, new SoftReference(
							caller1));
				} catch (PrivilegedActionException arg14) {
					throw new UndeclaredThrowableException(arg14.getCause());
				}
			}
		}

		return caller1.call(callable, cx, scope, thisObj, args);
	}

	private static byte[] loadBytecode() {
		String secureCallerClassName = PolicySecurityController.SecureCaller.class
				.getName();
		ClassFileWriter cfw = new ClassFileWriter(secureCallerClassName
				+ "Impl", secureCallerClassName, "<generated>");
		cfw.startMethod("<init>", "()V", 1);
		cfw.addALoad(0);
		cfw.addInvoke(183, secureCallerClassName, "<init>", "()V");
		cfw.add(177);
		cfw.stopMethod(1);
		String callableCallSig = "Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;Lorg/mozilla/javascript/Scriptable;[Ljava/lang/Object;)Ljava/lang/Object;";
		cfw.startMethod("call", "(Lorg/mozilla/javascript/Callable;"
				+ callableCallSig, 17);

		for (int i = 1; i < 6; ++i) {
			cfw.addALoad(i);
		}

		cfw.addInvoke(185, "org/mozilla/javascript/Callable", "call", "("
				+ callableCallSig);
		cfw.add(176);
		cfw.stopMethod(6);
		return cfw.toByteArray();
	}

	public abstract static class SecureCaller {
		public abstract Object call(Callable arg0, Context arg1,
				Scriptable arg2, Scriptable arg3, Object[] arg4);
	}

	private static class Loader extends SecureClassLoader
			implements
				GeneratedClassLoader {
		private final CodeSource codeSource;

		Loader(ClassLoader parent, CodeSource codeSource) {
			super(parent);
			this.codeSource = codeSource;
		}

		public Class<?> defineClass(String name, byte[] data) {
			return this
					.defineClass(name, data, 0, data.length, this.codeSource);
		}

		public void linkClass(Class<?> cl) {
			this.resolveClass(cl);
		}
	}
}