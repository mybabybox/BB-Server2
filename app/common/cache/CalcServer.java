package common.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import models.Category;
import models.FollowSocialRelation;
import models.Hashtag;
import models.LikeSocialRelation;
import models.Post;
import models.SocialRelation;
import models.User;

import org.joda.time.DateTime;

import play.Play;
import play.db.jpa.JPA;

import com.google.inject.Singleton;

import common.model.FeedFilter.FeedType;
import common.schedule.JobScheduler;
import common.thread.ThreadLocalOverride;
import common.thread.TransactionalRunnableTask;
import common.utils.NanoSecondStopWatch;
import domain.DefaultValues;

@Singleton
public class CalcServer {
	private play.api.Logger logger = play.api.Logger.apply(CalcServer.class);
	
	@Inject
    JedisCache jedisCache;
    
	public static final Boolean FEED_INIT_FLUSH_ALL = Play.application().configuration().getBoolean("feed.init.flush.all", true);
	public static final Long FEED_SCORE_COMPUTE_SCHEDULE = Play.application().configuration().getLong("feed.score.compute.schedule");
	public static final Long FEED_SCORE_HIGH_BASE = Play.application().configuration().getLong("feed.score.high.base");
	public static final Long FEED_HOME_COUNT_MAX = Play.application().configuration().getLong("feed.home.count.max");
	public static final int FEED_SNAPSHOT_EXPIRY_SECS = Play.application().configuration().getInt("feed.snapshot.expiry.secs");
	public static final int FEED_SNAPSHOT_LONG_EXPIRY_SECS = Play.application().configuration().getInt("feed.snapshot.long.expiry.secs");
	public static final int FEED_SOLD_CLEANUP_DAYS = Play.application().configuration().getInt("feed.sold.cleanup.days");
	public static final int FEED_RETRIEVAL_COUNT = DefaultValues.FEED_INFINITE_SCROLL_COUNT;
	
	public static final String CACHED_USERS = "CACHED_USERS";
	public static final String CACHED_PRODUCTS = "CACHED_PRODUCTS";
	
	private CalcFormula formula = new CalcFormula();
	
	private static CalcServer instance;
    
    public static CalcServer instance() {
        if (instance == null) {
            instance = play.Play.application().injector().instanceOf(CalcServer.class);
        }
        return instance; 
    }
    
	public void warmUpActivity() {
		NanoSecondStopWatch sw = new NanoSecondStopWatch();
		logger.underlyingLogger().debug("warmUpActivity starts");
		
		if (FEED_INIT_FLUSH_ALL) {
		    jedisCache.flushAll();
		}
		
		clearStaticQueues();
		//buildQueuesFromUsers();
		buildQueuesFromPosts();
		
		JobScheduler.getInstance().schedule(
		        "buildCategoryPopularQueue", 
		        FEED_SCORE_COMPUTE_SCHEDULE,  // initial delay 
		        FEED_SCORE_COMPUTE_SCHEDULE,  // interval
		        TimeUnit.HOURS,
				new Runnable() {
					public void run() {
					    try {
    						JPA.withTransaction(new play.libs.F.Callback0() {
    							@Override
    							public void invoke() throws Throwable {
    								buildCategoryPopularQueues();
    							}
    						});
					    } catch (Exception e) {
	                        logger.underlyingLogger().error("[JobScheduler] buildCategoryPopularQueues failed...", e);
	                    }
					}
				});
        
		sw.stop();
		logger.underlyingLogger().debug("warmUpActivity completed. Took "+sw.getElapsedSecs()+"s");
	}
	
	private void clearStaticQueues() {
	    jedisCache.remove(CACHED_USERS);
	    jedisCache.remove(CACHED_PRODUCTS);
	}
	
	public void clearCategoryQueues() {
		for(Category category : Category.getAllCategories()){
		    jedisCache.remove(getKey(FeedType.CATEGORY_POPULAR,category.id));
		    jedisCache.remove(getKey(FeedType.CATEGORY_POPULAR_NEW,category.id));
		    jedisCache.remove(getKey(FeedType.CATEGORY_POPULAR_USED,category.id));
		    jedisCache.remove(getKey(FeedType.CATEGORY_NEWEST,category.id));
		    jedisCache.remove(getKey(FeedType.CATEGORY_PRICE_LOW_HIGH,category.id));
		}
	}
	
	public void clearHashtagQueues() {
        for(Hashtag hashtag : Hashtag.getAllHashtags()){
            jedisCache.remove(getKey(FeedType.HASHTAG_POPULAR,hashtag.id));
            jedisCache.remove(getKey(FeedType.HASHTAG_POPULAR_NEW,hashtag.id));
            jedisCache.remove(getKey(FeedType.HASHTAG_POPULAR_USED,hashtag.id));
            jedisCache.remove(getKey(FeedType.HASHTAG_NEWEST,hashtag.id));
            jedisCache.remove(getKey(FeedType.HASHTAG_PRICE_LOW_HIGH,hashtag.id));
        }
	}
	
	public void clearUserQueues(User user) {
		jedisCache.remove(getKey(FeedType.USER_POSTED,user.id));
		jedisCache.remove(getKey(FeedType.USER_LIKED,user.id));
		jedisCache.remove(getKey(FeedType.USER_FOLLOWINGS,user.id));
		jedisCache.remove(getKey(FeedType.USER_FOLLOWERS,user.id));
		
		// only if we loop thru all users to build queues
		// dont clear USER_FOLLOWERS queue, it is created by USER_FOLLOWINGS indirectly 
        //jedisCache.remove(getKey(FeedType.USER_FOLLOWERS,user.id));
	}
	
