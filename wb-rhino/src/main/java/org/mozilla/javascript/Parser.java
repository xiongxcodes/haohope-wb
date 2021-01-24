/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Decompiler;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.FunctionNode;
import org.mozilla.javascript.IRFactory;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.ObjArray;
import org.mozilla.javascript.ScriptOrFnNode;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.TokenStream;
import org.mozilla.javascript.Node.Scope;
import org.mozilla.javascript.Node.Symbol;

public class Parser {
	static final int CLEAR_TI_MASK = 65535;
	static final int TI_AFTER_EOL = 65536;
	static final int TI_CHECK_LABEL = 131072;
	CompilerEnvirons compilerEnv;
	private ErrorReporter errorReporter;
	private String sourceURI;
	boolean calledByCompileFunction;
	private TokenStream ts;
	private int currentFlaggedToken;
	private int syntaxErrorCount;
	private IRFactory nf;
	private int nestingOfFunction;
	private Decompiler decompiler;
	private String encodedSource;
	ScriptOrFnNode currentScriptOrFn;
	Scope currentScope;
	private int nestingOfWith;
	private Map<String, Node> labelSet;
	private ObjArray loopSet;
	private ObjArray loopAndSwitchSet;
	private int endFlags;

	public int getCurrentLineNumber() {
		return this.ts.getLineno();
	}

	public Parser(CompilerEnvirons compilerEnv, ErrorReporter errorReporter) {
		this.compilerEnv = compilerEnv;
		this.errorReporter = errorReporter;
	}

	protected Decompiler createDecompiler(CompilerEnvirons compilerEnv) {
		return new Decompiler();
	}

	void addStrictWarning(String messageId, String messageArg) {
		if (this.compilerEnv.isStrictMode()) {
			this.addWarning(messageId, messageArg);
		}

	}

	void addWarning(String messageId, String messageArg) {
		String message = ScriptRuntime.getMessage1(messageId, messageArg);
		if (this.compilerEnv.reportWarningAsError()) {
			++this.syntaxErrorCount;
			this.errorReporter
					.error(message, this.sourceURI, this.ts.getLineno(),
							this.ts.getLine(), this.ts.getOffset());
		} else {
			this.errorReporter
					.warning(message, this.sourceURI, this.ts.getLineno(),
							this.ts.getLine(), this.ts.getOffset());
		}

	}

	void addError(String messageId) {
		++this.syntaxErrorCount;
		String message = ScriptRuntime.getMessage0(messageId);
		this.errorReporter.error(message, this.sourceURI, this.ts.getLineno(),
				this.ts.getLine(), this.ts.getOffset());
	}

	void addError(String messageId, String messageArg) {
		++this.syntaxErrorCount;
		String message = ScriptRuntime.getMessage1(messageId, messageArg);
		this.errorReporter.error(message, this.sourceURI, this.ts.getLineno(),
				this.ts.getLine(), this.ts.getOffset());
	}

	RuntimeException reportError(String messageId) {
		this.addError(messageId);
		throw new Parser.ParserException();
	}

	private int peekToken() throws IOException {
		int tt = this.currentFlaggedToken;
		if (tt == 0) {
			tt = this.ts.getToken();
			if (tt == 1) {
				do {
					tt = this.ts.getToken();
				} while (tt == 1);

				tt |= 65536;
			}

			this.currentFlaggedToken = tt;
		}

		return tt & '￿';
	}

	private int peekFlaggedToken() throws IOException {
		this.peekToken();
		return this.currentFlaggedToken;
	}

	private void consumeToken() {
		this.currentFlaggedToken = 0;
	}

	private int nextToken() throws IOException {
		int tt = this.peekToken();
		this.consumeToken();
		return tt;
	}

	private int nextFlaggedToken() throws IOException {
		this.peekToken();
		int ttFlagged = this.currentFlaggedToken;
		this.consumeToken();
		return ttFlagged;
	}

	private boolean matchToken(int toMatch) throws IOException {
		int tt = this.peekToken();
		if (tt != toMatch) {
			return false;
		} else {
			this.consumeToken();
			return true;
		}
	}

	private int peekTokenOrEOL() throws IOException {
		int tt = this.peekToken();
		if ((this.currentFlaggedToken & 65536) != 0) {
			tt = 1;
		}

		return tt;
	}

	private void setCheckForLabel() {
		if ((this.currentFlaggedToken & '￿') != 39) {
			throw Kit.codeBug();
		} else {
			this.currentFlaggedToken |= 131072;
		}
	}

	private void mustMatchToken(int toMatch, String messageId)
			throws IOException, Parser.ParserException {
		if (!this.matchToken(toMatch)) {
			this.reportError(messageId);
		}

	}

	private void mustHaveXML() {
		if (!this.compilerEnv.isXmlAvailable()) {
			this.reportError("msg.XML.not.available");
		}

	}

	public String getEncodedSource() {
		return this.encodedSource;
	}

	public boolean eof() {
		return this.ts.eof();
	}

	boolean insideFunction() {
		return this.nestingOfFunction != 0;
	}

	void pushScope(Node node) {
		Scope scopeNode = (Scope) node;
		if (scopeNode.getParentScope() != null) {
			throw Kit.codeBug();
		} else {
			scopeNode.setParent(this.currentScope);
			this.currentScope = scopeNode;
		}
	}

	void popScope() {
		this.currentScope = this.currentScope.getParentScope();
	}

	private Node enterLoop(Node loopLabel, boolean doPushScope) {
		Node loop = this.nf.createLoopNode(loopLabel, this.ts.getLineno());
		if (this.loopSet == null) {
			this.loopSet = new ObjArray();
			if (this.loopAndSwitchSet == null) {
				this.loopAndSwitchSet = new ObjArray();
			}
		}

		this.loopSet.push(loop);
		this.loopAndSwitchSet.push(loop);
		if (doPushScope) {
			this.pushScope(loop);
		}

		return loop;
	}

	private void exitLoop(boolean doPopScope) {
		this.loopSet.pop();
		this.loopAndSwitchSet.pop();
		if (doPopScope) {
			this.popScope();
		}

	}

	private Node enterSwitch(Node switchSelector, int lineno) {
		Node switchNode = this.nf.createSwitch(switchSelector, lineno);
		if (this.loopAndSwitchSet == null) {
			this.loopAndSwitchSet = new ObjArray();
		}

		this.loopAndSwitchSet.push(switchNode);
		return switchNode;
	}

	private void exitSwitch() {
		this.loopAndSwitchSet.pop();
	}

	public ScriptOrFnNode parse(String sourceString, String sourceURI,
			int lineno) {
		this.sourceURI = sourceURI;
		this.ts = new TokenStream(this, (Reader) null, sourceString, lineno);

		try {
			return this.parse();
		} catch (IOException arg4) {
			throw new IllegalStateException();
		}
	}

	public ScriptOrFnNode parse(Reader sourceReader, String sourceURI,
			int lineno) throws IOException {
		this.sourceURI = sourceURI;
		this.ts = new TokenStream(this, sourceReader, (String) null, lineno);
		return this.parse();
	}

