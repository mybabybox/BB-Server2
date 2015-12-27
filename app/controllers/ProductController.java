package controllers;

import static play.data.Form.form;
import handler.FeedHandler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;

import babybox.shopping.social.exception.SocialObjectNotCommentableException;
import models.Category;
import models.Collection;
import models.Comment;
import models.Conversation;
import models.Post;
import models.ReportedPost;
import models.Post.ConditionType;
import models.Resource;
import models.User;
import play.Play;
import play.data.DynamicForm;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import service.SocialRelationHandler;
import viewmodel.CommentVM;
import viewmodel.ConversationVM;
import viewmodel.PostVM;
import viewmodel.PostVMLite;
import viewmodel.ReportedPostVM;
import viewmodel.ResponseStatusVM;
import viewmodel.UserVM;
import common.model.FeedFilter.FeedType;
import common.utils.HtmlUtil;
import common.utils.HttpUtil;
import common.utils.ImageFileUtil;
import common.utils.NanoSecondStopWatch;
import controllers.Application.DeviceType;
import domain.CountryCode;
import domain.DefaultValues;
import domain.ReportedType;
import domain.SocialObjectType;

public class ProductController extends Controller{
	private static play.api.Logger logger = play.api.Logger.apply(ProductController.class);
	
	@Inject
    FeedHandler feedHandler;
    
	@Transactional
	public static Result createProductWithForm() {
		DynamicForm dynamicForm = DynamicForm.form().bindFromRequest();
		String catId = dynamicForm.get("catId");
	    String title = dynamicForm.get("title");
	    String body = dynamicForm.get("body");
	    String price = dynamicForm.get("price");
	    String conditionType = dynamicForm.get("conditionType");
	    Boolean freeDelivery = Boolean.valueOf(dynamicForm.get("freeDelivery"));
	    String countryCode = dynamicForm.get("countryCode");
	    String deviceType = dynamicForm.get("deviceType");
	    List<FilePart> images = request().body().asMultipartFormData().getFiles();
		return createProduct(
		        title, body, Long.parseLong(catId), Double.parseDouble(price), Post.parseConditionType(conditionType), images, 
		        freeDelivery, Post.parseCountryCode(countryCode), Application.parseDeviceType(deviceType));
	}
	
	@Transactional
	public static Result createProduct() {
	    Http.MultipartFormData multipartFormData = request().body().asMultipartFormData();
        Long catId = HttpUtil.getMultipartFormDataLong(multipartFormData, "catId");
        String title = HttpUtil.getMultipartFormDataString(multipartFormData, "title");
	    String body = HttpUtil.getMultipartFormDataString(multipartFormData, "body");
	    Double price = HttpUtil.getMultipartFormDataDouble(multipartFormData, "price");
	    String conditionType = HttpUtil.getMultipartFormDataString(multipartFormData, "conditionType");
	    Boolean freeDelivery = HttpUtil.getMultipartFormDataBoolean(multipartFormData, "freeDelivery");
	    String countryCode = HttpUtil.getMultipartFormDataString(multipartFormData, "countryCode");
	    String deviceType = HttpUtil.getMultipartFormDataString(multipartFormData, "deviceType");
	    List<FilePart> images = HttpUtil.getMultipartFormDataFiles(multipartFormData, "image", DefaultValues.MAX_POST_IMAGES);
	    
	    if (catId == null) {
	        catId = -1L;
	    }
	    
	    if (price == null) {
	        price = -1D;
	    }
	    
	    if (freeDelivery == null) {
	        freeDelivery = false;
	    }
	    
	    if (countryCode == null) {
	        countryCode = CountryCode.NA.name();
	    }
	    
		return createProduct(
		        title, body, catId, price, Post.parseConditionType(conditionType), images, 
		        freeDelivery, Post.parseCountryCode(countryCode), Application.parseDeviceType(deviceType));
	}

