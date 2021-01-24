/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.ObjToIntMap;
import org.mozilla.javascript.ScriptOrFnNode;

public class Node {
	public static final int FUNCTION_PROP = 1;
	public static final int LOCAL_PROP = 2;
	public static final int LOCAL_BLOCK_PROP = 3;
	public static final int REGEXP_PROP = 4;
	public static final int CASEARRAY_PROP = 5;
	public static final int TARGETBLOCK_PROP = 6;
	public static final int VARIABLE_PROP = 7;
	public static final int ISNUMBER_PROP = 8;
	public static final int DIRECTCALL_PROP = 9;
	public static final int SPECIALCALL_PROP = 10;
	public static final int SKIP_INDEXES_PROP = 11;
	public static final int OBJECT_IDS_PROP = 12;
	public static final int INCRDECR_PROP = 13;
	public static final int CATCH_SCOPE_PROP = 14;
	public static final int LABEL_ID_PROP = 15;
	public static final int MEMBER_TYPE_PROP = 16;
	public static final int NAME_PROP = 17;
	public static final int CONTROL_BLOCK_PROP = 18;
	public static final int PARENTHESIZED_PROP = 19;
	public static final int GENERATOR_END_PROP = 20;
	public static final int DESTRUCTURING_ARRAY_LENGTH = 21;
	public static final int DESTRUCTURING_NAMES = 22;
	public static final int LAST_PROP = 22;
	public static final int BOTH = 0;
	public static final int LEFT = 1;
	public static final int RIGHT = 2;
	public static final int NON_SPECIALCALL = 0;
	public static final int SPECIALCALL_EVAL = 1;
	public static final int SPECIALCALL_WITH = 2;
	public static final int DECR_FLAG = 1;
	public static final int POST_FLAG = 2;
	public static final int PROPERTY_FLAG = 1;
	public static final int ATTRIBUTE_FLAG = 2;
	public static final int DESCENDANTS_FLAG = 4;
	static final int END_UNREACHED = 0;
	static final int END_DROPS_OFF = 1;
	static final int END_RETURNS = 2;
	static final int END_RETURNS_VALUE = 4;
	static final int END_YIELDS = 8;
	int type;
	Node next;
	private Node first;
	private Node last;
	protected int lineno;
	private Node.PropListItem propListHead;

	public Node(int nodeType) {
		this.lineno = -1;
		this.type = nodeType;
	}

	public Node(int nodeType, Node child) {
		this.lineno = -1;
		this.type = nodeType;
		this.first = this.last = child;
		child.next = null;
	}

	public Node(int nodeType, Node left, Node right) {
		this.lineno = -1;
		this.type = nodeType;
		this.first = left;
		this.last = right;
		left.next = right;
		right.next = null;
	}

	public Node(int nodeType, Node left, Node mid, Node right) {
		this.lineno = -1;
		this.type = nodeType;
		this.first = left;
		this.last = right;
		left.next = mid;
		mid.next = right;
		right.next = null;
	}

	public Node(int nodeType, int line) {
		this.lineno = -1;
		this.type = nodeType;
		this.lineno = line;
	}

	public Node(int nodeType, Node child, int line) {
		this(nodeType, child);
		this.lineno = line;
	}

	public Node(int nodeType, Node left, Node right, int line) {
		this(nodeType, left, right);
		this.lineno = line;
	}

	public Node(int nodeType, Node left, Node mid, Node right, int line) {
		this(nodeType, left, mid, right);
		this.lineno = line;
	}

	public static Node newNumber(double number) {
		return new Node.NumberNode(number);
	}

	public static Node newString(String str) {
		return new Node.StringNode(41, str);
	}

	public static Node newString(int type, String str) {
		return new Node.StringNode(type, str);
	}

