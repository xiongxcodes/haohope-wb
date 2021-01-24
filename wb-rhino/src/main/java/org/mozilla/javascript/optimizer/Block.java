/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.optimizer;

import java.util.HashMap;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.ObjArray;
import org.mozilla.javascript.ObjToIntMap;
import org.mozilla.javascript.Node.Jump;
import org.mozilla.javascript.ObjToIntMap.Iterator;
import org.mozilla.javascript.optimizer.DataFlowBitSet;
import org.mozilla.javascript.optimizer.OptFunctionNode;

class Block {
	private Block[] itsSuccessors;
	private Block[] itsPredecessors;
	private int itsStartNodeIndex;
	private int itsEndNodeIndex;
	private int itsBlockID;
	private DataFlowBitSet itsLiveOnEntrySet;
	private DataFlowBitSet itsLiveOnExitSet;
	private DataFlowBitSet itsUseBeforeDefSet;
	private DataFlowBitSet itsNotDefSet;
	static final boolean DEBUG = false;
	private static int debug_blockCount;

	Block(int startNodeIndex, int endNodeIndex) {
		this.itsStartNodeIndex = startNodeIndex;
		this.itsEndNodeIndex = endNodeIndex;
	}

	static void runFlowAnalyzes(OptFunctionNode fn, Node[] statementNodes) {
		int paramCount = fn.fnode.getParamCount();
		int varCount = fn.fnode.getParamAndVarCount();
		int[] varTypes = new int[varCount];

		int theBlocks;
		for (theBlocks = 0; theBlocks != paramCount; ++theBlocks) {
			varTypes[theBlocks] = 3;
		}

		for (theBlocks = paramCount; theBlocks != varCount; ++theBlocks) {
			varTypes[theBlocks] = 0;
		}

		Block[] arg6 = buildBlocks(statementNodes);
		reachingDefDataFlow(fn, statementNodes, arg6, varTypes);
		typeFlow(fn, statementNodes, arg6, varTypes);

		for (int i = paramCount; i != varCount; ++i) {
			if (varTypes[i] == 1) {
				fn.setIsNumberVar(i);
			}
		}

	}

	private static Block[] buildBlocks(Node[] statementNodes) {
		HashMap theTargetBlocks = new HashMap();
		ObjArray theBlocks = new ObjArray();
		int beginNodeIndex = 0;

		int result;
		Block.FatBlock i;
		for (result = 0; result < statementNodes.length; ++result) {
			switch (statementNodes[result].getType()) {
				case 5 :
				case 6 :
				case 7 :
					i = newFatBlock(beginNodeIndex, result);
					if (statementNodes[beginNodeIndex].getType() == 130) {
						theTargetBlocks.put(statementNodes[beginNodeIndex], i);
					}

					theBlocks.add(i);
					beginNodeIndex = result + 1;
					break;
				case 130 :
					if (result != beginNodeIndex) {
						i = newFatBlock(beginNodeIndex, result - 1);
						if (statementNodes[beginNodeIndex].getType() == 130) {
							theTargetBlocks.put(statementNodes[beginNodeIndex],
									i);
						}

						theBlocks.add(i);
						beginNodeIndex = result;
					}
			}
		}

		if (beginNodeIndex != statementNodes.length) {
			Block.FatBlock arg9 = newFatBlock(beginNodeIndex,
					statementNodes.length - 1);
			if (statementNodes[beginNodeIndex].getType() == 130) {
				theTargetBlocks.put(statementNodes[beginNodeIndex], arg9);
			}

			theBlocks.add(arg9);
		}

		for (result = 0; result < theBlocks.size(); ++result) {
			i = (Block.FatBlock) ((Block.FatBlock) theBlocks.get(result));
			Node fb = statementNodes[i.realBlock.itsEndNodeIndex];
			int b = fb.getType();
			if (b != 5 && result < theBlocks.size() - 1) {
				Block.FatBlock target = (Block.FatBlock) ((Block.FatBlock) theBlocks
						.get(result + 1));
				i.addSuccessor(target);
				target.addPredecessor(i);
			}

			if (b == 7 || b == 6 || b == 5) {
				Node arg14 = ((Jump) fb).target;
				Block.FatBlock branchTargetBlock = (Block.FatBlock) theTargetBlocks
						.get(arg14);
				arg14.putProp(6, branchTargetBlock.realBlock);
				i.addSuccessor(branchTargetBlock);
				branchTargetBlock.addPredecessor(i);
			}
		}

		Block[] arg10 = new Block[theBlocks.size()];

		for (int arg11 = 0; arg11 < theBlocks.size(); ++arg11) {
			Block.FatBlock arg12 = (Block.FatBlock) ((Block.FatBlock) theBlocks
					.get(arg11));
			Block arg13 = arg12.realBlock;
			arg13.itsSuccessors = arg12.getSuccessors();
			arg13.itsPredecessors = arg12.getPredecessors();
			arg13.itsBlockID = arg11;
			arg10[arg11] = arg13;
		}

		return arg10;
	}

