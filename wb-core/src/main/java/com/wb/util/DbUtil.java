package com.wb.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.wb.common.Base;
import com.wb.common.Dictionary;
import com.wb.common.KVBuffer;
import com.wb.common.ScriptBuffer;
import com.wb.common.Var;
import com.wb.tool.DataOutput;
import com.wb.tool.DictRecord;
import com.wb.tool.Encrypter;
import com.wb.tool.Query;
import com.wb.tool.Updater;

public class DbUtil implements ApplicationContextAware {
    public static final Object[][] sqlTypes = new Object[][] {{"BIT", -7}, {"TINYINT", -6}, {"SMALLINT", 5},
        {"INTEGER", 4}, {"BIGINT", -5}, {"FLOAT", 6}, {"REAL", 7}, {"DOUBLE", 8}, {"NUMERIC", 2}, {"DECIMAL", 3},
        {"CHAR", 1}, {"VARCHAR", 12}, {"LONGVARCHAR", -1}, {"DATE", 91}, {"TIME", 92}, {"TIMESTAMP", 93},
        {"BINARY", -2}, {"VARBINARY", -3}, {"LONGVARBINARY", -4}, {"NULL", 0}, {"OTHER", 1111}, {"JAVA_OBJECT", 2000},
        {"DISTINCT", 2001}, {"STRUCT", 2002}, {"ARRAY", 2003}, {"BLOB", 2004}, {"CLOB", 2005}, {"REF", 2006},
        {"DATALINK", 70}, {"BOOLEAN", 16}, {"ROWID", -8}, {"NCHAR", -15}, {"NVARCHAR", -9}, {"LONGNVARCHAR", -16},
        {"NCLOB", 2011}, {"SQLXML", 2009}};
    // private static HashMap<Connection, Object[]> connectionMap = new HashMap();
    /**
     * 默认的数据源
     */
    private static DataSource defaultDataSource = null;
    /**
     * 其它数据源
     */
    private static Map<String, DataSource> targetDataSources = new HashMap<String, DataSource>();
    public static boolean usePagingData = true;

    public static Integer getFieldType(String name) {
        if (StringUtil.isEmpty(name)) {
            return 12;
        } else {
            int j = sqlTypes.length;

            for (int i = 0; i < j; ++i) {
                if (name.equalsIgnoreCase((String)sqlTypes[i][0])) {
                    return (Integer)sqlTypes[i][1];
                }
            }
            return StringUtil.isInteger(name) ? Integer.parseInt(name) : null;
        }
    }

    public static String getTypeName(int type) {
        int j = sqlTypes.length;
        switch (type) {
            case -15:
            case -9:
            case 1:
            case 12:
                return null;
            default:
                for (int i = 0; i < j; ++i) {
                    if (type == (Integer)sqlTypes[i][1]) {
                        return ((String)sqlTypes[i][0]).toLowerCase();
                    }
                }
                return Integer.toString(type);
        }
    }

    public static void importData(Connection connection, String tableName, JSONArray ja) throws Exception {
        importData(connection, tableName, (Object)ja, (String[])null, ' ');
    }

    public static void importData(Connection connection, String tableName, BufferedReader reader) throws Exception {
        importData(connection, tableName, (BufferedReader)reader, (String[])null, ' ');
    }

    public static void importData(Connection connection, String tableName, BufferedReader reader, String[] fieldList,
        char fieldSeparator) throws Exception {
        importData(connection, tableName, (Object)reader, fieldList, fieldSeparator);
    }

    private static void importData(Connection connection, String tableName, Object data, String[] fieldList,
        char fieldSeparator) throws Exception {
        ResultSet rs = null;
        PreparedStatement st = null;
        boolean jsonFormat = fieldList == null;
        BufferedReader reader = data instanceof BufferedReader ? (BufferedReader)data : null;
        try {
            st = connection
                .prepareStatement(StringUtil.concat(new String[] {"select * from ", tableName, " where 1=0"}));
            rs = st.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int j = meta.getColumnCount();
            int[] indexList;
            if (jsonFormat) {
                indexList = null;
            } else {
                indexList = new int[j];
            }
            int[] types = new int[j];
            String[] fieldNames = new String[j];
            String[] quoteNames = new String[j];

            int i;
            for (i = 0; i < j; ++i) {
                int k = i + 1;
                types[i] = meta.getColumnType(k);
                fieldNames[i] = meta.getColumnLabel(k);
                fieldNames[i] = getFieldName(fieldNames[i]);
                quoteNames[i] = StringUtil.quoteIf(fieldNames[i]);
                if (!jsonFormat) {
                    indexList[i] = StringUtil.indexOf(fieldList, fieldNames[i]);
                }
            }
            close(rs);
            close(st);
            st = connection.prepareStatement(StringUtil.concat(new String[] {"insert into ", tableName, "(",
                StringUtil.join(quoteNames, ","), ") values (?", StringUtil.repeat(",?", j - 1), ")"}));
            JSONObject record;
            String line;
            if (data instanceof JSONArray) {
                JSONArray ja = (JSONArray)data;
                int n = ja.length();

                for (int m = 0; m < n; ++m) {
                    record = ja.optJSONObject(m);

                    for (i = 0; i < j; ++i) {
                        setObject(st, i + 1, types[i], JsonUtil.opt(record, fieldNames[i]));
                    }
                    addBatch(st);
                }
            } else {
                for (; (line = reader.readLine()) != null; addBatch(st)) {
                    if (jsonFormat) {
                        record = new JSONObject(line);

                        for (i = 0; i < j; ++i) {
                            setObject(st, i + 1, types[i], JsonUtil.opt(record, fieldNames[i]));
                        }
                    } else {
                        String[] values = StringUtil.split(line, fieldSeparator);

                        for (i = 0; i < j; ++i) {
                            String value = indexList[i] == -1 ? null : values[indexList[i]];
                            setObject(st, i + 1, types[i], value);
                        }
                    }
                }
            }
            executeBatch(st);
        } finally {
            close(rs);
            close(st);
            if (reader != null) {
                reader.close();
            }
        }
    }

    public static void exportData(ResultSet rs, Writer writer) throws Exception {
        ResultSetMetaData meta = rs.getMetaData();
        boolean newLine = false;
        int j = meta.getColumnCount();
        int[] types = new int[j];
        String[] names = new String[j];
        int i;
        for (i = 0; i < j; ++i) {
            types[i] = meta.getColumnType(i + 1);
            names[i] = meta.getColumnLabel(i + 1);
            names[i] = getFieldName(names[i]);
        }
        while (rs.next()) {
            if (newLine) {
                writer.write(10);
            } else {
                newLine = true;
            }
            writer.write(123);

            for (i = 0; i < j; ++i) {
                int k = i + 1;
                if (i > 0) {
                    writer.write(44);
                }
                writer.write(StringUtil.quote(names[i]));
                writer.write(58);
                if (isBlobField(types[i])) {
                    InputStream stream = rs.getBinaryStream(k);

                    try {
                        writer.write(StringUtil.encode(stream));
                    } finally {
                        IOUtils.closeQuietly(stream);
                    }
                } else {
                    writer.write(StringUtil.encode(getObject(rs, k, types[i])));
                }
            }
            writer.write(125);
        }
        writer.flush();
    }

