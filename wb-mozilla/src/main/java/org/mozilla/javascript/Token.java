/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package org.mozilla.javascript;

public class Token {
	public static final boolean printTrees = false;
	static final boolean printICode = false;
	static final boolean printNames = false;
	public static final int ERROR = -1;
	public static final int EOF = 0;
	public static final int EOL = 1;
	public static final int FIRST_BYTECODE_TOKEN = 2;
	public static final int ENTERWITH = 2;
	public static final int LEAVEWITH = 3;
	public static final int RETURN = 4;
	public static final int GOTO = 5;
	public static final int IFEQ = 6;
	public static final int IFNE = 7;
	public static final int SETNAME = 8;
	public static final int BITOR = 9;
	public static final int BITXOR = 10;
	public static final int BITAND = 11;
	public static final int EQ = 12;
	public static final int NE = 13;
	public static final int LT = 14;
	public static final int LE = 15;
	public static final int GT = 16;
	public static final int GE = 17;
	public static final int LSH = 18;
	public static final int RSH = 19;
	public static final int URSH = 20;
	public static final int ADD = 21;
	public static final int SUB = 22;
	public static final int MUL = 23;
	public static final int DIV = 24;
	public static final int MOD = 25;
	public static final int NOT = 26;
	public static final int BITNOT = 27;
	public static final int POS = 28;
	public static final int NEG = 29;
	public static final int NEW = 30;
	public static final int DELPROP = 31;
	public static final int TYPEOF = 32;
	public static final int GETPROP = 33;
	public static final int GETPROPNOWARN = 34;
	public static final int SETPROP = 35;
	public static final int GETELEM = 36;
	public static final int SETELEM = 37;
	public static final int CALL = 38;
	public static final int NAME = 39;
	public static final int NUMBER = 40;
	public static final int STRING = 41;
	public static final int NULL = 42;
	public static final int THIS = 43;
	public static final int FALSE = 44;
	public static final int TRUE = 45;
	public static final int SHEQ = 46;
	public static final int SHNE = 47;
	public static final int REGEXP = 48;
	public static final int BINDNAME = 49;
	public static final int THROW = 50;
	public static final int RETHROW = 51;
	public static final int IN = 52;
	public static final int INSTANCEOF = 53;
	public static final int LOCAL_LOAD = 54;
	public static final int GETVAR = 55;
	public static final int SETVAR = 56;
	public static final int CATCH_SCOPE = 57;
	public static final int ENUM_INIT_KEYS = 58;
	public static final int ENUM_INIT_VALUES = 59;
	public static final int ENUM_INIT_ARRAY = 60;
	public static final int ENUM_NEXT = 61;
	public static final int ENUM_ID = 62;
	public static final int THISFN = 63;
	public static final int RETURN_RESULT = 64;
	public static final int ARRAYLIT = 65;
	public static final int OBJECTLIT = 66;
	public static final int GET_REF = 67;
	public static final int SET_REF = 68;
	public static final int DEL_REF = 69;
	public static final int REF_CALL = 70;
	public static final int REF_SPECIAL = 71;
	public static final int YIELD = 72;
	public static final int DEFAULTNAMESPACE = 73;
	public static final int ESCXMLATTR = 74;
	public static final int ESCXMLTEXT = 75;
	public static final int REF_MEMBER = 76;
	public static final int REF_NS_MEMBER = 77;
	public static final int REF_NAME = 78;
	public static final int REF_NS_NAME = 79;
	public static final int LAST_BYTECODE_TOKEN = 79;
	public static final int TRY = 80;
	public static final int SEMI = 81;
	public static final int LB = 82;
	public static final int RB = 83;
	public static final int LC = 84;
	public static final int RC = 85;
	public static final int LP = 86;
	public static final int RP = 87;
	public static final int COMMA = 88;
	public static final int ASSIGN = 89;
	public static final int ASSIGN_BITOR = 90;
	public static final int ASSIGN_BITXOR = 91;
	public static final int ASSIGN_BITAND = 92;
	public static final int ASSIGN_LSH = 93;
	public static final int ASSIGN_RSH = 94;
	public static final int ASSIGN_URSH = 95;
	public static final int ASSIGN_ADD = 96;
	public static final int ASSIGN_SUB = 97;
	public static final int ASSIGN_MUL = 98;
	public static final int ASSIGN_DIV = 99;
	public static final int ASSIGN_MOD = 100;
	public static final int FIRST_ASSIGN = 89;
	public static final int LAST_ASSIGN = 100;
	public static final int HOOK = 101;
	public static final int COLON = 102;
	public static final int OR = 103;
	public static final int AND = 104;
	public static final int INC = 105;
	public static final int DEC = 106;
	public static final int DOT = 107;
	public static final int FUNCTION = 108;
	public static final int EXPORT = 109;
	public static final int IMPORT = 110;
	public static final int IF = 111;
	public static final int ELSE = 112;
	public static final int SWITCH = 113;
	public static final int CASE = 114;
	public static final int DEFAULT = 115;
	public static final int WHILE = 116;
	public static final int DO = 117;
	public static final int FOR = 118;
	public static final int BREAK = 119;
	public static final int CONTINUE = 120;
	public static final int VAR = 121;
	public static final int WITH = 122;
	public static final int CATCH = 123;
	public static final int FINALLY = 124;
	public static final int VOID = 125;
	public static final int RESERVED = 126;
	public static final int EMPTY = 127;
	public static final int BLOCK = 128;
	public static final int LABEL = 129;
	public static final int TARGET = 130;
	public static final int LOOP = 131;
	public static final int EXPR_VOID = 132;
	public static final int EXPR_RESULT = 133;
	public static final int JSR = 134;
	public static final int SCRIPT = 135;
	public static final int TYPEOFNAME = 136;
	public static final int USE_STACK = 137;
	public static final int SETPROP_OP = 138;
	public static final int SETELEM_OP = 139;
	public static final int LOCAL_BLOCK = 140;
	public static final int SET_REF_OP = 141;
	public static final int DOTDOT = 142;
	public static final int COLONCOLON = 143;
	public static final int XML = 144;
	public static final int DOTQUERY = 145;
	public static final int XMLATTR = 146;
	public static final int XMLEND = 147;
	public static final int TO_OBJECT = 148;
	public static final int TO_DOUBLE = 149;
	public static final int GET = 150;
	public static final int SET = 151;
	public static final int LET = 152;
	public static final int CONST = 153;
	public static final int SETCONST = 154;
	public static final int SETCONSTVAR = 155;
	public static final int ARRAYCOMP = 156;
	public static final int LETEXPR = 157;
	public static final int WITHEXPR = 158;
	public static final int DEBUGGER = 159;
	public static final int CONDCOMMENT = 160;
	public static final int KEEPCOMMENT = 161;
	public static final int LAST_TOKEN = 162;

	public static String name(int token) {
		return String.valueOf(token);
	}
}