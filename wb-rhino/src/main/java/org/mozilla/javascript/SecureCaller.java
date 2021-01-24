/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.SecureClassLoader;
import java.util.Map;
import java.util.WeakHashMap;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public abstract class SecureCaller {
	private static final byte[] secureCallerImplBytecode = loadBytecode();
	private static final Map<CodeSource, Map<ClassLoader, SoftReference<SecureCaller>>> callers = new WeakHashMap();

	public abstract Object call(Callable arg0, Context arg1, Scriptable arg2,
			Scriptable arg3, Object[] arg4);

	static Object callSecurely(final CodeSource codeSource, Callable callable,
			Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		final Thread thread = Thread.currentThread();
		final ClassLoader classLoader = (ClassLoader) AccessController
				.doPrivileged(new PrivilegedAction() {
					public Object run() {
						return thread.getContextClassLoader();
					}
				});
		Map caller = callers;
		Map classLoaderMap;
		synchronized (callers) {
			classLoaderMap = (Map) callers.get(codeSource);
			if (classLoaderMap == null) {
				classLoaderMap = new WeakHashMap();
				callers.put(codeSource, classLoaderMap);
			}
		}

		SecureCaller caller1;
		synchronized (classLoaderMap) {
			SoftReference ref = (SoftReference) ((Map) classLoaderMap)
					.get(classLoader);
			if (ref != null) {
				caller1 = (SecureCaller) ref.get();
			} else {
				caller1 = null;
			}

			if (caller1 == null) {
				try {
					caller1 = (SecureCaller) AccessController
							.doPrivileged(new PrivilegedExceptionAction() {
								public Object run() throws Exception {
									Class thisClass = this.getClass();
									ClassLoader effectiveClassLoader;
									if (classLoader.loadClass(thisClass
											.getName()) != thisClass) {
										effectiveClassLoader = thisClass
												.getClassLoader();
									} else {
										effectiveClassLoader = classLoader;
									}

									SecureCaller.SecureClassLoaderImpl secCl = new SecureCaller.SecureClassLoaderImpl(
											effectiveClassLoader);
									Class c = secCl.defineAndLinkClass(
											SecureCaller.class.getName()
													+ "Impl",
											SecureCaller.secureCallerImplBytecode,
											codeSource);
									return c.newInstance();
								}
							});
					((Map) classLoaderMap).put(classLoader, new SoftReference(
							caller1));
				} catch (PrivilegedActionException arg13) {
					throw new UndeclaredThrowableException(arg13.getCause());
				}
			}
		}

		return caller1.call(callable, cx, scope, thisObj, args);
	}

	private static byte[] loadBytecode() {
		return (byte[]) ((byte[]) AccessController
				.doPrivileged(new PrivilegedAction() {
					public Object run() {
						return SecureCaller.loadBytecodePrivileged();
					}
				}));
	}

	private static byte[] loadBytecodePrivileged() {
		URL url = SecureCaller.class.getResource("SecureCallerImpl.clazz");

		try {
			InputStream e = url.openStream();

			try {
				ByteArrayOutputStream bout = new ByteArrayOutputStream();

				while (true) {
					int r = e.read();
					if (r == -1) {
						byte[] arg3 = bout.toByteArray();
						return arg3;
					}

					bout.write(r);
				}
			} finally {
				e.close();
			}
		} catch (IOException arg8) {
			throw new UndeclaredThrowableException(arg8);
		}
	}

	private static class SecureClassLoaderImpl extends SecureClassLoader {
		SecureClassLoaderImpl(ClassLoader parent) {
			super(parent);
		}

		Class<?> defineAndLinkClass(String name, byte[] bytes, CodeSource cs) {
			Class cl = this.defineClass(name, bytes, 0, bytes.length, cs);
			this.resolveClass(cl);
			return cl;
		}
	}
}