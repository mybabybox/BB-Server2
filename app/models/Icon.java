package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Query;

import common.cache.IconCache;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;

/**
 * 
 * @author keithlei
 *
 */
@Entity
public class Icon {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long id;
    
    //@Index(name = "Idx_Icon_Name")
    public String name;
    
    public String code;
    
    public String url;
    
    @Enumerated(EnumType.STRING)
    public IconType iconType;
    
    public static enum IconType {
        COUNTRY
    }
    
    public Icon(){}

    public Icon(String name, String code, String url, IconType iconType) {
        this.name = name;
        this.code = code;
        this.url = url;
        this.iconType = iconType;
    }
    
    public static List<Icon> loadCountryIcons() {
        return loadIcons(IconType.COUNTRY);
    }
    
    public static List<Icon> loadIcons(IconType iconType) {
        Query q = JPA.em().createQuery("Select i from Icon i where iconType = ?1");
        q.setParameter(1, iconType);
        return (List<Icon>)q.getResultList();
    }

    public static List<Icon> getCountryIcons() {
        return IconCache.getCountryIcons();
    }
    
    public static Icon getCountryIcon(String code) {
        return IconCache.getCountryIcon(code);
    }
    
    public String getName() {
    	return name;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getUrl() {
    	return url;
    }
    
    public IconType getIconType() {
        return iconType;
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
}
