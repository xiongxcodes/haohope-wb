/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Map;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.FieldAndMethods;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.InterfaceAdapter;
import org.mozilla.javascript.JavaMembers;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeDate;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.NativeJavaClass;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.Wrapper;

public class NativeJavaObject implements Scriptable, Wrapper, Serializable {
	static final long serialVersionUID = -6948590651130498591L;
	private static final int JSTYPE_UNDEFINED = 0;
	private static final int JSTYPE_NULL = 1;
	private static final int JSTYPE_BOOLEAN = 2;
	private static final int JSTYPE_NUMBER = 3;
	private static final int JSTYPE_STRING = 4;
	private static final int JSTYPE_JAVA_CLASS = 5;
	private static final int JSTYPE_JAVA_OBJECT = 6;
	private static final int JSTYPE_JAVA_ARRAY = 7;
	private static final int JSTYPE_OBJECT = 8;
	static final byte CONVERSION_TRIVIAL = 1;
	static final byte CONVERSION_NONTRIVIAL = 0;
	static final byte CONVERSION_NONE = 99;
	protected Scriptable prototype;
	protected Scriptable parent;
	protected transient Object javaObject;
	protected transient Class<?> staticType;
	protected transient JavaMembers members;
	private transient Map<String, FieldAndMethods> fieldAndMethods;
	private transient boolean isAdapter;
	private static final Object COERCED_INTERFACE_KEY = "Coerced Interface";
	private static Method adapter_writeAdapterObject;
	private static Method adapter_readAdapterObject;

	public NativeJavaObject() {
	}

	public NativeJavaObject(Scriptable scope, Object javaObject,
			Class<?> staticType) {
		this(scope, javaObject, staticType, false);
	}

	public NativeJavaObject(Scriptable scope, Object javaObject,
			Class<?> staticType, boolean isAdapter) {
		this.parent = scope;
		this.javaObject = javaObject;
		this.staticType = staticType;
		this.isAdapter = isAdapter;
		this.initMembers();
	}

	protected void initMembers() {
		Class dynamicType;
		if (this.javaObject != null) {
			dynamicType = this.javaObject.getClass();
		} else {
			dynamicType = this.staticType;
		}

		this.members = JavaMembers.lookupClass(this.parent, dynamicType,
				this.staticType, this.isAdapter);
		this.fieldAndMethods = this.members.getFieldAndMethodsObjects(this,
				this.javaObject, false);
	}

	public boolean has(String name, Scriptable start) {
		return this.members.has(name, false);
	}

	public boolean has(int index, Scriptable start) {
		return false;
	}

	public Object get(String name, Scriptable start) {
		if (this.fieldAndMethods != null) {
			Object result = this.fieldAndMethods.get(name);
			if (result != null) {
				return result;
			}
		}

		return this.members.get(this, name, this.javaObject, false);
	}

	public Object get(int index, Scriptable start) {
		throw this.members.reportMemberNotFound(Integer.toString(index));
	}

	public void put(String name, Scriptable start, Object value) {
		if (this.prototype != null && !this.members.has(name, false)) {
			this.prototype.put(name, this.prototype, value);
		} else {
			this.members.put(this, name, this.javaObject, value, false);
		}

	}

	public void put(int index, Scriptable start, Object value) {
		throw this.members.reportMemberNotFound(Integer.toString(index));
	}

	public boolean hasInstance(Scriptable value) {
		return false;
	}

	public void delete(String name) {
	}

	public void delete(int index) {
	}

	public Scriptable getPrototype() {
		return this.prototype == null && this.javaObject instanceof String
				? ScriptableObject.getClassPrototype(this.parent, "String")
				: this.prototype;
	}

	public void setPrototype(Scriptable m) {
		this.prototype = m;
	}

	public Scriptable getParentScope() {
		return this.parent;
	}

	public void setParentScope(Scriptable m) {
		this.parent = m;
	}

	public Object[] getIds() {
		return this.members.getIds(false);
	}

	public static Object wrap(Scriptable scope, Object obj, Class<?> staticType) {
		Context cx = Context.getContext();
		return cx.getWrapFactory().wrap(cx, scope, obj, staticType);
	}

	public Object unwrap() {
		return this.javaObject;
	}

