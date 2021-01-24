package com.wb.common;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONObject;

import com.wb.tool.Encrypter;
import com.wb.util.DbUtil;
import com.wb.util.StringUtil;
import com.wb.util.WebUtil;

public class Session {
    public static void verify(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String referer = StringUtil.opt(request.getHeader("Referer"));
        String redirect = WebUtil.fetch(request, "redirect");
        HttpSession session = request.getSession(false);
        if (Var.getBool("sys.session.verifyImage.enabled")) {
            if (session == null) {
                throw new Exception(Str.format(request, "vcExpired", new Object[0]));
            }

            String verifyCode = (String)session.getAttribute("sys.verifyCode");
            session.removeAttribute("sys.verifyCode");
            if (StringUtil.isEmpty(verifyCode)
                || !StringUtil.isSame(verifyCode, WebUtil.fetch(request, "verifyCode"))) {
                throw new Exception(Str.format(request, "vcInvalid", new Object[0]));
            }
        }

        if (session != null) {
            session.invalidate();
        }

        createSession(request, WebUtil.fetch(request, "username"), WebUtil.fetch(request, "password"), true);
        if (referer.endsWith("/login") || referer.endsWith("m?xwl=sys/session/login")) {
            referer = Var.getString("sys.home");
        }

        if (referer.endsWith("/tlogin") || referer.endsWith("m?xwl=sys/session/tlogin")) {
            referer = Var.getString("sys.homeMobile");
        }

        if (StringUtil.isEmpty(redirect)) {
            JSONObject jo = new JSONObject();
            jo.put("referer", referer);
            jo.put("dispname", request.getSession(true).getAttribute("sys.dispname"));
            WebUtil.send(response, jo.toString());
        } else {
            response.sendRedirect(redirect);
        }

    }

