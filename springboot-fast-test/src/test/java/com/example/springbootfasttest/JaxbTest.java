package com.example.springbootfasttest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.UUID;

/**
 * <p>
 *
 * </p>
 *
 * <p>
 * CreateDate:2025/12/13
 * </p>
 *
 * @author chenyupeng
 * @history Mender:chenyupeng；Date:2025/12/13；
 */

public class JaxbTest {
    public static void main(String[] args) {
        // nonce=136644785, echostr=5891250609890238191, timestamp=1765875374
        String test = getSHA1("test", "1765875374", "136644785", "");
        System.out.println(test);

        System.out.println(UUID.randomUUID().toString().replaceAll("-",""));
    }

    //503725f9bbdee6dd68fc51ace7c8c41f39d7e5f4
    //503725f9bbdee6dd68fc51ace7c8c41f39d7e5f4


    /**
     * 用SHA1算法生成安全签名
     * @param token 票据
     * @param timestamp 时间戳
     * @param nonce 随机字符串
     * @param encrypt 密文
     * @return 安全签名
     */
    public static String getSHA1(String token, String timestamp, String nonce, String encrypt)
    {

            String[] array = new String[] { token, timestamp, nonce, encrypt };
            StringBuffer sb = new StringBuffer();
            // 字符串排序
            Arrays.sort(array);
            for (int i = 0; i < 4; i++) {
                sb.append(array[i]);
            }
            String str = sb.toString();
            // SHA1签名生成
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        md.update(str.getBytes());
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

    }
}