	private static Block.FatBlock newFatBlock(int startNodeIndex,
			int endNodeIndex) {
		Block.FatBlock fb = new Block.FatBlock();
		fb.realBlock = new Block(startNodeIndex, endNodeIndex);
		return fb;
	}

	private static String toString(Block[] blockList, Node[] statementNodes) {
		return null;
	}

	private static void reachingDefDataFlow(OptFunctionNode fn,
			Node[] statementNodes, Block[] theBlocks, int[] varTypes) {
		for (int visit = 0; visit < theBlocks.length; ++visit) {
			theBlocks[visit].initLiveOnEntrySets(fn, statementNodes);
		}

		boolean[] arg10 = new boolean[theBlocks.length];
		boolean[] doneOnce = new boolean[theBlocks.length];
		int vIndex = theBlocks.length - 1;
		boolean needRescan = false;
		arg10[vIndex] = true;

		while (true) {
			if (arg10[vIndex] || !doneOnce[vIndex]) {
				doneOnce[vIndex] = true;
				arg10[vIndex] = false;
				if (theBlocks[vIndex].doReachedUseDataFlow()) {
					Block[] pred = theBlocks[vIndex].itsPredecessors;
					if (pred != null) {
						for (int i = 0; i < pred.length; ++i) {
							int index = pred[i].itsBlockID;
							arg10[index] = true;
							needRescan |= index > vIndex;
						}
					}
				}
			}

			if (vIndex == 0) {
				if (!needRescan) {
					theBlocks[0].markAnyTypeVariables(varTypes);
					return;
				}

				vIndex = theBlocks.length - 1;
				needRescan = false;
			} else {
				--vIndex;
			}
		}
	}

	private static void typeFlow(OptFunctionNode fn, Node[] statementNodes,
			Block[] theBlocks, int[] varTypes) {
		boolean[] visit = new boolean[theBlocks.length];
		boolean[] doneOnce = new boolean[theBlocks.length];
		int vIndex = 0;
		boolean needRescan = false;
		visit[vIndex] = true;

		while (true) {
			if (visit[vIndex] || !doneOnce[vIndex]) {
				doneOnce[vIndex] = true;
				visit[vIndex] = false;
				if (theBlocks[vIndex].doTypeFlow(fn, statementNodes, varTypes)) {
					Block[] succ = theBlocks[vIndex].itsSuccessors;
					if (succ != null) {
						for (int i = 0; i < succ.length; ++i) {
							int index = succ[i].itsBlockID;
							visit[index] = true;
							needRescan |= index < vIndex;
						}
					}
				}
			}

			if (vIndex == theBlocks.length - 1) {
				if (!needRescan) {
					return;
				}

				vIndex = 0;
				needRescan = false;
			} else {
				++vIndex;
			}
		}
	}

	private static boolean assignType(int[] varTypes, int index, int type) {
		return type != (varTypes[index] |= type);
	}

	private void markAnyTypeVariables(int[] varTypes) {
		for (int i = 0; i != varTypes.length; ++i) {
			if (this.itsLiveOnEntrySet.test(i)) {
				assignType(varTypes, i, 3);
			}
		}

	}

	private void lookForVariableAccess(OptFunctionNode fn, Node n) {
		Node child;
		switch (n.getType()) {
			case 55 :
				int child1 = fn.getVarIndex(n);
				if (!this.itsNotDefSet.test(child1)) {
					this.itsUseBeforeDefSet.set(child1);
				}
				break;
			case 56 :
				child = n.getFirstChild();
				Node rhs1 = child.getNext();
				this.lookForVariableAccess(fn, rhs1);
				this.itsNotDefSet.set(fn.getVarIndex(n));
				break;
			case 105 :
			case 106 :
				child = n.getFirstChild();
				if (child.getType() == 55) {
					int rhs = fn.getVarIndex(child);
					if (!this.itsNotDefSet.test(rhs)) {
						this.itsUseBeforeDefSet.set(rhs);
					}

					this.itsNotDefSet.set(rhs);
				}
				break;
			default :
				for (child = n.getFirstChild(); child != null; child = child
						.getNext()) {
					this.lookForVariableAccess(fn, child);
				}
		}

	}

	private void initLiveOnEntrySets(OptFunctionNode fn, Node[] statementNodes) {
		int listLength = fn.getVarCount();
		this.itsUseBeforeDefSet = new DataFlowBitSet(listLength);
		this.itsNotDefSet = new DataFlowBitSet(listLength);
		this.itsLiveOnEntrySet = new DataFlowBitSet(listLength);
		this.itsLiveOnExitSet = new DataFlowBitSet(listLength);

		for (int i = this.itsStartNodeIndex; i <= this.itsEndNodeIndex; ++i) {
			Node n = statementNodes[i];
			this.lookForVariableAccess(fn, n);
		}

		this.itsNotDefSet.not();
	}