	public void clearPostQueues(Post post) {
		jedisCache.remove(getKey(FeedType.PRODUCT_LIKES,post.id));
	}
	
	/**
	 * Static caches 
	 */
	
	public boolean isUserCached(User user) {
        return jedisCache.isMemberOfSortedSet(CACHED_USERS, user.id.toString());
    }
   
	public void addToCachedUsersQueue(User user) {
        if (!isUserCached(user)) {
            jedisCache.putToSortedSet(CACHED_USERS, new Date().getTime(), user.id.toString());
        }
    }
    
    public void removeFromCachedUsersQueue(User user) {
        jedisCache.removeMemberFromSortedSet(CACHED_USERS, user.id.toString());
    }
    
    public boolean isProductCached(Post post) {
        return jedisCache.isMemberOfSortedSet(CACHED_PRODUCTS, post.id.toString());
    }
   
    public void addToCachedProductsQueue(Post post) {
        if (!isProductCached(post)) {
            jedisCache.putToSortedSet(CACHED_PRODUCTS, new Date().getTime(), post.id.toString());
        }
    }
    
    public void removeFromCachedProductsQueue(Post post) {
        jedisCache.removeMemberFromSortedSet(CACHED_PRODUCTS, post.id.toString());
    }
    
	/**
	 * Main entry for building queues from users.
	 */
	private void buildQueuesFromUsers() {
		for(User user : User.getEligibleUsersForFeed()){
		    if (!FEED_INIT_FLUSH_ALL) {
		        clearUserQueues(user);
		    }
		    
		    buildQueuesForUser(user);
		}
	}
	
	private boolean eligibleToBuildQueues(User user) {
	    if (user.newUser || !user.active || user.deleted) {
            return false;
        }
	    
	    if (user.numFollowers == 0 && user.numFollowings == 0 && 
	            user.numLikes == 0 && user.numProducts == 0 && user.numStories == 0) {
	        return false;
	    }
	    
	    return true;
	}
	
	public void buildQueuesForUserAsync(final User user) {
	    JobScheduler.getInstance().run(
                new TransactionalRunnableTask() {
                    @Override
                    public void execute() {
                        buildQueuesForUserAsync(user);
                    }
                });
	}
	
	public void buildQueuesForUser(User user) {
	    if (isUserCached(user) || !eligibleToBuildQueues(user)) {
	        return;
	    }
	    
	    buildUserLikedQueue(user);
        buildUserFollowingsFollowersQueue(user);
        
        // mark cached
        addToCachedUsersQueue(user);
	}

	private void buildUserLikedQueue(User user) {
		NanoSecondStopWatch sw = new NanoSecondStopWatch();
		logger.underlyingLogger().debug("buildUserLikedPostQueue starts - u="+user.id);
		
		for (SocialRelation socialRelation : LikeSocialRelation.getUserLikedPosts(user.id)) {
			jedisCache.putToSortedSet(getKey(FeedType.USER_LIKED,user.id), socialRelation.getCreatedDate().getTime(), socialRelation.target.toString());
		}
		
		sw.stop();
		logger.underlyingLogger().debug("buildUserLikedPostQueue completed. Took "+sw.getElapsedSecs()+"s");
	}

	private void buildUserFollowingsFollowersQueue(User user) {
		NanoSecondStopWatch sw = new NanoSecondStopWatch();
		logger.underlyingLogger().debug("buildUserFollowingsQueue starts - u="+user.id);
		
		/*
		// only if we loop thru all users to build queues 
		for (SocialRelation socialRelation : FollowSocialRelation.getUserFollowings(user.id)) {
            // USER_FOLLOWINGS:actor adds target
            jedisCache.putToSortedSet(getKey(FeedType.USER_FOLLOWINGS,socialRelation.actor), socialRelation.getCreatedDate().getTime(), socialRelation.target.toString());
            // USER_FOLLOWERS:target adds actor  
            jedisCache.putToSortedSet(getKey(FeedType.USER_FOLLOWERS,socialRelation.target), socialRelation.getCreatedDate().getTime(), socialRelation.actor.toString());
        }
        */
		
		for (SocialRelation socialRelation : FollowSocialRelation.getUserFollowings(user.id)) {
		    // USER_FOLLOWINGS:actor adds target
			jedisCache.putToSortedSet(getKey(FeedType.USER_FOLLOWINGS,socialRelation.actor), socialRelation.getCreatedDate().getTime(), socialRelation.target.toString());
		}
		
		for (SocialRelation socialRelation : FollowSocialRelation.getUserFollowers(user.id)) {
            // USER_FOLLOWERS:target adds actor  
            jedisCache.putToSortedSet(getKey(FeedType.USER_FOLLOWERS,socialRelation.target), socialRelation.getCreatedDate().getTime(), socialRelation.actor.toString());
        }
		
		sw.stop();
		logger.underlyingLogger().debug("buildUserFollowingsQueue completed. Took "+sw.getElapsedSecs()+"s");
	}
	
