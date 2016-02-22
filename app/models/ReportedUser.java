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

import domain.AuditListener;
import domain.DefaultValues;
import domain.ReportedType;
import play.data.validation.Constraints.Required;
import play.db.jpa.JPA;

@Entity
@EntityListeners(AuditListener.class)
public class ReportedUser extends domain.Entity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long id;
    
    @Required
    public Long userId;
    
    @Required
    public Long reporterId;

    @Column(length=255)
    public String body;
    
    @Column(length=255)
    public String note;
    
    @Enumerated(EnumType.STRING)
    public ReportedType reportedType;

    @Column(nullable = false, columnDefinition = "TINYINT(1)")
    public boolean deleted = false;
    
    public ReportedUser() {}
    
    public ReportedUser(Long userId, Long reporterId, ReportedType reportedType) {
        this(userId, reporterId, null, reportedType);
    }
    
    public ReportedUser(Long userId, Long reporterId, String body, ReportedType reportedType) {
        this.userId = userId;
        this.reporterId = reporterId;
        this.body = body;
        this.reportedType = reportedType;
    }
    
    public static ReportedUser findById(Long id) {
        Query q = JPA.em().createQuery("SELECT r FROM ReportedUser r where id = ?1 and deleted = 0");
        q.setParameter(1, id);
        try {
            return (ReportedUser) q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
    
    public static List<ReportedUser> getReportedPosts() {
        Query q = JPA.em().createQuery("SELECT r FROM ReportedUser r where deleted = 0 order by CREATED_DATE desc");
        q.setMaxResults(DefaultValues.MAX_ITEMS_COUNT);
        try {
            return (List<ReportedUser>) q.getResultList();
        } catch (NoResultException e) {
            return null;
        }
    }
    
}