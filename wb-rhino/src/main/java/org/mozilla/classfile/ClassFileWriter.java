/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.classfile;

import java.io.IOException;
import java.io.OutputStream;

import org.mozilla.classfile.ClassFileField;
import org.mozilla.classfile.ClassFileMethod;
import org.mozilla.classfile.ConstantPool;
import org.mozilla.classfile.ExceptionTableEntry;
import org.mozilla.javascript.ObjArray;

public class ClassFileWriter {
	public static final short ACC_PUBLIC = 1;
	public static final short ACC_PRIVATE = 2;
	public static final short ACC_PROTECTED = 4;
	public static final short ACC_STATIC = 8;
	public static final short ACC_FINAL = 16;
	public static final short ACC_SYNCHRONIZED = 32;
	public static final short ACC_VOLATILE = 64;
	public static final short ACC_TRANSIENT = 128;
	public static final short ACC_NATIVE = 256;
	public static final short ACC_ABSTRACT = 1024;
	private static final int LineNumberTableSize = 16;
	private static final int ExceptionTableSize = 4;
	@SuppressWarnings("unused")
	private static final long FileHeaderConstant = -3819410108756852691L;
	@SuppressWarnings("unused")
	private static final boolean DEBUGSTACK = false;
	@SuppressWarnings("unused")
	private static final boolean DEBUGLABELS = false;
	@SuppressWarnings("unused")
	private static final boolean DEBUGCODE = false;
	private String generatedClassName;
	private ExceptionTableEntry[] itsExceptionTable;
	private int itsExceptionTableTop;
	private int[] itsLineNumberTable;
	private int itsLineNumberTableTop;
	private byte[] itsCodeBuffer = new byte[256];
	private int itsCodeBufferTop;
	private ConstantPool itsConstantPool;
	private ClassFileMethod itsCurrentMethod;
	private short itsStackTop;
	private short itsMaxStack;
	private short itsMaxLocals;
	private ObjArray itsMethods = new ObjArray();
	private ObjArray itsFields = new ObjArray();
	private ObjArray itsInterfaces = new ObjArray();
	private short itsFlags;
	private short itsThisClassIndex;
	private short itsSuperClassIndex;
	private short itsSourceFileNameIndex;
	private static final int MIN_LABEL_TABLE_SIZE = 32;
	private int[] itsLabelTable;
	private int itsLabelTableTop;
	private static final int MIN_FIXUP_TABLE_SIZE = 40;
	private long[] itsFixupTable;
	private int itsFixupTableTop;
	private ObjArray itsVarDescriptors;
	private char[] tmpCharBuffer = new char[64];

	public ClassFileWriter(String className, String superClassName,
			String sourceFileName) {
		this.generatedClassName = className;
		this.itsConstantPool = new ConstantPool(this);
		this.itsThisClassIndex = this.itsConstantPool.addClass(className);
		this.itsSuperClassIndex = this.itsConstantPool.addClass(superClassName);
		if (sourceFileName != null) {
			this.itsSourceFileNameIndex = this.itsConstantPool
					.addUtf8(sourceFileName);
		}

		this.itsFlags = 1;
	}

	public final String getClassName() {
		return this.generatedClassName;
	}

	public void addInterface(String interfaceName) {
		short interfaceIndex = this.itsConstantPool.addClass(interfaceName);
		this.itsInterfaces.add(new Short(interfaceIndex));
	}

	public void setFlags(short flags) {
		this.itsFlags = flags;
	}

	static String getSlashedForm(String name) {
		return name.replace('.', '/');
	}

	public static String classNameToSignature(String name) {
		int nameLength = name.length();
		int colonPos = 1 + nameLength;
		char[] buf = new char[colonPos + 1];
		buf[0] = 76;
		buf[colonPos] = 59;
		name.getChars(0, nameLength, buf, 1);

		for (int i = 1; i != colonPos; ++i) {
			if (buf[i] == 46) {
				buf[i] = 47;
			}
		}

		return new String(buf, 0, colonPos + 1);
	}

	public void addField(String fieldName, String type, int i) {
		short fieldNameIndex = this.itsConstantPool.addUtf8(fieldName);
		short typeIndex = this.itsConstantPool.addUtf8(type);
		this.itsFields
				.add(new ClassFileField(fieldNameIndex, typeIndex, i));
	}

	public void addField(String fieldName, String type, short flags, int value) {
		short fieldNameIndex = this.itsConstantPool.addUtf8(fieldName);
		short typeIndex = this.itsConstantPool.addUtf8(type);
		ClassFileField field = new ClassFileField(fieldNameIndex, typeIndex,
				flags);
		field.setAttributes(this.itsConstantPool.addUtf8("ConstantValue"), 0,
				0, this.itsConstantPool.addConstant(value));
		this.itsFields.add(field);
	}

	public void addField(String fieldName, String type, short flags, long value) {
		short fieldNameIndex = this.itsConstantPool.addUtf8(fieldName);
		short typeIndex = this.itsConstantPool.addUtf8(type);
		ClassFileField field = new ClassFileField(fieldNameIndex, typeIndex,
				flags);
		field.setAttributes(this.itsConstantPool.addUtf8("ConstantValue"), 0,
				2, this.itsConstantPool.addConstant(value));
		this.itsFields.add(field);
	}

	public void addField(String fieldName, String type, short flags,
			double value) {
		short fieldNameIndex = this.itsConstantPool.addUtf8(fieldName);
		short typeIndex = this.itsConstantPool.addUtf8(type);
		ClassFileField field = new ClassFileField(fieldNameIndex, typeIndex,
				flags);
		field.setAttributes(this.itsConstantPool.addUtf8("ConstantValue"), 0,
				2, this.itsConstantPool.addConstant(value));
		this.itsFields.add(field);
	}

	public void addVariableDescriptor(String name, String type, int startPC,
			int register) {
		short nameIndex = this.itsConstantPool.addUtf8(name);
		short descriptorIndex = this.itsConstantPool.addUtf8(type);
		int[] chunk = new int[]{nameIndex, descriptorIndex, startPC, register};
		if (this.itsVarDescriptors == null) {
			this.itsVarDescriptors = new ObjArray();
		}

		this.itsVarDescriptors.add(chunk);
	}

	public void startMethod(String methodName, String type, int i) {
		short methodNameIndex = this.itsConstantPool.addUtf8(methodName);
		short typeIndex = this.itsConstantPool.addUtf8(type);
		this.itsCurrentMethod = new ClassFileMethod(methodNameIndex, typeIndex,
				i);
		this.itsMethods.add(this.itsCurrentMethod);
	}

