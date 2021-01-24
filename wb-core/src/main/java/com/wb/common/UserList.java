package com.wb.common;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.alibaba.fastjson.JSON;
import com.github.xiongxcodes.tool.business.BusinessResponseGridData;
import com.wb.online.OnlineSessionVO;
import com.wb.online.OnlineUserService;
import com.wb.online.OnlineUserVO;
import com.wb.util.StringUtil;
import com.wb.util.WebUtil;

public class UserList implements ApplicationContextAware {
    private static HashMap<String, HashSet<HttpSession>> users = new HashMap();
    private static OnlineUserService onlineService = new DefaultOnlineUserServiceImpl();

    public static void add(String userId, HttpSession session) {
        if (Var.recordSession) {
            doAdd(userId, session);
        }

    }

    private static synchronized void doAdd(String userId, HttpSession session) {
        onlineService.doAdd(userId, session);
        /**
         * HashSet<HttpSession> sessions = (HashSet) users.get(userId); if (sessions == null) { sessions = new
         * HashSet(); users.put(userId, sessions); }
         * 
         * sessions.add(session);
         **/
    }

    public static void remove(String userId, HttpSession session) {
        if (Var.recordSession) {
            doRemove(userId, session);
        }

    }

    private static synchronized void doRemove(String userId, HttpSession session) {
        onlineService.doRemove(userId, session);
        /**
         * HashSet<HttpSession> sessions = (HashSet) users.get(userId); if (sessions != null && session != null) {
         * sessions.remove(session); if (sessions.isEmpty()) { users.remove(userId); } }
         **/

    }

    public static void invalidate(String[] userIds) {
        if (Var.recordSession) {
            doInvalidate(userIds);
        }

    }

    private static synchronized void doInvalidate(String[] userIds) {
        onlineService.doInvalidate(userIds);
        /**
         * String[] var6 = userIds; int var5 = userIds.length;
         * 
         * for (int var4 = 0; var4 < var5; ++var4) { String userId = var6[var4]; HashSet<HttpSession> sessions =
         * (HashSet) users.get(userId); if (sessions != null) { HttpSession[] sessionArray = (HttpSession[])
         * sessions.toArray(new HttpSession[sessions.size()]); HttpSession[] var10 = sessionArray; int var9 =
         * sessionArray.length;
         * 
         * for (int var8 = 0; var8 < var9; ++var8) { HttpSession session = var10[var8];
         * 
         * try { session.invalidate(); } catch (Throwable var12) { ; } }
         * 
         * users.remove(userId); } }
         **/

    }

    public static void update(String userId, String[] roles, boolean status) {
        if (Var.recordSession) {
            doUpdate(userId, roles, status);
        }

    }

    private static synchronized void doUpdate(String userId, String[] roles, boolean status) {
        onlineService.doUpdate(userId, roles, status);
        /**
         * HashSet<HttpSession> sessions = (HashSet) users.get(userId); if (sessions != null) { if (status) { Iterator
         * var5 = sessions.iterator();
         * 
         * while (var5.hasNext()) { HttpSession session = (HttpSession) var5.next();
         * 
         * try { session.setAttribute("sys.roles", roles); session.setAttribute("sys.roleList", StringUtil.join(roles,
         * ",")); } catch (Throwable var7) { ; } } } else { String[] userIds = new String[]{userId};
         * invalidate(userIds); } }
         **/
    }

    @SuppressWarnings("unchecked")
    public static synchronized HttpSession[] getSessions(String userId) {
        HashSet<HttpSession> sessions = (HashSet)users.get(userId);
        return sessions == null ? null : (HttpSession[])sessions.toArray(new HttpSession[sessions.size()]);
    }

    public static synchronized String[] getUsers() {
        return (String[])users.keySet().toArray(new String[users.size()]);
    }

    public static void getSessionList(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (Var.recordSession) {
            doGetSessionList(request, response);
        }

    }

