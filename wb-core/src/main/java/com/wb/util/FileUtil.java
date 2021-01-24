package com.wb.util;

import com.wb.common.Base;
import com.wb.common.UrlBuffer;
import com.wb.common.Var;
import com.wb.common.XwlBuffer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import javax.swing.filechooser.FileSystemView;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class FileUtil {
	public static File getUniqueFile(File file) {
		if (file.exists()) {
			File parent = file.getParentFile();
			String fullName = file.getName();
			String namePart = removeExtension(fullName);
			String extPart = getFileExt(fullName);
			boolean emptyExt = extPart.isEmpty();
			int i = 1;
			if (!emptyExt) {
				extPart = '.' + extPart;
			}

			do {
				if (emptyExt) {
					file = new File(parent, StringUtil.concat(new String[]{namePart, Integer.toString(i)}));
				} else {
					file = new File(parent, StringUtil.concat(new String[]{namePart, Integer.toString(i), extPart}));
				}

				++i;
			} while (file.exists());
		}

		return file;
	}

	public static String readString(File file) throws Exception {
		return FileUtils.readFileToString(file, "utf-8");
	}

	public static void writeString(File file, String content) throws Exception {
		FileUtils.writeStringToFile(file, content, "utf-8");
	}

	public static void saveStream(InputStream inputStream, File file) throws IOException {
		FileOutputStream os = new FileOutputStream(file);

		try {
			IOUtils.copy(inputStream, os);
		} finally {
			IOUtils.closeQuietly(inputStream);
			IOUtils.closeQuietly(os);
		}

	}

	public static String getFileExt(String fileName) {
		if (fileName != null) {
			int i = fileName.lastIndexOf(46);
			if (i != -1) {
				return fileName.substring(i + 1);
			}
		}

		return "";
	}

	public static String getFileExt(File file) {
		return getFileExt(file.getName());
	}

	public static String getFileType(File file) {
		String type;
		try {
			type = FileSystemView.getFileSystemView().getSystemTypeDescription(file);
		} catch (Throwable var3) {
			type = null;
		}

		return StringUtil.isEmpty(type) ? getFileExt(file.getName()) : type;
	}

	public static String getFilename(String path) {
		if (StringUtil.isEmpty(path)) {
			return "";
		} else {
			int p = Math.max(path.lastIndexOf(47), path.lastIndexOf(92));
			return p == -1 ? path : path.substring(p + 1);
		}
	}

	public static String removeExtension(String fileName) {
		String s = getFilename(fileName);
		int i = s.lastIndexOf(46);
		return i != -1 ? s.substring(0, i) : s;
	}

	public static String addExtension(String fileName, String extension) {
		if (fileName == null) {
			return null;
		} else {
			return fileName.toLowerCase().endsWith("." + extension.toLowerCase())
					? fileName
					: fileName + "." + extension;
		}
	}

	public static String getPath(String path) {
		return StringUtil.replaceAll(path, "\\", "/");
	}

	public static String getPath(File file) {
		return getPath(file.getAbsolutePath());
	}

	public static String getRelativePath(File parent, File child) {
		if (parent == null) {
			throw new NullPointerException("Parent file is null.");
		} else if (child == null) {
			throw new NullPointerException("Child file is null.");
		} else {
			String originChildPath = child.getAbsolutePath();
			String childPath = originChildPath + File.separatorChar;
			String parentPath = parent.getAbsolutePath() + File.separatorChar;
			if (childPath.equals(parentPath)) {
				return "";
			} else {
				return childPath.startsWith(parentPath)
						? getPath(originChildPath.substring(parentPath.length()))
						: null;
			}
		}
	}

	public static boolean isAncestor(File parent, File child) throws IOException {
		return isAncestor(parent, child, true);
	}

	public static boolean isAncestor(File parent, File child, boolean includeSelf) throws IOException {
		String parentPath = parent.getCanonicalPath();
		String childPath = child.getCanonicalPath();
		if (!parentPath.endsWith(String.valueOf(File.separatorChar))) {
			parentPath = parentPath + File.separatorChar;
		}

		if (!childPath.endsWith(String.valueOf(File.separatorChar))) {
			childPath = childPath + File.separatorChar;
		}

		return childPath.startsWith(parentPath) && (includeSelf || childPath.length() > parentPath.length());
	}

	public static void checkProctected(HttpServletRequest request, File file, boolean containModule)
			throws IOException {
		if (!isAncestor(Base.path, file) || isAncestor(new File(Base.path, "wb/system"), file)
				|| isAncestor(new File(Base.path, "META-INF"), file) || isAncestor(new File(Base.path, "WEB-INF"), file)
				|| containModule && isAncestor(new File(Base.path, "wb/modules"), file)) {
			SysUtil.accessDenied(request);
		}

	}

	public static boolean isEmpty(File folder) {
		String[] fs;
		return folder == null || (fs = folder.list()) == null || fs.length == 0;
	}

	public static boolean hasFolder(File folder) {
		File[] fs;
		if (folder != null && (fs = folder.listFiles()) != null && fs.length > 0) {
			File[] var5 = fs;
			int var4 = fs.length;

			for (int var3 = 0; var3 < var4; ++var3) {
				File file = var5[var3];
				if (file.isDirectory()) {
					return true;
				}
			}
		}

		return false;
	}

	public static boolean checkLastModified(File file, Date lastModified) {
		if (!file.isDirectory()) {
			return file.lastModified() >= lastModified.getTime();
		} else {
			File[] fs = file.listFiles();
			File[] var6 = fs;
			int var5 = fs.length;

			for (int var4 = 0; var4 < var5; ++var4) {
				File f = var6[var4];
				if (checkLastModified(f, lastModified)) {
					return true;
				}
			}

			return false;
		}
	}

	public static File[] listFiles(File file) {
		if (!file.exists()) {
			throw new RuntimeException("\"" + file.getName() + "\" does not exist.");
		} else {
			File[] fs = file.listFiles();
			return fs == null ? new File[0] : fs;
		}
	}

	public static Object[] syncCopy(File src, File dst) throws IOException {
		String name = src.getName();
		dst = new File(dst, name);
		File syncDst = getSyncPath(dst);
		boolean isDir = src.isDirectory();
		boolean sameParent = src.getParentFile().equals(dst.getParentFile());
		if (sameParent) {
			dst = getUniqueFile(dst);
		}

		boolean dstExists = dst.exists();
		if (isDir) {
			FileUtils.copyDirectory(src, dst);
		} else {
			FileUtils.copyFile(src, dst);
		}

		if (syncDst != null) {
			if (sameParent) {
				syncDst = getUniqueFile(syncDst);
			}

			if (isDir) {
				FileUtils.copyDirectory(src, syncDst);
			} else {
				FileUtils.copyFile(src, syncDst);
			}
		}

		Object[] result = new Object[]{getPath(dst), dstExists};
		return result;
	}

	public static void syncCopyA(File src, File dst) throws IOException {
		boolean isDir = src.isDirectory();
		if (isDir) {
			FileUtils.copyDirectory(src, dst);
		} else {
			FileUtils.copyFile(src, dst);
		}

		File syncPath = getSyncPath(dst);
		if (syncPath != null) {
			if (isDir) {
				FileUtils.copyDirectory(src, syncPath);
			} else {
				FileUtils.copyFile(src, syncPath);
			}
		}

	}

	public static void syncCreate(File file, boolean isDir) throws IOException {
		String name = file.getName();
		File syncPath = getSyncPath(file);
		if (file.exists()) {
			throw new IllegalArgumentException("\"" + name + "\" already exists.");
		} else if (syncPath != null && syncPath.exists()) {
			throw new IllegalArgumentException("\"" + syncPath.getAbsolutePath() + "\" already exists.");
		} else {
			if (isDir) {
				if (!file.mkdir()) {
					throw new IOException("Create \"" + name + "\" failure.");
				}

				if (syncPath != null && !syncPath.mkdir()) {
					throw new IOException("Create \"" + syncPath.getAbsolutePath() + "\" failure.");
				}
			} else {
				if (!file.createNewFile()) {
					throw new IOException("Create \"" + name + "\" failure.");
				}

				if (syncPath != null && !syncPath.createNewFile()) {
					throw new IOException("Create \"" + syncPath.getAbsolutePath() + "\" failure.");
				}
			}

			if (syncPath != null) {
				syncPath.setLastModified(file.lastModified());
			}

		}
	}

	public static void syncDelete(File file, boolean clearUrl) throws Exception {
		if (!FileUtils.deleteQuietly(file)) {
			throw new IOException("Cannot delete \"" + file.getName() + "\".");
		} else {
			File syncPath = getSyncPath(file);
			if (syncPath != null && !FileUtils.deleteQuietly(syncPath)) {
				throw new IOException("Cannot delete \"" + syncPath.toString() + "\".");
			} else {
				clearFiles(file, clearUrl);
			}
		}
	}

	public static void syncMove(File src, File dst) throws Exception {
		File syncDst = getSyncPath(dst);
		FileUtils.moveToDirectory(src, dst, true);
		if (syncDst != null) {
			try {
				File syncSrc = getSyncPath(src);
				if (syncSrc == null) {
					if (syncDst.isDirectory()) {
						FileUtils.copyDirectory(dst, syncDst);
					} else {
						FileUtils.copyFile(dst, syncDst);
					}
				} else {
					FileUtils.moveToDirectory(syncSrc, syncDst, true);
				}
			} catch (Throwable var4) {
				;
			}
		}

		clearFiles(src, false);
	}

	public static void syncRename(File file, File newFile) throws IOException {
		File syncPath = getSyncPath(file);
		if (!file.renameTo(newFile)) {
			throw new IOException("Cannot rename \"" + file.getName() + "\".");
		} else if (syncPath != null && !syncPath.renameTo(getSyncPath(newFile))) {
			throw new IOException("Cannot rename \"" + syncPath.toString() + "\".");
		}
	}

	public static void syncSave(File file, byte[] content) throws Exception {
		FileUtils.writeByteArrayToFile(file, content);
		File syncPath = getSyncPath(file);
		if (syncPath != null) {
			FileUtils.copyFile(file, syncPath);
		}

	}

	public static void syncSave(File file, String content) throws Exception {
		syncSave(file, content, "utf-8");
	}

	public static void syncSave(File file, String content, String charset) throws Exception {
		if (StringUtil.isEmpty(charset)) {
			FileUtils.writeStringToFile(file, content);
		} else {
			FileUtils.writeStringToFile(file, content, charset);
		}

		File syncPath = getSyncPath(file);
		if (syncPath != null) {
			FileUtils.copyFile(file, syncPath);
		}

		String relPath = getModulePath(file);
		if (relPath != null) {
			XwlBuffer.clear(relPath);
		}

	}

	public static File getSyncPath(File path) throws IOException {
		if (!Var.syncPath.isEmpty() && isAncestor(Base.path, path)) {
			File base = new File(Var.syncPath);
			String filePath = getPath(path);
			return filePath.length() < Base.pathLen ? base : new File(base, getPath(path).substring(Base.pathLen));
		} else {
			return null;
		}
	}

	public static String getIconPath(String iconCls) {
		int len = iconCls.length() - 5;
		return len > 0
				? StringUtil.concat(new String[]{"wb/images/", iconCls.substring(0, iconCls.length() - 5), ".png"})
				: null;
	}

	private static void clearFiles(File file, boolean clearUrl) throws Exception {
		File folder = file.getParentFile();
		File configFile = new File(folder, "folder.json");
		if (configFile.exists()) {
			JSONObject object = JsonUtil.readObject(configFile);
			JSONArray index = object.optJSONArray("index");
			if (index != null) {
				int j = index.length();

				for (int i = j - 1; i >= 0; --i) {
					File indexFile = new File(folder, index.getString(i));
					if (!indexFile.exists()) {
						index.remove(i);
					}
				}

				syncSave(configFile, object.toString());
			}
		}

		String relPath = getModulePath(file);
		if (relPath != null) {
			XwlBuffer.clear(relPath);
			if (clearUrl && UrlBuffer.remove(relPath)) {
				UrlBuffer.save();
			}
		}

	}

	public static String getModuleUrl(File file) {
		String path = getPath(file);
		return path.startsWith(Base.modulePathText) && path.toLowerCase().endsWith(".xwl")
				? "m?xwl=" + path.substring(Base.modulePathLen, path.length() - 4)
				: null;
	}

	public static String getModulePath(File file) {
		String path = getPath(file);
		return path.startsWith(Base.modulePathText) ? path.substring(Base.modulePathLen) : null;
	}

	public static String getModulePath(String url) {
		return getModulePath(url, false);
	}

	public static String getModulePath(String url, boolean silent) {
		if (url == null) {
			if (silent) {
				return null;
			} else {
				throw new NullPointerException("The requested url is not specified.");
			}
		} else if (url.startsWith("m?xwl=")) {
			return url.substring(6) + ".xwl";
		} else if (url.endsWith(".xwl")) {
			return url;
		} else {
			String shortcut = url;
			url = UrlBuffer.get("/" + url);
			if (url == null) {
				if (silent) {
					return null;
				} else {
					throw new NullPointerException("The requested url shortcut \"" + shortcut + "\" is not found.");
				}
			} else {
				return url;
			}
		}
	}

	public static JSONObject getModule(String url) throws Exception {
		return XwlBuffer.get(getModulePath(url));
	}
}