package babybox.events.listener;

import models.Activity;
import models.Activity.ActivityType;
import models.GameBadge.BadgeType;
import models.GameBadgeAwarded;
import models.Post;
import models.User;
import babybox.events.map.LikeEvent;
import babybox.events.map.UnlikeEvent;

import com.google.common.eventbus.Subscribe;

import common.cache.CalcServer;
import common.thread.TransactionalRunnableTask;
import common.utils.StringUtil;

public class LikeEventListener extends EventListener {
    private static final play.api.Logger logger = play.api.Logger.apply(LikeEventListener.class);
    
	@Subscribe
    public void recordLikeEvent(LikeEvent map){
	    try {
    		final Post post = (Post) map.get("post");
    		final User user = (User) map.get("user");
    		
    		CalcServer.instance().recalcScoreAndAddToCategoryPopularQueue(post);
            CalcServer.instance().addToLikeQueue(post, user);
            
            final Long postImageId = post.getImage();
            executeAsync(
                    new TransactionalRunnableTask() {
                        @Override
                        public void execute() {
                            // game badge
                            if (user.numLikes == 1) {
                                GameBadgeAwarded.recordGameBadge(user.id, BadgeType.LIKE_1);
                            } else if (user.numLikes == 10) {
                                GameBadgeAwarded.recordGameBadge(user.id, BadgeType.LIKE_10);
                            }
                            
                            // activity
                            if (user.id != post.owner.id) {
                                Activity activity = new Activity(
                                        ActivityType.LIKED, 
                                        post.owner.id,
                                        true, 
                                        user.id,
                                        user.id,
                                        user.displayName,
                                        post.id,
                                        postImageId,
                                        StringUtil.shortMessage(post.title));
                                activity.ensureUniqueAndCreate();
                            }                            
                        }
                    });
    	} catch(Exception e) {
            logger.underlyingLogger().error(e.getMessage(), e);
        }
    }
	
	@Subscribe
    public void recordUnlikeEvent(UnlikeEvent map){
	    try {
    		final Post post = (Post) map.get("post");
    		final User user = (User) map.get("user");
    		
    		CalcServer.instance().recalcScoreAndAddToCategoryPopularQueue(post);
            CalcServer.instance().removeFromLikeQueue(post, user);
    	} catch(Exception e) {
            logger.underlyingLogger().error(e.getMessage(), e);
        }
    }
}