    public static void exportExcel(ResultSet rs, OutputStream outputStream, JSONArray headers, String title,
        String keys, String dictTableNames) throws Exception {
        if (headers == null) {
            headers = getHeaders(rs, dictTableNames);
        }

        DataOutput.outputExcel(outputStream, headers, getData(rs, keys, dictTableNames), title,
            new JSONObject("{mergeInfo:[]}"), "Y-m-d", "H:i:s", false);
    }

    public static JSONArray getHeaders(ResultSet rs, String dictTableNames) throws Exception {
        ResultSetMetaData meta = rs.getMetaData();
        int j = meta.getColumnCount();
        String[] tables = null;
        JSONArray headers = new JSONArray();
        boolean hasDict = dictTableNames != null;
        if (hasDict) {
            tables = dictTableNames.split(",");
        }

        for (int i = 0; i < j; ++i) {
            String fieldName = meta.getColumnLabel(i + 1);
            fieldName = getFieldName(fieldName);
            DictRecord dictRecord;
            if (hasDict) {
                dictRecord = Dictionary.find(tables, fieldName);
            } else {
                dictRecord = null;
            }
            JSONObject object = new JSONObject();
            object.put("field", fieldName);
            if (dictRecord == null) {
                object.put("text", fieldName);
                int size = meta.getColumnDisplaySize(i + 1);
                size = Math.max(size, fieldName.length() + 3);
                if (size < 5) {
                    size = 5;
                }
                if (size > 18) {
                    size = 18;
                }
                object.put("width", size * 10);
            } else {
                object.put("text", dictRecord.dispText);
                object.put("width", dictRecord.dispWidth);
            }
            headers.put(object);
        }

        return headers;
    }

    public static void addBatch(PreparedStatement statement) throws SQLException {
        if (Var.batchUpdate) {
            statement.addBatch();
        } else {
            statement.executeUpdate();
        }
    }

    public static int[] executeBatch(PreparedStatement statement) throws SQLException {
        return Var.batchUpdate ? statement.executeBatch() : null;
    }

    public static Connection getConnection() throws Exception {
        return getConnection("");
    }

    public static Connection getConnection(HttpServletRequest request) throws Exception {
        return getConnection(request, (String)null);
    }

    public static Connection getConnection(String jndi) throws Exception {
        DataSource dataSource = null;
        if (StringUtil.isEmpty(jndi)) {
            dataSource = defaultDataSource;
        } else {
            dataSource = targetDataSources.get(jndi);
        }
        if (null == dataSource) {
            dataSource = defaultDataSource;
        }
        return defaultDataSource.getConnection();
        /**
         * if (StringUtil.isEmpty(jndi)) { jndi = Var.jndi; } else { jndi = Var.getString("sys.jndi." + jndi); }
         * 
         * InitialContext context = new InitialContext(); DataSource ds = (DataSource)context.lookup(jndi); Connection
         * conn = ds.getConnection(); if (Var.debug) { detectConnnection(conn); }
         * 
         * return conn;
         **/
    }

    public static List<String> datsSourceKeys() {
        List<String> keys = new ArrayList<String>();
        keys.add("default");
        keys.addAll(targetDataSources.keySet());
        return keys;
    }

    public static Connection getConnection(HttpServletRequest request, String jndi) throws Exception {
        String storeName;
        if (StringUtil.isEmpty(jndi)) {
            storeName = "conn@@";
        } else {
            storeName = "conn@@" + jndi;
        }

        Object obj = WebUtil.getObject(request, storeName);
        Connection conn;
        if (obj == null) {
            conn = getConnection(jndi);
            WebUtil.setObject(request, storeName, conn);
        } else {
            conn = (Connection)obj;
        }

        return conn;
    }

    public static void startTransaction(Connection connection, String isolation) throws Exception {
        if (!connection.getAutoCommit()) {
            connection.commit();
        }

        connection.setAutoCommit(false);
        if (!StringUtil.isEmpty(isolation)) {
            if (isolation.equals("readUncommitted")) {
                connection.setTransactionIsolation(1);
            } else if (isolation.equals("readCommitted")) {
                connection.setTransactionIsolation(2);
            } else if (isolation.equals("repeatableRead")) {
                connection.setTransactionIsolation(4);
            } else if (isolation.equals("serializable")) {
                connection.setTransactionIsolation(8);
            }
        }

    }

    private static void closeResult(ResultSet resultSet) {
        try {
            resultSet.close();
            resultSet = null;
        } catch (Throwable var2) {
        }
    }

    private static void closeStatement(Statement statement) {
        try {
            statement.close();
            statement = null;
        } catch (Throwable var2) {
        }
    }

    private static void closeConnection(Connection connection, boolean rollback) {
        try {
            if (connection.isClosed()) {
                return;
            }
            try {
                if (!connection.getAutoCommit()) {
                    if (rollback) {
                        connection.rollback();
                    } else {
                        connection.commit();
                    }
                }
            } catch (Throwable var7) {
                if (!rollback) {
                    connection.rollback();
                }
            } finally {
                connection.close();
                connection = null;
            }
        } catch (Throwable var9) {
        }
    }

    public static void close(Object object) {
        if (object instanceof ResultSet) {
            closeResult((ResultSet)object);
        } else if (object instanceof Statement) {
            closeStatement((Statement)object);
        } else if (object instanceof Connection) {
            closeConnection((Connection)object, true);
        }
    }

    public static void closeCommit(Connection connection) {
        if (connection != null) {
            closeConnection(connection, false);
        }
    }

    public static boolean isBlobField(int type) {
        switch (type) {
            case -4:
            case -3:
            case -2:
            case 2004:
                return true;
            default:
                return false;
        }
    }

    public static boolean isTextField(int type) {
        switch (type) {
            case -16:
            case -1:
            case 2005:
            case 2011:
                return true;
            default:
                return false;
        }
    }

    public static boolean isStringField(int type) {
        switch (type) {
            case -15:
            case -9:
            case 1:
            case 12:
                return true;
            default:
                return false;
        }
    }

    public static boolean maybeFloatField(int type) {
        switch (type) {
            case 2:
            case 3:
            case 6:
            case 7:
            case 8:
                return true;
            case 4:
            case 5:
            default:
                return false;
        }
    }

    public static void loadFirstRow(HttpServletRequest request, ResultSet resultSet, String prefix) throws Exception {
        if (resultSet.next()) {
            ResultSetMetaData meta = resultSet.getMetaData();
            int j = meta.getColumnCount();
            boolean hasPrefix = !StringUtil.isEmpty(prefix);
            for (int i = 1; i <= j; ++i) {
                String name = meta.getColumnLabel(i);
                name = getFieldName(name);
                if (hasPrefix) {
                    name = StringUtil.concat(new String[] {prefix, ".", name});
                }
                Object object = getObject(resultSet, i, meta.getColumnType(i));
                if (object instanceof ResultSet || object instanceof InputStream) {
                    WebUtil.setObject(request, SysUtil.getId(), object);
                }
                request.setAttribute(name, object);
            }
        }
    }

