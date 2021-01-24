/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.mozilla.javascript.FunctionNode;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.ObjArray;
import org.mozilla.javascript.ScriptOrFnNode;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Node.Jump;
import org.mozilla.javascript.Node.Scope;

public class NodeTransformer {
	private ObjArray loops;
	private ObjArray loopEnds;
	private boolean hasFinally;

	public final void transform(ScriptOrFnNode tree) {
		this.transformCompilationUnit(tree);

		for (int i = 0; i != tree.getFunctionCount(); ++i) {
			FunctionNode fn = tree.getFunctionNode(i);
			this.transform(fn);
		}

	}

	private void transformCompilationUnit(ScriptOrFnNode tree) {
		this.loops = new ObjArray();
		this.loopEnds = new ObjArray();
		this.hasFinally = false;
		boolean createScopeObjects = tree.getType() != 108
				|| ((FunctionNode) tree).requiresActivation();
		tree.flattenSymbolTable(!createScopeObjects);
		this.transformCompilationUnit_r(tree, tree, tree, createScopeObjects);
	}

	private void transformCompilationUnit_r(ScriptOrFnNode tree, Node parent,
			Scope scope, boolean createScopeObjects) {
		Node node = null;

		while (true) {
			while (true) {
				while (true) {
					Node previous = null;
					if (node == null) {
						node = parent.getFirstChild();
					} else {
						previous = node;
						node = node.getNext();
					}

					if (node == null) {
						return;
					}

					int type = node.getType();
					Scope nameSource;
					Node name;
					Node defining;
					Node arg21;
					if (createScopeObjects
							&& (type == 128 || type == 131 || type == 156)
							&& node instanceof Scope) {
						nameSource = (Scope) node;
						if (nameSource.symbolTable != null) {
							name = new Node(type == 156 ? 157 : 152);
							defining = new Node(152);
							name.addChildToBack(defining);
							Iterator n = nameSource.symbolTable.keySet()
									.iterator();

							while (n.hasNext()) {
								String elemtype = (String) n.next();
								defining.addChildToBack(Node.newString(39,
										elemtype));
							}

							nameSource.symbolTable = null;
							arg21 = node;
							node = replaceCurrent(parent, previous, node, name);
							type = node.getType();
							name.addChildToBack(arg21);
						}
					}

					Jump jsrFinally;
					Node arg15;
					Jump arg17;
					int arg20;
					int arg23;
					label260 : switch (type) {
						case 3 :
						case 130 :
							if (!this.loopEnds.isEmpty()
									&& this.loopEnds.peek() == node) {
								this.loopEnds.pop();
								this.loops.pop();
							}
							break;
						case 4 :
							boolean arg24 = tree.getType() == 108
									&& ((FunctionNode) tree).isGenerator();
							if (arg24) {
								node.putIntProp(20, 1);
							}

							if (this.hasFinally) {
								name = null;

								for (arg20 = this.loops.size() - 1; arg20 >= 0; --arg20) {
									arg21 = (Node) this.loops.get(arg20);
									arg23 = arg21.getType();
									if (arg23 == 80 || arg23 == 122) {
										Object arg27;
										if (arg23 == 80) {
											jsrFinally = new Jump(134);
											Node jsrtarget = ((Jump) arg21)
													.getFinally();
											jsrFinally.target = jsrtarget;
											arg27 = jsrFinally;
										} else {
											arg27 = new Node(3);
										}

										if (name == null) {
											name = new Node(128,
													node.getLineno());
										}

										name.addChildToBack((Node) arg27);
									}
								}

								if (name != null) {
									defining = node;
									arg21 = node.getFirstChild();
									node = replaceCurrent(parent, previous,
											node, name);
									if (arg21 != null && !arg24) {
										Node arg25 = new Node(133, arg21);
										name.addChildToFront(arg25);
										defining = new Node(64);
										name.addChildToBack(defining);
										this.transformCompilationUnit_r(tree,
												arg25, scope,
												createScopeObjects);
										continue;
									}

									name.addChildToBack(defining);
									continue;
								}
							}
							break;
						case 7 :
						case 32 :
							arg15 = node.getFirstChild();
							if (type == 7) {
								while (arg15.getType() == 26) {
									arg15 = arg15.getFirstChild();
								}

								if (arg15.getType() == 12
										|| arg15.getType() == 13) {
									name = arg15.getFirstChild();
									defining = arg15.getLastChild();
									if (name.getType() == 39
											&& name.getString().equals(
													"undefined")) {
										arg15 = defining;
									} else if (defining.getType() == 39
											&& defining.getString().equals(
													"undefined")) {
										arg15 = name;
									}
								}
							}

							if (arg15.getType() == 33) {
								arg15.setType(34);
							}
							break;
						case 8 :
						case 31 :
						case 39 :
						case 154 :
							if (!createScopeObjects) {
								label238 : {
									if (type == 39) {
										arg15 = node;
									} else {
										arg15 = node.getFirstChild();
										if (arg15.getType() != 49) {
											if (type != 31) {
												throw Kit.codeBug();
											}
											break label238;
										}
									}

									if (arg15.getScope() == null) {
										String arg19 = arg15.getString();
										Scope arg22 = scope
												.getDefiningScope(arg19);
										if (arg22 != null) {
											arg15.setScope(arg22);
											if (type == 39) {
												node.setType(55);
											} else if (type == 8) {
												node.setType(56);
												arg15.setType(41);
											} else if (type == 154) {
												node.setType(155);
												arg15.setType(41);
											} else {
												if (type != 31) {
													throw Kit.codeBug();
												}

												arg21 = new Node(44);
												node = replaceCurrent(parent,
														previous, node, arg21);
											}
										}
									}
								}
							}
							break;
						case 30 :
							this.visitNew(node, tree);
							break;
						case 38 :
							this.visitCall(node, tree);
							break;
						case 72 :
							((FunctionNode) tree).addResumptionPoint(node);
							break;
						case 80 :
							arg17 = (Jump) node;
							name = arg17.getFinally();
							if (name != null) {
								this.hasFinally = true;
								this.loops.push(node);
								this.loopEnds.push(name);
							}
							break;
						case 113 :
						case 129 :
						case 131 :
							this.loops.push(node);
							this.loopEnds.push(((Jump) node).target);
							break;
						case 119 :
						case 120 :
							arg17 = (Jump) node;
							Jump arg18 = arg17.getJumpStatement();
							if (arg18 == null) {
								Kit.codeBug();
							}

							arg20 = this.loops.size();

							while (arg20 != 0) {
								--arg20;
								arg21 = (Node) this.loops.get(arg20);
								if (arg21 == arg18) {
									if (type == 119) {
										arg17.target = arg18.target;
									} else {
										arg17.target = arg18.getContinue();
									}

									arg17.setType(5);
									break label260;
								}

								arg23 = arg21.getType();
								if (arg23 == 122) {
									Node tryNode = new Node(3);
									previous = addBeforeCurrent(parent,
											previous, node, tryNode);
								} else if (arg23 == 80) {
									Jump arg26 = (Jump) arg21;
									jsrFinally = new Jump(134);
									jsrFinally.target = arg26.getFinally();
									previous = addBeforeCurrent(parent,
											previous, node, jsrFinally);
								}
							}

							throw Kit.codeBug();
						case 122 :
							this.loops.push(node);
							arg15 = node.getNext();
							if (arg15.getType() != 3) {
								Kit.codeBug();
							}

							this.loopEnds.push(arg15);
							break;
						case 136 :
							nameSource = scope.getDefiningScope(node
									.getString());
							if (nameSource != null) {
								node.setScope(nameSource);
							}
							break;
						case 152 :
						case 157 :
							arg15 = node.getFirstChild();
							if (arg15.getType() == 152) {
								boolean arg16 = tree.getType() != 108
										|| ((FunctionNode) tree)
												.requiresActivation();
								node = this.visitLet(arg16, parent, previous,
										node);
								break;
							}
						case 121 :
						case 153 :
							arg15 = new Node(128);
							name = node.getFirstChild();

							label250 : while (true) {
								while (true) {
									if (name == null) {
										node = replaceCurrent(parent, previous,
												node, arg15);
										break label250;
									}

									defining = name;
									name = name.getNext();
									if (defining.getType() == 39) {
										if (!defining.hasChildren()) {
											continue;
										}

										arg21 = defining.getFirstChild();
										defining.removeChild(arg21);
										defining.setType(49);
										defining = new Node(type == 153
												? 154
												: 8, defining, arg21);
										break;
									}

									if (defining.getType() != 157) {
										throw Kit.codeBug();
									}
									break;
								}

								arg21 = new Node(132, defining,
										node.getLineno());
								arg15.addChildToBack(arg21);
							}
					}

					this.transformCompilationUnit_r(tree, node,
							node instanceof Scope ? (Scope) node : scope,
							createScopeObjects);
				}
			}
		}
	}

