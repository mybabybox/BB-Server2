package viewmodel;

import com.fasterxml.jackson.annotation.JsonProperty;

import models.User;

public class UserVMLite {
	@JsonProperty("id") public Long id;
    @JsonProperty("displayName") public String displayName;
    @JsonProperty("numLikes") public Long numLikes = 0L;
    @JsonProperty("numFollowings") public Long numFollowings = 0L;
    @JsonProperty("numFollowers") public Long numFollowers = 0L;
    @JsonProperty("numProducts") public Long numProducts = 0L;
    @JsonProperty("numComments") public Long numComments = 0L;
    @JsonProperty("numConversationsAsSender") public Long numConversationsAsSender = 0L;
    @JsonProperty("numConversationsAsRecipient") public Long numConversationsAsRecipient = 0L;
    @JsonProperty("numCollections") public Long numCollections = 0L;
    @JsonProperty("isFollowing") public boolean isFollowing = false;

    // admin readyonly fields
    @JsonProperty("createdDate") public Long createdDate;
    @JsonProperty("lastLogin") public Long lastLogin;
    @JsonProperty("totalLogin") public Long totalLogin;
    @JsonProperty("isLoggedIn") public boolean isLoggedIn = false;
    @JsonProperty("isFbLogin") public boolean isFbLogin = false;
    @JsonProperty("emailProvidedOnSignup") public boolean emailProvidedOnSignup = false;
    @JsonProperty("emailValidated") public boolean emailValidated = false;
    @JsonProperty("accountVerified") public boolean accountVerified = false;
    @JsonProperty("newUser") public boolean newUser = false;
    @JsonProperty("isAdmin") public boolean isAdmin = false;

    public UserVMLite(User user, User localUser) {
        this.id = user.id;
        
        if (!user.isLoggedIn()) {
            return;
        }
        
        this.displayName = user.displayName;
        this.numLikes = user.numLikes;
        this.numFollowers = user.numFollowers;
        this.numFollowings = user.numFollowings;
        this.numProducts = user.numProducts;
        this.numComments = user.numComments;
        this.numConversationsAsSender = user.numConversationsAsSender;
        this.numConversationsAsRecipient = user.numConversationsAsRecipient;
        this.numCollections = user.numCollections;
        if (!user.equals(localUser)) {
        	this.isFollowing = user.isFollowedBy(localUser);
        }
        
        this.createdDate = user.getCreatedDate().getTime();
        this.lastLogin = user.lastLogin.getTime();
        this.totalLogin = user.totalLogin;
        this.isLoggedIn = user.isLoggedIn();
        this.isFbLogin = user.fbLogin;
        this.emailProvidedOnSignup = user.emailProvidedOnSignup;
        this.emailValidated = user.emailValidated;
        this.accountVerified = user.accountVerified;
        this.newUser = user.isNewUser();
        this.isAdmin = user.isSuperAdmin();
    }
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Long getNumFollowings() {
        return numFollowings;
    }

    public void setNumFollowings(Long numFollowings) {
        this.numFollowings = numFollowings;
    }

    public Long getNumFollowers() {
        return numFollowers;
    }

    public void setNumFollowers(Long numFollowers) {
        this.numFollowers = numFollowers;
    }

    public Long getNumProducts() {
		return numProducts;
	}

	public void setNumProducts(Long numProducts) {
		this.numProducts = numProducts;
	}

	public void setFollowing(boolean isFollowing) {
		this.isFollowing = isFollowing;
	}

	public Long getNumLikes() {
        return numLikes;
    }

    public void setNumLikes(Long numLikes) {
        this.numLikes = numLikes;
    }

    public Long getNumComments() {
        return numComments;
    }

    public void setNumComments(Long numComments) {
        this.numComments = numComments;
    }
    
    public Long getNumConversationsAsSender() {
        return numConversationsAsSender;
    }

    public void setNumConversationsAsSender(Long numConversationsAsSender) {
        this.numConversationsAsSender = numConversationsAsSender;
    }
    
    public Long getNumConversationsAsRecipient() {
        return numConversationsAsRecipient;
    }

    public void setNumConversationsAsRecipient(Long numConversationsAsRecipient) {
        this.numConversationsAsRecipient = numConversationsAsRecipient;
    }
    
    public Long getNumCollections() {
        return numCollections;
    }

    public void setNumCollections(Long numCollections) {
        this.numCollections = numCollections;
    }

    public boolean isFollowing() {
        return isFollowing;
    }

    public void setIsFollowing(boolean isFollowing) {
        this.isFollowing = isFollowing;
    }
    
    public Long getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Long createdDate) {
        this.createdDate = createdDate;
    }
    
    public Long getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Long lastLogin) {
        this.lastLogin = lastLogin;
    }
    
    public Long getTotalLogin() {
        return totalLogin;
    }

    public void setTotalLogin(Long totalLogin) {
        this.totalLogin = totalLogin;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public void setIsLoggedIn(boolean isLoggedIn) {
        this.isLoggedIn = isLoggedIn;
    }

    public boolean isFbLogin() {
        return isFbLogin;
    }

    public void setIsFbLogin(boolean isFbLogin) {
        this.isFbLogin = isFbLogin;
    }

    public boolean getEmailValidated() {
        return emailValidated;
    }

    public void setEmailValidated(boolean emailValidated) {
        this.emailValidated = emailValidated;
    }

    public boolean getNewUser() {
        return newUser;
    }

    public void setNewUser(boolean newUser) {
        this.newUser = newUser;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }
}
