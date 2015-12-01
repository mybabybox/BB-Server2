package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import play.data.validation.Constraints.Required;
import play.db.jpa.JPA;

/**
 * 
 */
@Entity
public class GameTaskHistory extends domain.Entity {
    private static final play.api.Logger logger = play.api.Logger.apply(GameTaskHistory.class);

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long id;
    
    @Required
    public Long userId;
    
    @Required
    public Long gameTaskId;
    
    @Required
    public Boolean deleted = false;
    
	public GameTaskHistory() {
	}
	
	public GameTaskHistory(Long userId, Long gameTaskId) {
	    this.userId = userId;
	    this.gameTaskId = gameTaskId;
    }

	public static void recordGameTaskHistory(Long userId, Long gameTaskId) {
	    GameTaskHistory history = new GameTaskHistory(userId, gameTaskId);
	    history.save();
	}
	
	public static List<GameTaskHistory> getGameTasksHistory(Long userId) {
	    try {
            Query q = JPA.em().createQuery("SELECT t FROM GameTaskHistory t where userId = ?1 and deleted = false");
            q.setParameter(1, userId);
            return (List<GameTaskHistory>) q.getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
	}
	
	public static GameTaskHistory findById(Long id) {
        try {
            Query q = JPA.em().createQuery("SELECT t FROM GameTaskHistory t where id = ?1 and deleted = false");
            q.setParameter(1, id);
            return (GameTaskHistory) q.getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }
}
