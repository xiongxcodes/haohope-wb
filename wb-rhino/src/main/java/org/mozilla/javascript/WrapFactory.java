/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

public class WrapFactory {
	private boolean javaPrimitiveWrap = true;

	public Object wrap(Context cx, Scriptable scope, Object obj,
			Class<?> staticType) {
		if (obj != null && obj != Undefined.instance
				&& !(obj instanceof Scriptable)) {
			if (staticType != null && staticType.isPrimitive()) {
				return staticType == Void.TYPE
						? Undefined.instance
						: (staticType == Character.TYPE ? new Integer(
								((Character) obj).charValue()) : obj);
			} else {
				if (!this.isJavaPrimitiveWrap()) {
					if (obj instanceof String || obj instanceof Number
							|| obj instanceof Boolean) {
						return obj;
					}

					if (obj instanceof Character) {
						return String.valueOf(((Character) obj).charValue());
					}
				}

				Class cls = obj.getClass();
				if (cls.isArray()) {
					return NativeJavaArray.wrap(scope, obj);
				} else {
					return this.wrapAsJavaObject(cx, scope, obj, staticType);
				}
			}
		} else {
			return obj;
		}
	}

	public Scriptable wrapNewObject(Context cx, Scriptable scope, Object obj) {
		if (obj instanceof Scriptable) {
			return (Scriptable) obj;
		} else {
			Class cls = obj.getClass();
			return (Scriptable) (cls.isArray() ? NativeJavaArray.wrap(scope,
					obj) : this.wrapAsJavaObject(cx, scope, obj, (Class) null));
		}
	}

	public Scriptable wrapAsJavaObject(Context cx, Scriptable scope,
			Object javaObject, Class<?> staticType) {
		NativeJavaObject wrap = new NativeJavaObject(scope, javaObject,
				staticType);
		return wrap;
	}

	public final boolean isJavaPrimitiveWrap() {
		return this.javaPrimitiveWrap;
	}

	public final void setJavaPrimitiveWrap(boolean value) {
		Context cx = Context.getCurrentContext();
		if (cx != null && cx.isSealed()) {
			Context.onSealedMutation();
		}

		this.javaPrimitiveWrap = value;
	}
}