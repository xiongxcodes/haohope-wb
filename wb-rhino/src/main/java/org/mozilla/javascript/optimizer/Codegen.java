/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.optimizer;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import org.mozilla.classfile.ClassFileWriter;
import org.mozilla.classfile.ClassFileWriter.ClassFileFormatException;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Evaluator;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.FunctionNode;
import org.mozilla.javascript.GeneratedClassLoader;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.NativeFunction;
import org.mozilla.javascript.ObjArray;
import org.mozilla.javascript.ObjToIntMap;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptOrFnNode;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.SecurityController;
import org.mozilla.javascript.optimizer.BodyCodegen;
import org.mozilla.javascript.optimizer.OptFunctionNode;
import org.mozilla.javascript.optimizer.OptTransformer;
import org.mozilla.javascript.optimizer.Optimizer;

public class Codegen implements Evaluator {
	static final String DEFAULT_MAIN_METHOD_CLASS = "org.mozilla.javascript.optimizer.OptRuntime";
	private static final String SUPER_CLASS_NAME = "org.mozilla.javascript.NativeFunction";
	static final String DIRECT_CALL_PARENT_FIELD = "_dcp";
	private static final String ID_FIELD_NAME = "_id";
	private static final String REGEXP_INIT_METHOD_NAME = "_reInit";
	private static final String REGEXP_INIT_METHOD_SIGNATURE = "(Lorg/mozilla/javascript/RegExpProxy;Lorg/mozilla/javascript/Context;)V";
	static final String REGEXP_ARRAY_FIELD_NAME = "_re";
	static final String REGEXP_ARRAY_FIELD_TYPE = "[Ljava/lang/Object;";
	static final String FUNCTION_INIT_SIGNATURE = "(Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;)V";
	static final String FUNCTION_CONSTRUCTOR_SIGNATURE = "(Lorg/mozilla/javascript/Scriptable;Lorg/mozilla/javascript/Context;I)V";
	private static final Object globalLock = new Object();
	private static int globalSerialClassCounter;
	private CompilerEnvirons compilerEnv;
	private ObjArray directCallTargets;
	ScriptOrFnNode[] scriptOrFnNodes;
	private ObjToIntMap scriptOrFnIndexes;
	private String mainMethodClass = "org.mozilla.javascript.optimizer.OptRuntime";
	String mainClassName;
	String mainClassSignature;
	private double[] itsConstantList;
	private int itsConstantListSize;

	public void captureStackInfo(RhinoException ex) {
		throw new UnsupportedOperationException();
	}

	public String getSourcePositionFromStack(Context cx, int[] linep) {
		throw new UnsupportedOperationException();
	}

	public String getPatchedStack(RhinoException ex, String nativeStackTrace) {
		throw new UnsupportedOperationException();
	}

	public List<String> getScriptStack(RhinoException ex) {
		throw new UnsupportedOperationException();
	}

	public void setEvalScriptFlag(Script script) {
		throw new UnsupportedOperationException();
	}

	public Object compile(CompilerEnvirons compilerEnv, ScriptOrFnNode tree,
			String encodedSource, boolean returnFunction) {
		Object mainClassName = globalLock;
		int serial;
		synchronized (globalLock) {
			serial = ++globalSerialClassCounter;
		}

		String arg8 = "org.mozilla.javascript.gen.c" + serial;
		byte[] mainClassBytes = this.compileToClassFile(compilerEnv, arg8,
				tree, encodedSource, returnFunction);
		return new Object[]{arg8, mainClassBytes};
	}

	public Script createScriptObject(Object bytecode,
			Object staticSecurityDomain) {
		Class cl = this.defineClass(bytecode, staticSecurityDomain);

		try {
			Script script = (Script) cl.newInstance();
			return script;
		} catch (Exception arg5) {
			throw new RuntimeException("Unable to instantiate compiled class:"
					+ arg5.toString());
		}
	}

	public Function createFunctionObject(Context cx, Scriptable scope,
			Object bytecode, Object staticSecurityDomain) {
		Class cl = this.defineClass(bytecode, staticSecurityDomain);

		try {
			Constructor ex = cl.getConstructors()[0];
			Object[] initArgs = new Object[]{scope, cx, new Integer(0)};
			NativeFunction f = (NativeFunction) ex.newInstance(initArgs);
			return f;
		} catch (Exception arg8) {
			throw new RuntimeException("Unable to instantiate compiled class:"
					+ arg8.toString());
		}
	}

