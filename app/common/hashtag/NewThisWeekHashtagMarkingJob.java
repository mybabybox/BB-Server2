package common.hashtag;

import java.util.Date;

import common.utils.DateTimeUtil;
import models.Hashtag;
import models.Post;

public class NewThisWeekHashtagMarkingJob implements HashtagMarkingJob {
	private static play.api.Logger logger = play.api.Logger.apply(NewThisWeekHashtagMarkingJob.class);
	
	public void execute(Post post, Hashtag hashtag) {
		if (DateTimeUtil.withinAWeek(post.getCreatedDate().getTime(), new Date().getTime())) {
		    logger.underlyingLogger().info("Mark post id="+post.id);
			post.addHashtag(hashtag);
		}
	}
}