    private static synchronized void doGetSessionList(HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        String user = request.getParameter("user");
        int start = Integer.parseInt(request.getParameter("start"));
        int limit = Integer.parseInt(request.getParameter("limit"));
        /**
         * int index = -1; JSONArray rows = new JSONArray(); HashSet<HttpSession> sessions = (HashSet) users.get(user);
         * if (limit > Var.limitRecords) { limit = Var.limitRecords; }
         * 
         * int end = start + limit; if (sessions != null) { Iterator var12 = sessions.iterator();
         * 
         * while (var12.hasNext()) { HttpSession session = (HttpSession) var12.next(); ++index; if (index >= start) { if
         * (index >= end) { break; }
         * 
         * JSONObject row = new JSONObject();
         * 
         * try { row.put("ip", session.getAttribute("sys.ip")); row.put("userAgent",
         * session.getAttribute("sysx.userAgent")); row.put("createDate", new Date(session.getCreationTime()));
         * row.put("lastAccessDate", new Date(session.getLastAccessedTime())); rows.put(row); } catch (Throwable var14)
         * { --index; } } } }
         * 
         * JSONObject result = new JSONObject(); result.put("rows", rows); result.put("total", sessions == null ? 0 :
         * sessions.size()); WebUtil.send(response, result);
         **/
        BusinessResponseGridData<OnlineSessionVO> griddata = onlineService.doGetSessionList(user, start, limit);
        JSONObject result = new JSONObject();
        JSONArray rows = new JSONArray();
        for (OnlineSessionVO vo : griddata.getRows()) {
            JSONObject row = new JSONObject();
            row.put("ip", vo.getIp());
            row.put("userAgent", vo.getUserAgent());
            row.put("createDate", vo.getCreateDate());
            row.put("lastAccessDate", vo.getLastAccessDate());
            rows.put(row);
        }
        result.put("rows", rows);
        result.put("total", griddata.getTotal());
        WebUtil.send(response, result);

    }

    public static void getUserList(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (Var.recordSession) {
            doGetUserList(request, response);
        }

    }

    private static synchronized void doGetUserList(HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        int start = Integer.parseInt(request.getParameter("start"));
        int limit = Integer.parseInt(request.getParameter("limit"));
        WebUtil.send(response, JSON.toJSONString(onlineService.doGetUserList(start, limit)));
    }

    static class DefaultOnlineUserServiceImpl implements OnlineUserService {