	private Class<?> defineClass(Object bytecode, Object staticSecurityDomain) {
		Object[] nameBytesPair = (Object[]) ((Object[]) bytecode);
		String className = (String) nameBytesPair[0];
		byte[] classBytes = (byte[]) ((byte[]) nameBytesPair[1]);
		ClassLoader rhinoLoader = this.getClass().getClassLoader();
		GeneratedClassLoader loader = SecurityController.createLoader(
				rhinoLoader, staticSecurityDomain);

		Object e;
		try {
			Class x = loader.defineClass(className, classBytes);
			loader.linkClass(x);
			return x;
		} catch (SecurityException arg9) {
			e = arg9;
		} catch (IllegalArgumentException arg10) {
			e = arg10;
		}

		throw new RuntimeException("Malformed optimizer package " + e);
	}

	byte[] compileToClassFile(CompilerEnvirons compilerEnv,
			String mainClassName, ScriptOrFnNode scriptOrFn,
			String encodedSource, boolean returnFunction) {
		this.compilerEnv = compilerEnv;
		this.transform((ScriptOrFnNode) scriptOrFn);
		if (returnFunction) {
			scriptOrFn = ((ScriptOrFnNode) scriptOrFn).getFunctionNode(0);
		}

		this.initScriptOrFnNodesData((ScriptOrFnNode) scriptOrFn);
		this.mainClassName = mainClassName;
		this.mainClassSignature = ClassFileWriter
				.classNameToSignature(mainClassName);

		try {
			return this.generateCode(encodedSource);
		} catch (ClassFileFormatException arg6) {
			throw this.reportClassFileFormatException(
					(ScriptOrFnNode) scriptOrFn, arg6.getMessage());
		}
	}

	private RuntimeException reportClassFileFormatException(
			ScriptOrFnNode scriptOrFn, String message) {
		String msg = scriptOrFn instanceof FunctionNode
				? ScriptRuntime.getMessage2("msg.while.compiling.fn",
						((FunctionNode) scriptOrFn).getFunctionName(), message)
				: ScriptRuntime.getMessage1("msg.while.compiling.script",
						message);
		return Context.reportRuntimeError(msg, scriptOrFn.getSourceName(),
				scriptOrFn.getLineno(), (String) null, 0);
	}

	private void transform(ScriptOrFnNode tree) {
		initOptFunctions_r(tree);
		int optLevel = this.compilerEnv.getOptimizationLevel();
		HashMap possibleDirectCalls = null;
		if (optLevel > 0 && tree.getType() == 135) {
			int ot = tree.getFunctionCount();

			for (int i = 0; i != ot; ++i) {
				OptFunctionNode ofn = OptFunctionNode.get(tree, i);
				if (ofn.fnode.getFunctionType() == 1) {
					String name = ofn.fnode.getFunctionName();
					if (name.length() != 0) {
						if (possibleDirectCalls == null) {
							possibleDirectCalls = new HashMap();
						}

						possibleDirectCalls.put(name, ofn);
					}
				}
			}
		}

		if (possibleDirectCalls != null) {
			this.directCallTargets = new ObjArray();
		}

		OptTransformer arg7 = new OptTransformer(possibleDirectCalls,
				this.directCallTargets);
		arg7.transform(tree);
		if (optLevel > 0) {
			(new Optimizer()).optimize(tree);
		}

	}

	private static void initOptFunctions_r(ScriptOrFnNode scriptOrFn) {
		int i = 0;

		for (int N = scriptOrFn.getFunctionCount(); i != N; ++i) {
			FunctionNode fn = scriptOrFn.getFunctionNode(i);
			new OptFunctionNode(fn);
			initOptFunctions_r(fn);
		}

	}

	private void initScriptOrFnNodesData(ScriptOrFnNode scriptOrFn) {
		ObjArray x = new ObjArray();
		collectScriptOrFnNodes_r(scriptOrFn, x);
		int count = x.size();
		this.scriptOrFnNodes = new ScriptOrFnNode[count];
		x.toArray(this.scriptOrFnNodes);
		this.scriptOrFnIndexes = new ObjToIntMap(count);

		for (int i = 0; i != count; ++i) {
			this.scriptOrFnIndexes.put(this.scriptOrFnNodes[i], i);
		}

	}

	private static void collectScriptOrFnNodes_r(ScriptOrFnNode n, ObjArray x) {
		x.add(n);
		int nestedCount = n.getFunctionCount();

		for (int i = 0; i != nestedCount; ++i) {
			collectScriptOrFnNodes_r(n.getFunctionNode(i), x);
		}

	}

