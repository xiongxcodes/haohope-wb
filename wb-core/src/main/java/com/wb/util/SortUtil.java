package com.wb.util;

import java.io.File;
import java.text.CollationKey;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class SortUtil {
	public static File[] sort(File[] files) {
		sort(files, 0, false);
		return files;
	}

	public static File[] sort(File[] files, final int type, final boolean desc) {
		final Collator collator = Collator.getInstance();
		Arrays.sort(files, new Comparator<File>() {
			public int compare(File f1, File f2) {
				switch (type) {
					case 1 :
						Long l1 = Long.valueOf(f1.isDirectory() ? -1L : f1
								.length());
						Long l2 = Long.valueOf(f2.isDirectory() ? -1L : f2
								.length());
						if (desc) {
							return l2.compareTo(l1);
						}

						return l1.compareTo(l2);
					case 2 :
						CollationKey t1 = collator.getCollationKey(f1
								.isDirectory() ? "0" : "1"
								+ FileUtil.getFileType(f1).toLowerCase());
						CollationKey t2 = collator.getCollationKey(f2
								.isDirectory() ? "0" : "1"
								+ FileUtil.getFileType(f2).toLowerCase());
						if (desc) {
							return t2.compareTo(t1);
						}

						return t1.compareTo(t2);
					case 3 :
						Long d1 = Long.valueOf(f1.lastModified());
						Long d2 = Long.valueOf(f2.lastModified());
						boolean b1 = f1.isDirectory();
						boolean b2 = f2.isDirectory();
						if (b1 && !b2) {
							d1 = Long.valueOf(Long.MIN_VALUE);
						}

						if (b2 && !b1) {
							d2 = Long.valueOf(Long.MIN_VALUE);
						}

						if (desc) {
							return d2.compareTo(d1);
						}

						return d1.compareTo(d2);
					default :
						String file1 = f1.getName();
						String file2 = f2.getName();
						CollationKey k1 = collator.getCollationKey((f1
								.isDirectory() ? 0 : 1)
								+ FileUtil.removeExtension(file1).toLowerCase());
						CollationKey k2 = collator.getCollationKey((f2
								.isDirectory() ? 0 : 1)
								+ FileUtil.removeExtension(file2).toLowerCase());
						int result;
						if (desc) {
							result = k2.compareTo(k1);
						} else {
							result = k1.compareTo(k2);
						}

						if (result == 0) {
							CollationKey ke1 = collator.getCollationKey((f1
									.isDirectory() ? 0 : 1)
									+ FileUtil.getFileExt(file1).toLowerCase());
							CollationKey ke2 = collator.getCollationKey((f2
									.isDirectory() ? 0 : 1)
									+ FileUtil.getFileExt(file2).toLowerCase());
							if (desc) {
								result = ke2.compareTo(ke1);
							} else {
								result = ke1.compareTo(ke2);
							}
						}

						return result;
				}
			}
		});
		return files;
	}

	public static String[] sort(String[] list) {
		Arrays.sort(list, new Comparator<String>() {
			Collator collator = Collator.getInstance();

			public int compare(String s1, String s2) {
				CollationKey key1 = this.collator.getCollationKey(StringUtil
						.opt(s1).toLowerCase());
				CollationKey key2 = this.collator.getCollationKey(StringUtil
						.opt(s2).toLowerCase());
				return key1.compareTo(key2);
			}
		});
		return list;
	}

	public static <K, V> ArrayList<Entry<K, V>> sortKey(Map<K, V> map) {
		return sortKey(map, false);
	}

	public static <K, V> ArrayList<Entry<K, V>> sortKey(Map<K, V> map,
			final boolean keyAsNumber) {
		ArrayList list = new ArrayList(map.entrySet());
		final Collator collator = Collator.getInstance();
		Collections.sort(list, new Comparator<Entry<K, V>>() {
			public int compare(Entry<K, V> e1, Entry<K, V> e2) {
				Object k1 = e1.getKey();
				Object k2 = e2.getKey();
				if (keyAsNumber) {
					return k1 instanceof Number && k2 instanceof Number
							? (int) Math.ceil(((Number) k1).doubleValue()
									- ((Number) k2).doubleValue())
							: (int) Math.ceil(Double.parseDouble(k1.toString())
									- Double.parseDouble(k2.toString()));
				} else {
					CollationKey key1 = collator.getCollationKey(k1.toString()
							.toLowerCase());
					CollationKey key2 = collator.getCollationKey(k2.toString()
							.toLowerCase());
					return key1.compareTo(key2);
				}
			}
		});
		return list;
	}

	public static <K, V> ArrayList<Entry<K, V>> sortValue(Map<K, V> map) {
		return sortValue(map, false);
	}

	public static <K, V> ArrayList<Entry<K, V>> sortValue(Map<K, V> map,
			final boolean keyAsNumber) {
		ArrayList list = new ArrayList(map.entrySet());
		final Collator collator = Collator.getInstance();
		Collections.sort(list, new Comparator<Entry<K, V>>() {
			public int compare(Entry<K, V> e1, Entry<K, V> e2) {
				Object v1 = e1.getValue();
				Object v2 = e2.getValue();
				if (keyAsNumber) {
					return v1 instanceof Number && v2 instanceof Number
							? (int) Math.ceil(((Number) v1).doubleValue()
									- ((Number) v2).doubleValue())
							: (int) Math.ceil(Double.parseDouble(v1.toString())
									- Double.parseDouble(v2.toString()));
				} else {
					CollationKey key1 = collator.getCollationKey(v1.toString()
							.toLowerCase());
					CollationKey key2 = collator.getCollationKey(v2.toString()
							.toLowerCase());
					return key1.compareTo(key2);
				}
			}
		});
		return list;
	}

	public static <T> List<T> sort(List<T> list) {
		Collections.sort(list, new Comparator<T>() {
			public int compare(T b1, T b2) {
				return b1.toString().toLowerCase()
						.compareTo(b2.toString().toLowerCase());
			}
		});
		return list;
	}
}