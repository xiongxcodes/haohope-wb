/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;

public class JavaScriptException extends RhinoException {
	static final long serialVersionUID = -7666130513694669293L;
	private Object value;

	public JavaScriptException(Object value) {
		this(value, "", 0);
	}

	public JavaScriptException(Object value, String sourceName, int lineNumber) {
		this.recordErrorOrigin(sourceName, lineNumber, (String) null, 0);
		this.value = value;
	}

	public String details() {
		try {
			return ScriptRuntime.toString(this.value);
		} catch (RuntimeException arg1) {
			return this.value == null
					? "null"
					: (this.value instanceof Scriptable
							? ScriptRuntime
									.defaultObjectToString((Scriptable) this.value)
							: this.value.toString());
		}
	}

	public Object getValue() {
		return this.value;
	}

	public String getSourceName() {
		return this.sourceName();
	}

	public int getLineNumber() {
		return this.lineNumber();
	}
}