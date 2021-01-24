/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import java.util.ArrayList;
import org.mozilla.javascript.FunctionNode;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.ObjArray;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ScriptOrFnNode;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Node.Jump;
import org.mozilla.javascript.Node.Scope;

final class IRFactory {
	private Parser parser;
	private static final int LOOP_DO_WHILE = 0;
	private static final int LOOP_WHILE = 1;
	private static final int LOOP_FOR = 2;
	private static final int ALWAYS_TRUE_BOOLEAN = 1;
	private static final int ALWAYS_FALSE_BOOLEAN = -1;

	IRFactory(Parser parser) {
		this.parser = parser;
	}

	ScriptOrFnNode createScript() {
		return new ScriptOrFnNode(135);
	}

	void initScript(ScriptOrFnNode scriptNode, Node body) {
		Node children = body.getFirstChild();
		if (children != null) {
			scriptNode.addChildrenToBack(children);
		}

	}

	Node createLeaf(int nodeType) {
		return new Node(nodeType);
	}

	Node createSwitch(Node expr, int lineno) {
		Jump switchNode = new Jump(113, expr, lineno);
		Node block = new Node(128, switchNode);
		return block;
	}

	void addSwitchCase(Node switchBlock, Node caseExpression, Node statements) {
		if (switchBlock.getType() != 128) {
			throw Kit.codeBug();
		} else {
			Jump switchNode = (Jump) switchBlock.getFirstChild();
			if (switchNode.getType() != 113) {
				throw Kit.codeBug();
			} else {
				Node gotoTarget = Node.newTarget();
				if (caseExpression != null) {
					Jump caseNode = new Jump(114, caseExpression);
					caseNode.target = gotoTarget;
					switchNode.addChildToBack(caseNode);
				} else {
					switchNode.setDefault(gotoTarget);
				}

				switchBlock.addChildToBack(gotoTarget);
				switchBlock.addChildToBack(statements);
			}
		}
	}

	void closeSwitch(Node switchBlock) {
		if (switchBlock.getType() != 128) {
			throw Kit.codeBug();
		} else {
			Jump switchNode = (Jump) switchBlock.getFirstChild();
			if (switchNode.getType() != 113) {
				throw Kit.codeBug();
			} else {
				Node switchBreakTarget = Node.newTarget();
				switchNode.target = switchBreakTarget;
				Node defaultTarget = switchNode.getDefault();
				if (defaultTarget == null) {
					defaultTarget = switchBreakTarget;
				}

				switchBlock.addChildAfter(this.makeJump(5, defaultTarget),
						switchNode);
				switchBlock.addChildToBack(switchBreakTarget);
			}
		}
	}

	Node createVariables(int token, int lineno) {
		return new Node(token, lineno);
	}

	Node createExprStatement(Node expr, int lineno) {
		short type;
		if (this.parser.insideFunction()) {
			type = 132;
		} else {
			type = 133;
		}

		return new Node(type, expr, lineno);
	}

	Node createExprStatementNoReturn(Node expr, int lineno) {
		return new Node(132, expr, lineno);
	}

	Node createDefaultNamespace(Node expr, int lineno) {
		this.setRequiresActivation();
		Node n = this.createUnary(73, expr);
		Node result = this.createExprStatement(n, lineno);
		return result;
	}

	Node createName(String name) {
		this.checkActivationName(name, 39);
		return Node.newString(39, name);
	}

	private Node createName(int type, String name, Node child) {
		Node result = this.createName(name);
		result.setType(type);
		if (child != null) {
			result.addChildToBack(child);
		}

		return result;
	}

	Node createString(String string) {
		return Node.newString(string);
	}

	Node createNumber(double number) {
		return Node.newNumber(number);
	}

	Node createCatch(String varName, Node catchCond, Node stmts, int lineno) {
		if (catchCond == null) {
			catchCond = new Node(127);
		}

		return new Node(123, this.createName(varName), catchCond, stmts, lineno);
	}

	Node createThrow(Node expr, int lineno) {
		return new Node(50, expr, lineno);
	}

	Node createReturn(Node expr, int lineno) {
		return expr == null ? new Node(4, lineno) : new Node(4, expr, lineno);
	}

