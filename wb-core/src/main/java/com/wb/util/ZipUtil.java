package com.wb.util;

import com.syspatch.zip.ZipEntry;
import com.syspatch.zip.ZipInputStream;
import com.syspatch.zip.ZipOutputStream;
import com.wb.common.Var;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;

public class ZipUtil {
	public static void zip(File[] source, OutputStream outputStream) throws IOException {
		ZipOutputStream zipStream = new ZipOutputStream(outputStream);
		zipStream.fileCharset = Var.getString("sys.locale.filenameCharset");

		try {
			File[] var6 = source;
			int var5 = source.length;

			for (int var4 = 0; var4 < var5; ++var4) {
				File file = var6[var4];
				zip(file, zipStream, file.getName());
			}
		} finally {
			zipStream.close();
		}

	}

	public static void zip(File[] source, File zipFile) throws Exception {
		zip(source, (OutputStream) (new FileOutputStream(zipFile)));
	}

	private static void zip(File source, ZipOutputStream zipStream, String base) throws IOException {
		ZipEntry entry;
		if (source.isDirectory()) {
			entry = new ZipEntry(base + '/');
			entry.setTime(source.lastModified());
			zipStream.putNextEntry(entry);
			if (!StringUtil.isEmpty(base)) {
				base = base + '/';
			}

			File[] fileList = FileUtil.listFiles(source);
			File[] var8 = fileList;
			int var7 = fileList.length;

			for (int var6 = 0; var6 < var7; ++var6) {
				File file = var8[var6];
				zip(file, zipStream, base + file.getName());
			}
		} else {
			entry = new ZipEntry(base);
			entry.setTime(source.lastModified());
			zipStream.putNextEntry(entry);
			FileInputStream in = new FileInputStream(source);

			try {
				IOUtils.copy(in, zipStream);
			} finally {
				in.close();
			}
		}

	}

	public static void unzip(InputStream inputStream, File dest) throws IOException {
		ZipInputStream zipStream = new ZipInputStream(inputStream);
		zipStream.fileCharset = Var.getString("sys.locale.filenameCharset");

		ZipEntry z;
		File f;
		try {
			for (; (z = zipStream.getNextEntry()) != null; f.setLastModified(z.getTime())) {
				String name = z.getName();
				if (z.isDirectory()) {
					name = name.substring(0, name.length() - 1);
					f = new File(dest, name);
					if (!f.exists()) {
						f.mkdir();
					}
				} else {
					f = new File(dest, name);
					if (!f.exists()) {
						f.createNewFile();
					}

					FileOutputStream out = new FileOutputStream(f);

					try {
						IOUtils.copy(zipStream, out);
					} finally {
						out.close();
					}
				}
			}
		} finally {
			zipStream.close();
		}

	}

	public static void unzip(File zipFile, File dest) throws IOException {
		unzip((InputStream) (new FileInputStream(zipFile)), dest);
	}
}