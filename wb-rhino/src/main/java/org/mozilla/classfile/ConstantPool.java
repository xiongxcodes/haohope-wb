/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.classfile;

import org.mozilla.classfile.ClassFileWriter;
import org.mozilla.classfile.FieldOrMethodRef;
import org.mozilla.javascript.ObjToIntMap;
import org.mozilla.javascript.UintMap;

final class ConstantPool {
	private static final int ConstantPoolSize = 256;
	private static final byte CONSTANT_Class = 7;
	private static final byte CONSTANT_Fieldref = 9;
	private static final byte CONSTANT_Methodref = 10;
	private static final byte CONSTANT_InterfaceMethodref = 11;
	private static final byte CONSTANT_String = 8;
	private static final byte CONSTANT_Integer = 3;
	private static final byte CONSTANT_Float = 4;
	private static final byte CONSTANT_Long = 5;
	private static final byte CONSTANT_Double = 6;
	private static final byte CONSTANT_NameAndType = 12;
	private static final byte CONSTANT_Utf8 = 1;
	private ClassFileWriter cfw;
	private static final int MAX_UTF_ENCODING_SIZE = 65535;
	private UintMap itsStringConstHash = new UintMap();
	private ObjToIntMap itsUtf8Hash = new ObjToIntMap();
	private ObjToIntMap itsFieldRefHash = new ObjToIntMap();
	private ObjToIntMap itsMethodRefHash = new ObjToIntMap();
	private ObjToIntMap itsClassHash = new ObjToIntMap();
	private int itsTop;
	private int itsTopIndex;
	private byte[] itsPool;

	ConstantPool(ClassFileWriter cfw) {
		this.cfw = cfw;
		this.itsTopIndex = 1;
		this.itsPool = new byte[256];
		this.itsTop = 0;
	}

	int write(byte[] data, int offset) {
		offset = ClassFileWriter.putInt16((short) this.itsTopIndex, data,
				offset);
		System.arraycopy(this.itsPool, 0, data, offset, this.itsTop);
		offset += this.itsTop;
		return offset;
	}

	int getWriteSize() {
		return 2 + this.itsTop;
	}

	int addConstant(int k) {
		this.ensure(5);
		this.itsPool[this.itsTop++] = 3;
		this.itsTop = ClassFileWriter.putInt32(k, this.itsPool, this.itsTop);
		return (short) (this.itsTopIndex++);
	}

	int addConstant(long k) {
		this.ensure(9);
		this.itsPool[this.itsTop++] = 5;
		this.itsTop = ClassFileWriter.putInt64(k, this.itsPool, this.itsTop);
		int index = this.itsTopIndex;
		this.itsTopIndex += 2;
		return index;
	}

	int addConstant(float k) {
		this.ensure(5);
		this.itsPool[this.itsTop++] = 4;
		int bits = Float.floatToIntBits(k);
		this.itsTop = ClassFileWriter.putInt32(bits, this.itsPool, this.itsTop);
		return this.itsTopIndex++;
	}

	int addConstant(double k) {
		this.ensure(9);
		this.itsPool[this.itsTop++] = 6;
		long bits = Double.doubleToLongBits(k);
		this.itsTop = ClassFileWriter.putInt64(bits, this.itsPool, this.itsTop);
		int index = this.itsTopIndex;
		this.itsTopIndex += 2;
		return index;
	}

	int addConstant(String k) {
		int utf8Index = '￿' & this.addUtf8(k);
		int theIndex = this.itsStringConstHash.getInt(utf8Index, -1);
		if (theIndex == -1) {
			theIndex = this.itsTopIndex++;
			this.ensure(3);
			this.itsPool[this.itsTop++] = 8;
			this.itsTop = ClassFileWriter.putInt16(utf8Index, this.itsPool,
					this.itsTop);
			this.itsStringConstHash.put(utf8Index, theIndex);
		}

		return theIndex;
	}

	boolean isUnderUtfEncodingLimit(String s) {
		int strLen = s.length();
		return strLen * 3 <= '￿' ? true : (strLen > '￿'
				? false
				: strLen == this.getUtfEncodingLimit(s, 0, strLen));
	}

	int getUtfEncodingLimit(String s, int start, int end) {
		if ((end - start) * 3 <= '￿') {
			return end;
		} else {
			int limit = '￿';

			for (int i = start; i != end; ++i) {
				char c = s.charAt(i);
				if (0 != c && c <= 127) {
					--limit;
				} else if (c < 2047) {
					limit -= 2;
				} else {
					limit -= 3;
				}

				if (limit < 0) {
					return i;
				}
			}

			return end;
		}
	}

