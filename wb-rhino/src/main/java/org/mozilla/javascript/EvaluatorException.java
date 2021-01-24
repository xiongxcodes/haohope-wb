/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import org.mozilla.javascript.RhinoException;

public class EvaluatorException extends RhinoException {
	static final long serialVersionUID = -8743165779676009808L;

	public EvaluatorException(String detail) {
		super(detail);
	}

	public EvaluatorException(String detail, String sourceName, int lineNumber) {
		this(detail, sourceName, lineNumber, (String) null, 0);
	}

	public EvaluatorException(String detail, String sourceName, int lineNumber,
			String lineSource, int columnNumber) {
		super(detail);
		this.recordErrorOrigin(sourceName, lineNumber, lineSource, columnNumber);
	}

	public String getSourceName() {
		return this.sourceName();
	}

	public int getLineNumber() {
		return this.lineNumber();
	}

	public int getColumnNumber() {
		return this.columnNumber();
	}

	public String getLineSource() {
		return this.lineSource();
	}
}