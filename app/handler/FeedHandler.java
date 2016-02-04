package handler;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.google.inject.Singleton;

import models.Post;
import models.User;
import viewmodel.PostVMLite;
import viewmodel.SellerVM;
import viewmodel.UserVMLite;
import common.cache.CalcServer;
import common.model.FeedFilter.FeedType;
import domain.DefaultValues;

@Singleton
public class FeedHandler {
	
    @Inject
    CalcServer calcServer;

    private static boolean filterSold = false;
    
    public List<PostVMLite> getFeedPosts(
            Long id, Long offset, User localUser, FeedType feedType) {
        return getFeedPosts(id, offset, localUser, feedType, -1);
    }
    
    public List<PostVMLite> getFeedPosts(
            Long id, Long offset, User localUser, FeedType feedType, int limit) {
        
        filterSold = false;
    	List<Long> postIds = new ArrayList<>();
    	switch (feedType) {
    	case HOME_EXPLORE:
    	    postIds = calcServer.getHomeExploreFeed(id, offset.doubleValue());
    	    break;
	
        case HOME_FOLLOWING:
        	postIds = calcServer.getHomeFollowingFeed(id, offset.doubleValue());
        	filterSold = true;
        	break;
        
        case CATEGORY_POPULAR:
        	postIds = calcServer.getCategoryPopularFeed(id, offset.doubleValue());
        	break;
        	
        case CATEGORY_NEWEST:
        	postIds = calcServer.getCategoryNewestFeed(id, offset.doubleValue());
        	break;
        	
        case CATEGORY_PRICE_HIGH_LOW:
        	postIds = calcServer.getCategoryPriceHighLowFeed(id, offset.doubleValue());
        	break;
        	
        case CATEGORY_PRICE_LOW_HIGH:
        	postIds = calcServer.getCategoryPriceLowHighFeed(id, offset.doubleValue());
        	break;
        
        case USER_LIKED:
        	postIds = calcServer.getUserLikedFeed(id, offset.doubleValue());
        	break;
        
        case USER_POSTED:
        	postIds = calcServer.getUserPostedFeed(id, offset.doubleValue());
        	break;
        
        case PRODUCT_SUGGEST:
        	postIds = calcServer.getSuggestedProducts(id);
        	break;
        
        default:
        	break;
    	}
    	
    	List<PostVMLite> vms = new ArrayList<>();
    	if (postIds.size() == 0) {
    		return vms;
    	}
    	
    	if (limit > 0 && postIds.size() > limit) {
    	    postIds = postIds.subList(0, limit);
    	}
    	
    	List<Post> posts = Post.getPosts(postIds);
    	for (Post post : posts) {
    	    if (filterSold && post.sold) {
    	        continue;
    	    }
    	    
    		PostVMLite vm = new PostVMLite(post, localUser);
    		vm.offset = calcServer.getScore(calcServer.getKey(feedType, id), post.id).longValue();
    		vms.add(vm);
    	}
    	
    	/*
    	for (Long postId : postIds) {
    	    Post post = Post.findById(postId);
    	    if (post == null || (filterSold && post.sold)) {
                continue;
            }
            
            PostVMLite vm = new PostVMLite(post, localUser);
            vm.offset = calcServer.getScore(calcServer.getKey(feedType, id), post.id).longValue();
            vms.add(vm);
    	}
    	*/
    	
    	return vms;
    }
	
    public List<UserVMLite> getFeedUsers(
            Long id, Long offset, User localUser, FeedType feedType) {
            
        List<Long> userIds = new ArrayList<>();
        switch (feedType) {
        case USER_FOLLOWINGS:
            userIds = calcServer.getUserFollowingsFeed(id, offset.doubleValue());
            break;
            
        case USER_FOLLOWERS:
            userIds = calcServer.getUserFollowersFeed(id, offset.doubleValue());
            break;
            
        case USER_RECOMMENDED_SELLERS:
            userIds = calcServer.getUserRecommendedSellersFeed(id, offset.doubleValue());
            break;
            
        default:
            break;
        }
            
        List<UserVMLite> vms = new ArrayList<>();
        if (userIds.size() == 0) {
            return vms;
        }
        
        List<User> users = User.getUsers(userIds);
        for (User user : users) {
            if (user.newUser || !user.active || user.deleted) {
                continue;
            }
            
            UserVMLite vm = new UserVMLite(user, localUser);
            vm.offset = calcServer.getScore(calcServer.getKey(feedType, id), user.id).longValue();
            vms.add(vm);
        }
        
        return vms;
    }
    
    public List<SellerVM> getFeedSellers(
            Long id, Long offset, User localUser, FeedType feedType) {
            
        List<Long> userIds = new ArrayList<>();
        switch (feedType) {
        case USER_RECOMMENDED_SELLERS:
            userIds = calcServer.getUserRecommendedSellersFeed(id, offset.doubleValue());
            break;
            
        default:
            break;
        }
            
        List<SellerVM> vms = new ArrayList<>();
        if(userIds.size() == 0){
            return vms;
        }
        
        List<User> users = User.getUsers(userIds);
        for (User user : users) {
            if (user.newUser || !user.active || user.deleted) {
                continue;
            }
            
            // get first batch of seller products
            List<PostVMLite> posts = getFeedPosts(
                    user.id, 0L, localUser, FeedType.USER_POSTED, 
                    DefaultValues.MAX_SELLER_PRODUCTS_FOR_FEED);
            if (posts.size() > DefaultValues.MAX_SELLER_PRODUCTS_FOR_FEED) {
                posts = posts.subList(0, DefaultValues.MAX_SELLER_PRODUCTS_FOR_FEED);
            }
            
            SellerVM vm = new SellerVM(user, localUser, posts);
            vm.offset = calcServer.getScore(calcServer.getKey(feedType, id), user.id).longValue();
            vms.add(vm);
        }
        
        return vms;
    }
}
