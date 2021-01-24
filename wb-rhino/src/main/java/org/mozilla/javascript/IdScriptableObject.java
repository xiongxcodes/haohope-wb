/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.IdFunctionCall;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.UniqueTag;

public abstract class IdScriptableObject extends ScriptableObject
		implements
			IdFunctionCall {
	private transient volatile IdScriptableObject.PrototypeValues prototypeValues;

	public IdScriptableObject() {
	}

	public IdScriptableObject(Scriptable scope, Scriptable prototype) {
		super(scope, prototype);
	}

	protected final Object defaultGet(String name) {
		return super.get(name, this);
	}

	protected final void defaultPut(String name, Object value) {
		super.put(name, this, value);
	}

	public boolean has(String name, Scriptable start) {
		int info = this.findInstanceIdInfo(name);
		int id;
		if (info != 0) {
			id = info >>> 16;
			if ((id & 4) != 0) {
				return true;
			} else {
				int id1 = info & '￿';
				return NOT_FOUND != this.getInstanceIdValue(id1);
			}
		} else {
			if (this.prototypeValues != null) {
				id = this.prototypeValues.findId(name);
				if (id != 0) {
					return this.prototypeValues.has(id);
				}
			}

			return super.has(name, start);
		}
	}

	public Object get(String name, Scriptable start) {
		int info = this.findInstanceIdInfo(name);
		int id;
		if (info != 0) {
			id = info & '￿';
			return this.getInstanceIdValue(id);
		} else {
			if (this.prototypeValues != null) {
				id = this.prototypeValues.findId(name);
				if (id != 0) {
					return this.prototypeValues.get(id);
				}
			}

			return super.get(name, start);
		}
	}

	public void put(String name, Scriptable start, Object value) {
		int info = this.findInstanceIdInfo(name);
		int id;
		if (info != 0) {
			if (start == this && this.isSealed()) {
				throw Context.reportRuntimeError1("msg.modify.sealed", name);
			} else {
				id = info >>> 16;
				if ((id & 1) == 0) {
					if (start == this) {
						int id1 = info & '￿';
						this.setInstanceIdValue(id1, value);
					} else {
						start.put(name, start, value);
					}
				}

			}
		} else {
			if (this.prototypeValues != null) {
				id = this.prototypeValues.findId(name);
				if (id != 0) {
					if (start == this && this.isSealed()) {
						throw Context.reportRuntimeError1("msg.modify.sealed",
								name);
					}

					this.prototypeValues.set(id, start, value);
					return;
				}
			}

			super.put(name, start, value);
		}
	}

	public void delete(String name) {
		int info = this.findInstanceIdInfo(name);
		int id;
		if (info != 0 && !this.isSealed()) {
			id = info >>> 16;
			if ((id & 4) == 0) {
				int id1 = info & '￿';
				this.setInstanceIdValue(id1, NOT_FOUND);
			}

		} else {
			if (this.prototypeValues != null) {
				id = this.prototypeValues.findId(name);
				if (id != 0) {
					if (!this.isSealed()) {
						this.prototypeValues.delete(id);
					}

					return;
				}
			}

			super.delete(name);
		}
	}

	public int getAttributes(String name) {
		int info = this.findInstanceIdInfo(name);
		int id;
		if (info != 0) {
			id = info >>> 16;
			return id;
		} else {
			if (this.prototypeValues != null) {
				id = this.prototypeValues.findId(name);
				if (id != 0) {
					return this.prototypeValues.getAttributes(id);
				}
			}

			return super.getAttributes(name);
		}
	}

	public void setAttributes(String name, int attributes) {
		ScriptableObject.checkValidAttributes(attributes);
		int info = this.findInstanceIdInfo(name);
		int id;
		if (info != 0) {
			id = info >>> 16;
			if (attributes != id) {
				throw new RuntimeException(
						"Change of attributes for this id is not supported");
			}
		} else {
			if (this.prototypeValues != null) {
				id = this.prototypeValues.findId(name);
				if (id != 0) {
					this.prototypeValues.setAttributes(id, attributes);
					return;
				}
			}

			super.setAttributes(name, attributes);
		}
	}

	Object[] getIds(boolean getAll) {
		Object[] result = super.getIds(getAll);
		if (this.prototypeValues != null) {
			result = this.prototypeValues.getNames(getAll, result);
		}

		int maxInstanceId = this.getMaxInstanceId();
		if (maxInstanceId != 0) {
			Object[] ids = null;
			int count = 0;

			for (int tmp = maxInstanceId; tmp != 0; --tmp) {
				String name = this.getInstanceIdName(tmp);
				int info = this.findInstanceIdInfo(name);
				if (info != 0) {
					int attr = info >>> 16;
					if (((attr & 4) != 0 || NOT_FOUND != this
							.getInstanceIdValue(tmp))
							&& (getAll || (attr & 2) == 0)) {
						if (count == 0) {
							ids = new Object[tmp];
						}

						ids[count++] = name;
					}
				}
			}

			if (count != 0) {
				if (result.length == 0 && ids.length == count) {
					result = ids;
				} else {
					Object[] arg9 = new Object[result.length + count];
					System.arraycopy(result, 0, arg9, 0, result.length);
					System.arraycopy(ids, 0, arg9, result.length, count);
					result = arg9;
				}
			}
		}

		return result;
	}

	protected int getMaxInstanceId() {
		return 0;
	}

	protected static int instanceIdInfo(int attributes, int id) {
		return attributes << 16 | id;
	}

	protected int findInstanceIdInfo(String name) {
		return 0;
	}

	protected String getInstanceIdName(int id) {
		throw new IllegalArgumentException(String.valueOf(id));
	}

	protected Object getInstanceIdValue(int id) {
		throw new IllegalStateException(String.valueOf(id));
	}

	protected void setInstanceIdValue(int id, Object value) {
		throw new IllegalStateException(String.valueOf(id));
	}

	public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
			Scriptable thisObj, Object[] args) {
		throw f.unknown();
	}

	public final IdFunctionObject exportAsJSClass(int maxPrototypeId,
			Scriptable scope, boolean sealed) {
		if (scope != this && scope != null) {
			this.setParentScope(scope);
			this.setPrototype(getObjectPrototype(scope));
		}

		this.activatePrototypeMap(maxPrototypeId);
		IdFunctionObject ctor = this.prototypeValues
				.createPrecachedConstructor();
		if (sealed) {
			this.sealObject();
		}

		this.fillConstructorProperties(ctor);
		if (sealed) {
			ctor.sealObject();
		}

		ctor.exportAsScopeProperty();
		return ctor;
	}

	public final boolean hasPrototypeMap() {
		return this.prototypeValues != null;
	}

	public final void activatePrototypeMap(int maxPrototypeId) {
		IdScriptableObject.PrototypeValues values = new IdScriptableObject.PrototypeValues(
				this, maxPrototypeId);
		synchronized (this) {
			if (this.prototypeValues != null) {
				throw new IllegalStateException();
			} else {
				this.prototypeValues = values;
			}
		}
	}

	public final void initPrototypeMethod(Object tag, int id, String name,
			int arity) {
		Scriptable scope = ScriptableObject.getTopLevelScope(this);
		IdFunctionObject f = this.newIdFunction(tag, id, name, arity, scope);
		this.prototypeValues.initValue(id, name, f, 2);
	}

	public final void initPrototypeConstructor(IdFunctionObject f) {
		int id = this.prototypeValues.constructorId;
		if (id == 0) {
			throw new IllegalStateException();
		} else if (f.methodId() != id) {
			throw new IllegalArgumentException();
		} else {
			if (this.isSealed()) {
				f.sealObject();
			}

			this.prototypeValues.initValue(id, "constructor", f, 2);
		}
	}

	public final void initPrototypeValue(int id, String name, Object value,
			int attributes) {
		this.prototypeValues.initValue(id, name, value, attributes);
	}

	protected void initPrototypeId(int id) {
		throw new IllegalStateException(String.valueOf(id));
	}

	protected int findPrototypeId(String name) {
		throw new IllegalStateException(name);
	}

	protected void fillConstructorProperties(IdFunctionObject ctor) {
	}

	protected void addIdFunctionProperty(Scriptable obj, Object tag, int id,
			String name, int arity) {
		Scriptable scope = ScriptableObject.getTopLevelScope(obj);
		IdFunctionObject f = this.newIdFunction(tag, id, name, arity, scope);
		f.addAsProperty(obj);
	}

	protected static EcmaError incompatibleCallError(IdFunctionObject f) {
		throw ScriptRuntime
				.typeError1("msg.incompat.call", f.getFunctionName());
	}

	private IdFunctionObject newIdFunction(Object tag, int id, String name,
			int arity, Scriptable scope) {
		IdFunctionObject f = new IdFunctionObject(this, tag, id, name, arity,
				scope);
		if (this.isSealed()) {
			f.sealObject();
		}

		return f;
	}

	private void readObject(ObjectInputStream stream) throws IOException,
			ClassNotFoundException {
		stream.defaultReadObject();
		int maxPrototypeId = stream.readInt();
		if (maxPrototypeId != 0) {
			this.activatePrototypeMap(maxPrototypeId);
		}

	}

	private void writeObject(ObjectOutputStream stream) throws IOException {
		stream.defaultWriteObject();
		int maxPrototypeId = 0;
		if (this.prototypeValues != null) {
			maxPrototypeId = this.prototypeValues.getMaxId();
		}

		stream.writeInt(maxPrototypeId);
	}

	private static final class PrototypeValues implements Serializable {
		static final long serialVersionUID = 3038645279153854371L;
		private static final int VALUE_SLOT = 0;
		private static final int NAME_SLOT = 1;
		private static final int SLOT_SPAN = 2;
		private IdScriptableObject obj;
		private int maxId;
		private volatile Object[] valueArray;
		private volatile short[] attributeArray;
		private volatile int lastFoundId = 1;
		int constructorId;
		private IdFunctionObject constructor;
		private short constructorAttrs;

		PrototypeValues(IdScriptableObject obj, int maxId) {
			if (obj == null) {
				throw new IllegalArgumentException();
			} else if (maxId < 1) {
				throw new IllegalArgumentException();
			} else {
				this.obj = obj;
				this.maxId = maxId;
			}
		}

		final int getMaxId() {
			return this.maxId;
		}

		final void initValue(int id, String name, Object value, int attributes) {
			if (1 <= id && id <= this.maxId) {
				if (name == null) {
					throw new IllegalArgumentException();
				} else if (value == Scriptable.NOT_FOUND) {
					throw new IllegalArgumentException();
				} else {
					ScriptableObject.checkValidAttributes(attributes);
					if (this.obj.findPrototypeId(name) != id) {
						throw new IllegalArgumentException(name);
					} else if (id == this.constructorId) {
						if (!(value instanceof IdFunctionObject)) {
							throw new IllegalArgumentException(
									"consructor should be initialized with IdFunctionObject");
						} else {
							this.constructor = (IdFunctionObject) value;
							this.constructorAttrs = (short) attributes;
						}
					} else {
						this.initSlot(id, name, value, attributes);
					}
				}
			} else {
				throw new IllegalArgumentException();
			}
		}

		private void initSlot(int id, String name, Object value, int attributes) {
			Object[] array = this.valueArray;
			if (array == null) {
				throw new IllegalStateException();
			} else {
				if (value == null) {
					value = UniqueTag.NULL_VALUE;
				}

				int index = (id - 1) * 2;
				synchronized (this) {
					Object value2 = array[index + 0];
					if (value2 == null) {
						array[index + 0] = value;
						array[index + 1] = name;
						this.attributeArray[id - 1] = (short) attributes;
					} else if (!name.equals(array[index + 1])) {
						throw new IllegalStateException();
					}

				}
			}
		}

		final IdFunctionObject createPrecachedConstructor() {
			if (this.constructorId != 0) {
				throw new IllegalStateException();
			} else {
				this.constructorId = this.obj.findPrototypeId("constructor");
				if (this.constructorId == 0) {
					throw new IllegalStateException(
							"No id for constructor property");
				} else {
					this.obj.initPrototypeId(this.constructorId);
					if (this.constructor == null) {
						throw new IllegalStateException(this.obj.getClass()
								.getName()
								+ ".initPrototypeId() did not "
								+ "initialize id=" + this.constructorId);
					} else {
						this.constructor.initFunction(this.obj.getClassName(),
								ScriptableObject.getTopLevelScope(this.obj));
						this.constructor.markAsConstructor(this.obj);
						return this.constructor;
					}
				}
			}
		}

		final int findId(String name) {
			Object[] array = this.valueArray;
			if (array == null) {
				return this.obj.findPrototypeId(name);
			} else {
				int id = this.lastFoundId;
				if (name == array[(id - 1) * 2 + 1]) {
					return id;
				} else {
					id = this.obj.findPrototypeId(name);
					if (id != 0) {
						int nameSlot = (id - 1) * 2 + 1;
						array[nameSlot] = name;
						this.lastFoundId = id;
					}

					return id;
				}
			}
		}

		final boolean has(int id) {
			Object[] array = this.valueArray;
			if (array == null) {
				return true;
			} else {
				int valueSlot = (id - 1) * 2 + 0;
				Object value = array[valueSlot];
				return value == null ? true : value != Scriptable.NOT_FOUND;
			}
		}

		final Object get(int id) {
			Object value = this.ensureId(id);
			if (value == UniqueTag.NULL_VALUE) {
				value = null;
			}

			return value;
		}

		final void set(int id, Scriptable start, Object value) {
			if (value == Scriptable.NOT_FOUND) {
				throw new IllegalArgumentException();
			} else {
				this.ensureId(id);
				short attr = this.attributeArray[id - 1];
				if ((attr & 1) == 0) {
					int nameSlot;
					if (start == this.obj) {
						if (value == null) {
							value = UniqueTag.NULL_VALUE;
						}

						nameSlot = (id - 1) * 2 + 0;
						synchronized (this) {
							this.valueArray[nameSlot] = value;
						}
					} else {
						nameSlot = (id - 1) * 2 + 1;
						String name = (String) this.valueArray[nameSlot];
						start.put(name, start, value);
					}
				}

			}
		}

		final void delete(int id) {
			this.ensureId(id);
			short attr = this.attributeArray[id - 1];
			if ((attr & 4) == 0) {
				int valueSlot = (id - 1) * 2 + 0;
				synchronized (this) {
					this.valueArray[valueSlot] = Scriptable.NOT_FOUND;
					this.attributeArray[id - 1] = 0;
				}
			}

		}

		final int getAttributes(int id) {
			this.ensureId(id);
			return this.attributeArray[id - 1];
		}

		final void setAttributes(int id, int attributes) {
			ScriptableObject.checkValidAttributes(attributes);
			this.ensureId(id);
			synchronized (this) {
				this.attributeArray[id - 1] = (short) attributes;
			}
		}

		final Object[] getNames(boolean getAll, Object[] extraEntries) {
			Object[] names = null;
			int count = 0;

			int extra;
			for (extra = 1; extra <= this.maxId; ++extra) {
				Object tmp = this.ensureId(extra);
				if ((getAll || (this.attributeArray[extra - 1] & 2) == 0)
						&& tmp != Scriptable.NOT_FOUND) {
					int nameSlot = (extra - 1) * 2 + 1;
					String name = (String) this.valueArray[nameSlot];
					if (names == null) {
						names = new Object[this.maxId];
					}

					names[count++] = name;
				}
			}

			if (count == 0) {
				return extraEntries;
			} else if (extraEntries != null && extraEntries.length != 0) {
				extra = extraEntries.length;
				Object[] arg9 = new Object[extra + count];
				System.arraycopy(extraEntries, 0, arg9, 0, extra);
				System.arraycopy(names, 0, arg9, extra, count);
				return arg9;
			} else {
				if (count != names.length) {
					Object[] arg8 = new Object[count];
					System.arraycopy(names, 0, arg8, 0, count);
					names = arg8;
				}

				return names;
			}
		}

		private Object ensureId(int id) {
			Object[] array = this.valueArray;
			if (array == null) {
				synchronized (this) {
					array = this.valueArray;
					if (array == null) {
						array = new Object[this.maxId * 2];
						this.valueArray = array;
						this.attributeArray = new short[this.maxId];
					}
				}
			}

			int valueSlot = (id - 1) * 2 + 0;
			Object value = array[valueSlot];
			if (value == null) {
				if (id == this.constructorId) {
					this.initSlot(this.constructorId, "constructor",
							this.constructor, this.constructorAttrs);
					this.constructor = null;
				} else {
					this.obj.initPrototypeId(id);
				}

				value = array[valueSlot];
				if (value == null) {
					throw new IllegalStateException(this.obj.getClass()
							.getName()
							+ ".initPrototypeId(int id) "
							+ "did not initialize id=" + id);
				}
			}

			return value;
		}
	}
}