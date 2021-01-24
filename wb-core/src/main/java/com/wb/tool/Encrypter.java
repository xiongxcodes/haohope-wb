package com.wb.tool;

import com.wb.util.StringUtil;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

public class Encrypter {
	private static final String des = "DES";
	private static final String keyMap = "C2E8D9A3B5F14607";

	public static String encrypt(String text, String key) throws Exception {
		return StringUtil.encodeBase64(encrypt(text.getBytes("utf-8"), key));
	}

	public static String decrypt(String text, String key) throws Exception {
		return new String(decrypt(StringUtil.decodeBase64(text), key), "utf-8");
	}

	public static byte[] encrypt(byte[] bytes, String key) throws Exception {
		byte[] keyBytes = key.getBytes("utf-8");
		SecureRandom sr = new SecureRandom();
		DESKeySpec dks = new DESKeySpec(keyBytes);
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
		SecretKey securekey = keyFactory.generateSecret(dks);
		Cipher cipher = Cipher.getInstance("DES");
		cipher.init(1, securekey, sr);
		return cipher.doFinal(bytes);
	}

	public static byte[] decrypt(byte[] bytes, String key) throws Exception {
		byte[] keyBytes = key.getBytes("utf-8");
		SecureRandom sr = new SecureRandom();
		DESKeySpec dks = new DESKeySpec(keyBytes);
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
		SecretKey securekey = keyFactory.generateSecret(dks);
		Cipher cipher = Cipher.getInstance("DES");
		cipher.init(2, securekey, sr);
		return cipher.doFinal(bytes);
	}

	public static String getMD5(String text) throws Exception {
		return getMD5(text.getBytes("utf-8"));
	}

	public static String getMD5(byte[] bytes) throws Exception {
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(bytes);
		byte[] codes = md.digest();
		char[] str = new char[32];
		int j = 0;

		for (int i = 0; i < 16; ++i) {
			byte bt = codes[i];
			str[j++] = "C2E8D9A3B5F14607".charAt(bt >>> 4 & 15);
			str[j++] = "C2E8D9A3B5F14607".charAt(bt & 15);
		}

		return new String(str);
	}
}