	public void stopMethod(int j) {
		if (this.itsCurrentMethod == null) {
			throw new IllegalStateException("No method to stop");
		} else {
			this.fixLabelGotos();
			this.itsMaxLocals = (short) j;
			int lineNumberTableLength = 0;
			if (this.itsLineNumberTable != null) {
				lineNumberTableLength = 8 + this.itsLineNumberTableTop * 4;
			}

			int variableTableLength = 0;
			if (this.itsVarDescriptors != null) {
				variableTableLength = 8 + this.itsVarDescriptors.size() * 10;
			}

			int attrLength = 14 + this.itsCodeBufferTop + 2
					+ this.itsExceptionTableTop * 8 + 2 + lineNumberTableLength
					+ variableTableLength;
			if (attrLength > 65536) {
				throw new ClassFileWriter.ClassFileFormatException(
						"generated bytecode for method exceeds 64K limit.");
			} else {
				byte[] codeAttribute = new byte[attrLength];
				byte index = 0;
				short codeAttrIndex = this.itsConstantPool.addUtf8("Code");
				int arg18 = putInt16(codeAttrIndex, codeAttribute, index);
				attrLength -= 6;
				arg18 = putInt32(attrLength, codeAttribute, arg18);
				arg18 = putInt16(this.itsMaxStack, codeAttribute, arg18);
				arg18 = putInt16(this.itsMaxLocals, codeAttribute, arg18);
				arg18 = putInt32(this.itsCodeBufferTop, codeAttribute, arg18);
				System.arraycopy(this.itsCodeBuffer, 0, codeAttribute, arg18,
						this.itsCodeBufferTop);
				arg18 += this.itsCodeBufferTop;
				int attributeCount;
				if (this.itsExceptionTableTop > 0) {
					arg18 = putInt16(this.itsExceptionTableTop, codeAttribute,
							arg18);

					for (attributeCount = 0; attributeCount < this.itsExceptionTableTop; ++attributeCount) {
						ExceptionTableEntry variableTableAttrIndex = this.itsExceptionTable[attributeCount];
						short varCount = (short) this
								.getLabelPC(variableTableAttrIndex.itsStartLabel);
						short tableAttrLength = (short) this
								.getLabelPC(variableTableAttrIndex.itsEndLabel);
						short i = (short) this
								.getLabelPC(variableTableAttrIndex.itsHandlerLabel);
						short chunk = variableTableAttrIndex.itsCatchType;
						if (varCount == -1) {
							throw new IllegalStateException(
									"start label not defined");
						}

						if (tableAttrLength == -1) {
							throw new IllegalStateException(
									"end label not defined");
						}

						if (i == -1) {
							throw new IllegalStateException(
									"handler label not defined");
						}

						arg18 = putInt16(varCount, codeAttribute, arg18);
						arg18 = putInt16(tableAttrLength, codeAttribute, arg18);
						arg18 = putInt16(i, codeAttribute, arg18);
						arg18 = putInt16(chunk, codeAttribute, arg18);
					}
				} else {
					arg18 = putInt16(0, codeAttribute, arg18);
				}

				attributeCount = 0;
				if (this.itsLineNumberTable != null) {
					++attributeCount;
				}

				if (this.itsVarDescriptors != null) {
					++attributeCount;
				}

				arg18 = putInt16(attributeCount, codeAttribute, arg18);
				short arg19;
				int arg20;
				int arg21;
				if (this.itsLineNumberTable != null) {
					arg19 = this.itsConstantPool.addUtf8("LineNumberTable");
					arg18 = putInt16(arg19, codeAttribute, arg18);
					arg20 = 2 + this.itsLineNumberTableTop * 4;
					arg18 = putInt32(arg20, codeAttribute, arg18);
					arg18 = putInt16(this.itsLineNumberTableTop, codeAttribute,
							arg18);

					for (arg21 = 0; arg21 < this.itsLineNumberTableTop; ++arg21) {
						arg18 = putInt32(this.itsLineNumberTable[arg21],
								codeAttribute, arg18);
					}
				}

				if (this.itsVarDescriptors != null) {
					arg19 = this.itsConstantPool.addUtf8("LocalVariableTable");
					arg18 = putInt16(arg19, codeAttribute, arg18);
					arg20 = this.itsVarDescriptors.size();
					arg21 = 2 + arg20 * 10;
					arg18 = putInt32(arg21, codeAttribute, arg18);
					arg18 = putInt16(arg20, codeAttribute, arg18);

					for (int arg22 = 0; arg22 < arg20; ++arg22) {
						int[] arg23 = (int[]) ((int[]) this.itsVarDescriptors
								.get(arg22));
						int nameIndex = arg23[0];
						int descriptorIndex = arg23[1];
						int startPC = arg23[2];
						int register = arg23[3];
						int length = this.itsCodeBufferTop - startPC;
						arg18 = putInt16(startPC, codeAttribute, arg18);
						arg18 = putInt16(length, codeAttribute, arg18);
						arg18 = putInt16(nameIndex, codeAttribute, arg18);
						arg18 = putInt16(descriptorIndex, codeAttribute, arg18);
						arg18 = putInt16(register, codeAttribute, arg18);
					}
				}

				this.itsCurrentMethod.setCodeAttribute(codeAttribute);
				this.itsExceptionTable = null;
				this.itsExceptionTableTop = 0;
				this.itsLineNumberTableTop = 0;
				this.itsCodeBufferTop = 0;
				this.itsCurrentMethod = null;
				this.itsMaxStack = 0;
				this.itsStackTop = 0;
				this.itsLabelTableTop = 0;
				this.itsFixupTableTop = 0;
				this.itsVarDescriptors = null;
			}
		}
	}

	public void add(int theOpCode) {
		if (opcodeCount(theOpCode) != 0) {
			throw new IllegalArgumentException("Unexpected operands");
		} else {
			int newStack = this.itsStackTop + stackChange(theOpCode);
			if (newStack < 0 || 32767 < newStack) {
				badStack(newStack);
			}

			this.addToCodeBuffer(theOpCode);
			this.itsStackTop = (short) newStack;
			if (newStack > this.itsMaxStack) {
				this.itsMaxStack = (short) newStack;
			}

		}
	}

