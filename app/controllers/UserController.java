package controllers;

import static play.data.Form.form;
import handler.FeedHandler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import models.Activity;
import models.Collection;
import models.Comment;
import models.Conversation;
import models.ConversationOrder;
import models.FollowSocialRelation;
import models.GameBadge;
import models.GameBadgeAwarded;
import models.Location;
import models.Message;
import models.NotificationCounter;
import models.Post;
import models.PushNotificationToken;
import models.Resource;
import models.Settings;
import models.SocialRelation;
import models.User;
import models.Conversation.OrderTransactionState;
import models.GameBadge.BadgeType;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;

import babybox.shopping.social.exception.SocialObjectNotCommentableException;
import play.data.DynamicForm;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import providers.MyUsernamePasswordAuthProvider;
import service.SocialRelationHandler;
import viewmodel.ActivityVM;
import viewmodel.AdminConversationVM;
import viewmodel.CollectionVM;
import viewmodel.ConversationOrderVM;
import viewmodel.ConversationVM;
import viewmodel.GameBadgeVM;
import viewmodel.MessageVM;
import viewmodel.NotificationCounterVM;
import viewmodel.PostVMLite;
import viewmodel.ResponseStatusVM;
import viewmodel.SellerVM;
import viewmodel.UserVM;
import viewmodel.UserVMLite;
import common.cache.CalcServer;
import common.model.FeedFilter.FeedType;
import common.utils.HtmlUtil;
import common.utils.HttpUtil;
import common.utils.ImageFileUtil;
import common.utils.NanoSecondStopWatch;
import common.utils.StringUtil;
import common.utils.ValidationUtil;
import controllers.Application.DeviceType;
import domain.DefaultValues;
import domain.HighlightColor;
import domain.SocialObjectType;

public class UserController extends Controller {
    private static final play.api.Logger logger = play.api.Logger.apply(UserController.class);

    @Inject
    FeedHandler feedHandler;

    @Inject
    CalcServer calcServer;
    
    public static String getMobileUserKey(final play.mvc.Http.Request r, final Object key) {
		final String[] m = r.queryString().get(key);
		if(m != null && m.length > 0) {
			try {
				return URLDecoder.decode(m[0], "UTF-8");
			} catch (UnsupportedEncodingException e) {
				logger.underlyingLogger().error("Error in getMobileUserKey", e);
			}
		}
		return null;
	}
    
	@Transactional
	public Result getUserInfo() {
	    NanoSecondStopWatch sw = new NanoSecondStopWatch();
	    
		final User localUser = Application.getLocalUser(session());
		if (localUser == null) {
			return notFound();
		}
		
		// build queues if not yet built
		calcServer.buildQueuesForUser(localUser);
		calcServer.buildUserExploreFeedQueueIfNotExistAsync(localUser);
		
		// update last login and user agent
		localUser.updateLastLoginDate();
		Application.setMobileUserAgent(localUser);
		
		UserVM userInfo = new UserVM(localUser);
		
		sw.stop();
        if (logger.underlyingLogger().isDebugEnabled()) {
            logger.underlyingLogger().debug("[u="+localUser.getId()+"] getUserInfo(). Took "+sw.getElapsedMS()+"ms");
        }
		return ok(Json.toJson(userInfo));
	}
	
	@Transactional
	public Result getUserInfoById(Long id) {
	    NanoSecondStopWatch sw = new NanoSecondStopWatch();
	    
		final User localUser = Application.getLocalUser(session());
		final User user = User.findById(id);
		if (localUser == null || user == null) {
			return notFound();
		}
		
		// build queues if not yet built
        calcServer.buildQueuesForUser(user);
        
		UserVM userVM = new UserVM(user, localUser);
		
		sw.stop();
        if (logger.underlyingLogger().isDebugEnabled()) {
            logger.underlyingLogger().debug("[u="+localUser.getId()+"] getUserInfo(). Took "+sw.getElapsedMS()+"ms");
        }
		return ok(Json.toJson(userVM));
	}
	
	@Transactional
	public static Result uploadProfilePhoto() {
		final User localUser = Application.getLocalUser(session());
		if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
		logger.underlyingLogger().info("STS [u="+localUser.id+"] uploadProfilePhoto");

		FilePart image = HttpUtil.getMultipartFormDataFile(request().body().asMultipartFormData(), "profile-photo");
		String fileName = image.getFilename();
	    try {
	        boolean firstTimeUploadProfilePhoto = (localUser.getPhotoProfile() == null);
	        
            File fileTo = ImageFileUtil.copyImageFileToTemp(image.getFile(), fileName);
			localUser.setPhotoProfile(fileTo);
			
			if (firstTimeUploadProfilePhoto) {
    			// game badge
    	        GameBadgeAwarded.recordGameBadge(localUser, BadgeType.PROFILE_PHOTO);
			}
		} catch (IOException e) {
		    logger.underlyingLogger().error("["+localUser.id+"] Error in uploadProfilePhoto", e);
			return badRequest();
		}
		return ok();
	}
	
	@Transactional
	public static Result uploadCoverPhoto() {
		final User localUser = Application.getLocalUser(session());
		if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
		
		logger.underlyingLogger().info("STS [u="+localUser.id+"] uploadCoverPhoto");

		FilePart image = HttpUtil.getMultipartFormDataFile(request().body().asMultipartFormData(), "profile-photo");
		String fileName = image.getFilename();
	    try {
	    	File fileTo = ImageFileUtil.copyImageFileToTemp(image.getFile(), fileName);
			localUser.setCoverPhoto(fileTo);
		} catch (IOException e) {
		    logger.underlyingLogger().error("["+localUser.id+"] Error in uploadCoverPhoto", e);
			return badRequest();
		}
		return ok();
	}
	
	@Transactional
	public static Result getProfileImage() {
	    response().setHeader("Cache-Control", "max-age=1");
	    final User localUser = Application.getLocalUser(session());
		
		if(User.isLoggedIn(localUser) && localUser.getPhotoProfile() != null) {
			return ok(localUser.getPhotoProfile().getRealFile());
		}
		
		try {
			return ok(User.getDefaultUserPhoto());
		} catch (FileNotFoundException e) {
			return ok("no image set");
		}
	}
	