	private static Result createProduct(
	        String title, String body, Long catId, Double price, ConditionType conditionType,
	        List<FilePart> images, Boolean freeDelivery, CountryCode countryCode, DeviceType deviceType) {
	    
	    NanoSecondStopWatch sw = new NanoSecondStopWatch();
	    
		final User localUser = Application.getLocalUser(session());
		if (!localUser.isLoggedIn()) {
			logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
			return notFound();
		}
		
		Category category = Category.findById(catId);
        if (category == null) {
            return notFound();
        }
        
		try {
			Post newPost = localUser.createProduct(title, body, category, price, conditionType, freeDelivery, countryCode, deviceType);
			if (newPost == null) {
				return badRequest("Failed to create product. Invalid parameters.");
			}
			
			for (FilePart image : images) {
				String fileName = image.getFilename();
				File fileTo = ImageFileUtil.copyImageFileToTemp(image.getFile(), fileName);
				newPost.addPostPhoto(fileTo);
			}
			
			SocialRelationHandler.recordNewPost(newPost, localUser);
			ResponseStatusVM response = new ResponseStatusVM(SocialObjectType.POST, newPost.id, localUser.id, true);
			
			sw.stop();
	        if (logger.underlyingLogger().isDebugEnabled()) {
	            logger.underlyingLogger().debug("[u="+localUser.getId()+"][p="+newPost.id+"] createProduct(). Took "+sw.getElapsedMS()+"ms");
	        }
	        
			return ok(Json.toJson(response));
		} catch (IOException e) {
			logger.underlyingLogger().error("Error in createProduct", e);
		}
		
		return badRequest();
	}
	
	@Transactional
    public static Result editProductWithForm() {
        DynamicForm dynamicForm = DynamicForm.form().bindFromRequest();
        String id = dynamicForm.get("id");
        String catId = dynamicForm.get("catId");
        String title = dynamicForm.get("title");
        String body = dynamicForm.get("body");
        String price = dynamicForm.get("price");
        String conditionType = dynamicForm.get("conditionType");
        Boolean freeDelivery = Boolean.valueOf(dynamicForm.get("freeDelivery"));
        String countryCode = dynamicForm.get("countryCode");
        return editProduct(Long.parseLong(id), title, body, Long.parseLong(catId), 
                Double.parseDouble(price), Post.parseConditionType(conditionType), 
                freeDelivery, Post.parseCountryCode(countryCode));
    }
    
    @Transactional
    public static Result editProduct() {
        Http.MultipartFormData multipartFormData = request().body().asMultipartFormData();
        Long id = HttpUtil.getMultipartFormDataLong(multipartFormData, "id");
        Long catId = HttpUtil.getMultipartFormDataLong(multipartFormData, "catId");
        String title = HttpUtil.getMultipartFormDataString(multipartFormData, "title");
        String body = HttpUtil.getMultipartFormDataString(multipartFormData, "body");
        Double price = HttpUtil.getMultipartFormDataDouble(multipartFormData, "price");
        String conditionType = HttpUtil.getMultipartFormDataString(multipartFormData, "conditionType");
        Boolean freeDelivery = HttpUtil.getMultipartFormDataBoolean(multipartFormData, "freeDelivery");
        String countryCode = HttpUtil.getMultipartFormDataString(multipartFormData, "countryCode");
        
        if (id == null) {
            id = -1L;
        }

        if (catId == null) {
            catId = -1L;
        }

        if (price == null) {
            price = -1D;
        }
        
        if (freeDelivery == null) {
            freeDelivery = false;
        }
        
        if (countryCode == null) {
            countryCode = CountryCode.NA.name();
        }
        
        return editProduct(id, title, body, catId, price, Post.parseConditionType(conditionType), 
                freeDelivery, Post.parseCountryCode(countryCode));
    }

