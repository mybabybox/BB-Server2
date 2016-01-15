package common.cache;

import models.Hashtag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 */
public class HashtagCache {
    // Permanent cache loaded up on system startup.

    private static List<Hashtag> hashtags;
    private static final Map<Long, Hashtag> idsMap = new HashMap<>();
    private static final Map<String, Hashtag> namesMap = new HashMap<>();
    
    static {
        hashtags = Hashtag.loadHashtags();
        for (Hashtag hashtag : hashtags) {
            idsMap.put(hashtag.id, hashtag);
            namesMap.put(hashtag.name, hashtag);
        }
    }

    public static List<Hashtag> getAllHashtags() {
        return hashtags;
    }
    
    public static Hashtag getHashtag(Long id) {
    	return idsMap.get(id);
    }
    
    public static Hashtag getHashtag(String name) {
        return namesMap.get(name);
    }
}