	private byte[] generateCode(String encodedSource) {
		boolean hasScript = this.scriptOrFnNodes[0].getType() == 135;
		boolean hasFunctions = this.scriptOrFnNodes.length > 1 || !hasScript;
		String sourceFile = null;
		if (this.compilerEnv.isGenerateDebugInfo()) {
			sourceFile = this.scriptOrFnNodes[0].getSourceName();
		}

		ClassFileWriter cfw = new ClassFileWriter(this.mainClassName,
				"org.mozilla.javascript.NativeFunction", sourceFile);
		cfw.addField("_id", "I", 2);
		cfw.addField("_dcp", this.mainClassSignature, 2);
		cfw.addField("_re", "[Ljava/lang/Object;", 2);
		if (hasFunctions) {
			this.generateFunctionConstructor(cfw);
		}

		if (hasScript) {
			cfw.addInterface("org/mozilla/javascript/Script");
			this.generateScriptCtor(cfw);
			this.generateMain(cfw);
			this.generateExecute(cfw);
		}

		this.generateCallMethod(cfw);
		this.generateResumeGenerator(cfw);
		this.generateNativeFunctionOverrides(cfw, encodedSource);
		int count = this.scriptOrFnNodes.length;

		int N;
		for (N = 0; N != count; ++N) {
			ScriptOrFnNode j = this.scriptOrFnNodes[N];
			BodyCodegen bodygen = new BodyCodegen();
			bodygen.cfw = cfw;
			bodygen.codegen = this;
			bodygen.compilerEnv = this.compilerEnv;
			bodygen.scriptOrFn = j;
			bodygen.scriptOrFnIndex = N;

			try {
				bodygen.generateBodyCode();
			} catch (ClassFileFormatException arg10) {
				throw this
						.reportClassFileFormatException(j, arg10.getMessage());
			}

			if (j.getType() == 108) {
				OptFunctionNode ofn = OptFunctionNode.get(j);
				this.generateFunctionInit(cfw, ofn);
				if (ofn.isTargetOfDirectCall()) {
					this.emitDirectConstructor(cfw, ofn);
				}
			}
		}

		if (this.directCallTargets != null) {
			N = this.directCallTargets.size();

			for (int arg11 = 0; arg11 != N; ++arg11) {
				cfw.addField(getDirectTargetFieldName(arg11),
						this.mainClassSignature, 2);
			}
		}

		this.emitRegExpInit(cfw);
		this.emitConstantDudeInitializers(cfw);
		return cfw.toByteArray();
	}

	private void emitDirectConstructor(ClassFileWriter cfw, OptFunctionNode ofn) {
		cfw.startMethod(this.getDirectCtorName(ofn.fnode),
				this.getBodyMethodSignature(ofn.fnode), 10);
		int argCount = ofn.fnode.getParamCount();
		int firstLocal = 4 + argCount * 3 + 1;
		cfw.addALoad(0);
		cfw.addALoad(1);
		cfw.addALoad(2);
		cfw.addInvoke(
				182,
				"org/mozilla/javascript/BaseFunction",
				"createObject",
				"(Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;)Lorg/mozilla/javascript/Scriptable;");
		cfw.addAStore(firstLocal);
		cfw.addALoad(0);
		cfw.addALoad(1);
		cfw.addALoad(2);
		cfw.addALoad(firstLocal);

		int exitLabel;
		for (exitLabel = 0; exitLabel < argCount; ++exitLabel) {
			cfw.addALoad(4 + exitLabel * 3);
			cfw.addDLoad(5 + exitLabel * 3);
		}

		cfw.addALoad(4 + argCount * 3);
		cfw.addInvoke(184, this.mainClassName,
				this.getBodyMethodName(ofn.fnode),
				this.getBodyMethodSignature(ofn.fnode));
		exitLabel = cfw.acquireLabel();
		cfw.add(89);
		cfw.add(193, "org/mozilla/javascript/Scriptable");
		cfw.add(153, exitLabel);
		cfw.add(192, "org/mozilla/javascript/Scriptable");
		cfw.add(176);
		cfw.markLabel(exitLabel);
		cfw.addALoad(firstLocal);
		cfw.add(176);
		cfw.stopMethod((short) (firstLocal + 1));
	}

	static boolean isGenerator(ScriptOrFnNode node) {
		return node.getType() == 108 && ((FunctionNode) node).isGenerator();
	}

