/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.JavaMembers;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.MemberBox;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.Wrapper;

public class NativeJavaMethod extends BaseFunction {
	static final long serialVersionUID = -3440381785576412928L;
	private static final int PREFERENCE_EQUAL = 0;
	private static final int PREFERENCE_FIRST_ARG = 1;
	private static final int PREFERENCE_SECOND_ARG = 2;
	private static final int PREFERENCE_AMBIGUOUS = 3;
	private static final boolean debug = false;
	MemberBox[] methods;
	private String functionName;

	NativeJavaMethod(MemberBox[] methods) {
		this.functionName = methods[0].getName();
		this.methods = methods;
	}

	NativeJavaMethod(MemberBox method, String name) {
		this.functionName = name;
		this.methods = new MemberBox[]{method};
	}

	public NativeJavaMethod(Method method, String name) {
		this(new MemberBox(method), name);
	}

	public String getFunctionName() {
		return this.functionName;
	}

	static String scriptSignature(Object[] values) {
		StringBuffer sig = new StringBuffer();

		for (int i = 0; i != values.length; ++i) {
			Object value = values[i];
			String s;
			if (value == null) {
				s = "null";
			} else if (value instanceof Boolean) {
				s = "boolean";
			} else if (value instanceof String) {
				s = "string";
			} else if (value instanceof Number) {
				s = "number";
			} else if (value instanceof Scriptable) {
				if (value instanceof Undefined) {
					s = "undefined";
				} else if (value instanceof Wrapper) {
					Object wrapped = ((Wrapper) value).unwrap();
					s = wrapped.getClass().getName();
				} else if (value instanceof Function) {
					s = "function";
				} else {
					s = "object";
				}
			} else {
				s = JavaMembers.javaSignature(value.getClass());
			}

			if (i != 0) {
				sig.append(',');
			}

			sig.append(s);
		}

		return sig.toString();
	}

	String decompile(int indent, int flags) {
      StringBuffer sb = new StringBuffer();
      boolean justbody = 0 != (flags & 1);
      if(!justbody) {
         sb.append("function ");
         sb.append(this.getFunctionName());
         sb.append("() {");
      }
      sb.append("\n}\n");
      //sb.append("\n":"*/}\n");
      return sb.toString();
   }
	public String toString() {
		StringBuffer sb = new StringBuffer();
		int i = 0;

		for (int N = this.methods.length; i != N; ++i) {
			Method method = this.methods[i].method();
			sb.append(JavaMembers.javaSignature(method.getReturnType()));
			sb.append(' ');
			sb.append(method.getName());
			sb.append(JavaMembers
					.liveConnectSignature(this.methods[i].argTypes));
			sb.append('\n');
		}

		return sb.toString();
	}