	public int getType() {
		return this.type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public boolean hasChildren() {
		return this.first != null;
	}

	public Node getFirstChild() {
		return this.first;
	}

	public Node getLastChild() {
		return this.last;
	}

	public Node getNext() {
		return this.next;
	}

	public Node getChildBefore(Node child) {
		if (child == this.first) {
			return null;
		} else {
			Node n = this.first;

			do {
				if (n.next == child) {
					return n;
				}

				n = n.next;
			} while (n != null);

			throw new RuntimeException("node is not a child");
		}
	}

	public Node getLastSibling() {
		Node n;
		for (n = this; n.next != null; n = n.next) {
			;
		}

		return n;
	}

	public void addChildToFront(Node child) {
		child.next = this.first;
		this.first = child;
		if (this.last == null) {
			this.last = child;
		}

	}

	public void addChildToBack(Node child) {
		child.next = null;
		if (this.last == null) {
			this.first = this.last = child;
		} else {
			this.last.next = child;
			this.last = child;
		}
	}

	public void addChildrenToFront(Node children) {
		Node lastSib = children.getLastSibling();
		lastSib.next = this.first;
		this.first = children;
		if (this.last == null) {
			this.last = lastSib;
		}

	}

	public void addChildrenToBack(Node children) {
		if (this.last != null) {
			this.last.next = children;
		}

		this.last = children.getLastSibling();
		if (this.first == null) {
			this.first = children;
		}

	}

	public void addChildBefore(Node newChild, Node node) {
		if (newChild.next != null) {
			throw new RuntimeException(
					"newChild had siblings in addChildBefore");
		} else if (this.first == node) {
			newChild.next = this.first;
			this.first = newChild;
		} else {
			Node prev = this.getChildBefore(node);
			this.addChildAfter(newChild, prev);
		}
	}

	public void addChildAfter(Node newChild, Node node) {
		if (newChild.next != null) {
			throw new RuntimeException("newChild had siblings in addChildAfter");
		} else {
			newChild.next = node.next;
			node.next = newChild;
			if (this.last == node) {
				this.last = newChild;
			}

		}
	}

	public void removeChild(Node child) {
		Node prev = this.getChildBefore(child);
		if (prev == null) {
			this.first = this.first.next;
		} else {
			prev.next = child.next;
		}

		if (child == this.last) {
			this.last = prev;
		}

		child.next = null;
	}

	public void replaceChild(Node child, Node newChild) {
		newChild.next = child.next;
		if (child == this.first) {
			this.first = newChild;
		} else {
			Node prev = this.getChildBefore(child);
			prev.next = newChild;
		}

		if (child == this.last) {
			this.last = newChild;
		}

		child.next = null;
	}

	public void replaceChildAfter(Node prevChild, Node newChild) {
		Node child = prevChild.next;
		newChild.next = child.next;
		prevChild.next = newChild;
		if (child == this.last) {
			this.last = newChild;
		}

		child.next = null;
	}

	private static final String propToString(int propType) {
		return null;
	}

	private Node.PropListItem lookupProperty(int propType) {
		Node.PropListItem x;
		for (x = this.propListHead; x != null && propType != x.type; x = x.next) {
			;
		}

		return x;
	}

	private Node.PropListItem ensureProperty(int propType) {
		Node.PropListItem item = this.lookupProperty(propType);
		if (item == null) {
			item = new Node.PropListItem();
			item.type = propType;
			item.next = this.propListHead;
			this.propListHead = item;
		}

		return item;
	}

	public void removeProp(int propType) {
		Node.PropListItem x = this.propListHead;
		if (x != null) {
			Node.PropListItem prev = null;

			while (x.type != propType) {
				prev = x;
				x = x.next;
				if (x == null) {
					return;
				}
			}

			if (prev == null) {
				this.propListHead = x.next;
			} else {
				prev.next = x.next;
			}
		}

	}

	public Object getProp(int propType) {
		Node.PropListItem item = this.lookupProperty(propType);
		return item == null ? null : item.objectValue;
	}

	public int getIntProp(int propType, int defaultValue) {
		Node.PropListItem item = this.lookupProperty(propType);
		return item == null ? defaultValue : item.intValue;
	}

	public int getExistingIntProp(int propType) {
		Node.PropListItem item = this.lookupProperty(propType);
		if (item == null) {
			Kit.codeBug();
		}

		return item.intValue;
	}

	public void putProp(int propType, Object prop) {
		if (prop == null) {
			this.removeProp(propType);
		} else {
			Node.PropListItem item = this.ensureProperty(propType);
			item.objectValue = prop;
		}

	}

	public void putIntProp(int propType, int prop) {
		Node.PropListItem item = this.ensureProperty(propType);
		item.intValue = prop;
	}

	public int getLineno() {
		return this.lineno;
	}

	public final double getDouble() {
		return ((Node.NumberNode) this).number;
	}

	public final void setDouble(double number) {
		((Node.NumberNode) this).number = number;
	}

	public final String getString() {
		return ((Node.StringNode) this).str;
	}

	public final void setString(String s) {
		if (s == null) {
			Kit.codeBug();
		}

		((Node.StringNode) this).str = s;
	}

	public final Node.Scope getScope() {
		return ((Node.StringNode) this).scope;
	}

	public final void setScope(Node.Scope s) {
		if (s == null) {
			Kit.codeBug();
		}

		if (!(this instanceof Node.StringNode)) {
			throw Kit.codeBug();
		} else {
			((Node.StringNode) this).scope = s;
		}
	}

	public static Node newTarget() {
		return new Node(130);
	}

	public final int labelId() {
		if (this.type != 130 && this.type != 72) {
			Kit.codeBug();
		}

		return this.getIntProp(15, -1);
	}

	public void labelId(int labelId) {
		if (this.type != 130 && this.type != 72) {
			Kit.codeBug();
		}

		this.putIntProp(15, labelId);
	}

	public boolean hasConsistentReturnUsage() {
		int n = this.endCheck();
		return (n & 4) == 0 || (n & 11) == 0;
	}

	private int endCheckIf() {
		boolean rv = false;
		Node th = this.next;
		Node el = ((Node.Jump) this).target;
		int rv1 = th.endCheck();
		if (el != null) {
			rv1 |= el.endCheck();
		} else {
			rv1 |= 1;
		}

		return rv1;
	}

	private int endCheckSwitch() {
		int rv = 0;

		Node n;
		for (n = this.first.next; n != null && n.type == 114; n = n.next) {
			rv |= ((Node.Jump) n).target.endCheck();
		}

		rv &= -2;
		n = ((Node.Jump) this).getDefault();
		if (n != null) {
			rv |= n.endCheck();
		} else {
			rv |= 1;
		}

		rv |= this.getIntProp(18, 0);
		return rv;
	}

	private int endCheckTry() {
		boolean rv = false;
		Node n = ((Node.Jump) this).getFinally();
		int rv1;
		if (n != null) {
			rv1 = n.next.first.endCheck();
		} else {
			rv1 = 1;
		}

		if ((rv1 & 1) != 0) {
			rv1 &= -2;
			rv1 |= this.first.endCheck();
			n = ((Node.Jump) this).target;
			if (n != null) {
				for (n = n.next.first; n != null; n = n.next.next) {
					rv1 |= n.next.first.next.first.endCheck();
				}
			}
		}

		return rv1;
	}

	private int endCheckLoop() {
		boolean rv = false;

		Node n;
		for (n = this.first; n.next != this.last; n = n.next) {
			;
		}

		if (n.type != 6) {
			return 1;
		} else {
			int rv1 = ((Node.Jump) n).target.next.endCheck();
			if (n.first.type == 45) {
				rv1 &= -2;
			}

			rv1 |= this.getIntProp(18, 0);
			return rv1;
		}
	}

	private int endCheckBlock() {
		int rv = 1;

		for (Node n = this.first; (rv & 1) != 0 && n != null; n = n.next) {
			rv &= -2;
			rv |= n.endCheck();
		}

		return rv;
	}

	private int endCheckLabel() {
		boolean rv = false;
		int rv1 = this.next.endCheck();
		rv1 |= this.getIntProp(18, 0);
		return rv1;
	}

	private int endCheckBreak() {
		Node.Jump n = ((Node.Jump) this).jumpNode;
		n.putIntProp(18, 1);
		return 0;
	}

	private int endCheck() {
		switch (this.type) {
			case 4 :
				if (this.first != null) {
					return 4;
				}

				return 2;
			case 50 :
			case 120 :
				return 0;
			case 72 :
				return 8;
			case 119 :
				return this.endCheckBreak();
			case 128 :
			case 140 :
				if (this.first == null) {
					return 1;
				} else {
					switch (this.first.type) {
						case 7 :
							return this.first.endCheckIf();
						case 80 :
							return this.first.endCheckTry();
						case 113 :
							return this.first.endCheckSwitch();
						case 129 :
							return this.first.endCheckLabel();
						default :
							return this.endCheckBlock();
					}
				}
			case 130 :
				if (this.next != null) {
					return this.next.endCheck();
				}

				return 1;
			case 131 :
				return this.endCheckLoop();
			case 132 :
				if (this.first != null) {
					return this.first.endCheck();
				}

				return 1;
			default :
				return 1;
		}
	}

	public boolean hasSideEffects() {
		switch (this.type) {
			case -1 :
			case 2 :
			case 3 :
			case 4 :
			case 5 :
			case 6 :
			case 7 :
			case 8 :
			case 30 :
			case 31 :
			case 35 :
			case 37 :
			case 38 :
			case 50 :
			case 51 :
			case 56 :
			case 57 :
			case 64 :
			case 68 :
			case 69 :
			case 70 :
			case 72 :
			case 80 :
			case 81 :
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
			case 105 :
			case 106 :
			case 109 :
			case 110 :
			case 111 :
			case 112 :
			case 113 :
			case 116 :
			case 117 :
			case 118 :
			case 119 :
			case 120 :
			case 121 :
			case 122 :
			case 123 :
			case 124 :
			case 128 :
			case 129 :
			case 130 :
			case 131 :
			case 133 :
			case 134 :
			case 138 :
			case 139 :
			case 140 :
			case 141 :
			case 152 :
			case 153 :
			case 157 :
			case 158 :
				return true;
			case 0 :
			case 1 :
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
			case 26 :
			case 27 :
			case 28 :
			case 29 :
			case 32 :
			case 33 :
			case 34 :
			case 36 :
			case 39 :
			case 40 :
			case 41 :
			case 42 :
			case 43 :
			case 44 :
			case 45 :
			case 46 :
			case 47 :
			case 48 :
			case 49 :
			case 52 :
			case 53 :
			case 54 :
			case 55 :
			case 58 :
			case 59 :
			case 60 :
			case 61 :
			case 62 :
			case 63 :
			case 65 :
			case 66 :
			case 67 :
			case 71 :
			case 73 :
			case 74 :
			case 75 :
			case 76 :
			case 77 :
			case 78 :
			case 79 :
			case 82 :
			case 83 :
			case 84 :
			case 85 :
			case 86 :
			case 87 :
			case 102 :
			case 107 :
			case 108 :
			case 114 :
			case 115 :
			case 125 :
			case 126 :
			case 127 :
			case 135 :
			case 136 :
			case 137 :
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
			case 154 :
			case 155 :
			case 156 :
			default :
				return false;
			case 88 :
			case 132 :
				if (this.last != null) {
					return this.last.hasSideEffects();
				}

				return true;
			case 101 :
				if (this.first == null || this.first.next == null
						|| this.first.next.next == null) {
					Kit.codeBug();
				}

				return this.first.next.hasSideEffects()
						&& this.first.next.next.hasSideEffects();
			case 103 :
			case 104 :
				if (this.first == null || this.last == null) {
					Kit.codeBug();
				}

				return this.first.hasSideEffects()
						|| this.last.hasSideEffects();
		}
	}

	public String toString() {
		return String.valueOf(this.type);
	}

	private void toString(ObjToIntMap printIds, StringBuffer sb) {
	}

	public String toStringTree(ScriptOrFnNode treeTop) {
		return null;
	}

	private static void toStringTreeHelper(ScriptOrFnNode treeTop, Node n,
			ObjToIntMap printIds, int level, StringBuffer sb) {
	}

	private static void generatePrintIds(Node n, ObjToIntMap map) {
	}

	private static void appendPrintId(Node n, ObjToIntMap printIds,
			StringBuffer sb) {
	}

	private static class PropListItem {
		Node.PropListItem next;
		int type;
		int intValue;
		Object objectValue;

		private PropListItem() {
		}
	}

	static class Scope extends Node.Jump {
		protected LinkedHashMap<String, Node.Symbol> symbolTable;
		private Node.Scope parent;
		private ScriptOrFnNode top;

		public Scope(int nodeType) {
			super(nodeType);
		}

		public Scope(int nodeType, int lineno) {
			super(nodeType, lineno);
		}

		public Scope(int nodeType, Node n, int lineno) {
			super(nodeType, n, lineno);
		}

		public static Node.Scope splitScope(Node.Scope scope) {
			Node.Scope result = new Node.Scope(scope.getType());
			result.symbolTable = scope.symbolTable;
			scope.symbolTable = null;
			result.parent = scope.parent;
			scope.parent = result;
			result.top = scope.top;
			return result;
		}

		public static void joinScopes(Node.Scope source, Node.Scope dest) {
			source.ensureSymbolTable();
			dest.ensureSymbolTable();
			if (!Collections.disjoint(source.symbolTable.keySet(),
					dest.symbolTable.keySet())) {
				throw Kit.codeBug();
			} else {
				dest.symbolTable.putAll(source.symbolTable);
			}
		}

		public void setParent(Node.Scope parent) {
			this.parent = parent;
			this.top = parent == null ? (ScriptOrFnNode) this : parent.top;
		}

		public Node.Scope getParentScope() {
			return this.parent;
		}

		public Node.Scope getDefiningScope(String name) {
			for (Node.Scope sn = this; sn != null; sn = sn.parent) {
				if (sn.symbolTable != null && sn.symbolTable.containsKey(name)) {
					return sn;
				}
			}

			return null;
		}

		public Node.Symbol getSymbol(String name) {
			return this.symbolTable == null
					? null
					: (Node.Symbol) this.symbolTable.get(name);
		}

		public void putSymbol(String name, Node.Symbol symbol) {
			this.ensureSymbolTable();
			this.symbolTable.put(name, symbol);
			symbol.containingTable = this;
			this.top.addSymbol(symbol);
		}

		public Map<String, Node.Symbol> getSymbolTable() {
			return this.symbolTable;
		}

		private void ensureSymbolTable() {
			if (this.symbolTable == null) {
				this.symbolTable = new LinkedHashMap(5);
			}

		}
	}

	static class Symbol {
		int declType;
		int index;
		String name;
		Node.Scope containingTable;

		Symbol(int declType, String name) {
			this.declType = declType;
			this.name = name;
			this.index = -1;
		}
	}

	public static class Jump extends Node {
		public Node target;
		private Node target2;
		private Node.Jump jumpNode;

		public Jump(int type) {
			super(type);
		}

		Jump(int type, int lineno) {
			super(type, lineno);
		}

		Jump(int type, Node child) {
			super(type, child);
		}

		Jump(int type, Node child, int lineno) {
			super(type, child, lineno);
		}

		public final Node.Jump getJumpStatement() {
			if (this.type != 119 && this.type != 120) {
				Kit.codeBug();
			}

			return this.jumpNode;
		}

		public final void setJumpStatement(Node.Jump jumpStatement) {
			if (this.type != 119 && this.type != 120) {
				Kit.codeBug();
			}

			if (jumpStatement == null) {
				Kit.codeBug();
			}

			if (this.jumpNode != null) {
				Kit.codeBug();
			}

			this.jumpNode = jumpStatement;
		}

		public final Node getDefault() {
			if (this.type != 113) {
				Kit.codeBug();
			}

			return this.target2;
		}

		public final void setDefault(Node defaultTarget) {
			if (this.type != 113) {
				Kit.codeBug();
			}

			if (defaultTarget.type != 130) {
				Kit.codeBug();
			}

			if (this.target2 != null) {
				Kit.codeBug();
			}

			this.target2 = defaultTarget;
		}

		public final Node getFinally() {
			if (this.type != 80) {
				Kit.codeBug();
			}

			return this.target2;
		}

		public final void setFinally(Node finallyTarget) {
			if (this.type != 80) {
				Kit.codeBug();
			}

			if (finallyTarget.type != 130) {
				Kit.codeBug();
			}

			if (this.target2 != null) {
				Kit.codeBug();
			}

			this.target2 = finallyTarget;
		}

		public final Node.Jump getLoop() {
			if (this.type != 129) {
				Kit.codeBug();
			}

			return this.jumpNode;
		}

		public final void setLoop(Node.Jump loop) {
			if (this.type != 129) {
				Kit.codeBug();
			}

			if (loop == null) {
				Kit.codeBug();
			}

			if (this.jumpNode != null) {
				Kit.codeBug();
			}

			this.jumpNode = loop;
		}

		public final Node getContinue() {
			if (this.type != 131) {
				Kit.codeBug();
			}

			return this.target2;
		}

		public final void setContinue(Node continueTarget) {
			if (this.type != 131) {
				Kit.codeBug();
			}

			if (continueTarget.type != 130) {
				Kit.codeBug();
			}

			if (this.target2 != null) {
				Kit.codeBug();
			}

			this.target2 = continueTarget;
		}
	}

	private static class StringNode extends Node {
		String str;
		Node.Scope scope;

		StringNode(int type, String str) {
			super(type);
			this.str = str;
		}
	}

	private static class NumberNode extends Node {
		double number;

		NumberNode(double number) {
			super(40);
			this.number = number;
		}
	}
}