	private void generateResumeGenerator(ClassFileWriter cfw) {
		boolean hasGenerators = false;

		int startSwitch;
		for (startSwitch = 0; startSwitch < this.scriptOrFnNodes.length; ++startSwitch) {
			if (isGenerator(this.scriptOrFnNodes[startSwitch])) {
				hasGenerators = true;
			}
		}

		if (hasGenerators) {
			cfw.startMethod(
					"resumeGenerator",
					"(Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;ILjava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
					17);
			cfw.addALoad(0);
			cfw.addALoad(1);
			cfw.addALoad(2);
			cfw.addALoad(4);
			cfw.addALoad(5);
			cfw.addILoad(3);
			cfw.addLoadThis();
			cfw.add(180, cfw.getClassName(), "_id", "I");
			startSwitch = cfw
					.addTableSwitch(0, this.scriptOrFnNodes.length - 1);
			cfw.markTableSwitchDefault(startSwitch);
			int endlabel = cfw.acquireLabel();

			for (int i = 0; i < this.scriptOrFnNodes.length; ++i) {
				ScriptOrFnNode n = this.scriptOrFnNodes[i];
				cfw.markTableSwitchCase(startSwitch, i, 6);
				if (isGenerator(n)) {
					String type = "(" + this.mainClassSignature
							+ "Lorg/mozilla/javascript/Context;"
							+ "Lorg/mozilla/javascript/Scriptable;"
							+ "Ljava/lang/Object;"
							+ "Ljava/lang/Object;I)Ljava/lang/Object;";
					cfw.addInvoke(184, this.mainClassName,
							this.getBodyMethodName(n) + "_gen", type);
					cfw.add(176);
				} else {
					cfw.add(167, endlabel);
				}
			}

			cfw.markLabel(endlabel);
			pushUndefined(cfw);
			cfw.add(176);
			cfw.stopMethod(6);
		}
	}

	private void generateCallMethod(ClassFileWriter cfw) {
		cfw.startMethod(
				"call",
				"(Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;Lorg/mozilla/javascript/Scriptable;[Ljava/lang/Object;)Ljava/lang/Object;",
				17);
		int nonTopCallLabel = cfw.acquireLabel();
		cfw.addALoad(1);
		cfw.addInvoke(184, "org/mozilla/javascript/ScriptRuntime",
				"hasTopCall", "(Lorg/mozilla/javascript/Context;)Z");
		cfw.add(154, nonTopCallLabel);
		cfw.addALoad(0);
		cfw.addALoad(1);
		cfw.addALoad(2);
		cfw.addALoad(3);
		cfw.addALoad(4);
		cfw.addInvoke(
				184,
				"org/mozilla/javascript/ScriptRuntime",
				"doTopCall",
				"(Lorg/mozilla/javascript/Callable;Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;Lorg/mozilla/javascript/Scriptable;[Ljava/lang/Object;)Ljava/lang/Object;");
		cfw.add(176);
		cfw.markLabel(nonTopCallLabel);
		cfw.addALoad(0);
		cfw.addALoad(1);
		cfw.addALoad(2);
		cfw.addALoad(3);
		cfw.addALoad(4);
		int end = this.scriptOrFnNodes.length;
		boolean generateSwitch = 2 <= end;
		int switchStart = 0;
		short switchStackTop = 0;
		if (generateSwitch) {
			cfw.addLoadThis();
			cfw.add(180, cfw.getClassName(), "_id", "I");
			switchStart = cfw.addTableSwitch(1, end - 1);
		}

		for (int i = 0; i != end; ++i) {
			ScriptOrFnNode n = this.scriptOrFnNodes[i];
			if (generateSwitch) {
				if (i == 0) {
					cfw.markTableSwitchDefault(switchStart);
					switchStackTop = cfw.getStackTop();
				} else {
					cfw.markTableSwitchCase(switchStart, i - 1, switchStackTop);
				}
			}

			if (n.getType() == 108) {
				OptFunctionNode ofn = OptFunctionNode.get(n);
				if (ofn.isTargetOfDirectCall()) {
					int pcount = ofn.fnode.getParamCount();
					if (pcount != 0) {
						for (int p = 0; p != pcount; ++p) {
							cfw.add(190);
							cfw.addPush(p);
							int undefArg = cfw.acquireLabel();
							int beyond = cfw.acquireLabel();
							cfw.add(164, undefArg);
							cfw.addALoad(4);
							cfw.addPush(p);
							cfw.add(50);
							cfw.add(167, beyond);
							cfw.markLabel(undefArg);
							pushUndefined(cfw);
							cfw.markLabel(beyond);
							cfw.adjustStackTop(-1);
							cfw.addPush(0.0D);
							cfw.addALoad(4);
						}
					}
				}
			}

			cfw.addInvoke(184, this.mainClassName, this.getBodyMethodName(n),
					this.getBodyMethodSignature(n));
			cfw.add(176);
		}

		cfw.stopMethod(5);
	}

