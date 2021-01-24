package com.wb.common;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import com.wb.tool.TaskManager;
import com.wb.util.FileUtil;
import com.wb.util.StringUtil;
import com.wb.util.SysUtil;

public class Base implements Filter, ApplicationListener<ContextRefreshedEvent> {
    public static ServletContext servletContext;
    public static File path;
    public static int pathLen;
    public static File modulePath;
    public static String modulePathText;
    public static int modulePathLen;
    public static Date startTime;
    public static int rootPathLen;
    public static ConcurrentHashMap<String, Object> map = new ConcurrentHashMap();
    private static boolean initFailed;
    private static Throwable initError;

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        if (initFailed) {
            throw new RuntimeException(initError);
        } else {
            HttpServletRequest req = (HttpServletRequest)request;
            HttpServletResponse resp = (HttpServletResponse)response;
            String url;
            if (Var.useServletPath) {
                url = req.getServletPath();
            } else {
                url = req.getRequestURI().substring(rootPathLen);
            }
            String xwl = UrlBuffer.get(url);
            if (xwl != null) {
                request.setCharacterEncoding("utf-8");
                if (xwl.isEmpty()) {
                    xwl = request.getParameter("xwl");
                    if (xwl == null) {
                        resp.sendError(400, "null xwl");
                        return;
                    }

                    xwl = StringUtil.concat(new String[] {xwl, ".xwl"});
                }
                this.setRequest(req);
                Parser parser = new Parser(req, resp);
                parser.parse(xwl);
            } else {
                String lowerUrl = url.toLowerCase();
                if (!lowerUrl.startsWith("/wb/modules/") && !lowerUrl.startsWith("/wb/system/")) {
                    if (Var.cacheEnabled && lowerUrl.startsWith("/wb/")) {
                        FileBuffer.service(url, req, resp);
                    } else {
                        chain.doFilter(request, response);
                    }
                } else {
                    resp.sendError(403, url);
                }
            }
        }
    }

    public void init(FilterConfig config) throws ServletException {
        try {
            startTime = new Date();
            servletContext = config.getServletContext();
            path = new File(servletContext.getRealPath("/"));
            pathLen = FileUtil.getPath(path).length() + 1;
            modulePath = new File(path, "wb/modules");
            modulePathText = FileUtil.getPath(modulePath);
            modulePathLen = modulePathText.length() + 1;
            rootPathLen = servletContext.getContextPath().length();
            /**
             * SysUtil.reload(2); if (!Var.jndi.isEmpty() && !Var.getBool("sys.service.allowInstall")) {
             * SysUtil.reload(3); TaskManager.start(); }
             **/
            this.runInitScript();
        } catch (Throwable var3) {
            initFailed = true;
            initError = var3;
        }
    }

    private void setRequest(HttpServletRequest request) {
        long time = System.currentTimeMillis();
        request.setAttribute("sys.date", new Date(time - time % 1000L));
        request.setAttribute("sys.id", SysUtil.getId());
    }

    private void runInitScript() throws Exception {
        File file = new File(path, "wb/system/init.js");
        if (file.exists()) {
            ScriptBuffer.run(FileUtil.readString(file), "init.js");
        }
    }

    public void destroy() {
        try {
            TaskManager.stop();
        } catch (Throwable var2) {
            throw new RuntimeException(var2);
        }
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        try {
            SysUtil.reload(2);
            if (!Var.jndi.isEmpty() && !Var.getBool("sys.service.allowInstall")) {
                SysUtil.reload(3);
                TaskManager.start();
            }
            this.runInitScript();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}