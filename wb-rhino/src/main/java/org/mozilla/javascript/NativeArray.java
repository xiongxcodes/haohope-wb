/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import java.util.Arrays;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.NativeString;
import org.mozilla.javascript.ObjToIntMap;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

public class NativeArray extends IdScriptableObject {
	static final long serialVersionUID = 7331366857676127338L;
	private static final Object ARRAY_TAG = "Array";
	private static final Integer NEGATIVE_ONE = new Integer(-1);
	private static final int Id_length = 1;
	private static final int MAX_INSTANCE_ID = 1;
	private static final int Id_constructor = 1;
	private static final int Id_toString = 2;
	private static final int Id_toLocaleString = 3;
	private static final int Id_toSource = 4;
	private static final int Id_join = 5;
	private static final int Id_reverse = 6;
	private static final int Id_sort = 7;
	private static final int Id_push = 8;
	private static final int Id_pop = 9;
	private static final int Id_shift = 10;
	private static final int Id_unshift = 11;
	private static final int Id_splice = 12;
	private static final int Id_concat = 13;
	private static final int Id_slice = 14;
	private static final int Id_indexOf = 15;
	private static final int Id_lastIndexOf = 16;
	private static final int Id_every = 17;
	private static final int Id_filter = 18;
	private static final int Id_forEach = 19;
	private static final int Id_map = 20;
	private static final int Id_some = 21;
	private static final int MAX_PROTOTYPE_ID = 21;
	private static final int ConstructorId_join = -5;
	private static final int ConstructorId_reverse = -6;
	private static final int ConstructorId_sort = -7;
	private static final int ConstructorId_push = -8;
	private static final int ConstructorId_pop = -9;
	private static final int ConstructorId_shift = -10;
	private static final int ConstructorId_unshift = -11;
	private static final int ConstructorId_splice = -12;
	private static final int ConstructorId_concat = -13;
	private static final int ConstructorId_slice = -14;
	private static final int ConstructorId_indexOf = -15;
	private static final int ConstructorId_lastIndexOf = -16;
	private static final int ConstructorId_every = -17;
	private static final int ConstructorId_filter = -18;
	private static final int ConstructorId_forEach = -19;
	private static final int ConstructorId_map = -20;
	private static final int ConstructorId_some = -21;
	private long length;
	private Object[] dense;
	private boolean denseOnly;
	private static int maximumInitialCapacity = 10000;
	private static final int DEFAULT_INITIAL_CAPACITY = 10;
	private static final double GROW_FACTOR = 1.5D;
	private static final int MAX_PRE_GROW_SIZE = 1431655764;

	static void init(Scriptable scope, boolean sealed) {
		NativeArray obj = new NativeArray(0L);
		obj.exportAsJSClass(21, scope, sealed);
	}

	static int getMaximumInitialCapacity() {
		return maximumInitialCapacity;
	}

	static void setMaximumInitialCapacity(int maximumInitialCapacity) {
		NativeArray.maximumInitialCapacity = maximumInitialCapacity;
	}

	public NativeArray(long lengthArg) {
		this.denseOnly = lengthArg <= (long) maximumInitialCapacity;
		if (this.denseOnly) {
			int intLength = (int) lengthArg;
			if (intLength < 10) {
				intLength = 10;
			}

			this.dense = new Object[intLength];
			Arrays.fill(this.dense, Scriptable.NOT_FOUND);
		}

		this.length = lengthArg;
	}

	public NativeArray(Object[] array) {
		this.denseOnly = true;
		this.dense = array;
		this.length = (long) array.length;
	}

	public String getClassName() {
		return "Array";
	}

	protected int getMaxInstanceId() {
		return 1;
	}

	protected int findInstanceIdInfo(String s) {
		return s.equals("length") ? instanceIdInfo(6, 1) : super
				.findInstanceIdInfo(s);
	}

	protected String getInstanceIdName(int id) {
		return id == 1 ? "length" : super.getInstanceIdName(id);
	}

	protected Object getInstanceIdValue(int id) {
		return id == 1 ? ScriptRuntime.wrapNumber((double) this.length) : super
				.getInstanceIdValue(id);
	}

	protected void setInstanceIdValue(int id, Object value) {
		if (id == 1) {
			this.setLength(value);
		} else {
			super.setInstanceIdValue(id, value);
		}
	}

	protected void fillConstructorProperties(IdFunctionObject ctor) {
		this.addIdFunctionProperty(ctor, ARRAY_TAG, -5, "join", 2);
		this.addIdFunctionProperty(ctor, ARRAY_TAG, -6, "reverse", 1);
		this.addIdFunctionProperty(ctor, ARRAY_TAG, -7, "sort", 2);
		this.addIdFunctionProperty(ctor, ARRAY_TAG, -8, "push", 2);
		this.addIdFunctionProperty(ctor, ARRAY_TAG, -9, "pop", 2);
		this.addIdFunctionProperty(ctor, ARRAY_TAG, -10, "shift", 2);
		this.addIdFunctionProperty(ctor, ARRAY_TAG, -11, "unshift", 2);
		this.addIdFunctionProperty(ctor, ARRAY_TAG, -12, "splice", 2);
		this.addIdFunctionProperty(ctor, ARRAY_TAG, -13, "concat", 2);
		this.addIdFunctionProperty(ctor, ARRAY_TAG, -14, "slice", 2);
		this.addIdFunctionProperty(ctor, ARRAY_TAG, -15, "indexOf", 2);
		this.addIdFunctionProperty(ctor, ARRAY_TAG, -16, "lastIndexOf", 2);
		this.addIdFunctionProperty(ctor, ARRAY_TAG, -17, "every", 2);
		this.addIdFunctionProperty(ctor, ARRAY_TAG, -18, "filter", 2);
		this.addIdFunctionProperty(ctor, ARRAY_TAG, -19, "forEach", 2);
		this.addIdFunctionProperty(ctor, ARRAY_TAG, -20, "map", 2);
		this.addIdFunctionProperty(ctor, ARRAY_TAG, -21, "some", 2);
		super.fillConstructorProperties(ctor);
	}

