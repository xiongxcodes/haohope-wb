package com.wb.interact;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.alibaba.fastjson.JSON;
import com.wb.cache.MemoryCache;
import com.wb.cache.ModuleCache;
import com.wb.cache.WbCache;
import com.wb.common.Base;
import com.wb.lock.DefaultModuleLock;
import com.wb.lock.ModuleLock;
import com.wb.util.DbUtil;

public class Module implements ApplicationContextAware {
    private static ModuleLock lock = new DefaultModuleLock();
    public static WbCache<String, List<String>> buffer = new MemoryCache<String, List<String>>();

    public static void updateRole(List<String> modules, List<String> role, boolean checked) {
        if (null == role || role.size() == 0 || null == modules || modules.size() == 0) {
            return;
        }
        try {
            lock.lock();
            Connection conn = null;
            Statement st = null;
            ResultSet rs = null;
            try {
                conn = DbUtil.getConnection();
                conn.setAutoCommit(false);
                st = conn.createStatement();
                List<String> roles = null;
                Set<String> setRoles = new HashSet<String>();
                boolean addflag = false;
                for (String module : modules) {
                    roles = Module.buffer.get(module);
                    if (null == roles || roles.size() == 0) {
                        roles = new ArrayList<String>();
                        addflag = true;
                    }
                    if (checked) {
                        roles.addAll(role);
                    } else {
                        roles.removeAll(role);
                    }
                    setRoles.addAll(roles);
                    roles.clear();
                    roles.addAll(setRoles);
                    if (null == roles || roles.size() == 0) {
                        st.addBatch("DELETE FROM wb_module_role WHERE MODULEPATH = '" + module + "' ");
                        Module.buffer.remove(module);
                    } else {
                        if (addflag) {
                            st.addBatch("INSERT INTO wb_module_role(MODULEPATH,ROLES) values('" + module + "','"
                                + JSON.toJSONString(roles) + "') ");
                        } else {
                            st.addBatch("UPDATE wb_module_role set ROLES = '" + JSON.toJSONString(roles)
                                + "' WHERE MODULEPATH = '" + module + "' ");
                        }
                        Module.buffer.put(module, roles);
                    }
                }
                st.executeBatch();

                conn.commit();
            } finally {
                DbUtil.close(rs);
                DbUtil.close(st);
                DbUtil.close(conn);
            }
        } catch (Throwable var10) {
            throw new RuntimeException(var10);
        } finally {
            lock.unlock();
        }
    }

    public static void load() {
        try {
            lock.lock();
            buffer.clear();
            Connection conn = null;
            Statement st = null;
            ResultSet rs = null;
            try {
                conn = DbUtil.getConnection();
                st = conn.createStatement();
                rs = st.executeQuery("select * from wb_module_role ");
                String modulepath = "";
                List<String> roles = null;
                File file = null;
                boolean delflag = false;
                while (rs.next()) {
                    modulepath = rs.getString("MODULEPATH");
                    file = new File(Base.modulePath, modulepath);
                    if (file.exists()) {
                        roles = JSON.parseArray(rs.getString("ROLES"), String.class);
                        buffer.put(modulepath, roles);
                    } else {
                        delflag = true;
                        st.addBatch(" delete from wb_module_role WHERE MODULEPATH = '" + modulepath + "' ");
                    }
                }
                if (delflag) {
                    st.executeBatch();
                }
            } finally {
                DbUtil.close(rs);
                DbUtil.close(st);
                DbUtil.close(conn);
            }

        } catch (Throwable var10) {
            throw new RuntimeException(var10);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, ModuleCache> buffermap = applicationContext.getBeansOfType(ModuleCache.class);
        if (null != buffermap) {
            for (Entry<String, ModuleCache> entry : buffermap.entrySet()) {
                buffer = entry.getValue();
                break;
            }
        }
        Map<String, ModuleLock> lockmap = applicationContext.getBeansOfType(ModuleLock.class);
        if (null != lockmap) {
            for (Entry<String, ModuleLock> entry : lockmap.entrySet()) {
                lock = entry.getValue();
                break;
            }
        }
    }

}
