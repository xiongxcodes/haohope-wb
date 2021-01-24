package com.wb.interact;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONObject;

import com.wb.common.Base;
import com.wb.common.Resource;
import com.wb.common.Session;
import com.wb.common.Str;
import com.wb.common.Value;
import com.wb.common.XwlBuffer;
import com.wb.util.DbUtil;
import com.wb.util.FileUtil;
import com.wb.util.JsonUtil;
import com.wb.util.StringUtil;
import com.wb.util.SysUtil;
import com.wb.util.WbUtil;
import com.wb.util.WebUtil;

public class Portal {
    private static JSONArray getModuleList(HttpServletRequest request, String path, String[] roles, int type)
        throws Exception {
        int displayType;
        if (type == 2) {
            displayType = 3;
        } else {
            displayType = type;
        }

        File base;
        if (path == null) {
            base = Base.modulePath;
            path = "";
        } else {
            base = new File(Base.modulePath, path);
            if (!FileUtil.isAncestor(Base.modulePath, base)) {
                SysUtil.accessDenied(request);
            }
        }

        path = path + "/";
        ArrayList<Entry<String, Integer>> fileNames = IDE.getSortedFile(base);
        JSONObject content = null;
        JSONArray fileArray = new JSONArray();
        Iterator var19 = fileNames.iterator();

        while (var19.hasNext()) {
            Entry<String, Integer> entry = (Entry)var19.next();
            String fileName = (String)entry.getKey();
            File file = new File(base, fileName);
            if (file.exists() && XwlBuffer.canDisplay(file, roles, displayType)) {
                boolean isFolder = file.isDirectory();
                if (isFolder) {
                    File configFile = new File(file, "folder.json");
                    if (configFile.exists()) {
                        content = JsonUtil.readObject(configFile);
                    } else {
                        content = new JSONObject();
                    }
                } else {
                    content = XwlBuffer.get(path + fileName);
                }

                JSONObject fileObject = new JSONObject();
                String title = Str.getText(request, content.optString("title"));
                fileObject.put("text", StringUtil.select(new String[] {title, fileName}));
                String relPath = FileUtil.getModulePath(file);
                fileObject.put("path", relPath);
                fileObject.put("fileName", fileName);
                fileObject.put("inframe", Boolean.TRUE.equals(content.opt("inframe")));
                if (isFolder) {
                    fileObject.put("children", getModuleList(request, relPath, roles, type));
                } else {
                    String pageLink = (String)content.opt("pageLink");
                    if (!StringUtil.isEmpty(pageLink)) {
                        fileObject.put("pageLink", pageLink);
                    }

                    fileObject.put("leaf", true);
                }

                if (type == 1) {
                    fileObject.put("cls", "wb_pointer");
                }

                String iconCls = content.optString("iconCls");
                if (!StringUtil.isEmpty(iconCls)) {
                    fileObject.put("iconCls", iconCls);
                }

                if (type == 2) {
                    fileObject.put("checked", false);
                }

                fileArray.put(fileObject);
            }
        }

        return fileArray;
    }

