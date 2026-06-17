package powie.sixbees.utils;

public class StringUtils {
    public static String parsePlayerMessage(String message) {
        return message.substring(message.indexOf("»") + 1).trim();
    }

    public static String parsePlayerName(String input) {
        String prefix = input.substring(0, input.indexOf('»')).trim();
        if (prefix.startsWith("[")) return prefix.substring(prefix.indexOf(']') + 1).trim();
        return prefix;
    }
}
