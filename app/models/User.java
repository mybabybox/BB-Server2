package models;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NoResultException;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.StringUtils;

import models.Country.CountryCode;
import models.Post.ConditionType;
import models.TokenAction.Type;

import com.fasterxml.jackson.annotation.JsonIgnore;

import play.Play;
import play.data.format.Formats;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import babybox.shopping.social.exception.SocialObjectNotLikableException;
import be.objectify.deadbolt.core.models.Permission;
import be.objectify.deadbolt.core.models.Role;
import be.objectify.deadbolt.core.models.Subject;

import com.feth.play.module.pa.providers.oauth2.facebook.FacebookAuthProvider;
import com.feth.play.module.pa.providers.oauth2.facebook.FacebookAuthUser;
import com.feth.play.module.pa.providers.password.UsernamePasswordAuthUser;
import com.feth.play.module.pa.user.AuthUser;
import com.feth.play.module.pa.user.AuthUserIdentity;
import com.feth.play.module.pa.user.EmailIdentity;
import com.feth.play.module.pa.user.FirstLastNameIdentity;
import com.google.common.base.Strings;

import common.cache.CalcServer;
import common.collection.Pair;
import common.image.FaceFinder;
import common.utils.DateTimeUtil;
import common.utils.ImageFileUtil;
import common.utils.NanoSecondStopWatch;
import common.utils.StringUtil;
import controllers.Application.DeviceType;
import domain.DefaultValues;
import domain.Followable;
import domain.SocialObjectType;

@Entity
public class User extends SocialObject implements Subject, Followable {
	private static final play.api.Logger logger = play.api.Logger.apply(User.class);

	private static final String STORAGE_USER_NOIMAGE = 
            Play.application().configuration().getString("storage.user.noimage");
	
	private static final String STORAGE_USER_THUMBNAIL_NOIMAGE = 
            Play.application().configuration().getString("storage.user.thumbnail.noimage");
    
	private static final String STORAGE_USER_COVER_NOIMAGE = 
            Play.application().configuration().getString("storage.user.cover.noimage");
    
	public static final Long NO_LOGIN_ID = -1L;
	
	public static final String BB_ADMIN_NAME = "BabyBox 管理員";

	public String firstName;
	public String lastName;
	public String displayName;
	public String email;

	// Targeting info

	@OneToOne
	public UserInfo userInfo;

	// fb info

	@Column(nullable = false, columnDefinition = "TINYINT(1)")
	public boolean fbLogin;

	@Column(nullable = false, columnDefinition = "TINYINT(1)")
	public boolean mobileSignup;

	public DeviceType deviceType;
	
	@OneToOne
	@JsonIgnore
	public FbUserInfo fbUserInfo;

	// stats

	public Long numLikes = 0L;
	
	public Long numFollowings = 0L;

	public Long numFollowers = 0L;

	public Long numProducts = 0L;
	
	public Long numStories = 0L;
	
	public Long numComments = 0L;
	
	public Long numConversationsAsSender = 0L;
	
	public Long numConversationsAsRecipient = 0L;
	
	public Long numCollections = 0L;

	// system

	@JsonIgnore
	@Column(nullable = false, columnDefinition = "TINYINT(1)")
	public boolean active;

	@JsonIgnore
	@Column(nullable = false, columnDefinition = "TINYINT(1)")
    public boolean emailProvidedOnSignup;
	
	@JsonIgnore
	@Column(nullable = false, columnDefinition = "TINYINT(1)")
	public boolean emailValidated;
	
	@JsonIgnore
	@Column(nullable = false, columnDefinition = "TINYINT(1)")
    public boolean accountVerified;

	@JsonIgnore
	@Column(nullable = false, columnDefinition = "TINYINT(1)")
	public boolean newUser;

	@Formats.DateTime(pattern = "yyyy-MM-dd HH:mm:ss")
	@JsonIgnore
	public Date lastLogin;

	@JsonIgnore
	public Long totalLogin = 0L;

	@ManyToMany
	@JsonIgnore
	public List<SecurityRole> roles;

	@OneToMany(cascade = CascadeType.ALL)
	@JsonIgnore
	public List<LinkedAccount> linkedAccounts;

	@ManyToMany
	@JsonIgnore
	public List<UserPermission> permissions;

	@ManyToOne(cascade = CascadeType.REMOVE)
	@JsonIgnore
	public Folder albumPhotoProfile;

	@ManyToOne(cascade = CascadeType.REMOVE)
	@JsonIgnore
	public Folder albumCoverProfile;

	@JsonIgnore
	public String lastLoginUserAgent;

	@Override
	@JsonIgnore
	public String getIdentifier() {
		return Long.toString(id);
	}

