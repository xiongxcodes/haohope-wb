/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.regexp;

class SubString {
	static final SubString emptySubString = new SubString();
	char[] charArray;
	int index;
	int length;

	public SubString() {
	}

	public SubString(String str) {
		this.index = 0;
		this.charArray = str.toCharArray();
		this.length = str.length();
	}

	public SubString(char[] source, int start, int len) {
		this.index = 0;
		this.length = len;
		this.charArray = new char[len];

		for (int j = 0; j < len; ++j) {
			this.charArray[j] = source[start + j];
		}

	}

	public String toString() {
		return this.charArray == null ? "" : new String(this.charArray,
				this.index, this.length);
	}
}