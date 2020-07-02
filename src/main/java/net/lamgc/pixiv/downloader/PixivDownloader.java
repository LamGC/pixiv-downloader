package net.lamgc.pixiv.downloader;

import com.google.common.io.ByteStreams;
import net.lamgc.pixiv.downloader.filter.IllustFilter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Objects;

public abstract class PixivDownloader {

    private final IllustFilter filter;

    private final ImageFileStore fileStore;

    private final MetadataDatabase database;

    public PixivDownloader(IllustFilter filter, ImageFileStore fileStore, MetadataDatabase database) {
        Objects.requireNonNull(this.filter = filter);
        Objects.requireNonNull(this.fileStore = fileStore);
        Objects.requireNonNull(this.database = database);
    }

    IllustFilter getFilter() {
        return this.filter;
    }

    /**
     * 导入作品
     * @param id 作品Id
     * @param pageCount 作品总页数
     * @param pageIndex 当前作品页面页码
     * @param title 作品标题
     * @param description 作品说明
     * @param userId 作者Id
     * @param fileExtName 页面Id
     * @param tags 标签数组
     * @param imageInputStream 图片数据
     * @throws Exception 处理时可能发生的异常
     */
    void putIllust(int id,
                   int pageCount,
                   int pageIndex,
                   String title,
                   String description,
                   int userId,
                   String fileExtName,
                   String[] tags,
                   InputStream imageInputStream) throws Exception {
        if(!this.filter.filterIllust(id, pageCount, title, description, userId, fileExtName, tags)) {
            return;
        }
        ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
        ByteStreams.copy(imageInputStream, bufferStream);
        fileStore.save(id, pageIndex, title, userId, new ByteArrayInputStream(bufferStream.toByteArray()), fileExtName);
        this.database.putImageMetaData(id, pageIndex, title, description, userId,
                        new ByteArrayInputStream(bufferStream.toByteArray()), fileExtName, tags);
    }

}
