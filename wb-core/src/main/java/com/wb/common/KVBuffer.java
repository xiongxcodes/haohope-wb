package com.wb.common;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.wb.cache.KVCache;
import com.wb.cache.MemoryCache;
import com.wb.cache.WbCache;
import com.wb.lock.DefalutKVLock;
import com.wb.lock.KVLock;
import com.wb.util.DbUtil;
import com.wb.util.StringUtil;

public class KVBuffer implements ApplicationContextAware {
    private static KVLock lock = new DefalutKVLock();
    public static WbCache<String, ConcurrentHashMap<Object, String>> buffer =
        new MemoryCache<String, ConcurrentHashMap<Object, String>>();

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void reloadKey(Connection conn, String keyName, int type) throws Exception {
        PreparedStatement st = null;
        ResultSet rs = null;
        ConcurrentHashMap map = new ConcurrentHashMap();

        try {
            st = conn.prepareStatement(
                "select a.K,a.V from WB_KEY a, WB_KEY_TREE b where a.KEY_ID=b.KEY_ID and b.KEY_NAME=? order by b.KEY_NAME");
            st.setString(1, keyName);
            rs = st.executeQuery();

            while (rs.next()) {
                String k = rs.getString("K");
                map.put(type == 1 ? k : Integer.parseInt(k), rs.getString("V"));
            }

            if (map.isEmpty()) {
                buffer.remove(keyName);
            } else {
                buffer.put(keyName, map);
            }
        } finally {
            DbUtil.close(rs);
            DbUtil.close(st);
        }

    }

    public static String getList(String keyName) {
        return getList(keyName, "K", "V");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static String getList(String keyName, String k, String v) {
        ConcurrentHashMap<Object, String> map = (ConcurrentHashMap)buffer.get(keyName);
        if (map == null) {
            return "[]";
        } else {
            StringBuilder buf = new StringBuilder();
            Set<Entry<Object, String>> es = map.entrySet();
            boolean isFirst = true;
            buf.append("[");
            Iterator var10 = es.iterator();

            while (var10.hasNext()) {
                Entry<Object, String> e = (Entry)var10.next();
                if (isFirst) {
                    isFirst = false;
                } else {
                    buf.append(",");
                }

                buf.append("{\"");
                buf.append(k);
                buf.append("\":");
                Object K = e.getKey();
                if (K instanceof Integer) {
                    buf.append(Integer.toString((Integer)K));
                } else {
                    buf.append(StringUtil.quote((String)K));
                }

                buf.append(",\"");
                buf.append(v);
                buf.append("\":");
                String V = (String)e.getValue();
                if (V.startsWith("@")) {
                    V = V.substring(1);
                } else {
                    V = StringUtil.quote(V);
                }

                buf.append(V);
                buf.append("}");
            }

            buf.append("]");
            return buf.toString();
        }
    }

    public static void load() {
        try {
            lock.lock();
            buffer.clear();
            Connection conn = null;
            Statement st = null;
            ResultSet rs = null;
            String keyName = null;
            String preKeyName = null;
            ConcurrentHashMap map = new ConcurrentHashMap();
            try {
                conn = DbUtil.getConnection();
                st = conn.createStatement();
                for (rs = st.executeQuery(
                    "select a.K,a.V,b.KEY_NAME,b.TYPE from WB_KEY a, WB_KEY_TREE b where a.KEY_ID=b.KEY_ID order by b.KEY_NAME");
                    rs.next(); preKeyName = keyName) {
                    keyName = rs.getString("KEY_NAME");
                    if (preKeyName != null && !preKeyName.equals(keyName)) {
                        buffer.put(preKeyName, map);
                        map = new ConcurrentHashMap();
                    }
                    String K = rs.getString("K");
                    map.put(rs.getInt("TYPE") == 1 ? K : Integer.parseInt(K), rs.getString("V"));
                }
                if (preKeyName != null) {
                    buffer.put(preKeyName, map);
                }
            } finally {
                DbUtil.close(rs);
                DbUtil.close(st);
                DbUtil.close(conn);
            }
        } catch (Throwable var11) {
            throw new RuntimeException(var11);
        } finally {
            lock.unlock();
        }
    }

    public static String getValue(ConcurrentHashMap<?, ?> map, Object key) {
        if (key == null) {
            return null;
        } else {
            Object value;
            if (key instanceof Number) {
                value = map.get(((Number)key).intValue());
            } else {
                value = map.get(key.toString());
            }

            return value == null ? key.toString() : value.toString();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static String getValue(String keyName, Object key) {
        ConcurrentHashMap<Object, String> kv = (ConcurrentHashMap)buffer.get(keyName);
        return kv == null ? null : getValue(kv, key);
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
        Map<String, KVCache> buffermap = applicationContext.getBeansOfType(KVCache.class);
        if (null != buffermap) {
            for (Entry<String, KVCache> entry : buffermap.entrySet()) {
                buffer = entry.getValue();
                break;
            }
        }
        Map<String, KVLock> lockmap = applicationContext.getBeansOfType(KVLock.class);
        if (null != lockmap) {
            for (Entry<String, KVLock> entry : lockmap.entrySet()) {
                lock = entry.getValue();
                break;
            }
        }
    }
}