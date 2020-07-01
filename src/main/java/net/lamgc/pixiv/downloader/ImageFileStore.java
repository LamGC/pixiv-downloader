package net.lamgc.pixiv.downloader;

import java.io.InputStream;

public interface ImageFileStore {

    /**
     * 保存作品页面
     * @param id 作品Id
     * @param pageIndex 当前页码
     * @param title 作品标题
     * @param userId 用户Id
     * @param imageInputStream 图片输入流
     * @param fileExtName 图片拓展名
     */
    void save(int id, int pageIndex, String title, int userId,
              InputStream imageInputStream, String fileExtName) throws Exception;


}
