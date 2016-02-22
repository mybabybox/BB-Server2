package viewmodel;

import java.util.ArrayList;
import java.util.List;

import models.Collection;
import models.Post;

import org.apache.commons.collections.CollectionUtils;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CollectionVM {
    @JsonProperty("id") public Long id;
    @JsonProperty("ownerId") public Long ownerId;
    @JsonProperty("ownerName") public String ownerName;
    @JsonProperty("collectionName") public String collectionName;
    @JsonProperty("productImages") public List<Long> productImages = new ArrayList<>();
	
	//new added from android babybox
    @JsonProperty("name") public String name;
    @JsonProperty("desc") public String desc;
    @JsonProperty("system") public boolean system;
    @JsonProperty("seq") public Long seq;
	
	public CollectionVM() {}

	public CollectionVM(Collection collection) {
		this.ownerId = collection.owner.id;
		this.ownerName = collection.owner.name;
		this.id = collection.id;
		this.collectionName = collection.name;
		for(Post product : collection.products){
			if(product.folder != null && !CollectionUtils.isEmpty(product.folder.resources)) {
				this.productImages.add(product.folder.resources.get(0).getId());
			}
		}
	}

	public Long getProductId() {
		return id;
	}

	public void setProductId(Long productId) {
		this.id = productId;
	}

	public Long getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(Long ownerId) {
		this.ownerId = ownerId;
	}

	public Long getCollectionId() {
		return id;
	}

	public void setCollectionId(Long collectionId) {
		this.id = collectionId;
	}

	public String getOwnerName() {
		return ownerName;
	}

	public void setOwnerName(String ownerName) {
		this.ownerName = ownerName;
	}

	public String getCollectionName() {
		return collectionName;
	}

	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}

	public List<Long> getProductImages() {
		return productImages;
	}

	public void setProductImages(List<Long> productImages) {
		this.productImages = productImages;
	}
}