	Node createDebugger(int lineno) {
		return new Node(159, lineno);
	}

	Node createLabel(int lineno) {
		return new Jump(129, lineno);
	}

	Node getLabelLoop(Node label) {
		return ((Jump) label).getLoop();
	}

	Node createLabeledStatement(Node labelArg, Node statement) {
		Jump label = (Jump) labelArg;
		Node breakTarget = Node.newTarget();
		Node block = new Node(128, label, statement, breakTarget);
		label.target = breakTarget;
		return block;
	}

	Node createBreak(Node breakStatement, int lineno) {
		Jump n = new Jump(119, lineno);
		int t = breakStatement.getType();
		Jump jumpStatement;
		if (t != 131 && t != 129) {
			if (t != 128 || breakStatement.getFirstChild().getType() != 113) {
				throw Kit.codeBug();
			}

			jumpStatement = (Jump) breakStatement.getFirstChild();
		} else {
			jumpStatement = (Jump) breakStatement;
		}

		n.setJumpStatement(jumpStatement);
		return n;
	}

	Node createContinue(Node loop, int lineno) {
		if (loop.getType() != 131) {
			Kit.codeBug();
		}

		Jump n = new Jump(120, lineno);
		n.setJumpStatement((Jump) loop);
		return n;
	}

	Node createBlock(int lineno) {
		return new Node(128, lineno);
	}

	FunctionNode createFunction(String name) {
		return new FunctionNode(name);
	}

	Node initFunction(FunctionNode fnNode, int functionIndex, Node statements,
			int functionType) {
		fnNode.itsFunctionType = functionType;
		fnNode.addChildToBack(statements);
		int functionCount = fnNode.getFunctionCount();
		if (functionCount != 0) {
			fnNode.itsNeedsActivation = true;
		}

		Node result;
		if (functionType == 2) {
			String lastStmt = fnNode.getFunctionName();
			if (lastStmt != null && lastStmt.length() != 0) {
				result = new Node(132, new Node(8,
						Node.newString(49, lastStmt), new Node(63)));
				statements.addChildrenToFront(result);
			}
		}

		Node lastStmt1 = statements.getLastChild();
		if (lastStmt1 == null || lastStmt1.getType() != 4) {
			statements.addChildToBack(new Node(4));
		}

		result = Node.newString(108, fnNode.getFunctionName());
		result.putIntProp(1, functionIndex);
		return result;
	}

	void addChildToBack(Node parent, Node child) {
		parent.addChildToBack(child);
	}

	Node createScopeNode(int token, int lineno) {
		return new Scope(token, lineno);
	}

	Node createLoopNode(Node loopLabel, int lineno) {
		Scope result = new Scope(131, lineno);
		if (loopLabel != null) {
			((Jump) loopLabel).setLoop(result);
		}

		return result;
	}

	Node createWhile(Node loop, Node cond, Node body) {
		return this.createLoop((Jump) loop, 1, body, cond, (Node) null,
				(Node) null);
	}

	Node createDoWhile(Node loop, Node body, Node cond) {
		return this.createLoop((Jump) loop, 0, body, cond, (Node) null,
				(Node) null);
	}

	Node createFor(Node loop, Node init, Node test, Node incr, Node body) {
		if (init.getType() == 152) {
			Scope let = Scope.splitScope((Scope) loop);
			let.setType(152);
			let.addChildrenToBack(init);
			let.addChildToBack(this.createLoop((Jump) loop, 2, body, test,
					new Node(127), incr));
			return let;
		} else {
			return this.createLoop((Jump) loop, 2, body, test, init, incr);
		}
	}

