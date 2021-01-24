/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.tools.idswitch;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.tools.ToolErrorReporter;
import org.mozilla.javascript.tools.idswitch.CodePrinter;
import org.mozilla.javascript.tools.idswitch.FileBody;
import org.mozilla.javascript.tools.idswitch.IdValuePair;
import org.mozilla.javascript.tools.idswitch.SwitchGenerator;

public class Main {
	private static final String SWITCH_TAG_STR = "string_id_map";
	private static final String GENERATED_TAG_STR = "generated";
	private static final String STRING_TAG_STR = "string";
	private static final int NORMAL_LINE = 0;
	private static final int SWITCH_TAG = 1;
	private static final int GENERATED_TAG = 2;
	private static final int STRING_TAG = 3;
	private final List<IdValuePair> all_pairs = new ArrayList();
	private ToolErrorReporter R;
	private CodePrinter P;
	private FileBody body;
	private String source_file;
	private int tag_definition_end;
	private int tag_value_start;
	private int tag_value_end;

	private static boolean is_value_type(int id) {
		return id == 3;
	}

	private static String tag_name(int id) {
		switch (id) {
			case -2 :
				return "/generated";
			case -1 :
				return "/string_id_map";
			case 0 :
			default :
				return "";
			case 1 :
				return "string_id_map";
			case 2 :
				return "generated";
		}
	}

	void process_file(String file_path) throws IOException {
		this.source_file = file_path;
		this.body = new FileBody();
		Object is;
		if (file_path.equals("-")) {
			is = System.in;
		} else {
			is = new FileInputStream(file_path);
		}

		try {
			InputStreamReader os = new InputStreamReader((InputStream) is,
					"ASCII");
			this.body.readData(os);
		} finally {
			((InputStream) is).close();
		}

		this.process_file();
		if (this.body.wasModified()) {
			Object os1;
			if (file_path.equals("-")) {
				os1 = System.out;
			} else {
				os1 = new FileOutputStream(file_path);
			}

			try {
				OutputStreamWriter w = new OutputStreamWriter(
						(OutputStream) os1);
				this.body.writeData(w);
				w.flush();
			} finally {
				((OutputStream) os1).close();
			}
		}

	}

	private void process_file() {
		byte cur_state = 0;
		char[] buffer = this.body.getBuffer();
		int generated_begin = -1;
		int generated_end = -1;
		int time_stamp_begin = -1;
		int time_stamp_end = -1;
		this.body.startLineLoop();

		int tag_id;
		boolean bad_tag;
		String text1;
		do {
			if (!this.body.nextLine()) {
				if (cur_state != 0) {
					String text2 = ToolErrorReporter.getMessage(
							"msg.idswitch.file_end_in_switch",
							tag_name(cur_state));
					throw this.R.runtimeError(text2, this.source_file,
							this.body.getLineNumber(), (String) null, 0);
				}

				return;
			}

			int text = this.body.getLineBegin();
			int end = this.body.getLineEnd();
			tag_id = this.extract_line_tag_id(buffer, text, end);
			bad_tag = false;
			switch (cur_state) {
				case 0 :
					if (tag_id == 1) {
						cur_state = 1;
						this.all_pairs.clear();
						generated_begin = -1;
					} else if (tag_id == -1) {
						bad_tag = true;
					}
					break;
				case 1 :
					if (tag_id == 0) {
						this.look_for_id_definitions(buffer, text, end, false);
					} else if (tag_id == 3) {
						this.look_for_id_definitions(buffer, text, end, true);
					} else if (tag_id == 2) {
						if (generated_begin >= 0) {
							bad_tag = true;
						} else {
							cur_state = 2;
							time_stamp_begin = this.tag_definition_end;
							time_stamp_end = end;
						}
					} else if (tag_id == -1) {
						cur_state = 0;
						if (generated_begin >= 0 && !this.all_pairs.isEmpty()) {
							this.generate_java_code();
							text1 = this.P.toString();
							boolean different = this.body.setReplacement(
									generated_begin, generated_end, text1);
							if (different) {
								String stamp = this.get_time_stamp();
								this.body.setReplacement(time_stamp_begin,
										time_stamp_end, stamp);
							}
						}
					} else {
						bad_tag = true;
					}
					break;
				case 2 :
					if (tag_id == 0) {
						if (generated_begin < 0) {
							generated_begin = text;
						}
					} else if (tag_id == -2) {
						if (generated_begin < 0) {
							generated_begin = text;
						}

						cur_state = 1;
						generated_end = text;
					} else {
						bad_tag = true;
					}
			}
		} while (!bad_tag);

		text1 = ToolErrorReporter.getMessage("msg.idswitch.bad_tag_order",
				tag_name(tag_id));
		throw this.R.runtimeError(text1, this.source_file,
				this.body.getLineNumber(), (String) null, 0);
	}