    public static HashMap<String, String> getMap(String sql) throws Exception {
        ResultSet rs = null;
        Statement st = null;
        Connection conn = null;
        HashMap result = new HashMap();
        try {
            conn = getConnection();
            st = conn.createStatement();
            rs = st.executeQuery(sql);
            while (rs.next()) {
                result.put(rs.getString(1), rs.getString(2));
            }
        } finally {
            close(rs);
            close(st);
            close(conn);
        }
        return result;
    }

    public static String getTypeCategory(int type) {
        switch (type) {
            case -7:
            case -6:
            case -5:
            case 4:
            case 5:
            case 16:
                return "int";
            case 2:
            case 3:
            case 6:
            case 7:
            case 8:
                return "float";
            case 91:
            case 92:
            case 93:
                return "date";
            default:
                return "string";
        }
    }

    public static String getText(ResultSet rs, int index) throws Exception {
        return (String)getObject((ResultSet)rs, index, -1);
    }

    public static String getText(ResultSet rs, String fieldName) throws Exception {
        return (String)getObject(rs, fieldName, -1);
    }

    public static void setText(PreparedStatement statement, int index, String value) throws Exception {
        setObject(statement, index, -1, value);
    }

    public static Object getObject(CallableStatement statement, int index, int type) throws Exception {
        Object obj;
        switch (type) {
            case -16:
            case -1:
            case 2005:
            case 2011:
                Reader rd = statement.getCharacterStream(index);
                if (rd == null) {
                    obj = null;
                } else {
                    obj = SysUtil.readString(rd);
                }
                break;
            case -15:
            case -9:
            case 1:
            case 12:
                obj = statement.getString(index);
                break;
            case -7:
            case 16:
                obj = statement.getBoolean(index) ? 1 : 0;
                break;
            case -6:
                obj = statement.getByte(index);
                break;
            case -5:
                obj = statement.getLong(index);
                break;
            case 2:
            case 3:
                obj = statement.getBigDecimal(index);
                break;
            case 4:
                obj = statement.getInt(index);
                break;
            case 5:
                obj = statement.getShort(index);
                break;
            case 6:
            case 7:
                obj = statement.getFloat(index);
                break;
            case 8:
                obj = statement.getDouble(index);
                break;
            case 91:
                obj = statement.getDate(index);
                break;
            case 92:
                obj = statement.getTime(index);
                break;
            case 93:
                try {
                    obj = statement.getTimestamp(index);
                    break;
                } catch (Throwable var6) {
                    String ts = statement.getString(index);
                    if (StringUtil.isEmpty(ts)) {
                        return null;
                    }

                    return Timestamp.valueOf(ts);
                }
            default:
                obj = statement.getObject(index);
        }

        return statement.wasNull() ? null : obj;
    }

    public static Object getObject(ResultSet rs, int index, int type) throws Exception {
        Object obj;
        switch (type) {
            case -16:
            case -1:
            case 2005:
            case 2011:
                Reader rd = rs.getCharacterStream(index);
                if (rd == null) {
                    obj = null;
                } else {
                    obj = SysUtil.readString(rd);
                }
                break;
            case -15:
            case -9:
            case 1:
            case 12:
                obj = rs.getString(index);
                break;
            case -7:
            case 16:
                obj = rs.getBoolean(index) ? 1 : 0;
                break;
            case -6:
                obj = rs.getByte(index);
                break;
            case -5:
                obj = rs.getLong(index);
                break;
            case -4:
            case -3:
            case -2:
            case 2004:
                InputStream is = rs.getBinaryStream(index);
                if (is != null) {
                    is.close();
                }

                obj = "(blob)";
                break;
            case 2:
            case 3:
                obj = rs.getBigDecimal(index);
                break;
            case 4:
                obj = rs.getInt(index);
                break;
            case 5:
                obj = rs.getShort(index);
                break;
            case 6:
            case 7:
                obj = rs.getFloat(index);
                break;
            case 8:
                obj = rs.getDouble(index);
                break;
            case 91:
                obj = rs.getDate(index);
                break;
            case 92:
                obj = rs.getTime(index);
                break;
            case 93:
                try {
                    obj = rs.getTimestamp(index);
                    break;
                } catch (Throwable var6) {
                    String ts = rs.getString(index);
                    if (StringUtil.isEmpty(ts)) {
                        return null;
                    }

                    return Timestamp.valueOf(ts);
                }
            default:
                obj = rs.getObject(index);
        }

        return rs.wasNull() ? null : obj;
    }

    public static Object getObject(ResultSet rs, String fieldName, int type) throws Exception {
        Object obj;
        switch (type) {
            case -16:
            case -1:
            case 2005:
            case 2011:
                Reader rd = rs.getCharacterStream(fieldName);
                if (rd == null) {
                    obj = null;
                } else {
                    obj = SysUtil.readString(rd);
                }
                break;
            case -15:
            case -9:
            case 1:
            case 12:
                obj = rs.getString(fieldName);
                break;
            case -7:
            case 16:
                obj = rs.getBoolean(fieldName) ? 1 : 0;
                break;
            case -6:
                obj = rs.getByte(fieldName);
                break;
            case -5:
                obj = rs.getLong(fieldName);
                break;
            case -4:
            case -3:
            case -2:
            case 2004:
                InputStream is = rs.getBinaryStream(fieldName);
                if (is != null) {
                    is.close();
                }

                obj = "(blob)";
                break;
            case 2:
            case 3:
                obj = rs.getBigDecimal(fieldName);
                break;
            case 4:
                obj = rs.getInt(fieldName);
                break;
            case 5:
                obj = rs.getShort(fieldName);
                break;
            case 6:
            case 7:
                obj = rs.getFloat(fieldName);
                break;
            case 8:
                obj = rs.getDouble(fieldName);
                break;
            case 91:
                obj = rs.getDate(fieldName);
                break;
            case 92:
                obj = rs.getTime(fieldName);
                break;
            case 93:
                try {
                    obj = rs.getTimestamp(fieldName);
                    break;
                } catch (Throwable var6) {
                    String ts = rs.getString(fieldName);
                    if (StringUtil.isEmpty(ts)) {
                        return null;
                    }

                    return Timestamp.valueOf(ts);
                }
            default:
                obj = rs.getObject(fieldName);
        }

        return rs.wasNull() ? null : obj;
    }

