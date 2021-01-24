package com.wb.tool;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import com.wb.common.Str;
import com.wb.common.Var;
import com.wb.util.DbUtil;
import com.wb.util.JsonUtil;
import com.wb.util.StringUtil;
import com.wb.util.SysUtil;
import com.wb.util.WebUtil;

public class Query {
    public HttpServletRequest request;
    public String sql;
    public String jndi;
    public String arrayName;
    public JSONArray arrayData;
    public boolean batchUpdate;
    public String type;
    public String loadParams;
    public boolean returnStatement;
    public String transaction;
    public String isolation;
    public boolean uniqueUpdate;
    public String errorText;
    private String debugSql;
    private String formattedSql;
    private ArrayList<Object[]> paramList;
    private ArrayList<String> paramValList;
    private int paramCount;
    private PreparedStatement statement;

    public Object run() throws Exception {
        this.checkProperties();
        boolean hasArray = this.arrayData != null || !StringUtil.isEmpty(this.arrayName);
        Object result = null;
        this.sql = this.sql.trim();
        this.replaceMacros();
        Connection connection = DbUtil.getConnection(this.request, this.jndi);
        boolean isCommit = "commit".equals(this.transaction);
        if (isCommit) {
            if (connection.getAutoCommit()) {
                this.transaction = "start";
            }
        } else if (StringUtil.isEmpty(this.transaction) && (this.uniqueUpdate || hasArray)
            && connection.getAutoCommit()) {
            this.transaction = "start";
        }

        if ("start".equals(this.transaction)) {
            DbUtil.startTransaction(connection, this.isolation);
        }

        if (StringUtil.isEmpty(this.type)) {
            if (this.sql.startsWith("{")) {
                this.type = "call";
            } else {
                this.type = "execute";
            }
        }

        boolean isCall = "call".equals(this.type);
        if (isCall) {
            this.statement = connection.prepareCall(this.formattedSql);
        } else {
            Long beginIndex = (Long)this.request.getAttribute("beginIndex");
            Long endIndex = (Long)this.request.getAttribute("endIndex");
            // 游标类型：
            // ResultSet.TYPE_FORWORD_ONLY:只进游标
            // ResultSet.TYPE_SCROLL_INSENSITIVE:可滚动。但是不受其他用户对数据库更改的影响。
            // ResultSet.TYPE_SCROLL_SENSITIVE:可滚动。当其他用户更改数据库时这个记录也会改变。
            // 能否更新记录：
            // ResultSet.CONCUR_READ_ONLY,只读
            // ResultSet.CONCUR_UPDATABLE,可更新
            if (null != beginIndex && null != endIndex) {
                this.statement = connection.prepareStatement(this.formattedSql, ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);
                String driverName = connection.getMetaData().getDriverName().toUpperCase().trim();
                if (driverName.contains("MYSQL") && endIndex > 50000000l) {
                    endIndex = 50000000l;
                } else if (driverName.contains("ORACLE") && endIndex > 50000000l) {

                } else if (driverName.contains("SQLSERVER") && endIndex > 50000000l) {

                }
                this.statement.setMaxRows(endIndex.intValue());
            } else {
                this.statement = connection.prepareStatement(this.formattedSql);
            }
        }

        if (Var.fetchSize != -1) {
            this.statement.setFetchSize(Var.fetchSize);
        }

        WebUtil.setObject(this.request, SysUtil.getId(), this.statement);
        this.regParameters();
        if (hasArray) {
            this.executeBatch();
        } else {
            if (Var.debug) {
                this.printSql();
            }

            if ("query".equals(this.type)) {
                result = this.statement.executeQuery();
                WebUtil.setObject(this.request, SysUtil.getId(), result);
            } else {
                int affectedRows;
                if ("update".equals(this.type)) {
                    affectedRows = this.statement.executeUpdate();
                    result = affectedRows;
                    if (this.uniqueUpdate && affectedRows != 1) {
                        this.notUnique();
                    }
                } else {
                    boolean autoLoadParams = StringUtil.isEmpty(this.loadParams) || "auto".equals(this.loadParams);
                    if (this.statement.execute()) {
                        if (autoLoadParams) {
                            this.loadParams = "none";
                        }

                        result = this.statement.getResultSet();
                        WebUtil.setObject(this.request, SysUtil.getId(), result);
                    } else {
                        if (autoLoadParams) {
                            this.loadParams = "load";
                        }

                        affectedRows = this.statement.getUpdateCount();
                        result = affectedRows;
                        if (this.uniqueUpdate && affectedRows != 1) {
                            this.notUnique();
                        }
                    }

                    if (isCall && (this.paramCount > 0 || this.returnStatement)) {
                        HashMap<String, Object> map = this.getOutParameter("load".equals(this.loadParams));
                        if (map.size() > 0) {
                            if (map.containsKey("return")) {
                                throw new IllegalArgumentException("Invalid output parameter name \"return\"");
                            }

                            map.put("return", result);
                            result = map;
                        }
                    }
                }
            }
        }

        if (isCommit) {
            connection.commit();
            connection.setAutoCommit(true);
        }

        this.checkError(result);
        return result;
    }

