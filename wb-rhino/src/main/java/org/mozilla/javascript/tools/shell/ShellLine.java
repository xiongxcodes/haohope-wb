/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.tools.shell;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.tools.shell.FlexibleCompletor;

public class ShellLine {
	public static InputStream getStream(Scriptable scope) {
		ClassLoader classLoader = ShellLine.class.getClassLoader();
		Class readerClass = Kit.classOrNull(classLoader, "jline.ConsoleReader");
		if (readerClass == null) {
			return null;
		} else {
			try {
				Constructor e = readerClass.getConstructor(new Class[0]);
				Object reader = e.newInstance(new Object[0]);
				Method m = readerClass.getMethod("setBellEnabled",
						new Class[]{Boolean.TYPE});
				m.invoke(reader, new Object[]{Boolean.FALSE});
				Class completorClass = Kit.classOrNull(classLoader,
						"jline.Completor");
				m = readerClass.getMethod("addCompletor",
						new Class[]{completorClass});
				Object completor = Proxy.newProxyInstance(classLoader,
						new Class[]{completorClass}, new FlexibleCompletor(
								completorClass, scope));
				m.invoke(reader, new Object[]{completor});
				Class inputStreamClass = Kit.classOrNull(classLoader,
						"jline.ConsoleReaderInputStream");
				e = inputStreamClass.getConstructor(new Class[]{readerClass});
				return (InputStream) e.newInstance(new Object[]{reader});
			} catch (NoSuchMethodException arg8) {
				;
			} catch (InstantiationException arg9) {
				;
			} catch (IllegalAccessException arg10) {
				;
			} catch (InvocationTargetException arg11) {
				;
			}

			return null;
		}
	}
}