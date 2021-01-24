/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.mozilla.javascript.JavaMembers;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.JavaAdapter.JavaAdapterSignature;

public class ClassCache implements Serializable {
	private static final long serialVersionUID = -8866246036237312215L;
	private static final Object AKEY = "ClassCache";
	private volatile boolean cachingIsEnabled = true;
	private transient HashMap<Class<?>, JavaMembers> classTable;
	private transient HashMap<JavaAdapterSignature, Class<?>> classAdapterCache;
	private transient HashMap<Class<?>, Object> interfaceAdapterCache;
	private int generatedClassSerial;

	public static ClassCache get(Scriptable scope) {
		ClassCache cache = (ClassCache) ScriptableObject.getTopScopeValue(
				scope, AKEY);
		if (cache == null) {
			throw new RuntimeException(
					"Can\'t find top level scope for ClassCache.get");
		} else {
			return cache;
		}
	}

	public boolean associate(ScriptableObject topScope) {
		if (topScope.getParentScope() != null) {
			throw new IllegalArgumentException();
		} else {
			return this == topScope.associateValue(AKEY, this);
		}
	}

	public synchronized void clearCaches() {
		this.classTable = null;
		this.classAdapterCache = null;
		this.interfaceAdapterCache = null;
	}

	public final boolean isCachingEnabled() {
		return this.cachingIsEnabled;
	}

	public synchronized void setCachingEnabled(boolean enabled) {
		if (enabled != this.cachingIsEnabled) {
			if (!enabled) {
				this.clearCaches();
			}

			this.cachingIsEnabled = enabled;
		}
	}

	Map<Class<?>, JavaMembers> getClassCacheMap() {
		if (this.classTable == null) {
			this.classTable = new HashMap();
		}

		return this.classTable;
	}

	Map<JavaAdapterSignature, Class<?>> getInterfaceAdapterCacheMap() {
		if (this.classAdapterCache == null) {
			this.classAdapterCache = new HashMap();
		}

		return this.classAdapterCache;
	}

	public boolean isInvokerOptimizationEnabled() {
		return false;
	}

	public synchronized void setInvokerOptimizationEnabled(boolean enabled) {
	}

	public final synchronized int newClassSerialNumber() {
		return ++this.generatedClassSerial;
	}

	Object getInterfaceAdapter(Class<?> cl) {
		return this.interfaceAdapterCache == null
				? null
				: this.interfaceAdapterCache.get(cl);
	}

	synchronized void cacheInterfaceAdapter(Class<?> cl, Object iadapter) {
		if (this.cachingIsEnabled) {
			if (this.interfaceAdapterCache == null) {
				this.interfaceAdapterCache = new HashMap();
			}

			this.interfaceAdapterCache.put(cl, iadapter);
		}

	}
}