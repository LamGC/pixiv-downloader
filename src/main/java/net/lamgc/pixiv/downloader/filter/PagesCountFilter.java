package net.lamgc.pixiv.downloader.filter;

public class PagesCountFilter implements IllustFilter {

    private final int maxPages;
    private final int minPages;

    /**
     *
     * @param maxPages 最大页面数, {@code pages < maxPages}
     * @param minPages 最小页面数, {@code pages >= minPages}
     */
    public PagesCountFilter(int minPages, int maxPages) {
        this.maxPages = maxPages;
        this.minPages = minPages;
    }

    @Override
    public boolean filterIllust(int id, int pageCount, String title, String description, int userId, String fileExtName, String[] tags) {
        return min(pageCount) && max(pageCount);
    }

    private boolean max(int pagesCount) {
        return maxPages <= 0 || pagesCount < maxPages;
    }

    private boolean min(int pagesCount) {
        return minPages <= 0 || pagesCount >= minPages;
    }

}