	private ScriptOrFnNode parse() throws IOException {
		this.decompiler = this.createDecompiler(this.compilerEnv);
		this.nf = new IRFactory(this);
		this.currentScriptOrFn = this.nf.createScript();
		this.currentScope = this.currentScriptOrFn;
		int sourceStartOffset = this.decompiler.getCurrentOffset();
		this.encodedSource = null;
		this.decompiler.addToken(135);
		this.currentFlaggedToken = 0;
		this.syntaxErrorCount = 0;
		int baseLineno = this.ts.getLineno();
		Node pn = this.nf.createLeaf(128);

		int sourceEndOffset;
		try {
			while (true) {
				sourceEndOffset = this.peekToken();
				if (sourceEndOffset <= 0) {
					break;
				}

				Node msg1;
				if (sourceEndOffset == 108) {
					this.consumeToken();

					try {
						msg1 = this.function(this.calledByCompileFunction
								? 2
								: 1);
					} catch (Parser.ParserException arg6) {
						break;
					}
				} else {
					msg1 = this.statement();
				}

				this.nf.addChildToBack(pn, msg1);
			}
		} catch (StackOverflowError arg7) {
			String msg = ScriptRuntime
					.getMessage0("msg.too.deep.parser.recursion");
			throw Context.reportRuntimeError(msg, this.sourceURI,
					this.ts.getLineno(), (String) null, 0);
		}

		if (this.syntaxErrorCount != 0) {
			String sourceEndOffset1 = String.valueOf(this.syntaxErrorCount);
			sourceEndOffset1 = ScriptRuntime.getMessage1(
					"msg.got.syntax.errors", sourceEndOffset1);
			throw this.errorReporter.runtimeError(sourceEndOffset1,
					this.sourceURI, baseLineno, (String) null, 0);
		} else {
			this.currentScriptOrFn.setSourceName(this.sourceURI);
			this.currentScriptOrFn.setBaseLineno(baseLineno);
			this.currentScriptOrFn.setEndLineno(this.ts.getLineno());
			sourceEndOffset = this.decompiler.getCurrentOffset();
			this.currentScriptOrFn.setEncodedSourceBounds(sourceStartOffset,
					sourceEndOffset);
			this.nf.initScript(this.currentScriptOrFn, pn);
			if (this.compilerEnv.isGeneratingSource()) {
				this.encodedSource = this.decompiler.getEncodedSource();
			}

			this.decompiler = null;
			return this.currentScriptOrFn;
		}
	}

	private Node parseFunctionBody() throws IOException {
		++this.nestingOfFunction;
		Node pn = this.nf.createBlock(this.ts.getLineno());

		try {
			while (true) {
				int tt = this.peekToken();
				Node e;
				switch (tt) {
					case -1 :
					case 0 :
					case 85 :
						return pn;
					case 108 :
						this.consumeToken();
						e = this.function(1);
						break;
					default :
						e = this.statement();
				}

				this.nf.addChildToBack(pn, e);
			}
		} catch (Parser.ParserException arg6) {
			;
		} finally {
			--this.nestingOfFunction;
		}

		return pn;
	}

	private Node function(int functionType) throws IOException,
			Parser.ParserException {
		int syntheticType = functionType;
		int baseLineno = this.ts.getLineno();
		int functionSourceStart = this.decompiler
				.markFunctionStart(functionType);
		Node memberExprNode = null;
		String name;
		if (this.matchToken(39)) {
			name = this.ts.getString();
			this.decompiler.addName(name);
			if (!this.matchToken(86)) {
				if (this.compilerEnv.isAllowMemberExprAsFunctionName()) {
					Node nested = this.nf.createName(name);
					name = "";
					memberExprNode = this.memberExprTail(false, nested);
				}

				this.mustMatchToken(86, "msg.no.paren.parms");
			}
		} else if (this.matchToken(86)) {
			name = "";
		} else {
			name = "";
			if (this.compilerEnv.isAllowMemberExprAsFunctionName()) {
				memberExprNode = this.memberExpr(false);
			}

			this.mustMatchToken(86, "msg.no.paren.parms");
		}

		if (memberExprNode != null) {
			syntheticType = 2;
		}

		if (syntheticType != 2 && name.length() > 0) {
			this.defineSymbol(108, false, name);
		}

		boolean nested1 = this.insideFunction();
		FunctionNode fnNode = this.nf.createFunction(name);
		if (nested1 || this.nestingOfWith > 0) {
			fnNode.itsIgnoreDynamicScope = true;
		}

		int functionIndex = this.currentScriptOrFn.addFunction(fnNode);
		ScriptOrFnNode savedScriptOrFn = this.currentScriptOrFn;
		this.currentScriptOrFn = fnNode;
		Scope savedCurrentScope = this.currentScope;
		this.currentScope = fnNode;
		int savedNestingOfWith = this.nestingOfWith;
		this.nestingOfWith = 0;
		Map savedLabelSet = this.labelSet;
		this.labelSet = null;
		ObjArray savedLoopSet = this.loopSet;
		this.loopSet = null;
		ObjArray savedLoopAndSwitchSet = this.loopAndSwitchSet;
		this.loopAndSwitchSet = null;
		int savedFunctionEndFlags = this.endFlags;
		this.endFlags = 0;
		Node destructuring = null;

		int functionSourceEnd;
		Node body;
		try {
			this.decompiler.addToken(86);
			if (!this.matchToken(87)) {
				boolean pn = true;

				while (true) {
					if (!pn) {
						this.decompiler.addToken(88);
					}

					pn = false;
					int tt = this.peekToken();
					String s;
					if (tt != 82 && tt != 84) {
						this.mustMatchToken(39, "msg.no.parm");
						s = this.ts.getString();
						this.defineSymbol(86, false, s);
						this.decompiler.addName(s);
					} else {
						if (destructuring == null) {
							destructuring = new Node(88);
						}

						s = this.currentScriptOrFn.getNextTempName();
						this.defineSymbol(86, false, s);
						destructuring.addChildToBack(this.nf
								.createDestructuringAssignment(121,
										this.primaryExpr(),
										this.nf.createName(s)));
					}

					if (!this.matchToken(88)) {
						this.mustMatchToken(87, "msg.no.paren.after.parms");
						break;
					}
				}
			}

			this.decompiler.addToken(87);
			this.mustMatchToken(84, "msg.no.brace.body");
			this.decompiler.addEOL(84);
			body = this.parseFunctionBody();
			if (destructuring != null) {
				body.addChildToFront(new Node(132, destructuring, this.ts
						.getLineno()));
			}

			this.mustMatchToken(85, "msg.no.brace.after.body");
			if (this.compilerEnv.isStrictMode()
					&& !body.hasConsistentReturnUsage()) {
				String pn1 = name.length() > 0
						? "msg.no.return.value"
						: "msg.anon.no.return.value";
				this.addStrictWarning(pn1, name);
			}

			if (syntheticType == 2 && name.length() > 0
					&& this.currentScope.getSymbol(name) == null) {
				this.defineSymbol(108, false, name);
			}

			this.decompiler.addToken(85);
			functionSourceEnd = this.decompiler
					.markFunctionEnd(functionSourceStart);
			if (functionType != 2) {
				this.decompiler.addToken(1);
			}
		} finally {
			this.endFlags = savedFunctionEndFlags;
			this.loopAndSwitchSet = savedLoopAndSwitchSet;
			this.loopSet = savedLoopSet;
			this.labelSet = savedLabelSet;
			this.nestingOfWith = savedNestingOfWith;
			this.currentScriptOrFn = savedScriptOrFn;
			this.currentScope = savedCurrentScope;
		}

		fnNode.setEncodedSourceBounds(functionSourceStart, functionSourceEnd);
		fnNode.setSourceName(this.sourceURI);
		fnNode.setBaseLineno(baseLineno);
		fnNode.setEndLineno(this.ts.getLineno());
		Node pn2 = this.nf.initFunction(fnNode, functionIndex, body,
				syntheticType);
		if (memberExprNode != null) {
			pn2 = this.nf.createAssignment(89, memberExprNode, pn2);
			if (functionType != 2) {
				pn2 = this.nf.createExprStatementNoReturn(pn2, baseLineno);
			}
		}

		return pn2;
	}

