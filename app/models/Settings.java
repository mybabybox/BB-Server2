package models;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import play.data.validation.Constraints.Required;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;

/**
 * 
 */
@Entity
public class Settings extends domain.Entity implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long id;

	@Required
	public Long userId;
	
	public Boolean emailNewPost = true;
    
    public Boolean emailNewConversation = true;
    
    public Boolean emailNewComment = false;
    
    public Boolean emailNewPromotions = true;
    
    public Boolean pushNewConversation = true;
    
    public Boolean pushNewComment = true;
    
    public Boolean pushNewFollow = true;
    
    public Boolean pushNewFeedback = true;
    
    public Boolean pushNewPromotions = true;
    
	public Settings() {
	}
	
	public static Settings findById(Long id) {
        try {
            Query q = JPA.em().createQuery("SELECT s FROM Settings s where id = ?1");
            q.setParameter(1, id);
            return (Settings) q.getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }
	
	@Transactional
	public static Settings findByUserId(Long userId) {
        try {
            Query q = JPA.em().createQuery("SELECT s FROM Settings s where userId = ?1");
            q.setParameter(1, userId);
            return (Settings) q.getSingleResult();
        } catch (NoResultException nre) {
            Settings settings = new Settings();
            settings.userId = userId;
            settings.save();
            return settings;
        }
    }
}
