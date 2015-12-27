package common.cache;

import models.Icon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 */
public class IconCache {
    // Permanent cache loaded up on system startup.

    private static List<Icon> countryIcons;
    private static Map<String, Icon> countryCodeIconsMap;

    static {
        countryIcons = Icon.loadCountryIcons();
        countryCodeIconsMap = new HashMap<>();
        for (Icon icon : countryIcons) {
            countryCodeIconsMap.put(icon.code, icon);
        }
    }

    public static List<Icon> getCountryIcons() {
        return countryIcons;
    }
    
    public static Icon getCountryIcon(String code) {
        return countryCodeIconsMap.get(code);
    }
}