	private Node statements(Node scope) throws IOException {
		Node pn = scope != null ? scope : this.nf.createBlock(this.ts
				.getLineno());

		int tt;
		while ((tt = this.peekToken()) > 0 && tt != 85) {
			this.nf.addChildToBack(pn, this.statement());
		}

		return pn;
	}

	private Node condition() throws IOException, Parser.ParserException {
		this.mustMatchToken(86, "msg.no.paren.cond");
		this.decompiler.addToken(86);
		Node pn = this.expr(false);
		this.mustMatchToken(87, "msg.no.paren.after.cond");
		this.decompiler.addToken(87);
		if (pn.getProp(19) == null
				&& (pn.getType() == 8 || pn.getType() == 35 || pn.getType() == 37)) {
			this.addStrictWarning("msg.equal.as.assign", "");
		}

		return pn;
	}

	private Node matchJumpLabelName() throws IOException,
			Parser.ParserException {
		Node label = null;
		int tt = this.peekTokenOrEOL();
		if (tt == 39) {
			this.consumeToken();
			String name = this.ts.getString();
			this.decompiler.addName(name);
			if (this.labelSet != null) {
				label = (Node) this.labelSet.get(name);
			}

			if (label == null) {
				this.reportError("msg.undef.label");
			}
		}

		return label;
	}

	private Node statement() throws IOException {
		try {
			Node lineno = this.statementHelper((Node) null);
			if (lineno != null) {
				if (this.compilerEnv.isStrictMode() && !lineno.hasSideEffects()) {
					this.addStrictWarning("msg.no.side.effects", "");
				}

				return lineno;
			}
		} catch (Parser.ParserException arg2) {
			;
		}

		int lineno1 = this.ts.getLineno();

		while (true) {
			int tt = this.peekTokenOrEOL();
			this.consumeToken();
			switch (tt) {
				case -1 :
				case 0 :
				case 1 :
				case 81 :
					return this.nf.createExprStatement(
							this.nf.createName("error"), lineno1);
			}
		}
	}

