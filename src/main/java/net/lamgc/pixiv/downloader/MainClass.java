package net.lamgc.pixiv.downloader;

import net.lamgc.cgj.pixiv.PixivSearchLinkBuilder;
import net.lamgc.pixiv.downloader.deepdanbooru.DeepDanbooruDatabase;
import net.lamgc.pixiv.downloader.deepdanbooru.DeepDanbooruImageStore;
import net.lamgc.pixiv.downloader.filter.IllustFilter;
import net.lamgc.pixiv.downloader.filter.PagesCountFilter;
import org.apache.http.HttpHost;
import org.apache.http.client.CookieStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class MainClass {

    private final static Logger log = LoggerFactory.getLogger(MainClass.class);

    private final static File cookieStoreFile = new File("./cookies.store");

    public static void main(String[] args) throws Exception {
        download();
    }

    public static void getTagsToFile() throws Exception {
        MetadataDatabase database = new DeepDanbooruDatabase(new File("./deepDanbooru.db"));
        File tagsFile = new File("./tags.txt");
        if(!tagsFile.exists() && !tagsFile.createNewFile()) {
            throw new IOException("File create failure");
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tagsFile))) {
            for (String tag : database.getTags()) {
                writer.write(tag + "\n");
            }
            writer.flush();
        }
        database.close();
    }

    public static void download() throws Exception {
        if(!cookieStoreFile.exists()) {
            System.err.println("CookieStore文件不存在!");
            System.exit(1);
            return;
        }
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(cookieStoreFile));
        CookieStore cookieStore = (CookieStore) ois.readObject();

        IllustFilter filter = new PagesCountFilter(1, 2);
        ImageFileStore store = new DeepDanbooruImageStore(new File("./image/"));
        MetadataDatabase database = new DeepDanbooruDatabase(new File("./deepDanbooru.db"));
        PixivSearchDownloader downloader = new PixivSearchDownloader(cookieStore, new HttpHost("127.0.0.1", 1080),
                (id, pageCount, title, description, userId, fileExtName, tags) -> pageCount == 1,
                store, database);

        List<String> searchContentList = new ArrayList<>();
        searchContentList.add("VTuber");
        searchContentList.add("黑丝");
        searchContentList.add("白丝");
        searchContentList.add("碧蓝航线");
        searchContentList.add("公主连结");
        searchContentList.add("崩坏3");
        searchContentList.add("少女前线");

        for (String searchContent : searchContentList) {
            PixivSearchLinkBuilder searchBuilder = new PixivSearchLinkBuilder(searchContent);
            int currentPagesIndex = 5; // 5开始
            do {
                searchBuilder.setPage(++currentPagesIndex);
                log.info("{}. Url: {}", currentPagesIndex, searchBuilder.buildURL());
            } while(downloader.executeSearch(searchBuilder) != 0);
        }

        database.close();
    }


}
