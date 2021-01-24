/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.classfile;

import org.mozilla.classfile.ClassFileWriter;

final class ClassFileField {
	private short itsNameIndex;
	private short itsTypeIndex;
	private short itsFlags;
	private boolean itsHasAttributes;
	private short itsAttr1;
	private short itsAttr2;
	private short itsAttr3;
	private int itsIndex;

	ClassFileField(short nameIndex, short typeIndex, int i) {
		this.itsNameIndex = nameIndex;
		this.itsTypeIndex = typeIndex;
		this.itsFlags = (short) i;
		this.itsHasAttributes = false;
	}

	void setAttributes(short attr1, int i, int j, int index) {
		this.itsHasAttributes = true;
		this.itsAttr1 = attr1;
		this.itsAttr2 = (short) i;
		this.itsAttr3 = (short) j;
		this.itsIndex = index;
	}

	int write(byte[] data, int offset) {
		offset = ClassFileWriter.putInt16(this.itsFlags, data, offset);
		offset = ClassFileWriter.putInt16(this.itsNameIndex, data, offset);
		offset = ClassFileWriter.putInt16(this.itsTypeIndex, data, offset);
		if (!this.itsHasAttributes) {
			offset = ClassFileWriter.putInt16(0, data, offset);
		} else {
			offset = ClassFileWriter.putInt16(1, data, offset);
			offset = ClassFileWriter.putInt16(this.itsAttr1, data, offset);
			offset = ClassFileWriter.putInt16(this.itsAttr2, data, offset);
			offset = ClassFileWriter.putInt16(this.itsAttr3, data, offset);
			offset = ClassFileWriter.putInt16(this.itsIndex, data, offset);
		}

		return offset;
	}

	int getWriteSize() {
		byte size = 6;
		int size1;
		if (!this.itsHasAttributes) {
			size1 = size + 2;
		} else {
			size1 = size + 10;
		}

		return size1;
	}
}