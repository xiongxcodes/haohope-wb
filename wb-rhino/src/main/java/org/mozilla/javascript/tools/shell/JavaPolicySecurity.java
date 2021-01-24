/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.tools.shell;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.Enumeration;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.GeneratedClassLoader;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.tools.shell.Main;
import org.mozilla.javascript.tools.shell.SecurityProxy;

public class JavaPolicySecurity extends SecurityProxy {
	public Class<?> getStaticSecurityDomainClassInternal() {
		return ProtectionDomain.class;
	}

	public JavaPolicySecurity() {
		new CodeSource((URL) null, (Certificate[]) null);
	}

	protected void callProcessFileSecure(final Context cx,
			final Scriptable scope, final String filename) {
		AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				URL url = JavaPolicySecurity.this.getUrlObj(filename);
				ProtectionDomain staticDomain = JavaPolicySecurity.this
						.getUrlDomain(url);
				Main.processFileSecure(cx, scope, url.toExternalForm(),
						staticDomain);
				return null;
			}
		});
	}

	private URL getUrlObj(String url) {
		URL urlObj;
		try {
			urlObj = new URL(url);
		} catch (MalformedURLException arg6) {
			String curDir = System.getProperty("user.dir");
			curDir = curDir.replace('\\', '/');
			if (!curDir.endsWith("/")) {
				curDir = curDir + '/';
			}

			try {
				URL ex2 = new URL("file:" + curDir);
				urlObj = new URL(ex2, url);
			} catch (MalformedURLException arg5) {
				throw new RuntimeException("Can not construct file URL for \'"
						+ url + "\':" + arg5.getMessage());
			}
		}

		return urlObj;
	}

	private ProtectionDomain getUrlDomain(URL url) {
		CodeSource cs = new CodeSource(url, (Certificate[]) null);
		PermissionCollection pc = Policy.getPolicy().getPermissions(cs);
		return new ProtectionDomain(cs, pc);
	}

	public GeneratedClassLoader createClassLoader(ClassLoader parentLoader,
			Object securityDomain) {
		ProtectionDomain domain = (ProtectionDomain) securityDomain;
		return new JavaPolicySecurity.Loader(parentLoader, domain);
	}

	public Object getDynamicSecurityDomain(Object securityDomain) {
		ProtectionDomain staticDomain = (ProtectionDomain) securityDomain;
		return this.getDynamicDomain(staticDomain);
	}

	private ProtectionDomain getDynamicDomain(ProtectionDomain staticDomain) {
		JavaPolicySecurity.ContextPermissions p = new JavaPolicySecurity.ContextPermissions(
				staticDomain);
		ProtectionDomain contextDomain = new ProtectionDomain(
				(CodeSource) null, p);
		return contextDomain;
	}

	public Object callWithDomain(Object securityDomain, final Context cx,
			final Callable callable, final Scriptable scope,
			final Scriptable thisObj, final Object[] args) {
		ProtectionDomain staticDomain = (ProtectionDomain) securityDomain;
		ProtectionDomain dynamicDomain = this.getDynamicDomain(staticDomain);
		ProtectionDomain[] tmp = new ProtectionDomain[]{dynamicDomain};
		AccessControlContext restricted = new AccessControlContext(tmp);
		PrivilegedAction action = new PrivilegedAction() {
			public Object run() {
				return callable.call(cx, scope, thisObj, args);
			}
		};
		return AccessController.doPrivileged(action, restricted);
	}

	private static class ContextPermissions extends PermissionCollection {
		static final long serialVersionUID = -1721494496320750721L;
		AccessControlContext _context = AccessController.getContext();
		PermissionCollection _statisPermissions;

		ContextPermissions(ProtectionDomain staticDomain) {
			if (staticDomain != null) {
				this._statisPermissions = staticDomain.getPermissions();
			}

			this.setReadOnly();
		}

		public void add(Permission permission) {
			throw new RuntimeException("NOT IMPLEMENTED");
		}

		public boolean implies(Permission permission) {
			if (this._statisPermissions != null
					&& !this._statisPermissions.implies(permission)) {
				return false;
			} else {
				try {
					this._context.checkPermission(permission);
					return true;
				} catch (AccessControlException arg2) {
					return false;
				}
			}
		}

		public Enumeration<Permission> elements() {
			return new Enumeration() {
				public boolean hasMoreElements() {
					return false;
				}

				public Permission nextElement() {
					return null;
				}
			};
		}

		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append(this.getClass().getName());
			sb.append('@');
			sb.append(Integer.toHexString(System.identityHashCode(this)));
			sb.append(" (context=");
			sb.append(this._context);
			sb.append(", static_permitions=");
			sb.append(this._statisPermissions);
			sb.append(')');
			return sb.toString();
		}
	}

	private static class Loader extends ClassLoader
			implements
				GeneratedClassLoader {
		private ProtectionDomain domain;

		Loader(ClassLoader parent, ProtectionDomain domain) {
			super(parent != null ? parent : getSystemClassLoader());
			this.domain = domain;
		}

		public Class<?> defineClass(String name, byte[] data) {
			return super.defineClass(name, data, 0, data.length, this.domain);
		}

		public void linkClass(Class<?> cl) {
			this.resolveClass(cl);
		}
	}
}