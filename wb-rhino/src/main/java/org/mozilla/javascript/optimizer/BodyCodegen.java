/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.optimizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.mozilla.classfile.ClassFileWriter;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.FunctionNode;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.ScriptOrFnNode;
import org.mozilla.javascript.Node.Jump;
import org.mozilla.javascript.optimizer.Codegen;
import org.mozilla.javascript.optimizer.OptFunctionNode;
import org.mozilla.javascript.optimizer.OptRuntime;

class BodyCodegen {
	private static final int JAVASCRIPT_EXCEPTION = 0;
	private static final int EVALUATOR_EXCEPTION = 1;
	private static final int ECMAERROR_EXCEPTION = 2;
	private static final int THROWABLE_EXCEPTION = 3;
	static final int GENERATOR_TERMINATE = -1;
	static final int GENERATOR_START = 0;
	static final int GENERATOR_YIELD_START = 1;
	ClassFileWriter cfw;
	Codegen codegen;
	CompilerEnvirons compilerEnv;
	ScriptOrFnNode scriptOrFn;
	public int scriptOrFnIndex;
	private int savedCodeOffset;
	private OptFunctionNode fnCurrent;
	private boolean isTopLevel;
	private static final int MAX_LOCALS = 256;
	private int[] locals;
	private short firstFreeLocal;
	private short localsMax;
	private int itsLineNumber;
	private boolean hasVarsInRegs;
	private short[] varRegisters;
	private boolean inDirectCallFunction;
	private boolean itsForcedObjectParameters;
	private int enterAreaStartLabel;
	private int epilogueLabel;
	private short variableObjectLocal;
	private short popvLocal;
	private short contextLocal;
	private short argsLocal;
	private short operationLocal;
	private short thisObjLocal;
	private short funObjLocal;
	private short itsZeroArgArray;
	private short itsOneArgArray;
	private short scriptRegexpLocal;
	private short generatorStateLocal;
	private boolean isGenerator;
	private int generatorSwitch;
	private int maxLocals = 0;
	private int maxStack = 0;
	private Map<Node, BodyCodegen.FinallyReturnPoint> finallys;

	void generateBodyCode() {
		this.isGenerator = Codegen.isGenerator(this.scriptOrFn);
		this.initBodyGeneration();
		if (this.isGenerator) {
			String treeTop = "(" + this.codegen.mainClassSignature
					+ "Lorg/mozilla/javascript/Context;"
					+ "Lorg/mozilla/javascript/Scriptable;"
					+ "Ljava/lang/Object;"
					+ "Ljava/lang/Object;I)Ljava/lang/Object;";
			this.cfw.startMethod(
					this.codegen.getBodyMethodName(this.scriptOrFn) + "_gen",
					treeTop, 10);
		} else {
			this.cfw.startMethod(
					this.codegen.getBodyMethodName(this.scriptOrFn),
					this.codegen.getBodyMethodSignature(this.scriptOrFn), 10);
		}

		this.generatePrologue();
		Object treeTop1;
		if (this.fnCurrent != null) {
			treeTop1 = this.scriptOrFn.getLastChild();
		} else {
			treeTop1 = this.scriptOrFn;
		}

		this.generateStatement((Node) treeTop1);
		this.generateEpilogue();
		this.cfw.stopMethod((short) (this.localsMax + 1));
		if (this.isGenerator) {
			this.generateGenerator();
		}

	}

	private void generateGenerator() {
		this.cfw.startMethod(this.codegen.getBodyMethodName(this.scriptOrFn),
				this.codegen.getBodyMethodSignature(this.scriptOrFn), 10);
		this.initBodyGeneration();
		this.argsLocal = this.firstFreeLocal++;
		this.localsMax = this.firstFreeLocal;
		if (this.fnCurrent != null
				&& !this.inDirectCallFunction
				&& (!this.compilerEnv.isUseDynamicScope() || this.fnCurrent.fnode
						.getIgnoreDynamicScope())) {
			this.cfw.addALoad(this.funObjLocal);
			this.cfw.addInvoke(185, "org/mozilla/javascript/Scriptable",
					"getParentScope", "()Lorg/mozilla/javascript/Scriptable;");
			this.cfw.addAStore(this.variableObjectLocal);
		}

		this.cfw.addALoad(this.funObjLocal);
		this.cfw.addALoad(this.variableObjectLocal);
		this.cfw.addALoad(this.argsLocal);
		this.addScriptRuntimeInvoke(
				"createFunctionActivation",
				"(Lorg/mozilla/javascript/NativeFunction;Lorg/mozilla/javascript/Scriptable;[Ljava/lang/Object;)Lorg/mozilla/javascript/Scriptable;");
		this.cfw.addAStore(this.variableObjectLocal);
		this.cfw.add(187, this.codegen.mainClassName);
		this.cfw.add(89);
		this.cfw.addALoad(this.variableObjectLocal);
		this.cfw.addALoad(this.contextLocal);
		this.cfw.addPush(this.scriptOrFnIndex);
		this.cfw.addInvoke(183, this.codegen.mainClassName, "<init>",
				"(Lorg/mozilla/javascript/Scriptable;Lorg/mozilla/javascript/Context;I)V");
		this.cfw.add(89);
		if (this.isTopLevel) {
			Kit.codeBug();
		}

		this.cfw.add(42);
		this.cfw.add(180, this.codegen.mainClassName, "_dcp",
				this.codegen.mainClassSignature);
		this.cfw.add(181, this.codegen.mainClassName, "_dcp",
				this.codegen.mainClassSignature);
		this.generateNestedFunctionInits();
		this.cfw.addALoad(this.variableObjectLocal);
		this.cfw.addALoad(this.thisObjLocal);
		this.cfw.addLoadConstant(this.maxLocals);
		this.cfw.addLoadConstant(this.maxStack);
		this.addOptRuntimeInvoke(
				"createNativeGenerator",
				"(Lorg/mozilla/javascript/NativeFunction;Lorg/mozilla/javascript/Scriptable;Lorg/mozilla/javascript/Scriptable;II)Lorg/mozilla/javascript/Scriptable;");
		this.cfw.add(176);
		this.cfw.stopMethod((short) (this.localsMax + 1));
	}

	private void generateNestedFunctionInits() {
		int functionCount = this.scriptOrFn.getFunctionCount();

		for (int i = 0; i != functionCount; ++i) {
			OptFunctionNode ofn = OptFunctionNode.get(this.scriptOrFn, i);
			if (ofn.fnode.getFunctionType() == 1) {
				this.visitFunction(ofn, 1);
			}
		}

	}

	private void initBodyGeneration() {
		this.isTopLevel = this.scriptOrFn == this.codegen.scriptOrFnNodes[0];
		this.varRegisters = null;
		if (this.scriptOrFn.getType() == 108) {
			this.fnCurrent = OptFunctionNode.get(this.scriptOrFn);
			this.hasVarsInRegs = !this.fnCurrent.fnode.requiresActivation();
			if (this.hasVarsInRegs) {
				int n = this.fnCurrent.fnode.getParamAndVarCount();
				if (n != 0) {
					this.varRegisters = new short[n];
				}
			}

			this.inDirectCallFunction = this.fnCurrent.isTargetOfDirectCall();
			if (this.inDirectCallFunction && !this.hasVarsInRegs) {
				Codegen.badTree();
			}
		} else {
			this.fnCurrent = null;
			this.hasVarsInRegs = false;
			this.inDirectCallFunction = false;
		}

		this.locals = new int[256];
		this.funObjLocal = 0;
		this.contextLocal = 1;
		this.variableObjectLocal = 2;
		this.thisObjLocal = 3;
		this.localsMax = 4;
		this.firstFreeLocal = 4;
		this.popvLocal = -1;
		this.argsLocal = -1;
		this.itsZeroArgArray = -1;
		this.itsOneArgArray = -1;
		this.scriptRegexpLocal = -1;
		this.epilogueLabel = -1;
		this.enterAreaStartLabel = -1;
		this.generatorStateLocal = -1;
	}

	private void generatePrologue() {
		int debugVariableName;
		int linenum;
		if (this.inDirectCallFunction) {
			debugVariableName = this.scriptOrFn.getParamCount();
			if (this.firstFreeLocal != 4) {
				Kit.codeBug();
			}

			for (linenum = 0; linenum != debugVariableName; ++linenum) {
				this.varRegisters[linenum] = this.firstFreeLocal;
				this.firstFreeLocal = (short) (this.firstFreeLocal + 3);
			}

			if (!this.fnCurrent.getParameterNumberContext()) {
				this.itsForcedObjectParameters = true;

				for (linenum = 0; linenum != debugVariableName; ++linenum) {
					short varCount = this.varRegisters[linenum];
					this.cfw.addALoad(varCount);
					this.cfw.add(178, "java/lang/Void", "TYPE",
							"Ljava/lang/Class;");
					int constDeclarations = this.cfw.acquireLabel();
					this.cfw.add(166, constDeclarations);
					this.cfw.addDLoad(varCount + 1);
					this.addDoubleWrap();
					this.cfw.addAStore(varCount);
					this.cfw.markLabel(constDeclarations);
				}
			}
		}

		if (this.fnCurrent != null
				&& !this.inDirectCallFunction
				&& (!this.compilerEnv.isUseDynamicScope() || this.fnCurrent.fnode
						.getIgnoreDynamicScope())) {
			this.cfw.addALoad(this.funObjLocal);
			this.cfw.addInvoke(185, "org/mozilla/javascript/Scriptable",
					"getParentScope", "()Lorg/mozilla/javascript/Scriptable;");
			this.cfw.addAStore(this.variableObjectLocal);
		}

		this.argsLocal = this.firstFreeLocal++;
		this.localsMax = this.firstFreeLocal;
		if (this.isGenerator) {
			this.operationLocal = this.firstFreeLocal++;
			this.localsMax = this.firstFreeLocal;
			this.cfw.addALoad(this.thisObjLocal);
			this.generatorStateLocal = this.firstFreeLocal++;
			this.localsMax = this.firstFreeLocal;
			this.cfw.add(192,
					"org/mozilla/javascript/optimizer/OptRuntime$GeneratorState");
			this.cfw.add(89);
			this.cfw.addAStore(this.generatorStateLocal);
			this.cfw.add(
					180,
					"org/mozilla/javascript/optimizer/OptRuntime$GeneratorState",
					"thisObj", "Lorg/mozilla/javascript/Scriptable;");
			this.cfw.addAStore(this.thisObjLocal);
			if (this.epilogueLabel == -1) {
				this.epilogueLabel = this.cfw.acquireLabel();
			}

			ArrayList arg10 = ((FunctionNode) this.scriptOrFn)
					.getResumptionPoints();
			if (arg10 != null) {
				this.generateGetGeneratorResumptionPoint();
				this.generatorSwitch = this.cfw.addTableSwitch(0,
						arg10.size() + 0);
				this.generateCheckForThrowOrClose(-1, false, 0);
			}
		}

		if (this.fnCurrent == null && this.scriptOrFn.getRegexpCount() != 0) {
			this.scriptRegexpLocal = this.getNewWordLocal();
			this.codegen.pushRegExpArray(this.cfw, this.scriptOrFn,
					this.contextLocal, this.variableObjectLocal);
			this.cfw.addAStore(this.scriptRegexpLocal);
		}

		if (this.compilerEnv.isGenerateObserverCount()) {
			this.saveCurrentCodeOffset();
		}

		if (this.hasVarsInRegs) {
			debugVariableName = this.scriptOrFn.getParamCount();
			if (debugVariableName > 0 && !this.inDirectCallFunction) {
				this.cfw.addALoad(this.argsLocal);
				this.cfw.add(190);
				this.cfw.addPush(debugVariableName);
				linenum = this.cfw.acquireLabel();
				this.cfw.add(162, linenum);
				this.cfw.addALoad(this.argsLocal);
				this.cfw.addPush(debugVariableName);
				this.addScriptRuntimeInvoke("padArguments",
						"([Ljava/lang/Object;I)[Ljava/lang/Object;");
				this.cfw.addAStore(this.argsLocal);
				this.cfw.markLabel(linenum);
			}

			linenum = this.fnCurrent.fnode.getParamCount();
			int arg12 = this.fnCurrent.fnode.getParamAndVarCount();
			boolean[] arg13 = this.fnCurrent.fnode.getParamAndVarConst();
			short firstUndefVar = -1;

			for (int i = 0; i != arg12; ++i) {
				short reg = -1;
				if (i < linenum) {
					if (!this.inDirectCallFunction) {
						reg = this.getNewWordLocal();
						this.cfw.addALoad(this.argsLocal);
						this.cfw.addPush(i);
						this.cfw.add(50);
						this.cfw.addAStore(reg);
					}
				} else if (this.fnCurrent.isNumberVar(i)) {
					reg = this.getNewWordPairLocal(arg13[i]);
					this.cfw.addPush(0.0D);
					this.cfw.addDStore(reg);
				} else {
					reg = this.getNewWordLocal(arg13[i]);
					if (firstUndefVar == -1) {
						Codegen.pushUndefined(this.cfw);
						firstUndefVar = reg;
					} else {
						this.cfw.addALoad(firstUndefVar);
					}

					this.cfw.addAStore(reg);
				}

				if (reg >= 0) {
					if (arg13[i]) {
						this.cfw.addPush(0);
						this.cfw.addIStore(reg
								+ (this.fnCurrent.isNumberVar(i) ? 2 : 1));
					}

					this.varRegisters[i] = reg;
				}

				if (this.compilerEnv.isGenerateDebugInfo()) {
					String name = this.fnCurrent.fnode.getParamOrVarName(i);
					String type = this.fnCurrent.isNumberVar(i)
							? "D"
							: "Ljava/lang/Object;";
					int startPC = this.cfw.getCurrentCodeOffset();
					if (reg < 0) {
						reg = this.varRegisters[i];
					}

					this.cfw.addVariableDescriptor(name, type, startPC, reg);
				}
			}

		} else if (!this.isGenerator) {
			String arg11;
			if (this.fnCurrent != null) {
				arg11 = "activation";
				this.cfw.addALoad(this.funObjLocal);
				this.cfw.addALoad(this.variableObjectLocal);
				this.cfw.addALoad(this.argsLocal);
				this.addScriptRuntimeInvoke(
						"createFunctionActivation",
						"(Lorg/mozilla/javascript/NativeFunction;Lorg/mozilla/javascript/Scriptable;[Ljava/lang/Object;)Lorg/mozilla/javascript/Scriptable;");
				this.cfw.addAStore(this.variableObjectLocal);
				this.cfw.addALoad(this.contextLocal);
				this.cfw.addALoad(this.variableObjectLocal);
				this.addScriptRuntimeInvoke("enterActivationFunction",
						"(Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;)V");
			} else {
				arg11 = "global";
				this.cfw.addALoad(this.funObjLocal);
				this.cfw.addALoad(this.thisObjLocal);
				this.cfw.addALoad(this.contextLocal);
				this.cfw.addALoad(this.variableObjectLocal);
				this.cfw.addPush(0);
				this.addScriptRuntimeInvoke(
						"initScript",
						"(Lorg/mozilla/javascript/NativeFunction;Lorg/mozilla/javascript/Scriptable;Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;Z)V");
			}

			this.enterAreaStartLabel = this.cfw.acquireLabel();
			this.epilogueLabel = this.cfw.acquireLabel();
			this.cfw.markLabel(this.enterAreaStartLabel);
			this.generateNestedFunctionInits();
			if (this.compilerEnv.isGenerateDebugInfo()) {
				this.cfw.addVariableDescriptor(arg11,
						"Lorg/mozilla/javascript/Scriptable;",
						this.cfw.getCurrentCodeOffset(),
						this.variableObjectLocal);
			}

			if (this.fnCurrent == null) {
				this.popvLocal = this.getNewWordLocal();
				Codegen.pushUndefined(this.cfw);
				this.cfw.addAStore(this.popvLocal);
				linenum = this.scriptOrFn.getEndLineno();
				if (linenum != -1) {
					this.cfw.addLineNumberEntry((short) linenum);
				}
			} else {
				if (this.fnCurrent.itsContainsCalls0) {
					this.itsZeroArgArray = this.getNewWordLocal();
					this.cfw.add(178, "org/mozilla/javascript/ScriptRuntime",
							"emptyArgs", "[Ljava/lang/Object;");
					this.cfw.addAStore(this.itsZeroArgArray);
				}

				if (this.fnCurrent.itsContainsCalls1) {
					this.itsOneArgArray = this.getNewWordLocal();
					this.cfw.addPush(1);
					this.cfw.add(189, "java/lang/Object");
					this.cfw.addAStore(this.itsOneArgArray);
				}
			}

		}
	}