    private void addToHashtagPopularQueue(Hashtag hashtag, Post post) {
        if (post.soldMarked) {
            return;
        }
        if(post.hasHashtag(hashtag)) {
            Double timeScore = calculateTimeScore(post, true);
            jedisCache.putToSortedSet(getKey(FeedType.HASHTAG_POPULAR,hashtag.id),  timeScore.doubleValue() * FEED_SCORE_HIGH_BASE, post.id.toString());
            if (post.isNewCondition()) {
                jedisCache.putToSortedSet(getKey(FeedType.HASHTAG_POPULAR_NEW,hashtag.id),  timeScore.doubleValue() * FEED_SCORE_HIGH_BASE, post.id.toString());    
            } else {
                jedisCache.putToSortedSet(getKey(FeedType.HASHTAG_POPULAR_USED,hashtag.id),  timeScore.doubleValue() * FEED_SCORE_HIGH_BASE, post.id.toString());
            }
        }
    }

    private void addToHashtagNewestQueue(Hashtag hashtag, Post post) {
        if (post.soldMarked) {
            return;
        }
        if(post.hasHashtag(hashtag)) {
            jedisCache.putToSortedSet(getKey(FeedType.HASHTAG_NEWEST,hashtag.id), post.getCreatedDate().getTime(), post.id.toString());
        }
    }
    
	private void addToHashtagPriceLowHighQueue(Hashtag hashtag, Post post) {
	    if (post.soldMarked) {
            return;
	    }
	    if(post.hasHashtag(hashtag)) {
	        jedisCache.putToSortedSet(getKey(FeedType.HASHTAG_PRICE_LOW_HIGH, hashtag.id), post.price * FEED_SCORE_HIGH_BASE + post.id , post.id.toString());
	    }
	}
	
	public void addToRecommendedSellersQueue(User user) {
	    boolean added = jedisCache.isMemberOfSortedSet(getKey(FeedType.RECOMMENDED_SELLERS), user.id.toString());
        if (user.isRecommendedSeller() && !added) {
            jedisCache.putToSortedSet(getKey(FeedType.RECOMMENDED_SELLERS), user.getLastLogin().getTime(), user.id.toString());
        }
    }
	
	public void removeFromRecommendedSellersQueue(User user) {
        jedisCache.removeMemberFromSortedSet(getKey(FeedType.RECOMMENDED_SELLERS), user.id.toString());
    }
	
	/**
     * Main entry for building queues from posts.
     */
	private void buildQueuesFromPosts() {
	    NanoSecondStopWatch sw = new NanoSecondStopWatch();
        logger.underlyingLogger().debug("buildQueuesFromPosts starts");
        
        if (!FEED_INIT_FLUSH_ALL) {
            clearCategoryQueues();
            clearHashtagQueues();
        }

		for (Post post : Post.getEligiblePostsForFeeds()) {
		    if (!FEED_INIT_FLUSH_ALL) {
		        clearPostQueues(post);
		    }
		    
			buildQueuesForPost(post);
		}
		
		sw.stop();
        logger.underlyingLogger().debug("buildQueuesFromPosts completed. Took "+sw.getElapsedSecs()+"s");
	}

	private boolean eligibleToBuildQueues(Post post) {
        if (post.deleted) {
            return false;
        }
        
        return true;
    }
	
	public void buildQueuesForPost(Post post) {
	    if (isProductCached(post) || !eligibleToBuildQueues(post)) {
	        return;
	    }
	    
	    addToUserPostedQueue(post);
	    addToRecommendedSellersQueue(post.owner);
        
        // below queues skip sold products
        if (post.soldMarked) {
            return;
        }
        
        addToCategoryQueues(post);
        addToHashtagQueues(post);
        buildProductLikesQueue(post);
        
	    // mark cached
        addToCachedProductsQueue(post);
	}
	
	private void buildCategoryPopularQueues() {
	    NanoSecondStopWatch sw = new NanoSecondStopWatch();
        logger.underlyingLogger().debug("buildCategoryPopularQueue starts");
        
		for (Post post : Post.getEligiblePostsForFeeds()) {
		    if (post.soldMarked) {
                continue;
            }
			addToCategoryPopularQueue(post);
		}
		
		sw.stop();
        logger.underlyingLogger().debug("buildCategoryPopularQueue completed. Took "+sw.getElapsedSecs()+"s");
	}
	
	private void buildProductLikesQueue(Post post) {
		NanoSecondStopWatch sw = new NanoSecondStopWatch();
		logger.underlyingLogger().debug("buildProductLikesQueue starts - p="+post.id);
		
		for (SocialRelation socialRelation : LikeSocialRelation.getPostLikedUsers(post.id)) {
			jedisCache.putToSortedSet(getKey(FeedType.PRODUCT_LIKES,post.id), socialRelation.getCreatedDate().getTime(), socialRelation.actor.toString());
		}
		
		sw.stop();
		logger.underlyingLogger().debug("buildProductLikesQueue completed. Took "+sw.getElapsedSecs()+"s");
	}
	
	public void addToHashtagQueues(Post post){
		NanoSecondStopWatch sw = new NanoSecondStopWatch();
		logger.underlyingLogger().debug("addToHashtagQueues starts - p="+post.id);
		
		for (Hashtag hashtag: Hashtag.getAllHashtags()){
			addToHashtagPriceLowHighQueue(hashtag, post);
			addToHashtagNewestQueue(hashtag, post);
			addToHashtagPopularQueue(hashtag, post);
		}
		
		sw.stop();
		logger.underlyingLogger().debug("addToHashtagQueues completed. Took "+sw.getElapsedSecs()+"s");
	}

    public void removeFromHashtagQueues(Post post){
        for (Hashtag hashtag: Hashtag.getAllHashtags()){
            removeFromHashtagQueues(post, hashtag);    
        }
    }
    