	private void generateMain(ClassFileWriter cfw) {
		cfw.startMethod("main", "([Ljava/lang/String;)V", 9);
		cfw.add(187, cfw.getClassName());
		cfw.add(89);
		cfw.addInvoke(183, cfw.getClassName(), "<init>", "()V");
		cfw.add(42);
		cfw.addInvoke(184, this.mainMethodClass, "main",
				"(Lorg/mozilla/javascript/Script;[Ljava/lang/String;)V");
		cfw.add(177);
		cfw.stopMethod(1);
	}

	private void generateExecute(ClassFileWriter cfw) {
		cfw.startMethod(
				"exec",
				"(Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;)Ljava/lang/Object;",
				17);
		boolean CONTEXT_ARG = true;
		boolean SCOPE_ARG = true;
		cfw.addLoadThis();
		cfw.addALoad(1);
		cfw.addALoad(2);
		cfw.add(89);
		cfw.add(1);
		cfw.addInvoke(
				182,
				cfw.getClassName(),
				"call",
				"(Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;Lorg/mozilla/javascript/Scriptable;[Ljava/lang/Object;)Ljava/lang/Object;");
		cfw.add(176);
		cfw.stopMethod(3);
	}

	private void generateScriptCtor(ClassFileWriter cfw) {
		cfw.startMethod("<init>", "()V", 1);
		cfw.addLoadThis();
		cfw.addInvoke(183, "org.mozilla.javascript.NativeFunction", "<init>",
				"()V");
		cfw.addLoadThis();
		cfw.addPush(0);
		cfw.add(181, cfw.getClassName(), "_id", "I");
		cfw.add(177);
		cfw.stopMethod(1);
	}

	private void generateFunctionConstructor(ClassFileWriter cfw) {
		boolean SCOPE_ARG = true;
		boolean CONTEXT_ARG = true;
		boolean ID_ARG = true;
		cfw.startMethod(
				"<init>",
				"(Lorg/mozilla/javascript/Scriptable;Lorg/mozilla/javascript/Context;I)V",
				1);
		cfw.addALoad(0);
		cfw.addInvoke(183, "org.mozilla.javascript.NativeFunction", "<init>",
				"()V");
		cfw.addLoadThis();
		cfw.addILoad(3);
		cfw.add(181, cfw.getClassName(), "_id", "I");
		cfw.addLoadThis();
		cfw.addALoad(2);
		cfw.addALoad(1);
		int start = this.scriptOrFnNodes[0].getType() == 135 ? 1 : 0;
		int end = this.scriptOrFnNodes.length;
		if (start == end) {
			throw badTree();
		} else {
			boolean generateSwitch = 2 <= end - start;
			int switchStart = 0;
			short switchStackTop = 0;
			if (generateSwitch) {
				cfw.addILoad(3);
				switchStart = cfw.addTableSwitch(start + 1, end - 1);
			}

			for (int i = start; i != end; ++i) {
				if (generateSwitch) {
					if (i == start) {
						cfw.markTableSwitchDefault(switchStart);
						switchStackTop = cfw.getStackTop();
					} else {
						cfw.markTableSwitchCase(switchStart, i - 1 - start,
								switchStackTop);
					}
				}

				OptFunctionNode ofn = OptFunctionNode
						.get(this.scriptOrFnNodes[i]);
				cfw.addInvoke(182, this.mainClassName,
						this.getFunctionInitMethodName(ofn),
						"(Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;)V");
				cfw.add(177);
			}

			cfw.stopMethod(4);
		}
	}

	private void generateFunctionInit(ClassFileWriter cfw, OptFunctionNode ofn) {
		boolean CONTEXT_ARG = true;
		boolean SCOPE_ARG = true;
		cfw.startMethod(
				this.getFunctionInitMethodName(ofn),
				"(Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;)V",
				18);
		cfw.addLoadThis();
		cfw.addALoad(1);
		cfw.addALoad(2);
		cfw.addInvoke(182, "org/mozilla/javascript/NativeFunction",
				"initScriptFunction",
				"(Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;)V");
		int regexpCount = ofn.fnode.getRegexpCount();
		if (regexpCount != 0) {
			cfw.addLoadThis();
			this.pushRegExpArray(cfw, ofn.fnode, 1, 2);
			cfw.add(181, this.mainClassName, "_re", "[Ljava/lang/Object;");
		}

		cfw.add(177);
		cfw.stopMethod(3);
	}

