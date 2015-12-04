package common.utils;

import java.util.regex.Pattern;

public class ValidationUtil {
    
    public static final int USER_DISPLAYNAME_MIN_CHAR = 2;
    public static final int USER_DISPLAYNAME_MAX_CHAR = 18;

    private static final String EMAIL_FORMAT_REGEX =
            "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
    private static final String USER_DISPLAYNAME_FORMAT_REGEX =
            "^[_\\p{L}0-9-\\+]+(\\.[_\\p{L}0-9-]+)*$";      // \p{L} matches letter in any language
    //private static final String USER_DISPLAYNAME_FORMAT_REGEX =
    //        "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*$";

    public static boolean isEmailValid(String email) {
        if (StringUtil.hasWhitespace(email)) {
            return false;
        }
        return Pattern.matches(EMAIL_FORMAT_REGEX, email);
    }
    
    public static boolean isDisplayNameValid(String displayName) {
        if (StringUtil.hasWhitespace(displayName)) {
            return false;
        }
        return Pattern.matches(USER_DISPLAYNAME_FORMAT_REGEX, displayName);   
    }
}
