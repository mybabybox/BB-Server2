package babybox.events.listener;

import models.Story;
import models.User;
import babybox.events.map.DeleteStoryEvent;
import babybox.events.map.EditStoryEvent;
import babybox.events.map.StoryEvent;

import com.google.common.eventbus.Subscribe;

import common.thread.TransactionalRunnableTask;

public class StoryEventListener extends EventListener {
    private static final play.api.Logger logger = play.api.Logger.apply(StoryEventListener.class);
    
	@Subscribe
    public void recordStoryEvent(StoryEvent map){
	    try { 
    		final Story story = (Story) map.get("story");
    		final User user = (User) map.get("user");

    		executeAsync(
                    new TransactionalRunnableTask() {
                        @Override
                        public void execute() {
                            
                        }
                    });
    	} catch(Exception e) {
            logger.underlyingLogger().error(e.getMessage(), e);
        }
    }
	
	@Subscribe
    public void recordEditStoryEvent(EditStoryEvent map){
	    try {
	        Story story = (Story) map.get("story");
            
    	} catch(Exception e) {
            logger.underlyingLogger().error(e.getMessage(), e);
        }
    }
	
	@Subscribe
    public void recordDeleteStoryEvent(DeleteStoryEvent map){
	    try {
	        Story story = (Story) map.get("story");
    		
    	} catch(Exception e) {
            logger.underlyingLogger().error(e.getMessage(), e);
        }
    }
}
