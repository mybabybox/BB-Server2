package babybox.events.listener;

import models.Activity;
import models.Activity.ActivityType;
import models.Post;
import models.User;
import babybox.events.map.LikeEvent;
import babybox.events.map.UnlikeEvent;

import com.google.common.eventbus.Subscribe;

import common.cache.CalcServer;
import common.utils.StringUtil;
import email.SendgridEmailClient;

public class LikeEventListener {
    private static final play.api.Logger logger = play.api.Logger.apply(LikeEventListener.class);
    
	@Subscribe
    public void recordLikeEventInDB(LikeEvent map){
	    try {
    		Post post = (Post) map.get("post");
    		User user = (User) map.get("user");
           	if (post.onLikedBy(user)) {
           	    CalcServer.instance().recalcScoreAndAddToCategoryPopularQueue(post);
           	    CalcServer.instance().addToLikeQueue(post, user);
    	       	
    	       	if (user.id != post.owner.id) {
        	       	Activity activity = new Activity(
        					ActivityType.LIKED, 
        					post.owner.id,
        					true, 
        					user.id,
        					user.id,
        					user.displayName,
        					post.id,
        					post.getImage(),
        					StringUtil.shortMessage(post.title));
        	        activity.ensureUniqueAndCreate();
        	        
        	        // Sendgrid
        	        //SendgridEmailClient.getInstatnce().sendMailOnLike(user, post.owner, post.title);
    	       	}
           	}
    	} catch(Exception e) {
            logger.underlyingLogger().error(e.getMessage(), e);
        }
    }
	
	@Subscribe
    public void recordUnlikeEventInDB(UnlikeEvent map){
	    try {
    		Post post = (Post) map.get("post");
    		User user = (User) map.get("user");
           	if (post.onUnlikedBy(user)) {
           	    CalcServer.instance().recalcScoreAndAddToCategoryPopularQueue(post);
           	    CalcServer.instance().removeFromLikeQueue(post, user);
           	}
    	} catch(Exception e) {
            logger.underlyingLogger().error(e.getMessage(), e);
        }
    }
}