	private Node createLoop(Jump loop, int loopType, Node body, Node cond,
			Node init, Node incr) {
		Node bodyTarget = Node.newTarget();
		Node condTarget = Node.newTarget();
		if (loopType == 2 && cond.getType() == 127) {
			cond = new Node(45);
		}

		Jump IFEQ = new Jump(6, cond);
		IFEQ.target = bodyTarget;
		Node breakTarget = Node.newTarget();
		loop.addChildToBack(bodyTarget);
		loop.addChildrenToBack(body);
		if (loopType == 1 || loopType == 2) {
			loop.addChildrenToBack(new Node(127, loop.getLineno()));
		}

		loop.addChildToBack(condTarget);
		loop.addChildToBack(IFEQ);
		loop.addChildToBack(breakTarget);
		loop.target = breakTarget;
		Node continueTarget = condTarget;
		if (loopType == 1 || loopType == 2) {
			loop.addChildToFront(this.makeJump(5, condTarget));
			if (loopType == 2) {
				int initType = init.getType();
				if (initType != 127) {
					if (initType != 121 && initType != 152) {
						init = new Node(132, init);
					}

					loop.addChildToFront(init);
				}

				Node incrTarget = Node.newTarget();
				loop.addChildAfter(incrTarget, body);
				if (incr.getType() != 127) {
					incr = new Node(132, incr);
					loop.addChildAfter(incr, incrTarget);
				}

				continueTarget = incrTarget;
			}
		}

		loop.setContinue(continueTarget);
		return loop;
	}

	Node createForIn(int declType, Node loop, Node lhs, Node obj, Node body,
			boolean isForEach) {
		int destructuring = -1;
		int destructuringLen = 0;
		int type = lhs.getType();
		Node lvalue;
		Node localBlock;
		if (type != 121 && type != 152) {
			if (type != 65 && type != 66) {
				lvalue = this.makeReference(lhs);
				if (lvalue == null) {
					this.parser.reportError("msg.bad.for.in.lhs");
					return obj;
				}
			} else {
				destructuring = type;
				lvalue = lhs;
				destructuringLen = lhs.getIntProp(21, 0);
			}
		} else {
			localBlock = lhs.getLastChild();
			if (lhs.getFirstChild() != localBlock) {
				this.parser.reportError("msg.mult.index");
			}

			if (localBlock.getType() != 65 && localBlock.getType() != 66) {
				if (localBlock.getType() != 39) {
					this.parser.reportError("msg.bad.for.in.lhs");
					return obj;
				}

				lvalue = Node.newString(39, localBlock.getString());
			} else {
				type = destructuring = localBlock.getType();
				lvalue = localBlock;
				destructuringLen = localBlock.getIntProp(21, 0);
			}
		}

		localBlock = new Node(140);
		int initType = isForEach ? 59 : (destructuring != -1 ? 60 : 58);
		Node init = new Node(initType, obj);
		init.putProp(3, localBlock);
		Node cond = new Node(61);
		cond.putProp(3, localBlock);
		Node id = new Node(62);
		id.putProp(3, localBlock);
		Node newBody = new Node(128);
		Node assign;
		if (destructuring != -1) {
			assign = this.createDestructuringAssignment(declType, lvalue, id);
			if (!isForEach && (destructuring == 66 || destructuringLen != 2)) {
				this.parser.reportError("msg.bad.for.in.destruct");
			}
		} else {
			assign = this.simpleAssignment(lvalue, id);
		}

		newBody.addChildToBack(new Node(132, assign));
		newBody.addChildToBack(body);
		loop = this.createWhile(loop, cond, newBody);
		loop.addChildToFront(init);
		if (type == 121 || type == 152) {
			loop.addChildToFront(lhs);
		}

		localBlock.addChildToBack(loop);
		return localBlock;
	}