    private static Result editProduct(
            Long id, String title, String body, Long catId, Double price, Post.ConditionType conditionType, 
            Boolean freeDelivery, CountryCode countryCode) {
        
        NanoSecondStopWatch sw = new NanoSecondStopWatch();
        
        final User localUser = Application.getLocalUser(session());
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
        
        Post post = Post.findById(id);
        if (post == null) {
            logger.underlyingLogger().error(String.format("[u=%d][p=%d] editProduct() Post not found", localUser.id, id));
            return notFound();
        }
        
        Category oldCategory = post.category;
        Category category = Category.findById(catId);
        if (category == null) {
            logger.underlyingLogger().error(String.format("[u=%d][p=%d][cat=%d] editProduct() Category not found", localUser.id, id, catId));
            return notFound();
        }
        
        post = localUser.editProduct(post, title, body, category, price, conditionType, freeDelivery, countryCode);
        if (post == null) {
            logger.underlyingLogger().error(String.format("[u=%d][p=%d] editProduct() Failed to edit post", localUser.id, id));
            return badRequest();
        }
        
        // category changed, handle event
        if (catId != oldCategory.id) {
            SocialRelationHandler.recordEditPost(post, oldCategory);
        }
        ResponseStatusVM response = new ResponseStatusVM(SocialObjectType.POST, post.id, localUser.id, true);
        
        sw.stop();
        if (logger.underlyingLogger().isDebugEnabled()) {
            logger.underlyingLogger().debug("[u="+localUser.getId()+"][p="+id+"] editProduct(). Took "+sw.getElapsedMS()+"ms");
        }
        
        return ok(Json.toJson(response));
    }

	@Transactional
	public static Result createCollection() {
		final User localUser = Application.getLocalUser(session());
		DynamicForm form = DynamicForm.form().bindFromRequest();	
		Collection newCollection = localUser.createCollection(form.get("name"), form.get("description"));
		if (newCollection == null) {
			return badRequest("Failed to create Collection. Invalid parameters.");
		}
		return ok(Json.toJson(newCollection.id));
	}
	
	@Transactional
	public static Result addToCollection() {
		final User localUser = Application.getLocalUser(session());
		DynamicForm dynamicForm = DynamicForm.form().bindFromRequest();
		Long productId = Long.parseLong(dynamicForm.get("product_id"));
		Long collectionId = Long.parseLong(dynamicForm.get("collectionId"));
		Collection collection = null;
		if(collectionId == 0){
			String collectionName = dynamicForm.get("collectionName");
			collection = localUser.createCollection(collectionName);
		} else {
			collection = Collection.findById(collectionId);
		}
		collection.products.add(Post.findById(productId));
		return ok();
	}

	private static List<PostVMLite> getPostVMsFromPosts(List<Post> posts) {
		final User localUser = Application.getLocalUser(session());
		if (!localUser.isLoggedIn()) {
			logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
			return null;
		}
		
		List<PostVMLite> vms = new ArrayList<>();
		for (Post product : posts) {
			PostVMLite vm = new PostVMLite(product, localUser);
			vms.add(vm);
		}
		return vms;
	}
	
	@Transactional
	public static Result getProductImageById(Long id) {
		response().setHeader("Cache-Control", "max-age=604800");
		Resource resource = Resource.findById(id);
		if (resource == null || resource.getThumbnailFile() == null) {
			return ok();
		}
		return ok(resource.getThumbnailFile());
	}

	@Transactional
	public static Result getOriginalProductImageById(Long id) {
		response().setHeader("Cache-Control", "max-age=604800");
		Resource resource = Resource.findById(id);
		if (resource == null || resource.getRealFile() == null) {
			return ok();
		}
		return ok(resource.getRealFile());
	}
	
	@Transactional
	public static Result getMiniProductImageById(Long id) {
		response().setHeader("Cache-Control", "max-age=604800");
		Resource resource = Resource.findById(id);
		if (resource == null || resource.getMini() == null) {
			return ok();
		}
		return ok(new File(resource.getMini()));
	}

