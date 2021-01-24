/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.ConstProperties;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.Evaluator;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.FunctionNode;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.InterpretedFunction;
import org.mozilla.javascript.InterpreterData;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.NativeContinuation;
import org.mozilla.javascript.NativeGenerator;
import org.mozilla.javascript.NativeIterator;
import org.mozilla.javascript.NativeWith;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.NodeTransformer;
import org.mozilla.javascript.ObjArray;
import org.mozilla.javascript.ObjToIntMap;
import org.mozilla.javascript.Ref;
import org.mozilla.javascript.RegExpProxy;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptOrFnNode;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.SecurityUtilities;
import org.mozilla.javascript.UintMap;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.UniqueTag;
import org.mozilla.javascript.Node.Jump;
import org.mozilla.javascript.ObjToIntMap.Iterator;
import org.mozilla.javascript.ScriptRuntime.NoSuchMethodShim;
import org.mozilla.javascript.debug.DebugFrame;

public class Interpreter implements Evaluator {
	private static final int Icode_DUP = -1;
	private static final int Icode_DUP2 = -2;
	private static final int Icode_SWAP = -3;
	private static final int Icode_POP = -4;
	private static final int Icode_POP_RESULT = -5;
	private static final int Icode_IFEQ_POP = -6;
	private static final int Icode_VAR_INC_DEC = -7;
	private static final int Icode_NAME_INC_DEC = -8;
	private static final int Icode_PROP_INC_DEC = -9;
	private static final int Icode_ELEM_INC_DEC = -10;
	private static final int Icode_REF_INC_DEC = -11;
	private static final int Icode_SCOPE_LOAD = -12;
	private static final int Icode_SCOPE_SAVE = -13;
	private static final int Icode_TYPEOFNAME = -14;
	private static final int Icode_NAME_AND_THIS = -15;
	private static final int Icode_PROP_AND_THIS = -16;
	private static final int Icode_ELEM_AND_THIS = -17;
	private static final int Icode_VALUE_AND_THIS = -18;
	private static final int Icode_CLOSURE_EXPR = -19;
	private static final int Icode_CLOSURE_STMT = -20;
	private static final int Icode_CALLSPECIAL = -21;
	private static final int Icode_RETUNDEF = -22;
	private static final int Icode_GOSUB = -23;
	private static final int Icode_STARTSUB = -24;
	private static final int Icode_RETSUB = -25;
	private static final int Icode_LINE = -26;
	private static final int Icode_SHORTNUMBER = -27;
	private static final int Icode_INTNUMBER = -28;
	private static final int Icode_LITERAL_NEW = -29;
	private static final int Icode_LITERAL_SET = -30;
	private static final int Icode_SPARE_ARRAYLIT = -31;
	private static final int Icode_REG_IND_C0 = -32;
	private static final int Icode_REG_IND_C1 = -33;
	private static final int Icode_REG_IND_C2 = -34;
	private static final int Icode_REG_IND_C3 = -35;
	private static final int Icode_REG_IND_C4 = -36;
	private static final int Icode_REG_IND_C5 = -37;
	private static final int Icode_REG_IND1 = -38;
	private static final int Icode_REG_IND2 = -39;
	private static final int Icode_REG_IND4 = -40;
	private static final int Icode_REG_STR_C0 = -41;
	private static final int Icode_REG_STR_C1 = -42;
	private static final int Icode_REG_STR_C2 = -43;
	private static final int Icode_REG_STR_C3 = -44;
	private static final int Icode_REG_STR1 = -45;
	private static final int Icode_REG_STR2 = -46;
	private static final int Icode_REG_STR4 = -47;
	private static final int Icode_GETVAR1 = -48;
	private static final int Icode_SETVAR1 = -49;
	private static final int Icode_UNDEF = -50;
	private static final int Icode_ZERO = -51;
	private static final int Icode_ONE = -52;
	private static final int Icode_ENTERDQ = -53;
	private static final int Icode_LEAVEDQ = -54;
	private static final int Icode_TAIL_CALL = -55;
	private static final int Icode_LOCAL_CLEAR = -56;
	private static final int Icode_LITERAL_GETTER = -57;
	private static final int Icode_LITERAL_SETTER = -58;
	private static final int Icode_SETCONST = -59;
	private static final int Icode_SETCONSTVAR = -60;
	private static final int Icode_SETCONSTVAR1 = -61;
	private static final int Icode_GENERATOR = -62;
	private static final int Icode_GENERATOR_END = -63;
	private static final int Icode_DEBUGGER = -64;
	private static final int MIN_ICODE = -64;
	private CompilerEnvirons compilerEnv;
	private boolean itsInFunctionFlag;
	private boolean itsInTryFlag;
	private InterpreterData itsData;
	private ScriptOrFnNode scriptOrFn;
	private int itsICodeTop;
	private int itsStackDepth;
	private int itsLineNumber;
	private int itsDoubleTableTop;
	private ObjToIntMap itsStrings = new ObjToIntMap(20);
	private int itsLocalTop;
	private static final int MIN_LABEL_TABLE_SIZE = 32;
	private static final int MIN_FIXUP_TABLE_SIZE = 40;
	private int[] itsLabelTable;
	private int itsLabelTableTop;
	private long[] itsFixupTable;
	private int itsFixupTableTop;
	private ObjArray itsLiteralIds = new ObjArray();
	private int itsExceptionTableTop;
	private static final int EXCEPTION_TRY_START_SLOT = 0;
	private static final int EXCEPTION_TRY_END_SLOT = 1;
	private static final int EXCEPTION_HANDLER_SLOT = 2;
	private static final int EXCEPTION_TYPE_SLOT = 3;
	private static final int EXCEPTION_LOCAL_SLOT = 4;
	private static final int EXCEPTION_SCOPE_SLOT = 5;
	private static final int EXCEPTION_SLOT_SIZE = 6;
	private static final int ECF_TAIL = 1;

	private static Interpreter.CallFrame captureFrameForGenerator(
			Interpreter.CallFrame frame) {
		frame.frozen = true;
		Interpreter.CallFrame result = frame.cloneFrozen();
		frame.frozen = false;
		result.parentFrame = null;
		result.frameIndex = 0;
		return result;
	}

	private static String bytecodeName(int bytecode) {
		if (!validBytecode(bytecode)) {
			throw new IllegalArgumentException(String.valueOf(bytecode));
		} else {
			return String.valueOf(bytecode);
		}
	}

	private static boolean validIcode(int icode) {
		return -64 <= icode && icode <= -1;
	}

	private static boolean validTokenCode(int token) {
		return 2 <= token && token <= 79;
	}

	private static boolean validBytecode(int bytecode) {
		return validIcode(bytecode) || validTokenCode(bytecode);
	}

	public Object compile(CompilerEnvirons compilerEnv, ScriptOrFnNode tree,
			String encodedSource, boolean returnFunction) {
		this.compilerEnv = compilerEnv;
		(new NodeTransformer()).transform((ScriptOrFnNode) tree);
		if (returnFunction) {
			tree = ((ScriptOrFnNode) tree).getFunctionNode(0);
		}

		this.scriptOrFn = (ScriptOrFnNode) tree;
		this.itsData = new InterpreterData(compilerEnv.getLanguageVersion(),
				this.scriptOrFn.getSourceName(), encodedSource);
		this.itsData.topLevel = true;
		if (returnFunction) {
			this.generateFunctionICode();
		} else {
			this.generateICodeFromTree(this.scriptOrFn);
		}

		return this.itsData;
	}

	public Script createScriptObject(Object bytecode,
			Object staticSecurityDomain) {
		if (bytecode != this.itsData) {
			Kit.codeBug();
		}

		return InterpretedFunction.createScript(this.itsData,
				staticSecurityDomain);
	}

	public void setEvalScriptFlag(Script script) {
		((InterpretedFunction) script).idata.evalScriptFlag = true;
	}

	public Function createFunctionObject(Context cx, Scriptable scope,
			Object bytecode, Object staticSecurityDomain) {
		if (bytecode != this.itsData) {
			Kit.codeBug();
		}

		return InterpretedFunction.createFunction(cx, scope, this.itsData,
				staticSecurityDomain);
	}

	private void generateFunctionICode() {
		this.itsInFunctionFlag = true;
		FunctionNode theFunction = (FunctionNode) this.scriptOrFn;
		this.itsData.itsFunctionType = theFunction.getFunctionType();
		this.itsData.itsNeedsActivation = theFunction.requiresActivation();
		this.itsData.itsName = theFunction.getFunctionName();
		if (!theFunction.getIgnoreDynamicScope()
				&& this.compilerEnv.isUseDynamicScope()) {
			this.itsData.useDynamicScope = true;
		}

		if (theFunction.isGenerator()) {
			this.addIcode(-62);
			this.addUint16(theFunction.getBaseLineno() & '￿');
		}

		this.generateICodeFromTree(theFunction.getLastChild());
	}

	private void generateICodeFromTree(Node tree) {
		this.generateNestedFunctions();
		this.generateRegExpLiterals();
		this.visitStatement(tree, 0);
		this.fixLabelGotos();
		if (this.itsData.itsFunctionType == 0) {
			this.addToken(64);
		}

		if (this.itsData.itsICode.length != this.itsICodeTop) {
			byte[] tmp = new byte[this.itsICodeTop];
			System.arraycopy(this.itsData.itsICode, 0, tmp, 0, this.itsICodeTop);
			this.itsData.itsICode = tmp;
		}

		if (this.itsStrings.size() == 0) {
			this.itsData.itsStringTable = null;
		} else {
			this.itsData.itsStringTable = new String[this.itsStrings.size()];
			Iterator tmp1 = this.itsStrings.newIterator();
			tmp1.start();

			while (!tmp1.done()) {
				String str = (String) tmp1.getKey();
				int index = tmp1.getValue();
				if (this.itsData.itsStringTable[index] != null) {
					Kit.codeBug();
				}

				this.itsData.itsStringTable[index] = str;
				tmp1.next();
			}
		}

		if (this.itsDoubleTableTop == 0) {
			this.itsData.itsDoubleTable = null;
		} else if (this.itsData.itsDoubleTable.length != this.itsDoubleTableTop) {
			double[] tmp2 = new double[this.itsDoubleTableTop];
			System.arraycopy(this.itsData.itsDoubleTable, 0, tmp2, 0,
					this.itsDoubleTableTop);
			this.itsData.itsDoubleTable = tmp2;
		}

		if (this.itsExceptionTableTop != 0
				&& this.itsData.itsExceptionTable.length != this.itsExceptionTableTop) {
			int[] tmp3 = new int[this.itsExceptionTableTop];
			System.arraycopy(this.itsData.itsExceptionTable, 0, tmp3, 0,
					this.itsExceptionTableTop);
			this.itsData.itsExceptionTable = tmp3;
		}

		this.itsData.itsMaxVars = this.scriptOrFn.getParamAndVarCount();
		this.itsData.itsMaxFrameArray = this.itsData.itsMaxVars
				+ this.itsData.itsMaxLocals + this.itsData.itsMaxStack;
		this.itsData.argNames = this.scriptOrFn.getParamAndVarNames();
		this.itsData.argIsConst = this.scriptOrFn.getParamAndVarConst();
		this.itsData.argCount = this.scriptOrFn.getParamCount();
		this.itsData.encodedSourceStart = this.scriptOrFn
				.getEncodedSourceStart();
		this.itsData.encodedSourceEnd = this.scriptOrFn.getEncodedSourceEnd();
		if (this.itsLiteralIds.size() != 0) {
			this.itsData.literalIds = this.itsLiteralIds.toArray();
		}

	}

	private void generateNestedFunctions() {
		int functionCount = this.scriptOrFn.getFunctionCount();
		if (functionCount != 0) {
			InterpreterData[] array = new InterpreterData[functionCount];

			for (int i = 0; i != functionCount; ++i) {
				FunctionNode def = this.scriptOrFn.getFunctionNode(i);
				Interpreter jsi = new Interpreter();
				jsi.compilerEnv = this.compilerEnv;
				jsi.scriptOrFn = def;
				jsi.itsData = new InterpreterData(this.itsData);
				jsi.generateFunctionICode();
				array[i] = jsi.itsData;
			}

			this.itsData.itsNestedFunctions = array;
		}
	}

	private void generateRegExpLiterals() {
		int N = this.scriptOrFn.getRegexpCount();
		if (N != 0) {
			Context cx = Context.getContext();
			RegExpProxy rep = ScriptRuntime.checkRegExpProxy(cx);
			Object[] array = new Object[N];

			for (int i = 0; i != N; ++i) {
				String string = this.scriptOrFn.getRegexpString(i);
				String flags = this.scriptOrFn.getRegexpFlags(i);
				array[i] = rep.compileRegExp(cx, string, flags);
			}

			this.itsData.itsRegExpLiterals = array;
		}
	}

	private void updateLineNumber(Node node) {
		int lineno = node.getLineno();
		if (lineno != this.itsLineNumber && lineno >= 0) {
			if (this.itsData.firstLinePC < 0) {
				this.itsData.firstLinePC = lineno;
			}

			this.itsLineNumber = lineno;
			this.addIcode(-26);
			this.addUint16(lineno & '￿');
		}

	}

	private RuntimeException badTree(Node node) {
		throw new RuntimeException(node.toString());
	}

	private void visitStatement(Node node, int initialStackDepth) {
		int type = node.getType();
		Node child = node.getFirstChild();
		int localIndex;
		Node localIndex1;
		Jump localIndex2;
		int scopeIndex1;
		label112 : switch (type) {
			case -62 :
				break;
			case 2 :
				this.visitExpression(child, 0);
				this.addToken(2);
				this.stackChange(-1);
				break;
			case 3 :
				this.addToken(3);
				break;
			case 4 :
				this.updateLineNumber(node);
				if (node.getIntProp(20, 0) != 0) {
					this.addIcode(-63);
					this.addUint16(this.itsLineNumber & '￿');
				} else if (child != null) {
					this.visitExpression(child, 1);
					this.addToken(4);
					this.stackChange(-1);
				} else {
					this.addIcode(-22);
				}
				break;
			case 5 :
				localIndex1 = ((Jump) node).target;
				this.addGoto(localIndex1, type);
				break;
			case 6 :
			case 7 :
				localIndex1 = ((Jump) node).target;
				this.visitExpression(child, 0);
				this.addGoto(localIndex1, type);
				this.stackChange(-1);
				break;
			case 50 :
				this.updateLineNumber(node);
				this.visitExpression(child, 0);
				this.addToken(50);
				this.addUint16(this.itsLineNumber & '￿');
				this.stackChange(-1);
				break;
			case 51 :
				this.updateLineNumber(node);
				this.addIndexOp(51, this.getLocalBlockRef(node));
				break;
			case 57 :
				localIndex = this.getLocalBlockRef(node);
				scopeIndex1 = node.getExistingIntProp(14);
				String name1 = child.getString();
				child = child.getNext();
				this.visitExpression(child, 0);
				this.addStringPrefix(name1);
				this.addIndexPrefix(localIndex);
				this.addToken(57);
				this.addUint8(scopeIndex1 != 0 ? 1 : 0);
				this.stackChange(-1);
				break;
			case 58 :
			case 59 :
			case 60 :
				this.visitExpression(child, 0);
				this.addIndexOp(type, this.getLocalBlockRef(node));
				this.stackChange(-1);
				break;
			case 64 :
				this.updateLineNumber(node);
				this.addToken(64);
				break;
			case 80 :
				localIndex2 = (Jump) node;
				scopeIndex1 = this.getLocalBlockRef(localIndex2);
				int name = this.allocLocal();
				this.addIndexOp(-13, name);
				int tryStart = this.itsICodeTop;
				boolean savedFlag = this.itsInTryFlag;

				for (this.itsInTryFlag = true; child != null; child = child
						.getNext()) {
					this.visitStatement(child, initialStackDepth);
				}

				this.itsInTryFlag = savedFlag;
				Node catchTarget = localIndex2.target;
				if (catchTarget != null) {
					int finallyTarget = this.itsLabelTable[this
							.getTargetLabel(catchTarget)];
					this.addExceptionHandler(tryStart, finallyTarget,
							finallyTarget, false, scopeIndex1, name);
				}

				Node finallyTarget1 = localIndex2.getFinally();
				if (finallyTarget1 != null) {
					int finallyStartPC = this.itsLabelTable[this
							.getTargetLabel(finallyTarget1)];
					this.addExceptionHandler(tryStart, finallyStartPC,
							finallyStartPC, true, scopeIndex1, name);
				}

				this.addIndexOp(-56, name);
				this.releaseLocal(name);
				break;
			case 108 :
				localIndex = node.getExistingIntProp(1);
				scopeIndex1 = this.scriptOrFn.getFunctionNode(localIndex)
						.getFunctionType();
				if (scopeIndex1 == 3) {
					this.addIndexOp(-20, localIndex);
				} else if (scopeIndex1 != 1) {
					throw Kit.codeBug();
				}

				if (!this.itsInFunctionFlag) {
					this.addIndexOp(-19, localIndex);
					this.stackChange(1);
					this.addIcode(-5);
					this.stackChange(-1);
				}
				break;
			case 113 :
				this.updateLineNumber(node);
				this.visitExpression(child, 0);

				for (localIndex2 = (Jump) child.getNext(); localIndex2 != null; localIndex2 = (Jump) localIndex2
						.getNext()) {
					if (localIndex2.getType() != 114) {
						throw this.badTree(localIndex2);
					}

					Node scopeIndex = localIndex2.getFirstChild();
					this.addIcode(-1);
					this.stackChange(1);
					this.visitExpression(scopeIndex, 0);
					this.addToken(46);
					this.stackChange(-1);
					this.addGoto(localIndex2.target, -6);
					this.stackChange(-1);
				}

				this.addIcode(-4);
				this.stackChange(-1);
				break;
			case 122 :
			case 127 :
			case 128 :
			case 129 :
			case 131 :
				this.updateLineNumber(node);
			case 135 :
				while (true) {
					if (child == null) {
						break label112;
					}

					this.visitStatement(child, initialStackDepth);
					child = child.getNext();
				}
			case 124 :
				this.stackChange(1);
				localIndex = this.getLocalBlockRef(node);
				this.addIndexOp(-24, localIndex);
				this.stackChange(-1);

				while (child != null) {
					this.visitStatement(child, initialStackDepth);
					child = child.getNext();
				}

				this.addIndexOp(-25, localIndex);
				break;
			case 130 :
				this.markTargetLabel(node);
				break;
			case 132 :
			case 133 :
				this.updateLineNumber(node);
				this.visitExpression(child, 0);
				this.addIcode(type == 132 ? -4 : -5);
				this.stackChange(-1);
				break;
			case 134 :
				localIndex1 = ((Jump) node).target;
				this.addGoto(localIndex1, -23);
				break;
			case 140 :
				localIndex = this.allocLocal();
				node.putIntProp(2, localIndex);
				this.updateLineNumber(node);

				while (child != null) {
					this.visitStatement(child, initialStackDepth);
					child = child.getNext();
				}

				this.addIndexOp(-56, localIndex);
				this.releaseLocal(localIndex);
				break;
			case 159 :
				this.addIcode(-64);
				break;
			default :
				throw this.badTree(node);
		}

		if (this.itsStackDepth != initialStackDepth) {
			throw Kit.codeBug();
		}
	}

