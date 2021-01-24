/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import java.lang.reflect.Method;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.ClassCache;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.VMBridge;
import org.mozilla.javascript.WrapFactory;

public class InterfaceAdapter {
	private final Object proxyHelper;

	static Object create(Context cx, Class<?> cl, Callable function) {
		if (!cl.isInterface()) {
			throw new IllegalArgumentException();
		} else {
			Scriptable topScope = ScriptRuntime.getTopCallScope(cx);
			ClassCache cache = ClassCache.get(topScope);
			InterfaceAdapter adapter = (InterfaceAdapter) cache
					.getInterfaceAdapter(cl);
			ContextFactory cf = cx.getFactory();
			if (adapter == null) {
				Method[] methods = cl.getMethods();
				if (methods.length == 0) {
					throw Context.reportRuntimeError2(
							"msg.no.empty.interface.conversion",
							String.valueOf(function), cl.getClass().getName());
				}

				boolean canCallFunction = false;
				Class[] argTypes = methods[0].getParameterTypes();
				int i = 1;

				label42 : while (true) {
					if (i == methods.length) {
						canCallFunction = true;
						break;
					}

					Class[] types2 = methods[i].getParameterTypes();
					if (types2.length != argTypes.length) {
						break;
					}

					for (int j = 0; j != argTypes.length; ++j) {
						if (types2[j] != argTypes[j]) {
							break label42;
						}
					}

					++i;
				}

				if (!canCallFunction) {
					throw Context.reportRuntimeError2(
							"msg.no.function.interface.conversion",
							String.valueOf(function), cl.getClass().getName());
				}

				adapter = new InterfaceAdapter(cf, cl);
				cache.cacheInterfaceAdapter(cl, adapter);
			}

			return VMBridge.instance.newInterfaceProxy(adapter.proxyHelper, cf,
					adapter, function, topScope);
		}
	}

	private InterfaceAdapter(ContextFactory cf, Class<?> cl) {
		this.proxyHelper = VMBridge.instance.getInterfaceProxyHelper(cf,
				new Class[]{cl});
	}

	public Object invoke(ContextFactory cf, final Object target,
			final Scriptable topScope, final Method method, final Object[] args) {
		ContextAction action = new ContextAction() {
			public Object run(Context cx) {
				return InterfaceAdapter.this.invokeImpl(cx, target, topScope,
						method, args);
			}
		};
		return cf.call(action);
	}

	Object invokeImpl(Context cx, Object target, Scriptable topScope,
			Method method, Object[] args) {
		int N = args == null ? 0 : args.length;
		Callable function = (Callable) target;
		Object[] jsargs = new Object[N + 1];
		jsargs[N] = method.getName();
		if (N != 0) {
			WrapFactory result = cx.getWrapFactory();

			for (int javaResultType = 0; javaResultType != N; ++javaResultType) {
				jsargs[javaResultType] = result.wrap(cx, topScope,
						args[javaResultType], (Class) null);
			}
		}

		Object arg11 = function.call(cx, topScope, topScope, jsargs);
		Class arg12 = method.getReturnType();
		if (arg12 == Void.TYPE) {
			arg11 = null;
		} else {
			arg11 = Context.jsToJava(arg11, arg12);
		}

		return arg11;
	}
}