	@Transactional
	public Result viewProduct(Long id) {
		final User localUser = Application.getLocalUser(session());
		PostVM product = getProductInfoVM(id);
		Map<String, List<String>> images = new HashMap<>();
		List<String> originalImages = new ArrayList<>();
		List<String> miniImages = new ArrayList<>();
		for(Long imageId : product.images){
			originalImages.add("background-image:url('"+Application.APPLICATION_BASE_URL+"/image/get-post-image-by-id/"+imageId+"')");
		}
		images.put("original", originalImages);
		List<PostVMLite> suggestedPosts = feedHandler.getPostVM(id, 0l, localUser, FeedType.PRODUCT_SUGGEST);
		
		String metaTags = Application.generateHeaderMeta(product.title, product.body, "/image/get-post-image-by-id/"+product.images[0]);
		return ok(views.html.babybox.web.product.render(
		        Json.stringify(Json.toJson(product)), 
		        Json.stringify(Json.toJson(new UserVM(localUser))), 
		        images, 
		        Json.stringify(Json.toJson(suggestedPosts)),
		        metaTags));
	}
	
	@Transactional
	public Result getProductInfo(Long id) {
		PostVM post = getProductInfoVM(id);
		if (post == null) {
			return notFound();
		}
		return ok(Json.toJson(post));
	}
	
	public static PostVM getProductInfoVM(Long id) {
		User localUser = Application.getLocalUser(session());
		Post post = Post.findById(id);
		if (post == null) {
			return null;
		}
		onView(post, localUser);
		PostVM vm = new PostVM(post, localUser);
		return vm;
	}

	@Transactional
	public static Result likePost(Long id) {
		User localUser = Application.getLocalUser(session());
		if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
		
		Post post = Post.findById(id);
		SocialRelationHandler.recordLikePost(post, localUser);
		return ok();
	}

	@Transactional
	public static Result unlikePost(Long id) {
		User localUser = Application.getLocalUser(session());
		if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
		
		Post post = Post.findById(id); 
		SocialRelationHandler.recordUnLikePost(post, localUser);
		return ok();
	}

	@Transactional
	public static Result soldPost(Long id) {
		User localUser = Application.getLocalUser(session());
		if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
		
		Post post = Post.findById(id);
		if (post == null) {
		    return notFound();
		}
		
		if (post.owner.id == localUser.id || localUser.isSuperAdmin()) {
			SocialRelationHandler.recordSoldPost(post, localUser);
		}
		return ok();
	}

	@Transactional
	public static Result newComment() {
		final User localUser = Application.getLocalUser(session());
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
        
		DynamicForm form = form().bindFromRequest();
		Long postId = Long.parseLong(form.get("postId"));
		String body = form.get("body");
		
		Post post = Post.findById(postId);
		try {
			Comment comment = (Comment) post.onComment(localUser, body);
			
			// set device
			DeviceType deviceType = Application.parseDeviceType(form.get("deviceType"));
			comment.deviceType = deviceType;
			
			SocialRelationHandler.recordNewComment(comment, post);
			ResponseStatusVM response = new ResponseStatusVM(SocialObjectType.COMMENT, comment.id, comment.owner.id, true);
			return ok(Json.toJson(response));
		} catch (SocialObjectNotCommentableException e) {
		}
		return badRequest();
	}
	
	@Transactional
    public static Result deletePost(Long id) {
        final User localUser = Application.getLocalUser(session());
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
        
        Post post = Post.findById(id);

        if (logger.underlyingLogger().isDebugEnabled()) {
            logger.underlyingLogger().debug(String.format("[u=%d][c=%d][p=%d] deletePost", localUser.id, post.category.id, id));
        }

        if (localUser.equals(post.owner) || 
                localUser.isSuperAdmin()) {
        	localUser.deleteProduct(post);
        	SocialRelationHandler.recordDeletePost(post, localUser);
            return ok();
        }
        return badRequest("Failed to delete post. [u=" + localUser.id + "] not owner of post [id=" + id + "].");
    }
    
