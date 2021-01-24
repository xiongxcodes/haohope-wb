/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.tools.shell;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Synchronizer;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.Wrapper;
import org.mozilla.javascript.serialize.ScriptableInputStream;
import org.mozilla.javascript.serialize.ScriptableOutputStream;
import org.mozilla.javascript.tools.ToolErrorReporter;
import org.mozilla.javascript.tools.shell.Environment;
import org.mozilla.javascript.tools.shell.Main;
import org.mozilla.javascript.tools.shell.PipeThread;
import org.mozilla.javascript.tools.shell.QuitAction;
import org.mozilla.javascript.tools.shell.Runner;
import org.mozilla.javascript.tools.shell.ShellLine;

public class Global extends ImporterTopLevel {
	static final long serialVersionUID = 4029130780977538005L;
	NativeArray history;
	boolean attemptedJLineLoad;
	private InputStream inStream;
	private PrintStream outStream;
	private PrintStream errStream;
	private boolean sealedStdLib = false;
	boolean initialized;
	private QuitAction quitAction;
	private String[] prompts = new String[]{"js> ", "  > "};
	private HashMap<String, String> doctestCanonicalizations;

	public Global() {
	}

	public Global(Context cx) {
		this.init(cx);
	}

	public boolean isInitialized() {
		return this.initialized;
	}

	public void initQuitAction(QuitAction quitAction) {
		if (quitAction == null) {
			throw new IllegalArgumentException("quitAction is null");
		} else if (this.quitAction != null) {
			throw new IllegalArgumentException("The method is once-call.");
		} else {
			this.quitAction = quitAction;
		}
	}

	public void init(ContextFactory factory) {
		factory.call(new ContextAction() {
			public Object run(Context cx) {
				Global.this.init(cx);
				return null;
			}
		});
	}

	public void init(Context cx) {
		this.initStandardObjects(cx, this.sealedStdLib);
		String[] names = new String[]{"defineClass", "deserialize", "doctest",
				"gc", "help", "load", "loadClass", "print", "quit", "readFile",
				"readUrl", "runCommand", "seal", "serialize", "spawn", "sync",
				"toint32", "version"};
		this.defineFunctionProperties(names, Global.class, 2);
		Environment.defineClass(this);
		Environment environment = new Environment(this);
		this.defineProperty("environment", environment, 2);
		this.history = (NativeArray) cx.newArray(this, 0);
		this.defineProperty("history", this.history, 2);
		this.initialized = true;
	}

	public static void help(Context cx, Scriptable thisObj, Object[] args,
			Function funObj) {
		PrintStream out = getInstance(funObj).getOut();
		out.println(ToolErrorReporter.getMessage("msg.help"));
	}

	public static void gc(Context cx, Scriptable thisObj, Object[] args,
			Function funObj) {
		System.gc();
	}

	public static Object print(Context cx, Scriptable thisObj, Object[] args,
			Function funObj) {
		PrintStream out = getInstance(funObj).getOut();

		for (int i = 0; i < args.length; ++i) {
			if (i > 0) {
				out.print(" ");
			}

			String s = Context.toString(args[i]);
			out.print(s);
		}

		out.println();
		return Context.getUndefinedValue();
	}

	public static void quit(Context cx, Scriptable thisObj, Object[] args,
			Function funObj) {
		Global global = getInstance(funObj);
		if (global.quitAction != null) {
			int exitCode = args.length == 0 ? 0 : ScriptRuntime
					.toInt32(args[0]);
			global.quitAction.quit(cx, exitCode);
		}

	}

	public static double version(Context cx, Scriptable thisObj, Object[] args,
			Function funObj) {
		double result = (double) cx.getLanguageVersion();
		if (args.length > 0) {
			double d = Context.toNumber(args[0]);
			cx.setLanguageVersion((int) d);
		}

		return result;
	}

	public static void load(Context cx, Scriptable thisObj, Object[] args,
			Function funObj) {
		for (int i = 0; i < args.length; ++i) {
			Main.processFile(cx, thisObj, Context.toString(args[i]));
		}

	}

	public static void defineClass(Context cx, Scriptable thisObj,
			Object[] args, Function funObj) throws IllegalAccessException,
			InstantiationException, InvocationTargetException {
		Class clazz = getClass(args);
		if (!Scriptable.class.isAssignableFrom(clazz)) {
			throw reportRuntimeError("msg.must.implement.Scriptable");
		} else {
			ScriptableObject.defineClass(thisObj, clazz);
		}
	}

