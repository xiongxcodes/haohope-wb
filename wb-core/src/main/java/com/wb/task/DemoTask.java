package com.wb.task;

import com.wb.util.DateUtil;
import java.util.Date;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class DemoTask implements Job {
	public void execute(JobExecutionContext context) throws JobExecutionException {
		System.out.println(DateUtil.format(new Date(), "hh:mm:ss") + ": 示例任务 3");
	}
}