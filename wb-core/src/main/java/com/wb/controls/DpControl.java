package com.wb.controls;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import com.github.pagehelper.parser.CountSqlParser;
import com.wb.tool.DataProvider;
import com.wb.util.DbUtil;
import com.wb.util.StringUtil;
import com.wb.util.WebUtil;

public class DpControl extends Control {
    public void create() throws Exception {
        this.getContent(true);
    }

    private Long getTotal(String totalSql, String jndi, String totalLoadParams, String totalCountName,
        ResultSet totalResultSet) throws Exception {
        Object totalResult =
            this.getResult(DbUtil.run(this.request, totalSql, jndi, totalLoadParams, totalCountName.startsWith("@")),
                StringUtil.select(new String[] {totalCountName, "totalCount"}));
        if (totalResult == null) {
            throw new NullPointerException("No value in the totalSql.");
        }

        if (totalResult instanceof ResultSet) {
            totalResultSet = (ResultSet)totalResult;
            if (!totalResultSet.next()) {
                throw new NullPointerException("Empty total ResultSet.");
            }

            return Long.parseLong(totalResultSet.getString(1));
        } else {
            return Long.parseLong(totalResult.toString());
        }
    }

    public String getContent(boolean directOutput) throws Exception {
        if (this.gb("disabled", false)) {
            return null;
        } else {
            long startTime = System.currentTimeMillis();
            Long totalCount = null;
            ResultSet resultSet = null;
            ResultSet totalResultSet = null;
            String jndi = this.gs("jndi");
            String limitRecords = this.gs("limitRecords");
            String limitExportRecords = this.gs("limitExportRecords");
            String loadParams = this.gs("loadParams");
            String resultName = this.gs("resultName");
            String totalCountName = this.gs("totalCountName");
            String totalLoadParams = this.gs("totalLoadParams");
            String startParam = this.request.getParameter("start");
            String limitParam = this.request.getParameter("limit");
            String type = this.gs("type");
            boolean autoPage = this.gb("autoPage", true);

            String var31;
            try {
                if (type.isEmpty() && "1".equals(this.request.getParameter("_istree"))) {
                    type = "tree";
                }

                this.setWhereSql();
                if (StringUtil.isEmpty(type) || "array".equals(type)) {
                    this.setOrderSql();
                }

                String sql = createSql();// this.gs("sql");
                long beginIndex;
                if (StringUtil.isEmpty(startParam)) {
                    beginIndex = 1L;
                    this.request.setAttribute("start", 0L);
                } else {
                    beginIndex = Long.parseLong(startParam) + 1L;
                }

                long endIndex;
                if (StringUtil.isEmpty(limitParam)) {
                    endIndex = Long.MAX_VALUE;
                    this.request.setAttribute("limit", endIndex);
                } else {
                    endIndex = beginIndex + Long.parseLong(limitParam) - 1L;
                }

                this.request.setAttribute("beginIndex", beginIndex);
                this.request.setAttribute("endIndex", endIndex);
                Object result =
                    this.getResult(DbUtil.run(this.request, gr(sql), jndi, loadParams, resultName.startsWith("@")),
                        StringUtil.select(new String[] {resultName, "result"}));
                if (!(result instanceof ResultSet)) {
                    String text = StringUtil.concat(new String[] {
                        "{\"total\":1,\"metaData\":{\"fields\":[{\"name\":\"result\",\"type\":\"string\"}]},\"columns\":[{\"xtype\":\"rownumberer\",\"width\":40},{\"dataIndex\":\"result\",flex:1,\"text\":\"result\"}],\"rows\":[{\"result\":",
                        result == null ? "null" : StringUtil.quote(result.toString()), "}],\"elapsed\":",
                        Long.toString(System.currentTimeMillis() - startTime), "}"});
                    if (directOutput) {
                        WebUtil.send(this.response, text);
                        return null;
                    }

                    var31 = text;
                    return var31;
                }

                resultSet = (ResultSet)result;
                // 将游标移动到第一条记录
                resultSet.first();
                // 游标移动到要输出的第一条记录
                resultSet.relative((int)beginIndex - 2);
                if (!autoPage) {
                    String totalSql = this.gs("totalSql");
                    if (StringUtil.isEmpty(totalSql)) {
                        if (!totalCountName.isEmpty()) {
                            String totalCountVal = WebUtil.fetch(this.request, totalCountName);
                            if (StringUtil.isInteger(totalCountVal)) {
                                totalCount = Long.parseLong(totalCountVal);
                            }
                        } else {
                            CountSqlParser csp = new CountSqlParser();
                            totalSql = csp.getSimpleCountSql(gr(sql.replace("{#sql.orderBy#}", "")));
                            totalCount = getTotal(totalSql, jndi, totalLoadParams, totalCountName, totalResultSet);
                        }
                    } else {

                        totalCount = getTotal(totalSql, jndi, totalLoadParams, totalCountName, totalResultSet);
                        /**
                         * Object totalResult = this.getResult( DbUtil.run(this.request, totalSql, jndi,
                         * totalLoadParams, totalCountName.startsWith("@")), StringUtil.select(new String[]
                         * {totalCountName, "totalCount"})); if (totalResult == null) { throw new
                         * NullPointerException("No value in the totalSql."); }
                         * 
                         * if (totalResult instanceof ResultSet) { totalResultSet = (ResultSet)totalResult; if
                         * (!totalResultSet.next()) { throw new NullPointerException("Empty total ResultSet."); }
                         * 
                         * totalCount = Long.parseLong(totalResultSet.getString(1)); } else { totalCount =
                         * Long.parseLong(totalResult.toString()); }
                         **/
                    }
                }

                DataProvider dp = new DataProvider();
                dp.startTime = startTime;
                dp.request = this.request;
                dp.response = this.response;
                dp.resultSet = resultSet;
                dp.fields = this.gs("fields");
                dp.fieldsTag = this.gs("fieldsTag");
                dp.keyDefines = this.gs("keyDefines");
                dp.totalCount = totalCount;
                dp.createColumns = this.gb("createColumns", true);
                if (autoPage) {
                    dp.beginIndex = beginIndex;
                    dp.endIndex = endIndex;
                }

                if (!limitRecords.isEmpty()) {
                    dp.limitRecords = Integer.parseInt(limitRecords);
                }

                if (!limitExportRecords.isEmpty()) {
                    dp.limitExportRecords = Integer.parseInt(limitExportRecords);
                }

                dp.tag = this.gs("tag");
                dp.type = type;
                String dictTableNames = this.gs("dictTableNames");
                dp.createKeyValues = this.gb("createKeyValues", false);
                if (dictTableNames.isEmpty()) {
                    dp.dictTableNames = null;
                } else {
                    dp.dictTableNames = StringUtil.split(dictTableNames, ',', true);
                }

                dp.dictFieldsMap = this.gs("dictFieldsMap");
                if (directOutput) {
                    dp.output();
                    return null;
                }

                var31 = dp.getScript();
            } finally {
                DbUtil.close(resultSet);
                DbUtil.close(totalResultSet);
            }

            return var31;
        }
    }