	public String getClassName() {
		return "JavaObject";
	}

	public Object getDefaultValue(Class<?> hint) {
		if (hint == null && this.javaObject instanceof Boolean) {
			hint = ScriptRuntime.BooleanClass;
		}

		Object value;
		if (hint != null && hint != ScriptRuntime.StringClass) {
			String converterName;
			if (hint == ScriptRuntime.BooleanClass) {
				converterName = "booleanValue";
			} else {
				if (hint != ScriptRuntime.NumberClass) {
					throw Context.reportRuntimeError0("msg.default.value");
				}

				converterName = "doubleValue";
			}

			Object converterObject = this.get(converterName, this);
			if (converterObject instanceof Function) {
				Function b = (Function) converterObject;
				value = b.call(Context.getContext(), b.getParentScope(), this,
						ScriptRuntime.emptyArgs);
			} else if (hint == ScriptRuntime.NumberClass
					&& this.javaObject instanceof Boolean) {
				boolean b1 = ((Boolean) this.javaObject).booleanValue();
				value = ScriptRuntime.wrapNumber(b1 ? 1.0D : 0.0D);
			} else {
				value = this.javaObject.toString();
			}
		} else {
			value = this.javaObject.toString();
		}

		return value;
	}

	public static boolean canConvert(Object fromObj, Class<?> to) {
		int weight = getConversionWeight(fromObj, to);
		return weight < 99;
	}

	static int getConversionWeight(Object fromObj, Class<?> to) {
		int fromCode = getJSTypeCode(fromObj);
		switch (fromCode) {
			case 0 :
				if (to == ScriptRuntime.StringClass
						|| to == ScriptRuntime.ObjectClass) {
					return 1;
				}
				break;
			case 1 :
				if (!to.isPrimitive()) {
					return 1;
				}
				break;
			case 2 :
				if (to == Boolean.TYPE) {
					return 1;
				}

				if (to == ScriptRuntime.BooleanClass) {
					return 2;
				}

				if (to == ScriptRuntime.ObjectClass) {
					return 3;
				}

				if (to == ScriptRuntime.StringClass) {
					return 4;
				}
				break;
			case 3 :
				if (to.isPrimitive()) {
					if (to == Double.TYPE) {
						return 1;
					}

					if (to != Boolean.TYPE) {
						return 1 + getSizeRank(to);
					}
				} else {
					if (to == ScriptRuntime.StringClass) {
						return 9;
					}

					if (to == ScriptRuntime.ObjectClass) {
						return 10;
					}

					if (ScriptRuntime.NumberClass.isAssignableFrom(to)) {
						return 2;
					}
				}
				break;
			case 4 :
				if (to == ScriptRuntime.StringClass) {
					return 1;
				}

				if (to.isInstance(fromObj)) {
					return 2;
				}

				if (to.isPrimitive()) {
					if (to == Character.TYPE) {
						return 3;
					}

					if (to != Boolean.TYPE) {
						return 4;
					}
				}
				break;
			case 5 :
				if (to == ScriptRuntime.ClassClass) {
					return 1;
				}

				if (to == ScriptRuntime.ObjectClass) {
					return 3;
				}

				if (to == ScriptRuntime.StringClass) {
					return 4;
				}
				break;
			case 6 :
			case 7 :
				Object javaObj = fromObj;
				if (fromObj instanceof Wrapper) {
					javaObj = ((Wrapper) fromObj).unwrap();
				}

				if (to.isInstance(javaObj)) {
					return 0;
				}

				if (to == ScriptRuntime.StringClass) {
					return 2;
				}

				if (to.isPrimitive() && to != Boolean.TYPE) {
					return fromCode == 7 ? 99 : 2 + getSizeRank(to);
				}
				break;
			case 8 :
				if (Scriptable.class.isAssignableFrom(to)
						&& to.isInstance(fromObj)) {
					return 1;
				}

				if (to.isArray()) {
					if (fromObj instanceof NativeArray) {
						return 1;
					}
				} else {
					if (to == ScriptRuntime.ObjectClass) {
						return 2;
					}

					if (to == ScriptRuntime.StringClass) {
						return 3;
					}

					if (to == ScriptRuntime.DateClass) {
						if (fromObj instanceof NativeDate) {
							return 1;
						}
					} else {
						if (to.isInterface()) {
							if (fromObj instanceof Function
									&& to.getMethods().length == 1) {
								return 1;
							}

							return 11;
						}

						if (to.isPrimitive() && to != Boolean.TYPE) {
							return 3 + getSizeRank(to);
						}
					}
				}
		}

		return 99;
	}

