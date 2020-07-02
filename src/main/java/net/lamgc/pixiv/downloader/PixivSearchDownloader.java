package net.lamgc.pixiv.downloader;

import com.google.common.base.Throwables;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.lamgc.cgj.bot.sort.PreLoadDataAttribute;
import net.lamgc.cgj.pixiv.PixivDownload;
import net.lamgc.cgj.pixiv.PixivSearchLinkBuilder;
import net.lamgc.cgj.pixiv.PixivURL;
import net.lamgc.pixiv.downloader.filter.IllustFilter;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Pixiv搜索下载器.
 *
 */
public class PixivSearchDownloader extends PixivDownloader {

    private final static Logger log = LoggerFactory.getLogger(PixivSearchDownloader.class);

    private final CookieStore cookieStore;

    private final HttpClient httpClient;

    private final PixivDownload pixivDownload;

    public PixivSearchDownloader(CookieStore cookieStore,
                                 HttpHost proxy,
                                 IllustFilter filter,
                                 ImageFileStore fileStore,
                                 MetadataDatabase database) {
        super(filter, fileStore, database);
        this.cookieStore = cookieStore;
        pixivDownload = new PixivDownload(this.cookieStore, proxy);
        this.httpClient = pixivDownload.getHttpClient();
    }

    public int executeSearch(PixivSearchLinkBuilder searchLinkBuilder) throws IOException {
        log.info("正在搜索数据...");
        HttpGet searchRequest = new HttpGet(searchLinkBuilder.buildURL());
        PixivDownload.setCookieInRequest(searchRequest, cookieStore);
        HttpResponse searchResponse = httpClient.execute(searchRequest);
        if (searchResponse.getStatusLine().getStatusCode() != 200) {
            throw new IOException("Http Request error: " + searchResponse);
        }

        JsonObject resultBody = new Gson()
                .fromJson(EntityUtils.toString(searchResponse.getEntity()), JsonObject.class).getAsJsonObject("body");
        log.info(resultBody.toString());
        log.debug("正在处理信息...");
        int totalCount = 0;
        int count = 1;
        for (PixivSearchLinkBuilder.SearchArea searchArea : PixivSearchLinkBuilder.SearchArea.values()) {
            if (!resultBody.has(searchArea.jsonKey) ||
                    resultBody.getAsJsonObject(searchArea.jsonKey).getAsJsonArray("data").size() == 0) {
                log.debug("返回数据不包含 {}", searchArea.jsonKey);
                continue;
            }
            JsonArray illustsArray = resultBody
                    .getAsJsonObject(searchArea.jsonKey).getAsJsonArray("data");
            ArrayList<JsonElement> illustsList = new ArrayList<>();
            illustsArray.forEach(illustsList::add);

            log.debug("已找到与 {} 相关作品信息({})：",
                    searchLinkBuilder.getSearchCondition(), searchArea.name().toLowerCase());
            for (JsonElement jsonElement : illustsList) {
                JsonObject illustObj = jsonElement.getAsJsonObject();
                if (!illustObj.has("illustId")) {
                    continue;
                }
                int illustId = illustObj.get("illustId").getAsInt();
                StringBuilder builder = new StringBuilder("[");
                illustObj.get("tags").getAsJsonArray().forEach(el -> builder.append(el.getAsString()).append(", "));
                builder.replace(builder.length() - 2, builder.length(), "]");
                log.info("{} ({} / {})\n\t作品id: {}, \n\t作者名(作者id): {} ({}), \n\t" +
                                "作品标题: {}, \n\t作品Tags: {}, \n\t页数: {}页, \n\t作品链接: {}",
                        searchArea.name(),
                        count++,
                        illustsList.size(),
                        illustId,
                        illustObj.get("userName").getAsString(),
                        illustObj.get("userId").getAsInt(),
                        illustObj.get("illustTitle").getAsString(),
                        builder,
                        illustObj.get("pageCount").getAsInt(),
                        PixivURL.getPixivRefererLink(illustId)
                );

                try {
                    JsonObject illustPreLoadData = pixivDownload.getIllustPreLoadDataById(illustId).getAsJsonObject(
                            "illust").getAsJsonObject(String.valueOf(illustId));
                    int pagesCount = illustPreLoadData.get(PreLoadDataAttribute.PAGE.attrName).getAsInt();
                    String title = illustObj.get("illustTitle").getAsString();
                    String desc = illustObj.get("description").getAsString();
                    int userId = illustObj.get("userId").getAsInt();
                    JsonArray tags = illustObj.getAsJsonArray("tags");
                    String[] tagsArr = new String[tags.size()];
                    for (int i = 0; i < tags.size(); i++) {
                        tagsArr[i] = tags.get(i).getAsString();
                    }

                    if(!getFilter().filterIllust(illustId, pagesCount, title, desc, userId, null, tagsArr)) {
                        continue;
                    }

                    // 下载图片
                    List<String> pagesList = PixivDownload.getIllustAllPageDownload(httpClient, cookieStore,
                            illustId, PixivDownload.PageQuality.ORIGINAL);

                    for (int pageIndex = 0; pageIndex < pagesList.size(); pageIndex++) {
                        String link = pagesList.get(pageIndex);
                        String fileExtName = link.substring(link.lastIndexOf(".") + 1);
                        HttpGet imageRequest = new HttpGet(link);
                        imageRequest.setConfig(RequestConfig.custom().setSocketTimeout(10000).build());
                        imageRequest.addHeader(HttpHeaders.REFERER, PixivURL.getPixivRefererLink(illustId));
                        HttpResponse imageResponse = httpClient.execute(imageRequest);
                        log.info("StatusLine: {}, Content-Length: {}B", imageResponse.getStatusLine(),
                                imageResponse.getFirstHeader(HttpHeaders.CONTENT_LENGTH));
                        if(imageResponse.getStatusLine().getStatusCode() != 200) {
                            throw new IOException("Http Request error: " + searchResponse);
                        }
                        log.info("正在添加作品...");
                        try(InputStream imageInputStream = imageResponse.getEntity().getContent()) {
                            putIllust(illustId, pagesCount, pageIndex, title, desc, userId, fileExtName, tagsArr, imageInputStream);
                        }
                        log.info("添加作品成功!");
                        totalCount++;
                    }
                } catch(Exception e) {
                    log.error("获取作品时发生异常.(IllustId: {})\n{}", illustId, Throwables.getStackTraceAsString(e));
                }
            }
        }
        return totalCount;
    }

}
