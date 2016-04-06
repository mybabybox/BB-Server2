package models;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import controllers.Application.DeviceType;
import play.data.validation.Constraints.Required;
import play.db.jpa.JPA;

@Entity
public class PushNotificationToken extends domain.Entity {
    private static final play.api.Logger logger = play.api.Logger.apply(PushNotificationToken.class);
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long id;

    @Required
    public Long userId;
    
    @Required
    public String token;
    
    @Required
    public Long versionCode;
    
    @Enumerated(EnumType.STRING)
    public DeviceType deviceType;

    @Column(nullable = false, columnDefinition = "TINYINT(1)")
    public boolean deleted = false;

    /**
     * Ctor
     */
    public PushNotificationToken() {
    }

    public static void createUpdateToken(Long userId, String token, Long versionCode, DeviceType deviceType) {
        PushNotificationToken pushNotificationToken = findByUserIdVersionCode(userId, versionCode);
        if (token == null) {
            markDelete(userId);     // mark delete old versions
            pushNotificationToken = new PushNotificationToken();
            logger.underlyingLogger().debug("createUpdateToken() created new token");
        }
        
        if (!token.equals(pushNotificationToken.token)) {
            pushNotificationToken.userId = userId;
            pushNotificationToken.token = token;
            pushNotificationToken.versionCode = versionCode;
            pushNotificationToken.deviceType = deviceType;
            pushNotificationToken.setCreatedDate(new Date());
            pushNotificationToken.save();
            logger.underlyingLogger().debug("createUpdateToken() updated token");
        } else {
            logger.underlyingLogger().debug("createUpdateToken() same token. Skipped update");            
        }
    }

    ///////////////////////// Find APIs /////////////////////////
    public static PushNotificationToken findByUserId(Long userId) {
        try { 
            Query q = JPA.em().createQuery("SELECT t FROM PushNotificationToken t where userId = ?1 and deleted = false order by CREATED_DATE desc");
            q.setParameter(1, userId);
            q.setMaxResults(1);
            
            if (q.getMaxResults() > 1) {
                logger.underlyingLogger().error("[u="+userId+"] has "+q.getMaxResults()+" tokens!!");
            }
            
            return (PushNotificationToken) q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        } 
    }
    
    public static PushNotificationToken findByUserIdVersionCode(Long userId, Long versionCode) {
        try { 
            Query q = JPA.em().createQuery("SELECT t FROM PushNotificationToken t where userId = ?1 and versionCode = ?2 and deleted = false");
            q.setParameter(1, userId);
            q.setParameter(2, versionCode);
            return (PushNotificationToken) q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        } 
    }

    public static void markDelete(Long userId) {
        try {
            Query q = JPA.em().createQuery("update PushNotificationToken set deleted = 1 where userId = ?1");
            q.setParameter(1, userId);
            q.executeUpdate();
        } catch (Exception e) {
            logger.underlyingLogger().error("Failed to mark delete PushNotificationToken for userId="+userId, e);
        }
    }
}