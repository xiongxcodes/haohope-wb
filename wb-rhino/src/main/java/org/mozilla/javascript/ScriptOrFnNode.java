/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import java.util.ArrayList;
import org.mozilla.javascript.FunctionNode;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.ObjArray;
import org.mozilla.javascript.Node.Scope;
import org.mozilla.javascript.Node.Symbol;

public class ScriptOrFnNode extends Scope {
	private int encodedSourceStart;
	private int encodedSourceEnd;
	private String sourceName;
	private int endLineno = -1;
	private ObjArray functions;
	private ObjArray regexps;
	private ArrayList<Symbol> symbols = new ArrayList(4);
	private int paramCount = 0;
	private String[] variableNames;
	private boolean[] isConsts;
	private Object compilerData;
	private int tempNumber = 0;

	public ScriptOrFnNode(int nodeType) {
		super(nodeType);
		this.setParent((Scope) null);
	}

	public final String getSourceName() {
		return this.sourceName;
	}

	public final void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}

	public final int getEncodedSourceStart() {
		return this.encodedSourceStart;
	}

	public final int getEncodedSourceEnd() {
		return this.encodedSourceEnd;
	}

	public final void setEncodedSourceBounds(int start, int end) {
		this.encodedSourceStart = start;
		this.encodedSourceEnd = end;
	}

	public final int getBaseLineno() {
		return this.lineno;
	}

	public final void setBaseLineno(int lineno) {
		if (lineno < 0 || this.lineno >= 0) {
			Kit.codeBug();
		}

		this.lineno = lineno;
	}

	public final int getEndLineno() {
		return this.endLineno;
	}

	public final void setEndLineno(int lineno) {
		if (lineno < 0 || this.endLineno >= 0) {
			Kit.codeBug();
		}

		this.endLineno = lineno;
	}

	public final int getFunctionCount() {
		return this.functions == null ? 0 : this.functions.size();
	}

	public final FunctionNode getFunctionNode(int i) {
		return (FunctionNode) this.functions.get(i);
	}

	public final int addFunction(FunctionNode fnNode) {
		if (fnNode == null) {
			Kit.codeBug();
		}

		if (this.functions == null) {
			this.functions = new ObjArray();
		}

		this.functions.add(fnNode);
		return this.functions.size() - 1;
	}

	public final int getRegexpCount() {
		return this.regexps == null ? 0 : this.regexps.size() / 2;
	}

	public final String getRegexpString(int index) {
		return (String) this.regexps.get(index * 2);
	}

	public final String getRegexpFlags(int index) {
		return (String) this.regexps.get(index * 2 + 1);
	}

	public final int addRegexp(String string, String flags) {
		if (string == null) {
			Kit.codeBug();
		}

		if (this.regexps == null) {
			this.regexps = new ObjArray();
		}

		this.regexps.add(string);
		this.regexps.add(flags);
		return this.regexps.size() / 2 - 1;
	}

	public int getIndexForNameNode(Node nameNode) {
		if (this.variableNames == null) {
			throw Kit.codeBug();
		} else {
			Scope node = nameNode.getScope();
			Symbol symbol = node == null ? null : node.getSymbol(nameNode
					.getString());
			return symbol == null ? -1 : symbol.index;
		}
	}

	public final String getParamOrVarName(int index) {
		if (this.variableNames == null) {
			throw Kit.codeBug();
		} else {
			return this.variableNames[index];
		}
	}

	public final int getParamCount() {
		return this.paramCount;
	}

	public final int getParamAndVarCount() {
		if (this.variableNames == null) {
			throw Kit.codeBug();
		} else {
			return this.symbols.size();
		}
	}

	public final String[] getParamAndVarNames() {
		if (this.variableNames == null) {
			throw Kit.codeBug();
		} else {
			return this.variableNames;
		}
	}

	public final boolean[] getParamAndVarConst() {
		if (this.variableNames == null) {
			throw Kit.codeBug();
		} else {
			return this.isConsts;
		}
	}

	void addSymbol(Symbol symbol) {
		if (this.variableNames != null) {
			throw Kit.codeBug();
		} else {
			if (symbol.declType == 86) {
				++this.paramCount;
			}

			this.symbols.add(symbol);
		}
	}

	void flattenSymbolTable(boolean flattenAllTables) {
		if (!flattenAllTables) {
			ArrayList i = new ArrayList();
			if (this.symbolTable != null) {
				for (int symbol = 0; symbol < this.symbols.size(); ++symbol) {
					Symbol symbol1 = (Symbol) this.symbols.get(symbol);
					if (symbol1.containingTable == this) {
						i.add(symbol1);
					}
				}
			}

			this.symbols = i;
		}

		this.variableNames = new String[this.symbols.size()];
		this.isConsts = new boolean[this.symbols.size()];

		Symbol arg5;
		for (int arg4 = 0; arg4 < this.symbols.size(); arg5.index = arg4++) {
			arg5 = (Symbol) this.symbols.get(arg4);
			this.variableNames[arg4] = arg5.name;
			this.isConsts[arg4] = arg5.declType == 153;
		}

	}

	public final Object getCompilerData() {
		return this.compilerData;
	}

	public final void setCompilerData(Object data) {
		if (data == null) {
			throw new IllegalArgumentException();
		} else if (this.compilerData != null) {
			throw new IllegalStateException();
		} else {
			this.compilerData = data;
		}
	}

	public String getNextTempName() {
		return "$" + this.tempNumber++;
	}
}