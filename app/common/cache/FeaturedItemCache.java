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
    // Permanent cache loaded up on system startup.

    private static List<FeaturedItem> featuredItems;
    private static Map<Long, FeaturedItem> idsMap;
    private static Map<ItemType, List<FeaturedItem>> itemTypesMap;

    static {
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