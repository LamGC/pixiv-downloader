package net.lamgc.pixiv.downloader.util;

import org.apache.commons.codec.binary.Hex;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class MessageDigestUtils {

    private MessageDigestUtils() {}

    /**
     * 取数据Md5摘要
     * @param data 数据
     * @return 返回摘要结果
     */
    public static byte[] md5(byte[] data) {
        try {
            return MessageDigest.getInstance("MD5").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 取数据Md5摘要并返回字符串结果
     * @param data 数据
     * @param toLowerCase 是否全小写
     * @return 返回摘要结果的字符串形式
     */
    public static String md5ToString(byte[] data, boolean toLowerCase) {
        return Hex.encodeHexString(md5(data), toLowerCase);
    }

}