	Node createTryCatchFinally(Node tryBlock, Node catchBlocks,
			Node finallyBlock, int lineno) {
		boolean hasFinally = finallyBlock != null
				&& (finallyBlock.getType() != 128 || finallyBlock.hasChildren());
		if (tryBlock.getType() == 128 && !tryBlock.hasChildren() && !hasFinally) {
			return tryBlock;
		} else {
			boolean hasCatch = catchBlocks.hasChildren();
			if (!hasFinally && !hasCatch) {
				return tryBlock;
			} else {
				Node handlerBlock = new Node(140);
				Jump pn = new Jump(80, tryBlock, lineno);
				pn.putProp(3, handlerBlock);
				Node finallyTarget;
				Node finallyEnd;
				Node fBlock;
				if (hasCatch) {
					finallyTarget = Node.newTarget();
					pn.addChildToBack(this.makeJump(5, finallyTarget));
					finallyEnd = Node.newTarget();
					pn.target = finallyEnd;
					pn.addChildToBack(finallyEnd);
					fBlock = new Node(140);
					Node cb = catchBlocks.getFirstChild();
					boolean hasDefault = false;

					for (int scopeIndex = 0; cb != null; ++scopeIndex) {
						int rethrow = cb.getLineno();
						Node name = cb.getFirstChild();
						Node cond = name.getNext();
						Node catchStatement = cond.getNext();
						cb.removeChild(name);
						cb.removeChild(cond);
						cb.removeChild(catchStatement);
						catchStatement.addChildToBack(new Node(3));
						catchStatement.addChildToBack(this.makeJump(5,
								finallyTarget));
						Node condStmt;
						if (cond.getType() == 127) {
							condStmt = catchStatement;
							hasDefault = true;
						} else {
							condStmt = this.createIf(cond, catchStatement,
									(Node) null, rethrow);
						}

						Node catchScope = new Node(57, name,
								this.createUseLocal(handlerBlock));
						catchScope.putProp(3, fBlock);
						catchScope.putIntProp(14, scopeIndex);
						fBlock.addChildToBack(catchScope);
						fBlock.addChildToBack(this.createWith(
								this.createUseLocal(fBlock), condStmt, rethrow));
						cb = cb.getNext();
					}

					pn.addChildToBack(fBlock);
					if (!hasDefault) {
						Node arg20 = new Node(51);
						arg20.putProp(3, handlerBlock);
						pn.addChildToBack(arg20);
					}

					pn.addChildToBack(finallyTarget);
				}

				if (hasFinally) {
					finallyTarget = Node.newTarget();
					pn.setFinally(finallyTarget);
					pn.addChildToBack(this.makeJump(134, finallyTarget));
					finallyEnd = Node.newTarget();
					pn.addChildToBack(this.makeJump(5, finallyEnd));
					pn.addChildToBack(finallyTarget);
					fBlock = new Node(124, finallyBlock);
					fBlock.putProp(3, handlerBlock);
					pn.addChildToBack(fBlock);
					pn.addChildToBack(finallyEnd);
				}

				handlerBlock.addChildToBack(pn);
				return handlerBlock;
			}
		}
	}

	Node createWith(Node obj, Node body, int lineno) {
		this.setRequiresActivation();
		Node result = new Node(128, lineno);
		result.addChildToBack(new Node(2, obj));
		Node bodyNode = new Node(122, body, lineno);
		result.addChildrenToBack(bodyNode);
		result.addChildToBack(new Node(3));
		return result;
	}

	public Node createDotQuery(Node obj, Node body, int lineno) {
		this.setRequiresActivation();
		Node result = new Node(145, obj, body, lineno);
		return result;
	}

	Node createArrayLiteral(ObjArray elems, int skipCount, int destructuringLen) {
		int length = elems.size();
		int[] skipIndexes = null;
		if (skipCount != 0) {
			skipIndexes = new int[skipCount];
		}

		Node array = new Node(65);
		int i = 0;

		for (int j = 0; i != length; ++i) {
			Node elem = (Node) elems.get(i);
			if (elem != null) {
				array.addChildToBack(elem);
			} else {
				skipIndexes[j] = i;
				++j;
			}
		}

		if (skipCount != 0) {
			array.putProp(11, skipIndexes);
		}

		array.putIntProp(21, destructuringLen);
		return array;
	}

	Node createObjectLiteral(ObjArray elems) {
		int size = elems.size() / 2;
		Node object = new Node(66);
		Object[] properties;
		if (size == 0) {
			properties = ScriptRuntime.emptyArgs;
		} else {
			properties = new Object[size];

			for (int i = 0; i != size; ++i) {
				properties[i] = elems.get(2 * i);
				Node value = (Node) elems.get(2 * i + 1);
				object.addChildToBack(value);
			}
		}

		object.putProp(12, properties);
		return object;
	}

	Node createRegExp(int regexpIndex) {
		Node n = new Node(48);
		n.putIntProp(4, regexpIndex);
		return n;
	}

