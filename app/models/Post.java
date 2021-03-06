package models;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.NoResultException;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Query;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import models.Country.CountryCode;
import play.db.jpa.JPA;
import babybox.shopping.social.exception.SocialObjectNotCommentableException;

import com.fasterxml.jackson.annotation.JsonIgnore;

import common.cache.CalcServer;
import common.utils.StringUtil;
import controllers.Application.DeviceType;
import domain.Commentable;
import domain.DefaultValues;
import domain.Likeable;
import domain.SocialObjectType;

/**
 * ALTER TABLE Post CHANGE COLUMN noOfComments numComments int(11);
 * ALTER TABLE Post CHANGE COLUMN noOfLikes numLikes int(11);
 * ALTER TABLE Post CHANGE COLUMN noOfBuys numBuys int(11);
 * ALTER TABLE Post CHANGE COLUMN noOfViews numViews int(11);
 * ALTER TABLE Post CHANGE COLUMN noOfChats numChats int(11);
 * 
 * @author keithlei
 */
@Entity
public class Post extends SocialObject implements Likeable, Commentable {
	private static final play.api.Logger logger = play.api.Logger.apply(Post.class);
	
	public String title;

	@Column(length=2000)
	public String body;

	@ManyToOne(cascade = CascadeType.REMOVE)
	public Folder folder;

	@ManyToOne(cascade = CascadeType.REMOVE)
	public Collection collection;

	@ManyToOne
	public Category category;

	@Enumerated(EnumType.STRING)
    public ConditionType conditionType;
	
