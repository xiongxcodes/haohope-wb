package com.wb.tool;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import java.util.Properties;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.triggers.CronTriggerImpl;

import com.wb.common.ScriptBuffer;
import com.wb.common.Var;
import com.wb.task.ScriptProxy;
import com.wb.util.DbUtil;
import com.wb.util.StringUtil;

public class TaskManager {
    public static Scheduler scheduler;

    public static void loadTask(String taskId, String taskName, int intervalType, String intervalExpress,
        String className, String serverScript, Date beginDate, Date endDate) throws Exception {
        CronTriggerImpl trigger = new CronTriggerImpl();
        deleteTask(taskId);
        JobDetailImpl job;
        if (StringUtil.isEmpty(className)) {
            job = new JobDetailImpl(taskId, "DEFAULT", ScriptProxy.class);
            JobDataMap dataMap = job.getJobDataMap();
            dataMap.put("job.id", "job." + taskId);
            dataMap.put("job.serverScript", serverScript);
        } else {
            Class<Job> jobClass = (Class<Job>)Class.forName(className);
            job = new JobDetailImpl(taskId, "DEFAULT", jobClass);
        }

        job.setDescription(taskName);
        String[] express = StringUtil.split(intervalExpress, ":");
        switch (intervalType) {
            case 0:
                trigger.setCronExpression(String.format("*/%d * * * * ?", Integer.parseInt(express[0])));
                // trigger = TriggerUtils.makeSecondlyTrigger(Integer.parseInt(express[0]));
                break;
            case 1:
                trigger.setCronExpression(String.format("0 */%d * * * ?", Integer.parseInt(express[0])));
                // trigger = TriggerUtils.makeMinutelyTrigger(Integer.parseInt(express[0]));
                break;
            case 2:
                // 0 */2 * * *
                trigger.setCronExpression(String.format("0 0 */%d * * ?", Integer.parseInt(express[0])));
                // trigger = TriggerUtils.makeHourlyTrigger(Integer.parseInt(express[0]));
                break;
            case 3:
                trigger.setCronExpression(
                    String.format("0 %d %d * * ?", Integer.parseInt(express[1]), Integer.parseInt(express[0])));
                // trigger = TriggerUtils.makeDailyTrigger(Integer.parseInt(express[0]), Integer.parseInt(express[1]));
                break;
            case 4:
                // 0 1 2 3 4 5 6
                trigger.setCronExpression(String.format("0 %d %d ? * %d", Integer.parseInt(express[2]),
                    Integer.parseInt(express[1]), Integer.parseInt(express[0])));
                // TriggerUtils.makeWeeklyTrigger(Integer.parseInt(express[0]), Integer.parseInt(express[1]),
                // Integer.parseInt(express[2]));
                break;
            case 5:
                trigger.setCronExpression(String.format("0 %d %d %d * ?", Integer.parseInt(express[2]),
                    Integer.parseInt(express[1]), Integer.parseInt(express[0])));
                // trigger = TriggerUtils.makeMonthlyTrigger(Integer.parseInt(express[0]), Integer.parseInt(express[1]),
                // Integer.parseInt(express[2]));
        }
        trigger.setName(taskId);
        if (beginDate != null) {
            trigger.setStartTime(beginDate);
        }

        if (endDate != null) {
            trigger.setEndTime(endDate);
        }

        if (scheduler != null) {
            scheduler.scheduleJob(job, trigger);
        }

    }

    public static void deleteTask(String taskId) throws Exception {

        if (scheduler != null && scheduler.getJobDetail(JobKey.jobKey(taskId, "DEFAULT")) != null) {
            scheduler.deleteJob(JobKey.jobKey(taskId, "DEFAULT"));
        }

        ScriptBuffer.remove("job." + taskId);
    }

    public static synchronized void start() throws Exception {
        if (Var.getBool("sys.task.enabled")) {
            if (scheduler == null) {
                StdSchedulerFactory factory = new StdSchedulerFactory();
                Properties props = new Properties();
                props.put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
                props.put("org.quartz.threadPool.threadCount", Var.getString("sys.task.threadCount"));
                factory.initialize(props);
                scheduler = factory.getScheduler();
                scheduler.start();
            } else if (scheduler.isStarted()) {
                return;
            }

            Connection conn = null;
            Statement st = null;
            ResultSet rs = null;

            try {
                conn = DbUtil.getConnection();
                st = conn.createStatement();
                rs = st.executeQuery("select * from WB_TASK");

                while (rs.next()) {
                    if (rs.getInt("STATUS") != 0) {
                        loadTask(rs.getString("TASK_ID"), rs.getString("TASK_NAME"), rs.getInt("INTERVAL_TYPE"),
                            rs.getString("INTERVAL_EXPRESS"), rs.getString("CLASS_NAME"),
                            (String)DbUtil.getObject(rs, "SERVER_SCRIPT", 2011), rs.getTimestamp("BEGIN_DATE"),
                            rs.getTimestamp("END_DATE"));
                    }
                }
            } finally {
                DbUtil.close(rs);
                DbUtil.close(st);
                DbUtil.close(conn);
            }

        }
    }

    public static synchronized void stop() throws Exception {
        if (Var.getBool("sys.task.enabled")) {
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdown();
                scheduler = null;
                Thread.sleep((long)Var.getInt("sys.task.stopDelay"));
            }
        }
    }
}