    public void removeFromHashtagQueues(Post post, Hashtag hashtag){
        jedisCache.removeMemberFromSortedSet(getKey(FeedType.HASHTAG_POPULAR,hashtag.id), post.id.toString());
        jedisCache.removeMemberFromSortedSet(getKey(FeedType.HASHTAG_NEWEST,hashtag.id), post.id.toString());
        jedisCache.removeMemberFromSortedSet(getKey(FeedType.HASHTAG_PRICE_LOW_HIGH,hashtag.id), post.id.toString());
    
        // no need to check post condition type, try to clear from both queues
        jedisCache.removeMemberFromSortedSet(getKey(FeedType.HASHTAG_POPULAR_NEW,hashtag.id), post.id.toString());
        jedisCache.removeMemberFromSortedSet(getKey(FeedType.HASHTAG_POPULAR_USED,hashtag.id), post.id.toString());
    }
    
	public void recalcScoreAndAddToCategoryPopularQueue(Post post) {
	    addToCategoryPopularQueue(post);
	}
	
	public void addToCategoryQueues(Post post) {
        addToCategoryPopularQueue(post);
        addToCategoryNewestQueue(post);
        addToCategoryPriceLowHighQueue(post);
    }
	
	private void addToCategoryPopularQueue(Post post) {
        if (post.soldMarked) {
            return;
        }
        Double timeScore = calculateTimeScore(post, true);
        jedisCache.putToSortedSet(getKey(FeedType.CATEGORY_POPULAR,post.category.id),  timeScore.doubleValue() * FEED_SCORE_HIGH_BASE, post.id.toString());
        if (post.isNewCondition()) {
            jedisCache.putToSortedSet(getKey(FeedType.CATEGORY_POPULAR_NEW,post.category.id),  timeScore.doubleValue() * FEED_SCORE_HIGH_BASE, post.id.toString());    
        } else {
            jedisCache.putToSortedSet(getKey(FeedType.CATEGORY_POPULAR_USED,post.category.id),  timeScore.doubleValue() * FEED_SCORE_HIGH_BASE, post.id.toString());
        }
    }
    
    private void addToCategoryNewestQueue(Post post) {
        if (post.soldMarked) {
            return;
        }
    	jedisCache.putToSortedSet(getKey(FeedType.CATEGORY_NEWEST,post.category.id), post.getCreatedDate().getTime(), post.id.toString());
    }
    
    private void addToCategoryPriceLowHighQueue(Post post) {
        if (post.soldMarked) {
            return;
        }
    	jedisCache.putToSortedSet(getKey(FeedType.CATEGORY_PRICE_LOW_HIGH,post.category.id), post.price * FEED_SCORE_HIGH_BASE + post.id , post.id.toString());
    }
    
    public void removeFromCategoryQueues(Post post){
        removeFromCategoryQueues(post, post.category);
    }
        
    public void removeFromCategoryQueues(Post post, Category category){
        jedisCache.removeMemberFromSortedSet(getKey(FeedType.CATEGORY_POPULAR,category.id), post.id.toString());
        jedisCache.removeMemberFromSortedSet(getKey(FeedType.CATEGORY_NEWEST,category.id), post.id.toString());
        jedisCache.removeMemberFromSortedSet(getKey(FeedType.CATEGORY_PRICE_LOW_HIGH,category.id), post.id.toString());
    
        // no need to check post condition type, try to clear from both queues
        jedisCache.removeMemberFromSortedSet(getKey(FeedType.CATEGORY_POPULAR_NEW,category.id), post.id.toString());
        jedisCache.removeMemberFromSortedSet(getKey(FeedType.CATEGORY_POPULAR_USED,category.id), post.id.toString());
    }
    
    public void addToUserLikedQueue(Post post, User user){
        jedisCache.putToSortedSet(getKey(FeedType.USER_LIKED,user.id), new Date().getTime(), post.id.toString());
    }
    
    public void removeFromUserLikedQueue(Post post, User user){
        jedisCache.removeMemberFromSortedSet(getKey(FeedType.USER_LIKED,user.id), post.id.toString());
    }
    
    public void addToUserFollowingsFollowersQueue(Long userId, Long followingUserId, Double score){
        jedisCache.remove(getKey(FeedType.HOME_FOLLOWING,userId));
        jedisCache.putToSortedSet(getKey(FeedType.USER_FOLLOWINGS,userId), score, followingUserId.toString());
        jedisCache.putToSortedSet(getKey(FeedType.USER_FOLLOWERS,followingUserId), score, userId.toString());
    }
    
    public void removeFromUserFollowingsFollowersQueue(Long userId, Long followingUserId){
        jedisCache.remove(getKey(FeedType.HOME_FOLLOWING,userId));
        jedisCache.removeMemberFromSortedSet(getKey(FeedType.USER_FOLLOWINGS,userId), followingUserId.toString());
        jedisCache.removeMemberFromSortedSet(getKey(FeedType.USER_FOLLOWERS,followingUserId), userId.toString());
    }
    
    public void addToUserPostedQueue(Post post) {
        jedisCache.putToSortedSet(getKey(FeedType.USER_POSTED,post.owner.id), post.getCreatedDate().getTime(), post.id.toString());
    }
    
    public void removeFromUserPostedQueue(Post post, User user){
        jedisCache.removeMemberFromSortedSet(getKey(FeedType.USER_POSTED,user.id), post.id.toString());
    }
    
