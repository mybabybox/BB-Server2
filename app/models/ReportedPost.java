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
public class ReportedPost extends domain.Entity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long id;
    
    @Required
    public Long postId;
    
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
    
    public ReportedPost() {}
    
    public ReportedPost(Long postId, Long reporterId, ReportedType reportedType) {
        this(postId, reporterId, null, reportedType);
    }
    
    public ReportedPost(Long postId, Long reporterId, String body, ReportedType reportedType) {
        this.postId = postId;
        this.reporterId = reporterId;
        this.body = body;
        this.reportedType = reportedType;
    }
    
    public static ReportedPost findById(Long id) {
        Query q = JPA.em().createQuery("SELECT r FROM ReportedPost r where id = ?1 and deleted = 0");
        q.setParameter(1, id);
        try {
            return (ReportedPost) q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
    
    public static List<ReportedPost> getReportedPosts() {
        Query q = JPA.em().createQuery("SELECT r FROM ReportedPost r where deleted = 0 order by CREATED_DATE desc");
        q.setMaxResults(DefaultValues.MAX_ITEMS_COUNT);
        try {
            return (List<ReportedPost>) q.getResultList();
        } catch (NoResultException e) {
            return null;
        }
    }
    
}