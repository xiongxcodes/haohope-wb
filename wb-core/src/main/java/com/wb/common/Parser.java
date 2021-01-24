package com.wb.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.wb.controls.Control;
import com.wb.controls.ExtControl;
import com.wb.controls.ScriptControl;
import com.wb.controls.ServerScript;
import com.wb.interact.Controls;
import com.wb.interact.Portal;
import com.wb.util.DbUtil;
import com.wb.util.FileUtil;
import com.wb.util.JsonUtil;
import com.wb.util.LogUtil;
import com.wb.util.StringUtil;
import com.wb.util.SysUtil;
import com.wb.util.WbUtil;
import com.wb.util.WebUtil;

public class Parser {
    private StringBuilder headerHtml = new StringBuilder();
    private ArrayList<String> footerHtml = new ArrayList(15);
    private int htmlPointer;
    private StringBuilder headerScript = new StringBuilder();
    private ArrayList<String> footerScript = new ArrayList(15);
    private int scriptPointer;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private boolean notLoadNone;
    public static final int RUN_NORMAL = 0;
    public static final int RUN_MODULE = 1;
    public static final int RUN_CONTROL = 2;
    public static final int RUN_INVOKE = 3;
    public static final int RUN_INNER = 4;
    public static final int RUN_INNER_INVOKE = 5;

