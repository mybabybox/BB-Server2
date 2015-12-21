package viewmodel;

import com.fasterxml.jackson.annotation.JsonProperty;

import controllers.Application;
import models.Setting;
import models.User;

public class UserVM extends UserVMLite {
    @JsonProperty("aboutMe") public String aboutMe;
    @JsonProperty("location") public LocationVM location;
    @JsonProperty("setting") public SettingVM setting;
    @JsonProperty("isMobile") public boolean isMobile = false;

    public UserVM(User user) {
    	this(user, user);
    }
    
    public UserVM(User user, User localUser) {
    	super(user, localUser);
    	
    	// Fill up below for logged in users only
    	if (!user.isLoggedIn()) {
    	    return;
    	}
    	
        if (user.userInfo != null) {
            this.aboutMe = user.userInfo.aboutMe;
            this.location = new LocationVM(user.userInfo.location);
            if (user.id == localUser.id) {
                this.setting = new SettingVM(Setting.findByUserId(user.id));
            }
        }
        this.isMobile = Application.isMobileUser();
    }

    public String getAboutMe() {
        return aboutMe;
    }

    public void setAboutMe(String aboutMe) {
        this.aboutMe = aboutMe;
    }

    public LocationVM getLocation() {
        return location;
    }

    public void setLocation(LocationVM location) {
        this.location = location;
    }

    @Override
    public String toString() {
        return "id=" + id + "\n" +
                "name=" + lastName + " " + firstName + "\n" +
                "email=" + email + "\n" +
                "emailProvidedOnSignup=" + emailProvidedOnSignup + "\n" +
                "emailValidated=" + emailValidated + "\n" +
                "accountVerified=" + accountVerified + "\n" +
                "fbLogin=" + isFbLogin + "\n" +
                "signupDate=" + createdDate + "\n" +
                "lastLogin=" + lastLogin + "\n" +
                "totalLogin=" + totalLogin + "\n" +
                "numLikes=" + numLikes + "\n" +
                "numFollowers=" + numFollowers + "\n" +
                "numFollowings=" + numFollowings + "\n" +
                "numProducts=" + numProducts + "\n" +
                "numComments=" + numComments + "\n" +
                "numConversationsAsSender=" + numConversationsAsSender + "\n" +
                "numConversationsAsRecipient=" + numConversationsAsRecipient;
    }
}