	public void add(int theOpCode, int theOperand) {
		int newStack = this.itsStackTop + stackChange(theOpCode);
		if (newStack < 0 || 32767 < newStack) {
			badStack(newStack);
		}

		switch (theOpCode) {
			case 16 :
				if ((byte) theOperand != theOperand) {
					throw new IllegalArgumentException("out of range byte");
				}

				this.addToCodeBuffer(theOpCode);
				this.addToCodeBuffer((byte) theOperand);
				break;
			case 17 :
				if ((short) theOperand != theOperand) {
					throw new IllegalArgumentException("out of range short");
				}

				this.addToCodeBuffer(theOpCode);
				this.addToCodeInt16(theOperand);
				break;
			case 18 :
			case 19 :
			case 20 :
				if (0 <= theOperand && theOperand < 65536) {
					if (theOperand < 256 && theOpCode != 19 && theOpCode != 20) {
						this.addToCodeBuffer(theOpCode);
						this.addToCodeBuffer(theOperand);
					} else {
						if (theOpCode == 18) {
							this.addToCodeBuffer(19);
						} else {
							this.addToCodeBuffer(theOpCode);
						}

						this.addToCodeInt16(theOperand);
					}
					break;
				}

				throw new IllegalArgumentException("out of range index");
			case 21 :
			case 22 :
			case 23 :
			case 24 :
			case 25 :
			case 54 :
			case 55 :
			case 56 :
			case 57 :
			case 58 :
			case 169 :
				if (0 <= theOperand && theOperand < 65536) {
					if (theOperand >= 256) {
						this.addToCodeBuffer(196);
						this.addToCodeBuffer(theOpCode);
						this.addToCodeInt16(theOperand);
					} else {
						this.addToCodeBuffer(theOpCode);
						this.addToCodeBuffer(theOperand);
					}
					break;
				}

				throw new ClassFileWriter.ClassFileFormatException(
						"out of range variable");
			case 153 :
			case 154 :
			case 155 :
			case 156 :
			case 157 :
			case 158 :
			case 159 :
			case 160 :
			case 161 :
			case 162 :
			case 163 :
			case 164 :
			case 165 :
			case 166 :
			case 167 :
			case 168 :
			case 198 :
			case 199 :
				if ((theOperand & Integer.MIN_VALUE) != Integer.MIN_VALUE
						&& (theOperand < 0 || theOperand > '￿')) {
					throw new IllegalArgumentException("Bad label for branch");
				}

				int branchPC = this.itsCodeBufferTop;
				this.addToCodeBuffer(theOpCode);
				if ((theOperand & Integer.MIN_VALUE) != Integer.MIN_VALUE) {
					this.addToCodeInt16(theOperand);
				} else {
					int targetPC = this.getLabelPC(theOperand);
					if (targetPC != -1) {
						int offset = targetPC - branchPC;
						this.addToCodeInt16(offset);
					} else {
						this.addLabelFixup(theOperand, branchPC + 1);
						this.addToCodeInt16(0);
					}
				}
				break;
			case 180 :
			case 181 :
				if (0 <= theOperand && theOperand < 65536) {
					this.addToCodeBuffer(theOpCode);
					this.addToCodeInt16(theOperand);
					break;
				}

				throw new IllegalArgumentException("out of range field");
			case 188 :
				if (0 <= theOperand && theOperand < 256) {
					this.addToCodeBuffer(theOpCode);
					this.addToCodeBuffer(theOperand);
					break;
				}

				throw new IllegalArgumentException("out of range index");
			default :
				throw new IllegalArgumentException(
						"Unexpected opcode for 1 operand");
		}

		this.itsStackTop = (short) newStack;
		if (newStack > this.itsMaxStack) {
			this.itsMaxStack = (short) newStack;
		}

	}

	public void addLoadConstant(int k) {
		switch (k) {
			case 0 :
				this.add(3);
				break;
			case 1 :
				this.add(4);
				break;
			case 2 :
				this.add(5);
				break;
			case 3 :
				this.add(6);
				break;
			case 4 :
				this.add(7);
				break;
			case 5 :
				this.add(8);
				break;
			default :
				this.add(18, this.itsConstantPool.addConstant(k));
		}

	}

	public void addLoadConstant(long k) {
		this.add(20, this.itsConstantPool.addConstant(k));
	}

	public void addLoadConstant(float k) {
		this.add(18, this.itsConstantPool.addConstant(k));
	}

	public void addLoadConstant(double k) {
		this.add(20, this.itsConstantPool.addConstant(k));
	}

	public void addLoadConstant(String k) {
		this.add(18, this.itsConstantPool.addConstant(k));
	}

	public void add(int theOpCode, int theOperand1, int theOperand2) {
		int newStack = this.itsStackTop + stackChange(theOpCode);
		if (newStack < 0 || 32767 < newStack) {
			badStack(newStack);
		}

		if (theOpCode == 132) {
			label46 : {
				if (0 <= theOperand1 && theOperand1 < 65536) {
					if (0 <= theOperand2 && theOperand2 < 65536) {
						if (theOperand1 <= 255 && theOperand2 >= -128
								&& theOperand2 <= 127) {
							this.addToCodeBuffer(196);
							this.addToCodeBuffer(132);
							this.addToCodeBuffer(theOperand1);
							this.addToCodeBuffer(theOperand2);
						} else {
							this.addToCodeBuffer(196);
							this.addToCodeBuffer(132);
							this.addToCodeInt16(theOperand1);
							this.addToCodeInt16(theOperand2);
						}
						break label46;
					}

					throw new ClassFileWriter.ClassFileFormatException(
							"out of range increment");
				}

				throw new ClassFileWriter.ClassFileFormatException(
						"out of range variable");
			}
		} else {
			label64 : {
				if (theOpCode != 197) {
					throw new IllegalArgumentException(
							"Unexpected opcode for 2 operands");
				}

				if (0 <= theOperand1 && theOperand1 < 65536) {
					if (0 <= theOperand2 && theOperand2 < 256) {
						this.addToCodeBuffer(197);
						this.addToCodeInt16(theOperand1);
						this.addToCodeBuffer(theOperand2);
						break label64;
					}

					throw new IllegalArgumentException(
							"out of range dimensions");
				}

				throw new IllegalArgumentException("out of range index");
			}
		}

		this.itsStackTop = (short) newStack;
		if (newStack > this.itsMaxStack) {
			this.itsMaxStack = (short) newStack;
		}

	}

	public void add(int theOpCode, String className) {
		int newStack = this.itsStackTop + stackChange(theOpCode);
		if (newStack < 0 || 32767 < newStack) {
			badStack(newStack);
		}

		switch (theOpCode) {
			case 187 :
			case 189 :
			case 192 :
			case 193 :
				short classIndex = this.itsConstantPool.addClass(className);
				this.addToCodeBuffer(theOpCode);
				this.addToCodeInt16(classIndex);
				this.itsStackTop = (short) newStack;
				if (newStack > this.itsMaxStack) {
					this.itsMaxStack = (short) newStack;
				}

				return;
			case 188 :
			case 190 :
			case 191 :
			default :
				throw new IllegalArgumentException(
						"bad opcode for class reference");
		}
	}