	Node createIf(Node cond, Node ifTrue, Node ifFalse, int lineno) {
		int condStatus = isAlwaysDefinedBoolean(cond);
		if (condStatus == 1) {
			return ifTrue;
		} else if (condStatus == -1) {
			return ifFalse != null ? ifFalse : new Node(128, lineno);
		} else {
			Node result = new Node(128, lineno);
			Node ifNotTarget = Node.newTarget();
			Jump IFNE = new Jump(7, cond);
			IFNE.target = ifNotTarget;
			result.addChildToBack(IFNE);
			result.addChildrenToBack(ifTrue);
			if (ifFalse != null) {
				Node endTarget = Node.newTarget();
				result.addChildToBack(this.makeJump(5, endTarget));
				result.addChildToBack(ifNotTarget);
				result.addChildrenToBack(ifFalse);
				result.addChildToBack(endTarget);
			} else {
				result.addChildToBack(ifNotTarget);
			}

			return result;
		}
	}

	Node createCondExpr(Node cond, Node ifTrue, Node ifFalse) {
		int condStatus = isAlwaysDefinedBoolean(cond);
		return condStatus == 1 ? ifTrue : (condStatus == -1
				? ifFalse
				: new Node(101, cond, ifTrue, ifFalse));
	}

	Node createUnary(int nodeType, Node child) {
		int childType = child.getType();
		int status1;
		switch (nodeType) {
			case 26 :
				status1 = isAlwaysDefinedBoolean(child);
				if (status1 != 0) {
					byte type1;
					if (status1 == 1) {
						type1 = 44;
					} else {
						type1 = 45;
					}

					if (childType != 45 && childType != 44) {
						return new Node(type1);
					}

					child.setType(type1);
					return child;
				}
				break;
			case 27 :
				if (childType == 40) {
					status1 = ScriptRuntime.toInt32(child.getDouble());
					child.setDouble((double) (~status1));
					return child;
				}
			case 28 :
			case 30 :
			default :
				break;
			case 29 :
				if (childType == 40) {
					child.setDouble(-child.getDouble());
					return child;
				}
				break;
			case 31 :
				Node status;
				Node right;
				if (childType == 39) {
					child.setType(49);
					right = Node.newString(child.getString());
					status = new Node(nodeType, child, right);
				} else {
					Node type;
					if (childType != 33 && childType != 36) {
						if (childType == 67) {
							type = child.getFirstChild();
							child.removeChild(type);
							status = new Node(69, type);
						} else {
							status = new Node(45);
						}
					} else {
						type = child.getFirstChild();
						right = child.getLastChild();
						child.removeChild(type);
						child.removeChild(right);
						status = new Node(nodeType, type, right);
					}
				}

				return status;
			case 32 :
				if (childType == 39) {
					child.setType(136);
					return child;
				}
		}

		return new Node(nodeType, child);
	}

	Node createYield(Node child, int lineno) {
		if (!this.parser.insideFunction()) {
			this.parser.reportError("msg.bad.yield");
		}

		this.setRequiresActivation();
		this.setIsGenerator();
		return child != null ? new Node(72, child, lineno) : new Node(72,
				lineno);
	}

	Node createCallOrNew(int nodeType, Node child) {
		byte type = 0;
		String node;
		if (child.getType() == 39) {
			node = child.getString();
			if (node.equals("eval")) {
				type = 1;
			} else if (node.equals("With")) {
				type = 2;
			}
		} else if (child.getType() == 33) {
			node = child.getLastChild().getString();
			if (node.equals("eval")) {
				type = 1;
			}
		}

		Node node1 = new Node(nodeType, child);
		if (type != 0) {
			this.setRequiresActivation();
			node1.putIntProp(10, type);
		}

		return node1;
	}

	Node createIncDec(int nodeType, boolean post, Node child) {
		child = this.makeReference(child);
		if (child == null) {
			String childType1;
			if (nodeType == 106) {
				childType1 = "msg.bad.decr";
			} else {
				childType1 = "msg.bad.incr";
			}

			this.parser.reportError(childType1);
			return null;
		} else {
			int childType = child.getType();
			switch (childType) {
				case 33 :
				case 36 :
				case 39 :
				case 67 :
					Node n = new Node(nodeType, child);
					int incrDecrMask = 0;
					if (nodeType == 106) {
						incrDecrMask |= 1;
					}

					if (post) {
						incrDecrMask |= 2;
					}

					n.putIntProp(13, incrDecrMask);
					return n;
				default :
					throw Kit.codeBug();
			}
		}
	}

