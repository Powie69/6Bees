package powie.sixbees.utils;

public class StringUtils {
    public static String possessiveNoun(String string) {
        if (string == null || string.isBlank()) return "";
        return string.endsWith("s") ? string + "'" : string + "'s";
    }

    public static String getPlayerMessage(String message) {
        return message.substring(message.indexOf("»") + 1).trim();
    }
}
