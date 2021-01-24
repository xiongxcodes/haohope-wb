package com.wb.common;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.alibaba.fastjson.JSON;
import com.wb.cache.MemoryCache;
import com.wb.cache.VarCache;
import com.wb.cache.WbCache;
import com.wb.lock.DefaultVarLock;
import com.wb.lock.VarLock;
import com.wb.util.DbUtil;
import com.wb.util.JsonUtil;
import com.wb.util.StringUtil;
import com.wb.util.SysUtil;

public class Var implements ApplicationContextAware {
    public static boolean uncheckModified;
    public static boolean sendStreamGzip;
    public static int sendGzipMinSize;
    public static boolean printError;
    public static boolean showSocketError;
    public static boolean log;
    public static boolean taskLog;
    public static int limitRecords;
    public static int limitExportRecords;
    public static int gzipMinSize;
    public static boolean cacheEnabled;
    public static int cacheMaxAge;
    public static boolean debug;
    public static boolean homeConsolePrint;
    public static boolean homeShowApp;
    public static boolean consolePrint;
    public static boolean serverConsolePrint;
    public static boolean batchUpdate;
    public static int sessionTimeout;
    public static boolean uniqueLogin;
    public static int ajaxTimeout;
    public static int maskTimeout;
    public static boolean useLocalTime;
    public static boolean recordSession;
    public static int stringAsText;
    public static String syncPath;
    public static String jndi = "java:comp/env/default";
    public static String language;
    public static String defaultLanguage;
    public static String urlEncoding;
    public static String sessionVars;
    public static boolean forceUpperCase;
    public static boolean checkFieldReadOnly;
    public static boolean useServletPath;
    public static boolean autoLogout;
    public static String emptyString;
    public static int fetchSize;
    public static JSONArray cssLinks;
    public static JSONArray jsLinks;
    public static final File file;
    private static String fileid;
    public static VarLock lock = new DefaultVarLock();
    public static WbCache<String, Object> buffer = new MemoryCache<String, Object>();
    static {
        file = new File(Base.path, "wb/system/var.json");
    }

    public static Object get(String name) {
        if (StringUtil.isEmpty(name)) {
            throw new NullPointerException("Var name \"" + name + "\" can not be blank");
        } else {
            Object val = buffer.get(name);
            if (val == null) {
                throw new NullPointerException("Var \"" + name + "\" does not exist");
            } else {
                return val;
            }
        }
    }

    public static String getString(String name) {
        return get(name).toString();
    }

    public static int getInt(String name) {
        Object val = get(name);
        if (val instanceof Integer) {
            return (Integer)val;
        } else {
            throw new RuntimeException("Var \"" + name + "\" is not an integer value");
        }
    }

    public static double getDouble(String name) {
        Object val = get(name);
        if (val instanceof Double) {
            return (Double)val;
        } else {
            throw new RuntimeException("Var \"" + name + "\" is not a double value");
        }
    }

    public static boolean getBool(String name) {
        Object val = get(name);
        if (val instanceof Boolean) {
            return (Boolean)val;
        } else {
            throw new RuntimeException("Var \"" + name + "\" is not a boolean value");
        }
    }

    public static void delVar(String path, List<String> keys) {
        try {
            lock.lock();
            if (StringUtil.isEmpty(path)) {
                throw new RuntimeException("Empty path value.");
            } else {
                JSONArray names = new JSONArray(JSON.toJSONString(keys));
                JSONObject object = Var.loadVar();// JsonUtil.readObject(Var.file);
                JSONObject selFolder = (JSONObject)JsonUtil.getValue(object, path, '.');
                int j = names.length();
                int i;
                for (i = 0; i < j; ++i) {
                    selFolder.remove(names.optString(i));
                }
                // FileUtil.syncSave(Var.file, object.toString(2));
                Var.syncSave(object.toString(2));
                for (i = 0; i < j; ++i) {
                    Var.buffer.remove(StringUtil.concat(new String[] {path, ".", names.optString(i)}));
                }
            }
        } catch (Throwable var5) {
            throw new RuntimeException(var5);
        } finally {
            lock.unlock();
        }
    }

    public static List<String> getVars(String path) {
        JSONObject jo = Var.loadVar();// JsonUtil.readObject(Var.file);
        JSONObject folder = (JSONObject)JsonUtil.getValue(jo, path, '.');
        if (folder == null) {
            throw new IllegalArgumentException("指定路径变量不存在。");
        } else {
            Set<Entry<String, Object>> entrySet = folder.entrySet();
            List<String> items = new ArrayList<String>();
            Iterator var10 = entrySet.iterator();
            while (var10.hasNext()) {
                Entry<String, Object> entry = (Entry)var10.next();
                items.add(entry.getKey());
            }
            return items;
        }
    }

