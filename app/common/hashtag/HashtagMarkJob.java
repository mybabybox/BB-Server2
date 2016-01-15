package common.hashtag;

import models.Hashtag;
import models.Post;

public interface HashtagMarkJob {
	
    public void execute(Post post, Hashtag hashtag);
    
}