    public static void setObject(PreparedStatement statement, int index, int type, Object object) throws Exception {
        if (object != null && !(object instanceof String)) {
            if (object instanceof InputStream) {
                statement.setBinaryStream(index, (InputStream)object, ((InputStream)object).available());
            } else if (object instanceof Date) {
                statement.setTimestamp(index, new Timestamp(((Date)object).getTime()));
            } else if (object instanceof Double && !maybeFloatField(type)) {
                Object object1 = ((Double)object).intValue();
                statement.setObject(index, object1, type);
            } else {
                statement.setObject(index, object, type);
            }
        } else {
            String value;
            if (object == null) {
                value = null;
            } else {
                value = (String)object;
            }

            if (StringUtil.isEmpty(value)) {
                statement.setNull(index, type);
            } else {
                switch (type) {
                    case -16:
                    case -1:
                    case 2005:
                    case 2011:
                        statement.setCharacterStream(index, new StringReader(value), value.length());
                        break;
                    case -15:
                    case -9:
                    case 1:
                    case 12:
                        if (Var.emptyString.equals(value)) {
                            statement.setString(index, "");
                        } else {
                            statement.setString(index, value);
                        }
                        break;
                    case -7:
                    case 16:
                        statement.setBoolean(index, StringUtil.getBool(value));
                        break;
                    case -6:
                        statement.setByte(index, Byte.parseByte(StringUtil.convertBool(value)));
                        break;
                    case -5:
                        statement.setLong(index, Long.parseLong(StringUtil.convertBool(value)));
                        break;
                    case -4:
                    case -3:
                    case -2:
                    case 2004:
                        InputStream is = new ByteArrayInputStream(StringUtil.decodeBase64(value));
                        statement.setBinaryStream(index, is, is.available());
                        break;
                    case 2:
                    case 3:
                        statement.setBigDecimal(index, new BigDecimal(StringUtil.convertBool(value)));
                        break;
                    case 4:
                        statement.setInt(index, Integer.parseInt(StringUtil.convertBool(value)));
                        break;
                    case 5:
                        statement.setShort(index, Short.parseShort(StringUtil.convertBool(value)));
                        break;
                    case 6:
                    case 7:
                        statement.setFloat(index, Float.parseFloat(StringUtil.convertBool(value)));
                        break;
                    case 8:
                        statement.setDouble(index, Double.parseDouble(StringUtil.convertBool(value)));
                        break;
                    case 91:
                        if (value.indexOf(32) != -1) {
                            statement.setTimestamp(index, Timestamp.valueOf(DateUtil.fixTimestamp(value, false)));
                        } else {
                            statement.setDate(index, java.sql.Date.valueOf(DateUtil.fixTimestamp(value, true)));
                        }
                        break;
                    case 92:
                        if (value.indexOf(32) != -1) {
                            statement.setTimestamp(index, Timestamp.valueOf(DateUtil.fixTimestamp(value, false)));
                        } else {
                            statement.setTime(index, Time.valueOf(DateUtil.fixTime(value)));
                        }
                        break;
                    case 93:
                        statement.setTimestamp(index, Timestamp.valueOf(DateUtil.fixTimestamp(value, false)));
                        break;
                    default:
                        statement.setObject(index, value, type);
                }
            }
        }

    }

    public static Object run(HttpServletRequest request, String sql, String jndi, String loadParams,
        boolean returnStatement) throws Exception {
        Query query = new Query();
        query.request = request;
        query.sql = sql;
        query.jndi = jndi;
        query.loadParams = loadParams;
        query.returnStatement = returnStatement;
        return query.run();
    }

    public static Object run(HttpServletRequest request, String sql, String jndi) throws Exception {
        Query query = new Query();
        query.request = request;
        query.sql = sql;
        query.jndi = jndi;
        return query.run();
    }

    public static Object run(HttpServletRequest request, String sql) throws Exception {
        return run(request, sql, (String)null);
    }

    public static void update(HttpServletRequest request, String tableName, String mode) throws Exception {
        Updater updater = new Updater();
        updater.request = request;
        updater.tableName = tableName;
        updater.mode = mode;
        updater.run();
    }

    public static String getFieldName(String fieldName) {
        if (Var.forceUpperCase) {
            return fieldName.startsWith("#") ? fieldName.substring(1) : fieldName.toUpperCase();
        } else {
            return fieldName;
        }
    }

    public static JSONArray getFields(ResultSetMetaData meta, String[] dictTableNames, JSONObject dictFieldsMap,
        JSONObject keyDefines) throws Exception {
        int j = meta.getColumnCount();
        String[] mapTable = new String[1];
        JSONArray ja = new JSONArray();
        boolean hasDict = dictTableNames != null;

        for (int i = 0; i < j; ++i) {
            int k = i + 1;
            JSONObject jo = new JSONObject();
            String name = meta.getColumnLabel(k);
            name = getFieldName(name);
            if (StringUtil.isEmpty(name)) {
                name = "FIELD" + Integer.toString(k);
            }

            int type = meta.getColumnType(k);
            String category;
            if (keyDefines != null && keyDefines.has(name)) {
                category = "string";
            } else {
                category = getTypeCategory(type);
            }

            String format;
            switch (type) {
                case 91:
                    format = "Y-m-d";
                    break;
                case 92:
                    format = "H:i:s";
                    break;
                case 93:
                    format = "Y-m-d H:i:s.u";
                    break;
                default:
                    format = null;
            }

            jo.put("name", name);
            jo.put("type", category);
            if (format != null) {
                jo.put("dateFormat", format);
            }

            if (category.equals("string")) {
                jo.put("useNull", false);
            }

            ja.put(jo);
            if (hasDict) {
                DictRecord dictRecord = null;
                if (dictFieldsMap != null) {
                    mapTable[0] = dictFieldsMap.optString(name);
                    if (!StringUtil.isEmpty(mapTable[0])) {
                        dictRecord = Dictionary.find(mapTable, name);
                    }
                }

                if (dictRecord == null) {
                    dictRecord = Dictionary.find(dictTableNames, name);
                }

                if (dictRecord != null && dictRecord.keyName != null) {
                    jo = new JSONObject();
                    jo.put("name", name + "__V");
                    jo.put("type", "string");
                    jo.put("useNull", false);
                    ja.put(jo);
                }
            }
        }

        return ja;
    }