	private void generateNativeFunctionOverrides(ClassFileWriter cfw,
			String encodedSource) {
		cfw.startMethod("getLanguageVersion", "()I", 1);
		cfw.addPush(this.compilerEnv.getLanguageVersion());
		cfw.add(172);
		cfw.stopMethod(1);
		boolean Do_getFunctionName = false;
		boolean Do_getParamCount = true;
		boolean Do_getParamAndVarCount = true;
		boolean Do_getParamOrVarName = true;
		boolean Do_getEncodedSource = true;
		boolean Do_getParamOrVarConst = true;
		boolean SWITCH_COUNT = true;

		for (int methodIndex = 0; methodIndex != 6; ++methodIndex) {
			if (methodIndex != 4 || encodedSource != null) {
				byte methodLocals;
				switch (methodIndex) {
					case 0 :
						methodLocals = 1;
						cfw.startMethod("getFunctionName",
								"()Ljava/lang/String;", 1);
						break;
					case 1 :
						methodLocals = 1;
						cfw.startMethod("getParamCount", "()I", 1);
						break;
					case 2 :
						methodLocals = 1;
						cfw.startMethod("getParamAndVarCount", "()I", 1);
						break;
					case 3 :
						methodLocals = 2;
						cfw.startMethod("getParamOrVarName",
								"(I)Ljava/lang/String;", 1);
						break;
					case 4 :
						methodLocals = 1;
						cfw.startMethod("getEncodedSource",
								"()Ljava/lang/String;", 1);
						cfw.addPush(encodedSource);
						break;
					case 5 :
						methodLocals = 3;
						cfw.startMethod("getParamOrVarConst", "(I)Z", 1);
						break;
					default :
						throw Kit.codeBug();
				}

				int count = this.scriptOrFnNodes.length;
				int switchStart = 0;
				short switchStackTop = 0;
				if (count > 1) {
					cfw.addLoadThis();
					cfw.add(180, cfw.getClassName(), "_id", "I");
					switchStart = cfw.addTableSwitch(1, count - 1);
				}

				for (int i = 0; i != count; ++i) {
					ScriptOrFnNode n = this.scriptOrFnNodes[i];
					if (i == 0) {
						if (count > 1) {
							cfw.markTableSwitchDefault(switchStart);
							switchStackTop = cfw.getStackTop();
						}
					} else {
						cfw.markTableSwitchCase(switchStart, i - 1,
								switchStackTop);
					}

					int paramAndVarCount;
					int paramSwitchStart;
					switch (methodIndex) {
						case 0 :
							if (n.getType() == 135) {
								cfw.addPush("");
							} else {
								String arg20 = ((FunctionNode) n)
										.getFunctionName();
								cfw.addPush(arg20);
							}

							cfw.add(176);
							break;
						case 1 :
							cfw.addPush(n.getParamCount());
							cfw.add(172);
							break;
						case 2 :
							cfw.addPush(n.getParamAndVarCount());
							cfw.add(172);
							break;
						case 3 :
							paramAndVarCount = n.getParamAndVarCount();
							if (paramAndVarCount == 0) {
								cfw.add(1);
								cfw.add(176);
							} else if (paramAndVarCount == 1) {
								cfw.addPush(n.getParamOrVarName(0));
								cfw.add(176);
							} else {
								cfw.addILoad(1);
								int arg21 = cfw.addTableSwitch(1,
										paramAndVarCount - 1);
								paramSwitchStart = 0;

								while (true) {
									if (paramSwitchStart == paramAndVarCount) {
										break;
									}

									if (cfw.getStackTop() != 0) {
										Kit.codeBug();
									}

									String arg22 = n
											.getParamOrVarName(paramSwitchStart);
									if (paramSwitchStart == 0) {
										cfw.markTableSwitchDefault(arg21);
									} else {
										cfw.markTableSwitchCase(arg21,
												paramSwitchStart - 1, 0);
									}

									cfw.addPush(arg22);
									cfw.add(176);
									++paramSwitchStart;
								}
							}
							break;
						case 4 :
							cfw.addPush(n.getEncodedSourceStart());
							cfw.addPush(n.getEncodedSourceEnd());
							cfw.addInvoke(182, "java/lang/String", "substring",
									"(II)Ljava/lang/String;");
							cfw.add(176);
							break;
						case 5 :
							paramAndVarCount = n.getParamAndVarCount();
							boolean[] constness = n.getParamAndVarConst();
							if (paramAndVarCount == 0) {
								cfw.add(3);
								cfw.add(172);
							} else if (paramAndVarCount == 1) {
								cfw.addPush(constness[0]);
								cfw.add(172);
							} else {
								cfw.addILoad(1);
								paramSwitchStart = cfw.addTableSwitch(1,
										paramAndVarCount - 1);
								int j = 0;

								while (true) {
									if (j == paramAndVarCount) {
										break;
									}

									if (cfw.getStackTop() != 0) {
										Kit.codeBug();
									}

									if (j == 0) {
										cfw.markTableSwitchDefault(paramSwitchStart);
									} else {
										cfw.markTableSwitchCase(
												paramSwitchStart, j - 1, 0);
									}

									cfw.addPush(constness[j]);
									cfw.add(172);
									++j;
								}
							}
							break;
						default :
							throw Kit.codeBug();
					}
				}

				cfw.stopMethod(methodLocals);
			}
		}

	}

