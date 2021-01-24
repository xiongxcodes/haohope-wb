/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.optimizer;

import org.mozilla.javascript.Node;
import org.mozilla.javascript.ObjArray;
import org.mozilla.javascript.ScriptOrFnNode;
import org.mozilla.javascript.optimizer.Block;
import org.mozilla.javascript.optimizer.OptFunctionNode;

class Optimizer {
	static final int NoType = 0;
	static final int NumberType = 1;
	static final int AnyType = 3;
	private boolean inDirectCallFunction;
	OptFunctionNode theFunction;
	private boolean parameterUsedInNumberContext;

	void optimize(ScriptOrFnNode scriptOrFn) {
		int functionCount = scriptOrFn.getFunctionCount();

		for (int i = 0; i != functionCount; ++i) {
			OptFunctionNode f = OptFunctionNode.get(scriptOrFn, i);
			this.optimizeFunction(f);
		}

	}

	private void optimizeFunction(OptFunctionNode theFunction) {
		if (!theFunction.fnode.requiresActivation()) {
			this.inDirectCallFunction = theFunction.isTargetOfDirectCall();
			this.theFunction = theFunction;
			ObjArray statementsArray = new ObjArray();
			buildStatementList_r(theFunction.fnode, statementsArray);
			Node[] theStatementNodes = new Node[statementsArray.size()];
			statementsArray.toArray(theStatementNodes);
			Block.runFlowAnalyzes(theFunction, theStatementNodes);
			if (!theFunction.fnode.requiresActivation()) {
				this.parameterUsedInNumberContext = false;

				for (int i = 0; i < theStatementNodes.length; ++i) {
					this.rewriteForNumberVariables(theStatementNodes[i], 1);
				}

				theFunction
						.setParameterNumberContext(this.parameterUsedInNumberContext);
			}

		}
	}

	private void markDCPNumberContext(Node n) {
		if (this.inDirectCallFunction && n.getType() == 55) {
			int varIndex = this.theFunction.getVarIndex(n);
			if (this.theFunction.isParameter(varIndex)) {
				this.parameterUsedInNumberContext = true;
			}
		}

	}

	private boolean convertParameter(Node n) {
		if (this.inDirectCallFunction && n.getType() == 55) {
			int varIndex = this.theFunction.getVarIndex(n);
			if (this.theFunction.isParameter(varIndex)) {
				n.removeProp(8);
				return true;
			}
		}

		return false;
	}