	public Object call(Context cx, Scriptable scope, Scriptable thisObj,
			Object[] args) {
		if (this.methods.length == 0) {
			throw new RuntimeException("No methods defined for call");
		} else {
			int index = findFunction(cx, this.methods, args);
			if (index < 0) {
				Class arg12 = this.methods[0].method().getDeclaringClass();
				String arg13 = arg12.getName() + '.' + this.getFunctionName()
						+ '(' + scriptSignature(args) + ')';
				throw Context.reportRuntimeError1("msg.java.no_such_method",
						arg13);
			} else {
				MemberBox meth = this.methods[index];
				Class[] argTypes = meth.argTypes;
				Object[] javaObject;
				int retval;
				Class staticType;
				Object arg15;
				Object arg18;
				if (meth.vararg) {
					javaObject = new Object[argTypes.length];

					for (retval = 0; retval < argTypes.length - 1; ++retval) {
						javaObject[retval] = Context.jsToJava(args[retval],
								argTypes[retval]);
					}

					if (args.length == argTypes.length
							&& (args[args.length - 1] == null
									|| args[args.length - 1] instanceof NativeArray || args[args.length - 1] instanceof NativeJavaArray)) {
						arg15 = Context.jsToJava(args[args.length - 1],
								argTypes[argTypes.length - 1]);
					} else {
						staticType = argTypes[argTypes.length - 1]
								.getComponentType();
						arg15 = Array.newInstance(staticType, args.length
								- argTypes.length + 1);

						for (int wrapped = 0; wrapped < Array.getLength(arg15); ++wrapped) {
							Object value = Context.jsToJava(
									args[argTypes.length - 1 + wrapped],
									staticType);
							Array.set(arg15, wrapped, value);
						}
					}

					javaObject[argTypes.length - 1] = arg15;
					args = javaObject;
				} else {
					javaObject = args;

					for (retval = 0; retval < args.length; ++retval) {
						Object arg16 = args[retval];
						arg18 = Context.jsToJava(arg16, argTypes[retval]);
						if (arg18 != arg16) {
							if (javaObject == args) {
								args = (Object[]) args.clone();
							}

							args[retval] = arg18;
						}
					}
				}

				Object arg14;
				if (meth.isStatic()) {
					arg14 = null;
				} else {
					Scriptable arg17 = thisObj;
					staticType = meth.getDeclaringClass();

					while (true) {
						if (arg17 == null) {
							throw Context.reportRuntimeError3(
									"msg.nonjava.method",
									this.getFunctionName(),
									ScriptRuntime.toString(thisObj),
									staticType.getName());
						}

						if (arg17 instanceof Wrapper) {
							arg14 = ((Wrapper) arg17).unwrap();
							if (staticType.isInstance(arg14)) {
								break;
							}
						}

						arg17 = arg17.getPrototype();
					}
				}

				arg15 = meth.invoke(arg14, args);
				staticType = meth.method().getReturnType();
				arg18 = cx.getWrapFactory().wrap(cx, scope, arg15, staticType);
				if (arg18 == null && staticType == Void.TYPE) {
					arg18 = Undefined.instance;
				}

				return arg18;
			}
		}
	}