	static int getSizeRank(Class<?> aType) {
		return aType == Double.TYPE
				? 1
				: (aType == Float.TYPE ? 2 : (aType == Long.TYPE
						? 3
						: (aType == Integer.TYPE ? 4 : (aType == Short.TYPE
								? 5
								: (aType == Character.TYPE
										? 6
										: (aType == Byte.TYPE
												? 7
												: (aType == Boolean.TYPE
														? 99
														: 8)))))));
	}

	private static int getJSTypeCode(Object value) {
		if (value == null) {
			return 1;
		} else if (value == Undefined.instance) {
			return 0;
		} else if (value instanceof String) {
			return 4;
		} else if (value instanceof Number) {
			return 3;
		} else if (value instanceof Boolean) {
			return 2;
		} else if (value instanceof Scriptable) {
			return value instanceof NativeJavaClass
					? 5
					: (value instanceof NativeJavaArray
							? 7
							: (value instanceof Wrapper ? 6 : 8));
		} else if (value instanceof Class) {
			return 5;
		} else {
			Class valueClass = value.getClass();
			return valueClass.isArray() ? 7 : 6;
		}
	}

	public static Object coerceType(Class<?> type, Object value) {
		return coerceTypeImpl(type, value);
	}

	static Object coerceTypeImpl(Class<?> type, Object value) {
		if (value != null && value.getClass() == type) {
			return value;
		} else {
			switch (getJSTypeCode(value)) {
				case 0 :
					if (type == ScriptRuntime.StringClass
							|| type == ScriptRuntime.ObjectClass) {
						return "undefined";
					}

					reportConversionError("undefined", type);
					break;
				case 1 :
					if (type.isPrimitive()) {
						reportConversionError(value, type);
					}

					return null;
				case 2 :
					if (type != Boolean.TYPE
							&& type != ScriptRuntime.BooleanClass
							&& type != ScriptRuntime.ObjectClass) {
						if (type == ScriptRuntime.StringClass) {
							return value.toString();
						}

						reportConversionError(value, type);
						break;
					}

					return value;
				case 3 :
					if (type == ScriptRuntime.StringClass) {
						return ScriptRuntime.toString(value);
					}

					if (type == ScriptRuntime.ObjectClass) {
						return coerceToNumber(Double.TYPE, value);
					}

					if (type.isPrimitive() && type != Boolean.TYPE
							|| ScriptRuntime.NumberClass.isAssignableFrom(type)) {
						return coerceToNumber(type, value);
					}

					reportConversionError(value, type);
					break;
				case 4 :
					if (type != ScriptRuntime.StringClass
							&& !type.isInstance(value)) {
						if (type == Character.TYPE
								|| type == ScriptRuntime.CharacterClass) {
							if (((String) value).length() == 1) {
								return new Character(((String) value).charAt(0));
							}

							return coerceToNumber(type, value);
						}

						if (type.isPrimitive()
								&& type != Boolean.TYPE
								|| ScriptRuntime.NumberClass
										.isAssignableFrom(type)) {
							return coerceToNumber(type, value);
						}

						reportConversionError(value, type);
						break;
					}

					return value;
				case 5 :
					if (value instanceof Wrapper) {
						value = ((Wrapper) value).unwrap();
					}

					if (type != ScriptRuntime.ClassClass
							&& type != ScriptRuntime.ObjectClass) {
						if (type == ScriptRuntime.StringClass) {
							return value.toString();
						}

						reportConversionError(value, type);
						break;
					}

					return value;
				case 6 :
				case 7 :
					if (value instanceof Wrapper) {
						value = ((Wrapper) value).unwrap();
					}

					if (type.isPrimitive()) {
						if (type == Boolean.TYPE) {
							reportConversionError(value, type);
						}

						return coerceToNumber(type, value);
					}

					if (type == ScriptRuntime.StringClass) {
						return value.toString();
					}

					if (type.isInstance(value)) {
						return value;
					}

					reportConversionError(value, type);
					break;
				case 8 :
					if (type == ScriptRuntime.StringClass) {
						return ScriptRuntime.toString(value);
					}

					if (type.isPrimitive()) {
						if (type == Boolean.TYPE) {
							reportConversionError(value, type);
						}

						return coerceToNumber(type, value);
					}

					if (type.isInstance(value)) {
						return value;
					}

					if (type == ScriptRuntime.DateClass
							&& value instanceof NativeDate) {
						double arg10 = ((NativeDate) value).getJSTimeValue();
						return new Date((long) arg10);
					}

					Object glue;
					if (type.isArray() && value instanceof NativeArray) {
						NativeArray arg9 = (NativeArray) value;
						long arg11 = arg9.getLength();
						Class arg12 = type.getComponentType();
						glue = Array.newInstance(arg12, (int) arg11);

						for (int i = 0; (long) i < arg11; ++i) {
							try {
								Array.set(glue, i,
										coerceType(arg12, arg9.get(i, arg9)));
							} catch (EvaluatorException arg8) {
								reportConversionError(value, type);
							}
						}

						return glue;
					}

					if (value instanceof Wrapper) {
						value = ((Wrapper) value).unwrap();
						if (type.isInstance(value)) {
							return value;
						}

						reportConversionError(value, type);
					} else if (type.isInterface() && value instanceof Callable) {
						if (value instanceof ScriptableObject) {
							ScriptableObject so = (ScriptableObject) value;
							Object key = Kit.makeHashKeyFromPair(
									COERCED_INTERFACE_KEY, type);
							Object old = so.getAssociatedValue(key);
							if (old != null) {
								return old;
							}

							Context cx = Context.getContext();
							glue = InterfaceAdapter.create(cx, type,
									(Callable) value);
							glue = so.associateValue(key, glue);
							return glue;
						}

						reportConversionError(value, type);
					} else {
						reportConversionError(value, type);
					}
			}

			return value;
		}
	}

