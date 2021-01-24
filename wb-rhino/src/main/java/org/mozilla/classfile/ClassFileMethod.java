/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.classfile;

import org.mozilla.classfile.ClassFileWriter;

final class ClassFileMethod {
	private short itsNameIndex;
	private short itsTypeIndex;
	private short itsFlags;
	private byte[] itsCodeAttribute;

	ClassFileMethod(short nameIndex, short typeIndex, int i) {
		this.itsNameIndex = nameIndex;
		this.itsTypeIndex = typeIndex;
		this.itsFlags = (short) i;
	}

	void setCodeAttribute(byte[] codeAttribute) {
		this.itsCodeAttribute = codeAttribute;
	}

	int write(byte[] data, int offset) {
		offset = ClassFileWriter.putInt16(this.itsFlags, data, offset);
		offset = ClassFileWriter.putInt16(this.itsNameIndex, data, offset);
		offset = ClassFileWriter.putInt16(this.itsTypeIndex, data, offset);
		offset = ClassFileWriter.putInt16(1, data, offset);
		System.arraycopy(this.itsCodeAttribute, 0, data, offset,
				this.itsCodeAttribute.length);
		offset += this.itsCodeAttribute.length;
		return offset;
	}

	int getWriteSize() {
		return 8 + this.itsCodeAttribute.length;
	}
}