    private void checkProperties() {
        String[] isolations;
        if (!StringUtil.isEmpty(this.transaction)) {
            isolations = new String[] {"start", "commit", "none"};
            if (StringUtil.indexOf(isolations, this.transaction) == -1) {
                throw new IllegalArgumentException("Invalid transaction \"" + this.transaction + "\".");
            }
        }

        if (!StringUtil.isEmpty(this.loadParams)) {
            isolations = new String[] {"auto", "load", "none"};
            if (StringUtil.indexOf(isolations, this.loadParams) == -1) {
                throw new IllegalArgumentException("Invalid loadParams \"" + this.loadParams + "\".");
            }
        }

        if (!StringUtil.isEmpty(this.type)) {
            isolations = new String[] {"query", "update", "execute", "call"};
            if (StringUtil.indexOf(isolations, this.type) == -1) {
                throw new IllegalArgumentException("Invalid type \"" + this.type + "\".");
            }
        }

        if (!StringUtil.isEmpty(this.isolation)) {
            isolations = new String[] {"readCommitted", "readUncommitted", "repeatableRead", "serializable"};
            if (StringUtil.indexOf(isolations, this.isolation) == -1) {
                throw new IllegalArgumentException("Invalid isolation \"" + this.isolation + "\".");
            }
        }

    }

    private void executeBatch() throws Exception {
        JSONArray ja;
        if (this.arrayData == null) {
            Object obj = WebUtil.fetchObject(this.request, this.arrayName);
            if (obj instanceof JSONArray) {
                ja = (JSONArray)obj;
            } else {
                if (obj == null) {
                    return;
                }

                String val = obj.toString();
                if (val.isEmpty()) {
                    return;
                }

                ja = new JSONArray(val);
            }
        } else {
            ja = this.arrayData;
        }

        int j = ja.length();
        if (j != 0) {
            for (int i = 0; i < j; ++i) {
                JSONObject jo = ja.getJSONObject(i);

                for (int k = 0; k < this.paramCount; ++k) {
                    Object[] param = (Object[])this.paramList.get(k);
                    String name = (String)param[0];
                    if (!(Boolean)param[2] && jo.has(name)) {
                        Object valObj = JsonUtil.opt(jo, name);
                        DbUtil.setObject(this.statement, k + 1, (Integer)param[1], valObj);
                        if (Var.debug) {
                            this.paramValList.set(k, StringUtil.toString(valObj));
                        }
                    }
                }

                if (Var.debug) {
                    this.printSql();
                }

                if (this.batchUpdate) {
                    this.statement.addBatch();
                } else {
                    int affectedRows = this.statement.executeUpdate();
                    if (this.uniqueUpdate && affectedRows != 1) {
                        this.notUnique();
                    }
                }
            }

            if (this.batchUpdate) {
                this.statement.executeBatch();
            }

        }
    }

    private void notUnique() {
        throw new RuntimeException(Str.format(this.request, "updateNotUnique", new Object[0]));
    }

    private void replaceMacros() {
        StringBuilder buf = new StringBuilder();
        int startPos = 0;
        int endPos = 0;

        int lastPos;
        for (lastPos = 0;
            (startPos = this.sql.indexOf("{?", startPos)) > -1 && (endPos = this.sql.indexOf("?}", endPos)) > -1;
            lastPos = endPos) {
            buf.append(this.sql.substring(lastPos, startPos));
            startPos += 2;
            endPos += 2;
            buf.append("'{?");
            buf.append(this.sql.substring(endPos - 1, endPos));
            buf.append('\'');
        }

        buf.append(this.sql.substring(lastPos));
        this.debugSql = buf.toString();
        this.formattedSql = StringUtil.replaceAll(this.debugSql, "'{?}'", "?");
    }