	private void visitExpression(Node node, int contextFlags) {
		int type = node.getType();
		Node child = node.getFirstChild();
		int savedStackDepth = this.itsStackDepth;
		Node enterWith;
		Node with;
		int inum;
		int index;
		int arg9;
		String arg10;
		int arg12;
		switch (type) {
			case 8 :
				arg10 = child.getString();
				this.visitExpression(child, 0);
				child = child.getNext();
				this.visitExpression(child, 0);
				this.addStringOp(8, arg10);
				this.stackChange(-1);
				break;
			case 9 :
			case 10 :
			case 11 :
			case 12 :
			case 13 :
			case 14 :
			case 15 :
			case 16 :
			case 17 :
			case 18 :
			case 19 :
			case 20 :
			case 21 :
			case 22 :
			case 23 :
			case 24 :
			case 25 :
			case 31 :
			case 36 :
			case 46 :
			case 47 :
			case 52 :
			case 53 :
				this.visitExpression(child, 0);
				child = child.getNext();
				this.visitExpression(child, 0);
				this.addToken(type);
				this.stackChange(-1);
				break;
			case 26 :
			case 27 :
			case 28 :
			case 29 :
			case 32 :
			case 125 :
				this.visitExpression(child, 0);
				if (type == 125) {
					this.addIcode(-4);
					this.addIcode(-50);
				} else {
					this.addToken(type);
				}
				break;
			case 30 :
			case 38 :
			case 70 :
				if (type == 30) {
					this.visitExpression(child, 0);
				} else {
					this.generateCallFunAndThis(child);
				}

				for (arg9 = 0; (child = child.getNext()) != null; ++arg9) {
					this.visitExpression(child, 0);
				}

				arg12 = node.getIntProp(10, 0);
				if (arg12 != 0) {
					this.addIndexOp(-21, arg9);
					this.addUint8(arg12);
					this.addUint8(type == 30 ? 1 : 0);
					this.addUint16(this.itsLineNumber & '￿');
				} else {
					if (type == 38 && (contextFlags & 1) != 0
							&& !this.compilerEnv.isGenerateDebugInfo()
							&& !this.itsInTryFlag) {
						type = -55;
					}

					this.addIndexOp(type, arg9);
				}

				if (type == 30) {
					this.stackChange(-arg9);
				} else {
					this.stackChange(-1 - arg9);
				}

				if (arg9 > this.itsData.itsMaxCalleeArgs) {
					this.itsData.itsMaxCalleeArgs = arg9;
				}
				break;
			case 33 :
			case 34 :
				this.visitExpression(child, 0);
				child = child.getNext();
				this.addStringOp(type, child.getString());
				break;
			case 35 :
			case 138 :
				this.visitExpression(child, 0);
				child = child.getNext();
				arg10 = child.getString();
				child = child.getNext();
				if (type == 138) {
					this.addIcode(-1);
					this.stackChange(1);
					this.addStringOp(33, arg10);
					this.stackChange(-1);
				}

				this.visitExpression(child, 0);
				this.addStringOp(35, arg10);
				this.stackChange(-1);
				break;
			case 37 :
			case 139 :
				this.visitExpression(child, 0);
				child = child.getNext();
				this.visitExpression(child, 0);
				child = child.getNext();
				if (type == 139) {
					this.addIcode(-2);
					this.stackChange(2);
					this.addToken(36);
					this.stackChange(-1);
					this.stackChange(-1);
				}

				this.visitExpression(child, 0);
				this.addToken(37);
				this.stackChange(-2);
				break;
			case 39 :
			case 41 :
			case 49 :
				this.addStringOp(type, node.getString());
				this.stackChange(1);
				break;
			case 40 :
				double arg13 = node.getDouble();
				inum = (int) arg13;
				if ((double) inum == arg13) {
					if (inum == 0) {
						this.addIcode(-51);
						if (1.0D / arg13 < 0.0D) {
							this.addToken(29);
						}
					} else if (inum == 1) {
						this.addIcode(-52);
					} else if ((short) inum == inum) {
						this.addIcode(-27);
						this.addUint16(inum & '￿');
					} else {
						this.addIcode(-28);
						this.addInt(inum);
					}
				} else {
					index = this.getDoubleIndex(arg13);
					this.addIndexOp(40, index);
				}

				this.stackChange(1);
				break;
			case 42 :
			case 43 :
			case 44 :
			case 45 :
			case 63 :
				this.addToken(type);
				this.stackChange(1);
				break;
			case 48 :
				arg9 = node.getExistingIntProp(4);
				this.addIndexOp(48, arg9);
				this.stackChange(1);
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
			case 148 :
			case 149 :
			case 150 :
			case 151 :
			case 152 :
			case 153 :
			case 157 :
			default :
				throw this.badTree(node);
			case 54 :
				arg9 = this.getLocalBlockRef(node);
				this.addIndexOp(54, arg9);
				this.stackChange(1);
				break;
			case 55 :
				if (this.itsData.itsNeedsActivation) {
					Kit.codeBug();
				}

				arg9 = this.scriptOrFn.getIndexForNameNode(node);
				this.addVarOp(55, arg9);
				this.stackChange(1);
				break;
			case 56 :
				if (this.itsData.itsNeedsActivation) {
					Kit.codeBug();
				}

				arg9 = this.scriptOrFn.getIndexForNameNode(child);
				child = child.getNext();
				this.visitExpression(child, 0);
				this.addVarOp(56, arg9);
				break;
			case 61 :
			case 62 :
				this.addIndexOp(type, this.getLocalBlockRef(node));
				this.stackChange(1);
				break;
			case 65 :
			case 66 :
				this.visitLiteral(node, child);
				break;
			case 67 :
			case 69 :
				this.visitExpression(child, 0);
				this.addToken(type);
				break;
			case 68 :
			case 141 :
				this.visitExpression(child, 0);
				child = child.getNext();
				if (type == 141) {
					this.addIcode(-1);
					this.stackChange(1);
					this.addToken(67);
					this.stackChange(-1);
				}

				this.visitExpression(child, 0);
				this.addToken(68);
				this.stackChange(-1);
				break;
			case 71 :
				this.visitExpression(child, 0);
				this.addStringOp(type, (String) node.getProp(17));
				break;
			case 72 :
				if (child != null) {
					this.visitExpression(child, 0);
				} else {
					this.addIcode(-50);
					this.stackChange(1);
				}

				this.addToken(72);
				this.addUint16(node.getLineno() & '￿');
				break;
			case 73 :
			case 74 :
			case 75 :
				this.visitExpression(child, 0);
				this.addToken(type);
				break;
			case 76 :
			case 77 :
			case 78 :
			case 79 :
				arg9 = node.getIntProp(16, 0);
				arg12 = 0;

				do {
					this.visitExpression(child, 0);
					++arg12;
					child = child.getNext();
				} while (child != null);

				this.addIndexOp(type, arg9);
				this.stackChange(1 - arg12);
				break;
			case 88 :
				for (enterWith = node.getLastChild(); child != enterWith; child = child
						.getNext()) {
					this.visitExpression(child, 0);
					this.addIcode(-4);
					this.stackChange(-1);
				}

				this.visitExpression(child, contextFlags & 1);
				break;
			case 101 :
				enterWith = child.getNext();
				with = enterWith.getNext();
				this.visitExpression(child, 0);
				inum = this.itsICodeTop;
				this.addGotoOp(7);
				this.stackChange(-1);
				this.visitExpression(enterWith, contextFlags & 1);
				index = this.itsICodeTop;
				this.addGotoOp(5);
				this.resolveForwardGoto(inum);
				this.itsStackDepth = savedStackDepth;
				this.visitExpression(with, contextFlags & 1);
				this.resolveForwardGoto(index);
				break;
			case 103 :
			case 104 :
				this.visitExpression(child, 0);
				this.addIcode(-1);
				this.stackChange(1);
				arg9 = this.itsICodeTop;
				arg12 = type == 104 ? 7 : 6;
				this.addGotoOp(arg12);
				this.stackChange(-1);
				this.addIcode(-4);
				this.stackChange(-1);
				child = child.getNext();
				this.visitExpression(child, contextFlags & 1);
				this.resolveForwardGoto(arg9);
				break;
			case 105 :
			case 106 :
				this.visitIncDec(node, child);
				break;
			case 108 :
				arg9 = node.getExistingIntProp(1);
				FunctionNode arg11 = this.scriptOrFn.getFunctionNode(arg9);
				if (arg11.getFunctionType() != 2) {
					throw Kit.codeBug();
				}

				this.addIndexOp(-19, arg9);
				this.stackChange(1);
				break;
			case 136 :
				arg9 = -1;
				if (this.itsInFunctionFlag && !this.itsData.itsNeedsActivation) {
					arg9 = this.scriptOrFn.getIndexForNameNode(node);
				}

				if (arg9 == -1) {
					this.addStringOp(-14, node.getString());
					this.stackChange(1);
				} else {
					this.addVarOp(55, arg9);
					this.stackChange(1);
					this.addToken(32);
				}
				break;
			case 137 :
				this.stackChange(1);
				break;
			case 145 :
				this.updateLineNumber(node);
				this.visitExpression(child, 0);
				this.addIcode(-53);
				this.stackChange(-1);
				arg9 = this.itsICodeTop;
				this.visitExpression(child.getNext(), 0);
				this.addBackwardGoto(-54, arg9);
				break;
			case 154 :
				arg10 = child.getString();
				this.visitExpression(child, 0);
				child = child.getNext();
				this.visitExpression(child, 0);
				this.addStringOp(-59, arg10);
				this.stackChange(-1);
				break;
			case 155 :
				if (this.itsData.itsNeedsActivation) {
					Kit.codeBug();
				}

				arg9 = this.scriptOrFn.getIndexForNameNode(child);
				child = child.getNext();
				this.visitExpression(child, 0);
				this.addVarOp(155, arg9);
				break;
			case 156 :
				this.visitArrayComprehension(node, child, child.getNext());
				break;
			case 158 :
				enterWith = node.getFirstChild();
				with = enterWith.getNext();
				this.visitExpression(enterWith.getFirstChild(), 0);
				this.addToken(2);
				this.stackChange(-1);
				this.visitExpression(with.getFirstChild(), 0);
				this.addToken(3);
		}

		if (savedStackDepth + 1 != this.itsStackDepth) {
			Kit.codeBug();
		}

	}

	private void generateCallFunAndThis(Node left) {
		int type = left.getType();
		switch (type) {
			case 33 :
			case 36 :
				Node target1 = left.getFirstChild();
				this.visitExpression(target1, 0);
				Node id = target1.getNext();
				if (type == 33) {
					String property = id.getString();
					this.addStringOp(-16, property);
					this.stackChange(1);
				} else {
					this.visitExpression(id, 0);
					this.addIcode(-17);
				}
				break;
			case 39 :
				String target = left.getString();
				this.addStringOp(-15, target);
				this.stackChange(2);
				break;
			default :
				this.visitExpression(left, 0);
				this.addIcode(-18);
				this.stackChange(1);
		}

	}

	private void visitIncDec(Node node, Node child) {
		int incrDecrMask = node.getExistingIntProp(13);
		int childType = child.getType();
		Node ref;
		switch (childType) {
			case 33 :
				ref = child.getFirstChild();
				this.visitExpression(ref, 0);
				String index1 = ref.getNext().getString();
				this.addStringOp(-9, index1);
				this.addUint8(incrDecrMask);
				break;
			case 36 :
				ref = child.getFirstChild();
				this.visitExpression(ref, 0);
				Node index = ref.getNext();
				this.visitExpression(index, 0);
				this.addIcode(-10);
				this.addUint8(incrDecrMask);
				this.stackChange(-1);
				break;
			case 39 :
				String ref2 = child.getString();
				this.addStringOp(-8, ref2);
				this.addUint8(incrDecrMask);
				this.stackChange(1);
				break;
			case 55 :
				if (this.itsData.itsNeedsActivation) {
					Kit.codeBug();
				}

				int ref1 = this.scriptOrFn.getIndexForNameNode(child);
				this.addVarOp(-7, ref1);
				this.addUint8(incrDecrMask);
				this.stackChange(1);
				break;
			case 67 :
				ref = child.getFirstChild();
				this.visitExpression(ref, 0);
				this.addIcode(-11);
				this.addUint8(incrDecrMask);
				break;
			default :
				throw this.badTree(node);
		}

	}

	private void visitLiteral(Node node, Node child) {
		int type = node.getType();
		Object[] propertyIds = null;
		int count;
		if (type == 65) {
			count = 0;

			for (Node index = child; index != null; index = index.getNext()) {
				++count;
			}
		} else {
			if (type != 66) {
				throw this.badTree(node);
			}

			propertyIds = (Object[]) ((Object[]) node.getProp(12));
			count = propertyIds.length;
		}

		this.addIndexOp(-29, count);
		this.stackChange(2);

		int arg7;
		while (child != null) {
			arg7 = child.getType();
			if (arg7 == 150) {
				this.visitExpression(child.getFirstChild(), 0);
				this.addIcode(-57);
			} else if (arg7 == 151) {
				this.visitExpression(child.getFirstChild(), 0);
				this.addIcode(-58);
			} else {
				this.visitExpression(child, 0);
				this.addIcode(-30);
			}

			this.stackChange(-1);
			child = child.getNext();
		}

		if (type == 65) {
			int[] arg8 = (int[]) ((int[]) node.getProp(11));
			if (arg8 == null) {
				this.addToken(65);
			} else {
				int index1 = this.itsLiteralIds.size();
				this.itsLiteralIds.add(arg8);
				this.addIndexOp(-31, index1);
			}
		} else {
			arg7 = this.itsLiteralIds.size();
			this.itsLiteralIds.add(propertyIds);
			this.addIndexOp(66, arg7);
		}

		this.stackChange(-1);
	}

