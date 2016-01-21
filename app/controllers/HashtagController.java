package controllers;

import java.util.List;

import models.Category;
import models.Hashtag;
import models.Post;
import models.User;
import handler.FeedHandler;
import common.model.FeedFilter;
import common.model.FeedFilter.FeedType;
import com.google.inject.Inject;
import common.cache.CalcServer;

import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import viewmodel.CategoryVM;
import viewmodel.PostVMLite;

public class HashtagController extends Controller {
	@Inject
	FeedHandler feedHandler;
	private static play.api.Logger logger = play.api.Logger.apply(HashtagController.class);

	@Transactional 
	public Result getHashtagPriceHighLowFeed(Long id, String postType, Long offset) {
		final User localUser = Application.getLocalUser(session());
		List<PostVMLite> vms = feedHandler.getFeedPosts(id, offset, localUser, FeedType.HASHTAG_PRICE_HIGH_LOW);
		return ok(Json.toJson(vms));
	}
	@Transactional 
	public Result getHashtagPriceLowHighFeed(Long id, String postType, Long offset) {
		final User localUser = Application.getLocalUser(session());
		List<PostVMLite> vms = feedHandler.getFeedPosts(id, offset, localUser, FeedType.HASHTAG_PRICE_LOW_HIGH);
		return ok(Json.toJson(vms));
	}
	@Transactional 
	public Result getHashtagPopularFeed(Long id, String postType, Long offset) {
		final User localUser = Application.getLocalUser(session());
		List<PostVMLite> vms = feedHandler.getFeedPosts(id, offset, localUser, FeedType.HASHTAG_POPULAR);
		return ok(Json.toJson(vms));
	}
	@Transactional 
	public Result getHashtagNewestFeed(Long id, String postType, Long offset) {
		final User localUser = Application.getLocalUser(session());
		List<PostVMLite> vms = feedHandler.getFeedPosts(id, offset, localUser, FeedType.HASHTAG_NEWEST);
		return ok(Json.toJson(vms));
	}


	@Transactional
	public static Result putHashtag(Long id){
		Hashtag hashtag = Hashtag.findById(id);
		if (hashtag == null) {
			logger.underlyingLogger().warn(String.format("[hash=%d] Hashtag not found", id));
			return notFound();
		}
		CalcServer.instance().addToHashTagQueues(hashtag);
		return ok();
	}
	 
}