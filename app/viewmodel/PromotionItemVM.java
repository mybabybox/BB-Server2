package viewmodel;

import com.fasterxml.jackson.annotation.JsonProperty;

import models.PromotionItem;

public class PromotionItemVM {
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
	
	public PromotionItemVM(PromotionItem promotionItem) {
        this.id = promotionItem.id;
        if (promotionItem.getCreatedDate() != null) {
            this.createdDate = promotionItem.getCreatedDate().getTime();
        }
        this.itemType = promotionItem.itemType.name();
        this.name = promotionItem.name;
        this.description = promotionItem.description;
        this.image = promotionItem.image;
        this.seq = promotionItem.seq;
        
        this.destinationType = promotionItem.destinationType.name();
        this.destinationObjId = promotionItem.destinationObjId;
        this.destinationObjName = promotionItem.destinationObjName;
    }
}