package viewmodel;

import com.fasterxml.jackson.annotation.JsonProperty;

import models.Settings;
import models.SystemInfo;

public class SettingsVM {
	@JsonProperty("id") public long id;
	@JsonProperty("emailNewPost") public boolean emailNewPost;
	@JsonProperty("emailNewConversation") public boolean emailNewConversation;
	@JsonProperty("emailNewComment") public boolean emailNewComment;
	@JsonProperty("emailNewPromotions") public boolean emailNewPromotions;
	@JsonProperty("pushNewConversation") public boolean pushNewConversation;
	@JsonProperty("pushNewComment") public boolean pushNewComment;
	@JsonProperty("pushNewFollow") public boolean pushNewFollow;
	@JsonProperty("pushNewFeedback") public boolean pushNewFeedback;
	@JsonProperty("pushNewPromotions") public boolean pushNewPromotions;

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
