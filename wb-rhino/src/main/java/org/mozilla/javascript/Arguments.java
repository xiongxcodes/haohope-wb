/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.NativeCall;
import org.mozilla.javascript.NativeFunction;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.UniqueTag;

final class Arguments extends IdScriptableObject {
	static final long serialVersionUID = 4275508002492040609L;
	private static final int Id_callee = 1;
	private static final int Id_length = 2;
	private static final int Id_caller = 3;
	private static final int MAX_INSTANCE_ID = 3;
	private Object callerObj;
	private Object calleeObj;
	private Object lengthObj;
	private NativeCall activation;
	private Object[] args;

	public Arguments(NativeCall activation) {
		this.activation = activation;
		Scriptable parent = activation.getParentScope();
		this.setParentScope(parent);
		this.setPrototype(ScriptableObject.getObjectPrototype(parent));
		this.args = activation.originalArgs;
		this.lengthObj = new Integer(this.args.length);
		NativeFunction f = activation.function;
		this.calleeObj = f;
		int version = f.getLanguageVersion();
		if (version <= 130 && version != 0) {
			this.callerObj = null;
		} else {
			this.callerObj = NOT_FOUND;
		}

	}

	public String getClassName() {
		return "Object";
	}

	public boolean has(int index, Scriptable start) {
		return 0 <= index && index < this.args.length
				&& this.args[index] != NOT_FOUND ? true : super.has(index,
				start);
	}

	public Object get(int index, Scriptable start) {
		if (0 <= index && index < this.args.length) {
			Object value = this.args[index];
			if (value != NOT_FOUND) {
				if (this.sharedWithActivation(index)) {
					NativeFunction f = this.activation.function;
					String argName = f.getParamOrVarName(index);
					value = this.activation.get(argName, this.activation);
					if (value == NOT_FOUND) {
						Kit.codeBug();
					}
				}

				return value;
			}
		}

		return super.get(index, start);
	}

	private boolean sharedWithActivation(int index) {
		NativeFunction f = this.activation.function;
		int definedCount = f.getParamCount();
		if (index >= definedCount) {
			return false;
		} else {
			if (index < definedCount - 1) {
				String argName = f.getParamOrVarName(index);

				for (int i = index + 1; i < definedCount; ++i) {
					if (argName.equals(f.getParamOrVarName(i))) {
						return false;
					}
				}
			}

			return true;
		}
	}

	public void put(int index, Scriptable start, Object value) {
		if (0 <= index && index < this.args.length
				&& this.args[index] != NOT_FOUND) {
			if (this.sharedWithActivation(index)) {
				String argName = this.activation.function
						.getParamOrVarName(index);
				this.activation.put(argName, this.activation, value);
				return;
			}

			synchronized (this) {
				if (this.args[index] != NOT_FOUND) {
					if (this.args == this.activation.originalArgs) {
						this.args = (Object[]) this.args.clone();
					}

					this.args[index] = value;
					return;
				}
			}
		}

		super.put(index, start, value);
	}

	public void delete(int index) {
		if (0 <= index && index < this.args.length) {
			synchronized (this) {
				if (this.args[index] != NOT_FOUND) {
					if (this.args == this.activation.originalArgs) {
						this.args = (Object[]) this.args.clone();
					}

					this.args[index] = NOT_FOUND;
					return;
				}
			}
		}

		super.delete(index);
	}

	protected int getMaxInstanceId() {
		return 3;
	}

	protected int findInstanceIdInfo(String s) {
		byte id = 0;
		String attr = null;
		if (s.length() == 6) {
			char c = s.charAt(5);
			if (c == 101) {
				attr = "callee";
				id = 1;
			} else if (c == 104) {
				attr = "length";
				id = 2;
			} else if (c == 114) {
				attr = "caller";
				id = 3;
			}
		}

		if (attr != null && attr != s && !attr.equals(s)) {
			id = 0;
		}

		if (id == 0) {
			return super.findInstanceIdInfo(s);
		} else {
			switch (id) {
				case 1 :
				case 2 :
				case 3 :
					byte attr1 = 2;
					return instanceIdInfo(attr1, id);
				default :
					throw new IllegalStateException();
			}
		}
	}

	protected String getInstanceIdName(int id) {
		switch (id) {
			case 1 :
				return "callee";
			case 2 :
				return "length";
			case 3 :
				return "caller";
			default :
				return null;
		}
	}

	protected Object getInstanceIdValue(int id) {
		switch (id) {
			case 1 :
				return this.calleeObj;
			case 2 :
				return this.lengthObj;
			case 3 :
				Object value = this.callerObj;
				if (value == UniqueTag.NULL_VALUE) {
					value = null;
				} else if (value == null) {
					NativeCall caller = this.activation.parentActivationCall;
					if (caller != null) {
						value = caller.get("arguments", caller);
					}
				}

				return value;
			default :
				return super.getInstanceIdValue(id);
		}
	}

	protected void setInstanceIdValue(int id, Object value) {
		switch (id) {
			case 1 :
				this.calleeObj = value;
				return;
			case 2 :
				this.lengthObj = value;
				return;
			case 3 :
				this.callerObj = value != null ? value : UniqueTag.NULL_VALUE;
				return;
			default :
				super.setInstanceIdValue(id, value);
		}
	}

	Object[] getIds(boolean getAll) {
		Object[] ids = super.getIds(getAll);
		if (getAll && this.args.length != 0) {
			boolean[] present = null;
			int extraCount = this.args.length;

			int i;
			for (int tmp = 0; tmp != ids.length; ++tmp) {
				Object offset = ids[tmp];
				if (offset instanceof Integer) {
					i = ((Integer) offset).intValue();
					if (0 <= i && i < this.args.length) {
						if (present == null) {
							present = new boolean[this.args.length];
						}

						if (!present[i]) {
							present[i] = true;
							--extraCount;
						}
					}
				}
			}

			if (extraCount != 0) {
				Object[] arg7 = new Object[extraCount + ids.length];
				System.arraycopy(ids, 0, arg7, extraCount, ids.length);
				ids = arg7;
				int arg8 = 0;

				for (i = 0; i != this.args.length; ++i) {
					if (present == null || !present[i]) {
						ids[arg8] = new Integer(i);
						++arg8;
					}
				}

				if (arg8 != extraCount) {
					Kit.codeBug();
				}
			}
		}

		return ids;
	}
}