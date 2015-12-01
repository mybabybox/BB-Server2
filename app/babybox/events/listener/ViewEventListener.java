package babybox.events.listener;

import models.Post;
import models.User;
import babybox.events.map.TouchEvent;
import babybox.events.map.ViewEvent;

import com.google.common.eventbus.Subscribe;

import common.cache.CalcServer;

public class ViewEventListener extends EventListener {
    private static final play.api.Logger logger = play.api.Logger.apply(ViewEventListener.class);
    
	@Subscribe
    public void recordViewEvent(ViewEvent map){
	    try {
    		final Post post = (Post) map.get("post");
    		final User user = (User) map.get("user");
    		
    		if (post.onView(user)) {
    		    // CalcServer.instance().recalcScoreAndAddToCategoryPopularQueue(post);
    		}
    	} catch(Exception e) {
            logger.underlyingLogger().error(e.getMessage(), e);
        }
    }
	
	@Subscribe
    public void recordTouchEvent(TouchEvent map){
	    try {
            final Post post = (Post) map.get("post");
            final User user = (User) map.get("user");
            
            CalcServer.instance().recalcScoreAndAddToCategoryPopularQueue(post);
    	} catch(Exception e) {
            logger.underlyingLogger().error(e.getMessage(), e);
        }
    }
}
