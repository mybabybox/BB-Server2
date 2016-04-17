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
import javax.persistence.NonUniqueResultException;
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
    public String appVersion;
    
    @Required
    public Long versionCode = 0L;
    
    @Enumerated(EnumType.STRING)
    public DeviceType deviceType;

    @Column(nullable = false, columnDefinition = "TINYINT(1)")
    public boolean deleted = false;

    /**
     * Ctor
     */
    public PushNotificationToken() {
    }

    public static void createUpdateToken(Long userId, String token, String appVersion, DeviceType deviceType) {
        //PushNotificationToken pushNotificationToken = getToken(userId, appVersion, deviceType);
        PushNotificationToken pushNotificationToken = findByUserId(userId);
        if (pushNotificationToken == null) {
            pushNotificationToken = new PushNotificationToken();
            pushNotificationToken.setCreatedDate(new Date());
            logger.underlyingLogger().debug("createUpdateToken() created new token");
        } else {
            pushNotificationToken.setUpdatedDate(new Date());            
        }
        
        if (!token.equals(pushNotificationToken.token)) {
            pushNotificationToken.userId = userId;
            pushNotificationToken.token = token;
            pushNotificationToken.appVersion = appVersion;
            pushNotificationToken.deviceType = deviceType;
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
            //q.setMaxResults(1);
            return (PushNotificationToken) q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        } catch (NonUniqueResultException e) {
            logger.underlyingLogger().error("[u="+userId+"] has multiple tokens! Mark delete all tokens...");
            markDelete(userId);
            return null;
        }
    }
    
    public static PushNotificationToken getToken(Long userId, String appVersion, DeviceType deviceType) {
        try { 
            Query q = JPA.em().createQuery("SELECT t FROM PushNotificationToken t where userId = ?1 and appVersion = ?2 and deviceType = ?3 and deleted = false");
            q.setParameter(1, userId);
            q.setParameter(2, appVersion);
            q.setParameter(3, deviceType);
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