    public Parser(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    public void parse(String moduleFile) throws ServletException {
        this.parse(moduleFile, false, this.request.getParameter("xwlt") != null);
    }

    public void parse(String moduleFile, boolean innerMode, boolean isInvoke) throws ServletException {
        boolean hasExcept = false;
        List<FileItem> fileItemList = null;
        ConcurrentHashMap<String, Object> varMap = null;
        Object object = this.request.getAttribute("sysx.varMap");
        if (object == null) {
            varMap = new ConcurrentHashMap();
            this.request.setAttribute("sysx.varMap", varMap);
        } else {
            varMap = JSONObject.toConHashMap(object);
        }

        try {
            if (SysUtil.isNotCustomRequest(this.request) && ServletFileUpload.isMultipartContent(this.request)) {
                fileItemList = WebUtil.setUploadFile(this.request);
            }

            int runMode;
            if (innerMode) {
                runMode = isInvoke ? 5 : 4;
            } else {
                runMode = isInvoke ? 3 : 0;
            }

            this.execute(moduleFile, runMode, (String)null, (String)null);
        } catch (Throwable var13) {
            hasExcept = true;
            if (innerMode) {
                throw new ServletException(var13);
            }

            WebUtil.showException(var13, this.request, this.response);
        } finally {
            this.closeObjects(varMap, hasExcept);
            if (fileItemList != null) {
                WebUtil.clearUploadFile(this.request, fileItemList);
            }

        }

    }

    public void execute(String moduleFile, int runMode, String xwlId, String params) throws Exception {
        JSONObject fullModule = XwlBuffer.get(moduleFile);
        JSONObject module = (JSONObject)((JSONArray)fullModule.opt("children")).opt(0);
        JSONObject configs = (JSONObject)module.opt("configs");
        boolean runNormal = runMode == 0;
        boolean runInvoke = runMode == 3;
        if (runNormal || runInvoke) {
            String tokens = this.getString(configs, "tokens");
            if (tokens.isEmpty() || !this.checkToken(tokens)) {
                String method = this.getString(configs, "method");
                if (!method.isEmpty() && !method.equalsIgnoreCase(this.request.getMethod())) {
                    throw new IllegalArgumentException("Method not allowed");
                }

                if (Boolean.TRUE.equals(fullModule.opt("internalCall"))) {
                    throw new IllegalArgumentException(Str.format(this.request, "internalCall",
                        new Object[] {
                            StringUtil.select(new String[] {Str.getText(this.request, fullModule.optString("title")),
                                FileUtil.getFilename(moduleFile)}),
                            moduleFile}));
                }

                if (Boolean.TRUE.equals(fullModule.opt("loginRequired"))) {
                    if (!WebUtil.checkLogin(this.request, this.response)) {
                        return;
                    }

                    if (!WbUtil.canAccess(fullModule, moduleFile, Session.getRoles(this.request))) {
                        /**
                         * if ("d".equals(Var.getString("sys.app.versionType"))) { throw new Exception( "您没有权限访问该功能，请<a
                         * href=\"http://www.geejing.com/download.html\" target=\"_blank\">下载软件</a>后安装到本地使用完全的功能。"); }
                         **/

                        throw new Exception(Str.format(this.request, "forbidden",
                            new Object[] {StringUtil
                                .select(new String[] {Str.getText(this.request, fullModule.optString("title")),
                                    FileUtil.getFilename(moduleFile)}),
                                moduleFile}));
                    }
                }
            }
        }

        JSONObject events = (JSONObject)module.opt("events");
        JSONObject emptyJson = new JSONObject();
        JSONObject moduleGeneral = (JSONObject)Controls.get("module").opt("general");
        String theme = null;
        String touchTheme = null;
        boolean[] libTypes = null;
        boolean hasChildren = module.has("children");
        boolean hasEvents = events != null;
        HttpSession session = null;
        if (runMode == 4) {
            runNormal = true;
        } else if (runMode == 5) {
            runInvoke = true;
        }

        String content = this.getString(configs, "logMessage");
        if (!content.isEmpty()) {
            LogUtil.info(this.request, content);
        }

        content = ServerScript.getScript(configs, "initScript");
        if (!content.isEmpty()) {
            ScriptBuffer.run(StringUtil.concat(new String[] {(String)configs.opt("id"), ".is"}), content, this.request,
                this.response, moduleFile);
        }

        content = this.getString(configs, "serverMethod");
        if (!content.isEmpty()) {
            SysUtil.executeMethod(content, this.request, this.response);
        }

        boolean createFrame = this.getBool(configs, "createFrame", true);
        String beforeunload;
        if (createFrame && runNormal) {
            this.headerHtml.append(
                "<!DOCTYPE html>\n<html>\n<head>\n<meta http-equiv=\"content-type\" content=\"text/html;charset=utf-8\"/>\n<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge,chrome=1\">\n<meta name=\"description\" content=\"Welcome to www.geejing.com, we provide excellent solutions.\"/>\n<title>");
            beforeunload = this.getString(configs, "title");
            if (beforeunload.isEmpty()) {
                beforeunload = Str.getText(this.request, fullModule.optString("title"));
            } else if (beforeunload.equals("-")) {
                beforeunload = null;
            }

            if (!StringUtil.isEmpty(beforeunload)) {
                this.headerHtml.append(beforeunload);
            }

            this.headerHtml.append("</title>");
            this.appendScript(this.headerHtml, this.getString(configs, "head"));
            session = this.request.getSession(false);
            theme = session == null ? null : (String)session.getAttribute("sys.theme");
            if (theme == null) {
                theme = Var.getString("sys.app.theme");
            }

            touchTheme = session == null ? null : (String)session.getAttribute("sys.touchTheme");
            if (touchTheme == null) {
                touchTheme = Var.getString("sys.app.touchTheme");
            }

            libTypes = this.setLinks(configs, theme, touchTheme);
            String tagConfigs = this.getString(configs, "tagConfigs");
            this.appendScript(this.headerHtml, this.getString(configs, "headLast"));
            if (tagConfigs.isEmpty()) {
                this.headerHtml.append("\n</head>\n<body>");
            } else {
                this.headerHtml.append("\n</head>\n<body ");
                this.headerHtml.append(tagConfigs);
                this.headerHtml.append('>');
            }

            this.headerScript.append("<script language=\"javascript\" type=\"text/javascript\">");
        }

        if (createFrame && runInvoke) {
            this.addLinksScript(configs);
        }

        this.appendScript(this.headerHtml, this.getString(configs, "initHtml"));
        if (createFrame) {
            if (this.headerScript.length() > 0) {
                this.headerScript.append('\n');
            }

            if (runNormal && libTypes[1]) {
                this.headerScript.append("Ext.onReady(function(contextOptions,contextOwner){");
            } else if (runNormal && libTypes[2]) {
                this.headerScript.append("Ext.setup({");
                if (hasChildren) {
                    this.headerScript
                        .append(this.getTouchViewport((JSONArray)module.opt("children"), moduleGeneral, runNormal));
                }

                this.headerScript.append("onReady:function(contextOptions,contextOwner){");
            } else {
                this.headerScript.append("(function(contextOptions,contextOwner){");
            }

            String namespace = (String)configs.opt("itemId");
            if (namespace.equals("module")) {
                this.headerScript.append("\nvar app={isXNS:\"");
                this.headerScript.append(SysUtil.getId());
                this.headerScript.append("\"};");
            } else {
                this.headerScript.append("\nWb.ns(\"");
                this.headerScript.append(namespace);
                this.headerScript.append("\");\nvar app=");
                this.headerScript.append(namespace);
                this.headerScript.append(";\napp.isXNS=\"");
                this.headerScript.append(SysUtil.getId());
                this.headerScript.append("\";");
            }

            if (runNormal && libTypes[2]) {
                this.headerScript.append("\nthis.appScope=app;\napp[this.itemId]=this;");
            }

            this.headerScript.append("\napp.contextOwner=contextOwner;");
            if (runNormal) {
                this.headerScript.append("\nwindow.app=app;");
                if (this.notLoadNone) {
                    this.headerScript.append("\nWb.init({zo:");
                    if (Var.useLocalTime) {
                        Calendar cal = Calendar.getInstance();
                        this.headerScript.append((cal.get(15) + cal.get(16)) / '');
                    } else {
                        this.headerScript.append("-1");
                    }

                    this.headerScript.append(",lang:\"");
                    this.headerScript.append(Str.getLanguage(this.request));
                    this.headerScript.append('"');
                    if (Var.maskTimeout != 2000) {
                        this.headerScript.append(",mask:");
                        this.headerScript.append(Var.maskTimeout);
                    }

                    if (Var.ajaxTimeout != 0) {
                        this.headerScript.append(",timeout:");
                        this.headerScript.append(Var.ajaxTimeout);
                    }

                    if (!"modern".equals(theme)) {
                        this.headerScript.append(",theme:\"");
                        this.headerScript.append(theme);
                        this.headerScript.append('"');
                    }

                    if (!"classic".equals(touchTheme)) {
                        this.headerScript.append(",touchTheme:\"");
                        this.headerScript.append(touchTheme);
                        this.headerScript.append('"');
                    }

                    theme = session == null ? null : (String)session.getAttribute("sys.editTheme");
                    if (theme == null) {
                        theme = Var.getString("sys.ide.editTheme");
                    }

                    if (!"default".equals(theme)) {
                        this.headerScript.append(",editTheme:\"");
                        this.headerScript.append(theme);
                        this.headerScript.append('"');
                    }

                    this.headerScript.append("});");
                }
            } else if ((runMode == 2 || runMode == 1) && xwlId != null) {
                this.headerScript.append("\ncontextOwner[");
                this.headerScript.append(StringUtil.quote(xwlId));
                this.headerScript.append("]=app;");
            }
        }

        content = this.getString(configs, "importModules");
        if (!content.isEmpty()) {
            this.importModules(content);
        }

        content = ServerScript.getScript(configs, "serverScript");
        if (!content.isEmpty()) {
            ScriptBuffer.run(StringUtil.concat(new String[] {(String)configs.opt("id"), ".ss"}), content, this.request,
                this.response, moduleFile);
        }

        if (hasEvents) {
            beforeunload = this.getString(events, "beforeunload");
            if (!beforeunload.isEmpty()) {
                this.appendScript(this.headerScript,
                    StringUtil.concat(new String[] {"Wb.onUnload(function(){\n", beforeunload, "\n},contextOwner);"}));
            }

            this.appendScript(this.headerScript, this.getString(events, "initialize"));
        }

        if (hasChildren) {
            this.scan(module, moduleGeneral, emptyJson, runNormal);
        }

        if (!this.response.isCommitted()) {
            this.appendScript(this.headerHtml, this.getString(configs, "finalHtml"));
            if (hasEvents) {
                this.appendScript(this.headerScript, this.getString(events, "finalize"));
            }

            if (createFrame) {
                if (runNormal) {
                    if (libTypes[1]) {
                        this.headerScript.append("\n});");
                    } else if (libTypes[2]) {
                        this.headerScript.append("\n}});");
                    } else {
                        this.headerScript.append("\n})({});");
                    }
                } else if (runMode == 2) {
                    this.headerScript.append("\nreturn Wb.optMain(app);\n})(");
                    this.headerScript.append(params == null ? "{}" : params);
                    this.headerScript.append(",app)");
                } else if (runMode == 1) {
                    this.headerScript.append("\n})({},app);");
                } else {
                    try {
                        String buttons = Portal.getButtonListText(request);
                        this.headerScript.append("\ntry{app.grid._buttons_ = " + buttons + ";}catch(e){}\n");
                    } catch (Exception e) {
                    }

                    this.headerScript.append("\nreturn app;\n})();");
                }
            }

            if (runNormal) {
                if (createFrame) {
                    this.headerScript.append("\n</script>\n</body>\n</html>");
                }

                this.output();
            } else if (runInvoke) {
                this.output();
            }

        }
    }

    private boolean checkToken(String tokens) {
        String token = this.request.getParameter("_token");
        if (StringUtil.isEmpty(token)) {
            return false;
        } else {
            String[] ls = StringUtil.split(tokens, ",");
            String[] var7 = ls;
            int var6 = ls.length;

            for (int var5 = 0; var5 < var6; ++var5) {
                String s = var7[var5];
                if (token.equals(s.trim())) {
                    return true;
                }
            }

            return false;
        }
    }

    public void output() throws IOException {
        if (this.headerHtml.length() > 0 && this.headerScript.length() > 0) {
            this.headerHtml.append('\n');
        }

        this.headerHtml.append(this.headerScript);
        if (this.headerHtml.length() > 0) {
            if (WebUtil.jsonResponse(this.request)) {
                WebUtil.send(this.response, this.headerHtml.toString(), true);
            } else {
                WebUtil.send(this.response, this.headerHtml);
            }
        }

    }

    private void scan(JSONObject parentNode, JSONObject parentGeneral, JSONObject emptyJson, boolean normalType)
        throws Exception {
        JSONArray ja = (JSONArray)parentNode.opt("children");
        int j = ja.length();
        int k = j - 1;

        for (int i = 0; i < j; ++i) {
            JSONObject jo = (JSONObject)ja.opt(i);
            String type = (String)jo.opt("type");
            JSONObject meta = Controls.get(type);
            JSONObject general = (JSONObject)meta.opt("general");
            String className = (String)general.opt("class");
            Object control;
            boolean isScriptControl;
            if (className == null) {
                control = new ExtControl();
                isScriptControl = true;
            } else if (className.equals("null")) {
                control = null;
                isScriptControl = false;
                if (type.equals("xwl")) {
                    boolean rootParent = Boolean.TRUE.equals(parentGeneral.opt("root"));
                    this.addModule(jo, rootParent);
                    if (!rootParent && i < j - 1) {
                        this.headerScript.append(',');
                    }
                }
            } else {
                if (className.indexOf(46) == -1) {
                    className = "com.wb.controls." + className;
                }

                control = (Control)Class.forName(className).newInstance();
                isScriptControl = control instanceof ScriptControl;
            }

            if (control != null) {
                ((Control)control).init(this.request, this.response, jo, meta, parentGeneral, i == k, normalType);
                ((Control)control).create();
            }

            if (isScriptControl) {
                ScriptControl sc = (ScriptControl)control;
                this.appendScript(this.headerHtml, sc.getHeaderHtml());
                this.pushHtml(sc.getFooterHtml());
                this.appendScript(this.headerScript, sc.getHeaderScript());
                this.pushScript(sc.getFooterScript());
            }

            if (jo.has("children")) {
                this.scan(jo, general, emptyJson, normalType);
            }

            if (isScriptControl) {
                this.appendScript(this.headerHtml, this.popHtml());
                String lastScript = this.popScript();
                int quoteIndex = lastScript.lastIndexOf(125);
                JSONObject configItems;
                if (quoteIndex != -1 && (configItems = (JSONObject)jo.opt("__configs")) != null) {
                    this.appendScript(this.headerScript, lastScript.substring(0, quoteIndex));
                    this.headerScript.append(',');
                    this.scan(configItems, emptyJson, emptyJson, normalType);
                    this.appendScript(this.headerScript, lastScript.substring(quoteIndex));
                } else {
                    this.appendScript(this.headerScript, lastScript);
                }
            }
        }

    }

    private void addModule(JSONObject jo, boolean rootParent) throws Exception {
        JSONObject configs = (JSONObject)jo.opt("configs");
        String file = this.getString(configs, "file");
        if (file != null) {
            this.execute(FileUtil.getModulePath(file), rootParent ? 1 : 2, (String)configs.opt("itemId"),
                (String)configs.opt("params"));
        }

    }

    private void pushHtml(String script) {
        ++this.htmlPointer;
        if (this.footerHtml.size() < this.htmlPointer) {
            this.footerHtml.add(script);
        } else {
            this.footerHtml.set(this.htmlPointer - 1, script);
        }

    }

    private String popHtml() {
        --this.htmlPointer;
        return (String)this.footerHtml.get(this.htmlPointer);
    }

    private void pushScript(String script) {
        ++this.scriptPointer;
        if (this.footerScript.size() < this.scriptPointer) {
            this.footerScript.add(script);
        } else {
            this.footerScript.set(this.scriptPointer - 1, script);
        }

    }

    private String popScript() {
        --this.scriptPointer;
        return (String)this.footerScript.get(this.scriptPointer);
    }

    private void appendScript(StringBuilder buf, String script) {
        if (!StringUtil.isEmpty(script)) {
            if (buf.length() > 0) {
                buf.append('\n');
            }

            buf.append(script);
        }

    }

    private void importModules(String modules) throws Exception {
        JSONArray moduleArray = new JSONArray(modules);
        int j = moduleArray.length();

        for (int i = 0; i < j; ++i) {
            this.execute(FileUtil.getModulePath((String)moduleArray.opt(i)), 1, "importXwl" + (i + 1), (String)null);
        }

    }

    private boolean[] setLinks(JSONObject configs, String theme, String touchTheme) {
        ArrayList<String> cssArray = new ArrayList();
        ArrayList<String> jsArray = new ArrayList();
        JSONArray cssLinks = null;
        JSONArray jsLinks = null;
        String loadJS = this.getString(configs, "loadJS");
        String lang = Str.getClientLanguage(this.request);
        boolean[] libTypes = new boolean[4];
        String debugSuffix;
        if (Var.debug) {
            debugSuffix = "-debug";
        } else {
            debugSuffix = "";
        }

        this.request.setAttribute("debugSuffix", debugSuffix);
        String cssLinksText = this.getString(configs, "cssLinks");
        if (cssLinksText.isEmpty()) {
            cssLinks = Var.cssLinks;
        } else {
            cssLinks = new JSONArray();
            if (Var.cssLinks != null) {
                JsonUtil.addAll(cssLinks, Var.cssLinks);
            }

            JsonUtil.addAll(cssLinks, new JSONArray(cssLinksText));
        }

        String jsLinksText = this.getString(configs, "jsLinks");
        if (jsLinksText.isEmpty()) {
            jsLinks = Var.jsLinks;
        } else {
            jsLinks = new JSONArray();
            if (Var.jsLinks != null) {
                JsonUtil.addAll(jsLinks, Var.jsLinks);
            }

            JsonUtil.addAll(jsLinks, new JSONArray(jsLinksText));
        }

        this.notLoadNone = !"none".equals(loadJS);
        if (loadJS.isEmpty()) {
            loadJS = "ext";
        }

        if (this.notLoadNone) {
            jsArray.add(StringUtil
                .concat(new String[] {"wb/script/locale/wb-lang-", Str.getLanguage(this.request), debugSuffix, ".js"}));
        }

        if (loadJS.indexOf("ext") != -1) {
            libTypes[1] = true;
            cssArray.add(StringUtil.concat(new String[] {"wb/libs/ext/resources/ext-theme-", theme, "/ext-theme-",
                theme, "-all", debugSuffix, ".css"}));
            jsArray.add(StringUtil.concat(new String[] {"wb/libs/ext/ext-all", debugSuffix, ".js"}));
            jsArray.add(StringUtil
                .concat(new String[] {"wb/libs/ext/locale/ext-lang-", Str.optExtLanguage(lang), debugSuffix, ".js"}));
        }

        if (loadJS.indexOf("touch") != -1) {
            libTypes[2] = true;
            cssArray
                .add(StringUtil.concat(new String[] {"wb/libs/touch/resources/css/", touchTheme, debugSuffix, ".css"}));
            jsArray.add(StringUtil
                .concat(new String[] {"wb/libs/touch/locale/t-lang-", Str.optTouchLanguage(lang), debugSuffix, ".js"}));
            jsArray.add(StringUtil.concat(new String[] {"wb/libs/touch/sencha-touch-all", debugSuffix, ".js"}));
        }

        if (loadJS.indexOf("bootstrap") != -1) {
            libTypes[3] = true;
            cssArray.add(StringUtil.concat(new String[] {"wb/libs/bs/css/bootstrap", debugSuffix, ".css"}));
            jsArray.add(StringUtil.concat(new String[] {"wb/libs/jquery/jquery", debugSuffix, ".js"}));
            jsArray.add(StringUtil.concat(new String[] {"wb/libs/bs/js/bootstrap", debugSuffix, ".js"}));
        }

        if (loadJS.indexOf("jquery") != -1) {
            jsArray.add("wb/libs/jquery/jquery" + debugSuffix + ".js");
        }

        if (this.notLoadNone) {
            cssArray.add(StringUtil.concat(new String[] {"wb/css/style", debugSuffix, ".css"}));
            jsArray.add(StringUtil.concat(new String[] {"wb/script/wb", debugSuffix, ".js"}));
        }

        String value;
        int i;
        int j;
        int index;
        if (cssLinks != null) {
            j = cssLinks.length();

            for (i = 0; i < j; ++i) {
                value = cssLinks.getString(i);
                index = cssArray.indexOf(value);
                if (index != -1) {
                    cssArray.remove(index);
                }

                cssArray.add(value);
            }
        }

        if (jsLinks != null) {
            j = jsLinks.length();

            for (i = 0; i < j; ++i) {
                value = jsLinks.getString(i);
                index = jsArray.indexOf(value);
                if (index != -1) {
                    jsArray.remove(index);
                }

                jsArray.add(value);
            }
        }

        Iterator var19 = cssArray.iterator();

        String js;
        while (var19.hasNext()) {
            js = (String)var19.next();
            this.headerHtml.append("\n<link type=\"text/css\" rel=\"stylesheet\" href=\"");
            this.headerHtml.append(js);
            this.headerHtml.append("\">");
        }

        var19 = jsArray.iterator();

        while (var19.hasNext()) {
            js = (String)var19.next();
            this.headerHtml.append("\n<script type=\"text/javascript\" src=\"");
            this.headerHtml.append(js);
            this.headerHtml.append("\"></script>");
        }

        return libTypes;
    }

    private void addLinksScript(JSONObject configs) {
        this.request.setAttribute("debugSuffix", Var.debug ? "-debug" : "");
        String cssLinks = this.getString(configs, "cssLinks");
        String jsLinks = this.getString(configs, "jsLinks");
        boolean emptyCss = cssLinks.isEmpty();
        boolean emptyJs = jsLinks.isEmpty();
        if (!emptyCss || !emptyJs) {
            this.headerScript.append("$$@blink{");
            if (!emptyCss) {
                this.headerScript.append("\"css\":");
                this.headerScript.append(cssLinks);
            }

            if (!emptyJs) {
                if (!emptyCss) {
                    this.headerScript.append(',');
                }

                this.headerScript.append("\"js\":");
                this.headerScript.append(jsLinks);
            }

            if (this.getBool(configs, "recursiveLoad", false)) {
                this.headerScript.append(",\"recursive\":true");
            }

            this.headerScript.append("}$$@elink");
        }

    }

    private String getString(JSONObject object, String name) {
        String value = (String)object.opt(name);
        return value == null ? "" : WebUtil.replaceParams(this.request, value);
    }

    private boolean getBool(JSONObject object, String name, boolean defaultValue) {
        String value = this.getString(object, name);
        return value.isEmpty() ? defaultValue : Boolean.parseBoolean(value);
    }

    private void closeObjects(ConcurrentHashMap<String, Object> map, boolean isExcept) {
        Set<Entry<String, Object>> es = map.entrySet();
        ArrayList<Connection> connList = new ArrayList();
        ArrayList<Statement> stList = new ArrayList();
        Iterator var8 = es.iterator();

        while (var8.hasNext()) {
            Entry<String, Object> e = (Entry)var8.next();
            Object object = e.getValue();
            if (object != null) {
                if (object instanceof ResultSet) {
                    DbUtil.close((ResultSet)object);
                } else if (object instanceof Statement) {
                    stList.add((Statement)object);
                } else if (object instanceof Connection) {
                    connList.add((Connection)object);
                } else if (object instanceof InputStream) {
                    IOUtils.closeQuietly((InputStream)object);
                } else if (object instanceof OutputStream) {
                    IOUtils.closeQuietly((OutputStream)object);
                }
            }
        }

        var8 = stList.iterator();

        while (var8.hasNext()) {
            Statement st = (Statement)var8.next();
            DbUtil.close(st);
        }

        var8 = connList.iterator();

        while (var8.hasNext()) {
            Connection conn = (Connection)var8.next();
            if (isExcept) {
                DbUtil.close(conn);
            } else {
                DbUtil.closeCommit(conn);
            }
        }

    }

    private String getTouchViewport(JSONArray items, JSONObject parentGeneral, boolean normalType) throws Exception {
        if (items == null) {
            return "";
        } else {
            JSONObject meta = Controls.get("tviewport");
            StringBuilder script = new StringBuilder();
            int j = items.length();
            int k = j - 1;

            for (int i = 0; i < j; ++i) {
                JSONObject jo = (JSONObject)items.opt(i);
                if ("tviewport".equals(jo.opt("type"))) {
                    ExtControl control = new ExtControl();
                    control.normalMode = false;
                    control.init(this.request, this.response, jo, meta, parentGeneral, i == k, normalType);
                    control.create();
                    script.append("\nviewport:");
                    script.append(control.getHeaderScript());
                    script.append(control.getFooterScript());
                    script.append(',');
                    return script.toString();
                }
            }

            return "";
        }
    }
}