	private void generateGetGeneratorResumptionPoint() {
		this.cfw.addALoad(this.generatorStateLocal);
		this.cfw.add(180,
				"org/mozilla/javascript/optimizer/OptRuntime$GeneratorState",
				"resumptionPoint", "I");
	}

	private void generateSetGeneratorResumptionPoint(int nextState) {
		this.cfw.addALoad(this.generatorStateLocal);
		this.cfw.addLoadConstant(nextState);
		this.cfw.add(181,
				"org/mozilla/javascript/optimizer/OptRuntime$GeneratorState",
				"resumptionPoint", "I");
	}

	private void generateGetGeneratorStackState() {
		this.cfw.addALoad(this.generatorStateLocal);
		this.addOptRuntimeInvoke("getGeneratorStackState",
				"(Ljava/lang/Object;)[Ljava/lang/Object;");
	}

	private void generateEpilogue() {
		if (this.compilerEnv.isGenerateObserverCount()) {
			this.addInstructionCount();
		}

		if (this.isGenerator) {
			HashMap finallyHandler = ((FunctionNode) this.scriptOrFn)
					.getLiveLocals();
			int c;
			if (finallyHandler != null) {
				ArrayList exceptionObject = ((FunctionNode) this.scriptOrFn)
						.getResumptionPoints();

				for (int n = 0; n < exceptionObject.size(); ++n) {
					Node ret = (Node) exceptionObject.get(n);
					int[] startSwitch = (int[]) finallyHandler.get(ret);
					if (startSwitch != null) {
						this.cfw.markTableSwitchCase(this.generatorSwitch,
								this.getNextGeneratorState(ret));
						this.generateGetGeneratorLocalsState();

						for (c = 0; c < startSwitch.length; ++c) {
							this.cfw.add(89);
							this.cfw.addLoadConstant(c);
							this.cfw.add(50);
							this.cfw.addAStore(startSwitch[c]);
						}

						this.cfw.add(87);
						this.cfw.add(167, this.getTargetLabel(ret));
					}
				}
			}

			if (this.finallys != null) {
				Iterator arg8 = this.finallys.keySet().iterator();

				label58 : while (true) {
					Node arg10;
					do {
						if (!arg8.hasNext()) {
							break label58;
						}

						arg10 = (Node) arg8.next();
					} while (arg10.getType() != 124);

					BodyCodegen.FinallyReturnPoint arg11 = (BodyCodegen.FinallyReturnPoint) this.finallys
							.get(arg10);
					this.cfw.markLabel(arg11.tableLabel, 1);
					int arg12 = this.cfw.addTableSwitch(0,
							arg11.jsrPoints.size() - 1);
					c = 0;
					this.cfw.markTableSwitchDefault(arg12);

					for (int i = 0; i < arg11.jsrPoints.size(); ++i) {
						this.cfw.markTableSwitchCase(arg12, c);
						this.cfw.add(167,
								((Integer) arg11.jsrPoints.get(i)).intValue());
						++c;
					}
				}
			}
		}

		if (this.epilogueLabel != -1) {
			this.cfw.markLabel(this.epilogueLabel);
		}

		if (this.hasVarsInRegs) {
			this.cfw.add(176);
		} else {
			if (this.isGenerator) {
				if (((FunctionNode) this.scriptOrFn).getResumptionPoints() != null) {
					this.cfw.markTableSwitchDefault(this.generatorSwitch);
				}

				this.generateSetGeneratorResumptionPoint(-1);
				this.cfw.addALoad(this.variableObjectLocal);
				this.addOptRuntimeInvoke("throwStopIteration",
						"(Ljava/lang/Object;)V");
				Codegen.pushUndefined(this.cfw);
				this.cfw.add(176);
			} else if (this.fnCurrent == null) {
				this.cfw.addALoad(this.popvLocal);
				this.cfw.add(176);
			} else {
				this.generateActivationExit();
				this.cfw.add(176);
				int arg7 = this.cfw.acquireLabel();
				this.cfw.markHandler(arg7);
				short arg9 = this.getNewWordLocal();
				this.cfw.addAStore(arg9);
				this.generateActivationExit();
				this.cfw.addALoad(arg9);
				this.releaseWordLocal(arg9);
				this.cfw.add(191);
				this.cfw.addExceptionHandler(this.enterAreaStartLabel,
						this.epilogueLabel, arg7, (String) null);
			}

		}
	}

	private void generateGetGeneratorLocalsState() {
		this.cfw.addALoad(this.generatorStateLocal);
		this.addOptRuntimeInvoke("getGeneratorLocalsState",
				"(Ljava/lang/Object;)[Ljava/lang/Object;");
	}

	private void generateActivationExit() {
		if (this.fnCurrent != null && !this.hasVarsInRegs) {
			this.cfw.addALoad(this.contextLocal);
			this.addScriptRuntimeInvoke("exitActivationFunction",
					"(Lorg/mozilla/javascript/Context;)V");
		} else {
			throw Kit.codeBug();
		}
	}

	private void generateStatement(Node node) {
		this.updateLineNumber(node);
		int type = node.getType();
		Node child = node.getFirstChild();
		int finallyRegister;
		int enumType1;
		switch (type) {
			case 2 :
				this.generateExpression(child, node);
				this.cfw.addALoad(this.contextLocal);
				this.cfw.addALoad(this.variableObjectLocal);
				this.addScriptRuntimeInvoke(
						"enterWith",
						"(Ljava/lang/Object;Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;)Lorg/mozilla/javascript/Scriptable;");
				this.cfw.addAStore(this.variableObjectLocal);
				this.incReferenceWordLocal(this.variableObjectLocal);
				break;
			case 3 :
				this.cfw.addALoad(this.variableObjectLocal);
				this.addScriptRuntimeInvoke("leaveWith",
						"(Lorg/mozilla/javascript/Scriptable;)Lorg/mozilla/javascript/Scriptable;");
				this.cfw.addAStore(this.variableObjectLocal);
				this.decReferenceWordLocal(this.variableObjectLocal);
				break;
			case 4 :
			case 64 :
				if (!this.isGenerator) {
					if (child != null) {
						this.generateExpression(child, node);
					} else if (type == 4) {
						Codegen.pushUndefined(this.cfw);
					} else {
						if (this.popvLocal < 0) {
							throw Codegen.badTree();
						}

						this.cfw.addALoad(this.popvLocal);
					}
				}

				if (this.compilerEnv.isGenerateObserverCount()) {
					this.addInstructionCount();
				}

				if (this.epilogueLabel == -1) {
					if (!this.hasVarsInRegs) {
						throw Codegen.badTree();
					}

					this.epilogueLabel = this.cfw.acquireLabel();
				}

				this.cfw.add(167, this.epilogueLabel);
				break;
			case 5 :
			case 6 :
			case 7 :
			case 134 :
				if (this.compilerEnv.isGenerateObserverCount()) {
					this.addInstructionCount();
				}

				this.visitGoto((Jump) node, type, child);
				break;
			case 50 :
				this.generateExpression(child, node);
				if (this.compilerEnv.isGenerateObserverCount()) {
					this.addInstructionCount();
				}

				this.generateThrowJavaScriptException();
				break;
			case 51 :
				if (this.compilerEnv.isGenerateObserverCount()) {
					this.addInstructionCount();
				}

				this.cfw.addALoad(this.getLocalBlockRegister(node));
				this.cfw.add(191);
				break;
			case 57 :
				this.cfw.setStackTop(0);
				enumType1 = this.getLocalBlockRegister(node);
				finallyRegister = node.getExistingIntProp(14);
				String ret2 = child.getString();
				child = child.getNext();
				this.generateExpression(child, node);
				if (finallyRegister == 0) {
					this.cfw.add(1);
				} else {
					this.cfw.addALoad(enumType1);
				}

				this.cfw.addPush(ret2);
				this.cfw.addALoad(this.contextLocal);
				this.cfw.addALoad(this.variableObjectLocal);
				this.addScriptRuntimeInvoke(
						"newCatchScope",
						"(Ljava/lang/Throwable;Lorg/mozilla/javascript/Scriptable;Ljava/lang/String;Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;)Lorg/mozilla/javascript/Scriptable;");
				this.cfw.addAStore(enumType1);
				break;
			case 58 :
			case 59 :
			case 60 :
				this.generateExpression(child, node);
				this.cfw.addALoad(this.contextLocal);
				enumType1 = type == 58 ? 0 : (type == 59 ? 1 : 2);
				this.cfw.addPush(enumType1);
				this.addScriptRuntimeInvoke("enumInit",
						"(Ljava/lang/Object;Lorg/mozilla/javascript/Context;I)Ljava/lang/Object;");
				this.cfw.addAStore(this.getLocalBlockRegister(node));
				break;
			case 80 :
				this.visitTryCatchFinally((Jump) node, child);
				break;
			case 108 :
				enumType1 = node.getExistingIntProp(1);
				OptFunctionNode finallyRegister2 = OptFunctionNode.get(
						this.scriptOrFn, enumType1);
				int ret1 = finallyRegister2.fnode.getFunctionType();
				if (ret1 == 3) {
					this.visitFunction(finallyRegister2, ret1);
				} else if (ret1 != 1) {
					throw Codegen.badTree();
				}
				break;
			case 113 :
				if (this.compilerEnv.isGenerateObserverCount()) {
					this.addInstructionCount();
				}

				this.visitSwitch((Jump) node, child);
				break;
			case 122 :
			case 127 :
			case 128 :
			case 129 :
			case 131 :
			case 135 :
				if (this.compilerEnv.isGenerateObserverCount()) {
					this.addInstructionCount(1);
				}

				while (child != null) {
					this.generateStatement(child);
					child = child.getNext();
				}

				return;
			case 124 :
				if (this.compilerEnv.isGenerateObserverCount()) {
					this.saveCurrentCodeOffset();
				}

				this.cfw.setStackTop(1);
				short finallyRegister1 = this.getNewWordLocal();
				if (this.isGenerator) {
					this.generateIntegerWrap();
				}

				this.cfw.addAStore(finallyRegister1);

				while (child != null) {
					this.generateStatement(child);
					child = child.getNext();
				}

				if (this.isGenerator) {
					this.cfw.addALoad(finallyRegister1);
					this.cfw.add(192, "java/lang/Integer");
					this.generateIntegerUnwrap();
					BodyCodegen.FinallyReturnPoint ret = (BodyCodegen.FinallyReturnPoint) this.finallys
							.get(node);
					ret.tableLabel = this.cfw.acquireLabel();
					this.cfw.add(167, ret.tableLabel);
				} else {
					this.cfw.add(169, finallyRegister1);
				}

				this.releaseWordLocal((short) finallyRegister1);
				break;
			case 130 :
				if (this.compilerEnv.isGenerateObserverCount()) {
					this.addInstructionCount();
				}

				finallyRegister = this.getTargetLabel(node);
				this.cfw.markLabel(finallyRegister);
				if (this.compilerEnv.isGenerateObserverCount()) {
					this.saveCurrentCodeOffset();
				}
				break;
			case 132 :
				if (child.getType() == 56) {
					this.visitSetVar(child, child.getFirstChild(), false);
				} else if (child.getType() == 155) {
					this.visitSetConstVar(child, child.getFirstChild(), false);
				} else if (child.getType() == 72) {
					this.generateYieldPoint(child, false);
				} else {
					this.generateExpression(child, node);
					if (node.getIntProp(8, -1) != -1) {
						this.cfw.add(88);
					} else {
						this.cfw.add(87);
					}
				}
				break;
			case 133 :
				this.generateExpression(child, node);
				if (this.popvLocal < 0) {
					this.popvLocal = this.getNewWordLocal();
				}

				this.cfw.addAStore(this.popvLocal);
				break;
			case 140 :
				short enumType = this.getNewWordLocal();
				if (this.isGenerator) {
					this.cfw.add(1);
					this.cfw.addAStore(enumType);
				}

				node.putIntProp(2, enumType);

				while (child != null) {
					this.generateStatement(child);
					child = child.getNext();
				}

				this.releaseWordLocal((short) enumType);
				node.removeProp(2);
			case 159 :
				break;
			default :
				throw Codegen.badTree();
		}

	}

