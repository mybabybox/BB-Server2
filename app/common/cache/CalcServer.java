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
	public static final Long FEED_CATEGORY_EXPOSURE_MIN = Play.application().configuration().getLong("feed.category.exposure.min");
	public static final int FEED_SNAPSHOT_EXPIRY = Play.application().configuration().getInt("feed.snapshot.expiry");
	public static final int FEED_SNAPSHOT_LONG_EXPIRY = Play.application().configuration().getInt("feed.snapshot.long.expiry");
	public static final int FEED_SOLD_CLEANUP_DAYS = Play.application().configuration().getInt("feed.sold.cleanup.days");
	public static final int FEED_RETRIEVAL_COUNT = DefaultValues.FEED_INFINITE_SCROLL_COUNT;
	
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
		
		buildQueuesFromUsers();
		buildQueuesFromPosts();
		buildQueuesFromHashtags();
		
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
	
	public void clearCategoryQueues() {
		for(Category category : Category.getAllCategories()){
			jedisCache.remove(getKey(FeedType.CATEGORY_PRICE_HIGH_LOW,category.id));
			jedisCache.remove(getKey(FeedType.CATEGORY_NEWEST,category.id));
			jedisCache.remove(getKey(FeedType.CATEGORY_POPULAR,category.id));
			jedisCache.remove(getKey(FeedType.CATEGORY_PRICE_LOW_HIGH,category.id));
		}
	}
	
	public void clearHashtagsQueues() {
        for(Hashtag hashtag : Hashtag.getAllHashtags()){
            jedisCache.remove(getKey(FeedType.HASHTAG_PRICE_LOW_HIGH,hashtag.id));
            jedisCache.remove(getKey(FeedType.HASHTAG_PRICE_HIGH_LOW,hashtag.id));
            jedisCache.remove(getKey(FeedType.HASHTAG_POPULAR,hashtag.id));
            jedisCache.remove(getKey(FeedType.HASHTAG_NEWEST,hashtag.id));
        }
	}
	
	public void clearUserQueues(User user) {
		jedisCache.remove(getKey(FeedType.USER_POSTED,user.id));
		jedisCache.remove(getKey(FeedType.USER_LIKED,user.id));
		jedisCache.remove(getKey(FeedType.USER_FOLLOWINGS,user.id));
		// dont clear USER_FOLLOWERS queue, it is created by USER_FOLLOWINGS indirectly 
        //jedisCache.remove(getKey(FeedType.USER_FOLLOWERS,user.id));
	}
	
	public void clearPostQueues(Post post) {
		jedisCache.remove(getKey(FeedType.PRODUCT_LIKES,post.id));
	}


	public Long calculateBaseScore(Post post) {
		// skip already calculated posts during server startup
		if (ThreadLocalOverride.isServerStartingUp() && post.baseScore > 0L) {
			return post.baseScore;
		}
		return formula.computeBaseScore(post);
	}
	
	/**
	 * Main entry for building queues from users.
	 */
	private void buildQueuesFromUsers() {
		for(User user : User.getEligibleUsersForFeed()){
		    if (!FEED_INIT_FLUSH_ALL) {
		        clearUserQueues(user);
		    }
		    
			buildUserLikedPostQueue(user);
			buildUserFollowingsFollowersQueue(user);
			addToRecommendedSellersQueue(user);
		}
	}

	private void buildUserLikedPostQueue(User user) {
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
		
		for (SocialRelation socialRelation : FollowSocialRelation.getUserFollowings(user.id)) {
		    // USER_FOLLOWINGS:actor adds target
			jedisCache.putToSortedSet(getKey(FeedType.USER_FOLLOWINGS,socialRelation.actor), socialRelation.getCreatedDate().getTime(), socialRelation.target.toString());
			// USER_FOLLOWERS:target adds actor  
			jedisCache.putToSortedSet(getKey(FeedType.USER_FOLLOWERS,socialRelation.target), socialRelation.getCreatedDate().getTime(), socialRelation.actor.toString());
		}
		
		sw.stop();
		logger.underlyingLogger().debug("buildUserFollowingsQueue completed. Took "+sw.getElapsedSecs()+"s");
	}

	/**
     * Main entry for building queues from Hashtags.
     */
	private void buildQueuesFromHashtags() {
		NanoSecondStopWatch sw = new NanoSecondStopWatch();
		logger.underlyingLogger().debug("buildQueuesFromHashtags starts");

		if (!FEED_INIT_FLUSH_ALL) {
		    clearHashtagsQueues();
		}
		
		for (Hashtag hashtag: Hashtag.getAllHashtags()){
			addToHashtagQueues(hashtag);
		}
		sw.stop();
		logger.underlyingLogger().debug("buildQueuesFromHashtags completed. Took "+sw.getElapsedSecs()+"s");
	}

	public void addToHashtagQueues(Hashtag hashtag){
		for (Post post : Post.getEligiblePostsForFeeds()) {
			if (post.soldMarked) {
				continue;
			}
			addToHashtagPriceLowHighQueue(hashtag.id, post);
			addToHashtagNewestQueue(hashtag.id, post);
			addToHashtagPopularQueue(hashtag.id, post);
		}
	}
	
	private void addToHashtagPriceLowHighQueue(Long hashtag, Post post) {
	    if (post.soldMarked) {
            return;
	    }
	    List<Long> hashtagids = post.getSellerHashtagIds();
	    List<Long> systemTagid = post.getSystemHashtagIds();
	    hashtagids.addAll(systemTagid);
	    if(hashtagids.contains(hashtag))
		jedisCache.putToSortedSet(getKey(FeedType.HASHTAG_PRICE_LOW_HIGH, hashtag), post.price * FEED_SCORE_HIGH_BASE + post.id , post.id.toString());
	}
	
	private void addToHashtagNewestQueue(Long hashtag, Post post) {
	    if (post.soldMarked) {
            return;
        }
	    List<Long> hashtagids = post.getSellerHashtagIds();
	    List<Long> systemTagid = post.getSystemHashtagIds();
	    hashtagids.addAll(systemTagid);
	    if(hashtagids.contains(hashtag)) {
	    	jedisCache.putToSortedSet(getKey(FeedType.HASHTAG_NEWEST,hashtag), post.getCreatedDate().getTime(), post.id.toString());
	    }
	}
	
	private void addToHashtagPopularQueue(Long hashtag, Post post) {
	    if (post.soldMarked) {
            return;
        }
	    List<Long> hashtagids = post.getSellerHashtagIds();
	    List<Long> systemTagid = post.getSystemHashtagIds();
	    hashtagids.addAll(systemTagid);
	    if(hashtagids.contains(hashtag)){
	    	Double timeScore = calculateTimeScore(post, true);
	    	jedisCache.putToSortedSet(getKey(FeedType.HASHTAG_POPULAR,hashtag),  timeScore.doubleValue() * FEED_SCORE_HIGH_BASE, post.id.toString());
	    }
	}
		private void addToRecommendedSellersQueue(User user) {
        if (user.isRecommendedSeller()) {
            jedisCache.putToSortedSet(getKey(FeedType.RECOMMENDED_SELLERS), user.getLastLogin().getTime(), user.id.toString());
        }
    }
	/**
     * Main entry for building queues from posts.
     */
	private void buildQueuesFromPosts() {
	    NanoSecondStopWatch sw = new NanoSecondStopWatch();
        logger.underlyingLogger().debug("buildQueuesFromPosts starts");
        
        if (!FEED_INIT_FLUSH_ALL) {
            clearCategoryQueues();
        }

		for (Post post : Post.getEligiblePostsForFeeds()) {
		    if (!FEED_INIT_FLUSH_ALL) {
		        clearPostQueues(post);
		    }
		    
			addToUserPostedQueue(post);
			
			// below queues skip sold products
		    if (post.soldMarked) {
                continue;
            }
		    addToCategoryPriceLowHighQueue(post);
		    addToCategoryNewestQueue(post);
		    addToCategoryPopularQueue(post);
		    buildProductLikesQueue(post);
		}
		
		sw.stop();
        logger.underlyingLogger().debug("buildQueuesFromPosts completed. Took "+sw.getElapsedSecs()+"s");
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

	public Double calculateTimeScore(Post post) {
	    return calculateTimeScore(post, false);
	}
	
	private Double calculateTimeScore(Post post, boolean recalcBaseScore) {
	    if (recalcBaseScore) {
	        calculateBaseScore(post);
	    }
		return formula.computeTimeScore(post);
	}

	public void recalcScoreAndAddToCategoryPopularQueue(Post post) {
	    addToCategoryPopularQueue(post);
	}
	
	private void addToCategoryPopularQueue(Post post) {
	    if (post.soldMarked) {
            return;
        }
        Double timeScore = calculateTimeScore(post, true);
        jedisCache.putToSortedSet(getKey(FeedType.CATEGORY_POPULAR,post.category.id),  timeScore.doubleValue() * FEED_SCORE_HIGH_BASE, post.id.toString());
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

	public void addToUserPostedQueue(Post post) {
		jedisCache.putToSortedSet(getKey(FeedType.USER_POSTED,post.owner.id), post.getCreatedDate().getTime(), post.id.toString());
	}
	
	private void buildUserExploreFeedQueue(Long userId) {
		NanoSecondStopWatch sw = new NanoSecondStopWatch();
		logger.underlyingLogger().debug("buildUserExploreFeedQueue starts - u="+userId);
		
		User user = User.findById(userId);
		Map<Long, Long> map = new HashMap<Long, Long>();
		if (user != null) {
			map = user.getUserCategoriesRatioForFeed();
		}
		
		for (Category category : Category.getAllCategories()){
			Set<String> values = jedisCache.getSortedSetDsc(getKey(FeedType.CATEGORY_POPULAR,category.id));
			final List<Long> postIds = new ArrayList<>();
			for (String value : values) {
				try {
					postIds.add(Long.parseLong(value));
				} catch (Exception e) {
				}
			}
			
			Long percentage = FEED_CATEGORY_EXPOSURE_MIN;
			Long catViewPercentage = map.get(category.getId());
			if (catViewPercentage != null && catViewPercentage > percentage) {
				percentage = catViewPercentage;
			}
			
			// if post.size() is less than FEED_HOME_COUNT_MAX (limit of post)
			Long postsSize = postIds.size() > FEED_HOME_COUNT_MAX ? FEED_HOME_COUNT_MAX : postIds.size(); 
			Integer length =  (int) ((postsSize * percentage) / 100);
			postIds.subList(0, length);
			for(Long postId : postIds){
				jedisCache.putToSortedSet(getKey(FeedType.HOME_EXPLORE, userId), formula.randomizeScore(Post.findById(postId)) * FEED_SCORE_HIGH_BASE, postId.toString());
			}
			
			logger.underlyingLogger().debug(
                    "     cat="+category.getId()+" name="+category.getName()+" catFeedSize="+postsSize+" %="+percentage+" %catFeedSize="+postIds.size());
		}
		jedisCache.expire(getKey(FeedType.HOME_EXPLORE, userId), (user == null) ? FEED_SNAPSHOT_LONG_EXPIRY : FEED_SNAPSHOT_EXPIRY);
		
		sw.stop();
		logger.underlyingLogger().debug("buildUserExploreFeedQueue completed. Took "+sw.getElapsedSecs()+"s");
	}
	
	private void buildHomeFollowingFeedQueue(Long userId) {
		NanoSecondStopWatch sw = new NanoSecondStopWatch();
		logger.underlyingLogger().debug("buildHomeFollowingQueue starts - u="+userId);
		
		List<Long> followings = getUserFollowingsFeed(userId);
		for (Long followingUser : followings){
			Set<String> values = jedisCache.getSortedSetDsc(getKey(FeedType.USER_POSTED,followingUser));
			for (String value : values) {
				try {
					Long postId = Long.parseLong(value);
					jedisCache.putToSortedSet(getKey(FeedType.HOME_FOLLOWING,userId), getScore(getKey(FeedType.USER_POSTED, followingUser), postId), postId.toString());
				} catch (Exception e) {
				}
			}
		}
		jedisCache.expire(getKey(FeedType.HOME_FOLLOWING,userId), FEED_SNAPSHOT_EXPIRY);
		
		sw.stop();
		logger.underlyingLogger().debug("buildHomeFollowingQueue completed. Took "+sw.getElapsedSecs()+"s");
	}
	
	private void buildUserRecommendedSellersFeedQueue(Long userId) {
        NanoSecondStopWatch sw = new NanoSecondStopWatch();
        logger.underlyingLogger().debug("buildUserRecommendedSellersFeedQueue starts - u="+userId);
        
        // randomize RECOMMENDED_SELLERS queue
        Set<String> values = jedisCache.getSortedSetDsc(getKey(FeedType.RECOMMENDED_SELLERS));
        for (String value : values) {
            try {
                Long sellerId = Long.parseLong(value);
                jedisCache.putToSortedSet(getKey(FeedType.USER_RECOMMENDED_SELLERS, userId), Math.random() * FEED_SCORE_HIGH_BASE, sellerId.toString());
            } catch (Exception e) {
            }
        }
        jedisCache.expire(getKey(FeedType.USER_RECOMMENDED_SELLERS, userId), FEED_SNAPSHOT_LONG_EXPIRY);
        
        sw.stop();
        logger.underlyingLogger().debug("buildUserRecommendedSellersFeedQueue completed. Took "+sw.getElapsedSecs()+"s");
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
		
		jedisCache.expire(getKey(FeedType.PRODUCT_SUGGEST, postId), FEED_SNAPSHOT_EXPIRY);
		
		sw.stop();
		logger.underlyingLogger().debug("buildSuggestedProductQueue completed. Took "+sw.getElapsedSecs()+"s");
	}
	
	public boolean isLiked(Long userId, Long postId) {
		 String key = getKey(FeedType.USER_LIKED,userId);
	     return jedisCache.isMemberOfSortedSet(key, postId.toString());
	}
	
	public boolean isFollowed(Long userId, Long followingUserId) {
		 String key = getKey(FeedType.USER_FOLLOWINGS,userId);
	     return jedisCache.isMemberOfSortedSet(key, followingUserId.toString());
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
	
	public List<Long> getHomeExploreFeed(Long id, Double offset) {
		if(!jedisCache.exists(getKey(FeedType.HOME_EXPLORE,id))){
			buildUserExploreFeedQueue(id);
		}
		Set<String> values = jedisCache.getSortedSetDsc(getKey(FeedType.HOME_EXPLORE,id), offset);
        final List<Long> postIds = new ArrayList<>();
        for (String value : values) {
            try {
                postIds.add(Long.parseLong(value));
            } catch (Exception e) {
            }
        }
        jedisCache.expire(getKey(FeedType.HOME_EXPLORE,id), FEED_SNAPSHOT_EXPIRY);
        return postIds;
	}
	
	public List<Long> getHomeFollowingFeed(Long id, Double offset) {
		if(!jedisCache.exists(getKey(FeedType.HOME_FOLLOWING,id))){
			buildHomeFollowingFeedQueue(id);
		}
		Set<String> values = jedisCache.getSortedSetDsc(getKey(FeedType.HOME_FOLLOWING,id), offset);
        final List<Long> postIds = new ArrayList<>();
        for (String value : values) {
            try {
                postIds.add(Long.parseLong(value));
            } catch (Exception e) {
            }
        }
        jedisCache.expire(getKey(FeedType.HOME_FOLLOWING,id), FEED_SNAPSHOT_EXPIRY);
        return postIds;
	}
	
	public List<Long> getUserRecommendedSellersFeed(Long id, Double offset) {
        if(!jedisCache.exists(getKey(FeedType.USER_RECOMMENDED_SELLERS,id))){
            buildUserRecommendedSellersFeedQueue(id);
        }
        Set<String> values = jedisCache.getSortedSetDsc(getKey(FeedType.USER_RECOMMENDED_SELLERS,id), offset);
        final List<Long> postIds = new ArrayList<>();
        for (String value : values) {
            try {
                postIds.add(Long.parseLong(value));
            } catch (Exception e) {
            }
        }
        jedisCache.expire(getKey(FeedType.USER_RECOMMENDED_SELLERS,id), FEED_SNAPSHOT_LONG_EXPIRY);
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
	    long end = start + CalcServer.FEED_RETRIEVAL_COUNT;
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
        long end = start + CalcServer.FEED_RETRIEVAL_COUNT;
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
		if(!jedisCache.exists(getKey(FeedType.PRODUCT_SUGGEST, id))){
			buildSuggestedProductQueue(id);
		}
		
		Set<String> values = jedisCache.getSortedSetDsc(getKey(FeedType.PRODUCT_SUGGEST, id));
        final List<Long> postIds = new ArrayList<>();
        for (String value : values) {
            try {
                postIds.add(Long.parseLong(value));
            } catch (Exception e) {
            }
        }
        jedisCache.expire(getKey(FeedType.PRODUCT_SUGGEST, id), FEED_SNAPSHOT_EXPIRY);
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
	
	public void addToCategoryQueues(Post post) {
		addToCategoryPriceLowHighQueue(post);
		addToCategoryNewestQueue(post);
		addToCategoryPopularQueue(post);
	}
	
	public void removeFromCategoryQueues(Post post){
		removeFromCategoryQueues(post, post.category);
	}
	
	public void removeFromCategoryQueues(Post post, Category category){
	    removeMemberFromPriceLowHighPostQueue(post.id, category.id);
        removeMemberFromNewestPostQueue(post.id, category.id);
        removeMemberFromPopularPostQueue(post.id, category.id);
	}
	
	public void removeMemberFromPriceLowHighPostQueue(Long postId, Long categoryId){
		jedisCache.removeMemberFromSortedSet(getKey(FeedType.CATEGORY_PRICE_LOW_HIGH,categoryId), postId.toString());
	}
	
	public void removeMemberFromNewestPostQueue(Long postId, Long categoryId){
		jedisCache.removeMemberFromSortedSet(getKey(FeedType.CATEGORY_NEWEST,categoryId), postId.toString());
	}

	public void removeMemberFromPopularPostQueue(Long postId, Long categoryId){
		jedisCache.removeMemberFromSortedSet(getKey(FeedType.CATEGORY_POPULAR,categoryId), postId.toString());
	}
	
	public void addToLikeQueue(Post post, User user){
		jedisCache.putToSortedSet(getKey(FeedType.USER_LIKED,user.id), new Date().getTime(), post.id.toString());
	}
	
	public void removeFromLikeQueue(Post post, User user){
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
                soldPost.soldMarked = true;
                logger.underlyingLogger().info("[id="+soldPost.id+"] removeFromCategoryQueues: "+soldPost.title);
            }
        } else {
            logger.underlyingLogger().info("cleanupSoldPosts - There is no sold post before "+daysBefore.toString());
        }
    }
}
