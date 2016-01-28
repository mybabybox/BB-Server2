package common.cache;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import play.Play;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.google.inject.Singleton;
import common.serialize.JsonSerializer;

@Singleton
public class JedisCache {
    private static final play.api.Logger logger = play.api.Logger.apply(JedisCache.class);
    
    private static final String SYS_PREFIX = Play.application().configuration().getString("keyprefix", "prod_");
    
    // All Redis Cache Key Prefix
    public static final String USER_POSTS_PREFIX = SYS_PREFIX + "user_posts_";
    public static final String USER_FOLLOWERS_PREFIX = SYS_PREFIX + "user_followers_";
    public static final String USER_FOLLOWINGS_PREFIX = SYS_PREFIX + "user_followings_";
    public static final String CATEGORY_POPULAR_PREFIX = SYS_PREFIX + "category_popular_";
    public static final String CATEGORY_NEWEST_PREFIX = SYS_PREFIX + "category_newest_";
    public static final String CATEGORY_PRICE_LOW_HIGH_PREFIX = SYS_PREFIX + "category_price_low_high_";
    public static final String CATEGORY_PRICE_HIGH_LOW_PREFIX = SYS_PREFIX + "category_price_high_low_";
    
    //private static final Double POS_INF = 99999999999999999999.9;
    //private static final Double NEG_INF = -99999999999999999999.9;
    
    private static final Double POS_INF = Double.POSITIVE_INFINITY;
    private static final Double NEG_INF = Double.NEGATIVE_INFINITY;
    
    @Inject
    JedisPool jedisPool;
    
    public enum Status {
        OK,
        ERROR
    }
    
    public void putObj(String key, Object object) {
        putObj(key, object, -1);
    }
    
    public void putObj(String key, Object object, int expire) {
        String json = JsonSerializer.serialize(object);
        put(key, json, expire);
    }
    
    public Object getObj(String key, Class<?> clazz) {
        Object object = null;
        String json = get(key);
        if (json == null) {
            return null;
        } else {
            object = JsonSerializer.deserialize(json, clazz);
        }
        return object;
    }
    
    public Status put(String key, String value) {
        return put(key, value, -1);
    }
    
    public Status put(String key, String value, int expire) {
        Jedis j = null;
        try {
            j = getResource();
            String ret = j.set(key, value);
            if (!"OK".equalsIgnoreCase(ret)) {
                logger.underlyingLogger().error(ret);
                return Status.ERROR;
            }
            if (expire != -1) {
                expire(key, expire);
            }
            return Status.OK;
        } finally {
            returnResource(j);
        }
    }

    public String get(String key) {
        Jedis j = null;
        try {
            j = getResource();
            if (!j.exists(key)) {
                return null;
            }
            String value = j.get(key);
            if ("".equals(value.trim())) {
                j.del(key);     // del key with invalid value
                return null;
            }
            return value;
        } finally {
            returnResource(j);
        }
    }

    ////////////////////////////////////////
    // Set operations
    public Status putToSet(String key, String value) {
        Jedis j = null;
        try {
            j = getResource();
            j.sadd(key, value);
            return Status.OK;
        } finally {
            returnResource(j);
        }
    }

    public Set<String> getSetMembers(String key) {
        Jedis j = null;
        try {
            j = getResource();
            return j.smembers(key);
        } finally {
            returnResource(j);
        }
    }

    public boolean isMemberOfSet(String key, String value) {
        Jedis j = null;
        try {
            j = getResource();
            return j.sismember(key, value);
        } finally {
            returnResource(j);
        }
    }

    public void removeMemberFromSet(String key, String value) {
        Jedis j = null;
        try {
            j = getResource();
            j.srem(key, value);
        } finally {
            returnResource(j);
        }
    }
    ////////////////////////////////////////
    
    // Set operations
    public Status putToSortedSet(String key, double score, String member) {
        Jedis j = null;
        try {
            j = getResource();
            j.zadd(key, score, member);
            return Status.OK;
        } finally {
            returnResource(j);
        }
    }
    
    public Status removeMemberFromSortedSet(String key, String member) {
        Jedis j = null;
        try {
            j = getResource();
            j.zrem(key, member);
            return Status.OK;
        } finally {
            returnResource(j);
        }
    }
    
    public Status putMapToSortedSet(String key, Map<String, Double> value) {
        Jedis j = null;
        try {
            j = getResource();
            //j.zadd(key, value);
            return Status.OK;
        } finally {
            returnResource(j);
        }
    }
    
    public Set<String> getSortedSetAsc(String key, Double min) {
        Jedis j = null;
        try {
            j = getResource();
            return j.zrangeByScore(key, ++min, POS_INF, 0, CalcServer.FEED_RETRIEVAL_COUNT);
        } finally {
            returnResource(j);
        }
    }
    
    public Set<String> getSortedSetDsc(String key, Double max) {
        Jedis j = null;
        try {
            j = getResource();
            if(max == 0){
                max = POS_INF;
            }
            return j.zrevrangeByScore(key, --max, NEG_INF, 0, CalcServer.FEED_RETRIEVAL_COUNT); 
        } finally {
            returnResource(j);
        }
    }
    
    public Set<String> getSortedSetDsc(String key) {
        return getSortedSetDsc(key, 0L, -1L);
    }
    
    public Set<String> getSortedSetDsc(String key, long start, long end) {
        Jedis j = null;
        try {
            j = getResource();
            return j.zrevrange(key, start, end);     // end is inclusive
        } finally {
            returnResource(j);
        }
    }
    
    public Double getScore(String key, String member) {
        Jedis j = null;
        try {
            j = getResource();
            return j.zscore(key, member); 
        } finally {
            returnResource(j);
        }
    }
    
    public boolean isMemberOfSortedSet(String key, String value) {
        return getScore(key, value) != null;
    }
    
    /////////////
    

    public boolean exists(String key) {
        Jedis j = null;
        try {
            j = getResource();
            return j.exists(key);
        } finally {
            returnResource(j);
        }
    }
    
    public long remove(String key) {
        Jedis j = null;
        try {
            j = getResource();
            return j.del(key);
        } finally {
            returnResource(j);
        }
    }
    
    public long expire(String key, int secs) {
        Jedis j = null;
        try {
            j = getResource();
            return j.expire(key, secs);
        } finally {
            returnResource(j);
        }
    }
    
    public void flushAll() {
        Jedis j = null;
        try {
            j = getResource();
            j.flushAll();
        } finally {
            returnResource(j);
        }
    }
    
    public Jedis getResource() {
        return jedisPool.getResource();
    }
    
    private void returnResource(Jedis j) {
        if (j != null) {
            jedisPool.returnResource(j);
        }
    }
}
