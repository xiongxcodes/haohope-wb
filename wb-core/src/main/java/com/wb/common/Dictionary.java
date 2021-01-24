package com.wb.common;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.wb.cache.DictionaryCache;
import com.wb.cache.MemoryCache;
import com.wb.cache.WbCache;
import com.wb.lock.DefaultDictionaryLock;
import com.wb.lock.DictionaryLock;
import com.wb.tool.DictRecord;
import com.wb.tool.ParentDictRecord;
import com.wb.util.DbUtil;
import com.wb.util.StringUtil;

public class Dictionary implements ApplicationContextAware {
    private static DictionaryLock lock = new DefaultDictionaryLock();
    public static WbCache<String, ConcurrentHashMap<String, ParentDictRecord>> buffer =
        new MemoryCache<String, ConcurrentHashMap<String, ParentDictRecord>>();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static DictRecord find(String[] tableNames, String fieldName) {
        String upperFieldName = fieldName.toUpperCase();
        String[] var8 = tableNames;
        int var7 = tableNames.length;

        for (int var6 = 0; var6 < var7; ++var6) {
            String tableName = var8[var6];
            ConcurrentHashMap<String, DictRecord> fieldMap = (ConcurrentHashMap)buffer.get(tableName.toUpperCase());
            if (fieldMap != null) {
                DictRecord dictRecord = (DictRecord)fieldMap.get(upperFieldName);
                if (dictRecord != null) {
                    if (dictRecord.linkTo == null) {
                        return dictRecord;
                    }

                    String[] tableField = StringUtil.split(dictRecord.linkTo.toUpperCase(), '.');
                    fieldMap = (ConcurrentHashMap)buffer.get(tableField[0]);
                    if (fieldMap == null) {
                        return null;
                    }
                    return (DictRecord)fieldMap.get(tableField[1]);
                }
            }
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static HashMap<String, String> getKeyFields(String[] tableNames) {
        HashMap<String, String> keyMap = new HashMap();
        String[] var7 = tableNames;
        int var6 = tableNames.length;

        for (int var5 = 0; var5 < var6; ++var5) {
            String tableName = var7[var5];
            ConcurrentHashMap<String, DictRecord> fieldMap = (ConcurrentHashMap)buffer.get(tableName.toUpperCase());
            if (fieldMap != null) {
                Set<Entry<String, DictRecord>> es = fieldMap.entrySet();
                Iterator var10 = es.iterator();

                while (var10.hasNext()) {
                    Entry<String, DictRecord> e = (Entry)var10.next();
                    DictRecord dictRecord = (DictRecord)e.getValue();
                    if (dictRecord.keyName != null) {
                        keyMap.put((String)e.getKey(), dictRecord.keyName);
                    }
                }
            }
        }

        return keyMap;
    }

    public static DictRecord getDictRecord(ResultSet rs) throws Exception {
        DictRecord dictRecord = new DictRecord();
        dictRecord.linkTo = StringUtil.force(rs.getString("LINK_TO"));
        dictRecord.dispText = StringUtil.force(rs.getString("DISP_TEXT"));
        dictRecord.dispWidth = rs.getInt("DISP_WIDTH");
        if (rs.wasNull()) {
            dictRecord.dispWidth = -1;
        }

        dictRecord.dispFormat = StringUtil.force(rs.getString("DISP_FORMAT"));
        dictRecord.noList = StringUtil.getBool(rs.getString("NO_LIST"));
        dictRecord.noEdit = StringUtil.getBool(rs.getString("NO_EDIT"));
        dictRecord.autoWrap = StringUtil.getBool(rs.getString("AUTO_WRAP"));
        dictRecord.noBlank = StringUtil.getBool(rs.getString("NO_BLANK"));
        dictRecord.readOnly = StringUtil.getBool(rs.getString("READ_ONLY"));
        dictRecord.keyName = StringUtil.force(rs.getString("KEY_NAME"));
        dictRecord.fieldSize = rs.getInt("FIELD_SIZE");
        if (rs.wasNull()) {
            dictRecord.fieldSize = -1;
        }

        dictRecord.decimalPrecision = rs.getInt("DECIMAL_PRECISION");
        if (rs.wasNull()) {
            dictRecord.decimalPrecision = -1;
        }

        dictRecord.validator = StringUtil.force(rs.getString("VALIDATOR"));
        if (dictRecord.validator != null) {
            dictRecord.validator = dictRecord.validator.trim();
        }

        dictRecord.renderer = StringUtil.force(rs.getString("RENDERER"));
        if (dictRecord.renderer != null) {
            dictRecord.renderer = dictRecord.renderer.trim();
        }

        return dictRecord;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void load() {
        try {
            lock.lock();
            buffer.clear();
            Connection conn = null;
            Statement st = null;
            ResultSet rs = null;
            String tableName = null;
            String preTableName = null;
            ConcurrentHashMap map = new ConcurrentHashMap();

            try {
                conn = DbUtil.getConnection();
                st = conn.createStatement();

                for (rs = st.executeQuery("select * from WB_DICT order by TABLE_NAME"); rs.next();
                    preTableName = tableName) {
                    tableName = rs.getString("TABLE_NAME").toUpperCase();
                    if (preTableName != null && !preTableName.equals(tableName)) {
                        buffer.put(preTableName, map);
                        map = new ConcurrentHashMap();
                    }

                    map.put(rs.getString("FIELD_NAME").toUpperCase(), getDictRecord(rs));
                }

                if (preTableName != null) {
                    buffer.put(preTableName, map);
                }
            } finally {
                DbUtil.close(rs);
                DbUtil.close(st);
                DbUtil.close(conn);
            }

        } catch (Throwable var10) {
            throw new RuntimeException(var10);
        } finally {
            lock.unlock();
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
        Map<String, DictionaryCache> buffermap = applicationContext.getBeansOfType(DictionaryCache.class);
        if (null != buffermap) {
            for (Entry<String, DictionaryCache> entry : buffermap.entrySet()) {
                buffer = entry.getValue();
                break;
            }
        }
        Map<String, DictionaryLock> lockmap = applicationContext.getBeansOfType(DictionaryLock.class);
        if (null != lockmap) {
            for (Entry<String, DictionaryLock> entry : lockmap.entrySet()) {
                lock = entry.getValue();
                break;
            }
        }
    }
}