	private boolean doReachedUseDataFlow() {
		this.itsLiveOnExitSet.clear();
		if (this.itsSuccessors != null) {
			for (int i = 0; i < this.itsSuccessors.length; ++i) {
				this.itsLiveOnExitSet
						.or(this.itsSuccessors[i].itsLiveOnEntrySet);
			}
		}

		return this.itsLiveOnEntrySet.df2(this.itsLiveOnExitSet,
				this.itsUseBeforeDefSet, this.itsNotDefSet);
	}

	private static int findExpressionType(OptFunctionNode fn, Node n,
			int[] varTypes) {
		Node child;
		int result;
		switch (n.getType()) {
			case 9 :
			case 10 :
			case 11 :
			case 18 :
			case 19 :
			case 20 :
			case 22 :
			case 23 :
			case 24 :
			case 25 :
			case 28 :
			case 29 :
			case 105 :
			case 106 :
				return 1;
			case 12 :
			case 13 :
			case 14 :
			case 15 :
			case 16 :
			case 17 :
			case 26 :
			case 27 :
			case 31 :
			case 32 :
			case 33 :
			case 34 :
			case 35 :
			case 37 :
			case 39 :
			case 41 :
			case 42 :
			case 43 :
			case 44 :
			case 45 :
			case 46 :
			case 47 :
			case 48 :
			case 49 :
			case 50 :
			case 51 :
			case 52 :
			case 53 :
			case 54 :
			case 56 :
			case 57 :
			case 58 :
			case 59 :
			case 60 :
			case 61 :
			case 62 :
			case 63 :
			case 64 :
			case 67 :
			case 68 :
			case 69 :
			case 71 :
			case 72 :
			case 73 :
			case 74 :
			case 75 :
			case 76 :
			case 77 :
			case 78 :
			case 79 :
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
			default :
				child = n.getFirstChild();
				if (child == null) {
					return 3;
				}

				for (result = 0; child != null; child = child.getNext()) {
					result |= findExpressionType(fn, child, varTypes);
				}

				return result;
			case 21 :
				child = n.getFirstChild();
				result = findExpressionType(fn, child, varTypes);
				int rType = findExpressionType(fn, child.getNext(), varTypes);
				return result | rType;
			case 30 :
			case 38 :
			case 70 :
				return 3;
			case 36 :
				return 3;
			case 40 :
				return 1;
			case 55 :
				return varTypes[fn.getVarIndex(n)];
			case 65 :
			case 66 :
				return 3;
		}
	}

	private static boolean findDefPoints(OptFunctionNode fn, Node n,
			int[] varTypes) {
		boolean result = false;
		Node child = n.getFirstChild();
		int rValue;
		switch (n.getType()) {
			case 35 :
			case 138 :
				if (child.getType() == 55) {
					rValue = fn.getVarIndex(child);
					assignType(varTypes, rValue, 3);
				}

				while (child != null) {
					result |= findDefPoints(fn, child, varTypes);
					child = child.getNext();
				}

				return result;
			case 56 :
				Node rValue1 = child.getNext();
				int theType = findExpressionType(fn, rValue1, varTypes);
				int i = fn.getVarIndex(n);
				result |= assignType(varTypes, i, theType);
				break;
			case 105 :
			case 106 :
				if (child.getType() == 55) {
					rValue = fn.getVarIndex(child);
					result |= assignType(varTypes, rValue, 1);
				}
				break;
			default :
				while (child != null) {
					result |= findDefPoints(fn, child, varTypes);
					child = child.getNext();
				}
		}

		return result;
	}

	private boolean doTypeFlow(OptFunctionNode fn, Node[] statementNodes,
			int[] varTypes) {
		boolean changed = false;

		for (int i = this.itsStartNodeIndex; i <= this.itsEndNodeIndex; ++i) {
			Node n = statementNodes[i];
			if (n != null) {
				changed |= findDefPoints(fn, n, varTypes);
			}
		}

		return changed;
	}

	private void printLiveOnEntrySet(OptFunctionNode fn) {
	}

	private static class FatBlock {
		private ObjToIntMap successors;
		private ObjToIntMap predecessors;
		Block realBlock;

		private FatBlock() {
			this.successors = new ObjToIntMap();
			this.predecessors = new ObjToIntMap();
		}

		private static Block[] reduceToArray(ObjToIntMap map) {
			Block[] result = null;
			if (!map.isEmpty()) {
				result = new Block[map.size()];
				int i = 0;
				Iterator iter = map.newIterator();
				iter.start();

				while (!iter.done()) {
					Block.FatBlock fb = (Block.FatBlock) ((Block.FatBlock) iter
							.getKey());
					result[i++] = fb.realBlock;
					iter.next();
				}
			}

			return result;
		}

		void addSuccessor(Block.FatBlock b) {
			this.successors.put(b, 0);
		}

		void addPredecessor(Block.FatBlock b) {
			this.predecessors.put(b, 0);
		}

		Block[] getSuccessors() {
			return reduceToArray(this.successors);
		}

		Block[] getPredecessors() {
			return reduceToArray(this.predecessors);
		}
	}
}