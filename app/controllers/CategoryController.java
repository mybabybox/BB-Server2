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
import viewmodel.CategoryVM;
import viewmodel.CommentVM;
import viewmodel.ConversationVM;
import viewmodel.PostVM;
import viewmodel.PostVMLite;
import viewmodel.ResponseStatusVM;
import viewmodel.UserVM;
import common.model.FeedFilter;
import common.model.FeedFilter.FeedType;
import common.utils.HtmlUtil;
import common.utils.HttpUtil;
import common.utils.ImageFileUtil;
import common.utils.NanoSecondStopWatch;
import controllers.Application.DeviceType;
import domain.DefaultValues;
import domain.SocialObjectType;

public class CategoryController extends Controller{
	private static play.api.Logger logger = play.api.Logger.apply(CategoryController.class);
	
	@Inject
    FeedHandler feedHandler;
    
	@Transactional 
	public Result getCategoryPopularFeed(Long id, String conditionType, Long offset){
		final User localUser = Application.getLocalUser(session());
		FeedFilter.ConditionType condition = FeedFilter.ConditionType.ALL;
		try {
		    condition = FeedFilter.ConditionType.valueOf(conditionType);
		} catch (Exception e) {
		    ;
		}
		List<PostVMLite> vms = null;
		if (FeedFilter.ConditionType.ALL.equals(condition)) {
		    vms = feedHandler.getFeedPosts(id, offset, localUser, FeedType.CATEGORY_POPULAR);
		} else if (FeedFilter.ConditionType.NEW.equals(condition)) {
		    vms = feedHandler.getFeedPosts(id, offset, localUser, FeedType.CATEGORY_POPULAR_NEW);
		} else if (FeedFilter.ConditionType.USED.equals(condition)) {
		    vms = feedHandler.getFeedPosts(id, offset, localUser, FeedType.CATEGORY_POPULAR_USED);
		}
		return ok(Json.toJson(vms));
	}

	@Transactional 
	public Result getCategoryNewestFeed(Long id, String conditionType, Long offset){
		final User localUser = Application.getLocalUser(session());
		List<PostVMLite> vms = feedHandler.getFeedPosts(id, offset, localUser, FeedType.CATEGORY_NEWEST);
		return ok(Json.toJson(vms));
	}
	
	@Transactional 
	public Result getCategoryPriceLowHighFeed(Long id, String conditionType, Long offset){
		final User localUser = Application.getLocalUser(session());
		List<PostVMLite> vms = feedHandler.getFeedPosts(id, offset, localUser, FeedType.CATEGORY_PRICE_LOW_HIGH);
		return ok(Json.toJson(vms));
	}
	
	@Transactional 
	public Result getCategoryPriceHighLowFeed(Long id, String conditionType, Long offset) {
		final User localUser = Application.getLocalUser(session());
		List<PostVMLite> vms = feedHandler.getFeedPosts(id, offset, localUser, FeedType.CATEGORY_PRICE_HIGH_LOW);
		return ok(Json.toJson(vms));
	}
	
	@Transactional
    public static Result getCategories(){
        List<CategoryVM> categoryList = new ArrayList<CategoryVM>();
        for(Category category : Category.getCategories()){
            CategoryVM cvm = new CategoryVM(category);
            categoryList.add(cvm);
        }
        return ok(Json.toJson(categoryList));
    }
    
	@Transactional
    public static Result getSubCategories(Long categoryId){
        return ok(Json.toJson(getSubCategoryVMs(categoryId)));
    }
    
    public static List<CategoryVM> getSubCategoryVMs(Long categoryId) {
        List<CategoryVM> vms = new ArrayList<CategoryVM>();
        List<Category> subCategories = Category.getSubCategories(categoryId);
        if (subCategories != null) {
            for(Category category : subCategories){
                CategoryVM vm = new CategoryVM(category);
                vms.add(vm);
            }
        }
        return vms;
    }
    
    @Transactional
    public static Result getCategory(Long id){
        Category category = Category.findById(id);
        if (category == null) {
            logger.underlyingLogger().warn(String.format("[cat=%d] Category not found", id));
            return notFound();
        }
        
        CategoryVM categoryVM = new CategoryVM(category);
        return ok(Json.toJson(categoryVM));
    }
    
    @Transactional
    public Result viewCategory(Long id, String catagoryFilter){
        User localUser = Application.getLocalUser(session());
        Category category = Category.findById(id);
        if (category == null) {
            logger.underlyingLogger().warn(String.format("[category=%d][u=%d] Category not found", id, localUser.id));
            return Application.pathNotFound();
        }
        
        List<CategoryVM> subCategoryVMs = getSubCategoryVMs(id);
        
        CategoryVM categoryVM = new CategoryVM(category);
        List<PostVMLite> postVMs = new ArrayList<>();
        
        switch(catagoryFilter){
        case "popular":
            postVMs = feedHandler.getFeedPosts(id, 0L, localUser, FeedType.CATEGORY_POPULAR);
            break;
        case "newest":
            postVMs = feedHandler.getFeedPosts(id, 0L, localUser, FeedType.CATEGORY_NEWEST);
            break;
        case "high2low":
            postVMs = feedHandler.getFeedPosts(id, 0L, localUser, FeedType.CATEGORY_PRICE_HIGH_LOW);
            break;
        case "low2high":
            postVMs = feedHandler.getFeedPosts(id, 0L, localUser, FeedType.CATEGORY_PRICE_LOW_HIGH);
            break;
        }
        
        String metaTags = Application.generateHeaderMeta(category.name, category.description, category.icon);
        return ok(views.html.babybox.web.category.render(
                Json.stringify(Json.toJson(subCategoryVMs)),
                Json.stringify(Json.toJson(categoryVM)), 
                Json.stringify(Json.toJson(postVMs)), 
                Json.stringify(Json.toJson(new UserVM(localUser))),
                metaTags));
    }
}