    private static JSONArray getButtonList(HttpServletRequest request, String path, String[] roles, int type)
        throws Exception {
        int displayType;
        if (type == 2) {
            displayType = 3;
        } else {
            displayType = type;
        }

        File base;
        if (path == null) {
            base = Base.modulePath;
            path = "";
        } else {
            base = new File(Base.modulePath, path);
            if (!FileUtil.isAncestor(Base.modulePath, base)) {
                SysUtil.accessDenied(request);
            }
        }

        path = path + "/";
        ArrayList<Entry<String, Integer>> fileNames = IDE.getSortedFile(base);
        JSONObject content = null;
        JSONArray fileArray = new JSONArray();
        Iterator var19 = fileNames.iterator();

        String fileName = null;
        File file = null;
        JSONObject fileObject = null;
        String title = null;
        String iconCls = null;
        JSONObject button = null;
        String handler = null;
        String xtype = null;
        String afterrender = null;
        boolean selrecord = false;

        while (var19.hasNext()) {
            Entry<String, Integer> entry = (Entry)var19.next();
            fileName = (String)entry.getKey();
            file = new File(base, fileName);
            if (file.exists() && XwlBuffer.canDisplay(file, roles, displayType)) {
                boolean isFolder = file.isDirectory();
                if (isFolder) {
                    continue;
                } else {
                    content = XwlBuffer.get(path + fileName);
                }

                fileObject = new JSONObject();
                title = Str.getText(request, content.optString("title"));
                fileObject.put("text", StringUtil.select(new String[] {title, fileName}));
                iconCls = content.optString("iconCls");
                if (!StringUtil.isEmpty(iconCls)) {
                    fileObject.put("iconCls", iconCls);
                }
                fileObject.put("_tbar_button_", true);
                button = ((JSONObject)content.optJSONArray("children").get(0)).getJSONObject("configs");
                handler = button.optString("button_handler");
                if (!StringUtil.isEmpty(handler)) {
                    fileObject.put("_handler_", handler);
                }
                xtype = button.optString("button_xtype");
                if (!StringUtil.isEmpty(xtype)) {
                    fileObject.put("xtype", xtype);
                }
                afterrender = button.optString("button_afterrender");
                if (!StringUtil.isEmpty(afterrender)) {
                    fileObject.put("_afterrender_", afterrender);
                }
                selrecord = button.optBoolean("button_selrecord", false);
                fileObject.put("_selrecord_", selrecord);
                if (selrecord) {
                    fileObject.put("disabled", true);
                }
                fileArray.put(fileObject);
            }
        }

        return fileArray;
    }

    public static void getAppInfo(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String url = FileUtil.getModulePath(request.getParameter("url"));
        if (!WbUtil.canAccess(request, url)) {
            SysUtil.accessDenied(request);
        }

        WebUtil.send(response, WbUtil.getAppInfo(url, request));
    }