	public static void loadClass(Context cx, Scriptable thisObj, Object[] args,
			Function funObj) throws IllegalAccessException,
			InstantiationException {
		Class clazz = getClass(args);
		if (!Script.class.isAssignableFrom(clazz)) {
			throw reportRuntimeError("msg.must.implement.Script");
		} else {
			Script script = (Script) clazz.newInstance();
			script.exec(cx, thisObj);
		}
	}

	private static Class<?> getClass(Object[] args) {
		if (args.length == 0) {
			throw reportRuntimeError("msg.expected.string.arg");
		} else {
			Object arg0 = args[0];
			if (arg0 instanceof Wrapper) {
				Object className = ((Wrapper) arg0).unwrap();
				if (className instanceof Class) {
					return (Class) className;
				}
			}

			String className1 = Context.toString(args[0]);

			try {
				return Class.forName(className1);
			} catch (ClassNotFoundException arg3) {
				throw reportRuntimeError("msg.class.not.found", className1);
			}
		}
	}

	public static void serialize(Context cx, Scriptable thisObj, Object[] args,
			Function funObj) throws IOException {
		if (args.length < 2) {
			throw Context
					.reportRuntimeError("Expected an object to serialize and a filename to write the serialization to");
		} else {
			Object obj = args[0];
			String filename = Context.toString(args[1]);
			FileOutputStream fos = new FileOutputStream(filename);
			Scriptable scope = ScriptableObject.getTopLevelScope(thisObj);
			ScriptableOutputStream out = new ScriptableOutputStream(fos, scope);
			out.writeObject(obj);
			out.close();
		}
	}

	public static Object deserialize(Context cx, Scriptable thisObj,
			Object[] args, Function funObj) throws IOException,
			ClassNotFoundException {
		if (args.length < 1) {
			throw Context
					.reportRuntimeError("Expected a filename to read the serialization from");
		} else {
			String filename = Context.toString(args[0]);
			FileInputStream fis = new FileInputStream(filename);
			Scriptable scope = ScriptableObject.getTopLevelScope(thisObj);
			ScriptableInputStream in = new ScriptableInputStream(fis, scope);
			Object deserialized = in.readObject();
			in.close();
			return Context.toObject(deserialized, scope);
		}
	}

	public String[] getPrompts(Context cx) {
		if (ScriptableObject.hasProperty(this, "prompts")) {
			Object promptsJS = ScriptableObject.getProperty(this, "prompts");
			if (promptsJS instanceof Scriptable) {
				Scriptable s = (Scriptable) promptsJS;
				if (ScriptableObject.hasProperty(s, 0)
						&& ScriptableObject.hasProperty(s, 1)) {
					Object elem0 = ScriptableObject.getProperty(s, 0);
					if (elem0 instanceof Function) {
						elem0 = ((Function) elem0).call(cx, this, s,
								new Object[0]);
					}

					this.prompts[0] = Context.toString(elem0);
					Object elem1 = ScriptableObject.getProperty(s, 1);
					if (elem1 instanceof Function) {
						elem1 = ((Function) elem1).call(cx, this, s,
								new Object[0]);
					}

					this.prompts[1] = Context.toString(elem1);
				}
			}
		}

		return this.prompts;
	}

	public static Object doctest(Context cx, Scriptable thisObj, Object[] args,
			Function funObj) {
		if (args.length == 0) {
			return Boolean.FALSE;
		} else {
			String session = Context.toString(args[0]);
			Global global = getInstance(funObj);
			return new Integer(global.runDoctest(cx, global, session,
					(String) null, 0));
		}
	}

