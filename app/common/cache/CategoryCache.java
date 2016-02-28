package common.cache;

import models.Category;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 */
public class CategoryCache {
    // Permanent cache loaded up on system startup.

    private static List<Category> categories;
    private static final Map<Long, List<Category>> categoryToSubCategoryiesMap = new HashMap<>();
    private static final Map<Long, Category> allCategoriesMap = new HashMap<>();

    static {
        categories = Category.loadCategories();
        for (Category category : categories) {
        	allCategoriesMap.put(category.id, category);
        }
        
        List<Category> subCategories = Category.loadSubCategories();
        for (Category subCategory : subCategories) {
            allCategoriesMap.put(subCategory.id, subCategory);
            if (subCategory.parent != null) {
                if (!categoryToSubCategoryiesMap.containsKey(subCategory.parent.id)) {
                    categoryToSubCategoryiesMap.put(subCategory.parent.id, new ArrayList<Category>());
                }
                categoryToSubCategoryiesMap.get(subCategory.parent.id).add(subCategory);
            }
        }
    }

    public static List<Category> getCategories() {
        return categories;
    }
    
    public static List<Category> getSubCategories(Long categoryId) {
        return categoryToSubCategoryiesMap.get(categoryId);
    }
    
    public static Category getCategory(Long id) {
    	return allCategoriesMap.get(id);
    }
}
