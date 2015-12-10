package models;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import common.cache.FeaturedItemCache;

import domain.AuditListener;
import domain.Creatable;
import domain.Updatable;
import play.data.validation.Constraints.Required;
import play.db.jpa.JPA;

/**
 * 
 */
@Entity
@EntityListeners(AuditListener.class)
public class FeaturedItem extends domain.Entity  implements Serializable, Creatable, Updatable {
    private static final play.api.Logger logger = play.api.Logger.apply(FeaturedItem.class);

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long id;
    
    @Required
    public String name;
    
    @Column(length=1000)
    public String description;
    
    @Required
    public String image;
    
    @Required
    public int seq;
    
    @Enumerated(EnumType.STRING)
    public ItemType itemType;
    
    @Enumerated(EnumType.STRING)
    public DestinationType destinationType;
    
    public Long destinationObjId;
    
    public String destinationObjName;
    
    @Required
    public Boolean deleted = false;
    
	public static enum ItemType {
	    HOME_SLIDER
	}
	
	public static enum DestinationType {
        CATEGORY,
        POST,
        USER,
        HASHTAG
    }
	
	public FeaturedItem() {
	}
	
	public FeaturedItem(String name, String description, String image, int seq, ItemType itemType, 
	        DestinationType destinationType, Long destinationObjId, String destinationObjName) {
	    this.name = name;
	    this.description = description;
	    this.image = image;
	    this.seq = seq;
	    this.itemType = itemType;
	    this.destinationType = destinationType;
	    this.destinationObjId = destinationObjId;
	    this.destinationObjName = destinationObjName;
    }

	public static List<FeaturedItem> loadFeaturedItems() {
        Query q = JPA.em().createQuery("SELECT p FROM FeaturedItem p where deleted = false order by seq");
        return (List<FeaturedItem>)q.getResultList();
    }
	
	public static List<FeaturedItem> getFeaturedItems(ItemType itemType) {
	    return FeaturedItemCache.getFeaturedItems(itemType);
	}
	
	public static FeaturedItem findById(Long id) {
	    FeaturedItem featuredItem = FeaturedItemCache.getFeaturedItem(id);
        if (featuredItem != null) {
            return featuredItem;
        }
        
        try {
            Query q = JPA.em().createQuery("SELECT p FROM FeaturedItem p where id = ?1 and deleted = false");
            q.setParameter(1, id);
            return (FeaturedItem) q.getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }
}