    @Transactional
    public static Result deleteComment(Long id) {
        final User localUser = Application.getLocalUser(session());
        if (logger.underlyingLogger().isDebugEnabled()) {
            logger.underlyingLogger().debug(String.format("[u=%d][cmt=%d] deleteComment", localUser.id, id));
        }

        try {
	        Comment comment = Comment.findById(id);
	        if (localUser.equals(comment.owner) ||
	                localUser.isSuperAdmin()) {
	        	Post post = Post.findById(comment.socialObject);
	            post.onDeleteComment(localUser, comment);
	            SocialRelationHandler.recordDeleteComment(comment, post);
	            return ok();
	        }
        } catch (SocialObjectNotCommentableException e) {
		}
        return badRequest("Failed to delete comment. [u="+localUser.id+"] not owner of comment [id=" + id + "].");
    }

	@Transactional
	public static Result onView(Post post, User localUser) {
		if (!localUser.isLoggedIn()) {
			return notFound();
		}
		
		SocialRelationHandler.recordViewPost(post, localUser);
		return ok();
	}
	
	@Transactional 
	public Result getSuggestedProducts(Long id) {
		final User localUser = Application.getLocalUser(session());
		if (!localUser.isLoggedIn()) {
			logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
			return notFound();
		}
		List<PostVMLite> vms = feedHandler.getPostVM(id, 0l, localUser, FeedType.PRODUCT_SUGGEST);
		return ok(Json.toJson(vms));
	}

	public static List<CommentVM> getComments(User user, Post post, Long offset){
		List<CommentVM> comments = new ArrayList<CommentVM>();
		for (Comment comment : post.getPostComments(offset)) {
			CommentVM commentVM = new CommentVM(comment, user);
			comments.add(commentVM);
		}
		return comments;
	}
	
	
	@Transactional
	public static Result getPostComments(Long id, Long offset) {
		final User localUser = Application.getLocalUser(session());
		if (!localUser.isLoggedIn()) {
			logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
			return notFound();
		}
		
		Post post = Post.findById(id);
		if (post == null) {
			return ok();
		}
		
		return ok(Json.toJson(getComments(localUser, post, offset)));
	}

	@Transactional
    public Result viewComments(Long postId) {
		final User localUser = Application.getLocalUser(session());
		Post post = Post.findById(postId);
		if (post == null) {
			return ok();
		}
		
        return ok(views.html.babybox.web.comments.render(Json.stringify(Json.toJson(getComments(localUser, post, 0L))), Json.stringify(Json.toJson(new UserVM(localUser)))));
    }

	@Transactional
	public static Result getConversations(Long id) {
		final User localUser = Application.getLocalUser(session());
		if (!localUser.isLoggedIn()) {
			logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
			return notFound();
		}
		
		// Only owner can view all chats for a product
		Post post = Post.findById(id);
		if (post == null) {
			return notFound();
		}
		
		if (post.owner.id == localUser.id || localUser.isSuperAdmin()) {
			List<ConversationVM> vms = new ArrayList<>();
			List<Conversation> conversations = post.findConversations();
			if (conversations != null) {
				for (Conversation conversation : conversations) {
					// archived, dont show
					if (conversation.isArchivedBy(localUser)) {
						continue;
					}

					ConversationVM vm = new ConversationVM(conversation, localUser);
					vms.add(vm);
				}
			}
			return ok(Json.toJson(vms));
		}
		return badRequest();
	}
	
	@Transactional
    public static Result reportPost() {
	    final User localUser = Application.getLocalUser(session());
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
        
        DynamicForm form = form().bindFromRequest();
        Long postId = Long.parseLong(form.get("postId"));
        String body = form.get("body");
        String reportedType = form.get("reportedType");
        
        try {
            ReportedPost reportedPost = new ReportedPost(postId, localUser.id, body, ReportedType.valueOf(reportedType));
            reportedPost.save();
            return ok();
        } catch (Exception e) {
        }
        return badRequest();
	}
	