	private void emitRegExpInit(ClassFileWriter cfw) {
		int totalRegCount = 0;

		int doInit;
		for (doInit = 0; doInit != this.scriptOrFnNodes.length; ++doInit) {
			totalRegCount += this.scriptOrFnNodes[doInit].getRegexpCount();
		}

		if (totalRegCount != 0) {
			cfw.startMethod(
					"_reInit",
					"(Lorg/mozilla/javascript/RegExpProxy;Lorg/mozilla/javascript/Context;)V",
					42);
			cfw.addField("_reInitDone", "Z", 10);
			cfw.add(178, this.mainClassName, "_reInitDone", "Z");
			doInit = cfw.acquireLabel();
			cfw.add(153, doInit);
			cfw.add(177);
			cfw.markLabel(doInit);

			for (int i = 0; i != this.scriptOrFnNodes.length; ++i) {
				ScriptOrFnNode n = this.scriptOrFnNodes[i];
				int regCount = n.getRegexpCount();

				for (int j = 0; j != regCount; ++j) {
					String reFieldName = this.getCompiledRegexpName(n, j);
					String reFieldType = "Ljava/lang/Object;";
					String reString = n.getRegexpString(j);
					String reFlags = n.getRegexpFlags(j);
					cfw.addField(reFieldName, reFieldType, 10);
					cfw.addALoad(0);
					cfw.addALoad(1);
					cfw.addPush(reString);
					if (reFlags == null) {
						cfw.add(1);
					} else {
						cfw.addPush(reFlags);
					}

					cfw.addInvoke(
							185,
							"org/mozilla/javascript/RegExpProxy",
							"compileRegExp",
							"(Lorg/mozilla/javascript/Context;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;");
					cfw.add(179, this.mainClassName, reFieldName, reFieldType);
				}
			}

			cfw.addPush(1);
			cfw.add(179, this.mainClassName, "_reInitDone", "Z");
			cfw.add(177);
			cfw.stopMethod(2);
		}
	}

	private void emitConstantDudeInitializers(ClassFileWriter cfw) {
		int N = this.itsConstantListSize;
		if (N != 0) {
			cfw.startMethod("<clinit>", "()V", 24);
			double[] array = this.itsConstantList;

			for (int i = 0; i != N; ++i) {
				double num = array[i];
				String constantName = "_k" + i;
				String constantType = getStaticConstantWrapperType(num);
				cfw.addField(constantName, constantType, 10);
				int inum = (int) num;
				if ((double) inum == num) {
					cfw.add(187, "java/lang/Integer");
					cfw.add(89);
					cfw.addPush(inum);
					cfw.addInvoke(183, "java/lang/Integer", "<init>", "(I)V");
				} else {
					cfw.addPush(num);
					addDoubleWrap(cfw);
				}

				cfw.add(179, this.mainClassName, constantName, constantType);
			}

			cfw.add(177);
			cfw.stopMethod(0);
		}
	}

	void pushRegExpArray(ClassFileWriter cfw, ScriptOrFnNode n, int contextArg,
			int scopeArg) {
		int regexpCount = n.getRegexpCount();
		if (regexpCount == 0) {
			throw badTree();
		} else {
			cfw.addPush(regexpCount);
			cfw.add(189, "java/lang/Object");
			cfw.addALoad(contextArg);
			cfw.addInvoke(184, "org/mozilla/javascript/ScriptRuntime",
					"checkRegExpProxy",
					"(Lorg/mozilla/javascript/Context;)Lorg/mozilla/javascript/RegExpProxy;");
			cfw.add(89);
			cfw.addALoad(contextArg);
			cfw.addInvoke(184, this.mainClassName, "_reInit",
					"(Lorg/mozilla/javascript/RegExpProxy;Lorg/mozilla/javascript/Context;)V");

			for (int i = 0; i != regexpCount; ++i) {
				cfw.add(92);
				cfw.addALoad(contextArg);
				cfw.addALoad(scopeArg);
				cfw.add(178, this.mainClassName,
						this.getCompiledRegexpName(n, i), "Ljava/lang/Object;");
				cfw.addInvoke(
						185,
						"org/mozilla/javascript/RegExpProxy",
						"wrapRegExp",
						"(Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;Ljava/lang/Object;)Lorg/mozilla/javascript/Scriptable;");
				cfw.addPush(i);
				cfw.add(95);
				cfw.add(83);
			}

			cfw.add(87);
		}
	}