	private void visitArrayComprehension(Node node, Node initStmt, Node expr) {
		this.visitStatement(initStmt, this.itsStackDepth);
		this.visitExpression(expr, 0);
	}

	private int getLocalBlockRef(Node node) {
		Node localBlock = (Node) node.getProp(3);
		return localBlock.getExistingIntProp(2);
	}

	private int getTargetLabel(Node target) {
		int label = target.labelId();
		if (label != -1) {
			return label;
		} else {
			label = this.itsLabelTableTop;
			if (this.itsLabelTable == null
					|| label == this.itsLabelTable.length) {
				if (this.itsLabelTable == null) {
					this.itsLabelTable = new int[32];
				} else {
					int[] tmp = new int[this.itsLabelTable.length * 2];
					System.arraycopy(this.itsLabelTable, 0, tmp, 0, label);
					this.itsLabelTable = tmp;
				}
			}

			this.itsLabelTableTop = label + 1;
			this.itsLabelTable[label] = -1;
			target.labelId(label);
			return label;
		}
	}

	private void markTargetLabel(Node target) {
		int label = this.getTargetLabel(target);
		if (this.itsLabelTable[label] != -1) {
			Kit.codeBug();
		}

		this.itsLabelTable[label] = this.itsICodeTop;
	}

	private void addGoto(Node target, int gotoOp) {
		int label = this.getTargetLabel(target);
		if (label >= this.itsLabelTableTop) {
			Kit.codeBug();
		}

		int targetPC = this.itsLabelTable[label];
		if (targetPC != -1) {
			this.addBackwardGoto(gotoOp, targetPC);
		} else {
			int gotoPC = this.itsICodeTop;
			this.addGotoOp(gotoOp);
			int top = this.itsFixupTableTop;
			if (this.itsFixupTable == null || top == this.itsFixupTable.length) {
				if (this.itsFixupTable == null) {
					this.itsFixupTable = new long[40];
				} else {
					long[] tmp = new long[this.itsFixupTable.length * 2];
					System.arraycopy(this.itsFixupTable, 0, tmp, 0, top);
					this.itsFixupTable = tmp;
				}
			}

			this.itsFixupTableTop = top + 1;
			this.itsFixupTable[top] = (long) label << 32 | (long) gotoPC;
		}

	}

	private void fixLabelGotos() {
		for (int i = 0; i < this.itsFixupTableTop; ++i) {
			long fixup = this.itsFixupTable[i];
			int label = (int) (fixup >> 32);
			int jumpSource = (int) fixup;
			int pc = this.itsLabelTable[label];
			if (pc == -1) {
				throw Kit.codeBug();
			}

			this.resolveGoto(jumpSource, pc);
		}

		this.itsFixupTableTop = 0;
	}

	private void addBackwardGoto(int gotoOp, int jumpPC) {
		int fromPC = this.itsICodeTop;
		if (fromPC <= jumpPC) {
			throw Kit.codeBug();
		} else {
			this.addGotoOp(gotoOp);
			this.resolveGoto(fromPC, jumpPC);
		}
	}

	private void resolveForwardGoto(int fromPC) {
		if (this.itsICodeTop < fromPC + 3) {
			throw Kit.codeBug();
		} else {
			this.resolveGoto(fromPC, this.itsICodeTop);
		}
	}

	private void resolveGoto(int fromPC, int jumpPC) {
		int offset = jumpPC - fromPC;
		if (0 <= offset && offset <= 2) {
			throw Kit.codeBug();
		} else {
			int offsetSite = fromPC + 1;
			if (offset != (short) offset) {
				if (this.itsData.longJumps == null) {
					this.itsData.longJumps = new UintMap();
				}

				this.itsData.longJumps.put(offsetSite, jumpPC);
				offset = 0;
			}

			byte[] array = this.itsData.itsICode;
			array[offsetSite] = (byte) (offset >> 8);
			array[offsetSite + 1] = (byte) offset;
		}
	}

	private void addToken(int token) {
		if (!validTokenCode(token)) {
			throw Kit.codeBug();
		} else {
			this.addUint8(token);
		}
	}

	private void addIcode(int icode) {
		if (!validIcode(icode)) {
			throw Kit.codeBug();
		} else {
			this.addUint8(icode & 255);
		}
	}

	private void addUint8(int value) {
		if ((value & -256) != 0) {
			throw Kit.codeBug();
		} else {
			byte[] array = this.itsData.itsICode;
			int top = this.itsICodeTop;
			if (top == array.length) {
				array = this.increaseICodeCapacity(1);
			}

			array[top] = (byte) value;
			this.itsICodeTop = top + 1;
		}
	}

	private void addUint16(int value) {
		if ((value & -65536) != 0) {
			throw Kit.codeBug();
		} else {
			byte[] array = this.itsData.itsICode;
			int top = this.itsICodeTop;
			if (top + 2 > array.length) {
				array = this.increaseICodeCapacity(2);
			}

			array[top] = (byte) (value >>> 8);
			array[top + 1] = (byte) value;
			this.itsICodeTop = top + 2;
		}
	}

	private void addInt(int i) {
		byte[] array = this.itsData.itsICode;
		int top = this.itsICodeTop;
		if (top + 4 > array.length) {
			array = this.increaseICodeCapacity(4);
		}

		array[top] = (byte) (i >>> 24);
		array[top + 1] = (byte) (i >>> 16);
		array[top + 2] = (byte) (i >>> 8);
		array[top + 3] = (byte) i;
		this.itsICodeTop = top + 4;
	}

	private int getDoubleIndex(double num) {
		int index = this.itsDoubleTableTop;
		if (index == 0) {
			this.itsData.itsDoubleTable = new double[64];
		} else if (this.itsData.itsDoubleTable.length == index) {
			double[] na = new double[index * 2];
			System.arraycopy(this.itsData.itsDoubleTable, 0, na, 0, index);
			this.itsData.itsDoubleTable = na;
		}

		this.itsData.itsDoubleTable[index] = num;
		this.itsDoubleTableTop = index + 1;
		return index;
	}

	private void addGotoOp(int gotoOp) {
		byte[] array = this.itsData.itsICode;
		int top = this.itsICodeTop;
		if (top + 3 > array.length) {
			array = this.increaseICodeCapacity(3);
		}

		array[top] = (byte) gotoOp;
		this.itsICodeTop = top + 1 + 2;
	}

	private void addVarOp(int op, int varIndex) {
		switch (op) {
			case 55 :
			case 56 :
				if (varIndex < 128) {
					this.addIcode(op == 55 ? -48 : -49);
					this.addUint8(varIndex);
					return;
				}
			case -7 :
				this.addIndexOp(op, varIndex);
				return;
			case 155 :
				if (varIndex < 128) {
					this.addIcode(-61);
					this.addUint8(varIndex);
					return;
				}

				this.addIndexOp(-60, varIndex);
				return;
			default :
				throw Kit.codeBug();
		}
	}

	private void addStringOp(int op, String str) {
		this.addStringPrefix(str);
		if (validIcode(op)) {
			this.addIcode(op);
		} else {
			this.addToken(op);
		}

	}

	private void addIndexOp(int op, int index) {
		this.addIndexPrefix(index);
		if (validIcode(op)) {
			this.addIcode(op);
		} else {
			this.addToken(op);
		}

	}

	private void addStringPrefix(String str) {
		int index = this.itsStrings.get(str, -1);
		if (index == -1) {
			index = this.itsStrings.size();
			this.itsStrings.put(str, index);
		}

		if (index < 4) {
			this.addIcode(-41 - index);
		} else if (index <= 255) {
			this.addIcode(-45);
			this.addUint8(index);
		} else if (index <= '￿') {
			this.addIcode(-46);
			this.addUint16(index);
		} else {
			this.addIcode(-47);
			this.addInt(index);
		}

	}

	private void addIndexPrefix(int index) {
		if (index < 0) {
			Kit.codeBug();
		}

		if (index < 6) {
			this.addIcode(-32 - index);
		} else if (index <= 255) {
			this.addIcode(-38);
			this.addUint8(index);
		} else if (index <= '￿') {
			this.addIcode(-39);
			this.addUint16(index);
		} else {
			this.addIcode(-40);
			this.addInt(index);
		}

	}

	private void addExceptionHandler(int icodeStart, int icodeEnd,
			int handlerStart, boolean isFinally, int exceptionObjectLocal,
			int scopeLocal) {
		int top = this.itsExceptionTableTop;
		int[] table = this.itsData.itsExceptionTable;
		if (table == null) {
			if (top != 0) {
				Kit.codeBug();
			}

			table = new int[12];
			this.itsData.itsExceptionTable = table;
		} else if (table.length == top) {
			table = new int[table.length * 2];
			System.arraycopy(this.itsData.itsExceptionTable, 0, table, 0, top);
			this.itsData.itsExceptionTable = table;
		}

		table[top + 0] = icodeStart;
		table[top + 1] = icodeEnd;
		table[top + 2] = handlerStart;
		table[top + 3] = isFinally ? 1 : 0;
		table[top + 4] = exceptionObjectLocal;
		table[top + 5] = scopeLocal;
		this.itsExceptionTableTop = top + 6;
	}

	private byte[] increaseICodeCapacity(int extraSize) {
		int capacity = this.itsData.itsICode.length;
		int top = this.itsICodeTop;
		if (top + extraSize <= capacity) {
			throw Kit.codeBug();
		} else {
			capacity *= 2;
			if (top + extraSize > capacity) {
				capacity = top + extraSize;
			}

			byte[] array = new byte[capacity];
			System.arraycopy(this.itsData.itsICode, 0, array, 0, top);
			this.itsData.itsICode = array;
			return array;
		}
	}

	private void stackChange(int change) {
		if (change <= 0) {
			this.itsStackDepth += change;
		} else {
			int newDepth = this.itsStackDepth + change;
			if (newDepth > this.itsData.itsMaxStack) {
				this.itsData.itsMaxStack = newDepth;
			}

			this.itsStackDepth = newDepth;
		}

	}

	private int allocLocal() {
		int localSlot = this.itsLocalTop++;
		if (this.itsLocalTop > this.itsData.itsMaxLocals) {
			this.itsData.itsMaxLocals = this.itsLocalTop;
		}

		return localSlot;
	}

	private void releaseLocal(int localSlot) {
		--this.itsLocalTop;
		if (localSlot != this.itsLocalTop) {
			Kit.codeBug();
		}

	}

	private static int getShort(byte[] iCode, int pc) {
		return iCode[pc] << 8 | iCode[pc + 1] & 255;
	}

	private static int getIndex(byte[] iCode, int pc) {
		return (iCode[pc] & 255) << 8 | iCode[pc + 1] & 255;
	}

	private static int getInt(byte[] iCode, int pc) {
		return iCode[pc] << 24 | (iCode[pc + 1] & 255) << 16
				| (iCode[pc + 2] & 255) << 8 | iCode[pc + 3] & 255;
	}

	private static int getExceptionHandler(Interpreter.CallFrame frame,
			boolean onlyFinally) {
		int[] exceptionTable = frame.idata.itsExceptionTable;
		if (exceptionTable == null) {
			return -1;
		} else {
			int pc = frame.pc - 1;
			int best = -1;
			int bestStart = 0;
			int bestEnd = 0;

			for (int i = 0; i != exceptionTable.length; i += 6) {
				int start = exceptionTable[i + 0];
				int end = exceptionTable[i + 1];
				if (start <= pc && pc < end
						&& (!onlyFinally || exceptionTable[i + 3] == 1)) {
					if (best >= 0) {
						if (bestEnd < end) {
							continue;
						}

						if (bestStart > start) {
							Kit.codeBug();
						}

						if (bestEnd == end) {
							Kit.codeBug();
						}
					}

					best = i;
					bestStart = start;
					bestEnd = end;
				}
			}

			return best;
		}
	}

	private static void dumpICode(InterpreterData idata) {
	}

	private static int bytecodeSpan(int bytecode) {
		switch (bytecode) {
			case -63 :
			case -62 :
			case 50 :
			case 72 :
				return 3;
			case -61 :
			case -49 :
			case -48 :
				return 2;
			case -54 :
			case -23 :
			case -6 :
			case 5 :
			case 6 :
			case 7 :
				return 3;
			case -47 :
				return 5;
			case -46 :
				return 3;
			case -45 :
				return 2;
			case -40 :
				return 5;
			case -39 :
				return 3;
			case -38 :
				return 2;
			case -28 :
				return 5;
			case -27 :
				return 3;
			case -26 :
				return 3;
			case -21 :
				return 5;
			case -11 :
			case -10 :
			case -9 :
			case -8 :
			case -7 :
				return 2;
			case 57 :
				return 2;
			default :
				if (!validBytecode(bytecode)) {
					throw Kit.codeBug();
				} else {
					return 1;
				}
		}
	}

	static int[] getLineNumbers(InterpreterData data) {
		UintMap presentLines = new UintMap();
		byte[] iCode = data.itsICode;
		int iCodeLength = iCode.length;

		int span;
		for (int pc = 0; pc != iCodeLength; pc += span) {
			byte bytecode = iCode[pc];
			span = bytecodeSpan(bytecode);
			if (bytecode == -26) {
				if (span != 3) {
					Kit.codeBug();
				}

				int line = getIndex(iCode, pc + 1);
				presentLines.put(line, 0);
			}
		}

		return presentLines.getKeys();
	}

	public void captureStackInfo(RhinoException ex) {
		Context cx = Context.getCurrentContext();
		if (cx != null && cx.lastInterpreterFrame != null) {
			Interpreter.CallFrame[] array;
			int interpreterFrameCount;
			if (cx.previousInterpreterInvocations != null
					&& cx.previousInterpreterInvocations.size() != 0) {
				interpreterFrameCount = cx.previousInterpreterInvocations
						.size();
				if (cx.previousInterpreterInvocations.peek() == cx.lastInterpreterFrame) {
					--interpreterFrameCount;
				}

				array = new Interpreter.CallFrame[interpreterFrameCount + 1];
				cx.previousInterpreterInvocations.toArray(array);
			} else {
				array = new Interpreter.CallFrame[1];
			}

			array[array.length - 1] = (Interpreter.CallFrame) cx.lastInterpreterFrame;
			interpreterFrameCount = 0;

			for (int linePC = 0; linePC != array.length; ++linePC) {
				interpreterFrameCount += 1 + array[linePC].frameIndex;
			}

			int[] arg8 = new int[interpreterFrameCount];
			int linePCIndex = interpreterFrameCount;
			int i = array.length;

			while (i != 0) {
				--i;

				for (Interpreter.CallFrame frame = array[i]; frame != null; frame = frame.parentFrame) {
					--linePCIndex;
					arg8[linePCIndex] = frame.pcSourceLineStart;
				}
			}

			if (linePCIndex != 0) {
				Kit.codeBug();
			}

			ex.interpreterStackInfo = array;
			ex.interpreterLineData = arg8;
		} else {
			ex.interpreterStackInfo = null;
			ex.interpreterLineData = null;
		}
	}

	public String getSourcePositionFromStack(Context cx, int[] linep) {
		Interpreter.CallFrame frame = (Interpreter.CallFrame) cx.lastInterpreterFrame;
		InterpreterData idata = frame.idata;
		if (frame.pcSourceLineStart >= 0) {
			linep[0] = getIndex(idata.itsICode, frame.pcSourceLineStart);
		} else {
			linep[0] = 0;
		}

		return idata.itsSourceFile;
	}

