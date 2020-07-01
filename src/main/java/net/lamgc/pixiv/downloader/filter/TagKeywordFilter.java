package net.lamgc.pixiv.downloader.filter;

public class TagKeywordFilter implements IllustFilter {

    private final String targetTag;

    private final boolean ignoreCase;

    private final boolean fullMatch;

    public TagKeywordFilter(String targetTag, boolean ignoreCase, boolean fullMatch) {
        this.targetTag = targetTag;
        this.ignoreCase = ignoreCase;
        this.fullMatch = fullMatch;
    }

    @Override
    public boolean filterIllust(int id, int pageCount, String title, String description, int userId, String fileExtName, String[] tags) {
        for (String tag : tags) {
            if(fullMatch ?
                    ignoreCase ? tag.equalsIgnoreCase(targetTag) : tag.equals(targetTag) :
                    tag.contains(targetTag)
            ) {
                return true;
            }
        }
         return false;
    }

}
