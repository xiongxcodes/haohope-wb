package com.wb.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.Session;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.ProgressListener;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.wb.common.Base;
import com.wb.common.Parser;
import com.wb.common.SessionBridge;
import com.wb.common.Str;
import com.wb.common.UrlBuffer;
import com.wb.common.UserList;
import com.wb.common.Var;
import com.wb.common.XwlBuffer;
import com.wb.tool.Console;

public class WebUtil {
    public static String submit(String url) throws IOException {
        return submit(url, "GET", (JSONObject)null);
    }

    public static String submit(String url, JSONObject params) throws IOException {
        return submit(url, "POST", params);
    }

    public static String submit(String url, String method, JSONObject params) throws IOException {
        return new String(submitBytes(url, method, params, Var.getInt("sys.session.submitTimeout")), "utf-8");
    }

    public static byte[] submitBytes(String url, String method, JSONObject params, int timeout) throws IOException {
        return submitBytes(url, method, params, timeout, "application/x-www-form-urlencoded; charset=utf-8");
    }

    public static byte[] submitBytes(String url, String method, byte[] data, int timeout, String contentType)
        throws IOException {
        HttpURLConnection conn = (HttpURLConnection)(new URL(url)).openConnection();

        byte[] var10;
        try {
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
            conn.setUseCaches(false);
            conn.setRequestMethod(method);
            if (data != null) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", contentType);
                conn.setRequestProperty("Content-Length", Integer.toString(data.length));
                OutputStream os = conn.getOutputStream();

                try {
                    os.write(data);
                    os.flush();
                } finally {
                    os.close();
                }
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            InputStream is = conn.getInputStream();

            try {
                IOUtils.copy(is, bos);
            } finally {
                is.close();
            }

            var10 = bos.toByteArray();
        } finally {
            conn.disconnect();
        }

        return var10;
    }

    public static byte[] submitBytes(String url, String method, JSONObject params, int timeout, String contentType)
        throws IOException {
        byte[] data;
        if (params == null) {
            data = null;
        } else {
            data = getParamsText(params).getBytes("utf-8");
        }

        return submitBytes(url, method, data, timeout, contentType);
    }

    private static String getParamsText(JSONObject jo) throws IOException {
        StringBuilder sb = new StringBuilder();
        Set<Entry<String, Object>> es = jo.entrySet();
        boolean isFirst = true;
        Iterator var5 = es.iterator();

        while (var5.hasNext()) {
            Entry<String, Object> e = (Entry)var5.next();
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append("&");
            }

            sb.append((String)e.getKey());
            sb.append("=");
            sb.append(URLEncoder.encode(e.getValue().toString(), "utf-8"));
        }

