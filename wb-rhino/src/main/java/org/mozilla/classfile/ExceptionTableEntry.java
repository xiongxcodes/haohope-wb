/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.classfile;

final class ExceptionTableEntry {
	int itsStartLabel;
	int itsEndLabel;
	int itsHandlerLabel;
	short itsCatchType;

	ExceptionTableEntry(int startLabel, int endLabel, int handlerLabel,
			short catchType) {
		this.itsStartLabel = startLabel;
		this.itsEndLabel = endLabel;
		this.itsHandlerLabel = handlerLabel;
		this.itsCatchType = catchType;
	}
}