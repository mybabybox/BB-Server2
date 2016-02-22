package models;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Query;

import common.cache.CountryCache;
import domain.AuditListener;
import play.db.jpa.JPA;

/**
 * 
 * @author keithlei
 *
 */
@Entity
@EntityListeners(AuditListener.class)
public class Country extends domain.Entity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long id;
    
    //@Index(name = "Idx_Icon_Name")
    public String name;
    
    public String icon;

    public int seq;
    
    @Column(nullable = false, columnDefinition = "TINYINT(1)")
    public boolean deleted = false;
    
    @Enumerated(EnumType.STRING)
    public CountryCode code;
    
    public static enum CountryCode {
        NA,
        INTL,  // international
        HK,    // Hong Kong
        US,    // US
        JP,    // Japan
        KR,    // South Korea
        TW,    // Taiwan
        TH,    // Thailand
        MY,    // Malaysia
        ID,    // Indonesia
        AU,    // Australia
        NZ,    // New Zealand
        DE,    // Germany
        GB,    // UK
        IT,    // Italy
        FR,    // France
        ES,    // Spain
        NL,    // Netherlands
        NO,    // Norway
        IE,    // Ireland
        DK,    // Denmark
        SE,    // Sweden
        CH,    // Switzerland
        IS,    // Iceland
        CA,    // Canada
   }
    
    public Country() {}

    public Country(String name, CountryCode code, String icon) {
        this.name = name;
        this.code = code;
        this.icon = icon;
        this.seq = code.ordinal();
    }
    
    public static List<Country> loadCountries() {
        Query q = JPA.em().createQuery("Select c from Country c where deleted = false order by seq");
        return (List<Country>)q.getResultList();
    }

    public static List<Country> getCountries() {
        return CountryCache.getCountries();
    }
    
    public static Country getCountry(CountryCode code) {
        return CountryCache.getCountry(code);
    }
    
    public String getName() {
    	return name;
    }
    
    public CountryCode getCode() {
        return code;
    }
    
    public String getIcon() {
    	return icon;
    }
}