        /**
         * <p>
         * Title: doAdd
         * </p>
         * <p>
         * Description:
         * </p>
         * 
         * @param userId
         * @param session
         * @see com.wb.online.OnlineUserService#doAdd(java.lang.String, javax.servlet.http.HttpSession)
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public void doAdd(String userId, HttpSession session) {
            HashSet<HttpSession> sessions = (HashSet)users.get(userId);
            if (sessions == null) {
                sessions = new HashSet();
                users.put(userId, sessions);
            } else {
                /*
                boolean uniqueLogin = Var.getBool("sys.session.uniqueLogin");
                if(uniqueLogin && sessions.size() > 0) {
                	for(HttpSession _session:sessions) {
                		_session.invalidate();
                	}
                	sessions.clear();
                }*/
            }
            sessions.add(session);
        }

        /**
         * <p>
         * Title: doRemove
         * </p>
         * <p>
         * Description:
         * </p>
         * 
         * @param userId
         * @param session
         * @see com.wb.online.OnlineUserService#doRemove(java.lang.String, javax.servlet.http.HttpSession)
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public void doRemove(String userId, HttpSession session) {
            HashSet<HttpSession> sessions = (HashSet)users.get(userId);
            if (sessions != null && session != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    users.remove(userId);
                }
            }
        }

        /**
         * <p>
         * Title: doInvalidate
         * </p>
         * <p>
         * Description:
         * </p>
         * 
         * @param userIds
         * @see com.wb.online.OnlineUserService#doInvalidate(java.lang.String[])
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public void doInvalidate(String[] userIds) {
            String[] var6 = userIds;
            int var5 = userIds.length;

            for (int var4 = 0; var4 < var5; ++var4) {
                String userId = var6[var4];
                HashSet<HttpSession> sessions = (HashSet)users.get(userId);
                if (sessions != null) {
                    HttpSession[] sessionArray = (HttpSession[])sessions.toArray(new HttpSession[sessions.size()]);
                    HttpSession[] var10 = sessionArray;
                    int var9 = sessionArray.length;

                    for (int var8 = 0; var8 < var9; ++var8) {
                        HttpSession session = var10[var8];

                        try {
                            session.invalidate();
                        } catch (Throwable var12) {
                            ;
                        }
                    }

                    users.remove(userId);
                }
            }
        }

        /**
         * <p>
         * Title: doUpdate
         * </p>
         * <p>
         * Description:
         * </p>
         * 
         * @param userId
         * @param roles
         * @param status
         * @see com.wb.online.OnlineUserService#doUpdate(java.lang.String, java.lang.String[], boolean)
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public void doUpdate(String userId, String[] roles, boolean status) {
            HashSet<HttpSession> sessions = (HashSet)users.get(userId);
            if (sessions != null) {
                if (status) {
                    Iterator var5 = sessions.iterator();

                    while (var5.hasNext()) {
                        HttpSession session = (HttpSession)var5.next();

                        try {
                            session.setAttribute("sys.roles", roles);
                            session.setAttribute("sys.roleList", StringUtil.join(roles, ","));
                        } catch (Throwable var7) {
                            ;
                        }
                    }
                } else {
                    String[] userIds = new String[] {userId};
                    invalidate(userIds);
                }
            }
        }

        /**
         * <p>
         * Title: doGetUserList
         * </p>
         * <p>
         * Description:
         * </p>
         * 
         * @param start
         * @param limit
         * @return
         * @see com.wb.online.OnlineUserService#doGetUserList(int, int)
         */
        @Override
        public BusinessResponseGridData<OnlineUserVO> doGetUserList(int start, int limit) {
            int index = -1;
            List<OnlineUserVO> rows = new ArrayList<OnlineUserVO>();
            Set<Entry<String, HashSet<HttpSession>>> es = users.entrySet();
            if (limit > Var.limitRecords) {
                limit = Var.limitRecords;
            }

            int end = start + limit;
            Iterator var14 = es.iterator();

            while (var14.hasNext()) {
                Entry<String, HashSet<HttpSession>> e = (Entry)var14.next();
                ++index;
                if (index >= start) {
                    if (index >= end) {
                        break;
                    }

                    HashSet<HttpSession> sessions = (HashSet)e.getValue();
                    int sessionCount = 0;
                    HttpSession session = null;

                    for (Iterator var16 = sessions.iterator(); var16.hasNext(); ++sessionCount) {
                        HttpSession sess = (HttpSession)var16.next();
                        if (session == null) {
                            session = sess;
                        }
                    }

                    if (sessionCount != 0) {
                        OnlineUserVO row = new OnlineUserVO();
                        row.setSessionCount(sessionCount);
                        row.setUser(e.getKey());

                        try {
                            row.setUsername((String)session.getAttribute(OnlineUserService.USERNAME));
                            row.setDispname((String)session.getAttribute(OnlineUserService.DISPNAME));
                            row.setIp((String)session.getAttribute(OnlineUserService.IP));
                            row.setUserAgent((String)session.getAttribute(OnlineUserService.USERAGENT));
                            rows.add(row);
                        } catch (Throwable var17) {
                            --index;
                        }
                    }
                }
            }
            return BusinessResponseGridData.instance(users.size(), rows);
        }

        /**
         * <p>
         * Title: doGetSessionList
         * </p>
         * <p>
         * Description:
         * </p>
         * 
         * @param user
         * @param start
         * @param limit
         * @return
         * @see com.wb.online.OnlineUserService#doGetSessionList(java.lang.String, int, int)
         */
        @Override
        public BusinessResponseGridData<OnlineSessionVO> doGetSessionList(String user, int start, int limit) {
            int index = -1;
            List<OnlineSessionVO> rows = new ArrayList<OnlineSessionVO>();
            HashSet<HttpSession> sessions = (HashSet)users.get(user);
            if (limit > Var.limitRecords) {
                limit = Var.limitRecords;
            }

            int end = start + limit;
            if (sessions != null) {
                Iterator var12 = sessions.iterator();

                while (var12.hasNext()) {
                    HttpSession session = (HttpSession)var12.next();
                    ++index;
                    if (index >= start) {
                        if (index >= end) {
                            break;
                        }

                        OnlineSessionVO row = new OnlineSessionVO();

                        try {
                            row.setIp((String)session.getAttribute(OnlineUserService.IP));
                            row.setUserAgent((String)session.getAttribute(OnlineUserService.USERAGENT));
                            row.setCreateDate(new Date(session.getCreationTime()));
                            row.setLastAccessDate(new Date(session.getLastAccessedTime()));
                            rows.add(row);
                        } catch (Throwable var14) {
                            --index;
                        }
                    }
                }
            }
            return BusinessResponseGridData.instance(sessions == null ? 0l : sessions.size(), rows);
            // return new OnlineSessionGridResult(sessions == null ? 0l : sessions.size(),rows);
        }

    }

    /**
     * <p>
     * Title: setApplicationContext
     * </p>
     * <p>
     * Description:
     * </p>
     * 
     * @param applicationContext
     * @throws BeansException
     * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, OnlineUserService> buffermap = applicationContext.getBeansOfType(OnlineUserService.class);
        if (null != buffermap) {
            for (Entry<String, OnlineUserService> entry : buffermap.entrySet()) {
                onlineService = entry.getValue();
                break;
            }
        }

    }
}