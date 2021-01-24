package com.wb.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.wb.common.Dictionary;
import com.wb.common.FileBuffer;
import com.wb.common.KVBuffer;
import com.wb.common.ScriptBuffer;
import com.wb.common.Str;
import com.wb.common.UrlBuffer;
import com.wb.common.Var;
import com.wb.common.XwlBuffer;
import com.wb.fit.CustomRequest;
import com.wb.interact.Controls;
import com.wb.interact.Module;

public class SysUtil {
    private static long currentId = 0L;
    private static byte serverId0;
    private static byte serverId1;
    private static Object lock = new Object();
    public static final byte[] digits = new byte[] {48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 65, 66, 67, 68, 69, 70, 71,
        72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90};
    public static final int bufferSize = 4096;
    private static final ConcurrentHashMap<String, Method> methodBuffer = new ConcurrentHashMap();

    public static String getId() {
        return UUID.randomUUID().toString().replace("-", "");
        /**
         * Object var2 = lock; long id; synchronized (lock) { if (currentId == 0L) { currentId = (new Date()).getTime()
         * * 10000L; String serverId = Var.getString("sys.serverId"); serverId0 = (byte)serverId.charAt(0); serverId1 =
         * (byte)serverId.charAt(1); }
         * 
         * id = (long)(currentId++); }
         * 
         * return numToString(id);
         **/
    }

    private static String numToString(long num) {
        byte[] buf = new byte[13];
        byte charPos = 12;
        buf[0] = serverId0;

        long val;
        for (buf[1] = serverId1; (val = num / 36L) > 0L; num = val) {
            buf[charPos--] = digits[(byte)((int)(num % 36L))];
        }

        buf[charPos] = digits[(byte)((int)num)];
        return new String(buf);
    }

    public static String getRootError(Throwable e) {
        Throwable c = e;

        Throwable cause;
        do {
            cause = c;
            c = c.getCause();
        } while (c != null);

        String message = cause.getMessage();
        if (StringUtil.isEmpty(message)) {
            message = cause.toString();
        }

        return StringUtil.toLine(message.trim());
    }

    public static boolean isNotCustomRequest(HttpServletRequest request) {
        return !(request instanceof CustomRequest);
    }

    public static Throwable getRootExcept(Throwable e) {
        Throwable c = e;

        Throwable cause;
        do {
            cause = c;
            c = c.getCause();
        } while (c != null);

        return cause;
    }

    public static void executeMethod(String classMethodName, HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        Method method = (Method)methodBuffer.get(classMethodName);
        if (method == null) {
            int pos = classMethodName.lastIndexOf(46);
            String className;
            String methodName;
            if (pos == -1) {
                className = "";
                methodName = classMethodName;
            } else {
                className = classMethodName.substring(0, pos);
                methodName = classMethodName.substring(pos + 1);
            }

            Class<?> cls = Class.forName(className);
            method = cls.getMethod(methodName, HttpServletRequest.class, HttpServletResponse.class);
            methodBuffer.put(classMethodName, method);
        }

        method.invoke((Object)null, request, response);
    }

    public static ByteArrayInputStream toByteArrayInputStream(InputStream is) throws IOException {
        if (is instanceof ByteArrayInputStream) {
            return (ByteArrayInputStream)is;
        } else {
            byte[] bytes;
            try {
                bytes = IOUtils.toByteArray(is);
            } finally {
                is.close();
            }

            return new ByteArrayInputStream(bytes);
        }
    }

    public static String readString(Reader reader) throws IOException {
        try {
            char[] buf = new char[4096];
            StringBuilder sb = new StringBuilder();

            int len;
            while ((len = reader.read(buf)) > 0) {
                sb.append(buf, 0, len);
            }

            String var5 = sb.toString();
            return var5;
        } finally {
            reader.close();
        }
    }

    public static void error(String msg) {
        throw new RuntimeException(msg);
    }

    public static void error(String msg, String errorNo) throws RuntimeException {
        throw new RuntimeException(StringUtil.concat(new String[] {"#WBE", errorNo, ":", msg}));
    }

    public static boolean isArray(Object object) {
        return object == null ? false : object.getClass().isArray();
    }

    public static Object[] javaArray(Object[] value) {
        return value;
    }

    public static Integer[] javaIntArray(Integer[] value) {
        return value;
    }

    public static String javaString(Object value) {
        return value.toString();
    }

    public static Integer javaInt(Object value) {
        if (value == null) {
            return null;
        } else {
            return value instanceof Number ? ((Number)value).intValue() : Integer.parseInt(value.toString());
        }
    }

