package com.wb.common;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONObject;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.wb.cache.MemoryCache;
import com.wb.cache.UrlCache;
import com.wb.cache.WbCache;
import com.wb.lock.DefaultUrlLock;
import com.wb.lock.UrlLock;
import com.wb.util.DbUtil;
import com.wb.util.FileUtil;
import com.wb.util.JsonUtil;
import com.wb.util.StringUtil;
import com.wb.util.SysUtil;

public class UrlBuffer implements ApplicationContextAware {
    public static WbCache<String, String> buffer = new MemoryCache<String, String>();
    private static UrlLock lock = new DefaultUrlLock();
    private static final File file;
    private static String fileid;
    static {
        file = new File(Base.path, "wb/system/url.json");
    }

    public static String get(String url) {
        return (String)buffer.get(url);
    }

    public static void put(String url, String path) {
        buffer.put(url, path);
    }

    public static boolean exists(String url, File file) throws IOException {
        if (!StringUtil.isEmpty(url)) {
            String urlPath = get('/' + url);
            String relPath;
            if (file == null) {
                relPath = null;
            } else {
                relPath = FileUtil.getModulePath(file);
            }

            if (urlPath != null && (relPath == null || !StringUtil.isSame(urlPath, relPath))) {
                return false;
            }
        }

        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean remove(String path) {
        Set<Entry<String, String>> es = buffer.entrySet();
        boolean result = false;
        String delPath = StringUtil.concat(new String[] {path, "/"});
        Iterator var7 = es.iterator();

        while (var7.hasNext()) {
            Entry<String, String> e = (Entry)var7.next();
            String key = (String)e.getKey();
            String modulePath = StringUtil.concat(new String[] {(String)e.getValue(), "/"});
            if (modulePath.startsWith(delPath) && key.length() > 1) {
                buffer.remove(key);
                if (!result) {
                    result = true;
                }
            }
        }

        return result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
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
                rs = st.executeQuery("SELECT a.ID,a.CONTENT FROM wb_urls a ORDER BY a.CREATETIME DESC  ");
                if (rs.next()) {
                    fileid = rs.getString("ID");
                    object = new JSONObject(rs.getString("CONTENT"));
                } else {
                    fileid = SysUtil.getId();
                    object = JsonUtil.readObject(file);
                    pst = conn.prepareStatement(" INSERT INTO wb_urls(ID,CONTENT,CREATETIME) values(?,?,?) ");
                    pst.setObject(1, fileid);
                    pst.setObject(2, object.toString());
                    pst.setObject(3, new Date());
                    pst.execute();
                }

                Set<Entry<String, Object>> es = object.entrySet();
                Iterator var3 = es.iterator();

                while (var3.hasNext()) {
                    Entry<String, Object> e = (Entry)var3.next();
                    put((String)e.getKey(), (String)e.getValue());
                }

                String portal = Var.getString("sys.portal");
                if (portal.endsWith(".xwl")) {
                    put("/", portal);
                } else {
                    put("/", StringUtil.select(new String[] {(String)buffer.get("/" + portal), "index"}));
                }

                put("/m", "");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                DbUtil.close(pst);
                DbUtil.close(rs);
                DbUtil.close(st);
                DbUtil.close(conn);
            }
        } catch (Throwable var4) {
            throw new RuntimeException(var4);
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void save() throws Exception {
        Set<Entry<String, String>> es = buffer.entrySet();
        JSONObject object = new JSONObject();
        Iterator var4 = es.iterator();
        while (var4.hasNext()) {
            Entry<String, String> e = (Entry)var4.next();
            String key = (String)e.getKey();
            if (!key.equals("/") && !key.equals("/m")) {
                object.put(key, e.getValue());
            }
        }
        Connection conn = null;
        PreparedStatement pst = null;
        try {
            conn = DbUtil.getConnection();

            if (StringUtil.isEmpty(fileid)) {
                fileid = SysUtil.getId();
                pst = conn.prepareStatement(" INSERT INTO wb_urls(ID,CONTENT,CREATETIME) values(?,?,?) ");
                pst.setObject(1, fileid);
                pst.setObject(2, object.toString(2));
                pst.setObject(3, new Date());
                pst.execute();
            } else {
                pst = conn.prepareStatement(" UPDATE wb_urls SET CONTENT=?,UPDATETIME=? WHERE ID=? ");
                pst.setObject(1, fileid);
                pst.setObject(2, new Date());
                pst.setObject(3, object.toString(2));
                pst.execute();
            }

        } finally {
            DbUtil.close(pst);
            DbUtil.close(conn);
        }
        // FileUtil.syncSave(file, object.toString());
    }

    @SuppressWarnings("rawtypes")
    public static String find(File file) {
        String path = FileUtil.getModulePath(file);
        if (path == null) {
            return null;
        } else {
            Set<Entry<String, String>> es = buffer.entrySet();
            Iterator var5 = es.iterator();

            String key;
            Entry e;
            do {
                if (!var5.hasNext()) {
                    return null;
                }
                e = (Entry)var5.next();
                key = (String)e.getKey();
            } while (!path.equals(e.getValue()) || key.equals("/"));

            return key.substring(1);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean change(String src, String dest, boolean isDir) {
        Set<Entry<String, String>> es = buffer.entrySet();
        int srcLen = src.length() + 1;
        boolean result = false;
        src = src + '/';
        if (isDir) {
            dest = dest + '/';
        }

        Iterator var10 = es.iterator();

        while (var10.hasNext()) {
            Entry<String, String> e = (Entry)var10.next();
            String key = (String)e.getKey();
            String value = (String)e.getValue();
            String path = StringUtil.concat(new String[] {value, "/"});
            if (path.startsWith(src)) {
                if (isDir) {
                    buffer.put(key, dest + value.substring(srcLen));
                } else {
                    buffer.put(key, dest);
                }

                if (!result) {
                    result = true;
                }
            }
        }

        return result;
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
        Map<String, UrlCache> buffermap = applicationContext.getBeansOfType(UrlCache.class);
        if (null != buffermap) {
            for (Entry<String, UrlCache> entry : buffermap.entrySet()) {
                buffer = entry.getValue();
                break;
            }
        }
        Map<String, UrlLock> lockmap = applicationContext.getBeansOfType(UrlLock.class);
        if (null != lockmap) {
            for (Entry<String, UrlLock> entry : lockmap.entrySet()) {
                lock = entry.getValue();
                break;
            }
        }
    }
}