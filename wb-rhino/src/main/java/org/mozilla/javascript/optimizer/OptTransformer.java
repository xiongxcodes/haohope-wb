/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.optimizer;

import java.util.Map;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.NodeTransformer;
import org.mozilla.javascript.ObjArray;
import org.mozilla.javascript.ScriptOrFnNode;
import org.mozilla.javascript.optimizer.OptFunctionNode;

class OptTransformer extends NodeTransformer {
	private Map<String, OptFunctionNode> possibleDirectCalls;
	private ObjArray directCallTargets;

	OptTransformer(Map<String, OptFunctionNode> possibleDirectCalls,
			ObjArray directCallTargets) {
		this.possibleDirectCalls = possibleDirectCalls;
		this.directCallTargets = directCallTargets;
	}

	protected void visitNew(Node node, ScriptOrFnNode tree) {
		this.detectDirectCall(node, tree);
		super.visitNew(node, tree);
	}

	protected void visitCall(Node node, ScriptOrFnNode tree) {
		this.detectDirectCall(node, tree);
		super.visitCall(node, tree);
	}

	private void detectDirectCall(Node node, ScriptOrFnNode tree) {
		if (tree.getType() == 108) {
			Node left = node.getFirstChild();
			int argCount = 0;

			for (Node arg = left.getNext(); arg != null; ++argCount) {
				arg = arg.getNext();
			}

			if (argCount == 0) {
				OptFunctionNode.get(tree).itsContainsCalls0 = true;
			}

			if (this.possibleDirectCalls != null) {
				String targetName = null;
				if (left.getType() == 39) {
					targetName = left.getString();
				} else if (left.getType() == 33) {
					targetName = left.getFirstChild().getNext().getString();
				} else if (left.getType() == 34) {
					throw Kit.codeBug();
				}

				if (targetName != null) {
					OptFunctionNode ofn = (OptFunctionNode) this.possibleDirectCalls
							.get(targetName);
					if (ofn != null && argCount == ofn.fnode.getParamCount()
							&& !ofn.fnode.requiresActivation()
							&& argCount <= 32) {
						node.putProp(9, ofn);
						if (!ofn.isTargetOfDirectCall()) {
							int index = this.directCallTargets.size();
							this.directCallTargets.add(ofn);
							ofn.setDirectTargetIndex(index);
						}
					}
				}
			}
		}

	}
}