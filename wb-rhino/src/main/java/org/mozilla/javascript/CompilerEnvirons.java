/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import java.util.Set;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.DefaultErrorReporter;
import org.mozilla.javascript.ErrorReporter;

public class CompilerEnvirons {
	private ErrorReporter errorReporter;
	private int languageVersion;
	private boolean generateDebugInfo;
	private boolean useDynamicScope;
	private boolean reservedKeywordAsIdentifier;
	private boolean allowMemberExprAsFunctionName;
	private boolean xmlAvailable;
	private int optimizationLevel;
	private boolean generatingSource;
	private boolean strictMode;
	private boolean warningAsError;
	private boolean generateObserverCount;
	Set<String> activationNames;

	public CompilerEnvirons() {
		this.errorReporter = DefaultErrorReporter.instance;
		this.languageVersion = 0;
		this.generateDebugInfo = true;
		this.useDynamicScope = false;
		this.reservedKeywordAsIdentifier = false;
		this.allowMemberExprAsFunctionName = false;
		this.xmlAvailable = true;
		this.optimizationLevel = 0;
		this.generatingSource = true;
		this.strictMode = false;
		this.warningAsError = false;
		this.generateObserverCount = false;
	}

	public void initFromContext(Context cx) {
		this.setErrorReporter(cx.getErrorReporter());
		this.languageVersion = cx.getLanguageVersion();
		this.useDynamicScope = cx.compileFunctionsWithDynamicScopeFlag;
		this.generateDebugInfo = !cx.isGeneratingDebugChanged()
				|| cx.isGeneratingDebug();
		this.reservedKeywordAsIdentifier = cx.hasFeature(3);
		this.allowMemberExprAsFunctionName = cx.hasFeature(2);
		this.strictMode = cx.hasFeature(11);
		this.warningAsError = cx.hasFeature(12);
		this.xmlAvailable = cx.hasFeature(6);
		this.optimizationLevel = cx.getOptimizationLevel();
		this.generatingSource = cx.isGeneratingSource();
		this.activationNames = cx.activationNames;
		this.generateObserverCount = cx.generateObserverCount;
	}

	public final ErrorReporter getErrorReporter() {
		return this.errorReporter;
	}

	public void setErrorReporter(ErrorReporter errorReporter) {
		if (errorReporter == null) {
			throw new IllegalArgumentException();
		} else {
			this.errorReporter = errorReporter;
		}
	}

	public final int getLanguageVersion() {
		return this.languageVersion;
	}

	public void setLanguageVersion(int languageVersion) {
		Context.checkLanguageVersion(languageVersion);
		this.languageVersion = languageVersion;
	}

	public final boolean isGenerateDebugInfo() {
		return this.generateDebugInfo;
	}

	public void setGenerateDebugInfo(boolean flag) {
		this.generateDebugInfo = flag;
	}

	public final boolean isUseDynamicScope() {
		return this.useDynamicScope;
	}

	public final boolean isReservedKeywordAsIdentifier() {
		return this.reservedKeywordAsIdentifier;
	}

	public void setReservedKeywordAsIdentifier(boolean flag) {
		this.reservedKeywordAsIdentifier = flag;
	}

	public final boolean isAllowMemberExprAsFunctionName() {
		return this.allowMemberExprAsFunctionName;
	}

	public void setAllowMemberExprAsFunctionName(boolean flag) {
		this.allowMemberExprAsFunctionName = flag;
	}

	public final boolean isXmlAvailable() {
		return this.xmlAvailable;
	}

	public void setXmlAvailable(boolean flag) {
		this.xmlAvailable = flag;
	}

	public final int getOptimizationLevel() {
		return this.optimizationLevel;
	}

	public void setOptimizationLevel(int level) {
		Context.checkOptimizationLevel(level);
		this.optimizationLevel = level;
	}

	public final boolean isGeneratingSource() {
		return this.generatingSource;
	}

	public final boolean isStrictMode() {
		return this.strictMode;
	}

	public final boolean reportWarningAsError() {
		return this.warningAsError;
	}

	public void setGeneratingSource(boolean generatingSource) {
		this.generatingSource = generatingSource;
	}

	public boolean isGenerateObserverCount() {
		return this.generateObserverCount;
	}

	public void setGenerateObserverCount(boolean generateObserverCount) {
		this.generateObserverCount = generateObserverCount;
	}
}