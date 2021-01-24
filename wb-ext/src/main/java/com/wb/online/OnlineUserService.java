package com.wb.online;

import javax.servlet.http.HttpSession;

import com.github.xiongxcodes.tool.business.BusinessResponseGridData;

public interface OnlineUserService {
    public final static String USER = "sys.user";
    public final static String USERNAME = "sys.username";
    public final static String DISPNAME = "sys.dispname";
    public final static String IP = "sys.ip";
    public final static String USERAGENT = "sysx.userAgent";
    public final static String SIMPLELISTENER = "sysx.simpleListener";

    public void doAdd(String userId, HttpSession session);

    public void doRemove(String userId, HttpSession session);

    public void doInvalidate(String[] userIds);

    public void doUpdate(String userId, String[] roles, boolean status);

    public BusinessResponseGridData<OnlineUserVO> doGetUserList(int start, int limit);

    public BusinessResponseGridData<OnlineSessionVO> doGetSessionList(String user, int start, int limit);
}