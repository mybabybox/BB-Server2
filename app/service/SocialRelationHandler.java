package service;

import models.Category;
import models.Comment;
import models.Conversation;
import models.Message;
import models.Post;
import models.Story;
import models.User;
import babybox.events.handler.EventHandler;
import babybox.events.map.CommentEvent;
import babybox.events.map.ConversationEvent;
import babybox.events.map.DeleteCommentEvent;
import babybox.events.map.DeletePostEvent;
import babybox.events.map.EditPostEvent;
import babybox.events.map.FollowEvent;
import babybox.events.map.LikeEvent;
import babybox.events.map.MessageEvent;
import babybox.events.map.PostEvent;
import babybox.events.map.SoldEvent;
import babybox.events.map.StoryEvent;
import babybox.events.map.TouchEvent;
import babybox.events.map.UnFollowEvent;
import babybox.events.map.UnlikeEvent;
import babybox.events.map.ViewEvent;

public class SocialRelationHandler {
	
	public static void recordLikePost(Post post, User localUser) {
		LikeEvent likeEvent = new LikeEvent();
		likeEvent.put("user", localUser);
		likeEvent.put("post", post);
		EventHandler.getInstance().getEventBus().post(likeEvent);
	}
	
	public static void recordUnLikePost(Post post, User localUser) {
		UnlikeEvent likeEvent = new UnlikeEvent();
		likeEvent.put("user", localUser);
		likeEvent.put("post", post);
		EventHandler.getInstance().getEventBus().post(likeEvent);
	}

	public static void recordNewPost(Post post, User localUser) {
		PostEvent postEvent = new PostEvent();
		postEvent.put("user", localUser);
		postEvent.put("post", post);
		EventHandler.getInstance().getEventBus().post(postEvent);
	}
	
	public static void recordEditPost(Post post, Category category) {
		EditPostEvent postEvent = new EditPostEvent();
		postEvent.put("post", post);
		postEvent.put("category", category);
		EventHandler.getInstance().getEventBus().post(postEvent);
	}
	
	public static void recordDeletePost(Post post) {
        DeletePostEvent postEvent = new DeletePostEvent();
        postEvent.put("post", post);
        EventHandler.getInstance().getEventBus().post(postEvent);
    }
	
	public static void recordNewStory(Story story, User localUser) {
	    StoryEvent storyEvent = new StoryEvent();
	    storyEvent.put("user", localUser);
	    storyEvent.put("story", story);
        EventHandler.getInstance().getEventBus().post(storyEvent);
    }
    
    public static void recordEditStory(Story story) {
        EditPostEvent postEvent = new EditPostEvent();
        postEvent.put("story", story);
        EventHandler.getInstance().getEventBus().post(postEvent);
    }
    
    public static void recordDeleteStory(Story story) {
        DeletePostEvent postEvent = new DeletePostEvent();
        postEvent.put("story", story);
        EventHandler.getInstance().getEventBus().post(postEvent);
    }
	
	public static void recordFollowUser(User localUser, User user) {
		FollowEvent followEvent = new FollowEvent();
		followEvent.put("localUser", localUser);
		followEvent.put("user", user);
		EventHandler.getInstance().getEventBus().post(followEvent);
	}
	
	public static void recordUnFollowUser(User localUser, User user) {
		UnFollowEvent followEvent = new UnFollowEvent();
		followEvent.put("localUser", localUser);
		followEvent.put("user", user);
		EventHandler.getInstance().getEventBus().post(followEvent);
	}
	
	public static void recordNewComment(Comment comment, Post post) {
		CommentEvent commentEvent = new CommentEvent();
		commentEvent.put("comment", comment);
		commentEvent.put("post", post);
		EventHandler.getInstance().getEventBus().post(commentEvent);
	}
	
	public static void recordDeleteComment(Comment comment, Post post) {
		DeleteCommentEvent commentEvent = new DeleteCommentEvent();
		commentEvent.put("comment", comment);
		commentEvent.put("post", comment.getPost());
		EventHandler.getInstance().getEventBus().post(commentEvent);
	}
	
	public static void recordSoldPost(Post post, User localUser) {
		SoldEvent soldEvent = new SoldEvent();
		soldEvent.put("post", post);
		soldEvent.put("user", localUser);
		EventHandler.getInstance().getEventBus().post(soldEvent);
	}
	
	public static void recordNewConversation(Conversation conversation, Post post) {
	    ConversationEvent conversationEvent = new ConversationEvent();
	    conversationEvent.put("conversation", conversation);
	    conversationEvent.put("post", post);
        EventHandler.getInstance().getEventBus().post(conversationEvent);
	}
	
	public static void recordNewMessage(Message message, User sender, User recipient, Boolean notify) {
        MessageEvent messageEvent = new MessageEvent();
        messageEvent.put("message", message);
        messageEvent.put("sender", sender);
        messageEvent.put("recipient", recipient);
        messageEvent.put("notify", notify);
        EventHandler.getInstance().getEventBus().post(messageEvent);
    }
	
	public static void recordViewPost(Post post, User localUser) {
		ViewEvent viewEvent = new ViewEvent();
		viewEvent.put("post", post);
		viewEvent.put("user", localUser);
		EventHandler.getInstance().getEventBus().post(viewEvent);
	}
	
	public static void recordTouchPost(Post post, User localUser) {
	    TouchEvent touchEvent = new TouchEvent();
	    touchEvent.put("post", post);
	    touchEvent.put("user", localUser);
        EventHandler.getInstance().getEventBus().post(touchEvent);
    }
}
