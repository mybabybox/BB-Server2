package models;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.apache.commons.lang3.builder.EqualsBuilder;

import common.cache.HashtagCache;

import domain.Likeable;
import domain.Postable;
import domain.SocialObjectType;
import play.db.jpa.JPA;

@Entity
public class Hashtag extends SocialObject implements Likeable, Postable {

	public String icon;

	@Column(length=2000)
	public String description;
    
	public String jobClass;

	public boolean rerun = false;
	
	public int seq;

	public Hashtag() {
		this.objectType = SocialObjectType.HASHTAG;
	}
	
	public Hashtag(String name, String description, User owner, String icon, int seq) {
		this();
		this.name = name;
		this.description = description;
		this.owner = owner;
		this.icon = icon;
		this.seq = seq;
		this.system = true;
	}
	
	public static List<Hashtag> loadHashtags() {
		try {
            Query q = JPA.em().createQuery("SELECT c FROM Hashtag c where deleted = 0 order by seq");
            return (List<Hashtag>) q.getResultList();
        } catch (NoResultException nre) {
            return null;
        }
    }

	public static Hashtag findById(Long id) {
	    Hashtag hashtag = HashtagCache.getHashtag(id);
		if (hashtag != null) {
			return hashtag;
		}

		try {
            Query q = JPA.em().createQuery("SELECT c FROM Hashtag c where id = ?1 and deleted = 0");
            q.setParameter(1, id);
            return (Hashtag) q.getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }

	public static Hashtag findByName(String name) {
        Hashtag hashtag = HashtagCache.getHashtag(name);
        if (hashtag != null) {
            return hashtag;
        }

        try {
            Query q = JPA.em().createQuery("SELECT t FROM Hashtag t where name = ?1 and deleted = 0");
            q.setParameter(1, name);
            return (Hashtag) q.getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }

	public static List<Hashtag> getRerunHashtags() {
		try {
            Query q = JPA.em().createQuery("SELECT t FROM Hashtag t where rerun = true and deleted = 0");
            List<Hashtag> list = (List<Hashtag>)q.getResultList();
            return list;
        } catch (NoResultException nre) {
            return null;
        }
    }
	
	public static List<Hashtag> getAllSystemHashtags() {
		try {
            Query q = JPA.em().createQuery("SELECT t FROM Hashtag t where system = true and jobClass is not null and deleted = 0");
            List<Hashtag> list = (List<Hashtag>)q.getResultList();
            return list;
        } catch (NoResultException nre) {
            return null;
        }
    }
	
	public static List<Hashtag> getAllHashtags() {
		return HashtagCache.getAllHashtags();
	}
	
	@Override
    public boolean equals(Object o) {
        if (o != null && o instanceof Hashtag) {
            final Hashtag other = (Hashtag) o;
            return new EqualsBuilder().append(id, other.id).isEquals();
        } 
        return false;
    }
}