	private void generateIntegerWrap() {
		this.cfw.addInvoke(184, "java/lang/Integer", "valueOf",
				"(I)Ljava/lang/Integer;");
	}

	private void generateIntegerUnwrap() {
		this.cfw.addInvoke(182, "java/lang/Integer", "intValue", "()I");
	}

	private void generateThrowJavaScriptException() {
		this.cfw.add(187, "org/mozilla/javascript/JavaScriptException");
		this.cfw.add(90);
		this.cfw.add(95);
		this.cfw.addPush(this.scriptOrFn.getSourceName());
		this.cfw.addPush(this.itsLineNumber);
		this.cfw.addInvoke(183, "org/mozilla/javascript/JavaScriptException",
				"<init>", "(Ljava/lang/Object;Ljava/lang/String;I)V");
		this.cfw.add(191);
	}

	private int getNextGeneratorState(Node node) {
		int nodeIndex = ((FunctionNode) this.scriptOrFn).getResumptionPoints()
				.indexOf(node);
		return nodeIndex + 1;
	}

	private void generateExpression(Node node, Node parent) {
		int type = node.getType();
		Node child = node.getFirstChild();
		int initStmt;
		Node expr;
		Node initStmt1;
		OptFunctionNode expr1;
		int leaveWith1;
		int expr3;
		switch (type) {
			case 8 :
				this.visitSetName(node, child);
				break;
			case 9 :
			case 10 :
			case 11 :
			case 18 :
			case 19 :
			case 20 :
				this.visitBitOp(node, type, child);
				break;
			case 12 :
			case 13 :
			case 46 :
			case 47 :
				initStmt = this.cfw.acquireLabel();
				expr3 = this.cfw.acquireLabel();
				this.visitIfJumpEqOp(node, child, initStmt, expr3);
				this.addJumpedBooleanWrap(initStmt, expr3);
				break;
			case 14 :
			case 15 :
			case 16 :
			case 17 :
			case 52 :
			case 53 :
				initStmt = this.cfw.acquireLabel();
				expr3 = this.cfw.acquireLabel();
				this.visitIfJumpRelOp(node, child, initStmt, expr3);
				this.addJumpedBooleanWrap(initStmt, expr3);
				break;
			case 21 :
				this.generateExpression(child, node);
				this.generateExpression(child.getNext(), node);
				switch (node.getIntProp(8, -1)) {
					case 0 :
						this.cfw.add(99);
						return;
					case 1 :
						this.addOptRuntimeInvoke("add",
								"(DLjava/lang/Object;)Ljava/lang/Object;");
						return;
					case 2 :
						this.addOptRuntimeInvoke("add",
								"(Ljava/lang/Object;D)Ljava/lang/Object;");
						return;
					default :
						if (child.getType() == 41) {
							this.addScriptRuntimeInvoke("add",
									"(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/String;");
							return;
						} else {
							if (child.getNext().getType() == 41) {
								this.addScriptRuntimeInvoke("add",
										"(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/String;");
							} else {
								this.cfw.addALoad(this.contextLocal);
								this.addScriptRuntimeInvoke(
										"add",
										"(Ljava/lang/Object;Ljava/lang/Object;Lorg/mozilla/javascript/Context;)Ljava/lang/Object;");
							}

							return;
						}
				}
			case 22 :
				this.visitArithmetic(node, 103, child, parent);
				break;
			case 23 :
				this.visitArithmetic(node, 107, child, parent);
				break;
			case 24 :
			case 25 :
				this.visitArithmetic(node, type == 24 ? 111 : 115, child,
						parent);
				break;
			case 26 :
				initStmt = this.cfw.acquireLabel();
				expr3 = this.cfw.acquireLabel();
				leaveWith1 = this.cfw.acquireLabel();
				this.generateIfJump(child, node, initStmt, expr3);
				this.cfw.markLabel(initStmt);
				this.cfw.add(178, "java/lang/Boolean", "FALSE",
						"Ljava/lang/Boolean;");
				this.cfw.add(167, leaveWith1);
				this.cfw.markLabel(expr3);
				this.cfw.add(178, "java/lang/Boolean", "TRUE",
						"Ljava/lang/Boolean;");
				this.cfw.markLabel(leaveWith1);
				this.cfw.adjustStackTop(-1);
				break;
			case 27 :
				this.generateExpression(child, node);
				this.addScriptRuntimeInvoke("toInt32", "(Ljava/lang/Object;)I");
				this.cfw.addPush(-1);
				this.cfw.add(130);
				this.cfw.add(135);
				this.addDoubleWrap();
				break;
			case 28 :
			case 29 :
				this.generateExpression(child, node);
				this.addObjectToDouble();
				if (type == 29) {
					this.cfw.add(119);
				}

				this.addDoubleWrap();
				break;
			case 30 :
			case 38 :
				initStmt = node.getIntProp(10, 0);
				if (initStmt == 0) {
					expr1 = (OptFunctionNode) node.getProp(9);
					if (expr1 != null) {
						this.visitOptimizedCall(node, expr1, type, child);
					} else if (type == 38) {
						this.visitStandardCall(node, child);
					} else {
						this.visitStandardNew(node, child);
					}
				} else {
					this.visitSpecialCall(node, type, initStmt, child);
				}
				break;
			case 31 :
				this.generateExpression(child, node);
				child = child.getNext();
				this.generateExpression(child, node);
				this.cfw.addALoad(this.contextLocal);
				this.addScriptRuntimeInvoke(
						"delete",
						"(Ljava/lang/Object;Ljava/lang/Object;Lorg/mozilla/javascript/Context;)Ljava/lang/Object;");
				break;
			case 32 :
				this.generateExpression(child, node);
				this.addScriptRuntimeInvoke("typeof",
						"(Ljava/lang/Object;)Ljava/lang/String;");
				break;
			case 33 :
			case 34 :
				this.visitGetProp(node, child);
				break;
			case 35 :
			case 138 :
				this.visitSetProp(type, node, child);
				break;
			case 36 :
				this.generateExpression(child, node);
				this.generateExpression(child.getNext(), node);
				this.cfw.addALoad(this.contextLocal);
				if (node.getIntProp(8, -1) != -1) {
					this.addScriptRuntimeInvoke("getObjectIndex",
							"(Ljava/lang/Object;DLorg/mozilla/javascript/Context;)Ljava/lang/Object;");
				} else {
					this.cfw.addALoad(this.variableObjectLocal);
					this.addScriptRuntimeInvoke(
							"getObjectElem",
							"(Ljava/lang/Object;Ljava/lang/Object;Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;)Ljava/lang/Object;");
				}
				break;
			case 37 :
			case 139 :
				this.visitSetElem(type, node, child);
				break;
			case 39 :
				this.cfw.addALoad(this.contextLocal);
				this.cfw.addALoad(this.variableObjectLocal);
				this.cfw.addPush(node.getString());
				this.addScriptRuntimeInvoke(
						"name",
						"(Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;Ljava/lang/String;)Ljava/lang/Object;");
				break;
			case 40 :
				double initStmt3 = node.getDouble();
				if (node.getIntProp(8, -1) != -1) {
					this.cfw.addPush(initStmt3);
				} else {
					this.codegen.pushNumberAsObject(this.cfw, initStmt3);
				}
				break;
			case 41 :
				this.cfw.addPush(node.getString());
				break;
			case 42 :
				this.cfw.add(1);
				break;
			case 43 :
				this.cfw.addALoad(this.thisObjLocal);
				break;
			case 44 :
				this.cfw.add(178, "java/lang/Boolean", "FALSE",
						"Ljava/lang/Boolean;");
				break;
			case 45 :
				this.cfw.add(178, "java/lang/Boolean", "TRUE",
						"Ljava/lang/Boolean;");
				break;
			case 48 :
				initStmt = node.getExistingIntProp(4);
				if (this.fnCurrent == null) {
					this.cfw.addALoad(this.scriptRegexpLocal);
				} else {
					this.cfw.addALoad(this.funObjLocal);
					this.cfw.add(180, this.codegen.mainClassName, "_re",
							"[Ljava/lang/Object;");
				}

				this.cfw.addPush(initStmt);
				this.cfw.add(50);
				break;
			case 49 :
				while (child != null) {
					this.generateExpression(child, node);
					child = child.getNext();
				}

				this.cfw.addALoad(this.contextLocal);
				this.cfw.addALoad(this.variableObjectLocal);
				this.cfw.addPush(node.getString());
				this.addScriptRuntimeInvoke(
						"bind",
						"(Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;Ljava/lang/String;)Lorg/mozilla/javascript/Scriptable;");
				break;
			case 50 :
			case 51 :
			case 57 :
			case 58 :
			case 59 :
			case 60 :
			case 64 :
			case 80 :
			case 81 :
			case 82 :
			case 83 :
			case 84 :
			case 85 :
			case 86 :
			case 87 :
			case 89 :
			case 90 :
			case 91 :
			case 92 :
			case 93 :
			case 94 :
			case 95 :
			case 96 :
			case 97 :
			case 98 :
			case 99 :
			case 100 :
			case 102 :
			case 107 :
			case 109 :
			case 110 :
			case 111 :
			case 112 :
			case 113 :
			case 114 :
			case 115 :
			case 116 :
			case 117 :
			case 118 :
			case 119 :
			case 120 :
			case 121 :
			case 122 :
			case 123 :
			case 124 :
			case 126 :
			case 127 :
			case 128 :
			case 129 :
			case 130 :
			case 131 :
			case 132 :
			case 133 :
			case 134 :
			case 135 :
			case 140 :
			case 142 :
			case 143 :
			case 144 :
			case 146 :
			case 147 :
			case 150 :
			case 151 :
			case 152 :
			case 153 :
			case 157 :
			default :
				throw new RuntimeException("Unexpected node type " + type);
			case 54 :
				this.cfw.addALoad(this.getLocalBlockRegister(node));
				break;
			case 55 :
				this.visitGetVar(node);
				break;
			case 56 :
				this.visitSetVar(node, child, true);
				break;
			case 61 :
			case 62 :
				initStmt = this.getLocalBlockRegister(node);
				this.cfw.addALoad(initStmt);
				if (type == 61) {
					this.addScriptRuntimeInvoke("enumNext",
							"(Ljava/lang/Object;)Ljava/lang/Boolean;");
				} else {
					this.cfw.addALoad(this.contextLocal);
					this.addScriptRuntimeInvoke("enumId",
							"(Ljava/lang/Object;Lorg/mozilla/javascript/Context;)Ljava/lang/Object;");
				}
				break;
			case 63 :
				this.cfw.add(42);
				break;
			case 65 :
				this.visitArrayLiteral(node, child);
				break;
			case 66 :
				this.visitObjectLiteral(node, child);
				break;
			case 67 :
				this.generateExpression(child, node);
				this.cfw.addALoad(this.contextLocal);
				this.addScriptRuntimeInvoke(
						"refGet",
						"(Lorg/mozilla/javascript/Ref;Lorg/mozilla/javascript/Context;)Ljava/lang/Object;");
				break;
			case 68 :
			case 141 :
				this.generateExpression(child, node);
				child = child.getNext();
				if (type == 141) {
					this.cfw.add(89);
					this.cfw.addALoad(this.contextLocal);
					this.addScriptRuntimeInvoke(
							"refGet",
							"(Lorg/mozilla/javascript/Ref;Lorg/mozilla/javascript/Context;)Ljava/lang/Object;");
				}

				this.generateExpression(child, node);
				this.cfw.addALoad(this.contextLocal);
				this.addScriptRuntimeInvoke(
						"refSet",
						"(Lorg/mozilla/javascript/Ref;Ljava/lang/Object;Lorg/mozilla/javascript/Context;)Ljava/lang/Object;");
				break;
			case 69 :
				this.generateExpression(child, node);
				this.cfw.addALoad(this.contextLocal);
				this.addScriptRuntimeInvoke(
						"refDel",
						"(Lorg/mozilla/javascript/Ref;Lorg/mozilla/javascript/Context;)Ljava/lang/Object;");
				break;
			case 70 :
				this.generateFunctionAndThisObj(child, node);
				child = child.getNext();
				this.generateCallArgArray(node, child, false);
				this.cfw.addALoad(this.contextLocal);
				this.addScriptRuntimeInvoke(
						"callRef",
						"(Lorg/mozilla/javascript/Callable;Lorg/mozilla/javascript/Scriptable;[Ljava/lang/Object;Lorg/mozilla/javascript/Context;)Lorg/mozilla/javascript/Ref;");
				break;
			case 71 :
				String initStmt2 = (String) node.getProp(17);
				this.generateExpression(child, node);
				this.cfw.addPush(initStmt2);
				this.cfw.addALoad(this.contextLocal);
				this.addScriptRuntimeInvoke(
						"specialRef",
						"(Ljava/lang/Object;Ljava/lang/String;Lorg/mozilla/javascript/Context;)Lorg/mozilla/javascript/Ref;");
				break;
			case 72 :
				this.generateYieldPoint(node, true);
				break;
			case 73 :
				this.generateExpression(child, node);
				this.cfw.addALoad(this.contextLocal);
				this.addScriptRuntimeInvoke("setDefaultNamespace",
						"(Ljava/lang/Object;Lorg/mozilla/javascript/Context;)Ljava/lang/Object;");
				break;
			case 74 :
				this.generateExpression(child, node);
				this.cfw.addALoad(this.contextLocal);
				this.addScriptRuntimeInvoke("escapeAttributeValue",
						"(Ljava/lang/Object;Lorg/mozilla/javascript/Context;)Ljava/lang/String;");
				break;
			case 75 :
				this.generateExpression(child, node);
				this.cfw.addALoad(this.contextLocal);
				this.addScriptRuntimeInvoke("escapeTextValue",
						"(Ljava/lang/Object;Lorg/mozilla/javascript/Context;)Ljava/lang/String;");
				break;
			case 76 :
			case 77 :
			case 78 :
			case 79 :
				initStmt = node.getIntProp(16, 0);

				do {
					this.generateExpression(child, node);
					child = child.getNext();
				} while (child != null);

				this.cfw.addALoad(this.contextLocal);
				String expr2;
				String leaveWith2;
				switch (type) {
					case 76 :
						expr2 = "memberRef";
						leaveWith2 = "(Ljava/lang/Object;Ljava/lang/Object;Lorg/mozilla/javascript/Context;I)Lorg/mozilla/javascript/Ref;";
						break;
					case 77 :
						expr2 = "memberRef";
						leaveWith2 = "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Lorg/mozilla/javascript/Context;I)Lorg/mozilla/javascript/Ref;";
						break;
					case 78 :
						expr2 = "nameRef";
						leaveWith2 = "(Ljava/lang/Object;Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;I)Lorg/mozilla/javascript/Ref;";
						this.cfw.addALoad(this.variableObjectLocal);
						break;
					case 79 :
						expr2 = "nameRef";
						leaveWith2 = "(Ljava/lang/Object;Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;I)Lorg/mozilla/javascript/Ref;";
						this.cfw.addALoad(this.variableObjectLocal);
						break;
					default :
						throw Kit.codeBug();
				}

				this.cfw.addPush(initStmt);
				this.addScriptRuntimeInvoke(expr2, leaveWith2);
				break;
			case 88 :
				for (initStmt1 = child.getNext(); initStmt1 != null; initStmt1 = initStmt1
						.getNext()) {
					this.generateExpression(child, node);
					this.cfw.add(87);
					child = initStmt1;
				}

				this.generateExpression(child, node);
				break;
			case 101 :
				initStmt1 = child.getNext();
				expr = initStmt1.getNext();
				this.generateExpression(child, node);
				this.addScriptRuntimeInvoke("toBoolean",
						"(Ljava/lang/Object;)Z");
				leaveWith1 = this.cfw.acquireLabel();
				this.cfw.add(153, leaveWith1);
				short stack = this.cfw.getStackTop();
				this.generateExpression(initStmt1, node);
				int afterHook = this.cfw.acquireLabel();
				this.cfw.add(167, afterHook);
				this.cfw.markLabel(leaveWith1, stack);
				this.generateExpression(expr, node);
				this.cfw.markLabel(afterHook);
				break;
			case 103 :
			case 104 :
				this.generateExpression(child, node);
				this.cfw.add(89);
				this.addScriptRuntimeInvoke("toBoolean",
						"(Ljava/lang/Object;)Z");
				initStmt = this.cfw.acquireLabel();
				if (type == 104) {
					this.cfw.add(153, initStmt);
				} else {
					this.cfw.add(154, initStmt);
				}

				this.cfw.add(87);
				this.generateExpression(child.getNext(), node);
				this.cfw.markLabel(initStmt);
				break;
			case 105 :
			case 106 :
				this.visitIncDec(node);
				break;
			case 108 :
				if (this.fnCurrent != null || parent.getType() != 135) {
					initStmt = node.getExistingIntProp(1);
					expr1 = OptFunctionNode.get(this.scriptOrFn, initStmt);
					leaveWith1 = expr1.fnode.getFunctionType();
					if (leaveWith1 != 2) {
						throw Codegen.badTree();
					}

					this.visitFunction(expr1, leaveWith1);
				}
				break;
			case 125 :
				this.generateExpression(child, node);
				this.cfw.add(87);
				Codegen.pushUndefined(this.cfw);
				break;
			case 136 :
				this.visitTypeofname(node);
			case 137 :
				break;
			case 145 :
				this.visitDotQuery(node, child);
				break;
			case 148 :
				initStmt = -1;
				if (child.getType() == 40) {
					initStmt = child.getIntProp(8, -1);
				}

				if (initStmt != -1) {
					child.removeProp(8);
					this.generateExpression(child, node);
					child.putIntProp(8, initStmt);
				} else {
					this.generateExpression(child, node);
					this.addDoubleWrap();
				}
				break;
			case 149 :
				this.generateExpression(child, node);
				this.addObjectToDouble();
				break;
			case 154 :
				this.visitSetConst(node, child);
				break;
			case 155 :
				this.visitSetConstVar(node, child, true);
				break;
			case 156 :
				expr = child.getNext();
				this.generateStatement(child);
				this.generateExpression(expr, node);
				break;
			case 158 :
				expr = child.getNext();
				Node leaveWith = expr.getNext();
				this.generateStatement(child);
				this.generateExpression(expr.getFirstChild(), expr);
				this.generateStatement(leaveWith);
		}

	}