    public static void initHome(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String desktopString = Resource.getString(request, "desktop", (String)null);
        String[] roles = Session.getRoles(request);
        if (desktopString == null) {
            desktopString = Resource.getString("sys.home.desktop", (String)null);
        }

        String userOptions;
        int treeWidth;
        int viewIndex;
        boolean treeCollapsed;
        boolean treeHidden;
        boolean navIconHidden;
        if (desktopString != null) {
            JSONObject desktop = new JSONObject(desktopString);
            userOptions = desktop.optString("userOptions", "{}");
            treeWidth = desktop.optInt("treeWidth", 200);
            viewIndex = desktop.optInt("viewIndex", 2);
            treeCollapsed = desktop.optBoolean("treeCollapsed", false);
            treeHidden = desktop.optBoolean("treeHidden", false);
            navIconHidden = desktop.optBoolean("navIconHidden", true);
            JSONArray pages = desktop.optJSONArray("pages");
            int i;
            int activeIndex;
            String url;
            String title;
            String pageLink;
            JSONObject portlet;
            JSONObject module;
            if (pages != null) {
                i = pages.length();
                activeIndex = desktop.optInt("active", 0);
                JSONArray tabItems = new JSONArray();

                for (int portlets = 0; portlets < i; ++portlets) {
                    JSONObject page = pages.optJSONObject(portlets);
                    pageLink = page.optString("url");
                    String k = FileUtil.getModulePath(pageLink, true);
                    portlet = XwlBuffer.get(k, true);
                    if (portlet != null && !WbUtil.canAccess(portlet, k, roles)) {
                        portlet = null;
                    }

                    if (portlet == null) {
                        if (i <= activeIndex) {
                            --activeIndex;
                        }
                    } else {
                        module = new JSONObject();
                        module.put("url", pageLink);
                        String l = Str.getText(request, (String)portlet.opt("title"));
                        module.put("title", StringUtil.select(new String[] {l, FileUtil.getFilename(k)}));
                        module.put("iconCls", (String)portlet.opt("iconCls"));
                        module.put("inframe", Boolean.TRUE.equals(portlet.opt("inframe")));
                        title = page.optString("params");
                        if (!StringUtil.isEmpty(title)) {
                            module.put("params", new JSONObject(title));
                        }

                        url = (String)portlet.opt("pageLink");
                        if (!StringUtil.isEmpty(url)) {
                            JsonUtil.apply(module, new JSONObject(url));
                        }

                        tabItems.put(module);
                    }
                }

                request.setAttribute("activeIndex", Math.max(activeIndex, 0));
                request.setAttribute("tabItems", StringUtil.text(tabItems.toString()));
            }

            JSONArray portlets = desktop.optJSONArray("portlets");
            if (portlets != null) {
                activeIndex = portlets.length();

                for (i = 0; i < activeIndex; ++i) {
                    JSONArray cols = portlets.optJSONArray(i);
                    int l = cols.length();

                    for (int k = 0; k < l; ++k) {
                        portlet = cols.optJSONObject(k);
                        url = FileUtil.getModulePath(portlet.optString("url"), true);
                        module = XwlBuffer.get(url, true);
                        if (module != null && !WbUtil.canAccess(module, url, roles)) {
                            module = null;
                        }

                        if (module == null) {
                            cols.remove(k);
                            --k;
                            --l;
                        } else {
                            title = Str.getText(request, (String)module.opt("title"));
                            portlet.put("title", StringUtil.select(new String[] {title, FileUtil.getFilename(url)}));
                            portlet.put("iconCls", (String)module.opt("iconCls"));
                            portlet.put("inframe", Boolean.TRUE.equals(module.opt("inframe")));
                            pageLink = (String)module.opt("pageLink");
                            if (!StringUtil.isEmpty(pageLink)) {
                                JsonUtil.apply(portlet, new JSONObject(pageLink));
                            }
                        }
                    }
                }

                request.setAttribute("portlets", StringUtil.text(portlets.toString()));
            }
        } else {
            userOptions = "{}";
            treeWidth = 200;
            viewIndex = 2;
            treeCollapsed = false;
            treeHidden = false;
            navIconHidden = true;
        }

        request.setAttribute("treeWidth", treeWidth);
        request.setAttribute("viewIndex", viewIndex);
        request.setAttribute("treeCollapsed", treeCollapsed);
        request.setAttribute("treeHidden", treeHidden);
        request.setAttribute("navIconHidden", navIconHidden);
        request.setAttribute("userOptions", StringUtil.text(userOptions));
    }

    public static void saveDesktop(HttpServletRequest request, HttpServletResponse response) throws Exception {
        doSaveDesktop(request, 1);
    }

    public static void saveAsDefaultDesktop(HttpServletRequest request, HttpServletResponse response) throws Exception {
        doSaveDesktop(request, 2);
    }

    public static void saveAsAllDesktop(HttpServletRequest request, HttpServletResponse response) throws Exception {
        doSaveDesktop(request, 3);
    }

    private static void doSaveDesktop(HttpServletRequest request, int type) throws Exception {
        JSONObject desktop = new JSONObject();
        desktop.put("treeWidth", Integer.parseInt(request.getParameter("treeWidth")));
        desktop.put("viewIndex", Integer.parseInt(request.getParameter("viewIndex")));
        desktop.put("treeCollapsed", Boolean.parseBoolean(request.getParameter("treeCollapsed")));
        desktop.put("treeHidden", Boolean.parseBoolean(request.getParameter("treeHidden")));
        desktop.put("navIconHidden", Boolean.parseBoolean(request.getParameter("navIconHidden")));
        desktop.put("pages", new JSONArray(request.getParameter("pages")));
        desktop.put("portlets", new JSONArray(request.getParameter("portlets")));
        desktop.put("active", Integer.parseInt(request.getParameter("active")));
        desktop.put("userOptions", request.getParameter("userOptions"));
        if (type == 1) {
            Resource.set(request, "desktop", desktop.toString());
        } else {
            if (type == 3) {
                DbUtil.run(request, "delete from WB_RESOURCE where RES_ID like '%@desktop'");
            }

            Resource.set("sys.home.desktop", desktop.toString());
        }

    }

