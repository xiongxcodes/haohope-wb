package com.wb.util;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtil {
	public static String format(Date date, String format) {
		if (date == null) {
			return "";
		} else {
			SimpleDateFormat dateFormat = new SimpleDateFormat(format);
			return dateFormat.format(date);
		}
	}

	public static String format(Date date) {
		return format(date, "yyyy-MM-dd HH:mm:ss");
	}

	public static String formatDate(Date date) {
		return format(date, "yyyy-MM-dd");
	}

	public static Timestamp getTimestamp(long time) {
		return new Timestamp(time);
	}

	public static Timestamp now() {
		return new Timestamp(System.currentTimeMillis());
	}

	public static int daysInMonth(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal.getActualMaximum(5);
	}

	public static int dayOfMonth(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal.get(5);
	}

	public static int yearOf(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal.get(1);
	}

	public static int dayOfYear(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal.get(6);
	}

	public static int dayOfWeek(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal.get(7);
	}

	public static String dateToStr(Date value) {
		if (value == null) {
			return null;
		} else {
			Timestamp t = new Timestamp(value.getTime());
			return t.toString();
		}
	}

	public static Timestamp strToDate(String value) {
		return StringUtil.isEmpty(value) ? null : Timestamp.valueOf(value);
	}

	public static boolean isDate(String dateStr) {
		int len = dateStr.length();
		if (len < 19) {
			return false;
		} else {
			for (int i = 0; i < len; ++i) {
				char ch = dateStr.charAt(i);
				switch (i) {
					case 4 :
					case 7 :
						if (ch != '-') {
							return false;
						}
						break;
					case 10 :
						if (ch != ' ') {
							return false;
						}
						break;
					case 13 :
					case 16 :
						if (ch != ':') {
							return false;
						}
						break;
					case 19 :
						if (ch != '.') {
							return false;
						}
						break;
					default :
						if (ch < '0' || ch > '9') {
							return false;
						}
				}
			}

			return true;
		}
	}

	public static Date incYear(Date date, int years) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(1, years);
		return cal.getTime();
	}

	public static Date incMonth(Date date, int months) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(2, months);
		return cal.getTime();
	}

	public static int hourOfDay(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal.get(11);
	}

	public static String format(long milliSecs) {
		long h = milliSecs / 3600000L;
		long hm = milliSecs % 3600000L;
		long m = hm / 60000L;
		long mm = hm % 60000L;
		long s = mm / 1000L;
		long sm = mm % 1000L;
		return StringUtil.concat(new String[]{Long.toString(h), ":", Long.toString(m), ":", Long.toString(s),
				Float.toString((float) sm / 1000.0F).substring(1)});
	}

	public static Date incDay(Date date, long days) {
		return new Date(date.getTime() + 86400000L * days);
	}

	public static Date incSecond(Date date, long seconds) {
		return new Date(date.getTime() + 1000L * seconds);
	}

	public static int getElapsedDays(Date beginDate, Date endDate) {
		return (int) ((endDate.getTime() - beginDate.getTime()) / 86400000L);
	}

	public static String fixTime(String str) {
		if (str.indexOf(58) == -1) {
			return "00:00:00";
		} else {
			int b = str.indexOf(32);
			int e = str.indexOf(46);
			if (b == -1) {
				b = 0;
			} else {
				++b;
			}

			if (e == -1) {
				e = str.length();
			}

			return str.substring(b, e);
		}
	}

	public static String fixTimestamp(String str, boolean dateOnly) {
		int pos = str.indexOf(32);
		String timePart = null;
		String datePart;
		if (pos == -1) {
			datePart = str;
			if (!dateOnly) {
				timePart = "00:00:00";
			}
		} else {
			datePart = str.substring(0, pos);
			if (!dateOnly) {
				timePart = str.substring(pos + 1);
			}
		}

		String[] sec = StringUtil.split(datePart, "-");
		if (sec.length == 3) {
			StringBuilder buf = new StringBuilder(dateOnly ? 10 : 30);
			buf.append(sec[0]);
			buf.append('-');
			if (sec[1].length() == 1) {
				buf.append('0');
			}

			buf.append(sec[1]);
			buf.append('-');
			if (sec[2].length() == 1) {
				buf.append('0');
			}

			buf.append(sec[2]);
			if (!dateOnly) {
				buf.append(' ');
				buf.append(timePart);
			}

			return buf.toString();
		} else {
			return str;
		}
	}
}