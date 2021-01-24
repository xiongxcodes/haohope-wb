/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import java.util.HashSet;
import java.util.Set;
import org.mozilla.javascript.ClassShutter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.NativeJavaClass;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class NativeJavaPackage extends ScriptableObject {
	static final long serialVersionUID = 7445054382212031523L;
	private String packageName;
	private ClassLoader classLoader;
	private Set<String> negativeCache;

	NativeJavaPackage(boolean internalUsage, String packageName,
			ClassLoader classLoader) {
		this.negativeCache = null;
		this.packageName = packageName;
		this.classLoader = classLoader;
	}

	public NativeJavaPackage(String packageName, ClassLoader classLoader) {
		this(false, packageName, classLoader);
	}

	public NativeJavaPackage(String packageName) {
		this(false, packageName, Context.getCurrentContext()
				.getApplicationClassLoader());
	}

	public String getClassName() {
		return "JavaPackage";
	}

	public boolean has(String id, Scriptable start) {
		return true;
	}

	public boolean has(int index, Scriptable start) {
		return false;
	}

	public void put(String id, Scriptable start, Object value) {
	}

	public void put(int index, Scriptable start, Object value) {
		throw Context.reportRuntimeError0("msg.pkg.int");
	}

	public Object get(String id, Scriptable start) {
		return this.getPkgProperty(id, start, true);
	}

	public Object get(int index, Scriptable start) {
		return NOT_FOUND;
	}

	NativeJavaPackage forcePackage(String name, Scriptable scope) {
		Object cached = super.get(name, this);
		if (cached != null && cached instanceof NativeJavaPackage) {
			return (NativeJavaPackage) cached;
		} else {
			String newPackage = this.packageName.length() == 0
					? name
					: this.packageName + "." + name;
			NativeJavaPackage pkg = new NativeJavaPackage(true, newPackage,
					this.classLoader);
			ScriptRuntime.setObjectProtoAndParent(pkg, scope);
			super.put(name, this, pkg);
			return pkg;
		}
	}

	synchronized Object getPkgProperty(String name, Scriptable start,
			boolean createPkg) {
		Object cached = super.get(name, start);
		if (cached != NOT_FOUND) {
			return cached;
		} else if (this.negativeCache != null
				&& this.negativeCache.contains(name)) {
			return null;
		} else {
			String className = this.packageName.length() == 0
					? name
					: this.packageName + '.' + name;
			Context cx = Context.getContext();
			ClassShutter shutter = cx.getClassShutter();
			Object newValue = null;
			if (shutter == null || shutter.visibleToScripts(className)) {
				Class pkg = null;
				if (this.classLoader != null) {
					pkg = Kit.classOrNull(this.classLoader, className);
				} else {
					pkg = Kit.classOrNull(className);
				}

				if (pkg != null) {
					newValue = new NativeJavaClass(getTopLevelScope(this), pkg);
					((Scriptable) newValue).setPrototype(this.getPrototype());
				}
			}

			if (newValue == null) {
				if (createPkg) {
					NativeJavaPackage pkg1 = new NativeJavaPackage(true,
							className, this.classLoader);
					ScriptRuntime.setObjectProtoAndParent(pkg1,
							this.getParentScope());
					newValue = pkg1;
				} else {
					if (this.negativeCache == null) {
						this.negativeCache = new HashSet();
					}

					this.negativeCache.add(name);
				}
			}

			if (newValue != null) {
				super.put(name, start, newValue);
			}

			return newValue;
		}
	}

	public Object getDefaultValue(Class<?> ignored) {
		return this.toString();
	}

	public String toString() {
		return "[JavaPackage " + this.packageName + "]";
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof NativeJavaPackage)) {
			return false;
		} else {
			NativeJavaPackage njp = (NativeJavaPackage) obj;
			return this.packageName.equals(njp.packageName)
					&& this.classLoader == njp.classLoader;
		}
	}

	public int hashCode() {
		return this.packageName.hashCode()
				^ (this.classLoader == null ? 0 : this.classLoader.hashCode());
	}
}