    public static void saveUserOptions(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String desktopString = Resource.getString(request, "desktop", (String)null);
        if (desktopString == null) {
            desktopString = Resource.getString("sys.home.desktop", (String)null);
        }

        JSONObject desktop = new JSONObject(StringUtil.select(new String[] {desktopString, "{}"}));
        desktop.put("userOptions", request.getParameter("userOptions"));
        Resource.set(request, "desktop", desktop.toString());
    }

    public static String getButtonListText(HttpServletRequest request) throws Exception {
        return getButtonList(request, request.getParameter("xwl"), Session.getRoles(request), 1).toString();
    }

    public static String getAppListText(HttpServletRequest request) throws Exception {
        JSONObject result = new JSONObject();
        result.put("fileName", "Root");
        result.put("children", getModuleList(request, request.getParameter("path"), Session.getRoles(request), 1));
        return result.toString();
    }

    public static void getAppList(HttpServletRequest request, HttpServletResponse response) throws Exception {
        WebUtil.send(response, getAppListText(request));
    }

    public static void getPermList(HttpServletRequest request, HttpServletResponse response) throws Exception {
        WebUtil.send(response, (new JSONObject()).put("children",
            getModuleList(request, request.getParameter("path"), Session.getRoles(request), 2)));
    }

    public static void getUserPermList(HttpServletRequest request, HttpServletResponse response) throws Exception {
        WebUtil.send(response, (new JSONObject()).put("children",
            getModuleList(request, request.getParameter("path"), Session.getRoles(request), 3)));
    }

    public static void getAllList(HttpServletRequest request, HttpServletResponse response) throws Exception {
        WebUtil.send(response, (new JSONObject()).put("children",
            getModuleList(request, request.getParameter("path"), Session.getRoles(request), 4)));
    }

    public static void setTheme(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String theme = request.getParameter("theme");
        Value.set(request, "theme", theme);
        WebUtil.setSessionValue(request, "sys.theme", theme);
    }

    public static void setTouchTheme(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String theme = request.getParameter("theme");
        Value.set(request, "touchTheme", theme);
        WebUtil.setSessionValue(request, "sys.touchTheme", theme);
    }

    public static void initTouchHome(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession(false);
        boolean isNotLogin = session == null || session.getAttribute("sys.logined") == null;
        request.setAttribute("isNotLogin", isNotLogin ? 1 : 0);
    }

    private static void searchModule(HttpServletRequest request, HttpServletResponse response, boolean isPerm)
        throws Exception {
        JSONArray array = new JSONArray();
        String query = request.getParameter("query").toLowerCase();
        String[] roles = Session.getRoles(request);
        if (query.isEmpty()) {
            query = ".xwl";
        }

        doSearchFile(request, Base.modulePath, query.toLowerCase(), "", "", array, isPerm, roles);
        WebUtil.send(response, (new JSONObject()).put("rows", array));
    }

    public static void searchAppModule(HttpServletRequest request, HttpServletResponse response) throws Exception {
        searchModule(request, response, false);
    }

    public static void searchPermModule(HttpServletRequest request, HttpServletResponse response) throws Exception {
        searchModule(request, response, true);
    }

