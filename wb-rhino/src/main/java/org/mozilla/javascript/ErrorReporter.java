/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import org.mozilla.javascript.EvaluatorException;

public interface ErrorReporter {
	void warning(String arg0, String arg1, int arg2, String arg3, int arg4);

	void error(String arg0, String arg1, int arg2, String arg3, int arg4);

	EvaluatorException runtimeError(String arg0, String arg1, int arg2,
			String arg3, int arg4);
}