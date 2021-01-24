/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.debug;

public interface DebuggableScript {
	boolean isTopLevel();

	boolean isFunction();

	String getFunctionName();

	int getParamCount();

	int getParamAndVarCount();

	String getParamOrVarName(int arg0);

	String getSourceName();

	boolean isGeneratedScript();

	int[] getLineNumbers();

	int getFunctionCount();

	DebuggableScript getFunction(int arg0);

	DebuggableScript getParent();
}