	private Node statementHelper(Node statementLabel) throws IOException,
			Parser.ParserException {
		Node pn = null;
		int tt = this.peekToken();
		int ttFlagged;
		Node nsLine;
		Node expr;
		Node lineno;
		Node arg71;
		int arg78;
		switch (tt) {
			case -1 :
			case 81 :
				this.consumeToken();
				pn = this.nf.createLeaf(127);
				return pn;
			case 4 :
			case 72 :
				pn = this.returnOrYield(tt, false);
				break;
			case 39 :
				arg78 = this.ts.getLineno();
				String arg77 = this.ts.getString();
				this.setCheckForLabel();
				pn = this.expr(false);
				if (pn.getType() == 129) {
					if (this.peekToken() != 102) {
						Kit.codeBug();
					}

					this.consumeToken();
					this.decompiler.addName(arg77);
					this.decompiler.addEOL(102);
					if (this.labelSet == null) {
						this.labelSet = new HashMap();
					} else if (this.labelSet.containsKey(arg77)) {
						this.reportError("msg.dup.label");
					}

					boolean arg76;
					if (statementLabel == null) {
						arg76 = true;
						statementLabel = pn;
					} else {
						arg76 = false;
					}

					this.labelSet.put(arg77, statementLabel);

					try {
						pn = this.statementHelper(statementLabel);
					} finally {
						this.labelSet.remove(arg77);
					}

					if (arg76) {
						pn = this.nf.createLabeledStatement(statementLabel, pn);
					}

					return pn;
				}

				pn = this.nf.createExprStatement(pn, arg78);
				break;
			case 50 :
				this.consumeToken();
				if (this.peekTokenOrEOL() == 1) {
					this.reportError("msg.bad.throw.eol");
				}

				ttFlagged = this.ts.getLineno();
				this.decompiler.addToken(50);
				pn = this.nf.createThrow(this.expr(false), ttFlagged);
				break;
			case 80 :
				this.consumeToken();
				ttFlagged = this.ts.getLineno();
				expr = null;
				lineno = null;
				this.decompiler.addToken(80);
				if (this.peekToken() != 84) {
					this.reportError("msg.no.brace.try");
				}

				this.decompiler.addEOL(84);
				nsLine = this.statement();
				this.decompiler.addEOL(85);
				expr = this.nf.createLeaf(128);
				boolean arg74 = false;
				int arg75 = this.peekToken();
				if (arg75 == 123) {
					while (this.matchToken(123)) {
						if (arg74) {
							this.reportError("msg.catch.unreachable");
						}

						this.decompiler.addToken(123);
						this.mustMatchToken(86, "msg.no.paren.catch");
						this.decompiler.addToken(86);
						this.mustMatchToken(39, "msg.bad.catchcond");
						String arg79 = this.ts.getString();
						this.decompiler.addName(arg79);
						Node catchCond = null;
						if (this.matchToken(111)) {
							this.decompiler.addToken(111);
							catchCond = this.expr(false);
						} else {
							arg74 = true;
						}

						this.mustMatchToken(87, "msg.bad.catchcond");
						this.decompiler.addToken(87);
						this.mustMatchToken(84, "msg.no.brace.catchblock");
						this.decompiler.addEOL(84);
						this.nf.addChildToBack(
								expr,
								this.nf.createCatch(arg79, catchCond,
										this.statements((Node) null),
										this.ts.getLineno()));
						this.mustMatchToken(85, "msg.no.brace.after.body");
						this.decompiler.addEOL(85);
					}
				} else if (arg75 != 124) {
					this.mustMatchToken(124, "msg.try.no.catchfinally");
				}

				if (this.matchToken(124)) {
					this.decompiler.addToken(124);
					this.decompiler.addEOL(84);
					lineno = this.statement();
					this.decompiler.addEOL(85);
				}

				pn = this.nf.createTryCatchFinally(nsLine, expr, lineno,
						ttFlagged);
				return pn;
			case 84 :
				this.consumeToken();
				if (statementLabel != null) {
					this.decompiler.addToken(84);
				}

				arg71 = this.nf.createScopeNode(128, this.ts.getLineno());
				this.pushScope(arg71);

				try {
					this.statements(arg71);
					this.mustMatchToken(85, "msg.no.brace.block");
					if (statementLabel != null) {
						this.decompiler.addEOL(85);
					}

					nsLine = arg71;
				} finally {
					this.popScope();
				}

				return nsLine;
			case 108 :
				this.consumeToken();
				pn = this.function(3);
				return pn;
			case 111 :
				this.consumeToken();
				this.decompiler.addToken(111);
				ttFlagged = this.ts.getLineno();
				nsLine = this.condition();
				this.decompiler.addEOL(84);
				expr = this.statement();
				lineno = null;
				if (this.matchToken(112)) {
					this.decompiler.addToken(85);
					this.decompiler.addToken(112);
					this.decompiler.addEOL(84);
					lineno = this.statement();
				}

				this.decompiler.addEOL(85);
				pn = this.nf.createIf(nsLine, expr, lineno, ttFlagged);
				return pn;
			case 113 :
				this.consumeToken();
				this.decompiler.addToken(113);
				ttFlagged = this.ts.getLineno();
				this.mustMatchToken(86, "msg.no.paren.switch");
				this.decompiler.addToken(86);
				pn = this.enterSwitch(this.expr(false), ttFlagged);

				try {
					this.mustMatchToken(87, "msg.no.paren.after.switch");
					this.decompiler.addToken(87);
					this.mustMatchToken(84, "msg.no.brace.switch");
					this.decompiler.addEOL(84);
					boolean arg73 = false;

					label2502 : while (true) {
						tt = this.nextToken();
						switch (tt) {
							case 85 :
								break label2502;
							case 114 :
								this.decompiler.addToken(114);
								expr = this.expr(false);
								this.mustMatchToken(102, "msg.no.colon.case");
								this.decompiler.addEOL(102);
								break;
							case 115 :
								if (arg73) {
									this.reportError("msg.double.switch.default");
								}

								this.decompiler.addToken(115);
								arg73 = true;
								expr = null;
								this.mustMatchToken(102, "msg.no.colon.case");
								this.decompiler.addEOL(102);
								break;
							default :
								this.reportError("msg.bad.switch");
								break label2502;
						}

						lineno = this.nf.createLeaf(128);

						while ((tt = this.peekToken()) != 85 && tt != 114
								&& tt != 115 && tt != 0) {
							this.nf.addChildToBack(lineno, this.statement());
						}

						this.nf.addSwitchCase(pn, expr, lineno);
					}

					this.decompiler.addEOL(85);
					this.nf.closeSwitch(pn);
				} finally {
					this.exitSwitch();
				}

				return pn;
			case 115 :
				this.consumeToken();
				this.mustHaveXML();
				this.decompiler.addToken(115);
				int arg72 = this.ts.getLineno();
				if (!this.matchToken(39) || !this.ts.getString().equals("xml")) {
					this.reportError("msg.bad.namespace");
				}

				this.decompiler.addName(" xml");
				if (!this.matchToken(39)
						|| !this.ts.getString().equals("namespace")) {
					this.reportError("msg.bad.namespace");
				}

				this.decompiler.addName(" namespace");
				if (!this.matchToken(89)) {
					this.reportError("msg.bad.namespace");
				}

				this.decompiler.addToken(89);
				expr = this.expr(false);
				pn = this.nf.createDefaultNamespace(expr, arg72);
				break;
			case 116 :
				this.consumeToken();
				this.decompiler.addToken(116);
				arg71 = this.enterLoop(statementLabel, true);

				try {
					nsLine = this.condition();
					this.decompiler.addEOL(84);
					expr = this.statement();
					this.decompiler.addEOL(85);
					pn = this.nf.createWhile(arg71, nsLine, expr);
				} finally {
					this.exitLoop(true);
				}

				return pn;
			case 117 :
				this.consumeToken();
				this.decompiler.addToken(117);
				this.decompiler.addEOL(84);
				arg71 = this.enterLoop(statementLabel, true);

				try {
					nsLine = this.statement();
					this.decompiler.addToken(85);
					this.mustMatchToken(116, "msg.no.while.do");
					this.decompiler.addToken(116);
					expr = this.condition();
					pn = this.nf.createDoWhile(arg71, nsLine, expr);
				} finally {
					this.exitLoop(true);
				}

				this.matchToken(81);
				this.decompiler.addEOL(81);
				return pn;
			case 118 :
				this.consumeToken();
				boolean arg70 = false;
				this.decompiler.addToken(118);
				nsLine = this.enterLoop(statementLabel, true);

				try {
					Node name = null;
					int varName = -1;
					if (this.matchToken(39)) {
						this.decompiler.addName(this.ts.getString());
						if (this.ts.getString().equals("each")) {
							arg70 = true;
						} else {
							this.reportError("msg.no.paren.for");
						}
					}

					this.mustMatchToken(86, "msg.no.paren.for");
					this.decompiler.addToken(86);
					tt = this.peekToken();
					if (tt == 81) {
						expr = this.nf.createLeaf(127);
					} else if (tt != 121 && tt != 152) {
						expr = this.expr(true);
					} else {
						this.consumeToken();
						this.decompiler.addToken(tt);
						expr = this.variables(true, tt);
						varName = tt;
					}

					if (this.matchToken(52)) {
						this.decompiler.addToken(52);
						lineno = this.expr(false);
					} else {
						this.mustMatchToken(81, "msg.no.semi.for");
						this.decompiler.addToken(81);
						if (this.peekToken() == 81) {
							lineno = this.nf.createLeaf(127);
						} else {
							lineno = this.expr(false);
						}

						this.mustMatchToken(81, "msg.no.semi.for.cond");
						this.decompiler.addToken(81);
						if (this.peekToken() == 87) {
							name = this.nf.createLeaf(127);
						} else {
							name = this.expr(false);
						}
					}

					this.mustMatchToken(87, "msg.no.paren.for.ctrl");
					this.decompiler.addToken(87);
					this.decompiler.addEOL(84);
					Node firstLabel = this.statement();
					this.decompiler.addEOL(85);
					if (name == null) {
						pn = this.nf.createForIn(varName, nsLine, expr, lineno,
								firstLabel, arg70);
					} else {
						pn = this.nf.createFor(nsLine, expr, lineno, name,
								firstLabel);
					}
				} finally {
					this.exitLoop(true);
				}

				return pn;
			case 119 :
				this.consumeToken();
				ttFlagged = this.ts.getLineno();
				this.decompiler.addToken(119);
				nsLine = this.matchJumpLabelName();
				if (nsLine == null) {
					if (this.loopAndSwitchSet == null
							|| this.loopAndSwitchSet.size() == 0) {
						this.reportError("msg.bad.break");
						return null;
					}

					nsLine = (Node) this.loopAndSwitchSet.peek();
				}

				pn = this.nf.createBreak(nsLine, ttFlagged);
				break;
			case 120 :
				this.consumeToken();
				ttFlagged = this.ts.getLineno();
				this.decompiler.addToken(120);
				expr = this.matchJumpLabelName();
				if (expr == null) {
					if (this.loopSet == null || this.loopSet.size() == 0) {
						this.reportError("msg.continue.outside");
						return null;
					}

					nsLine = (Node) this.loopSet.peek();
				} else {
					nsLine = this.nf.getLabelLoop(expr);
					if (nsLine == null) {
						this.reportError("msg.continue.nonloop");
						return null;
					}
				}

				pn = this.nf.createContinue(nsLine, ttFlagged);
				break;
			case 121 :
			case 153 :
				this.consumeToken();
				this.decompiler.addToken(tt);
				pn = this.variables(false, tt);
				break;
			case 122 :
				this.consumeToken();
				this.decompiler.addToken(122);
				ttFlagged = this.ts.getLineno();
				this.mustMatchToken(86, "msg.no.paren.with");
				this.decompiler.addToken(86);
				nsLine = this.expr(false);
				this.mustMatchToken(87, "msg.no.paren.after.with");
				this.decompiler.addToken(87);
				this.decompiler.addEOL(84);
				++this.nestingOfWith;

				try {
					expr = this.statement();
				} finally {
					--this.nestingOfWith;
				}

				this.decompiler.addEOL(85);
				pn = this.nf.createWith(nsLine, expr, ttFlagged);
				return pn;
			case 152 :
				this.consumeToken();
				this.decompiler.addToken(152);
				if (this.peekToken() == 86) {
					return this.let(true);
				}

				pn = this.variables(false, tt);
				if (this.peekToken() != 81) {
					return pn;
				}
				break;
			case 159 :
				this.consumeToken();
				this.decompiler.addToken(159);
				pn = this.nf.createDebugger(this.ts.getLineno());
				break;
			default :
				arg78 = this.ts.getLineno();
				pn = this.expr(false);
				pn = this.nf.createExprStatement(pn, arg78);
		}

		ttFlagged = this.peekFlaggedToken();
		switch (ttFlagged & 65535) {
			case -1 :
			case 0 :
			case 85 :
				break;
			case 81 :
				this.consumeToken();
				break;
			default :
				if ((ttFlagged & 65536) == 0) {
					this.reportError("msg.no.semi.stmt");
				}
		}

		this.decompiler.addEOL(81);
		return pn;
	}

