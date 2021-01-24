/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;

public class SecurityUtilities {
	public static String getSystemProperty(final String name) {
		return (String) AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				return System.getProperty(name);
			}
		});
	}

	public static ProtectionDomain getProtectionDomain(final Class<?> clazz) {
		return (ProtectionDomain) AccessController
				.doPrivileged(new PrivilegedAction() {
					public Object run() {
						return clazz.getProtectionDomain();
					}
				});
	}
}