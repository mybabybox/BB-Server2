package babybox.events.listener;

import models.Conversation;
import models.Post;
import babybox.events.map.ConversationEvent;

import com.google.common.eventbus.Subscribe;

import common.cache.CalcServer;

public class ConversationEventListener extends EventListener {
    private static final play.api.Logger logger = play.api.Logger.apply(ConversationEventListener.class);
    
	@Subscribe
    public void recordConversationEvent(ConversationEvent map){
	    try {
    	    final Conversation conversation = (Conversation) map.get("conversation");
    	    final Post post = (Post) map.get("post");
    	    
    	    CalcServer.instance().recalcScoreAndAddToCategoryPopularQueue(post);
    	} catch(Exception e) {
            logger.underlyingLogger().error(e.getMessage(), e);
        }
    }
}	
