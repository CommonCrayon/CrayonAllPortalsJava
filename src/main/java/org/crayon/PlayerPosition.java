package org.crayon;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Player position and facing, as copied to the clipboard by F3+C. */
record PlayerPosition(String dimension, double x, double z, double yaw) {

    // /execute in minecraft:<dimension> run tp @s <x> <y> <z> <yaw> <pitch>
    private static final Pattern F3C = Pattern.compile(
            "/execute in minecraft:(\\w+) run tp @s ([\\-\\d.]+) ([\\-\\d.]+) ([\\-\\d.]+) ([\\-\\d.]+) ([\\-\\d.]+)");

    static Optional<PlayerPosition> parse(String text) {
        if (text == null) return Optional.empty();
        Matcher matcher = F3C.matcher(text);
        if (!matcher.find()) return Optional.empty();
        try {
            return Optional.of(new PlayerPosition(
                    matcher.group(1),
                    Double.parseDouble(matcher.group(2)),
                    Double.parseDouble(matcher.group(4)),
                    Double.parseDouble(matcher.group(5))));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