	public int runDoctest(Context cx, Scriptable scope, String session,
			String sourceName, int lineNumber) {
		this.doctestCanonicalizations = new HashMap();
		String[] lines = session.split("\n|\r");
		String prompt0 = this.prompts[0].trim();
		String prompt1 = this.prompts[1].trim();
		int testCount = 0;

		int i;
		for (i = 0; i < lines.length && !lines[i].trim().startsWith(prompt0); ++i) {
			;
		}

		String inputString;
		String expectedString;
		String resultString;
		do {
			if (i >= lines.length) {
				return testCount;
			}

			inputString = lines[i].trim().substring(prompt0.length());
			inputString = inputString + "\n";
			++i;

			while (i < lines.length && lines[i].trim().startsWith(prompt1)) {
				inputString = inputString
						+ lines[i].trim().substring(prompt1.length());
				inputString = inputString + "\n";
				++i;
			}

			for (expectedString = ""; i < lines.length
					&& !lines[i].trim().startsWith(prompt0); ++i) {
				expectedString = expectedString + lines[i] + "\n";
			}

			PrintStream savedOut = this.getOut();
			PrintStream savedErr = this.getErr();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ByteArrayOutputStream err = new ByteArrayOutputStream();
			this.setOut(new PrintStream(out));
			this.setErr(new PrintStream(err));
			resultString = "";
			ErrorReporter savedErrorReporter = cx.getErrorReporter();
			cx.setErrorReporter(new ToolErrorReporter(false, this.getErr()));

			try {
				++testCount;
				Object message = cx.evaluateString(scope, inputString,
						"doctest input", 1, (Object) null);
				if (message != Context.getUndefinedValue()
						&& (!(message instanceof Function) || !inputString
								.trim().startsWith("function"))) {
					resultString = Context.toString(message);
				}
			} catch (RhinoException arg22) {
				ToolErrorReporter.reportException(cx.getErrorReporter(), arg22);
			} finally {
				this.setOut(savedOut);
				this.setErr(savedErr);
				cx.setErrorReporter(savedErrorReporter);
				resultString = resultString + err.toString() + out.toString();
			}
		} while (this.doctestOutputMatches(expectedString, resultString));

		String arg24 = "doctest failure running:\n" + inputString
				+ "expected: " + expectedString + "actual: " + resultString
				+ "\n";
		if (sourceName != null) {
			throw Context.reportRuntimeError(arg24, sourceName, lineNumber + i
					- 1, (String) null, 0);
		} else {
			throw Context.reportRuntimeError(arg24);
		}
	}

	private boolean doctestOutputMatches(String expected, String actual) {
		expected = expected.trim();
		actual = actual.trim().replace("\r\n", "\n");
		if (expected.equals(actual)) {
			return true;
		} else {
			Entry expectedMatcher;
			for (Iterator p = this.doctestCanonicalizations.entrySet()
					.iterator(); p.hasNext(); expected = expected.replace(
					(CharSequence) expectedMatcher.getKey(),
					(CharSequence) expectedMatcher.getValue())) {
				expectedMatcher = (Entry) p.next();
			}

			if (expected.equals(actual)) {
				return true;
			} else {
				Pattern p1 = Pattern.compile("@[0-9a-fA-F]+");
				Matcher expectedMatcher1 = p1.matcher(expected);
				Matcher actualMatcher = p1.matcher(actual);

				while (expectedMatcher1.find()) {
					if (!actualMatcher.find()) {
						return false;
					}

					if (actualMatcher.start() != expectedMatcher1.start()) {
						return false;
					}

					int start = expectedMatcher1.start();
					if (!expected.substring(0, start).equals(
							actual.substring(0, start))) {
						return false;
					}

					String expectedGroup = expectedMatcher1.group();
					String actualGroup = actualMatcher.group();
					String mapping = (String) this.doctestCanonicalizations
							.get(expectedGroup);
					if (mapping == null) {
						this.doctestCanonicalizations.put(expectedGroup,
								actualGroup);
						expected = expected.replace(expectedGroup, actualGroup);
					} else if (!actualGroup.equals(mapping)) {
						return false;
					}

					if (expected.equals(actual)) {
						return true;
					}
				}

				return false;
			}
		}
	}

	public static Object spawn(Context cx, Scriptable thisObj, Object[] args,
			Function funObj) {
		Scriptable scope = funObj.getParentScope();
		Runner runner;
		if (args.length != 0 && args[0] instanceof Function) {
			Object[] thread = null;
			if (args.length > 1 && args[1] instanceof Scriptable) {
				thread = cx.getElements((Scriptable) args[1]);
			}

			if (thread == null) {
				thread = ScriptRuntime.emptyArgs;
			}

			runner = new Runner(scope, (Function) args[0], thread);
		} else {
			if (args.length == 0 || !(args[0] instanceof Script)) {
				throw reportRuntimeError("msg.spawn.args");
			}

			runner = new Runner(scope, (Script) args[0]);
		}

		runner.factory = cx.getFactory();
		Thread thread1 = new Thread(runner);
		thread1.start();
		return thread1;
	}

	public static Object sync(Context cx, Scriptable thisObj, Object[] args,
			Function funObj) {
		if (args.length == 1 && args[0] instanceof Function) {
			return new Synchronizer((Function) args[0]);
		} else {
			throw reportRuntimeError("msg.sync.args");
		}
	}

