/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeFunction;
import org.mozilla.javascript.NativeIterator;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

public final class NativeGenerator extends IdScriptableObject {
	private static final long serialVersionUID = 1645892441041347273L;
	private static final Object GENERATOR_TAG = "Generator";
	public static final int GENERATOR_SEND = 0;
	public static final int GENERATOR_THROW = 1;
	public static final int GENERATOR_CLOSE = 2;
	private static final int Id_close = 1;
	private static final int Id_next = 2;
	private static final int Id_send = 3;
	private static final int Id_throw = 4;
	private static final int Id___iterator__ = 5;
	private static final int MAX_PROTOTYPE_ID = 5;
	private NativeFunction function;
	private Object savedState;
	private String lineSource;
	private int lineNumber;
	private boolean firstTime = true;
	private boolean locked;

	static NativeGenerator init(ScriptableObject scope, boolean sealed) {
		NativeGenerator prototype = new NativeGenerator();
		if (scope != null) {
			prototype.setParentScope(scope);
			prototype.setPrototype(getObjectPrototype(scope));
		}

		prototype.activatePrototypeMap(5);
		if (sealed) {
			prototype.sealObject();
		}

		if (scope != null) {
			scope.associateValue(GENERATOR_TAG, prototype);
		}

		return prototype;
	}

	private NativeGenerator() {
	}

	public NativeGenerator(Scriptable scope, NativeFunction function,
			Object savedState) {
		this.function = function;
		this.savedState = savedState;
		Scriptable top = ScriptableObject.getTopLevelScope(scope);
		this.setParentScope(top);
		NativeGenerator prototype = (NativeGenerator) ScriptableObject
				.getTopScopeValue(top, GENERATOR_TAG);
		this.setPrototype(prototype);
	}

	public String getClassName() {
		return "Generator";
	}

	public void finalize() throws Throwable {
		if (this.savedState != null) {
			Context cx = Context.getCurrentContext();
			ContextFactory factory = cx != null
					? cx.getFactory()
					: ContextFactory.getGlobal();
			factory.call(new NativeGenerator.CloseGeneratorAction(this));
		}

	}

	protected void initPrototypeId(int id) {
		String s;
		byte arity;
		switch (id) {
			case 1 :
				arity = 1;
				s = "close";
				break;
			case 2 :
				arity = 1;
				s = "next";
				break;
			case 3 :
				arity = 0;
				s = "send";
				break;
			case 4 :
				arity = 0;
				s = "throw";
				break;
			case 5 :
				arity = 1;
				s = "__iterator__";
				break;
			default :
				throw new IllegalArgumentException(String.valueOf(id));
		}

		this.initPrototypeMethod(GENERATOR_TAG, id, s, arity);
	}

	public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
			Scriptable thisObj, Object[] args) {
		if (!f.hasTag(GENERATOR_TAG)) {
			return super.execIdCall(f, cx, scope, thisObj, args);
		} else {
			int id = f.methodId();
			if (!(thisObj instanceof NativeGenerator)) {
				throw incompatibleCallError(f);
			} else {
				NativeGenerator generator = (NativeGenerator) thisObj;
				switch (id) {
					case 1 :
						return generator.resume(cx, scope, 2,
								new NativeGenerator.GeneratorClosedException());
					case 2 :
						generator.firstTime = false;
						return generator.resume(cx, scope, 0,
								Undefined.instance);
					case 3 :
						Object arg = args.length > 0
								? args[0]
								: Undefined.instance;
						if (generator.firstTime
								&& !arg.equals(Undefined.instance)) {
							throw ScriptRuntime.typeError0("msg.send.newborn");
						}

						return generator.resume(cx, scope, 0, arg);
					case 4 :
						return generator.resume(cx, scope, 1, args.length > 0
								? args[0]
								: Undefined.instance);
					case 5 :
						return thisObj;
					default :
						throw new IllegalArgumentException(String.valueOf(id));
				}
			}
		}
	}

	private Object resume(Context cx, Scriptable scope, int operation,
			Object value) {
		Object e;
		if (this.savedState == null) {
			if (operation == 2) {
				return Undefined.instance;
			} else {
				if (operation == 1) {
					e = value;
				} else {
					e = NativeIterator.getStopIterationObject(scope);
				}

				throw new JavaScriptException(e, this.lineSource,
						this.lineNumber);
			}
		} else {
			boolean arg18 = false;

			label178 : {
				Object arg5;
				try {
					arg18 = true;
					synchronized (this) {
						if (this.locked) {
							throw ScriptRuntime
									.typeError0("msg.already.exec.gen");
						}

						this.locked = true;
					}

					e = this.function.resumeGenerator(cx, scope, operation,
							this.savedState, value);
					arg18 = false;
					break label178;
				} catch (NativeGenerator.GeneratorClosedException arg23) {
					arg5 = Undefined.instance;
					arg18 = false;
				} catch (RhinoException arg24) {
					this.lineNumber = arg24.lineNumber();
					this.lineSource = arg24.lineSource();
					this.savedState = null;
					throw arg24;
				} finally {
					if (arg18) {
						synchronized (this) {
							this.locked = false;
						}

						if (operation == 2) {
							this.savedState = null;
						}

					}
				}

				synchronized (this) {
					this.locked = false;
				}

				if (operation == 2) {
					this.savedState = null;
				}

				return arg5;
			}

			synchronized (this) {
				this.locked = false;
			}

			if (operation == 2) {
				this.savedState = null;
			}

			return e;
		}
	}

	protected int findPrototypeId(String s) {
		byte id = 0;
		String X = null;
		int s_length = s.length();
		char c;
		if (s_length == 4) {
			c = s.charAt(0);
			if (c == 110) {
				X = "next";
				id = 2;
			} else if (c == 115) {
				X = "send";
				id = 3;
			}
		} else if (s_length == 5) {
			c = s.charAt(0);
			if (c == 99) {
				X = "close";
				id = 1;
			} else if (c == 116) {
				X = "throw";
				id = 4;
			}
		} else if (s_length == 12) {
			X = "__iterator__";
			id = 5;
		}

		if (X != null && X != s && !X.equals(s)) {
			id = 0;
		}

		return id;
	}

	public static class GeneratorClosedException extends RuntimeException {
		private static final long serialVersionUID = 2561315658662379681L;
	}

	private static class CloseGeneratorAction implements ContextAction {
		private NativeGenerator generator;

		CloseGeneratorAction(NativeGenerator generator) {
			this.generator = generator;
		}

		public Object run(Context cx) {
			Scriptable scope = ScriptableObject
					.getTopLevelScope(this.generator);
			Callable closeGenerator = new Callable() {
				public Object call(Context cx, Scriptable scope,
						Scriptable thisObj, Object[] args) {
					return ((NativeGenerator) thisObj).resume(cx, scope, 2,
							new NativeGenerator.GeneratorClosedException());
				}
			};
			return ScriptRuntime.doTopCall(closeGenerator, cx, scope,
					this.generator, (Object[]) null);
		}
	}
}