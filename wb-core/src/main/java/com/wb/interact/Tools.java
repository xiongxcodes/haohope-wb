package com.wb.interact;

import java.io.File;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;

import com.wb.common.Base;
import com.wb.common.KVBuffer;
import com.wb.common.Session;
import com.wb.common.XwlBuffer;
import com.wb.util.DbUtil;
import com.wb.util.FileUtil;
import com.wb.util.SortUtil;
import com.wb.util.StringUtil;
import com.wb.util.SysUtil;
import com.wb.util.WbUtil;
import com.wb.util.WebUtil;

public class Tools {
    public static void getDictTree(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String type = request.getParameter("type");
        String jndi = request.getParameter("jndi");
        String schema = request.getParameter("schema");
        String result;
        if ("db".equals(type)) {
            HashSet<String> tables = getDictTables(request, jndi);
            HashSet<String> copyTables = new HashSet();
            Iterator var9 = tables.iterator();

            while (var9.hasNext()) {
                String t = (String)var9.next();
                copyTables.add(t);
            }

            result = DBE.getSchemaList(jndi, tables);
            if (result == null) {
                result = DBE.getTableList(jndi, (String)null, copyTables);
            }
        } else if ("schema".equals(type)) {
            result = DBE.getTableList(jndi, schema, getDictTables(request, jndi));
        } else {
            result = DBE.getDbList();
        }

        WebUtil.send(response, result);
    }

    private static HashSet<String> getDictTables(HttpServletRequest request, String jndi) throws Exception {
        boolean otherDb = !"default".equals(jndi);
        int jndiLen = jndi.length() + 1;
        HashSet<String> tables = new HashSet();
        ResultSet rs = (ResultSet)DbUtil.run(request, "select distinct TABLE_NAME from WB_DICT");
        jndi = jndi.toUpperCase() + '.';

        while (rs.next()) {
            String tableName = rs.getString(1).toUpperCase();
            if (otherDb) {
                if (tableName.startsWith(jndi)) {
                    tables.add(tableName.substring(jndiLen));
                }
            } else if (tableName.indexOf(46) == -1) {
                tables.add(tableName);
            }
        }

        return tables;
    }

    public static void loadKeyNames(HttpServletRequest request, HttpServletResponse response) {
        boolean isFirst = true;
        StringBuilder buf = new StringBuilder();
        ArrayList<Entry<String, ConcurrentHashMap<Object, String>>> keys = SortUtil.sortKey(KVBuffer.buffer);
        buf.append("[");

        Entry key;
        for (Iterator var6 = keys.iterator(); var6.hasNext(); buf.append(StringUtil.quote((String)key.getKey()))) {
            key = (Entry)var6.next();
            if (isFirst) {
                isFirst = false;
            } else {
                buf.append(",");
            }
        }

        buf.append("]");
        request.setAttribute("keyNameList", StringUtil.quote(buf.toString(), false));
    }

    public static void delModulesPerm(HttpServletRequest request, HttpServletResponse response) throws Exception {
        JSONArray destroy = new JSONArray(request.getParameter("destroy"));
        int j = destroy.length();
        if (j != 0) {
            String[] roles = new String[j];

            for (int i = 0; i < j; ++i) {
                roles[i] = destroy.getJSONObject(i).getString("ROLE_ID");
            }

            doDelPerm(Base.modulePath, roles);
        }
    }

    private static void doDelPerm(File path, String[] delRoles) throws Exception {
        File[] files = FileUtil.listFiles(path);
        File[] var7 = files;
        int var6 = files.length;
        List<String> modules = new ArrayList<String>();
        for (int var5 = 0; var5 < var6; ++var5) {
            File file = var7[var5];
            if (file.isDirectory()) {
                doDelPerm(file, delRoles);
            } else {
                String filename = file.getName();
                if (filename.endsWith(".xwl")) {
                    // IDE.updateModule(file, (JSONObject)null, delRoles, false);
                    modules.add(FileUtil.getModulePath(file));
                }
            }
        }
        Module.updateRole(modules, Arrays.asList(delRoles), false);
    }

    public static void setModulesPerm(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String[] roles = new String[1];
        String[] userRoles = Session.getRoles(request);
        boolean checked = Boolean.parseBoolean(request.getParameter("checked"));
        JSONArray pathList = new JSONArray(request.getParameter("path"));
        int j = pathList.length();
        roles[0] = request.getParameter("role");

        List<String> modules = new ArrayList<String>();
        for (int i = 0; i < j; ++i) {
            File file = new File(Base.modulePath, pathList.getString(i));
            if (!file.isDirectory()) {
                if (!WbUtil.canAccess(XwlBuffer.get(file), pathList.getString(i), userRoles)) {
                    SysUtil.accessDenied(request);
                }
                modules.add(pathList.getString(i));
                // IDE.updateModule(file, (JSONObject)null, roles, checked);
            }
        }
        List<String> myroles = new ArrayList<String>();
        myroles.add(request.getParameter("role"));
        Module.updateRole(modules, myroles, checked);

    }
}