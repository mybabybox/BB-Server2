package common.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import common.collection.Pair;
import controllers.Application;

/**
 *  Handle Html, Link conversion.
 */
public class HtmlUtil {
    public static boolean IN_TEST = false;

    static final String TAGWORD_MARKER = "#!";
    private static final Pattern TAGWORD_REGEX = Pattern.compile("(\\s?|^)"+TAGWORD_MARKER+"(.+?)(\\s|$)");
    private static final int URL_TRUNCATE_LEN = 60;

    /**
     * Convert the given text to Html, with href links.
     * @param text
     * @return
     */
    public static String convertTextToHtml(String text) {
        // escape html special chars
        text = escapeHtmlSpecialChars(text);

        // convert any url text to href click-able links
        String result = text;
        if (text != null && text.contains("http")) {
            String str = "(?i)\\b((?:https?://|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:\'\".,<>?«»“”‘’]))";
            Pattern patt = Pattern.compile(str);
            Matcher matcher = patt.matcher(text);

            while (matcher.find()) {
                String urlRef = matcher.group();
                String urlDisplay;
                if (urlRef.length() > URL_TRUNCATE_LEN) {
                    urlDisplay = urlRef.substring(0, URL_TRUNCATE_LEN)+"...";
                } else {
                    urlDisplay = urlRef;
                }
                result = result.replace(urlRef, "<a href=\""+urlRef+"\" target=\"_blank\">"+urlDisplay+"</a>");
            }
        }
        return result;
    }

    /**
     * Convert the given text to Html, and extract TagWords.
     * @param text
     * @return
     */
    public static Pair<String, String> convertTextWithTagWords(String text) {
        StringBuilder tagWords = new StringBuilder();
        String resultText = text;

        Matcher matcher = TAGWORD_REGEX.matcher(text);
        while (matcher.find()) {
            String tagword = matcher.group(2);
            if (tagword != null) {
                // cover both space before or after tagword
                resultText = resultText.replace(TAGWORD_MARKER+tagword+" ", "");
                resultText = resultText.replace(" "+TAGWORD_MARKER+tagword, "");
                tagWords.append(tagword.toUpperCase()).append(",");
            }
        }

        String resultTagWords = (tagWords.length() > 0) ? tagWords.toString() : null;
        return new Pair<>(resultText, resultTagWords);
    }


    private static String escapeHtmlSpecialChars(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    public static String appendImage(String src, int width, int height) {
        return "<img src='"+src+"' width='"+width+"' height='"+height+"' border='0' style='display:block;border:none;outline:none;text-decoration:none'></img>";
    }
    
    public static String appendBr() {
        return "<br>";
    }
    
    public static String appendP(String text) {
        return "<p>"+text+"</p>";
    }
    
    public static String appendTitle(String title) {
        return "<h2>"+title+"</h2>";
    }
    
    public static String fullUrl(String url) {
        if (url.startsWith("http")) {
            return url;
        }
        
        if (!url.startsWith("/")) {
            url = "/"+url;
        }
        
        return Application.APPLICATION_BASE_URL + url;
    }
}