	@OneToMany(cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
	@OrderBy("CREATED_DATE")
	@JsonIgnore
	public List<Comment> comments;

	public Double price = 0.0;

	@Column(nullable = false, columnDefinition = "TINYINT(1)")
	public boolean sold = false;
	
	@Column(nullable = false, columnDefinition = "TINYINT(1)")
	public boolean soldMarked = false;
	
	@Temporal(TemporalType.TIMESTAMP)
    public Date soldDate;
    
	// Seller fields
	public Double originalPrice = 0.0;
	
	@Column(nullable = false, columnDefinition = "TINYINT(1)")
	public boolean freeDelivery = false;
	
	@Enumerated(EnumType.ORDINAL)
	public Country.CountryCode countryCode = Country.CountryCode.NA;
	
	public int numViews = 0;
	public int numComments = 0;
	public int numLikes = 0;
	public int numConversations = 0;
	public int numBuys = 0;
	
	public Long baseScoreAdjust = 0L;
	public Long baseScore = 0L;
	public Double timeScore = 0D;

	@OneToMany
    public List<Hashtag> hashtags = new ArrayList<>();
	
	public DeviceType deviceType;
	
	public static enum ConditionType {
	    NEW_WITH_TAG,
	    NEW_WITHOUT_TAG,
	    USED
	}
	
	/**
	 * Ctor
	 */
	public Post() {}

	public Post(User owner, String body, Category category, DeviceType deviceType) {
		this.owner = owner;
		this.body = body;
		this.category = category;
		this.price = 0.0;
		this.objectType = SocialObjectType.POST;
		this.deviceType = deviceType;
	}

	public Post(User owner, String title, String body, Category category, Double price, ConditionType conditionType, 
	        Double originalPrice, boolean freeDelivery, Country.CountryCode countryCode, DeviceType deviceType) {
		this.owner = owner;
		this.title = title;
		this.body = body;
		this.category = category;
		this.price = price;
		this.conditionType = conditionType;
		this.originalPrice = originalPrice;
		this.freeDelivery = freeDelivery;
		this.countryCode = countryCode;
		this.objectType = SocialObjectType.POST;
		this.deviceType = deviceType;
	}

	public boolean isNewCondition() {
        return conditionType == null ||    // should never happens...
                ConditionType.NEW_WITH_TAG.equals(conditionType) || 
                ConditionType.NEW_WITHOUT_TAG.equals(conditionType);
                
    }
	
    public boolean hasHashtag(Hashtag hashtag) {
        return hashtags.contains(hashtag);
    }
    
    public List<Hashtag> getHashtags() {
        return hashtags;
    }
    
    public void addHashtag(Hashtag hashtag) {
        if (hasHashtag(hashtag)) {
            return;
        }
        hashtags.add(hashtag);
    }
	
	public static ConditionType parseConditionType(String conditionType) {
        try {
            return Enum.valueOf(ConditionType.class, conditionType);
        } catch (Exception e) {
            return null;
        }
    }
	
	public static CountryCode parseCountryCode(String countryCode) {
        try {
            return Enum.valueOf(CountryCode.class, countryCode);
        } catch (Exception e) {
            return CountryCode.NA;
        }
    }
	
	@Override
	public boolean onLikedBy(User user) {
	    if (logger.underlyingLogger().isDebugEnabled()) {
            logger.underlyingLogger().debug("[p="+this.id+"][u="+user.id+"] onLikeBy");
        }
	    
	    if (!user.isLoggedIn()) {
            return false;
        }
	    
		if (!isLikedBy(user)) {
			boolean liked = recordLike(user);
			if (liked) {
				this.numLikes++;
				user.numLikes++;
			} else {
				logger.underlyingLogger().debug(String.format("Post [p=%d] already liked by User [u=%d]", this.id, user.id));
			}
			return liked;
		}
		return false;
	}

	@Override
	public boolean onUnlikedBy(User user) {
	    if (logger.underlyingLogger().isDebugEnabled()) {
            logger.underlyingLogger().debug("[p="+this.id+"][u="+user.id+"] onUnlikeBy");
        }
	    
	    if (!user.isLoggedIn()) {
            return false;
        }
	    
		if (isLikedBy(user)) {
			boolean unliked = 
					LikeSocialRelation.unlike(
							user.id, SocialObjectType.USER, this.id, SocialObjectType.POST);
			if (unliked) {
			    if (this.numLikes > 0) {
			        this.numLikes--;
			    }
			    if (user.numLikes > 0) {
			        user.numLikes--;
			    }
			} else {
				logger.underlyingLogger().debug(String.format("Post [p=%d] already unliked by User [u=%d]", this.id, user.id));
			}
			return unliked;
		}
		return false;
	}
	
	@Override
	public boolean isLikedBy(User user){
		return CalcServer.instance().isLiked(user.id, this.id);
	}

	@Override
	public void save() {
		super.save();
	}

	public List<Comment> getLatestComments(int count) {
		int start = Math.max(0, comments.size() - count);
		int end = comments.size();
		return comments.subList(start, end);
	}
	
	public List<Comment> getPostComments(Long offset) {
		double maxOffset = Math.floor((double) comments.size() / (double) DefaultValues.DEFAULT_INFINITE_SCROLL_COUNT);
		if (offset > maxOffset) {
			return new ArrayList<>();
		}
		
		int start = Long.valueOf(offset).intValue() * DefaultValues.DEFAULT_INFINITE_SCROLL_COUNT;
		int end = Math.min(start+DefaultValues.DEFAULT_INFINITE_SCROLL_COUNT, comments.size());
		return comments.subList(start, end);
	}
	
	public List<Comment> getComments() {
		return comments;
	}

	public void setComments(List<Comment> comments) {
		this.comments = comments;
	}

	public Resource addPostPhoto(File source) throws IOException {
		ensureAlbumExist();
		Resource photo = this.folder.addFile(source, SocialObjectType.POST_PHOTO);
		photo.save();
		return photo;
	}

	public void ensureAlbumExist() {
		if (this.folder == null) {
			this.folder = Folder.createFolder(this.owner, "post-ps", "", true);
			this.merge();
		}
	}

	public Long getImage() {
	    Long[] images = getImages();
        if (images != null && images.length > 0) {
            return images[0];
        }
        return null;
	}
	
	public Long[] getImages() {
	    return Folder.getResources(folder);
	}
	
	///////////////////// Query APIs /////////////////////
	public static Post findById(Long id) {
		try {
			Query q = JPA.em().createQuery("SELECT p FROM Post p where id = ?1 and deleted = false");
			q.setParameter(1, id);
			return (Post) q.getSingleResult();
		} catch (NoResultException nre) {
			return null;
		}
	}

	public static List<Post> getEligiblePostsForFeeds() {
		try {
			Query q = JPA.em().createQuery("SELECT p FROM Post p where deleted = false");
			return (List<Post>) q.getResultList();
		} catch (NoResultException nre) {
			return null;
		}
	}

	public static List<Post> getUserPosts(Long id) {
		try {
			Query q = JPA.em().createQuery("SELECT p FROM Post p where owner = ?1 and deleted = false");
			q.setParameter(1, User.findById(id));
			return (List<Post>) q.getResultList();
		} catch (NoResultException nre) {
			return null;
		}
	}

	@Override
	public SocialObject onComment(User user, String body) 
			throws SocialObjectNotCommentableException {
		
		Comment comment = new Comment(this, user, body);
		comment.save();

		// merge into Post
		if (comments == null) {
			comments = new ArrayList<>();
		}
		
		this.comments.add(comment);
		this.numComments++;
		this.merge();

        user.numComments++;
        recordCommentProduct(user, comment);
        
        return comment;
	}

	@Override
	public void onDeleteComment(User user, Comment comment)
			throws SocialObjectNotCommentableException {
		
        this.comments.remove(comment);
        comment.deleted = true;
        comment.deletedBy = user;
        comment.save();
        
        if (user.numComments > 0) {
            user.numComments--;
        }
        
        if (this.numComments > 0) {
            this.numComments--;
        }
	}

	public boolean onSold(User user) {
	    if (!user.isLoggedIn()) {
            return false;
        }
	    
	    if (this.sold) {
	        return false;
	    }
	    
		this.sold = true;
		this.soldDate = new Date();
		this.save();
		return true;
	}
	
	public boolean onView(User user) {
	    if (!user.isLoggedIn()) {
            return false;
        }
	    
		boolean viewed = recordView(user);
		if (viewed) {
			this.numViews++;
		}
		return viewed;
	}

	public static List<Post> getPostsByCategory(Category category) {
		try {
			Query q = JPA.em().createQuery("SELECT p FROM Post p where category = ?1 and deleted = false");
			q.setParameter(1,category);
			return (List<Post>) q.getResultList();
		} catch (NoResultException nre) {
			return null;
		}
	}

	public static List<Post> getPosts(List<Long> ids) {
		try {
			 Query query = JPA.em().createQuery(
			            "select p from Post p where "+
			            "p.id in ("+StringUtil.collectionToString(ids, ",")+") and "+
			            "p.deleted = false ORDER BY FIELD(p.id,"+StringUtil.collectionToString(ids, ",")+")");
			 return (List<Post>) query.getResultList();
		} catch (NoResultException nre) {
			return null;
		}
	}
	
	public static List<Post> getPosts(List<Long> ids, int offset) {
		try {
			 Query query = JPA.em().createQuery(
					 "select p from Post p where "+
							 "p.id in ("+StringUtil.collectionToString(ids, ",")+") and "+
							 "p.deleted = false ORDER BY FIELD(p.id,"+StringUtil.collectionToString(ids, ",")+")");
			 query.setFirstResult(offset * CalcServer.FEED_RETRIEVAL_COUNT);
			 query.setMaxResults(CalcServer.FEED_RETRIEVAL_COUNT);
			 return (List<Post>) query.getResultList();
		} catch (NoResultException nre) {
			return null;
		}
	}
	
	public static List<Post> getUnmarkedSoldPostsAfter(Date date) {
	    try {
    	    Query query = JPA.em().createQuery("Select p from Post p where sold = ?1 and soldMarked = ?2 and soldDate < ?3");
            query.setParameter(1, true);
            query.setParameter(2, false);
            query.setParameter(3, date);
            return (List<Post>) query.getResultList();
	    } catch (NoResultException nre) {
            return null;
        }
	}
}