	private void generateYieldPoint(Node node, boolean exprContext) {
		short top = this.cfw.getStackTop();
		this.maxStack = this.maxStack > top ? this.maxStack : top;
		if (this.cfw.getStackTop() != 0) {
			this.generateGetGeneratorStackState();

			for (int child = 0; child < top; ++child) {
				this.cfw.add(90);
				this.cfw.add(95);
				this.cfw.addLoadConstant(child);
				this.cfw.add(95);
				this.cfw.add(83);
			}

			this.cfw.add(87);
		}

		Node arg7 = node.getFirstChild();
		if (arg7 != null) {
			this.generateExpression(arg7, node);
		} else {
			Codegen.pushUndefined(this.cfw);
		}

		int nextState = this.getNextGeneratorState(node);
		this.generateSetGeneratorResumptionPoint(nextState);
		boolean hasLocals = this.generateSaveLocals(node);
		this.cfw.add(176);
		this.generateCheckForThrowOrClose(this.getTargetLabel(node), hasLocals,
				nextState);
		if (top != 0) {
			this.generateGetGeneratorStackState();

			for (int i = 0; i < top; ++i) {
				this.cfw.add(89);
				this.cfw.addLoadConstant(top - i - 1);
				this.cfw.add(50);
				this.cfw.add(95);
			}

			this.cfw.add(87);
		}

		if (exprContext) {
			this.cfw.addALoad(this.argsLocal);
		}

	}

	private void generateCheckForThrowOrClose(int label, boolean hasLocals,
			int nextState) {
		int throwLabel = this.cfw.acquireLabel();
		int closeLabel = this.cfw.acquireLabel();
		this.cfw.markLabel(throwLabel);
		this.cfw.addALoad(this.argsLocal);
		this.generateThrowJavaScriptException();
		this.cfw.markLabel(closeLabel);
		this.cfw.addALoad(this.argsLocal);
		this.cfw.add(192, "java/lang/Throwable");
		this.cfw.add(191);
		if (label != -1) {
			this.cfw.markLabel(label);
		}

		if (!hasLocals) {
			this.cfw.markTableSwitchCase(this.generatorSwitch, nextState);
		}

		this.cfw.addILoad(this.operationLocal);
		this.cfw.addLoadConstant(2);
		this.cfw.add(159, closeLabel);
		this.cfw.addILoad(this.operationLocal);
		this.cfw.addLoadConstant(1);
		this.cfw.add(159, throwLabel);
	}

	private void generateIfJump(Node node, Node parent, int trueLabel,
			int falseLabel) {
		int type = node.getType();
		Node child = node.getFirstChild();
		switch (type) {
			case 12 :
			case 13 :
			case 46 :
			case 47 :
				this.visitIfJumpEqOp(node, child, trueLabel, falseLabel);
				break;
			case 14 :
			case 15 :
			case 16 :
			case 17 :
			case 52 :
			case 53 :
				this.visitIfJumpRelOp(node, child, trueLabel, falseLabel);
				break;
			case 26 :
				this.generateIfJump(child, node, falseLabel, trueLabel);
				break;
			case 103 :
			case 104 :
				int interLabel = this.cfw.acquireLabel();
				if (type == 104) {
					this.generateIfJump(child, node, interLabel, falseLabel);
				} else {
					this.generateIfJump(child, node, trueLabel, interLabel);
				}

				this.cfw.markLabel(interLabel);
				child = child.getNext();
				this.generateIfJump(child, node, trueLabel, falseLabel);
				break;
			default :
				this.generateExpression(node, parent);
				this.addScriptRuntimeInvoke("toBoolean",
						"(Ljava/lang/Object;)Z");
				this.cfw.add(154, trueLabel);
				this.cfw.add(167, falseLabel);
		}

	}

	private void visitFunction(OptFunctionNode ofn, int functionType) {
		int fnIndex = this.codegen.getIndex(ofn.fnode);
		this.cfw.add(187, this.codegen.mainClassName);
		this.cfw.add(89);
		this.cfw.addALoad(this.variableObjectLocal);
		this.cfw.addALoad(this.contextLocal);
		this.cfw.addPush(fnIndex);
		this.cfw.addInvoke(183, this.codegen.mainClassName, "<init>",
				"(Lorg/mozilla/javascript/Scriptable;Lorg/mozilla/javascript/Context;I)V");
		this.cfw.add(89);
		if (this.isTopLevel) {
			this.cfw.add(42);
		} else {
			this.cfw.add(42);
			this.cfw.add(180, this.codegen.mainClassName, "_dcp",
					this.codegen.mainClassSignature);
		}

		this.cfw.add(181, this.codegen.mainClassName, "_dcp",
				this.codegen.mainClassSignature);
		int directTargetIndex = ofn.getDirectTargetIndex();
		if (directTargetIndex >= 0) {
			this.cfw.add(89);
			if (this.isTopLevel) {
				this.cfw.add(42);
			} else {
				this.cfw.add(42);
				this.cfw.add(180, this.codegen.mainClassName, "_dcp",
						this.codegen.mainClassSignature);
			}

			this.cfw.add(95);
			this.cfw.add(181, this.codegen.mainClassName,
					Codegen.getDirectTargetFieldName(directTargetIndex),
					this.codegen.mainClassSignature);
		}

		if (functionType != 2) {
			this.cfw.addPush(functionType);
			this.cfw.addALoad(this.variableObjectLocal);
			this.cfw.addALoad(this.contextLocal);
			this.addOptRuntimeInvoke(
					"initFunction",
					"(Lorg/mozilla/javascript/NativeFunction;ILorg/mozilla/javascript/Scriptable;Lorg/mozilla/javascript/Context;)V");
		}
	}

	private int getTargetLabel(Node target) {
		int labelId = target.labelId();
		if (labelId == -1) {
			labelId = this.cfw.acquireLabel();
			target.labelId(labelId);
		}

		return labelId;
	}

	private void visitGoto(Jump node, int type, Node child) {
		Node target = node.target;
		if (type != 6 && type != 7) {
			if (type == 134) {
				if (this.isGenerator) {
					this.addGotoWithReturn(target);
				} else {
					this.addGoto(target, 168);
				}
			} else {
				this.addGoto(target, 167);
			}
		} else {
			if (child == null) {
				throw Codegen.badTree();
			}

			int targetLabel = this.getTargetLabel(target);
			int fallThruLabel = this.cfw.acquireLabel();
			if (type == 6) {
				this.generateIfJump(child, node, targetLabel, fallThruLabel);
			} else {
				this.generateIfJump(child, node, fallThruLabel, targetLabel);
			}

			this.cfw.markLabel(fallThruLabel);
		}

	}

	private void addGotoWithReturn(Node target) {
		BodyCodegen.FinallyReturnPoint ret = (BodyCodegen.FinallyReturnPoint) this.finallys
				.get(target);
		this.cfw.addLoadConstant(ret.jsrPoints.size());
		this.addGoto(target, 167);
		int retLabel = this.cfw.acquireLabel();
		this.cfw.markLabel(retLabel);
		ret.jsrPoints.add(Integer.valueOf(retLabel));
	}