        return sb.toString();
    }

    public static ArrayList<Session> getSocketSessions(String name) throws IOException {
        String[] users = UserList.getUsers();
        ArrayList<Session> allSessions = new ArrayList();
        String[] var7 = users;
        int var6 = users.length;

        for (int var5 = 0; var5 < var6; ++var5) {
            String user = var7[var5];
            ArrayList<Session> val = getSocketSessions(user, name);
            if (val != null) {
                allSessions.addAll(val);
            }
        }

        return allSessions;
    }

    public static ArrayList<Session> getSocketSessions(String userId, String name) throws IOException {
        HttpSession[] httpSessions = UserList.getSessions(userId);
        if (httpSessions == null) {
            return null;
        } else {
            ArrayList<Session> allSessions = new ArrayList();
            HttpSession[] var8 = httpSessions;
            int var7 = httpSessions.length;

            for (int var6 = 0; var6 < var7; ++var6) {
                HttpSession httpSession = var8[var6];
                Session[] sessions = SessionBridge.getSessions(httpSession, name);
                if (sessions != null) {
                    Session[] var12 = sessions;
                    int var11 = sessions.length;

                    for (int var10 = 0; var10 < var11; ++var10) {
                        Session session = var12[var10];
                        allSessions.add(session);
                    }
                }
            }

            return allSessions.size() > 0 ? allSessions : null;
        }
    }

    public static void sendSocketText(Session session, String data, boolean successful, boolean feedback)
        throws IOException {
        StringBuilder buf = new StringBuilder(data == null ? 44 : 44 + data.length());
        buf.append("{\"success\":");
        buf.append(successful ? "true" : "false");
        buf.append(",\"feedback\":");
        buf.append(feedback ? "true" : "false");
        buf.append(",\"data\":");
        buf.append(StringUtil.quote(data));
        buf.append("}");
        String result = buf.toString();
        Basic basic = session.getBasicRemote();
        synchronized (session) {
            basic.sendText(result);
        }
    }

    public static void sendSocketText(Session session, String data) throws IOException {
        sendSocketText(session, data, true, false);
    }

    public static void send(String name, Object object) throws IOException {
        ArrayList<Session> socketSessions = getSocketSessions(name);
        String text = object == null ? "" : object.toString();
        Iterator var5 = socketSessions.iterator();

        while (var5.hasNext()) {
            Session session = (Session)var5.next();
            if (session.isOpen()) {
                sendSocketText(session, text);
            }
        }

    }

    public static void sendWithUrl(String name, String url, Object object) throws IOException {
        ArrayList<Session> socketSessions = getSocketSessions(name);
        String text = object == null ? "" : object.toString();
        if (url.startsWith("m?xwl=")) {
            url = url.substring(6) + ".xwl";
        }

        Iterator var6 = socketSessions.iterator();

        while (var6.hasNext()) {
            Session session = (Session)var6.next();
            if (session.isOpen() && url.equals(session.getUserProperties().get("xwl"))) {
                sendSocketText(session, text);
            }
        }

    }

    public static void send(String userId, String name, Object object) throws IOException {
        ArrayList<Session> socketSessions = getSocketSessions(userId, name);
        if (socketSessions != null) {
            String text = object == null ? "" : object.toString();
            Iterator var6 = socketSessions.iterator();

            while (var6.hasNext()) {
                Session session = (Session)var6.next();
                if (session.isOpen()) {
                    sendSocketText(session, text);
                }
            }

        }
    }

    public static void send(HttpSession httpSession, String name, Object object) throws IOException {
        Session[] sessions = SessionBridge.getSessions(httpSession, name);
        if (sessions != null) {
            String text = object == null ? "" : object.toString();
            Session[] var8 = sessions;
            int var7 = sessions.length;

            for (int var6 = 0; var6 < var7; ++var6) {
                Session session = var8[var6];
                if (session.isOpen()) {
                    sendSocketText(session, text);
                }
            }

        }
    }

    public static String[] getSortInfo(HttpServletRequest request) {
        String sort = request.getParameter("sort");
        if (StringUtil.isEmpty(sort)) {
            return null;
        } else {
            JSONObject jo = (new JSONArray(sort)).getJSONObject(0);
            String[] result = new String[] {jo.getString("property"), jo.optString("direction")};
            return result;
        }
    }

    public static String encodeFilename(HttpServletRequest request, String filename) throws IOException {
        String agent = StringUtil.opt(request.getHeader("user-agent")).toLowerCase();
        if (agent.indexOf("opera") != -1) {
            return StringUtil.concat(new String[] {"filename*=\"utf-8''", encode(filename), "\""});
        } else {
            return agent.indexOf("trident") == -1 && agent.indexOf("msie") == -1 && agent.indexOf("edge") == -1
                ? StringUtil
                    .concat(new String[] {"filename=\"", new String(filename.getBytes("utf-8"), "ISO-8859-1"), "\""})
                : StringUtil.concat(new String[] {"filename=\"", encode(filename), "\""});
        }
    }

    public static String encode(String string) throws IOException {
        return StringUtil.replaceAll(URLEncoder.encode(string, "utf-8"), "+", "%20");
    }

    public static String decode(String string) throws IOException {
        return !Var.urlEncoding.isEmpty() && !StringUtil.isEmpty(string)
            ? new String(string.getBytes(Var.urlEncoding), "utf-8") : string;
    }

    public static void clearUpload(HttpServletRequest request, List<FileItem> list) {
        FileItem item;
        for (Iterator var3 = list.iterator(); var3.hasNext(); item.delete()) {
            item = (FileItem)var3.next();
            if (!item.isFormField()) {
                IOUtils.closeQuietly((InputStream)request.getAttribute(item.getFieldName()));
            }
        }

        String uploadId = (String)request.getAttribute("sys.uploadId");
        if (uploadId != null) {
            HttpSession session = request.getSession(true);
            session.removeAttribute("sys.upread." + uploadId);
            session.removeAttribute("sys.uplen." + uploadId);
        }

    }

    public static String getIdWithUser(HttpServletRequest request, String id) {
        String user = fetch(request, "sys.user");
        return StringUtil.concat(new String[] {StringUtil.opt(user), "@", id});
    }

    public static void setSessionValue(HttpServletRequest request, String name, Object value) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new RuntimeException("Session does not exist.");
        } else {
            session.setAttribute(name, value);
        }
    }

    public static Object fetchObject(HttpServletRequest request, String name) {
        HttpSession session = request.getSession(false);
        Object value;
        if (session != null && (value = session.getAttribute(name)) != null) {
            return value;
        } else {
            value = request.getAttribute(name);
            return value == null ? request.getParameter(name) : value;
        }
    }

    public static String fetch(HttpServletRequest request, String name) {
        Object object = fetchObject(request, name);
        return object == null ? null : object.toString();
    }

    public static JSONObject fetch(HttpServletRequest request) {
        JSONObject json = new JSONObject();
        Iterator<?> requestParams = request.getParameterMap().entrySet().iterator();
        Enumeration requestAttrs = request.getAttributeNames();

        while (requestParams.hasNext()) {
            Entry<?, ?> entry = (Entry)requestParams.next();
            json.put((String)entry.getKey(), ((String[])entry.getValue())[0]);
        }

        String name;
        while (requestAttrs.hasMoreElements()) {
            name = requestAttrs.nextElement().toString();
            if (!name.startsWith("sysx.")) {
                json.put(name, request.getAttribute(name));
            }
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            Enumeration sessionAttrs = session.getAttributeNames();

            while (sessionAttrs.hasMoreElements()) {
                name = sessionAttrs.nextElement().toString();
                if (!name.startsWith("sysx.")) {
                    json.put(name, session.getAttribute(name));
                }
            }
        }

        return json;
    }

    public static String replaceParams(HttpServletRequest request, String text) {
        if (request != null && !StringUtil.isEmpty(text)) {
            int start = 0;
            int startPos = text.indexOf("{#", start);
            int endPos = text.indexOf("#}", startPos + 2);
            if (startPos != -1 && endPos != -1) {
                StringBuilder buf;
                for (buf = new StringBuilder(text.length()); startPos != -1 && endPos != -1;
                    endPos = text.indexOf("#}", startPos + 2)) {
                    String paramName = text.substring(startPos + 2, endPos);
                    String paramValue;
                    if (paramName.startsWith("Var.")) {
                        paramValue = Var.getString(paramName.substring(4));
                    } else if (paramName.startsWith("Str.")) {
                        paramValue = Str.format(request, paramName.substring(4), new Object[0]);
                    } else {
                        paramValue = fetch(request, paramName);
                    }

                    buf.append(text.substring(start, startPos));
                    if (paramValue != null) {
                        buf.append(paramValue);
                    }

                    start = endPos + 2;
                    startPos = text.indexOf("{#", start);
                }

                buf.append(text.substring(start));
                return buf.toString();
            } else {
                return text;
            }
        } else {
            return text;
        }
    }

    public static void send(HttpServletResponse response, Object object) throws IOException {
        InputStream inputStream = object instanceof InputStream ? (InputStream)object : null;

        try {
            if (response.isCommitted()) {
                return;
            }

            OutputStream outputStream = response.getOutputStream();
            if (inputStream == null) {
                byte[] bytes;
                if (object instanceof byte[]) {
                    bytes = (byte[])object;
                } else {
                    String text;
                    if (object == null) {
                        text = "";
                    } else {
                        text = object.toString();
                    }

                    bytes = text.getBytes("utf-8");
                    if (StringUtil.isEmpty(response.getContentType())) {
                        response.setContentType("text/html;charset=utf-8");
                    }
                }

                int len = bytes.length;
                if (len >= Var.sendGzipMinSize && Var.sendGzipMinSize != -1) {
                    response.setHeader("Content-Encoding", "gzip");
                    GZIPOutputStream gos = new GZIPOutputStream(outputStream);

                    try {
                        gos.write(bytes);
                    } finally {
                        gos.close();
                    }
                } else {
                    response.setContentLength(len);
                    outputStream.write(bytes);
                }
            } else if (Var.sendStreamGzip) {
                response.setHeader("Content-Encoding", "gzip");
                GZIPOutputStream gos = new GZIPOutputStream(outputStream);

                try {
                    IOUtils.copy(inputStream, gos);
                } finally {
                    gos.close();
                }
            } else {
                IOUtils.copy(inputStream, outputStream);
            }

            response.flushBuffer();
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }

        }

    }

    public static void send(HttpServletResponse response, String text, boolean successful) throws IOException {
        send((HttpServletResponse)response, StringUtil.textareaQuote(StringUtil
            .concat(new String[] {"{success:", Boolean.toString(successful), ",value:", StringUtil.quote(text), "}"})));
    }

    public static void send(HttpServletResponse response, Object object, boolean successful) throws IOException {
        String text;
        if (object == null) {
            text = null;
        } else {
            text = object.toString();
        }

        send(response, text, successful);
    }

    public static boolean fromAjax(HttpServletRequest request) {
        try {
            return "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"));
        } catch (Throwable var2) {
            return false;
        }
    }

    public static void showException(Throwable exception, HttpServletRequest request, HttpServletResponse response)
        throws ServletException {
        boolean isAjax = fromAjax(request);
        boolean jsonResp = jsonResponse(request);
        boolean directOutput = isAjax || jsonResp;
        discardInputStream(request);
        String errorMessage = SysUtil.getStackMessage(exception);
        Console.printToClient(request, errorMessage, "error", false);
        if (Var.printError && !directOutput) {
            Throwable rootExcept = SysUtil.getRootExcept(exception);
            if (rootExcept instanceof ServletException) {
                throw (ServletException)rootExcept;
            } else {
                throw new ServletException(rootExcept);
            }
        } else {
            try {
                if (Var.printError) {
                    System.err.println(errorMessage);
                }

                if (!response.isCommitted()) {
                    String rootError = SysUtil.getRootError(exception);
                    response.reset();
                    if (directOutput) {
                        if (jsonResp) {
                            send(response, rootError, false);
                        } else {
                            response.setStatus(500);
                            send((HttpServletResponse)response, rootError);
                        }
                    } else {
                        response.sendError(500, rootError);
                    }
                }
            } catch (Throwable var8) {
                ;
            }
        }
    }

    public static boolean checkLogin(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("sys.logined") != null) {
            return true;
        } else {
            boolean isAjax = fromAjax(request);
            boolean jsonResp = jsonResponse(request);
            boolean directOutput = isAjax || jsonResp;
            if (directOutput) {
                if (jsonResp) {
                    send(response, "$WBE201: Login required", false);
                } else {
                    response.setStatus(401);
                    send((HttpServletResponse)response, "Login required");
                }
            } else {
                Parser parser = new Parser(request, response);
                if (isTouchModule(request)) {
                    parser.parse("sys/session/tlogin.xwl");
                } else {
                    parser.parse("sys/session/login.xwl");
                }
            }

            discardInputStream(request);
            return false;
        }
    }

    private static boolean isTouchModule(HttpServletRequest request) throws Exception {
        String url;
        if (Var.useServletPath) {
            url = request.getServletPath();
        } else {
            url = request.getRequestURI().substring(Base.rootPathLen);
        }

        String xwl = UrlBuffer.get(url);
        if (StringUtil.isEmpty(xwl)) {
            xwl = request.getParameter("xwl");
            if (StringUtil.isEmpty(xwl)) {
                return false;
            }

            xwl = StringUtil.concat(new String[] {xwl, ".xwl"});
        }

        JSONObject module = XwlBuffer.get(xwl, true);
        return module == null ? false : module.has("hasTouch");
    }

    public static boolean hasRole(HttpServletRequest request, String roleName) {
        String[] roles = (String[])fetchObject(request, "sys.roles");
        return StringUtil.indexOf(roles, roleName) != -1;
    }

    public static List<FileItem> setUploadFile(HttpServletRequest request) throws Exception {
        DiskFileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        long maxSize = (long)Var.getInt("sys.service.upload.maxSize");
        long totalMaxSize = (long)Var.getInt("sys.service.upload.totalMaxSize");
        final String uploadId = request.getParameter("uploadId");
        HashMap<String, Integer> multiFilesMap = null;
        if (totalMaxSize != -1L) {
            upload.setSizeMax(totalMaxSize * 1024L);
        }

        long maxByteSize;
        if (maxSize == -1L) {
            maxByteSize = -1L;
        } else {
            maxByteSize = maxSize * 1024L;
        }

        factory.setSizeThreshold(Var.getInt("sys.service.upload.bufferSize"));
        if (uploadId != null && uploadId.indexOf(46) == -1) {
            request.setAttribute("sys.uploadId", uploadId);
            final HttpSession session = request.getSession(true);
            if (session != null) {
                upload.setProgressListener(new ProgressListener() {
                    public void update(long read, long length, int id) {
                        session.setAttribute("sys.upread." + uploadId, Long.valueOf(read));
                        session.setAttribute("sys.uplen." + uploadId, Long.valueOf(length));
                    }
                });
            }
        }

        List<FileItem> list = upload.parseRequest(request);
        if (list != null && list.size() != 0) {
            try {
                Iterator var19 = list.iterator();

                while (true) {
                    while (true) {
                        String fieldName;
                        FileItem item;
                        do {
                            do {
                                if (!var19.hasNext()) {
                                    return list;
                                }

                                item = (FileItem)var19.next();
                                fieldName = item.getFieldName();
                            } while (fieldName.indexOf(46) != -1);
                        } while (fieldName.indexOf(64) != -1);

                        if (item.isFormField()) {
                            if (request.getAttribute(fieldName) != null) {
                                throw new RuntimeException("Duplicate parameters \"" + fieldName + "\" found.");
                            }

                            request.setAttribute(fieldName, item.getString("utf-8"));
                        } else {
                            String fileName = FileUtil.getFilename(item.getName());
                            long fileSize = item.getSize();
                            if (maxByteSize != -1L && fileSize > maxByteSize) {
                                throw new IllegalArgumentException("Maximum allowable size is " + maxSize + "KB");
                            }

                            if (request.getAttribute(fieldName) != null) {
                                if (multiFilesMap == null) {
                                    multiFilesMap = new HashMap();
                                }

                                Integer fileIndex = (Integer)multiFilesMap.get(fieldName);
                                if (fileIndex == null) {
                                    fileIndex = 0;
                                }

                                fileIndex = fileIndex + 1;
                                multiFilesMap.put(fieldName, fileIndex);
                                String fileIndexText = "@" + Integer.toString(fileIndex);
                                if (StringUtil.isEmpty(fileName) && fileSize == 0L) {
                                    request.setAttribute(fieldName + fileIndexText, "");
                                } else {
                                    request.setAttribute(fieldName + fileIndexText, item.getInputStream());
                                }

                                request.setAttribute(
                                    StringUtil.concat(new String[] {fieldName, fileIndexText, "__name"}), fileName);
                                request.setAttribute(
                                    StringUtil.concat(new String[] {fieldName, fileIndexText, "__size"}), fileSize);
                            } else {
                                if (StringUtil.isEmpty(fileName) && fileSize == 0L) {
                                    request.setAttribute(fieldName, "");
                                } else {
                                    request.setAttribute(fieldName, item.getInputStream());
                                }

                                request.setAttribute(fieldName + "__name", fileName);
                                request.setAttribute(fieldName + "__size", fileSize);
                            }
                        }
                    }
                }
            } catch (Throwable var20) {
                clearUploadFile(request, list);
                throw new Exception(var20);
            }
        } else {
            return null;
        }
    }

    public static boolean hasFile(HttpServletRequest request, String name) {
        return request.getAttribute(name) instanceof InputStream;
    }

    public static void discardInputStream(HttpServletRequest request) {
        try {
            InputStream stream = request.getInputStream();
            byte[] skipBuffer = new byte[2048];

            while (stream.read(skipBuffer) != -1) {
                ;
            }
        } catch (Throwable var3) {
            ;
        }

    }

    public static void clearUploadFile(HttpServletRequest request, List<FileItem> list) {
        Iterator var4 = list.iterator();

        while (var4.hasNext()) {
            FileItem item = (FileItem)var4.next();
            if (!item.isFormField() && !item.isInMemory()) {
                Object object = request.getAttribute(item.getFieldName());
                if (object instanceof InputStream) {
                    IOUtils.closeQuietly((InputStream)object);
                }

                item.delete();
            }
        }

        String uploadId = (String)request.getAttribute("sys.uploadId");
        if (uploadId != null) {
            HttpSession session = request.getSession(true);
            session.removeAttribute("sys.upread." + uploadId);
            session.removeAttribute("sys.uplen." + uploadId);
        }

    }

    public static boolean jsonResponse(HttpServletRequest request) {
        return exists(request, "_jsonresp");
    }

    public static boolean exists(HttpServletRequest request, String name) {
        if (request == null) {
            return false;
        } else {
            Object value;
            if (name.startsWith("sys.")) {
                value = request.getAttribute(name);
            } else {
                value = fetch(request, name);
            }

            return value == null ? false : "1".equals(value.toString());
        }
    }

    public static Object getObject(HttpServletRequest request, String name) {
        Object object = request.getAttribute("sysx.varMap");
        if (object != null) {
            ConcurrentHashMap<String, Object> map = JSONObject.toConHashMap(object);
            return map.get(name);
        } else {
            return null;
        }
    }

    public static void applyAttributes(HttpServletRequest request, JSONObject params) {
        if (params != null) {
            Set<Entry<String, Object>> es = params.entrySet();
            Iterator var4 = es.iterator();

            while (var4.hasNext()) {
                Entry<String, Object> e = (Entry)var4.next();
                request.setAttribute((String)e.getKey(), e.getValue());
            }

        }
    }

    public static void include(HttpServletRequest request, HttpServletResponse response, String path) throws Exception {
        doInclude(request, response, path, false);
    }

    public static void forward(HttpServletRequest request, HttpServletResponse response, String path) throws Exception {
        doInclude(request, response, path, true);
    }

    private static void doInclude(HttpServletRequest request, HttpServletResponse response, String path,
        boolean isForward) throws Exception {
        String xwl = FileUtil.getModulePath(path, true);
        if (xwl == null) {
            if (isForward) {
                request.getRequestDispatcher(path).forward(request, response);
            } else {
                request.getRequestDispatcher(path).include(request, response);
            }
        } else {
            if (isForward) {
                response.resetBuffer();
            }

            if (xwl.isEmpty()) {
                xwl = request.getParameter("xwl");
                if (xwl == null) {
                    response.sendError(400, "null xwl");
                    return;
                }

                xwl = StringUtil.concat(new String[] {xwl, ".xwl"});
            }

            Parser parser = new Parser(request, response);
            parser.parse(xwl);
        }

    }

    public static void sendMessage(String fromUser, JSONArray toUsers, String msg) throws Exception {
        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        Timestamp now = DateUtil.now();

        try {
            conn = DbUtil.getConnection();
            st = conn.prepareStatement("select USER_NAME,DISPLAY_NAME from WB_USER where USER_ID=?");
            st.setString(1, fromUser);
            rs = st.executeQuery();
            rs.next();
            String userName = rs.getString(1);
            String dispName = rs.getString(2);
            rs.close();
            st.close();
            JSONObject jo = new JSONObject();
            jo.put("module", "m?xwl=my/im");
            jo.put("count", 1);
            jo.put("title", dispName);
            jo.put("msg", msg);
            String homeText = jo.toString();
            jo = new JSONObject();
            jo.put("FROM_SYS", true);
            jo.put("FROM_USER", fromUser);
            jo.put("USER_NAME", userName);
            jo.put("DISPLAY_NAME", dispName);
            jo.put("USER_ID", fromUser);
            jo.put("NOT_READ", 1);
            jo.put("SEND_DATE", now);
            jo.put("TEXT_CONTENT", msg);
            jo.put("IM_ID", SysUtil.getId());
            jo.put("SEND_DATE", now);
            jo.put("msgId", SysUtil.getId());
            st = conn.prepareStatement("insert into WB_IM values (?,?,?,?,1,null,null,?,null)");
            int j = toUsers.length();

            for (int i = 0; i < j; ++i) {
                String userId = toUsers.getString(i);
                st.setString(1, SysUtil.getId());
                if (i == 0) {
                    st.setTimestamp(2, now);
                    st.setString(3, fromUser);
                }

                st.setString(4, userId);
                if (i == 0) {
                    DbUtil.setText(st, 5, msg);
                }

                st.executeUpdate();
                jo.put("TO_USER", userId);
                send((String)userId, "sys.im", jo.toString());
                send((String)userId, "sys.home", homeText);
            }
        } finally {
            DbUtil.close(rs);
            DbUtil.close(st);
            DbUtil.close(conn);
        }

    }

    public static void setObject(HttpServletRequest request, String name, Object value) {
        Object object = request.getAttribute("sysx.varMap");
        ConcurrentHashMap<String, Object> map = JSONObject.toConHashMap(object);
        if (map.containsKey(name)) {
            throw new IllegalArgumentException("Key \"" + name + "\" already exists.");
        } else {
            map.put(name, value);
        }
    }
}