    public static void add(String path, String name, String type, String valueStr, String configStr, String remark) {
        try {
            Var.lock.lock();
            JSONObject object = Var.loadVar();// JsonUtil.readObject(Var.file);
            boolean isNew = true;
            if (name.indexOf(46) != -1) {
                throw new RuntimeException("名称 \"" + name + "\" 不能包含符号 “.”。");
            } else {
                Object folderObject = JsonUtil.getValue(object, path, '.');
                if (folderObject instanceof JSONObject) {
                    JSONObject folder = (JSONObject)folderObject;
                    if (folder.has(name)) {
                        if (isNew) {
                            throw new RuntimeException("名称 \"" + name + "\" 已经存在。");
                        }
                    } else if (!isNew) {
                        throw new RuntimeException("名称 \"" + name + "\" 不存在。");
                    }

                    Object primativeVal;
                    if (type.equals("int")) {
                        primativeVal = Integer.parseInt(valueStr);
                    } else if (type.equals("bool")) {
                        primativeVal = Boolean.parseBoolean(valueStr);
                    } else if (type.equals("double")) {
                        primativeVal = Double.parseDouble(valueStr);
                    } else {
                        primativeVal = valueStr;
                    }

                    JSONArray value;
                    if (isNew) {
                        value = new JSONArray();
                        value.put(primativeVal);
                        value.put(remark);
                        JSONObject config;
                        if (StringUtil.isEmpty(configStr)) {
                            config = new JSONObject();
                        } else {
                            config = new JSONObject(configStr);
                        }

                        config.put("type", type);
                        value.put(config);
                        folder.put(name, value);
                    } else {
                        value = folder.getJSONArray(name);
                        value.put(0, primativeVal);
                    }

                    // FileUtil.syncSave(Var.file, object.toString(2));
                    Var.syncSave(object.toString(2));
                    Var.buffer.put(path + '.' + name, primativeVal);
                    // Var.loadBasicVars();
                } else {
                    throw new RuntimeException("目录 \"" + path + "\" 不存在或不是一个目录。");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            lock.unlock();
        }
    }

    public static void set(String name, Object value) {
        if (name == null) {
            throw new NullPointerException("Null variable name");
        } else if (value == null) {
            throw new NullPointerException("Null variable value");
        } else {
            try {
                lock.lock();
                Connection conn = null;
                Statement st = null;
                ResultSet rs = null;
                PreparedStatement pst = null;
                JSONObject object = null;
                try {
                    conn = DbUtil.getConnection();
                    st = conn.createStatement();
                    rs = st.executeQuery("SELECT a.ID , a.CONTENT FROM wb_vars a  WHERE a.id = '" + fileid + "' ");
                    if (rs.next()) {
                        object = new JSONObject(rs.getString("CONTENT"));
                    } else {
                        fileid = "";
                        object = JsonUtil.readObject(file);
                    }
                    // getValues(object, "");
                    // loadBasicVars();
                    Object valObject = JsonUtil.getValue(object, name, '.');
                    if (!(valObject instanceof JSONArray)) {
                        throw new RuntimeException("\"" + name + "\" is not a variable.");
                    } else {
                        JSONArray valArray = (JSONArray)valObject;
                        valArray.put(0, value);
                        // FileUtil.syncSave(file, object.toString(2));
                        if (StringUtil.isEmpty(fileid)) {
                            fileid = SysUtil.getId();
                            pst = conn.prepareStatement(" INSERT INTO wb_vars(ID,CONTENT,CREATETIME) values(?,?,?) ");
                            pst.setObject(1, fileid);
                            pst.setObject(2, object.toString(2));
                            pst.setObject(3, new Date());
                            pst.execute();
                        } else {
                            pst = conn.prepareStatement(" UPDATE wb_vars SET CONTENT=?,UPDATETIME=? WHERE ID=? ");
                            pst.setObject(1, object.toString(2));
                            pst.setObject(2, new Date());
                            pst.setObject(3, fileid);
                            pst.execute();
                        }
                        buffer.put(name, value);
                        loadBasicVars();
                    }
                } finally {
                    DbUtil.close(pst);
                    DbUtil.close(rs);
                    DbUtil.close(st);
                    DbUtil.close(conn);
                }
            } catch (Throwable var5) {
                var5.printStackTrace();
                throw new RuntimeException(var5);
            } finally {
                lock.unlock();
            }
        }
    }

    public static void syncSave(String str) {
        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement pst = null;
        try {
            lock.lock();
            conn = DbUtil.getConnection();
            if (StringUtil.isEmpty(fileid)) {
                fileid = SysUtil.getId();
                pst = conn.prepareStatement(" INSERT INTO wb_vars(ID,CONTENT,CREATETIME) values(?,?,?) ");
                pst.setObject(1, fileid);
                pst.setObject(2, str);
                pst.setObject(3, new Date());
                pst.execute();
            } else {
                pst = conn.prepareStatement(" UPDATE wb_vars SET CONTENT=?,UPDATETIME=? WHERE ID=? ");
                pst.setObject(1, str);
                pst.setObject(2, new Date());
                pst.setObject(3, fileid);
                pst.execute();
            }
            loadBasicVars();
        } catch (Throwable var5) {
            throw new RuntimeException(var5);
        } finally {
            DbUtil.close(pst);
            DbUtil.close(rs);
            DbUtil.close(conn);
            lock.unlock();
        }
    }

    public static JSONObject loadVar() {
        try {
            lock.lock();
            Connection conn = null;
            Statement st = null;
            ResultSet rs = null;
            PreparedStatement pst = null;
            JSONObject object = null;
            try {
                conn = DbUtil.getConnection();
                st = conn.createStatement();
                if (StringUtil.isEmpty(fileid)) {
                    rs = st.executeQuery("SELECT a.ID,a.CONTENT FROM wb_vars a ORDER BY a.CREATETIME DESC  ");
                } else {
                    rs = st.executeQuery("SELECT a.ID,a.CONTENT FROM wb_vars a WHERE a.ID = '" + fileid
                        + "' ORDER BY a.CREATETIME DESC  ");
                }
                if (rs.next()) {
                    fileid = rs.getString("ID");
                    object = new JSONObject(rs.getString("CONTENT"));
                } else {
                    object = JsonUtil.readObject(file);
                    fileid = SysUtil.getId();
                    pst = conn.prepareStatement(" INSERT INTO wb_vars(ID,CONTENT,CREATETIME) values(?,?,?) ");
                    pst.setObject(1, fileid);
                    pst.setObject(2, object.toString(2));
                    pst.setObject(3, new Date());
                    pst.execute();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                DbUtil.close(pst);
                DbUtil.close(rs);
                DbUtil.close(st);
                DbUtil.close(conn);
            }
            return object;
        } catch (Throwable var11) {
            throw new RuntimeException(var11);
        } finally {
            lock.unlock();
        }
    }

    public static void load() {
        try {
            lock.lock();
            buffer.clear();
            Connection conn = null;
            Statement st = null;
            ResultSet rs = null;
            PreparedStatement pst = null;
            try {
                conn = DbUtil.getConnection();
                st = conn.createStatement();
                JSONObject object = null;
                rs = st.executeQuery("SELECT a.ID,a.CONTENT FROM wb_vars a ORDER BY a.CREATETIME DESC  ");
                if (rs.next()) {
                    fileid = rs.getString("ID");
                    object = new JSONObject(rs.getString("CONTENT"));
                } else {
                    object = JsonUtil.readObject(file);
                    pst = conn.prepareStatement(" INSERT INTO wb_vars(ID,CONTENT,CREATETIME) values(?,?,?) ");
                    pst.setObject(1, SysUtil.getId());
                    pst.setObject(2, object.toString());
                    pst.setObject(3, new Date());
                    pst.execute();
                }
                if (buffer.isEmpty()) {
                    getValues(object, "");
                }
                loadBasicVars();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                DbUtil.close(pst);
                DbUtil.close(rs);
                DbUtil.close(st);
                DbUtil.close(conn);
            }

        } catch (Throwable var11) {
            var11.printStackTrace();
            throw new RuntimeException(var11);
        } finally {
            lock.unlock();
        }
    }

    /**
     * public static synchronized void load() { try { JSONObject object = JsonUtil.readObject(file); getValues(object,
     * ""); loadBasicVars(); } catch (Throwable var1) { throw new RuntimeException(var1); } }
     **/

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void getValues(JSONObject object, String parentName) {
        Set<Entry<String, Object>> items = object.entrySet();
        Iterator var7 = items.iterator();

        while (var7.hasNext()) {
            Entry<String, Object> item = (Entry)var7.next();
            Object value = item.getValue();
            String name = parentName + (String)item.getKey();
            if (value instanceof JSONObject) {
                getValues((JSONObject)value, name + '.');
            } else {
                JSONArray jsonArray = (JSONArray)value;
                if ("double".equals(jsonArray.getJSONObject(2).opt("type"))) {
                    buffer.put(name, ((Number)jsonArray.opt(0)).doubleValue());
                } else {
                    buffer.put(name, jsonArray.opt(0));
                }
            }
        }

    }

    public static void loadBasicVars() {
        uncheckModified = !getBool("sys.cache.checkModified");
        sendStreamGzip = getBool("sys.sendStreamGzip");
        sendGzipMinSize = getInt("sys.sendGzipMinSize");
        printError = getBool("sys.printError");
        showSocketError = getBool("sys.network.showSocketError");
        log = getBool("sys.log");
        taskLog = getBool("sys.task.log");
        limitRecords = getInt("sys.controls.limitRecords");
        limitExportRecords = getInt("sys.controls.limitExportRecords");
        gzipMinSize = getInt("sys.cache.gzipMinSize");
        cacheEnabled = getBool("sys.cache.enabled");
        cacheMaxAge = getInt("sys.cache.maxAge");
        debug = getBool("sys.debug");
        homeConsolePrint = getBool("sys.app.homeConsolePrint");
        homeShowApp = getBool("sys.app.homeShowApp");
        consolePrint = getBool("sys.ide.consolePrint");
        serverConsolePrint = getBool("sys.serverConsolePrint");
        batchUpdate = getBool("sys.db.batchUpdate");
        sessionTimeout = getInt("sys.session.sessionTimeout");
        uniqueLogin = getBool("sys.session.uniqueLogin");
        ajaxTimeout = getInt("sys.session.ajaxTimeout");
        maskTimeout = getInt("sys.session.maskTimeout");
        useLocalTime = getBool("sys.locale.useLocalTime");
        recordSession = getBool("sys.session.recordSession");
        stringAsText = getInt("sys.db.stringAsText");
        syncPath = getString("sys.ide.syncPath");
        jndi = getString("sys.jndi.default");
        language = getString("sys.locale.language");
        defaultLanguage = getString("sys.locale.defaultLanguage");
        urlEncoding = getString("sys.locale.urlEncoding");
        sessionVars = getString("sys.session.sessionVars");
        forceUpperCase = getBool("sys.db.forceUpperCase");
        checkFieldReadOnly = getBool("sys.db.checkFieldReadOnly");
        useServletPath = getBool("sys.useServletPath");
        autoLogout = getBool("sys.session.autoLogout");
        emptyString = getString("sys.db.emptyString");
        fetchSize = getInt("sys.db.fetchSize");
        String value = getString("sys.app.cssLinks");
        String[] items;
        String item;
        int var3;
        int var4;
        String[] var5;
        if (StringUtil.isEmpty(value)) {
            cssLinks = null;
        } else {
            cssLinks = new JSONArray();
            items = StringUtil.split(value, ';');
            var5 = items;
            var4 = items.length;

            for (var3 = 0; var3 < var4; ++var3) {
                item = var5[var3];
                cssLinks.put(item.trim());
            }
        }

        value = getString("sys.app.jsLinks");
        if (StringUtil.isEmpty(value)) {
            jsLinks = null;
        } else {
            jsLinks = new JSONArray();
            items = StringUtil.split(value, ';');
            var5 = items;
            var4 = items.length;

            for (var3 = 0; var3 < var4; ++var3) {
                item = var5[var3];
                jsLinks.put(item.trim());
            }
        }

        if (getBool("sys.optimize")) {
            debug = false;
            homeConsolePrint = false;
            printError = false;
            consolePrint = false;
            serverConsolePrint = false;
            uncheckModified = true;
        }

    }

    /**
     * <p>
     * Title: setApplicationContext
     * </p>
     * <p>
     * Description:
     * </p>
     * 
     * @param applicationContext
     * @throws BeansException
     * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, VarCache> buffermap = applicationContext.getBeansOfType(VarCache.class);
        if (null != buffermap) {
            for (Entry<String, VarCache> entry : buffermap.entrySet()) {
                buffer = entry.getValue();
                break;
            }
        }
        Map<String, VarLock> lockmap = applicationContext.getBeansOfType(VarLock.class);
        if (null != lockmap) {
            for (Entry<String, VarLock> entry : lockmap.entrySet()) {
                lock = entry.getValue();
                break;
            }
        }
    }
}