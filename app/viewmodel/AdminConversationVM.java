package viewmodel;

import models.Conversation;
import models.Post;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AdminConversationVM {
	private static final play.api.Logger logger = play.api.Logger.apply(ConversationVM.class);
	
	@JsonProperty("id") public Long id;
	@JsonProperty("postId") public Long postId;
	@JsonProperty("postImage") public Long postImage;
	@JsonProperty("postTitle") public String postTitle;
	@JsonProperty("postPrice") public Long postPrice;
	@JsonProperty("postSold") public boolean postSold;
	@JsonProperty("user1Id") public Long user1Id;
	@JsonProperty("user1Name") public String user1Name;
	@JsonProperty("user2Id") public Long user2Id;
	@JsonProperty("user2Name") public String user2Name;
	@JsonProperty("lastMessage") public String lastMessage;
	@JsonProperty("lastMessageHasImage") public boolean lastMessageHasImage;
	@JsonProperty("lastMessageDate") public Long lastMessageDate;
	
	public AdminConversationVM(Conversation conversation) {
		Post post = conversation.post;
		this.id = conversation.id;
		this.postId = post.id;
		this.postTitle = post.title;
		this.postPrice = post.price.longValue();
		this.postSold = post.sold;
		this.user1Id = conversation.user1.id;
		this.user1Name = conversation.user1.displayName;
		this.user2Id = conversation.user2.id;
		this.user2Name = conversation.user2.displayName;
		this.lastMessage = conversation.lastMessage;
		this.lastMessageHasImage = conversation.lastMessageHasImage;
		this.lastMessageDate = conversation.lastMessageDate.getTime();
		
		Long[] images = post.getImages();
        if (images != null && images.length > 0) {
        	this.postImage = images[0];
        }
	}
}
