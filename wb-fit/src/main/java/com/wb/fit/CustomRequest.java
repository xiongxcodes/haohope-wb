/** <a href="http://www.cpupk.com/decompiler">Eclipse Class Decompiler</a> plugin, Copyright (c) 2017 Chen Chao. **/
package com.wb.fit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import org.json.JSONObject;

public class CustomRequest implements HttpServletRequest {
    HashMap<String, Object> attributeMap = new HashMap();
    HashMap<String, String[]> paramMap = new HashMap();

    public void setParams(JSONObject params) {
        if (params != null) {
            Set es = params.entrySet();
            Iterator arg3 = es.iterator();

            while (arg3.hasNext()) {
                Entry e = (Entry)arg3.next();
                this.attributeMap.put((String)e.getKey(), e.getValue());
            }

        }
    }

    public AsyncContext getAsyncContext() {
        return null;
    }

    public Object getAttribute(String name) {
        return this.attributeMap.get(name);
    }

    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(this.attributeMap.keySet());
    }

    public String getCharacterEncoding() {
        return "utf-8";
    }

    public int getContentLength() {
        return 0;
    }

    public long getContentLengthLong() {
        return 0L;
    }

    public String getContentType() {
        return null;
    }

    public DispatcherType getDispatcherType() {
        return null;
    }

    public ServletInputStream getInputStream() throws IOException {
        return null;
    }

    public String getLocalAddr() {
        return null;
    }

    public String getLocalName() {
        return null;
    }

    public int getLocalPort() {
        return 0;
    }

    public Locale getLocale() {
        return null;
    }

    public Enumeration<Locale> getLocales() {
        return null;
    }

    public String getParameter(String name) {
        Object value = this.attributeMap.get(name);
        return value == null ? null : value.toString();
    }

    public Map<String, String[]> getParameterMap() {
        return this.paramMap;
    }

    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(this.paramMap.keySet());
    }

    public String[] getParameterValues(String arg0) {
        return null;
    }

    public String getProtocol() {
        return null;
    }

    public BufferedReader getReader() throws IOException {
        return null;
    }

    public String getRealPath(String arg0) {
        return null;
    }

    public String getRemoteAddr() {
        return "127.0.0.1";
    }

    public String getRemoteHost() {
        return "localhost";
    }

    public int getRemotePort() {
        return 0;
    }

    public RequestDispatcher getRequestDispatcher(String arg0) {
        return null;
    }

    public String getScheme() {
        return null;
    }

    public String getServerName() {
        return null;
    }

    public int getServerPort() {
        return 0;
    }

    public ServletContext getServletContext() {
        return null;
    }

    public boolean isAsyncStarted() {
        return false;
    }

    public boolean isAsyncSupported() {
        return false;
    }

    public boolean isSecure() {
        return false;
    }

    public void removeAttribute(String name) {
        this.attributeMap.remove(name);
    }

    public void setAttribute(String name, Object value) {
        this.attributeMap.put(name, value);
    }

    public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException {}

    public AsyncContext startAsync() throws IllegalStateException {
        return null;
    }

    public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1) throws IllegalStateException {
        return null;
    }

    public boolean authenticate(HttpServletResponse arg0) throws IOException, ServletException {
        return false;
    }

    public String changeSessionId() {
        return null;
    }

    public String getAuthType() {
        return null;
    }

    public String getContextPath() {
        return null;
    }

    public Cookie[] getCookies() {
        return null;
    }

    public long getDateHeader(String arg0) {
        return 0L;
    }

    public String getHeader(String arg0) {
        return null;
    }

    public Enumeration<String> getHeaderNames() {
        return null;
    }

    public Enumeration<String> getHeaders(String arg0) {
        return null;
    }

    public int getIntHeader(String arg0) {
        return 0;
    }

    public String getMethod() {
        return null;
    }

    public Part getPart(String arg0) throws IOException, ServletException {
        return null;
    }

    public Collection<Part> getParts() throws IOException, ServletException {
        return null;
    }

    public String getPathInfo() {
        return null;
    }

    public String getPathTranslated() {
        return null;
    }

    public String getQueryString() {
        return null;
    }

    public String getRemoteUser() {
        return null;
    }

    public String getRequestURI() {
        return null;
    }

    public StringBuffer getRequestURL() {
        return null;
    }

    public String getRequestedSessionId() {
        return null;
    }

    public String getServletPath() {
        return "";
    }

    public HttpSession getSession() {
        return null;
    }

    public HttpSession getSession(boolean arg0) {
        return null;
    }

    public Principal getUserPrincipal() {
        return null;
    }

    public boolean isRequestedSessionIdFromCookie() {
        return false;
    }

    public boolean isRequestedSessionIdFromURL() {
        return false;
    }

    public boolean isRequestedSessionIdFromUrl() {
        return false;
    }

    public boolean isRequestedSessionIdValid() {
        return false;
    }

    public boolean isUserInRole(String arg0) {
        return false;
    }

    public void login(String arg0, String arg1) throws ServletException {}

    public void logout() throws ServletException {}
}