    public static Long javaLong(Object value) {
        if (value == null) {
            return null;
        } else {
            return value instanceof Number ? ((Number)value).longValue() : Long.parseLong(value.toString());
        }
    }

    public static Float javaFloat(Object value) {
        if (value == null) {
            return null;
        } else {
            return value instanceof Number ? ((Number)value).floatValue() : Float.parseFloat(value.toString());
        }
    }

    public static Double javaDouble(Object value) {
        if (value == null) {
            return null;
        } else {
            return value instanceof Number ? ((Number)value).doubleValue() : Double.parseDouble(value.toString());
        }
    }

    public static Boolean javaBool(Boolean value) {
        return value;
    }

    public static boolean isMap(Object object) {
        return object instanceof Map;
    }

    public static boolean isIterable(Object object) {
        return object instanceof Iterable;
    }

    public static void reload(int type) {
        if (type == 1 || type == 2) {
            Var.load();
            Controls.load();
            FileBuffer.load();
            ScriptBuffer.load();
            Str.load();
            UrlBuffer.load();
            XwlBuffer.load();
        }

        if (type == 1 || type == 3) {
            KVBuffer.load();
            Dictionary.load();
            Module.load();
        }

    }

    public static Object getValue(Object[][] data, String key) {
        Object[][] var5 = data;
        int var4 = data.length;

        for (int var3 = 0; var3 < var4; ++var3) {
            Object[] item = var5[var3];
            if (key.equals(item[0])) {
                return item[1];
            }
        }

        return null;
    }

    public static void accessDenied(HttpServletRequest request) {
        throw new RuntimeException(Str.format(request, "accessDenied", new Object[0]));
    }

    public static void accessDenied(HttpServletRequest request, String msg) {
        throw new RuntimeException(Str.format(request, "accessDenied1", new Object[0]) + msg);
    }

    public static void accessDenied() {
        throw new RuntimeException("Access denied.");
    }

    public static void accessDenied(String msg) {
        throw new RuntimeException("Access denied: " + msg);
    }

    public static String getStackMessage(Throwable exception) {
        StringWriter writer = new StringWriter();
        PrintWriter pwriter = new PrintWriter(writer, true);
        exception.printStackTrace(pwriter);
        return writer.toString();
    }

    public static ByteArrayInputStream getEmptyStream() {
        return new ByteArrayInputStream(new byte[0]);
    }

    public static ArrayList<String> getObjectMembers(Object object) {
        return getClassMembers(object.getClass());
    }

    public static ArrayList<String> getClassMembers(Class<?> cls) {
        ArrayList<String> list = new ArrayList();
        Field[] fields = cls.getFields();
        Method[] methods = cls.getMethods();
        Field[] var8 = fields;
        int var7 = fields.length;

        String name;
        int var6;
        for (var6 = 0; var6 < var7; ++var6) {
            Field field = var8[var6];
            name = field.getName();
            if (list.indexOf(name) == -1) {
                list.add(name);
            }
        }

        Method[] var10 = methods;
        var7 = methods.length;

        for (var6 = 0; var6 < var7; ++var6) {
            Method method = var10[var6];
            name = method.getName();
            if (list.indexOf(name) == -1) {
                list.add(name);
            }
        }

        return list;
    }

    public static String getMacAddress() {
        try {
            NetworkInterface network = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            byte[] mac = network.getHardwareAddress();
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < mac.length; ++i) {
                sb.append(String.format("%02X%s", mac[i], i < mac.length - 1 ? "-" : ""));
            }

            return sb.toString();
        } catch (Throwable var4) {
            return "invalid";
        }
    }

    public static String getLineSeparator() {
        String sp = Var.getString("sys.locale.lineSeparator");
        if ("\\r".equals(sp)) {
            return "\r";
        } else {
            return "\\n".endsWith(sp) ? "\n" : "\r\n";
        }
    }

    public static <T> HashSet<T> toHashSet(T[] items) {
        if (items == null) {
            return null;
        } else {
            HashSet hashSet = new HashSet();
            Object[] arg4 = items;
            int arg3 = items.length;

            for (int arg2 = 0; arg2 < arg3; ++arg2) {
                Object item = arg4[arg2];
                hashSet.add(item);
            }

            return hashSet;
        }
    }

    public static <T> HashSet<T> toHashSet(Iterable<T> items) {
        if (items == null) {
            return null;
        } else {
            HashSet hashSet = new HashSet();
            Iterator arg2 = items.iterator();

            while (arg2.hasNext()) {
                Object item = (Object)arg2.next();
                hashSet.add(item);
            }

            return hashSet;
        }
    }
}