	private String get_time_stamp() {
		SimpleDateFormat f = new SimpleDateFormat(
				" \'Last update:\' yyyy-MM-dd HH:mm:ss z");
		return f.format(new Date());
	}

	private void generate_java_code() {
		this.P.clear();
		IdValuePair[] pairs = new IdValuePair[this.all_pairs.size()];
		this.all_pairs.toArray(pairs);
		SwitchGenerator g = new SwitchGenerator();
		g.char_tail_test_threshold = 2;
		g.setReporter(this.R);
		g.setCodePrinter(this.P);
		g.generateSwitch(pairs, "0");
	}

	private int extract_line_tag_id(char[] array, int cursor, int end) {
		int id = 0;
		cursor = skip_white_space(array, cursor, end);
		int after_leading_white_space = cursor;
		cursor = this.look_for_slash_slash(array, cursor, end);
		if (cursor != end) {
			boolean at_line_start = after_leading_white_space + 2 == cursor;
			cursor = skip_white_space(array, cursor, end);
			if (cursor != end && array[cursor] == 35) {
				++cursor;
				boolean end_tag = false;
				if (cursor != end && array[cursor] == 47) {
					++cursor;
					end_tag = true;
				}

				int tag_start;
				for (tag_start = cursor; cursor != end; ++cursor) {
					char tag_end = array[cursor];
					if (tag_end == 35 || tag_end == 61
							|| is_white_space(tag_end)) {
						break;
					}
				}

				if (cursor != end) {
					int arg12 = cursor;
					cursor = skip_white_space(array, cursor, end);
					if (cursor != end) {
						char c = array[cursor];
						if (c == 61 || c == 35) {
							id = this.get_tag_id(array, tag_start, arg12,
									at_line_start);
							if (id != 0) {
								String bad = null;
								if (c == 35) {
									if (end_tag) {
										id = -id;
										if (is_value_type(id)) {
											bad = "msg.idswitch.no_end_usage";
										}
									}

									this.tag_definition_end = cursor + 1;
								} else {
									if (end_tag) {
										bad = "msg.idswitch.no_end_with_value";
									} else if (!is_value_type(id)) {
										bad = "msg.idswitch.no_value_allowed";
									}

									id = this.extract_tag_value(array,
											cursor + 1, end, id);
								}

								if (bad != null) {
									String s = ToolErrorReporter.getMessage(
											bad, tag_name(id));
									throw this.R.runtimeError(s,
											this.source_file,
											this.body.getLineNumber(),
											(String) null, 0);
								}
							}
						}
					}
				}
			}
		}

		return id;
	}

	private int look_for_slash_slash(char[] array, int cursor, int end) {
		while (true) {
			if (cursor + 2 <= end) {
				char c = array[cursor++];
				if (c != 47) {
					continue;
				}

				c = array[cursor++];
				if (c != 47) {
					continue;
				}

				return cursor;
			}

			return end;
		}
	}

	private int extract_tag_value(char[] array, int cursor, int end, int id) {
		boolean found = false;
		cursor = skip_white_space(array, cursor, end);
		if (cursor != end) {
			int value_start = cursor;
			int value_end = cursor;

			while (cursor != end) {
				char c = array[cursor];
				if (is_white_space(c)) {
					int after_space = skip_white_space(array, cursor + 1, end);
					if (after_space != end && array[after_space] == 35) {
						value_end = cursor;
						cursor = after_space;
						break;
					}

					cursor = after_space + 1;
				} else {
					if (c == 35) {
						value_end = cursor;
						break;
					}

					++cursor;
				}
			}

			if (cursor != end) {
				found = true;
				this.tag_value_start = value_start;
				this.tag_value_end = value_end;
				this.tag_definition_end = cursor + 1;
			}
		}

		return found ? id : 0;
	}

	private int get_tag_id(char[] array, int begin, int end,
			boolean at_line_start) {
		if (at_line_start) {
			if (equals("string_id_map", array, begin, end)) {
				return 1;
			}

			if (equals("generated", array, begin, end)) {
				return 2;
			}
		}

		return equals("string", array, begin, end) ? 3 : 0;
	}

	private void look_for_id_definitions(char[] array, int begin, int end,
			boolean use_tag_value_as_string) {
		int cursor = skip_white_space(array, begin, end);
		int id_start = cursor;
		int name_start = skip_matched_prefix("Id_", array, cursor, end);
		if (name_start >= 0) {
			cursor = skip_name_char(array, name_start, end);
			int name_end = cursor;
			if (name_start != cursor) {
				cursor = skip_white_space(array, cursor, end);
				if (cursor != end && array[cursor] == 61) {
					if (use_tag_value_as_string) {
						name_start = this.tag_value_start;
						name_end = this.tag_value_end;
					}

					this.add_id(array, id_start, name_end, name_start, name_end);
				}
			}
		}

	}

