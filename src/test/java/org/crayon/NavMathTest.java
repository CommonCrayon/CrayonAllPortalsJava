package org.crayon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NavMathTest {

    private static final double EPS = 1e-9;

    @Test
    void requiredYawMatchesMinecraftCompass() {
        // 0 = south (+Z), 90 = west (-X), -90 = east (+X), ±180 = north (-Z)
        assertEquals(0.0, NavMath.requiredYaw(0, 0, 0, 100), EPS);
        assertEquals(90.0, NavMath.requiredYaw(0, 0, -100, 0), EPS);
        assertEquals(-90.0, NavMath.requiredYaw(0, 0, 100, 0), EPS);
        assertEquals(180.0, Math.abs(NavMath.requiredYaw(0, 0, 0, -100)), EPS);
    }

    @Test
    void requiredYawIsRelativeToPlayer() {
        // Target is south-east of the player
        assertEquals(-45.0, NavMath.requiredYaw(10, 10, 110, 110), EPS);
    }

    @Test
    void wrapDegreesKeepsAnglesInRange() {
        assertEquals(0.0, NavMath.wrapDegrees(360), EPS);
        assertEquals(-170.0, NavMath.wrapDegrees(190), EPS);
        assertEquals(170.0, NavMath.wrapDegrees(-190), EPS);
        assertEquals(-180.0, NavMath.wrapDegrees(180), EPS);
        assertEquals(10.0, NavMath.wrapDegrees(730), EPS);
    }

    @Test
    void turnAmountTakesShortestWayAcrossTheSeam() {
        assertEquals(20.0, NavMath.turnAmount(170, -170), EPS);  // right, through ±180
        assertEquals(-20.0, NavMath.turnAmount(-170, 170), EPS); // left, through ±180
        assertEquals(-45.0, NavMath.turnAmount(0, -45), EPS);
    }

    @Test
    void turnAmountHandlesUnwrappedF3CYaw() {
        // F3+C reports yaw outside ±180 after the player has spun around
        assertEquals(-10.0, NavMath.turnAmount(730, 0), EPS);
        assertEquals(10.0, NavMath.turnAmount(-370, 0), EPS);
    }

    @Test
    void distanceIsNotTruncatedPerAxis() {
        assertEquals(5.0, NavMath.distance(0, 0, 3, 4), EPS);
        // The old code truncated each axis to an int first, which gave 0 here
        assertEquals(1.2728, NavMath.distance(0, 0, 0.9, 0.9), 1e-4);
    }

    @Test
    void netherDividesByEight() {
        assertEquals(256.0, NavMath.toDimension(2048, "the_nether"), EPS);
        assertEquals(-1751.25, NavMath.toDimension(-14010, "the_nether"), EPS);
        assertEquals(2048.0, NavMath.toDimension(2048, "overworld"), EPS);
    }

    @Test
    void chunkRoundsTowardNegativeInfinity() {
        assertEquals(0, NavMath.chunk(0));
        assertEquals(0, NavMath.chunk(15.9));
        assertEquals(1, NavMath.chunk(16));
        assertEquals(-1, NavMath.chunk(-0.5));
        assertEquals(-1, NavMath.chunk(-16));
        assertEquals(-2, NavMath.chunk(-17));
    }
}