	@OneToMany(cascade = CascadeType.REMOVE)
	@JsonIgnore
	public List<Folder> folders;

	@Override
	@JsonIgnore
	public List<? extends Role> getRoles() {
		return roles;
	}

	@Override
	public List<? extends Permission> getPermissions() {
		return permissions;
	}

	public User() {
		this.objectType = SocialObjectType.USER;
	}

	public User(String firstName, String lastName, String displayName) {
		this();
		this.firstName = firstName;
		this.lastName = lastName;
		this.displayName = displayName;
		this.name = firstName;
	}
	
	public void likesOn(SocialObject target)
			throws SocialObjectNotLikableException {
		target.onLikedBy(this);
	}

	public static User searchEmail(String email) {
		CriteriaBuilder builder = JPA.em().getCriteriaBuilder();
		CriteriaQuery<User> criteria = builder.createQuery(User.class);
		Root<User> root = criteria.from(User.class);
		criteria.select(root);
		Predicate predicate = (builder.equal(root.get("email"), email));
		criteria.where(predicate);
		return JPA.em().createQuery(criteria).getSingleResult();
	}

	public boolean hasCompleteInfo() {
	    if (StringUtils.isEmpty(email)) {
	        return false;
	    }
	    return true;
	}
	
	public Resource setPhotoProfile(File file) throws IOException {
		ensureAlbumPhotoProfileExist();

		// Pre-process file to have face centered.
		//BufferedImage croppedImage = FaceFinder.getSquarePictureWithFace(file);
		BufferedImage croppedImage = ImageFileUtil.cropImageFile(file);
		ImageFileUtil.writeFileWithImage(file, croppedImage);

		Resource newPhoto = this.albumPhotoProfile.addFile(file, SocialObjectType.PROFILE_PHOTO);
		this.albumPhotoProfile.setHighPriorityFile(newPhoto);
		newPhoto.save();

		return newPhoto;
	}

	public Resource setCoverPhoto(File file) throws IOException {
		ensureCoverPhotoProfileExist();

		// Pre-process file to have face centered.
		BufferedImage croppedImage = FaceFinder.getRectPictureWithFace(file, 2.29d);
		//BufferedImage croppedImage = ImageFileUtil.readImageFile(file);
		ImageFileUtil.writeFileWithImage(file, croppedImage);

		Resource cover_photo = this.albumCoverProfile.addFile(file, SocialObjectType.COVER_PHOTO);
		this.albumCoverProfile.setHighPriorityFile(cover_photo);
		cover_photo.save();
		return cover_photo;
	}

	public void removePhotoProfile(Resource resource) throws IOException {
		this.albumPhotoProfile.removeFile(resource);
	}

	/**
	 * get the photo profile
	 * 
	 * @return the resource, null if not exist
	 */
	@JsonIgnore
	public Resource getPhotoProfile() {
		if (this.albumPhotoProfile != null) {
			Resource file = this.albumPhotoProfile.getHighPriorityFile();
			if (file != null) {
				return file;
			}
		}
		return null;
	}

	public String getPhotoProfileURL() {
		Resource resource = getPhotoProfile();
		if (resource == null) {
			return "";
		}
		return resource.getPath();
	}

	@JsonIgnore
	public Resource getCoverProfile() {
		if (this.albumCoverProfile != null) {
			Resource file = this.albumCoverProfile.getHighPriorityFile();
			if (file != null) {
				return file;
			}
		}
		return null;
	}

	@JsonIgnore
	public Resource getMiniProfileImage() {
		if (this.albumPhotoProfile != null) {
			Resource file = this.albumPhotoProfile.getHighPriorityFile();
			if (file != null) {
				return file;
			}
		}
		return null;
	}

	public String getCoverProfileURL() {
		Resource resource = getCoverProfile();
		if (resource == null) {
			return "";
		}
		return resource.getPath();
	}

	/**
	 * ensure the existence of the system folder: albumPhotoProfile
	 */
	private void ensureAlbumPhotoProfileExist() {
		if (this.albumPhotoProfile == null) {
			this.albumPhotoProfile = createFolder("profile", "", true);
			this.merge();
		}
	}

	/**
	 * ensure the existence of the system folder: albumPhotoProfile
	 */
	private void ensureCoverPhotoProfileExist() {

		if (this.albumCoverProfile == null) {
			this.albumCoverProfile = createFolder("cover", "", true);
			this.merge();
		}
	}
	
