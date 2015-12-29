package viewmodel;

import com.fasterxml.jackson.annotation.JsonProperty;

import models.Activity;

public class ActivityVM {
    @JsonProperty("id") public Long id;
    @JsonProperty("createdDate") public Long createdDate;
    @JsonProperty("activityType") public String activityType;
    @JsonProperty("userIsOwner") public Boolean userIsOwner;
    @JsonProperty("actor") public Long actor;
    @JsonProperty("actorImage") public Long actorImage;
    @JsonProperty("actorName") public String actorName;
    @JsonProperty("actorType") public String actorType;
    @JsonProperty("target") public Long target;
    @JsonProperty("targetImage") public Long targetImage;
    @JsonProperty("targetName") public String targetName;
    @JsonProperty("targetType") public String targetType;
    @JsonProperty("viewed") public Boolean viewed;
	
	public ActivityVM() {}
	
	public ActivityVM(Activity activity) {
		this.id = activity.id;
		this.createdDate = activity.getCreatedDate().getTime();
		this.activityType = activity.activityType.name();
		this.userIsOwner = activity.userIsOwner;
        this.actor = activity.actor;
        this.actorImage = activity.actorImage;
        this.actorName = activity.actorName;
        if (activity.actorType != null) {
            this.actorType = activity.actorType.name();
        }
        this.target = activity.target;
        this.targetImage = activity.targetImage;
        this.targetName = activity.targetName;
        if (activity.targetType != null) {
            this.targetType = activity.targetType.name();
        }
        this.viewed = activity.viewed;
	}
}
