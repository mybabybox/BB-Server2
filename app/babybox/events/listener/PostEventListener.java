package babybox.events.listener;

import models.Activity;
import models.Category;
import models.GameBadgeAwarded;
import models.Post;
import models.SystemInfo;
import models.User;
import models.Activity.ActivityType;
import models.GameBadge.BadgeType;
import babybox.events.map.DeletePostEvent;
import babybox.events.map.EditPostEvent;
import babybox.events.map.PostEvent;

import com.google.common.eventbus.Subscribe;

import common.cache.CalcServer;
import common.thread.TransactionalRunnableTask;
import common.utils.StringUtil;
import email.SendgridEmailClient;

public class PostEventListener extends EventListener {
    private static final play.api.Logger logger = play.api.Logger.apply(PostEventListener.class);
    
	@Subscribe
    public void recordPostEvent(PostEvent map){
	    try { 
    		final Post post = (Post) map.get("post");
    		final User user = (User) map.get("user");

    		CalcServer.instance().addToCategoryQueues(post);
            CalcServer.instance().addToUserPostedQueue(post);
            
            final Long postImageId = post.getImage();
    		executeAsync(
                    new TransactionalRunnableTask() {
                        @Override
                        public void execute() {
                            // game badge
                            if (user.numProducts == 1) {
                                GameBadgeAwarded.recordGameBadge(user, BadgeType.POST_1);
                            } else if (user.numProducts == 10) {
                                GameBadgeAwarded.recordGameBadge(user, BadgeType.POST_10);
                            }
                            
                            if (user.numProducts == 1) {
                                // activity
                                User babyboxUser = SystemInfo.getInfo().getBabyBoxCustomerCare();
                                Activity activity = new Activity(
                                        ActivityType.FIRST_POST, 
                                        user.id,
                                        false, 
                                        babyboxUser.id,
                                        babyboxUser.id,
                                        "",
                                        post.id,
                                        postImageId, 
                                        StringUtil.shortMessage(post.title));
                                activity.ensureUniqueAndCreate();
                            }
                            
                            // Sendgrid
                            SendgridEmailClient.getInstatnce().sendMailOnPost(post);
                        }
                    });
    	} catch(Exception e) {
            logger.underlyingLogger().error(e.getMessage(), e);
        }
    }
	
	@Subscribe
    public void recordEditPostEvent(EditPostEvent map){
	    try {
            Post post = (Post) map.get("post");
            Category category = (Category) map.get("category");
            
            // same category, no change
            if (post.category.id == category.id) {
                return;
            }
            
            CalcServer.instance().removeFromCategoryQueues(post, category);
            CalcServer.instance().addToCategoryQueues(post);
    	} catch(Exception e) {
            logger.underlyingLogger().error(e.getMessage(), e);
        }
    }
	
	@Subscribe
    public void recordDeletePostEvent(DeletePostEvent map){
	    try {
    		Post post = (Post) map.get("post");
    		User user = (User) map.get("user");
    		
    		CalcServer.instance().removeFromCategoryQueues(post);
    		CalcServer.instance().removeFromUserPostedQueue(post, post.owner);
    		CalcServer.instance().removeFromAllUsersLikedQueues(post);
    	} catch(Exception e) {
            logger.underlyingLogger().error(e.getMessage(), e);
        }
    }
}
