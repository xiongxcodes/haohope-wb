/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.optimizer;

import org.mozilla.javascript.FunctionNode;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.ScriptOrFnNode;

final class OptFunctionNode {
	FunctionNode fnode;
	private boolean[] numberVarFlags;
	private int directTargetIndex = -1;
	private boolean itsParameterNumberContext;
	boolean itsContainsCalls0;
	boolean itsContainsCalls1;

	OptFunctionNode(FunctionNode fnode) {
		this.fnode = fnode;
		fnode.setCompilerData(this);
	}

	static OptFunctionNode get(ScriptOrFnNode scriptOrFn, int i) {
		FunctionNode fnode = scriptOrFn.getFunctionNode(i);
		return (OptFunctionNode) fnode.getCompilerData();
	}

	static OptFunctionNode get(ScriptOrFnNode scriptOrFn) {
		return (OptFunctionNode) scriptOrFn.getCompilerData();
	}

	boolean isTargetOfDirectCall() {
		return this.directTargetIndex >= 0;
	}

	int getDirectTargetIndex() {
		return this.directTargetIndex;
	}

	void setDirectTargetIndex(int directTargetIndex) {
		if (directTargetIndex < 0 || this.directTargetIndex >= 0) {
			Kit.codeBug();
		}

		this.directTargetIndex = directTargetIndex;
	}

	void setParameterNumberContext(boolean b) {
		this.itsParameterNumberContext = b;
	}

	boolean getParameterNumberContext() {
		return this.itsParameterNumberContext;
	}

	int getVarCount() {
		return this.fnode.getParamAndVarCount();
	}

	boolean isParameter(int varIndex) {
		return varIndex < this.fnode.getParamCount();
	}

	boolean isNumberVar(int varIndex) {
		varIndex -= this.fnode.getParamCount();
		return varIndex >= 0 && this.numberVarFlags != null
				? this.numberVarFlags[varIndex]
				: false;
	}

	void setIsNumberVar(int varIndex) {
		varIndex -= this.fnode.getParamCount();
		if (varIndex < 0) {
			Kit.codeBug();
		}

		if (this.numberVarFlags == null) {
			int size = this.fnode.getParamAndVarCount()
					- this.fnode.getParamCount();
			this.numberVarFlags = new boolean[size];
		}

		this.numberVarFlags[varIndex] = true;
	}

	int getVarIndex(Node n) {
		int index = n.getIntProp(7, -1);
		if (index == -1) {
			int type = n.getType();
			Node node;
			if (type == 55) {
				node = n;
			} else {
				if (type != 56 && type != 155) {
					throw Kit.codeBug();
				}

				node = n.getFirstChild();
			}

			index = this.fnode.getIndexForNameNode(node);
			if (index < 0) {
				throw Kit.codeBug();
			}

			n.putIntProp(7, index);
		}

		return index;
	}
}