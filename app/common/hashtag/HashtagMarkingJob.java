package common.hashtag;

import models.Hashtag;
import models.Post;

public interface HashtagMarkingJob {
	
    public void execute(Post post, Hashtag hashtag);
    
}