	@Transactional
	public static Result getCoverImage() {
	    response().setHeader("Cache-Control", "max-age=1");
	    final User localUser = Application.getLocalUser(session());
		
		if(User.isLoggedIn(localUser) && localUser.getCoverProfile() != null) {
			return ok(localUser.getCoverProfile().getRealFile());
		}
		
		try {
			return ok(User.getDefaultCoverPhoto());
		} catch (FileNotFoundException e) {
			return ok("no image set");
		}
	}

	@Transactional
	public static Result editUserInfo() {
	    final User localUser = Application.getLocalUser(session());
	    
	    logger.underlyingLogger().info(String.format("[u=%d] editUserInfo", localUser.id));
	    
	    // Basic info
	    DynamicForm form = DynamicForm.form().bindFromRequest();
	    String email = form.get("email");
	    String displayName = form.get("displayName");
	    String firstName = form.get("firstName");
	    String lastName = form.get("lastName");
	    String aboutMe = form.get("aboutMe");
	    if (StringUtils.isEmpty(displayName) || StringUtils.isEmpty(firstName) || StringUtils.isEmpty(lastName)) {
	        logger.underlyingLogger().error(String.format(
	                "[u=%d][displayName=%s][firstName=%s][lastName=%s] displayName, firstName or lastName missing", 
	                localUser.id, displayName, firstName, lastName));
            return badRequest("請填寫您的顯示名稱與姓名");
        }
	    
	    displayName = displayName.trim();
	    firstName = firstName.trim();
	    lastName = lastName.trim();
	    if (aboutMe != null) {
	        aboutMe = aboutMe.trim();
	    }
	    
	    if (!localUser.displayName.equals(displayName)) {
	        if (StringUtil.hasWhitespace(displayName)) {
                logger.underlyingLogger().error(String.format(
                        "[u=%d][displayName=%s] displayName contains whitespace", localUser.id, displayName));
                return badRequest("\""+displayName+"\" 不可有空格");
            }
	        if (!ValidationUtil.isDisplayNameValid(displayName)) {
                logger.underlyingLogger().error(String.format(
                        "[u=%d][displayName=%s] displayName incorrect format", localUser.id, displayName));
                return badRequest("\""+displayName+"\" 格式不正確");
	        }
	        if (User.isDisplayNameExists(displayName)) {
                logger.underlyingLogger().error(String.format(
                        "[u=%d][displayName=%s] displayName already exists", localUser.id, displayName));
                return badRequest("\""+displayName+"\" 已被選用。請選擇另一個顯示名稱重試");
            }
        }
        
	    // Email - handle email ONLY for fb signup with no email provided
	    boolean emailAllowedToChange = localUser.fbLogin && !localUser.emailProvidedOnSignup;
        if (emailAllowedToChange && 
                (localUser.email == null || !localUser.email.equals(email))) {
            if (StringUtils.isEmpty(email)) {
                logger.underlyingLogger().error(
                        String.format("[u=%d] email is missing", localUser.id));
                return badRequest("請填寫電郵");
            }
            if (StringUtil.hasWhitespace(email)) {
                logger.underlyingLogger().error(String.format(
                        "[u=%d][email=%s] email contains whitespace", localUser.id, email));
                return badRequest("\""+email+"\" 不可有空格");
            }
            if (!ValidationUtil.isEmailValid(email)) {
                logger.underlyingLogger().error(String.format(
                        "[u=%d][email=%s] email incorrect format", localUser.id, email));
                return badRequest("\""+email+"\" 格式不正確");
            }
            if (User.isEmailExists(email)) {
                logger.underlyingLogger().error(
                        String.format("[u=%d][email=%s] email already exists", 
                                localUser.id, email));
                return badRequest("\""+email+"\" 已登記。請確認你的電郵重試");
            }
            
            // set email
            localUser.email = email;
        }
        
		// UserInfo
        Location location = Location.getLocationById(Integer.valueOf(form.get("location")));
        if (location == null) {
            logger.underlyingLogger().error(String.format(
                    "[u=%d][birthYear=%s][location=%s] location missing", localUser.id, location.displayName));
            return badRequest("請填寫地區");
        }
        
        localUser.displayName = displayName;
        localUser.name = displayName;
        localUser.firstName = firstName;
        localUser.lastName = lastName;
        
        localUser.userInfo.location = location;
        localUser.userInfo.aboutMe = aboutMe;
        localUser.userInfo.save();
        localUser.save();
        
        if (localUser.hasCompleteInfo()) {
            GameBadgeAwarded.recordGameBadge(localUser, BadgeType.PROFILE_INFO);
        }
        
        return ok(Json.toJson(new UserVM(localUser)));
	}
    
	@Transactional
    public static Result editUserNotificationSettings() {
	    NanoSecondStopWatch sw = new NanoSecondStopWatch();
	    
        final User localUser = Application.getLocalUser(session());
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
        
        Settings settings = Settings.findByUserId(localUser.id);
        try {
            DynamicForm form = form().bindFromRequest();
            settings.emailNewPost = Boolean.parseBoolean(form.get("emailNewPost"));
            settings.emailNewConversation = Boolean.parseBoolean(form.get("emailNewConversation"));
            settings.emailNewComment = Boolean.parseBoolean(form.get("emailNewComment"));
            settings.emailNewPromotions = Boolean.parseBoolean(form.get("emailNewPromotions"));
            settings.pushNewConversation = Boolean.parseBoolean(form.get("pushNewConversation"));
            settings.pushNewComment = Boolean.parseBoolean(form.get("pushNewComment"));
            settings.pushNewFollow = Boolean.parseBoolean(form.get("pushNewFollow"));
            settings.pushNewFeedback = Boolean.parseBoolean(form.get("pushNewFeedback"));
            settings.pushNewPromotions = Boolean.parseBoolean(form.get("pushNewPromotions"));
            settings.save();
        } catch (Exception e) {
            logger.underlyingLogger().error("["+localUser.id+"] Error in editUserNotificationSettings", e);
            return badRequest();
        }
        
        sw.stop();
        logger.underlyingLogger().info("[u="+localUser.id+"] editUserNotificationSettings. Took "+sw.getElapsedMS()+"ms");
        return ok(Json.toJson(new UserVM(localUser)));
    }
    
