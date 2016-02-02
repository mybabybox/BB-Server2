package common.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.IllegalFieldValueException;

public class DateTimeUtil {
    
    public static final long SECOND_MILLIS = 1000;
    public static final long MINUTE_MILLIS = 60 * SECOND_MILLIS;
    public static final long HOUR_MILLIS = 60 * MINUTE_MILLIS;
    public static final long DAY_MILLIS = 24 * HOUR_MILLIS;
    public static final long WEEK_MILLIS = 7 * DAY_MILLIS;
    public static final long MONTH_MILLIS = 30 * DAY_MILLIS;
    public static final long YEAR_MILLIS = 365 * DAY_MILLIS;

    private static DateTime TODAY;
    
    private static SimpleDateFormat formatter = new SimpleDateFormat("MM-dd-yyyy HH:mm");
    
    public static boolean withinSecs(long start, long end, long secs) {
        return Math.abs(end - start) < secs * SECOND_MILLIS;
    }
    
    public static boolean withinMins(long start, long end, long mins) {
        return Math.abs(end - start) < mins * MINUTE_MILLIS;
    }
    
    public static boolean withinHours(long start, long end, long hours) {
        return Math.abs(end - start) < hours * HOUR_MILLIS;
    }
    
    public static boolean withinDays(long start, long end, long days) {
        return Math.abs(end - start) < days * DAY_MILLIS;
    }

    public static boolean withinWeeks(long start, long end, long weeks) {
        return Math.abs(end - start) < weeks * WEEK_MILLIS;
    }
    
    public static boolean withinADay(long start, long end) {
        return withinDays(start, end, 1);
    }
    
    public static boolean withinAWeek(long start, long end) {
        return withinWeeks(start, end, 1);
    }
    
    public static DateTime getToday() {
        DateTime now = DateTime.now();
        if (TODAY != null && 
                now.getYear() == TODAY.getYear() && 
                now.getMonthOfYear() == TODAY.getMonthOfYear() && 
                now.getDayOfMonth() == TODAY.getDayOfMonth()) {
            return TODAY;
        }
        
        TODAY = new DateTime(now.getYear(), now.getMonthOfYear(), now.getDayOfMonth(), 0, 0, 0);
        return TODAY;
    }
    
    public static DateTime getTomorrow() {
        return getToday().plusDays(1);
    }
    
    public static boolean isDateOfBirthValid(String year, String month, String day) {
        try {
            parseDate(year, month, day);
        } catch (IllegalFieldValueException ie) {
            return false;
        }
        return true;
    }
    
    public static boolean isDayOfMonthValid(String year, String month, String day) {
        try {
            parseDate(year, month, day);
        } catch (IllegalFieldValueException ie) {
            if ("dayOfMonth".equals(ie.getFieldName()))
                return false;
        }
        return true;
    }
    
    public static DateTime parseDate(String year, String month, String day) {
        if (year.startsWith("<")) {     // e.g. <1960
            year = year.replaceAll("<", "").trim();
        }
        
        if (StringUtils.isEmpty(day)) {
            day = "1";
        }
        
        int y = Integer.valueOf(year);
        int m = Integer.valueOf(month);
        int d = Integer.valueOf(day);
        
        if (d <= 0) {       // day is optional most of the times
            d = 1;
        }
        
        return new DateTime(y, m ,d, 12, 0);
    }
    
    public static String toString(Date date) {
        return formatter.format(date);
    }
}
