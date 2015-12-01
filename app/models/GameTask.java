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

import common.cache.GameTaskCache;
import play.data.validation.Constraints.Required;
import play.db.jpa.JPA;

/**
 * 
 */
@Entity
public class GameTask extends domain.Entity {
    private static final play.api.Logger logger = play.api.Logger.apply(GameTask.class);

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long id;
    
    @Required
    public String name;
    
    @Enumerated(EnumType.STRING)
    public TaskType taskType;
    
    @Column(length=1000)
    public String shortDescription;
    
    @Column(length=2000)
    public String description;
    
    @Required
    public String icon;
    
    @Required
    public int seq;
    
    @Required
    public Boolean deleted = false;
    
	public static enum TaskType {
	    PROFILE_PHOTO,
	    LIKE_1,
	    LIKE_10,
	    FOLLOW_1,
	    FOLLOW_10,
	    POST_1,
	    POST_10
	}
	
	public GameTask() {
	}
	
	public GameTask(TaskType taskType, String name, String shortDescription, String description, String icon) {
	    this.taskType = taskType;
	    this.name = name;
	    this.shortDescription = shortDescription;
	    this.description = description;
	    this.icon = icon;
	    this.seq = taskType.ordinal();
    }

	public static List<GameTask> loadGameTasks() {
        Query q = JPA.em().createQuery("SELECT t FROM GameTask t where deleted = false");
        return (List<GameTask>)q.getResultList();
    }
	
	public static List<GameTask> getAllGameTasks() {
	    return GameTaskCache.getGameTasks();
	}
	
	public static GameTask findById(Long id) {
	    GameTask gameTask = GameTaskCache.getGameTask(id);
        if (gameTask != null) {
            return gameTask;
        }
        
        try {
            Query q = JPA.em().createQuery("SELECT t FROM GameTask t where id = ?1 and deleted = false");
            q.setParameter(1, id);
            return (GameTask) q.getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }
	
	public static GameTask findByTaskType(TaskType taskType) {
	    GameTask gameTask = GameTaskCache.getGameTask(taskType);
        if (gameTask != null) {
            return gameTask;
        }
        
        try {
            Query q = JPA.em().createQuery("SELECT t FROM GameTask t where taskType = ?1 and deleted = false");
            q.setParameter(1, taskType);
            return (GameTask) q.getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }
}
