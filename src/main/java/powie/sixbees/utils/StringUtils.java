package powie.sixbees.utils;

public class StringUtils {
    public static String possessiveNoun(String string) {
        if (string == null || string.isBlank()) return "";
        return string.endsWith("s") ? string + "'" : string + "'s";
    }
}