	Node createPropertyGet(Node target, String namespace, String name,
			int memberTypeFlags) {
		Node elem;
		if (namespace == null && memberTypeFlags == 0) {
			if (target == null) {
				return this.createName(name);
			} else {
				this.checkActivationName(name, 33);
				if (ScriptRuntime.isSpecialProperty(name)) {
					elem = new Node(71, target);
					elem.putProp(17, name);
					return new Node(67, elem);
				} else {
					return new Node(33, target, this.createString(name));
				}
			}
		} else {
			elem = this.createString(name);
			memberTypeFlags |= 1;
			return this.createMemberRefGet(target, namespace, elem,
					memberTypeFlags);
		}
	}

	Node createElementGet(Node target, String namespace, Node elem,
			int memberTypeFlags) {
		if (namespace == null && memberTypeFlags == 0) {
			if (target == null) {
				throw Kit.codeBug();
			} else {
				return new Node(36, target, elem);
			}
		} else {
			return this.createMemberRefGet(target, namespace, elem,
					memberTypeFlags);
		}
	}

	private Node createMemberRefGet(Node target, String namespace, Node elem,
			int memberTypeFlags) {
		Node nsNode = null;
		if (namespace != null) {
			if (namespace.equals("*")) {
				nsNode = new Node(42);
			} else {
				nsNode = this.createName(namespace);
			}
		}

		Node ref;
		if (target == null) {
			if (namespace == null) {
				ref = new Node(78, elem);
			} else {
				ref = new Node(79, nsNode, elem);
			}
		} else if (namespace == null) {
			ref = new Node(76, target, elem);
		} else {
			ref = new Node(77, target, nsNode, elem);
		}

		if (memberTypeFlags != 0) {
			ref.putIntProp(16, memberTypeFlags);
		}

		return new Node(67, ref);
	}

	Node createBinary(int nodeType, Node left, Node right) {
		String s2;
		String leftStatus2;
		label83 : {
			int leftStatus;
			double leftStatus1;
			switch (nodeType) {
				case 21 :
					if (left.type == 41) {
						if (right.type == 41) {
							leftStatus2 = right.getString();
							break label83;
						}

						if (right.type == 40) {
							leftStatus2 = ScriptRuntime.numberToString(
									right.getDouble(), 10);
							break label83;
						}
					} else if (left.type == 40) {
						if (right.type == 40) {
							left.setDouble(left.getDouble() + right.getDouble());
							return left;
						}

						if (right.type == 41) {
							leftStatus2 = ScriptRuntime.numberToString(
									left.getDouble(), 10);
							s2 = right.getString();
							right.setString(leftStatus2.concat(s2));
							return right;
						}
					}
					break;
				case 22 :
					if (left.type == 40) {
						leftStatus1 = left.getDouble();
						if (right.type == 40) {
							left.setDouble(leftStatus1 - right.getDouble());
							return left;
						}

						if (leftStatus1 == 0.0D) {
							return new Node(29, right);
						}
					} else if (right.type == 40 && right.getDouble() == 0.0D) {
						return new Node(28, left);
					}
					break;
				case 23 :
					if (left.type == 40) {
						leftStatus1 = left.getDouble();
						if (right.type == 40) {
							left.setDouble(leftStatus1 * right.getDouble());
							return left;
						}

						if (leftStatus1 == 1.0D) {
							return new Node(28, right);
						}
					} else if (right.type == 40 && right.getDouble() == 1.0D) {
						return new Node(28, left);
					}
					break;
				case 24 :
					if (right.type == 40) {
						leftStatus1 = right.getDouble();
						if (left.type == 40) {
							left.setDouble(left.getDouble() / leftStatus1);
							return left;
						}

						if (leftStatus1 == 1.0D) {
							return new Node(28, left);
						}
					}
					break;
				case 103 :
					leftStatus = isAlwaysDefinedBoolean(left);
					if (leftStatus == 1) {
						return left;
					}

					if (leftStatus == -1) {
						return right;
					}
					break;
				case 104 :
					leftStatus = isAlwaysDefinedBoolean(left);
					if (leftStatus == -1) {
						return left;
					}

					if (leftStatus == 1) {
						return right;
					}
			}

			return new Node(nodeType, left, right);
		}

		s2 = left.getString();
		left.setString(s2.concat(leftStatus2));
		return left;
	}