    public static String getColumns(ResultSetMetaData meta, String[] dictTableNames, JSONObject dictFieldsMap,
        JSONObject keyDefines) throws Exception {
        int j = meta.getColumnCount();
        String[] mapTable = new String[1];
        String keyItems = null;
        StringBuilder buf = new StringBuilder();
        DictRecord fieldDict = null;
        buf.append('[');
        buf.append("{\"xtype\":\"rownumberer\"}");

        for (int i = 0; i < j; ++i) {
            int index;
            int len;
            int fieldNameLen;
            String category;
            String editor;
            String precision;
            String scale;
            boolean isDateTime;
            boolean hasRenderer;
            boolean hasFieldDict;
            boolean hasKeyName;
            boolean addExtracConfig;
            label323:
            {
                addExtracConfig = false;
                index = i + 1;
                String fieldName = meta.getColumnLabel(index);
                fieldName = getFieldName(fieldName);
                if (StringUtil.isEmpty(fieldName)) {
                    fieldName = "FIELD" + Integer.toString(index);
                }

                fieldDict = null;
                if (dictFieldsMap != null) {
                    mapTable[0] = dictFieldsMap.optString(fieldName);
                    if (!StringUtil.isEmpty(mapTable[0])) {
                        fieldDict = Dictionary.find(mapTable, fieldName);
                    }
                }

                if (fieldDict == null && dictTableNames != null) {
                    fieldDict = Dictionary.find(dictTableNames, fieldName);
                }

                hasFieldDict = fieldDict != null;
                hasKeyName = hasFieldDict && fieldDict.keyName != null;
                int type;
                if (keyDefines != null && keyDefines.has(fieldName)) {
                    len = 200;
                    precision = "200";
                    scale = "0";
                    type = 12;
                } else {
                    if (hasKeyName) {
                        len = 10;
                    } else {
                        len = meta.getPrecision(index);
                        if (len <= 0) {
                            len = 100;
                        }
                    }

                    precision = Integer.toString(len);
                    int scaleNum = meta.getScale(index);
                    if (scaleNum < 0) {
                        scaleNum = 100;
                    }

                    scale = Integer.toString(scaleNum);
                    type = meta.getColumnType(index);
                    if ((type == -9 || type == 12) && len > Var.stringAsText) {
                        type = 2005;
                    }
                }

                fieldNameLen = fieldName.length();
                fieldName = StringUtil.quote(fieldName);
                buf.append(',');
                buf.append("{\"dataIndex\":");
                buf.append(fieldName);
                buf.append(",\"text\":");
                buf.append(
                    hasFieldDict && fieldDict.dispText != null ? StringUtil.quote(fieldDict.dispText) : fieldName);
                editor = null;
                isDateTime = false;
                hasRenderer = hasFieldDict && (fieldDict.renderer != null || hasKeyName);
                switch (type) {
                    case -16:
                    case -1:
                    case 2005:
                    case 2011:
                        category = "text";
                        editor = "\"textarea\",\"height\":120";
                        len = 18;
                        if (!hasRenderer) {
                            buf.append(",\"renderer\":\"Wb.clobRenderer\"");
                        }
                        break label323;
                    case -7:
                    case 16:
                        category = "number";
                        if (!hasKeyName) {
                            buf.append(",\"align\":\"right\"");
                        }

                        editor = "\"numberfield\",\"maxValue\":1,\"minValue\":0";
                        break label323;
                    case -6:
                    case -5:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                    case 8:
                        category = "number";
                        if (!hasKeyName) {
                            buf.append(",\"align\":\"right\"");
                        }

                        if (hasFieldDict) {
                            String dictSize;
                            if (fieldDict.fieldSize == -1) {
                                dictSize = precision;
                            } else {
                                dictSize = Integer.toString(fieldDict.fieldSize);
                            }

                            String dictScale;
                            if (fieldDict.decimalPrecision == -1) {
                                dictScale = scale;
                            } else {
                                dictScale = Integer.toString(fieldDict.decimalPrecision);
                            }

                            editor =
                                StringUtil.concat(new String[] {"\"numberfield\",\"decimalPrecision\":", dictScale});
                            if (fieldDict.validator == null) {
                                editor = StringUtil.concat(new String[] {editor, ",\"validator\":\"Wb.numValidator(",
                                    dictSize, ",", dictScale, ")\""});
                            }
                        } else {
                            editor = StringUtil.concat(new String[] {"\"numberfield\",\"decimalPrecision\":", scale,
                                ",\"validator\":\"Wb.numValidator(", precision, ",", scale, ")\""});
                        }
                        break label323;
                    case -4:
                    case -3:
                    case -2:
                    case 2004:
                        category = "blob";
                        editor = "\"filefield\"";
                        len = 16;
                        if (!hasRenderer) {
                            buf.append(",\"renderer\":\"Wb.blobRenderer\"");
                        }
                        break label323;
                    case 91:
                        category = "date";
                        editor = "\"datefield\"";
                        isDateTime = true;
                        len = 12;
                        break label323;
                    case 92:
                        category = "time";
                        editor = "\"timefield\"";
                        isDateTime = true;
                        len = 10;
                        if (!hasRenderer) {
                            buf.append(",\"renderer\":\"Wb.timeRenderer\"");
                        }
                        break label323;
                    case 93:
                        category = "timestamp";
                        editor = "\"datetimefield\"";
                        isDateTime = true;
                        len = 18;
                        break label323;
                }

                category = "string";
                editor = "\"textfield\",\"maxLength\":"
                    + (hasFieldDict && fieldDict.fieldSize != -1 ? Integer.toString(fieldDict.fieldSize) : precision);
            }

            if (hasFieldDict) {
                if (isDateTime && fieldDict.fieldSize != -1) {
                    switch (fieldDict.fieldSize) {
                        case 1:
                            editor = "\"datefield\"";
                            len = 12;
                            break;
                        case 2:
                            editor = "\"timefield\"";
                            len = 10;
                            break;
                        case 3:
                            category = "timestamp";
                            editor = "\"datetimefield\"";
                            len = 18;
                    }
                }

                if (fieldDict.noList) {
                    buf.append(",\"hidden\":true,\"showInMenu\":false");
                }

                if (fieldDict.dispFormat != null) {
                    buf.append(",\"format\":");
                    buf.append(StringUtil.quote(fieldDict.dispFormat));
                }

                if (hasRenderer) {
                    if (fieldDict.renderer != null) {
                        if (fieldDict.renderer.startsWith("{")) {
                            addExtracConfig = true;
                        } else {
                            buf.append(
                                ",\"renderer\":\"(function(value,metaData,record,rowIndex,colIndex,store,view){");
                            buf.append(StringUtil.text(fieldDict.renderer));
                            buf.append("})\"");
                        }
                    } else {
                        buf.append(",\"renderer\":\"Wb.kvRenderer\"");
                    }
                }

                if (fieldDict.autoWrap) {
                    buf.append(",\"autoWrap\":true");
                }
            }

            buf.append(",\"category\":\"");
            buf.append(category);
            if (hasFieldDict && fieldDict.dispWidth != -1) {
                if (fieldDict.dispWidth < 10) {
                    buf.append("\",\"flex\":");
                    buf.append(fieldDict.dispWidth);
                } else {
                    buf.append("\",\"width\":");
                    buf.append(fieldDict.dispWidth);
                }
            } else {
                buf.append("\",\"width\":");
                len = Math.max(len, fieldNameLen + 3);
                if (len < 5) {
                    len = 5;
                }

                if (len > 18) {
                    len = 18;
                }

                buf.append(len * 10);
            }

            if (hasKeyName) {
                keyItems = KVBuffer.getList(fieldDict.keyName);
                buf.append(",\"keyName\":");
                buf.append(StringUtil.quote(fieldDict.keyName));
                buf.append(",\"keyItems\":");
                buf.append(keyItems);
            }

            if ((!hasFieldDict || !fieldDict.noEdit) && editor != null) {
                if (hasFieldDict && fieldDict.validator != null && fieldDict.validator.startsWith("{")) {
                    if ("blob".equals(category)) {
                        buf.append(",\"blobEditor\":{");
                    } else {
                        buf.append(",\"editor\":{");
                    }

                    JSONObject editorConfig = new JSONObject(fieldDict.validator);
                    if (editorConfig.has("validator")) {
                        buf.append("\"validator\":\"(function(value){");
                        buf.append(StringUtil.text(editorConfig.getString("validator")));
                        buf.append("})\",");
                        editorConfig.remove("validator");
                    }

                    String editorExtraItems = editorConfig.toString();
                    editorExtraItems = editorExtraItems.substring(1, editorExtraItems.length() - 1);
                    buf.append(editorExtraItems);
                    buf.append("},\"editable\":true");
                } else {
                    if ("blob".equals(category)) {
                        buf.append(",\"blobEditor\":{\"xtype\":");
                    } else {
                        buf.append(",\"editor\":{\"xtype\":");
                    }

                    if (hasKeyName) {
                        buf.append("\"combo\",\"keyName\":");
                        buf.append(StringUtil.quote(fieldDict.keyName));
                        buf.append(
                            ",\"displayField\":\"V\",\"valueField\":\"K\",\"forceSelection\":true,\"queryMode\":\"local\",\"store\":{\"fields\":[\"K\",\"V\"],\"sorters\":\"K\",\"data\":");
                        buf.append(keyItems);
                        buf.append('}');
                    } else {
                        buf.append(editor);
                    }

                    if (hasFieldDict) {
                        if (meta.isNullable(index) == 0 || fieldDict.noBlank) {
                            buf.append(",\"allowBlank\":false,\"required\":true");
                        }

                        if (Var.checkFieldReadOnly && meta.isReadOnly(index) || fieldDict.readOnly) {
                            buf.append(",\"readOnly\":true");
                        }

                        if (fieldDict.validator != null) {
                            buf.append(",\"validator\":\"(function(value){");
                            buf.append(StringUtil.text(fieldDict.validator));
                            buf.append("})\"");
                        }
                    } else {
                        if (meta.isNullable(index) == 0) {
                            buf.append(",\"allowBlank\":false,\"required\":true");
                        }

                        if (Var.checkFieldReadOnly && meta.isReadOnly(index)) {
                            buf.append(",\"readOnly\":true");
                        }
                    }

                    buf.append("},\"editable\":true");
                }
            }

            buf.append(",\"metaType\":\"");
            buf.append(meta.getColumnTypeName(index));
            buf.append("\",\"metaRequired\":");
            buf.append(meta.isNullable(index) == 0 ? "true" : "false");
            buf.append(",\"metaSize\":");
            buf.append(precision);
            buf.append(",\"metaScale\":");
            buf.append(scale);
            if (addExtracConfig) {
                JSONObject colConfig = new JSONObject(fieldDict.renderer);
                if (colConfig.has("renderer")) {
                    buf.append(",\"renderer\":\"(function(value,metaData,record,rowIndex,colIndex,store,view){");
                    buf.append(StringUtil.text(colConfig.getString("renderer")));
                    buf.append("})\"");
                    colConfig.remove("renderer");
                }

                String colExtraItems = colConfig.toString();
                colExtraItems = colExtraItems.substring(1, colExtraItems.length() - 1);
                buf.append(',');
                buf.append(colExtraItems);
            }

            buf.append('}');
        }

        buf.append(']');
        return buf.toString();
    }

