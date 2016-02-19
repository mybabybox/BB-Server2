package viewmodel;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import domain.DefaultValues;
import models.Comment;
import models.Post;
import models.Story;
import models.User;

public class StoryVM extends StoryVMLite {
	@JsonProperty("createdDate") public Long createdDate;
	@JsonProperty("updatedDate") public Long updatedDate;
	@JsonProperty("ownerNumStories") public Long ownerNumStories;
	@JsonProperty("ownerNumFollowers") public Long ownerNumFollowers;
	@JsonProperty("ownerLastLogin") public Long ownerLastLogin;
	@JsonProperty("body") public String body;
	@JsonProperty("categoryId") public Long categoryId;
	@JsonProperty("categoryName") public String categoryName;
	@JsonProperty("categoryIcon") public String categoryIcon;
	@JsonProperty("categoryType") public String categoryType;	
    @JsonProperty("latestComments") public List<CommentVM> latestComments;
    @JsonProperty("relatedPosts") public List<PostVMLite> relatedPosts;
    
	@JsonProperty("isOwner") public boolean isOwner = false;
	@JsonProperty("isFollowingOwner") public boolean isFollowingOwner = false;
    
	@JsonProperty("deviceType") public String deviceType;
	
    public StoryVM(Story story, User user) {
    	super(story, user);
    	
        this.ownerNumStories = story.owner.numStories;
        this.ownerNumFollowers = story.owner.numFollowers;
        this.ownerLastLogin = 
                story.owner.lastLogin == null? 
                        story.getUpdatedDate().getTime() : story.owner.lastLogin.getTime();
        this.createdDate = story.getCreatedDate().getTime();
        this.updatedDate = story.getUpdatedDate().getTime();
        this.body = story.body;
        
        this.latestComments = new ArrayList<>();
        for (Comment comment : story.getLatestComments(DefaultValues.LATEST_COMMENTS_COUNT)) {
        	this.latestComments.add(new CommentVM(comment, user));
        }
        
        this.relatedPosts = new ArrayList<>();
        for (Post relatedPost : story.getRelatedPosts()) {
            this.relatedPosts.add(new PostVMLite(relatedPost));
        }
        
        this.isOwner = (story.owner.id == user.id);
        this.isFollowingOwner = user.isFollowing(story.owner);
        
        this.deviceType = story.deviceType == null? "" : story.deviceType.name();
    }

    public Long getOwnerNumStories() {
        return ownerNumStories;
    }

    public void setOwnerNumStories(Long ownerNumStories) {
        this.ownerNumStories = ownerNumStories;
    }
    
    public Long getOwnerNumFollowers() {
        return ownerNumFollowers;
    }

    public void setOwnerNumFollowers(Long ownerNumFollowers) {
        this.ownerNumFollowers = ownerNumFollowers;
    }
    
    public Long getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Long createdDate) {
        this.createdDate = createdDate;
    }

    public Long getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(Long updatedDate) {
        this.updatedDate = updatedDate;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public List<CommentVM> getLatestComments() {
		return latestComments;
	}

	public void setLatestComments(List<CommentVM> latestComments) {
		this.latestComments = latestComments;
	}

	public boolean isOwner() {
        return isOwner;
    }

    public void setIsOwner(boolean isOwner) {
        this.isOwner = isOwner;
    }
    
    public boolean isFollowingOwner() {
        return isFollowingOwner;
    }

    public void setIsFollowingOwner(boolean isFollowingOwner) {
        this.isFollowingOwner = isFollowingOwner;
    }
}