	@Transactional
    public Category createCategory(String name, String description, String icon, int seq) {
		
        if (Strings.isNullOrEmpty(name) || 
        		Strings.isNullOrEmpty(description) || 
                Strings.isNullOrEmpty(icon)) {
            logger.underlyingLogger().warn("Missing parameters to createCategory");
            return null;
        }
        
        Category category = new Category(name, description, this, icon, seq);
        category.save();
        return category;
    }
	
	@Transactional
	public Post createProduct(
	        String title, String body, Category category, Double price, ConditionType conditionType, 
	        Double originalPrice, boolean freeDelivery, CountryCode countryCode, DeviceType deviceType) {
	    
		if (Strings.isNullOrEmpty(title) || 
				Strings.isNullOrEmpty(body) || category == null || price == -1D) {
			logger.underlyingLogger().warn("Missing parameters to createProduct");
			return null;
		}
		
		Post post = new Post(
		        this, title, body, category, price, conditionType, 
		        originalPrice, freeDelivery, countryCode, deviceType);
		post.save();
		
		recordPostProduct(this, post);
		
		this.numProducts++;
		
		return post;
	}
	
	@Transactional
    public Story createStory(String body, DeviceType deviceType) {
        
        if (Strings.isNullOrEmpty(body)) {
            logger.underlyingLogger().warn("Missing parameters to createStory");
            return null;
        }
        
        Story story = new Story(this, body, deviceType);
        story.save();
        
        recordPostStory(this, story);
        
        this.numStories++;
        
        return story;
    }
	
	@Transactional
    public Post editProduct(
            Post post, String title, String body, Category category, Double price, Post.ConditionType conditionType, 
            Double originalPrice, boolean freeDelivery, CountryCode countryCode) {
	    
	    if (post == null || 
	            Strings.isNullOrEmpty(title) || Strings.isNullOrEmpty(body) || 
	            category == null || price == -1D) {
            logger.underlyingLogger().warn("Missing parameters to editProduct");
            return null;
        }
        
        post.title = title;
        post.body = body;
        post.category = category;
        post.price = price;
        post.conditionType = conditionType;
        post.originalPrice = originalPrice;
        post.freeDelivery = freeDelivery;
        post.countryCode = countryCode;
        post.merge();
        
        return post;
    }
	
	@Transactional
    public Post editStory(
            Post post, String body, Category category) {
        
        if (post == null || Strings.isNullOrEmpty(body) || category == null) {
            logger.underlyingLogger().warn("Missing parameters to editStory");
            return null;
        }
        
        post.body = body;
        post.category = category;
        post.merge();
        
        return post;
    }
    
	@Transactional
	public void deletePost(Post post) {
        post.deleted = true;
        post.deletedBy = this;
        if (this.numProducts > 0) {
            this.numProducts--;
        }
        post.save();
	}
	
	/**
	 * create a folder with the type: IMG (contain only image Resource types)
	 * 
	 * @param name
	 * @param description
	 * @param system
	 * @return
	 */
	public Folder createFolder(String name, String description, boolean system) {
		if (ensureAlbumExistWithGivenName(name)) {
			Folder folder = createFolder(name, description,
					SocialObjectType.FOLDER, system);
			folders.add(folder);
			this.merge(); // Add folder to existing User as new albumn
			return folder;
		}
		return null;
	}

	private Folder createFolder(String name, String description,
			SocialObjectType type, boolean system) {

		Folder folder = new Folder(name);
		folder.owner = this;
		folder.name = name;
		folder.description = description;
		folder.objectType = type;
		folder.system = system;
		folder.save();
		return folder;
	}

	private boolean ensureAlbumExistWithGivenName(String name) {
		if (folders != null && folders.contains(new Folder(name))) {
			return false;
		}

		folders = new ArrayList<>();
		return true;
	}
	
	public static boolean existsByAuthUserIdentity(
			final AuthUserIdentity identity) {
		final Query exp;
		if (identity instanceof UsernamePasswordAuthUser) {
			exp = getUsernamePasswordAuthUserFind((UsernamePasswordAuthUser) identity);
		} else {
			exp = getAuthUserFind(identity);
		}
		return exp.getResultList().size() > 0;
	}

	private static Query getAuthUserFind(final AuthUserIdentity identity) {
		Query q = JPA.em().createQuery(
				"SELECT u FROM User u, IN (u.linkedAccounts) l where active = ?1 and l.providerUserId = ?2 and l.providerKey = ?3 and u.deleted = false");
		q.setParameter(1, true);
		q.setParameter(2, identity.getId());
		q.setParameter(3, identity.getProvider());
		return q;
	}

