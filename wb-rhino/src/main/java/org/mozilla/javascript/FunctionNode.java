/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import java.util.ArrayList;
import java.util.HashMap;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.ScriptOrFnNode;

public class FunctionNode extends ScriptOrFnNode {
	public static final int FUNCTION_STATEMENT = 1;
	public static final int FUNCTION_EXPRESSION = 2;
	public static final int FUNCTION_EXPRESSION_STATEMENT = 3;
	String functionName;
	int itsFunctionType;
	boolean itsNeedsActivation;
	boolean itsIgnoreDynamicScope;
	boolean itsIsGenerator;
	ArrayList<Node> generatorResumePoints;
	HashMap<Node, int[]> liveLocals;

	public FunctionNode(String name) {
		super(108);
		this.functionName = name;
	}

	public String getFunctionName() {
		return this.functionName;
	}

	public boolean requiresActivation() {
		return this.itsNeedsActivation;
	}

	public boolean getIgnoreDynamicScope() {
		return this.itsIgnoreDynamicScope;
	}

	public boolean isGenerator() {
		return this.itsIsGenerator;
	}

	public void addResumptionPoint(Node target) {
		if (this.generatorResumePoints == null) {
			this.generatorResumePoints = new ArrayList();
		}

		this.generatorResumePoints.add(target);
	}

	public ArrayList<Node> getResumptionPoints() {
		return this.generatorResumePoints;
	}

	public HashMap<Node, int[]> getLiveLocals() {
		return this.liveLocals;
	}

	public void addLiveLocals(Node node, int[] locals) {
		if (this.liveLocals == null) {
			this.liveLocals = new HashMap();
		}

		this.liveLocals.put(node, locals);
	}

	public int getFunctionType() {
		return this.itsFunctionType;
	}
}