    private void regParameters() throws Exception {
        int index = 1;
        int startPos = 0;
        int endPos = 0;
        this.paramList = new ArrayList();
        if (Var.debug) {
            this.paramValList = new ArrayList();
        }

        CallableStatement callStatement;
        if (this.statement instanceof CallableStatement) {
            callStatement = (CallableStatement)this.statement;
        } else {
            callStatement = null;
        }

        for (boolean isCall = callStatement != null;
            (startPos = this.sql.indexOf("{?", startPos)) > -1 && (endPos = this.sql.indexOf("?}", endPos)) > -1;
            ++index) {
            startPos += 2;
            String param = this.sql.substring(startPos, endPos);
            endPos += 2;
            String orgParam = param;
            boolean isOutParam = isCall && param.startsWith("@");
            String paraName;
            int dotPos;
            int type;
            Integer typeObj;
            if (isOutParam) {
                param = param.substring(1);
                dotPos = param.indexOf(46);
                String typeText;
                if (dotPos == -1) {
                    typeText = "varchar";
                    paraName = param;
                } else {
                    typeText = param.substring(0, dotPos);
                    paraName = param.substring(dotPos + 1);
                }

                boolean hasSub = typeText.indexOf(61) != -1;
                if (hasSub) {
                    typeObj = DbUtil.getFieldType(StringUtil.getNamePart(typeText));
                    if (typeObj == null) {
                        throw new Exception("Invalid type " + typeText);
                    }

                    type = typeObj;
                    int subType = Integer.parseInt(StringUtil.getValuePart(typeText));
                    callStatement.registerOutParameter(index, type, subType);
                } else {
                    typeObj = DbUtil.getFieldType(typeText);
                    if (typeObj == null) {
                        throw new Exception("Invalid type " + typeText);
                    }

                    type = typeObj;
                    callStatement.registerOutParameter(index, type);
                }

                if (Var.debug) {
                    this.paramValList.add(orgParam);
                }
            } else {
                dotPos = param.indexOf(46);
                if (dotPos == -1) {
                    type = 12;
                    paraName = param;
                } else {
                    typeObj = DbUtil.getFieldType(param.substring(0, dotPos));
                    if (typeObj == null) {
                        type = 12;
                        paraName = param;
                    } else {
                        type = typeObj;
                        paraName = param.substring(dotPos + 1);
                    }
                }

                Object obj = WebUtil.fetchObject(this.request, paraName);
                DbUtil.setObject(this.statement, index, type, obj);
                if (Var.debug) {
                    this.paramValList.add(StringUtil.toString(obj));
                }
            }

            Object[] paramObjects = new Object[] {paraName, type, isOutParam};
            this.paramList.add(paramObjects);
        }

        this.paramCount = this.paramList.size();
    }

    private HashMap<String, Object> getOutParameter(boolean loadOutParams) throws Exception {
        CallableStatement st = (CallableStatement)this.statement;
        HashMap<String, Object> map = new HashMap();
        map.put("sys.statement", st);
        if (!loadOutParams) {
            return map;
        } else {
            for (int i = 0; i < this.paramCount; ++i) {
                Object[] param = (Object[])this.paramList.get(i);
                if ((Boolean)param[2]) {
                    Object object = DbUtil.getObject(st, i + 1, (Integer)param[1]);
                    if (object instanceof ResultSet) {
                        WebUtil.setObject(this.request, SysUtil.getId(), object);
                    }

                    map.put((String)param[0], object);
                }
            }

            return map;
        }
    }

    private void checkError(Object object) throws Exception {
        if (!StringUtil.isEmpty(this.errorText) && object instanceof ResultSet) {
            ResultSet rs = (ResultSet)object;
            if (rs.next()) {
                throw new RuntimeException(this.errorText);
            }
        }

    }

    private void printSql() {
        String sql = this.debugSql;

        String s;
        for (Iterator var3 = this.paramValList.iterator(); var3.hasNext();
            sql = StringUtil.replaceFirst(sql, "{?}", s)) {
            s = (String)var3.next();
        }

        Console.log(this.request, sql);
    }
}