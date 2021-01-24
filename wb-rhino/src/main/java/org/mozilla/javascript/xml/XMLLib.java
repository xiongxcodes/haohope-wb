/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.xml;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Ref;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public abstract class XMLLib {
	private static final Object XML_LIB_KEY = new Object();

	public static XMLLib extractFromScopeOrNull(Scriptable scope) {
		ScriptableObject so = ScriptRuntime.getLibraryScopeOrNull(scope);
		if (so == null) {
			return null;
		} else {
			ScriptableObject.getProperty(so, "XML");
			return (XMLLib) so.getAssociatedValue(XML_LIB_KEY);
		}
	}

	public static XMLLib extractFromScope(Scriptable scope) {
		XMLLib lib = extractFromScopeOrNull(scope);
		if (lib != null) {
			return lib;
		} else {
			String msg = ScriptRuntime.getMessage0("msg.XML.not.available");
			throw Context.reportRuntimeError(msg);
		}
	}

	protected final XMLLib bindToScope(Scriptable scope) {
		ScriptableObject so = ScriptRuntime.getLibraryScopeOrNull(scope);
		if (so == null) {
			throw new IllegalStateException();
		} else {
			return (XMLLib) so.associateValue(XML_LIB_KEY, this);
		}
	}

	public abstract boolean isXMLName(Context arg0, Object arg1);

	public abstract Ref nameRef(Context arg0, Object arg1, Scriptable arg2,
			int arg3);

	public abstract Ref nameRef(Context arg0, Object arg1, Object arg2,
			Scriptable arg3, int arg4);

	public abstract String escapeAttributeValue(Object arg0);

	public abstract String escapeTextValue(Object arg0);

	public abstract Object toDefaultXmlNamespace(Context arg0, Object arg1);

	public void setIgnoreComments(boolean b) {
		throw new UnsupportedOperationException();
	}

	public void setIgnoreWhitespace(boolean b) {
		throw new UnsupportedOperationException();
	}

	public void setIgnoreProcessingInstructions(boolean b) {
		throw new UnsupportedOperationException();
	}

	public void setPrettyPrinting(boolean b) {
		throw new UnsupportedOperationException();
	}

	public void setPrettyIndent(int i) {
		throw new UnsupportedOperationException();
	}

	public boolean isIgnoreComments() {
		throw new UnsupportedOperationException();
	}

	public boolean isIgnoreProcessingInstructions() {
		throw new UnsupportedOperationException();
	}

	public boolean isIgnoreWhitespace() {
		throw new UnsupportedOperationException();
	}

	public boolean isPrettyPrinting() {
		throw new UnsupportedOperationException();
	}

	public int getPrettyIndent() {
		throw new UnsupportedOperationException();
	}

	public abstract static class Factory {
		public static XMLLib.Factory create(final String className) {
			return new XMLLib.Factory() {
				public String getImplementationClassName() {
					return className;
				}
			};
		}

		public abstract String getImplementationClassName();
	}
}