	@Transactional
    public static Result getReportedPosts() {
	    final User localUser = Application.getLocalUser(session());
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
        
        if (!localUser.isSuperAdmin()) {
            return badRequest();
        }
        
        List<ReportedPost> reportedPosts = ReportedPost.getReportedPosts();
        List<ReportedPostVM> vms = new ArrayList<>();
        for (ReportedPost reportedPost : reportedPosts) {
            vms.add(new ReportedPostVM(reportedPost));
        }
        return ok(Json.toJson(vms));
	}
	   
    @Transactional
    public static Result deleteReportedPost(Long id) {
        final User localUser = Application.getLocalUser(session());
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
        
        if (!localUser.isSuperAdmin()) {
            return badRequest();
        }
        
        ReportedPost reportedPost = ReportedPost.findById(id);
        if (reportedPost != null) {
            reportedPost.deleted = true;
            reportedPost.save();
        }
        return badRequest();
    }
    
	@Transactional
    public static Result updateReportedPostNote() {
        final User localUser = Application.getLocalUser(session());
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
        
        if (!localUser.isSuperAdmin()) {
            return badRequest();
        }
        
        DynamicForm form = form().bindFromRequest();
        Long id = Long.parseLong(form.get("id"));
        String note = form.get("note");
        
        ReportedPost reportedPost = ReportedPost.findById(id);
        if (reportedPost == null) {
            return notFound();
        }
        
        try {
            reportedPost.note = note;
            reportedPost.save();
        } catch (Exception e) {
            return notFound();
        }
        
        return ok();
    }
    
	@Transactional
	public static Result adjustUpPostScore(Long id) {
	    final User localUser = Application.getLocalUser(session());
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
        
        Post post = Post.findById(id);
        if (post == null) {
            return notFound();
        }
        
        post.baseScoreAdjust += DefaultValues.DEFAULT_ADJUST_POST_SCORE;
        post.save();
        
        SocialRelationHandler.recordTouchPost(post, localUser);
        
        if (logger.underlyingLogger().isDebugEnabled()) {
            logger.underlyingLogger().debug("[u="+localUser.getId()+"][p="+post.id+"] adjustUpPostScore()");
        }
	    return ok(DefaultValues.DEFAULT_ADJUST_POST_SCORE+"");
	}
	
	@Transactional
	public static Result adjustDownPostScore(Long id) {
	    final User localUser = Application.getLocalUser(session());
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
        
        Post post = Post.findById(id);
        if (post == null) {
            return notFound();
        }
        
        post.baseScoreAdjust -= DefaultValues.DEFAULT_ADJUST_POST_SCORE;
        post.save();
        
        SocialRelationHandler.recordTouchPost(post, localUser);
        
        if (logger.underlyingLogger().isDebugEnabled()) {
            logger.underlyingLogger().debug("[u="+localUser.getId()+"][p="+post.id+"] adjustDownPostScore()");
        }
        return ok(-DefaultValues.DEFAULT_ADJUST_POST_SCORE+"");
    }

	@Transactional
    public static Result resetAdjustPostScore(Long id) {
	    final User localUser = Application.getLocalUser(session());
        if (!localUser.isLoggedIn()) {
            logger.underlyingLogger().error(String.format("[u=%d] User not logged in", localUser.id));
            return notFound();
        }
        
        Post post = Post.findById(id);
        if (post == null) {
            return notFound();
        }
        
        post.baseScoreAdjust = 0L;
        post.save();

        SocialRelationHandler.recordTouchPost(post, localUser);
        
        if (logger.underlyingLogger().isDebugEnabled()) {
            logger.underlyingLogger().debug("[u="+localUser.getId()+"][p="+post.id+"] resetAdjustPostScore()");
        }
        return ok();
    }
}