	private static Object coerceToNumber(Class<?> type, Object value) {
		Class valueClass = value.getClass();
		if (type != Character.TYPE && type != ScriptRuntime.CharacterClass) {
			if (type != ScriptRuntime.ObjectClass
					&& type != ScriptRuntime.DoubleClass && type != Double.TYPE) {
				double max;
				double min;
				if (type != ScriptRuntime.FloatClass && type != Float.TYPE) {
					if (type != ScriptRuntime.IntegerClass
							&& type != Integer.TYPE) {
						if (type != ScriptRuntime.LongClass
								&& type != Long.TYPE) {
							return type != ScriptRuntime.ShortClass
									&& type != Short.TYPE
									? (type != ScriptRuntime.ByteClass
											&& type != Byte.TYPE
											? new Double(toDouble(value))
											: (valueClass == ScriptRuntime.ByteClass
													? value
													: new Byte(
															(byte) ((int) toInteger(
																	value,
																	ScriptRuntime.ByteClass,
																	-128.0D,
																	127.0D)))))
									: (valueClass == ScriptRuntime.ShortClass
											? value
											: new Short(
													(short) ((int) toInteger(
															value,
															ScriptRuntime.ShortClass,
															-32768.0D, 32767.0D))));
						} else if (valueClass == ScriptRuntime.LongClass) {
							return value;
						} else {
							max = Double.longBitsToDouble(4890909195324358655L);
							min = Double
									.longBitsToDouble(-4332462841530417152L);
							return new Long(toInteger(value,
									ScriptRuntime.LongClass, min, max));
						}
					} else {
						return valueClass == ScriptRuntime.IntegerClass
								? value
								: new Integer((int) toInteger(value,
										ScriptRuntime.IntegerClass,
										-2.147483648E9D, 2.147483647E9D));
					}
				} else if (valueClass == ScriptRuntime.FloatClass) {
					return value;
				} else {
					max = toDouble(value);
					if (!Double.isInfinite(max) && !Double.isNaN(max)
							&& max != 0.0D) {
						min = Math.abs(max);
						return min < 1.401298464324817E-45D
								? new Float(max > 0.0D ? 0.0D : -0.0D)
								: (min > 3.4028234663852886E38D
										? new Float(max > 0.0D
												? Float.POSITIVE_INFINITY
												: Float.NEGATIVE_INFINITY)
										: new Float((float) max));
					} else {
						return new Float((float) max);
					}
				}
			} else {
				return valueClass == ScriptRuntime.DoubleClass
						? value
						: new Double(toDouble(value));
			}
		} else {
			return valueClass == ScriptRuntime.CharacterClass
					? value
					: new Character((char) ((int) toInteger(value,
							ScriptRuntime.CharacterClass, 0.0D, 65535.0D)));
		}
	}

