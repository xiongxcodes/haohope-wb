/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.optimizer;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeFunction;
import org.mozilla.javascript.NativeGenerator;
import org.mozilla.javascript.NativeIterator;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public final class OptRuntime extends ScriptRuntime {
	public static final Double zeroObj = new Double(0.0D);
	public static final Double oneObj = new Double(1.0D);
	public static final Double minusOneObj = new Double(-1.0D);

	public static Object call0(Callable fun, Scriptable thisObj, Context cx,
			Scriptable scope) {
		return fun.call(cx, scope, thisObj, ScriptRuntime.emptyArgs);
	}

	public static Object call1(Callable fun, Scriptable thisObj, Object arg0,
			Context cx, Scriptable scope) {
		return fun.call(cx, scope, thisObj, new Object[]{arg0});
	}

	public static Object call2(Callable fun, Scriptable thisObj, Object arg0,
			Object arg1, Context cx, Scriptable scope) {
		return fun.call(cx, scope, thisObj, new Object[]{arg0, arg1});
	}

	public static Object callN(Callable fun, Scriptable thisObj, Object[] args,
			Context cx, Scriptable scope) {
		return fun.call(cx, scope, thisObj, args);
	}

	public static Object callName(Object[] args, String name, Context cx,
			Scriptable scope) {
		Callable f = getNameFunctionAndThis(name, cx, scope);
		Scriptable thisObj = lastStoredScriptable(cx);
		return f.call(cx, scope, thisObj, args);
	}

	public static Object callName0(String name, Context cx, Scriptable scope) {
		Callable f = getNameFunctionAndThis(name, cx, scope);
		Scriptable thisObj = lastStoredScriptable(cx);
		return f.call(cx, scope, thisObj, ScriptRuntime.emptyArgs);
	}

	public static Object callProp0(Object value, String property, Context cx,
			Scriptable scope) {
		Callable f = getPropFunctionAndThis(value, property, cx, scope);
		Scriptable thisObj = lastStoredScriptable(cx);
		return f.call(cx, scope, thisObj, ScriptRuntime.emptyArgs);
	}

	public static Object add(Object val1, double val2) {
		if (val1 instanceof Scriptable) {
			val1 = ((Scriptable) val1).getDefaultValue((Class) null);
		}

		return !(val1 instanceof String)
				? wrapDouble(toNumber(val1) + val2)
				: ((String) val1).concat(toString(val2));
	}

	public static Object add(double val1, Object val2) {
		if (val2 instanceof Scriptable) {
			val2 = ((Scriptable) val2).getDefaultValue((Class) null);
		}

		return !(val2 instanceof String)
				? wrapDouble(toNumber(val2) + val1)
				: toString(val1).concat((String) val2);
	}

	public static Object elemIncrDecr(Object obj, double index, Context cx,
			int incrDecrMask) {
		return ScriptRuntime.elemIncrDecr(obj, new Double(index), cx,
				incrDecrMask);
	}

	public static Object[] padStart(Object[] currentArgs, int count) {
		Object[] result = new Object[currentArgs.length + count];
		System.arraycopy(currentArgs, 0, result, count, currentArgs.length);
		return result;
	}

	public static void initFunction(NativeFunction fn, int functionType,
			Scriptable scope, Context cx) {
		ScriptRuntime.initFunction(cx, scope, fn, functionType, false);
	}

	public static Object callSpecial(Context cx, Callable fun,
			Scriptable thisObj, Object[] args, Scriptable scope,
			Scriptable callerThis, int callType, String fileName, int lineNumber) {
		return ScriptRuntime.callSpecial(cx, fun, thisObj, args, scope,
				callerThis, callType, fileName, lineNumber);
	}

	public static Object newObjectSpecial(Context cx, Object fun,
			Object[] args, Scriptable scope, Scriptable callerThis, int callType) {
		return ScriptRuntime.newSpecial(cx, fun, args, scope, callType);
	}

	public static Double wrapDouble(double num) {
		if (num == 0.0D) {
			if (1.0D / num > 0.0D) {
				return zeroObj;
			}
		} else {
			if (num == 1.0D) {
				return oneObj;
			}

			if (num == -1.0D) {
				return minusOneObj;
			}

			if (num != num) {
				return NaNobj;
			}
		}

		return new Double(num);
	}

	static String encodeIntArray(int[] array) {
		if (array == null) {
			return null;
		} else {
			int n = array.length;
			char[] buffer = new char[1 + n * 2];
			buffer[0] = 1;

			for (int i = 0; i != n; ++i) {
				int value = array[i];
				int shift = 1 + i * 2;
				buffer[shift] = (char) (value >>> 16);
				buffer[shift + 1] = (char) value;
			}

			return new String(buffer);
		}
	}

	private static int[] decodeIntArray(String str, int arraySize) {
		if (arraySize == 0) {
			if (str != null) {
				throw new IllegalArgumentException();
			} else {
				return null;
			}
		} else if (str.length() != 1 + arraySize * 2 && str.charAt(0) != 1) {
			throw new IllegalArgumentException();
		} else {
			int[] array = new int[arraySize];

			for (int i = 0; i != arraySize; ++i) {
				int shift = 1 + i * 2;
				array[i] = str.charAt(shift) << 16 | str.charAt(shift + 1);
			}

			return array;
		}
	}

	public static Scriptable newArrayLiteral(Object[] objects,
			String encodedInts, int skipCount, Context cx, Scriptable scope) {
		int[] skipIndexces = decodeIntArray(encodedInts, skipCount);
		return newArrayLiteral(objects, skipIndexces, cx, scope);
	}

	public static void main(final Script script, final String[] args) {
		ContextFactory.getGlobal().call(new ContextAction() {
			public Object run(Context cx) {
				ScriptableObject global = ScriptRuntime.getGlobal(cx);
				Object[] argsCopy = new Object[args.length];
				System.arraycopy(args, 0, argsCopy, 0, args.length);
				Scriptable argsObj = cx.newArray(global, argsCopy);
				global.defineProperty("arguments", argsObj, 2);
				script.exec(cx, global);
				return null;
			}
		});
	}

	public static void throwStopIteration(Object obj) {
		throw new JavaScriptException(
				NativeIterator.getStopIterationObject((Scriptable) obj), "", 0);
	}

	public static Scriptable createNativeGenerator(NativeFunction funObj,
			Scriptable scope, Scriptable thisObj, int maxLocals, int maxStack) {
		return new NativeGenerator(scope, funObj,
				new OptRuntime.GeneratorState(thisObj, maxLocals, maxStack));
	}

	public static Object[] getGeneratorStackState(Object obj) {
		OptRuntime.GeneratorState rgs = (OptRuntime.GeneratorState) obj;
		if (rgs.stackState == null) {
			rgs.stackState = new Object[rgs.maxStack];
		}

		return rgs.stackState;
	}

	public static Object[] getGeneratorLocalsState(Object obj) {
		OptRuntime.GeneratorState rgs = (OptRuntime.GeneratorState) obj;
		if (rgs.localsState == null) {
			rgs.localsState = new Object[rgs.maxLocals];
		}

		return rgs.localsState;
	}

	public static class GeneratorState {
		static final String CLASS_NAME = "org/mozilla/javascript/optimizer/OptRuntime$GeneratorState";
		public int resumptionPoint;
		static final String resumptionPoint_NAME = "resumptionPoint";
		static final String resumptionPoint_TYPE = "I";
		public Scriptable thisObj;
		static final String thisObj_NAME = "thisObj";
		static final String thisObj_TYPE = "Lorg/mozilla/javascript/Scriptable;";
		Object[] stackState;
		Object[] localsState;
		int maxLocals;
		int maxStack;

		GeneratorState(Scriptable thisObj, int maxLocals, int maxStack) {
			this.thisObj = thisObj;
			this.maxLocals = maxLocals;
			this.maxStack = maxStack;
		}
	}
}