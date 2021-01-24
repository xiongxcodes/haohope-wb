/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Evaluator;
import org.mozilla.javascript.SecurityUtilities;

public abstract class RhinoException extends RuntimeException {
	private String sourceName;
	private int lineNumber;
	private String lineSource;
	private int columnNumber;
	Object interpreterStackInfo;
	int[] interpreterLineData;

	RhinoException() {
		Evaluator e = Context.createInterpreter();
		if (e != null) {
			e.captureStackInfo(this);
		}

	}

	RhinoException(String details) {
		super(details);
		Evaluator e = Context.createInterpreter();
		if (e != null) {
			e.captureStackInfo(this);
		}

	}

	public final String getMessage() {
		String details = this.details();
		if (this.sourceName != null && this.lineNumber > 0) {
			StringBuffer buf = new StringBuffer(details);
			buf.append(" (");
			if (this.sourceName != null) {
				buf.append(this.sourceName);
			}

			if (this.lineNumber > 0) {
				buf.append('#');
				buf.append(this.lineNumber);
			}

			buf.append(')');
			return buf.toString();
		} else {
			return details;
		}
	}

	public String details() {
		return super.getMessage();
	}

	public final String sourceName() {
		return this.sourceName;
	}

	public final void initSourceName(String sourceName) {
		if (sourceName == null) {
			throw new IllegalArgumentException();
		} else if (this.sourceName != null) {
			throw new IllegalStateException();
		} else {
			this.sourceName = sourceName;
		}
	}

	public final int lineNumber() {
		return this.lineNumber;
	}

	public final void initLineNumber(int lineNumber) {
		if (lineNumber <= 0) {
			throw new IllegalArgumentException(String.valueOf(lineNumber));
		} else if (this.lineNumber > 0) {
			throw new IllegalStateException();
		} else {
			this.lineNumber = lineNumber;
		}
	}

	public final int columnNumber() {
		return this.columnNumber;
	}

	public final void initColumnNumber(int columnNumber) {
		if (columnNumber <= 0) {
			throw new IllegalArgumentException(String.valueOf(columnNumber));
		} else if (this.columnNumber > 0) {
			throw new IllegalStateException();
		} else {
			this.columnNumber = columnNumber;
		}
	}

	public final String lineSource() {
		return this.lineSource;
	}

	public final void initLineSource(String lineSource) {
		if (lineSource == null) {
			throw new IllegalArgumentException();
		} else if (this.lineSource != null) {
			throw new IllegalStateException();
		} else {
			this.lineSource = lineSource;
		}
	}

	final void recordErrorOrigin(String sourceName, int lineNumber,
			String lineSource, int columnNumber) {
		if (lineNumber == -1) {
			lineNumber = 0;
		}

		if (sourceName != null) {
			this.initSourceName(sourceName);
		}

		if (lineNumber != 0) {
			this.initLineNumber(lineNumber);
		}

		if (lineSource != null) {
			this.initLineSource(lineSource);
		}

		if (columnNumber != 0) {
			this.initColumnNumber(columnNumber);
		}

	}

	private String generateStackTrace() {
		CharArrayWriter writer = new CharArrayWriter();
		super.printStackTrace(new PrintWriter(writer));
		String origStackTrace = writer.toString();
		Evaluator e = Context.createInterpreter();
		return e != null ? e.getPatchedStack(this, origStackTrace) : null;
	}

	public String getScriptStackTrace() {
		return this.getScriptStackTrace(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".js");
			}
		});
	}

	public String getScriptStackTrace(FilenameFilter filter) {
		List interpreterStack = null;
		Evaluator interpreter = Context.createInterpreter();
		if (interpreter != null) {
			interpreterStack = interpreter.getScriptStack(this);
		}

		int interpreterStackIndex = 0;
		StringBuffer buffer = new StringBuffer();
		String lineSeparator = SecurityUtilities
				.getSystemProperty("line.separator");
		StackTraceElement[] stack = this.getStackTrace();

		for (int i = 0; i < stack.length; ++i) {
			StackTraceElement e = stack[i];
			String name = e.getFileName();
			if (e.getLineNumber() > -1 && name != null
					&& filter.accept((File) null, name)) {
				buffer.append("\tat ");
				buffer.append(e.getFileName());
				buffer.append(':');
				buffer.append(e.getLineNumber());
				buffer.append(lineSeparator);
			} else if (interpreterStack != null
					&& interpreterStack.size() > interpreterStackIndex
					&& "org.mozilla.javascript.Interpreter".equals(e
							.getClassName())
					&& "interpretLoop".equals(e.getMethodName())) {
				buffer.append((String) interpreterStack
						.get(interpreterStackIndex++));
			}
		}

		return buffer.toString();
	}

	public void printStackTrace(PrintWriter s) {
		if (this.interpreterStackInfo == null) {
			super.printStackTrace(s);
		} else {
			s.print(this.generateStackTrace());
		}

	}

	public void printStackTrace(PrintStream s) {
		if (this.interpreterStackInfo == null) {
			super.printStackTrace(s);
		} else {
			s.print(this.generateStackTrace());
		}

	}
}