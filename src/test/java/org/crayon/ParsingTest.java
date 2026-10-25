package org.crayon;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParsingTest {

    private static final double EPS = 1e-9;

    @Test
    void parsesF3CClipboard() {
        PlayerPosition p = PlayerPosition
                .parse("/execute in minecraft:overworld run tp @s 330.39 75.00 153.07 -71.79 29.14")
                .orElseThrow();
        assertEquals("overworld", p.dimension());
        assertEquals(330.39, p.x(), EPS);
        assertEquals(153.07, p.z(), EPS);
        assertEquals(-71.79, p.yaw(), EPS);
    }

    @Test
    void parsesNetherF3C() {
        PlayerPosition p = PlayerPosition
                .parse("/execute in minecraft:the_nether run tp @s -1751.25 64.00 -1867.25 1234.50 0.00")
                .orElseThrow();
        assertEquals("the_nether", p.dimension());
        assertEquals(1234.5, p.yaw(), EPS);
    }

    @Test
    void ignoresOtherClipboardText() {
        assertTrue(PlayerPosition.parse("just some copied text").isEmpty());
        assertTrue(PlayerPosition.parse(null).isEmpty());
    }

    @Test
    void parsesReadmeExample() {
        List<Target> targets = Target.parseAll("""
                "0:Steve"
                [3,2024,310]
                [21,10085,5017]
                [37,12618,6806]
                [36,14063,2784]
                [57,16504,5538]
                [85,20213,3297]
                [86,19333,6757]
                [121,20924,10810]
                """);
        assertEquals(8, targets.size());
        assertEquals(new Target(3, 2024, 310, ""), targets.get(0));
        assertEquals(new Target(121, 20924, 10810, ""), targets.get(7));
    }

    @Test
    void parsesNotesSpacesAndNegativeCoordinates() {
        List<Target> targets = Target.parseAll("[ 107 , -14010 , -14938 ] \"lava lake nearby\"\n[1, 2048.5, 0]");
        assertEquals(2, targets.size());
        assertEquals(new Target(107, -14010, -14938, "lava lake nearby"), targets.get(0));
        assertEquals(2048.5, targets.get(1).x(), EPS);
    }

    @Test
    void skipsLinesThatDontFit() {
        List<Target> targets = Target.parseAll("[99999999999, 1, 2]\nnot a target\n[1, 2, 3]");
        assertEquals(List.of(new Target(1, 2, 3, "")), targets);
    }

    @Test
    void targetKeyIgnoresNoteButNotCoordinates() {
        assertEquals(new Target(3, 2024, 310, "a").key(), new Target(3, 2024, 310, "b").key());
        assertNotEquals(new Target(3, 2024, 310, "").key(), new Target(3, 2024, 311, "").key());
    }
}