    private static boolean doSearchFile(HttpServletRequest request, File folder, String searchName, String parentText,
        String parentFile, JSONArray array, boolean isPerm, String[] roles) throws Exception {
        File[] files = FileUtil.listFiles(folder);
        File[] var14 = files;
        int var13 = files.length;

        for (int var12 = 0; var12 < var13; ++var12) {
            File file = var14[var12];
            if (XwlBuffer.canDisplay(file, roles, isPerm ? 3 : 1)) {
                if (file.isDirectory()) {
                    File indexFile = new File(file, "folder.json");
                    String folderFile = file.getName();
                    String folderTitle;
                    JSONObject jo;
                    if (indexFile.exists()) {
                        jo = JsonUtil.readObject(indexFile);
                        folderTitle = jo.optString("title");
                        if (folderTitle.isEmpty()) {
                            folderTitle = folderFile;
                        }
                    } else {
                        folderTitle = folderFile;
                    }

                    folderTitle = Str.getText(request, folderTitle);
                    if (!isPerm && folderTitle.toLowerCase().indexOf(searchName) != -1) {
                        jo = new JSONObject();
                        jo.put("path", parentText);
                        jo.put("title", folderTitle);
                        jo.put("file", folderFile);
                        jo.put("parentFile", parentFile);
                        jo.put("isFolder", true);
                        array.put(jo);
                        if (array.length() > 99) {
                            return true;
                        }
                    }

                    if (doSearchFile(request, file, searchName,
                        StringUtil.concat(new String[] {parentText, "/", folderTitle}),
                        StringUtil.concat(new String[] {parentFile, "/", folderFile}), array, isPerm, roles)) {
                        return true;
                    }
                } else {
                    String path = FileUtil.getModulePath(file);
                    if (path.endsWith(".xwl")) {
                        JSONObject moduleData = XwlBuffer.get(path);
                        String title = moduleData.optString("title");
                        if (title.isEmpty()) {
                            title = path.substring(path.lastIndexOf(47) + 1);
                        }

                        title = Str.getText(request, title);
                        if (title.toLowerCase().indexOf(searchName) != -1) {
                            JSONObject jo = new JSONObject();
                            jo.put("path", parentText);
                            jo.put("title", title);
                            jo.put("file", file.getName());
                            jo.put("parentFile", parentFile);
                            jo.put("isFolder", false);
                            array.put(jo);
                            if (array.length() > 99) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    public static void getMobileAppList(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ArrayList<JSONObject> appList = new ArrayList();
        JSONArray outputList = new JSONArray();
        String[] roles = Session.getRoles(request);
        scanMobileApp(request, appList, new File(Base.modulePath, "apps"), roles);
        Iterator var6 = appList.iterator();

        while (var6.hasNext()) {
            JSONObject jo = (JSONObject)var6.next();
            outputList.put(jo);
        }

        WebUtil.send(response, (new JSONObject()).put("rows", outputList));
    }

    private static void scanMobileApp(HttpServletRequest request, ArrayList<JSONObject> appList, File path,
        String[] roles) throws Exception {
        ArrayList<Entry<String, Integer>> fileNames = IDE.getSortedFile(path);
        Iterator var15 = fileNames.iterator();

        while (var15.hasNext()) {
            Entry<String, Integer> entry = (Entry)var15.next();
            String fileName = (String)entry.getKey();
            File file = new File(path, fileName);
            if (XwlBuffer.canDisplay(file, roles, 2)) {
                if (file.isDirectory()) {
                    scanMobileApp(request, appList, file, roles);
                } else {
                    String url = FileUtil.getModulePath(file);
                    JSONObject content = XwlBuffer.get(url, true);
                    if (content != null) {
                        JSONObject viewport = getViewport(content);
                        if (viewport != null) {
                            viewport = viewport.getJSONObject("configs");
                            JSONObject item = new JSONObject();
                            String title = Str.getText(request, content.optString("title"));
                            item.put("title", StringUtil.select(new String[] {title, file.getName()}));
                            String image = viewport.optString("appImage");
                            if (image.isEmpty()) {
                                String glyph = viewport.optString("appGlyph");
                                if (glyph.isEmpty()) {
                                    item.put("glyph", "&#xf10a;");
                                } else {
                                    item.put("glyph", StringUtil.concat(new String[] {"&#x", glyph, ";"}));
                                }
                            } else {
                                item.put("image", image);
                            }

                            item.put("url", url);
                            appList.add(item);
                        }
                    }
                }
            }
        }

    }

    private static JSONObject getViewport(JSONObject rootNode) throws Exception {
        JSONObject module = (JSONObject)((JSONArray)rootNode.opt("children")).opt(0);
        JSONArray items = (JSONArray)module.opt("children");
        if (items == null) {
            return null;
        } else {
            int j = items.length();

            for (int i = 0; i < j; ++i) {
                JSONObject jo = (JSONObject)items.opt(i);
                if ("tviewport".equals(jo.opt("type"))) {
                    return jo;
                }
            }

            return null;
        }
    }
}