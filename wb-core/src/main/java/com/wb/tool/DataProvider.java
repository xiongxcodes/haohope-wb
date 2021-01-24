package com.wb.tool;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.wb.common.Dictionary;
import com.wb.common.KVBuffer;
import com.wb.common.Var;
import com.wb.util.DbUtil;
import com.wb.util.StringUtil;
import com.wb.util.WebUtil;

public class DataProvider {
    public HttpServletRequest request;
    public HttpServletResponse response;
    public ResultSet resultSet;
    public Long totalCount;
    public long startTime;
    public long beginIndex = 1L;
    public long endIndex = Long.MAX_VALUE;
    public String type;
    public String[] dictTableNames;
    public String dictFieldsMap;
    public boolean createKeyValues;
    public String fields;
    public String fieldsTag;
    public String tag;
    public Integer limitRecords;
    public Integer limitExportRecords;
    public boolean createColumns = true;
    public String keyDefines;
    private static JSONArray treeMeta = new JSONArray(
        "[{name:'parentId',type:'auto',defaultValue:null,useNull:true},{name:'index',type:'int',defaultValue:-1,persist:false,convert:null},{name:'depth',type:'int',defaultValue:0,persist:false,convert:null},{name:'expanded',type:'bool',defaultValue:false,persist:false},{name:'expandable',type:'bool',defaultValue:true,persist:false},{name:'checked',type:'bool',defaultValue:null,persist:false},{name:'leaf',type:'bool',defaultValue:false},{name:'cls',type:'string',defaultValue:'',persist:false,convert:null},{name:'iconCls',type:'string',defaultValue:'',persist:false,convert:null},{name:'icon',type:'string',defaultValue:'',persist:false,convert:null},{name:'root',type:'bool',defaultValue:false,persist:false},{name:'isLast',type:'bool',defaultValue:false,persist:false},{name:'isFirst',type:'bool',defaultValue:false,persist:false},{name:'allowDrop',type:'bool',defaultValue:true,persist:false},{name:'allowDrag',type:'bool',defaultValue:true,persist:false},{name:'loaded',type:'bool',defaultValue:false,persist:false},{name:'loading',type:'bool',defaultValue:false,persist:false},{name:'href',type:'string',defaultValue:'',persist:false,convert:null},{name:'hrefTarget',type:'string',defaultValue:'',persist:false,convert:null},{name:'qtip',type:'string',defaultValue:'',persist:false,convert:null},{name:'qtitle',type:'string',defaultValue:'',persist:false,convert:null},{name:'qshowDelay',type:'int',defaultValue:0,persist:false,convert:null},{name:'children',type:'auto',defaultValue:null,persist:false,convert:null},{name:'visible',type:'bool',defaultValue:true,persist:false}]");

    public String getScript() throws Exception {
        if (!"array".equals(this.type) && !StringUtil.isEmpty(this.type)) {
            if ("tree".equals(this.type)) {
                return this.getArray(true);
            } else if ("object".equals(this.type)) {
                return this.getObject();
            } else {
                throw new IllegalArgumentException("The type is invalid.");
            }
        } else {
            return this.getArray(false);
        }
    }

    public void output() throws Exception {
        String script;
        if (!"array".equals(this.type) && !StringUtil.isEmpty(this.type)) {
            if ("tree".equals(this.type)) {
                script = this.getArray(true);
            } else {
                if (!"object".equals(this.type)) {
                    DbUtil.outputBlob(this.resultSet, this.request, this.response, this.type);
                    return;
                }

                script = this.getObject();
            }
        } else {
            script = this.getArray(false);
        }

        if (WebUtil.jsonResponse(this.request)) {
            WebUtil.send(this.response, script, true);
        } else {
            WebUtil.send(this.response, script);
        }

    }