    private Object getResult(Object result, String resultName) throws Exception {
        if (result instanceof HashMap) {
            HashMap<?, ?> map = (HashMap)result;
            Set<?> es = map.entrySet();
            String itemId = StringUtil.select(new String[] {this.gs("itemId")});

            Entry entry;
            String name;
            Object val;
            for (Iterator var10 = es.iterator(); var10.hasNext(); this.request.setAttribute(name, entry.getValue())) {
                val = var10.next();
                entry = (Entry)val;
                name = (String)entry.getKey();
                if (name.equals("return")) {
                    if (!itemId.isEmpty()) {
                        name = itemId;
                    }
                } else if (!itemId.isEmpty()) {
                    name = StringUtil.concat(new String[] {itemId, ".", name});
                }
            }

            String rsIndex;
            if (resultName.startsWith("@") && StringUtil.isInteger(rsIndex = resultName.substring(1))) {
                return this.getMoreResult(map, Integer.parseInt(rsIndex));
            } else {
                val = map.get(resultName);
                if (val == null) {
                    return map.get("return");
                } else {
                    return val;
                }
            }
        } else {
            return result;
        }
    }

    private Object getMoreResult(HashMap<?, ?> map, int index) throws Exception {
        CallableStatement st = (CallableStatement)map.get("sys.statement");

        for (int i = 1; i < index; ++i) {
            st.getMoreResults();
        }

        Object result = st.getResultSet();
        if (result == null) {
            result = st.getUpdateCount();
        }

        return result;
    }

    private String createSql() {
        String sql = this.gs("sql", false);
        return sql;
    }

    private void setWhereSql() {
        String filter = this.request.getParameter("filter");
        String query = this.request.getParameter("query");
        if ((!StringUtil.isEmpty(filter) || !StringUtil.isEmpty(query))
            && this.request.getAttribute("sql.where") == null) {
            String where = DbUtil.getWhereSql(filter, query);
            if (!StringUtil.isEmpty(where)) {
                this.request.setAttribute("sql.where", " AND (" + where + " )");
            }
        }
    }

    private void setOrderSql() {
        String sort = this.request.getParameter("sort");
        if (!StringUtil.isEmpty(sort) && this.request.getAttribute("sql.orderBy") == null) {
            String orderExp = DbUtil.getOrderSql(sort, this.gs("orderFields"));
            if (orderExp != null) {
                this.request.setAttribute("sql.orderBy", " order by " + orderExp);
                this.request.setAttribute("sql.orderFields", "," + orderExp);
            }

        }
    }
}