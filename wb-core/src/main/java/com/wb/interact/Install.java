package com.wb.interact;

import com.wb.common.Base;
import com.wb.common.ScriptBuffer;
import com.wb.common.UrlBuffer;
import com.wb.common.Var;
import com.wb.tool.TaskManager;
import com.wb.util.DateUtil;
import com.wb.util.DbUtil;
import com.wb.util.FileUtil;
import com.wb.util.JsonUtil;
import com.wb.util.StringUtil;
import com.wb.util.SysUtil;
import com.wb.util.WebUtil;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class Install {
	public static void checkInstall(HttpServletRequest request, HttpServletResponse response) throws Exception {
		if (!Var.getBool("sys.service.allowInstall")) {
			throw new RuntimeException("系统已经安装完成，如需重新安装请设置变量sys.service.allowInstall为true。");
		}
	}

	public static void generateJndi(HttpServletRequest request, HttpServletResponse response) throws Exception {
		checkInstall(request, response);
		String overwrite = request.getParameter("overwrite");
		if (overwrite == null) {
			throw new NullPointerException("overwrite 参数为null");
		} else {
			char dbType = request.getParameter("dbType").charAt(0);
			String contextFilename = null;
			String libFilename = null;
			switch (dbType) {
				case 'm' :
					contextFilename = "m-context.xml";
					libFilename = "mysql51.jar";
				case 'n' :
				case 'p' :
				case 'q' :
				case 'r' :
				default :
					break;
				case 'o' :
					contextFilename = "o-context.xml";
					libFilename = "ojdbc6.jar";
					break;
				case 's' :
					contextFilename = "s-context.xml";
					libFilename = "sqljdbc4.jar";
			}

			File metaFolder = new File(Base.path, "META-INF");
			File contextFile = new File(metaFolder, "context.xml");
			if (!metaFolder.exists() && !metaFolder.mkdir()) {
				throw new IOException("创建META-INF目录失败。");
			} else {
				if (!Boolean.parseBoolean(overwrite) && contextFile.exists()) {
					SysUtil.error("文件 \"META-INF/context.xml\" 已经存在，确定要覆盖吗？", "106");
				}

				File rootFolder = Base.path.getParentFile().getParentFile();
				File libFolder = new File(rootFolder, "lib");
				if (!(new File(libFolder, libFilename)).exists()) {
					FileUtils.copyFileToDirectory(new File(Base.path, "wb/system/database/" + libFilename), libFolder);
				}

				String xml = FileUtil.readString(new File(Base.path, "wb/system/database/" + contextFilename));
				xml = WebUtil.replaceParams(request, xml);
				FileUtil.writeString(contextFile, xml);
				File syncFolder = new File(rootFolder, "conf/Catalina/localhost");
				if (!syncFolder.exists()) {
					syncFolder.mkdirs();
				}

				FileUtil.writeString(new File(syncFolder, Base.path.getName() + ".xml"), xml);
			}
		}
	}

	public static synchronized void getPack(HttpServletRequest request, HttpServletResponse response) throws Exception {
		getReleasePath();
		IDE.makeFile(request, Base.path, false);
		exportWBTables("r".equals(request.getParameter("type")));
		createPack(request, response);
	}

	private static void exportWBTables(boolean userRelease) throws Exception {
		Connection conn = null;
		Statement st = null;
		ResultSet rs = null;
		String[] withoutTables = new String[]{"WB_BBS_FILE", "WB_BBS_POST", "WB_BBS_TITLE", "WB_DBM", "WB_FLOW",
				"WB_FLOW_DEMO", "WB_FLOW_LIST", "WB_FLOW_USER", "WB_IM", "WB_LOG", "WB_SN", "WB_SYS1", "WB_SYS2",
				"WB_VALUE", "WB_VERSION"};
		String[] sqls = FileUtil.readString(new File(Base.path, "wb/system/database/sqls.sql")).split(";");
		File tablesPath = new File(Base.path, "wb/system/database/tables");
		File syncPath = FileUtil.getSyncPath(tablesPath);

		try {
			FileUtils.cleanDirectory(tablesPath);
			if (syncPath != null) {
				FileUtils.cleanDirectory(syncPath);
			}

			conn = DbUtil.getConnection();
			st = conn.createStatement();
			st.executeUpdate("update WB_USER set LOGIN_TIMES=0");
			String[] var16 = sqls;
			int var15 = sqls.length;

			for (int var14 = 0; var14 < var15; ++var14) {
				String sql = var16[var14];
				String upperSQL = sql.toUpperCase();
				int index = upperSQL.indexOf("CREATE TABLE");
				if (index != -1) {
					String tableName = sql.substring(index + 13, sql.indexOf(40)).trim();
					if (userRelease || StringUtil.indexOf(withoutTables, tableName) == -1) {
						BufferedWriter writer = null;
						rs = null;

						try {
							File destTable = new File(tablesPath, tableName + ".dat");
							if (userRelease) {
								rs = st.executeQuery("select * from " + tableName);
							} else if (tableName.equals("WB_USER")) {
								rs = st.executeQuery("select * from WB_USER where USER_ID in ('admin','test')");
							} else if (tableName.equals("WB_RESOURCE")) {
								rs = st.executeQuery(
										"select * from WB_RESOURCE where RES_ID in ('admin@desktop','test@desktop','admin@collectPages','test@collectPages')");
							} else if (tableName.equals("WB_USER_ROLE")) {
								rs = st.executeQuery(
										"select * from WB_USER_ROLE where USER_ID='test' and ROLE_ID='demo' or USER_ID='admin'");
							} else {
								rs = st.executeQuery("select * from " + tableName);
							}

							writer = new BufferedWriter(
									new OutputStreamWriter(new FileOutputStream(destTable), "utf-8"));
							DbUtil.exportData(rs, writer);
							writer.close();
							writer = null;
							if (syncPath != null) {
								FileUtils.copyFileToDirectory(destTable, syncPath);
							}
						} finally {
							if (writer != null) {
								writer.close();
							}

							if (rs != null) {
								rs.close();
							}

						}
					}
				}
			}
		} finally {
			DbUtil.close(rs);
			DbUtil.close(st);
			DbUtil.close(conn);
		}

	}

	public static void testJNDI(HttpServletRequest request, HttpServletResponse response) throws Exception {
		checkInstall(request, response);
		String jndi = request.getParameter("jndi");
		Var.set("sys.jndi.default", jndi);
		Var.jndi = jndi;
		DbUtil.getConnection().close();
	}

	public static void setup(HttpServletRequest request, HttpServletResponse response) throws Exception {
		checkInstall(request, response);
		String jndi = request.getParameter("jndiText");
		String type = request.getParameter("typeText");
		String versionType = Var.getString("sys.app.versionType");
		boolean canCreate = true;
		boolean isDemoVersion = "d".equals(versionType);
		String[] reservedTables = new String[]{"WB_BBS_FILE", "WB_BBS_POST", "WB_BBS_TITLE", "WB_DBM", "WB_LOG",
				"WB_SN", "WB_SYS1", "WB_SYS2", "WB_USER", "WB_USER_ROLE"};
		Var.set("sys.jndi.default", jndi);
		Var.jndi = jndi;
		if ("postgresql".equals(type) && !"e".equals(versionType)) {
			throw new RuntimeException("postgresql数据库类型只有企业版本支持。");
		} else {
			Connection conn = null;
			Statement st = null;
			File mapJson = new File(Base.path, "wb/system/map.json");
			File tablePath = new File(Base.path, "wb/system/database/tables");

			try {
				JSONArray mapArray = JsonUtil.readArray(mapJson);
				JSONObject jo = new JSONObject();
				jo.put("var.json", FileUtil.readString(Var.file));
				FileUtil.syncSave(mapJson, mapArray.put(jo).toString());
				String[] sqls = replaceSQLMacro(FileUtil.readString(new File(Base.path, "wb/system/database/sqls.sql")),
						type).split(";");
				conn = DbUtil.getConnection();
				st = conn.createStatement();
				String[] var21 = sqls;
				int var20 = sqls.length;

				for (int var19 = 0; var19 < var20; ++var19) {
					String sql = var21[var19];
					int index = sql.indexOf("CREATE TABLE");
					if (index == -1) {
						if (canCreate) {
							st.executeUpdate(sql.trim());
						}
					} else {
						String tableName = sql.substring(index + 13, sql.indexOf(40)).trim();
						if (isDemoVersion) {
							canCreate = StringUtil.indexOf(reservedTables, tableName) == -1;
							if (!canCreate) {
								continue;
							}
						}

						try {
							st.executeUpdate("DROP TABLE " + tableName);
						} catch (Throwable var26) {
							;
						}

						st.executeUpdate(sql.trim());
						File file = new File(tablePath, StringUtil.concat(new String[]{tableName, ".dat"}));
						if (file.exists()) {
							DbUtil.importData(conn, tableName,
									new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf-8")));
						}
					}
				}
			} finally {
				DbUtil.close(st);
				DbUtil.close(conn);
			}

			Var.set("sys.db.defaultType", type);
			if (isDemoVersion) {
				Var.set("sys.portal", "demo-home");
				UrlBuffer.buffer.put("/", "sys/portal/demo-home.xwl");
			} else {
				Var.set("sys.portal", "index");
				UrlBuffer.put("/", "sys/portal/index.xwl");
			}

			Var.set("sys.service.allowInstall", false);
			SysUtil.reload(3);
			TaskManager.start();
		}
	}

	private static File getReleasePath() {
		String releasePathVar = Var.getString("sys.ide.releasePath");
		if (releasePathVar.isEmpty()) {
			throw new RuntimeException("发布目录变量 \"sys.ide.releasePath\" 未指定。");
		} else {
			File releasePath = new File(releasePathVar);
			if (!releasePath.exists()) {
				throw new RuntimeException("发布目录 \"" + releasePath + "\" 不存在。");
			} else {
				return releasePath;
			}
		}
	}

	private static void createPack(HttpServletRequest request, HttpServletResponse response) throws Exception {
		File releasePath = getReleasePath();
		String type = request.getParameter("type");
		boolean userReleaseVersion = "r".equals(type);
		File destPath = FileUtil.getUniqueFile(
				new File(releasePath, (userReleaseVersion ? Var.getString("sys.app.title") : "wb8" + type) + "("
						+ DateUtil.format(new Date(), "yyyy-MM-dd") + ")"));
		File ssproto;
		if (userReleaseVersion) {
			File[] files = FileUtil.listFiles(Base.path);
			File[] var10 = files;
			int var9 = files.length;

			for (int var8 = 0; var8 < var9; ++var8) {
				ssproto = var10[var8];
				FileUtils.copyDirectoryToDirectory(ssproto, destPath);
			}

			ssproto = new File(destPath, "wb/system/var.json");
			JSONObject varObject = JsonUtil.readObject(ssproto);
			((JSONArray) JsonUtil.getValue(varObject, "sys.db.defaultType", '.')).put(0, "");
			((JSONArray) JsonUtil.getValue(varObject, "sys.jndi.default", '.')).put(0, "");
			((JSONArray) JsonUtil.getValue(varObject, "sys.ide.releasePath", '.')).put(0, "");
			((JSONArray) JsonUtil.getValue(varObject, "sys.ide.syncPath", '.')).put(0, "");
			((JSONArray) JsonUtil.getValue(varObject, "sys.portal", '.')).put(0, "setup");
			((JSONArray) JsonUtil.getValue(varObject, "sys.service.allowInstall", '.')).put(0, true);
			FileUtil.writeString(ssproto, varObject.toString(2));
		} else {
			File destWebInf = new File(destPath, "wb/WEB-INF");
			FileUtils.copyDirectoryToDirectory(new File(Base.path, "WEB-INF/lib"), destWebInf);
			FileUtils.copyFileToDirectory(new File(Base.path, "WEB-INF/web.xml"), destWebInf);
			FileUtils.copyDirectoryToDirectory(new File(Base.path, "wb"), new File(destPath, "wb"));
			if ("e".equals(type)) {
				FileUtils.copyDirectory(new File(Var.getString("sys.ide.sourcePath")), new File(destPath, "source"));
			}

			FileUtils.copyFileToDirectory(
					new File(releasePath, "webbuilder-" + Var.getString("sys.app.version") + ".jar"),
					new File(destPath, "wb/WEB-INF/lib"));
			FileUtils.copyDirectoryToDirectory(new File(releasePath, "misc"), destPath);
			FileUtils.copyFileToDirectory(new File(releasePath, "license.html"), destPath);
			FileUtils.copyFileToDirectory(new File(releasePath, "readme.html"), destPath);
			ssproto = new File(destPath, "wb/wb/system/database/ssproto.sql");
			if (!ssproto.delete()) {
				throw new RuntimeException("无法删除 \"" + ssproto + "\"。");
			}

			String packJSPath = "wb/wb/system/pack.js";
			File packJS = new File(destPath, packJSPath);
			if (!packJS.delete()) {
				throw new RuntimeException("无法删除 \"" + packJS + "\"。");
			}

			packJSPath = "wb/system/pack.js";
			packJS = new File(Base.path, packJSPath);
			request.setAttribute("destPath", destPath);
			request.setAttribute("releasePath", releasePath);
			ScriptBuffer.run(packJSPath, FileUtil.readString(packJS), request, response, packJSPath);
		}

		WebUtil.send(response, destPath);
	}

	private static String replaceSQLMacro(String sql, String dbType) throws Exception {
		JSONObject object = JsonUtil.readObject(new File(Base.path, "wb/system/database/types.json"));
		JSONObject items = object.getJSONObject(dbType);
		Set<Entry<String, Object>> es = items.entrySet();

		Entry entry;
		for (Iterator var6 = es.iterator(); var6.hasNext(); sql = StringUtil.replaceAll(sql,
				"{#" + (String) entry.getKey() + "#}", entry.getValue().toString())) {
			entry = (Entry) var6.next();
		}

		return sql;
	}

	public static void register(HttpServletRequest request, HttpServletResponse response) throws Exception {
		Var.set("sys.app.licensee", request.getParameter("licText"));
		Var.set("sys.app.serialNumber", request.getParameter("snText"));
	}

	public static void verify(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String sn;
		String msg;
		try {
			sn = request.getParameter("snText");
			msg = WebUtil.submit("http://www.geejing.com/sn-verify", (new JSONObject()).put("sn", sn));
		} catch (Throwable var5) {
			throw new RuntimeException("验证失败，网络无效或服务暂时不可访问。");
		}

		if (!"ok".equals(msg)) {
			throw new RuntimeException("验证失败，序列号 “" + sn + "” 无效。");
		}
	}
}