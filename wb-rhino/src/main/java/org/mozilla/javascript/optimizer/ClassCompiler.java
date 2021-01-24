/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.optimizer;

import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.FunctionNode;
import org.mozilla.javascript.JavaAdapter;
import org.mozilla.javascript.ObjToIntMap;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ScriptOrFnNode;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.optimizer.Codegen;

public class ClassCompiler {
	private String mainMethodClassName;
	private CompilerEnvirons compilerEnv;
	private Class<?> targetExtends;
	private Class<?>[] targetImplements;

	public ClassCompiler(CompilerEnvirons compilerEnv) {
		if (compilerEnv == null) {
			throw new IllegalArgumentException();
		} else {
			this.compilerEnv = compilerEnv;
			this.mainMethodClassName = "org.mozilla.javascript.optimizer.OptRuntime";
		}
	}

	public void setMainMethodClass(String className) {
		this.mainMethodClassName = className;
	}

	public String getMainMethodClass() {
		return this.mainMethodClassName;
	}

	public CompilerEnvirons getCompilerEnv() {
		return this.compilerEnv;
	}

	public Class<?> getTargetExtends() {
		return this.targetExtends;
	}

	public void setTargetExtends(Class<?> extendsClass) {
		this.targetExtends = extendsClass;
	}

	public Class<?>[] getTargetImplements() {
		return this.targetImplements == null
				? null
				: (Class[]) ((Class[]) this.targetImplements.clone());
	}

	public void setTargetImplements(Class<?>[] implementsClasses) {
		this.targetImplements = implementsClasses == null
				? null
				: (Class[]) ((Class[]) implementsClasses.clone());
	}

	protected String makeAuxiliaryClassName(String mainClassName,
			String auxMarker) {
		return mainClassName + auxMarker;
	}

	public Object[] compileToClassFiles(String source, String sourceLocation,
			int lineno, String mainClassName) {
		Parser p = new Parser(this.compilerEnv,
				this.compilerEnv.getErrorReporter());
		ScriptOrFnNode tree = p.parse(source, sourceLocation, lineno);
		String encodedSource = p.getEncodedSource();
		Class superClass = this.getTargetExtends();
		Class[] interfaces = this.getTargetImplements();
		boolean isPrimary = interfaces == null && superClass == null;
		String scriptClassName;
		if (isPrimary) {
			scriptClassName = mainClassName;
		} else {
			scriptClassName = this.makeAuxiliaryClassName(mainClassName, "1");
		}

		Codegen codegen = new Codegen();
		codegen.setMainMethodClass(this.mainMethodClassName);
		byte[] scriptClassBytes = codegen.compileToClassFile(this.compilerEnv,
				scriptClassName, tree, encodedSource, false);
		if (isPrimary) {
			return new Object[]{scriptClassName, scriptClassBytes};
		} else {
			int functionCount = tree.getFunctionCount();
			ObjToIntMap functionNames = new ObjToIntMap(functionCount);

			for (int mainClassBytes = 0; mainClassBytes != functionCount; ++mainClassBytes) {
				FunctionNode ofn = tree.getFunctionNode(mainClassBytes);
				String name = ofn.getFunctionName();
				if (name != null && name.length() != 0) {
					functionNames.put(name, ofn.getParamCount());
				}
			}

			if (superClass == null) {
				superClass = ScriptRuntime.ObjectClass;
			}

			byte[] arg18 = JavaAdapter.createAdapterCode(functionNames,
					mainClassName, superClass, interfaces, scriptClassName);
			return new Object[]{mainClassName, arg18, scriptClassName,
					scriptClassBytes};
		}
	}
}