    public void removeFromAllUsersLikedQueues(Post post) {
        NanoSecondStopWatch sw = new NanoSecondStopWatch();
        logger.underlyingLogger().debug("removeFromAllUsersLikedQueues starts - p="+post.id);
        
        for (SocialRelation socialRelation : LikeSocialRelation.getPostLikedUsers(post.id)) {
            jedisCache.removeMemberFromSortedSet(getKey(FeedType.USER_LIKED,socialRelation.actor), post.id.toString());
        }
        
        sw.stop();
        logger.underlyingLogger().debug("removeFromAllUsersLikedQueues completed. Took "+sw.getElapsedSecs()+"s");
    }
    
    public void buildUserExploreFeedQueueIfNotExistAsync(final User user) {
        if(!jedisCache.exists(getKey(FeedType.HOME_EXPLORE,user.id))){
            JobScheduler.getInstance().run(
                    new TransactionalRunnableTask() {
                        @Override
                        public void execute() {
                            buildUserExploreFeedQueueIfNotExist(user.id);
                        }
                    });
        }
    }
    
    public void buildUserExploreFeedQueueIfNotExist(Long id) {
        if(!jedisCache.exists(getKey(FeedType.HOME_EXPLORE,id))){
            buildUserExploreFeedQueue(id);
        } 
    }
    
	private void buildUserExploreFeedQueue(Long id) {
	    NanoSecondStopWatch sw = new NanoSecondStopWatch();
		logger.underlyingLogger().debug("buildUserExploreFeedQueue starts - u="+id);
		
		User user = User.findById(id);
		Map<Long, Integer> map = new HashMap<>();
		if (user != null) {
			map = user.getUserCategoriesRatioForFeed();
		}
		
		for (Category category : Category.getAllCategories()) {
		    int percentage = category.minPercentFeedExposure;
            Integer catViewPercentage = map.get(category.getId());
            if (catViewPercentage != null && catViewPercentage > percentage) {
                percentage = catViewPercentage;
            }
            if (percentage > category.maxPercentFeedExposure) {
                percentage = category.maxPercentFeedExposure;
            }

		    Long catPostSize = FEED_HOME_COUNT_MAX * percentage / 100;
			Set<String> values = jedisCache.getSortedSetDsc(getKey(FeedType.CATEGORY_POPULAR,category.id), 0L, catPostSize - 1);
			
			List<Long> catPostIds = new ArrayList<>();
			for (String value : values) {
				try {
				    catPostIds.add(Long.parseLong(value));
				} catch (Exception e) {
				}
			}
			
			logger.underlyingLogger().debug(
                    String.format("[cat=%d name=%s minPercent=%d maxPercent=%d percent=%d catPostSize=%d catPostIds.size=%d]", 
                            category.id, category.name, category.minPercentFeedExposure, category.maxPercentFeedExposure, 
                            percentage, catPostSize, catPostIds.size()));
			
			for (Long postId : catPostIds) {
			    Post post = Post.findById(postId);
			    if (post != null && post.deleted) {
			        continue;
			    }
			    Double randomizedScore = formula.randomizeScore(post);
			    if (randomizedScore > 0) {
			        jedisCache.putToSortedSet(getKey(FeedType.HOME_EXPLORE, id), randomizedScore * FEED_SCORE_HIGH_BASE, postId.toString());
			    }
			}
		}
		
		sw.stop();
		logger.underlyingLogger().debug("buildUserExploreFeedQueue completed. Took "+sw.getElapsedSecs()+"s");
	}
	
	public void buildHomeFollowingFeedQueueIfNotExist(Long id) {
	    if(!jedisCache.exists(getKey(FeedType.HOME_FOLLOWING,id))){
	        buildHomeFollowingFeedQueue(id);
        }
	}
	
	private void buildHomeFollowingFeedQueue(Long id) {
		NanoSecondStopWatch sw = new NanoSecondStopWatch();
		logger.underlyingLogger().debug("buildHomeFollowingQueue starts - u="+id);
		
		List<Long> followings = getUserFollowingsFeed(id);
		for (Long followingUser : followings){
			Set<String> values = jedisCache.getSortedSetDsc(getKey(FeedType.USER_POSTED,followingUser));
			for (String value : values) {
				try {
					Long postId = Long.parseLong(value);
					jedisCache.putToSortedSet(getKey(FeedType.HOME_FOLLOWING,id), getScore(getKey(FeedType.USER_POSTED, followingUser), postId), postId.toString());
				} catch (Exception e) {
				}
			}
		}
		jedisCache.expire(getKey(FeedType.HOME_FOLLOWING,id), FEED_SNAPSHOT_EXPIRY_SECS);
		
		sw.stop();
		logger.underlyingLogger().debug("buildHomeFollowingQueue completed. Took "+sw.getElapsedSecs()+"s");
	}
	
	public void buildUserRecommendedSellersFeedQueueIfNotExist(Long id) {
	    if (!jedisCache.exists(getKey(FeedType.USER_RECOMMENDED_SELLERS,id))) {
            buildUserRecommendedSellersFeedQueue(id);
        }
	}
	
