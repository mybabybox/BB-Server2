package common.hashtag;

import java.util.Date;

import common.utils.DateTimeUtil;
import models.Hashtag;
import models.Post;

public class NewThisWeekHashtagMarkJob implements HashtagMarkJob {
	
    public void execute(Post post, Hashtag hashtag) {
        if (DateTimeUtil.withinAWeek(post.getCreatedDate().getTime(), new Date().getTime())) {
            post.addHashtag(hashtag);    
        }
    }
    
}