    public static void preparePagingData() throws Exception {
        if (usePagingData) {
            usePagingData = false;
            File file = new File(Base.path, "wb/libs/ext/ux/data/PagingData.dat");
            ScriptBuffer.run(Encrypter.decrypt(FileUtil.readString(file), "basePath"), "PagingData.js");
        }

    }

    public static String getDbName(Connection conn) {
        try {
            DatabaseMetaData meta = conn.getMetaData();
            return meta.getDatabaseProductName();
        } catch (Throwable var2) {
            return null;
        }
    }

    public static void outputBlob(ResultSet resultSet, HttpServletRequest request, HttpServletResponse response,
        String contentType) throws Exception {
        Object inputStream = null;

        try {
            ResultSetMetaData meta = resultSet.getMetaData();
            int rowCount = meta.getColumnCount();
            String name = getFieldName(meta.getColumnLabel(1));
            String size = null;
            if (StringUtil.isEmpty(name)) {
                name = "blob";
            }

            response.reset();
            if (!resultSet.next()) {
                throw new Exception("Empty ResultSet.");
            }

            switch (rowCount) {
                case 1:
                    inputStream = resultSet.getBinaryStream(1);
                    break;
                case 2:
                    name = resultSet.getString(2);
                    inputStream = resultSet.getBinaryStream(1);
                    break;
                case 3:
                    name = resultSet.getString(2);
                    size = resultSet.getString(3);
                    inputStream = resultSet.getBinaryStream(1);
            }

            OutputStream outputStream = response.getOutputStream();
            if ("download".equals(contentType)) {
                contentType = "application/force-download";
            } else if ("stream".equals(contentType)) {
                contentType = "application/octet-stream";
            } else if ("image".equals(contentType)) {
                if (inputStream == null) {
                    File nullGif = new File(Base.path, "wb/images/null.png");
                    inputStream = new FileInputStream(nullGif);
                    size = Long.toString(nullGif.length());
                    contentType = "image/gif";
                } else {
                    String extName = FileUtil.getFileExt(name);
                    if (extName.isEmpty()) {
                        contentType = "image/jpg";
                    } else {
                        contentType = "image/" + extName;
                    }
                }
            }

            response.setHeader("content-type", contentType);
            response.setHeader("content-disposition", "attachment;" + WebUtil.encodeFilename(request, name));
            if (size != null) {
                response.setHeader("content-length", size);
            }

            if (inputStream != null) {
                IOUtils.copy((InputStream)inputStream, outputStream);
            }

            response.flushBuffer();
        } finally {
            IOUtils.closeQuietly((InputStream)inputStream);
        }

    }

