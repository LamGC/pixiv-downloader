package net.lamgc.pixiv.downloader;

import java.io.InputStream;

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

    void close() throws Exception;

}
