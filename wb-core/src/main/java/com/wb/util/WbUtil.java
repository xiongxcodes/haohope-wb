package com.wb.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONObject;

import com.wb.common.Parser;
import com.wb.common.Session;
import com.wb.common.Str;
import com.wb.common.XwlBuffer;
import com.wb.fit.CustomRequest;
import com.wb.fit.CustomResponse;
import com.wb.interact.Module;

public class WbUtil {
    public static String run(String url, JSONObject params, boolean isInvoke) throws Exception {
        CustomResponse response = new CustomResponse((HttpServletResponse)null);
        CustomRequest request = new CustomRequest();
        request.setParams(params);
        Parser parser = new Parser(request, response);
        parser.parse(FileUtil.getModulePath(url), true, isInvoke);
        return getResponseString(response);
    }

    public static String run(String url, JSONObject params, HttpServletRequest request, boolean isInvoke)
        throws Exception {
        CustomResponse response = new CustomResponse((HttpServletResponse)null);
        if (params != null) {
            WebUtil.applyAttributes(request, params);
        }

        Parser parser = new Parser(request, response);
        parser.execute(FileUtil.getModulePath(url), isInvoke ? 5 : 4, (String)null, (String)null);
        return getResponseString(response);
    }

    public static void run(String url, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Parser parser = new Parser(request, response);
        parser.execute(FileUtil.getModulePath(url), 1, (String)null, (String)null);
    }

    public static JSONObject getAppInfo(String url, HttpServletRequest request) throws Exception {
        String path = FileUtil.getModulePath(url, true);
        if (path == null) {
            return null;
        } else {
            JSONObject obj = XwlBuffer.get(path, true);
            if (obj == null) {
                return null;
            } else {
                String[] names = new String[] {"title", "iconCls"};
                JSONObject info = JsonUtil.copy(obj, names);
                String title = info.optString("title");
                if (title.startsWith("Str.")) {
                    info.put("title", Str.format(request, title.substring(4), new Object[0]));
                } else if (title.isEmpty()) {
                    info.put("title", FileUtil.getFilename(path));
                }

                info.put("url", path);
                return info;
            }
        }
    }

    public static JSONObject getModuleRoles() throws Exception {
        JSONObject perms = new JSONObject();
        // scanModulePerm(Base.modulePath, perms);
        List<String> roles = null;
        JSONObject roles1 = null;
        for (Map.Entry<String, List<String>> e : Module.buffer.entrySet()) {
            roles = e.getValue();
            if (null != roles && roles.size() != 0) {
                roles1 = new JSONObject();
                for (String r : roles) {
                    roles1.put(r, 1);
                }
                perms.put(e.getKey(), roles1);
            }
        }
        return perms;
    }

    public static String getResponseString(CustomResponse response) throws Exception {
        byte[] data = response.getBytes();
        String result;
        if (data.length > 2 && data[0] == 31 && data[1] == -117) {
            GZIPInputStream is = new GZIPInputStream(new ByteArrayInputStream(data));

            try {
                result = StringUtil.getString(is);
            } finally {
                is.close();
            }
        } else {
            result = new String(data, "utf-8");
        }

        return result;
    }

    private static void scanModulePerm(File path, JSONObject perms) throws Exception {
        File[] files = FileUtil.listFiles(path);
        File[] var10 = files;
        int var9 = files.length;

        for (int var8 = 0; var8 < var9; ++var8) {
            File file = var10[var8];
            if (file.isDirectory()) {
                scanModulePerm(file, perms);
            } else {
                String filename = file.getName();
                if (filename.endsWith(".xwl")) {
                    String filePath = FileUtil.getModulePath(file);
                    JSONObject jo = XwlBuffer.get(filePath);
                    JSONObject roles = (JSONObject)jo.opt("roles");
                    perms.put(filePath, roles);
                }
            }
        }

    }

    public static boolean canAccess(HttpServletRequest request, String path) {
        if (request != null && path != null) {

            try {
                JSONObject module = XwlBuffer.get(FileUtil.getModulePath(path));
                String[] roles = Session.getRoles(request);
                return canAccess(module, FileUtil.getModulePath(path), roles);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }

    public static boolean canAccess(HttpSession session, String path) throws Exception {
        if (session != null && path != null) {
            JSONObject module = XwlBuffer.get(FileUtil.getModulePath(path));
            String[] roles = (String[])session.getAttribute("sys.roles");
            return canAccess(module, FileUtil.getModulePath(path), roles);
        } else {
            return false;
        }
    }

    public static boolean canAccess(String path, String[] roles) throws Exception {
        if (path != null) {
            JSONObject module = XwlBuffer.get(FileUtil.getModulePath(path));
            return canAccess(module, path, roles);
        } else {
            return false;
        }
    }

    public static boolean canAccess(JSONObject module, String path, String[] roles) {
        boolean noLoginRequired = Boolean.FALSE.equals(module.opt("loginRequired"));
        if (noLoginRequired || module.getBoolean("hidden")) {
            return true;
        } else if (roles == null) {
            return false;
        } else {
            JSONObject setRoles = (JSONObject)module.opt("roles");
            String[] var7 = roles;
            int var6 = roles.length;

            for (int var5 = 0; var5 < var6; ++var5) {
                String role = var7[var5];
                if (setRoles.has(role)) {
                    return true;
                }
            }

            List<String> setRoles1 = Module.buffer.get(path);
            if (null == setRoles1) {
                return false;
            }
            for (int var5 = 0; var5 < var6; ++var5) {
                String role = var7[var5];
                if (setRoles1.contains(role)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static boolean canAccess(JSONObject module, String[] roles) {
        boolean noLoginRequired = Boolean.FALSE.equals(module.opt("loginRequired"));
        if (noLoginRequired) {
            return true;
        } else if (roles == null) {
            return false;
        } else {
            JSONObject setRoles = (JSONObject)module.opt("roles");
            String[] var7 = roles;
            int var6 = roles.length;

            for (int var5 = 0; var5 < var6; ++var5) {
                String role = var7[var5];
                if (setRoles.has(role)) {
                    return true;
                }
            }

            return false;
        }
    }

    public static boolean loginRequired(String path) {
        try {
            return loginRequired(XwlBuffer.get(FileUtil.getModulePath(path)));
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    public static boolean loginRequired(JSONObject module) {
        if (null == module) {
            return true;
        } else {
            return Boolean.TRUE.equals(module.opt("loginRequired"));
        }
    }
}