	public String getPatchedStack(RhinoException ex, String nativeStackTrace) {
		String tag = "org.mozilla.javascript.Interpreter.interpretLoop";
		StringBuffer sb = new StringBuffer(nativeStackTrace.length() + 1000);
		String lineSeparator = SecurityUtilities
				.getSystemProperty("line.separator");
		Interpreter.CallFrame[] array = (Interpreter.CallFrame[]) ((Interpreter.CallFrame[]) ex.interpreterStackInfo);
		int[] linePC = ex.interpreterLineData;
		int arrayIndex = array.length;
		int linePCIndex = linePC.length;
		int offset = 0;

		while (arrayIndex != 0) {
			--arrayIndex;
			int pos = nativeStackTrace.indexOf(tag, offset);
			if (pos < 0) {
				break;
			}

			for (pos += tag.length(); pos != nativeStackTrace.length(); ++pos) {
				char frame = nativeStackTrace.charAt(pos);
				if (frame == 10 || frame == 13) {
					break;
				}
			}

			sb.append(nativeStackTrace.substring(offset, pos));
			offset = pos;

			for (Interpreter.CallFrame arg14 = array[arrayIndex]; arg14 != null; arg14 = arg14.parentFrame) {
				if (linePCIndex == 0) {
					Kit.codeBug();
				}

				--linePCIndex;
				InterpreterData idata = arg14.idata;
				sb.append(lineSeparator);
				sb.append("\tat script");
				if (idata.itsName != null && idata.itsName.length() != 0) {
					sb.append('.');
					sb.append(idata.itsName);
				}

				sb.append('(');
				sb.append(idata.itsSourceFile);
				int pc = linePC[linePCIndex];
				if (pc >= 0) {
					sb.append(':');
					sb.append(getIndex(idata.itsICode, pc));
				}

				sb.append(')');
			}
		}

		sb.append(nativeStackTrace.substring(offset));
		return sb.toString();
	}

	public List<String> getScriptStack(RhinoException ex) {
		if (ex.interpreterStackInfo == null) {
			return null;
		} else {
			ArrayList list = new ArrayList();
			String lineSeparator = SecurityUtilities
					.getSystemProperty("line.separator");
			Interpreter.CallFrame[] array = (Interpreter.CallFrame[]) ((Interpreter.CallFrame[]) ex.interpreterStackInfo);
			int[] linePC = ex.interpreterLineData;
			int arrayIndex = array.length;
			int linePCIndex = linePC.length;

			while (arrayIndex != 0) {
				--arrayIndex;
				StringBuilder sb = new StringBuilder();

				for (Interpreter.CallFrame frame = array[arrayIndex]; frame != null; frame = frame.parentFrame) {
					if (linePCIndex == 0) {
						Kit.codeBug();
					}

					--linePCIndex;
					InterpreterData idata = frame.idata;
					sb.append("\tat ");
					sb.append(idata.itsSourceFile);
					int pc = linePC[linePCIndex];
					if (pc >= 0) {
						sb.append(':');
						sb.append(getIndex(idata.itsICode, pc));
					}

					if (idata.itsName != null && idata.itsName.length() != 0) {
						sb.append(" (");
						sb.append(idata.itsName);
						sb.append(')');
					}

					sb.append(lineSeparator);
				}

				list.add(sb.toString());
			}

			return list;
		}
	}

	static String getEncodedSource(InterpreterData idata) {
		return idata.encodedSource == null ? null : idata.encodedSource
				.substring(idata.encodedSourceStart, idata.encodedSourceEnd);
	}

	private static void initFunction(Context cx, Scriptable scope,
			InterpretedFunction parent, int index) {
		InterpretedFunction fn = InterpretedFunction.createFunction(cx, scope,
				parent, index);
		ScriptRuntime.initFunction(cx, scope, fn, fn.idata.itsFunctionType,
				parent.idata.evalScriptFlag);
	}

	static Object interpret(InterpretedFunction ifun, Context cx,
			Scriptable scope, Scriptable thisObj, Object[] args) {
		if (!ScriptRuntime.hasTopCall(cx)) {
			Kit.codeBug();
		}

		if (cx.interpreterSecurityDomain != ifun.securityDomain) {
			Object frame1 = cx.interpreterSecurityDomain;
			cx.interpreterSecurityDomain = ifun.securityDomain;

			Object arg5;
			try {
				arg5 = ifun.securityController.callWithDomain(
						ifun.securityDomain, cx, ifun, scope, thisObj, args);
			} finally {
				cx.interpreterSecurityDomain = frame1;
			}

			return arg5;
		} else {
			Interpreter.CallFrame frame = new Interpreter.CallFrame();
			initFrame(cx, scope, thisObj, args, (double[]) null, 0,
					args.length, ifun, (Interpreter.CallFrame) null, frame);
			frame.isContinuationsTopFrame = cx.isContinuationsTopCall;
			cx.isContinuationsTopCall = false;
			return interpretLoop(cx, frame, (Object) null);
		}
	}

	public static Object resumeGenerator(Context cx, Scriptable scope,
			int operation, Object savedState, Object value) {
		Interpreter.CallFrame frame = (Interpreter.CallFrame) savedState;
		Interpreter.GeneratorState generatorState = new Interpreter.GeneratorState(
				operation, value);
		if (operation == 2) {
			try {
				return interpretLoop(cx, frame, generatorState);
			} catch (RuntimeException arg7) {
				if (arg7 != value) {
					throw arg7;
				} else {
					return Undefined.instance;
				}
			}
		} else {
			Object result = interpretLoop(cx, frame, generatorState);
			if (generatorState.returnedException != null) {
				throw generatorState.returnedException;
			} else {
				return result;
			}
		}
	}

	public static Object restartContinuation(NativeContinuation c, Context cx,
			Scriptable scope, Object[] args) {
		if (!ScriptRuntime.hasTopCall(cx)) {
			return ScriptRuntime.doTopCall(c, cx, scope, (Scriptable) null,
					args);
		} else {
			Object arg;
			if (args.length == 0) {
				arg = Undefined.instance;
			} else {
				arg = args[0];
			}

			Interpreter.CallFrame capturedFrame = (Interpreter.CallFrame) c
					.getImplementation();
			if (capturedFrame == null) {
				return arg;
			} else {
				Interpreter.ContinuationJump cjump = new Interpreter.ContinuationJump(
						c, (Interpreter.CallFrame) null);
				cjump.result = arg;
				return interpretLoop(cx, (Interpreter.CallFrame) null, cjump);
			}
		}
	}