	private void buildUserRecommendedSellersFeedQueue(Long id) {
        NanoSecondStopWatch sw = new NanoSecondStopWatch();
        logger.underlyingLogger().debug("buildUserRecommendedSellersFeedQueue starts - u="+id);
        
        // randomize RECOMMENDED_SELLERS queue
        Set<String> values = jedisCache.getSortedSetDsc(getKey(FeedType.RECOMMENDED_SELLERS));
        for (String value : values) {
            try {
                Long sellerId = Long.parseLong(value);
                jedisCache.putToSortedSet(getKey(FeedType.USER_RECOMMENDED_SELLERS, id), Math.random() * FEED_SCORE_HIGH_BASE, sellerId.toString());
            } catch (Exception e) {
            }
        }
        
        sw.stop();
        logger.underlyingLogger().debug("buildUserRecommendedSellersFeedQueue completed. Took "+sw.getElapsedSecs()+"s");
    }
	
	public void buildSuggestedProductQueueIfNotExist(Long id) {
	    if(!jedisCache.exists(getKey(FeedType.PRODUCT_SUGGEST, id))){
            buildSuggestedProductQueue(id);
        }    
	}
	
	private void buildSuggestedProductQueue(Long postId) {
		NanoSecondStopWatch sw = new NanoSecondStopWatch();
		logger.underlyingLogger().debug("buildSuggestedProductQueue starts - p="+postId);
		
		List<Long> users = getProductLikesQueue(postId);
		List<Long> suggestedPostIds = new ArrayList<>();
		for (Long userId : users){
			Set<String> values = jedisCache.getSortedSetDsc(getKey(FeedType.USER_LIKED, userId));
			for (String value : values) {
				try {
					Long suggestedPostId = Long.parseLong(value);
					suggestedPostIds.add(suggestedPostId);
				} catch (Exception e) {
				}
			}
		}
		Collections.shuffle(suggestedPostIds);
		suggestedPostIds = suggestedPostIds.subList(0, suggestedPostIds.size() <= 20 ? suggestedPostIds.size() : 20 );
		
		for(Long suggestedPostId : suggestedPostIds){
			jedisCache.putToSortedSet(getKey(FeedType.PRODUCT_SUGGEST, postId), Math.random() * FEED_SCORE_HIGH_BASE, suggestedPostId.toString());
		}
		
		jedisCache.expire(getKey(FeedType.PRODUCT_SUGGEST, postId), FEED_SNAPSHOT_EXPIRY_SECS);
		
		sw.stop();
		logger.underlyingLogger().debug("buildSuggestedProductQueue completed. Took "+sw.getElapsedSecs()+"s");
	}
	
	// helpers
	
	public Long calculateBaseScore(Post post) {
        // skip already calculated posts during server startup
        if (ThreadLocalOverride.isServerStartingUp() && post.baseScore > 0L) {
            return post.baseScore;
        }
        return formula.computeBaseScore(post);
    }
	
	public Double calculateTimeScore(Post post) {
        return calculateTimeScore(post, false);
    }
    
    private Double calculateTimeScore(Post post, boolean recalcBaseScore) {
        if (recalcBaseScore) {
            calculateBaseScore(post);
        }
        return formula.computeTimeScore(post);
    }
    
	public Double getScore(String key, Long postId){
		return jedisCache.getScore(key, postId.toString());
	}
	
	public String getKey(FeedType feedType) {
	    return getKey(feedType, null);
	}
	
	public String getKey(FeedType feedType, Long keyId) {
		// Only 1 queue CATEGORY_PRICE_LOW_HIGH
		if (FeedType.CATEGORY_PRICE_HIGH_LOW.equals(feedType)) {
			feedType = FeedType.CATEGORY_PRICE_LOW_HIGH;
		}
		// Only 1 queue HASHTAG_PRICE_LOW_HIGH
		if (FeedType.HASHTAG_PRICE_HIGH_LOW.equals(feedType)) {
			feedType = FeedType.HASHTAG_PRICE_LOW_HIGH;
		}
		
		if (FeedType.RECOMMENDED_SELLERS.equals(feedType) || keyId == null) {
		    return feedType.toString();
		}
		return feedType+":"+keyId;
	}
	
	public void cleanupSoldPosts() {
	    DateTime daysBefore = (new DateTime()).minusDays(FEED_SOLD_CLEANUP_DAYS);
        List<Post> soldPosts = Post.getUnmarkedSoldPostsAfter(daysBefore.toDate());
        if (soldPosts != null) {
            logger.underlyingLogger().info("cleanupSoldPosts - There are "+soldPosts.size()+" sold posts before "+daysBefore.toString());
            for (Post soldPost : soldPosts) {
                removeFromCategoryQueues(soldPost);
                removeFromHashtagQueues(soldPost);
                soldPost.soldMarked = true;
                logger.underlyingLogger().info("[id="+soldPost.id+"] removed from category and hashtag queues: "+soldPost.title);
            }
        } else {
            logger.underlyingLogger().info("cleanupSoldPosts - There is no sold post before "+daysBefore.toString());
        }
    }
	
	// feeds
	
	public List<Long> getHomeExploreFeed(Long id, Double offset) {
	    buildUserExploreFeedQueueIfNotExist(id);

        Set<String> values = jedisCache.getSortedSetDsc(getKey(FeedType.HOME_EXPLORE,id), offset);
        final List<Long> postIds = new ArrayList<>();
        for (String value : values) {
            try {
                postIds.add(Long.parseLong(value));
            } catch (Exception e) {
            }
        }
        jedisCache.expire(getKey(FeedType.HOME_EXPLORE, id), (id == User.NO_LOGIN_ID) ? FEED_SNAPSHOT_LONG_EXPIRY_SECS : FEED_SNAPSHOT_EXPIRY_SECS);
        return postIds;
    }
    