	public void add(int theOpCode, String className, String fieldName,
			String fieldType) {
		int newStack = this.itsStackTop + stackChange(theOpCode);
		char fieldTypeChar = fieldType.charAt(0);
		int fieldSize = fieldTypeChar != 74 && fieldTypeChar != 68 ? 1 : 2;
		switch (theOpCode) {
			case 178 :
			case 180 :
				newStack += fieldSize;
				break;
			case 179 :
			case 181 :
				newStack -= fieldSize;
				break;
			default :
				throw new IllegalArgumentException(
						"bad opcode for field reference");
		}

		if (newStack < 0 || 32767 < newStack) {
			badStack(newStack);
		}

		short fieldRefIndex = this.itsConstantPool.addFieldRef(className,
				fieldName, fieldType);
		this.addToCodeBuffer(theOpCode);
		this.addToCodeInt16(fieldRefIndex);
		this.itsStackTop = (short) newStack;
		if (newStack > this.itsMaxStack) {
			this.itsMaxStack = (short) newStack;
		}

	}

	public void addInvoke(int theOpCode, String className, String methodName,
			String methodType) {
		int parameterInfo = sizeOfParameters(methodType);
		int parameterCount = parameterInfo >>> 16;
		short stackDiff = (short) parameterInfo;
		int newStack = this.itsStackTop + stackDiff;
		newStack += stackChange(theOpCode);
		if (newStack < 0 || 32767 < newStack) {
			badStack(newStack);
		}

		switch (theOpCode) {
			case 182 :
			case 183 :
			case 184 :
			case 185 :
				this.addToCodeBuffer(theOpCode);
				short methodRefIndex;
				if (theOpCode == 185) {
					methodRefIndex = this.itsConstantPool
							.addInterfaceMethodRef(className, methodName,
									methodType);
					this.addToCodeInt16(methodRefIndex);
					this.addToCodeBuffer(parameterCount + 1);
					this.addToCodeBuffer(0);
				} else {
					methodRefIndex = this.itsConstantPool.addMethodRef(
							className, methodName, methodType);
					this.addToCodeInt16(methodRefIndex);
				}

				this.itsStackTop = (short) newStack;
				if (newStack > this.itsMaxStack) {
					this.itsMaxStack = (short) newStack;
				}

				return;
			default :
				throw new IllegalArgumentException(
						"bad opcode for method reference");
		}
	}

	public void addPush(int k) {
		if ((byte) k == k) {
			if (k == -1) {
				this.add(2);
			} else if (0 <= k && k <= 5) {
				this.add((byte) (3 + k));
			} else {
				this.add(16, (byte) k);
			}
		} else if ((short) k == k) {
			this.add(17, (short) k);
		} else {
			this.addLoadConstant(k);
		}

	}

	public void addPush(boolean k) {
		this.add(k ? 4 : 3);
	}

	public void addPush(long k) {
		int ik = (int) k;
		if ((long) ik == k) {
			this.addPush(ik);
			this.add(133);
		} else {
			this.addLoadConstant(k);
		}

	}

	public void addPush(double k) {
		if (k == 0.0D) {
			this.add(14);
			if (1.0D / k < 0.0D) {
				this.add(119);
			}
		} else if (k != 1.0D && k != -1.0D) {
			this.addLoadConstant(k);
		} else {
			this.add(15);
			if (k < 0.0D) {
				this.add(119);
			}
		}

	}

	@SuppressWarnings("unused")
	public void addPush(String k) {
		int length = k.length();
		int limit = this.itsConstantPool.getUtfEncodingLimit(k, 0, length);
		if (limit == length) {
			this.addLoadConstant(k);
		} else {
			String SB = "java/lang/StringBuffer";
			this.add(187, "java/lang/StringBuffer");
			this.add(89);
			this.addPush(length);
			this.addInvoke(183, "java/lang/StringBuffer", "<init>", "(I)V");
			int cursor = 0;

			while (true) {
				this.add(89);
				String s = k.substring(cursor, limit);
				this.addLoadConstant(s);
				this.addInvoke(182, "java/lang/StringBuffer", "append",
						"(Ljava/lang/String;)Ljava/lang/StringBuffer;");
				this.add(87);
				if (limit == length) {
					this.addInvoke(182, "java/lang/StringBuffer", "toString",
							"()Ljava/lang/String;");
					return;
				}

				cursor = limit;
				limit = this.itsConstantPool.getUtfEncodingLimit(k, limit,
						length);
			}
		}
	}

	public boolean isUnderStringSizeLimit(String k) {
		return this.itsConstantPool.isUnderUtfEncodingLimit(k);
	}

	public void addIStore(int local) {
		this.xop(59, 54, local);
	}

	public void addLStore(int local) {
		this.xop(63, 55, local);
	}

	public void addFStore(int local) {
		this.xop(67, 56, local);
	}

	public void addDStore(int local) {
		this.xop(71, 57, local);
	}

	public void addAStore(int local) {
		this.xop(75, 58, local);
	}

	public void addILoad(int local) {
		this.xop(26, 21, local);
	}

	public void addLLoad(int local) {
		this.xop(30, 22, local);
	}

	public void addFLoad(int local) {
		this.xop(34, 23, local);
	}

	public void addDLoad(int local) {
		this.xop(38, 24, local);
	}

	public void addALoad(int local) {
		this.xop(42, 25, local);
	}

	public void addLoadThis() {
		this.add(42);
	}

	private void xop(int shortOp, int op, int local) {
		switch (local) {
			case 0 :
				this.add(shortOp);
				break;
			case 1 :
				this.add(shortOp + 1);
				break;
			case 2 :
				this.add(shortOp + 2);
				break;
			case 3 :
				this.add(shortOp + 3);
				break;
			default :
				this.add(op, local);
		}

	}

