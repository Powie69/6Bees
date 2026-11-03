package powie.sixbees.utils;

public class StringUtils {
    public static String parsePlayerMessage(String message) {
        return message.substring(message.indexOf("»") + 1).trim();
    }

    public static String parsePlayerName(String message) {
        String prefix = message.substring(0, message.indexOf('»')).trim();
        if (prefix.startsWith("[")) return prefix.substring(prefix.indexOf(']') + 1).trim();
        return prefix;
    }

    public static String toPossessive(String input) {
        String trimmed = input.trim();
        if (trimmed.isEmpty()) return "";
        boolean endsWithS = trimmed.substring(trimmed.length() - 1).equalsIgnoreCase("s");
        return endsWithS ? trimmed + "'" : trimmed + "'s";
    }
}