    public List<Long> getHomeFollowingFeed(Long id, Double offset) {
        buildHomeFollowingFeedQueueIfNotExist(id);

        Set<String> values = jedisCache.getSortedSetDsc(getKey(FeedType.HOME_FOLLOWING,id), offset);
        final List<Long> postIds = new ArrayList<>();
        for (String value : values) {
            try {
                postIds.add(Long.parseLong(value));
            } catch (Exception e) {
            }
        }
        jedisCache.expire(getKey(FeedType.HOME_FOLLOWING,id), FEED_SNAPSHOT_EXPIRY_SECS);
        return postIds;
    }
    
    public List<Long> getCategoryPopularFeed(Long id, Double offset) {
        Set<String> values = jedisCache.getSortedSetDsc(getKey(FeedType.CATEGORY_POPULAR,id), offset);
        final List<Long> postIds = new ArrayList<>();
        for (String value : values) {
            try {
                postIds.add(Long.parseLong(value));
            } catch (Exception e) {
            }
        }
        return postIds;
    }
    
    public List<Long> getCategoryPopularNewFeed(Long id, Double offset) {
        Set<String> values = jedisCache.getSortedSetDsc(getKey(FeedType.CATEGORY_POPULAR_NEW,id), offset);
        final List<Long> postIds = new ArrayList<>();
        for (String value : values) {
            try {
                postIds.add(Long.parseLong(value));
            } catch (Exception e) {
            }
        }
        return postIds;
    }
    
    public List<Long> getCategoryPopularUsedFeed(Long id, Double offset) {
        Set<String> values = jedisCache.getSortedSetDsc(getKey(FeedType.CATEGORY_POPULAR_USED,id), offset);
        final List<Long> postIds = new ArrayList<>();
        for (String value : values) {
            try {
                postIds.add(Long.parseLong(value));
            } catch (Exception e) {
            }
        }
        return postIds;
    }
    
    public List<Long> getCategoryNewestFeed(Long id, Double offset) {
        Set<String> values = jedisCache.getSortedSetDsc(getKey(FeedType.CATEGORY_NEWEST,id), offset);
        final List<Long> postIds = new ArrayList<>();
        for (String value : values) {
            try {
                postIds.add(Long.parseLong(value));
            } catch (Exception e) {
            }
        }
        return postIds;
    }
    
    public List<Long> getCategoryPriceLowHighFeed(Long id, Double offset) {
        Set<String> values = jedisCache.getSortedSetAsc(getKey(FeedType.CATEGORY_PRICE_LOW_HIGH,id), offset);
        final List<Long> postIds = new ArrayList<>();
    
        for (String value : values) {
            try {
                postIds.add(Long.parseLong(value));
            } catch (Exception e) {
            }
        }
        return postIds;
    }
    
    public List<Long> getCategoryPriceHighLowFeed(Long id, Double offset) {
        Set<String> values = jedisCache.getSortedSetDsc(getKey(FeedType.CATEGORY_PRICE_HIGH_LOW,id), offset);
        final List<Long> postIds = new ArrayList<>();
        for (String value : values) {
            try {
                postIds.add(Long.parseLong(value));
            } catch (Exception e) {
            }
        }
        return postIds;
    }
    
    public List<Long> getHashtagPopularFeed(Long id, Double offset) {
        Set<String> values = jedisCache.getSortedSetDsc(getKey(FeedType.HASHTAG_POPULAR,id), offset);
        final List<Long> postIds = new ArrayList<>();
    
        for (String value : values) {
            try {
                postIds.add(Long.parseLong(value));
            } catch (Exception e) {
            }
        }
        return postIds;
    }
    
    public List<Long> getHashtagPopularNewFeed(Long id, Double offset) {
        Set<String> values = jedisCache.getSortedSetDsc(getKey(FeedType.HASHTAG_POPULAR_NEW,id), offset);
        final List<Long> postIds = new ArrayList<>();
    
        for (String value : values) {
            try {
                postIds.add(Long.parseLong(value));
            } catch (Exception e) {
            }
        }
        return postIds;
    }
    
    public List<Long> getHashtagPopularUsedFeed(Long id, Double offset) {
        Set<String> values = jedisCache.getSortedSetDsc(getKey(FeedType.HASHTAG_POPULAR_USED,id), offset);
        final List<Long> postIds = new ArrayList<>();
    
        for (String value : values) {
            try {
                postIds.add(Long.parseLong(value));
            } catch (Exception e) {
            }
        }
        return postIds;
    }
    
    public List<Long> getHashtagNewestFeed(Long id, Double offset) {
        Set<String> values = jedisCache.getSortedSetDsc(getKey(FeedType.HASHTAG_NEWEST,id), offset);
        final List<Long> postIds = new ArrayList<>();
    
        for (String value : values) {
            try {
                postIds.add(Long.parseLong(value));
            } catch (Exception e) {
            }
        }
        return postIds;
    }
    
    public List<Long> getHashtagPriceLowHighFeed(Long id, Double offset) {
        Set<String> values = jedisCache.getSortedSetAsc(getKey(FeedType.HASHTAG_PRICE_LOW_HIGH,id), offset);
        final List<Long> postIds = new ArrayList<>();
    
        for (String value : values) {
            try {
                postIds.add(Long.parseLong(value));
            } catch (Exception e) {
            }
        }
        return postIds;
    }
    