	private int rewriteForNumberVariables(Node n, int desired) {
		Node child;
		int type;
		int indexType;
		Node target1;
		switch (n.getType()) {
			case 9 :
			case 10 :
			case 11 :
			case 18 :
			case 19 :
			case 22 :
			case 23 :
			case 24 :
			case 25 :
				child = n.getFirstChild();
				target1 = child.getNext();
				type = this.rewriteForNumberVariables(child, 1);
				indexType = this.rewriteForNumberVariables(target1, 1);
				this.markDCPNumberContext(child);
				this.markDCPNumberContext(target1);
				if (type == 1) {
					if (indexType == 1) {
						n.putIntProp(8, 0);
						return 1;
					}

					if (!this.convertParameter(target1)) {
						n.removeChild(target1);
						n.addChildToBack(new Node(149, target1));
						n.putIntProp(8, 0);
					}

					return 1;
				} else {
					if (indexType == 1) {
						if (!this.convertParameter(child)) {
							n.removeChild(child);
							n.addChildToFront(new Node(149, child));
							n.putIntProp(8, 0);
						}

						return 1;
					}

					if (!this.convertParameter(child)) {
						n.removeChild(child);
						n.addChildToFront(new Node(149, child));
					}

					if (!this.convertParameter(target1)) {
						n.removeChild(target1);
						n.addChildToBack(new Node(149, target1));
					}

					n.putIntProp(8, 0);
					return 1;
				}
			case 14 :
			case 15 :
			case 16 :
			case 17 :
				child = n.getFirstChild();
				target1 = child.getNext();
				type = this.rewriteForNumberVariables(child, 1);
				indexType = this.rewriteForNumberVariables(target1, 1);
				this.markDCPNumberContext(child);
				this.markDCPNumberContext(target1);
				if (this.convertParameter(child)) {
					if (this.convertParameter(target1)) {
						return 0;
					}

					if (indexType == 1) {
						n.putIntProp(8, 2);
					}
				} else if (this.convertParameter(target1)) {
					if (type == 1) {
						n.putIntProp(8, 1);
					}
				} else if (type == 1) {
					if (indexType == 1) {
						n.putIntProp(8, 0);
					} else {
						n.putIntProp(8, 1);
					}
				} else if (indexType == 1) {
					n.putIntProp(8, 2);
				}

				return 0;
			case 21 :
				child = n.getFirstChild();
				target1 = child.getNext();
				type = this.rewriteForNumberVariables(child, 1);
				indexType = this.rewriteForNumberVariables(target1, 1);
				if (this.convertParameter(child)) {
					if (this.convertParameter(target1)) {
						return 0;
					}

					if (indexType == 1) {
						n.putIntProp(8, 2);
					}
				} else if (this.convertParameter(target1)) {
					if (type == 1) {
						n.putIntProp(8, 1);
					}
				} else if (type == 1) {
					if (indexType == 1) {
						n.putIntProp(8, 0);
						return 1;
					}

					n.putIntProp(8, 1);
				} else if (indexType == 1) {
					n.putIntProp(8, 2);
				}

				return 0;
			case 36 :
				child = n.getFirstChild();
				target1 = child.getNext();
				type = this.rewriteForNumberVariables(child, 1);
				if (type == 1 && !this.convertParameter(child)) {
					n.removeChild(child);
					n.addChildToFront(new Node(148, child));
				}

				indexType = this.rewriteForNumberVariables(target1, 1);
				if (indexType == 1 && !this.convertParameter(target1)) {
					n.putIntProp(8, 2);
				}

				return 0;
			case 37 :
			case 139 :
				child = n.getFirstChild();
				target1 = child.getNext();
				Node type1 = target1.getNext();
				indexType = this.rewriteForNumberVariables(child, 1);
				if (indexType == 1 && !this.convertParameter(child)) {
					n.removeChild(child);
					n.addChildToFront(new Node(148, child));
				}

				int indexType1 = this.rewriteForNumberVariables(target1, 1);
				if (indexType1 == 1 && !this.convertParameter(target1)) {
					n.putIntProp(8, 1);
				}

				int rValueType = this.rewriteForNumberVariables(type1, 1);
				if (rValueType == 1 && !this.convertParameter(type1)) {
					n.removeChild(type1);
					n.addChildToBack(new Node(148, type1));
				}

				return 0;
			case 38 :
				child = n.getFirstChild();
				this.rewriteAsObjectChildren(child, child.getFirstChild());
				child = child.getNext();
				OptFunctionNode target2 = (OptFunctionNode) n.getProp(9);
				if (target2 != null) {
					for (; child != null; child = child.getNext()) {
						type = this.rewriteForNumberVariables(child, 1);
						if (type == 1) {
							this.markDCPNumberContext(child);
						}
					}
				} else {
					this.rewriteAsObjectChildren(n, child);
				}

				return 0;
			case 40 :
				n.putIntProp(8, 0);
				return 1;
			case 55 :
				int child1 = this.theFunction.getVarIndex(n);
				if (this.inDirectCallFunction
						&& this.theFunction.isParameter(child1) && desired == 1) {
					n.putIntProp(8, 0);
					return 1;
				} else {
					if (this.theFunction.isNumberVar(child1)) {
						n.putIntProp(8, 0);
						return 1;
					}

					return 0;
				}
			case 56 :
				child = n.getFirstChild();
				target1 = child.getNext();
				type = this.rewriteForNumberVariables(target1, 1);
				indexType = this.theFunction.getVarIndex(n);
				if (this.inDirectCallFunction
						&& this.theFunction.isParameter(indexType)) {
					if (type == 1) {
						if (!this.convertParameter(target1)) {
							n.putIntProp(8, 0);
							return 1;
						}

						this.markDCPNumberContext(target1);
						return 0;
					}

					return type;
				} else {
					if (this.theFunction.isNumberVar(indexType)) {
						if (type != 1) {
							n.removeChild(target1);
							n.addChildToBack(new Node(149, target1));
						}

						n.putIntProp(8, 0);
						this.markDCPNumberContext(target1);
						return 1;
					}

					if (type == 1 && !this.convertParameter(target1)) {
						n.removeChild(target1);
						n.addChildToBack(new Node(148, target1));
					}

					return 0;
				}
			case 105 :
			case 106 :
				child = n.getFirstChild();
				if (child.getType() == 55) {
					if (this.rewriteForNumberVariables(child, 1) == 1
							&& !this.convertParameter(child)) {
						n.putIntProp(8, 0);
						this.markDCPNumberContext(child);
						return 1;
					}

					return 0;
				} else {
					if (child.getType() == 36) {
						return this.rewriteForNumberVariables(child, 1);
					}

					return 0;
				}
			case 132 :
				child = n.getFirstChild();
				int target = this.rewriteForNumberVariables(child, 1);
				if (target == 1) {
					n.putIntProp(8, 0);
				}

				return 0;
			default :
				this.rewriteAsObjectChildren(n, n.getFirstChild());
				return 0;
		}
	}

	private void rewriteAsObjectChildren(Node n, Node child) {
		Node nextChild;
		for (; child != null; child = nextChild) {
			nextChild = child.getNext();
			int type = this.rewriteForNumberVariables(child, 0);
			if (type == 1 && !this.convertParameter(child)) {
				n.removeChild(child);
				Node nuChild = new Node(148, child);
				if (nextChild == null) {
					n.addChildToBack(nuChild);
				} else {
					n.addChildBefore(nuChild, nextChild);
				}
			}
		}

	}

	private static void buildStatementList_r(Node node, ObjArray statements) {
		int type = node.getType();
		if (type != 128 && type != 140 && type != 131 && type != 108) {
			statements.add(node);
		} else {
			for (Node child = node.getFirstChild(); child != null; child = child
					.getNext()) {
				buildStatementList_r(child, statements);
			}
		}

	}
}