	private static final boolean nowAllSet(int before, int after, int mask) {
		return (before & mask) != mask && (after & mask) == mask;
	}

	private Node returnOrYield(int tt, boolean exprContext) throws IOException,
			Parser.ParserException {
		if (!this.insideFunction()) {
			this.reportError(tt == 4 ? "msg.bad.return" : "msg.bad.yield");
		}

		this.consumeToken();
		this.decompiler.addToken(tt);
		int lineno = this.ts.getLineno();
		Node e;
		switch (this.peekTokenOrEOL()) {
			case -1 :
			case 0 :
			case 1 :
			case 72 :
			case 81 :
			case 83 :
			case 85 :
			case 87 :
				e = null;
				break;
			default :
				e = this.expr(false);
		}

		int before = this.endFlags;
		Node ret;
		if (tt == 4) {
			if (e == null) {
				this.endFlags |= 2;
			} else {
				this.endFlags |= 4;
			}

			ret = this.nf.createReturn(e, lineno);
			if (nowAllSet(before, this.endFlags, 6)) {
				this.addStrictWarning("msg.return.inconsistent", "");
			}
		} else {
			this.endFlags |= 8;
			ret = this.nf.createYield(e, lineno);
			if (!exprContext) {
				ret = new Node(132, ret, lineno);
			}
		}

		if (nowAllSet(before, this.endFlags, 12)) {
			String name = ((FunctionNode) this.currentScriptOrFn)
					.getFunctionName();
			if (name.length() == 0) {
				this.addError("msg.anon.generator.returns", "");
			} else {
				this.addError("msg.generator.returns", name);
			}
		}

		return ret;
	}

	private Node variables(boolean inFor, int declType) throws IOException,
			Parser.ParserException {
		Node result = this.nf.createVariables(declType, this.ts.getLineno());
		boolean first = true;

		do {
			Node destructuring = null;
			String s = null;
			int tt = this.peekToken();
			if (tt != 82 && tt != 84) {
				this.mustMatchToken(39, "msg.bad.var");
				s = this.ts.getString();
				if (!first) {
					this.decompiler.addToken(88);
				}

				first = false;
				this.decompiler.addName(s);
				this.defineSymbol(declType, inFor, s);
			} else {
				destructuring = this.primaryExpr();
			}

			Node init = null;
			if (this.matchToken(89)) {
				this.decompiler.addToken(89);
				init = this.assignExpr(inFor);
			}

			if (destructuring != null) {
				if (init == null) {
					if (!inFor) {
						this.reportError("msg.destruct.assign.no.init");
					}

					this.nf.addChildToBack(result, destructuring);
				} else {
					this.nf.addChildToBack(result, this.nf
							.createDestructuringAssignment(declType,
									destructuring, init));
				}
			} else {
				Node name = this.nf.createName(s);
				if (init != null) {
					this.nf.addChildToBack(name, init);
				}

				this.nf.addChildToBack(result, name);
			}
		} while (this.matchToken(88));

		return result;
	}

	private Node let(boolean isStatement) throws IOException,
			Parser.ParserException {
		this.mustMatchToken(86, "msg.no.paren.after.let");
		this.decompiler.addToken(86);
		Node result = this.nf.createScopeNode(152, this.ts.getLineno());
		this.pushScope(result);

		try {
			Node vars = this.variables(false, 152);
			this.nf.addChildToBack(result, vars);
			this.mustMatchToken(87, "msg.no.paren.let");
			this.decompiler.addToken(87);
			if (isStatement && this.peekToken() == 84) {
				this.consumeToken();
				this.decompiler.addEOL(84);
				this.nf.addChildToBack(result, this.statements((Node) null));
				this.mustMatchToken(85, "msg.no.curly.let");
				this.decompiler.addToken(85);
			} else {
				result.setType(157);
				this.nf.addChildToBack(result, this.expr(false));
				if (isStatement) {
					result = this.nf.createExprStatement(result,
							this.ts.getLineno());
				}
			}
		} finally {
			this.popScope();
		}

		return result;
	}

	void defineSymbol(int declType, boolean ignoreNotInBlock, String name) {
		Scope definingScope = this.currentScope.getDefiningScope(name);
		Symbol symbol = definingScope != null
				? definingScope.getSymbol(name)
				: null;
		boolean error = false;
		if (symbol != null && (symbol.declType == 153 || declType == 153)) {
			error = true;
		} else {
			switch (declType) {
				case 86 :
					if (symbol != null) {
						this.addWarning("msg.dup.parms", name);
					}

					this.currentScriptOrFn.putSymbol(name, new Symbol(declType,
							name));
					break;
				case 108 :
				case 121 :
				case 153 :
					if (symbol != null) {
						if (symbol.declType == 121) {
							this.addStrictWarning("msg.var.redecl", name);
						} else if (symbol.declType == 86) {
							this.addStrictWarning("msg.var.hides.arg", name);
						}
					} else {
						this.currentScriptOrFn.putSymbol(name, new Symbol(
								declType, name));
					}
					break;
				case 152 :
					if (symbol != null && definingScope == this.currentScope) {
						error = symbol.declType == 152;
					}

					int currentScopeType = this.currentScope.getType();
					if (!ignoreNotInBlock
							&& (currentScopeType == 131 || currentScopeType == 111)) {
						this.addError("msg.let.decl.not.in.block");
					}

					this.currentScope.putSymbol(name,
							new Symbol(declType, name));
					break;
				default :
					throw Kit.codeBug();
			}
		}

		if (error) {
			this.addError(symbol.declType == 153
					? "msg.const.redecl"
					: (symbol.declType == 152
							? "msg.let.redecl"
							: (symbol.declType == 121
									? "msg.var.redecl"
									: (symbol.declType == 108
											? "msg.fn.redecl"
											: "msg.parm.redecl"))), name);
		}

	}

	private Node expr(boolean inForInit) throws IOException,
			Parser.ParserException {
		Node pn;
		for (pn = this.assignExpr(inForInit); this.matchToken(88); pn = this.nf
				.createBinary(88, pn, this.assignExpr(inForInit))) {
			this.decompiler.addToken(88);
			if (this.compilerEnv.isStrictMode() && !pn.hasSideEffects()) {
				this.addStrictWarning("msg.no.side.effects", "");
			}

			if (this.peekToken() == 72) {
				this.reportError("msg.yield.parenthesized");
			}
		}

		return pn;
	}

	private Node assignExpr(boolean inForInit) throws IOException,
			Parser.ParserException {
		int tt = this.peekToken();
		if (tt == 72) {
			this.consumeToken();
			return this.returnOrYield(tt, true);
		} else {
			Node pn = this.condExpr(inForInit);
			tt = this.peekToken();
			if (89 <= tt && tt <= 100) {
				this.consumeToken();
				this.decompiler.addToken(tt);
				pn = this.nf.createAssignment(tt, pn,
						this.assignExpr(inForInit));
			}

			return pn;
		}
	}

	private Node condExpr(boolean inForInit) throws IOException,
			Parser.ParserException {
		Node pn = this.orExpr(inForInit);
		if (this.matchToken(101)) {
			this.decompiler.addToken(101);
			Node ifTrue = this.assignExpr(false);
			this.mustMatchToken(102, "msg.no.colon.cond");
			this.decompiler.addToken(102);
			Node ifFalse = this.assignExpr(inForInit);
			return this.nf.createCondExpr(pn, ifTrue, ifFalse);
		} else {
			return pn;
		}
	}