	public static User findByAuthUserIdentity(final AuthUserIdentity identity) {
		if (identity == null) {
			return null;
		}

		if (identity instanceof UsernamePasswordAuthUser) {
			// Bypass login
			if (controllers.Application.isDev() && 
					controllers.Application.LOGIN_BYPASS_ALL == true) {
				return User.findByEmail(identity.getId());
			}
			return findByUsernamePasswordIdentity((UsernamePasswordAuthUser) identity);
		} else {
			try {
				return (User) getAuthUserFind(identity).getSingleResult();
			} catch(NoResultException e) {
				return null;
			} catch (Exception e) {
				logger.underlyingLogger().error("Error in findByAuthUserIdentity", e);
				return null;
			}
		}
	}

	@Transactional
	public static User findByUsernamePasswordIdentity(
			final UsernamePasswordAuthUser identity) {
		try {
			return (User) getUsernamePasswordAuthUserFind(identity)
					.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} catch (Exception e) {
			logger.underlyingLogger().error("Error in findByUsernamePasswordIdentity", e);
			return null;
		}
	}

	@Transactional
	@JsonIgnore
	private static Query getUsernamePasswordAuthUserFind(
			final UsernamePasswordAuthUser identity) {

		Query q = JPA.em().createQuery(
				"SELECT u FROM User u, IN (u.linkedAccounts) l where active = ?1 and email = ?2 and  l.providerKey = ?3 and u.deleted = false");
		q.setParameter(1, true);
		q.setParameter(2, identity.getEmail());
		q.setParameter(3, identity.getProvider());
		return q;
	}

	public void merge(final User otherUser) {
		for (final LinkedAccount acc : otherUser.linkedAccounts) {
			this.linkedAccounts.add(LinkedAccount.create(acc));
		}
		// do all other merging stuff here - like resources, etc.

		// deactivate the merged user that got added to this one
		otherUser.active = false;
		this.merge();
		otherUser.merge();
	}

	public static User create(final AuthUser authUser) {
		final User user = new User();
		user.roles = Collections.singletonList(
				SecurityRole.findByRoleName(SecurityRole.RoleType.USER.name()));
		user.setCreatedDate(new Date());
		user.active = true;
		user.newUser = true;
		user.lastLogin = new Date();
		user.totalLogin = 1L;
		user.fbLogin = false;
		user.emailProvidedOnSignup = true;
		user.emailValidated = true;   // bypass play-authen
		user.accountVerified = false; // verify account post signup
		
		if (authUser instanceof EmailIdentity) {
			final EmailIdentity identity = (EmailIdentity) authUser;
			user.email = identity.getEmail();
			
			if (StringUtils.isEmpty(user.email)) {
			    user.emailProvidedOnSignup = false;
			}
		}

		/* 
		 * User name inherited from SocialObject and it's being used 
		 * in many places. No longer valid as we only shows user display name
		 * now. User name will be set to display name during signup info step.
		 * See Application.doSaveSignupInfo 
		 * 
        if (authUser instanceof NameIdentity) {
            final NameIdentity identity = (NameIdentity) authUser;
            final String name = identity.getName();
            if (name != null) {
                user.name = name;
            }
        }
		*/

		if (authUser instanceof FirstLastNameIdentity) {
			final FirstLastNameIdentity identity = (FirstLastNameIdentity) authUser;
			final String firstName = identity.getFirstName();
			final String lastName = identity.getLastName();
			if (firstName != null) {
				user.firstName = firstName;
			}
			if (lastName != null) {
				user.lastName = lastName;
			}
		}

		if (authUser instanceof FacebookAuthUser) {
			final FacebookAuthUser fbAuthUser = (FacebookAuthUser) authUser;
			FbUserInfo fbUserInfo = new FbUserInfo(fbAuthUser);
			fbUserInfo.save();

			user.fbLogin = true;
			user.fbUserInfo = fbUserInfo;
			//user.emailValidated = fbAuthUser.isVerified();
			user.accountVerified = true;
			user.save();
			
			// save fb friends
			saveFbFriends(authUser, user);
		}

		user.save();
		user.linkedAccounts = Collections.singletonList(
				LinkedAccount.create(authUser).addUser(user));
		//user.saveManyToManyAssociations("roles");
		//user.saveManyToManyAssociations("permissions");

		return user;
	}

	private static void saveFbFriends(final AuthUser authUser, final User user) {
	    // TODO commented out for play 2.4 upgrade
        /*
        final FacebookAuthUser fbAuthUser = (FacebookAuthUser) authUser;
        JsonNode frds = fbAuthUser.getFBFriends();
        
        if (frds.has("data")) {
        	List<FbUserFriend> fbUserFriends = null;
        	try {
        		fbUserFriends = new ObjectMapper().readValue(frds.get("data").traverse(), new TypeReference<List<FbUserFriend>>() {});
        	} catch(Exception e) {
        
        	}
        	for (FbUserFriend frnd : fbUserFriends) {
        		frnd.user = user;
        		frnd.save();
        	}
        	logger.underlyingLogger().info("[u="+user.id+"] saveFbFriends="+fbUserFriends.size());
        }
        */
	}

