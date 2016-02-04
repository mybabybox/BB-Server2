package viewmodel;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import models.User;

public class SellerVM extends UserVMLite {
    @JsonProperty("aboutMe") public String aboutMe;
    @JsonProperty("posts") public List<PostVMLite> posts;
    @JsonProperty("numMoreProducts") public Long numMoreProducts = 0L;

    public SellerVM(User user, User localUser, List<PostVMLite> posts) {
    	super(user, localUser);

    	if (user.userInfo != null) {
    	    this.aboutMe = user.userInfo.aboutMe;
    	}
    	
    	this.posts = posts;
    	if (this.numProducts > this.posts.size()) {
    	    this.numMoreProducts = this.numProducts - this.posts.size();
    	}
    }

    public List<PostVMLite> getPosts() {
        return posts;
    }

    public void setPosts(List<PostVMLite> posts) {
        this.posts = posts;
    }

    public Long getNumMoreProducts() {
        return numMoreProducts;
    }

    public void setNumMoreProducts(Long numMoreProducts) {
        this.numMoreProducts = numMoreProducts;
    }
}

