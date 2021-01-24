/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.optimizer;

class DataFlowBitSet {
	private int[] itsBits;
	private int itsSize;

	DataFlowBitSet(int size) {
		this.itsSize = size;
		this.itsBits = new int[size + 31 >> 5];
	}

	void set(int n) {
		if (0 > n || n >= this.itsSize) {
			this.badIndex(n);
		}

		this.itsBits[n >> 5] |= 1 << (n & 31);
	}

	boolean test(int n) {
		if (0 > n || n >= this.itsSize) {
			this.badIndex(n);
		}

		return (this.itsBits[n >> 5] & 1 << (n & 31)) != 0;
	}

	void not() {
		int bitsLength = this.itsBits.length;

		for (int i = 0; i < bitsLength; ++i) {
			this.itsBits[i] = ~this.itsBits[i];
		}

	}

	void clear(int n) {
		if (0 > n || n >= this.itsSize) {
			this.badIndex(n);
		}

		this.itsBits[n >> 5] &= ~(1 << (n & 31));
	}

	void clear() {
		int bitsLength = this.itsBits.length;

		for (int i = 0; i < bitsLength; ++i) {
			this.itsBits[i] = 0;
		}

	}

	void or(DataFlowBitSet b) {
		int bitsLength = this.itsBits.length;

		for (int i = 0; i < bitsLength; ++i) {
			this.itsBits[i] |= b.itsBits[i];
		}

	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("DataFlowBitSet, size = ");
		sb.append(this.itsSize);
		sb.append('\n');
		int bitsLength = this.itsBits.length;

		for (int i = 0; i < bitsLength; ++i) {
			sb.append(Integer.toHexString(this.itsBits[i]));
			sb.append(' ');
		}

		return sb.toString();
	}

	boolean df(DataFlowBitSet in, DataFlowBitSet gen, DataFlowBitSet notKill) {
		int bitsLength = this.itsBits.length;
		boolean changed = false;

		for (int i = 0; i < bitsLength; ++i) {
			int oldBits = this.itsBits[i];
			this.itsBits[i] = (in.itsBits[i] | gen.itsBits[i])
					& notKill.itsBits[i];
			changed |= oldBits != this.itsBits[i];
		}

		return changed;
	}

	boolean df2(DataFlowBitSet in, DataFlowBitSet gen, DataFlowBitSet notKill) {
		int bitsLength = this.itsBits.length;
		boolean changed = false;

		for (int i = 0; i < bitsLength; ++i) {
			int oldBits = this.itsBits[i];
			this.itsBits[i] = in.itsBits[i] & notKill.itsBits[i]
					| gen.itsBits[i];
			changed |= oldBits != this.itsBits[i];
		}

		return changed;
	}

	private void badIndex(int n) {
		throw new RuntimeException("DataFlowBitSet bad index " + n);
	}
}