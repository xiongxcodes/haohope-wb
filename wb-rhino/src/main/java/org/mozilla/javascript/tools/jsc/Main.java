/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript.tools.jsc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.optimizer.ClassCompiler;
import org.mozilla.javascript.tools.SourceReader;
import org.mozilla.javascript.tools.ToolErrorReporter;

public class Main {
	private boolean printHelp;
	private ToolErrorReporter reporter = new ToolErrorReporter(true);
	private CompilerEnvirons compilerEnv = new CompilerEnvirons();
	private ClassCompiler compiler;
	private String targetName;
	private String targetPackage;
	private String destinationDir;
	private String characterEncoding;

	public static void main(String[] args) {
		Main main = new Main();
		args = main.processOptions(args);
		if (args == null) {
			if (main.printHelp) {
				System.out.println(ToolErrorReporter.getMessage(
						"msg.jsc.usage", Main.class.getName()));
				System.exit(0);
			}

			System.exit(1);
		}

		if (!main.reporter.hasReportedError()) {
			main.processSource(args);
		}

	}

	public Main() {
		this.compilerEnv.setErrorReporter(this.reporter);
		this.compiler = new ClassCompiler(this.compilerEnv);
	}

	public String[] processOptions(String[] args) {
		this.targetPackage = "";
		this.compilerEnv.setGenerateDebugInfo(false);
		int i = 0;

		while (i < args.length) {
			String arg = args[i];
			int arg13;
			int arg15;
			if (!arg.startsWith("-")) {
				arg13 = args.length - i;
				if (this.targetName != null && arg13 > 1) {
					this.addError("msg.multiple.js.to.file", this.targetName);
					return null;
				}

				String[] arg14 = new String[arg13];

				for (arg15 = 0; arg15 != arg13; ++arg15) {
					arg14[arg15] = args[i + arg15];
				}

				return arg14;
			}

			if (!arg.equals("-help") && !arg.equals("-h")
					&& !arg.equals("--help")) {
				label207 : {
					label206 : {
						label218 : {
							try {
								if (arg.equals("-version")) {
									++i;
									if (i < args.length) {
										arg13 = Integer.parseInt(args[i]);
										this.compilerEnv
												.setLanguageVersion(arg13);
										break label218;
									}
								}

								if (arg.equals("-opt") || arg.equals("-O")) {
									++i;
									if (i < args.length) {
										arg13 = Integer.parseInt(args[i]);
										this.compilerEnv
												.setOptimizationLevel(arg13);
										break label218;
									}
								}
							} catch (NumberFormatException arg10) {
								badUsage(args[i]);
								return null;
							}

							if (arg.equals("-nosource")) {
								this.compilerEnv.setGeneratingSource(false);
							} else if (!arg.equals("-debug")
									&& !arg.equals("-g")) {
								label220 : {
									if (arg.equals("-main-method-class")) {
										++i;
										if (i < args.length) {
											this.compiler
													.setMainMethodClass(args[i]);
											break label220;
										}
									}

									if (arg.equals("-encoding")) {
										++i;
										if (i < args.length) {
											this.characterEncoding = args[i];
											break label220;
										}
									}

									String targetImplements;
									int arg12;
									char arg17;
									if (arg.equals("-o")) {
										++i;
										if (i < args.length) {
											targetImplements = args[i];
											arg12 = targetImplements.length();
											if (arg12 != 0
													&& Character
															.isJavaIdentifierStart(targetImplements
																	.charAt(0))) {
												for (arg15 = 1; arg15 < arg12; ++arg15) {
													arg17 = targetImplements
															.charAt(arg15);
													if (!Character
															.isJavaIdentifierPart(arg17)) {
														if (arg17 == 46
																&& arg15 == arg12 - 6
																&& targetImplements
																		.endsWith(".class")) {
															targetImplements = targetImplements
																	.substring(
																			0,
																			arg15);
															break;
														}

														this.addError(
																"msg.invalid.classfile.name",
																targetImplements);
														break;
													}
												}

												this.targetName = targetImplements;
											} else {
												this.addError(
														"msg.invalid.classfile.name",
														targetImplements);
											}
											break label220;
										}
									}

									if (arg.equals("-observe-instruction-count")) {
										this.compilerEnv
												.setGenerateObserverCount(true);
									}

									if (arg.equals("-package")) {
										++i;
										if (i < args.length) {
											targetImplements = args[i];
											arg12 = targetImplements.length();
											arg15 = 0;

											while (true) {
												if (arg15 != arg12) {
													arg17 = targetImplements
															.charAt(arg15);
													if (!Character
															.isJavaIdentifierStart(arg17)) {
														break label207;
													}

													++arg15;

													while (arg15 != arg12) {
														arg17 = targetImplements
																.charAt(arg15);
														if (!Character
																.isJavaIdentifierPart(arg17)) {
															break;
														}

														++arg15;
													}

													if (arg15 != arg12) {
														if (arg17 != 46
																|| arg15 == arg12 - 1) {
															break label207;
														}

														++arg15;
														continue;
													}
												}

												this.targetPackage = targetImplements;
												break label220;
											}
										}
									}

									if (arg.equals("-extends")) {
										++i;
										if (i < args.length) {
											targetImplements = args[i];

											Class arg11;
											try {
												arg11 = Class
														.forName(targetImplements);
											} catch (ClassNotFoundException arg9) {
												throw new Error(arg9.toString());
											}

											this.compiler
													.setTargetExtends(arg11);
											break label220;
										}
									}

									if (arg.equals("-implements")) {
										++i;
										if (i < args.length) {
											targetImplements = args[i];
											StringTokenizer st = new StringTokenizer(
													targetImplements, ",");
											ArrayList list = new ArrayList();

											while (st.hasMoreTokens()) {
												String implementsClasses = st
														.nextToken();

												try {
													list.add(Class
															.forName(implementsClasses));
												} catch (ClassNotFoundException arg8) {
													throw new Error(
															arg8.toString());
												}
											}

											Class[] arg16 = (Class[]) list
													.toArray(new Class[list
															.size()]);
											this.compiler
													.setTargetImplements(arg16);
											break label220;
										}
									}

									if (!arg.equals("-d")) {
										break label206;
									}

									++i;
									if (i >= args.length) {
										break label206;
									}

									this.destinationDir = args[i];
								}
							} else {
								this.compilerEnv.setGenerateDebugInfo(true);
							}
						}

						++i;
						continue;
					}

					badUsage(arg);
					return null;
				}

				this.addError("msg.package.name", this.targetPackage);
				return null;
			}

			this.printHelp = true;
			return null;
		}

		p(ToolErrorReporter.getMessage("msg.no.file"));
		return null;
	}

