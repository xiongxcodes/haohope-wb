/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import org.mozilla.javascript.GeneratedClassLoader;
import org.mozilla.javascript.SecurityUtilities;

public class DefiningClassLoader extends ClassLoader
		implements
			GeneratedClassLoader {
	private final ClassLoader parentLoader;

	public DefiningClassLoader() {
		this.parentLoader = this.getClass().getClassLoader();
	}

	public DefiningClassLoader(ClassLoader parentLoader) {
		this.parentLoader = parentLoader;
	}

	public Class<?> defineClass(String name, byte[] data) {
		return super.defineClass(name, data, 0, data.length,
				SecurityUtilities.getProtectionDomain(this.getClass()));
	}

	public void linkClass(Class<?> cl) {
		this.resolveClass(cl);
	}

	public Class<?> loadClass(String name, boolean resolve)
			throws ClassNotFoundException {
		Class cl = this.findLoadedClass(name);
		if (cl == null) {
			if (this.parentLoader != null) {
				cl = this.parentLoader.loadClass(name);
			} else {
				cl = this.findSystemClass(name);
			}
		}

		if (resolve) {
			this.resolveClass(cl);
		}

		return cl;
	}
}