	public static void merge(final AuthUser oldUser, final AuthUser newUser) {
		User.findByAuthUserIdentity(oldUser).merge(
				User.findByAuthUserIdentity(newUser));
	}

	public void updateLastLoginDate() {
        if (isLoggedIn()) {
            this.lastLogin = new Date();
            this.save();
        }
	}
	
	@JsonIgnore
	public Set<String> getProviders() {
		final Set<String> providerKeys = new HashSet<String>(
				linkedAccounts.size());
		for (final LinkedAccount acc : linkedAccounts) {
			providerKeys.add(acc.providerKey);
		}
		return providerKeys;
	}

	public static void addLinkedAccount(final AuthUser oldUser, final AuthUser newUser) {
		final User u = User.findByAuthUserIdentity(oldUser);
		u.linkedAccounts.add(LinkedAccount.create(newUser));
		u.save();
	}

	public static User findByEmail(final String email) {
		try {
			Query q = JPA.em().createQuery(
					"SELECT u FROM User u where active = ?1 and email = ?2 and deleted = false");
			q.setParameter(1, true);
			q.setParameter(2, email);
			return (User) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} catch (Exception e) {
			logger.underlyingLogger().error("Error in findByEmail", e);
			return null;
		}
	}

	public static User findByFbEmail(final String email) {
		try {
			Query q = JPA.em().createQuery(
					"SELECT u FROM User u where active = ?1 and email = ?2 and providerKey = ?3 and deleted = false");
			q.setParameter(1, true);
			q.setParameter(2, email);
			q.setParameter(3, FacebookAuthProvider.PROVIDER_KEY);
			return (User) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} catch (Exception e) {
			logger.underlyingLogger().error("Error in findByFbEmail", e);
			return null;
		}
	}

	@Transactional
	public boolean isSuperAdmin() {
	    if (roles == null) {
	        return false;
	    }
	    
		for (SecurityRole role : roles) {
			if (SecurityRole.RoleType.SUPER_ADMIN.name().equals(role.roleName)) {
				return true;
			}
		}
		return false;
	}
	
	@Transactional
	public boolean isSystemUser() {
		for (SecurityRole role : roles) {
			if (SecurityRole.RoleType.SYSTEM_USER.name().equals(role.roleName)) {
				return true;
			}
		}
		if (isSuperAdmin()) {
			return true;
		}
		return false;
	}

	@Transactional
    public boolean isPromotedSeller() {
        if (roles == null) {
            return false;
        }
        
        for (SecurityRole role : roles) {
            if (SecurityRole.RoleType.PROMOTED_SELLER.name().equals(role.roleName)) {
                return true;
            }
        }
        return false;
    }
	
	@Transactional
    public boolean isVerifiedSeller() {
        if (roles == null) {
            return false;
        }
        
        for (SecurityRole role : roles) {
            if (SecurityRole.RoleType.VERIFIED_SELLER.name().equals(role.roleName)) {
                return true;
            }
        }
        return false;
    }
	
	public static boolean isEmailExists(String email) {
        final User user = User.findByEmail(email);
        if (user != null) {
            return true;
        }
        return false;
    }
	
	@Transactional
	public boolean isRecommendedSeller() {
	    if (isSystemUser() || newUser || system || !active || deleted) {
	        return false;
	    }
	    
	    boolean isRecommended = isPromotedSeller() || isVerifiedSeller() || 
	            this.numProducts > DefaultValues.MIN_RECOMMENDED_SELLER_PRODUCTS;  // temp conditions
	    
	    // must have profile photo
	    /*
	    if (this.getPhotoProfile() == null) {
	        return false;
	    }
	    */
	    
	    if (this.numProducts < DefaultValues.MIN_RECOMMENDED_SELLER_PRODUCTS) {
	        return false;
	    }
	    
	    return isRecommended;
	}
	
	@Transactional
	public static boolean isDisplayNameExists(String displayName) {
		NanoSecondStopWatch sw = new NanoSecondStopWatch();

		try {
    		Query q = JPA.em().createQuery("SELECT count(u) FROM User u where displayName = ?1 and system = false and deleted = false");
    		q.setParameter(1, displayName);
    		Long count = (Long)q.getSingleResult();
    		if (count > 0) {
    			logger.underlyingLogger().error("[displayName="+displayName+"][count="+count+"] already exists");
    		}
    		boolean exists = count > 0;
    		
    		sw.stop();
            logger.underlyingLogger().info("isDisplayNameExists="+exists+". Took "+sw.getElapsedMS()+"ms");
            return exists;
    	} catch (NoResultException e) {
            return false;
        }
    }

