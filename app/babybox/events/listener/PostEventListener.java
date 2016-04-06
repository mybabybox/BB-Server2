package babybox.events.listener;

import models.Activity;
import models.Category;
import models.GameBadgeAwarded;
import models.Post;
import models.PostToMark;
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
            
            if (user.isRecommendedSeller()) {
                CalcServer.instance().addToRecommendedSellersQueue(user);
            }
            
            final Long postImageId = post.getImage();
    		executeAsync(
                    new TransactionalRunnableTask() {
                        @Override
                        public void execute() {
                            // To be marked by PostMarker 
                            PostToMark mark = new PostToMark(post.id);
                            mark.save();
                            
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
            final Post post = (Post) map.get("post");
            final Category oldCategory = (Category) map.get("category");
            
            // category/subcategory change
            if (oldCategory != null) {
                if (post.category.id != oldCategory.id) {
                    CalcServer.instance().removeFromCategoryQueues(post, oldCategory);
                }
            }
            
            CalcServer.instance().addToCategoryQueues(post);
            
            executeAsync(
                    new TransactionalRunnableTask() {
                        @Override
                        public void execute() {
                            // To be marked by PostMarker 
                            PostToMark mark = new PostToMark(post.id);
                            mark.save();
                        }
                    });
    	} catch(Exception e) {
            logger.underlyingLogger().error(e.getMessage(), e);
        }
    }
	
	@Subscribe
    public void recordDeletePostEvent(DeletePostEvent map){
	    try {
	        final Post post = (Post) map.get("post");
    		
    		CalcServer.instance().removeFromCategoryQueues(post);
    		CalcServer.instance().removeFromUserPostedQueue(post, post.owner);
    		CalcServer.instance().removeFromAllUsersLikedQueues(post);
    		
    		if (!post.owner.isRecommendedSeller()) {
                CalcServer.instance().removeFromRecommendedSellersQueue(post.owner);
            }
    	} catch(Exception e) {
            logger.underlyingLogger().error(e.getMessage(), e);
        }
    }
}
