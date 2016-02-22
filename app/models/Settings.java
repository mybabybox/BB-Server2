package models;

import java.io.Serializable;

import javax.persistence.Column;
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
	
	@Column(nullable = false, columnDefinition = "TINYINT(1)")
	public boolean emailNewPost = true;
    
	@Column(nullable = false, columnDefinition = "TINYINT(1)")
    public boolean emailNewConversation = true;
    
	@Column(nullable = false, columnDefinition = "TINYINT(1)")
    public boolean emailNewComment = false;
    
	@Column(nullable = false, columnDefinition = "TINYINT(1)")
    public boolean emailNewPromotions = true;
    
	@Column(nullable = false, columnDefinition = "TINYINT(1)")
    public boolean pushNewConversation = true;
    
	@Column(nullable = false, columnDefinition = "TINYINT(1)")
    public boolean pushNewComment = true;
    
	@Column(nullable = false, columnDefinition = "TINYINT(1)")
    public boolean pushNewFollow = true;
    
	@Column(nullable = false, columnDefinition = "TINYINT(1)")
    public boolean pushNewFeedback = true;
    
	@Column(nullable = false, columnDefinition = "TINYINT(1)")
    public boolean pushNewPromotions = true;
    
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
