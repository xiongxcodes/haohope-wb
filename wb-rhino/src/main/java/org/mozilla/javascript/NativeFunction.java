/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Decompiler;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.NativeCall;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.UintMap;
import org.mozilla.javascript.debug.DebuggableScript;

public abstract class NativeFunction extends BaseFunction {
	public final void initScriptFunction(Context cx, Scriptable scope) {
		ScriptRuntime.setFunctionProtoAndParent(this, scope);
	}

	final String decompile(int indent, int flags) {
		String encodedSource = this.getEncodedSource();
		if (encodedSource == null) {
			return super.decompile(indent, flags);
		} else {
			UintMap properties = new UintMap(1);
			properties.put(1, indent);
			return Decompiler.decompile(encodedSource, flags, properties);
		}
	}

	public int getLength() {
		int paramCount = this.getParamCount();
		if (this.getLanguageVersion() != 120) {
			return paramCount;
		} else {
			Context cx = Context.getContext();
			NativeCall activation = ScriptRuntime.findFunctionActivation(cx,
					this);
			return activation == null
					? paramCount
					: activation.originalArgs.length;
		}
	}

	public int getArity() {
		return this.getParamCount();
	}

	public String jsGet_name() {
		return this.getFunctionName();
	}

	public String getEncodedSource() {
		return null;
	}

	public DebuggableScript getDebuggableView() {
		return null;
	}

	public Object resumeGenerator(Context cx, Scriptable scope, int operation,
			Object state, Object value) {
		throw new EvaluatorException("resumeGenerator() not implemented");
	}

	protected abstract int getLanguageVersion();

	protected abstract int getParamCount();

	protected abstract int getParamAndVarCount();

	protected abstract String getParamOrVarName(int arg0);

	protected boolean getParamOrVarConst(int index) {
		return false;
	}
}