	private void visitArrayLiteral(Node node, Node child) {
		int count = 0;

		for (Node skipIndexes = child; skipIndexes != null; skipIndexes = skipIndexes
				.getNext()) {
			++count;
		}

		this.addNewObjectArray(count);

		for (int arg4 = 0; arg4 != count; ++arg4) {
			this.cfw.add(89);
			this.cfw.addPush(arg4);
			this.generateExpression(child, node);
			this.cfw.add(83);
			child = child.getNext();
		}

		int[] arg5 = (int[]) ((int[]) node.getProp(11));
		if (arg5 == null) {
			this.cfw.add(1);
			this.cfw.add(3);
		} else {
			this.cfw.addPush(OptRuntime.encodeIntArray(arg5));
			this.cfw.addPush(arg5.length);
		}

		this.cfw.addALoad(this.contextLocal);
		this.cfw.addALoad(this.variableObjectLocal);
		this.addOptRuntimeInvoke(
				"newArrayLiteral",
				"([Ljava/lang/Object;Ljava/lang/String;ILorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;)Lorg/mozilla/javascript/Scriptable;");
	}

	private void visitObjectLiteral(Node node, Node child) {
		Object[] properties = (Object[]) ((Object[]) node.getProp(12));
		int count = properties.length;
		this.addNewObjectArray(count);

		for (int child2 = 0; child2 != count; ++child2) {
			this.cfw.add(89);
			this.cfw.addPush(child2);
			Object i = properties[child2];
			if (i instanceof String) {
				this.cfw.addPush((String) i);
			} else {
				this.cfw.addPush(((Integer) i).intValue());
				this.addScriptRuntimeInvoke("wrapInt", "(I)Ljava/lang/Integer;");
			}

			this.cfw.add(83);
		}

		this.addNewObjectArray(count);
		Node arg7 = child;

		int childType;
		int arg8;
		for (arg8 = 0; arg8 != count; ++arg8) {
			this.cfw.add(89);
			this.cfw.addPush(arg8);
			childType = child.getType();
			if (childType == 150) {
				this.generateExpression(child.getFirstChild(), node);
			} else if (childType == 151) {
				this.generateExpression(child.getFirstChild(), node);
			} else {
				this.generateExpression(child, node);
			}

			this.cfw.add(83);
			child = child.getNext();
		}

		this.cfw.addPush(count);
		this.cfw.add(188, 10);

		for (arg8 = 0; arg8 != count; ++arg8) {
			this.cfw.add(89);
			this.cfw.addPush(arg8);
			childType = arg7.getType();
			if (childType == 150) {
				this.cfw.add(2);
			} else if (childType == 151) {
				this.cfw.add(4);
			} else {
				this.cfw.add(3);
			}

			this.cfw.add(79);
			arg7 = arg7.getNext();
		}

		this.cfw.addALoad(this.contextLocal);
		this.cfw.addALoad(this.variableObjectLocal);
		this.addScriptRuntimeInvoke(
				"newObjectLiteral",
				"([Ljava/lang/Object;[Ljava/lang/Object;[ILorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;)Lorg/mozilla/javascript/Scriptable;");
	}

	private void visitSpecialCall(Node node, int type, int specialType,
			Node child) {
		this.cfw.addALoad(this.contextLocal);
		if (type == 30) {
			this.generateExpression(child, node);
		} else {
			this.generateFunctionAndThisObj(child, node);
		}

		child = child.getNext();
		this.generateCallArgArray(node, child, false);
		String methodName;
		String callSignature;
		if (type == 30) {
			methodName = "newObjectSpecial";
			callSignature = "(Lorg/mozilla/javascript/Context;Ljava/lang/Object;[Ljava/lang/Object;Lorg/mozilla/javascript/Scriptable;Lorg/mozilla/javascript/Scriptable;I)Ljava/lang/Object;";
			this.cfw.addALoad(this.variableObjectLocal);
			this.cfw.addALoad(this.thisObjLocal);
			this.cfw.addPush(specialType);
		} else {
			methodName = "callSpecial";
			callSignature = "(Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Callable;Lorg/mozilla/javascript/Scriptable;[Ljava/lang/Object;Lorg/mozilla/javascript/Scriptable;Lorg/mozilla/javascript/Scriptable;ILjava/lang/String;I)Ljava/lang/Object;";
			this.cfw.addALoad(this.variableObjectLocal);
			this.cfw.addALoad(this.thisObjLocal);
			this.cfw.addPush(specialType);
			String sourceName = this.scriptOrFn.getSourceName();
			this.cfw.addPush(sourceName == null ? "" : sourceName);
			this.cfw.addPush(this.itsLineNumber);
		}

		this.addOptRuntimeInvoke(methodName, callSignature);
	}

	private void visitStandardCall(Node node, Node child) {
		if (node.getType() != 38) {
			throw Codegen.badTree();
		} else {
			Node firstArgChild = child.getNext();
			int childType = child.getType();
			String methodName;
			String signature;
			String argCount;
			Node arg;
			if (firstArgChild == null) {
				if (childType == 39) {
					argCount = child.getString();
					this.cfw.addPush(argCount);
					methodName = "callName0";
					signature = "(Ljava/lang/String;Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;)Ljava/lang/Object;";
				} else if (childType == 33) {
					Node arg9 = child.getFirstChild();
					this.generateExpression(arg9, node);
					arg = arg9.getNext();
					String property = arg.getString();
					this.cfw.addPush(property);
					methodName = "callProp0";
					signature = "(Ljava/lang/Object;Ljava/lang/String;Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;)Ljava/lang/Object;";
				} else {
					if (childType == 34) {
						throw Kit.codeBug();
					}

					this.generateFunctionAndThisObj(child, node);
					methodName = "call0";
					signature = "(Lorg/mozilla/javascript/Callable;Lorg/mozilla/javascript/Scriptable;Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;)Ljava/lang/Object;";
				}
			} else if (childType == 39) {
				argCount = child.getString();
				this.generateCallArgArray(node, firstArgChild, false);
				this.cfw.addPush(argCount);
				methodName = "callName";
				signature = "([Ljava/lang/Object;Ljava/lang/String;Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;)Ljava/lang/Object;";
			} else {
				int arg10 = 0;

				for (arg = firstArgChild; arg != null; arg = arg.getNext()) {
					++arg10;
				}

				this.generateFunctionAndThisObj(child, node);
				if (arg10 == 1) {
					this.generateExpression(firstArgChild, node);
					methodName = "call1";
					signature = "(Lorg/mozilla/javascript/Callable;Lorg/mozilla/javascript/Scriptable;Ljava/lang/Object;Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;)Ljava/lang/Object;";
				} else if (arg10 == 2) {
					this.generateExpression(firstArgChild, node);
					this.generateExpression(firstArgChild.getNext(), node);
					methodName = "call2";
					signature = "(Lorg/mozilla/javascript/Callable;Lorg/mozilla/javascript/Scriptable;Ljava/lang/Object;Ljava/lang/Object;Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;)Ljava/lang/Object;";
				} else {
					this.generateCallArgArray(node, firstArgChild, false);
					methodName = "callN";
					signature = "(Lorg/mozilla/javascript/Callable;Lorg/mozilla/javascript/Scriptable;[Ljava/lang/Object;Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;)Ljava/lang/Object;";
				}
			}

			this.cfw.addALoad(this.contextLocal);
			this.cfw.addALoad(this.variableObjectLocal);
			this.addOptRuntimeInvoke(methodName, signature);
		}
	}

	private void visitStandardNew(Node node, Node child) {
		if (node.getType() != 30) {
			throw Codegen.badTree();
		} else {
			Node firstArgChild = child.getNext();
			this.generateExpression(child, node);
			this.cfw.addALoad(this.contextLocal);
			this.cfw.addALoad(this.variableObjectLocal);
			this.generateCallArgArray(node, firstArgChild, false);
			this.addScriptRuntimeInvoke(
					"newObject",
					"(Ljava/lang/Object;Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;[Ljava/lang/Object;)Lorg/mozilla/javascript/Scriptable;");
		}
	}

	private void visitOptimizedCall(Node node, OptFunctionNode target,
			int type, Node child) {
		Node firstArgChild = child.getNext();
		short thisObjLocal = 0;
		if (type == 30) {
			this.generateExpression(child, node);
		} else {
			this.generateFunctionAndThisObj(child, node);
			thisObjLocal = this.getNewWordLocal();
			this.cfw.addAStore(thisObjLocal);
		}

		int beyond = this.cfw.acquireLabel();
		int directTargetIndex = target.getDirectTargetIndex();
		if (this.isTopLevel) {
			this.cfw.add(42);
		} else {
			this.cfw.add(42);
			this.cfw.add(180, this.codegen.mainClassName, "_dcp",
					this.codegen.mainClassSignature);
		}

		this.cfw.add(180, this.codegen.mainClassName,
				Codegen.getDirectTargetFieldName(directTargetIndex),
				this.codegen.mainClassSignature);
		this.cfw.add(92);
		int regularCall = this.cfw.acquireLabel();
		this.cfw.add(166, regularCall);
		short stackHeight = this.cfw.getStackTop();
		this.cfw.add(95);
		this.cfw.add(87);
		if (this.compilerEnv.isUseDynamicScope()) {
			this.cfw.addALoad(this.contextLocal);
			this.cfw.addALoad(this.variableObjectLocal);
		} else {
			this.cfw.add(89);
			this.cfw.addInvoke(185, "org/mozilla/javascript/Scriptable",
					"getParentScope", "()Lorg/mozilla/javascript/Scriptable;");
			this.cfw.addALoad(this.contextLocal);
			this.cfw.add(95);
		}

		if (type == 30) {
			this.cfw.add(1);
		} else {
			this.cfw.addALoad(thisObjLocal);
		}

		for (Node argChild = firstArgChild; argChild != null; argChild = argChild
				.getNext()) {
			int dcp_register = this.nodeIsDirectCallParameter(argChild);
			if (dcp_register >= 0) {
				this.cfw.addALoad(dcp_register);
				this.cfw.addDLoad(dcp_register + 1);
			} else if (argChild.getIntProp(8, -1) == 0) {
				this.cfw.add(178, "java/lang/Void", "TYPE", "Ljava/lang/Class;");
				this.generateExpression(argChild, node);
			} else {
				this.generateExpression(argChild, node);
				this.cfw.addPush(0.0D);
			}
		}

		this.cfw.add(178, "org/mozilla/javascript/ScriptRuntime", "emptyArgs",
				"[Ljava/lang/Object;");
		this.cfw.addInvoke(184, this.codegen.mainClassName, type == 30
				? this.codegen.getDirectCtorName(target.fnode)
				: this.codegen.getBodyMethodName(target.fnode), this.codegen
				.getBodyMethodSignature(target.fnode));
		this.cfw.add(167, beyond);
		this.cfw.markLabel(regularCall, stackHeight);
		this.cfw.add(87);
		this.cfw.addALoad(this.contextLocal);
		this.cfw.addALoad(this.variableObjectLocal);
		if (type != 30) {
			this.cfw.addALoad(thisObjLocal);
			this.releaseWordLocal(thisObjLocal);
		}

		this.generateCallArgArray(node, firstArgChild, true);
		if (type == 30) {
			this.addScriptRuntimeInvoke(
					"newObject",
					"(Ljava/lang/Object;Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;[Ljava/lang/Object;)Lorg/mozilla/javascript/Scriptable;");
		} else {
			this.cfw.addInvoke(
					185,
					"org/mozilla/javascript/Callable",
					"call",
					"(Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;Lorg/mozilla/javascript/Scriptable;[Ljava/lang/Object;)Ljava/lang/Object;");
		}

		this.cfw.markLabel(beyond);
	}

	private void generateCallArgArray(Node node, Node argChild,
			boolean directCall) {
		int argCount = 0;

		for (Node i = argChild; i != null; i = i.getNext()) {
			++argCount;
		}

		if (argCount == 1 && this.itsOneArgArray >= 0) {
			this.cfw.addALoad(this.itsOneArgArray);
		} else {
			this.addNewObjectArray(argCount);
		}

		for (int arg7 = 0; arg7 != argCount; ++arg7) {
			if (!this.isGenerator) {
				this.cfw.add(89);
				this.cfw.addPush(arg7);
			}

			if (!directCall) {
				this.generateExpression(argChild, node);
			} else {
				int tempLocal = this.nodeIsDirectCallParameter(argChild);
				if (tempLocal >= 0) {
					this.dcpLoadAsObject(tempLocal);
				} else {
					this.generateExpression(argChild, node);
					int childNumberFlag = argChild.getIntProp(8, -1);
					if (childNumberFlag == 0) {
						this.addDoubleWrap();
					}
				}
			}

			if (this.isGenerator) {
				short arg8 = this.getNewWordLocal();
				this.cfw.addAStore(arg8);
				this.cfw.add(192, "[Ljava/lang/Object;");
				this.cfw.add(89);
				this.cfw.addPush(arg7);
				this.cfw.addALoad(arg8);
				this.releaseWordLocal(arg8);
			}

			this.cfw.add(83);
			argChild = argChild.getNext();
		}

	}

