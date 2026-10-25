package org.crayon;

import java.awt.*;

final class WindowPlacement {

    private WindowPlacement() {}

    /**
     * Moves the window to its saved position if that's still on a connected screen
     * (e.g. a second monitor might be unplugged), otherwise centres it. Call after sizing the window.
     */
    static void place(Window window, Point saved) {
        if (saved != null) {
            // The top strip must be visible so the window can still be grabbed and dragged
            Rectangle grabArea = new Rectangle(saved.x, saved.y, Math.max(1, Math.min(window.getWidth(), 120)), 30);
            for (GraphicsDevice device : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
                if (device.getDefaultConfiguration().getBounds().intersects(grabArea)) {
                    window.setLocation(saved);
                    return;
                }
            }
        }
        window.setLocationRelativeTo(null);
    }
}