	private static Object interpretLoop(Context cx,
			Interpreter.CallFrame frame, Object throwable) {
		UniqueTag DBL_MRK = UniqueTag.DOUBLE_MARK;
		Object undefined = Undefined.instance;
		boolean instructionCounting = cx.instructionThreshold != 0;
		boolean INVOCATION_COST = true;
		boolean EXCEPTION_COST = true;
		String stringReg = null;
		int indexReg = -1;
		if (cx.lastInterpreterFrame != null) {
			if (cx.previousInterpreterInvocations == null) {
				cx.previousInterpreterInvocations = new ObjArray();
			}

			cx.previousInterpreterInvocations.push(cx.lastInterpreterFrame);
		}

		Interpreter.GeneratorState generatorState = null;
		if (throwable != null) {
			if (throwable instanceof Interpreter.GeneratorState) {
				generatorState = (Interpreter.GeneratorState) throwable;
				enterFrame(cx, frame, ScriptRuntime.emptyArgs, true);
				throwable = null;
			} else if (!(throwable instanceof Interpreter.ContinuationJump)) {
				Kit.codeBug();
			}
		}

		Object interpreterResult = null;
		double interpreterResultDbl = 0.0D;

		while (true) {
			while (true) {
				label869 : while (true) {
					while (true) {
						while (true) {
							label862 : while (true) {
								try {
									if (throwable != null) {
										frame = processThrowable(cx, throwable,
												frame, indexReg,
												instructionCounting);
										throwable = frame.throwable;
										frame.throwable = null;
									} else if (generatorState == null
											&& frame.frozen) {
										Kit.codeBug();
									}

									Object[] EX_CATCH_STATE = frame.stack;
									double[] EX_FINALLY_STATE = frame.sDbl;
									Object[] EX_NO_JS_STATE = frame.varSource.stack;
									double[] exState = frame.varSource.sDbl;
									int[] cjump = frame.varSource.stackAttributes;
									byte[] onlyFinally = frame.idata.itsICode;
									String[] ex = frame.idata.itsStringTable;
									int stackTop = frame.savedStackTop;
									cx.lastInterpreterFrame = frame;

									label858 : while (true) {
										while (true) {
											while (true) {
												label851 : while (true) {
													short op = onlyFinally[frame.pc++];
													Object offset;
													Object x;
													Object val;
													Object skipIndexces;
													double d2;
													Scriptable arg40;
													double arg41;
													boolean arg44;
													int arg45;
													Callable arg48;
													Ref arg51;
													Object[] arg52;
													double arg55;
													int arg62;
													boolean arg64;
													Scriptable arg68;
													double arg75;
													int arg76;
													switch (op) {
														case -64 :
															if (frame.debuggerFrame != null) {
																frame.debuggerFrame
																		.onDebuggerStatement(cx);
															}
															continue;
														case -63 :
															frame.frozen = true;
															arg76 = getIndex(
																	onlyFinally,
																	frame.pc);
															generatorState.returnedException = new JavaScriptException(
																	NativeIterator
																			.getStopIterationObject(frame.scope),
																	frame.idata.itsSourceFile,
																	arg76);
															break label851;
														case -62 :
															if (!frame.frozen) {
																--frame.pc;
																Interpreter.CallFrame arg80 = captureFrameForGenerator(frame);
																arg80.frozen = true;
																NativeGenerator arg74 = new NativeGenerator(
																		frame.scope,
																		arg80.fnOrScript,
																		arg80);
																frame.result = arg74;
																break label851;
															}
														case 72 :
															if (!frame.frozen) {
																return freezeGenerator(
																		cx,
																		frame,
																		stackTop,
																		generatorState);
															}

															offset = thawGenerator(
																	frame,
																	stackTop,
																	generatorState,
																	op);
															if (offset != Scriptable.NOT_FOUND) {
																throwable = offset;
																break label858;
															}
															continue;
														case -61 :
															indexReg = onlyFinally[frame.pc++];
														case 155 :
															if (!frame.useActivation) {
																if ((cjump[indexReg] & 1) == 0) {
																	throw Context
																			.reportRuntimeError1(
																					"msg.var.redecl",
																					frame.idata.argNames[indexReg]);
																}

																if ((cjump[indexReg] & 8) != 0) {
																	EX_NO_JS_STATE[indexReg] = EX_CATCH_STATE[stackTop];
																	cjump[indexReg] &= -9;
																	exState[indexReg] = EX_FINALLY_STATE[stackTop];
																}
															} else {
																offset = EX_CATCH_STATE[stackTop];
																if (offset == DBL_MRK) {
																	offset = ScriptRuntime
																			.wrapNumber(EX_FINALLY_STATE[stackTop]);
																}

																stringReg = frame.idata.argNames[indexReg];
																if (!(frame.scope instanceof ConstProperties)) {
																	throw Kit
																			.codeBug();
																}

																ConstProperties arg73 = (ConstProperties) frame.scope;
																arg73.putConst(
																		stringReg,
																		frame.scope,
																		offset);
															}
															continue;
														case -60 :
														case 0 :
														case 1 :
														case 80 :
														case 81 :
														case 82 :
														case 83 :
														case 84 :
														case 85 :
														case 86 :
														case 87 :
														case 88 :
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
														case 101 :
														case 102 :
														case 103 :
														case 104 :
														case 105 :
														case 106 :
														case 107 :
														case 108 :
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
														case 125 :
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
														case 136 :
														case 137 :
														case 138 :
														case 139 :
														case 140 :
														case 141 :
														case 142 :
														case 143 :
														case 144 :
														case 145 :
														case 146 :
														case 147 :
														case 148 :
														case 149 :
														case 150 :
														case 151 :
														case 152 :
														case 153 :
														case 154 :
														default :
															dumpICode(frame.idata);
															throw new RuntimeException(
																	"Unknown icode : "
																			+ op
																			+ " @ pc : "
																			+ (frame.pc - 1));
														case -59 :
															offset = EX_CATCH_STATE[stackTop];
															if (offset == DBL_MRK) {
																offset = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop]);
															}

															--stackTop;
															arg68 = (Scriptable) EX_CATCH_STATE[stackTop];
															EX_CATCH_STATE[stackTop] = ScriptRuntime
																	.setConst(
																			arg68,
																			offset,
																			cx,
																			stringReg);
															continue;
														case -58 :
															offset = EX_CATCH_STATE[stackTop];
															--stackTop;
															arg45 = (int) EX_FINALLY_STATE[stackTop];
															((Object[]) ((Object[]) EX_CATCH_STATE[stackTop]))[arg45] = offset;
															((int[]) ((int[]) EX_CATCH_STATE[stackTop - 1]))[arg45] = 1;
															EX_FINALLY_STATE[stackTop] = (double) (arg45 + 1);
															continue;
														case -57 :
															offset = EX_CATCH_STATE[stackTop];
															--stackTop;
															arg45 = (int) EX_FINALLY_STATE[stackTop];
															((Object[]) ((Object[]) EX_CATCH_STATE[stackTop]))[arg45] = offset;
															((int[]) ((int[]) EX_CATCH_STATE[stackTop - 1]))[arg45] = -1;
															EX_FINALLY_STATE[stackTop] = (double) (arg45 + 1);
															continue;
														case -56 :
															indexReg += frame.localShift;
															EX_CATCH_STATE[indexReg] = null;
															continue;
														case -55 :
														case 38 :
														case 70 :
															if (instructionCounting) {
																cx.instructionCount += 100;
															}

															stackTop -= 1 + indexReg;
															Callable arg79 = (Callable) EX_CATCH_STATE[stackTop];
															arg68 = (Scriptable) EX_CATCH_STATE[stackTop + 1];
															if (op == 70) {
																arg52 = getArgsArray(
																		EX_CATCH_STATE,
																		EX_FINALLY_STATE,
																		stackTop + 2,
																		indexReg);
																EX_CATCH_STATE[stackTop] = ScriptRuntime
																		.callRef(
																				arg79,
																				arg68,
																				arg52,
																				cx);
																continue;
															}

															arg40 = frame.scope;
															if (frame.useActivation) {
																arg40 = ScriptableObject
																		.getTopLevelScope(frame.scope);
															}

															if (arg79 instanceof InterpretedFunction) {
																InterpretedFunction arg60 = (InterpretedFunction) arg79;
																if (frame.fnOrScript.securityDomain == arg60.securityDomain) {
																	Interpreter.CallFrame arg49 = frame;
																	Interpreter.CallFrame arg59 = new Interpreter.CallFrame();
																	if (op == -55) {
																		arg49 = frame.parentFrame;
																		exitFrame(
																				cx,
																				frame,
																				(Object) null);
																	}

																	initFrame(
																			cx,
																			arg40,
																			arg68,
																			EX_CATCH_STATE,
																			EX_FINALLY_STATE,
																			stackTop + 2,
																			indexReg,
																			arg60,
																			arg49,
																			arg59);
																	if (op != -55) {
																		frame.savedStackTop = stackTop;
																		frame.savedCallOp = op;
																	}

																	frame = arg59;
																	continue label862;
																}
															}

															if (arg79 instanceof NativeContinuation) {
																Interpreter.ContinuationJump arg65 = new Interpreter.ContinuationJump(
																		(NativeContinuation) arg79,
																		frame);
																if (indexReg == 0) {
																	arg65.result = undefined;
																} else {
																	arg65.result = EX_CATCH_STATE[stackTop + 2];
																	arg65.resultDbl = EX_FINALLY_STATE[stackTop + 2];
																}

																throwable = arg65;
																break label858;
															}

															InterpretedFunction arg57;
															if (arg79 instanceof IdFunctionObject) {
																IdFunctionObject arg61 = (IdFunctionObject) arg79;
																if (NativeContinuation
																		.isContinuationConstructor(arg61)) {
																	frame.stack[stackTop] = captureContinuation(
																			cx,
																			frame.parentFrame,
																			false);
																	continue;
																}

																if (BaseFunction
																		.isApplyOrCall(arg61)) {
																	arg48 = ScriptRuntime
																			.getCallable(arg68);
																	if (arg48 instanceof InterpretedFunction) {
																		arg57 = (InterpretedFunction) arg48;
																		if (frame.fnOrScript.securityDomain == arg57.securityDomain) {
																			frame = initFrameForApplyOrCall(
																					cx,
																					frame,
																					indexReg,
																					EX_CATCH_STATE,
																					EX_FINALLY_STATE,
																					stackTop,
																					op,
																					arg40,
																					arg61,
																					arg57);
																			continue label862;
																		}
																	}
																}
															}

															if (arg79 instanceof NoSuchMethodShim) {
																NoSuchMethodShim arg63 = (NoSuchMethodShim) arg79;
																arg48 = arg63.noSuchMethodMethod;
																if (arg48 instanceof InterpretedFunction) {
																	arg57 = (InterpretedFunction) arg48;
																	if (frame.fnOrScript.securityDomain == arg57.securityDomain) {
																		frame = initFrameForNoSuchMethod(
																				cx,
																				frame,
																				indexReg,
																				EX_CATCH_STATE,
																				EX_FINALLY_STATE,
																				stackTop,
																				op,
																				arg68,
																				arg40,
																				arg63,
																				arg57);
																		continue label862;
																	}
																}
															}

															cx.lastInterpreterFrame = frame;
															frame.savedCallOp = op;
															frame.savedStackTop = stackTop;
															EX_CATCH_STATE[stackTop] = arg79
																	.call(cx,
																			arg40,
																			arg68,
																			getArgsArray(
																					EX_CATCH_STATE,
																					EX_FINALLY_STATE,
																					stackTop + 2,
																					indexReg));
															cx.lastInterpreterFrame = null;
															continue;
														case -54 :
															arg64 = stack_boolean(
																	frame,
																	stackTop);
															x = ScriptRuntime
																	.updateDotQuery(
																			arg64,
																			frame.scope);
															if (x != null) {
																EX_CATCH_STATE[stackTop] = x;
																frame.scope = ScriptRuntime
																		.leaveDotQuery(frame.scope);
																frame.pc += 2;
																continue;
															}

															--stackTop;
															break;
														case -53 :
															offset = EX_CATCH_STATE[stackTop];
															if (offset == DBL_MRK) {
																offset = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop]);
															}

															--stackTop;
															frame.scope = ScriptRuntime
																	.enterDotQuery(
																			offset,
																			frame.scope);
															continue;
														case -52 :
															++stackTop;
															EX_CATCH_STATE[stackTop] = DBL_MRK;
															EX_FINALLY_STATE[stackTop] = 1.0D;
															continue;
														case -51 :
															++stackTop;
															EX_CATCH_STATE[stackTop] = DBL_MRK;
															EX_FINALLY_STATE[stackTop] = 0.0D;
															continue;
														case -50 :
															++stackTop;
															EX_CATCH_STATE[stackTop] = undefined;
															continue;
														case -49 :
															indexReg = onlyFinally[frame.pc++];
														case 56 :
															if (!frame.useActivation) {
																if ((cjump[indexReg] & 1) == 0) {
																	EX_NO_JS_STATE[indexReg] = EX_CATCH_STATE[stackTop];
																	exState[indexReg] = EX_FINALLY_STATE[stackTop];
																}
															} else {
																offset = EX_CATCH_STATE[stackTop];
																if (offset == DBL_MRK) {
																	offset = ScriptRuntime
																			.wrapNumber(EX_FINALLY_STATE[stackTop]);
																}

																stringReg = frame.idata.argNames[indexReg];
																frame.scope
																		.put(stringReg,
																				frame.scope,
																				offset);
															}
															continue;
														case -48 :
															indexReg = onlyFinally[frame.pc++];
														case 55 :
															++stackTop;
															if (!frame.useActivation) {
																EX_CATCH_STATE[stackTop] = EX_NO_JS_STATE[indexReg];
																EX_FINALLY_STATE[stackTop] = exState[indexReg];
															} else {
																stringReg = frame.idata.argNames[indexReg];
																EX_CATCH_STATE[stackTop] = frame.scope
																		.get(stringReg,
																				frame.scope);
															}
															continue;
														case -47 :
															stringReg = ex[getInt(
																	onlyFinally,
																	frame.pc)];
															frame.pc += 4;
															continue;
														case -46 :
															stringReg = ex[getIndex(
																	onlyFinally,
																	frame.pc)];
															frame.pc += 2;
															continue;
														case -45 :
															stringReg = ex[255 & onlyFinally[frame.pc]];
															++frame.pc;
															continue;
														case -44 :
															stringReg = ex[3];
															continue;
														case -43 :
															stringReg = ex[2];
															continue;
														case -42 :
															stringReg = ex[1];
															continue;
														case -41 :
															stringReg = ex[0];
															continue;
														case -40 :
															indexReg = getInt(
																	onlyFinally,
																	frame.pc);
															frame.pc += 4;
															continue;
														case -39 :
															indexReg = getIndex(
																	onlyFinally,
																	frame.pc);
															frame.pc += 2;
															continue;
														case -38 :
															indexReg = 255 & onlyFinally[frame.pc];
															++frame.pc;
															continue;
														case -37 :
															indexReg = 5;
															continue;
														case -36 :
															indexReg = 4;
															continue;
														case -35 :
															indexReg = 3;
															continue;
														case -34 :
															indexReg = 2;
															continue;
														case -33 :
															indexReg = 1;
															continue;
														case -32 :
															indexReg = 0;
															continue;
														case -31 :
														case 65 :
														case 66 :
															Object[] arg78 = (Object[]) ((Object[]) EX_CATCH_STATE[stackTop]);
															--stackTop;
															int[] arg72 = (int[]) ((int[]) EX_CATCH_STATE[stackTop]);
															if (op == 66) {
																Object[] arg56 = (Object[]) ((Object[]) frame.idata.literalIds[indexReg]);
																arg40 = ScriptRuntime
																		.newObjectLiteral(
																				arg56,
																				arg78,
																				arg72,
																				cx,
																				frame.scope);
															} else {
																int[] arg58 = null;
																if (op == -31) {
																	arg58 = (int[]) ((int[]) frame.idata.literalIds[indexReg]);
																}

																arg40 = ScriptRuntime
																		.newArrayLiteral(
																				arg78,
																				arg58,
																				cx,
																				frame.scope);
															}

															EX_CATCH_STATE[stackTop] = arg40;
															continue;
														case -30 :
															offset = EX_CATCH_STATE[stackTop];
															if (offset == DBL_MRK) {
																offset = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop]);
															}

															--stackTop;
															arg45 = (int) EX_FINALLY_STATE[stackTop];
															((Object[]) ((Object[]) EX_CATCH_STATE[stackTop]))[arg45] = offset;
															EX_FINALLY_STATE[stackTop] = (double) (arg45 + 1);
															continue;
														case -29 :
															++stackTop;
															EX_CATCH_STATE[stackTop] = new int[indexReg];
															++stackTop;
															EX_CATCH_STATE[stackTop] = new Object[indexReg];
															EX_FINALLY_STATE[stackTop] = 0.0D;
															continue;
														case -28 :
															++stackTop;
															EX_CATCH_STATE[stackTop] = DBL_MRK;
															EX_FINALLY_STATE[stackTop] = (double) getInt(
																	onlyFinally,
																	frame.pc);
															frame.pc += 4;
															continue;
														case -27 :
															++stackTop;
															EX_CATCH_STATE[stackTop] = DBL_MRK;
															EX_FINALLY_STATE[stackTop] = (double) getShort(
																	onlyFinally,
																	frame.pc);
															frame.pc += 2;
															continue;
														case -26 :
															frame.pcSourceLineStart = frame.pc;
															if (frame.debuggerFrame != null) {
																arg76 = getIndex(
																		onlyFinally,
																		frame.pc);
																frame.debuggerFrame
																		.onLineChange(
																				cx,
																				arg76);
															}

															frame.pc += 2;
															continue;
														case -25 :
															if (instructionCounting) {
																addInstructionCount(
																		cx,
																		frame,
																		0);
															}

															indexReg += frame.localShift;
															offset = EX_CATCH_STATE[indexReg];
															if (offset != DBL_MRK) {
																throwable = offset;
																break label858;
															}

															frame.pc = (int) EX_FINALLY_STATE[indexReg];
															if (instructionCounting) {
																frame.pcPrevBranch = frame.pc;
															}
															continue;
														case -24 :
															if (stackTop == frame.emptyStackTop + 1) {
																indexReg += frame.localShift;
																EX_CATCH_STATE[indexReg] = EX_CATCH_STATE[stackTop];
																EX_FINALLY_STATE[indexReg] = EX_FINALLY_STATE[stackTop];
																--stackTop;
															} else if (stackTop != frame.emptyStackTop) {
																Kit.codeBug();
															}
															continue;
														case -23 :
															++stackTop;
															EX_CATCH_STATE[stackTop] = DBL_MRK;
															EX_FINALLY_STATE[stackTop] = (double) (frame.pc + 2);
															break;
														case -22 :
															frame.result = undefined;
															break label851;
														case -21 :
															if (instructionCounting) {
																cx.instructionCount += 100;
															}

															arg76 = onlyFinally[frame.pc] & 255;
															boolean arg71 = onlyFinally[frame.pc + 1] != 0;
															arg62 = getIndex(
																	onlyFinally,
																	frame.pc + 2);
															if (arg71) {
																stackTop -= indexReg;
																skipIndexces = EX_CATCH_STATE[stackTop];
																if (skipIndexces == DBL_MRK) {
																	skipIndexces = ScriptRuntime
																			.wrapNumber(EX_FINALLY_STATE[stackTop]);
																}

																Object[] arg47 = getArgsArray(
																		EX_CATCH_STATE,
																		EX_FINALLY_STATE,
																		stackTop + 1,
																		indexReg);
																EX_CATCH_STATE[stackTop] = ScriptRuntime
																		.newSpecial(
																				cx,
																				skipIndexces,
																				arg47,
																				frame.scope,
																				arg76);
															} else {
																stackTop -= 1 + indexReg;
																Scriptable arg53 = (Scriptable) EX_CATCH_STATE[stackTop + 1];
																arg48 = (Callable) EX_CATCH_STATE[stackTop];
																Object[] arg54 = getArgsArray(
																		EX_CATCH_STATE,
																		EX_FINALLY_STATE,
																		stackTop + 2,
																		indexReg);
																EX_CATCH_STATE[stackTop] = ScriptRuntime
																		.callSpecial(
																				cx,
																				arg48,
																				arg53,
																				arg54,
																				frame.scope,
																				frame.thisObj,
																				arg76,
																				frame.idata.itsSourceFile,
																				arg62);
															}

															frame.pc += 4;
															continue;
														case -20 :
															initFunction(
																	cx,
																	frame.scope,
																	frame.fnOrScript,
																	indexReg);
															continue;
														case -19 :
															++stackTop;
															EX_CATCH_STATE[stackTop] = InterpretedFunction
																	.createFunction(
																			cx,
																			frame.scope,
																			frame.fnOrScript,
																			indexReg);
															continue;
														case -18 :
															offset = EX_CATCH_STATE[stackTop];
															if (offset == DBL_MRK) {
																offset = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop]);
															}

															EX_CATCH_STATE[stackTop] = ScriptRuntime
																	.getValueFunctionAndThis(
																			offset,
																			cx);
															++stackTop;
															EX_CATCH_STATE[stackTop] = ScriptRuntime
																	.lastStoredScriptable(cx);
															continue;
														case -17 :
															offset = EX_CATCH_STATE[stackTop - 1];
															if (offset == DBL_MRK) {
																offset = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop - 1]);
															}

															x = EX_CATCH_STATE[stackTop];
															if (x == DBL_MRK) {
																x = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop]);
															}

															EX_CATCH_STATE[stackTop - 1] = ScriptRuntime
																	.getElemFunctionAndThis(
																			offset,
																			x,
																			cx);
															EX_CATCH_STATE[stackTop] = ScriptRuntime
																	.lastStoredScriptable(cx);
															continue;
														case -16 :
															offset = EX_CATCH_STATE[stackTop];
															if (offset == DBL_MRK) {
																offset = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop]);
															}

															EX_CATCH_STATE[stackTop] = ScriptRuntime
																	.getPropFunctionAndThis(
																			offset,
																			stringReg,
																			cx,
																			frame.scope);
															++stackTop;
															EX_CATCH_STATE[stackTop] = ScriptRuntime
																	.lastStoredScriptable(cx);
															continue;
														case -15 :
															++stackTop;
															EX_CATCH_STATE[stackTop] = ScriptRuntime
																	.getNameFunctionAndThis(
																			stringReg,
																			cx,
																			frame.scope);
															++stackTop;
															EX_CATCH_STATE[stackTop] = ScriptRuntime
																	.lastStoredScriptable(cx);
															continue;
														case -14 :
															++stackTop;
															EX_CATCH_STATE[stackTop] = ScriptRuntime
																	.typeofName(
																			frame.scope,
																			stringReg);
															continue;
														case -13 :
															indexReg += frame.localShift;
															EX_CATCH_STATE[indexReg] = frame.scope;
															continue;
														case -12 :
															indexReg += frame.localShift;
															frame.scope = (Scriptable) EX_CATCH_STATE[indexReg];
															continue;
														case -11 :
															arg51 = (Ref) EX_CATCH_STATE[stackTop];
															EX_CATCH_STATE[stackTop] = ScriptRuntime
																	.refIncrDecr(
																			arg51,
																			cx,
																			onlyFinally[frame.pc]);
															++frame.pc;
															continue;
														case -10 :
															offset = EX_CATCH_STATE[stackTop];
															if (offset == DBL_MRK) {
																offset = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop]);
															}

															--stackTop;
															x = EX_CATCH_STATE[stackTop];
															if (x == DBL_MRK) {
																x = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop]);
															}

															EX_CATCH_STATE[stackTop] = ScriptRuntime
																	.elemIncrDecr(
																			x,
																			offset,
																			cx,
																			onlyFinally[frame.pc]);
															++frame.pc;
															continue;
														case -9 :
															offset = EX_CATCH_STATE[stackTop];
															if (offset == DBL_MRK) {
																offset = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop]);
															}

															EX_CATCH_STATE[stackTop] = ScriptRuntime
																	.propIncrDecr(
																			offset,
																			stringReg,
																			cx,
																			onlyFinally[frame.pc]);
															++frame.pc;
															continue;
														case -8 :
															++stackTop;
															EX_CATCH_STATE[stackTop] = ScriptRuntime
																	.nameIncrDecr(
																			frame.scope,
																			stringReg,
																			cx,
																			onlyFinally[frame.pc]);
															++frame.pc;
															continue;
														case -7 :
															++stackTop;
															byte arg77 = onlyFinally[frame.pc];
															if (!frame.useActivation) {
																EX_CATCH_STATE[stackTop] = DBL_MRK;
																x = EX_NO_JS_STATE[indexReg];
																if (x == DBL_MRK) {
																	arg55 = exState[indexReg];
																} else {
																	arg55 = ScriptRuntime
																			.toNumber(x);
																	EX_NO_JS_STATE[indexReg] = DBL_MRK;
																}

																d2 = (arg77 & 1) == 0
																		? arg55 + 1.0D
																		: arg55 - 1.0D;
																exState[indexReg] = d2;
																EX_FINALLY_STATE[stackTop] = (arg77 & 2) == 0
																		? d2
																		: arg55;
															} else {
																String arg70 = frame.idata.argNames[indexReg];
																EX_CATCH_STATE[stackTop] = ScriptRuntime
																		.nameIncrDecr(
																				frame.scope,
																				arg70,
																				cx,
																				arg77);
															}

															++frame.pc;
															continue;
														case -6 :
															if (!stack_boolean(
																	frame,
																	stackTop--)) {
																frame.pc += 2;
																continue;
															}

															EX_CATCH_STATE[stackTop--] = null;
															break;
														case -5 :
															frame.result = EX_CATCH_STATE[stackTop];
															frame.resultDbl = EX_FINALLY_STATE[stackTop];
															EX_CATCH_STATE[stackTop] = null;
															--stackTop;
															continue;
														case -4 :
															EX_CATCH_STATE[stackTop] = null;
															--stackTop;
															continue;
														case -3 :
															offset = EX_CATCH_STATE[stackTop];
															EX_CATCH_STATE[stackTop] = EX_CATCH_STATE[stackTop - 1];
															EX_CATCH_STATE[stackTop - 1] = offset;
															double arg69 = EX_FINALLY_STATE[stackTop];
															EX_FINALLY_STATE[stackTop] = EX_FINALLY_STATE[stackTop - 1];
															EX_FINALLY_STATE[stackTop - 1] = arg69;
															continue;
														case -2 :
															EX_CATCH_STATE[stackTop + 1] = EX_CATCH_STATE[stackTop - 1];
															EX_FINALLY_STATE[stackTop + 1] = EX_FINALLY_STATE[stackTop - 1];
															EX_CATCH_STATE[stackTop + 2] = EX_CATCH_STATE[stackTop];
															EX_FINALLY_STATE[stackTop + 2] = EX_FINALLY_STATE[stackTop];
															stackTop += 2;
															continue;
														case -1 :
															EX_CATCH_STATE[stackTop + 1] = EX_CATCH_STATE[stackTop];
															EX_FINALLY_STATE[stackTop + 1] = EX_FINALLY_STATE[stackTop];
															++stackTop;
															continue;
														case 2 :
															offset = EX_CATCH_STATE[stackTop];
															if (offset == DBL_MRK) {
																offset = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop]);
															}

															--stackTop;
															frame.scope = ScriptRuntime
																	.enterWith(
																			offset,
																			cx,
																			frame.scope);
															continue;
														case 3 :
															frame.scope = ScriptRuntime
																	.leaveWith(frame.scope);
															continue;
														case 4 :
															frame.result = EX_CATCH_STATE[stackTop];
															frame.resultDbl = EX_FINALLY_STATE[stackTop];
															--stackTop;
															break label851;
														case 5 :
															break;
														case 6 :
															if (!stack_boolean(
																	frame,
																	stackTop--)) {
																frame.pc += 2;
																continue;
															}
															break;
														case 7 :
															if (stack_boolean(
																	frame,
																	stackTop--)) {
																frame.pc += 2;
																continue;
															}
															break;
														case 8 :
															offset = EX_CATCH_STATE[stackTop];
															if (offset == DBL_MRK) {
																offset = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop]);
															}

															--stackTop;
															arg68 = (Scriptable) EX_CATCH_STATE[stackTop];
															EX_CATCH_STATE[stackTop] = ScriptRuntime
																	.setName(
																			arg68,
																			offset,
																			cx,
																			frame.scope,
																			stringReg);
															continue;
														case 9 :
														case 10 :
														case 11 :
														case 18 :
														case 19 :
															arg76 = stack_int32(
																	frame,
																	stackTop - 1);
															arg45 = stack_int32(
																	frame,
																	stackTop);
															--stackTop;
															EX_CATCH_STATE[stackTop] = DBL_MRK;
															switch (op) {
																case 9 :
																	arg76 |= arg45;
																	break;
																case 10 :
																	arg76 ^= arg45;
																	break;
																case 11 :
																	arg76 &= arg45;
																case 12 :
																case 13 :
																case 14 :
																case 15 :
																case 16 :
																case 17 :
																default :
																	break;
																case 18 :
																	arg76 <<= arg45;
																	break;
																case 19 :
																	arg76 >>= arg45;
															}

															EX_FINALLY_STATE[stackTop] = (double) arg76;
															continue;
														case 12 :
														case 13 :
															--stackTop;
															x = EX_CATCH_STATE[stackTop + 1];
															val = EX_CATCH_STATE[stackTop];
															if (x == DBL_MRK) {
																if (val == DBL_MRK) {
																	arg64 = EX_FINALLY_STATE[stackTop] == EX_FINALLY_STATE[stackTop + 1];
																} else {
																	arg64 = ScriptRuntime
																			.eqNumber(
																					EX_FINALLY_STATE[stackTop + 1],
																					val);
																}
															} else if (val == DBL_MRK) {
																arg64 = ScriptRuntime
																		.eqNumber(
																				EX_FINALLY_STATE[stackTop],
																				x);
															} else {
																arg64 = ScriptRuntime
																		.eq(val,
																				x);
															}

															arg64 ^= op == 13;
															EX_CATCH_STATE[stackTop] = ScriptRuntime
																	.wrapBoolean(arg64);
															continue;
														case 14 :
														case 15 :
														case 16 :
														case 17 :
															label832 : {
																--stackTop;
																offset = EX_CATCH_STATE[stackTop + 1];
																x = EX_CATCH_STATE[stackTop];
																double ifun;
																if (offset == DBL_MRK) {
																	arg41 = EX_FINALLY_STATE[stackTop + 1];
																	ifun = stack_double(
																			frame,
																			stackTop);
																} else {
																	if (x != DBL_MRK) {
																		switch (op) {
																			case 14 :
																				arg44 = ScriptRuntime
																						.cmp_LT(x,
																								offset);
																				break label832;
																			case 15 :
																				arg44 = ScriptRuntime
																						.cmp_LE(x,
																								offset);
																				break label832;
																			case 16 :
																				arg44 = ScriptRuntime
																						.cmp_LT(offset,
																								x);
																				break label832;
																			case 17 :
																				arg44 = ScriptRuntime
																						.cmp_LE(offset,
																								x);
																				break label832;
																			default :
																				throw Kit
																						.codeBug();
																		}
																	}

																	arg41 = ScriptRuntime
																			.toNumber(offset);
																	ifun = EX_FINALLY_STATE[stackTop];
																}

																switch (op) {
																	case 14 :
																		arg44 = ifun < arg41;
																		break;
																	case 15 :
																		arg44 = ifun <= arg41;
																		break;
																	case 16 :
																		arg44 = ifun > arg41;
																		break;
																	case 17 :
																		arg44 = ifun >= arg41;
																		break;
																	default :
																		throw Kit
																				.codeBug();
																}
															}

															EX_CATCH_STATE[stackTop] = ScriptRuntime
																	.wrapBoolean(arg44);
															continue;
														case 20 :
															arg75 = stack_double(
																	frame,
																	stackTop - 1);
															arg62 = stack_int32(
																	frame,
																	stackTop) & 31;
															--stackTop;
															EX_CATCH_STATE[stackTop] = DBL_MRK;
															EX_FINALLY_STATE[stackTop] = (double) (ScriptRuntime
																	.toUint32(arg75) >>> arg62);
															continue;
														case 21 :
															--stackTop;
															do_add(EX_CATCH_STATE,
																	EX_FINALLY_STATE,
																	stackTop,
																	cx);
															continue;
														case 22 :
														case 23 :
														case 24 :
														case 25 :
															arg75 = stack_double(
																	frame,
																	stackTop);
															--stackTop;
															arg55 = stack_double(
																	frame,
																	stackTop);
															EX_CATCH_STATE[stackTop] = DBL_MRK;
															switch (op) {
																case 22 :
																	arg55 -= arg75;
																	break;
																case 23 :
																	arg55 *= arg75;
																	break;
																case 24 :
																	arg55 /= arg75;
																	break;
																case 25 :
																	arg55 %= arg75;
															}

															EX_FINALLY_STATE[stackTop] = arg55;
															continue;
														case 26 :
															EX_CATCH_STATE[stackTop] = ScriptRuntime
																	.wrapBoolean(!stack_boolean(
																			frame,
																			stackTop));
															continue;
														case 27 :
															arg76 = stack_int32(
																	frame,
																	stackTop);
															EX_CATCH_STATE[stackTop] = DBL_MRK;
															EX_FINALLY_STATE[stackTop] = (double) (~arg76);
															continue;
														case 28 :
														case 29 :
															arg75 = stack_double(
																	frame,
																	stackTop);
															EX_CATCH_STATE[stackTop] = DBL_MRK;
															if (op == 29) {
																arg75 = -arg75;
															}

															EX_FINALLY_STATE[stackTop] = arg75;
															continue;
														case 30 :
															if (instructionCounting) {
																cx.instructionCount += 100;
															}

															stackTop -= indexReg;
															offset = EX_CATCH_STATE[stackTop];
															if (offset instanceof InterpretedFunction) {
																InterpretedFunction arg66 = (InterpretedFunction) offset;
																if (frame.fnOrScript.securityDomain == arg66.securityDomain) {
																	arg40 = arg66
																			.createObject(
																					cx,
																					frame.scope);
																	Interpreter.CallFrame arg42 = new Interpreter.CallFrame();
																	initFrame(
																			cx,
																			frame.scope,
																			arg40,
																			EX_CATCH_STATE,
																			EX_FINALLY_STATE,
																			stackTop + 1,
																			indexReg,
																			arg66,
																			frame,
																			arg42);
																	EX_CATCH_STATE[stackTop] = arg40;
																	frame.savedStackTop = stackTop;
																	frame.savedCallOp = op;
																	frame = arg42;
																	continue label862;
																}
															}

															if (!(offset instanceof Function)) {
																if (offset == DBL_MRK) {
																	offset = ScriptRuntime
																			.wrapNumber(EX_FINALLY_STATE[stackTop]);
																}

																throw ScriptRuntime
																		.notFunctionError(offset);
															}

															Function arg67 = (Function) offset;
															if (arg67 instanceof IdFunctionObject) {
																IdFunctionObject arg50 = (IdFunctionObject) arg67;
																if (NativeContinuation
																		.isContinuationConstructor(arg50)) {
																	frame.stack[stackTop] = captureContinuation(
																			cx,
																			frame.parentFrame,
																			false);
																	continue;
																}
															}

															arg52 = getArgsArray(
																	EX_CATCH_STATE,
																	EX_FINALLY_STATE,
																	stackTop + 1,
																	indexReg);
															EX_CATCH_STATE[stackTop] = arg67
																	.construct(
																			cx,
																			frame.scope,
																			arg52);
															continue;
														case 31 :
															offset = EX_CATCH_STATE[stackTop];
															if (offset == DBL_MRK) {
																offset = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop]);
															}

															--stackTop;
															x = EX_CATCH_STATE[stackTop];
															if (x == DBL_MRK) {
																x = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop]);
															}

															EX_CATCH_STATE[stackTop] = ScriptRuntime
																	.delete(x,
																			offset,
																			cx);
															continue;
														case 32 :
															offset = EX_CATCH_STATE[stackTop];
															if (offset == DBL_MRK) {
																offset = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop]);
															}

															EX_CATCH_STATE[stackTop] = ScriptRuntime
																	.typeof(offset);
															continue;
														case 33 :
															offset = EX_CATCH_STATE[stackTop];
															if (offset == DBL_MRK) {
																offset = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop]);
															}

															EX_CATCH_STATE[stackTop] = ScriptRuntime
																	.getObjectProp(
																			offset,
																			stringReg,
																			cx,
																			frame.scope);
															continue;
														case 34 :
															offset = EX_CATCH_STATE[stackTop];
															if (offset == DBL_MRK) {
																offset = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop]);
															}

															EX_CATCH_STATE[stackTop] = ScriptRuntime
																	.getObjectPropNoWarn(
																			offset,
																			stringReg,
																			cx);
															continue;
														case 35 :
															offset = EX_CATCH_STATE[stackTop];
															if (offset == DBL_MRK) {
																offset = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop]);
															}

															--stackTop;
															x = EX_CATCH_STATE[stackTop];
															if (x == DBL_MRK) {
																x = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop]);
															}

															EX_CATCH_STATE[stackTop] = ScriptRuntime
																	.setObjectProp(
																			x,
																			stringReg,
																			offset,
																			cx);
															continue;
														case 36 :
															--stackTop;
															offset = EX_CATCH_STATE[stackTop];
															if (offset == DBL_MRK) {
																offset = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop]);
															}

															val = EX_CATCH_STATE[stackTop + 1];
															if (val != DBL_MRK) {
																x = ScriptRuntime
																		.getObjectElem(
																				offset,
																				val,
																				cx,
																				frame.scope);
															} else {
																arg41 = EX_FINALLY_STATE[stackTop + 1];
																x = ScriptRuntime
																		.getObjectIndex(
																				offset,
																				arg41,
																				cx);
															}

															EX_CATCH_STATE[stackTop] = x;
															continue;
														case 37 :
															stackTop -= 2;
															offset = EX_CATCH_STATE[stackTop + 2];
															if (offset == DBL_MRK) {
																offset = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop + 2]);
															}

															x = EX_CATCH_STATE[stackTop];
															if (x == DBL_MRK) {
																x = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop]);
															}

															skipIndexces = EX_CATCH_STATE[stackTop + 1];
															if (skipIndexces != DBL_MRK) {
																val = ScriptRuntime
																		.setObjectElem(
																				x,
																				skipIndexces,
																				offset,
																				cx);
															} else {
																d2 = EX_FINALLY_STATE[stackTop + 1];
																val = ScriptRuntime
																		.setObjectIndex(
																				x,
																				d2,
																				offset,
																				cx);
															}

															EX_CATCH_STATE[stackTop] = val;
															continue;
														case 39 :
															++stackTop;
															EX_CATCH_STATE[stackTop] = ScriptRuntime
																	.name(cx,
																			frame.scope,
																			stringReg);
															continue;
														case 40 :
															++stackTop;
															EX_CATCH_STATE[stackTop] = DBL_MRK;
															EX_FINALLY_STATE[stackTop] = frame.idata.itsDoubleTable[indexReg];
															continue;
														case 41 :
															++stackTop;
															EX_CATCH_STATE[stackTop] = stringReg;
															continue;
														case 42 :
															++stackTop;
															EX_CATCH_STATE[stackTop] = null;
															continue;
														case 43 :
															++stackTop;
															EX_CATCH_STATE[stackTop] = frame.thisObj;
															continue;
														case 44 :
															++stackTop;
															EX_CATCH_STATE[stackTop] = Boolean.FALSE;
															continue;
														case 45 :
															++stackTop;
															EX_CATCH_STATE[stackTop] = Boolean.TRUE;
															continue;
														case 46 :
														case 47 :
															--stackTop;
															arg64 = shallowEquals(
																	EX_CATCH_STATE,
																	EX_FINALLY_STATE,
																	stackTop);
															arg64 ^= op == 47;
															EX_CATCH_STATE[stackTop] = ScriptRuntime
																	.wrapBoolean(arg64);
															continue;
														case 48 :
															++stackTop;
															EX_CATCH_STATE[stackTop] = frame.scriptRegExps[indexReg];
															continue;
														case 49 :
															++stackTop;
															EX_CATCH_STATE[stackTop] = ScriptRuntime
																	.bind(cx,
																			frame.scope,
																			stringReg);
															continue;
														case 50 :
															offset = EX_CATCH_STATE[stackTop];
															if (offset == DBL_MRK) {
																offset = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop]);
															}

															--stackTop;
															arg45 = getIndex(
																	onlyFinally,
																	frame.pc);
															throwable = new JavaScriptException(
																	offset,
																	frame.idata.itsSourceFile,
																	arg45);
															break label858;
														case 51 :
															indexReg += frame.localShift;
															throwable = EX_CATCH_STATE[indexReg];
															break label858;
														case 52 :
														case 53 :
															offset = EX_CATCH_STATE[stackTop];
															if (offset == DBL_MRK) {
																offset = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop]);
															}

															--stackTop;
															x = EX_CATCH_STATE[stackTop];
															if (x == DBL_MRK) {
																x = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop]);
															}

															if (op == 52) {
																arg44 = ScriptRuntime
																		.in(x,
																				offset,
																				cx);
															} else {
																arg44 = ScriptRuntime
																		.instanceOf(
																				x,
																				offset,
																				cx);
															}

															EX_CATCH_STATE[stackTop] = ScriptRuntime
																	.wrapBoolean(arg44);
															continue;
														case 54 :
															++stackTop;
															indexReg += frame.localShift;
															EX_CATCH_STATE[stackTop] = EX_CATCH_STATE[indexReg];
															EX_FINALLY_STATE[stackTop] = EX_FINALLY_STATE[indexReg];
															continue;
														case 57 :
															--stackTop;
															indexReg += frame.localShift;
															arg64 = frame.idata.itsICode[frame.pc] != 0;
															Throwable arg46 = (Throwable) EX_CATCH_STATE[stackTop + 1];
															if (!arg64) {
																arg40 = null;
															} else {
																arg40 = (Scriptable) EX_CATCH_STATE[indexReg];
															}

															EX_CATCH_STATE[indexReg] = ScriptRuntime
																	.newCatchScope(
																			arg46,
																			arg40,
																			stringReg,
																			cx,
																			frame.scope);
															++frame.pc;
															continue;
														case 58 :
														case 59 :
														case 60 :
															offset = EX_CATCH_STATE[stackTop];
															if (offset == DBL_MRK) {
																offset = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop]);
															}

															--stackTop;
															indexReg += frame.localShift;
															arg45 = op == 58
																	? 0
																	: (op == 59
																			? 1
																			: 2);
															EX_CATCH_STATE[indexReg] = ScriptRuntime
																	.enumInit(
																			offset,
																			cx,
																			arg45);
															continue;
														case 61 :
														case 62 :
															indexReg += frame.localShift;
															offset = EX_CATCH_STATE[indexReg];
															++stackTop;
															EX_CATCH_STATE[stackTop] = op == 61
																	? ScriptRuntime
																			.enumNext(offset)
																	: ScriptRuntime
																			.enumId(offset,
																					cx);
															continue;
														case 63 :
															++stackTop;
															EX_CATCH_STATE[stackTop] = frame.fnOrScript;
															continue;
														case 64 :
															break label851;
														case 67 :
															arg51 = (Ref) EX_CATCH_STATE[stackTop];
															EX_CATCH_STATE[stackTop] = ScriptRuntime
																	.refGet(arg51,
																			cx);
															continue;
														case 68 :
															offset = EX_CATCH_STATE[stackTop];
															if (offset == DBL_MRK) {
																offset = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop]);
															}

															--stackTop;
															Ref arg43 = (Ref) EX_CATCH_STATE[stackTop];
															EX_CATCH_STATE[stackTop] = ScriptRuntime
																	.refSet(arg43,
																			offset,
																			cx);
															continue;
														case 69 :
															arg51 = (Ref) EX_CATCH_STATE[stackTop];
															EX_CATCH_STATE[stackTop] = ScriptRuntime
																	.refDel(arg51,
																			cx);
															continue;
														case 71 :
															offset = EX_CATCH_STATE[stackTop];
															if (offset == DBL_MRK) {
																offset = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop]);
															}

															EX_CATCH_STATE[stackTop] = ScriptRuntime
																	.specialRef(
																			offset,
																			stringReg,
																			cx);
															continue;
														case 73 :
															offset = EX_CATCH_STATE[stackTop];
															if (offset == DBL_MRK) {
																offset = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop]);
															}

															EX_CATCH_STATE[stackTop] = ScriptRuntime
																	.setDefaultNamespace(
																			offset,
																			cx);
															continue;
														case 74 :
															offset = EX_CATCH_STATE[stackTop];
															if (offset != DBL_MRK) {
																EX_CATCH_STATE[stackTop] = ScriptRuntime
																		.escapeAttributeValue(
																				offset,
																				cx);
															}
															continue;
														case 75 :
															offset = EX_CATCH_STATE[stackTop];
															if (offset != DBL_MRK) {
																EX_CATCH_STATE[stackTop] = ScriptRuntime
																		.escapeTextValue(
																				offset,
																				cx);
															}
															continue;
														case 76 :
															offset = EX_CATCH_STATE[stackTop];
															if (offset == DBL_MRK) {
																offset = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop]);
															}

															--stackTop;
															x = EX_CATCH_STATE[stackTop];
															if (x == DBL_MRK) {
																x = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop]);
															}

															EX_CATCH_STATE[stackTop] = ScriptRuntime
																	.memberRef(
																			x,
																			offset,
																			cx,
																			indexReg);
															continue;
														case 77 :
															offset = EX_CATCH_STATE[stackTop];
															if (offset == DBL_MRK) {
																offset = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop]);
															}

															--stackTop;
															x = EX_CATCH_STATE[stackTop];
															if (x == DBL_MRK) {
																x = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop]);
															}

															--stackTop;
															val = EX_CATCH_STATE[stackTop];
															if (val == DBL_MRK) {
																val = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop]);
															}

															EX_CATCH_STATE[stackTop] = ScriptRuntime
																	.memberRef(
																			val,
																			x,
																			offset,
																			cx,
																			indexReg);
															continue;
														case 78 :
															offset = EX_CATCH_STATE[stackTop];
															if (offset == DBL_MRK) {
																offset = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop]);
															}

															EX_CATCH_STATE[stackTop] = ScriptRuntime
																	.nameRef(
																			offset,
																			cx,
																			frame.scope,
																			indexReg);
															continue;
														case 79 :
															offset = EX_CATCH_STATE[stackTop];
															if (offset == DBL_MRK) {
																offset = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop]);
															}

															--stackTop;
															x = EX_CATCH_STATE[stackTop];
															if (x == DBL_MRK) {
																x = ScriptRuntime
																		.wrapNumber(EX_FINALLY_STATE[stackTop]);
															}

															EX_CATCH_STATE[stackTop] = ScriptRuntime
																	.nameRef(
																			x,
																			offset,
																			cx,
																			frame.scope,
																			indexReg);
															continue;
													}

													if (instructionCounting) {
														addInstructionCount(cx,
																frame, 2);
													}

													arg76 = getShort(
															onlyFinally,
															frame.pc);
													if (arg76 != 0) {
														frame.pc += arg76 - 1;
													} else {
														frame.pc = frame.idata.longJumps
																.getExistingInt(frame.pc);
													}

													if (instructionCounting) {
														frame.pcPrevBranch = frame.pc;
													}
												}

												exitFrame(cx, frame,
														(Object) null);
												interpreterResult = frame.result;
												interpreterResultDbl = frame.resultDbl;
												if (frame.parentFrame == null) {
													break label869;
												}

												frame = frame.parentFrame;
												if (frame.frozen) {
													frame = frame.cloneFrozen();
												}

												setCallResult(frame,
														interpreterResult,
														interpreterResultDbl);
												interpreterResult = null;
												continue label862;
											}
										}
									}
								} catch (Throwable arg32) {
									if (throwable != null) {
										arg32.printStackTrace(System.err);
										throw new IllegalStateException();
									}

									throwable = arg32;
								}

								if (throwable == null) {
									Kit.codeBug();
								}

								boolean arg33 = true;
								boolean arg34 = true;
								boolean arg35 = false;
								Interpreter.ContinuationJump arg37 = null;
								int arg36;
								if (generatorState != null
										&& generatorState.operation == 2
										&& throwable == generatorState.value) {
									arg36 = 1;
								} else if (throwable instanceof JavaScriptException) {
									arg36 = 2;
								} else if (throwable instanceof EcmaError) {
									arg36 = 2;
								} else if (throwable instanceof EvaluatorException) {
									arg36 = 2;
								} else if (throwable instanceof RuntimeException) {
									arg36 = cx.hasFeature(13) ? 2 : 1;
								} else if (throwable instanceof Error) {
									arg36 = cx.hasFeature(13) ? 2 : 0;
								} else if (throwable instanceof Interpreter.ContinuationJump) {
									arg36 = 1;
									arg37 = (Interpreter.ContinuationJump) throwable;
								} else {
									arg36 = cx.hasFeature(13) ? 2 : 1;
								}

								if (instructionCounting) {
									try {
										addInstructionCount(cx, frame, 100);
									} catch (RuntimeException arg30) {
										throwable = arg30;
										arg36 = 1;
									} catch (Error arg31) {
										throwable = arg31;
										arg37 = null;
										arg36 = 0;
									}
								}

								if (frame.debuggerFrame != null
										&& throwable instanceof RuntimeException) {
									RuntimeException arg38 = (RuntimeException) throwable;

									try {
										frame.debuggerFrame.onExceptionThrown(
												cx, arg38);
									} catch (Throwable arg29) {
										throwable = arg29;
										arg37 = null;
										arg36 = 0;
									}
								}

								do {
									if (arg36 != 0) {
										boolean arg39 = arg36 != 2;
										indexReg = getExceptionHandler(frame,
												arg39);
										if (indexReg >= 0) {
											continue label862;
										}
									}

									exitFrame(cx, frame, throwable);
									frame = frame.parentFrame;
									if (frame == null) {
										if (arg37 == null) {
											break label869;
										}

										if (arg37.branchFrame != null) {
											Kit.codeBug();
										}

										if (arg37.capturedFrame != null) {
											indexReg = -1;
											continue label862;
										} else {
											interpreterResult = arg37.result;
											interpreterResultDbl = arg37.resultDbl;
											throwable = null;
											break label869;
										}
									}
								} while (arg37 == null
										|| arg37.branchFrame != frame);

								indexReg = -1;
							}
						}
					}
				}

				if (cx.previousInterpreterInvocations != null
						&& cx.previousInterpreterInvocations.size() != 0) {
					cx.lastInterpreterFrame = cx.previousInterpreterInvocations
							.pop();
				} else {
					cx.lastInterpreterFrame = null;
					cx.previousInterpreterInvocations = null;
				}

				if (throwable != null) {
					if (throwable instanceof RuntimeException) {
						throw (RuntimeException) throwable;
					}

					throw (Error) throwable;
				}

				return interpreterResult != DBL_MRK
						? interpreterResult
						: ScriptRuntime.wrapNumber(interpreterResultDbl);
			}
		}
	}

	private static Interpreter.CallFrame initFrameForNoSuchMethod(Context cx,
			Interpreter.CallFrame frame, int indexReg, Object[] stack,
			double[] sDbl, int stackTop, int op, Scriptable funThisObj,
			Scriptable calleeScope, NoSuchMethodShim noSuchMethodShim,
			InterpretedFunction ifun) {
		Object[] argsArray = null;
		int shift = stackTop + 2;
		Object[] elements = new Object[indexReg];

		for (int callParentFrame = 0; callParentFrame < indexReg; ++shift) {
			Object calleeFrame = stack[shift];
			if (calleeFrame == UniqueTag.DOUBLE_MARK) {
				calleeFrame = ScriptRuntime.wrapNumber(sDbl[shift]);
			}

			elements[callParentFrame] = calleeFrame;
			++callParentFrame;
		}

		argsArray = new Object[]{noSuchMethodShim.methodName,
				cx.newArray(calleeScope, elements)};
		Interpreter.CallFrame arg15 = frame;
		Interpreter.CallFrame arg16 = new Interpreter.CallFrame();
		if (op == -55) {
			arg15 = frame.parentFrame;
			exitFrame(cx, frame, (Object) null);
		}

		initFrame(cx, calleeScope, funThisObj, argsArray, (double[]) null, 0,
				2, ifun, arg15, arg16);
		if (op != -55) {
			frame.savedStackTop = stackTop;
			frame.savedCallOp = op;
		}

		return arg16;
	}

	private static boolean shallowEquals(Object[] stack, double[] sDbl,
			int stackTop) {
		Object rhs = stack[stackTop + 1];
		Object lhs = stack[stackTop];
		UniqueTag DBL_MRK = UniqueTag.DOUBLE_MARK;
		double rdbl;
		double ldbl;
		if (rhs == DBL_MRK) {
			rdbl = sDbl[stackTop + 1];
			if (lhs == DBL_MRK) {
				ldbl = sDbl[stackTop];
			} else {
				if (!(lhs instanceof Number)) {
					return false;
				}

				ldbl = ((Number) lhs).doubleValue();
			}
		} else {
			if (lhs != DBL_MRK) {
				return ScriptRuntime.shallowEq(lhs, rhs);
			}

			ldbl = sDbl[stackTop];
			if (rhs == DBL_MRK) {
				rdbl = sDbl[stackTop + 1];
			} else {
				if (!(rhs instanceof Number)) {
					return false;
				}

				rdbl = ((Number) rhs).doubleValue();
			}
		}

		return ldbl == rdbl;
	}

	private static Interpreter.CallFrame processThrowable(Context cx,
			Object throwable, Interpreter.CallFrame frame, int indexReg,
			boolean instructionCounting) {
		int rewindCount;
		int enterCount;
		if (indexReg >= 0) {
			if (frame.frozen) {
				frame = frame.cloneFrozen();
			}

			int[] cjump = frame.idata.itsExceptionTable;
			frame.pc = cjump[indexReg + 2];
			if (instructionCounting) {
				frame.pcPrevBranch = frame.pc;
			}

			frame.savedStackTop = frame.emptyStackTop;
			rewindCount = frame.localShift + cjump[indexReg + 5];
			enterCount = frame.localShift + cjump[indexReg + 4];
			frame.scope = (Scriptable) frame.stack[rewindCount];
			frame.stack[enterCount] = throwable;
			throwable = null;
		} else {
			Interpreter.ContinuationJump arg10 = (Interpreter.ContinuationJump) throwable;
			throwable = null;
			if (arg10.branchFrame != frame) {
				Kit.codeBug();
			}

			if (arg10.capturedFrame == null) {
				Kit.codeBug();
			}

			rewindCount = arg10.capturedFrame.frameIndex + 1;
			if (arg10.branchFrame != null) {
				rewindCount -= arg10.branchFrame.frameIndex;
			}

			enterCount = 0;
			Interpreter.CallFrame[] enterFrames = null;
			Interpreter.CallFrame x = arg10.capturedFrame;

			for (int i = 0; i != rewindCount; ++i) {
				if (!x.frozen) {
					Kit.codeBug();
				}

				if (isFrameEnterExitRequired(x)) {
					if (enterFrames == null) {
						enterFrames = new Interpreter.CallFrame[rewindCount - i];
					}

					enterFrames[enterCount] = x;
					++enterCount;
				}

				x = x.parentFrame;
			}

			while (enterCount != 0) {
				--enterCount;
				Object arg11 = enterFrames[enterCount];
				enterFrame(cx, (Interpreter.CallFrame) arg11,
						ScriptRuntime.emptyArgs, true);
			}

			frame = arg10.capturedFrame.cloneFrozen();
			setCallResult(frame, arg10.result, arg10.resultDbl);
		}

		frame.throwable = throwable;
		return frame;
	}

	private static Object freezeGenerator(Context cx,
			Interpreter.CallFrame frame, int stackTop,
			Interpreter.GeneratorState generatorState) {
		if (generatorState.operation == 2) {
			throw ScriptRuntime.typeError0("msg.yield.closing");
		} else {
			frame.frozen = true;
			frame.result = frame.stack[stackTop];
			frame.resultDbl = frame.sDbl[stackTop];
			frame.savedStackTop = stackTop;
			--frame.pc;
			ScriptRuntime.exitActivationFunction(cx);
			return frame.result != UniqueTag.DOUBLE_MARK
					? frame.result
					: ScriptRuntime.wrapNumber(frame.resultDbl);
		}
	}

	private static Object thawGenerator(Interpreter.CallFrame frame,
			int stackTop, Interpreter.GeneratorState generatorState, int op) {
		frame.frozen = false;
		int sourceLine = getIndex(frame.idata.itsICode, frame.pc);
		frame.pc += 2;
		if (generatorState.operation == 1) {
			return new JavaScriptException(generatorState.value,
					frame.idata.itsSourceFile, sourceLine);
		} else if (generatorState.operation == 2) {
			return generatorState.value;
		} else if (generatorState.operation != 0) {
			throw Kit.codeBug();
		} else {
			if (op == 72) {
				frame.stack[stackTop] = generatorState.value;
			}

			return Scriptable.NOT_FOUND;
		}
	}

	private static Interpreter.CallFrame initFrameForApplyOrCall(Context cx,
			Interpreter.CallFrame frame, int indexReg, Object[] stack,
			double[] sDbl, int stackTop, int op, Scriptable calleeScope,
			IdFunctionObject ifun, InterpretedFunction iApplyCallable) {
		Scriptable applyThis;
		if (indexReg != 0) {
			Object calleeFrame = stack[stackTop + 2];
			if (calleeFrame == UniqueTag.DOUBLE_MARK) {
				calleeFrame = ScriptRuntime.wrapNumber(sDbl[stackTop + 2]);
			}

			applyThis = ScriptRuntime.toObjectOrNull(cx, calleeFrame);
		} else {
			applyThis = null;
		}

		if (applyThis == null) {
			applyThis = ScriptRuntime.getTopCallScope(cx);
		}

		if (op == -55) {
			exitFrame(cx, frame, (Object) null);
			frame = frame.parentFrame;
		} else {
			frame.savedStackTop = stackTop;
			frame.savedCallOp = op;
		}

		Interpreter.CallFrame arg12 = new Interpreter.CallFrame();
		if (BaseFunction.isApply(ifun)) {
			Object[] argCount = indexReg < 2
					? ScriptRuntime.emptyArgs
					: ScriptRuntime.getApplyArguments(cx, stack[stackTop + 3]);
			initFrame(cx, calleeScope, applyThis, argCount, (double[]) null, 0,
					argCount.length, iApplyCallable, frame, arg12);
		} else {
			int arg13;
			for (arg13 = 1; arg13 < indexReg; ++arg13) {
				stack[stackTop + 1 + arg13] = stack[stackTop + 2 + arg13];
				sDbl[stackTop + 1 + arg13] = sDbl[stackTop + 2 + arg13];
			}

			arg13 = indexReg < 2 ? 0 : indexReg - 1;
			initFrame(cx, calleeScope, applyThis, stack, sDbl, stackTop + 2,
					arg13, iApplyCallable, frame, arg12);
		}

		return arg12;
	}

	private static void initFrame(Context cx, Scriptable callerScope,
			Scriptable thisObj, Object[] args, double[] argsDbl, int argShift,
			int argCount, InterpretedFunction fnOrScript,
			Interpreter.CallFrame parentFrame, Interpreter.CallFrame frame) {
		InterpreterData idata = fnOrScript.idata;
		boolean useActivation = idata.itsNeedsActivation;
		DebugFrame debuggerFrame = null;
		if (cx.debugger != null) {
			debuggerFrame = cx.debugger.getFrame(cx, idata);
			if (debuggerFrame != null) {
				useActivation = true;
			}
		}

		if (useActivation) {
			if (argsDbl != null) {
				args = getArgsArray(args, argsDbl, argShift, argCount);
			}

			argShift = 0;
			argsDbl = null;
		}

		Scriptable scope;
		if (idata.itsFunctionType != 0) {
			if (!idata.useDynamicScope) {
				scope = fnOrScript.getParentScope();
			} else {
				scope = callerScope;
			}

			if (useActivation) {
				scope = ScriptRuntime.createFunctionActivation(fnOrScript,
						scope, args);
			}
		} else {
			scope = callerScope;
			ScriptRuntime.initScript(fnOrScript, thisObj, cx, callerScope,
					fnOrScript.idata.evalScriptFlag);
		}

		if (idata.itsNestedFunctions != null) {
			if (idata.itsFunctionType != 0 && !idata.itsNeedsActivation) {
				Kit.codeBug();
			}

			for (int scriptRegExps = 0; scriptRegExps < idata.itsNestedFunctions.length; ++scriptRegExps) {
				InterpreterData emptyStackTop = idata.itsNestedFunctions[scriptRegExps];
				if (emptyStackTop.itsFunctionType == 1) {
					initFunction(cx, scope, fnOrScript, scriptRegExps);
				}
			}
		}

		Scriptable[] arg23 = null;
		if (idata.itsRegExpLiterals != null) {
			if (idata.itsFunctionType != 0) {
				arg23 = fnOrScript.functionRegExps;
			} else {
				arg23 = fnOrScript.createRegExpWraps(cx, scope);
			}
		}

		int arg24 = idata.itsMaxVars + idata.itsMaxLocals - 1;
		int maxFrameArray = idata.itsMaxFrameArray;
		if (maxFrameArray != arg24 + idata.itsMaxStack + 1) {
			Kit.codeBug();
		}

		Object[] stack;
		int[] stackAttributes;
		double[] sDbl;
		boolean stackReuse;
		if (frame.stack != null && maxFrameArray <= frame.stack.length) {
			stackReuse = true;
			stack = frame.stack;
			stackAttributes = frame.stackAttributes;
			sDbl = frame.sDbl;
		} else {
			stackReuse = false;
			stack = new Object[maxFrameArray];
			stackAttributes = new int[maxFrameArray];
			sDbl = new double[maxFrameArray];
		}

		int varCount = idata.getParamAndVarCount();

		int definedArgs;
		for (definedArgs = 0; definedArgs < varCount; ++definedArgs) {
			if (idata.getParamOrVarConst(definedArgs)) {
				stackAttributes[definedArgs] = 13;
			}
		}

		definedArgs = idata.argCount;
		if (definedArgs > argCount) {
			definedArgs = argCount;
		}

		frame.parentFrame = parentFrame;
		frame.frameIndex = parentFrame == null ? 0 : parentFrame.frameIndex + 1;
		if (frame.frameIndex > cx.getMaximumInterpreterStackDepth()) {
			throw Context.reportRuntimeError("Exceeded maximum stack depth");
		} else {
			frame.frozen = false;
			frame.fnOrScript = fnOrScript;
			frame.idata = idata;
			frame.stack = stack;
			frame.stackAttributes = stackAttributes;
			frame.sDbl = sDbl;
			frame.varSource = frame;
			frame.localShift = idata.itsMaxVars;
			frame.emptyStackTop = arg24;
			frame.debuggerFrame = debuggerFrame;
			frame.useActivation = useActivation;
			frame.thisObj = thisObj;
			frame.scriptRegExps = arg23;
			frame.result = Undefined.instance;
			frame.pc = 0;
			frame.pcPrevBranch = 0;
			frame.pcSourceLineStart = idata.firstLinePC;
			frame.scope = scope;
			frame.savedStackTop = arg24;
			frame.savedCallOp = 0;
			System.arraycopy(args, argShift, stack, 0, definedArgs);
			if (argsDbl != null) {
				System.arraycopy(argsDbl, argShift, sDbl, 0, definedArgs);
			}

			int i;
			for (i = definedArgs; i != idata.itsMaxVars; ++i) {
				stack[i] = Undefined.instance;
			}

			if (stackReuse) {
				for (i = arg24 + 1; i != stack.length; ++i) {
					stack[i] = null;
				}
			}

			enterFrame(cx, frame, args, false);
		}
	}

	private static boolean isFrameEnterExitRequired(Interpreter.CallFrame frame) {
		return frame.debuggerFrame != null || frame.idata.itsNeedsActivation;
	}

	private static void enterFrame(Context cx, Interpreter.CallFrame frame,
			Object[] args, boolean continuationRestart) {
		boolean usesActivation = frame.idata.itsNeedsActivation;
		boolean isDebugged = frame.debuggerFrame != null;
		if (usesActivation || isDebugged) {
			Scriptable scope = frame.scope;
			if (scope == null) {
				Kit.codeBug();
			} else if (continuationRestart) {
				label55 : {
					do {
						if (!(scope instanceof NativeWith)) {
							break label55;
						}

						scope = scope.getParentScope();
					} while (scope != null
							&& (frame.parentFrame == null || frame.parentFrame.scope != scope));

					Kit.codeBug();
				}
			}

			if (isDebugged) {
				frame.debuggerFrame.onEnter(cx, scope, frame.thisObj, args);
			}

			if (usesActivation) {
				ScriptRuntime.enterActivationFunction(cx, scope);
			}
		}

	}

	private static void exitFrame(Context cx, Interpreter.CallFrame frame,
			Object throwable) {
		if (frame.idata.itsNeedsActivation) {
			ScriptRuntime.exitActivationFunction(cx);
		}

		if (frame.debuggerFrame != null) {
			try {
				if (throwable instanceof Throwable) {
					frame.debuggerFrame.onExit(cx, true, throwable);
				} else {
					Interpreter.ContinuationJump cjump = (Interpreter.ContinuationJump) throwable;
					Object ex;
					if (cjump == null) {
						ex = frame.result;
					} else {
						ex = cjump.result;
					}

					if (ex == UniqueTag.DOUBLE_MARK) {
						double resultDbl;
						if (cjump == null) {
							resultDbl = frame.resultDbl;
						} else {
							resultDbl = cjump.resultDbl;
						}

						ex = ScriptRuntime.wrapNumber(resultDbl);
					}

					frame.debuggerFrame.onExit(cx, false, ex);
				}
			} catch (Throwable arg6) {
				System.err
						.println("RHINO USAGE WARNING: onExit terminated with exception");
				arg6.printStackTrace(System.err);
			}
		}

	}

	private static void setCallResult(Interpreter.CallFrame frame,
			Object callResult, double callResultDbl) {
		if (frame.savedCallOp == 38) {
			frame.stack[frame.savedStackTop] = callResult;
			frame.sDbl[frame.savedStackTop] = callResultDbl;
		} else if (frame.savedCallOp == 30) {
			if (callResult instanceof Scriptable) {
				frame.stack[frame.savedStackTop] = callResult;
			}
		} else {
			Kit.codeBug();
		}

		frame.savedCallOp = 0;
	}

	public static NativeContinuation captureContinuation(Context cx) {
		if (cx.lastInterpreterFrame != null
				&& cx.lastInterpreterFrame instanceof Interpreter.CallFrame) {
			return captureContinuation(cx,
					(Interpreter.CallFrame) cx.lastInterpreterFrame, true);
		} else {
			throw new IllegalStateException("Interpreter frames not found");
		}
	}

	private static NativeContinuation captureContinuation(Context cx,
			Interpreter.CallFrame frame, boolean requireContinuationsTopFrame) {
		NativeContinuation c = new NativeContinuation();
		ScriptRuntime.setObjectProtoAndParent(c,
				ScriptRuntime.getTopCallScope(cx));
		Interpreter.CallFrame x = frame;

		Interpreter.CallFrame outermost;
		for (outermost = frame; x != null && !x.frozen; x = x.parentFrame) {
			x.frozen = true;

			for (int i = x.savedStackTop + 1; i != x.stack.length; ++i) {
				x.stack[i] = null;
				x.stackAttributes[i] = 0;
			}

			if (x.savedCallOp == 38) {
				x.stack[x.savedStackTop] = null;
			} else if (x.savedCallOp != 30) {
				Kit.codeBug();
			}

			outermost = x;
		}

		if (requireContinuationsTopFrame) {
			while (outermost.parentFrame != null) {
				outermost = outermost.parentFrame;
			}

			if (!outermost.isContinuationsTopFrame) {
				throw new IllegalStateException(
						"Cannot capture continuation from JavaScript code not called directly by executeScriptWithContinuations or callFunctionWithContinuations");
			}
		}

		c.initImplementation(frame);
		return c;
	}

	private static int stack_int32(Interpreter.CallFrame frame, int i) {
		Object x = frame.stack[i];
		double value;
		if (x == UniqueTag.DOUBLE_MARK) {
			value = frame.sDbl[i];
		} else {
			value = ScriptRuntime.toNumber(x);
		}

		return ScriptRuntime.toInt32(value);
	}

	private static double stack_double(Interpreter.CallFrame frame, int i) {
		Object x = frame.stack[i];
		return x != UniqueTag.DOUBLE_MARK
				? ScriptRuntime.toNumber(x)
				: frame.sDbl[i];
	}

	private static boolean stack_boolean(Interpreter.CallFrame frame, int i) {
		Object x = frame.stack[i];
		if (x == Boolean.TRUE) {
			return true;
		} else if (x == Boolean.FALSE) {
			return false;
		} else {
			double d;
			if (x == UniqueTag.DOUBLE_MARK) {
				d = frame.sDbl[i];
				return d == d && d != 0.0D;
			} else if (x != null && x != Undefined.instance) {
				if (!(x instanceof Number)) {
					return x instanceof Boolean
							? ((Boolean) x).booleanValue()
							: ScriptRuntime.toBoolean(x);
				} else {
					d = ((Number) x).doubleValue();
					return d == d && d != 0.0D;
				}
			} else {
				return false;
			}
		}
	}

	private static void do_add(Object[] stack, double[] sDbl, int stackTop,
			Context cx) {
		Object rhs = stack[stackTop + 1];
		Object lhs = stack[stackTop];
		double d;
		boolean leftRightOrder;
		String rstr;
		String lDbl1;
		double lDbl2;
		if (rhs == UniqueTag.DOUBLE_MARK) {
			d = sDbl[stackTop + 1];
			if (lhs == UniqueTag.DOUBLE_MARK) {
				sDbl[stackTop] += d;
				return;
			}

			leftRightOrder = true;
		} else {
			if (lhs != UniqueTag.DOUBLE_MARK) {
				if (!(lhs instanceof Scriptable)
						&& !(rhs instanceof Scriptable)) {
					if (lhs instanceof String) {
						lDbl1 = (String) lhs;
						rstr = ScriptRuntime.toString(rhs);
						stack[stackTop] = lDbl1.concat(rstr);
					} else if (rhs instanceof String) {
						lDbl1 = ScriptRuntime.toString(lhs);
						rstr = (String) rhs;
						stack[stackTop] = lDbl1.concat(rstr);
					} else {
						lDbl2 = lhs instanceof Number ? ((Number) lhs)
								.doubleValue() : ScriptRuntime.toNumber(lhs);
						double rDbl = rhs instanceof Number ? ((Number) rhs)
								.doubleValue() : ScriptRuntime.toNumber(rhs);
						stack[stackTop] = UniqueTag.DOUBLE_MARK;
						sDbl[stackTop] = lDbl2 + rDbl;
					}
				} else {
					stack[stackTop] = ScriptRuntime.add(lhs, rhs, cx);
				}

				return;
			}

			d = sDbl[stackTop];
			lhs = rhs;
			leftRightOrder = false;
		}

		if (lhs instanceof Scriptable) {
			rhs = ScriptRuntime.wrapNumber(d);
			if (!leftRightOrder) {
				Object lDbl = lhs;
				lhs = rhs;
				rhs = lDbl;
			}

			stack[stackTop] = ScriptRuntime.add(lhs, rhs, cx);
		} else if (lhs instanceof String) {
			lDbl1 = (String) lhs;
			rstr = ScriptRuntime.toString(d);
			if (leftRightOrder) {
				stack[stackTop] = lDbl1.concat(rstr);
			} else {
				stack[stackTop] = rstr.concat(lDbl1);
			}
		} else {
			lDbl2 = lhs instanceof Number
					? ((Number) lhs).doubleValue()
					: ScriptRuntime.toNumber(lhs);
			stack[stackTop] = UniqueTag.DOUBLE_MARK;
			sDbl[stackTop] = lDbl2 + d;
		}

	}

	private static Object[] getArgsArray(Object[] stack, double[] sDbl,
			int shift, int count) {
		if (count == 0) {
			return ScriptRuntime.emptyArgs;
		} else {
			Object[] args = new Object[count];

			for (int i = 0; i != count; ++shift) {
				Object val = stack[shift];
				if (val == UniqueTag.DOUBLE_MARK) {
					val = ScriptRuntime.wrapNumber(sDbl[shift]);
				}

				args[i] = val;
				++i;
			}

			return args;
		}
	}

	private static void addInstructionCount(Context cx,
			Interpreter.CallFrame frame, int extra) {
		cx.instructionCount += frame.pc - frame.pcPrevBranch + extra;
		if (cx.instructionCount > cx.instructionThreshold) {
			cx.observeInstructionCount(cx.instructionCount);
			cx.instructionCount = 0;
		}

	}

	static class GeneratorState {
		int operation;
		Object value;
		RuntimeException returnedException;

		GeneratorState(int operation, Object value) {
			this.operation = operation;
			this.value = value;
		}
	}

	private static final class ContinuationJump implements Serializable {
		static final long serialVersionUID = 7687739156004308247L;
		Interpreter.CallFrame capturedFrame;
		Interpreter.CallFrame branchFrame;
		Object result;
		double resultDbl;

		ContinuationJump(NativeContinuation c, Interpreter.CallFrame current) {
			this.capturedFrame = (Interpreter.CallFrame) c.getImplementation();
			if (this.capturedFrame != null && current != null) {
				Interpreter.CallFrame chain1 = this.capturedFrame;
				Interpreter.CallFrame chain2 = current;
				int diff = chain1.frameIndex - current.frameIndex;
				if (diff != 0) {
					if (diff < 0) {
						chain1 = current;
						chain2 = this.capturedFrame;
						diff = -diff;
					}

					do {
						chain1 = chain1.parentFrame;
						--diff;
					} while (diff != 0);

					if (chain1.frameIndex != chain2.frameIndex) {
						Kit.codeBug();
					}
				}

				while (chain1 != chain2 && chain1 != null) {
					chain1 = chain1.parentFrame;
					chain2 = chain2.parentFrame;
				}

				this.branchFrame = chain1;
				if (this.branchFrame != null && !this.branchFrame.frozen) {
					Kit.codeBug();
				}
			} else {
				this.branchFrame = null;
			}

		}
	}

	private static class CallFrame implements Cloneable, Serializable {
		static final long serialVersionUID = -2843792508994958978L;
		Interpreter.CallFrame parentFrame;
		int frameIndex;
		boolean frozen;
		InterpretedFunction fnOrScript;
		InterpreterData idata;
		Object[] stack;
		int[] stackAttributes;
		double[] sDbl;
		Interpreter.CallFrame varSource;
		int localShift;
		int emptyStackTop;
		DebugFrame debuggerFrame;
		boolean useActivation;
		boolean isContinuationsTopFrame;
		Scriptable thisObj;
		Scriptable[] scriptRegExps;
		Object result;
		double resultDbl;
		int pc;
		int pcPrevBranch;
		int pcSourceLineStart;
		Scriptable scope;
		int savedStackTop;
		int savedCallOp;
		Object throwable;

		private CallFrame() {
		}

		Interpreter.CallFrame cloneFrozen() {
			if (!this.frozen) {
				Kit.codeBug();
			}

			Interpreter.CallFrame copy;
			try {
				copy = (Interpreter.CallFrame) this.clone();
			} catch (CloneNotSupportedException arg2) {
				throw new IllegalStateException();
			}

			copy.stack = (Object[]) this.stack.clone();
			copy.stackAttributes = (int[]) this.stackAttributes.clone();
			copy.sDbl = (double[]) this.sDbl.clone();
			copy.frozen = false;
			return copy;
		}
	}
}