/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Interpreter;
import org.mozilla.javascript.InterpreterData;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.NativeFunction;
import org.mozilla.javascript.RegExpProxy;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.SecurityController;
import org.mozilla.javascript.debug.DebuggableScript;

final class InterpretedFunction extends NativeFunction implements Script {
	static final long serialVersionUID = 541475680333911468L;
	InterpreterData idata;
	SecurityController securityController;
	Object securityDomain;
	Scriptable[] functionRegExps;

	private InterpretedFunction(InterpreterData idata,
			Object staticSecurityDomain) {
		this.idata = idata;
		Context cx = Context.getContext();
		SecurityController sc = cx.getSecurityController();
		Object dynamicDomain;
		if (sc != null) {
			dynamicDomain = sc.getDynamicSecurityDomain(staticSecurityDomain);
		} else {
			if (staticSecurityDomain != null) {
				throw new IllegalArgumentException();
			}

			dynamicDomain = null;
		}

		this.securityController = sc;
		this.securityDomain = dynamicDomain;
	}

	private InterpretedFunction(InterpretedFunction parent, int index) {
		this.idata = parent.idata.itsNestedFunctions[index];
		this.securityController = parent.securityController;
		this.securityDomain = parent.securityDomain;
	}

	static InterpretedFunction createScript(InterpreterData idata,
			Object staticSecurityDomain) {
		InterpretedFunction f = new InterpretedFunction(idata,
				staticSecurityDomain);
		return f;
	}

	static InterpretedFunction createFunction(Context cx, Scriptable scope,
			InterpreterData idata, Object staticSecurityDomain) {
		InterpretedFunction f = new InterpretedFunction(idata,
				staticSecurityDomain);
		f.initInterpretedFunction(cx, scope);
		return f;
	}

	static InterpretedFunction createFunction(Context cx, Scriptable scope,
			InterpretedFunction parent, int index) {
		InterpretedFunction f = new InterpretedFunction(parent, index);
		f.initInterpretedFunction(cx, scope);
		return f;
	}

	Scriptable[] createRegExpWraps(Context cx, Scriptable scope) {
		if (this.idata.itsRegExpLiterals == null) {
			Kit.codeBug();
		}

		RegExpProxy rep = ScriptRuntime.checkRegExpProxy(cx);
		int N = this.idata.itsRegExpLiterals.length;
		Scriptable[] array = new Scriptable[N];

		for (int i = 0; i != N; ++i) {
			array[i] = rep.wrapRegExp(cx, scope,
					this.idata.itsRegExpLiterals[i]);
		}

		return array;
	}

	private void initInterpretedFunction(Context cx, Scriptable scope) {
		this.initScriptFunction(cx, scope);
		if (this.idata.itsRegExpLiterals != null) {
			this.functionRegExps = this.createRegExpWraps(cx, scope);
		}

	}

	public String getFunctionName() {
		return this.idata.itsName == null ? "" : this.idata.itsName;
	}

	public Object call(Context cx, Scriptable scope, Scriptable thisObj,
			Object[] args) {
		return !ScriptRuntime.hasTopCall(cx) ? ScriptRuntime.doTopCall(this,
				cx, scope, thisObj, args) : Interpreter.interpret(this, cx,
				scope, thisObj, args);
	}

	public Object exec(Context cx, Scriptable scope) {
		if (!this.isScript()) {
			throw new IllegalStateException();
		} else {
			return !ScriptRuntime.hasTopCall(cx)
					? ScriptRuntime.doTopCall(this, cx, scope, scope,
							ScriptRuntime.emptyArgs) : Interpreter.interpret(
							this, cx, scope, scope, ScriptRuntime.emptyArgs);
		}
	}

	public boolean isScript() {
		return this.idata.itsFunctionType == 0;
	}

	public String getEncodedSource() {
		return Interpreter.getEncodedSource(this.idata);
	}

	public DebuggableScript getDebuggableView() {
		return this.idata;
	}

	public Object resumeGenerator(Context cx, Scriptable scope, int operation,
			Object state, Object value) {
		return Interpreter.resumeGenerator(cx, scope, operation, state, value);
	}

	protected int getLanguageVersion() {
		return this.idata.languageVersion;
	}

	protected int getParamCount() {
		return this.idata.argCount;
	}

	protected int getParamAndVarCount() {
		return this.idata.argNames.length;
	}

	protected String getParamOrVarName(int index) {
		return this.idata.argNames[index];
	}

	protected boolean getParamOrVarConst(int index) {
		return this.idata.argIsConst[index];
	}
}