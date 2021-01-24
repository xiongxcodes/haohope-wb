/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.tools.shell;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ErrorReporter;

public class ShellContextFactory extends ContextFactory {
	private boolean strictMode;
	private boolean warningAsError;
	private int languageVersion;
	private int optimizationLevel;
	private boolean generatingDebug;
	private ErrorReporter errorReporter;
	private String characterEncoding;

	protected boolean hasFeature(Context cx, int featureIndex) {
		switch (featureIndex) {
			case 8 :
			case 9 :
			case 11 :
				return this.strictMode;
			case 10 :
			default :
				return super.hasFeature(cx, featureIndex);
			case 12 :
				return this.warningAsError;
		}
	}

	protected void onContextCreated(Context cx) {
		cx.setLanguageVersion(this.languageVersion);
		cx.setOptimizationLevel(this.optimizationLevel);
		if (this.errorReporter != null) {
			cx.setErrorReporter(this.errorReporter);
		}

		cx.setGeneratingDebug(this.generatingDebug);
		super.onContextCreated(cx);
	}

	public void setStrictMode(boolean flag) {
		this.checkNotSealed();
		this.strictMode = flag;
	}

	public void setWarningAsError(boolean flag) {
		this.checkNotSealed();
		this.warningAsError = flag;
	}

	public void setLanguageVersion(int version) {
		Context.checkLanguageVersion(version);
		this.checkNotSealed();
		this.languageVersion = version;
	}

	public void setOptimizationLevel(int optimizationLevel) {
		Context.checkOptimizationLevel(optimizationLevel);
		this.checkNotSealed();
		this.optimizationLevel = optimizationLevel;
	}

	public void setErrorReporter(ErrorReporter errorReporter) {
		if (errorReporter == null) {
			throw new IllegalArgumentException();
		} else {
			this.errorReporter = errorReporter;
		}
	}

	public void setGeneratingDebug(boolean generatingDebug) {
		this.generatingDebug = generatingDebug;
	}

	public String getCharacterEncoding() {
		return this.characterEncoding;
	}

	public void setCharacterEncoding(String characterEncoding) {
		this.characterEncoding = characterEncoding;
	}
}