	private Node orExpr(boolean inForInit) throws IOException,
			Parser.ParserException {
		Node pn = this.andExpr(inForInit);
		if (this.matchToken(103)) {
			this.decompiler.addToken(103);
			pn = this.nf.createBinary(103, pn, this.orExpr(inForInit));
		}

		return pn;
	}

	private Node andExpr(boolean inForInit) throws IOException,
			Parser.ParserException {
		Node pn = this.bitOrExpr(inForInit);
		if (this.matchToken(104)) {
			this.decompiler.addToken(104);
			pn = this.nf.createBinary(104, pn, this.andExpr(inForInit));
		}

		return pn;
	}

	private Node bitOrExpr(boolean inForInit) throws IOException,
			Parser.ParserException {
		Node pn;
		for (pn = this.bitXorExpr(inForInit); this.matchToken(9); pn = this.nf
				.createBinary(9, pn, this.bitXorExpr(inForInit))) {
			this.decompiler.addToken(9);
		}

		return pn;
	}

	private Node bitXorExpr(boolean inForInit) throws IOException,
			Parser.ParserException {
		Node pn;
		for (pn = this.bitAndExpr(inForInit); this.matchToken(10); pn = this.nf
				.createBinary(10, pn, this.bitAndExpr(inForInit))) {
			this.decompiler.addToken(10);
		}

		return pn;
	}

	private Node bitAndExpr(boolean inForInit) throws IOException,
			Parser.ParserException {
		Node pn;
		for (pn = this.eqExpr(inForInit); this.matchToken(11); pn = this.nf
				.createBinary(11, pn, this.eqExpr(inForInit))) {
			this.decompiler.addToken(11);
		}

		return pn;
	}

	private Node eqExpr(boolean inForInit) throws IOException,
			Parser.ParserException {
		Node pn = this.relExpr(inForInit);

		while (true) {
			int tt = this.peekToken();
			switch (tt) {
				case 12 :
				case 13 :
				case 46 :
				case 47 :
					this.consumeToken();
					int decompilerToken = tt;
					int parseToken = tt;
					if (this.compilerEnv.getLanguageVersion() == 120) {
						switch (tt) {
							case 12 :
								parseToken = 46;
								break;
							case 13 :
								parseToken = 47;
								break;
							case 46 :
								decompilerToken = 12;
								break;
							case 47 :
								decompilerToken = 13;
						}
					}

					this.decompiler.addToken(decompilerToken);
					pn = this.nf.createBinary(parseToken, pn,
							this.relExpr(inForInit));
					break;
				default :
					return pn;
			}
		}
	}

	private Node relExpr(boolean inForInit) throws IOException,
			Parser.ParserException {
		Node pn = this.shiftExpr();

		while (true) {
			int tt = this.peekToken();
			switch (tt) {
				case 52 :
					if (inForInit) {
						return pn;
					}
				case 14 :
				case 15 :
				case 16 :
				case 17 :
				case 53 :
					this.consumeToken();
					this.decompiler.addToken(tt);
					pn = this.nf.createBinary(tt, pn, this.shiftExpr());
					break;
				default :
					return pn;
			}
		}
	}

	private Node shiftExpr() throws IOException, Parser.ParserException {
		Node pn = this.addExpr();

		while (true) {
			int tt = this.peekToken();
			switch (tt) {
				case 18 :
				case 19 :
				case 20 :
					this.consumeToken();
					this.decompiler.addToken(tt);
					pn = this.nf.createBinary(tt, pn, this.addExpr());
					break;
				default :
					return pn;
			}
		}
	}

	private Node addExpr() throws IOException, Parser.ParserException {
		Node pn = this.mulExpr();

		while (true) {
			int tt = this.peekToken();
			if (tt != 21 && tt != 22) {
				return pn;
			}

			this.consumeToken();
			this.decompiler.addToken(tt);
			pn = this.nf.createBinary(tt, pn, this.mulExpr());
		}
	}

	private Node mulExpr() throws IOException, Parser.ParserException {
		Node pn = this.unaryExpr();

		while (true) {
			int tt = this.peekToken();
			switch (tt) {
				case 23 :
				case 24 :
				case 25 :
					this.consumeToken();
					this.decompiler.addToken(tt);
					pn = this.nf.createBinary(tt, pn, this.unaryExpr());
					break;
				default :
					return pn;
			}
		}
	}

	private Node unaryExpr() throws IOException, Parser.ParserException {
		int tt = this.peekToken();
		Node pn;
		switch (tt) {
			case -1 :
				this.consumeToken();
				return this.nf.createName("error");
			case 14 :
				if (this.compilerEnv.isXmlAvailable()) {
					this.consumeToken();
					pn = this.xmlInitializer();
					return this.memberExprTail(true, pn);
				}
			default :
				pn = this.memberExpr(true);
				tt = this.peekTokenOrEOL();
				if (tt != 105 && tt != 106) {
					return pn;
				}

				this.consumeToken();
				this.decompiler.addToken(tt);
				return this.nf.createIncDec(tt, true, pn);
			case 21 :
				this.consumeToken();
				this.decompiler.addToken(28);
				return this.nf.createUnary(28, this.unaryExpr());
			case 22 :
				this.consumeToken();
				this.decompiler.addToken(29);
				return this.nf.createUnary(29, this.unaryExpr());
			case 26 :
			case 27 :
			case 32 :
			case 125 :
				this.consumeToken();
				this.decompiler.addToken(tt);
				return this.nf.createUnary(tt, this.unaryExpr());
			case 31 :
				this.consumeToken();
				this.decompiler.addToken(31);
				return this.nf.createUnary(31, this.unaryExpr());
			case 105 :
			case 106 :
				this.consumeToken();
				this.decompiler.addToken(tt);
				return this.nf.createIncDec(tt, false, this.memberExpr(true));
		}
	}

	private Node xmlInitializer() throws IOException {
		int tt = this.ts.getFirstXMLToken();
		if (tt != 144 && tt != 147) {
			this.reportError("msg.syntax");
			return null;
		} else {
			Node pnXML = this.nf.createLeaf(30);
			String xml = this.ts.getString();
			boolean fAnonymous = xml.trim().startsWith("<>");
			Node pn = this.nf.createName(fAnonymous ? "XMLList" : "XML");
			this.nf.addChildToBack(pnXML, pn);
			pn = null;

			while (true) {
				switch (tt) {
					case 144 :
						xml = this.ts.getString();
						this.decompiler.addName(xml);
						this.mustMatchToken(84, "msg.syntax");
						this.decompiler.addToken(84);
						Node expr = this.peekToken() == 85 ? this.nf
								.createString("") : this.expr(false);
						this.mustMatchToken(85, "msg.syntax");
						this.decompiler.addToken(85);
						if (pn == null) {
							pn = this.nf.createString(xml);
						} else {
							pn = this.nf.createBinary(21, pn,
									this.nf.createString(xml));
						}

						if (this.ts.isXMLAttribute()) {
							expr = this.nf.createUnary(74, expr);
							Node prepend = this.nf.createBinary(21,
									this.nf.createString("\""), expr);
							expr = this.nf.createBinary(21, prepend,
									this.nf.createString("\""));
						} else {
							expr = this.nf.createUnary(75, expr);
						}

						pn = this.nf.createBinary(21, pn, expr);
						tt = this.ts.getNextXMLToken();
						break;
					case 147 :
						xml = this.ts.getString();
						this.decompiler.addName(xml);
						if (pn == null) {
							pn = this.nf.createString(xml);
						} else {
							pn = this.nf.createBinary(21, pn,
									this.nf.createString(xml));
						}

						this.nf.addChildToBack(pnXML, pn);
						return pnXML;
					default :
						this.reportError("msg.syntax");
						return null;
				}
			}
		}
	}

