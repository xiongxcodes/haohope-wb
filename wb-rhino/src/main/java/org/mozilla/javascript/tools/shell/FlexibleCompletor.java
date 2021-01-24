/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.tools.shell;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

class FlexibleCompletor implements InvocationHandler {
	private Method completeMethod;
	private Scriptable global;

	FlexibleCompletor(Class<?> completorClass, Scriptable global)
			throws NoSuchMethodException {
		this.global = global;
		this.completeMethod = completorClass.getMethod("complete", new Class[]{
				String.class, Integer.TYPE, List.class});
	}

	public Object invoke(Object proxy, Method method, Object[] args) {
		if (method.equals(this.completeMethod)) {
			int result = this.complete((String) args[0],
					((Integer) args[1]).intValue(), (List) args[2]);
			return Integer.valueOf(result);
		} else {
			throw new NoSuchMethodError(method.toString());
		}
	}

	public int complete(String buffer, int cursor, List<String> candidates) {
		int m;
		for (m = cursor - 1; m >= 0; --m) {
			char namesAndDots = buffer.charAt(m);
			if (!Character.isJavaIdentifierPart(namesAndDots)
					&& namesAndDots != 46) {
				break;
			}
		}

		String arg11 = buffer.substring(m + 1, cursor);
		String[] names = arg11.split("\\.", -1);
		Scriptable obj = this.global;

		for (int ids = 0; ids < names.length - 1; ++ids) {
			Object lastPart = obj.get(names[ids], this.global);
			if (!(lastPart instanceof Scriptable)) {
				return buffer.length();
			}

			obj = (Scriptable) lastPart;
		}

		Object[] arg12 = obj instanceof ScriptableObject
				? ((ScriptableObject) obj).getAllIds()
				: obj.getIds();
		String arg13 = names[names.length - 1];

		for (int i = 0; i < arg12.length; ++i) {
			if (arg12[i] instanceof String) {
				String id = (String) arg12[i];
				if (id.startsWith(arg13)) {
					if (obj.get(id, obj) instanceof Function) {
						id = id + "(";
					}

					candidates.add(id);
				}
			}
		}

		return buffer.length() - arg13.length();
	}
}