	protected void visitNew(Node node, ScriptOrFnNode tree) {
	}

	protected void visitCall(Node node, ScriptOrFnNode tree) {
	}

	protected Node visitLet(boolean createWith, Node parent, Node previous,
			Node scopeNode) {
		Node vars = scopeNode.getFirstChild();
		Node body = vars.getNext();
		scopeNode.removeChild(vars);
		scopeNode.removeChild(body);
		boolean isExpression = scopeNode.getType() == 157;
		Node result;
		Node newVars;
		Node current;
		Node stringNode;
		Node init;
		if (!createWith) {
			result = new Node(isExpression ? 88 : 128);
			result = replaceCurrent(parent, previous, scopeNode, result);
			newVars = new Node(88);

			for (Node arg16 = vars.getFirstChild(); arg16 != null; arg16 = arg16
					.getNext()) {
				current = arg16;
				if (arg16.getType() == 157) {
					stringNode = arg16.getFirstChild();
					if (stringNode.getType() != 152) {
						throw Kit.codeBug();
					}

					if (isExpression) {
						body = new Node(88, stringNode.getNext(), body);
					} else {
						body = new Node(128,
								new Node(132, stringNode.getNext()), body);
					}

					Scope.joinScopes((Scope) arg16, (Scope) scopeNode);
					current = stringNode.getFirstChild();
				}

				if (current.getType() != 39) {
					throw Kit.codeBug();
				}

				stringNode = Node.newString(current.getString());
				stringNode.setScope((Scope) scopeNode);
				init = current.getFirstChild();
				if (init == null) {
					init = new Node(125, Node.newNumber(0.0D));
				}

				newVars.addChildToBack(new Node(56, stringNode, init));
			}

			if (isExpression) {
				result.addChildToBack(newVars);
				scopeNode.setType(88);
				result.addChildToBack(scopeNode);
				scopeNode.addChildToBack(body);
			} else {
				result.addChildToBack(new Node(132, newVars));
				scopeNode.setType(128);
				result.addChildToBack(scopeNode);
				scopeNode.addChildrenToBack(body);
			}
		} else {
			result = new Node(isExpression ? 158 : 128);
			result = replaceCurrent(parent, previous, scopeNode, result);
			ArrayList v = new ArrayList();
			current = new Node(66);
			stringNode = vars.getFirstChild();

			while (true) {
				if (stringNode == null) {
					current.putProp(12, v.toArray());
					newVars = new Node(2, current);
					result.addChildToBack(newVars);
					result.addChildToBack(new Node(122, body));
					result.addChildToBack(new Node(3));
					break;
				}

				init = stringNode;
				if (stringNode.getType() == 157) {
					List init1 = (List) stringNode.getProp(22);
					Node c = stringNode.getFirstChild();
					if (c.getType() != 152) {
						throw Kit.codeBug();
					}

					if (isExpression) {
						body = new Node(88, c.getNext(), body);
					} else {
						body = new Node(128, new Node(132, c.getNext()), body);
					}

					if (init1 != null) {
						v.addAll(init1);

						for (int i = 0; i < init1.size(); ++i) {
							current.addChildToBack(new Node(125, Node
									.newNumber(0.0D)));
						}
					}

					init = c.getFirstChild();
				}

				if (init.getType() != 39) {
					throw Kit.codeBug();
				}

				v.add(ScriptRuntime.getIndexObject(init.getString()));
				Node arg17 = init.getFirstChild();
				if (arg17 == null) {
					arg17 = new Node(125, Node.newNumber(0.0D));
				}

				current.addChildToBack(arg17);
				stringNode = stringNode.getNext();
			}
		}

		return result;
	}

	private static Node addBeforeCurrent(Node parent, Node previous,
			Node current, Node toAdd) {
		if (previous == null) {
			if (current != parent.getFirstChild()) {
				Kit.codeBug();
			}

			parent.addChildToFront(toAdd);
		} else {
			if (current != previous.getNext()) {
				Kit.codeBug();
			}

			parent.addChildAfter(toAdd, previous);
		}

		return toAdd;
	}

	private static Node replaceCurrent(Node parent, Node previous,
			Node current, Node replacement) {
		if (previous == null) {
			if (current != parent.getFirstChild()) {
				Kit.codeBug();
			}

			parent.replaceChild(current, replacement);
		} else if (previous.next == current) {
			parent.replaceChildAfter(previous, replacement);
		} else {
			parent.replaceChild(current, replacement);
		}

		return replacement;
	}
}