	private void generateFunctionAndThisObj(Node node, Node parent) {
		int type = node.getType();
		switch (node.getType()) {
			case 33 :
			case 36 :
				Node name1 = node.getFirstChild();
				this.generateExpression(name1, node);
				Node id = name1.getNext();
				if (type == 33) {
					String property = id.getString();
					this.cfw.addPush(property);
					this.cfw.addALoad(this.contextLocal);
					this.cfw.addALoad(this.variableObjectLocal);
					this.addScriptRuntimeInvoke(
							"getPropFunctionAndThis",
							"(Ljava/lang/Object;Ljava/lang/String;Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;)Lorg/mozilla/javascript/Callable;");
				} else {
					if (node.getIntProp(8, -1) != -1) {
						throw Codegen.badTree();
					}

					this.generateExpression(id, node);
					this.cfw.addALoad(this.contextLocal);
					this.addScriptRuntimeInvoke(
							"getElemFunctionAndThis",
							"(Ljava/lang/Object;Ljava/lang/Object;Lorg/mozilla/javascript/Context;)Lorg/mozilla/javascript/Callable;");
				}
				break;
			case 34 :
				throw Kit.codeBug();
			case 35 :
			case 37 :
			case 38 :
			default :
				this.generateExpression(node, parent);
				this.cfw.addALoad(this.contextLocal);
				this.addScriptRuntimeInvoke(
						"getValueFunctionAndThis",
						"(Ljava/lang/Object;Lorg/mozilla/javascript/Context;)Lorg/mozilla/javascript/Callable;");
				break;
			case 39 :
				String name = node.getString();
				this.cfw.addPush(name);
				this.cfw.addALoad(this.contextLocal);
				this.cfw.addALoad(this.variableObjectLocal);
				this.addScriptRuntimeInvoke(
						"getNameFunctionAndThis",
						"(Ljava/lang/String;Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;)Lorg/mozilla/javascript/Callable;");
		}

		this.cfw.addALoad(this.contextLocal);
		this.addScriptRuntimeInvoke("lastStoredScriptable",
				"(Lorg/mozilla/javascript/Context;)Lorg/mozilla/javascript/Scriptable;");
	}

	private void updateLineNumber(Node node) {
		this.itsLineNumber = node.getLineno();
		if (this.itsLineNumber != -1) {
			this.cfw.addLineNumberEntry((short) this.itsLineNumber);
		}
	}

	private void visitTryCatchFinally(Jump node, Node child) {
		short savedVariableObject = this.getNewWordLocal();
		this.cfw.addALoad(this.variableObjectLocal);
		this.cfw.addAStore(savedVariableObject);
		int startLabel = this.cfw.acquireLabel();
		this.cfw.markLabel(startLabel, 0);
		Node catchTarget = node.target;
		Node finallyTarget = node.getFinally();
		if (this.isGenerator && finallyTarget != null) {
			BodyCodegen.FinallyReturnPoint realEnd = new BodyCodegen.FinallyReturnPoint();
			if (this.finallys == null) {
				this.finallys = new HashMap();
			}

			this.finallys.put(finallyTarget, realEnd);
			this.finallys.put(finallyTarget.getNext(), realEnd);
		}

		while (child != null) {
			this.generateStatement(child);
			child = child.getNext();
		}

		int realEnd1 = this.cfw.acquireLabel();
		this.cfw.add(167, realEnd1);
		int exceptionLocal = this.getLocalBlockRegister(node);
		int finallyHandler;
		if (catchTarget != null) {
			finallyHandler = catchTarget.labelId();
			this.generateCatchBlock(0, savedVariableObject, finallyHandler,
					startLabel, exceptionLocal);
			this.generateCatchBlock(1, savedVariableObject, finallyHandler,
					startLabel, exceptionLocal);
			this.generateCatchBlock(2, savedVariableObject, finallyHandler,
					startLabel, exceptionLocal);
			Context finallyLabel = Context.getCurrentContext();
			if (finallyLabel != null && finallyLabel.hasFeature(13)) {
				this.generateCatchBlock(3, savedVariableObject, finallyHandler,
						startLabel, exceptionLocal);
			}
		}

		if (finallyTarget != null) {
			finallyHandler = this.cfw.acquireLabel();
			this.cfw.markHandler(finallyHandler);
			this.cfw.addAStore(exceptionLocal);
			this.cfw.addALoad(savedVariableObject);
			this.cfw.addAStore(this.variableObjectLocal);
			int finallyLabel1 = finallyTarget.labelId();
			if (this.isGenerator) {
				this.addGotoWithReturn(finallyTarget);
			} else {
				this.cfw.add(168, finallyLabel1);
			}

			this.cfw.addALoad(exceptionLocal);
			if (this.isGenerator) {
				this.cfw.add(192, "java/lang/Throwable");
			}

			this.cfw.add(191);
			this.cfw.addExceptionHandler(startLabel, finallyLabel1,
					finallyHandler, (String) null);
		}

		this.releaseWordLocal(savedVariableObject);
		this.cfw.markLabel(realEnd1);
	}

	private void generateCatchBlock(int exceptionType,
			short savedVariableObject, int catchLabel, int startLabel,
			int exceptionLocal) {
		int handler = this.cfw.acquireLabel();
		this.cfw.markHandler(handler);
		this.cfw.addAStore(exceptionLocal);
		this.cfw.addALoad(savedVariableObject);
		this.cfw.addAStore(this.variableObjectLocal);
		String exceptionName;
		if (exceptionType == 0) {
			exceptionName = "org/mozilla/javascript/JavaScriptException";
		} else if (exceptionType == 1) {
			exceptionName = "org/mozilla/javascript/EvaluatorException";
		} else if (exceptionType == 2) {
			exceptionName = "org/mozilla/javascript/EcmaError";
		} else {
			if (exceptionType != 3) {
				throw Kit.codeBug();
			}

			exceptionName = "java/lang/Throwable";
		}

		this.cfw.addExceptionHandler(startLabel, catchLabel, handler,
				exceptionName);
		this.cfw.add(167, catchLabel);
	}

	private boolean generateSaveLocals(Node node) {
		int count = 0;

		for (int ls = 0; ls < this.firstFreeLocal; ++ls) {
			if (this.locals[ls] != 0) {
				++count;
			}
		}

		if (count == 0) {
			((FunctionNode) this.scriptOrFn).addLiveLocals(node, (int[]) null);
			return false;
		} else {
			this.maxLocals = this.maxLocals > count ? this.maxLocals : count;
			int[] arg5 = new int[count];
			int s = 0;

			int i;
			for (i = 0; i < this.firstFreeLocal; ++i) {
				if (this.locals[i] != 0) {
					arg5[s] = i;
					++s;
				}
			}

			((FunctionNode) this.scriptOrFn).addLiveLocals(node, arg5);
			this.generateGetGeneratorLocalsState();

			for (i = 0; i < count; ++i) {
				this.cfw.add(89);
				this.cfw.addLoadConstant(i);
				this.cfw.addALoad(arg5[i]);
				this.cfw.add(83);
			}

			this.cfw.add(87);
			return true;
		}
	}

	private void visitSwitch(Jump switchNode, Node child) {
		this.generateExpression(child, switchNode);
		short selector = this.getNewWordLocal();
		this.cfw.addAStore(selector);

		for (Jump caseNode = (Jump) child.getNext(); caseNode != null; caseNode = (Jump) caseNode
				.getNext()) {
			if (caseNode.getType() != 114) {
				throw Codegen.badTree();
			}

			Node test = caseNode.getFirstChild();
			this.generateExpression(test, caseNode);
			this.cfw.addALoad(selector);
			this.addScriptRuntimeInvoke("shallowEq",
					"(Ljava/lang/Object;Ljava/lang/Object;)Z");
			this.addGoto(caseNode.target, 154);
		}

		this.releaseWordLocal(selector);
	}

	private void visitTypeofname(Node node) {
		if (this.hasVarsInRegs) {
			int varIndex = this.fnCurrent.fnode.getIndexForNameNode(node);
			if (varIndex >= 0) {
				if (this.fnCurrent.isNumberVar(varIndex)) {
					this.cfw.addPush("number");
				} else if (this.varIsDirectCallParameter(varIndex)) {
					short dcp_register = this.varRegisters[varIndex];
					this.cfw.addALoad(dcp_register);
					this.cfw.add(178, "java/lang/Void", "TYPE",
							"Ljava/lang/Class;");
					int isNumberLabel = this.cfw.acquireLabel();
					this.cfw.add(165, isNumberLabel);
					short stack = this.cfw.getStackTop();
					this.cfw.addALoad(dcp_register);
					this.addScriptRuntimeInvoke("typeof",
							"(Ljava/lang/Object;)Ljava/lang/String;");
					int beyond = this.cfw.acquireLabel();
					this.cfw.add(167, beyond);
					this.cfw.markLabel(isNumberLabel, stack);
					this.cfw.addPush("number");
					this.cfw.markLabel(beyond);
				} else {
					this.cfw.addALoad(this.varRegisters[varIndex]);
					this.addScriptRuntimeInvoke("typeof",
							"(Ljava/lang/Object;)Ljava/lang/String;");
				}

				return;
			}
		}

		this.cfw.addALoad(this.variableObjectLocal);
		this.cfw.addPush(node.getString());
		this.addScriptRuntimeInvoke("typeofName",
				"(Lorg/mozilla/javascript/Scriptable;Ljava/lang/String;)Ljava/lang/String;");
	}

	private void saveCurrentCodeOffset() {
		this.savedCodeOffset = this.cfw.getCurrentCodeOffset();
	}

	private void addInstructionCount() {
		int count = this.cfw.getCurrentCodeOffset() - this.savedCodeOffset;
		if (count != 0) {
			this.addInstructionCount(count);
		}
	}

	private void addInstructionCount(int count) {
		this.cfw.addALoad(this.contextLocal);
		this.cfw.addPush(count);
		this.addScriptRuntimeInvoke("addInstructionCount",
				"(Lorg/mozilla/javascript/Context;I)V");
	}

	private void visitIncDec(Node node) {
		int incrDecrMask = node.getExistingIntProp(13);
		Node child = node.getFirstChild();
		Node refChild;
		switch (child.getType()) {
			case 33 :
				refChild = child.getFirstChild();
				this.generateExpression(refChild, node);
				this.generateExpression(refChild.getNext(), node);
				this.cfw.addALoad(this.contextLocal);
				this.cfw.addPush(incrDecrMask);
				this.addScriptRuntimeInvoke(
						"propIncrDecr",
						"(Ljava/lang/Object;Ljava/lang/String;Lorg/mozilla/javascript/Context;I)Ljava/lang/Object;");
				break;
			case 34 :
				throw Kit.codeBug();
			case 36 :
				refChild = child.getFirstChild();
				this.generateExpression(refChild, node);
				this.generateExpression(refChild.getNext(), node);
				this.cfw.addALoad(this.contextLocal);
				this.cfw.addPush(incrDecrMask);
				if (refChild.getNext().getIntProp(8, -1) != -1) {
					this.addOptRuntimeInvoke("elemIncrDecr",
							"(Ljava/lang/Object;DLorg/mozilla/javascript/Context;I)Ljava/lang/Object;");
				} else {
					this.addScriptRuntimeInvoke(
							"elemIncrDecr",
							"(Ljava/lang/Object;Ljava/lang/Object;Lorg/mozilla/javascript/Context;I)Ljava/lang/Object;");
				}
				break;
			case 39 :
				this.cfw.addALoad(this.variableObjectLocal);
				this.cfw.addPush(child.getString());
				this.cfw.addALoad(this.contextLocal);
				this.cfw.addPush(incrDecrMask);
				this.addScriptRuntimeInvoke(
						"nameIncrDecr",
						"(Lorg/mozilla/javascript/Scriptable;Ljava/lang/String;Lorg/mozilla/javascript/Context;I)Ljava/lang/Object;");
				break;
			case 55 :
				if (!this.hasVarsInRegs) {
					Kit.codeBug();
				}

				int varIndex;
				short reg;
				boolean refChild1;
				if (node.getIntProp(8, -1) != -1) {
					refChild1 = (incrDecrMask & 2) != 0;
					varIndex = this.fnCurrent.getVarIndex(child);
					reg = this.varRegisters[varIndex];
					int offset = this.varIsDirectCallParameter(varIndex)
							? 1
							: 0;
					this.cfw.addDLoad(reg + offset);
					if (refChild1) {
						this.cfw.add(92);
					}

					this.cfw.addPush(1.0D);
					if ((incrDecrMask & 1) == 0) {
						this.cfw.add(99);
					} else {
						this.cfw.add(103);
					}

					if (!refChild1) {
						this.cfw.add(92);
					}

					this.cfw.addDStore(reg + offset);
				} else {
					refChild1 = (incrDecrMask & 2) != 0;
					varIndex = this.fnCurrent.getVarIndex(child);
					reg = this.varRegisters[varIndex];
					this.cfw.addALoad(reg);
					if (refChild1) {
						this.cfw.add(89);
					}

					this.addObjectToDouble();
					this.cfw.addPush(1.0D);
					if ((incrDecrMask & 1) == 0) {
						this.cfw.add(99);
					} else {
						this.cfw.add(103);
					}

					this.addDoubleWrap();
					if (!refChild1) {
						this.cfw.add(89);
					}

					this.cfw.addAStore(reg);
				}
				break;
			case 67 :
				refChild = child.getFirstChild();
				this.generateExpression(refChild, node);
				this.cfw.addALoad(this.contextLocal);
				this.cfw.addPush(incrDecrMask);
				this.addScriptRuntimeInvoke(
						"refIncrDecr",
						"(Lorg/mozilla/javascript/Ref;Lorg/mozilla/javascript/Context;I)Ljava/lang/Object;");
				break;
			default :
				Codegen.badTree();
		}

	}

	private static boolean isArithmeticNode(Node node) {
		int type = node.getType();
		return type == 22 || type == 25 || type == 24 || type == 23;
	}