    @Transactional
	public static Result getProfileImageById(Long id) {
        response().setHeader("Cache-Control", "max-age=1");
        User user = User.findById(id);
    	
		if(User.isLoggedIn(user) && user.getPhotoProfile() != null) {
			return ok(user.getPhotoProfile().getRealFile());
		}
		
		try {
			return ok(User.getDefaultUserPhoto());
		} catch (FileNotFoundException e) {
			return ok("no image set");
		}
	}
    
	@Transactional
	public static Result getCoverImageById(Long id) {
	    response().setHeader("Cache-Control", "max-age=1");
	    User user = User.findById(id);

	    if(User.isLoggedIn(user) && user.getCoverProfile() != null) {
			return ok(user.getCoverProfile().getRealFile());
		}
		try {
			return ok(User.getDefaultCoverPhoto());
		} catch (FileNotFoundException e) {
			return ok("no image set");
		}
	}
	
	@Transactional
	public static Result getMiniProfileImageById(Long id) {
	    response().setHeader("Cache-Control", "max-age=1");
		final User user = User.findById(id);
		
		if(User.isLoggedIn(user) && user.getPhotoProfile() != null) {
			return ok(user.getPhotoProfile().getMiniFile());
		} 
		
		try {
			return ok(User.getDefaultThumbnailUserPhoto());
		} catch (FileNotFoundException e) {
			return ok("no image set");
		}
	}
	
	@Transactional
	public static Result getThumbnailProfileImageById(Long id) {
	    response().setHeader("Cache-Control", "max-age=1");
		final User user = User.findById(id);
		
		if(User.isLoggedIn(user) && user.getPhotoProfile() != null) {
			return ok(new File(user.getPhotoProfile().getThumbnail()));
		}
		
		try {
			return ok(User.getDefaultThumbnailUserPhoto());
		} catch (FileNotFoundException e) {
			return ok("no image set");
		}
	}
	
	@Transactional
	public static Result getThumbnailCoverImageById(Long id) {
	    response().setHeader("Cache-Control", "max-age=1");
		final User user = User.findById(id);
		
		if(User.isLoggedIn(user) && user.getCoverProfile() != null) {
			return ok(new File(user.getCoverProfile().getThumbnail()));
		}
		
		try {
			return ok(User.getDefaultCoverPhoto());
		} catch (FileNotFoundException e) {
			return ok("no image set");
		}
	}
	
	@Transactional
	public static Result getMessages(Long conversationId, Long offset) {
        NanoSecondStopWatch sw = new NanoSecondStopWatch();

		final User localUser = Application.getLocalUser(session());
		if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
		
		List<MessageVM> vms = new ArrayList<>();
		Conversation conversation = Conversation.findById(conversationId); 
		List<Message> messages =  conversation.getMessages(localUser, offset);
		if (messages != null) {
			for (Message message : messages) {
				MessageVM vm = new MessageVM(message);
				vms.add(vm);
			}
		}
		Map<String, Object> map = new HashMap<>();
		map.put("messages", vms);

        sw.stop();
        logger.underlyingLogger().info("[u="+localUser.id+"][c="+conversationId+"] getMessages(offset="+offset+") size="+vms.size()+". Took "+sw.getElapsedMS()+"ms");
		return ok(Json.toJson(map));
	}
	
	@Transactional
    public static Result getMessagesForAdmin(Long conversationId, Long offset) {
        NanoSecondStopWatch sw = new NanoSecondStopWatch();

        final User localUser = Application.getLocalUser(session());
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
        
        List<MessageVM> vms = new ArrayList<>();
        Conversation conversation = Conversation.findById(conversationId); 
        List<Message> messages =  conversation.getMessagesForAdmin(offset);
        if (messages != null) {
            for (Message message : messages) {
                MessageVM vm = new MessageVM(message);
                vms.add(vm);
            }
        }
        Map<String, Object> map = new HashMap<>();
        map.put("messages", vms);

        sw.stop();
        logger.underlyingLogger().info("[u="+localUser.id+"][c="+conversationId+"] getMessagesForAdmin(offset="+offset+") size="+vms.size()+". Took "+sw.getElapsedMS()+"ms");
        return ok(Json.toJson(map));
    }
	
	@Transactional
    public static Result newMessage() {
        final User localUser = Application.getLocalUser(session());
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
        
        try {
	        Http.MultipartFormData multipartFormData = request().body().asMultipartFormData();
			Long conversationId = HttpUtil.getMultipartFormDataLong(multipartFormData, "conversationId");
		    String body = HttpUtil.getMultipartFormDataString(multipartFormData, "body");
		    Boolean system = HttpUtil.getMultipartFormDataBoolean(multipartFormData, "system");
	        
		    if (system == null) {
		        system = false;
		    }
		    
	        Message message = newMessage(conversationId, localUser, body, system);
	        
	        List<FilePart> images = HttpUtil.getMultipartFormDataFiles(multipartFormData, "image", DefaultValues.MAX_MESSAGE_IMAGES);
	        for (FilePart image : images) {
				String fileName = image.getFilename();
				File file = image.getFile();
				File fileTo = ImageFileUtil.copyImageFileToTemp(file, fileName);
				message.addMessagePhoto(fileTo, localUser);
			}
	        
	        MessageVM vm = new MessageVM(message);
	        return ok(Json.toJson(vm));
		} catch (IOException e) {
			logger.underlyingLogger().error("["+localUser.id+"] Error in newMessage", e);
		}
        
        return badRequest();
    }
	
	@Transactional
	public static Message newMessage(Long conversationId, User sender, String body, boolean system) {
        Conversation conversation = Conversation.findById(conversationId);
        return conversation.addMessage(sender, body, system);
    }
	
