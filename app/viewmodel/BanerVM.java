package viewmodel;

import com.fasterxml.jackson.annotation.JsonProperty;

import models.Category;

public class BanerVM {
	@JsonProperty("link") public String link;
	@JsonProperty("imageUrl") public String imageUrl;

    public BanerVM() {
    }

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}
}