	static int findFunction(Context cx, MemberBox[] methodsOrCtors,
			Object[] args) {
		if (methodsOrCtors.length == 0) {
			return -1;
		} else {
			int extraBestFitsCount;
			int buf;
			if (methodsOrCtors.length == 1) {
				MemberBox arg15 = methodsOrCtors[0];
				Class[] arg16 = arg15.argTypes;
				extraBestFitsCount = arg16.length;
				if (arg15.vararg) {
					--extraBestFitsCount;
					if (extraBestFitsCount > args.length) {
						return -1;
					}
				} else if (extraBestFitsCount != args.length) {
					return -1;
				}

				for (buf = 0; buf != extraBestFitsCount; ++buf) {
					if (!NativeJavaObject.canConvert(args[buf], arg16[buf])) {
						return -1;
					}
				}

				return 0;
			} else {
				int firstBestFit = -1;
				int[] extraBestFits = null;
				extraBestFitsCount = 0;

				MemberBox firstFitMember;
				label149 : for (buf = 0; buf < methodsOrCtors.length; ++buf) {
					firstFitMember = methodsOrCtors[buf];
					Class[] memberName = firstFitMember.argTypes;
					int memberClass = memberName.length;
					if (firstFitMember.vararg) {
						--memberClass;
						if (memberClass > args.length) {
							continue;
						}
					} else if (memberClass != args.length) {
						continue;
					}

					int betterCount;
					for (betterCount = 0; betterCount < memberClass; ++betterCount) {
						if (!NativeJavaObject.canConvert(args[betterCount],
								memberName[betterCount])) {
							continue label149;
						}
					}

					if (firstBestFit < 0) {
						firstBestFit = buf;
					} else {
						betterCount = 0;
						int worseCount = 0;

						for (int j = -1; j != extraBestFitsCount; ++j) {
							int bestFitIndex;
							if (j == -1) {
								bestFitIndex = firstBestFit;
							} else {
								bestFitIndex = extraBestFits[j];
							}

							MemberBox bestFit = methodsOrCtors[bestFitIndex];
							if (cx.hasFeature(13)
									&& (bestFit.member().getModifiers() & 1) != (firstFitMember
											.member().getModifiers() & 1)) {
								if ((bestFit.member().getModifiers() & 1) == 0) {
									++betterCount;
								} else {
									++worseCount;
								}
							} else {
								int preference = preferSignature(args,
										memberName, firstFitMember.vararg,
										bestFit.argTypes, bestFit.vararg);
								if (preference == 3) {
									break;
								}

								if (preference == 1) {
									++betterCount;
								} else {
									if (preference != 2) {
										if (preference != 0) {
											Kit.codeBug();
										}

										if (bestFit.isStatic()
												&& bestFit
														.getDeclaringClass()
														.isAssignableFrom(
																firstFitMember
																		.getDeclaringClass())) {
											if (j == -1) {
												firstBestFit = buf;
											} else {
												extraBestFits[j] = buf;
											}
										}
										continue label149;
									}

									++worseCount;
								}
							}
						}

						if (betterCount == 1 + extraBestFitsCount) {
							firstBestFit = buf;
							extraBestFitsCount = 0;
						} else if (worseCount != 1 + extraBestFitsCount) {
							if (extraBestFits == null) {
								extraBestFits = new int[methodsOrCtors.length - 1];
							}

							extraBestFits[extraBestFitsCount] = buf;
							++extraBestFitsCount;
						}
					}
				}

				if (firstBestFit < 0) {
					return -1;
				} else if (extraBestFitsCount == 0) {
					return firstBestFit;
				} else {
					StringBuffer arg17 = new StringBuffer();

					for (int arg18 = -1; arg18 != extraBestFitsCount; ++arg18) {
						int arg19;
						if (arg18 == -1) {
							arg19 = firstBestFit;
						} else {
							arg19 = extraBestFits[arg18];
						}

						arg17.append("\n    ");
						arg17.append(methodsOrCtors[arg19].toJavaDeclaration());
					}

					firstFitMember = methodsOrCtors[firstBestFit];
					String arg20 = firstFitMember.getName();
					String arg21 = firstFitMember.getDeclaringClass().getName();
					if (methodsOrCtors[0].isMethod()) {
						throw Context.reportRuntimeError3(
								"msg.constructor.ambiguous", arg20,
								scriptSignature(args), arg17.toString());
					} else {
						throw Context.reportRuntimeError4(
								"msg.method.ambiguous", arg21, arg20,
								scriptSignature(args), arg17.toString());
					}
				}
			}
		}
	}

	private static int preferSignature(Object[] args, Class<?>[] sig1,
			boolean vararg1, Class<?>[] sig2, boolean vararg2) {
		int alength = args.length;
		if (!vararg1 && vararg2) {
			return 1;
		} else if (vararg1 && !vararg2) {
			return 2;
		} else {
			if (vararg1 && vararg2) {
				if (sig1.length < sig2.length) {
					return 2;
				}

				if (sig1.length > sig2.length) {
					return 1;
				}

				alength = Math.min(args.length, sig1.length - 1);
			}

			int totalPreference = 0;

			for (int j = 0; j < alength; ++j) {
				Class type1 = sig1[j];
				Class type2 = sig2[j];
				if (type1 != type2) {
					Object arg = args[j];
					int rank1 = NativeJavaObject
							.getConversionWeight(arg, type1);
					int rank2 = NativeJavaObject
							.getConversionWeight(arg, type2);
					byte preference;
					if (rank1 < rank2) {
						preference = 1;
					} else if (rank1 > rank2) {
						preference = 2;
					} else if (rank1 == 0) {
						if (type1.isAssignableFrom(type2)) {
							preference = 2;
						} else if (type2.isAssignableFrom(type1)) {
							preference = 1;
						} else {
							preference = 3;
						}
					} else {
						preference = 3;
					}

					totalPreference |= preference;
					if (totalPreference == 3) {
						break;
					}
				}
			}

			return totalPreference;
		}
	}

	private static void printDebug(String msg, MemberBox member, Object[] args) {
	}
}