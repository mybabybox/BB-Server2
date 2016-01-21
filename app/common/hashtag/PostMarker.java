package common.hashtag;

import models.PostToMark;

import java.util.ArrayList;
import java.util.List;
import models.Hashtag;
import models.Post;
import play.db.jpa.JPA;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import akka.event.slf4j.Logger;
import common.schedule.JobScheduler;

public class PostMarker {


	public static void PostMarkerSchedule(){
		try {
			//classloadar to get all loaded classes.
			ClassLoader classLoader = PostToMark.class.getClassLoader();
			Class[] paramString = new Class[2];	
			paramString[0] = Post.class;
			paramString[1] = Hashtag.class;

			for(PostToMark postmark :PostToMark.getAllMarkPost()){
				for(Hashtag hash : Hashtag.getAllEligibleSystemHashtags()){
					try {
						//jobClass name must be qualified name in DB, for eg: common.hashtag.NewThisWeekHashtagMarkingJob
						Class cls = classLoader.loadClass(hash.jobClass);
						Object job = cls.newInstance();
						Method method = cls.getDeclaredMethod("execute", paramString);
						Post post = Post.findById(postmark.postid);
						method.invoke(job, new Object[]{post,hash});
					} catch (InvocationTargetException e) {
						System.out.println(e.getTargetException());
					}
				}
				postmark.delete();
			}
		} catch (Exception e) {
			System.out.println(e);
		}
	}				
}