	private void argumentList(Node listNode) throws IOException,
			Parser.ParserException {
		boolean matched = this.matchToken(87);
		if (!matched) {
			boolean first = true;

			do {
				if (!first) {
					this.decompiler.addToken(88);
				}

				first = false;
				if (this.peekToken() == 72) {
					this.reportError("msg.yield.parenthesized");
				}

				this.nf.addChildToBack(listNode, this.assignExpr(false));
			} while (this.matchToken(88));

			this.mustMatchToken(87, "msg.no.paren.arg");
		}

		this.decompiler.addToken(87);
	}

	private Node memberExpr(boolean allowCallSyntax) throws IOException,
			Parser.ParserException {
		int tt = this.peekToken();
		Node pn;
		if (tt == 30) {
			this.consumeToken();
			this.decompiler.addToken(30);
			pn = this.nf.createCallOrNew(30, this.memberExpr(false));
			if (this.matchToken(86)) {
				this.decompiler.addToken(86);
				this.argumentList(pn);
			}

			tt = this.peekToken();
			if (tt == 84) {
				this.nf.addChildToBack(pn, this.primaryExpr());
			}
		} else {
			pn = this.primaryExpr();
		}

		return this.memberExprTail(allowCallSyntax, pn);
	}

	private Node memberExprTail(boolean allowCallSyntax, Node pn)
			throws IOException, Parser.ParserException {
		while (true) {
			int tt = this.peekToken();
			switch (tt) {
				case 82 :
					this.consumeToken();
					this.decompiler.addToken(82);
					pn = this.nf.createElementGet(pn, (String) null,
							this.expr(false), 0);
					this.mustMatchToken(83, "msg.no.bracket.index");
					this.decompiler.addToken(83);
					break;
				case 86 :
					if (allowCallSyntax) {
						this.consumeToken();
						this.decompiler.addToken(86);
						pn = this.nf.createCallOrNew(38, pn);
						this.argumentList(pn);
						break;
					}
				default :
					return pn;
				case 107 :
				case 142 :
					this.consumeToken();
					this.decompiler.addToken(tt);
					byte memberTypeFlags = 0;
					if (tt == 142) {
						this.mustHaveXML();
						memberTypeFlags = 4;
					}

					String s;
					if (!this.compilerEnv.isXmlAvailable()) {
						this.mustMatchToken(39, "msg.no.name.after.dot");
						s = this.ts.getString();
						this.decompiler.addName(s);
						pn = this.nf.createPropertyGet(pn, (String) null, s,
								memberTypeFlags);
					} else {
						tt = this.nextToken();
						switch (tt) {
							case 23 :
								this.decompiler.addName("*");
								pn = this
										.propertyName(pn, "*", memberTypeFlags);
								continue;
							case 39 :
								s = this.ts.getString();
								this.decompiler.addName(s);
								pn = this.propertyName(pn, s, memberTypeFlags);
								continue;
							case 50 :
								this.decompiler.addName("throw");
								pn = this.propertyName(pn, "throw",
										memberTypeFlags);
								continue;
							case 146 :
								this.decompiler.addToken(146);
								pn = this.attributeAccess(pn, memberTypeFlags);
								continue;
							default :
								this.reportError("msg.no.name.after.dot");
						}
					}
					break;
				case 145 :
					this.consumeToken();
					this.mustHaveXML();
					this.decompiler.addToken(145);
					pn = this.nf.createDotQuery(pn, this.expr(false),
							this.ts.getLineno());
					this.mustMatchToken(87, "msg.no.paren");
					this.decompiler.addToken(87);
			}
		}
	}

	private Node attributeAccess(Node pn, int memberTypeFlags)
			throws IOException {
		memberTypeFlags |= 2;
		int tt = this.nextToken();
		switch (tt) {
			case 23 :
				this.decompiler.addName("*");
				pn = this.propertyName(pn, "*", memberTypeFlags);
				break;
			case 39 :
				String s = this.ts.getString();
				this.decompiler.addName(s);
				pn = this.propertyName(pn, s, memberTypeFlags);
				break;
			case 82 :
				this.decompiler.addToken(82);
				pn = this.nf.createElementGet(pn, (String) null,
						this.expr(false), memberTypeFlags);
				this.mustMatchToken(83, "msg.no.bracket.index");
				this.decompiler.addToken(83);
				break;
			default :
				this.reportError("msg.no.name.after.xmlAttr");
				pn = this.nf.createPropertyGet(pn, (String) null, "?",
						memberTypeFlags);
		}

		return pn;
	}

	private Node propertyName(Node pn, String name, int memberTypeFlags)
			throws IOException, Parser.ParserException {
		String namespace = null;
		if (this.matchToken(143)) {
			this.decompiler.addToken(143);
			namespace = name;
			int tt = this.nextToken();
			switch (tt) {
				case 23 :
					this.decompiler.addName("*");
					name = "*";
					break;
				case 39 :
					name = this.ts.getString();
					this.decompiler.addName(name);
					break;
				case 82 :
					this.decompiler.addToken(82);
					pn = this.nf.createElementGet(pn, name, this.expr(false),
							memberTypeFlags);
					this.mustMatchToken(83, "msg.no.bracket.index");
					this.decompiler.addToken(83);
					return pn;
				default :
					this.reportError("msg.no.name.after.coloncolon");
					name = "?";
			}
		}

		pn = this.nf.createPropertyGet(pn, namespace, name, memberTypeFlags);
		return pn;
	}

	private Node arrayComprehension(String arrayName, Node expr)
			throws IOException, Parser.ParserException {
		if (this.nextToken() != 118) {
			throw Kit.codeBug();
		} else {
			this.decompiler.addName(" ");
			this.decompiler.addToken(118);
			boolean isForEach = false;
			if (this.matchToken(39)) {
				this.decompiler.addName(this.ts.getString());
				if (this.ts.getString().equals("each")) {
					isForEach = true;
				} else {
					this.reportError("msg.no.paren.for");
				}
			}

			this.mustMatchToken(86, "msg.no.paren.for");
			this.decompiler.addToken(86);
			int tt = this.peekToken();
			String name;
			if (tt != 82 && tt != 84) {
				if (tt != 39) {
					this.reportError("msg.bad.var");
					return this.nf.createNumber(0.0D);
				}

				this.consumeToken();
				name = this.ts.getString();
				this.decompiler.addName(name);
			} else {
				name = this.currentScriptOrFn.getNextTempName();
				this.defineSymbol(86, false, name);
				expr = this.nf.createBinary(88, this.nf.createAssignment(89,
						this.primaryExpr(), this.nf.createName(name)), expr);
			}

			Node init = this.nf.createName(name);
			this.defineSymbol(152, false, name);
			this.mustMatchToken(52, "msg.in.after.for.name");
			this.decompiler.addToken(52);
			Node iterator = this.expr(false);
			this.mustMatchToken(87, "msg.no.paren.for.ctrl");
			this.decompiler.addToken(87);
			tt = this.peekToken();
			Node body;
			Node loop;
			if (tt == 118) {
				body = this.arrayComprehension(arrayName, expr);
			} else {
				loop = this.nf.createCallOrNew(38,
						this.nf.createPropertyGet(
								this.nf.createName(arrayName), (String) null,
								"push", 0));
				loop.addChildToBack(expr);
				body = new Node(132, loop, this.ts.getLineno());
				if (tt == 111) {
					this.consumeToken();
					this.decompiler.addToken(111);
					int lineno = this.ts.getLineno();
					Node cond = this.condition();
					body = this.nf.createIf(cond, body, (Node) null, lineno);
				}

				this.mustMatchToken(83, "msg.no.bracket.arg");
				this.decompiler.addToken(83);
			}

			loop = this.enterLoop((Node) null, true);

			Node lineno1;
			try {
				lineno1 = this.nf.createForIn(152, loop, init, iterator, body,
						isForEach);
			} finally {
				this.exitLoop(false);
			}

			return lineno1;
		}
	}

