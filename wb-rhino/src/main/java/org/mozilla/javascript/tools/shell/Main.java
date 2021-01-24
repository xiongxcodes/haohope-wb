/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.tools.shell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.GeneratedClassLoader;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.SecurityController;
import org.mozilla.javascript.tools.SourceReader;
import org.mozilla.javascript.tools.ToolErrorReporter;
import org.mozilla.javascript.tools.shell.Global;
import org.mozilla.javascript.tools.shell.QuitAction;
import org.mozilla.javascript.tools.shell.SecurityProxy;
import org.mozilla.javascript.tools.shell.ShellContextFactory;

public class Main {
	public static ShellContextFactory shellContextFactory = new ShellContextFactory();
	public static Global global = new Global();
	protected static ToolErrorReporter errorReporter;
	protected static int exitCode = 0;
	private static final int EXITCODE_RUNTIME_ERROR = 3;
	private static final int EXITCODE_FILE_NOT_FOUND = 4;
	static boolean processStdin = true;
	static List<String> fileList = new ArrayList();
	private static SecurityProxy securityImpl;

	public static void main(String[] args) {
		try {
			if (Boolean.getBoolean("rhino.use_java_policy_security")) {
				initJavaPolicySecuritySupport();
			}
		} catch (SecurityException arg1) {
			arg1.printStackTrace(System.err);
		}

		int result = exec(args);
		if (result != 0) {
			System.exit(result);
		}

	}

	public static int exec(String[] origArgs) {
		errorReporter = new ToolErrorReporter(false, global.getErr());
		shellContextFactory.setErrorReporter(errorReporter);
		String[] args = processOptions(origArgs);
		if (processStdin) {
			fileList.add(null);
		}

		if (!global.initialized) {
			global.init(shellContextFactory);
		}

		Main.IProxy iproxy = new Main.IProxy(1);
		iproxy.args = args;
		shellContextFactory.call(iproxy);
		return exitCode;
	}

	static void processFiles(Context cx, String[] args) {
		Object[] array = new Object[args.length];
		System.arraycopy(args, 0, array, 0, args.length);
		Scriptable argsObj = cx.newArray(global, array);
		global.defineProperty("arguments", argsObj, 2);
		Iterator i$ = fileList.iterator();

		while (i$.hasNext()) {
			String file = (String) i$.next();
			processSource(cx, file);
		}

	}

	public static Global getGlobal() {
		return global;
	}

	public static String[] processOptions(String[] args) {
		for (int i = 0; i != args.length; ++i) {
			String arg = args[i];
			if (!arg.startsWith("-")) {
				processStdin = false;
				fileList.add(arg);
				String[] arg9 = new String[args.length - i - 1];
				System.arraycopy(args, i + 1, arg9, 0, args.length - i - 1);
				return arg9;
			}

			int iproxy;
			label134 : {
				String usageError;
				if (arg.equals("-version")) {
					++i;
					if (i == args.length) {
						usageError = arg;
					} else {
						label129 : {
							try {
								iproxy = Integer.parseInt(args[i]);
							} catch (NumberFormatException arg5) {
								usageError = args[i];
								break label129;
							}

							if (Context.isValidLanguageVersion(iproxy)) {
								shellContextFactory.setLanguageVersion(iproxy);
								continue;
							}

							usageError = args[i];
						}
					}
				} else if (!arg.equals("-opt") && !arg.equals("-O")) {
					if (arg.equals("-encoding")) {
						++i;
						if (i != args.length) {
							String arg7 = args[i];
							shellContextFactory.setCharacterEncoding(arg7);
							continue;
						}

						usageError = arg;
					} else {
						if (arg.equals("-strict")) {
							shellContextFactory.setStrictMode(true);
							errorReporter.setIsReportingWarnings(true);
							continue;
						}

						if (arg.equals("-fatal-warnings")) {
							shellContextFactory.setWarningAsError(true);
							continue;
						}

						if (arg.equals("-e")) {
							processStdin = false;
							++i;
							if (i != args.length) {
								if (!global.initialized) {
									global.init(shellContextFactory);
								}

								Main.IProxy arg8 = new Main.IProxy(2);
								arg8.scriptText = args[i];
								shellContextFactory.call(arg8);
								continue;
							}

							usageError = arg;
						} else {
							if (arg.equals("-w")) {
								errorReporter.setIsReportingWarnings(true);
								continue;
							}

							if (arg.equals("-f")) {
								processStdin = false;
								++i;
								if (i != args.length) {
									fileList.add(args[i].equals("-")
											? null
											: args[i]);
									continue;
								}

								usageError = arg;
							} else {
								if (arg.equals("-sealedlib")) {
									global.setSealedStdLib(true);
									continue;
								}

								if (arg.equals("-debug")) {
									shellContextFactory
											.setGeneratingDebug(true);
									continue;
								}

								if (arg.equals("-?") || arg.equals("-help")) {
									global.getOut().println(
											ToolErrorReporter.getMessage(
													"msg.shell.usage",
													Main.class.getName()));
									System.exit(1);
								}

								usageError = arg;
							}
						}
					}
				} else {
					++i;
					if (i == args.length) {
						usageError = arg;
					} else {
						label133 : {
							try {
								iproxy = Integer.parseInt(args[i]);
							} catch (NumberFormatException arg6) {
								usageError = args[i];
								break label133;
							}

							if (iproxy == -2) {
								iproxy = -1;
								break label134;
							}

							if (Context.isValidOptimizationLevel(iproxy)) {
								break label134;
							}

							usageError = args[i];
						}
					}
				}

				global.getOut().println(
						ToolErrorReporter.getMessage("msg.shell.invalid",
								usageError));
				global.getOut().println(
						ToolErrorReporter.getMessage("msg.shell.usage",
								Main.class.getName()));
				System.exit(1);
				return null;
			}

			shellContextFactory.setOptimizationLevel(iproxy);
		}

		return new String[0];
	}

