/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import java.util.List;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptOrFnNode;
import org.mozilla.javascript.Scriptable;

public interface Evaluator {
	Object compile(CompilerEnvirons arg0, ScriptOrFnNode arg1, String arg2,
			boolean arg3);

	Function createFunctionObject(Context arg0, Scriptable arg1, Object arg2,
			Object arg3);

	Script createScriptObject(Object arg0, Object arg1);

	void captureStackInfo(RhinoException arg0);

	String getSourcePositionFromStack(Context arg0, int[] arg1);

	String getPatchedStack(RhinoException arg0, String arg1);

	List<String> getScriptStack(RhinoException arg0);

	void setEvalScriptFlag(Script arg0);
}