	@Transactional
    public static Result deleteConversation(Long id) {
		final User localUser = Application.getLocalUser(session());
		if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
		
        Conversation.archiveConversation(id, localUser);
        return ok();
    }

	@Transactional
	public static Result getConversations() {
        NanoSecondStopWatch sw = new NanoSecondStopWatch();

		final User localUser = Application.getLocalUser(session());
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }

		List<ConversationVM> vms = new ArrayList<>();
        List<Conversation> conversations = Conversation.getUserConversations(localUser);
        if (conversations != null && conversations.size() > 0) {
            Long count = 0L;
            for (Conversation conversation : conversations) {
                // archived, dont show
                if (conversation.isArchivedBy(localUser)) {
                    continue;
                }

                ConversationVM vm = new ConversationVM(conversation, localUser);
                vms.add(vm);
                
                if (vm.unread > 0) {
                    count++;
                }
            }
            NotificationCounter.setConversationsCount(localUser.id, count);
        } else {
            NotificationCounter.resetConversationsCount(localUser.id);            
        }
        
        sw.stop();
        logger.underlyingLogger().info("[u="+localUser.id+"] getConversations. Took "+sw.getElapsedMS()+"ms");
		return ok(Json.toJson(vms));
	}
	
	@Transactional
    public static Result getUserConversations(Long offset) {
	    NanoSecondStopWatch sw = new NanoSecondStopWatch();
	    
        final User localUser = Application.getLocalUser(session());
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
        
        List<ConversationVM> vms = new ArrayList<>();
        List<Conversation> conversations = Conversation.getUserConversations(localUser, offset);
        if (conversations != null && conversations.size() > 0) {
            for (Conversation conversation : conversations) {
                // archived, dont show
                if (conversation.isArchivedBy(localUser)) {
                    continue;
                }

                ConversationVM vm = new ConversationVM(conversation, localUser);
                vms.add(vm);
            }
        } else if (offset == 0) {
            NotificationCounter.resetConversationsCount(localUser.id);
        }
        
        sw.stop();
        logger.underlyingLogger().info("[u="+localUser.id+"][offset="+offset+"] getUserConversations. Took "+sw.getElapsedMS()+"ms");
        return ok(Json.toJson(vms));
    }
	
	@Transactional
    public static Result getLatestConversations(Long offset) {
        NanoSecondStopWatch sw = new NanoSecondStopWatch();

        final User localUser = Application.getLocalUser(session());
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
        
        if (!localUser.isSuperAdmin()) {
            logger.underlyingLogger().error(String.format("[u=%d] User is not super admin. Failed to get latest conversations !!", localUser.id));
            return badRequest();
        }

        List<AdminConversationVM> vms = new ArrayList<>();
        List<Conversation> conversations = Conversation.getLatestConversations(offset);
        if (conversations != null && conversations.size() > 0) {
            for (Conversation conversation : conversations) {
                AdminConversationVM vm = new AdminConversationVM(conversation);
                vms.add(vm);
            }
        }
        
        sw.stop();
        logger.underlyingLogger().info("[u="+localUser.id+"] getLatestConversations. Took "+sw.getElapsedMS()+"ms");
        return ok(Json.toJson(vms));
    }
	
	@Transactional
	public static Result getConversation(Long id) {
        NanoSecondStopWatch sw = new NanoSecondStopWatch();

		final User localUser = Application.getLocalUser(session());
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
        
		Conversation conversation = Conversation.findById(id);
		if (conversation == null) {
			return notFound();
		}

		if (conversation.user1.id != localUser.id && conversation.user2.id != localUser.id && !localUser.isSuperAdmin()) {
            logger.underlyingLogger().error(String.format("[u=%d][conv=%d] User is not owner of conversation. Failed to get conversation !!", localUser.id, conversation.id));
            return badRequest();
        }
		
        sw.stop();
        logger.underlyingLogger().info("[u="+localUser.id+"] getConversation. Took "+sw.getElapsedMS()+"ms");
		return ok(Json.toJson(new ConversationVM(conversation, localUser)));
	}
	
	@Transactional
    public static Result openConversation(Long postId) {
		NanoSecondStopWatch sw = new NanoSecondStopWatch();
		
        final User localUser = Application.getLocalUser(session());
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
        
        Post post = Post.findById(postId);
        if (post == null) {
        	logger.underlyingLogger().error(String.format("[p=%d][u1=%d][u2=%d] Post not exist. Will not open conversation", postId, localUser.id, post.owner.id));
            return notFound();
        }
        if (localUser.id == post.owner.id) {
            logger.underlyingLogger().error(String.format("[p=%d][u1=%d][u2=%d] Same user. Will not open conversation", postId, localUser.id, post.owner.id));
            return badRequest();
        }
        
        // New conversation always opened by buyer
        Conversation conversation = Conversation.openConversation(post, localUser);
        ConversationVM conversationVM = new ConversationVM(conversation, localUser);
        
        sw.stop();
        if (logger.underlyingLogger().isDebugEnabled()) {
            logger.underlyingLogger().debug(String.format("[p=%d][u1=%d][u2=%d] openConversation. Took "+sw.getElapsedMS()+"ms", postId, localUser.id, post.owner.id));
        }
		
		return ok(Json.toJson(conversationVM));
    }
	
	@Transactional
	public static Result uploadMessagePhoto() {
		final User localUser = Application.getLocalUser(session());
		if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
		
        DynamicForm form = DynamicForm.form().bindFromRequest();
        try {
        	Long messageId = Long.valueOf(form.get("messageId"));
        	Message message = Message.findById(Long.valueOf(messageId));
        	
	        FilePart image = HttpUtil.getMultipartFormDataFile(request().body().asMultipartFormData(), "send-photo0");
	        String fileName = image.getFilename();
            File fileTo = ImageFileUtil.copyImageFileToTemp(image.getFile(), fileName);
            Long id = message.addMessagePhoto(fileTo,localUser).id;
            return ok(id.toString());
        } catch (NumberFormatException e) {
            logger.underlyingLogger().error("["+localUser.id+"] Error in uploadMessagePhoto", e);
            return badRequest();
        } catch (IOException e) {
            logger.underlyingLogger().error("["+localUser.id+"] Error in uploadMessagePhoto", e);
            return badRequest();
        }
    }
	
	@Transactional
	public static Result getUnreadMessageCount() {
		final User localUser = Application.getLocalUser(session());
		if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
		
		Map<String, Long> vm = new HashMap<>();
		vm.put("count", Conversation.getUnreadConversationCount(localUser));
		return ok(Json.toJson(vm));
	}
	
	@Transactional
    public static Result updateConversationNote() {
        final User localUser = Application.getLocalUser(session());
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
        
        DynamicForm form = form().bindFromRequest();
        Long conversationId = Long.parseLong(form.get("conversationId"));
        String body = form.get("body");
        
        Conversation conversation = Conversation.findById(conversationId);
        if (conversation == null) {
            return notFound();
        }
        
        try {
            conversation.note = body;
            conversation.save();
        } catch (Exception e) {
            logger.underlyingLogger().error("["+localUser.id+"] Error in updateConversationNote", e);
            return notFound();
        }
        
        return ok();
    }
	
	@Transactional
	public static Result updateConversationOrderTransactionState(Long id, String state) {
	    final User localUser = Application.getLocalUser(session());
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
        
        Conversation conversation = Conversation.findById(id);
        if (conversation == null) {
            return notFound();
        }
        
        try {
            OrderTransactionState orderTransactionState = OrderTransactionState.valueOf(state);
            conversation.orderTransactionState = orderTransactionState;
            conversation.save();
        } catch (Exception e) {
            logger.underlyingLogger().error("["+localUser.id+"] Error in updateConversationOrderTransactionState", e);
            return notFound();
        }
        
	    return ok();
	}
	
	@Transactional
	public static Result highlightConversation(Long id, String color) {
	    final User localUser = Application.getLocalUser(session());
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
        
        Conversation conversation = Conversation.findById(id);
        if (conversation == null) {
            return notFound();
        }
        
        try {
            HighlightColor highlightColor = HighlightColor.valueOf(color);
            conversation.highlightColor = highlightColor;
            conversation.save();
        } catch (Exception e) {
            logger.underlyingLogger().error("["+localUser.id+"] Error in highlightConversation", e);
            return notFound();
        }
        
	    return ok();
	}

	@Transactional
	public static Result getMessageImageById(Long id) {
	    response().setHeader("Cache-Control", "max-age=604800");
	    final User localUser = Application.getLocalUser(session());
	    if (localUser == null) {
            String userKey = UserController.getMobileUserKey(request(), Application.APP_USER_KEY);
            logger.underlyingLogger().error(String.format("[key=%s] getOriginalMessageImageById() User is null", userKey));
            return notFound();
        }
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] getOriginalMessageImageById() User not logged in", localUser.id));
            return notFound();
        }
        
        Resource resource = Resource.findById(id);
        if (resource != null && 
                (resource.isUserAuthorized(localUser) || localUser.isSuperAdmin())) {
            return ok(resource.getThumbnailFile());
        }
        
        logger.underlyingLogger().error(String.format("[u=%d][res=%d] getMessageImageById() User not authorized", localUser.id, id));
        return unauthorized();
	}

    @Transactional
    public static Result getOriginalMessageImageById(Long id) {
        response().setHeader("Cache-Control", "max-age=604800");
        final User localUser = Application.getLocalUser(session());
        if (localUser == null) {
            String userKey = UserController.getMobileUserKey(request(), Application.APP_USER_KEY);
            logger.underlyingLogger().error(String.format("[key=%s] getOriginalMessageImageById() User is null", userKey));
            return notFound();
        }
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] getOriginalMessageImageById() User not logged in", localUser.id));
            return notFound();
        }
        
        Resource resource = Resource.findById(id);
        if (resource != null && 
                (resource.isUserAuthorized(localUser) || localUser.isSuperAdmin())) {
            return ok(resource.getRealFile());
        }
        
        logger.underlyingLogger().error(String.format("[u=%d][res=%d] getOriginalMessageImageById() User not authorized", localUser.id, id));
        return unauthorized();
    }

    @Transactional
    public static Result getMiniMessageImageById(Long id) {
    	final User localUser = Application.getLocalUser(session());
    	if (localUser == null) {
            String userKey = UserController.getMobileUserKey(request(), Application.APP_USER_KEY);
            logger.underlyingLogger().error(String.format("[key=%s] getOriginalMessageImageById() User is null", userKey));
            return notFound();
        }
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] getOriginalMessageImageById() User not logged in", localUser.id));
            return notFound();
        }
        
        Resource resource = Resource.findById(id);
        if (resource != null && 
                (resource.isUserAuthorized(localUser) || localUser.isSuperAdmin())) {
            return ok(resource.getMiniFile());
        }
        
        logger.underlyingLogger().error(String.format("[u=%d][res=%d] getMiniMessageImageById() User not authorized", localUser.id, id));
        return unauthorized();
    }
	
    @Transactional
    public static Result inviteByEmail(String email) {
		final User localUser = Application.getLocalUser(session());
		if (!localUser.isLoggedIn()) {
		    logger.underlyingLogger().info("Not signed in. Skipped signup invitation to: "+email);
        } else {
            /*GameAccount gameAccount = GameAccount.findByUserId(localUser.id);
            gameAccount.sendInvitation(email);*/
        }
		return ok();
	}
  
    @Transactional
    public Result viewProfile(Long id) {
        NanoSecondStopWatch sw = new NanoSecondStopWatch();
        
        final User localUser = Application.getLocalUser(session());
        User user = User.findById(id);
        if (user == null) {
            logger.underlyingLogger().warn(String.format("[user=%d][u=%d] User not found", id, localUser.id));
            return Application.pathNotFound();
        }

        // build queues if not yet built
        calcServer.buildQueuesForUser(user);
        
		sw.stop();
        if (logger.underlyingLogger().isDebugEnabled()) {
            logger.underlyingLogger().debug("[u="+id+"] viewProfile(). Took "+sw.getElapsedMS()+"ms");
        }
        
		String metaTags = Application.generateHeaderMeta(user.displayName, "", "/image/get-profile-image-by-id/"+user.getId());
		return ok(views.html.babybox.web.profile.render(
    	        Json.stringify(Json.toJson(new UserVM(user,localUser))), 
    	        Json.stringify(Json.toJson(new UserVM(localUser))),
    	        metaTags));
    }
    
    @Transactional 
    public Result getRecommendedSellers() {
        final User localUser = Application.getLocalUser(session());
        List<UserVMLite> vms = feedHandler.getFeedUsers(localUser.id, 0L, localUser, FeedType.USER_RECOMMENDED_SELLERS);
        return ok(Json.toJson(vms));
    }
    
    @Transactional 
    public Result getRecommendedSellersFeed(Long offset) {
        final User localUser = Application.getLocalUser(session());
        List<SellerVM> vms = feedHandler.getFeedSellers(localUser.id, offset, localUser, FeedType.USER_RECOMMENDED_SELLERS);
        return ok(Json.toJson(vms));
    }
    
	@Transactional 
	public Result getHomeExploreFeed(Long offset) {
		final User localUser = Application.getLocalUser(session());
		if (offset == 0) {
            calcServer.buildQueuesForUser(localUser);
        }
		List<PostVMLite> vms = feedHandler.getFeedPosts(localUser.id, offset, localUser, FeedType.HOME_EXPLORE);
		return ok(Json.toJson(vms));
	}
	
	@Transactional 
	public Result getHomeFollowingFeed(Long offset) {
		final User localUser = Application.getLocalUser(session());
		if (!localUser.isLoggedIn()) {
			logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
			return notFound();
		}
		
		if (offset == 0) {
            calcServer.buildQueuesForUser(localUser);
        }
		List<PostVMLite> vms = feedHandler.getFeedPosts(localUser.id, offset, localUser, FeedType.HOME_FOLLOWING);
		return ok(Json.toJson(vms));
	}
	
    @Transactional
    public Result getUserPostedFeed(Long id, Long offset) {
    	final User localUser = Application.getLocalUser(session());
        List<PostVMLite> vms = feedHandler.getFeedPosts(id, offset, localUser, FeedType.USER_POSTED);
		return ok(Json.toJson(vms));
    }
    
    @Transactional
    public Result getUserLikedFeed(Long id, Long offset){
        final User localUser = Application.getLocalUser(session());
        List<PostVMLite> vms = feedHandler.getFeedPosts(id, offset, localUser, FeedType.USER_LIKED);
        return ok(Json.toJson(vms));
    }
    
    @Transactional
    public static Result getUserCollections(Long id) {
    	List<CollectionVM> vms = new ArrayList<>();
		for(Collection collection : Collection.getUserProductCollections(id)) {
			CollectionVM vm = new CollectionVM(collection);
			vms.add(vm);
		}
		return ok(Json.toJson(vms));
    }
    
    @Transactional
    public static Result getCollection(Long id) {
        Collection collection = Collection.findById(id);
        return ok(Json.toJson(new CollectionVM(collection)));
    }
    
    @Transactional
    public static Result followUser(Long id) {
    	final User localUser = Application.getLocalUser(session());
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return redirect("/login");
        }
        
        final User user = User.findById(id);
        if (user == null) {
            logger.underlyingLogger().error(String.format("[u=%d] User is null", id));
            return notFound();
        }
        
        SocialRelationHandler.recordFollowUser(localUser, user);
		return ok();
    }
    
    @Transactional
    public static Result unfollowUser(Long id) {
    	final User localUser = Application.getLocalUser(session());
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return redirect("/login");
        }
        
        final User user = User.findById(id);
        if (user == null) {
            logger.underlyingLogger().error(String.format("[u=%d] User is null", id));
            return notFound();
        }
        
        SocialRelationHandler.recordUnFollowUser(localUser, user);
		return ok();
    }
    
    @Transactional
    public Result viewFollowings(Long id) {
    	final User localUser = Application.getLocalUser(session());
        User user = User.findById(id);
        if (user == null) {
            logger.underlyingLogger().warn(String.format("[user=%d][u=%d] User not found", id, localUser.id));
            return Application.pathNotFound();
        }
        
        List<UserVMLite> userFollowings = getFollowings(id, 0L, localUser);
    	return ok(views.html.babybox.web.followers.render(
    	        Json.stringify(Json.toJson(userFollowings)), 
    	        Json.stringify(Json.toJson(new UserVM(user,localUser))),
    	        Json.stringify(Json.toJson(new UserVM(localUser)))));
    }
    
    @Transactional
    public Result viewFollowers(Long id) {
    	final User localUser = Application.getLocalUser(session());    
    	User user = User.findById(id);
        if (user == null) {
            logger.underlyingLogger().warn(String.format("[user=%d][u=%d] User not found", id, localUser.id));
            return Application.pathNotFound();
        }
        
        List<UserVMLite> userFollowers = getFollowers(id, 0L, localUser);
    	return ok(views.html.babybox.web.followers.render(
    	        Json.stringify(Json.toJson(userFollowers)),
    	        Json.stringify(Json.toJson(new UserVM(user,localUser))),
    	        Json.stringify(Json.toJson(new UserVM(localUser)))));
    }
    
    @Transactional
    public Result getFollowings(Long id, Long offset) {
    	final User localUser = Application.getLocalUser(session());
    	List<UserVMLite> userFollowings = getFollowings(id, offset, localUser);
    	return ok(Json.toJson(userFollowings));
    }
    
    @Transactional
    public Result getFollowers(Long id,Long offset) {
    	final User localUser = Application.getLocalUser(session());   
    	List<UserVMLite> userFollowers = getFollowers(id, offset, localUser);
    	return ok(Json.toJson(userFollowers));
    }
    
    public List<UserVMLite> getFollowings(Long id, Long offset, User localUser) {
        return feedHandler.getFeedUsers(id, offset, localUser, FeedType.USER_FOLLOWINGS);
        /*
        List<FollowSocialRelation> followings = FollowSocialRelation.getUserFollowings(id, offset);
        List<UserVMLite> userFollowings = new ArrayList<UserVMLite>();
        for (SocialRelation socialRelation : followings) {
            User user = User.findById(socialRelation.target);
            if (user != null) {
                UserVMLite uservm = new UserVMLite(user, localUser);
                userFollowings.add(uservm);
            }
        }
        return userFollowings;
        */
    }
    
    public List<UserVMLite> getFollowers(Long id, Long offset, User localUser) {
        return feedHandler.getFeedUsers(id, offset, localUser, FeedType.USER_FOLLOWERS);
        /*
        List<FollowSocialRelation> followers = FollowSocialRelation.getUserFollowers(id, offset);
        List<UserVMLite> userFollowers = new ArrayList<UserVMLite>();
        for(SocialRelation socialRelation : followers){
            User user = User.findById(socialRelation.actor);
            if (user != null) {
                UserVMLite uservm = new UserVMLite(user, localUser);
                userFollowers.add(uservm);
            }
        }
        return userFollowers;
        */
    }
    
    @Transactional
    public static Result getNotificationCounter() {
    	final User localUser = Application.getLocalUser(session());
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
        
        NotificationCounter counter = NotificationCounter.getNotificationCounter(localUser.id);
        if (counter != null) {
        	return ok(Json.toJson(new NotificationCounterVM(counter)));
        }
        return ok();
    }
    
    @Transactional
    public static Result resetActivitiesCount() {
    	final User localUser = Application.getLocalUser(session());
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
        
        NotificationCounter.resetActivitiesCount(localUser.id);
        return ok();
    }
    
    @Transactional
    public static Result resetConversationsCount() {
    	final User localUser = Application.getLocalUser(session());
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
        
        NotificationCounter.resetConversationsCount(localUser.id);
        return ok();
    }
    
    @Transactional
    public static Result getActivities(Long offset){
    	User localUser = Application.getLocalUser(session());
    	if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
    	
    	List<Activity> activities = Activity.getActivities(localUser.id, offset);
    	List<ActivityVM> vms = new ArrayList<>();
		for (Activity activity : activities) {
			ActivityVM vm = new ActivityVM(activity);
			vms.add(vm);
			
			// mark read
			if (!activity.viewed) {
    			activity.viewed = true;
    			activity.save();
			}
		}
		
		// reset notification counter for the recipient
        if (offset == 0) {
            resetActivitiesCount();
        }
        
		return ok(Json.toJson(vms));
	}
    
    @Transactional
    public static Result newConversationOrder() {
        try {
            DynamicForm form = form().bindFromRequest();
            Long conversationId = Long.parseLong(form.get("conversationId"));
            Double offeredPrice = Double.parseDouble(form.get("offeredPrice"));
            return newConversationOrder(conversationId, offeredPrice);
        } catch (Exception e) {
            return badRequest();
        }
    }
    
    // obsolete
    @Transactional
    public static Result newConversationOrder2(Long conversationId) {
        return newConversationOrder(conversationId, -1D);
    }
    
    @Transactional
    private static Result newConversationOrder(Long conversationId, Double offeredPrice) {
        NanoSecondStopWatch sw = new NanoSecondStopWatch();
        
        User localUser = Application.getLocalUser(session());
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
        
        Conversation conversation = Conversation.findById(conversationId);
        if (conversation == null) {
            logger.underlyingLogger().error(String.format("[conv=%d] Conversation not found", conversationId));
            return notFound();
        }
        
        // default price 
        if (offeredPrice < 0) {
            offeredPrice = conversation.post.price;
        }
        
        ConversationOrder order = ConversationOrder.getActiveOrder(conversation);
        if (order == null || order.isOrderClosed()) {
            // close out old order
            if (order != null) {
                order.active = false;
                order.save();
            }
            
            ConversationOrder newOrder = new ConversationOrder(conversation, offeredPrice);
            newOrder.save();
            
            sw.stop();
            if (logger.underlyingLogger().isDebugEnabled()) {
                logger.underlyingLogger().debug("[u="+localUser.getId()+"][conv="+conversation.id+"][price="+newOrder.offeredPrice+"][order="+newOrder.id+"] newConversationOrder(). Took "+sw.getElapsedMS()+"ms");
            }
            return ok(Json.toJson(new ConversationOrderVM(newOrder, localUser)));
        }
        return badRequest();
    }
    
    @Transactional
    public static Result cancelConversationOrder(Long id) {
        NanoSecondStopWatch sw = new NanoSecondStopWatch();
        
        User localUser = Application.getLocalUser(session());
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
        
        ConversationOrder order = ConversationOrder.findById(id);
        if (!order.active) {
            return badRequest();
        }
        
        if (order != null && !order.isOrderClosed()) {
            order.cancelled = true;
            order.cancelDate = new Date();
            order.save();
            
            sw.stop();
            if (logger.underlyingLogger().isDebugEnabled()) {
                logger.underlyingLogger().debug("[u="+localUser.getId()+"][conv="+order.conversation.id+"][order="+order.id+"] cancelConversationOrder(). Took "+sw.getElapsedMS()+"ms");
            }
            return ok(Json.toJson(new ConversationOrderVM(order, localUser)));
        }
        return badRequest();
    }
    
    @Transactional
    public static Result acceptConversationOrder(Long id) {
        NanoSecondStopWatch sw = new NanoSecondStopWatch();
        
        User localUser = Application.getLocalUser(session());
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
        
        ConversationOrder order = ConversationOrder.findById(id);
        if (!order.active) {
            return badRequest();
        }
        
        if (order != null && !order.isOrderClosed()) {
            order.accepted = true;
            order.acceptDate = new Date();
            order.save();
            
            sw.stop();
            if (logger.underlyingLogger().isDebugEnabled()) {
                logger.underlyingLogger().debug("[u="+localUser.getId()+"][conv="+order.conversation.id+"][order="+order.id+"] acceptConversationOrder(). Took "+sw.getElapsedMS()+"ms");
            }
            return ok(Json.toJson(new ConversationOrderVM(order, localUser)));
        }
        return badRequest();
    }
    
    @Transactional
    public static Result declineConversationOrder(Long id) {
        NanoSecondStopWatch sw = new NanoSecondStopWatch();
        
        User localUser = Application.getLocalUser(session());
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
        
        ConversationOrder order = ConversationOrder.findById(id);
        if (!order.active) {
            return badRequest();
        }
        
        if (order != null && !order.isOrderClosed()) {
            order.declined = true;
            order.declineDate = new Date();
            order.save();
            
            sw.stop();
            if (logger.underlyingLogger().isDebugEnabled()) {
                logger.underlyingLogger().debug("[u="+localUser.getId()+"][conv="+order.conversation.id+"][order="+order.id+"] declineConversationOrder(). Took "+sw.getElapsedMS()+"ms");
            }
            return ok(Json.toJson(new ConversationOrderVM(order, localUser)));
        }
        return badRequest();
    }
    
    @Transactional
    public static Result saveApnToken(String token, String appVersion) {
        return savePushNotificationToken(token, appVersion, DeviceType.IOS);
    }
    
    @Transactional
    public static Result saveGcmToken(String token, String appVersion) {
        return savePushNotificationToken(token, appVersion, DeviceType.ANDROID);
    }
    
    @Transactional
    public static Result saveApnKey(String key, Long versionCode) {
        return savePushNotificationToken(key, versionCode.toString(), DeviceType.IOS);
    }
    
    @Transactional
    public static Result saveGcmKey(String key, Long versionCode) {
        return savePushNotificationToken(key, versionCode.toString(), DeviceType.ANDROID);
    }
    
    @Transactional
    public static Result savePushNotificationToken(String token, String appVersion, DeviceType deviceType) {
        NanoSecondStopWatch sw = new NanoSecondStopWatch();
        
        User localUser = Application.getLocalUser(session());
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }

        PushNotificationToken.createUpdateToken(localUser.id, token, appVersion, deviceType);
        
        sw.stop();
        if (logger.underlyingLogger().isDebugEnabled()) {
            logger.underlyingLogger().debug("[u="+localUser.getId()+"][token="+token+"][appVersion="+appVersion+"][deviceType="+deviceType.toString()+"] savePushNotifictionTokenKey(). Took "+sw.getElapsedMS()+"ms");
        }
        return ok();
    }
    
    @Transactional
    public static Result deleteAccount(Long id){
        NanoSecondStopWatch sw = new NanoSecondStopWatch();
        
        User localUser = Application.getLocalUser(session());
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }

        if (!localUser.isSuperAdmin()) {
            logger.underlyingLogger().error(String.format("[u=%d] User is not super admin. Failed to delete account !!", localUser.id));
            return badRequest();
        }
        
        User user = User.findById(id);
        if (user != null) {
            user.deleted = true;
            user.save();    
        }
        
        sw.stop();
        if (logger.underlyingLogger().isDebugEnabled()) {
            logger.underlyingLogger().debug("[u="+localUser.getId()+"] deleteAccount(). Took "+sw.getElapsedMS()+"ms");
        }
        return ok();
    }
    
    @Transactional
    public static Result getUsersBySignup(Long offset) {
        final User localUser = Application.getLocalUser(session());
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
        
        if (!localUser.isSuperAdmin()) {
            logger.underlyingLogger().error(String.format("[u=%d] User is not super admin. Failed to get users by signup !!", localUser.id));
            return badRequest();
        }
        
        List<User> users = User.getUsersBySignup(offset);
        List<UserVMLite> vms = new ArrayList<>();
        for (User user : users) {
            if (user != null) {
                UserVMLite vm = new UserVMLite(user, localUser);
                vms.add(vm);
            }
        }
        return ok(Json.toJson(vms));
    }
    
    @Transactional
    public static Result getUsersByLogin(Long offset) {
        final User localUser = Application.getLocalUser(session());
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
        
        if (!localUser.isSuperAdmin()) {
            logger.underlyingLogger().error(String.format("[u=%d] User is not super admin. Failed to get users by login !!", localUser.id));
            return badRequest();
        }
        
        List<User> users = User.getUsersByLogin(offset);
        List<UserVMLite> vms = new ArrayList<>();
        for (User user : users) {
            if (user != null) {
                UserVMLite vm = new UserVMLite(user, localUser);
                vms.add(vm);
            }
        }
        return ok(Json.toJson(vms));
    }
    
    @Transactional
    public static Result getGameBadges(Long userId) {
        return getGameBadges(userId, false);
    }
    
    @Transactional
    public static Result getGameBadgesAwarded(Long userId) {
        return getGameBadges(userId, true);        
    }
    
    private static Result getGameBadges(Long userId, Boolean awardedOnly) {
        NanoSecondStopWatch sw = new NanoSecondStopWatch();
        
        Map<Long, GameBadgeAwarded> map = new HashMap<>();
        for (GameBadgeAwarded badgeAwarded : GameBadgeAwarded.getGameBadgesAwarded(userId)) {
            map.put(badgeAwarded.gameBadgeId, badgeAwarded);
        }
        
        List<GameBadgeVM> vms = new ArrayList<>();
        GameBadgeVM vm = null;
        for (GameBadge gameBadge : GameBadge.getAllGameBadges()) {
            if (map.containsKey(gameBadge.id)) {
                vm = new GameBadgeVM(gameBadge, map.get(gameBadge.id));
            } else if (!awardedOnly) {
                vm = new GameBadgeVM(gameBadge);
            }
            vms.add(vm);
        }
        
        sw.stop();
        if (logger.underlyingLogger().isDebugEnabled()) {
            logger.underlyingLogger().debug("[u="+userId+"][awardedOnly="+awardedOnly+"] getGameBadges() returns "+vms.size()+" badges. Took "+sw.getElapsedMS()+"ms");
        }
        return ok(Json.toJson(vms));
    }
}