    public static String[] buildSQLs(String jndi, String tableName, boolean ignoreBlob, int scriptType,
        HttpServletRequest request, JSONObject fields, JSONObject whereFields, JSONObject fieldsMap) throws Exception {
        String[] sqls = new String[4];
        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        StringBuilder selectFields = new StringBuilder();
        StringBuilder insertFields = new StringBuilder();
        StringBuilder insertParams = new StringBuilder();
        StringBuilder condition = new StringBuilder();
        StringBuilder updateParams = new StringBuilder();
        boolean isFirstSelect = true;
        boolean isFirstUpdate = true;
        boolean isFirstCondi = true;
        boolean hasRequest = request != null;
        boolean useDouble = Var.getBool("sys.db.useDouble");
        boolean whereUseDate = Var.getBool("sys.db.whereUseDate");
        boolean whereUseFloat = Var.getBool("sys.db.whereUseFloat");

        try {
            if (request == null) {
                conn = getConnection(jndi);
            } else {
                conn = getConnection(request, jndi);
            }

            st = conn.prepareStatement("select * from " + tableName + " where 1=0");
            rs = st.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int j = meta.getColumnCount() + 1;

            for (int i = 1; i < j; ++i) {
                int type = meta.getColumnType(i);
                String typeName = getTypeName(type);
                int precision = meta.getPrecision(i);
                if (precision <= 0) {
                    precision = 100;
                }

                boolean isText = isTextField(type) || precision > 10000;
                boolean isBlob = isBlobField(type);
                boolean isDateTime = type == 92 || type == 91 || type == 93;
                int scale = meta.getScale(i);
                if (scale < 0) {
                    scale = 100;
                }

                boolean isFloat = maybeFloatField(type) && scale > 0;
                if (isFloat && useDouble) {
                    type = 8;
                    typeName = "double";
                }

                boolean required = meta.isNullable(i) == 0;
                boolean readOnly = Var.checkFieldReadOnly && meta.isReadOnly(i);
                String fieldName = meta.getColumnLabel(i);
                fieldName = getFieldName(fieldName);
                String fieldValueName = fieldName;
                fieldName = StringUtil.quoteIf(fieldName);
                if (fieldsMap != null) {
                    String mapName = fieldsMap.optString(fieldValueName, (String)null);
                    if (mapName != null) {
                        fieldValueName = mapName;
                    }
                }

                if (!isBlob || !hasRequest || !StringUtil.isEmpty(WebUtil.fetch(request, fieldValueName))
                    || "1".equals(WebUtil.fetch(request, "$" + fieldValueName))) {
                    if ((!ignoreBlob || !isBlob)
                        && (fields == null || fields.has(fieldValueName) || fields.has("$" + fieldValueName))) {
                        if (isFirstSelect) {
                            isFirstSelect = false;
                        } else {
                            selectFields.append(',');
                        }

                        selectFields.append(fieldName);
                        if (!readOnly) {
                            if (isFirstUpdate) {
                                isFirstUpdate = false;
                            } else {
                                insertFields.append(',');
                                insertParams.append(',');
                                updateParams.append(',');
                            }

                            String param;
                            switch (scriptType) {
                                case 1:
                                    if (typeName == null) {
                                        param = StringUtil.concat(new String[] {"{?", fieldValueName, "?}"});
                                    } else {
                                        param =
                                            StringUtil.concat(new String[] {"{?", typeName, ".", fieldValueName, "?}"});
                                    }
                                    break;
                                case 2:
                                    param = StringUtil.concat(new String[] {"{#", fieldValueName, "#}"});
                                    break;
                                default:
                                    param = fieldValueName;
                            }

                            insertFields.append(fieldName);
                            insertParams.append(param);
                            updateParams.append(fieldName);
                            updateParams.append('=');
                            updateParams.append(param);
                        }
                    }

                    if (!isText && !isBlob && (!isFloat || whereUseFloat) && (!isDateTime || whereUseDate)
                        && (whereFields == null || whereFields.has(fieldValueName)
                            || whereFields.has("#" + fieldValueName))) {
                        if (isFirstCondi) {
                            isFirstCondi = false;
                        } else {
                            condition.append(" and ");
                        }

                        condition.append(getCondition(fieldName, fieldValueName, isStringField(type), typeName,
                            required, scriptType));
                    }
                }
            }
            preparePagingData();
            sqls[0] = StringUtil.concat(new String[] {"insert into ", tableName, " (", insertFields.toString(),
                ") values (", insertParams.toString(), ")"});
            sqls[1] = StringUtil.concat(
                new String[] {"update ", tableName, " set ", updateParams.toString(), " where ", condition.toString()});
            sqls[2] = StringUtil.concat(new String[] {"delete from ", tableName, " where ", condition.toString()});
            sqls[3] = StringUtil.concat(new String[] {"select ", selectFields.toString(), " from ", tableName,
                " where ", condition.toString()});
            return sqls;
        } finally {
            close(rs);
            close(st);
            if (request == null) {
                close(conn);
            }
        }
    }

    private static String getCondition(String fieldName, String fieldValueName, boolean isStringField, String typeName,
        boolean required, int scriptType) {
        StringBuilder buf = new StringBuilder();
        switch (scriptType) {
            case 1:
                if (isStringField) {
                    buf.append("({?#");
                    buf.append(fieldValueName);
                    buf.append("?} is null and (");
                    buf.append(fieldName);
                    buf.append(" is null or ");
                    buf.append(fieldName);
                    buf.append("='') or ");
                    buf.append(fieldName);
                    buf.append("={?");
                    if (typeName == null) {
                        buf.append('#');
                    } else {
                        buf.append(typeName);
                        buf.append(".#");
                    }

                    buf.append(fieldValueName);
                    buf.append("?})");
                } else {
                    if (!required) {
                        buf.append("({?#");
                        buf.append(fieldValueName);
                        buf.append("?} is null and ");
                        buf.append(fieldName);
                        buf.append(" is null or ");
                    }

                    buf.append(fieldName);
                    buf.append("={?");
                    if (typeName == null) {
                        buf.append('#');
                    } else {
                        buf.append(typeName);
                        buf.append(".#");
                    }

                    buf.append(fieldValueName);
                    if (required) {
                        buf.append("?}");
                    } else {
                        buf.append("?})");
                    }
                }
                break;
            case 2:
                if (!required) {
                    buf.append("({##");
                    buf.append(fieldValueName);
                    buf.append("#} is null and ");
                    buf.append(fieldName);
                    buf.append(" is null or ");
                }

                buf.append(fieldName);
                buf.append("={##");
                buf.append(fieldValueName);
                if (required) {
                    buf.append("#}");
                } else {
                    buf.append("#})");
                }
                break;
            default:
                if (!required) {
                    buf.append("(#");
                    buf.append(fieldValueName);
                    buf.append(" is null and ");
                    buf.append(fieldName);
                    buf.append(" is null or ");
                }

                buf.append(fieldName);
                buf.append("=#");
                buf.append(fieldValueName);
                if (!required) {
                    buf.append(')');
                }
        }

        return buf.toString();
    }

    public static String getOrderSql(String sort, String orderFields) {
        JSONArray ja = new JSONArray(sort);
        int j = ja.length();
        if (j > 0) {
            StringBuilder exp = new StringBuilder();
            JSONObject orderJo;
            String defaultPrefix;
            if (StringUtil.isEmpty(orderFields)) {
                orderJo = null;
                defaultPrefix = null;
            } else {
                orderJo = new JSONObject(orderFields);
                defaultPrefix = orderJo.optString("default", (String)null);
            }

            for (int i = 0; i < j; ++i) {
                JSONObject jo = ja.getJSONObject(i);
                if (i > 0) {
                    exp.append(',');
                }

                String property = jo.getString("property");
                if (!StringUtil.checkName(property)) {
                    throw new IllegalArgumentException("Invalid name \"" + property + "\".");
                }

                if (orderJo != null) {
                    if (orderJo.has(property)) {
                        String prefix = orderJo.optString(property);
                        if (!prefix.isEmpty()) {
                            exp.append(prefix);
                            exp.append('.');
                        }
                    } else if (defaultPrefix != null) {
                        exp.append(defaultPrefix);
                        exp.append('.');
                    }
                }

                exp.append(property);
                if (StringUtil.isSame(jo.optString("direction"), "desc")) {
                    exp.append(" desc");
                }
            }

            return exp.toString();
        } else {
            return null;
        }
    }

    public static String getOrderBySql(HttpServletRequest request, String orderFields) {
        String sort = request.getParameter("sort");
        if (StringUtil.isEmpty(sort)) {
            return "";
        } else {
            String orderExp = getOrderSql(sort, orderFields);
            return " order by " + orderExp;
        }
    }

    public static String getOrderSql(HttpServletRequest request, String orderFields) {
        String sort = request.getParameter("sort");
        return StringUtil.isEmpty(sort) ? "" : "," + getOrderSql(sort, orderFields);
    }

    public static JSONArray getData(ResultSet rs) throws Exception {
        return getData(rs, (String)null, (String)null);
    }

