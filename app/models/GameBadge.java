package models;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import common.cache.GameBadgeCache;
import play.data.validation.Constraints.Required;
import play.db.jpa.JPA;

/**
 * 
 */
@Entity
public class GameBadge extends domain.Entity {
    private static final play.api.Logger logger = play.api.Logger.apply(GameBadge.class);

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long id;
    
    @Required
    public String name;
    
    @Enumerated(EnumType.STRING)
    public BadgeType badgeType;
    
    @Column(length=1000)
    public String description;
    
    @Required
    public String icon;
    
    @Required
    public String icon2;
    
    @Required
    public int seq;
    
    @Required
    public Boolean deleted = false;
    
	public static enum BadgeType {
	    PROFILE_PHOTO,
	    PROFILE_INFO,
	    LIKE_1,
	    LIKE_10,
	    FOLLOW_1,
	    FOLLOW_10,
	    POST_1,
	    POST_10
	}
	
	public GameBadge() {
	}
	
	public GameBadge(BadgeType badgeType, String name, String description, String icon, String icon2) {
	    this.badgeType = badgeType;
	    this.name = name;
	    this.description = description;
	    this.icon = icon;
	    this.icon2 = icon2;
	    this.seq = badgeType.ordinal();
    }

	public static List<GameBadge> loadGameBadges() {
        Query q = JPA.em().createQuery("SELECT b FROM GameBadge b where deleted = false");
        return (List<GameBadge>)q.getResultList();
    }
	
	public static List<GameBadge> getAllGameBadges() {
	    return GameBadgeCache.getGameBadges();
	}
	
	public static GameBadge findById(Long id) {
	    GameBadge gameBadge = GameBadgeCache.getGameBadge(id);
        if (gameBadge != null) {
            return gameBadge;
        }
        
        try {
            Query q = JPA.em().createQuery("SELECT b FROM GameBadge b where id = ?1 and deleted = false");
            q.setParameter(1, id);
            return (GameBadge) q.getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }
	
	public static GameBadge findByBadgeType(BadgeType badgeType) {
	    GameBadge gameBadge = GameBadgeCache.getGameBadge(badgeType);
        if (gameBadge != null) {
            return gameBadge;
        }
        
        try {
            Query q = JPA.em().createQuery("SELECT b FROM GameBadge b where badgeType = ?1 and deleted = false");
            q.setParameter(1, badgeType.name());
            return (GameBadge) q.getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }
}