	private static void badUsage(String s) {
		System.err.println(ToolErrorReporter.getMessage("msg.jsc.bad.usage",
				Main.class.getName(), s));
	}

	public void processSource(String[] filenames) {
		for (int i = 0; i != filenames.length; ++i) {
			String filename = filenames[i];
			if (!filename.endsWith(".js")) {
				this.addError("msg.extension.not.js", filename);
				return;
			}

			File f = new File(filename);
			String source = this.readSource(f);
			if (source == null) {
				return;
			}

			String mainClassName = this.targetName;
			if (mainClassName == null) {
				String compiled = f.getName();
				String targetTopDir = compiled.substring(0,
						compiled.length() - 3);
				mainClassName = this.getClassName(targetTopDir);
			}

			if (this.targetPackage.length() != 0) {
				mainClassName = this.targetPackage + "." + mainClassName;
			}

			Object[] arg18 = this.compiler.compileToClassFiles(source,
					filename, 1, mainClassName);
			if (arg18 == null || arg18.length == 0) {
				return;
			}

			File arg19 = null;
			if (this.destinationDir != null) {
				arg19 = new File(this.destinationDir);
			} else {
				String j = f.getParent();
				if (j != null) {
					arg19 = new File(j);
				}
			}

			for (int arg20 = 0; arg20 != arg18.length; arg20 += 2) {
				String className = (String) arg18[arg20];
				byte[] bytes = (byte[]) ((byte[]) arg18[arg20 + 1]);
				File outfile = this.getOutputFile(arg19, className);

				try {
					FileOutputStream ioe = new FileOutputStream(outfile);

					try {
						ioe.write(bytes);
					} finally {
						ioe.close();
					}
				} catch (IOException arg17) {
					this.addFormatedError(arg17.toString());
				}
			}
		}

	}

	private String readSource(File f) {
		String absPath = f.getAbsolutePath();
		if (!f.isFile()) {
			this.addError("msg.jsfile.not.found", absPath);
			return null;
		} else {
			try {
				return (String) SourceReader.readFileOrUrl(absPath, true,
						this.characterEncoding);
			} catch (FileNotFoundException arg3) {
				this.addError("msg.couldnt.open", absPath);
			} catch (IOException arg4) {
				this.addFormatedError(arg4.toString());
			}

			return null;
		}
	}

	private File getOutputFile(File parentDir, String className) {
		String path = className.replace('.', File.separatorChar);
		path = path.concat(".class");
		File f = new File(parentDir, path);
		String dirPath = f.getParent();
		if (dirPath != null) {
			File dir = new File(dirPath);
			if (!dir.exists()) {
				dir.mkdirs();
			}
		}

		return f;
	}

	String getClassName(String name) {
		char[] s = new char[name.length() + 1];
		int j = 0;
		if (!Character.isJavaIdentifierStart(name.charAt(0))) {
			s[j++] = 95;
		}

		for (int i = 0; i < name.length(); ++j) {
			char c = name.charAt(i);
			if (Character.isJavaIdentifierPart(c)) {
				s[j] = c;
			} else {
				s[j] = 95;
			}

			++i;
		}

		return (new String(s)).trim();
	}

	private static void p(String s) {
		System.out.println(s);
	}

	private void addError(String messageId, String arg) {
		String msg;
		if (arg == null) {
			msg = ToolErrorReporter.getMessage(messageId);
		} else {
			msg = ToolErrorReporter.getMessage(messageId, arg);
		}

		this.addFormatedError(msg);
	}

	private void addFormatedError(String message) {
		this.reporter.error(message, (String) null, -1, (String) null, -1);
	}
}