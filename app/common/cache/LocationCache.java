package common.cache;

import models.Location;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * Date: 27/7/14
 * Time: 10:46 AM
 * To change this template use File | Settings | File Templates.
 */
public class LocationCache {
    // Permanent cache loaded up on system startup.

    // Districts
    private static List<Location> hkDistrictsList;
    private static final Map<Long, Location> hkDistrictsMap = new HashMap<>();

    static {
        hkDistrictsList = Location.getHongKongDistricts();
        for (Location district : hkDistrictsList) {
            hkDistrictsMap.put(district.id, district);
        }
    }

    public static Location getDistrict(Long locationId) {
        return hkDistrictsMap.get(locationId);
    }

    public static List<Location> getHongKongDistricts() {
        return hkDistrictsList;
    }
}
