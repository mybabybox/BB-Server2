package common.cache;

import models.Country;
import models.Country.CountryCode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 */
public class CountryCache {
    // Permanent cache loaded up on system startup.

    private static List<Country> countries;
    private static Map<CountryCode, Country> countriesMap;

    static {
        countries = Country.loadCountries();
        countriesMap = new HashMap<>();
        for (Country country : countries) {
            countriesMap.put(country.code, country);
        }
    }

    public static List<Country> getCountries() {
        return countries;
    }
    
    public static Country getCountry(CountryCode code) {
        return countriesMap.get(code);
    }
}