	protected void initPrototypeId(int id) {
		String s;
		byte arity;
		switch (id) {
			case 1 :
				arity = 1;
				s = "constructor";
				break;
			case 2 :
				arity = 0;
				s = "toString";
				break;
			case 3 :
				arity = 1;
				s = "toLocaleString";
				break;
			case 4 :
				arity = 0;
				s = "toSource";
				break;
			case 5 :
				arity = 1;
				s = "join";
				break;
			case 6 :
				arity = 0;
				s = "reverse";
				break;
			case 7 :
				arity = 1;
				s = "sort";
				break;
			case 8 :
				arity = 1;
				s = "push";
				break;
			case 9 :
				arity = 1;
				s = "pop";
				break;
			case 10 :
				arity = 1;
				s = "shift";
				break;
			case 11 :
				arity = 1;
				s = "unshift";
				break;
			case 12 :
				arity = 1;
				s = "splice";
				break;
			case 13 :
				arity = 1;
				s = "concat";
				break;
			case 14 :
				arity = 1;
				s = "slice";
				break;
			case 15 :
				arity = 1;
				s = "indexOf";
				break;
			case 16 :
				arity = 1;
				s = "lastIndexOf";
				break;
			case 17 :
				arity = 1;
				s = "every";
				break;
			case 18 :
				arity = 1;
				s = "filter";
				break;
			case 19 :
				arity = 1;
				s = "forEach";
				break;
			case 20 :
				arity = 1;
				s = "map";
				break;
			case 21 :
				arity = 1;
				s = "some";
				break;
			default :
				throw new IllegalArgumentException(String.valueOf(id));
		}

		this.initPrototypeMethod(ARRAY_TAG, id, s, arity);
	}