	private Node primaryExpr() throws IOException, Parser.ParserException {
		int ttFlagged = this.nextFlaggedToken();
		int tt = ttFlagged & '￿';
		Node pn;
		ObjArray flags;
		String arg16;
		int arg21;
		switch (tt) {
			case -1 :
				break;
			case 0 :
				this.reportError("msg.unexpected.eof");
				break;
			case 24 :
			case 99 :
				this.ts.readRegExp(tt);
				arg16 = this.ts.regExpFlags;
				this.ts.regExpFlags = null;
				String arg19 = this.ts.getString();
				this.decompiler.addRegexp(arg19, arg16);
				arg21 = this.currentScriptOrFn.addRegexp(arg19, arg16);
				return this.nf.createRegExp(arg21);
			case 39 :
				arg16 = this.ts.getString();
				if ((ttFlagged & 131072) != 0 && this.peekToken() == 102) {
					return this.nf.createLabel(this.ts.getLineno());
				}

				this.decompiler.addName(arg16);
				if (this.compilerEnv.isXmlAvailable()) {
					pn = this.propertyName((Node) null, arg16, 0);
				} else {
					pn = this.nf.createName(arg16);
				}

				return pn;
			case 40 :
				double arg17 = this.ts.getNumber();
				this.decompiler.addNumber(arg17);
				return this.nf.createNumber(arg17);
			case 41 :
				arg16 = this.ts.getString();
				this.decompiler.addString(arg16);
				return this.nf.createString(arg16);
			case 42 :
			case 43 :
			case 44 :
			case 45 :
				this.decompiler.addToken(tt);
				return this.nf.createLeaf(tt);
			case 82 :
				flags = new ObjArray();
				int arg18 = 0;
				boolean arg20 = false;
				this.decompiler.addToken(82);
				boolean arg22 = true;

				while (true) {
					while (true) {
						tt = this.peekToken();
						if (tt == 88) {
							this.consumeToken();
							this.decompiler.addToken(88);
							if (!arg22) {
								arg22 = true;
							} else {
								flags.add((Object) null);
								++arg18;
							}
						} else {
							if (tt == 83) {
								this.consumeToken();
								this.decompiler.addToken(83);
								arg21 = flags.size() + (arg22 ? 1 : 0);
								return this.nf.createArrayLiteral(flags, arg18,
										arg21);
							}

							if (arg18 == 0 && flags.size() == 1 && tt == 118) {
								Node arg23 = this.nf.createScopeNode(156,
										this.ts.getLineno());
								String tempName = this.currentScriptOrFn
										.getNextTempName();
								this.pushScope(arg23);

								Node arg12;
								try {
									this.defineSymbol(152, false, tempName);
									Node expr = (Node) flags.get(0);
									Node block = this.nf.createBlock(this.ts
											.getLineno());
									Node init = new Node(
											132,
											this.nf.createAssignment(
													89,
													this.nf.createName(tempName),
													this.nf.createCallOrNew(
															30,
															this.nf.createName("Array"))),
											this.ts.getLineno());
									block.addChildToBack(init);
									block.addChildToBack(this
											.arrayComprehension(tempName, expr));
									arg23.addChildToBack(block);
									arg23.addChildToBack(this.nf
											.createName(tempName));
									arg12 = arg23;
								} finally {
									this.popScope();
								}

								return arg12;
							}

							if (!arg22) {
								this.reportError("msg.no.bracket.arg");
							}

							flags.add(this.assignExpr(false));
							arg22 = false;
						}
					}
				}
			case 84 :
				flags = new ObjArray();
				this.decompiler.addToken(84);
				if (!this.matchToken(85)) {
					boolean re = true;

					label242 : do {
						if (!re) {
							this.decompiler.addToken(88);
						} else {
							re = false;
						}

						tt = this.peekToken();
						Object index;
						switch (tt) {
							case 39 :
							case 41 :
								this.consumeToken();
								String s = this.ts.getString();
								if (tt == 39) {
									if (s.equals("get")
											&& this.peekToken() == 39) {
										this.decompiler.addToken(150);
										this.consumeToken();
										s = this.ts.getString();
										this.decompiler.addName(s);
										index = ScriptRuntime.getIndexObject(s);
										if (!this.getterSetterProperty(flags,
												index, true)) {
											break label242;
										}
										break;
									}

									if (s.equals("set")
											&& this.peekToken() == 39) {
										this.decompiler.addToken(151);
										this.consumeToken();
										s = this.ts.getString();
										this.decompiler.addName(s);
										index = ScriptRuntime.getIndexObject(s);
										if (!this.getterSetterProperty(flags,
												index, false)) {
											break label242;
										}
										break;
									}

									this.decompiler.addName(s);
								} else {
									this.decompiler.addString(s);
								}

								index = ScriptRuntime.getIndexObject(s);
								this.plainProperty(flags, index);
								break;
							case 40 :
								this.consumeToken();
								double n = this.ts.getNumber();
								this.decompiler.addNumber(n);
								index = ScriptRuntime.getIndexObject(n);
								this.plainProperty(flags, index);
								break;
							case 85 :
								break label242;
							default :
								this.reportError("msg.bad.prop");
								break label242;
						}
					} while (this.matchToken(88));

					this.mustMatchToken(85, "msg.no.brace.prop");
				}

				this.decompiler.addToken(85);
				return this.nf.createObjectLiteral(flags);
			case 86 :
				this.decompiler.addToken(86);
				pn = this.expr(false);
				pn.putProp(19, Boolean.TRUE);
				this.decompiler.addToken(87);
				this.mustMatchToken(87, "msg.no.paren");
				return pn;
			case 108 :
				return this.function(2);
			case 126 :
				this.reportError("msg.reserved.id");
				break;
			case 146 :
				this.mustHaveXML();
				this.decompiler.addToken(146);
				pn = this.attributeAccess((Node) null, 0);
				return pn;
			case 152 :
				this.decompiler.addToken(152);
				return this.let(false);
			default :
				this.reportError("msg.syntax");
		}

		return null;
	}

	private void plainProperty(ObjArray elems, Object property)
			throws IOException {
		this.mustMatchToken(102, "msg.no.colon.prop");
		this.decompiler.addToken(66);
		elems.add(property);
		elems.add(this.assignExpr(false));
	}

	private boolean getterSetterProperty(ObjArray elems, Object property,
			boolean isGetter) throws IOException {
		Node f = this.function(2);
		if (f.getType() != 108) {
			this.reportError("msg.bad.prop");
			return false;
		} else {
			int fnIndex = f.getExistingIntProp(1);
			FunctionNode fn = this.currentScriptOrFn.getFunctionNode(fnIndex);
			if (fn.getFunctionName().length() != 0) {
				this.reportError("msg.bad.prop");
				return false;
			} else {
				elems.add(property);
				if (isGetter) {
					elems.add(this.nf.createUnary(150, f));
				} else {
					elems.add(this.nf.createUnary(151, f));
				}

				return true;
			}
		}
	}

	private static class ParserException extends RuntimeException {
		static final long serialVersionUID = 5882582646773765630L;

		private ParserException() {
		}
	}
}