	@Transactional
	public static Long getTodaySignupCount() {
		NanoSecondStopWatch sw = new NanoSecondStopWatch();

		try {
    		Query q = JPA.em().createQuery(
    				"SELECT count(u) FROM User u where " +  
    						"system = false and deleted = false and " + 
    				"CREATED_DATE >= ?1 and CREATED_DATE < ?2");
    		q.setParameter(1, DateTimeUtil.getToday().toDate());
    		q.setParameter(2, DateTimeUtil.getTomorrow().toDate());
    		Long count = (Long)q.getSingleResult();
    
    		sw.stop();
    		logger.underlyingLogger().info("getTodaySignupCount="+count+". Took "+sw.getElapsedMS()+"ms");
    		return count;
		} catch (NoResultException e) {
            return 0L;
        }
	}

	@Transactional
	public static Pair<Integer,String> getAndroidTargetEdmUsers() {
	    try {
    		StringBuilder sb = new StringBuilder();
    
    		Query q = JPA.em().createNativeQuery(
    				"select CONCAT(id,',',email,',',firstName,',',lastName,',') from User where deleted=0 and emailValidated=1 and "+
    						"email is not null and email not like '%abc.com' and email not like '%xxx.com' and firstName is not null and lastName is not null and id not in (1,2,4,5,102,1098,1124,575,1374,1119,1431) "+
    						"and id not in (select g.userId from gameaccounttransaction g where g.transactionDescription like '%APP%') "+
    						"and (lastLoginUserAgent is NULL OR lastLoginUserAgent not like '%iphone%') "+
    				"order by id");
    		List<Object> results = (List<Object>) q.getResultList();
    		for (Object res : results) {
    			if (res instanceof String) {
    				sb.append((String)res).append("\n");
    			} else {
    				try {
    					sb.append(new String((byte[])res, "UTF-8")).append("\n");
    				} catch (Exception e) {
    					logger.underlyingLogger().error("Failed to create string");
    				}
    			}
    		}
    		return new Pair<>(results.size(),sb.toString());
	    } catch (NoResultException e) {
            return new Pair<>();
        }
	}

	@JsonIgnore
	public LinkedAccount getAccountByProvider(final String providerKey) {
		return LinkedAccount.findByProviderKey(this, providerKey);
	}

	public static void verify(final User unverified) {
		// You might want to wrap this into a transaction
		unverified.emailValidated = true;
		unverified.save();
		TokenAction.deleteByUser(unverified, Type.EMAIL_VERIFICATION);
	}

	public void changePassword(final UsernamePasswordAuthUser authUser, final boolean create) {
		LinkedAccount a = this.getAccountByProvider(authUser.getProvider());
		if (a == null) {
			if (create) {
				a = LinkedAccount.create(authUser);
				a.user = this;
			} else {
				throw new RuntimeException(
						"Account not enabled for password usage");
			}
		}
		a.providerUserId = authUser.getHashedPassword();
		a.save();
	}

	public void resetPassword(final UsernamePasswordAuthUser authUser,
			final boolean create) {
		// You might want to wrap this into a transaction
		this.changePassword(authUser, create);
		TokenAction.deleteByUser(this, Type.PASSWORD_RESET);
	}

