package io.github.abcnova.friendlyfire;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class VersionComparator {
    private static final Pattern NUMBER = Pattern.compile("\\d+");

    private VersionComparator() { }

    static int compare(String first, String second) {
        Matcher left = NUMBER.matcher(first == null ? "" : first);
        Matcher right = NUMBER.matcher(second == null ? "" : second);
        while (true) {
            boolean hasLeft = left.find();
            boolean hasRight = right.find();
            if (!hasLeft && !hasRight) break;
            long a = hasLeft ? parse(left.group()) : 0;
            long b = hasRight ? parse(right.group()) : 0;
            int result = Long.compare(a, b);
            if (result != 0) return result;
        }
        return 0;
    }

    private static long parse(String part) {
        try { return Long.parseLong(part); }
        catch (NumberFormatException ignored) { return Long.MAX_VALUE; }
    }
}