	private void visitArithmetic(Node node, int opCode, Node child, Node parent) {
		int childNumberFlag = node.getIntProp(8, -1);
		if (childNumberFlag != -1) {
			this.generateExpression(child, node);
			this.generateExpression(child.getNext(), node);
			this.cfw.add(opCode);
		} else {
			boolean childOfArithmetic = isArithmeticNode(parent);
			this.generateExpression(child, node);
			if (!isArithmeticNode(child)) {
				this.addObjectToDouble();
			}

			this.generateExpression(child.getNext(), node);
			if (!isArithmeticNode(child.getNext())) {
				this.addObjectToDouble();
			}

			this.cfw.add(opCode);
			if (!childOfArithmetic) {
				this.addDoubleWrap();
			}
		}

	}

	private void visitBitOp(Node node, int type, Node child) {
		int childNumberFlag = node.getIntProp(8, -1);
		this.generateExpression(child, node);
		if (type == 20) {
			this.addScriptRuntimeInvoke("toUint32", "(Ljava/lang/Object;)J");
			this.generateExpression(child.getNext(), node);
			this.addScriptRuntimeInvoke("toInt32", "(Ljava/lang/Object;)I");
			this.cfw.addPush(31);
			this.cfw.add(126);
			this.cfw.add(125);
			this.cfw.add(138);
			this.addDoubleWrap();
		} else {
			if (childNumberFlag == -1) {
				this.addScriptRuntimeInvoke("toInt32", "(Ljava/lang/Object;)I");
				this.generateExpression(child.getNext(), node);
				this.addScriptRuntimeInvoke("toInt32", "(Ljava/lang/Object;)I");
			} else {
				this.addScriptRuntimeInvoke("toInt32", "(D)I");
				this.generateExpression(child.getNext(), node);
				this.addScriptRuntimeInvoke("toInt32", "(D)I");
			}

			switch (type) {
				case 9 :
					this.cfw.add(128);
					break;
				case 10 :
					this.cfw.add(130);
					break;
				case 11 :
					this.cfw.add(126);
					break;
				case 12 :
				case 13 :
				case 14 :
				case 15 :
				case 16 :
				case 17 :
				default :
					throw Codegen.badTree();
				case 18 :
					this.cfw.add(120);
					break;
				case 19 :
					this.cfw.add(122);
			}

			this.cfw.add(135);
			if (childNumberFlag == -1) {
				this.addDoubleWrap();
			}

		}
	}

	private int nodeIsDirectCallParameter(Node node) {
		if (node.getType() == 55 && this.inDirectCallFunction
				&& !this.itsForcedObjectParameters) {
			int varIndex = this.fnCurrent.getVarIndex(node);
			if (this.fnCurrent.isParameter(varIndex)) {
				return this.varRegisters[varIndex];
			}
		}

		return -1;
	}

	private boolean varIsDirectCallParameter(int varIndex) {
		return this.fnCurrent.isParameter(varIndex)
				&& this.inDirectCallFunction && !this.itsForcedObjectParameters;
	}

	private void genSimpleCompare(int type, int trueGOTO, int falseGOTO) {
		if (trueGOTO == -1) {
			throw Codegen.badTree();
		} else {
			switch (type) {
				case 14 :
					this.cfw.add(152);
					this.cfw.add(155, trueGOTO);
					break;
				case 15 :
					this.cfw.add(152);
					this.cfw.add(158, trueGOTO);
					break;
				case 16 :
					this.cfw.add(151);
					this.cfw.add(157, trueGOTO);
					break;
				case 17 :
					this.cfw.add(151);
					this.cfw.add(156, trueGOTO);
					break;
				default :
					throw Codegen.badTree();
			}

			if (falseGOTO != -1) {
				this.cfw.add(167, falseGOTO);
			}

		}
	}

	private void visitIfJumpRelOp(Node node, Node child, int trueGOTO,
			int falseGOTO) {
		if (trueGOTO != -1 && falseGOTO != -1) {
			int type = node.getType();
			Node rChild = child.getNext();
			if (type != 53 && type != 52) {
				int childNumberFlag = node.getIntProp(8, -1);
				int left_dcp_register = this.nodeIsDirectCallParameter(child);
				int right_dcp_register = this.nodeIsDirectCallParameter(rChild);
				if (childNumberFlag != -1) {
					if (childNumberFlag != 2) {
						this.generateExpression(child, node);
					} else if (left_dcp_register != -1) {
						this.dcpLoadAsNumber(left_dcp_register);
					} else {
						this.generateExpression(child, node);
						this.addObjectToDouble();
					}

					if (childNumberFlag != 1) {
						this.generateExpression(rChild, node);
					} else if (right_dcp_register != -1) {
						this.dcpLoadAsNumber(right_dcp_register);
					} else {
						this.generateExpression(rChild, node);
						this.addObjectToDouble();
					}

					this.genSimpleCompare(type, trueGOTO, falseGOTO);
				} else {
					if (left_dcp_register != -1 && right_dcp_register != -1) {
						short routine = this.cfw.getStackTop();
						int leftIsNotNumber = this.cfw.acquireLabel();
						this.cfw.addALoad(left_dcp_register);
						this.cfw.add(178, "java/lang/Void", "TYPE",
								"Ljava/lang/Class;");
						this.cfw.add(166, leftIsNotNumber);
						this.cfw.addDLoad(left_dcp_register + 1);
						this.dcpLoadAsNumber(right_dcp_register);
						this.genSimpleCompare(type, trueGOTO, falseGOTO);
						if (routine != this.cfw.getStackTop()) {
							throw Codegen.badTree();
						}

						this.cfw.markLabel(leftIsNotNumber);
						int rightIsNotNumber = this.cfw.acquireLabel();
						this.cfw.addALoad(right_dcp_register);
						this.cfw.add(178, "java/lang/Void", "TYPE",
								"Ljava/lang/Class;");
						this.cfw.add(166, rightIsNotNumber);
						this.cfw.addALoad(left_dcp_register);
						this.addObjectToDouble();
						this.cfw.addDLoad(right_dcp_register + 1);
						this.genSimpleCompare(type, trueGOTO, falseGOTO);
						if (routine != this.cfw.getStackTop()) {
							throw Codegen.badTree();
						}

						this.cfw.markLabel(rightIsNotNumber);
						this.cfw.addALoad(left_dcp_register);
						this.cfw.addALoad(right_dcp_register);
					} else {
						this.generateExpression(child, node);
						this.generateExpression(rChild, node);
					}

					if (type == 17 || type == 16) {
						this.cfw.add(95);
					}

					String routine1 = type != 14 && type != 16
							? "cmp_LE"
							: "cmp_LT";
					this.addScriptRuntimeInvoke(routine1,
							"(Ljava/lang/Object;Ljava/lang/Object;)Z");
					this.cfw.add(154, trueGOTO);
					this.cfw.add(167, falseGOTO);
				}

			} else {
				this.generateExpression(child, node);
				this.generateExpression(rChild, node);
				this.cfw.addALoad(this.contextLocal);
				this.addScriptRuntimeInvoke(type == 53 ? "instanceOf" : "in",
						"(Ljava/lang/Object;Ljava/lang/Object;Lorg/mozilla/javascript/Context;)Z");
				this.cfw.add(154, trueGOTO);
				this.cfw.add(167, falseGOTO);
			}
		} else {
			throw Codegen.badTree();
		}
	}

	private void visitIfJumpEqOp(Node node, Node child, int trueGOTO,
			int falseGOTO) {
		if (trueGOTO != -1 && falseGOTO != -1) {
			short stackInitial = this.cfw.getStackTop();
			int type = node.getType();
			Node rChild = child.getNext();
			int child_dcp_register;
			if (child.getType() != 42 && rChild.getType() != 42) {
				child_dcp_register = this.nodeIsDirectCallParameter(child);
				if (child_dcp_register != -1 && rChild.getType() == 148) {
					Node name1 = rChild.getFirstChild();
					if (name1.getType() == 40) {
						this.cfw.addALoad(child_dcp_register);
						this.cfw.add(178, "java/lang/Void", "TYPE",
								"Ljava/lang/Class;");
						int testCode = this.cfw.acquireLabel();
						this.cfw.add(166, testCode);
						this.cfw.addDLoad(child_dcp_register + 1);
						this.cfw.addPush(name1.getDouble());
						this.cfw.add(151);
						if (type == 12) {
							this.cfw.add(153, trueGOTO);
						} else {
							this.cfw.add(154, trueGOTO);
						}

						this.cfw.add(167, falseGOTO);
						this.cfw.markLabel(testCode);
					}
				}

				this.generateExpression(child, node);
				this.generateExpression(rChild, node);
				String name2;
				short testCode1;
				switch (type) {
					case 12 :
						name2 = "eq";
						testCode1 = 154;
						break;
					case 13 :
						name2 = "eq";
						testCode1 = 153;
						break;
					case 46 :
						name2 = "shallowEq";
						testCode1 = 154;
						break;
					case 47 :
						name2 = "shallowEq";
						testCode1 = 153;
						break;
					default :
						throw Codegen.badTree();
				}

				this.addScriptRuntimeInvoke(name2,
						"(Ljava/lang/Object;Ljava/lang/Object;)Z");
				this.cfw.add(testCode1, trueGOTO);
				this.cfw.add(167, falseGOTO);
			} else {
				if (child.getType() == 42) {
					child = rChild;
				}

				this.generateExpression(child, node);
				if (type != 46 && type != 47) {
					if (type != 12) {
						if (type != 13) {
							throw Codegen.badTree();
						}

						child_dcp_register = trueGOTO;
						trueGOTO = falseGOTO;
						falseGOTO = child_dcp_register;
					}

					this.cfw.add(89);
					child_dcp_register = this.cfw.acquireLabel();
					this.cfw.add(199, child_dcp_register);
					short name = this.cfw.getStackTop();
					this.cfw.add(87);
					this.cfw.add(167, trueGOTO);
					this.cfw.markLabel(child_dcp_register, name);
					Codegen.pushUndefined(this.cfw);
					this.cfw.add(165, trueGOTO);
				} else {
					child_dcp_register = type == 46 ? 198 : 199;
					this.cfw.add(child_dcp_register, trueGOTO);
				}

				this.cfw.add(167, falseGOTO);
			}

			if (stackInitial != this.cfw.getStackTop()) {
				throw Codegen.badTree();
			}
		} else {
			throw Codegen.badTree();
		}
	}

	private void visitSetName(Node node, Node child) {
		String name;
		for (name = node.getFirstChild().getString(); child != null; child = child
				.getNext()) {
			this.generateExpression(child, node);
		}

		this.cfw.addALoad(this.contextLocal);
		this.cfw.addALoad(this.variableObjectLocal);
		this.cfw.addPush(name);
		this.addScriptRuntimeInvoke(
				"setName",
				"(Lorg/mozilla/javascript/Scriptable;Ljava/lang/Object;Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;Ljava/lang/String;)Ljava/lang/Object;");
	}

	private void visitSetConst(Node node, Node child) {
		String name;
		for (name = node.getFirstChild().getString(); child != null; child = child
				.getNext()) {
			this.generateExpression(child, node);
		}

		this.cfw.addALoad(this.contextLocal);
		this.cfw.addPush(name);
		this.addScriptRuntimeInvoke(
				"setConst",
				"(Lorg/mozilla/javascript/Scriptable;Ljava/lang/Object;Lorg/mozilla/javascript/Context;Ljava/lang/String;)Ljava/lang/Object;");
	}

	private void visitGetVar(Node node) {
		if (!this.hasVarsInRegs) {
			Kit.codeBug();
		}

		int varIndex = this.fnCurrent.getVarIndex(node);
		short reg = this.varRegisters[varIndex];
		if (this.varIsDirectCallParameter(varIndex)) {
			if (node.getIntProp(8, -1) != -1) {
				this.dcpLoadAsNumber(reg);
			} else {
				this.dcpLoadAsObject(reg);
			}
		} else if (this.fnCurrent.isNumberVar(varIndex)) {
			this.cfw.addDLoad(reg);
		} else {
			this.cfw.addALoad(reg);
		}

	}

	private void visitSetVar(Node node, Node child, boolean needValue) {
		if (!this.hasVarsInRegs) {
			Kit.codeBug();
		}

		int varIndex = this.fnCurrent.getVarIndex(node);
		this.generateExpression(child.getNext(), node);
		boolean isNumber = node.getIntProp(8, -1) != -1;
		short reg = this.varRegisters[varIndex];
		boolean[] constDeclarations = this.fnCurrent.fnode
				.getParamAndVarConst();
		if (constDeclarations[varIndex]) {
			if (!needValue) {
				if (isNumber) {
					this.cfw.add(88);
				} else {
					this.cfw.add(87);
				}
			}
		} else if (this.varIsDirectCallParameter(varIndex)) {
			if (isNumber) {
				if (needValue) {
					this.cfw.add(92);
				}

				this.cfw.addALoad(reg);
				this.cfw.add(178, "java/lang/Void", "TYPE", "Ljava/lang/Class;");
				int isNumberVar = this.cfw.acquireLabel();
				int beyond = this.cfw.acquireLabel();
				this.cfw.add(165, isNumberVar);
				short stack = this.cfw.getStackTop();
				this.addDoubleWrap();
				this.cfw.addAStore(reg);
				this.cfw.add(167, beyond);
				this.cfw.markLabel(isNumberVar, stack);
				this.cfw.addDStore(reg + 1);
				this.cfw.markLabel(beyond);
			} else {
				if (needValue) {
					this.cfw.add(89);
				}

				this.cfw.addAStore(reg);
			}
		} else {
			boolean isNumberVar1 = this.fnCurrent.isNumberVar(varIndex);
			if (isNumber) {
				if (isNumberVar1) {
					this.cfw.addDStore(reg);
					if (needValue) {
						this.cfw.addDLoad(reg);
					}
				} else {
					if (needValue) {
						this.cfw.add(92);
					}

					this.addDoubleWrap();
					this.cfw.addAStore(reg);
				}
			} else {
				if (isNumberVar1) {
					Kit.codeBug();
				}

				this.cfw.addAStore(reg);
				if (needValue) {
					this.cfw.addALoad(reg);
				}
			}
		}

	}

