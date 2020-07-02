package net.lamgc.pixiv.downloader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class MainClass {

    private final static Logger log = LoggerFactory.getLogger(MainClass.class);

    private final static File cookieStoreFile = new File("./cookies.store");

    private final static File searchList = new File("./search.list");

    public static void main(String[] args) throws Exception {
        download();
        getTagsToFile();
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


        if(!searchList.exists()) {
            System.err.println("搜索列表不存在!");
            System.exit(1);
            return;
        }
        final JsonObject searchInfo = new Gson()
                .fromJson(new String(Files.readAllBytes(searchList.toPath())), JsonObject.class);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> saveSearchInfo(searchInfo)));

        IllustFilter filter = new PagesCountFilter(0, 2);
        ImageFileStore store = new DeepDanbooruImageStore(new File("./image/"));
        MetadataDatabase database = new DeepDanbooruDatabase(new File("./deepDanbooru.db"));
        PixivSearchDownloader downloader = new PixivSearchDownloader(cookieStore, new HttpHost("127.0.0.1", 1080),
                filter,
                store, database);

        JsonArray searchContentArray = searchInfo.getAsJsonArray("searchContent");
        int currentPagesIndex = searchInfo.get("lastSearchPagesIndex").getAsInt();
        if(currentPagesIndex <= 0) {
            currentPagesIndex = 1;
        }
        for (int searchContentIndex = searchInfo.get("lastSearchContentIndex").getAsInt();
             searchContentIndex < searchContentArray.size(); searchContentIndex++) {
            String searchContent = searchContentArray.get(searchContentIndex).getAsString();

            searchInfo.addProperty("lastSearchContent", searchContent);
            PixivSearchLinkBuilder searchBuilder = new PixivSearchLinkBuilder(searchContent);
            do {
                searchInfo.addProperty("lastSearchPagesIndex", currentPagesIndex);
                log.info("{}. Url: {}", currentPagesIndex, searchBuilder.buildURL());
                saveSearchInfo(searchInfo);
                searchBuilder.setPage(currentPagesIndex++);
            } while(downloader.executeSearch(searchBuilder) != 0);
            currentPagesIndex = 1;
        }

        database.close();
    }

    private static void saveSearchInfo(JsonObject searchInfo) {
        try {
            Files.write(searchList.toPath(),
                    new GsonBuilder()
                            .serializeNulls()
                            .setPrettyPrinting()
                            .create()
                            .toJson(searchInfo)
                            .getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log.error("保存配置文件失败！", e);
        }
    }


}
