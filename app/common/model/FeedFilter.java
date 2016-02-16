package common.model;

public class FeedFilter {

    public enum FeedType {
        HOME_EXPLORE,
        HOME_FOLLOWING,
        CATEGORY_POPULAR,
        CATEGORY_POPULAR_NEW,
        CATEGORY_POPULAR_USED,
        CATEGORY_NEWEST,
        CATEGORY_PRICE_LOW_HIGH,
        CATEGORY_PRICE_HIGH_LOW,
        HASHTAG_POPULAR,
        HASHTAG_POPULAR_NEW,
        HASHTAG_POPULAR_USED,
        HASHTAG_NEWEST,
        HASHTAG_PRICE_LOW_HIGH,
        HASHTAG_PRICE_HIGH_LOW,
        USER_POSTED,
        USER_LIKED,
        USER_FOLLOWINGS,
        USER_FOLLOWERS,
        PRODUCT_LIKES,
        PRODUCT_SUGGEST,
        RECOMMENDED_SELLERS,
        USER_RECOMMENDED_SELLERS
    }

    public enum ConditionType {
        ALL,
        NEW,
        USED
    }

    public FeedType feedType;
    public ConditionType conditionType;
    public Long objId;

    public FeedFilter(FeedType feedType) {
        this(feedType, ConditionType.ALL, -1L);
    }

    public FeedFilter(FeedType feedType, ConditionType conditionType) {
        this(feedType, conditionType, -1L);
    }

    public FeedFilter(FeedType feedType, Long objId) {
        this(feedType, ConditionType.ALL, objId);
    }

    public FeedFilter(FeedType feedType, ConditionType conditionType, Long objId) {
        this.feedType = feedType;
        this.conditionType = conditionType;
        this.objId = objId;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null)
            return false;

        if (other == this)
            return true;

        if (!(other instanceof FeedFilter))
            return false;

        FeedFilter o = (FeedFilter) other;
        return this.feedType.equals(o.feedType) &&
                this.conditionType.equals(o.conditionType) &&
                this.objId.equals(o.objId);
    }

    @Override
    public String toString() {
        return "feedType=" + (feedType == null? "" : feedType.name()) +
                "\nconditionType=" + (conditionType == null? "" : conditionType.name()) +
                "\nobjId=" + objId;
    }
}