	void pushNumberAsObject(ClassFileWriter cfw, double num) {
		if (num == 0.0D) {
			if (1.0D / num > 0.0D) {
				cfw.add(178, "org/mozilla/javascript/optimizer/OptRuntime",
						"zeroObj", "Ljava/lang/Double;");
			} else {
				cfw.addPush(num);
				addDoubleWrap(cfw);
			}
		} else {
			if (num == 1.0D) {
				cfw.add(178, "org/mozilla/javascript/optimizer/OptRuntime",
						"oneObj", "Ljava/lang/Double;");
				return;
			}

			if (num == -1.0D) {
				cfw.add(178, "org/mozilla/javascript/optimizer/OptRuntime",
						"minusOneObj", "Ljava/lang/Double;");
			} else if (num != num) {
				cfw.add(178, "org/mozilla/javascript/ScriptRuntime", "NaNobj",
						"Ljava/lang/Double;");
			} else if (this.itsConstantListSize >= 2000) {
				cfw.addPush(num);
				addDoubleWrap(cfw);
			} else {
				int N = this.itsConstantListSize;
				int index = 0;
				if (N == 0) {
					this.itsConstantList = new double[64];
				} else {
					double[] constantName;
					for (constantName = this.itsConstantList; index != N
							&& constantName[index] != num; ++index) {
						;
					}

					if (N == constantName.length) {
						constantName = new double[N * 2];
						System.arraycopy(this.itsConstantList, 0, constantName,
								0, N);
						this.itsConstantList = constantName;
					}
				}

				if (index == N) {
					this.itsConstantList[N] = num;
					this.itsConstantListSize = N + 1;
				}

				String arg7 = "_k" + index;
				String constantType = getStaticConstantWrapperType(num);
				cfw.add(178, this.mainClassName, arg7, constantType);
			}
		}

	}

	private static void addDoubleWrap(ClassFileWriter cfw) {
		cfw.addInvoke(184, "org/mozilla/javascript/optimizer/OptRuntime",
				"wrapDouble", "(D)Ljava/lang/Double;");
	}

	private static String getStaticConstantWrapperType(double num) {
		int inum = (int) num;
		return (double) inum == num
				? "Ljava/lang/Integer;"
				: "Ljava/lang/Double;";
	}

	static void pushUndefined(ClassFileWriter cfw) {
		cfw.add(178, "org/mozilla/javascript/Undefined", "instance",
				"Ljava/lang/Object;");
	}

	int getIndex(ScriptOrFnNode n) {
		return this.scriptOrFnIndexes.getExisting(n);
	}

	static String getDirectTargetFieldName(int i) {
		return "_dt" + i;
	}

	String getDirectCtorName(ScriptOrFnNode n) {
		return "_n" + this.getIndex(n);
	}

	String getBodyMethodName(ScriptOrFnNode n) {
		return "_c" + this.getIndex(n);
	}

	String getBodyMethodSignature(ScriptOrFnNode n) {
		StringBuffer sb = new StringBuffer();
		sb.append('(');
		sb.append(this.mainClassSignature);
		sb.append("Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;Lorg/mozilla/javascript/Scriptable;");
		if (n.getType() == 108) {
			OptFunctionNode ofn = OptFunctionNode.get(n);
			if (ofn.isTargetOfDirectCall()) {
				int pCount = ofn.fnode.getParamCount();

				for (int i = 0; i != pCount; ++i) {
					sb.append("Ljava/lang/Object;D");
				}
			}
		}

		sb.append("[Ljava/lang/Object;)Ljava/lang/Object;");
		return sb.toString();
	}

	String getFunctionInitMethodName(OptFunctionNode ofn) {
		return "_i" + this.getIndex(ofn.fnode);
	}

	String getCompiledRegexpName(ScriptOrFnNode n, int regexpIndex) {
		return "_re" + this.getIndex(n) + "_" + regexpIndex;
	}

	static RuntimeException badTree() {
		throw new RuntimeException("Bad tree in codegen");
	}

	void setMainMethodClass(String className) {
		this.mainMethodClass = className;
	}
}