	public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
			Scriptable thisObj, Object[] args) {
		if (!f.hasTag(ARRAY_TAG)) {
			return super.execIdCall(f, cx, scope, thisObj, args);
		} else {
			int id = f.methodId();

			while (true) {
				switch (id) {
					case -21 :
					case -20 :
					case -19 :
					case -18 :
					case -17 :
					case -16 :
					case -15 :
					case -14 :
					case -13 :
					case -12 :
					case -11 :
					case -10 :
					case -9 :
					case -8 :
					case -7 :
					case -6 :
					case -5 :
						thisObj = ScriptRuntime.toObject(scope, args[0]);
						Object[] arg8 = new Object[args.length - 1];

						for (int i = 0; i < arg8.length; ++i) {
							arg8[i] = args[i + 1];
						}

						args = arg8;
						id = -id;
						break;
					case -4 :
					case -3 :
					case -2 :
					case -1 :
					case 0 :
					default :
						throw new IllegalArgumentException(String.valueOf(id));
					case 1 :
						boolean inNewExpr = thisObj == null;
						if (!inNewExpr) {
							return f.construct(cx, scope, args);
						}

						return jsConstructor(cx, scope, args);
					case 2 :
						return toStringHelper(cx, scope, thisObj,
								cx.hasFeature(4), false);
					case 3 :
						return toStringHelper(cx, scope, thisObj, false, true);
					case 4 :
						return toStringHelper(cx, scope, thisObj, true, false);
					case 5 :
						return js_join(cx, thisObj, args);
					case 6 :
						return js_reverse(cx, thisObj, args);
					case 7 :
						return js_sort(cx, scope, thisObj, args);
					case 8 :
						return js_push(cx, thisObj, args);
					case 9 :
						return js_pop(cx, thisObj, args);
					case 10 :
						return js_shift(cx, thisObj, args);
					case 11 :
						return js_unshift(cx, thisObj, args);
					case 12 :
						return js_splice(cx, scope, thisObj, args);
					case 13 :
						return js_concat(cx, scope, thisObj, args);
					case 14 :
						return this.js_slice(cx, thisObj, args);
					case 15 :
						return this.indexOfHelper(cx, thisObj, args, false);
					case 16 :
						return this.indexOfHelper(cx, thisObj, args, true);
					case 17 :
					case 18 :
					case 19 :
					case 20 :
					case 21 :
						return this.iterativeMethod(cx, id, scope, thisObj,
								args);
				}
			}
		}
	}

	public Object get(int index, Scriptable start) {
		return !this.denseOnly
				&& this.isGetterOrSetter((String) null, index, false) ? super
				.get(index, start) : (this.dense != null && 0 <= index
				&& index < this.dense.length ? this.dense[index] : super.get(
				index, start));
	}

	public boolean has(int index, Scriptable start) {
		return !this.denseOnly
				&& this.isGetterOrSetter((String) null, index, false) ? super
				.has(index, start) : (this.dense != null && 0 <= index
				&& index < this.dense.length
				? this.dense[index] != NOT_FOUND
				: super.has(index, start));
	}

	private static long toArrayIndex(String id) {
		double d = ScriptRuntime.toNumber(id);
		if (d == d) {
			long index = ScriptRuntime.toUint32(d);
			if ((double) index == d && index != 4294967295L
					&& Long.toString(index).equals(id)) {
				return index;
			}
		}

		return -1L;
	}

	public void put(String id, Scriptable start, Object value) {
		super.put(id, start, value);
		if (start == this) {
			long index = toArrayIndex(id);
			if (index >= this.length) {
				this.length = index + 1L;
				this.denseOnly = false;
			}
		}

	}

	private boolean ensureCapacity(int capacity) {
		if (capacity > this.dense.length) {
			if (capacity > 1431655764) {
				this.denseOnly = false;
				return false;
			}

			capacity = Math.max(capacity,
					(int) ((double) this.dense.length * 1.5D));
			Object[] newDense = new Object[capacity];
			System.arraycopy(this.dense, 0, newDense, 0, this.dense.length);
			Arrays.fill(newDense, this.dense.length, newDense.length,
					Scriptable.NOT_FOUND);
			this.dense = newDense;
		}

		return true;
	}

	public void put(int index, Scriptable start, Object value) {
		if (start == this
				&& !this.isSealed()
				&& this.dense != null
				&& 0 <= index
				&& (this.denseOnly || !this.isGetterOrSetter((String) null,
						index, true))) {
			if (index < this.dense.length) {
				this.dense[index] = value;
				if (this.length <= (long) index) {
					this.length = (long) index + 1L;
				}

				return;
			}

			if (this.denseOnly
					&& (double) index < (double) this.dense.length * 1.5D
					&& this.ensureCapacity(index + 1)) {
				this.dense[index] = value;
				this.length = (long) index + 1L;
				return;
			}

			this.denseOnly = false;
		}

		super.put(index, start, value);
		if (start == this && this.length <= (long) index) {
			this.length = (long) index + 1L;
		}

	}

	public void delete(int index) {
		if (this.dense == null || 0 > index || index >= this.dense.length
				|| this.isSealed() || !this.denseOnly
				&& this.isGetterOrSetter((String) null, index, true)) {
			super.delete(index);
		} else {
			this.dense[index] = NOT_FOUND;
		}

	}

	public Object[] getIds() {
		Object[] superIds = super.getIds();
		if (this.dense == null) {
			return superIds;
		} else {
			int N = this.dense.length;
			long currentLength = this.length;
			if ((long) N > currentLength) {
				N = (int) currentLength;
			}

			if (N == 0) {
				return superIds;
			} else {
				int superLength = superIds.length;
				Object[] ids = new Object[N + superLength];
				int presentCount = 0;

				for (int tmp = 0; tmp != N; ++tmp) {
					if (this.dense[tmp] != NOT_FOUND) {
						ids[presentCount] = new Integer(tmp);
						++presentCount;
					}
				}

				if (presentCount != N) {
					Object[] arg8 = new Object[presentCount + superLength];
					System.arraycopy(ids, 0, arg8, 0, presentCount);
					ids = arg8;
				}

				System.arraycopy(superIds, 0, ids, presentCount, superLength);
				return ids;
			}
		}
	}

	public Object getDefaultValue(Class<?> hint) {
		if (hint == ScriptRuntime.NumberClass) {
			Context cx = Context.getContext();
			if (cx.getLanguageVersion() == 120) {
				return new Long(this.length);
			}
		}

		return super.getDefaultValue(hint);
	}

	private static Object jsConstructor(Context cx, Scriptable scope,
			Object[] args) {
		if (args.length == 0) {
			return new NativeArray(0L);
		} else if (cx.getLanguageVersion() == 120) {
			return new NativeArray(args);
		} else {
			Object arg0 = args[0];
			if (args.length <= 1 && arg0 instanceof Number) {
				long len = ScriptRuntime.toUint32(arg0);
				if ((double) len != ((Number) arg0).doubleValue()) {
					throw Context.reportRuntimeError0("msg.arraylength.bad");
				} else {
					return new NativeArray(len);
				}
			} else {
				return new NativeArray(args);
			}
		}
	}

	public long getLength() {
		return this.length;
	}

	public long jsGet_length() {
		return this.getLength();
	}

	void setDenseOnly(boolean denseOnly) {
		if (denseOnly && !this.denseOnly) {
			throw new IllegalArgumentException();
		} else {
			this.denseOnly = denseOnly;
		}
	}

	private void setLength(Object val) {
		double d = ScriptRuntime.toNumber(val);
		long longVal = ScriptRuntime.toUint32(d);
		if ((double) longVal != d) {
			throw Context.reportRuntimeError0("msg.arraylength.bad");
		} else {
			if (this.denseOnly) {
				if (longVal < this.length) {
					Arrays.fill(this.dense, (int) longVal, this.dense.length,
							NOT_FOUND);
					this.length = longVal;
					return;
				}

				if (longVal < 1431655764L
						&& (double) longVal < (double) this.length * 1.5D
						&& this.ensureCapacity((int) longVal)) {
					this.length = longVal;
					return;
				}

				this.denseOnly = false;
			}

			if (longVal < this.length) {
				if (this.length - longVal > 4096L) {
					Object[] i = this.getIds();

					for (int i1 = 0; i1 < i.length; ++i1) {
						Object id = i[i1];
						if (id instanceof String) {
							String index = (String) id;
							long index1 = toArrayIndex(index);
							if (index1 >= longVal) {
								this.delete(index);
							}
						} else {
							int arg12 = ((Integer) id).intValue();
							if ((long) arg12 >= longVal) {
								this.delete(arg12);
							}
						}
					}
				} else {
					for (long arg11 = longVal; arg11 < this.length; ++arg11) {
						deleteElem(this, arg11);
					}
				}
			}

			this.length = longVal;
		}
	}

	static long getLengthProperty(Context cx, Scriptable obj) {
		return obj instanceof NativeString ? (long) ((NativeString) obj)
				.getLength() : (obj instanceof NativeArray
				? ((NativeArray) obj).getLength()
				: ScriptRuntime.toUint32(ScriptRuntime.getObjectProp(obj,
						"length", cx)));
	}

	private static Object setLengthProperty(Context cx, Scriptable target,
			long length) {
		return ScriptRuntime.setObjectProp(target, "length",
				ScriptRuntime.wrapNumber((double) length), cx);
	}

	private static void deleteElem(Scriptable target, long index) {
		int i = (int) index;
		if ((long) i == index) {
			target.delete(i);
		} else {
			target.delete(Long.toString(index));
		}

	}

	private static Object getElem(Context cx, Scriptable target, long index) {
		if (index > 2147483647L) {
			String id = Long.toString(index);
			return ScriptRuntime.getObjectProp(target, id, cx);
		} else {
			return ScriptRuntime.getObjectIndex(target, (int) index, cx);
		}
	}

	private static void setElem(Context cx, Scriptable target, long index,
			Object value) {
		if (index > 2147483647L) {
			String id = Long.toString(index);
			ScriptRuntime.setObjectProp(target, id, value, cx);
		} else {
			ScriptRuntime.setObjectIndex(target, (int) index, value, cx);
		}

	}

	private static String toStringHelper(Context cx, Scriptable scope,
			Scriptable thisObj, boolean toSource, boolean toLocale) {
		long length = getLengthProperty(cx, thisObj);
		StringBuilder result = new StringBuilder(256);
		String separator;
		if (toSource) {
			result.append('[');
			separator = ", ";
		} else {
			separator = ",";
		}

		boolean haslast = false;
		long i = 0L;
		boolean toplevel;
		boolean iterating;
		if (cx.iterating == null) {
			toplevel = true;
			iterating = false;
			cx.iterating = new ObjToIntMap(31);
		} else {
			toplevel = false;
			iterating = cx.iterating.has(thisObj);
		}

		try {
			if (!iterating) {
				cx.iterating.put(thisObj, 0);

				for (i = 0L; i < length; ++i) {
					if (i > 0L) {
						result.append(separator);
					}

					Object elem = getElem(cx, thisObj, i);
					if (elem != null && elem != Undefined.instance) {
						haslast = true;
						if (toSource) {
							result.append(ScriptRuntime.uneval(cx, scope, elem));
						} else if (elem instanceof String) {
							String fun = (String) elem;
							if (toSource) {
								result.append('\"');
								result.append(ScriptRuntime.escapeString(fun));
								result.append('\"');
							} else {
								result.append(fun);
							}
						} else {
							if (toLocale) {
								Callable arg19 = ScriptRuntime
										.getPropFunctionAndThis(elem,
												"toLocaleString", cx);
								Scriptable funThis = ScriptRuntime
										.lastStoredScriptable(cx);
								elem = arg19.call(cx, scope, funThis,
										ScriptRuntime.emptyArgs);
							}

							result.append(ScriptRuntime.toString(elem));
						}
					} else {
						haslast = false;
					}
				}
			}
		} finally {
			if (toplevel) {
				cx.iterating = null;
			}

		}

		if (toSource) {
			if (!haslast && i > 0L) {
				result.append(", ]");
			} else {
				result.append(']');
			}
		}

		return result.toString();
	}

	private static String js_join(Context cx, Scriptable thisObj, Object[] args) {
		long llength = getLengthProperty(cx, thisObj);
		int length = (int) llength;
		if (llength != (long) length) {
			throw Context.reportRuntimeError1("msg.arraylength.too.big",
					String.valueOf(llength));
		} else {
			String separator = args.length >= 1
					&& args[0] != Undefined.instance ? ScriptRuntime
					.toString(args[0]) : ",";
			int sb;
			Object i;
			if (thisObj instanceof NativeArray) {
				NativeArray buf = (NativeArray) thisObj;
				if (buf.denseOnly) {
					StringBuilder arg12 = new StringBuilder();

					for (sb = 0; sb < length; ++sb) {
						if (sb != 0) {
							arg12.append(separator);
						}

						if (sb < buf.dense.length) {
							i = buf.dense[sb];
							if (i != null && i != Undefined.instance
									&& i != Scriptable.NOT_FOUND) {
								arg12.append(ScriptRuntime.toString(i));
							}
						}
					}

					return arg12.toString();
				}
			}

			if (length == 0) {
				return "";
			} else {
				String[] arg11 = new String[length];
				int total_size = 0;

				String str;
				for (sb = 0; sb != length; ++sb) {
					i = getElem(cx, thisObj, (long) sb);
					if (i != null && i != Undefined.instance) {
						str = ScriptRuntime.toString(i);
						total_size += str.length();
						arg11[sb] = str;
					}
				}

				total_size += (length - 1) * separator.length();
				StringBuilder arg13 = new StringBuilder(total_size);

				for (int arg14 = 0; arg14 != length; ++arg14) {
					if (arg14 != 0) {
						arg13.append(separator);
					}

					str = arg11[arg14];
					if (str != null) {
						arg13.append(str);
					}
				}

				return arg13.toString();
			}
		}
	}

	private static Scriptable js_reverse(Context cx, Scriptable thisObj,
			Object[] args) {
		if (thisObj instanceof NativeArray) {
			NativeArray len = (NativeArray) thisObj;
			if (len.denseOnly) {
				int i = 0;

				for (int arg13 = (int) len.length - 1; i < arg13; --arg13) {
					Object temp = len.dense[i];
					len.dense[i] = len.dense[arg13];
					len.dense[arg13] = temp;
					++i;
				}

				return thisObj;
			}
		}

		long arg12 = getLengthProperty(cx, thisObj);
		long half = arg12 / 2L;

		for (long i1 = 0L; i1 < half; ++i1) {
			long j = arg12 - i1 - 1L;
			Object temp1 = getElem(cx, thisObj, i1);
			Object temp2 = getElem(cx, thisObj, j);
			setElem(cx, thisObj, i1, temp2);
			setElem(cx, thisObj, j, temp1);
		}

		return thisObj;
	}

	private static Scriptable js_sort(Context cx, Scriptable scope,
			Scriptable thisObj, Object[] args) {
		long length = getLengthProperty(cx, thisObj);
		if (length <= 1L) {
			return thisObj;
		} else {
			Object compare;
			Object[] cmpBuf;
			if (args.length > 0 && Undefined.instance != args[0]) {
				compare = args[0];
				cmpBuf = new Object[2];
			} else {
				compare = null;
				cmpBuf = null;
			}

			if (thisObj instanceof NativeArray) {
				NativeArray ilength = (NativeArray) thisObj;
				if (ilength.denseOnly) {
					int arg11 = (int) length;
					heapsort(cx, scope, ilength.dense, arg11, compare, cmpBuf);
					return thisObj;
				}
			}

			if (length >= 2147483647L) {
				heapsort_extended(cx, scope, thisObj, length, compare, cmpBuf);
			} else {
				int arg10 = (int) length;
				Object[] working = new Object[arg10];

				int i;
				for (i = 0; i != arg10; ++i) {
					working[i] = getElem(cx, thisObj, (long) i);
				}

				heapsort(cx, scope, working, arg10, compare, cmpBuf);

				for (i = 0; i != arg10; ++i) {
					setElem(cx, thisObj, (long) i, working[i]);
				}
			}

			return thisObj;
		}
	}

	private static boolean isBigger(Context cx, Scriptable scope, Object x,
			Object y, Object cmp, Object[] cmpBuf) {
		if (cmp == null) {
			if (cmpBuf != null) {
				Kit.codeBug();
			}
		} else if (cmpBuf == null || cmpBuf.length != 2) {
			Kit.codeBug();
		}

		Object undef = Undefined.instance;
		Object notfound = Scriptable.NOT_FOUND;
		if (y != undef && y != notfound) {
			if (x != undef && x != notfound) {
				if (cmp == null) {
					String fun1 = ScriptRuntime.toString(x);
					String funThis1 = ScriptRuntime.toString(y);
					return fun1.compareTo(funThis1) > 0;
				} else {
					cmpBuf[0] = x;
					cmpBuf[1] = y;
					Callable fun = ScriptRuntime.getValueFunctionAndThis(cmp,
							cx);
					Scriptable funThis = ScriptRuntime.lastStoredScriptable(cx);
					Object ret = fun.call(cx, scope, funThis, cmpBuf);
					double d = ScriptRuntime.toNumber(ret);
					return d > 0.0D;
				}
			} else {
				return true;
			}
		} else {
			return false;
		}
	}

	private static void heapsort(Context cx, Scriptable scope, Object[] array,
			int length, Object cmp, Object[] cmpBuf) {
		if (length <= 1) {
			Kit.codeBug();
		}

		int i = length / 2;

		Object pivot;
		while (i != 0) {
			--i;
			pivot = array[i];
			heapify(cx, scope, pivot, array, i, length, cmp, cmpBuf);
		}

		i = length;

		while (i != 1) {
			--i;
			pivot = array[i];
			array[i] = array[0];
			heapify(cx, scope, pivot, array, 0, i, cmp, cmpBuf);
		}

	}

	private static void heapify(Context cx, Scriptable scope, Object pivot,
			Object[] array, int i, int end, Object cmp, Object[] cmpBuf) {
		while (true) {
			int child = i * 2 + 1;
			if (child < end) {
				Object childVal = array[child];
				if (child + 1 < end) {
					Object nextVal = array[child + 1];
					if (isBigger(cx, scope, nextVal, childVal, cmp, cmpBuf)) {
						++child;
						childVal = nextVal;
					}
				}

				if (isBigger(cx, scope, childVal, pivot, cmp, cmpBuf)) {
					array[i] = childVal;
					i = child;
					continue;
				}
			}

			array[i] = pivot;
			return;
		}
	}

	private static void heapsort_extended(Context cx, Scriptable scope,
			Scriptable target, long length, Object cmp, Object[] cmpBuf) {
		if (length <= 1L) {
			Kit.codeBug();
		}

		long i = length / 2L;

		Object pivot;
		while (i != 0L) {
			--i;
			pivot = getElem(cx, target, i);
			heapify_extended(cx, scope, pivot, target, i, length, cmp, cmpBuf);
		}

		i = length;

		while (i != 1L) {
			--i;
			pivot = getElem(cx, target, i);
			setElem(cx, target, i, getElem(cx, target, 0L));
			heapify_extended(cx, scope, pivot, target, 0L, i, cmp, cmpBuf);
		}

	}

	private static void heapify_extended(Context cx, Scriptable scope,
			Object pivot, Scriptable target, long i, long end, Object cmp,
			Object[] cmpBuf) {
		while (true) {
			long child = i * 2L + 1L;
			if (child < end) {
				Object childVal = getElem(cx, target, child);
				if (child + 1L < end) {
					Object nextVal = getElem(cx, target, child + 1L);
					if (isBigger(cx, scope, nextVal, childVal, cmp, cmpBuf)) {
						++child;
						childVal = nextVal;
					}
				}

				if (isBigger(cx, scope, childVal, pivot, cmp, cmpBuf)) {
					setElem(cx, target, i, childVal);
					i = child;
					continue;
				}
			}

			setElem(cx, target, i, pivot);
			return;
		}
	}

	private static Object js_push(Context cx, Scriptable thisObj, Object[] args) {
		if (thisObj instanceof NativeArray) {
			NativeArray length = (NativeArray) thisObj;
			if (length.denseOnly
					&& length.ensureCapacity((int) length.length + args.length)) {
				for (int i = 0; i < args.length; ++i) {
					length.dense[(int) (length.length++)] = args[i];
				}

				return ScriptRuntime.wrapNumber((double) length.length);
			}
		}

		long arg5 = getLengthProperty(cx, thisObj);

		for (int lengthObj = 0; lengthObj < args.length; ++lengthObj) {
			setElem(cx, thisObj, arg5 + (long) lengthObj, args[lengthObj]);
		}

		arg5 += (long) args.length;
		Object arg6 = setLengthProperty(cx, thisObj, arg5);
		return cx.getLanguageVersion() == 120 ? (args.length == 0
				? Undefined.instance
				: args[args.length - 1]) : arg6;
	}

	private static Object js_pop(Context cx, Scriptable thisObj, Object[] args) {
		Object result;
		if (thisObj instanceof NativeArray) {
			NativeArray length = (NativeArray) thisObj;
			if (length.denseOnly && length.length > 0L) {
				--length.length;
				result = length.dense[(int) length.length];
				length.dense[(int) length.length] = NOT_FOUND;
				return result;
			}
		}

		long arg5 = getLengthProperty(cx, thisObj);
		if (arg5 > 0L) {
			--arg5;
			result = getElem(cx, thisObj, arg5);
		} else {
			result = Undefined.instance;
		}

		setLengthProperty(cx, thisObj, arg5);
		return result;
	}

	private static Object js_shift(Context cx, Scriptable thisObj, Object[] args) {
		if (thisObj instanceof NativeArray) {
			NativeArray result = (NativeArray) thisObj;
			if (result.denseOnly && result.length > 0L) {
				--result.length;
				Object arg9 = result.dense[0];
				System.arraycopy(result.dense, 1, result.dense, 0,
						(int) result.length);
				result.dense[(int) result.length] = NOT_FOUND;
				return arg9;
			}
		}

		long length = getLengthProperty(cx, thisObj);
		Object arg8;
		if (length > 0L) {
			long i = 0L;
			--length;
			arg8 = getElem(cx, thisObj, i);
			if (length > 0L) {
				for (i = 1L; i <= length; ++i) {
					Object temp = getElem(cx, thisObj, i);
					setElem(cx, thisObj, i - 1L, temp);
				}
			}
		} else {
			arg8 = Undefined.instance;
		}

		setLengthProperty(cx, thisObj, length);
		return arg8;
	}

	private static Object js_unshift(Context cx, Scriptable thisObj,
			Object[] args) {
		if (thisObj instanceof NativeArray) {
			NativeArray length = (NativeArray) thisObj;
			if (length.denseOnly
					&& length.ensureCapacity((int) length.length + args.length)) {
				System.arraycopy(length.dense, 0, length.dense, args.length,
						(int) length.length);

				for (int i = 0; i < args.length; ++i) {
					length.dense[i] = args[i];
				}

				length.length += (long) args.length;
				return ScriptRuntime.wrapNumber((double) length.length);
			}
		}

		long arg8 = getLengthProperty(cx, thisObj);
		int argc = args.length;
		if (args.length <= 0) {
			return ScriptRuntime.wrapNumber((double) arg8);
		} else {
			if (arg8 > 0L) {
				for (long i1 = arg8 - 1L; i1 >= 0L; --i1) {
					Object temp = getElem(cx, thisObj, i1);
					setElem(cx, thisObj, i1 + (long) argc, temp);
				}
			}

			for (int arg9 = 0; arg9 < args.length; ++arg9) {
				setElem(cx, thisObj, (long) arg9, args[arg9]);
			}

			arg8 += (long) args.length;
			return setLengthProperty(cx, thisObj, arg8);
		}
	}

	private static Object js_splice(Context cx, Scriptable scope,
			Scriptable thisObj, Object[] args) {
		NativeArray na = null;
		boolean denseMode = false;
		if (thisObj instanceof NativeArray) {
			na = (NativeArray) thisObj;
			denseMode = na.denseOnly;
		}

		scope = getTopLevelScope(scope);
		int argc = args.length;
		if (argc == 0) {
			return ScriptRuntime.newObject(cx, scope, "Array", (Object[]) null);
		} else {
			long length = getLengthProperty(cx, thisObj);
			long begin = toSliceIndex(ScriptRuntime.toInteger(args[0]), length);
			--argc;
			long count;
			if (args.length == 1) {
				count = length - begin;
			} else {
				double end = ScriptRuntime.toInteger(args[1]);
				if (end < 0.0D) {
					count = 0L;
				} else if (end > (double) (length - begin)) {
					count = length - begin;
				} else {
					count = (long) end;
				}

				--argc;
			}

			long arg20 = begin + count;
			Object result;
			if (count != 0L) {
				if (count == 1L && cx.getLanguageVersion() == 120) {
					result = getElem(cx, thisObj, begin);
				} else if (denseMode) {
					int delta = (int) (arg20 - begin);
					Object[] last = new Object[delta];
					System.arraycopy(na.dense, (int) begin, last, 0, delta);
					result = cx.newArray(scope, last);
				} else {
					Scriptable arg21 = ScriptRuntime.newObject(cx, scope,
							"Array", (Object[]) null);

					for (long arg23 = begin; arg23 != arg20; ++arg23) {
						Object i = getElem(cx, thisObj, arg23);
						setElem(cx, arg21, arg23 - begin, i);
					}

					result = arg21;
				}
			} else if (cx.getLanguageVersion() == 120) {
				result = Undefined.instance;
			} else {
				result = ScriptRuntime.newObject(cx, scope, "Array",
						(Object[]) null);
			}

			long arg22 = (long) argc - count;
			if (denseMode && length + arg22 < 2147483647L
					&& na.ensureCapacity((int) (length + arg22))) {
				System.arraycopy(na.dense, (int) arg20, na.dense,
						(int) (begin + (long) argc), (int) (length - arg20));
				if (argc > 0) {
					System.arraycopy(args, 2, na.dense, (int) begin, argc);
				}

				if (arg22 < 0L) {
					Arrays.fill(na.dense, (int) (length + arg22), (int) length,
							NOT_FOUND);
				}

				na.length = length + arg22;
				return result;
			} else {
				long argoffset;
				Object temp;
				if (arg22 > 0L) {
					for (argoffset = length - 1L; argoffset >= arg20; --argoffset) {
						temp = getElem(cx, thisObj, argoffset);
						setElem(cx, thisObj, argoffset + arg22, temp);
					}
				} else if (arg22 < 0L) {
					for (argoffset = arg20; argoffset < length; ++argoffset) {
						temp = getElem(cx, thisObj, argoffset);
						setElem(cx, thisObj, argoffset + arg22, temp);
					}
				}

				int arg24 = args.length - argc;

				for (int arg25 = 0; arg25 < argc; ++arg25) {
					setElem(cx, thisObj, begin + (long) arg25, args[arg25
							+ arg24]);
				}

				setLengthProperty(cx, thisObj, length + arg22);
				return result;
			}
		}
	}

	private static Scriptable js_concat(Context cx, Scriptable scope,
			Scriptable thisObj, Object[] args) {
		scope = getTopLevelScope(scope);
		Function ctor = ScriptRuntime.getExistingCtor(cx, scope, "Array");
		Scriptable result = ctor.construct(cx, scope, ScriptRuntime.emptyArgs);
		int i;
		if (thisObj instanceof NativeArray && result instanceof NativeArray) {
			NativeArray length = (NativeArray) thisObj;
			NativeArray denseResult = (NativeArray) result;
			if (length.denseOnly && denseResult.denseOnly) {
				boolean slot = true;
				int length1 = (int) length.length;

				for (i = 0; i < args.length && slot; ++i) {
					if (args[i] instanceof NativeArray) {
						NativeArray arg = (NativeArray) args[i];
						slot = arg.denseOnly;
						length1 = (int) ((long) length1 + arg.length);
					} else {
						++length1;
					}
				}

				if (slot && denseResult.ensureCapacity(length1)) {
					System.arraycopy(length.dense, 0, denseResult.dense, 0,
							(int) length.length);
					i = (int) length.length;

					for (int arg18 = 0; arg18 < args.length && slot; ++arg18) {
						if (args[arg18] instanceof NativeArray) {
							NativeArray arg19 = (NativeArray) args[arg18];
							System.arraycopy(arg19.dense, 0, denseResult.dense,
									i, (int) arg19.length);
							i += (int) arg19.length;
						} else {
							denseResult.dense[i++] = args[arg18];
						}
					}

					denseResult.length = (long) length1;
					return result;
				}
			}
		}

		long arg15 = 0L;
		long arg14;
		if (ScriptRuntime.instanceOf(thisObj, ctor, cx)) {
			arg14 = getLengthProperty(cx, thisObj);

			for (arg15 = 0L; arg15 < arg14; ++arg15) {
				Object arg16 = getElem(cx, thisObj, arg15);
				setElem(cx, result, arg15, arg16);
			}
		} else {
			setElem(cx, result, arg15++, thisObj);
		}

		for (i = 0; i < args.length; ++i) {
			if (ScriptRuntime.instanceOf(args[i], ctor, cx)) {
				Scriptable arg17 = (Scriptable) args[i];
				arg14 = getLengthProperty(cx, arg17);

				for (long j = 0L; j < arg14; ++arg15) {
					Object temp = getElem(cx, arg17, j);
					setElem(cx, result, arg15, temp);
					++j;
				}
			} else {
				setElem(cx, result, arg15++, args[i]);
			}
		}

		return result;
	}

	private Scriptable js_slice(Context cx, Scriptable thisObj, Object[] args) {
		Scriptable scope = getTopLevelScope(this);
		Scriptable result = ScriptRuntime.newObject(cx, scope, "Array",
				(Object[]) null);
		long length = getLengthProperty(cx, thisObj);
		long begin;
		long end;
		if (args.length == 0) {
			begin = 0L;
			end = length;
		} else {
			begin = toSliceIndex(ScriptRuntime.toInteger(args[0]), length);
			if (args.length == 1) {
				end = length;
			} else {
				end = toSliceIndex(ScriptRuntime.toInteger(args[1]), length);
			}
		}

		for (long slot = begin; slot < end; ++slot) {
			Object temp = getElem(cx, thisObj, slot);
			setElem(cx, result, slot - begin, temp);
		}

		return result;
	}

	private static long toSliceIndex(double value, long length) {
		long result;
		if (value < 0.0D) {
			if (value + (double) length < 0.0D) {
				result = 0L;
			} else {
				result = (long) (value + (double) length);
			}
		} else if (value > (double) length) {
			result = length;
		} else {
			result = (long) value;
		}

		return result;
	}

	private Object indexOfHelper(Context cx, Scriptable thisObj, Object[] args,
			boolean isLast) {
		Object compareTo = args.length > 0 ? args[0] : Undefined.instance;
		long length = getLengthProperty(cx, thisObj);
		long start;
		if (isLast) {
			if (args.length < 2) {
				start = length - 1L;
			} else {
				start = (long) ScriptRuntime.toInt32(ScriptRuntime
						.toNumber(args[1]));
				if (start >= length) {
					start = length - 1L;
				} else if (start < 0L) {
					start += length;
				}
			}
		} else if (args.length < 2) {
			start = 0L;
		} else {
			start = (long) ScriptRuntime.toInt32(ScriptRuntime
					.toNumber(args[1]));
			if (start < 0L) {
				start += length;
				if (start < 0L) {
					start = 0L;
				}
			}
		}

		if (thisObj instanceof NativeArray) {
			NativeArray i = (NativeArray) thisObj;
			if (i.denseOnly) {
				int i1;
				if (isLast) {
					for (i1 = (int) start; i1 >= 0; --i1) {
						if (i.dense[i1] != Scriptable.NOT_FOUND
								&& ScriptRuntime.shallowEq(i.dense[i1],
										compareTo)) {
							return new Long((long) i1);
						}
					}
				} else {
					for (i1 = (int) start; (long) i1 < length; ++i1) {
						if (i.dense[i1] != Scriptable.NOT_FOUND
								&& ScriptRuntime.shallowEq(i.dense[i1],
										compareTo)) {
							return new Long((long) i1);
						}
					}
				}

				return NEGATIVE_ONE;
			}
		}

		long arg11;
		if (isLast) {
			for (arg11 = start; arg11 >= 0L; --arg11) {
				if (ScriptRuntime.shallowEq(getElem(cx, thisObj, arg11),
						compareTo)) {
					return new Long(arg11);
				}
			}
		} else {
			for (arg11 = start; arg11 < length; ++arg11) {
				if (ScriptRuntime.shallowEq(getElem(cx, thisObj, arg11),
						compareTo)) {
					return new Long(arg11);
				}
			}
		}

		return NEGATIVE_ONE;
	}

	private Object iterativeMethod(Context cx, int id, Scriptable scope,
			Scriptable thisObj, Object[] args) {
		Object callbackArg = args.length > 0 ? args[0] : Undefined.instance;
		if (callbackArg != null && callbackArg instanceof Function) {
			Function f = (Function) callbackArg;
			Scriptable parent = ScriptableObject.getTopLevelScope(f);
			Scriptable thisArg;
			if (args.length >= 2 && args[1] != null
					&& args[1] != Undefined.instance) {
				thisArg = ScriptRuntime.toObject(cx, scope, args[1]);
			} else {
				thisArg = parent;
			}

			long length = getLengthProperty(cx, thisObj);
			Scriptable array = ScriptRuntime.newObject(cx, scope, "Array",
					(Object[]) null);
			long j = 0L;

			for (long i = 0L; i < length; ++i) {
				Object[] innerArgs = new Object[3];
				Object elem = i > 2147483647L ? ScriptableObject.getProperty(
						thisObj, Long.toString(i)) : ScriptableObject
						.getProperty(thisObj, (int) i);
				if (elem != Scriptable.NOT_FOUND) {
					innerArgs[0] = elem;
					innerArgs[1] = new Long(i);
					innerArgs[2] = thisObj;
					Object result = f.call(cx, parent, thisArg, innerArgs);
					switch (id) {
						case 17 :
							if (!ScriptRuntime.toBoolean(result)) {
								return Boolean.FALSE;
							}
							break;
						case 18 :
							if (ScriptRuntime.toBoolean(result)) {
								setElem(cx, array, j++, innerArgs[0]);
							}
						case 19 :
						default :
							break;
						case 20 :
							setElem(cx, array, i, result);
							break;
						case 21 :
							if (ScriptRuntime.toBoolean(result)) {
								return Boolean.TRUE;
							}
					}
				}
			}

			switch (id) {
				case 17 :
					return Boolean.TRUE;
				case 18 :
				case 20 :
					return array;
				case 19 :
				default :
					return Undefined.instance;
				case 21 :
					return Boolean.FALSE;
			}
		} else {
			throw ScriptRuntime.notFunctionError(ScriptRuntime
					.toString(callbackArg));
		}
	}

	protected int findPrototypeId(String s) {
		byte id;
		String X;
		id = 0;
		X = null;
		char c;
		label77 : switch (s.length()) {
			case 3 :
				c = s.charAt(0);
				if (c == 109) {
					if (s.charAt(2) == 112 && s.charAt(1) == 97) {
						id = 20;
						return id;
					}
				} else if (c == 112 && s.charAt(2) == 112 && s.charAt(1) == 111) {
					id = 9;
					return id;
				}
				break;
			case 4 :
				switch (s.charAt(2)) {
					case 'i' :
						X = "join";
						id = 5;
						break label77;
					case 'm' :
						X = "some";
						id = 21;
						break label77;
					case 'r' :
						X = "sort";
						id = 7;
						break label77;
					case 's' :
						X = "push";
						id = 8;
					default :
						break label77;
				}
			case 5 :
				c = s.charAt(1);
				if (c == 104) {
					X = "shift";
					id = 10;
				} else if (c == 108) {
					X = "slice";
					id = 14;
				} else if (c == 118) {
					X = "every";
					id = 17;
				}
				break;
			case 6 :
				c = s.charAt(0);
				if (c == 99) {
					X = "concat";
					id = 13;
				} else if (c == 102) {
					X = "filter";
					id = 18;
				} else if (c == 115) {
					X = "splice";
					id = 12;
				}
				break;
			case 7 :
				switch (s.charAt(0)) {
					case 'f' :
						X = "forEach";
						id = 19;
						break label77;
					case 'i' :
						X = "indexOf";
						id = 15;
						break label77;
					case 'r' :
						X = "reverse";
						id = 6;
						break label77;
					case 'u' :
						X = "unshift";
						id = 11;
					default :
						break label77;
				}
			case 8 :
				c = s.charAt(3);
				if (c == 111) {
					X = "toSource";
					id = 4;
				} else if (c == 116) {
					X = "toString";
					id = 2;
				}
			case 9 :
			case 10 :
			case 12 :
			case 13 :
			default :
				break;
			case 11 :
				c = s.charAt(0);
				if (c == 99) {
					X = "constructor";
					id = 1;
				} else if (c == 108) {
					X = "lastIndexOf";
					id = 16;
				}
				break;
			case 14 :
				X = "toLocaleString";
				id = 3;
		}

		if (X != null && X != s && !X.equals(s)) {
			id = 0;
		}

		return id;
	}
}