    public String getArray(boolean isTree) throws Exception {
        long count = 0L;
        boolean rowOnly = WebUtil.exists(this.request, "sys.rowOnly");
        boolean hasDict = this.dictTableNames != null;
        boolean first = true;
        boolean hasTotal = this.totalCount != null;
        StringBuilder buf = new StringBuilder();
        ResultSetMetaData meta = this.resultSet.getMetaData();
        int maxRecs;
        if (WebUtil.exists(this.request, "sys.fromExport")) {
            if (this.limitExportRecords == null) {
                maxRecs = Var.limitExportRecords;
            } else if (this.limitExportRecords == -1) {
                maxRecs = Integer.MAX_VALUE;
            } else {
                maxRecs = this.limitExportRecords;
            }
        } else if (this.limitRecords == null) {
            maxRecs = Var.limitRecords;
        } else if (this.limitRecords == -1) {
            maxRecs = Integer.MAX_VALUE;
        } else {
            maxRecs = this.limitRecords;
        }

        int j = meta.getColumnCount();
        String[] names = new String[j];
        String[] keyNames = new String[j];
        int[] types = new int[j];
        Object[] keyMaps;
        if (this.createKeyValues) {
            keyMaps = new Object[j];
        } else {
            keyMaps = null;
        }

        JSONObject dictFieldsObj;
        if (StringUtil.isEmpty(this.dictFieldsMap)) {
            dictFieldsObj = null;
        } else {
            dictFieldsObj = new JSONObject(this.dictFieldsMap);
        }

        boolean hasKeyDefine = !StringUtil.isEmpty(this.keyDefines);
        Object[] kdMaps;
        JSONObject kd;
        if (hasKeyDefine) {
            kd = new JSONObject(this.keyDefines);
            kdMaps = new Object[j];
        } else {
            kd = null;
            kdMaps = null;
        }

        int i;
        for (i = 0; i < j; ++i) {
            names[i] = meta.getColumnLabel(i + 1);
            names[i] = DbUtil.getFieldName(names[i]);
            if (StringUtil.isEmpty(names[i])) {
                names[i] = "FIELD" + Integer.toString(i + 1);
            }

            if (this.createKeyValues) {
                if (hasDict) {
                    DictRecord dictRecord = Dictionary.find(this.dictTableNames, names[i]);
                    if (dictRecord != null && dictRecord.keyName != null) {
                        keyMaps[i] = KVBuffer.buffer.get(dictRecord.keyName);
                    } else {
                        keyMaps[i] = null;
                    }
                } else {
                    keyMaps[i] = null;
                }
            }

            if (hasKeyDefine) {
                String kdValue = (String)kd.opt(names[i]);
                if (kdValue == null) {
                    kdMaps[i] = null;
                } else {
                    kdMaps[i] = KVBuffer.buffer.get(kdValue);
                }
            }

            keyNames[i] = StringUtil.quote(names[i] + "__V");
            names[i] = StringUtil.quote(names[i]);
            types[i] = meta.getColumnType(i + 1);
        }

        buf.append("{\"success\":true");
        if (!rowOnly && !"-".equals(this.fields)) {
            JSONArray sysMeta =
                DbUtil.getFields(meta, this.createKeyValues ? this.dictTableNames : null, dictFieldsObj, kd);
            if (!StringUtil.isEmpty(this.fields)) {
                this.mergeFields(sysMeta, new JSONArray(this.fields));
            }

            buf.append(",\"metaData\":{\"fields\":");
            if (isTree) {
                buf.append(this.mergeFields(sysMeta, treeMeta).toString());
            } else {
                buf.append(sysMeta.toString());
            }

            if (!StringUtil.isEmpty(this.fieldsTag)) {
                buf.insert(buf.length() - 1, ',' + this.fieldsTag.substring(1, this.fieldsTag.length() - 1));
            }

            buf.append('}');
        }

        if (isTree) {
            buf.append(",\"children\":[");
        } else {
            if (!rowOnly && (this.createColumns || hasDict)) {
                buf.append(",\"columns\":");
                buf.append(DbUtil.getColumns(meta, this.dictTableNames, dictFieldsObj, kd));
            }

            buf.append(",\"rows\":[");
        }
        /**
         * while (this.resultSet.next()) { ++count; if (count > (long)maxRecs) { --count; break; }
         * 
         * if (count >= this.beginIndex) { if (count > this.endIndex) { if (hasTotal) { break; } } else { if (first) {
         * first = false; } else { buf.append(','); }
         * 
         * buf.append('{');
         * 
         * for (i = 0; i < j; ++i) { if (i > 0) { buf.append(','); }
         * 
         * Object object = DbUtil.getObject(this.resultSet, i + 1, types[i]); if (hasKeyDefine && kdMaps[i] != null) {
         * object = KVBuffer.getValue((ConcurrentHashMap)kdMaps[i], object); }
         * 
         * buf.append(names[i]); buf.append(':'); String val; if (!isTree) { buf.append(StringUtil.encode(object)); }
         * else { if (object == null) { val = "null"; } else { val = object.toString(); if (val.equals("[]") &&
         * "\"children\"".equals(names[i])) { val = "[]"; } else { val = StringUtil.encode(val); } }
         * 
         * buf.append(val); }
         * 
         * if (this.createKeyValues && keyMaps[i] != null) { buf.append(','); buf.append(keyNames[i]); buf.append(':');
         * if (object == null) { buf.append("null"); } else { val = KVBuffer.getValue((ConcurrentHashMap)keyMaps[i],
         * object); buf.append(StringUtil.quote(val)); } } }
         * 
         * buf.append('}'); } } }
         **/
        while (this.resultSet.next()) {
            if (first) {
                first = false;
            } else {
                buf.append(',');
            }

            buf.append('{');

            for (i = 0; i < j; ++i) {
                if (i > 0) {
                    buf.append(',');
                }

                Object object = DbUtil.getObject(this.resultSet, i + 1, types[i]);
                if (hasKeyDefine && kdMaps[i] != null) {
                    object = KVBuffer.getValue((ConcurrentHashMap)kdMaps[i], object);
                }

                buf.append(names[i]);
                buf.append(':');
                String val;
                if (!isTree) {
                    buf.append(StringUtil.encode(object));
                } else {
                    if (object == null) {
                        val = "null";
                    } else {
                        val = object.toString();
                        if (val.equals("[]") && "\"children\"".equals(names[i])) {
                            val = "[]";
                        } else {
                            val = StringUtil.encode(val);
                        }
                    }

                    buf.append(val);
                }

                if (this.createKeyValues && keyMaps[i] != null) {
                    buf.append(',');
                    buf.append(keyNames[i]);
                    buf.append(':');
                    if (object == null) {
                        buf.append("null");
                    } else {
                        val = KVBuffer.getValue((ConcurrentHashMap)keyMaps[i], object);
                        buf.append(StringUtil.quote(val));
                    }
                }
            }

            buf.append('}');
        }
        if (!hasTotal) {
            this.totalCount = count;
        }

        buf.append("],\"total\":");
        buf.append(this.totalCount);
        if (!StringUtil.isEmpty(this.tag)) {
            buf.append(',');
            if (this.tag.charAt(0) == '{' && this.tag.charAt(this.tag.length() - 1) == '}') {
                buf.append(this.tag.substring(1, this.tag.length() - 1));
            } else {
                buf.append(this.tag);
            }
        }

        if (this.startTime > 0L) {
            buf.append(",\"elapsed\":");
            buf.append(Long.toString(System.currentTimeMillis() - this.startTime));
        }

        buf.append("}");
        return buf.toString();
    }

