/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.jdk13;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.InterfaceAdapter;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.VMBridge;

public class VMBridge_jdk13 extends VMBridge {
	private ThreadLocal<Object[]> contextLocal = new ThreadLocal();

	protected Object getThreadContextHelper() {
		Object[] storage = (Object[]) this.contextLocal.get();
		if (storage == null) {
			storage = new Object[1];
			this.contextLocal.set(storage);
		}

		return storage;
	}

	protected Context getContext(Object contextHelper) {
		Object[] storage = (Object[]) ((Object[]) contextHelper);
		return (Context) storage[0];
	}

	protected void setContext(Object contextHelper, Context cx) {
		Object[] storage = (Object[]) ((Object[]) contextHelper);
		storage[0] = cx;
	}

	protected ClassLoader getCurrentThreadClassLoader() {
		return Thread.currentThread().getContextClassLoader();
	}

	protected boolean tryToMakeAccessible(Object accessibleObject) {
		if (!(accessibleObject instanceof AccessibleObject)) {
			return false;
		} else {
			AccessibleObject accessible = (AccessibleObject) accessibleObject;
			if (accessible.isAccessible()) {
				return true;
			} else {
				try {
					accessible.setAccessible(true);
				} catch (Exception arg3) {
					;
				}

				return accessible.isAccessible();
			}
		}
	}

	protected Object getInterfaceProxyHelper(ContextFactory cf,
			Class<?>[] interfaces) {
		ClassLoader loader = interfaces[0].getClassLoader();
		Class cl = Proxy.getProxyClass(loader, interfaces);

		try {
			Constructor c = cl
					.getConstructor(new Class[]{InvocationHandler.class});
			return c;
		} catch (NoSuchMethodException arg6) {
			throw Kit.initCause(new IllegalStateException(), arg6);
		}
	}

	protected Object newInterfaceProxy(Object proxyHelper,
			final ContextFactory cf, final InterfaceAdapter adapter,
			final Object target, final Scriptable topScope) {
		Constructor c = (Constructor) proxyHelper;
		InvocationHandler handler = new InvocationHandler() {
			public Object invoke(Object proxy, Method method, Object[] args) {
				return adapter.invoke(cf, target, topScope, method, args);
			}
		};

		try {
			Object proxy = c.newInstance(new Object[]{handler});
			return proxy;
		} catch (InvocationTargetException arg9) {
			throw Context.throwAsScriptRuntimeEx(arg9);
		} catch (IllegalAccessException arg10) {
			throw Kit.initCause(new IllegalStateException(), arg10);
		} catch (InstantiationException arg11) {
			throw Kit.initCause(new IllegalStateException(), arg11);
		}
	}

	protected boolean isVarArgs(Member member) {
		return false;
	}
}