    public List<Long> getHashtagPriceHighLowFeed(Long id, Double offset) {
        Set<String> values = jedisCache.getSortedSetDsc(getKey(FeedType.HASHTAG_PRICE_HIGH_LOW,id), offset);
        final List<Long> postIds = new ArrayList<>();
    
        for (String value : values) {
            try {
                postIds.add(Long.parseLong(value));
            } catch (Exception e) {
            }
        }
        return postIds;
    }
        
    public List<Long> getUserPostedFeed(Long id, Double offset) {
        Set<String> values = jedisCache.getSortedSetDsc(getKey(FeedType.USER_POSTED,id), offset);
        final List<Long> postIds = new ArrayList<>();
        for (String value : values) {
            try {
                postIds.add(Long.parseLong(value));
            } catch (Exception e) {
            }
        }
        return postIds;
    }
    
    public List<Long> getUserLikedFeed(Long id, Double offset) {
        Set<String> values = jedisCache.getSortedSetDsc(getKey(FeedType.USER_LIKED,id), offset);
        final List<Long> postIds = new ArrayList<>();
        for (String value : values) {
            try {
                postIds.add(Long.parseLong(value));
            } catch (Exception e) {
            }
        }
        return postIds;
    }
    
    public List<Long> getUserRecommendedSellersFeed(Long id, Double offset) {
        buildUserRecommendedSellersFeedQueueIfNotExist(id);
        
        Set<String> values = jedisCache.getSortedSetDsc(getKey(FeedType.USER_RECOMMENDED_SELLERS,id), offset, DefaultValues.DEFAULT_INFINITE_SCROLL_COUNT);
        final List<Long> postIds = new ArrayList<>();
        for (String value : values) {
            try {
                postIds.add(Long.parseLong(value));
            } catch (Exception e) {
            }
        }
        jedisCache.expire(getKey(FeedType.USER_RECOMMENDED_SELLERS, id), (id == User.NO_LOGIN_ID) ? FEED_SNAPSHOT_LONG_EXPIRY_SECS : FEED_SNAPSHOT_EXPIRY_SECS);
        return postIds;
    }
       
    public List<Long> getUserFollowingsFeed(Long id) {
        Set<String> values = jedisCache.getSortedSetDsc(getKey(FeedType.USER_FOLLOWINGS,id));
        final List<Long> userIds = new ArrayList<>();
        for (String value : values) {
            try {
                userIds.add(Long.parseLong(value));
            } catch (Exception e) {
            }
        }
        return userIds;
    }
    
    public List<Long> getUserFollowingsFeed(Long id, Double offset) {
        long start = offset.longValue() * CalcServer.FEED_RETRIEVAL_COUNT;
        long end = start + CalcServer.FEED_RETRIEVAL_COUNT - 1;
        Set<String> values = jedisCache.getSortedSetDsc(getKey(FeedType.USER_FOLLOWINGS,id),start,end);
        final List<Long> userIds = new ArrayList<>();
        for (String value : values) {
            try {
                userIds.add(Long.parseLong(value));
            } catch (Exception e) {
            }
        }
        return userIds;
    }
    
    public List<Long> getUserFollowersFeed(Long id) {
        Set<String> values = jedisCache.getSortedSetDsc(getKey(FeedType.USER_FOLLOWERS,id));
        final List<Long> userIds = new ArrayList<>();
        for (String value : values) {
            try {
                userIds.add(Long.parseLong(value));
            } catch (Exception e) {
            }
        }
        return userIds;
    }
    
    public List<Long> getUserFollowersFeed(Long id, Double offset) {
        long start = offset.longValue() * CalcServer.FEED_RETRIEVAL_COUNT;
        long end = start + CalcServer.FEED_RETRIEVAL_COUNT - 1;
        Set<String> values = jedisCache.getSortedSetDsc(getKey(FeedType.USER_FOLLOWERS,id),start,end);
        final List<Long> userIds = new ArrayList<>();
        for (String value : values) {
            try {
                userIds.add(Long.parseLong(value));
            } catch (Exception e) {
            }
        }
        return userIds;
    }
    
    public List<Long> getSuggestedProducts(Long id) {
        buildSuggestedProductQueueIfNotExist(id);
        
        Set<String> values = jedisCache.getSortedSetDsc(getKey(FeedType.PRODUCT_SUGGEST, id));
        final List<Long> postIds = new ArrayList<>();
        for (String value : values) {
            try {
                postIds.add(Long.parseLong(value));
            } catch (Exception e) {
            }
        }
        jedisCache.expire(getKey(FeedType.PRODUCT_SUGGEST, id), FEED_SNAPSHOT_EXPIRY_SECS);
        return postIds;
    }
    
    public List<Long> getProductLikesQueue(Long postId) {
        Set<String> values = jedisCache.getSortedSetDsc(getKey(FeedType.PRODUCT_LIKES,postId));
        final List<Long> userIds = new ArrayList<>();
        for (String value : values) {
            try {
                userIds.add(Long.parseLong(value));
            } catch (Exception e) {
            }
        }
        return userIds;
    }
    
    public boolean isLiked(Long userId, Long postId) {
        String key = getKey(FeedType.USER_LIKED,userId);
        return jedisCache.isMemberOfSortedSet(key, postId.toString());
    }
   
    public boolean isFollowed(Long userId, Long followingUserId) {
        String key = getKey(FeedType.USER_FOLLOWINGS,userId);
        return jedisCache.isMemberOfSortedSet(key, followingUserId.toString());
    }
}
