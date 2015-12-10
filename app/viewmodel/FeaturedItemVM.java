package viewmodel;

import com.fasterxml.jackson.annotation.JsonProperty;

import models.FeaturedItem;

public class FeaturedItemVM {
    @JsonProperty("id") public Long id;
    @JsonProperty("createdDate") public Long createdDate;
	@JsonProperty("itemType") public String itemType;
	@JsonProperty("name") public String name;
	@JsonProperty("description") public String description;
	@JsonProperty("image") public String image;
	@JsonProperty("seq") public int seq;
	
	@JsonProperty("destinationType") public String destinationType;
	@JsonProperty("destinationObjId") public Long destinationObjId;
	@JsonProperty("destinationObjName") public String destinationObjName;
	
	public FeaturedItemVM(FeaturedItem featuredItem) {
        this.id = featuredItem.id;
        if (featuredItem.getCreatedDate() != null) {
            this.createdDate = featuredItem.getCreatedDate().getTime();
        }
        this.itemType = featuredItem.itemType.name();
        this.name = featuredItem.name;
        this.description = featuredItem.description;
        this.image = featuredItem.image;
        this.seq = featuredItem.seq;
        
        this.destinationType = featuredItem.destinationType.name();
        this.destinationObjId = featuredItem.destinationObjId;
        this.destinationObjName = featuredItem.destinationObjName;
    }
}