package models;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import common.utils.FileUtil;

import com.fasterxml.jackson.annotation.JsonIgnore;

import play.Play;
import play.data.validation.Constraints.Required;
import play.db.jpa.JPA;
import domain.DefaultValues;
import domain.SocialObjectType;

/**
 * A resource can be a file or an external url, is contained always in a Folder
 * 
 */

@Entity
public class Resource extends SocialObject {

    public static final String STORAGE_PATH = 
            Play.application().configuration().getString("storage.path");
    public static final int STORAGE_PARTITION_DIR_MAX = 
            Play.application().configuration().getInt("storage.partition.dir.max", 20000);
    
	public Resource() {
	}
	
	@JsonIgnore
	@Required
	@ManyToOne
	public Folder folder;

	@Required
	public String resourceName;

	@Lob
	public String description;

	@Required
	public Integer priority = 0;

    public String authorizedUserIds;
	
	public Resource(SocialObjectType objectType) {
		this.objectType = objectType;
	}

	public Boolean isImage() {
		return FileUtil.isImage(resourceName);
	}

	public Boolean isExternal() {
		return FileUtil.isExternal(resourceName);
	}

	@Override
	public String toString() {
		return super.toString() + " " + resourceName;
	}

	public String getPath() {
		return getStoragePath("");
	}

	public String getThumbnail() {
        return getStoragePath("thumbnail.");
	}

	public String getMini() {
        return getStoragePath("mini.");
	}
	
	public File getRealFile() {
        return getFileObject(getPath());
	}

    public File getThumbnailFile() {
        return getFileObject(getThumbnail());
	}
    
    public File getMiniFile() {
        return getFileObject(getMini());
    }

	public Long getSize() {
		if (isExternal()) {
			return null;
		} else {
			return org.apache.commons.io.FileUtils.sizeOf(getRealFile());
		}
	}

    private String getStoragePath(String filePrefix) {
		if (isExternal()) {
			return resourceName;
		} else {
            if (filePrefix == null) {
                filePrefix = "";
            }
			return STORAGE_PATH+getStoragePartition()+"/"+
                   owner.id+"/"+folder.id+"/"+id+ "/"+filePrefix+resourceName;
		}
    }

    private String getStoragePartition() {
        return "part"+(owner.id / STORAGE_PARTITION_DIR_MAX);
    }

    public static File getFileObject(String path) {
		File f = new File(path);
		if (f.exists()) {
			return f;
		}
		return null;
	}
    
    public static File getStorageStaticImage(String path) {
        path = STORAGE_PATH + path;
        File file = new File(path);
        if (file.exists()) {
            return file;
        }
        return null;
    }
    
    public void setAuthorizedUsers(List<User> users) {
        if (users == null || users.size() == 0) {
            return;
        }
        
        String authorizedUserIds = "";
        for (User user : users) {
            authorizedUserIds += user.id + DefaultValues.DELIMITER_COMMA;
        }
        if (authorizedUserIds.endsWith(DefaultValues.DELIMITER_COMMA)) {
            authorizedUserIds = authorizedUserIds.substring(0, authorizedUserIds.length() - 1);
        }
        
        this.authorizedUserIds = authorizedUserIds;
        this.save();
    }
    
    public boolean isUserAuthorized(User user) {
        if (user != null && owner != null && user.id == owner.id) {
            return true;
        }
        
        if (authorizedUserIds == null) {
            return false;
        }
        
        List<String> ids = Arrays.asList(authorizedUserIds.split(DefaultValues.DELIMITER_COMMA));
        if (ids.contains(String.valueOf(user.id))) {
            return true;
        }
        return false;
    }
    
    ///////////////////////// SQL Query /////////////////////////
	public static Resource findById(Long id) {
	    try {
    		Query q = JPA.em().createQuery("SELECT r FROM Resource r where id = ?1");
    		q.setParameter(1, id);
    		return (Resource) q.getSingleResult();
	    } catch (NoResultException e) {
            return null;
        }
	}
	
	public static List<Resource> findAllResourceOfFolder(Long id) {
		Query q = JPA.em().createQuery("SELECT r FROM Resource r where folder.id = ?1");
		q.setParameter(1, id);
		return (List<Resource>) q.getResultList();
	}

	public Folder getFolder() {
		return folder;
	}

	public void setFolder(Folder folder) {
		this.folder = folder;
	}

	public String getResourceName() {
		return resourceName;
	}

	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getPriority() {
		return priority;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
	}

}
