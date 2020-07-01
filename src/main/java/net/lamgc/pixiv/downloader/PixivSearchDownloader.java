package net.lamgc.pixiv.downloader;

import net.lamgc.pixiv.downloader.filter.IllustFilter;

public class PixivSearchDownloader extends PixivDownloader {

    public PixivSearchDownloader(IllustFilter filter, ImageFileStore fileStore, MetadataDatabase database) {
        super(filter, fileStore, database);
    }

    public void executeSearch() {

    }

}
