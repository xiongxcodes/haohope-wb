/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.ScriptRuntime;

class DefaultErrorReporter implements ErrorReporter {
	static final DefaultErrorReporter instance = new DefaultErrorReporter();
	private boolean forEval;
	private ErrorReporter chainedReporter;

	static ErrorReporter forEval(ErrorReporter reporter) {
		DefaultErrorReporter r = new DefaultErrorReporter();
		r.forEval = true;
		r.chainedReporter = reporter;
		return r;
	}

	public void warning(String message, String sourceURI, int line,
			String lineText, int lineOffset) {
		if (this.chainedReporter != null) {
			this.chainedReporter.warning(message, sourceURI, line, lineText,
					lineOffset);
		}

	}

	public void error(String message, String sourceURI, int line,
			String lineText, int lineOffset) {
		if (this.forEval) {
			String error = "SyntaxError";
			String TYPE_ERROR_NAME = "TypeError";
			String DELIMETER = ": ";
			String prefix = "TypeError: ";
			if (message.startsWith("TypeError: ")) {
				error = "TypeError";
				message = message.substring("TypeError: ".length());
			}

			throw ScriptRuntime.constructError(error, message, sourceURI, line,
					lineText, lineOffset);
		} else if (this.chainedReporter != null) {
			this.chainedReporter.error(message, sourceURI, line, lineText,
					lineOffset);
		} else {
			throw this.runtimeError(message, sourceURI, line, lineText,
					lineOffset);
		}
	}

	public EvaluatorException runtimeError(String message, String sourceURI,
			int line, String lineText, int lineOffset) {
		return this.chainedReporter != null
				? this.chainedReporter.runtimeError(message, sourceURI, line,
						lineText, lineOffset) : new EvaluatorException(message,
						sourceURI, line, lineText, lineOffset);
	}
}