	public int addTableSwitch(int low, int high) {
		if (low > high) {
			throw new ClassFileWriter.ClassFileFormatException("Bad bounds: "
					+ low + ' ' + high);
		} else {
			int newStack = this.itsStackTop + stackChange(170);
			if (newStack < 0 || 32767 < newStack) {
				badStack(newStack);
			}

			int entryCount = high - low + 1;
			int padSize = 3 & ~this.itsCodeBufferTop;
			int N = this.addReservedCodeSpace(1 + padSize + 4
					* (3 + entryCount));
			int switchStart = N;

			for (this.itsCodeBuffer[N++] = -86; padSize != 0; --padSize) {
				this.itsCodeBuffer[N++] = 0;
			}

			N += 4;
			N = putInt32(low, this.itsCodeBuffer, N);
			putInt32(high, this.itsCodeBuffer, N);
			this.itsStackTop = (short) newStack;
			if (newStack > this.itsMaxStack) {
				this.itsMaxStack = (short) newStack;
			}

			return switchStart;
		}
	}

	public final void markTableSwitchDefault(int switchStart) {
		this.setTableSwitchJump(switchStart, -1, this.itsCodeBufferTop);
	}

	public final void markTableSwitchCase(int switchStart, int caseIndex) {
		this.setTableSwitchJump(switchStart, caseIndex, this.itsCodeBufferTop);
	}

	public final void markTableSwitchCase(int switchStart, int caseIndex,
			int stackTop) {
		if (0 <= stackTop && stackTop <= this.itsMaxStack) {
			this.itsStackTop = (short) stackTop;
			this.setTableSwitchJump(switchStart, caseIndex,
					this.itsCodeBufferTop);
		} else {
			throw new IllegalArgumentException("Bad stack index: " + stackTop);
		}
	}

	public void setTableSwitchJump(int switchStart, int caseIndex,
			int jumpTarget) {
		if (0 <= jumpTarget && jumpTarget <= this.itsCodeBufferTop) {
			if (caseIndex < -1) {
				throw new IllegalArgumentException("Bad case index: "
						+ caseIndex);
			} else {
				int padSize = 3 & ~switchStart;
				int caseOffset;
				if (caseIndex < 0) {
					caseOffset = switchStart + 1 + padSize;
				} else {
					caseOffset = switchStart + 1 + padSize + 4
							* (3 + caseIndex);
				}

				if (0 <= switchStart
						&& switchStart <= this.itsCodeBufferTop - 16 - padSize
								- 1) {
					if ((255 & this.itsCodeBuffer[switchStart]) != 170) {
						throw new IllegalArgumentException(switchStart
								+ " is not offset of tableswitch statement");
					} else if (0 <= caseOffset
							&& caseOffset + 4 <= this.itsCodeBufferTop) {
						putInt32(jumpTarget - switchStart, this.itsCodeBuffer,
								caseOffset);
					} else {
						throw new ClassFileWriter.ClassFileFormatException(
								"Too big case index: " + caseIndex);
					}
				} else {
					throw new IllegalArgumentException(switchStart
							+ " is outside a possible range of tableswitch"
							+ " in already generated code");
				}
			}
		} else {
			throw new IllegalArgumentException("Bad jump target: " + jumpTarget);
		}
	}

	public int acquireLabel() {
		int top = this.itsLabelTableTop;
		if (this.itsLabelTable == null || top == this.itsLabelTable.length) {
			if (this.itsLabelTable == null) {
				this.itsLabelTable = new int[32];
			} else {
				int[] tmp = new int[this.itsLabelTable.length * 2];
				System.arraycopy(this.itsLabelTable, 0, tmp, 0, top);
				this.itsLabelTable = tmp;
			}
		}

		this.itsLabelTableTop = top + 1;
		this.itsLabelTable[top] = -1;
		return top | Integer.MIN_VALUE;
	}

	public void markLabel(int label) {
		if (label >= 0) {
			throw new IllegalArgumentException("Bad label, no biscuit");
		} else {
			label &= Integer.MAX_VALUE;
			if (label > this.itsLabelTableTop) {
				throw new IllegalArgumentException("Bad label");
			} else if (this.itsLabelTable[label] != -1) {
				throw new IllegalStateException("Can only mark label once");
			} else {
				this.itsLabelTable[label] = this.itsCodeBufferTop;
			}
		}
	}

	public void markLabel(int label, int i) {
		this.markLabel(label);
		this.itsStackTop = (short) i;
	}

	public void markHandler(int theLabel) {
		this.itsStackTop = 1;
		this.markLabel(theLabel);
	}

	private int getLabelPC(int label) {
		if (label >= 0) {
			throw new IllegalArgumentException("Bad label, no biscuit");
		} else {
			label &= Integer.MAX_VALUE;
			if (label >= this.itsLabelTableTop) {
				throw new IllegalArgumentException("Bad label");
			} else {
				return this.itsLabelTable[label];
			}
		}
	}

	private void addLabelFixup(int label, int fixupSite) {
		if (label >= 0) {
			throw new IllegalArgumentException("Bad label, no biscuit");
		} else {
			label &= Integer.MAX_VALUE;
			if (label >= this.itsLabelTableTop) {
				throw new IllegalArgumentException("Bad label");
			} else {
				int top = this.itsFixupTableTop;
				if (this.itsFixupTable == null
						|| top == this.itsFixupTable.length) {
					if (this.itsFixupTable == null) {
						this.itsFixupTable = new long[40];
					} else {
						long[] tmp = new long[this.itsFixupTable.length * 2];
						System.arraycopy(this.itsFixupTable, 0, tmp, 0, top);
						this.itsFixupTable = tmp;
					}
				}

				this.itsFixupTableTop = top + 1;
				this.itsFixupTable[top] = (long) label << 32 | (long) fixupSite;
			}
		}
	}

	private void fixLabelGotos() {
		byte[] codeBuffer = this.itsCodeBuffer;

		for (int i = 0; i < this.itsFixupTableTop; ++i) {
			long fixup = this.itsFixupTable[i];
			int label = (int) (fixup >> 32);
			int fixupSite = (int) fixup;
			int pc = this.itsLabelTable[label];
			if (pc == -1) {
				throw new RuntimeException();
			}

			int offset = pc - (fixupSite - 1);
			if ((short) offset != offset) {
				throw new ClassFileWriter.ClassFileFormatException(
						"Program too complex: too big jump offset");
			}

			codeBuffer[fixupSite] = (byte) (offset >> 8);
			codeBuffer[fixupSite + 1] = (byte) offset;
		}

		this.itsFixupTableTop = 0;
	}

	public int getCurrentCodeOffset() {
		return this.itsCodeBufferTop;
	}

	public short getStackTop() {
		return this.itsStackTop;
	}

	public void setStackTop(int i) {
		this.itsStackTop = (short) i;
	}

	public void adjustStackTop(int delta) {
		int newStack = this.itsStackTop + delta;
		if (newStack < 0 || 32767 < newStack) {
			badStack(newStack);
		}

		this.itsStackTop = (short) newStack;
		if (newStack > this.itsMaxStack) {
			this.itsMaxStack = (short) newStack;
		}

	}