    public String getObject() throws Exception {
        JSONObject jo = new JSONObject();
        if (this.resultSet.next()) {
            ResultSetMetaData meta = this.resultSet.getMetaData();
            JSONObject kd;
            if (StringUtil.isEmpty(this.keyDefines)) {
                kd = null;
            } else {
                kd = new JSONObject(this.keyDefines);
            }

            int j = meta.getColumnCount();

            for (int i = 0; i < j; ++i) {
                int type = meta.getColumnType(i + 1);
                String key = meta.getColumnLabel(i + 1);
                key = DbUtil.getFieldName(key);
                if (StringUtil.isEmpty(key)) {
                    key = "FIELD" + Integer.toString(i + 1);
                }

                Object value = DbUtil.getObject(this.resultSet, i + 1, type);
                if (value == null) {
                    value = JSONObject.NULL;
                } else if (kd != null) {
                    String kdValue = (String)kd.opt(key);
                    if (kdValue != null) {
                        value = KVBuffer.getValue((ConcurrentHashMap)KVBuffer.buffer.get(kdValue), value);
                    }
                }

                jo.put(key, value);
            }
        }

        return jo.toString();
    }

    private JSONArray mergeFields(JSONArray source, JSONArray dest) {
        int j = source.length() - 1;
        int l = dest.length();

        int k;
        for (k = 0; k < l; ++k) {
            JSONObject destObj = dest.getJSONObject(k);
            String destName = destObj.getString("name");

            for (int i = j; i >= 0; --i) {
                JSONObject sourceObj = source.getJSONObject(i);
                if (destName.equals(sourceObj.getString("name"))) {
                    source.remove(i);
                    --j;
                    break;
                }
            }
        }

        for (k = 0; k < l; ++k) {
            source.put(dest.getJSONObject(k));
        }

        return source;
    }
}