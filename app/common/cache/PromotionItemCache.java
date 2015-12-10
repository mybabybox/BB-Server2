package common.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.PromotionItem;
import models.PromotionItem.ItemType;

/**
 * 
 */
public class PromotionItemCache {
    // Permanent cache loaded up on system startup.

    private static List<PromotionItem> promotionItems;
    private static Map<Long, PromotionItem> idsMap;
    private static Map<ItemType, List<PromotionItem>> itemTypesMap;

    static {
        promotionItems = PromotionItem.loadPromotionItems();
        idsMap = new HashMap<>();
        itemTypesMap = new HashMap<>();
        for (PromotionItem item : promotionItems) {
            // 1. id -> item
            idsMap.put(item.id, item);
            
            // 2. itemType -> item
            List<PromotionItem> items = itemTypesMap.get(item.itemType);
            if (items == null) {
                items = new ArrayList<>();
                itemTypesMap.put(item.itemType, items);
            }
            items.add(item);
        }
    }

    public static List<PromotionItem> getPromotionItems() {
        return promotionItems;
    }
    
    public static PromotionItem getPromotionItem(Long id) {
        return idsMap.get(id);
    }
    
    public static List<PromotionItem> getPromotionItems(ItemType itemType) {
        if (itemTypesMap.containsKey(itemType)) {
            return itemTypesMap.get(itemType);
        }
        return null;
    }
}