package viewmodel;

import com.fasterxml.jackson.annotation.JsonProperty;

import models.Settings;
import models.SystemInfo;

public class SettingsVM {
	@JsonProperty("id") public long id;
	@JsonProperty("emailNewPost") public Boolean emailNewPost;
	@JsonProperty("emailNewConversation") public Boolean emailNewConversation;
	@JsonProperty("emailNewComment") public Boolean emailNewComment;
	@JsonProperty("emailNewPromotions") public Boolean emailNewPromotions;
	@JsonProperty("pushNewConversation") public Boolean pushNewConversation;
	@JsonProperty("pushNewComment") public Boolean pushNewComment;
	@JsonProperty("pushNewFollow") public Boolean pushNewFollow;
	@JsonProperty("pushNewFeedback") public Boolean pushNewFeedback;
	@JsonProperty("pushNewPromotions") public Boolean pushNewPromotions;

	@JsonProperty("systemAndroidVersion") public String systemAndroidVersion;
    @JsonProperty("systemIosVersion") public String systemIosVersion;
    
    public SettingsVM(Settings settings) {
        this.id = settings.id;
        this.emailNewPost = settings.emailNewPost;
        this.emailNewConversation = settings.emailNewConversation;
        this.emailNewComment = settings.emailNewComment;
        this.emailNewPromotions = settings.emailNewPromotions;
        this.pushNewConversation = settings.pushNewConversation;
        this.pushNewComment = settings.pushNewComment;
        this.pushNewFollow = settings.pushNewFollow;
        this.pushNewFeedback = settings.pushNewFeedback;
        this.pushNewPromotions = settings.pushNewPromotions;
        
        this.systemAndroidVersion = SystemInfo.getInfo().androidVersion;
        this.systemIosVersion = SystemInfo.getInfo().iosVersion;
    }
}