	private void add_id(char[] array, int id_start, int id_end, int name_start,
			int name_end) {
		String name = new String(array, name_start, name_end - name_start);
		String value = new String(array, id_start, id_end - id_start);
		IdValuePair pair = new IdValuePair(name, value);
		pair.setLineNumber(this.body.getLineNumber());
		this.all_pairs.add(pair);
	}

	private static boolean is_white_space(int c) {
		return c == 32 || c == 9;
	}

	private static int skip_white_space(char[] array, int begin, int end) {
		int cursor;
		for (cursor = begin; cursor != end; ++cursor) {
			char c = array[cursor];
			if (!is_white_space(c)) {
				break;
			}
		}

		return cursor;
	}

	private static int skip_matched_prefix(String prefix, char[] array,
			int begin, int end) {
		int cursor = -1;
		int prefix_length = prefix.length();
		if (prefix_length <= end - begin) {
			cursor = begin;

			for (int i = 0; i != prefix_length; ++cursor) {
				if (prefix.charAt(i) != array[cursor]) {
					cursor = -1;
					break;
				}

				++i;
			}
		}

		return cursor;
	}

	private static boolean equals(String str, char[] array, int begin, int end) {
		if (str.length() == end - begin) {
			int i = begin;

			for (int j = 0; i != end; ++j) {
				if (array[i] != str.charAt(j)) {
					return false;
				}

				++i;
			}

			return true;
		} else {
			return false;
		}
	}

	private static int skip_name_char(char[] array, int begin, int end) {
		int cursor;
		for (cursor = begin; cursor != end; ++cursor) {
			char c = array[cursor];
			if ((97 > c || c > 122) && (65 > c || c > 90) && (48 > c || c > 57)
					&& c != 95) {
				break;
			}
		}

		return cursor;
	}

	public static void main(String[] args) {
		Main self = new Main();
		int status = self.exec(args);
		System.exit(status);
	}

	private int exec(String[] args) {
		this.R = new ToolErrorReporter(true, System.err);
		int arg_count = this.process_options(args);
		if (arg_count == 0) {
			this.option_error(ToolErrorReporter
					.getMessage("msg.idswitch.no_file_argument"));
			return -1;
		} else if (arg_count > 1) {
			this.option_error(ToolErrorReporter
					.getMessage("msg.idswitch.too_many_arguments"));
			return -1;
		} else {
			this.P = new CodePrinter();
			this.P.setIndentStep(4);
			this.P.setIndentTabSize(0);

			try {
				this.process_file(args[0]);
				return 0;
			} catch (IOException arg3) {
				this.print_error(ToolErrorReporter.getMessage(
						"msg.idswitch.io_error", arg3.toString()));
				return -1;
			} catch (EvaluatorException arg4) {
				return -1;
			}
		}
	}

	private int process_options(String[] args) {
		byte status = 1;
		boolean show_usage = false;
		boolean show_version = false;
		int N = args.length;

		label59 : for (int i = 0; i != N; ++i) {
			String arg = args[i];
			int arg_length = arg.length();
			if (arg_length >= 2 && arg.charAt(0) == 45) {
				if (arg.charAt(1) == 45) {
					if (arg_length == 2) {
						args[i] = null;
						break;
					}

					if (arg.equals("--help")) {
						show_usage = true;
					} else {
						if (!arg.equals("--version")) {
							this.option_error(ToolErrorReporter.getMessage(
									"msg.idswitch.bad_option", arg));
							status = -1;
							break;
						}

						show_version = true;
					}
				} else {
					int j = 1;

					while (j != arg_length) {
						char c = arg.charAt(j);
						switch (c) {
							case 'h' :
								show_usage = true;
								++j;
								break;
							default :
								this.option_error(ToolErrorReporter.getMessage(
										"msg.idswitch.bad_option_char",
										String.valueOf(c)));
								status = -1;
								break label59;
						}
					}
				}

				args[i] = null;
			}
		}

		if (status == 1) {
			if (show_usage) {
				this.show_usage();
				status = 0;
			}

			if (show_version) {
				this.show_version();
				status = 0;
			}
		}

		if (status != 1) {
			System.exit(status);
		}

		return this.remove_nulls(args);
	}

	private void show_usage() {
		System.out.println(ToolErrorReporter.getMessage("msg.idswitch.usage"));
		System.out.println();
	}

	private void show_version() {
		System.out
				.println(ToolErrorReporter.getMessage("msg.idswitch.version"));
	}

	private void option_error(String str) {
		this.print_error(ToolErrorReporter.getMessage(
				"msg.idswitch.bad_invocation", str));
	}

	private void print_error(String text) {
		System.err.println(text);
	}

	private int remove_nulls(String[] array) {
		int N = array.length;

		int cursor;
		for (cursor = 0; cursor != N && array[cursor] != null; ++cursor) {
			;
		}

		int destination = cursor;
		if (cursor != N) {
			++cursor;

			for (; cursor != N; ++cursor) {
				String elem = array[cursor];
				if (elem != null) {
					array[destination] = elem;
					++destination;
				}
			}
		}

		return destination;
	}
}