	private Node simpleAssignment(Node left, Node right) {
		int nodeType = left.getType();
		Node ref;
		switch (nodeType) {
			case 33 :
			case 36 :
				ref = left.getFirstChild();
				Node id = left.getLastChild();
				byte type;
				if (nodeType == 33) {
					type = 35;
				} else {
					type = 37;
				}

				return new Node(type, ref, id, right);
			case 39 :
				left.setType(49);
				return new Node(8, left, right);
			case 67 :
				ref = left.getFirstChild();
				this.checkMutableReference(ref);
				return new Node(68, ref, right);
			default :
				throw Kit.codeBug();
		}
	}

	private void checkMutableReference(Node n) {
		int memberTypeFlags = n.getIntProp(16, 0);
		if ((memberTypeFlags & 4) != 0) {
			this.parser.reportError("msg.bad.assign.left");
		}

	}

	Node createAssignment(int assignType, Node left, Node right) {
		Node ref = this.makeReference(left);
		if (ref == null) {
			if (left.getType() != 65 && left.getType() != 66) {
				this.parser.reportError("msg.bad.assign.left");
				return right;
			} else if (assignType != 89) {
				this.parser.reportError("msg.bad.destruct.op");
				return right;
			} else {
				return this.createDestructuringAssignment(-1, left, right);
			}
		} else {
			byte assignOp;
			switch (assignType) {
				case 89 :
					return this.simpleAssignment(ref, right);
				case 90 :
					assignOp = 9;
					break;
				case 91 :
					assignOp = 10;
					break;
				case 92 :
					assignOp = 11;
					break;
				case 93 :
					assignOp = 18;
					break;
				case 94 :
					assignOp = 19;
					break;
				case 95 :
					assignOp = 20;
					break;
				case 96 :
					assignOp = 21;
					break;
				case 97 :
					assignOp = 22;
					break;
				case 98 :
					assignOp = 23;
					break;
				case 99 :
					assignOp = 24;
					break;
				case 100 :
					assignOp = 25;
					break;
				default :
					throw Kit.codeBug();
			}

			int nodeType = ref.getType();
			Node opLeft;
			Node op;
			switch (nodeType) {
				case 33 :
				case 36 :
					opLeft = ref.getFirstChild();
					op = ref.getLastChild();
					int type = nodeType == 33 ? 138 : 139;
					Node opLeft1 = new Node(137);
					Node op1 = new Node(assignOp, opLeft1, right);
					return new Node(type, opLeft, op, op1);
				case 39 :
					opLeft = new Node(assignOp, ref, right);
					op = Node.newString(49, ref.getString());
					return new Node(8, op, opLeft);
				case 67 :
					ref = ref.getFirstChild();
					this.checkMutableReference(ref);
					opLeft = new Node(137);
					op = new Node(assignOp, opLeft, right);
					return new Node(141, ref, op);
				default :
					throw Kit.codeBug();
			}
		}
	}

	Node createDestructuringAssignment(int type, Node left, Node right) {
		String tempName = this.parser.currentScriptOrFn.getNextTempName();
		Node result = this.destructuringAssignmentHelper(type, left, right,
				tempName);
		Node comma = result.getLastChild();
		comma.addChildToBack(this.createName(tempName));
		return result;
	}

