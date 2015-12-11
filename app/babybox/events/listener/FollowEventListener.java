package babybox.events.listener;

import java.util.Date;

import models.Activity;
import models.GameBadgeAwarded;
import models.User;
import models.Activity.ActivityType;
import models.GameBadge.BadgeType;
import babybox.events.map.FollowEvent;
import babybox.events.map.UnFollowEvent;

import com.google.common.eventbus.Subscribe;

import common.cache.CalcServer;
import common.thread.TransactionalRunnableTask;

public class FollowEventListener extends EventListener {
    private static final play.api.Logger logger = play.api.Logger.apply(FollowEventListener.class);
    
	@Subscribe
    public void recordFollowEvent(FollowEvent map){
	    try {
    		final User localUser = (User) map.get("localUser");
    		final User user = (User) map.get("user");
    		
    		// why we require this, if we are renewing HOME_FOLLOWING feed after every 2 mins 
    		if (localUser.onFollow(user)) {
    		    // ideally use FollowSocialRelation.CREATED_DATE
                Long score = new Date().getTime();
                CalcServer.instance().addToFollowQueue(localUser.id, user.id, score.doubleValue());
                
                executeAsync(
                        new TransactionalRunnableTask() {
                            @Override
                            public void execute() {
                                // game badge
                                if (localUser.numFollowings == 3) {
                                    GameBadgeAwarded.recordGameBadge(localUser.id, BadgeType.FOLLOW_3);
                                } else if (localUser.numFollowings == 10) {
                                    GameBadgeAwarded.recordGameBadge(localUser.id, BadgeType.FOLLOW_10);
                                }
                                
                                // activity
                                Activity activity = new Activity(
                                        ActivityType.FOLLOWED, 
                                        user.id,
                                        false, 
                                        localUser.id,
                                        localUser.id,
                                        localUser.displayName,
                                        user.id,
                                        user.id,
                                        user.displayName);
                                activity.ensureUniqueAndCreate();
                            }
                        });
    		}
    	} catch(Exception e) {
            logger.underlyingLogger().error(e.getMessage(), e);
        }
    }
	
	@Subscribe
    public void recordUnFollowEvent(UnFollowEvent map){
	    try {
    		final User localUser = (User) map.get("localUser");
    		final User user = (User) map.get("user");
    		
    		if (localUser.onUnFollow(user)) {
    		    CalcServer.instance().removeFromFollowQueue(localUser.id, user.id);                          
    		}
    	} catch(Exception e) {
            logger.underlyingLogger().error(e.getMessage(), e);
        }
    }
}