	private void visitSetConstVar(Node node, Node child, boolean needValue) {
		if (!this.hasVarsInRegs) {
			Kit.codeBug();
		}

		int varIndex = this.fnCurrent.getVarIndex(node);
		this.generateExpression(child.getNext(), node);
		boolean isNumber = node.getIntProp(8, -1) != -1;
		short reg = this.varRegisters[varIndex];
		int beyond = this.cfw.acquireLabel();
		int noAssign = this.cfw.acquireLabel();
		short stack;
		if (isNumber) {
			this.cfw.addILoad(reg + 2);
			this.cfw.add(154, noAssign);
			stack = this.cfw.getStackTop();
			this.cfw.addPush(1);
			this.cfw.addIStore(reg + 2);
			this.cfw.addDStore(reg);
			if (needValue) {
				this.cfw.addDLoad(reg);
				this.cfw.markLabel(noAssign, stack);
			} else {
				this.cfw.add(167, beyond);
				this.cfw.markLabel(noAssign, stack);
				this.cfw.add(88);
			}
		} else {
			this.cfw.addILoad(reg + 1);
			this.cfw.add(154, noAssign);
			stack = this.cfw.getStackTop();
			this.cfw.addPush(1);
			this.cfw.addIStore(reg + 1);
			this.cfw.addAStore(reg);
			if (needValue) {
				this.cfw.addALoad(reg);
				this.cfw.markLabel(noAssign, stack);
			} else {
				this.cfw.add(167, beyond);
				this.cfw.markLabel(noAssign, stack);
				this.cfw.add(87);
			}
		}

		this.cfw.markLabel(beyond);
	}

	private void visitGetProp(Node node, Node child) {
		this.generateExpression(child, node);
		Node nameChild = child.getNext();
		this.generateExpression(nameChild, node);
		if (node.getType() == 34) {
			this.cfw.addALoad(this.contextLocal);
			this.addScriptRuntimeInvoke(
					"getObjectPropNoWarn",
					"(Ljava/lang/Object;Ljava/lang/String;Lorg/mozilla/javascript/Context;)Ljava/lang/Object;");
		} else {
			int childType = child.getType();
			if (childType == 43 && nameChild.getType() == 41) {
				this.cfw.addALoad(this.contextLocal);
				this.addScriptRuntimeInvoke(
						"getObjectProp",
						"(Lorg/mozilla/javascript/Scriptable;Ljava/lang/String;Lorg/mozilla/javascript/Context;)Ljava/lang/Object;");
			} else {
				this.cfw.addALoad(this.contextLocal);
				this.cfw.addALoad(this.variableObjectLocal);
				this.addScriptRuntimeInvoke(
						"getObjectProp",
						"(Ljava/lang/Object;Ljava/lang/String;Lorg/mozilla/javascript/Context;Lorg/mozilla/javascript/Scriptable;)Ljava/lang/Object;");
			}

		}
	}

	private void visitSetProp(int type, Node node, Node child) {
		Node objectChild = child;
		this.generateExpression(child, node);
		child = child.getNext();
		if (type == 138) {
			this.cfw.add(89);
		}

		Node nameChild = child;
		this.generateExpression(child, node);
		child = child.getNext();
		if (type == 138) {
			this.cfw.add(90);
			if (objectChild.getType() == 43 && nameChild.getType() == 41) {
				this.cfw.addALoad(this.contextLocal);
				this.addScriptRuntimeInvoke(
						"getObjectProp",
						"(Lorg/mozilla/javascript/Scriptable;Ljava/lang/String;Lorg/mozilla/javascript/Context;)Ljava/lang/Object;");
			} else {
				this.cfw.addALoad(this.contextLocal);
				this.addScriptRuntimeInvoke(
						"getObjectProp",
						"(Ljava/lang/Object;Ljava/lang/String;Lorg/mozilla/javascript/Context;)Ljava/lang/Object;");
			}
		}

		this.generateExpression(child, node);
		this.cfw.addALoad(this.contextLocal);
		this.addScriptRuntimeInvoke(
				"setObjectProp",
				"(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;Lorg/mozilla/javascript/Context;)Ljava/lang/Object;");
	}

	private void visitSetElem(int type, Node node, Node child) {
		this.generateExpression(child, node);
		child = child.getNext();
		if (type == 139) {
			this.cfw.add(89);
		}

		this.generateExpression(child, node);
		child = child.getNext();
		boolean indexIsNumber = node.getIntProp(8, -1) != -1;
		if (type == 139) {
			if (indexIsNumber) {
				this.cfw.add(93);
				this.cfw.addALoad(this.contextLocal);
				this.addOptRuntimeInvoke("getObjectIndex",
						"(Ljava/lang/Object;DLorg/mozilla/javascript/Context;)Ljava/lang/Object;");
			} else {
				this.cfw.add(90);
				this.cfw.addALoad(this.contextLocal);
				this.addScriptRuntimeInvoke(
						"getObjectElem",
						"(Ljava/lang/Object;Ljava/lang/Object;Lorg/mozilla/javascript/Context;)Ljava/lang/Object;");
			}
		}

		this.generateExpression(child, node);
		this.cfw.addALoad(this.contextLocal);
		if (indexIsNumber) {
			this.addScriptRuntimeInvoke(
					"setObjectIndex",
					"(Ljava/lang/Object;DLjava/lang/Object;Lorg/mozilla/javascript/Context;)Ljava/lang/Object;");
		} else {
			this.addScriptRuntimeInvoke(
					"setObjectElem",
					"(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Lorg/mozilla/javascript/Context;)Ljava/lang/Object;");
		}

	}

	private void visitDotQuery(Node node, Node child) {
		this.updateLineNumber(node);
		this.generateExpression(child, node);
		this.cfw.addALoad(this.variableObjectLocal);
		this.addScriptRuntimeInvoke(
				"enterDotQuery",
				"(Ljava/lang/Object;Lorg/mozilla/javascript/Scriptable;)Lorg/mozilla/javascript/Scriptable;");
		this.cfw.addAStore(this.variableObjectLocal);
		this.cfw.add(1);
		int queryLoopStart = this.cfw.acquireLabel();
		this.cfw.markLabel(queryLoopStart);
		this.cfw.add(87);
		this.generateExpression(child.getNext(), node);
		this.addScriptRuntimeInvoke("toBoolean", "(Ljava/lang/Object;)Z");
		this.cfw.addALoad(this.variableObjectLocal);
		this.addScriptRuntimeInvoke("updateDotQuery",
				"(ZLorg/mozilla/javascript/Scriptable;)Ljava/lang/Object;");
		this.cfw.add(89);
		this.cfw.add(198, queryLoopStart);
		this.cfw.addALoad(this.variableObjectLocal);
		this.addScriptRuntimeInvoke("leaveDotQuery",
				"(Lorg/mozilla/javascript/Scriptable;)Lorg/mozilla/javascript/Scriptable;");
		this.cfw.addAStore(this.variableObjectLocal);
	}

	private int getLocalBlockRegister(Node node) {
		Node localBlock = (Node) node.getProp(3);
		int localSlot = localBlock.getExistingIntProp(2);
		return localSlot;
	}

	private void dcpLoadAsNumber(int dcp_register) {
		this.cfw.addALoad(dcp_register);
		this.cfw.add(178, "java/lang/Void", "TYPE", "Ljava/lang/Class;");
		int isNumberLabel = this.cfw.acquireLabel();
		this.cfw.add(165, isNumberLabel);
		short stack = this.cfw.getStackTop();
		this.cfw.addALoad(dcp_register);
		this.addObjectToDouble();
		int beyond = this.cfw.acquireLabel();
		this.cfw.add(167, beyond);
		this.cfw.markLabel(isNumberLabel, stack);
		this.cfw.addDLoad(dcp_register + 1);
		this.cfw.markLabel(beyond);
	}

	private void dcpLoadAsObject(int dcp_register) {
		this.cfw.addALoad(dcp_register);
		this.cfw.add(178, "java/lang/Void", "TYPE", "Ljava/lang/Class;");
		int isNumberLabel = this.cfw.acquireLabel();
		this.cfw.add(165, isNumberLabel);
		short stack = this.cfw.getStackTop();
		this.cfw.addALoad(dcp_register);
		int beyond = this.cfw.acquireLabel();
		this.cfw.add(167, beyond);
		this.cfw.markLabel(isNumberLabel, stack);
		this.cfw.addDLoad(dcp_register + 1);
		this.addDoubleWrap();
		this.cfw.markLabel(beyond);
	}

	private void addGoto(Node target, int jumpcode) {
		int targetLabel = this.getTargetLabel(target);
		this.cfw.add(jumpcode, targetLabel);
	}

	private void addObjectToDouble() {
		this.addScriptRuntimeInvoke("toNumber", "(Ljava/lang/Object;)D");
	}

	private void addNewObjectArray(int size) {
		if (size == 0) {
			if (this.itsZeroArgArray >= 0) {
				this.cfw.addALoad(this.itsZeroArgArray);
			} else {
				this.cfw.add(178, "org/mozilla/javascript/ScriptRuntime",
						"emptyArgs", "[Ljava/lang/Object;");
			}
		} else {
			this.cfw.addPush(size);
			this.cfw.add(189, "java/lang/Object");
		}

	}

	private void addScriptRuntimeInvoke(String methodName,
			String methodSignature) {
		this.cfw.addInvoke(184, "org.mozilla.javascript.ScriptRuntime",
				methodName, methodSignature);
	}

	private void addOptRuntimeInvoke(String methodName, String methodSignature) {
		this.cfw.addInvoke(184, "org/mozilla/javascript/optimizer/OptRuntime",
				methodName, methodSignature);
	}

	private void addJumpedBooleanWrap(int trueLabel, int falseLabel) {
		this.cfw.markLabel(falseLabel);
		int skip = this.cfw.acquireLabel();
		this.cfw.add(178, "java/lang/Boolean", "FALSE", "Ljava/lang/Boolean;");
		this.cfw.add(167, skip);
		this.cfw.markLabel(trueLabel);
		this.cfw.add(178, "java/lang/Boolean", "TRUE", "Ljava/lang/Boolean;");
		this.cfw.markLabel(skip);
		this.cfw.adjustStackTop(-1);
	}

	private void addDoubleWrap() {
		this.addOptRuntimeInvoke("wrapDouble", "(D)Ljava/lang/Double;");
	}

	private short getNewWordPairLocal(boolean isConst) {
		short result = this.getConsecutiveSlots(2, isConst);
		if (result < 255) {
			this.locals[result] = 1;
			this.locals[result + 1] = 1;
			if (isConst) {
				this.locals[result + 2] = 1;
			}

			if (result != this.firstFreeLocal) {
				return result;
			}

			for (int i = this.firstFreeLocal + 2; i < 256; ++i) {
				if (this.locals[i] == 0) {
					this.firstFreeLocal = (short) i;
					if (this.localsMax < this.firstFreeLocal) {
						this.localsMax = this.firstFreeLocal;
					}

					return result;
				}
			}
		}

		throw Context.reportRuntimeError("Program too complex (out of locals)");
	}

	private short getNewWordLocal(boolean isConst) {
		short result = this.getConsecutiveSlots(1, isConst);
		if (result < 255) {
			this.locals[result] = 1;
			if (isConst) {
				this.locals[result + 1] = 1;
			}

			if (result != this.firstFreeLocal) {
				return result;
			}

			for (int i = this.firstFreeLocal + 2; i < 256; ++i) {
				if (this.locals[i] == 0) {
					this.firstFreeLocal = (short) i;
					if (this.localsMax < this.firstFreeLocal) {
						this.localsMax = this.firstFreeLocal;
					}

					return result;
				}
			}
		}

		throw Context.reportRuntimeError("Program too complex (out of locals)");
	}

	private short getNewWordLocal() {
		short result = this.firstFreeLocal;
		this.locals[result] = 1;

		for (int i = this.firstFreeLocal + 1; i < 256; ++i) {
			if (this.locals[i] == 0) {
				this.firstFreeLocal = (short) i;
				if (this.localsMax < this.firstFreeLocal) {
					this.localsMax = this.firstFreeLocal;
				}

				return result;
			}
		}

		throw Context.reportRuntimeError("Program too complex (out of locals)");
	}

	private short getConsecutiveSlots(int count, boolean isConst) {
		if (isConst) {
			++count;
		}

		short result;
		for (result = this.firstFreeLocal; result < 255; ++result) {
			int i;
			for (i = 0; i < count && this.locals[result + i] == 0; ++i) {
				;
			}

			if (i >= count) {
				break;
			}
		}

		return result;
	}

	private void incReferenceWordLocal(short local) {
		++this.locals[local];
	}

	private void decReferenceWordLocal(short local) {
		--this.locals[local];
	}

	private void releaseWordLocal(short local) {
		if (local < this.firstFreeLocal) {
			this.firstFreeLocal = local;
		}

		this.locals[local] = 0;
	}

	class FinallyReturnPoint {
		public List<Integer> jsrPoints = new ArrayList();
		public int tableLabel = 0;
	}
}