	public static Object runCommand(Context cx, Scriptable thisObj,
			Object[] args, Function funObj) throws IOException {
		int L = args.length;
		if (L != 0 && (L != 1 || !(args[0] instanceof Scriptable))) {
			InputStream in = null;
			Object out = null;
			Object err = null;
			ByteArrayOutputStream outBytes = null;
			ByteArrayOutputStream errBytes = null;
			Object outObj = null;
			Object errObj = null;
			String[] environment = null;
			Scriptable params = null;
			Object[] addArgs = null;
			if (args[L - 1] instanceof Scriptable) {
				params = (Scriptable) args[L - 1];
				--L;
				Object global = ScriptableObject.getProperty(params, "env");
				if (global != Scriptable.NOT_FOUND) {
					if (global == null) {
						environment = new String[0];
					} else {
						if (!(global instanceof Scriptable)) {
							throw reportRuntimeError("msg.runCommand.bad.env");
						}

						Scriptable cmd = (Scriptable) global;
						Object[] exitCode = ScriptableObject
								.getPropertyIds(cmd);
						environment = new String[exitCode.length];

						for (int s = 0; s != exitCode.length; ++s) {
							Object keyObj = exitCode[s];
							Object val;
							String key;
							if (keyObj instanceof String) {
								key = (String) keyObj;
								val = ScriptableObject.getProperty(cmd, key);
							} else {
								int ikey = ((Number) keyObj).intValue();
								key = Integer.toString(ikey);
								val = ScriptableObject.getProperty(cmd, ikey);
							}

							if (val == ScriptableObject.NOT_FOUND) {
								val = Undefined.instance;
							}

							environment[s] = key + '='
									+ ScriptRuntime.toString(val);
						}
					}
				}

				Object arg23 = ScriptableObject.getProperty(params, "input");
				if (arg23 != Scriptable.NOT_FOUND) {
					in = toInputStream(arg23);
				}

				outObj = ScriptableObject.getProperty(params, "output");
				if (outObj != Scriptable.NOT_FOUND) {
					out = toOutputStream(outObj);
					if (out == null) {
						outBytes = new ByteArrayOutputStream();
						out = outBytes;
					}
				}

				errObj = ScriptableObject.getProperty(params, "err");
				if (errObj != Scriptable.NOT_FOUND) {
					err = toOutputStream(errObj);
					if (err == null) {
						errBytes = new ByteArrayOutputStream();
						err = errBytes;
					}
				}

				Object arg25 = ScriptableObject.getProperty(params, "args");
				if (arg25 != Scriptable.NOT_FOUND) {
					Scriptable arg27 = Context.toObject(arg25,
							getTopLevelScope(thisObj));
					addArgs = cx.getElements(arg27);
				}
			}

			Global arg22 = getInstance(funObj);
			if (out == null) {
				out = arg22 != null ? arg22.getOut() : System.out;
			}

			if (err == null) {
				err = arg22 != null ? arg22.getErr() : System.err;
			}

			String[] arg24 = new String[addArgs == null ? L : L
					+ addArgs.length];

			int arg26;
			for (arg26 = 0; arg26 != L; ++arg26) {
				arg24[arg26] = ScriptRuntime.toString(args[arg26]);
			}

			if (addArgs != null) {
				for (arg26 = 0; arg26 != addArgs.length; ++arg26) {
					arg24[L + arg26] = ScriptRuntime.toString(addArgs[arg26]);
				}
			}

			arg26 = runProcess(arg24, environment, in, (OutputStream) out,
					(OutputStream) err);
			String arg28;
			if (outBytes != null) {
				arg28 = ScriptRuntime.toString(outObj) + outBytes.toString();
				ScriptableObject.putProperty(params, "output", arg28);
			}

			if (errBytes != null) {
				arg28 = ScriptRuntime.toString(errObj) + errBytes.toString();
				ScriptableObject.putProperty(params, "err", arg28);
			}

			return new Integer(arg26);
		} else {
			throw reportRuntimeError("msg.runCommand.bad.args");
		}
	}

	public static void seal(Context cx, Scriptable thisObj, Object[] args,
			Function funObj) {
		int i;
		Object arg;
		for (i = 0; i != args.length; ++i) {
			arg = args[i];
			if (!(arg instanceof ScriptableObject) || arg == Undefined.instance) {
				if (arg instanceof Scriptable && arg != Undefined.instance) {
					throw reportRuntimeError("msg.shell.seal.not.scriptable");
				} else {
					throw reportRuntimeError("msg.shell.seal.not.object");
				}
			}
		}

		for (i = 0; i != args.length; ++i) {
			arg = args[i];
			((ScriptableObject) arg).sealObject();
		}

	}

