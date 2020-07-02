package net.lamgc.pixiv.downloader.filter;

import java.util.regex.Pattern;

/**
 * Tag关键字过滤器.
 */
public class TagKeywordFilter implements IllustFilter {

    private final boolean useRegex;

    private final String targetTag;

    private final boolean ignoreCase;

    private final boolean fullMatch;

    private final Pattern matchPattern;

    /**
     * 使用关键词构造Tag过滤器
     * @param targetTag 目标关键词
     * @param ignoreCase 是否忽略大小写
     * @param fullMatch 是否完全匹配
     */
    public TagKeywordFilter(String targetTag, boolean ignoreCase, boolean fullMatch) {
        this.useRegex = false;
        this.ignoreCase = ignoreCase;
        this.targetTag = ignoreCase ? targetTag.toLowerCase() : targetTag;
        this.fullMatch = fullMatch;
        this.matchPattern = null;
    }

    /**
     * 使用正则表达式构建Tag过滤器
     * @param matchPattern 正则表达式对象
     */
    public TagKeywordFilter(Pattern matchPattern) {
        this.useRegex = true;
        this.ignoreCase = false;
        this.targetTag = null;
        this.fullMatch = false;
        this.matchPattern = matchPattern;
    }

    @Override
    public boolean filterIllust(int id, int pageCount, String title, String description, int userId, String fileExtName, String[] tags) {
        return useRegex ? regexMatch(tags) : container(tags);
    }

    private boolean regexMatch(String[] tags) {
        for(String tag : tags) {
            if(matchPattern.matcher(tag).matches()) {
                return true;
            }
        }
        return false;
    }

    private boolean container(String[] tags) {
        for (String tag : tags) {
            if(fullMatch ?
                    ( ignoreCase ? tag.equalsIgnoreCase(targetTag) : tag.equals(targetTag) ) :
                    ( ignoreCase ? tag.toLowerCase().contains(targetTag) : tag.contains(targetTag) )
            ) {
                return true;
            }
        }
        return false;
    }

}
