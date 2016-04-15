package babybox.events.listener;

import java.util.HashSet;
import java.util.Set;

import mobile.PushNotificationSender;
import models.Activity;
import models.Comment;
import models.Post;
import models.Activity.ActivityType;
import babybox.events.map.CommentEvent;
import babybox.events.map.DeleteCommentEvent;

import com.google.common.eventbus.Subscribe;

import common.cache.CalcServer;
import common.thread.TransactionalRunnableTask;
import common.utils.StringUtil;
import domain.DefaultValues;
import email.SendgridEmailClient;

public class CommentEventListener extends EventListener {
    private static final play.api.Logger logger = play.api.Logger.apply(CommentEventListener.class);
    
	@Subscribe
	public void recordCommentEvent(CommentEvent map){
	    try {
    		final Comment comment = (Comment) map.get("comment");
    		final Post post = (Post) map.get("post");
    		
    		CalcServer.instance().recalcScoreAndAddToCategoryPopularQueue(post);
            
            // firstly, send to post owner
    		final Long postImageId = post.getImage();
            if (comment.owner.id != post.owner.id) {
                executeAsync(
                        new TransactionalRunnableTask() {
                            @Override
                            public void execute() {
                                // activity
                                Activity activity = new Activity(
                                        ActivityType.NEW_COMMENT, 
                                        post.owner.id,
                                        true,
                                        comment.owner.id, 
                                        comment.owner.id,
                                        comment.owner.name,
                                        post.id,
                                        postImageId, 
                                        StringUtil.shortMessage(comment.body));
                                activity.ensureUniqueAndMerge();    // only record latest comment activity from sender
                                
                                // Push notification
                                PushNotificationSender.sendNewCommentNotification(
                                        post.owner.id, 
                                        comment.owner.name,
                                        post.title, 
                                        post.id);
                                
                                // Sendgrid
                                SendgridEmailClient.getInstatnce().sendMailOnComment(comment.owner, post.owner, post.title, comment.body);
                            }
                        });
            }
            
            // fan out to all commenters
            Set<Long> commenterIds = new HashSet<>();
            for (Comment otherComment : post.comments) {
                // 1. skip post owner here, sent already
                // 2. skip comment owner
                // 3. remove duplicates
                if (otherComment.owner.id == post.owner.id || 
                        otherComment.owner.id == comment.owner.id || 
                        commenterIds.contains(otherComment.owner.id)) {
                    continue;
                }
                
                // safety measure, fan out to max N commenters
                if (commenterIds.size() > DefaultValues.ACTIVITY_NEW_COMMENT_MAX_FAN_OUT) {
                    break;
                }
                
                final Long otherCommenterId = otherComment.owner.id;
                executeAsync(
                        new TransactionalRunnableTask() {
                            @Override
                            public void execute() {
                                Activity activity = new Activity(
                                        ActivityType.NEW_COMMENT, 
                                        otherCommenterId,
                                        false, 
                                        comment.owner.id, 
                                        comment.owner.id, 
                                        comment.owner.name,
                                        post.id,
                                        postImageId, 
                                        StringUtil.shortMessage(comment.body));
                                activity.ensureUniqueAndMerge();
                                
                                // Push notification
                                PushNotificationSender.sendNewCommentNotification(
                                        otherCommenterId, 
                                        comment.owner.name,
                                        post.title, 
                                        post.id);
                                
                                // Sendgrid
                                // NOTE: skip email for other commenters
                            }
                        });
                
                commenterIds.add(otherComment.owner.id);
            }
	    } catch(Exception e) {
            logger.underlyingLogger().error(e.getMessage(), e);
        }
	}
	
	@Subscribe
	public void recordDeleteCommentEvent(DeleteCommentEvent map) {
	    try {
    		final Comment comment = (Comment) map.get("comment");
    		final Post post = (Post) map.get("post");
    		
    		CalcServer.instance().recalcScoreAndAddToCategoryPopularQueue(post);
	    } catch(Exception e) {
            logger.underlyingLogger().error(e.getMessage(), e);
        }
	}
}
