package common.cache;

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
    private static Map<ItemType, PromotionItem> itemTypesMap;

    static {
        promotionItems = PromotionItem.loadPromotionItems();
        idsMap = new HashMap<>();
        itemTypesMap = new HashMap<>();
        for (PromotionItem promotionItem : promotionItems) {
            idsMap.put(promotionItem.id, promotionItem);
            itemTypesMap.put(promotionItem.itemType, promotionItem);
        }
    }

    public static List<PromotionItem> getPromotionItems() {
        return promotionItems;
    }
    
    public static PromotionItem getPromotionItem(Long id) {
        if (idsMap.containsKey(id)) {
            return idsMap.get(id);
        }
        return null;
    }
    
    public static PromotionItem getPromotionItem(ItemType itemType) {
        if (itemTypesMap.containsKey(itemType)) {
            return itemTypesMap.get(itemType);
        }
        return null;
    }
}