package org.crayon;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** One stronghold (or ring) to navigate to, in overworld coordinates. */
record Target(int id, double x, double z, String note) {

    // [id, x, z] optionally followed by "note"
    private static final Pattern LINE = Pattern.compile(
            "\\[\\s*([+-]?\\d+)\\s*,\\s*([+-]?\\d+(?:\\.\\d+)?)\\s*,\\s*([+-]?\\d+(?:\\.\\d+)?)\\s*]\\s*(?:\"([^\"]*)\")?");

    Target {
        if (note == null) note = "";
    }

    /** Parses every target line in the text. Lines that don't match (or don't fit in a number) are skipped. */
    static List<Target> parseAll(String text) {
        List<Target> targets = new ArrayList<>();
        Matcher matcher = LINE.matcher(text);
        while (matcher.find()) {
            try {
                targets.add(new Target(
                        Integer.parseInt(matcher.group(1)),
                        Double.parseDouble(matcher.group(2)),
                        Double.parseDouble(matcher.group(3)),
                        matcher.group(4)));
            } catch (NumberFormatException ignored) {
                // e.g. an id too large for an int
            }
        }
        return targets;
    }

    /** Identity used to find the same target again after a relaunch. Ignores the note. */
    String key() {
        return id + "," + x + "," + z;
    }
}
