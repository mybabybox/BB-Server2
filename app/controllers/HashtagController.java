package controllers;

import java.util.List;

import models.User;
import handler.FeedHandler;
import common.model.FeedFilter;
import common.model.FeedFilter.FeedType;

import com.google.inject.Inject;

import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import viewmodel.PostVMLite;

public class HashtagController extends Controller {
    private static play.api.Logger logger = play.api.Logger.apply(HashtagController.class);
    
    @Inject
    FeedHandler feedHandler;

    @Transactional 
    public Result getHashtagPopularFeed(Long id, String conditionType, Long offset) {
        final User localUser = Application.getLocalUser(session());
        FeedFilter.ConditionType condition = FeedFilter.ConditionType.ALL;
        try {
            condition = FeedFilter.ConditionType.valueOf(conditionType);
        } catch (Exception e) {
            ;
        }
        List<PostVMLite> vms = null;
        if (FeedFilter.ConditionType.ALL.equals(condition)) {
            vms = feedHandler.getFeedPosts(id, offset, localUser, FeedType.HASHTAG_POPULAR);
        } else if (FeedFilter.ConditionType.NEW.equals(condition)) {
            vms = feedHandler.getFeedPosts(id, offset, localUser, FeedType.HASHTAG_POPULAR_NEW);
        } else if (FeedFilter.ConditionType.USED.equals(condition)) {
            vms = feedHandler.getFeedPosts(id, offset, localUser, FeedType.HASHTAG_POPULAR_USED);
        }
        return ok(Json.toJson(vms));
    }
	
    @Transactional 
    public Result getHashtagNewestFeed(Long id, String conditionType, Long offset) {
        final User localUser = Application.getLocalUser(session());
        List<PostVMLite> vms = feedHandler.getFeedPosts(id, offset, localUser, FeedType.HASHTAG_NEWEST);
        return ok(Json.toJson(vms));
    }
	
    @Transactional 
    public Result getHashtagPriceLowHighFeed(Long id, String conditionType, Long offset) {
        final User localUser = Application.getLocalUser(session());
        List<PostVMLite> vms = feedHandler.getFeedPosts(id, offset, localUser, FeedType.HASHTAG_PRICE_LOW_HIGH);
        return ok(Json.toJson(vms));
    }
	
	@Transactional 
	public Result getHashtagPriceHighLowFeed(Long id, String conditionType, Long offset) {
	    final User localUser = Application.getLocalUser(session());
	    List<PostVMLite> vms = feedHandler.getFeedPosts(id, offset, localUser, FeedType.HASHTAG_PRICE_HIGH_LOW);
	    return ok(Json.toJson(vms));
	}
}