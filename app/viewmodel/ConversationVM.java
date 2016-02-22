package viewmodel;

import models.Conversation;
import models.Conversation.OrderTransactionState;
import models.ConversationOrder;
import models.Post;
import models.User;

import com.fasterxml.jackson.annotation.JsonProperty;

import domain.HighlightColor;

public class ConversationVM {
	private static final play.api.Logger logger = play.api.Logger.apply(ConversationVM.class);
	
	@JsonProperty("id") public Long id;
	@JsonProperty("postId") public Long postId;
	@JsonProperty("postImage") public Long postImage;
	@JsonProperty("postTitle") public String postTitle;
	@JsonProperty("postPrice") public double postPrice;
	@JsonProperty("postOwner") public boolean postOwner;
	@JsonProperty("postSold") public boolean postSold;
	@JsonProperty("userId") public Long userId;
	@JsonProperty("userName") public String userName;
	@JsonProperty("lastMessage") public String lastMessage;
	@JsonProperty("lastMessageHasImage") public boolean lastMessageHasImage;
	@JsonProperty("lastMessageDate") public Long lastMessageDate;
	@JsonProperty("unread") public Long unread = 0L;
	@JsonProperty("order") public ConversationOrderVM order;
	
	// seller
	@JsonProperty("note") public String note;
	@JsonProperty("orderTransactionState") public String orderTransactionState;
    @JsonProperty("highlightColor") public String highlightColor;
	
	public ConversationVM(Conversation conversation, User localUser) {
		User otherUser = conversation.otherUser(localUser);
		Post post = conversation.post;
		this.id = conversation.id;
		this.postId = post.id;
		this.postTitle = post.title;
		this.postPrice = post.price;
		this.postOwner = post.owner.id == localUser.id;
		this.postSold = post.sold;
		this.userId = otherUser.id;
		this.userName = otherUser.displayName;
		this.lastMessage = conversation.lastMessage;
		this.lastMessageHasImage = conversation.lastMessageHasImage;
		this.lastMessageDate = conversation.lastMessageDate.getTime();
		this.unread = conversation.getUnreadCount(localUser);
		
		// Seller order management
		if (this.postOwner) {
		    this.note = conversation.note;
		    this.orderTransactionState = conversation.orderTransactionState.name();
		    this.highlightColor = conversation.highlightColor.name();
		} else {
		    this.note = "";
		    this.orderTransactionState = OrderTransactionState.NA.name();
            this.highlightColor = HighlightColor.NONE.name();
		}
		
		// Last active order
		ConversationOrder order = ConversationOrder.getActiveOrder(conversation);
		if (order != null) {
		    this.order = new ConversationOrderVM(order, localUser);		    
		}
		
		Long[] images = post.getImages();
        if (images != null && images.length > 0) {
        	this.postImage = images[0];
        }
	}
}
