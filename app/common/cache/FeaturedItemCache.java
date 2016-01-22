package common.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.FeaturedItem;
import models.FeaturedItem.ItemType;

/**
 * 
 */
public class FeaturedItemCache {
    private static final play.api.Logger logger = play.api.Logger.apply(FeaturedItemCache.class);

    private static List<FeaturedItem> featuredItems;
    private static Map<Long, FeaturedItem> idsMap;
    private static Map<ItemType, List<FeaturedItem>> itemTypesMap;

    synchronized public static void refresh() {
        featuredItems = FeaturedItem.loadFeaturedItems();
        idsMap = new HashMap<>();
        itemTypesMap = new HashMap<>();
        for (FeaturedItem item : featuredItems) {
            // 1. id -> item
            idsMap.put(item.id, item);
            
            // 2. itemType -> item
            List<FeaturedItem> items = itemTypesMap.get(item.itemType);
            if (items == null) {
                items = new ArrayList<>();
                itemTypesMap.put(item.itemType, items);
            }
            items.add(item);
        }
        logger.underlyingLogger().debug("FeaturedItemCache refreshed");
    }
    
    public static List<FeaturedItem> getFeaturedItems() {
        return featuredItems;
    }
    
    public static FeaturedItem getFeaturedItem(Long id) {
        return idsMap.get(id);
    }
    
    public static List<FeaturedItem> getFeaturedItems(ItemType itemType) {
        if (itemTypesMap.containsKey(itemType)) {
            return itemTypesMap.get(itemType);
        }
        return null;
    }
}