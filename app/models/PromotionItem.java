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

import common.cache.PromotionItemCache;

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
public class PromotionItem extends domain.Entity  implements Serializable, Creatable, Updatable {
    private static final play.api.Logger logger = play.api.Logger.apply(PromotionItem.class);

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
	
	public PromotionItem() {
	}
	
	public PromotionItem(String name, String description, String image, int seq, ItemType itemType, 
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

	public static List<PromotionItem> loadPromotionItems() {
        Query q = JPA.em().createQuery("SELECT p FROM PromotionItem p where deleted = false order by seq");
        return (List<PromotionItem>)q.getResultList();
    }
	
	public static List<PromotionItem> getPromotionItems(ItemType itemType) {
	    return PromotionItemCache.getPromotionItems(itemType);
	}
	
	public static PromotionItem findById(Long id) {
	    PromotionItem promotionItem = PromotionItemCache.getPromotionItem(id);
        if (promotionItem != null) {
            return promotionItem;
        }
        
        try {
            Query q = JPA.em().createQuery("SELECT p FROM PromotionItem p where id = ?1 and deleted = false");
            q.setParameter(1, id);
            return (PromotionItem) q.getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }
}