    public static User findByDisplayName(String displayName) {
        try { 
            Query q = JPA.em().createQuery("SELECT u FROM User u where displayName = ?1 and deleted = false");
            q.setParameter(1, displayName);
            return (User) q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

	public static User findById(Long id) {
		try { 
			Query q = JPA.em().createQuery("SELECT u FROM User u where id = ?1 and deleted = false");
			q.setParameter(1, id);
			return (User) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public static File getDefaultUserPhoto() throws FileNotFoundException {
		return new File(STORAGE_USER_NOIMAGE);
	}

	public static File getDefaultThumbnailUserPhoto() throws FileNotFoundException {
		return new File(STORAGE_USER_THUMBNAIL_NOIMAGE);
	}

	public static File getDefaultCoverPhoto() throws FileNotFoundException {
		return new File(STORAGE_USER_COVER_NOIMAGE);
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Date getLastLogin() {
		return lastLogin;
	}

	public void setLastLogin(Date lastLogin) {
		this.lastLogin = lastLogin;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isEmailProvidedOnSignup() {
        return emailProvidedOnSignup;
    }

    public void setEmailProvidedOnSignup(boolean emailProvidedOnSignup) {
        this.emailProvidedOnSignup = emailProvidedOnSignup;
    }

	public boolean isEmailValidated() {
		return emailValidated;
	}

	public void setEmailValidated(boolean emailValidated) {
		this.emailValidated = emailValidated;
	}

	public boolean isNewUser() {
		return newUser;
	}

	public void setNewUser(boolean newUser) {
		this.newUser = newUser;
	}

	public List<LinkedAccount> getLinkedAccounts() {
		return linkedAccounts;
	}

	public void setLinkedAccounts(List<LinkedAccount> linkedAccounts) {
		this.linkedAccounts = linkedAccounts;
	}

	public Folder getAlbumPhotoProfile() {
		return albumPhotoProfile;
	}

	public void setAlbumPhotoProfile(Folder albumPhotoProfile) {
		this.albumPhotoProfile = albumPhotoProfile;
	}

	public Folder getAlbumCoverProfile() {
		return albumCoverProfile;
	}

	public void setAlbumCoverProfile(Folder albumCoverProfile) {
		this.albumCoverProfile = albumCoverProfile;
	}

	public List<Folder> getFolders() {
		return folders;
	}

	public void setFolders(List<Folder> folders) {
		this.folders = folders;
	}

	public void setRoles(List<SecurityRole> roles) {
		this.roles = roles;
	}

	public void setPermissions(List<UserPermission> permissions) {
		this.permissions = permissions;
	}

	public boolean isLoggedIn() {
		return isLoggedIn(this);
	}

	public static boolean isLoggedIn(User user) {
		return user != null && user.id != NO_LOGIN_ID;
	}

	public static boolean isLoggedIn(Long userId) {
		return userId != NO_LOGIN_ID;
	}

	public static User noLoginUser() {
		User noLoginUser = new User();
		noLoginUser.id = NO_LOGIN_ID;
		return noLoginUser;
	}

	public Collection createCollection(String name) {
	    return createCollection(name, "");
	}
	   
	public Collection createCollection(String name, String description) {
		Collection collection = new Collection(this, name, description);
		collection.save();
		this.numCollections++;
		return collection;
	}

	@Override
	public boolean onFollow(User user) {
		if (logger.underlyingLogger().isDebugEnabled()) {
			logger.underlyingLogger().debug("[localUser="+this.id+"][u="+user.id+"] onFollow");
		}
		
		if (!user.isLoggedIn() || this.id == user.id) {
		    return false;
		}
		
		if (!isFollowing(user)) {
			boolean followed = recordFollow(user);
			if (followed) {
				this.numFollowings++;
				user.numFollowers++;
			} else {
				logger.underlyingLogger().debug(String.format("User [u=%d] already followed User [u=%d]", this.id, user.id));
			}
			return followed;
		}
		return false;
	}

	@Override
	public boolean onUnFollow(User user) {
		if (logger.underlyingLogger().isDebugEnabled()) {
			logger.underlyingLogger().debug("[localUser="+this.id+"][u="+user.id+"] onUnFollow");
		}
		
		if (!user.isLoggedIn()) {
            return false;
        }
		
		if (isFollowing(user)) {
			boolean unfollowed = 
					FollowSocialRelation.unfollow(
							this.id, SocialObjectType.USER, user.id, SocialObjectType.USER);
			if (unfollowed) {
			    if (this.numFollowings > 0) {
			        this.numFollowings--;
			    }
			    if (user.numFollowers > 0) {
			        user.numFollowers--;
			    }
			} else {
				logger.underlyingLogger().debug(String.format("User [u=%d] already unfollowed User [u=%d]", this.id, user.id));
			}
			return unfollowed;
		}
		return false;
	}

	@JsonIgnore
	public boolean isFollowing(User user) {
		return FollowSocialRelation.isFollowing(this.id, SocialObjectType.USER, user.id, SocialObjectType.USER);
	}
	
	@JsonIgnore
	public boolean isFollowedBy(User user) {
	    return CalcServer.instance().isFollowed(user.id, this.id);
		//return FollowSocialRelation.isFollowing(user.id, SocialObjectType.USER, this.id, SocialObjectType.USER);
	}

	public List<Collection> getUserCollection() {
		try {
			Query q = JPA.em().createQuery("SELECT c FROM Collection c where deleted = false and owner = ?");
			q.setParameter(1, this);
			return (List<Collection>) q.getResultList();
		} catch (NoResultException nre) {
			return null;
		}
	}

	/**
	 * Select c.id, (count(p.id)/(Select count(*) from ViewSocialRelation vr where vr.actor = ?1))*100 
	 * from ViewSocialRelation vsr, post p, category c 
	 * where vsr.actor = ?1 and vsr.target = p.id and p.category_id = c.id group by c.id
	 * @return
	 */
	public Map<Long, Integer> getUserCategoriesRatioForFeed() {
	    NanoSecondStopWatch sw = new NanoSecondStopWatch();
	    logger.underlyingLogger().debug(String.format("[u=%d] getUserCategoriesForFeed()", this.id));
	    
	    Map<Long, Integer> result = new HashMap<>();
	    try {
    		Query q = JPA.em().createNativeQuery("Select c.id, (count(p.id)/(Select count(*) from ViewSocialRelation vr where vr.actor = ?1))*100 "
    				+ "from ViewSocialRelation vsr, post p, category c "
    				+ "where vsr.actor = ?1 and vsr.target = p.id and p.category_id = c.id  "
    				+ "group by c.id");
    		q.setParameter(1, this);
    		List<Object[]> feeds = q.getResultList();
    		for (Object[] feed : feeds) {
    		    BigInteger catId = (BigInteger)feed[0];
    		    BigDecimal percent = (BigDecimal)feed[1];
    			result.put(catId.longValue(), percent.intValue());
    			logger.underlyingLogger().debug("   catId="+catId+" %="+percent);
    		}
	    } catch (NoResultException nre) {
        }
	    
	    sw.stop();
        if (logger.underlyingLogger().isDebugEnabled()) {
            logger.underlyingLogger().debug(String.format("[u=%d] getUserCategoriesForFeed(). Took "+sw.getElapsedMS()+"ms", this.id));
        }
        
		return result;
	}

	public static List<User> getEligibleUsersForFeed() {
		// TODO query only eligible user
		try {
			Query q = JPA.em().createQuery("SELECT u FROM User u where deleted = false");
			return (List<User>) q.getResultList();
		} catch (NoResultException nre) {
			return null;
		}
	}

	public List<Post> getUserLikedPosts() {
		try {
			Query query = JPA.em().createQuery(
					"Select p from Post p where p.id in " + 
							"(select pr.target from LikeSocialRelation pr " + 
					"where pr.actor = ?1 and pr.actorType = ?2 and pr.targetType = ?3) and p.deleted = false order by UPDATED_DATE desc");
			query.setParameter(1, this.id);
			query.setParameter(2, SocialObjectType.USER);
			query.setParameter(3, SocialObjectType.POST);
			return (List<Post>)query.getResultList();
		} catch (NoResultException nre) {
			return null;
		}
	}

	public List<Post> getUserPosts() {
		try {
			Query q = JPA.em().createQuery("SELECT p FROM Post p where owner = ?1 and deleted = false");
			q.setParameter(1, this);
			return (List<Post>) q.getResultList();
		} catch (NoResultException nre) {
			return null;
		}
	}
	
	public static List<User> getUsersBySignup(Long offset) {
        Query q = JPA.em().createQuery("Select u from User u where deleted = false order by CREATED_DATE desc");
        try {
            q.setFirstResult((int) (offset * DefaultValues.DEFAULT_INFINITE_SCROLL_COUNT));
            q.setMaxResults(DefaultValues.DEFAULT_INFINITE_SCROLL_COUNT);
            return (List<User>) q.getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }
	
	public static List<User> getUsersByLogin(Long offset) {
        Query q = JPA.em().createQuery("Select u from User u where deleted = false order by lastLogin desc");
        try {
            q.setFirstResult((int) (offset * DefaultValues.DEFAULT_INFINITE_SCROLL_COUNT));
            q.setMaxResults(DefaultValues.DEFAULT_INFINITE_SCROLL_COUNT);
            return (List<User>) q.getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }
	
	public static List<User> getUsers(List<Long> ids) {
        try {
             Query query = JPA.em().createQuery(
                        "select u from User u where "+
                        "u.id in ("+StringUtil.collectionToString(ids, ",")+") and "+
                        "u.deleted = false ORDER BY FIELD(u.id,"+StringUtil.collectionToString(ids, ",")+")");
             return (List<User>) query.getResultList();
        } catch (NoResultException nre) {
            return null;
        }
    }
    
    public static List<User> getUsers(List<Long> ids, int offset) {
        try {
             Query query = JPA.em().createQuery(
                     "select u from User u where "+
                             "u.id in ("+StringUtil.collectionToString(ids, ",")+") and "+
                             "u.deleted = false ORDER BY FIELD(u.id,"+StringUtil.collectionToString(ids, ",")+")");
             query.setFirstResult(offset * CalcServer.FEED_RETRIEVAL_COUNT);
             query.setMaxResults(CalcServer.FEED_RETRIEVAL_COUNT);
             return (List<User>) query.getResultList();
        } catch (NoResultException nre) {
            return null;
        }
    }
}
