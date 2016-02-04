package viewmodel;

import com.fasterxml.jackson.annotation.JsonProperty;

import models.User;

public class UserVMLite {
	@JsonProperty("id") public Long id;
    @JsonProperty("displayName") public String displayName;
    @JsonProperty("firstName") public String firstName;
    @JsonProperty("lastName") public String lastName;
    @JsonProperty("email") public String email;
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
    @JsonProperty("lastLoginUserAgent") public String lastLoginUserAgent;
    @JsonProperty("totalLogin") public Long totalLogin;
    @JsonProperty("isLoggedIn") public boolean isLoggedIn = false;
    @JsonProperty("isFbLogin") public boolean isFbLogin = false;
    @JsonProperty("emailProvidedOnSignup") public boolean emailProvidedOnSignup = false;
    @JsonProperty("emailValidated") public boolean emailValidated = false;
    @JsonProperty("accountVerified") public boolean accountVerified = false;
    @JsonProperty("newUser") public boolean newUser = false;
    @JsonProperty("isAdmin") public boolean isAdmin = false;
    @JsonProperty("isPromotedSeller") public boolean isPromotedSeller = false;
    @JsonProperty("isVerifiedSeller") public boolean isVerifiedSeller = false;
    @JsonProperty("isRecommendedSeller") public boolean isRecommendedSeller = false;
    
    public UserVMLite(User user, User localUser) {
        this.id = user.id;
        
        // Fill up below for logged in users only
        this.isLoggedIn = user.isLoggedIn();
        if (!user.isLoggedIn()) {
            return;
        }
        
        this.displayName = user.displayName;
        this.email = user.email;
        this.firstName = user.firstName;
        this.lastName = user.lastName;
        this.numLikes = user.numLikes;
        this.numFollowers = user.numFollowers;
        this.numFollowings = user.numFollowings;
        this.numProducts = user.numProducts;
        this.numComments = user.numComments;
        this.numConversationsAsSender = user.numConversationsAsSender;
        this.numConversationsAsRecipient = user.numConversationsAsRecipient;
        this.numCollections = user.numCollections;
        
        if (localUser != null && !user.equals(localUser)) {
        	this.isFollowing = user.isFollowedBy(localUser);
        }
        
        this.createdDate = user.getCreatedDate().getTime();
        this.lastLogin = user.lastLogin.getTime();
        this.lastLoginUserAgent = user.lastLoginUserAgent;
        this.totalLogin = user.totalLogin;
        this.isFbLogin = user.fbLogin;
        this.emailProvidedOnSignup = user.emailProvidedOnSignup;
        this.emailValidated = user.emailValidated;
        this.accountVerified = user.accountVerified;
        this.newUser = user.isNewUser();
        this.isAdmin = user.isSuperAdmin();
        this.isPromotedSeller = user.isPromotedSeller();
        this.isVerifiedSeller = user.isVerifiedSeller();
        this.isRecommendedSeller = user.isRecommendedSeller();
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

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public boolean isPromotedSeller() {
        return isPromotedSeller;
    }

    public void setIsPromotedSeller(boolean isPromotedSeller) {
        this.isPromotedSeller = isPromotedSeller;
    }

    public boolean isVerifiedSeller() {
        return isVerifiedSeller;
    }

    public void setIsVerifiedSeller(boolean isVerifiedSeller) {
        this.isVerifiedSeller = isVerifiedSeller;
    }
    
    public boolean isRecommendedSeller() {
        return isRecommendedSeller;
    }
    
    public void setIsRecommendedSeller(boolean isRecommendedSeller) {
        this.isRecommendedSeller = isRecommendedSeller;
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