	short addUtf8(String k) {
		int theIndex = this.itsUtf8Hash.get(k, -1);
		if (theIndex == -1) {
			int strLen = k.length();
			boolean tooBigString;
			if (strLen > '￿') {
				tooBigString = true;
			} else {
				tooBigString = false;
				this.ensure(3 + strLen * 3);
				int top = this.itsTop;
				this.itsPool[top++] = 1;
				top += 2;
				char[] chars = this.cfw.getCharBuffer(strLen);
				k.getChars(0, strLen, chars, 0);
				int utfLen = 0;

				while (true) {
					if (utfLen == strLen) {
						utfLen = top - (this.itsTop + 1 + 2);
						if (utfLen > '￿') {
							tooBigString = true;
						} else {
							this.itsPool[this.itsTop + 1] = (byte) (utfLen >>> 8);
							this.itsPool[this.itsTop + 2] = (byte) utfLen;
							this.itsTop = top;
							theIndex = this.itsTopIndex++;
							this.itsUtf8Hash.put(k, theIndex);
						}
						break;
					}

					char c = chars[utfLen];
					if (c != 0 && c <= 127) {
						this.itsPool[top++] = (byte) c;
					} else if (c > 2047) {
						this.itsPool[top++] = (byte) (224 | c >> 12);
						this.itsPool[top++] = (byte) (128 | c >> 6 & 63);
						this.itsPool[top++] = (byte) (128 | c & 63);
					} else {
						this.itsPool[top++] = (byte) (192 | c >> 6);
						this.itsPool[top++] = (byte) (128 | c & 63);
					}

					++utfLen;
				}
			}

			if (tooBigString) {
				throw new IllegalArgumentException("Too big string");
			}
		}

		return (short) theIndex;
	}

	private short addNameAndType(String name, String type) {
		short nameIndex = this.addUtf8(name);
		short typeIndex = this.addUtf8(type);
		this.ensure(5);
		this.itsPool[this.itsTop++] = 12;
		this.itsTop = ClassFileWriter.putInt16(nameIndex, this.itsPool,
				this.itsTop);
		this.itsTop = ClassFileWriter.putInt16(typeIndex, this.itsPool,
				this.itsTop);
		return (short) (this.itsTopIndex++);
	}

	short addClass(String className) {
		int theIndex = this.itsClassHash.get(className, -1);
		if (theIndex == -1) {
			String slashed = className;
			if (className.indexOf(46) > 0) {
				slashed = ClassFileWriter.getSlashedForm(className);
				theIndex = this.itsClassHash.get(slashed, -1);
				if (theIndex != -1) {
					this.itsClassHash.put(className, theIndex);
				}
			}

			if (theIndex == -1) {
				short utf8Index = this.addUtf8(slashed);
				this.ensure(3);
				this.itsPool[this.itsTop++] = 7;
				this.itsTop = ClassFileWriter.putInt16(utf8Index, this.itsPool,
						this.itsTop);
				theIndex = this.itsTopIndex++;
				this.itsClassHash.put(slashed, theIndex);
				if (className != slashed) {
					this.itsClassHash.put(className, theIndex);
				}
			}
		}

		return (short) theIndex;
	}

	short addFieldRef(String className, String fieldName, String fieldType) {
		FieldOrMethodRef ref = new FieldOrMethodRef(className, fieldName,
				fieldType);
		int theIndex = this.itsFieldRefHash.get(ref, -1);
		if (theIndex == -1) {
			short ntIndex = this.addNameAndType(fieldName, fieldType);
			short classIndex = this.addClass(className);
			this.ensure(5);
			this.itsPool[this.itsTop++] = 9;
			this.itsTop = ClassFileWriter.putInt16(classIndex, this.itsPool,
					this.itsTop);
			this.itsTop = ClassFileWriter.putInt16(ntIndex, this.itsPool,
					this.itsTop);
			theIndex = this.itsTopIndex++;
			this.itsFieldRefHash.put(ref, theIndex);
		}

		return (short) theIndex;
	}

	short addMethodRef(String className, String methodName, String methodType) {
		FieldOrMethodRef ref = new FieldOrMethodRef(className, methodName,
				methodType);
		int theIndex = this.itsMethodRefHash.get(ref, -1);
		if (theIndex == -1) {
			short ntIndex = this.addNameAndType(methodName, methodType);
			short classIndex = this.addClass(className);
			this.ensure(5);
			this.itsPool[this.itsTop++] = 10;
			this.itsTop = ClassFileWriter.putInt16(classIndex, this.itsPool,
					this.itsTop);
			this.itsTop = ClassFileWriter.putInt16(ntIndex, this.itsPool,
					this.itsTop);
			theIndex = this.itsTopIndex++;
			this.itsMethodRefHash.put(ref, theIndex);
		}

		return (short) theIndex;
	}

	short addInterfaceMethodRef(String className, String methodName,
			String methodType) {
		short ntIndex = this.addNameAndType(methodName, methodType);
		short classIndex = this.addClass(className);
		this.ensure(5);
		this.itsPool[this.itsTop++] = 11;
		this.itsTop = ClassFileWriter.putInt16(classIndex, this.itsPool,
				this.itsTop);
		this.itsTop = ClassFileWriter.putInt16(ntIndex, this.itsPool,
				this.itsTop);
		return (short) (this.itsTopIndex++);
	}

	void ensure(int howMuch) {
		if (this.itsTop + howMuch > this.itsPool.length) {
			int newCapacity = this.itsPool.length * 2;
			if (this.itsTop + howMuch > newCapacity) {
				newCapacity = this.itsTop + howMuch;
			}

			byte[] tmp = new byte[newCapacity];
			System.arraycopy(this.itsPool, 0, tmp, 0, this.itsTop);
			this.itsPool = tmp;
		}

	}
}