	private static void initJavaPolicySecuritySupport() {
		Object exObj;
		try {
			Class ex = Class
					.forName("org.mozilla.javascript.tools.shell.JavaPolicySecurity");
			securityImpl = (SecurityProxy) ex.newInstance();
			SecurityController.initGlobal(securityImpl);
			return;
		} catch (ClassNotFoundException arg1) {
			exObj = arg1;
		} catch (IllegalAccessException arg2) {
			exObj = arg2;
		} catch (InstantiationException arg3) {
			exObj = arg3;
		} catch (LinkageError arg4) {
			exObj = arg4;
		}

		throw Kit.initCause(new IllegalStateException(
				"Can not load security support: " + exObj), (Throwable) exObj);
	}

	public static void processSource(Context cx, String filename) {
		if (filename != null && !filename.equals("-")) {
			processFile(cx, global, filename);
		} else {
			PrintStream ps = global.getErr();
			if (filename == null) {
				ps.println(cx.getImplementationVersion());
			}

			cx.setOptimizationLevel(-1);
			String charEnc = shellContextFactory.getCharacterEncoding();
			if (charEnc == null) {
				charEnc = System.getProperty("file.encoding");
			}

			BufferedReader in;
			try {
				in = new BufferedReader(new InputStreamReader(global.getIn(),
						charEnc));
			} catch (UnsupportedEncodingException arg12) {
				throw new UndeclaredThrowableException(arg12);
			}

			int lineno = 1;
			boolean hitEOF = false;

			label75 : while (true) {
				String source;
				Script arg14;
				do {
					if (hitEOF) {
						ps.println();
						break label75;
					}

					String[] prompts = global.getPrompts(cx);
					if (filename == null) {
						ps.print(prompts[0]);
					}

					ps.flush();
					source = "";

					while (true) {
						String script;
						try {
							script = in.readLine();
						} catch (IOException arg13) {
							ps.println(arg13.toString());
							break;
						}

						if (script == null) {
							hitEOF = true;
							break;
						}

						source = source + script + "\n";
						++lineno;
						if (cx.stringIsCompilableUnit(source)) {
							break;
						}

						ps.print(prompts[1]);
					}

					arg14 = loadScriptFromSource(cx, source, "<stdin>", lineno,
							(Object) null);
				} while (arg14 == null);

				Object result = evaluateScript(arg14, cx, global);
				if (result != Context.getUndefinedValue()
						&& (!(result instanceof Function) || !source.trim()
								.startsWith("function"))) {
					try {
						ps.println(Context.toString(result));
					} catch (RhinoException arg11) {
						ToolErrorReporter.reportException(
								cx.getErrorReporter(), arg11);
					}
				}

				NativeArray h = global.history;
				h.put((int) h.getLength(), h, source);
			}
		}

		System.gc();
	}

	public static void processFile(Context cx, Scriptable scope, String filename) {
		if (securityImpl == null) {
			processFileSecure(cx, scope, filename, (Object) null);
		} else {
			securityImpl.callProcessFileSecure(cx, scope, filename);
		}

	}

	static void processFileSecure(Context cx, Scriptable scope, String path,
			Object securityDomain) {
		Script script;
		if (path.endsWith(".class")) {
			script = loadCompiledScript(cx, path, securityDomain);
		} else {
			String source = (String) readFileOrUrl(path, true);
			if (source == null) {
				exitCode = 4;
				return;
			}

			if (source.length() > 0 && source.charAt(0) == 35) {
				for (int i = 1; i != source.length(); ++i) {
					char c = source.charAt(i);
					if (c == 10 || c == 13) {
						source = source.substring(i);
						break;
					}
				}
			}

			script = loadScriptFromSource(cx, source, path, 1, securityDomain);
		}

		if (script != null) {
			evaluateScript(script, cx, scope);
		}

	}