    public static void logout(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    public static String[] getRoles(HttpServletRequest request) throws Exception {
        HttpSession session = request.getSession(false);
        return session == null ? null : (String[])session.getAttribute("sys.roles");
        /**
         * Authentication authentication = SecurityContextHolder.getContext().getAuthentication(); if (null ==
         * authentication) { return null; } List<? extends GrantedAuthority> grantedAuthoritys = (List<? extends
         * GrantedAuthority>)authentication.getAuthorities(); int len = grantedAuthoritys.size(); String[] roles = new
         * String[len]; for (int i = 0; i < len; i++) { roles[i] = grantedAuthoritys.get(i).getAuthority(); } return
         * roles;
         **/
    }

    public static String getUserId(HttpServletRequest request) throws Exception {
        HttpSession session = request.getSession(false);
        return session == null ? null : (String)session.getAttribute("sys.user");
        /**
         * Authentication authentication = SecurityContextHolder.getContext().getAuthentication(); if (null ==
         * authentication) { return null; } return (String)authentication.getPrincipal();
         **/
    }

    private static void storeUserValues(HttpServletRequest request, HttpSession session, String userId)
        throws Exception {
        String[] names = Var.sessionVars.split(",");
        String[] valueIds = new String[names.length];
        ArrayList<String> roles = new ArrayList();
        int i = 0;
        ResultSet rs = (ResultSet)DbUtil.run(request,
            "select a.ROLE_ID from WB_USER_ROLE a, WB_ROLE b where a.ROLE_ID=b.ROLE_ID and a.USER_ID={?sys.user?} and a.ROLE_ID<>'default' and b.STATUS<>0");
        roles.add("default");

        while (rs.next()) {
            roles.add(rs.getString(1));
        }

        String[] roleArray = (String[])roles.toArray(new String[roles.size()]);
        session.setAttribute("sys.roles", roleArray);
        session.setAttribute("sys.roleList", StringUtil.join(roleArray, ","));
        rs = (ResultSet)DbUtil.run(request,
            "select DEPT_ID from WB_PERSON where USER_ID={?sys.user?} and DEPT_ID is not null");
        session.setAttribute("sys.dept", rs.next() ? rs.getString(1) : "");
        String[] var14 = names;
        int var13 = names.length;

        for (int var12 = 0; var12 < var13; ++var12) {
            String name = var14[var12];
            valueIds[i++] = StringUtil.concat(new String[] {"'", userId, "@", name, "'"});
        }

        rs = (ResultSet)DbUtil.run(request,
            "select VAL_ID,VAL_CONTENT from WB_VALUE where VAL_ID in (" + StringUtil.join(valueIds, ",") + ")");

        while (rs.next()) {
            String fieldName = rs.getString("VAL_ID");
            fieldName = fieldName.substring(fieldName.indexOf(64) + 1);
            String fieldValue = rs.getString("VAL_CONTENT");
            session.setAttribute("sys." + fieldName, fieldValue);
        }
    }

    public static void createSession(HttpServletRequest request, String userId, String username, String dispname,
        String useLang, String[] roleids, String[] deptids) {
        request.setAttribute("username", username);
        HttpSession session = request.getSession();
        session.setAttribute("sys.user", userId);
        session.setAttribute("sys.username", username);
        session.setAttribute("sys.dispname", dispname);
        session.setAttribute("sysx.userAgent", StringUtil.substring(request.getHeader("user-agent"), 0, 500));
        session.setAttribute("sys.ip", request.getRemoteAddr());
        session.setAttribute("sys.lang", useLang);

        ArrayList<String> roles = new ArrayList<>();
        roles.add("default");
        if (null != roleids && roleids.length > 0) {
            roles.addAll(Arrays.asList(roleids));
        }
        String[] roleArray = (String[])roles.toArray(new String[roles.size()]);
        session.setAttribute("sys.roles", roleArray);
        session.setAttribute("sys.roleList", StringUtil.join(roleArray, ","));
        if (null != deptids && deptids.length > 0) {
            session.setAttribute("sys.dept", deptids[0]);
        }
        SimpleListener simpleListener = new SimpleListener();
        session.setAttribute("sysx.simpleListener", simpleListener);
    }

    public static void createSession(HttpServletRequest request, String username, String password,
        boolean verifyPassword) throws Exception {
        int timeout = Var.sessionTimeout;
        request.setAttribute("username", username);
        ResultSet rs = (ResultSet)DbUtil.run(request,
            "select USER_ID,USER_NAME,DISPLAY_NAME,PASSWORD,USE_LANG from WB_USER where USER_NAME={?username?} and STATUS=1");
        if (!rs.next()) {
            throw new IllegalArgumentException(Str.format(request, "userNotExist", new Object[] {username}));
        } else {
            String userId = rs.getString("USER_ID");
            username = rs.getString("USER_NAME");
            String dispname = rs.getString("DISPLAY_NAME");
            String useLang = rs.getString("USE_LANG");
            if (verifyPassword && !rs.getString("PASSWORD").equals(Encrypter.getMD5(password))) {
                throw new IllegalArgumentException(Str.format(request, "passwordInvalid", new Object[0]));
            } else {
                HttpSession session = request.getSession(true);
                session.setAttribute("sys.logined", true);
                if (timeout == -2) {
                    timeout = Integer.MAX_VALUE;
                }

                if (timeout > -2) {
                    session.setMaxInactiveInterval(timeout);
                }

                session.setAttribute("sys.user", userId);
                session.setAttribute("sys.username", username);
                session.setAttribute("sys.dispname", dispname);
                session.setAttribute("sysx.userAgent", StringUtil.substring(request.getHeader("user-agent"), 0, 500));
                session.setAttribute("sys.ip", request.getRemoteAddr());
                session.setAttribute("sys.lang", useLang);
                DbUtil.run(request, "update WB_USER set LOGIN_TIMES=LOGIN_TIMES+1 where USER_ID={?sys.user?}");
                storeUserValues(request, session, userId);
                SimpleListener simpleListener = new SimpleListener();
                if (Var.uniqueLogin) {
                    String[] userIds = new String[] {userId};
                    UserList.invalidate(userIds);
                }

                session.setAttribute("sysx.simpleListener", simpleListener);
            }
        }
    }
}