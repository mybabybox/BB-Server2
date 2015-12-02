package models;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import common.utils.NanoSecondStopWatch;

import domain.AuditListener;
import domain.Creatable;
import domain.Updatable;
import play.data.validation.Constraints.Required;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;

/**
 * 
 */
@Entity
@EntityListeners(AuditListener.class)
public class GameBadgeHistory extends domain.Entity implements Serializable, Creatable, Updatable {
    private static final play.api.Logger logger = play.api.Logger.apply(GameBadgeHistory.class);

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long id;
    
    @Required
    public Long userId;
    
    @Required
    public Long gameBadgeId;
    
    @Required
    public Boolean deleted = false;
    
	public GameBadgeHistory() {
	}
	
	public GameBadgeHistory(Long userId, Long gameBadgeId) {
	    this.userId = userId;
	    this.gameBadgeId = gameBadgeId;
    }

	@Transactional
	public static void recordGameBadge(Long userId, GameBadge.BadgeType badgeType) {
	    NanoSecondStopWatch sw = new NanoSecondStopWatch();
	    
	    GameBadge gameBadge = GameBadge.findByBadgeType(badgeType);
	    if (gameBadge == null) {
	        logger.underlyingLogger().error("[u="+userId+"] recordGameBadge() badgeType="+badgeType.name()+" does not exist !!");
	        return;
	    }
	    
	    GameBadgeHistory history = getGameBadgeHistory(userId, gameBadge.id);
	    if (history != null) {
	        logger.underlyingLogger().warn("[u="+userId+"] recordGameBadge() badgeType="+badgeType.name()+" awarded already, skipped..");
	        return;
	    }
	    
	    history = new GameBadgeHistory(userId, gameBadge.id);
        history.save();
        
        sw.stop();
        if (logger.underlyingLogger().isDebugEnabled()) {
            logger.underlyingLogger().debug("[u="+userId+"] recordGameBadge() badgeType="+badgeType.name()+". Took "+sw.getElapsedMS()+"ms");
        }
	}
	
	public static List<GameBadgeHistory> getGameBadgesHistory(Long userId) {
	    try {
            Query q = JPA.em().createQuery("SELECT h FROM GameBadgeHistory h where userId = ?1 and deleted = false");
            q.setParameter(1, userId);
            return (List<GameBadgeHistory>) q.getResultList();
        } catch (NoResultException nre) {
            return null;
        }
	}
	
	public static GameBadgeHistory getGameBadgeHistory(Long userId, Long gameBadgeId) {
        try {
            Query q = JPA.em().createQuery("SELECT h FROM GameBadgeHistory h where userId = ?1 and gameBadgeId = ?2 and deleted = false");
            q.setParameter(1, userId);
            q.setParameter(2, gameBadgeId);
            return (GameBadgeHistory) q.getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }
	
	public static GameBadgeHistory findById(Long id) {
        try {
            Query q = JPA.em().createQuery("SELECT h FROM GameBadgeHistory h where id = ?1 and deleted = false");
            q.setParameter(1, id);
            return (GameBadgeHistory) q.getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }
}
