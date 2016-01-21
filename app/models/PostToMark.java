package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import play.db.jpa.JPA;
import play.db.jpa.Transactional;

@Entity
public class PostToMark  {

    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    public Long id;
    
    public Long postid;

    public PostToMark(){}
    	
    public PostToMark(Long pid){
    	this();
    	this.postid=pid;
    }

    @Transactional
    public void save() {
        JPA.em().persist(this);
        JPA.em().flush();
    }
    
    @Transactional
    public void delete() {
        JPA.em().remove(this);
    }
    
    @Transactional
    public void merge() {
        JPA.em().merge(this);
    }
    
    @Transactional
    public void refresh() {
        JPA.em().refresh(this);
    }
    
    public static List<PostToMark> getAllMarkPost() {
		try {
            Query q = JPA.em().createQuery("SELECT p FROM PostToMark p");
            List<PostToMark> list = (List<PostToMark>)q.getResultList();
            return list;
        } catch (NoResultException nre) {
            return null;
        }
    }
}