	private void addToCodeBuffer(int b) {
		int N = this.addReservedCodeSpace(1);
		this.itsCodeBuffer[N] = (byte) b;
	}

	private void addToCodeInt16(int value) {
		int N = this.addReservedCodeSpace(2);
		putInt16(value, this.itsCodeBuffer, N);
	}

	private int addReservedCodeSpace(int size) {
		if (this.itsCurrentMethod == null) {
			throw new IllegalArgumentException("No method to add to");
		} else {
			int oldTop = this.itsCodeBufferTop;
			int newTop = oldTop + size;
			if (newTop > this.itsCodeBuffer.length) {
				int newSize = this.itsCodeBuffer.length * 2;
				if (newTop > newSize) {
					newSize = newTop;
				}

				byte[] tmp = new byte[newSize];
				System.arraycopy(this.itsCodeBuffer, 0, tmp, 0, oldTop);
				this.itsCodeBuffer = tmp;
			}

			this.itsCodeBufferTop = newTop;
			return oldTop;
		}
	}

	public void addExceptionHandler(int startLabel, int endLabel,
			int handlerLabel, String catchClassName) {
		if ((startLabel & Integer.MIN_VALUE) != Integer.MIN_VALUE) {
			throw new IllegalArgumentException("Bad startLabel");
		} else if ((endLabel & Integer.MIN_VALUE) != Integer.MIN_VALUE) {
			throw new IllegalArgumentException("Bad endLabel");
		} else if ((handlerLabel & Integer.MIN_VALUE) != Integer.MIN_VALUE) {
			throw new IllegalArgumentException("Bad handlerLabel");
		} else {
			short catch_type_index = catchClassName == null
					? 0
					: this.itsConstantPool.addClass(catchClassName);
			ExceptionTableEntry newEntry = new ExceptionTableEntry(startLabel,
					endLabel, handlerLabel, catch_type_index);
			int N = this.itsExceptionTableTop;
			if (N == 0) {
				this.itsExceptionTable = new ExceptionTableEntry[4];
			} else if (N == this.itsExceptionTable.length) {
				ExceptionTableEntry[] tmp = new ExceptionTableEntry[N * 2];
				System.arraycopy(this.itsExceptionTable, 0, tmp, 0, N);
				this.itsExceptionTable = tmp;
			}

			this.itsExceptionTable[N] = newEntry;
			this.itsExceptionTableTop = N + 1;
		}
	}

	public void addLineNumberEntry(short lineNumber) {
		if (this.itsCurrentMethod == null) {
			throw new IllegalArgumentException("No method to stop");
		} else {
			int N = this.itsLineNumberTableTop;
			if (N == 0) {
				this.itsLineNumberTable = new int[16];
			} else if (N == this.itsLineNumberTable.length) {
				int[] tmp = new int[N * 2];
				System.arraycopy(this.itsLineNumberTable, 0, tmp, 0, N);
				this.itsLineNumberTable = tmp;
			}

			this.itsLineNumberTable[N] = (this.itsCodeBufferTop << 16)
					+ lineNumber;
			this.itsLineNumberTableTop = N + 1;
		}
	}

	public void write(OutputStream oStream) throws IOException {
		byte[] array = this.toByteArray();
		oStream.write(array);
	}

	private int getWriteSize() {
		byte size = 0;
		if (this.itsSourceFileNameIndex != 0) {
			this.itsConstantPool.addUtf8("SourceFile");
		}

		int arg2 = size + 8;
		arg2 += this.itsConstantPool.getWriteSize();
		arg2 += 2;
		arg2 += 2;
		arg2 += 2;
		arg2 += 2;
		arg2 += 2 * this.itsInterfaces.size();
		arg2 += 2;

		int i;
		for (i = 0; i < this.itsFields.size(); ++i) {
			arg2 += ((ClassFileField) ((ClassFileField) this.itsFields.get(i)))
					.getWriteSize();
		}

		arg2 += 2;

		for (i = 0; i < this.itsMethods.size(); ++i) {
			arg2 += ((ClassFileMethod) ((ClassFileMethod) this.itsMethods
					.get(i))).getWriteSize();
		}

		if (this.itsSourceFileNameIndex != 0) {
			arg2 += 2;
			arg2 += 2;
			arg2 += 4;
			arg2 += 2;
		} else {
			arg2 += 2;
		}

		return arg2;
	}

	public byte[] toByteArray() {
		int dataSize = this.getWriteSize();
		byte[] data = new byte[dataSize];
		byte offset = 0;
		short sourceFileAttributeNameIndex = 0;
		if (this.itsSourceFileNameIndex != 0) {
			sourceFileAttributeNameIndex = this.itsConstantPool
					.addUtf8("SourceFile");
		}

		int arg6 = putInt64(-3819410108756852691L, data, offset);
		arg6 = this.itsConstantPool.write(data, arg6);
		arg6 = putInt16(this.itsFlags, data, arg6);
		arg6 = putInt16(this.itsThisClassIndex, data, arg6);
		arg6 = putInt16(this.itsSuperClassIndex, data, arg6);
		arg6 = putInt16(this.itsInterfaces.size(), data, arg6);

		int i;
		for (i = 0; i < this.itsInterfaces.size(); ++i) {
			short method = ((Short) ((Short) this.itsInterfaces.get(i)))
					.shortValue();
			arg6 = putInt16(method, data, arg6);
		}

		arg6 = putInt16(this.itsFields.size(), data, arg6);

		for (i = 0; i < this.itsFields.size(); ++i) {
			ClassFileField arg7 = (ClassFileField) this.itsFields.get(i);
			arg6 = arg7.write(data, arg6);
		}

		arg6 = putInt16(this.itsMethods.size(), data, arg6);

		for (i = 0; i < this.itsMethods.size(); ++i) {
			ClassFileMethod arg8 = (ClassFileMethod) this.itsMethods.get(i);
			arg6 = arg8.write(data, arg6);
		}

		if (this.itsSourceFileNameIndex != 0) {
			arg6 = putInt16(1, data, arg6);
			arg6 = putInt16(sourceFileAttributeNameIndex, data, arg6);
			arg6 = putInt32(2, data, arg6);
			arg6 = putInt16(this.itsSourceFileNameIndex, data, arg6);
		} else {
			arg6 = putInt16(0, data, arg6);
		}

		if (arg6 != dataSize) {
			throw new RuntimeException();
		} else {
			return data;
		}
	}

