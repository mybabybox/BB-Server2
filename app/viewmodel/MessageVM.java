package viewmodel;

import models.Message;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MessageVM {
	@JsonProperty("id") public Long id;
	@JsonProperty("createdDate") public Long createdDate;
	@JsonProperty("senderId") public Long senderId;
	@JsonProperty("senderName") public String senderName;
	@JsonProperty("receiverId") public Long receiverId;
    @JsonProperty("receiverName") public String receiverName;
	@JsonProperty("body") public String body;
	@JsonProperty("hasImage") public boolean hasImage = false;
	@JsonProperty("image") public Long image = -1L;
	@JsonProperty("system") public boolean system = false;
	
	public MessageVM(Message message) {
		this.id = message.id;
		this.createdDate = message.getCreatedDate().getTime();
		this.senderId = message.sender.id;
		this.senderName = message.sender.name;
        this.receiverId = message.receiver().id;
        this.receiverName = message.receiver().name;
		this.body = message.body;
		this.system = message.system;
		
		Long image = message.getImage();
        if (image != null) {
        	this.hasImage = true;
        	this.image = image;
        }
	}
}