    public static JSONArray getData(ResultSet rs, String keys, String dictTableNames) throws Exception {
        JSONArray result = new JSONArray();
        HashMap keyMap;
        if (StringUtil.isEmpty(dictTableNames)) {
            keyMap = new HashMap();
        } else {
            keyMap = Dictionary.getKeyFields(StringUtil.split(dictTableNames, ','));
        }

        if (!StringUtil.isEmpty(keys)) {
            String[] items = StringUtil.split(keys, ',');
            String[] var18 = items;
            int var17 = items.length;

            for (int var16 = 0; var16 < var17; ++var16) {
                String item = var18[var16];
                keyMap.put(StringUtil.getNamePart(item), StringUtil.getValuePart(item));
            }
        }

        ResultSetMetaData meta = rs.getMetaData();
        int j = meta.getColumnCount();
        String[] fields = new String[j];
        int[] types = new int[j];
        boolean[] hasKeys = new boolean[j];
        Object[] kdMaps = new Object[j];

        int i;
        for (i = 0; i < j; ++i) {
            fields[i] = getFieldName(meta.getColumnLabel(i + 1));
            types[i] = meta.getColumnType(i + 1);
            hasKeys[i] = keyMap.containsKey(fields[i]);
            if (hasKeys[i]) {
                kdMaps[i] = KVBuffer.buffer.get(keyMap.get(fields[i]));
            } else {
                kdMaps[i] = null;
            }
        }

        while (rs.next()) {
            JSONObject record = new JSONObject();

            for (i = 0; i < j; ++i) {
                Object value = getObject(rs, i + 1, types[i]);
                if (hasKeys[i]) {
                    value = KVBuffer.getValue((ConcurrentHashMap)kdMaps[i], value);
                }

                if (value == null) {
                    value = JSONObject.NULL;
                }

                record.put(fields[i], value);
            }

            result.put(record);
        }

        return result;
    }

    public static JSONArray getArray(ResultSet rs) throws Exception {
        ResultSetMetaData meta = rs.getMetaData();
        int type = meta.getColumnType(1);
        JSONArray result = new JSONArray();

        while (rs.next()) {
            result.put(getObject((ResultSet)rs, 1, type));
        }

        return result;
    }

    private static String createInSql(JSONArray f) {
        if (null == f || f.length() <= 0) {
            return null;
        }
        StringBuilder inSql = new StringBuilder();
        inSql.append(" ( ");
        for (int i = 0; i < f.length(); i++) {
            if (i != 0) {
                inSql.append(",");
            }
            inSql.append("'" + f.getString(i) + "'");
        }
        inSql.append(" ) ");
        return inSql.toString();
    }

    private static JSONArray checkParams(JSONArray f) {
        if (null == f || f.length() <= 0) {
            return null;
        }
        JSONArray _f = new JSONArray();
        String field = "";
        String type = "";
        String comparison = "";
        String value = "";
        JSONObject obj = null;
        for (int i = 0; i < f.length(); i++) {
            obj = f.getJSONObject(i);
            field = obj.getString("field");
            type = obj.getString("type");
            comparison = obj.getString("comparison");
            value = obj.getString("value");
            if (StringUtil.isEmpty(field) || StringUtil.isEmpty(comparison) || StringUtil.isEmpty(value)) {
                continue;
            }
            _f.put(obj);
        }
        return _f;
    }

    private static String getWhereSql(JSONArray f, String connector) {
        f = checkParams(f);
        if (null == f || f.length() <= 0) {
            return null;
        }
        StringBuilder where = new StringBuilder();
        JSONObject obj = null;
        String field = "";
        String type = "";
        String comparison = "";
        String value = "";
        JSONArray values = null;
        String inSql = "";
        where.append(" ( ");
        for (int i = 0; i < f.length(); i++) {
            obj = f.getJSONObject(i);
            field = obj.getString("field");
            type = obj.getString("type");
            comparison = obj.getString("comparison");
            value = obj.getString("value");
            if (StringUtil.isEmpty(value)) {
                continue;
            }
            if ("datetime".equals(type) && !StringUtil.isEmpty(value)) {
                value = value.replace(" 上午", "").replace(" 下午", "");
            }
            if (i != 0) {
                where.append(" " + connector + " ");
            }
            where.append(" ( " + field);
            if ("contains".equals(comparison)) {
                where.append(" LIKE '%" + value + "%' ");
            } else if ("not contains".equals(comparison)) {
                where.append(" NOT LIKE '%" + value + "%' ");
            } else if ("eq".equals(comparison)) {
                if ("numeric".endsWith(type)) {
                    where.append(" = " + value + " ");
                } else {
                    where.append(" = '" + value + "' ");
                }
            } else if ("ne".equals(comparison)) {
                if ("numeric".endsWith(type)) {
                    where.append(" != " + value + " ");
                } else {
                    where.append(" != '" + value + "' ");
                }
            } else if ("lt".equals(comparison)) {
                if ("numeric".endsWith(type)) {
                    where.append(" < " + value + " OR " + field + " = " + value + " ");
                } else {
                    where.append(" < '" + value + "' OR " + field + " =  '" + value + "' ");
                }
            } else if ("gt".equals(comparison)) {
                if ("numeric".endsWith(type)) {
                    where.append(" < " + value + " OR " + field + " = " + value + " ");
                } else {
                    where.append(" < '" + value + "' OR " + field + " = '" + value + "' ");
                }
            } else if ("startwith".equals(comparison)) {
                where.append(" LIKE '" + value + "%' ");
            } else if ("not startwith".equals(comparison)) {
                where.append(" NOT LIKE '" + value + "%' ");
            } else if ("endwith".equals(comparison)) {
                where.append(" LIKE '%" + value + "' ");
            } else if ("not endwith".equals(comparison)) {
                where.append(" NOT LIKE '%" + value + "' ");
            } else if ("in".equals(comparison)) {
                values = new JSONArray(value);
                inSql = createInSql(values);
                where.append(" IN " + inSql);
            } else if ("regexp".equals(comparison)) {// REGEXP
                where.append(" REGEXP '" + value + "' ");
            } else if ("list".equals(type)) {
                values = new JSONArray(value);
                inSql = createInSql(values);
                where.append(" IN " + inSql);
            }
            where.append(" ) ");
        }
        where.append(" ) ");
        return where.toString();
    }

    public static String getWhereSql(String filter, String query) {
        try {
            StringBuilder whereSql = new StringBuilder();
            if (null != filter) {
                String whereSql1 = getWhereSql(new JSONArray(filter), "AND");
                if (!StringUtil.isEmpty(whereSql1)) {
                    whereSql.append(whereSql1);
                }
            }
            if (null != query) {
                String whereSql2 = getWhereSql(new JSONArray(query), "OR");
                if (!StringUtil.isEmpty(whereSql2)) {
                    if (!StringUtil.isEmpty(whereSql.toString())) {
                        whereSql.append(" AND ");
                    }
                    whereSql.append(whereSql2);
                }
            }
            return whereSql.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        DbUtil.targetDataSources = applicationContext.getBeansOfType(DataSource.class);
        String key = "";
        if (null != DbUtil.targetDataSources) {
            for (Entry<String, DataSource> e : DbUtil.targetDataSources.entrySet()) {
                if ("default".equalsIgnoreCase(e.getKey())) {
                    key = e.getKey();
                    DbUtil.defaultDataSource = e.getValue();
                    break;
                }
            }
        }
        if (null != DbUtil.targetDataSources && null == DbUtil.defaultDataSource) {
            for (Entry<String, DataSource> e : DbUtil.targetDataSources.entrySet()) {
                key = e.getKey();
                DbUtil.defaultDataSource = e.getValue();
                break;
            }
        }
        if (null != DbUtil.targetDataSources && StringUtils.isNotEmpty(key)) {
            DbUtil.targetDataSources.remove(key);
        }
    }
}