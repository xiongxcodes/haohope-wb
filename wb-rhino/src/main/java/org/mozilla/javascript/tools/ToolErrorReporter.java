/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.tools;

import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.SecurityUtilities;
import org.mozilla.javascript.WrappedException;

public class ToolErrorReporter implements ErrorReporter {
	private final String messagePrefix;
	private boolean hasReportedErrorFlag;
	private boolean reportWarnings;
	private PrintStream err;

	public ToolErrorReporter(boolean reportWarnings) {
		this(reportWarnings, System.err);
	}

	public ToolErrorReporter(boolean reportWarnings, PrintStream err) {
		this.messagePrefix = "js: ";
		this.reportWarnings = reportWarnings;
		this.err = err;
	}

	public static String getMessage(String messageId) {
		return getMessage(messageId, (Object[]) null);
	}

	public static String getMessage(String messageId, String argument) {
		Object[] args = new Object[]{argument};
		return getMessage(messageId, args);
	}

	public static String getMessage(String messageId, Object arg1, Object arg2) {
		Object[] args = new Object[]{arg1, arg2};
		return getMessage(messageId, args);
	}

	public static String getMessage(String messageId, Object[] args) {
		Context cx = Context.getCurrentContext();
		Locale locale = cx == null ? Locale.getDefault() : cx.getLocale();
		ResourceBundle rb = ResourceBundle.getBundle(
				"org.mozilla.javascript.tools.resources.Messages", locale);

		String formatString;
		try {
			formatString = rb.getString(messageId);
		} catch (MissingResourceException arg6) {
			throw new RuntimeException(
					"no message resource found for message property "
							+ messageId);
		}

		if (args == null) {
			return formatString;
		} else {
			MessageFormat formatter = new MessageFormat(formatString);
			return formatter.format(args);
		}
	}

	private static String getExceptionMessage(RhinoException ex) {
		String msg;
		if (ex instanceof JavaScriptException) {
			msg = getMessage("msg.uncaughtJSException", ex.details());
		} else if (ex instanceof EcmaError) {
			msg = getMessage("msg.uncaughtEcmaError", ex.details());
		} else if (ex instanceof EvaluatorException) {
			msg = ex.details();
		} else {
			msg = ex.toString();
		}

		return msg;
	}

	public void warning(String message, String sourceName, int line,
			String lineSource, int lineOffset) {
		if (this.reportWarnings) {
			this.reportErrorMessage(message, sourceName, line, lineSource,
					lineOffset, true);
		}
	}

	public void error(String message, String sourceName, int line,
			String lineSource, int lineOffset) {
		this.hasReportedErrorFlag = true;
		this.reportErrorMessage(message, sourceName, line, lineSource,
				lineOffset, false);
	}

	public EvaluatorException runtimeError(String message, String sourceName,
			int line, String lineSource, int lineOffset) {
		return new EvaluatorException(message, sourceName, line, lineSource,
				lineOffset);
	}

	public boolean hasReportedError() {
		return this.hasReportedErrorFlag;
	}

	public boolean isReportingWarnings() {
		return this.reportWarnings;
	}

	public void setIsReportingWarnings(boolean reportWarnings) {
		this.reportWarnings = reportWarnings;
	}

	public static void reportException(ErrorReporter er, RhinoException ex) {
		if (er instanceof ToolErrorReporter) {
			((ToolErrorReporter) er).reportException(ex);
		} else {
			String msg = getExceptionMessage(ex);
			er.error(msg, ex.sourceName(), ex.lineNumber(), ex.lineSource(),
					ex.columnNumber());
		}

	}

	public void reportException(RhinoException ex) {
		if (ex instanceof WrappedException) {
			WrappedException lineSeparator = (WrappedException) ex;
			lineSeparator.printStackTrace(this.err);
		} else {
			String lineSeparator1 = SecurityUtilities
					.getSystemProperty("line.separator");
			String msg = getExceptionMessage(ex) + lineSeparator1
					+ ex.getScriptStackTrace();
			this.reportErrorMessage(msg, ex.sourceName(), ex.lineNumber(),
					ex.lineSource(), ex.columnNumber(), false);
		}

	}

	private void reportErrorMessage(String message, String sourceName,
			int line, String lineSource, int lineOffset, boolean justWarning) {
		if (line > 0) {
			String args = String.valueOf(line);
			Object[] args1;
			if (sourceName != null) {
				args1 = new Object[]{sourceName, args, message};
				message = getMessage("msg.format3", args1);
			} else {
				args1 = new Object[]{args, message};
				message = getMessage("msg.format2", args1);
			}
		} else {
			Object[] args2 = new Object[]{message};
			message = getMessage("msg.format1", args2);
		}

		if (justWarning) {
			message = getMessage("msg.warning", message);
		}

		this.err.println("js: " + message);
		if (null != lineSource) {
			this.err.println("js: " + lineSource);
			this.err.println("js: " + this.buildIndicator(lineOffset));
		}

	}

	private String buildIndicator(int offset) {
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < offset - 1; ++i) {
			sb.append(".");
		}

		sb.append("^");
		return sb.toString();
	}
}