	public static Script loadScriptFromSource(Context cx, String scriptSource,
			String path, int lineno, Object securityDomain) {
		try {
			return cx.compileString(scriptSource, path, lineno, securityDomain);
		} catch (EvaluatorException arg6) {
			exitCode = 3;
		} catch (RhinoException arg7) {
			ToolErrorReporter.reportException(cx.getErrorReporter(), arg7);
			exitCode = 3;
		} catch (VirtualMachineError arg8) {
			arg8.printStackTrace();
			String msg = ToolErrorReporter.getMessage(
					"msg.uncaughtJSException", arg8.toString());
			exitCode = 3;
			Context.reportError(msg);
		}

		return null;
	}

	private static Script loadCompiledScript(Context cx, String path,
			Object securityDomain) {
		byte[] data = (byte[]) ((byte[]) readFileOrUrl(path, false));
		if (data == null) {
			exitCode = 4;
			return null;
		} else {
			int nameStart = path.lastIndexOf(47);
			if (nameStart < 0) {
				nameStart = 0;
			} else {
				++nameStart;
			}

			int nameEnd = path.lastIndexOf(46);
			if (nameEnd < nameStart) {
				nameEnd = path.length();
			}

			String name = path.substring(nameStart, nameEnd);

			try {
				GeneratedClassLoader inex = SecurityController.createLoader(
						cx.getApplicationClassLoader(), securityDomain);
				Class clazz = inex.defineClass(name, data);
				inex.linkClass(clazz);
				if (!Script.class.isAssignableFrom(clazz)) {
					throw Context
							.reportRuntimeError("msg.must.implement.Script");
				}

				return (Script) clazz.newInstance();
			} catch (RhinoException arg8) {
				ToolErrorReporter.reportException(cx.getErrorReporter(), arg8);
				exitCode = 3;
			} catch (IllegalAccessException arg9) {
				exitCode = 3;
				Context.reportError(arg9.toString());
			} catch (InstantiationException arg10) {
				exitCode = 3;
				Context.reportError(arg10.toString());
			}

			return null;
		}
	}

	public static Object evaluateScript(Script script, Context cx,
			Scriptable scope) {
		try {
			return script.exec(cx, scope);
		} catch (RhinoException arg4) {
			ToolErrorReporter.reportException(cx.getErrorReporter(), arg4);
			exitCode = 3;
		} catch (VirtualMachineError arg5) {
			arg5.printStackTrace();
			String msg = ToolErrorReporter.getMessage(
					"msg.uncaughtJSException", arg5.toString());
			exitCode = 3;
			Context.reportError(msg);
		}

		return Context.getUndefinedValue();
	}

	public static InputStream getIn() {
		return getGlobal().getIn();
	}

	public static void setIn(InputStream in) {
		getGlobal().setIn(in);
	}

	public static PrintStream getOut() {
		return getGlobal().getOut();
	}

	public static void setOut(PrintStream out) {
		getGlobal().setOut(out);
	}

	public static PrintStream getErr() {
		return getGlobal().getErr();
	}

	public static void setErr(PrintStream err) {
		getGlobal().setErr(err);
	}

	private static Object readFileOrUrl(String path, boolean convertToString) {
		try {
			return SourceReader.readFileOrUrl(path, convertToString,
					shellContextFactory.getCharacterEncoding());
		} catch (IOException arg2) {
			Context.reportError(ToolErrorReporter.getMessage(
					"msg.couldnt.read.source", path, arg2.getMessage()));
			return null;
		}
	}

	static {
		global.initQuitAction(new Main.IProxy(3));
	}

	private static class IProxy implements ContextAction, QuitAction {
		private static final int PROCESS_FILES = 1;
		private static final int EVAL_INLINE_SCRIPT = 2;
		private static final int SYSTEM_EXIT = 3;
		private int type;
		String[] args;
		String scriptText;

		IProxy(int type) {
			this.type = type;
		}

		public Object run(Context cx) {
			if (this.type == 1) {
				Main.processFiles(cx, this.args);
			} else {
				if (this.type != 2) {
					throw Kit.codeBug();
				}

				Script script = Main.loadScriptFromSource(cx, this.scriptText,
						"<command>", 1, (Object) null);
				if (script != null) {
					Main.evaluateScript(script, cx, Main.global);
				}
			}

			return null;
		}

		public void quit(Context cx, int exitCode) {
			if (this.type == 3) {
				System.exit(exitCode);
			} else {
				throw Kit.codeBug();
			}
		}
	}
}