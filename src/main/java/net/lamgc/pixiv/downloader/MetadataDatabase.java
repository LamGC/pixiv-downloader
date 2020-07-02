package net.lamgc.pixiv.downloader;

import java.io.InputStream;
import java.util.Set;

public interface MetadataDatabase {

    /**
     * 插入一张图片信息
     * @param id 图片Id
     * @param imageInputStream 作品数据图片数据输入流
     * @param fileExtName 作品页格式拓展名
     * @param tags 作品标签名
     */
    void putImageMetaData(int id, int pageIndex, String title,
                          String description,
                          int userId,
                          InputStream imageInputStream,
                          String fileExtName,
                          String[] tags) throws Exception;

    /**
     * 获取数据库中的所有标签.
     * @return 返回存储了数据库中已有的不重复的标签Set
     * @throws Exception 当获取发生异常时抛出
     */
    Set<String> getTags() throws Exception;

    void close() throws Exception;

}