	private static double toDouble(Object value) {
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		} else if (value instanceof String) {
			return ScriptRuntime.toNumber((String) value);
		} else if (value instanceof Scriptable) {
			return value instanceof Wrapper ? toDouble(((Wrapper) value)
					.unwrap()) : ScriptRuntime.toNumber(value);
		} else {
			Method meth;
			try {
				meth = value.getClass()
						.getMethod("doubleValue", (Class[]) null);
			} catch (NoSuchMethodException arg4) {
				meth = null;
			} catch (SecurityException arg5) {
				meth = null;
			}

			if (meth != null) {
				try {
					return ((Number) meth.invoke(value, (Object[]) null))
							.doubleValue();
				} catch (IllegalAccessException arg2) {
					reportConversionError(value, Double.TYPE);
				} catch (InvocationTargetException arg3) {
					reportConversionError(value, Double.TYPE);
				}
			}

			return ScriptRuntime.toNumber(value.toString());
		}
	}

	private static long toInteger(Object value, Class<?> type, double min,
			double max) {
		double d = toDouble(value);
		if (Double.isInfinite(d) || Double.isNaN(d)) {
			reportConversionError(ScriptRuntime.toString(value), type);
		}

		if (d > 0.0D) {
			d = Math.floor(d);
		} else {
			d = Math.ceil(d);
		}

		if (d < min || d > max) {
			reportConversionError(ScriptRuntime.toString(value), type);
		}

		return (long) d;
	}

	static void reportConversionError(Object value, Class<?> type) {
		throw Context.reportRuntimeError2("msg.conversion.not.allowed",
				String.valueOf(value), JavaMembers.javaSignature(type));
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		out.writeBoolean(this.isAdapter);
		if (this.isAdapter) {
			if (adapter_writeAdapterObject == null) {
				throw new IOException();
			}

			Object[] args = new Object[]{this.javaObject, out};

			try {
				adapter_writeAdapterObject.invoke((Object) null, args);
			} catch (Exception arg3) {
				throw new IOException();
			}
		} else {
			out.writeObject(this.javaObject);
		}

		if (this.staticType != null) {
			out.writeObject(this.staticType.getClass().getName());
		} else {
			out.writeObject((Object) null);
		}

	}

	private void readObject(ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		in.defaultReadObject();
		this.isAdapter = in.readBoolean();
		if (this.isAdapter) {
			if (adapter_readAdapterObject == null) {
				throw new ClassNotFoundException();
			}

			Object[] className = new Object[]{this, in};

			try {
				this.javaObject = adapter_readAdapterObject.invoke(
						(Object) null, className);
			} catch (Exception arg3) {
				throw new IOException();
			}
		} else {
			this.javaObject = in.readObject();
		}

		String className1 = (String) in.readObject();
		if (className1 != null) {
			this.staticType = Class.forName(className1);
		} else {
			this.staticType = null;
		}

		this.initMembers();
	}

	static {
		Class[] sig2 = new Class[2];
		Class cl = Kit.classOrNull("org.mozilla.javascript.JavaAdapter");
		if (cl != null) {
			try {
				sig2[0] = ScriptRuntime.ObjectClass;
				sig2[1] = Kit.classOrNull("java.io.ObjectOutputStream");
				adapter_writeAdapterObject = cl.getMethod("writeAdapterObject",
						sig2);
				sig2[0] = ScriptRuntime.ScriptableClass;
				sig2[1] = Kit.classOrNull("java.io.ObjectInputStream");
				adapter_readAdapterObject = cl.getMethod("readAdapterObject",
						sig2);
			} catch (Exception arg2) {
				adapter_writeAdapterObject = null;
				adapter_readAdapterObject = null;
			}
		}

	}
}