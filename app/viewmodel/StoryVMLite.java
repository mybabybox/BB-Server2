package viewmodel;

import com.fasterxml.jackson.annotation.JsonProperty;

import models.Story;
import models.User;

public class StoryVMLite {
	@JsonProperty("id") public Long id;
	@JsonProperty("ownerId") public Long ownerId;
    @JsonProperty("ownerName") public String ownerName;

	@JsonProperty("images") public Long[] images;
	@JsonProperty("hasImage") public Boolean hasImage = false;
	
	@JsonProperty("numLikes") public int numLikes;
	@JsonProperty("numComments") public int numComments;
	@JsonProperty("numViews") public int numViews;
	@JsonProperty("isLiked") public boolean isLiked = false;
    
	// for feed
	@JsonProperty("offset") public Long offset;
	
	// admin fields
	@JsonProperty("baseScore") public Long baseScore = 0L;
	@JsonProperty("timeScore") public Double timeScore = 0D;

	public StoryVMLite(Story story) {
	    this(story, null);
	}
	
    public StoryVMLite(Story story, User user) {
        this.id = story.id;
        this.ownerId = story.owner.id;
        this.ownerName = story.owner.displayName;
        
        Long[] images = story.getImages();
        if (images != null && images.length > 0) {
            this.hasImage = true;
            this.images = images;
        }
        
        this.numLikes = story.numLikes;
        this.numComments = story.numComments;
        this.numViews = story.numViews;
        
        if (user != null) {
            this.isLiked = story.isLikedBy(user);
            
            if (user.isSuperAdmin()) {
                this.baseScore = story.baseScore;
                this.timeScore = story.timeScore;
            }
        }
    }
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }
    
    public int getNumComments() {
        return numComments;
    }

    public void setNumComments(int numComments) {
        this.numComments = numComments;
    }

    public Long[] getImages() {
		return images;
	}
	
    public void setImages(Long[] images) {
		this.images = images;
	}
	
	public Boolean getHasImage() {
		return hasImage;
	}
	
	public void setHasImage(Boolean hasImage) {
		this.hasImage = hasImage;
	}
	
    public int getNumViews() {
        return numViews;
    }

    public void setNumViews(int numViews) {
        this.numViews = numViews;
    }

    public int getNumLikes() {
        return numLikes;
    }

    public void setNumLikes(int numLikes) {
        this.numLikes = numLikes;
    }

    public boolean isLiked() {
        return isLiked;
    }

    public void setIsLiked(boolean isLiked) {
        this.isLiked = isLiked;
    }
}