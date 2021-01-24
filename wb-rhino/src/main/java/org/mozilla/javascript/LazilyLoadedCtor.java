/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public final class LazilyLoadedCtor implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final int STATE_BEFORE_INIT = 0;
	private static final int STATE_INITIALIZING = 1;
	private static final int STATE_WITH_VALUE = 2;
	private final ScriptableObject scope;
	private final String propertyName;
	private final String className;
	private final boolean sealed;
	private Object initializedValue;
	private int state;

	public LazilyLoadedCtor(ScriptableObject scope, String propertyName,
			String className, boolean sealed) {
		this.scope = scope;
		this.propertyName = propertyName;
		this.className = className;
		this.sealed = sealed;
		this.state = 0;
		scope.addLazilyInitializedValue(propertyName, 0, this, 2);
	}

	void init() {
		synchronized (this) {
			if (this.state == 1) {
				throw new IllegalStateException("Recursive initialization for "
						+ this.propertyName);
			} else {
				if (this.state == 0) {
					this.state = 1;
					Object value = Scriptable.NOT_FOUND;

					try {
						value = this.buildValue();
					} finally {
						this.initializedValue = value;
						this.state = 2;
					}
				}

			}
		}
	}

	Object getValue() {
		if (this.state != 2) {
			throw new IllegalStateException(this.propertyName);
		} else {
			return this.initializedValue;
		}
	}

	private Object buildValue() {
		Class cl = this.cast(Kit.classOrNull(this.className));
		if (cl != null) {
			try {
				BaseFunction ex = ScriptableObject.buildClassCtor(this.scope,
						cl, this.sealed, false);
				if (ex != null) {
					return ex;
				}

				Object ex1 = this.scope.get(this.propertyName, this.scope);
				if (ex1 != Scriptable.NOT_FOUND) {
					return ex1;
				}
			} catch (InvocationTargetException arg3) {
				Throwable target = arg3.getTargetException();
				if (target instanceof RuntimeException) {
					throw (RuntimeException) target;
				}
			} catch (RhinoException arg4) {
				;
			} catch (InstantiationException arg5) {
				;
			} catch (IllegalAccessException arg6) {
				;
			} catch (SecurityException arg7) {
				;
			}
		}

		return Scriptable.NOT_FOUND;
	}

	private Class<? extends Scriptable> cast(Class<?> cl) {
		return (Class<? extends Scriptable>) cl;
	}
}