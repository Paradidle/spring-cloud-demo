package com.example.springbootfasttest.utils;

import java.security.MessageDigest;
import java.util.Arrays;

/**
 * SHA1算法加密工具
 */
public class SHA1Util {

	/**
	 * 用SHA1算法生成安全签名
	 * @param args 加密参数
	 * @return 安全签名
	 */
	public static String getSortSHA1(String... args)
			  {
		try {
			StringBuffer sb = new StringBuffer();
			// 字符串排序
			Arrays.sort(args);
			for (String arg : args) {
				sb.append(arg);
			}
			String str = sb.toString();
			return getSHA1(str);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 用SHA1算法生成安全签名
	 * @param encryptStr 加密参数串
	 * @return 安全签名
	 */
	public static String getSHA1(String encryptStr)
	{
		try {
			// SHA1签名生成
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			md.update(encryptStr.getBytes());
			byte[] digest = md.digest();

			StringBuffer hexstr = new StringBuffer();
			String shaHex = "";
			for (int i = 0; i < digest.length; i++) {
				shaHex = Integer.toHexString(digest[i] & 0xFF);
				if (shaHex.length() < 2) {
					hexstr.append(0);
				}
				hexstr.append(shaHex);
			}
			return hexstr.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
