package viewmodel;

import com.fasterxml.jackson.annotation.JsonProperty;

import models.Settings;
import models.User;

public class UserVM extends UserVMLite {
    @JsonProperty("aboutMe") public String aboutMe;
    @JsonProperty("location") public LocationVM location;
    @JsonProperty("settings") public SettingsVM settings;

    public UserVM(User user) {
    	this(user, null);
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
            if (localUser == null || user.id == localUser.id) {
                this.settings = new SettingsVM(Settings.findByUserId(user.id));
            }
        }
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
}

