package net.lamgc.pixiv.downloader.filter;

public interface IllustFilter {

    /**
     * 过滤作品
     * @param id 作品Id
     * @param pageCount 作品页数
     * @param title 作品标题
     * @param description 作品说明内容
     * @param userId 作者Id
     * @param fileExtName 文件拓展名, 如未知则传入null
     * @param tags 标签
     * @return 如果返回true, 则继续处理, 否则跳过该作品.
     */
    boolean filterIllust(int id,
                         int pageCount,
                         String title,
                         String description,
                         int userId,
                         String fileExtName,
                         final String[] tags);

}