	public static Object readFile(Context cx, Scriptable thisObj,
			Object[] args, Function funObj) throws IOException {
		if (args.length == 0) {
			throw reportRuntimeError("msg.shell.readFile.bad.args");
		} else {
			String path = ScriptRuntime.toString(args[0]);
			String charCoding = null;
			if (args.length >= 2) {
				charCoding = ScriptRuntime.toString(args[1]);
			}

			return readUrl(path, charCoding, true);
		}
	}

	public static Object readUrl(Context cx, Scriptable thisObj, Object[] args,
			Function funObj) throws IOException {
		if (args.length == 0) {
			throw reportRuntimeError("msg.shell.readUrl.bad.args");
		} else {
			String url = ScriptRuntime.toString(args[0]);
			String charCoding = null;
			if (args.length >= 2) {
				charCoding = ScriptRuntime.toString(args[1]);
			}

			return readUrl(url, charCoding, false);
		}
	}

	public static Object toint32(Context cx, Scriptable thisObj, Object[] args,
			Function funObj) {
		Object arg = args.length != 0 ? args[0] : Undefined.instance;
		return arg instanceof Integer ? arg : ScriptRuntime
				.wrapInt(ScriptRuntime.toInt32(arg));
	}

	public InputStream getIn() {
		if (this.inStream == null && !this.attemptedJLineLoad) {
			InputStream jlineStream = ShellLine.getStream(this);
			if (jlineStream != null) {
				this.inStream = jlineStream;
			}

			this.attemptedJLineLoad = true;
		}

		return this.inStream == null ? System.in : this.inStream;
	}

	public void setIn(InputStream in) {
		this.inStream = in;
	}

	public PrintStream getOut() {
		return this.outStream == null ? System.out : this.outStream;
	}

	public void setOut(PrintStream out) {
		this.outStream = out;
	}

	public PrintStream getErr() {
		return this.errStream == null ? System.err : this.errStream;
	}

	public void setErr(PrintStream err) {
		this.errStream = err;
	}

	public void setSealedStdLib(boolean value) {
		this.sealedStdLib = value;
	}

	private static Global getInstance(Function function) {
		Scriptable scope = function.getParentScope();
		if (!(scope instanceof Global)) {
			throw reportRuntimeError("msg.bad.shell.function.scope",
					String.valueOf(scope));
		} else {
			return (Global) scope;
		}
	}

	private static int runProcess(String[] cmd, String[] environment,
			InputStream in, OutputStream out, OutputStream err)
			throws IOException {
		Process p;
		if (environment == null) {
			p = Runtime.getRuntime().exec(cmd);
		} else {
			p = Runtime.getRuntime().exec(cmd, environment);
		}

		int ignore;
		try {
			PipeThread inThread = null;
			if (in != null) {
				inThread = new PipeThread(false, in, p.getOutputStream());
				inThread.start();
			} else {
				p.getOutputStream().close();
			}

			PipeThread outThread = null;
			if (out != null) {
				outThread = new PipeThread(true, p.getInputStream(), out);
				outThread.start();
			} else {
				p.getInputStream().close();
			}

			PipeThread errThread = null;
			if (err != null) {
				errThread = new PipeThread(true, p.getErrorStream(), err);
				errThread.start();
			} else {
				p.getErrorStream().close();
			}

			while (true) {
				try {
					p.waitFor();
					if (outThread != null) {
						outThread.join();
					}

					if (inThread != null) {
						inThread.join();
					}

					if (errThread != null) {
						errThread.join();
					}
					break;
				} catch (InterruptedException arg12) {
					;
				}
			}

			ignore = p.exitValue();
		} finally {
			p.destroy();
		}

		return ignore;
	}

	static void pipe(boolean fromProcess, InputStream from, OutputStream to)
			throws IOException {
		try {
			boolean ex = true;
			byte[] buffer = new byte[4096];

			while (true) {
				int n;
				if (!fromProcess) {
					n = from.read(buffer, 0, 4096);
				} else {
					try {
						n = from.read(buffer, 0, 4096);
					} catch (IOException arg15) {
						break;
					}
				}

				if (n < 0) {
					break;
				}

				if (fromProcess) {
					to.write(buffer, 0, n);
					to.flush();
				} else {
					try {
						to.write(buffer, 0, n);
						to.flush();
					} catch (IOException arg14) {
						break;
					}
				}
			}
		} finally {
			try {
				if (fromProcess) {
					from.close();
				} else {
					to.close();
				}
			} catch (IOException arg13) {
				;
			}

		}

	}

