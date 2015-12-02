package models;

import java.io.Serializable;
import java.util.ArrayList;
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
public class GameBadgeAwarded extends domain.Entity implements Serializable, Creatable, Updatable {
    private static final play.api.Logger logger = play.api.Logger.apply(GameBadgeAwarded.class);

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long id;
    
    @Required
    public Long userId;
    
    @Required
    public Long gameBadgeId;
    
    @Required
    public Boolean deleted = false;
    
	public GameBadgeAwarded() {
	}
	
	public GameBadgeAwarded(Long userId, Long gameBadgeId) {
	    this.userId = userId;
	    this.gameBadgeId = gameBadgeId;
    }

	@Transactional
	public static void recordGameBadge(Long userId, GameBadge.BadgeType badgeType) {
	    NanoSecondStopWatch sw = new NanoSecondStopWatch();
	    
	    GameBadge badge = GameBadge.findByBadgeType(badgeType);
	    if (badge == null) {
	        logger.underlyingLogger().error("[u="+userId+"] recordGameBadge() badgeType="+badgeType.name()+" does not exist !!");
	        return;
	    }
	    
	    boolean awarded = isGameBadgeAwarded(userId, badge.id);
	    if (awarded) {
	        logger.underlyingLogger().warn("[u="+userId+"] recordGameBadge() badgeType="+badgeType.name()+" awarded already, skipped..");
	        return;
	    }
	    
	    GameBadgeAwarded badgeAwarded = new GameBadgeAwarded(userId, badge.id);
	    badgeAwarded.save();
        
        sw.stop();
        if (logger.underlyingLogger().isDebugEnabled()) {
            logger.underlyingLogger().debug("[u="+userId+"] recordGameBadge() badgeType="+badgeType.name()+". Took "+sw.getElapsedMS()+"ms");
        }
	}
	
	public static List<GameBadgeAwarded> getGameBadgesAwarded(Long userId) {
	    try {
            Query q = JPA.em().createQuery("SELECT b FROM GameBadgeAwarded b where userId = ?1 and deleted = false");
            q.setParameter(1, userId);
            return (List<GameBadgeAwarded>) q.getResultList();
        } catch (NoResultException nre) {
            return new ArrayList<>();
        }
	}
	
	public static boolean isGameBadgeAwarded(Long userId, Long gameBadgeId) {
        try {
            Query q = JPA.em().createQuery("SELECT count(b) FROM GameBadgeAwarded b where userId = ?1 and gameBadgeId = ?2 and deleted = false");
            q.setParameter(1, userId);
            q.setParameter(2, gameBadgeId);
            Long count = (Long)q.getSingleResult();
            return count > 0;
        } catch (NoResultException nre) {
            return false;
        }
    }
	
	public static GameBadgeAwarded findById(Long id) {
        try {
            Query q = JPA.em().createQuery("SELECT b FROM GameBadgeAwarded b where id = ?1 and deleted = false");
            q.setParameter(1, id);
            return (GameBadgeAwarded) q.getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }
}
