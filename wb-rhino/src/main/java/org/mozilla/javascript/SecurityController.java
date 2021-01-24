/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.GeneratedClassLoader;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;

public abstract class SecurityController {
	private static SecurityController global;

	static SecurityController global() {
		return global;
	}

	public static boolean hasGlobal() {
		return global != null;
	}

	public static void initGlobal(SecurityController controller) {
		if (controller == null) {
			throw new IllegalArgumentException();
		} else if (global != null) {
			throw new SecurityException(
					"Cannot overwrite already installed global SecurityController");
		} else {
			global = controller;
		}
	}

	public abstract GeneratedClassLoader createClassLoader(ClassLoader arg0,
			Object arg1);

	public static GeneratedClassLoader createLoader(ClassLoader parent,
			Object staticDomain) {
		Context cx = Context.getContext();
		if (parent == null) {
			parent = cx.getApplicationClassLoader();
		}

		SecurityController sc = cx.getSecurityController();
		GeneratedClassLoader loader;
		if (sc == null) {
			loader = cx.createClassLoader(parent);
		} else {
			Object dynamicDomain = sc.getDynamicSecurityDomain(staticDomain);
			loader = sc.createClassLoader(parent, dynamicDomain);
		}

		return loader;
	}

	public static Class<?> getStaticSecurityDomainClass() {
		SecurityController sc = Context.getContext().getSecurityController();
		return sc == null ? null : sc.getStaticSecurityDomainClassInternal();
	}

	public Class<?> getStaticSecurityDomainClassInternal() {
		return null;
	}

	public abstract Object getDynamicSecurityDomain(Object arg0);

	public Object callWithDomain(Object securityDomain, Context cx,
			final Callable callable, Scriptable scope,
			final Scriptable thisObj, final Object[] args) {
		return this.execWithDomain(cx, scope, new Script() {
			public Object exec(Context cx, Scriptable scope) {
				return callable.call(cx, scope, thisObj, args);
			}
		}, securityDomain);
	}

	public Object execWithDomain(Context cx, Scriptable scope, Script script,
			Object securityDomain) {
		throw new IllegalStateException("callWithDomain should be overridden");
	}
}