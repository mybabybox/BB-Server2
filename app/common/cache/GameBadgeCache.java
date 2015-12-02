package common.cache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.GameBadge;
import models.GameBadge.BadgeType;

/**
 * 
 */
public class GameBadgeCache {
    // Permanent cache loaded up on system startup.

    private static List<GameBadge> gameBadges;
    private static Map<Long, GameBadge> idsMap;
    private static Map<BadgeType, GameBadge> badgeTypesMap;

    static {
        gameBadges = GameBadge.loadGameBadges();
        idsMap = new HashMap<>();
        badgeTypesMap = new HashMap<>();
        for (GameBadge gameBadge : gameBadges) {
            idsMap.put(gameBadge.id, gameBadge);
            badgeTypesMap.put(gameBadge.badgeType, gameBadge);
        }
    }

    public static List<GameBadge> getGameBadges() {
        return gameBadges;
    }
    
    public static GameBadge getGameBadge(Long id) {
        if (idsMap.containsKey(id)) {
            return idsMap.get(id);
        }
        return null;
    }
    
    public static GameBadge getGameBadge(BadgeType badgeType) {
        if (badgeTypesMap.containsKey(badgeType)) {
            return badgeTypesMap.get(badgeType);
        }
        return null;
    }
}