package org.crayon;

/**
 * Navigation maths, kept free of Swing so it can be unit tested.
 * Yaw follows Minecraft's F3 convention: 0 = south (+Z), 90 = west (-X), -90 = east (+X), ±180 = north (-Z).
 */
final class NavMath {

    /** Overworld blocks per nether block. */
    static final double NETHER_SCALE = 8.0;

    private NavMath() {}

    /** The yaw (as shown in F3) that faces from the player toward the target. Range (-180, 180]. */
    static double requiredYaw(double playerX, double playerZ, double targetX, double targetZ) {
        return Math.toDegrees(Math.atan2(playerX - targetX, targetZ - playerZ));
    }

    /** Wraps any angle into [-180, 180). */
    static double wrapDegrees(double degrees) {
        return degrees - 360.0 * Math.floor((degrees + 180.0) / 360.0);
    }

    /** Shortest turn from the current yaw to the required yaw. Positive means turn right. */
    static double turnAmount(double currentYaw, double requiredYaw) {
        return wrapDegrees(requiredYaw - currentYaw);
    }

    static double distance(double playerX, double playerZ, double targetX, double targetZ) {
        return Math.hypot(targetX - playerX, targetZ - playerZ);
    }

    static boolean isNether(String dimension) {
        return dimension != null && dimension.contains("nether");
    }

    /** Converts an overworld coordinate into the player's current dimension. */
    static double toDimension(double overworldCoord, String dimension) {
        return isNether(dimension) ? overworldCoord / NETHER_SCALE : overworldCoord;
    }

    /** Chunk index containing a block coordinate (rounds toward negative infinity, like Minecraft). */
    static int chunk(double blockCoord) {
        return (int) Math.floor(blockCoord / 16.0);
    }
}