	static int putInt64(long value, byte[] array, int offset) {
		offset = putInt32((int) (value >>> 32), array, offset);
		return putInt32((int) value, array, offset);
	}

	private static void badStack(int value) {
		String s;
		if (value < 0) {
			s = "Stack underflow: " + value;
		} else {
			s = "Too big stack: " + value;
		}

		throw new IllegalStateException(s);
	}

	private static int sizeOfParameters(String pString) {
		int length = pString.length();
		int rightParenthesis = pString.lastIndexOf(41);
		if (3 <= length && pString.charAt(0) == 40 && 1 <= rightParenthesis
				&& rightParenthesis + 1 < length) {
			boolean ok = true;
			int index = 1;
			int stackDiff = 0;
			int count = 0;

			label55 : while (index != rightParenthesis) {
				switch (pString.charAt(index)) {
					case 'D' :
					case 'J' :
						--stackDiff;
					case 'B' :
					case 'C' :
					case 'F' :
					case 'I' :
					case 'S' :
					case 'Z' :
						--stackDiff;
						++count;
						++index;
						break;
					case 'E' :
					case 'G' :
					case 'H' :
					case 'K' :
					case 'M' :
					case 'N' :
					case 'O' :
					case 'P' :
					case 'Q' :
					case 'R' :
					case 'T' :
					case 'U' :
					case 'V' :
					case 'W' :
					case 'X' :
					case 'Y' :
					default :
						ok = false;
						break label55;
					case '[' :
						++index;

						char c;
						for (c = pString.charAt(index); c == 91; c = pString
								.charAt(index)) {
							++index;
						}

						switch (c) {
							case 'B' :
							case 'C' :
							case 'D' :
							case 'F' :
							case 'I' :
							case 'J' :
							case 'S' :
							case 'Z' :
								--stackDiff;
								++count;
								++index;
								continue;
							case 'E' :
							case 'G' :
							case 'H' :
							case 'K' :
							case 'M' :
							case 'N' :
							case 'O' :
							case 'P' :
							case 'Q' :
							case 'R' :
							case 'T' :
							case 'U' :
							case 'V' :
							case 'W' :
							case 'X' :
							case 'Y' :
							default :
								ok = false;
								break label55;
							case 'L' :
						}
					case 'L' :
						--stackDiff;
						++count;
						++index;
						int semicolon = pString.indexOf(59, index);
						if (index + 1 > semicolon
								|| semicolon >= rightParenthesis) {
							ok = false;
							break label55;
						}

						index = semicolon + 1;
				}
			}

			if (ok) {
				switch (pString.charAt(rightParenthesis + 1)) {
					case 'D' :
					case 'J' :
						++stackDiff;
					case 'B' :
					case 'C' :
					case 'F' :
					case 'I' :
					case 'L' :
					case 'S' :
					case 'Z' :
					case '[' :
						++stackDiff;
						break;
					case 'E' :
					case 'G' :
					case 'H' :
					case 'K' :
					case 'M' :
					case 'N' :
					case 'O' :
					case 'P' :
					case 'Q' :
					case 'R' :
					case 'T' :
					case 'U' :
					case 'W' :
					case 'X' :
					case 'Y' :
					default :
						ok = false;
					case 'V' :
				}

				if (ok) {
					return count << 16 | '￿' & stackDiff;
				}
			}
		}

		throw new IllegalArgumentException("Bad parameter signature: "
				+ pString);
	}

	static int putInt16(int value, byte[] array, int offset) {
		array[offset + 0] = (byte) (value >>> 8);
		array[offset + 1] = (byte) value;
		return offset + 2;
	}

	static int putInt32(int value, byte[] array, int offset) {
		array[offset + 0] = (byte) (value >>> 24);
		array[offset + 1] = (byte) (value >>> 16);
		array[offset + 2] = (byte) (value >>> 8);
		array[offset + 3] = (byte) value;
		return offset + 4;
	}

	static int opcodeCount(int opcode) {
		switch (opcode) {
			case 0 :
			case 1 :
			case 2 :
			case 3 :
			case 4 :
			case 5 :
			case 6 :
			case 7 :
			case 8 :
			case 9 :
			case 10 :
			case 11 :
			case 12 :
			case 13 :
			case 14 :
			case 15 :
			case 26 :
			case 27 :
			case 28 :
			case 29 :
			case 30 :
			case 31 :
			case 32 :
			case 33 :
			case 34 :
			case 35 :
			case 36 :
			case 37 :
			case 38 :
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
			case 50 :
			case 51 :
			case 52 :
			case 53 :
			case 59 :
			case 60 :
			case 61 :
			case 62 :
			case 63 :
			case 64 :
			case 65 :
			case 66 :
			case 67 :
			case 68 :
			case 69 :
			case 70 :
			case 71 :
			case 72 :
			case 73 :
			case 74 :
			case 75 :
			case 76 :
			case 77 :
			case 78 :
			case 79 :
			case 80 :
			case 81 :
			case 82 :
			case 83 :
			case 84 :
			case 85 :
			case 86 :
			case 87 :
			case 88 :
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
			case 101 :
			case 102 :
			case 103 :
			case 104 :
			case 105 :
			case 106 :
			case 107 :
			case 108 :
			case 109 :
			case 110 :
			case 111 :
			case 112 :
			case 113 :
			case 114 :
			case 115 :
			case 116 :
			case 117 :
			case 118 :
			case 119 :
			case 120 :
			case 121 :
			case 122 :
			case 123 :
			case 124 :
			case 125 :
			case 126 :
			case 127 :
			case 128 :
			case 129 :
			case 130 :
			case 131 :
			case 133 :
			case 134 :
			case 135 :
			case 136 :
			case 137 :
			case 138 :
			case 139 :
			case 140 :
			case 141 :
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
			case 152 :
			case 172 :
			case 173 :
			case 174 :
			case 175 :
			case 176 :
			case 177 :
			case 190 :
			case 191 :
			case 194 :
			case 195 :
			case 196 :
			case 202 :
			case 254 :
			case 255 :
				return 0;
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
			case 54 :
			case 55 :
			case 56 :
			case 57 :
			case 58 :
			case 153 :
			case 154 :
			case 155 :
			case 156 :
			case 157 :
			case 158 :
			case 159 :
			case 160 :
			case 161 :
			case 162 :
			case 163 :
			case 164 :
			case 165 :
			case 166 :
			case 167 :
			case 168 :
			case 169 :
			case 178 :
			case 179 :
			case 180 :
			case 181 :
			case 182 :
			case 183 :
			case 184 :
			case 185 :
			case 187 :
			case 188 :
			case 189 :
			case 192 :
			case 193 :
			case 198 :
			case 199 :
			case 200 :
			case 201 :
				return 1;
			case 132 :
			case 197 :
				return 2;
			case 170 :
			case 171 :
				return -1;
			case 186 :
			case 203 :
			case 204 :
			case 205 :
			case 206 :
			case 207 :
			case 208 :
			case 209 :
			case 210 :
			case 211 :
			case 212 :
			case 213 :
			case 214 :
			case 215 :
			case 216 :
			case 217 :
			case 218 :
			case 219 :
			case 220 :
			case 221 :
			case 222 :
			case 223 :
			case 224 :
			case 225 :
			case 226 :
			case 227 :
			case 228 :
			case 229 :
			case 230 :
			case 231 :
			case 232 :
			case 233 :
			case 234 :
			case 235 :
			case 236 :
			case 237 :
			case 238 :
			case 239 :
			case 240 :
			case 241 :
			case 242 :
			case 243 :
			case 244 :
			case 245 :
			case 246 :
			case 247 :
			case 248 :
			case 249 :
			case 250 :
			case 251 :
			case 252 :
			case 253 :
			default :
				throw new IllegalArgumentException("Bad opcode: " + opcode);
		}
	}