	private static InputStream toInputStream(Object value) throws IOException {
		Object is = null;
		String s = null;
		if (value instanceof Wrapper) {
			Object unwrapped = ((Wrapper) value).unwrap();
			if (unwrapped instanceof InputStream) {
				is = (InputStream) unwrapped;
			} else if (unwrapped instanceof byte[]) {
				is = new ByteArrayInputStream((byte[]) ((byte[]) unwrapped));
			} else if (unwrapped instanceof Reader) {
				s = readReader((Reader) unwrapped);
			} else if (unwrapped instanceof char[]) {
				s = new String((char[]) ((char[]) unwrapped));
			}
		}

		if (is == null) {
			if (s == null) {
				s = ScriptRuntime.toString(value);
			}

			is = new ByteArrayInputStream(s.getBytes());
		}

		return (InputStream) is;
	}

	private static OutputStream toOutputStream(Object value) {
		OutputStream os = null;
		if (value instanceof Wrapper) {
			Object unwrapped = ((Wrapper) value).unwrap();
			if (unwrapped instanceof OutputStream) {
				os = (OutputStream) unwrapped;
			}
		}

		return os;
	}

	private static String readUrl(String filePath, String charCoding,
			boolean urlIsFile) throws IOException {
		Object is = null;

		String length2;
		try {
			int chunkLength;
			if (!urlIsFile) {
				URL r = new URL(filePath);
				URLConnection length = r.openConnection();
				is = length.getInputStream();
				chunkLength = length.getContentLength();
				if (chunkLength <= 0) {
					chunkLength = 1024;
				}

				if (charCoding == null) {
					String type = length.getContentType();
					if (type != null) {
						charCoding = getCharCodingFromType(type);
					}
				}
			} else {
				File r1 = new File(filePath);
				long length1 = r1.length();
				chunkLength = (int) length1;
				if ((long) chunkLength != length1) {
					throw new IOException("Too big file size: " + length1);
				}

				if (chunkLength == 0) {
					String arg7 = "";
					return arg7;
				}

				is = new FileInputStream(r1);
			}

			InputStreamReader r2;
			if (charCoding == null) {
				r2 = new InputStreamReader((InputStream) is);
			} else {
				r2 = new InputStreamReader((InputStream) is, charCoding);
			}

			length2 = readReader(r2, chunkLength);
		} finally {
			if (is != null) {
				((InputStream) is).close();
			}

		}

		return length2;
	}

	private static String getCharCodingFromType(String type) {
		int i = type.indexOf(59);
		if (i >= 0) {
			int end = type.length();
			++i;

			while (i != end && type.charAt(i) <= 32) {
				++i;
			}

			String charset = "charset";
			if (charset.regionMatches(true, 0, type, i, charset.length())) {
				for (i += charset.length(); i != end && type.charAt(i) <= 32; ++i) {
					;
				}

				if (i != end && type.charAt(i) == 61) {
					++i;

					while (i != end && type.charAt(i) <= 32) {
						++i;
					}

					if (i != end) {
						while (type.charAt(end - 1) <= 32) {
							--end;
						}

						return type.substring(i, end);
					}
				}
			}
		}

		return null;
	}

	private static String readReader(Reader reader) throws IOException {
		return readReader(reader, 4096);
	}

	private static String readReader(Reader reader, int initialBufferSize)
			throws IOException {
		char[] buffer = new char[initialBufferSize];
		int offset = 0;

		while (true) {
			int n = reader.read(buffer, offset, buffer.length - offset);
			if (n < 0) {
				return new String(buffer, 0, offset);
			}

			offset += n;
			if (offset == buffer.length) {
				char[] tmp = new char[buffer.length * 2];
				System.arraycopy(buffer, 0, tmp, 0, offset);
				buffer = tmp;
			}
		}
	}

	static RuntimeException reportRuntimeError(String msgId) {
		String message = ToolErrorReporter.getMessage(msgId);
		return Context.reportRuntimeError(message);
	}

	static RuntimeException reportRuntimeError(String msgId, String msgArg) {
		String message = ToolErrorReporter.getMessage(msgId, msgArg);
		return Context.reportRuntimeError(message);
	}
}