package models;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.NoResultException;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Query;

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
 * ALTER TABLE Story CHANGE COLUMN noOfComments numComments int(11);
 * ALTER TABLE Story CHANGE COLUMN noOfLikes numLikes int(11);
 * ALTER TABLE Story CHANGE COLUMN noOfViews numViews int(11);
 * 
 * @author keithlei
 */
@Entity
public class Story extends SocialObject implements Likeable, Commentable {
	private static final play.api.Logger logger = play.api.Logger.apply(Story.class);
	
	@Column(length=2000)
	public String body;

	@ManyToOne(cascade = CascadeType.REMOVE)
	public Folder folder;

	@ManyToOne(cascade = CascadeType.REMOVE)
	public Collection collection;

	@OneToMany(cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
	@OrderBy("CREATED_DATE")
	@JsonIgnore
	public List<Comment> comments;

	public int numViews = 0;
	public int numComments = 0;
	public int numLikes = 0;
	
	public Long baseScoreAdjust = 0L;
	public Long baseScore = 0L;
	public Double timeScore = 0D;

	@OneToMany
    public List<Hashtag> hashtags = new ArrayList<>();
	
	@OneToMany
    public List<Post> relatedPosts = new ArrayList<>();

	public DeviceType deviceType;
	
	/**
	 * Ctor
	 */
	public Story() {}

	public Story(User owner, String body, DeviceType deviceType) {
		this.owner = owner;
		this.body = body;
		this.objectType = SocialObjectType.STORY;
		this.deviceType = deviceType;
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
	
    public void addRelatedPost(Post post) {
        relatedPosts.add(post);
    }
    
	public List<Post> getRelatedPosts() {
        return relatedPosts;
    }
    
	@Override
	public boolean onLikedBy(User user) {
	    if (logger.underlyingLogger().isDebugEnabled()) {
            logger.underlyingLogger().debug("[s="+this.id+"][u="+user.id+"] onLikeBy");
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
				logger.underlyingLogger().debug(String.format("Story [s=%d] already liked by User [u=%d]", this.id, user.id));
			}
			return liked;
		}
		return false;
	}

	@Override
	public boolean onUnlikedBy(User user) {
	    if (logger.underlyingLogger().isDebugEnabled()) {
            logger.underlyingLogger().debug("[s="+this.id+"][u="+user.id+"] onUnlikeBy");
        }
	    
	    if (!user.isLoggedIn()) {
            return false;
        }
	    
		if (isLikedBy(user)) {
			boolean unliked = 
					LikeSocialRelation.unlike(
							user.id, SocialObjectType.USER, this.id, SocialObjectType.STORY);
			if (unliked) {
			    if (this.numLikes > 0) {
			        this.numLikes--;
			    }
			    if (user.numLikes > 0) {
			        user.numLikes--;
			    }
			} else {
				logger.underlyingLogger().debug(String.format("Story [s=%d] already unliked by User [u=%d]", this.id, user.id));
			}
			return unliked;
		}
		return false;
	}
	
	@Override
	public boolean isLikedBy(User user){
		//return CalcServer.instance().isLiked(user.id, this.id);
	    return false;      // needs to query another liked queue for story
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
	
	public List<Comment> getStoryComments(Long offset) {
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

	public Resource addStoryPhoto(File source) throws IOException {
		ensureAlbumExist();
		Resource photo = this.folder.addFile(source, SocialObjectType.STORY_PHOTO);
		photo.save();
		return photo;
	}

	public void ensureAlbumExist() {
		if (this.folder == null) {
			this.folder = Folder.createFolder(this.owner, "story-ps", "", true);
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
	public static Story findById(Long id) {
		try {
			Query q = JPA.em().createQuery("SELECT s FROM Story s where id = ?1 and deleted = false");
			q.setParameter(1, id);
			return (Story) q.getSingleResult();
		} catch (NoResultException nre) {
			return null;
		}
	}

	public static List<Story> getEligibleStoriesForFeeds() {
		try {
			Query q = JPA.em().createQuery("SELECT s FROM Story s where deleted = false");
			return (List<Story>) q.getResultList();
		} catch (NoResultException nre) {
			return null;
		}
	}

	public static List<Story> getUserStories(Long id) {
		try {
			Query q = JPA.em().createQuery("SELECT s FROM Story s where owner = ?1 and deleted = false");
			q.setParameter(1, User.findById(id));
			return (List<Story>) q.getResultList();
		} catch (NoResultException nre) {
			return null;
		}
	}

	@Override
	public SocialObject onComment(User user, String body) 
			throws SocialObjectNotCommentableException {
		
		Comment comment = new Comment(this, user, body);
		comment.save();

		// merge into Story
		if (comments == null) {
			comments = new ArrayList<>();
		}
		
		this.comments.add(comment);
		this.numComments++;
		this.merge();

        user.numComments++;
        recordCommentStory(user, comment);
        
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

	public static List<Story> getStories(List<Long> ids) {
		try {
			 Query query = JPA.em().createQuery(
			            "select s from Story s where "+
			            "s.id in ("+StringUtil.collectionToString(ids, ",")+") and "+
			            "s.deleted = false ORDER BY FIELD(s.id,"+StringUtil.collectionToString(ids, ",")+")");
			 return (List<Story>) query.getResultList();
		} catch (NoResultException nre) {
			return null;
		}
	}
	
	public static List<Story> getStories(List<Long> ids, int offset) {
		try {
			 Query query = JPA.em().createQuery(
					 "select s from Story s where "+
							 "s.id in ("+StringUtil.collectionToString(ids, ",")+") and "+
							 "s.deleted = false ORDER BY FIELD(s.id,"+StringUtil.collectionToString(ids, ",")+")");
			 query.setFirstResult(offset * CalcServer.FEED_RETRIEVAL_COUNT);
			 query.setMaxResults(CalcServer.FEED_RETRIEVAL_COUNT);
			 return (List<Story>) query.getResultList();
		} catch (NoResultException nre) {
			return null;
		}
	}
}