	static int stackChange(int opcode) {
		switch (opcode) {
			case 0 :
			case 47 :
			case 49 :
			case 95 :
			case 116 :
			case 117 :
			case 118 :
			case 119 :
			case 132 :
			case 134 :
			case 138 :
			case 139 :
			case 143 :
			case 145 :
			case 146 :
			case 147 :
			case 167 :
			case 169 :
			case 177 :
			case 178 :
			case 179 :
			case 184 :
			case 188 :
			case 189 :
			case 190 :
			case 192 :
			case 193 :
			case 196 :
			case 200 :
			case 202 :
			case 254 :
			case 255 :
				return 0;
			case 1 :
			case 2 :
			case 3 :
			case 4 :
			case 5 :
			case 6 :
			case 7 :
			case 8 :
			case 11 :
			case 12 :
			case 13 :
			case 16 :
			case 17 :
			case 18 :
			case 19 :
			case 21 :
			case 23 :
			case 25 :
			case 26 :
			case 27 :
			case 28 :
			case 29 :
			case 34 :
			case 35 :
			case 36 :
			case 37 :
			case 42 :
			case 43 :
			case 44 :
			case 45 :
			case 89 :
			case 90 :
			case 91 :
			case 133 :
			case 135 :
			case 140 :
			case 141 :
			case 168 :
			case 187 :
			case 197 :
			case 201 :
				return 1;
			case 9 :
			case 10 :
			case 14 :
			case 15 :
			case 20 :
			case 22 :
			case 24 :
			case 30 :
			case 31 :
			case 32 :
			case 33 :
			case 38 :
			case 39 :
			case 40 :
			case 41 :
			case 92 :
			case 93 :
			case 94 :
				return 2;
			case 46 :
			case 48 :
			case 50 :
			case 51 :
			case 52 :
			case 53 :
			case 54 :
			case 56 :
			case 58 :
			case 59 :
			case 60 :
			case 61 :
			case 62 :
			case 67 :
			case 68 :
			case 69 :
			case 70 :
			case 75 :
			case 76 :
			case 77 :
			case 78 :
			case 87 :
			case 96 :
			case 98 :
			case 100 :
			case 102 :
			case 104 :
			case 106 :
			case 108 :
			case 110 :
			case 112 :
			case 114 :
			case 120 :
			case 121 :
			case 122 :
			case 123 :
			case 124 :
			case 125 :
			case 126 :
			case 128 :
			case 130 :
			case 136 :
			case 137 :
			case 142 :
			case 144 :
			case 149 :
			case 150 :
			case 153 :
			case 154 :
			case 155 :
			case 156 :
			case 157 :
			case 158 :
			case 170 :
			case 171 :
			case 172 :
			case 174 :
			case 176 :
			case 180 :
			case 181 :
			case 182 :
			case 183 :
			case 185 :
			case 191 :
			case 194 :
			case 195 :
			case 198 :
			case 199 :
				return -1;
			case 55 :
			case 57 :
			case 63 :
			case 64 :
			case 65 :
			case 66 :
			case 71 :
			case 72 :
			case 73 :
			case 74 :
			case 88 :
			case 97 :
			case 99 :
			case 101 :
			case 103 :
			case 105 :
			case 107 :
			case 109 :
			case 111 :
			case 113 :
			case 115 :
			case 127 :
			case 129 :
			case 131 :
			case 159 :
			case 160 :
			case 161 :
			case 162 :
			case 163 :
			case 164 :
			case 165 :
			case 166 :
			case 173 :
			case 175 :
				return -2;
			case 79 :
			case 81 :
			case 83 :
			case 84 :
			case 85 :
			case 86 :
			case 148 :
			case 151 :
			case 152 :
				return -3;
			case 80 :
			case 82 :
				return -4;
			case 186 :
			case 203 :
			case 204 :
			case 205 :
			case 206 :
			case 207 :
			case 208 :
			case 209 :
			case 210 :
			case 211 :
			case 212 :
			case 213 :
			case 214 :
			case 215 :
			case 216 :
			case 217 :
			case 218 :
			case 219 :
			case 220 :
			case 221 :
			case 222 :
			case 223 :
			case 224 :
			case 225 :
			case 226 :
			case 227 :
			case 228 :
			case 229 :
			case 230 :
			case 231 :
			case 232 :
			case 233 :
			case 234 :
			case 235 :
			case 236 :
			case 237 :
			case 238 :
			case 239 :
			case 240 :
			case 241 :
			case 242 :
			case 243 :
			case 244 :
			case 245 :
			case 246 :
			case 247 :
			case 248 :
			case 249 :
			case 250 :
			case 251 :
			case 252 :
			case 253 :
			default :
				throw new IllegalArgumentException("Bad opcode: " + opcode);
		}
	}

	private static String bytecodeStr(int code) {
		return "";
	}

	final char[] getCharBuffer(int minimalSize) {
		if (minimalSize > this.tmpCharBuffer.length) {
			int newSize = this.tmpCharBuffer.length * 2;
			if (minimalSize > newSize) {
				newSize = minimalSize;
			}

			this.tmpCharBuffer = new char[newSize];
		}

		return this.tmpCharBuffer;
	}

	public static class ClassFileFormatException extends RuntimeException {
		private static final long serialVersionUID = 1263998431033790599L;

		ClassFileFormatException(String message) {
			super(message);
		}
	}
}