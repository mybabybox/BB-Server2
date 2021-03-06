package models;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import play.data.validation.Constraints.Required;
import play.db.jpa.JPA;
import domain.AuditListener;
import domain.Creatable;
import domain.Updatable;

/**
 * 1. No unclosed order record
 *      Buyer action:   Offer
 *      Seller action:  NA
 *      
 * 2. Buyer offered:
 *      Buyer action:   Cancel Offer
 *      Seller action:  Accept / Decline
 * 
 * [If offer not yet accepted by Seller]
 * 3. Buyer cancelled offer: 
 *      Buyer action:   Offer
 *      Seller action:  NA (Message: Buyer cancelled offer)
 * 
 * [If offer by Buyer]
 * 4. Seller accepted offer:
 *      Buyer action:   Offer Again (Message: Seller accepted your offer)       TODO: Leave feedback
 *      Seller action:  NA (Message: You accepted offer from buyer)             TODO: Leave feedback
 *      
 * [If offer by Buyer]
 * 5. Seller declined offer:
 *      Buyer action:   Offer (Message: Seller declined your offer)
 *      Seller action:  NA (Message: You declined offer from buyer)
 * 
 * @author keithlei
 *
 */
@Entity
@EntityListeners(AuditListener.class)
public class ConversationOrder extends domain.Entity implements Serializable, Creatable, Updatable {
    private static final play.api.Logger logger = play.api.Logger.apply(ConversationOrder.class);
    
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long id;

	@Required
	@ManyToOne
	public Conversation conversation;
	
	@Required
	@ManyToOne
    public User user1;                 // buyer
	
	@Required
	@ManyToOne
    public User user2;                 // seller

	public Double offeredPrice = 0.0;
	
	@Column(nullable = false, columnDefinition = "TINYINT(1)")
	public boolean cancelled = false;  // by buyer
    
    @Temporal(TemporalType.TIMESTAMP)
    public Date cancelDate;
	
    @Column(nullable = false, columnDefinition = "TINYINT(1)")
	public boolean accepted = false;   // by seller
    
    @Temporal(TemporalType.TIMESTAMP)
    public Date acceptDate;
    
    @Column(nullable = false, columnDefinition = "TINYINT(1)")
    public boolean declined = false;   // by seller
    
    @Temporal(TemporalType.TIMESTAMP)
    public Date declineDate;
    
    @Column(nullable = false, columnDefinition = "TINYINT(1)")
    public boolean active = true;
    
    @Column(nullable = false, columnDefinition = "TINYINT(1)")
	public boolean deleted = false; 
	
	public ConversationOrder() {}
	
	public ConversationOrder(Conversation conversation, Double offeredPrice) {
		this.conversation = conversation;
		this.user1 = conversation.user1;
		this.user2 = conversation.user2;
		this.offeredPrice = offeredPrice;
		this.active = true;
	}
	
	@Override
	public void preSave() {
	}
	
	public boolean isOrderClosed() {
        return this.cancelled || this.accepted || this.declined;
    }
	
	public static ConversationOrder findById(Long id) {
		try {
		    Query q = JPA.em().createQuery(
	                "SELECT o from ConversationOrder o where id = ?1 and deleted = false");
	        q.setParameter(1, id);
			return (ConversationOrder) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}
	
	public static ConversationOrder getActiveOrder(Conversation conversation) {
	    Query q = JPA.em().createQuery(
                "SELECT o from ConversationOrder o where conversation = ?1 and active = ?2 and deleted = false");
        q.setParameter(1, conversation);
        q.setParameter(2, true);
        
        try {
            return (ConversationOrder) q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        } catch (NonUniqueResultException e) {
            logger.underlyingLogger().error("[conv="+conversation.id+"] has multiple active orders! Mark delete all orders...");
            markDelete(conversation);
            return null;
        }
	}
	
	public static void markDelete(Conversation conversation) {
        try {
            Query q = JPA.em().createQuery("update ConversationOrder set active = 0, deleted = 1 where conversation = ?1");
            q.setParameter(1, conversation);
            q.executeUpdate();
        } catch (Exception e) {
            logger.underlyingLogger().error("Failed to mark delete ConversationOrder for conversationId="+conversation.id, e);
        }
    }
}