	private Node destructuringAssignmentHelper(int variableType, Node left,
			Node right, String tempName) {
		Node result = this.createScopeNode(157,
				this.parser.getCurrentLineNumber());
		result.addChildToFront(new Node(152, this.createName(39, tempName,
				right)));

		try {
			this.parser.pushScope(result);
			this.parser.defineSymbol(152, true, tempName);
		} finally {
			this.parser.popScope();
		}

		Node comma = new Node(88);
		result.addChildToBack(comma);
		int setOp = variableType == 153 ? 154 : 8;
		ArrayList destructuringNames = new ArrayList();
		boolean empty = true;
		int type = left.getType();
		int index;
		Node rightElem;
		String name;
		if (type == 65) {
			index = 0;
			int[] propertyIds = (int[]) ((int[]) left.getProp(11));
			int n = 0;
			Node id = left.getFirstChild();

			while (true) {
				if (propertyIds != null) {
					while (n < propertyIds.length && propertyIds[n] == index) {
						++n;
						++index;
					}
				}

				if (id == null) {
					break;
				}

				rightElem = new Node(36, this.createName(tempName),
						this.createNumber((double) index));
				if (id.getType() == 39) {
					name = id.getString();
					comma.addChildToBack(new Node(setOp, this.createName(49,
							name, (Node) null), rightElem));
					if (variableType != -1) {
						this.parser.defineSymbol(variableType, true, name);
						destructuringNames.add(name);
					}
				} else {
					comma.addChildToBack(this.destructuringAssignmentHelper(
							variableType, id, rightElem,
							this.parser.currentScriptOrFn.getNextTempName()));
				}

				++index;
				empty = false;
				id = id.getNext();
			}
		} else if (type == 66) {
			index = 0;
			Object[] arg18 = (Object[]) ((Object[]) left.getProp(12));

			for (Node arg19 = left.getFirstChild(); arg19 != null; arg19 = arg19
					.getNext()) {
				Object arg20 = arg18[index];
				rightElem = arg20 instanceof String
						? new Node(33, this.createName(tempName),
								this.createString((String) arg20)) : new Node(
								36, this.createName(tempName),
								this.createNumber((double) ((Number) arg20)
										.intValue()));
				if (arg19.getType() == 39) {
					name = arg19.getString();
					comma.addChildToBack(new Node(setOp, this.createName(49,
							name, (Node) null), rightElem));
					if (variableType != -1) {
						this.parser.defineSymbol(variableType, true, name);
						destructuringNames.add(name);
					}
				} else {
					comma.addChildToBack(this.destructuringAssignmentHelper(
							variableType, arg19, rightElem,
							this.parser.currentScriptOrFn.getNextTempName()));
				}

				++index;
				empty = false;
			}
		} else if (type != 33 && type != 36) {
			this.parser.reportError("msg.bad.assign.left");
		} else {
			comma.addChildToBack(this.simpleAssignment(left,
					this.createName(tempName)));
		}

		if (empty) {
			comma.addChildToBack(this.createNumber(0.0D));
		}

		result.putProp(22, destructuringNames);
		return result;
	}

	Node createUseLocal(Node localBlock) {
		if (140 != localBlock.getType()) {
			throw Kit.codeBug();
		} else {
			Node result = new Node(54);
			result.putProp(3, localBlock);
			return result;
		}
	}

	private Jump makeJump(int type, Node target) {
		Jump n = new Jump(type);
		n.target = target;
		return n;
	}

	private Node makeReference(Node node) {
		int type = node.getType();
		switch (type) {
			case 33 :
			case 36 :
			case 39 :
			case 67 :
				return node;
			case 38 :
				node.setType(70);
				return new Node(67, node);
			default :
				return null;
		}
	}

	private static int isAlwaysDefinedBoolean(Node node) {
		switch (node.getType()) {
			case 40 :
				double num = node.getDouble();
				if (num == num && num != 0.0D) {
					return 1;
				}

				return -1;
			case 41 :
			case 43 :
			default :
				return 0;
			case 42 :
			case 44 :
				return -1;
			case 45 :
				return 1;
		}
	}

	private void checkActivationName(String name, int token) {
		if (this.parser.insideFunction()) {
			boolean activation = false;
			if ("arguments".equals(name)
					|| this.parser.compilerEnv.activationNames != null
					&& this.parser.compilerEnv.activationNames.contains(name)) {
				activation = true;
			} else if ("length".equals(name) && token == 33
					&& this.parser.compilerEnv.getLanguageVersion() == 120) {
				activation = true;
			}

			if (activation) {
				this.setRequiresActivation();
			}
		}

	}

	private void setRequiresActivation() {
		if (this.parser.insideFunction()) {
			((FunctionNode) this.parser.currentScriptOrFn).itsNeedsActivation = true;
		}

	}

	private void setIsGenerator() {
		if (this.parser.insideFunction()) {
			((FunctionNode) this.parser.currentScriptOrFn).itsIsGenerator = true;
		}

	}
}