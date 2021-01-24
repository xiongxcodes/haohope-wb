package com.wb.task;

import com.wb.common.ScriptBuffer;
import com.wb.common.Var;
import com.wb.util.DateUtil;
import com.wb.util.LogUtil;
import com.wb.util.StringUtil;
import com.wb.util.SysUtil;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class ScriptProxy implements Job {
	public void execute(JobExecutionContext context) throws JobExecutionException {
		long start = System.currentTimeMillis();
		String jobDesc = context.getJobDetail().getDescription();

		try {
			if (Var.taskLog) {
				LogUtil.info("Start job " + jobDesc);
			}

			JobDataMap dataMap = context.getJobDetail().getJobDataMap();
			String serverScript = dataMap.getString("job.serverScript");
			if (!StringUtil.isEmpty(serverScript)) {
				ScriptBuffer.run(dataMap.getString("job.id"), serverScript, context);
			}

			if (Var.taskLog) {
				LogUtil.info(StringUtil.concat(new String[]{"Finish job ", jobDesc, " in ",
						DateUtil.format(System.currentTimeMillis() - start)}));
			}
		} catch (Throwable var7) {
			if (Var.taskLog) {
				LogUtil.error(StringUtil.concat(new String[]{"Execute job ", jobDesc, " failed with error ",
						SysUtil.getRootError(var7), " in ", DateUtil.format(System.currentTimeMillis() - start)}));
			}

			if (Var.printError) {
				throw new RuntimeException(var7);
			}
		}

	}
}